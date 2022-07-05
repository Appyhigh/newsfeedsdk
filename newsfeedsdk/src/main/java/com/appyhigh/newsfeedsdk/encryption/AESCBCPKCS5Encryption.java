package com.appyhigh.newsfeedsdk.encryption;


//import LogDetail;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AESCBCPKCS5Encryption {
    private final static String TAG = AESCBCPKCS5Encryption.class.getSimpleName();
    private AESCBCPKCS5Encryption actionsInstance;

    private final String ALGORITHM = "AES/CBC/PKCS5Padding";

    private static final int keySize = 256 / 8; // 32 for Key
    private static final int blockSize = 128 / 8; // 16 for IV

    private final static byte[] saltBytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
    static final int iterationCount = 1000;
    static final int keyStrength = 384;
    // keyStrength may be take alternate value 256 but gives shorter length key, 384 gives exact 48 length

    static byte[] key = new byte[keySize];
    static byte[] iv = new byte[blockSize];

    public AESCBCPKCS5Encryption() {
    }

    public AESCBCPKCS5Encryption getInstance(final String PASSWORD_STRING) {
//        if (actionsInstance == null) {
        LogDetail.LogDE("Private Key", PASSWORD_STRING);
            actionsInstance = new AESCBCPKCS5Encryption();

            actionsInstance.updateKEY_IV(PASSWORD_STRING);

//        }

        return actionsInstance;
    }


    public void updateKEY_IV(String password_string) {
        try {
            System.out.println("updateKEY_IV: " + password_string);
            byte[] initialBytes = password_string.getBytes(StandardCharsets.UTF_8);

            MessageDigest msgDigest = MessageDigest.getInstance("SHA-256");
            msgDigest.update(initialBytes);
            byte[] passwordDigestBytes = msgDigest.digest();

            String sb = byteTohex(passwordDigestBytes);
            char[] passwordChars = sb.toCharArray();

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            PBEKeySpec pbeKeySpec = new PBEKeySpec(passwordChars, saltBytes, iterationCount, keyStrength);

            SecretKey tmp = factory.generateSecret(pbeKeySpec);
            SecretKeySpec keySecretKeySpec = new SecretKeySpec(tmp.getEncoded(), "AES");

            key = new byte[keySize];
            iv = new byte[blockSize];

            //Copy keySecretKeySpec from 0 to keySize == 32 into key byte array
            System.arraycopy(keySecretKeySpec.getEncoded(), 0, key, 0, keySize);
            //Copy keySecretKeySpec from 32 to blockSize == 16 into key byte array
            System.arraycopy(keySecretKeySpec.getEncoded(), 32, iv, 0, blockSize);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String byteTohex(byte[] passwordDigestBytes) {
        //convert the byte to hex format method
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < passwordDigestBytes.length; i++) {
            stringBuilder.append(Integer.toString((passwordDigestBytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return stringBuilder.toString();
    }

    public String encrypt(byte[] plaintext) {
        return encrypt(plaintext, false);
    }

    public String encrypt(byte[] plaintext, boolean attachPublicKey) {
        try {
            if (plaintext == null) {
                throw new NullPointerException("text to be encrypted should not be null");
            }

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] cipherText = cipher.doFinal(plaintext);
            byte[] encode = Base64.getEncoder().encode(cipherText);

            if (attachPublicKey) {
                String stt = new String(encode) + "." + SessionUser.Instance().getPublicKey();
                return stt;
            } else {
                return new String(encode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new String(plaintext);

    }

    public String decrypt(byte[] encryptedText) {
        try {

            if (encryptedText == null) {
                throw new NullPointerException("text to be decrypted should not be null");
            }

            byte[] decodeText = Base64.getDecoder().decode(encryptedText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decryptedText = cipher.doFinal(decodeText);
            return new String(decryptedText);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new String(encryptedText);
    }

}
