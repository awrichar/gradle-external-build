package co.arichardson.gradle.make

import co.arichardson.gradle.make.context.BuildConfigContext
import co.arichardson.gradle.make.context.BuildOutputContext
import co.arichardson.gradle.make.internal.DefaultExternalNativeLibrarySpec
import co.arichardson.gradle.make.tasks.OutputRedirectingExec
import org.gradle.api.Task
import org.gradle.api.file.CopySpec
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.tasks.StopExecutionException
import org.gradle.language.cpp.CppSourceSet
import org.gradle.language.nativeplatform.tasks.AbstractNativeCompileTask
import org.gradle.model.ModelMap
import org.gradle.model.Mutate
import org.gradle.model.Path
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.tasks.ObjectFilesToBinary
import org.gradle.platform.base.ComponentType
import org.gradle.platform.base.TypeBuilder

class ExternalBuildPlugin extends RuleSource {
    public static final String EXTERNAL_SOURCE = 'externalSource'
    public static final String EXTERNAL_BUILD_TASK = 'externalBuild'

    @ComponentType
    void registerExternalLibraryType(TypeBuilder<ExternalNativeLibrarySpec> builder) {
        builder.defaultImplementation(DefaultExternalNativeLibrarySpec)
    }

    @Mutate
    void configureExternalLibraries(ModelMap<ExternalNativeLibrarySpec> libraries) {
        libraries.all { library ->
            library.binaries.withType(NativeBinarySpec) { binary ->
                // Evaluate the "buildOutput" block
                BuildOutputContext outputContext = new BuildOutputContext(binary)
                Utils.invokeWithContext(library.buildOutput, outputContext)

                // Create the source set to include exported headers
                binary.sources.create(EXTERNAL_SOURCE, CppSourceSet) {
                    it.exportedHeaders.srcDirs = outputContext.headersContext.srcDirs
                }

                // Disable all normal compile tasks
                binary.tasks.withType(AbstractNativeCompileTask) {
                    it.enabled = false
                }

                // Replace the create/link task with a simple copy
                binary.tasks.withType(ObjectFilesToBinary) { mainTask ->
                    FileOperations ops = mainTask.services.get(FileOperations)

                    mainTask.inputs.file outputContext.outputFile
                    mainTask.doFirst {
                        ops.copy(new ClosureBackedAction<CopySpec>({
                            it.from outputContext.outputFile
                            it.into mainTask.outputFile.parentFile
                            it.rename { mainTask.outputFile.name }
                        }))

                        throw new StopExecutionException()
                    }
                }
            }
        }
    }

    @Mutate
    void createExternalLibraryTasks(ModelMap<Task> tasks, @Path('binaries') ModelMap<NativeBinarySpec> binaries) {
        List<Task> buildTasks = []

        binaries.findAll { it.component in ExternalNativeLibrarySpec } .each { NativeBinarySpec binary ->
            ExternalNativeLibrarySpec library = binary.component as ExternalNativeLibrarySpec

            // Create a task for the external build
            String taskName = binary.tasks.taskName(EXTERNAL_BUILD_TASK)
            tasks.create(taskName, library.buildTaskType)
            Task buildTask = tasks.get(taskName) as OutputRedirectingExec

            // Configure the task with the "buildConfig" block
            BuildConfigContext inputContext = new BuildConfigContext(binary, buildTask)
            Utils.invokeWithContext(library.buildConfig, inputContext)

            // Reuse an existing task if a duplicate exists
            Task existingTask = buildTasks.find { it.equals(buildTask) }
            if (existingTask) {
                buildTask = existingTask
            } else {
                buildTasks << buildTask
            }

            buildTask.dependsOn(binary.libs*.linkFiles)
            binary.sources.get(EXTERNAL_SOURCE).builtBy(buildTask)
        }
    }
}
