package com.cisco.gradle.externalbuild

import com.cisco.gradle.externalbuild.context.BuildConfigContext
import com.cisco.gradle.externalbuild.context.BuildOutputContext
import com.cisco.gradle.externalbuild.internal.DefaultExternalNativeExecutableSpec
import com.cisco.gradle.externalbuild.internal.DefaultExternalNativeLibrarySpec
import com.cisco.gradle.externalbuild.internal.DefaultExternalNativeTestExecutableSpec
import com.cisco.gradle.externalbuild.tasks.MultiOutputStream
import com.cisco.gradle.externalbuild.tasks.OutputRedirectingExec
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.StopExecutionException
import org.gradle.language.cpp.CppSourceSet
import org.gradle.language.nativeplatform.tasks.AbstractNativeCompileTask
import org.gradle.model.Defaults
import org.gradle.model.Each
import org.gradle.model.Finalize
import org.gradle.model.Model
import org.gradle.model.ModelMap
import org.gradle.model.Mutate
import org.gradle.model.Path
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.NativeExecutableBinarySpec
import org.gradle.nativeplatform.SharedLibraryBinarySpec
import org.gradle.nativeplatform.StaticLibraryBinarySpec
import org.gradle.nativeplatform.tasks.InstallExecutable
import org.gradle.nativeplatform.tasks.ObjectFilesToBinary
import org.gradle.nativeplatform.test.tasks.RunTestExecutable
import org.gradle.platform.base.BinaryTasks
import org.gradle.platform.base.ComponentType
import org.gradle.platform.base.TypeBuilder

class ExternalBuildPlugin implements Plugin<Project> {
    public static final String EXTERNAL_SOURCE = 'externalSource'
    public static final String EXTERNAL_BUILD_TASK = 'externalBuild'

    public static final String TEST_RUN_TASK = 'run'
    public static final String TEST_RESULTS_DIR = 'test-results'
    public static final String TEST_RESULTS_FILE = 'output.txt'

    @Override
    void apply(final Project project) {
        project.getPluginManager().apply('cpp')
    }

    static class ExternalBuildSpec {
        private Map<NativeBinarySpec, Task> buildTasks = [:]
        private Map<NativeBinarySpec, Task> runTasks = [:]
        private Map<NativeBinarySpec, File> outputFiles = [:]

        private NativeBinarySpec binaryForBuildTask(Task task) {
            return buildTasks.find{ it.value.name == task.name }?.key
        }

        private NativeBinarySpec binaryForRunTask(Task task) {
            return runTasks.find{ it.value.name == task.name }?.key
        }
    }

    static class Rules extends RuleSource {
        @Model
        ExternalBuildSpec externalBuild() {
            return new ExternalBuildSpec()
        }

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

        @Defaults
        void createExternalSourceSet(@Each NativeBinarySpec binary, ExternalBuildSpec build) {
            if (!(binary.component in ExternalNativeComponentSpec)) {
                return
            }

            binary.sources.create(EXTERNAL_SOURCE, CppSourceSet)
        }

        @Defaults
        void createExternalBuildTask(@Each NativeBinarySpec binary, ExternalBuildSpec build) {
            if (!(binary.component in ExternalNativeComponentSpec)) {
                return
            }

            ExternalNativeComponentSpec component = binary.component as ExternalNativeComponentSpec
            Class<Task> taskType = component.buildTaskType ?: OutputRedirectingExec
            String taskName = binary.tasks.taskName(EXTERNAL_BUILD_TASK)

            binary.tasks.create(taskName, taskType) { Task buildTask ->
                buildTask.logger.info("Created ${buildTask} to build ${binary}")
                build.buildTasks[binary] = buildTask
            }
        }

        @Mutate
        void attachExternalBuildTask(@Each NativeBinarySpec binary, ExternalBuildSpec build) {
            if (!(binary.component in ExternalNativeComponentSpec)) {
                return
            }

            ExternalNativeComponentSpec component = binary.component as ExternalNativeComponentSpec
            CppSourceSet externalSource = binary.sources.get(EXTERNAL_SOURCE) as CppSourceSet
            externalSource.builtBy(build.buildTasks[binary])

            // Evaluate the "buildOutput" block
            BuildOutputContext outputContext = new BuildOutputContext(binary)
            component.buildOutput.execute(outputContext)
            build.outputFiles[binary] = outputContext.outputFile

            // Preserve the name of the selected output file
            if (outputContext.outputFile) {
                renameOutputFile(binary, outputContext.outputFile.name)
            }
        }

