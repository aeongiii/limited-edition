package com.sparta.limited_edition.security;

import io.github.cdimascio.dotenv.Dotenv;
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

    // Base64 인코딩된 비밀키를 .env에서 읽어온다.
    private static final Dotenv dotenv = Dotenv.configure().load(); // .env 파일 가져오기
    private static final String base64EncodedSecretKey = dotenv.get("AES_SECRET_KEY"); // 키 가져오기

    // 필요한 시점에 비밀키 생성
    private static SecretKeySpec getSecretKey() {
        if (base64EncodedSecretKey == null || base64EncodedSecretKey.isEmpty()) {
            throw new IllegalArgumentException(".env 파일에 키가 없습니다.");
        }

        byte[] decodedKey = Base64.getDecoder().decode(base64EncodedSecretKey); // Base64 디코딩
        System.out.println(dotenv.get("AES_SECRET_KEY")); // 키 값이 제대로 출력되는지 확인
        return new SecretKeySpec(decodedKey, ALGORITHM);
    }

    // 암호화
    public static String encrypt(String data) throws Exception {
        SecretKey key = getSecretKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM); // AES 알고리즘에 대한 Cipher 객체 생성
        cipher.init(Cipher.ENCRYPT_MODE, key); // 암호화 모드로 초기화
        byte[] encryptedData = cipher.doFinal(data.getBytes()); // 데이터를 암호화하여 바이트 배열로 만들기
        return Base64.getEncoder().encodeToString(encryptedData); // 암호화한 데이터를 Base64로 인코딩 후 반환
    }

    // 복호화
    public static String decrypt(String encryptedData) throws Exception {
        SecretKey key = getSecretKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key); // 복호화 모드로 초기화
        byte[] decodeBytes = Base64.getDecoder().decode(encryptedData); // Base64 디코딩 -> 바이트 배열로 변환
        byte[] decryptedData = cipher.doFinal(decodeBytes); // 데이터 복호화해서 바이트 배열로 만들기
        return new String(decryptedData); // 복호화된 데이터를 문자열로 반환
    }
}
