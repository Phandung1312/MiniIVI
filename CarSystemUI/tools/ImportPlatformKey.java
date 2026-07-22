import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;

public final class ImportPlatformKey {
    private ImportPlatformKey() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Usage: ImportPlatformKey <platform.pk8> <platform.x509.pem> <output.p12> <password>");
        }

        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(Files.readAllBytes(Path.of(args[0]))));
        Certificate certificate;
        try (InputStream input = Files.newInputStream(Path.of(args[1]))) {
            certificate = CertificateFactory.getInstance("X.509").generateCertificate(input);
        }

        char[] password = args[3].toCharArray();
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, password);
        keyStore.setKeyEntry("platform", privateKey, password, new Certificate[] {certificate});
        try (var output = Files.newOutputStream(Path.of(args[2]))) {
            keyStore.store(output, password);
        }
    }
}
