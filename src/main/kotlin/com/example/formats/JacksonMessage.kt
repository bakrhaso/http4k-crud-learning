package com.example.formats

import org.http4k.core.Body
import org.http4k.format.Jackson.array
import org.http4k.format.Jackson.boolean
import org.http4k.format.Jackson.json
import org.http4k.format.Jackson.number
import org.http4k.format.Jackson.obj
import org.http4k.format.Jackson.string

val jacksonMessageLens = Body.json().toLens()

val jacksonMessage = obj(
	"thisIsAString" to string("stringValue"),
	"thisIsANumber" to number(12345),
	"thisIsAList" to array(listOf(boolean(true)))
)
