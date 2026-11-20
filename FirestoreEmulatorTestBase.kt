package com.example.base

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.net.HttpURLConnection
import java.net.URL

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
abstract class FirestoreEmulatorTestBase {

    protected lateinit var context: Context
    protected lateinit var databaseId: String
    protected lateinit var auth: FirebaseAuth
    protected lateinit var db: FirebaseFirestore

    @Before
    fun setUpFirebaseEmulators() {
        context = ApplicationProvider.getApplicationContext()
        databaseId = context.getString(R.string.firestore_database_id)

        val appName = "gideon-emulator-test"
        val firebaseApp = FirebaseApp.getApps(context).firstOrNull { it.name == appName }
            ?: FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setProjectId(PROJECT_ID)
                    .setApplicationId("1:123456789012:android:1234567890abcdef")
                    .setApiKey("AIzaSyDummyEmulatorKeyForTestingOnly12345")
                    .build(),
                appName
            )

        auth = FirebaseAuth.getInstance(firebaseApp)
        db = FirebaseFirestore.getInstance(firebaseApp, databaseId)

        if (!emulatorsBound) {
            auth.useEmulator(EMULATOR_HOST, AUTH_PORT)
            db.useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
            db.firestoreSettings = firestoreSettings {
                setLocalCacheSettings(memoryCacheSettings {})
            }
            emulatorsBound = true
        }

        auth.signOut()
        clearFirestoreEmulator()
        clearAuthEmulator()
    }

    protected suspend fun signInTestUser(
        email: String,
        password: String = "GideonPass123!",
        verifyEmail: Boolean = true
    ): FirebaseUser = withContext(Dispatchers.IO) {
        try {
            auth.createUserWithEmailAndPassword(email, password).await()
        } catch (_: Exception) {
            auth.signInWithEmailAndPassword(email, password).await()
        }
        val user = requireNotNull(auth.currentUser) { "Failed to authenticate $email" }
        if (verifyEmail && !user.isEmailVerified) {
            user.sendEmailVerification().await()
            verifyLatestEmailInEmulator(email)
            user.reload().await()
            user.getIdToken(true).await()
        }
        user
    }

    private fun verifyLatestEmailInEmulator(email: String) {
        val url = URL("http://$EMULATOR_HOST:$AUTH_PORT/emulator/v1/projects/$PROJECT_ID/oobCodes")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
        }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        val json = JSONObject(body)
        val codes = json.optJSONArray("oobCodes") ?: return
        for (i in codes.length() - 1 downTo 0) {
            val item = codes.getJSONObject(i)
            if (item.optString("email").equals(email, ignoreCase = true) &&
                item.optString("requestType") == "VERIFY_EMAIL"
            ) {
                val oobLink = item.optString("oobLink")
                if (oobLink.isNotBlank()) {
                    val verifyConn = (URL(oobLink).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 5000
                        readTimeout = 5000
                    }
                    verifyConn.responseCode
                    verifyConn.disconnect()
                    return
                }
            }
        }
    }

    private fun clearFirestoreEmulator() {
        val url = URL(
            "http://$EMULATOR_HOST:$FIRESTORE_PORT/emulator/v1/projects/$PROJECT_ID/databases/$databaseId/documents"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            connectTimeout = 5000
            readTimeout = 5000
        }
        conn.responseCode
        conn.disconnect()
    }

    private fun clearAuthEmulator() {
        val url = URL("http://$EMULATOR_HOST:$AUTH_PORT/emulator/v1/projects/$PROJECT_ID/accounts")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            connectTimeout = 5000
            readTimeout = 5000
        }
        conn.responseCode
        conn.disconnect()
    }

    companion object {
        const val PROJECT_ID = "demo-no-project"
        const val EMULATOR_HOST = "127.0.0.1"
        const val FIRESTORE_PORT = 8085
        const val AUTH_PORT = 9099

        @Volatile
        private var emulatorsBound = false
    }
}
