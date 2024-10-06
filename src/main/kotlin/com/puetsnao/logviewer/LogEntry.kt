package com.puetsnao.logviewer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.InputStreamReader

// Data class para representar la estructura de cada log
data class LogEntry(
    val timestamp: String,
    val message: Map<String, Any>?,  // Solo se llena si es JSON válido
    val rawMessage: String?          // Solo se llena si no es JSON
)

@RestController
@RequestMapping("/api/logs")
class LogController {

    // Endpoint para subir el archivo y procesarlo
    @PostMapping("/upload")
    fun uploadLogFile(@RequestParam("file") file: MultipartFile): ResponseEntity<List<LogEntry>> {
        val logs = mutableListOf<LogEntry>()
        val objectMapper = jacksonObjectMapper()  // Crear instancia de ObjectMapper

        // Leer el archivo línea por línea
        BufferedReader(InputStreamReader(file.inputStream)).use { reader ->
            reader.forEachLine { line ->
                // Dividimos la línea por la primera aparición de "] " para obtener el timestamp y el mensaje
                val parts = line.split("] ", limit = 2)
                if (parts.size >= 2) {
                    val timestamp = parts[0].replace("[", "").trim()  // Extraer timestamp
                    var rawMessage = parts[1].trim()  // Extraer el mensaje crudo

                    // Eliminar la palabra "info" al inicio si está presente
                    if (rawMessage.startsWith("info")) {
                        rawMessage = rawMessage.removePrefix("info").trim()
                    }

                    // Intentar parsear el rawMessage como JSON si es posible
                    val logEntry = try {
                        val message: MutableMap<String, Any> = objectMapper.readValue(rawMessage)

                        // Verificar si el campo "response" existe y está como cadena
                        if (message.containsKey("response") && message["response"] is String) {
                            val responseString = message["response"] as String

                            // Intentar convertir la cadena "response" a JSON si está escapada
                            try {
                                val jsonResponse: Map<String, Any> = objectMapper.readValue(responseString)
                                message["response"] = jsonResponse  // Reemplazar la cadena por el objeto JSON
                            } catch (e: Exception) {
                                // Si no es JSON válido, dejar el valor tal como está
                                println("El campo 'response' no es un JSON válido: $responseString")
                            }
                        }

                        LogEntry(timestamp, message, null)  // Llenar solo el campo "message"
                    } catch (e: Exception) {
                        LogEntry(timestamp, null, rawMessage)  // Llenar solo el campo "rawMessage"
                    }

                    // Agregar la entrada del log a la lista
                    logs.add(logEntry)
                }
            }
        }

        // Devolver la lista de logs procesados como JSON
        return ResponseEntity(logs, HttpStatus.OK)
    }
}
