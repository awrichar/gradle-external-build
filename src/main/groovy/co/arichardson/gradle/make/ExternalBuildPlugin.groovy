package co.arichardson.gradle.make

import co.arichardson.gradle.make.context.BuildConfigContext
import co.arichardson.gradle.make.context.BuildOutputContext
import co.arichardson.gradle.make.internal.DefaultExternalNativeExecutableSpec
import co.arichardson.gradle.make.internal.DefaultExternalNativeLibrarySpec
import co.arichardson.gradle.make.internal.DefaultExternalNativeTestExecutableSpec
import org.gradle.api.Task
import org.gradle.api.tasks.StopExecutionException
import org.gradle.language.cpp.CppSourceSet
import org.gradle.language.nativeplatform.tasks.AbstractNativeCompileTask
import org.gradle.model.Finalize
import org.gradle.model.ModelMap
import org.gradle.model.Mutate
import org.gradle.model.Path
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.NativeExecutableBinarySpec
import org.gradle.nativeplatform.tasks.InstallExecutable
import org.gradle.nativeplatform.tasks.ObjectFilesToBinary
import org.gradle.nativeplatform.test.tasks.RunTestExecutable
import org.gradle.platform.base.ComponentSpec
import org.gradle.platform.base.ComponentType
import org.gradle.platform.base.TypeBuilder
import org.gradle.platform.base.internal.BinaryNamingScheme

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

    @ComponentType
    void registerExternalTestExecutableType(TypeBuilder<ExternalNativeTestExecutableSpec> builder) {
        builder.defaultImplementation(DefaultExternalNativeTestExecutableSpec)
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
            tasks.create(taskName, component.buildTaskType)
            Task buildTask = tasks.get(taskName)

            // Configure the task with the "buildConfig" block
            BuildConfigContext inputContext = new BuildConfigContext(binary, buildTask)
            Utils.invokeWithContext(component.buildConfig, inputContext)

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
            Utils.invokeWithContext(component.buildOutput, outputContext)

            // Disable all normal compile tasks
            binary.tasks.withType(AbstractNativeCompileTask) {
                it.enabled = false
            }

            // Replace the create/link task with a simple copy
            binary.tasks.withType(ObjectFilesToBinary) { mainTask ->
                mainTask.inputs.file outputContext.outputFile
                mainTask.doFirst {
                    mainTask.project.copy {
                        it.from outputContext.outputFile
                        it.into mainTask.outputFile.parentFile
                        it.rename { mainTask.outputFile.name }
                    }

                    throw new StopExecutionException()
                }
            }
        }
    }

    @Finalize
    void createTestRunTask(@Path('binaries') ModelMap<NativeExecutableBinarySpec> binaries) {
        withComponentType(binaries, ExternalNativeTestExecutableSpec).each { NativeExecutableBinarySpec binary ->
            binary.tasks.create(binary.tasks.taskName('run'), RunTestExecutable, {})
        }
    }

    @Finalize
    void configureTestRunTask(ModelMap<Task> tasks, @Path('binaries') ModelMap<NativeExecutableBinarySpec> binaries) {
        withComponentType(binaries, ExternalNativeTestExecutableSpec).each { NativeExecutableBinarySpec binary ->
            BinaryNamingScheme namingScheme = binary.getNamingScheme()
            InstallExecutable installTask = binary.tasks.install as InstallExecutable
            RunTestExecutable runTask = getTestRunTask(binary)

            runTask.inputs.files(installTask.outputs.files)
            runTask.executable = installTask.runScript
            runTask.outputDir = namingScheme.getOutputDirectory(runTask.project.buildDir, 'test-results')
        }
    }

    @Mutate
    void attachTestTaskToCheckLifecycle(@Path('tasks.check') Task checkTask, @Path('binaries') ModelMap<NativeExecutableBinarySpec> binaries) {
        withComponentType(binaries, ExternalNativeTestExecutableSpec).each { NativeExecutableBinarySpec binary ->
            if (binary.buildable) {
                checkTask.dependsOn(getTestRunTask(binary))
            }
        }
    }

    private static RunTestExecutable getTestRunTask(NativeExecutableBinarySpec binary) {
        binary.tasks.find { it in RunTestExecutable } as RunTestExecutable
    }
}
