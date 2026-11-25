package com.example.core

sealed interface AuthSession {
    val sessionKey: String
    val displayName: String

    data class Google(
        val uid: String,
        override val displayName: String
    ) : AuthSession {
        override val sessionKey: String get() = "google_$uid"
    }

    data class Local(
        val username: String,
        override val displayName: String
    ) : AuthSession {
        override val sessionKey: String get() = "local_$username"
    }
}
