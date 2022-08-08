package com.appyhigh.newsfeedsdk.utils;

import com.appyhigh.newsfeedsdk.encryption.LogDetail;

public class CustomEncryption {

    public static void main(String[] args) {

        String value = "Gain Java Knowledge";
        int secret_key = 8;

        LogDetail.LogDE("original_value : ", value);
        String encrypt = getEncryptedValue(value, secret_key);
        LogDetail.LogDE("encrypted value: ", encrypt);

        String decrypt = getDecryptedValue(encrypt, secret_key);
        LogDetail.LogDE("decrypted value: ", decrypt);
    }

     public static String getDecryptedValue(String encrypt, int secret_key) {
        String decrypted = "";
        for(int i =0; i < encrypt.length();i++) {
            char ch = encrypt.charAt(i);
            ch -= secret_key;
            decrypted = decrypted + ch;
        }
        return decrypted;
    }

    public static String getEncryptedValue(String value, int secret_key) {
        String encrypt = "";
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            ch += secret_key;
            encrypt = encrypt + ch;
        }
        return encrypt;
    }
}