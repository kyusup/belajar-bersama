package id.belajarbersama.domain.identity;

/**
 * Abstraction over an external authentication provider.
 *
 * <p>Login is not implemented in Phase 2. This type exists so application code does not couple to a
 * single OAuth SDK.
 */
public interface IdentityProvider {
    IdentityProviderId id();
}
