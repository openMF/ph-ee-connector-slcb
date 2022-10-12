package org.mifos.connector.slcb.utils;

import org.apache.commons.codec.binary.Base64;
import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;

public class SecurityUtils {

    /**
     * Encrypts the [content] using the [privateKey]
     * @param content data to be encrypted
     * @param privateKey encryption key
     * @return encrypted data
     * @throws NoSuchPaddingException see @getSecretKey
     * @throws IllegalBlockSizeException see @encryptFromCipher
     * @throws NoSuchAlgorithmException see @getCipher
     * @throws BadPaddingException see @encryptFromCipher
     * @throws InvalidKeySpecException see @getSecretKey
     * @throws InvalidKeyException see @encrypt
     */
    public static String signContent(String content, String privateKey) throws
            NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException,
            BadPaddingException, InvalidKeySpecException, InvalidKeyException {

        return encrypt(content, privateKey);
    }

    /**
     * Generates [SecretKey] instance using custom password and salt
     *
     * @param key the base key used for generating secret
     * @return [SecretKey] An instance of the [SecretKey]
     */
    public static SecretKey getSecretKey(String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("RSA");
        KeySpec spec = new PBEKeySpec(key.toCharArray(), key.getBytes(), 65536, 2048);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "RSA");
    }

    /**
     * Generates [PublicKey] object from String public key
     *
     * @param key string value of public key
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static PublicKey getPublicKeyFromString(String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = Base64.decodeBase64(key);
        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(publicKeySpec);
    }

    /**
     * Applies given cipher on a plain text
     *
     * @param input  text to be encoded
     * @param cipher teh instance of the [Cipher]
     * @return [String] encrypted data as a Base64 encoded text
     */
    public static String encryptFromCipher(String input, Cipher cipher) throws IllegalBlockSizeException, BadPaddingException {
        byte[] cipherText = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeBase64String(cipherText);
    }

    /**
     * @return [Cipher] returns the default instance of [Cipher]
     */
    public static Cipher getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        return Cipher.getInstance("RSA");
    }

    /**
     * Encrypts the string data using [key] (SecretKey) and [iv] (IvParameterSpec)
     *
     * @param input  text to be encoded
     * @param encKey secret key to be used for encryption
     * @return [String] encoded data as plain text
     */
    public static String encrypt(String input, String encKey) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException {
        PublicKey publicKey = getPublicKeyFromString(encKey);
        Cipher cipher = getCipher();
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return encryptFromCipher(input, cipher);
    }

}
