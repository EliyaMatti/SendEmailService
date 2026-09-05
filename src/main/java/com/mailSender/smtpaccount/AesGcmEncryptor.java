package com.mailSender.smtpaccount;

import com.mailSender.common.exception.ApiException;
import com.mailSender.config.ApplicationProfiles;
import com.mailSender.config.ExcelmailProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile(ApplicationProfiles.API)
public class AesGcmEncryptor {

  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int GCM_TAG_BITS = 128;
  private static final int IV_BYTES = 12;

  private final ExcelmailProperties properties;
  private final SecureRandom random = new SecureRandom();

  public AesGcmEncryptor(ExcelmailProperties properties) {
    this.properties = properties;
  }

  public String encrypt(String plaintext) {
    try {
      byte[] key = decodeKey();
      byte[] iv = new byte[IV_BYTES];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
      buffer.put(iv);
      buffer.put(ciphertext);
      return Base64.getEncoder().encodeToString(buffer.array());
    } catch (GeneralSecurityException e) {
      throw new ApiException("ENCRYPTION_ERROR", "Unable to protect SMTP credentials.", 500);
    }
  }

  public String decrypt(String payload, int keyVersion) {
    if (keyVersion != properties.getCrypto().getKeyVersion()) {
      throw new ApiException("ENCRYPTION_ERROR", "SMTP credential key version is not supported.", 500);
    }
    try {
      byte[] raw = Base64.getDecoder().decode(payload);
      ByteBuffer buffer = ByteBuffer.wrap(raw);
      byte[] iv = new byte[IV_BYTES];
      buffer.get(iv);
      byte[] ciphertext = new byte[buffer.remaining()];
      buffer.get(ciphertext);
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(
          Cipher.DECRYPT_MODE, new SecretKeySpec(decodeKey(), "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      throw new ApiException("ENCRYPTION_ERROR", "Unable to read stored SMTP credentials.", 500);
    }
  }

  public int currentKeyVersion() {
    return properties.getCrypto().getKeyVersion();
  }

  private byte[] decodeKey() {
    String key = properties.getCrypto().getKey();
    if (key == null || key.isBlank()) {
      throw new ApiException("ENCRYPTION_ERROR", "APP_ENCRYPTION_KEY is not configured.", 500);
    }
    byte[] decoded = Base64.getDecoder().decode(key);
    if (decoded.length != 16 && decoded.length != 32) {
      throw new ApiException("ENCRYPTION_ERROR", "APP_ENCRYPTION_KEY must be a 128- or 256-bit key.", 500);
    }
    return decoded;
  }
}
