package com.example

import com.example.formats.kotlinXMessage
import com.example.formats.kotlinXMessageLens
import com.example.routes.ExampleContractRoute
import com.example.user.userRoutes
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.http4k.contract.bind
import org.http4k.contract.contract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.events.*
import org.http4k.filter.CorsPolicy
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.ResponseFilters
import org.http4k.filter.ServerFilters
import org.http4k.format.KotlinxSerialization
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.transactions.TransactionManager

fun app(): HttpHandler = routes(
	"/ping" bind GET to {
		Response(OK).body("pong")
	},

	"/formats/json/kotlinx" bind GET to {
		Response(OK).with(kotlinXMessageLens of kotlinXMessage)
	},

	"/contract/api/v1" bind contract {
		renderer = OpenApi3(ApiInfo("HelloWorld API", "v1.0"))

		// Return Swagger API definition under /contract/api/v1/swagger.json
		descriptionPath = "/swagger.json"

		// You can use security filter tio protect routes
		security = ApiKeySecurity(Query.int().required("api"), { it == 42 }) // Allow only requests with &api=42

		// Add contract routes
		routes += ExampleContractRoute()
	},

	"api/v1/users" bind userRoutes()
)

/**
 * @return left = kotlinx, right = java
 */
private fun getClock(): Pair<kotlinx.datetime.Clock, java.time.Clock> {
	return Pair(kotlinx.datetime.Clock.System, java.time.Clock.systemUTC())
}

fun main() {
	// clock
	val (kotlinClock, javaClock) = getClock()

	// database config/connection
	val hikariConfig = HikariConfig().apply {
		jdbcUrl = "jdbc:postgresql://localhost:5432/postgres"
		driverClassName = "org.postgresql.Driver"
		username = "postgres"
		password = "examplepassword"
		maximumPoolSize = 6
		// if these options are set here, they do not need to be duplicated in DatabaseConfig
		isReadOnly = false
		transactionIsolation = "TRANSACTION_SERIALIZABLE"
	}
	val dataSource = HikariDataSource(hikariConfig)
	val database = Database.connect(dataSource, databaseConfig = DatabaseConfig())
	TransactionManager.defaultDatabase = database

	// structured logging
	val events =
		EventFilters.AddTimestamp(javaClock)
			.then(EventFilters.AddEventName())
			.then(EventFilters.AddZipkinTraces())
			.then(AddRequestCount())
			.then(AutoMarshallingEvents(KotlinxSerialization))

	// cors
	val cors = ServerFilters.Cors(CorsPolicy.UnsafeGlobalPermissive)

	val printingApp: HttpHandler = ResponseFilters.ReportHttpTransaction {
		// to "emit" an event, just invoke() the Events!
		events(
			IncomingHttpRequest(
				uri = it.request.uri,
				status = it.response.status.code,
				duration = it.duration.toMillis(),
//				requestBody = it.request.body
			)
		)
	}
		.then(ServerFilters.CatchAll {
			events(ErrorEvent(it))
			Response(INTERNAL_SERVER_ERROR)
		})
		.then(DebuggingFilters.PrintRequestAndResponse())
		.then(cors)
		.then(app())

	val server = printingApp.asServer(SunHttp(9000)).start()

	println("Server started on " + server.port())
}

// this is our custom event which will be printed in a structured way
data class IncomingHttpRequest(
	val uri: Uri,
	val status: Int,
	val duration: Long,
//	val requestBody: Body,
) : Event

data class ErrorEvent(
	val error: Throwable
) : Event

// here is a new EventFilter that adds custom metadata to the emitted events
fun AddRequestCount(): EventFilter {
	var requestCount = 0
	return EventFilter { next ->
		{
			next(it + ("requestCount" to requestCount++))
		}
	}
}
