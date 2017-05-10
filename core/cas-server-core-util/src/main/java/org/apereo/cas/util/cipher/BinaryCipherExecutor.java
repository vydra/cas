package org.apereo.cas.util.cipher;

import com.google.common.base.Throwables;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.crypto.AesCipherService;
import org.apache.shiro.crypto.CipherService;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.OctJwkGenerator;
import org.jose4j.jwk.OctetSequenceJsonWebKey;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Map;

/**
 * A implementation that is based on algorithms
 * provided by the default platform's JCE. By default AES encryption is
 * used.
 * @author Misagh Moayyed
 * @since 4.2
 */
public class BinaryCipherExecutor extends AbstractCipherExecutor<byte[], byte[]> {

    /** Secret key IV algorithm. Default is {@code AES}. */
    private String secretKeyAlgorithm = "AES";

    private String encryptionSecretKey;


    /**
     * Instantiates a new cryptic ticket cipher executor.
     *
     * @param encryptionSecretKey the encryption secret key
     * @param signingSecretKey    the signing key
     * @param signingKeySize      the signing key size
     * @param encryptionKeySize   the encryption key size
     */
    public BinaryCipherExecutor(final String encryptionSecretKey,
                                final String signingSecretKey,
                                final int signingKeySize,
                                final int encryptionKeySize) {

        String signingKeyToUse = signingSecretKey;
        if (StringUtils.isBlank(signingKeyToUse)) {
            logger.warn("Secret key for signing is not defined. CAS will attempt to auto-generate the signing key");
            signingKeyToUse = generateOctetJsonWebKeyOfSize(signingKeySize);
            logger.warn("Generated signing key {} of size {}. The generated key MUST be added to CAS settings.",
                    signingKeyToUse, signingKeySize);
        }
        setSigningKey(signingKeyToUse);

        if (StringUtils.isBlank(encryptionSecretKey)) {
            logger.warn("No encryption key is defined. CAS will attempt to auto-generate keys");
            this.encryptionSecretKey = RandomStringUtils.randomAlphabetic(encryptionKeySize);
            logger.warn("Generated encryption key {} of size {}. The generated key MUST be added to CAS settings.",
                    this.encryptionSecretKey, encryptionKeySize);
        } else {
            this.encryptionSecretKey = encryptionSecretKey;
        }
    }


    public void setSecretKeyAlgorithm(final String secretKeyAlgorithm) {
        this.secretKeyAlgorithm = secretKeyAlgorithm;
    }

    @Override
    public byte[] encode(final byte[] value) {
        try {
            final Key key = new SecretKeySpec(this.encryptionSecretKey.getBytes(StandardCharsets.UTF_8),
                    this.secretKeyAlgorithm);
            final CipherService cipher = new AesCipherService();
            final byte[] result = cipher.encrypt(value, key.getEncoded()).getBytes();
            return sign(result);
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            throw Throwables.propagate(e);
        }
    }

    @Override
    public byte[] decode(final byte[] value) {
        try {
            final byte[] verifiedValue = verifySignature(value);
            final Key key = new SecretKeySpec(this.encryptionSecretKey.getBytes(StandardCharsets.UTF_8),
                    this.secretKeyAlgorithm);
            final CipherService cipher = new AesCipherService();
            final byte[] result = cipher.decrypt(verifiedValue, key.getEncoded()).getBytes();
            return result;
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            throw Throwables.propagate(e);
        }
    }

    private String generateOctetJsonWebKeyOfSize(final int size) {
        try {
            final OctetSequenceJsonWebKey octetKey = OctJwkGenerator.generateJwk(size);
            final Map<String, Object> params = octetKey.toParams(JsonWebKey.OutputControlLevel.INCLUDE_SYMMETRIC);
            return params.get("k").toString();
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            throw Throwables.propagate(e);
        }
    }
}
