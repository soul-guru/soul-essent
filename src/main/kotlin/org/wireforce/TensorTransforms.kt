package org.wireforce

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.ssl.SSLContextBuilder
import org.slf4j.LoggerFactory
import org.wireforce.dto.KtorModelOutput
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * Utility class for handling tensor transformations and interacting with a Python-based tensor processing service.
 *
 * This class provides functionality for executing commands, installing Python dependencies,
 * prefetching tensor models, initializing and monitoring a Python daemon, and making calls to the daemon for textual tasks.
 *
 * @throws IOException if an I/O error occurs while executing commands.
 */
class TensorTransforms {
	companion object {
		private val pythonBin = System.getenv().getOrDefault("PYTHON_BIN", "python3")
		private val logger = LoggerFactory.getLogger(TensorTransforms::class.java)

		private val filePaths by lazy {
			val javaClass = TensorTransforms::class.java

			mapOf(
				"modelFetcher" to System.getenv().getOrElse("TENSOR_PYTHON_PATH_FETCHER_FILE") {
					javaClass.getResource("python/fetch_models.py")?.file
				},
				"pythonFile" to System.getenv().getOrElse("TENSOR_PYTHON_PATH_MAIN_FILE") {
					javaClass.getResource("python/main.py")?.file
				},
				"pythonRequirements" to System.getenv().getOrElse("TENSOR_PYTHON_PATH_REQ_FILE") {
					javaClass.getResource("python/requirements.txt")?.file
				}
			)
		}

		@Throws(IOException::class)
		fun execCmd(cmd: String?): String {
			val s = Scanner(Runtime.getRuntime().exec(cmd).inputStream).useDelimiter("\\A")
			return if (s.hasNext()) s.next() else ""
		}

		/**
		 * Creates and configures an instance of the HttpClient with Apache engine for making HTTP requests.
		 *
		 * @param followRedirects Whether to automatically follow redirects. Default is true.
		 * @param socketTimeout The timeout for individual socket operations in milliseconds. Default is 30,000 milliseconds.
		 * @param connectTimeout The timeout for establishing a connection in milliseconds. Default is 30,000 milliseconds.
		 * @param connectionRequestTimeout The timeout for obtaining a connection from the connection manager in milliseconds. Default is 20,000 milliseconds.
		 * @param maxConnTotal The maximum number of total open connections. Default is 1,000.
		 * @param maxConnPerRoute The maximum number of open connections per route. Default is 100.
		 * @return An instance of HttpClient configured with the specified parameters.
		 */
		private fun createHttpClient(
			followRedirects: Boolean = true,
			socketTimeout: Int = 30_000,
			connectTimeout: Int = 30_000,
			connectionRequestTimeout: Int = 20_000,
			maxConnTotal: Int = 1000,
			maxConnPerRoute: Int = 100
		) = HttpClient(Apache) {
			engine {
				this.followRedirects = followRedirects
				this.socketTimeout = socketTimeout
				this.connectTimeout = connectTimeout
				this.connectionRequestTimeout = connectionRequestTimeout

				customizeClient {
					setMaxConnTotal(maxConnTotal)
					setMaxConnPerRoute(maxConnPerRoute)
					setSSLContext(
						SSLContextBuilder
							.create()
							.loadTrustMaterial(TrustSelfSignedStrategy())
							.build()
					)
					setSSLHostnameVerifier(NoopHostnameVerifier())
				}
			}
		}

		private val pathToModelFetcher by lazy { filePaths["modelFetcher"] }
		private val pathToPythonFile by lazy { filePaths["pythonFile"] }
		private val pathToPythonRequirements by lazy { filePaths["pythonRequirements"] }

		private val client by lazy { createHttpClient() }

		// Configuration related to the Tensor Python service
		private val host = System.getenv().getOrDefault("TENSOR_PYTHON_HOST", "localhost")

		// Lazily initialized daemon port with a random default value
		private val daemonPort by lazy {
			System.getenv().getOrDefault("TENSOR_PYTHON_PORT", (4557..9557).random().toString()).toInt()
		}

		// Concatenated URL for the Tensor Python daemon
		private val daemonHost by lazy {
			"https://$host:$daemonPort"
		}

		// Gson instance for JSON serialization and deserialization
		private val gson by lazy {
			Gson()
		}
	}

	private var _isAlive = false


	/**
	 * Checks if the Python daemon is alive.
	 *
	 * @return true if the daemon is alive; false otherwise.
	 */
	fun isAlive() = _isAlive


