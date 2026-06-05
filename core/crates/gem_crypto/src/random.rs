use ring::rand::{SecureRandom, SystemRandom};

use crate::CryptoError;

pub fn bytes<const N: usize>() -> Result<[u8; N], CryptoError> {
    let random = SystemRandom::new();
    let mut bytes = [0u8; N];
    random.fill(&mut bytes).map_err(|_| CryptoError::Random)?;
    Ok(bytes)
}
