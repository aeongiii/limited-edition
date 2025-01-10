package com.sparta.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

// 복호화 가능한 AES 알고리즘으로 이메일, 이름, 주소 암호화
@Component
public class EncryptionUtil {

    // 사용할 암호화 알고리즘
    private static final String ALGORITHM = "AES";

    @Value("${AES_SECRET_KEY}")
    private String base64EncodedSecretKey;

    // 비밀키 생성
    private SecretKeySpec getSecretKey() {
        if (base64EncodedSecretKey == null || base64EncodedSecretKey.isEmpty()) {
            throw new IllegalArgumentException("AES_SECRET_KEY 값이 비어 있습니다.");
        }

        byte[] decodedKey = Base64.getDecoder().decode(base64EncodedSecretKey);
        return new SecretKeySpec(decodedKey, ALGORITHM);
    }

    // 암호화
    public String encrypt(String data) throws Exception {
        SecretKey key = getSecretKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedData = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedData);
    }
    // 복호화
    public String decrypt(String encryptedData) throws Exception {
        SecretKey key = getSecretKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decodeBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedData = cipher.doFinal(decodeBytes);
        return new String(decryptedData);
    }
}
