use argon2::{Algorithm, Argon2, Params, Version};
use ring::aead::Nonce;
use zeroize::Zeroizing;

use super::{
    constants::{
        AES_GCM_NONCE_LEN, AES_GCM_TAG_LEN, DEFAULT_ARGON2_ITERATIONS, DEFAULT_ARGON2_MEMORY_KIB, DEFAULT_ARGON2_OUTPUT_LEN, DEFAULT_ARGON2_PARALLELISM, MAX_ARGON2_ITERATIONS,
        MAX_ARGON2_MEMORY_KIB, MAX_ARGON2_PARALLELISM,
    },
    types::{CipherParams, KdfParams},
};
use crate::KeystoreError;

impl KdfParams {
    pub(super) fn default_argon2id() -> Result<Self, KeystoreError> {
        Ok(Self::Argon2id {
            memory_kib: DEFAULT_ARGON2_MEMORY_KIB,
            iterations: DEFAULT_ARGON2_ITERATIONS,
            parallelism: DEFAULT_ARGON2_PARALLELISM,
            salt: gem_crypto::random::bytes()?,
            output_len: DEFAULT_ARGON2_OUTPUT_LEN,
        })
    }

    pub(super) fn with_random_salt(&self) -> Result<Self, KeystoreError> {
        match self {
            Self::Argon2id {
                memory_kib,
                iterations,
                parallelism,
                output_len,
                ..
            } => Ok(Self::Argon2id {
                memory_kib: *memory_kib,
                iterations: *iterations,
                parallelism: *parallelism,
                salt: gem_crypto::random::bytes()?,
                output_len: *output_len,
            }),
        }
    }

    pub(super) fn validate(&self) -> Result<(), KeystoreError> {
        match self {
            Self::Argon2id {
                memory_kib,
                iterations,
                parallelism,
                output_len,
                ..
            } => {
                if *memory_kib == 0 || *memory_kib > MAX_ARGON2_MEMORY_KIB {
                    return Err(KeystoreError::corrupt_file("invalid Argon2 memory"));
                }
                if *iterations == 0 || *iterations > MAX_ARGON2_ITERATIONS {
                    return Err(KeystoreError::corrupt_file("invalid Argon2 iterations"));
                }
                if *parallelism == 0 || *parallelism > MAX_ARGON2_PARALLELISM {
                    return Err(KeystoreError::corrupt_file("invalid Argon2 parallelism"));
                }
                if *output_len != DEFAULT_ARGON2_OUTPUT_LEN {
                    return Err(KeystoreError::corrupt_file("invalid Argon2 output length"));
                }
                Ok(())
            }
        }
    }
}

impl CipherParams {
    pub(super) fn random_aes256_gcm() -> Result<Self, KeystoreError> {
        Ok(Self::Aes256Gcm {
            nonce: gem_crypto::random::bytes::<AES_GCM_NONCE_LEN>()?,
            tag_len: AES_GCM_TAG_LEN,
        })
    }

    pub(super) fn validate(&self) -> Result<(), KeystoreError> {
        match self {
            Self::Aes256Gcm { tag_len, .. } => {
                if *tag_len != AES_GCM_TAG_LEN {
                    return Err(KeystoreError::corrupt_file("invalid AES-GCM tag length"));
                }
                Ok(())
            }
        }
    }

    pub(super) fn nonce(&self) -> Result<Nonce, KeystoreError> {
        match self {
            Self::Aes256Gcm { nonce, .. } => Ok(Nonce::assume_unique_for_key(*nonce)),
        }
    }

    pub(super) fn tag_len(&self) -> u8 {
        match self {
            Self::Aes256Gcm { tag_len, .. } => *tag_len,
        }
    }
}

pub(super) fn derive_key(password: &[u8], kdf: &KdfParams) -> Result<Zeroizing<[u8; 32]>, KeystoreError> {
    super::format::validate_v4_password(password)?;
    kdf.validate()?;
    match kdf {
        KdfParams::Argon2id {
            memory_kib,
            iterations,
            parallelism,
            salt,
            output_len,
        } => {
            let params = Params::new(
                *memory_kib,
                *iterations,
                *parallelism,
                Some(usize::try_from(*output_len).map_err(|_| KeystoreError::corrupt_file("invalid key length"))?),
            )
            .map_err(|error| KeystoreError::corrupt_file(error.to_string()))?;
            let argon2 = Argon2::new(Algorithm::Argon2id, Version::V0x13, params);
            let mut key = Zeroizing::new([0u8; 32]);
            argon2
                .hash_password_into(password, salt, key.as_mut())
                .map_err(|error| KeystoreError::corrupt_file(error.to_string()))?;
            Ok(key)
        }
    }
}
