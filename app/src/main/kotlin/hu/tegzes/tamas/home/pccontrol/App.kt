package hu.tegzes.tamas.home.pccontrol

import io.javalin.Javalin
import io.javalin.http.Handler
import java.awt.GraphicsEnvironment

data class SwitchStatus(val active: Boolean)

class Switch(val path: String, val stateFun: (Configuration) -> Boolean?) {
    var state = SwitchStatus(false)
    val active: Boolean
        get() = state.active
    val getHandler = Handler { ctx ->
        stateFun(configuration)?.let { state = SwitchStatus(it) }
        println("GET " + ctx.body() + state)
        println()
        ctx.json(state)
    }
    val postHandler = Handler { ctx ->
        println("POST" + ctx.body())
        state = ctx.bodyAsClass(SwitchStatus::class.java)
        ctx.status(200)
    }
}

data class Configuration(val usingMain: Boolean, val usingSide: Boolean, val sidePortrait: Boolean?)

private val graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment()

val configuration: Configuration
    get() {
        var usingMain = false
        var usingSide = false
        var sidePortrait: Boolean? = null
        graphicsEnvironment.screenDevices.forEach {
            val bounds = it.defaultConfiguration.bounds
            when {
                bounds.height == 1440 && bounds.width == 2560 -> {
                    usingMain = true
                }
                bounds.height == 1440 && bounds.width == 900 -> {
                    usingSide = true
                    sidePortrait = true
                }
                bounds.height == 900 && bounds.width == 1440 -> {
                    usingSide = true
                    sidePortrait = false
                }
            }
        }
        return Configuration(usingMain, usingSide, sidePortrait)
    }


fun main() {
    val app = Javalin.create().start(5000)
    val useMain = Switch("/main", Configuration::usingMain)
    val useSide = Switch("/side", Configuration::usingSide)
    val sidePortrait = Switch("/side_portrait", Configuration::sidePortrait)
    val switches = listOf(useMain, useSide, sidePortrait)
    for (switch in switches) {
        app.get(switch.path, switch.getHandler)
        app.post(switch.path, switch.postHandler)
        app.after { ctx ->
            if (ctx.method() == "POST") {
                setDisplayConf(useMain.active, useSide.active, sidePortrait.active)
            }
        }
    }
}

val runtime: Runtime = Runtime.getRuntime()
fun setDisplayConf(useMain: Boolean, useSide: Boolean, sidePortrait: Boolean) {
    if (useMain != configuration.usingMain
        || useSide != configuration.usingSide
        || (configuration.usingSide && sidePortrait != configuration.sidePortrait)
    ) {
        val mainPart = if (useMain) "main" else "noMain"
        val sidePart = if (useSide) "side${if (sidePortrait) "Portrait" else "Landscape"}" else "noSide"
        val filename = """${System.getenv("APPDATA")}\MonitorSwitcher\Profiles\$mainPart+$sidePart.xml"""
        runtime.exec("""MonitorSwitcher.exe -load:${filename}""").waitFor()
    }
}
