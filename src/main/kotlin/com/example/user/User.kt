package com.example.user

import java.time.Instant

data class User(
	val email: String,
	val username: String,
	val password: String,
	val bio: String?,
	val image: String?,
	val createdAt: Instant,
	val updatedAt: Instant,
)
