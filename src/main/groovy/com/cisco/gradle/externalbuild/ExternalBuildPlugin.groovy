package com.cisco.gradle.externalbuild

import com.cisco.gradle.externalbuild.context.BuildConfigContext
import com.cisco.gradle.externalbuild.context.BuildOutputContext
import com.cisco.gradle.externalbuild.internal.DefaultExternalNativeExecutableSpec
import com.cisco.gradle.externalbuild.internal.DefaultExternalNativeLibrarySpec
import com.cisco.gradle.externalbuild.internal.DefaultExternalNativeTestExecutableSpec
import com.cisco.gradle.externalbuild.tasks.MultiOutputStream
import org.gradle.api.Task
import org.gradle.language.cpp.CppSourceSet
import org.gradle.language.nativeplatform.tasks.AbstractNativeCompileTask
import org.gradle.model.Defaults
import org.gradle.model.Each
import org.gradle.model.Finalize
import org.gradle.model.Model
import org.gradle.model.ModelMap
import org.gradle.model.Path
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.tasks.InstallExecutable
import org.gradle.nativeplatform.tasks.ObjectFilesToBinary
import org.gradle.nativeplatform.test.tasks.RunTestExecutable
import org.gradle.platform.base.BinaryTasks
import org.gradle.platform.base.ComponentType
import org.gradle.platform.base.TypeBuilder

class ExternalBuildPlugin extends RuleSource {
    public static final String EXTERNAL_SOURCE = 'externalSource'
    public static final String EXTERNAL_BUILD_TASK = 'externalBuild'

    public static final String TEST_RUN_TASK = 'run'
    public static final String TEST_RESULTS_DIR = 'test-results'
    public static final String TEST_RESULTS_FILE = 'output.txt'

    static class ExternalBuildSpec {
        private Map<Task, NativeBinarySpec> buildTasks = [:]
        private Map<Task, NativeBinarySpec> runTasks = [:]
    }

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
    void addExternalSourceSet(@Each ExternalNativeComponentSpec component) {
        component.binaries.withType(NativeBinarySpec) { binary ->
            binary.sources.create(EXTERNAL_SOURCE, CppSourceSet)
        }
    }

    @BinaryTasks
    void createExternalBuildTasks(ModelMap<Task> tasks, NativeBinarySpec binary, ExternalBuildSpec build) {
        if (!(binary.component in ExternalNativeComponentSpec)) {
            return
        }

        ExternalNativeComponentSpec component = binary.component as ExternalNativeComponentSpec
        CppSourceSet externalSource = binary.sources.get(EXTERNAL_SOURCE) as CppSourceSet

        // Create the external build task
        tasks.create(binary.tasks.taskName(EXTERNAL_BUILD_TASK), component.buildTaskType) { Task buildTask ->
            build.buildTasks[buildTask] = binary
            externalSource.builtBy(buildTask)
        }

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
            mainTask.inputs.file(outputContext.outputFile)
            mainTask.doFirst {
                mainTask.project.copy {
                    it.from outputContext.outputFile
                    it.into mainTask.outputFile.parentFile
                    it.rename { mainTask.outputFile.name }
                    it.fileMode 0755
                }
            }
        }
    }

    @Finalize
    void configureExternalBuildTask(@Each Task task, ExternalBuildSpec build) {
        if (!(task in build.buildTasks)) {
            return
        }

        NativeBinarySpec binary = build.buildTasks[task]
        ExternalNativeComponentSpec component = binary.component as ExternalNativeComponentSpec

        // Evaluate the "buildConfig" block
        BuildConfigContext inputContext = new BuildConfigContext(binary, task)
        component.buildConfig.execute(inputContext)

        // Set library dependencies
        task.dependsOn(binary.libs*.linkFiles)

        // Check for a duplicate task, and reuse it if possible
        Task duplicateTask = build.buildTasks.find { Task otherTask, NativeBinarySpec otherBinary ->
            binary != otherBinary && task.equals(otherTask) && otherTask.enabled
        }?.key
        if (duplicateTask) {
            task.dependsOn(duplicateTask)
            task.enabled = false
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
            build.runTasks[runTask] = binary
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
        if (!(runTask in build.runTasks)) {
            return
        }

        NativeBinarySpec binary = build.runTasks[runTask]

        // Set dependencies between the install task and the run task
        binary.tasks.withType(InstallExecutable) { InstallExecutable installTask ->
            runTask.dependsOn(installTask)
            runTask.inputs.files(installTask)
            runTask.executable = installTask.runScript
        }
    }
}
