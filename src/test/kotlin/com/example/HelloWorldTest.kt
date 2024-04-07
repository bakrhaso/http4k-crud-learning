package com.example

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class HelloWorldTest {

    @Test
	@Disabled(value = "Disabled since there's no test set-up for database, etc.")
    fun `Ping test`() {
//        assertEquals(Response(OK).body("pong"), app(Request(GET, "/ping")))
    }

}
