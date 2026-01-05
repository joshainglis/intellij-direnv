package systems.fehn.intellijdirenv.services

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import systems.fehn.intellijdirenv.MyBundle
import systems.fehn.intellijdirenv.notificationGroup
import systems.fehn.intellijdirenv.settings.DirenvSettingsState
import systems.fehn.intellijdirenv.switchNull

/**
 * Represents a summary of environment variable changes from a direnv import.
 */
data class EnvChangeSummary(
    val added: List<String> = emptyList(),
    val modified: List<String> = emptyList(),
    val removed: List<String> = emptyList()
) {
    val hasChanges: Boolean get() = added.isNotEmpty() || modified.isNotEmpty() || removed.isNotEmpty()
}

@Service(Service.Level.PROJECT)
class DirenvProjectService(private val project: Project) {
    private val logger by lazy { logger<DirenvProjectService>() }

    private val projectDir = project.guessProjectDir()
        .switchNull(
            onNull = { logger.warn("Could not determine project dir of project ${project.name}") },
        )

    val projectEnvrcFile: VirtualFile?
        get() = projectDir?.findChild(".envrc")?.takeUnless { it.isDirectory }
            .switchNull(
                onNull = { logger.trace { "Project ${project.name} contains no .envrc file" } },
                onNonNull = { logger.trace { "Project ${project.name} has .envrc file ${it.path}" } },
            )

    private val envService by lazy { ApplicationManager.getApplication().getService(EnvironmentService::class.java) }

    private val jsonFactory by lazy { JsonFactory() }

    fun importDirenv(envrcFile: VirtualFile, notifyNoChange: Boolean = true) {
        val process = executeDirenv(envrcFile, "export", "json")

        if (process.waitFor() != 0) {
            handleDirenvError(process, envrcFile)
            return
        }

        jsonFactory.createParser(process.inputStream).use { parser ->

            try {
                val changeSummary = handleDirenvOutput(parser)

                if (changeSummary.hasChanges) {
                    val content = buildChangeSummaryMessage(changeSummary)
                    notificationGroup
                        .createNotification(
                            MyBundle.message("executedSuccessfully"),
                            content,
                            NotificationType.INFORMATION,
                        ).notify(project)
                } else if (notifyNoChange) {
                    notificationGroup
                        .createNotification(
                            MyBundle.message("alreadyUpToDate"),
                            "",
                            NotificationType.INFORMATION,
                        ).notify(project)
                }
            } catch (e: EnvironmentService.ManipulateEnvironmentException) {
                notificationGroup
                    .createNotification(
                        MyBundle.message("exceptionNotification"),
                        e.localizedMessage,
                        NotificationType.ERROR,
                    ).notify(project)
            }
        }
    }

    private fun handleDirenvOutput(parser: JsonParser): EnvChangeSummary {
        val added = mutableListOf<String>()
        val modified = mutableListOf<String>()
        val removed = mutableListOf<String>()

        while (parser.nextToken() != null) {
            if (parser.currentToken == JsonToken.FIELD_NAME) {
                val varName = parser.currentName
                val currentValue = envService.getVariable(varName)

                when (parser.nextToken()) {
                    JsonToken.VALUE_NULL -> {
                        if (currentValue != null) {
                            envService.unsetVariable(varName)
                            removed.add(varName)
                            logger.trace { "Removed variable $varName" }
                        }
                    }
                    JsonToken.VALUE_STRING -> {
                        val newValue = parser.valueAsString
                        if (currentValue == null) {
                            added.add(varName)
                            logger.trace { "Added variable $varName" }
                        } else if (currentValue != newValue) {
                            modified.add(varName)
                            logger.trace { "Modified variable $varName" }
                        }
                        envService.setVariable(varName, newValue)
                    }
                    else -> continue
                }
            }
        }

        return EnvChangeSummary(added, modified, removed)
    }

    private fun buildChangeSummaryMessage(summary: EnvChangeSummary): String {
        val parts = mutableListOf<String>()

        if (summary.added.isNotEmpty()) {
            val count = summary.added.size
            parts.add(MyBundle.message("changeSummary.added", count))
        }
        if (summary.modified.isNotEmpty()) {
            val count = summary.modified.size
            parts.add(MyBundle.message("changeSummary.modified", count))
        }
        if (summary.removed.isNotEmpty()) {
            val count = summary.removed.size
            parts.add(MyBundle.message("changeSummary.removed", count))
        }

        return parts.joinToString(", ")
    }

    private fun handleDirenvError(process: Process, envrcFile: VirtualFile) {
        val error = process.errorStream.bufferedReader().readText()

        val notification = if (error.contains(" is blocked")) {
            notificationGroup
                .createNotification(
                    MyBundle.message("envrcNotYetAllowed"),
                    "",
                    NotificationType.WARNING,
                )
                .addAction(
                    NotificationAction.create(MyBundle.message("allow")) { _, notification ->
                        notification.hideBalloon()
                        executeDirenv(envrcFile, "allow").waitFor()

                        importDirenv(envrcFile)
                    },
                )
        } else {
            logger.error(error)

            notificationGroup
                .createNotification(
                    MyBundle.message("errorDuringDirenv"),
                    "",
                    NotificationType.ERROR,
                )
        }

        notification
            .addAction(
                NotificationAction.create(MyBundle.message("openEnvrc")) { _, it ->
                    it.hideBalloon()

                    FileEditorManager.getInstance(project).openFile(envrcFile, true, true)
                },
            )
            .notify(project)
    }

    private fun executeDirenv(envrcFile: VirtualFile, vararg args: String): Process {
        val workingDir = envrcFile.parent.path

        val cli = GeneralCommandLine("direnv", *args)
            .withWorkDirectory(workingDir)

        val appSettings = DirenvSettingsState.getInstance()
        if (appSettings.direnvSettingsPath.isNotEmpty()) {
            cli.withExePath(appSettings.direnvSettingsPath)
        }

        return cli.createProcess()
    }
}
