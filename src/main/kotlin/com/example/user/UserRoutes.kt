package com.example.user

import org.http4k.core.Body
import org.http4k.core.Method.*
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson.auto
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
			val user: User = userLens(it)
			repo.insert(user)
			Response(CREATED)
		},

		// delete
		"/{username}" bind DELETE to {
			val userName = it.path("username") ?: return@to Response(BAD_REQUEST)

			// should be 1 since username is unique
			val numDeleted = repo.delete(userName)
			if (numDeleted > 0) {
				Response(OK)
			} else {
				Response(NOT_FOUND)
			}
		},
	)
}
