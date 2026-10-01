package com.example.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    data object Unauthenticated : AuthState()
    data object Authenticating : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthManager(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    private val _authState = MutableStateFlow<AuthState>(
        auth.currentUser?.let { AuthState.Authenticated(it) } ?: AuthState.Unauthenticated
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val currentUserId: String?
        get() = auth.currentUser?.uid

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _authState.value = if (user != null) {
                AuthState.Authenticated(user)
            } else {
                AuthState.Unauthenticated
            }
        }
    }

    suspend fun signInWithGoogle(): Result<FirebaseUser> {
        _authState.value = AuthState.Authenticating
        return try {
            val serverClientId = context.getString(R.string.default_web_client_id)

            val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: throw Exception("Firebase user is null after sign-in")
                _authState.value = AuthState.Authenticated(user)
                Result.success(user)
            } else {
                throw Exception("Unexpected credential type: ${credential::class.java.name}")
            }
        } catch (e: GetCredentialCancellationException) {
            _authState.value = auth.currentUser?.let { AuthState.Authenticated(it) } ?: AuthState.Unauthenticated
            Result.failure(Exception("Sign in cancelled by user"))
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Google sign in failed"
            _authState.value = AuthState.Error(errorMsg)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }
}
