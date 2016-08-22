package co.arichardson.gradle.make

import co.arichardson.gradle.make.context.BuildConfigContext
import co.arichardson.gradle.make.context.BuildOutputContext
import co.arichardson.gradle.make.internal.DefaultExternalNativeExecutableSpec
import co.arichardson.gradle.make.internal.DefaultExternalNativeLibrarySpec
import org.gradle.api.Task
import org.gradle.api.tasks.StopExecutionException
import org.gradle.language.cpp.CppSourceSet
import org.gradle.language.nativeplatform.tasks.AbstractNativeCompileTask
import org.gradle.model.ModelMap
import org.gradle.model.Mutate
import org.gradle.model.Path
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.tasks.ObjectFilesToBinary
import org.gradle.platform.base.ComponentSpec
import org.gradle.platform.base.ComponentType
import org.gradle.platform.base.TypeBuilder

class ExternalBuildPlugin extends RuleSource {
    public static final String EXTERNAL_SOURCE = 'externalSource'
    public static final String EXTERNAL_BUILD_TASK = 'externalBuild'

    @ComponentType
    void registerExternalLibraryType(TypeBuilder<ExternalNativeLibrarySpec> builder) {
        builder.defaultImplementation(DefaultExternalNativeLibrarySpec)
    }

    @ComponentType
    void registerExternalExecutableType(TypeBuilder<ExternalNativeExecutableSpec> builder) {
        builder.defaultImplementation(DefaultExternalNativeExecutableSpec)
    }

    @Mutate
    void addExternalSourceSet(ModelMap<ExternalNativeComponentSpec> components) {
        components.all { component ->
            component.binaries.withType(NativeBinarySpec) { binary ->
                binary.sources.create(EXTERNAL_SOURCE, CppSourceSet)
            }
        }
    }

    private static <T extends NativeBinarySpec> List<T> withComponentType(ModelMap<T> binaries, Class<ComponentSpec> componentClass) {
        binaries.findAll { it.component in componentClass }
    }

    @Mutate
    void createExternalLibraryTasks(ModelMap<Task> tasks, @Path('binaries') ModelMap<NativeBinarySpec> binaries) {
        List<Task> buildTasks = []

        withComponentType(binaries, ExternalNativeComponentSpec).each { NativeBinarySpec binary ->
            ExternalNativeComponentSpec component = binary.component as ExternalNativeComponentSpec
            CppSourceSet externalSource = binary.sources.get(EXTERNAL_SOURCE) as CppSourceSet

            // Create a task for the external build
            String taskName = binary.tasks.taskName(EXTERNAL_BUILD_TASK)
            Task buildTask = null
            binary.tasks.create(taskName, component.buildTaskType) { buildTask = it }

            // Configure the task with the "buildConfig" block
            BuildConfigContext inputContext = new BuildConfigContext(binary, buildTask)
            component.buildConfig.execute(inputContext)

            // Reuse an existing task if a duplicate exists
            Task existingTask = buildTasks.find { it.equals(buildTask) }
            if (existingTask) {
                buildTask = existingTask
            } else {
                buildTasks << buildTask
            }

            // Set up dependencies for the task
            buildTask.dependsOn(binary.libs*.linkFiles)
            externalSource.builtBy(buildTask)

            // Evaluate the "buildOutput" block
            BuildOutputContext outputContext = new BuildOutputContext(binary)
            component.buildOutput.execute(outputContext)

            // Disable all normal compile tasks
            binary.tasks.withType(AbstractNativeCompileTask) {
                it.enabled = false
            }

            // Replace the create/link task with a simple copy
            binary.tasks.withType(ObjectFilesToBinary) { mainTask ->
                mainTask.deleteAllActions()

                mainTask.inputs.file outputContext.outputFile
                mainTask.doFirst {
                    mainTask.project.copy {
                        it.from outputContext.outputFile
                        it.into mainTask.outputFile.parentFile
                        it.rename { mainTask.outputFile.name }
                    }
                }
            }
        }
    }
}
