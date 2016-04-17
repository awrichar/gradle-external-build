package co.arichardson.gradle.make.internal

import co.arichardson.gradle.make.ExternalNativeExecutableSpec
import co.arichardson.gradle.make.tasks.OutputRedirectingExec
import org.gradle.api.Task
import org.gradle.nativeplatform.internal.DefaultNativeExecutableSpec

class DefaultExternalNativeExecutableSpec extends DefaultNativeExecutableSpec implements ExternalNativeExecutableSpec {
    private Class<Task> buildTaskType = OutputRedirectingExec
    private Closure<Void> buildConfigAction = {}
    private Closure<Void> buildOutputAction = {}

    @Override
    Closure<Void> getBuildConfig() {
        return buildConfigAction
    }

    @Override
    void buildConfig(Closure<Void> action) {
        buildConfigAction = action
    }

    @Override
    void buildConfig(Class<Task> actionType, Closure<Void> action) {
        buildTaskType = actionType
        buildConfigAction = action
    }

    @Override
    Class<Task> getBuildTaskType() {
        return buildTaskType
    }

    @Override
    Closure<Void> getBuildOutput() {
        return buildOutputAction
    }

    @Override
    void buildOutput(Closure<Void> action) {
        buildOutputAction = action
    }
}