	/**
	 * Installs Python dependencies listed in the specified requirements file using 'python3 -m pip install'.
	 * The installation is performed asynchronously on the IO dispatcher to avoid blocking the main thread.
	 * Error handling is included to catch exceptions during the installation process.
	 */
	suspend fun installDependencyFromRequirements() {
		withContext(Dispatchers.IO) {
			try {
				val requirementsFile = File(pathToPythonRequirements!!)
				if (requirementsFile.exists()) {
					requirementsFile.readLines(Charset.forName("UTF-8"))
						.forEach {
							execCmd("$pythonBin -m pip install $it")
							logger.info("+ $it installed...")
						}
				} else {
					logger.warn("Requirements file not found: $pathToPythonRequirements")
				}
			} catch (e: Exception) {
				logger.error("Error installing Python dependencies", e)
			}
		}
	}

	/**
	 * Asynchronously prefetches information about all tensor models using a Python script.
	 * The prefetch operation is performed on the IO dispatcher to avoid blocking the main thread.
	 * Information is fetched by executing a Python script specified by the 'pathToModelFetcher' property.
	 * Environment variables for Hugging Face Hub cache, Transformers cache, and HF_HOME are set during the process.
	 * Logging statements provide insights into the progress of the prefetching operation.
	 */
	suspend fun prefetchTensorModels() {
		withContext(Dispatchers.IO) {
			try {
				logger.info("- Starting to get information about all models")

				val modelFetcherScript = File(requireNotNull(pathToModelFetcher))

				// Create a ProcessBuilder for executing the Python script
				val pb = ProcessBuilder(pythonBin, modelFetcherScript.absolutePath).inheritIO()

				// Set environment variables for Hugging Face Hub cache, Transformers cache, and HF_HOME
				val environment = pb.environment()

				environment["HUGGINGFACE_HUB_CACHE"] = System.getenv().getOrDefault("HUGGINGFACE_HUB_CACHE", "./hf/HUGGINGFACE_HUB_CACHE")
				environment["TRANSFORMERS_CACHE"] = System.getenv().getOrDefault("TRANSFORMERS_CACHE", "./hf/TRANSFORMERS_CACHE")
				environment["HF_HOME"] = System.getenv().getOrDefault("HF_HOME", "./hf/HF_HOME")

				// Start the process and wait for its completion
				pb.start().waitFor()

				logger.info("+ Models installed and ready to go")
			} catch (e: Exception) {
				// Handle exceptions during the prefetching process
				logger.error("Error prefetching tensor models", e)
			}
		}
	}


	/**
	 * Checks if the specified domain is alive by making a non-blocking HTTP GET request to the given host.
	 *
	 * @param overrideHost An optional host to override the default daemon host.
	 * @return true if the domain is alive; false otherwise.
	 */
	private suspend fun checkDomainAlive(overrideHost: String? = null): Boolean = try {
		client.get(overrideHost ?: daemonHost)
		true
	} catch (e: Throwable) {
		false
	}


	/**
	 * Initializes and starts a Python process for the Tensor application.
	 *
	 * This function performs the following steps:
	 * 1. Cleans up any previous Python process by killing its associated PID.
	 * 2. Constructs a {@link ProcessBuilder} for executing the Python script.
	 * 3. Configures the process with the necessary environment variables.
	 * 4. Starts the Python process asynchronously using {@link Dispatchers.IO}.
	 * 5. Writes the process information, including PID and daemon host, to files.
	 * 6. Waits for the Tensor Python daemon to become responsive.
	 * 7. Marks the TensorTransforms as alive.
	 *
	 * @return The {@link Process} object representing the started Python process.
	 * @throws IllegalStateException If the path to the Python file is null.
	 */
	suspend fun init(): Process {
		// Step 1: Clean up any previous Python process
		cleanUpPreviousProcess()

		// Step 2: Obtain the Python file and create a ProcessBuilder
		val pythonFile = File(requireNotNull(pathToPythonFile))
		val processBuilder = ProcessBuilder(pythonBin, pythonFile.absolutePath).apply {
			directory(pythonFile.parentFile)
			environment().apply {
				// Step 3: Configure the process with necessary environment variables
				putAll(getEnvironmentVariables())
				put("WORK_COUNT", System.getenv().getOrDefault("PYTHON_DAEMON_WORKERS", "1"))
				put("FASTAPI_HOST", host)
				put("FASTAPI_PORT", daemonPort.toString())
			}
		}

		// Step 4: Start the Python process asynchronously using Dispatchers.IO
		val process = withContext(Dispatchers.IO) {
			processBuilder.start()
		}

		// Step 5: Write process information to files
		writeProcessInfoToFile(process)

		// Step 6: Wait for the Tensor Python daemon to become responsive
		waitForDomainAlive()

		// Step 7: Mark TensorTransforms as alive
		_isAlive = true

		return process
	}

	/**
	 * Cleans up any previous Python process by killing its associated PID and deleting related files.
	 * If the file "./python.pid" exists, it reads the PID, kills the process, and deletes the file.
	 */
	private fun cleanUpPreviousProcess() {
		File("./python.pid").takeIf { it.exists() }?.run {
			readText(Charset.forName("UTF-8")).let { pid ->
				Runtime.getRuntime().exec("kill -9 $pid")
				delete()
			}
		}
	}

