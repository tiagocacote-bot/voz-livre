package br.pessoal.vozlivre;

import java.io.*;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * Password-based encryption for exported backups (AES-256-GCM, PBKDF2 key). Pure Java, so it is
 * unit-testable on the JVM and has no Android or network dependency.
 */
public final class Crypto {
  private static final byte[] MAGIC = {'V', 'L', 'E', '1'};
  private static final int SALT = 16, IV = 12, TAG = 128, ITER = 120000, KEY = 256;

  private Crypto() {}

  private static byte[] key(String password, byte[] salt) throws Exception {
    PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITER, KEY);
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    return factory.generateSecret(spec).getEncoded();
  }

  public static byte[] encrypt(byte[] plain, String password) throws Exception {
    if (password == null || password.isEmpty())
      throw new IllegalArgumentException(I18n.t("Informe uma senha."));
    byte[] salt = new byte[SALT], iv = new byte[IV];
    SecureRandom random = new SecureRandom();
    random.nextBytes(salt);
    random.nextBytes(iv);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(
        Cipher.ENCRYPT_MODE,
        new SecretKeySpec(key(password, salt), "AES"),
        new GCMParameterSpec(TAG, iv));
    byte[] data = cipher.doFinal(plain);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(MAGIC);
    out.write(salt);
    out.write(iv);
    out.write(data);
    return out.toByteArray();
  }

  public static boolean encrypted(byte[] data) {
    return data != null
        && data.length > MAGIC.length + SALT + IV
        && data[0] == MAGIC[0]
        && data[1] == MAGIC[1]
        && data[2] == MAGIC[2]
        && data[3] == MAGIC[3];
  }

  public static byte[] decrypt(byte[] data, String password) throws Exception {
    if (!encrypted(data))
      throw new IllegalArgumentException(I18n.t("Este backup não está protegido por senha."));
    int offset = MAGIC.length;
    byte[] salt = new byte[SALT], iv = new byte[IV];
    System.arraycopy(data, offset, salt, 0, SALT);
    offset += SALT;
    System.arraycopy(data, offset, iv, 0, IV);
    offset += IV;
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(
        Cipher.DECRYPT_MODE,
        new SecretKeySpec(key(password, salt), "AES"),
        new GCMParameterSpec(TAG, iv));
    try {
      return cipher.doFinal(data, offset, data.length - offset);
    } catch (AEADBadTagException e) {
      throw new IllegalArgumentException(I18n.t("Senha incorreta ou backup danificado."));
    }
  }
}