        @Mutate
        void configureExternalBuildTask(@Each Task task,  ExternalBuildSpec build) {
            NativeBinarySpec binary = build.binaryForBuildTask(task)
            if (!binary) return

            ExternalNativeComponentSpec component = binary.component as ExternalNativeComponentSpec

            // Evaluate the "buildConfig" block
            BuildConfigContext inputContext = new BuildConfigContext(binary, task)
            component.buildConfig.execute(inputContext)

            // Set dependencies from "lib" entries
            task.dependsOn(binary.libs*.linkFiles)

            // Short-circuit the task if any duplicate tasks have already run
            task.doFirst {
                Task duplicate = findDuplicateTasks(task, build).find{ it.state.executed }
                if (duplicate) {
                    throw new StopExecutionException("Not running ${task}, because ${duplicate} has already run.")
                }
            }
        }

        @Mutate
        void disableCompileTask(@Each AbstractNativeCompileTask task, ExternalBuildSpec build) {
            NativeBinarySpec binary = build.buildTasks.keySet().find { task in it.tasks }
            if (!binary) return
            task.enabled = false
        }

        @Mutate
        void configureLinkTask(@Each ObjectFilesToBinary task, ExternalBuildSpec build) {
            NativeBinarySpec binary = build.buildTasks.keySet().find { task in it.tasks }
            if (!binary) return

            ExternalNativeComponentSpec component = binary.component as ExternalNativeComponentSpec
            File outputFile = build.outputFiles[binary]

            // Replace the normal create/link actions with a simple copy
            task.actions = []
            if (outputFile) {
                task.source(outputFile)
                task.doFirst {
                    task.project.copy {
                        it.from outputFile
                        it.into getOutputFile(task).parentFile
                        it.fileMode 0755
                    }
                }
            }
        }

        @BinaryTasks
        void createTestRunTasks(ModelMap<Task> tasks, NativeBinarySpec binary, ExternalBuildSpec build,
                                @Path('buildDir') File buildDir, @Path('tasks.check') Task checkTask) {

            if (!(binary.component in ExternalNativeTestExecutableSpec)) {
                return
            }

            String runTaskName = binary.tasks.taskName(TEST_RUN_TASK)
            File resultsDir = binary.namingScheme.getOutputDirectory(buildDir, TEST_RESULTS_DIR)
            File resultsFile = new File(resultsDir, TEST_RESULTS_FILE)

            // Create the run task
            binary.tasks.create(runTaskName, RunTestExecutable) { RunTestExecutable runTask ->
                build.runTasks[binary] = runTask
                checkTask.dependsOn(runTask)

                runTask.outputDir = resultsDir
                runTask.doFirst {
                    resultsDir.mkdirs()
                    runTask.standardOutput = new MultiOutputStream(System.out, resultsFile.newOutputStream())
                }
            }
        }

        @Finalize
        void configureTestRunTask(@Each RunTestExecutable runTask, ExternalBuildSpec build) {
            NativeBinarySpec binary = build.binaryForRunTask(runTask)
            if (!binary) return

            // Set dependencies between the install task and the run task
            binary.tasks.withType(InstallExecutable) { InstallExecutable installTask ->
                runTask.dependsOn(installTask)
                runTask.inputs.files(installTask)
                runTask.executable(getRunScript(installTask))
            }
        }

        private static List<Task> findDuplicateTasks(Task task, ExternalBuildSpec build) {
            return build.buildTasks.findAll { NativeBinarySpec otherBinary, Task otherTask ->
                task.equals(otherTask) && task.name != otherTask.name
            }*.value
        }

        private static File getOutputFile(ObjectFilesToBinary task) {
            if (task.hasProperty('outputFile') && task.outputFile in File) {
                // for backwards compatibility
                return task.outputFile
            } else if (task.hasProperty('outputFile')) {
                return task.outputFile.asFile.get()
            } else {
                return task.linkedFile.asFile.get()
            }
        }

        private static File getRunScript(InstallExecutable task) {
            if (task.hasProperty('runScript')) {
                // for backwards compatibility
                return task.runScript
            } else {
                return task.runScriptFile.get().asFile
            }
        }

        private static void renameOutputFile(NativeBinarySpec binarySpec, String name) {
            if (binarySpec in SharedLibraryBinarySpec) {
                File folder = binarySpec.sharedLibraryFile.parentFile
                binarySpec.sharedLibraryFile = new File(folder, name)
                // TODO: Windows might need to treat this differently
                binarySpec.sharedLibraryLinkFile = binarySpec.sharedLibraryFile
            } else if (binarySpec in StaticLibraryBinarySpec) {
                File folder = binarySpec.staticLibraryFile.parentFile
                binarySpec.staticLibraryFile = new File(folder, name)
            } else if (binarySpec in NativeExecutableBinarySpec) {
                File folder = binarySpec.executable.file.parentFile
                binarySpec.executable.file = new File(folder, name)
            } else {
                throw new IllegalArgumentException("Unsupported binary type: ${binarySpec.class.name}")
            }
        }
    }
}
