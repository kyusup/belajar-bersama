package id.belajarbersama.infrastructure.auth;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

final class AppleClientSecret {
    private AppleClientSecret() {}

    static String generate(OidcSettings settings) throws Exception {
        String pem =
                settings.applePrivateKey()
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        ECPrivateKey privateKey =
                (ECPrivateKey)
                        KeyFactory.getInstance("EC")
                                .generatePrivate(new PKCS8EncodedKeySpec(decoded));
        Instant now = Instant.now();
        JWTClaimsSet claims =
                new JWTClaimsSet.Builder()
                        .issuer(settings.appleTeamId())
                        .subject(settings.appleClientId())
                        .audience("https://appleid.apple.com")
                        .issueTime(Date.from(now))
                        .expirationTime(Date.from(now.plusSeconds(3600)))
                        .build();
        SignedJWT jwt =
                new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.ES256)
                                .keyID(settings.appleKeyId())
                                .type(JOSEObjectType.JWT)
                                .build(),
                        claims);
        jwt.sign(new ECDSASigner(privateKey));
        return jwt.serialize();
    }
}
