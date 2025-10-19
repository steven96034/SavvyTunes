package com.example.geminispotifyapp.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

class EncryptedPreferenceManager @Inject constructor() {

    private companion object {
        /**
         * The name of the Android KeyStore provider.
         * This constant is used to specify the provider when working with the Android KeyStore system,
         * which is a secure storage facility for cryptographic keys.
         */
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        /**
         * Alias for the key in the Android Keystore.
         *
         * This alias is used to identify and retrieve the cryptographic key
         * stored securely within the Android Keystore system. It's crucial
         * that this alias remains consistent for accessing the correct key.
         */
        private const val KEY_ALIAS = "my_keystore_alias"
        /**
         * Specifies the cryptographic transformation to be used for encryption and decryption.
         *
         * This constant defines the algorithm, mode, and padding scheme.
         * - **AES**: Advanced Encryption Standard, a symmetric block cipher.
         * - **GCM**: Galois/Counter Mode, an authenticated encryption mode that provides both confidentiality and data integrity.
         * - **NoPadding**: Indicates that no padding will be applied to the plaintext before encryption. GCM mode
         *   inherently handles data of varying lengths without requiring traditional padding schemes.
         *
         * This specific transformation (AES/GCM/NoPadding) is a common and secure choice for modern encryption needs.
         */
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        /**
         * Separator used to distinguish between the Initialization Vector (IV) and the ciphertext in the encrypted string.
         * This specific separator "]" is chosen because it's unlikely to appear naturally in Base64 encoded data,
         * reducing the chances of collision or incorrect parsing.
         */
        private const val IV_SEPARATOR = "]"
    }

    /**
     * Retrieves or generates a secret key for encryption and decryption.
     *
     * This function first attempts to retrieve an existing key with the alias [KEY_ALIAS]
     * from the Android Keystore. If the key exists, it is returned.
     *
     * If the key does not exist, a new AES key is generated with the following properties:
     * - Alias: [KEY_ALIAS]
     * - Purpose: Encryption and Decryption
     * - Block Mode: GCM (Galois/Counter Mode)
     * - Padding: None (GCM handles padding internally)
     * - Key Size: 256 bits
     *
     * The newly generated key is then stored in the Android Keystore under the specified alias
     * and returned.
     *
     * @return The [javax.crypto.SecretKey] used for encryption and decryption.
     * @throws KeyStoreException If there is an issue accessing or loading the KeyStore.
     * @throws NoSuchAlgorithmException If the specified cryptographic algorithm is not available.
     * @throws UnrecoverableKeyException If the key cannot be recovered from the keystore.
     * @throws InvalidAlgorithmParameterException If the provided key generation parameters are invalid.
     * @throws CertificateException If there is an issue with certificates during KeyStore loading.
     * @throws IOException If an I/O error occurs during KeyStore loading.
     */
    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts the given data using AES encryption.
     *
     * @param data The string to be encrypted.
     * @return A string containing the Base64 encoded IV and the Base64 encoded encrypted data, separated by [IV_SEPARATOR].
     * @throws NoSuchAlgorithmException If the specified algorithm (AES) is not available.
     * @throws NoSuchPaddingException If the specified padding scheme is not available.
     * @throws InvalidKeyException If the provided secret key is invalid.
     * @throws IllegalBlockSizeException If the input data length is not a multiple of the block size.
     * @throws BadPaddingException If the input data has been padded incorrectly.
     */
    fun encrypt(data: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv, Base64.DEFAULT) + IV_SEPARATOR + Base64.encodeToString(encryptedData, Base64.DEFAULT)
    }

    /**
     * Decrypts an encrypted string.
     *
     * The encrypted string is expected to be in the format "IV_BASE64${IV_SEPARATOR}ENCRYPTED_DATA_BASE64".
     * IV (Initialization Vector) is a random value used in encryption to ensure that the same plaintext
     * encrypts to different ciphertexts each time.
     *
     * @param encryptedDataString The string to decrypt, formatted as described above.
     * @return The original, decrypted string.
     * @throws IllegalArgumentException if the `encryptedDataString` is not in the expected format.
     * @throws GeneralSecurityException if any cryptographic error occurs during decryption (e.g., incorrect key, invalid IV).
     */
    fun decrypt(encryptedDataString: String): String {
        val parts = encryptedDataString.split(IV_SEPARATOR)
        if (parts.size != 2) throw IllegalArgumentException("Invalid encrypted data format")

        val iv = Base64.decode(parts[0], Base64.DEFAULT)
        val encryptedData = Base64.decode(parts[1], Base64.DEFAULT)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        return String(cipher.doFinal(encryptedData), Charsets.UTF_8)
    }
}