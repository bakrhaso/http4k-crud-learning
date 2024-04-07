package com.example.user

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction

class UserRepository {

	init {
		transaction {
			SchemaUtils.create(Users)
		}
	}

	private object Users : IntIdTable("users") {
		val email = text("email").uniqueIndex()
		val username = text("name").uniqueIndex()
		val password = text("password")
		val bio = text("bio").nullable()
		val image = text("image").nullable()
		val createdAt = timestamp("created_at")
		val updatedAt = timestamp("updated_at")
	}

	private fun userMapper(result: ResultRow) = User(
		email = result[Users.email],
		username = result[Users.username],
		password = result[Users.password],
		bio = result[Users.bio],
		image = result[Users.image],
		createdAt = result[Users.createdAt],
		updatedAt = result[Users.updatedAt],
	)

	fun getAll() = transaction {
		Users
			.selectAll()
			.map(::userMapper)
	}

	fun getByUsername(username: String) = transaction {
		Users
			.selectAll()
			.where { Users.username eq username }
			.map(::userMapper)
			.firstOrNull()
	}

	fun insert(user: User) = transaction {
		Users.insert {
			it[email] = user.email
			it[username] = user.username
			it[password] = user.password
			it[bio] = user.bio
			it[image] = user.image
			it[createdAt] = user.createdAt
			it[updatedAt] = user.updatedAt
		}
	}

	fun delete(username: String) = transaction {
		Users.deleteWhere { Users.username eq username }
	}
}
