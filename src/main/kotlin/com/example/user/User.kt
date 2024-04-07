package com.example.user

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class User(
	val email: String,
	val username: String,
	val password: String,
	val bio: String?,
	val image: String?,
	val createdAt: Instant,
	val updatedAt: Instant,
)
