package com.example.geminispotifyapp

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import javax.inject.Inject
import kotlin.test.assertFailsWith
import android.util.Base64
import com.example.geminispotifyapp.data.local.EncryptedPreferenceManager

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class EncryptedPreferenceManagerTest {

    // Hilt's rule, must be added to the test class
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    // Inject the object we want to test from Hilt
    @Inject
    lateinit var encryptedPreferenceManager: EncryptedPreferenceManager

    // Before each test method runs, inject dependencies
    @Before
    fun setUp() {
        hiltRule.inject()
        // Clean up keys that may have been left from the previous test to ensure independence between tests
        cleanupKeyStore()
    }

    // After each test method runs, clean up the keys again
    @After
    fun tearDown() {
        cleanupKeyStore()
    }

    @Test
    fun encrypt_and_decrypt_successfully() {
        // Arrange
        val originalText = "This is a secret message for testing!"

        // Act
        val encryptedText = encryptedPreferenceManager.encrypt(originalText)
        val decryptedText = encryptedPreferenceManager.decrypt(encryptedText)

        // Assert
        // 1. Confirm that the encrypted text is not empty
        assertThat(encryptedText).isNotEmpty()
        // 2. Confirm that the encrypted text is different from the original text
        assertThat(encryptedText).isNotEqualTo(originalText)
        // 3. Confirm that the decrypted text is the same as the original text, verifying the entire encryption/decryption process
        assertThat(decryptedText).isEqualTo(originalText)
    }

    @Test
    fun encrypt_same_text_multiple_times_produces_different_results() {
        // Arrange
        val originalText = "Test for IV"

        // Act
        val encryptedText1 = encryptedPreferenceManager.encrypt(originalText)
        val encryptedText2 = encryptedPreferenceManager.encrypt(originalText)

        // Assert
        // Because the IV (Initialization Vector) is different each time, the encryption result should be different even if the original text is the same
        assertThat(encryptedText1).isNotEqualTo(encryptedText2)

        // They can still be decrypted successfully individually
        assertThat(encryptedPreferenceManager.decrypt(encryptedText1)).isEqualTo(originalText)
        assertThat(encryptedPreferenceManager.decrypt(encryptedText2)).isEqualTo(originalText)
    }

    @Test
    fun decrypt_with_invalid_data_format_throws_IllegalArgumentException() {
        // Arrange
        val malformedData = "this is not a valid encrypted string"

        // Act & Assert
        // Verify that passing a malformed encrypted string throws the expected IllegalArgumentException
        val exception = assertFailsWith<IllegalArgumentException> {
            encryptedPreferenceManager.decrypt(malformedData)
        }

        // We can further assert the message of the Exception
        assertThat(exception.message).isEqualTo("Invalid encrypted data format")
    }

    @Test
    fun decrypt_with_tampered_data_fails() {
        // Arrange
        val originalText = "Don't tamper with this data"
        val encryptedText = encryptedPreferenceManager.encrypt(originalText)

        // Act: Tamper with the data correctly
        // 1. Split the IV and the encrypted data
        val parts = encryptedText.split("]")
        val ivBase64 = parts[0]
        val encryptedDataBase64 = parts[1]

        // 2. Decode the encrypted data from Base64 to a byte array
        val encryptedBytes = Base64.decode(encryptedDataBase64, Base64.DEFAULT)

        // 3. Tamper with the byte array (e.g., modify the first byte)
        //    Here we do a simple addition and use toByte() to ensure it's within the byte range
        if (encryptedBytes.isNotEmpty()) {
            encryptedBytes[0] = (encryptedBytes[0] + 1).toByte()
        }

        // 4. Re-encode the tampered bytes to Base64
        val tamperedDataBase64 = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)

        // 5. Reassemble into a seemingly valid encrypted string
        val tamperedEncryptedText = "$ivBase64]$tamperedDataBase64"


        // Assert
        // Because GCM mode provides authentication, the tampered data should fail to decrypt and throw an AEADBadTagException
        // This exception is a subclass of GeneralSecurityException
        assertFailsWith<javax.crypto.AEADBadTagException> {
            encryptedPreferenceManager.decrypt(tamperedEncryptedText)
        }
    }

    /**
     * Helper function to clean up the key in the Android Keystore before and after tests.
     * This is a very good practice to avoid tests interfering with each other.
     */
    private fun cleanupKeyStore() {
        val keyStore: KeyStore
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            if (keyStore.containsAlias("my_keystore_alias")) {
                keyStore.deleteEntry("my_keystore_alias")
            }
        } catch (e: Exception) {
            // Ignore exceptions during cleanup, as an error would occur anyway if the key doesn't exist
            e.printStackTrace()
        }
    }
}