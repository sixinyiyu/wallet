use gem_hash::blake2::blake2b_256;
use gem_hash::keccak::keccak256;
use gem_hash::sha2::hash160;
use gem_hash::sha3::sha3_256;
use primitives::chain_cosmos::CosmosChain;
use primitives::{Address, BitcoinChain, Chain, ChainType, hex};
use signer::{Ed25519KeyPair, secp256k1_public_key, secp256k1_uncompressed_public_key};

use crate::AccountDerivationError;

const SECP256K1_UNCOMPRESSED_PUBLIC_KEY_PREFIX: u8 = 0x04;
const SECP256K1_COMPRESSED_PUBLIC_KEY_LEN: usize = 33;
const SECP256K1_UNCOMPRESSED_PUBLIC_KEY_LEN: usize = 65;
const EVM_ADDRESS_LEN: usize = 20;

pub(super) fn address_from_private_key(private_key: &[u8], chain: Chain) -> Result<String, AccountDerivationError> {
    match chain.chain_type() {
        ChainType::Ethereum => ethereum_address_from_private_key(private_key),
        ChainType::HyperCore => ethereum_address_from_private_key(private_key),
        ChainType::Solana => solana_address_from_private_key(private_key),
        ChainType::Tron => tron_address_from_private_key(private_key),
        ChainType::Aptos => aptos_address_from_private_key(private_key),
        ChainType::Sui => sui_address_from_private_key(private_key),
        ChainType::Near => near_address_from_private_key(private_key),
        ChainType::Stellar => stellar_address_from_private_key(private_key),
        ChainType::Algorand => algorand_address_from_private_key(private_key),
        ChainType::Cosmos => cosmos_address_from_private_key(private_key, chain),
        ChainType::Ton => ton_address_from_private_key(private_key),
        ChainType::Xrp => xrp_address_from_private_key(private_key),
        ChainType::Polkadot => polkadot_address_from_private_key(private_key),
        ChainType::Cardano => cardano_address_from_private_key(private_key),
        ChainType::Bitcoin => bitcoin_address_from_private_key(private_key, chain),
    }
}

// Canonical public key stored on the account, in the same form used to derive the address:
// uncompressed secp256k1 for EVM/Tron/Injective, compressed for other Cosmos and XRP, raw 32-byte
// ed25519 elsewhere. Bitcoin family uses an account extended key (set separately) and Cardano has no
// single-key form, so both return None.
pub(super) fn public_key_from_private_key(private_key: &[u8], chain: Chain) -> Result<Option<Vec<u8>>, AccountDerivationError> {
    let public_key = match chain.chain_type() {
        ChainType::Ethereum | ChainType::HyperCore | ChainType::Tron => checked_uncompressed_secp256k1_public_key(private_key)?,
        ChainType::Xrp => checked_compressed_secp256k1_public_key(private_key)?,
        ChainType::Cosmos => match CosmosChain::from_chain(chain).ok_or_else(|| AccountDerivationError::unsupported_chain(chain))? {
            CosmosChain::Injective => checked_uncompressed_secp256k1_public_key(private_key)?,
            CosmosChain::Cosmos | CosmosChain::Osmosis | CosmosChain::Celestia | CosmosChain::Thorchain | CosmosChain::Mayachain | CosmosChain::Sei | CosmosChain::Noble => {
                checked_compressed_secp256k1_public_key(private_key)?
            }
        },
        ChainType::Solana | ChainType::Aptos | ChainType::Sui | ChainType::Near | ChainType::Stellar | ChainType::Algorand | ChainType::Ton | ChainType::Polkadot => {
            ed25519_key_pair(private_key)?.public_key_bytes.to_vec()
        }
        ChainType::Bitcoin | ChainType::Cardano => return Ok(None),
    };
    Ok(Some(public_key))
}

fn ethereum_address_from_private_key(private_key: &[u8]) -> Result<String, AccountDerivationError> {
    let public_key = checked_uncompressed_secp256k1_public_key(private_key)?;
    let address = last_20_bytes(&keccak256(&public_key[1..]), "invalid Ethereum address length")?;
    Ok(gem_evm::EthereumAddress::from_bytes(address).encode())
}

fn tron_address_from_private_key(private_key: &[u8]) -> Result<String, AccountDerivationError> {
    let public_key = checked_uncompressed_secp256k1_public_key(private_key)?;
    let public_key_hash = last_20_bytes(&keccak256(&public_key[1..]), "invalid Tron public key hash length")?;
    Ok(gem_tron::address::TronAddress::from(public_key_hash).encode())
}

fn checked_uncompressed_secp256k1_public_key(private_key: &[u8]) -> Result<Vec<u8>, AccountDerivationError> {
    let public_key = secp256k1_uncompressed_public_key(private_key).map_err(|_| AccountDerivationError::InvalidPrivateKey)?;
    if public_key.len() != SECP256K1_UNCOMPRESSED_PUBLIC_KEY_LEN || public_key.first() != Some(&SECP256K1_UNCOMPRESSED_PUBLIC_KEY_PREFIX) {
        return Err(AccountDerivationError::InvalidPrivateKey);
    }
    Ok(public_key)
}

fn checked_compressed_secp256k1_public_key(private_key: &[u8]) -> Result<Vec<u8>, AccountDerivationError> {
    let public_key = secp256k1_public_key(private_key).map_err(|_| AccountDerivationError::InvalidPrivateKey)?;
    if public_key.len() != SECP256K1_COMPRESSED_PUBLIC_KEY_LEN {
        return Err(AccountDerivationError::InvalidPrivateKey);
    }
    Ok(public_key)
}

