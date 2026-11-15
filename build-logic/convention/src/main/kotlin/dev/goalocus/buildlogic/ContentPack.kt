package dev.goalocus.buildlogic

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.process.ExecOperations

/**
 * `goalocus.content-pack` — runs `tools/contentc` on `content/` and adds the resulting
 * `content/content-pack.json` to the app's assets. Content errors fail the build.
 */
class ContentPackConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val root = isolated.rootProject.projectDirectory
            val compileContentPack = tasks.register<CompileContentPackTask>("compileContentPack") {
                group = "goalocus"
                description = "Validates content/ and compiles it into the app's content pack."
                contentDirectory.set(root.dir("content"))
                toolSources.from(root.dir("tools/contentc/contentc").asFileTree.matching { include("**/*.py") })
                toolDirectory.set(root.dir("tools/contentc"))
                pythonExecutable.set(providers.gradleProperty("goalocus.python").orElse("python3"))
                outputDirectory.set(layout.buildDirectory.dir("generated/goalocus/contentPack"))
            }
            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationAndroidComponentsExtension> {
                    onVariants { variant ->
                        variant.sources.assets?.addGeneratedSourceDirectory(
                            compileContentPack,
                            CompileContentPackTask::outputDirectory,
                        )
                    }
                }
            }
        }
    }
}

@CacheableTask
abstract class CompileContentPackTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val contentDirectory: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val toolSources: ConfigurableFileCollection

    @get:Internal
    abstract val toolDirectory: DirectoryProperty

    @get:Input
    abstract val pythonExecutable: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    protected abstract val execOperations: ExecOperations

    @TaskAction
    fun compile() {
        val packFile = outputDirectory.file(PACK_ASSET_PATH).get().asFile
        packFile.parentFile.mkdirs()
        val python = pythonExecutable.get()
        try {
            execOperations.exec {
                workingDir = toolDirectory.get().asFile
                environment("PYTHONDONTWRITEBYTECODE", "1")
                commandLine(
                    python,
                    "-m",
                    "contentc",
                    "build",
                    contentDirectory.get().asFile.absolutePath,
                    "--out",
                    packFile.absolutePath,
                )
            }
        } catch (e: Exception) {
            throw GradleException(
                "Content pack build failed. If the content checker printed errors above, fix them in content/. " +
                    "If '$python' couldn't be started, install Python 3.11+ or set goalocus.python " +
                    "in ~/.gradle/gradle.properties.",
                e,
            )
        }
    }

    companion object {
        /** Path inside the APK's assets; must match `AssetContentSource` in the app. */
        const val PACK_ASSET_PATH = "content/content-pack.json"
    }
}
