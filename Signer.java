import com.android.apksig.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;

public final class Signer {
  public static void main(String[] args) throws Exception {
    if (args[0].equals("verify")) {
      ApkVerifier.Result r =
          new ApkVerifier.Builder(new File(args[1]))
              .setMinCheckedPlatformVersion(26)
              .build()
              .verify();
      System.out.println(
          "verified="
              + r.isVerified()
              + " v2="
              + r.isVerifiedUsingV2Scheme()
              + " v3="
              + r.isVerifiedUsingV3Scheme());
      for (Object error : r.getErrors()) System.out.println(error);
      if (!r.isVerified()) throw new Exception("Assinatura inválida");
      return;
    }
    char[] pass =
        new String(Files.readAllBytes(Paths.get(args[4])), java.nio.charset.StandardCharsets.UTF_8)
            .trim()
            .toCharArray();
    KeyStore store = KeyStore.getInstance("PKCS12");
    try (InputStream in = new FileInputStream(args[3])) {
      store.load(in, pass);
    }
    PrivateKey key = (PrivateKey) store.getKey("vozlivre", pass);
    X509Certificate cert = (X509Certificate) store.getCertificate("vozlivre");
    ApkSigner.SignerConfig cfg =
        new ApkSigner.SignerConfig.Builder("vozlivre", key, Collections.singletonList(cert))
            .build();
    new ApkSigner.Builder(Collections.singletonList(cfg))
        .setInputApk(new File(args[1]))
        .setOutputApk(new File(args[2]))
        .setMinSdkVersion(26)
        .setV1SigningEnabled(true)
        .setV2SigningEnabled(true)
        .setV3SigningEnabled(true)
        .setV4SigningEnabled(false)
        .setCreatedBy("Voz Livre personal build")
        .build()
        .sign();
  }
}
