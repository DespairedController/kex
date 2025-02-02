package org.vorpal.research.kex

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import org.vorpal.research.kex.config.FileConfig
import org.vorpal.research.kex.config.RunnerCmdConfig
import org.vorpal.research.kex.config.RuntimeConfig
import org.vorpal.research.kex.config.kexConfig
import org.vorpal.research.kex.launcher.*
import org.vorpal.research.kex.util.getPathSeparator
import org.vorpal.research.kthelper.logging.log
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

@ExperimentalSerializationApi
@InternalSerializationApi
fun main(args: Array<String>) {
    val cmd = RunnerCmdConfig(args)
    val properties = cmd.getCmdValue("config", "kex.ini")
    kexConfig.initialize(cmd, RuntimeConfig, FileConfig(properties))

    // initialize output dir
    (cmd.getCmdValue("output")?.let { Paths.get(it) }
        ?: kexConfig.getPathValue("kex", "outputDir")
        ?: Files.createTempDirectory(Paths.get("."), "kex-output"))
        .toAbsolutePath().also {
            RuntimeConfig.setValue("kex", "outputDir", it)
        }

    val logName = kexConfig.getStringValue("kex", "log", "kex.log")
    kexConfig.initLog(logName)

    val classPaths = cmd.getCmdValue("classpath")?.split(getPathSeparator())
    require(classPaths != null, cmd::printHelp)

    val targetName = cmd.getCmdValue("target")
    require(targetName != null, cmd::printHelp)

    try {
        val launcher: KexLauncher = when (cmd.getEnumValue("mode", LaunchMode.Concolic, ignoreCase = true)) {
            LaunchMode.Symbolic -> SymbolicLauncher(classPaths, targetName)
            LaunchMode.Concolic -> ConcolicLauncher(classPaths, targetName)
            LaunchMode.LibChecker -> LibraryCheckLauncher(classPaths, targetName)
            LaunchMode.DefectChecker -> DefectCheckerLauncher(classPaths, targetName)
        }
        launcher.launch()
    } catch (e: LauncherException) {
        log.error(e.message)
        exitProcess(1)
    }
}