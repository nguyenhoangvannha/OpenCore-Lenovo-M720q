package com.lenovo.m720q.efitool

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val isSuccess: Boolean get() = exitCode == 0
}

object RootHelper {

    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val exit = process.waitFor()
            exit == 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun requestRootPermission(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("su")
            DataOutputStream(process.outputStream).use { os ->
                os.writeBytes("id\n")
                os.writeBytes("exit\n")
                os.flush()
            }
            val exitCode = process.waitFor()
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            exitCode == 0 && output.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }

    suspend fun runSuCommand(
        command: String,
        onLog: ((String) -> Unit)? = null
    ): CommandResult = withContext(Dispatchers.IO) {
        var stdout = ""
        var stderr = ""
        var exitCode = -1

        try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            val inputStream = BufferedReader(InputStreamReader(process.inputStream))
            val errorStream = BufferedReader(InputStreamReader(process.errorStream))

            outputStream.writeBytes("$command\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()

            val outThread = Thread {
                val sb = StringBuilder()
                var line: String?
                while (inputStream.readLine().also { line = it } != null) {
                    sb.appendLine(line)
                    onLog?.invoke(line ?: "")
                }
                stdout = sb.toString()
            }

            val errThread = Thread {
                val sb = StringBuilder()
                var line: String?
                while (errorStream.readLine().also { line = it } != null) {
                    sb.appendLine(line)
                    onLog?.invoke("[ERR] $line")
                }
                stderr = sb.toString()
            }

            outThread.start()
            errThread.start()

            exitCode = process.waitFor()
            outThread.join()
            errThread.join()
        } catch (e: Exception) {
            stderr = e.localizedMessage ?: "Unknown error"
            onLog?.invoke("[EXCEPTION] $stderr")
        }

        CommandResult(exitCode, stdout.trim(), stderr.trim())
    }
}
