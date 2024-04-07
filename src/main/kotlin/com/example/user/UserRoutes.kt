package com.example.user

import kotlinx.serialization.json.Json
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.KotlinxSerialization.auto
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes

private val usersLens = Body.auto<List<User>>().toLens()
private val userLens = Body.auto<User>().toLens()

fun userRoutes(): RoutingHttpHandler {
	val repo = UserRepository()

	return routes(
		// get all
		"" bind GET to {
			val users = repo.getAll()
			Response(OK).with(usersLens of users)
		},

		// get by username
		"/{username}" bind GET to {
			val userName = it.path("username") ?: return@to Response(BAD_REQUEST)
			val user = repo.getByUsername(userName) ?: return@to Response(NOT_FOUND)
			Response(OK).with(userLens of user)
		},

		// insert
		"" bind POST to {
			val userJson: String = it.bodyString()
			val user: User = Json.decodeFromString(userJson)
			repo.insert(user)
			Response(CREATED)
		},
	)
}
