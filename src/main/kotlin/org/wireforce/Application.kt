package org.wireforce

import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import org.fusesource.jansi.AnsiConsole
import org.slf4j.LoggerFactory
import org.wireforce.plugins.configureHttp
import org.wireforce.plugins.configureRouting
import org.wireforce.plugins.configureRoutingApplications
import java.util.*

private val logger = LoggerFactory.getLogger(Application::class.java)

lateinit var tensor: TensorTransforms

fun main(args: Array<String>) {
	val banner64 = "IF9fX19fXyAgICAgX19fX19fICAgICBfXyAgX18gICAgIF9fICAgICAgICAgICAgX19fX19fICAgICBfX19fX18gICAgIF9fX19fXyAgICAgX19fX19fICAgICBfXyAgIF9fICAgICBfX19fX18gIAovXCAgX19fXCAgIC9cICBfXyBcICAgL1wgXC9cIFwgICAvXCBcICAgICAgICAgIC9cICBfX19cICAgL1wgIF9fX1wgICAvXCAgX19fXCAgIC9cICBfX19cICAgL1wgIi0uXCBcICAgL1xfXyAgX1wgClwgXF9fXyAgXCAgXCBcIFwvXCBcICBcIFwgXF9cIFwgIFwgXCBcX19fXyAgICAgXCBcICBfX1wgICBcIFxfX18gIFwgIFwgXF9fXyAgXCAgXCBcICBfX1wgICBcIFwgXC0uICBcICBcL18vXCBcLyAKIFwvXF9fX19fXCAgXCBcX19fX19cICBcIFxfX19fX1wgIFwgXF9fX19fXCAgICAgXCBcX19fX19cICBcL1xfX19fX1wgIFwvXF9fX19fXCAgXCBcX19fX19cICBcIFxfXFwiXF9cICAgIFwgXF9cIAogIFwvX19fX18vICAgXC9fX19fXy8gICBcL19fX19fLyAgIFwvX19fX18vICAgICAgXC9fX19fXy8gICBcL19fX19fLyAgIFwvX19fX18vICAgXC9fX19fXy8gICBcL18vIFwvXy8gICAgIFwvXy8gCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA="

	AnsiConsole.systemInstall()

	val env = System.getenv()

	val mapOfEnvKeys = listOf(
		"PYTHON_DAEMON_WORKERS" to "",

		"TENSOR_PYTHON_HOST" to "",
		"TENSOR_PYTHON_PORT" to "",

		"TENSOR_PYTHON_PASS_PIP_INSTALLATION" to "",
		"TENSOR_PYTHON_PASS_MODEL_PREFETCHING" to "",

		"TENSOR_PYTHON_PATH_FETCHER_FILE" to "",
		"TENSOR_PYTHON_PATH_REQ_FILE" to "",
		"TENSOR_PYTHON_PATH_MAIN_FILE" to "",

		"HUGGINGFACE_HUB_CACHE" to "",
		"TRANSFORMERS_CACHE" to "",
		"HF_HOME" to "",

		"RUN_KTOR_FORCE" to "",

	)

	println(String(Base64.getDecoder().decode(banner64)))
	println()
	println("  Welcome to SOUL Essent! A system that will help make yourvirtual agents more alive\n  through high-level tools aimed at speed and task focus.")
	println("  Use all the capabilities of the Ktor server to speed up processing a large number\n  of requests, the Python server to work with HuggingFace and other Python language sets!")
	println()
	println("  We install PIP dependencies every system startup,\n  unless you set the RUN_KTOR_FORCE flag in the environment variables\n")

	mapOfEnvKeys.forEach { envPair ->
		println("  - ${envPair.first.uppercase()}: ${envPair.second.capitalize()} (${env.getOrDefault(envPair.first.uppercase(), "-")}) ")
	}

	println()

//	logger.info("Python version is " + execCmd("python3 --version"))
//
//	execCmd("python3 --version")

	tensor = TensorTransforms()

	runBlocking {
		if (!env.getOrDefault("TENSOR_PYTHON_PASS_PIP_INSTALLATION", "false").toBoolean()) {
			tensor.installDependencyFromRequirements()
		}

		if (!env.getOrDefault("TENSOR_PYTHON_PASS_MODEL_PREFETCHING", "false").toBoolean()) {
			tensor.prefetchTensorModels()
		}

		tensor.init()
	}

	if (tensor.isAlive() || System.getenv().getOrDefault("RUN_KTOR_FORCE", "false").toBoolean()) {
		io.ktor.server.cio.EngineMain.main(args)
	}
}

fun Application.module() {
	configureRoutingApplications()
	configureRouting()
	configureHttp()
}