	/**
	 * Retrieves the environment variables required for Tensor application initialization.
	 *
	 * @return A map containing environment variable names and their corresponding values.
	 */
	private fun getEnvironmentVariables(): Map<String, String> {
		return mapOf(
			"HUGGINGFACE_HUB_CACHE" to System.getenv().getOrDefault("HUGGINGFACE_HUB_CACHE", "./hf/HUGGINGFACE_HUB_CACHE"),
			"TRANSFORMERS_CACHE" to System.getenv().getOrDefault("TRANSFORMERS_CACHE", "./hf/TRANSFORMERS_CACHE"),
			"HF_HOME" to System.getenv().getOrDefault("HF_HOME", "./hf/HF_HOME")
		)
	}

	/**
	 * Writes process information, including the PID and daemon host, to files "./python.pid" and "./python-domain.host".
	 *
	 * @param process The {@link Process} object representing the Python process.
	 */
	private fun writeProcessInfoToFile(process: Process) {
		File("./python.pid").writeText(process.pid().toString())
		File("./python-domain.host").writeText(daemonHost)
	}

	/**
	 * Suspends execution until the Tensor Python daemon becomes responsive.
	 * It checks the daemon's liveness at intervals and delays for 5 seconds between checks.
	 */
	private suspend fun waitForDomainAlive() {
		while (!checkDomainAlive()) {
			withContext(Dispatchers.IO) {
				delay(5.seconds.toLong(DurationUnit.MILLISECONDS))
			}
		}
	}

	/**
	 * Calls a textual task in the specified model using the Tensor Python daemon.
	 *
	 * This function performs the following steps:
	 * 1. Checks if the Tensor Python daemon is alive; returns null if not.
	 * 2. Sends a POST request to the daemon's pipeline endpoint with the specified task and model.
	 * 3. Serializes the provided value as JSON and sets it as the request body.
	 * 4. Checks the response status; returns null if not in the 2xx range.
	 * 5. Parses the response body, expecting it to be in JSON format, into a {@link KtorModelOutput<T>} object.
	 *
	 * @param task The textual task to perform.
	 * @param model The name of the model to use for the task.
	 * @param value The input value for the task.
	 * @return The result of the textual task wrapped in a {@link KtorModelOutput<T>} object, or null if the daemon is not alive or the request fails.
	 * @param <T> The type of the result.
	 */
	suspend fun <T> callTextualTaskInModel(task: String, model: String, value: String): KtorModelOutput<T>? {
		// Step 1: Check if the Tensor Python daemon is alive; return null if not
		if (!isAlive()) return null

		// Step 2: Send a POST request to the daemon's pipeline endpoint with the specified task and model
		val response = client.post("$daemonHost/transformers/pipeline/$task?model=$model") {
			contentType(ContentType.Application.Json)
			setBody(
				gson.toJson(
					mapOf(
						"value" to value
					)
				)
			)
		}

		// Step 3: Check the response status; return null if not in the 2xx range
		if (response.status.value !in 200..299) {
			return null
		}

		// Step 4: Parse the response body into a KtorModelOutput<T> object
		return response.bodyAsText(Charset.forName("UTF-8")).let { data ->
			val collectionType = object : TypeToken<KtorModelOutput<T>?>() {}.type

			gson.fromJson(data, collectionType)
		}
	}


	/**
	 * Performs a raw GET request to the specified path using the Tensor Python daemon.
	 *
	 * This function performs the following steps:
	 * 1. Checks if the Tensor Python daemon is alive; returns null if not.
	 * 2. Sends a GET request to the specified path on the daemon.
	 * 3. Checks the response status; returns null if not in the 2xx range.
	 * 4. Retrieves the response body as raw text and prints it.
	 * 5. Deserializes the response body into an object of the specified type using Gson.
	 *
	 * @param path The path for the GET request.
	 * @return The result of the GET request deserialized into an object of type T, or null if the daemon is not alive or the request fails.
	 * @param <T> The type of the result.
	 */
	suspend fun <T> rawGetCall(path: String): T? {
		// Step 1: Check if the Tensor Python daemon is alive; return null if not
		if (!isAlive()) return null

		// Step 2: Send a GET request to the specified path on the daemon
		val response = client.get("$daemonHost$path") {}

		// Step 3: Check the response status; return null if not in the 2xx range
		if (response.status.value !in 200..299) {
			return null
		}

		// Step 4: Retrieve the response body as raw text and print it
		val dataRaw = response.bodyAsText(Charset.forName("UTF-8"))

		logger.info("calling $path")
		logger.info(dataRaw)

		// Step 5: Deserialize the response body into an object of type T using Gson
		return gson.fromJson(dataRaw, (object : TypeToken<T>() {}).type)
	}
}

