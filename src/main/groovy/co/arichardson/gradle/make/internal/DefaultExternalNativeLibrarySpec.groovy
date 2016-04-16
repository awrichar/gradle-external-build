package co.arichardson.gradle.make.internal

import co.arichardson.gradle.make.ExternalNativeLibrarySpec
import co.arichardson.gradle.make.tasks.OutputRedirectingExec
import org.gradle.api.Task
import org.gradle.nativeplatform.internal.DefaultNativeLibrarySpec

class DefaultExternalNativeLibrarySpec extends DefaultNativeLibrarySpec implements ExternalNativeLibrarySpec {
    private Class<Task> buildTaskType = OutputRedirectingExec
    private Closure<Void> buildInputAction = {}
    private Closure<Void> buildOutputAction = {}

    @Override
    Closure<Void> getBuildInput() {
        return buildInputAction
    }

    @Override
    void buildInput(Closure<Void> action) {
        buildInputAction = action
    }

    @Override
    void buildInput(Class<Task> actionType, Closure<Void> action) {
        buildTaskType = actionType
        buildInputAction = action
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
