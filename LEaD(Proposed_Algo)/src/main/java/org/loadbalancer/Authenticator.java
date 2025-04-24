package org.loadbalancer;

import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Authenticator implements Shamir Secret Sharing.
 */
public final class Authenticator {
    public final static int LIMIT = 1 << 16;
    /**
     * A cryptographically strong random number generator used to set up
     * parameters for the Authenticator.
     */
    private static final SecureRandom rng = new SecureRandom();
    /**
     * Secret is {@code S} in {@code f(x) = S + m*x}.
     */
    private static final int secret = rng.nextInt(LIMIT);
    /**
     * Coefficient is {@code m} in {@code f(x) = S + m*x}.
     */
    private static final int coefficient = rng.nextInt(LIMIT);
    private final static Logger logger = Logger.getLogger(Utils.LOGGER_NAME);
    /**
     * Counter makes sure successive requests to generate secret parts
     * yields different (and consecutive) values.
     */
    private static int counter = 0;

    /**
     * Prevent instantiation by defining a private constructor.
     */
    private Authenticator() {
        final String summary = "cannot be instantiated";
        final String detail = "Authenticator is a utility class";
        final String message = String.format("%s:%s.", summary, detail);
        throw new AssertionError(message);
    }

    /**
     * @return a new generated SecretPart.
     */
    public static SecretPart generate() {
        counter++;
        int x = counter;
        return new SecretPart(x, f(x));
    }

    /**
     * @param u   one verifiable part of the secret
     * @param v   another verifiable part of the secret
     * @param <U> secret part verifiable
     * @param <V> secret part verifiable
     * @return verify that the secret re-generated using the parts matches
     * the actual secret using Lagrange's interpolation.
     */
    public static <U extends Verifiable, V extends Verifiable> boolean verify(U u, V v) {
        SecretPart one = u.secretPart();
        SecretPart two = v.secretPart();
        if (one == two) {
            throw new IllegalStateException("identical secret parts");
        }
        int deltaY = two.y - one.y;
        int deltaX = two.x - one.x;
        int term = (deltaY * one.x) / deltaX;
        int S = (one.y - term);
        logger.log(Level.CONFIG, Utils.log("verify two secret parts", new Utils.LogPair("one", one), new Utils.LogPair("two", two), new Utils.LogPair("S", secret), new Utils.LogPair("m", coefficient), new Utils.LogPair("interpolated-secret", S)));
        return S == secret;
    }

    /**
     * @param x variable
     * @return {@code f(x) = S + m*x}.
     */
    private static int f(int x) {
        return secret + (coefficient * x);
    }

    /**
     * Verifiable is anything that presents its secret part for secret
     * {@link #verify(Verifiable, Verifiable) verification}.
     */
    public interface Verifiable {
        Authenticator.SecretPart secretPart();
    }

    /**
     * SecretPart is an {@code (x, f(x))} pair, which can be used to
     * reconstruct the secret when k = 2 parts come together.
     */
    public record SecretPart(int x, int y) {
        @Override
        public String toString() {
            return String.format("(%d, %d)", x, y);
        }
    }
}
