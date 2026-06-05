use crate::AccountDerivationError;

pub(super) const HARDENED_OFFSET: u32 = 1 << 31;

#[derive(Clone, Copy)]
pub(super) struct DerivationPathComponent {
    pub(super) index: u32,
    pub(super) hardened: bool,
}

pub(super) fn parse_derivation_path(path: &str) -> Result<Vec<DerivationPathComponent>, AccountDerivationError> {
    let mut parts = path.split('/');
    if parts.next() != Some("m") {
        return Err(AccountDerivationError::invalid_input(format!("invalid derivation path: {path}")));
    }

    let mut components = Vec::new();
    for part in parts {
        if part.is_empty() {
            return Err(AccountDerivationError::invalid_input(format!("invalid derivation path: {path}")));
        }
        let (raw_index, hardened) = if let Some(raw_index) = part.strip_suffix('\'') {
            (raw_index, true)
        } else if let Some(raw_index) = part.strip_suffix('h') {
            (raw_index, true)
        } else {
            (part, false)
        };
        let index = raw_index
            .parse::<u32>()
            .map_err(|_| AccountDerivationError::invalid_input(format!("invalid derivation path: {path}")))?;
        if index >= HARDENED_OFFSET {
            return Err(AccountDerivationError::invalid_input(format!("invalid derivation path: {path}")));
        }
        components.push(DerivationPathComponent { index, hardened });
    }
    Ok(components)
}

pub(super) fn component_number(component: DerivationPathComponent) -> u32 {
    if component.hardened { component.index | HARDENED_OFFSET } else { component.index }
}