fn solana_address_from_private_key(private_key: &[u8]) -> Result<String, AccountDerivationError> {
    let key_pair = ed25519_key_pair(private_key)?;
    Ok(gem_solana::address::SolanaAddress::from_bytes(key_pair.public_key_bytes).encode())
}

fn stellar_address_from_private_key(private_key: &[u8]) -> Result<String, AccountDerivationError> {
    let key_pair = ed25519_key_pair(private_key)?;
    Ok(gem_stellar::StellarAddress::from_public_key(&key_pair.public_key_bytes)?.encode())
}

fn algorand_address_from_private_key(private_key: &[u8]) -> Result<String, AccountDerivationError> {
    let key_pair = ed25519_key_pair(private_key)?;
    Ok(gem_algorand::AlgorandAddress::from_public_key(&key_pair.public_key_bytes)?.encode())
}

fn cosmos_address_from_private_key(private_key: &[u8], chain: Chain) -> Result<String, AccountDerivationError> {
    let cosmos_chain = CosmosChain::from_chain(chain).ok_or_else(|| AccountDerivationError::unsupported_chain(chain))?;
    let public_key_hash = match cosmos_chain {
        CosmosChain::Injective => {
            let public_key = checked_uncompressed_secp256k1_public_key(private_key)?;
            last_20_bytes(&keccak256(&public_key[1..]), "invalid Injective public key hash length")?
        }
        CosmosChain::Cosmos | CosmosChain::Osmosis | CosmosChain::Celestia | CosmosChain::Thorchain | CosmosChain::Mayachain | CosmosChain::Sei | CosmosChain::Noble => {
            let public_key = checked_compressed_secp256k1_public_key(private_key)?;
            hash160(&public_key)
        }
    };
    Ok(gem_cosmos::address::CosmosAddress::from_public_key_hash(chain, public_key_hash)
        .ok_or_else(|| AccountDerivationError::unsupported_chain(chain))?
        .encode())
}

fn ton_address_from_private_key(private_key: &[u8]) -> Result<String, AccountDerivationError> {
    let key_pair = ed25519_key_pair(private_key)?;
    Ok(gem_ton::signer::WalletV4R2::new(key_pair.public_key_bytes)?.address.encode_non_bounceable())
}

fn xrp_address_from_private_key(private_key: &[u8]) -> Result<String, AccountDerivationError> {
    let public_key = checked_compressed_secp256k1_public_key(private_key)?;
    Ok(gem_xrp::XrpAddress::from_public_key_hash(hash160(&public_key)).encode())
}

fn polkadot_address_from_private_key(private_key: &[u8]) -> Result<String, AccountDerivationError> {
    let key_pair = ed25519_key_pair(private_key)?;
    Ok(gem_polkadot::PolkadotAddress::from_public_key(key_pair.public_key_bytes).encode())
}

fn cardano_address_from_private_key(private_key: &[u8]) -> Result<String, AccountDerivationError> {
    crate::cardano::address_from_extended_private_key(private_key)
}

fn bitcoin_address_from_private_key(private_key: &[u8], chain: Chain) -> Result<String, AccountDerivationError> {
    let bitcoin_chain = BitcoinChain::from_chain(chain).ok_or_else(|| AccountDerivationError::unsupported_chain(chain))?;
    let public_key = checked_compressed_secp256k1_public_key(private_key)?;
    Ok(gem_bitcoin::BitcoinAddress::from_public_key(bitcoin_chain, &public_key)?.encode())
}

fn near_address_from_private_key(private_key: &[u8]) -> Result<String, AccountDerivationError> {
    let key_pair = ed25519_key_pair(private_key)?;
    Ok(hex::encode(key_pair.public_key_bytes))
}

fn aptos_address_from_private_key(private_key: &[u8]) -> Result<String, AccountDerivationError> {
    let key_pair = ed25519_key_pair(private_key)?;
    let mut input = Vec::with_capacity(33);
    input.extend_from_slice(&key_pair.public_key_bytes);
    input.push(0x00);
    Ok(gem_aptos::AccountAddress::from_bytes(&sha3_256(&input))?.encode())
}

fn sui_address_from_private_key(private_key: &[u8]) -> Result<String, AccountDerivationError> {
    let key_pair = ed25519_key_pair(private_key)?;
    let mut input = Vec::with_capacity(33);
    input.push(0x00);
    input.extend_from_slice(&key_pair.public_key_bytes);
    Ok(gem_sui::address::SuiAddress::from_bytes(&blake2b_256(&input))
        .map_err(|error| AccountDerivationError::invalid_input(error.to_string()))?
        .encode())
}

fn ed25519_key_pair(private_key: &[u8]) -> Result<Ed25519KeyPair, AccountDerivationError> {
    Ed25519KeyPair::from_private_key(private_key).map_err(|_| AccountDerivationError::InvalidPrivateKey)
}

fn last_20_bytes(bytes: &[u8], error: &'static str) -> Result<[u8; EVM_ADDRESS_LEN], AccountDerivationError> {
    if bytes.len() < EVM_ADDRESS_LEN {
        return Err(AccountDerivationError::invalid_input(error));
    }
    bytes[bytes.len() - EVM_ADDRESS_LEN..].try_into().map_err(|_| AccountDerivationError::invalid_input(error))
}
