package org.apereo.cas.authentication;

import org.apereo.cas.authentication.principal.Service;

/**
 * This is {@link DefaultAuthenticationSystemSupport}.
 *
 * @author Misagh Moayyed
 * @author Dmitriy Kopylenko
 * @since 4.2.0
 */
public class DefaultAuthenticationSystemSupport implements AuthenticationSystemSupport {

    private AuthenticationTransactionManager authenticationTransactionManager;

    private PrincipalElectionStrategy principalElectionStrategy;

    public DefaultAuthenticationSystemSupport() {
    }

    public DefaultAuthenticationSystemSupport(final AuthenticationTransactionManager authenticationTransactionManager,
                                              final PrincipalElectionStrategy principalElectionStrategy) {
        this.authenticationTransactionManager = authenticationTransactionManager;
        this.principalElectionStrategy = principalElectionStrategy;
    }

    @Override
    public AuthenticationTransactionManager getAuthenticationTransactionManager() {
        return this.authenticationTransactionManager;
    }

    @Override
    public PrincipalElectionStrategy getPrincipalElectionStrategy() {
        return this.principalElectionStrategy;
    }

    @Override
    public AuthenticationResultBuilder handleInitialAuthenticationTransaction(final Credential... credential) throws
            AuthenticationException {

        final DefaultAuthenticationResultBuilder builder =
                new DefaultAuthenticationResultBuilder(this.principalElectionStrategy);
        if (credential != null && credential.length > 0) {
            builder.collect(credential[0]);
        }

        return this.handleAuthenticationTransaction(builder, credential);
    }

    @Override
    public AuthenticationResultBuilder establishAuthenticationContextFromInitial(final Authentication authentication,
                                                                                 final Credential credentials) {
        return new DefaultAuthenticationResultBuilder(this.principalElectionStrategy)
                .collect(authentication).collect(credentials);
    }

    @Override
    public AuthenticationResultBuilder handleAuthenticationTransaction(final AuthenticationResultBuilder authenticationResultBuilder,
                                                                       final Credential... credential) throws AuthenticationException {

        this.authenticationTransactionManager.handle(AuthenticationTransaction.wrap(credential), authenticationResultBuilder);
        return authenticationResultBuilder;
    }

    @Override
    public AuthenticationResult finalizeAllAuthenticationTransactions(final AuthenticationResultBuilder authenticationResultBuilder,
                                                                      final Service service) {
        return authenticationResultBuilder.build(service);
    }

    @Override
    public AuthenticationResult handleAndFinalizeSingleAuthenticationTransaction(final Service service, final Credential... credential)
            throws AuthenticationException {

        return this.finalizeAllAuthenticationTransactions(this.handleInitialAuthenticationTransaction(credential), service);
    }

    public void setAuthenticationTransactionManager(final AuthenticationTransactionManager authenticationTransactionManager) {
        this.authenticationTransactionManager = authenticationTransactionManager;
    }

    public void setPrincipalElectionStrategy(final PrincipalElectionStrategy principalElectionStrategy) {
        this.principalElectionStrategy = principalElectionStrategy;
    }
}
