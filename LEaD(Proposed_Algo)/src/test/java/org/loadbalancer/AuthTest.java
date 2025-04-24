package org.loadbalancer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AuthTest {
    @Test
    void testAuth() {
        Authenticator.SecretPart one = Authenticator.generate();
        Authenticator.SecretPart two = Authenticator.generate();
        Authenticator.SecretPart three = Authenticator.generate();
        VerifiableSecretPart a = new VerifiableSecretPart(one);
        VerifiableSecretPart b = new VerifiableSecretPart(two);
        VerifiableSecretPart c = new VerifiableSecretPart(three);
        Assertions.assertTrue(Authenticator.verify(a, b));
        Assertions.assertTrue(Authenticator.verify(b, c));
        Assertions.assertTrue(Authenticator.verify(c, a));
    }

    /**
     * VerifiableSecretPart is a wrapper around {@link Authenticator.SecretPart
     * SecretPart} that implements {@link Authenticator.Verifiable Verifiable}.
     */
    private static class VerifiableSecretPart implements Authenticator.Verifiable {
        Authenticator.SecretPart part;

        public VerifiableSecretPart(Authenticator.SecretPart part) {
            this.part = part;
        }

        @Override
        public Authenticator.SecretPart secretPart() {
            return this.part;
        }
    }
}
