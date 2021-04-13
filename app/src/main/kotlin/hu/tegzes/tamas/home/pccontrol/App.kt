package hu.tegzes.tamas.home.pccontrol

import io.javalin.Javalin

data class SwitchRequest(val active: Boolean)

fun main() {
    var state = SwitchRequest(false)
    val app = Javalin.create().start(5000)
    app.get("/") { ctx ->
        println("GET " + ctx.body() + state)
        ctx.json(state)
    }
    app.post("/") { ctx ->
        println("POST" + ctx.body())
        state = ctx.bodyAsClass(SwitchRequest::class.java)
        ctx.json(state)
    }
}
