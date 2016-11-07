package com.cisco.gradle.externalbuild.internal

import com.cisco.gradle.externalbuild.ExternalNativeComponentSpec
import com.cisco.gradle.externalbuild.ExternalNativeLibrarySpec
import com.cisco.gradle.externalbuild.context.BuildConfigContext
import com.cisco.gradle.externalbuild.context.BuildOutputContext
import com.cisco.gradle.externalbuild.tasks.OutputRedirectingExec
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.internal.Actions
import org.gradle.nativeplatform.internal.DefaultNativeLibrarySpec

class DefaultExternalNativeLibrarySpec extends DefaultNativeLibrarySpec implements ExternalNativeLibrarySpec {
    private Class<Task> buildTaskType = OutputRedirectingExec
    private Action<BuildConfigContext> buildConfigAction = {}
    private Action<BuildOutputContext> buildOutputAction = {}

    @Override
    Action<BuildConfigContext> getBuildConfig() {
        return buildConfigAction
    }

    @Override
    void buildConfig(Action<BuildConfigContext> action) {
        buildConfigAction = action
    }

    @Override
    void buildConfig(Closure<Void> action) {
        buildConfig(new ClosureBackedAction<BuildConfigContext>(action))
    }

    @Override
    void buildConfig(Class<Task> actionType, Action<BuildConfigContext> action) {
        buildTaskType = actionType
        buildConfigAction = action
    }

    @Override
    void buildConfig(Class<Task> actionType, Closure<Void> action) {
        buildConfig(actionType, new ClosureBackedAction<BuildConfigContext>(action))
    }

    @Override
    void buildConfig(ExternalNativeComponentSpec component) {
        buildTaskType = component.buildTaskType
        buildConfigAction = component.buildConfig
    }

    @Override
    void buildConfig(ExternalNativeComponentSpec component, Action<BuildConfigContext> action) {
        buildTaskType = component.buildTaskType
        buildConfigAction = Actions.composite(component.buildConfig, action)
    }

    @Override
    void buildConfig(ExternalNativeComponentSpec component, Closure<Void> action) {
        buildConfig(component, new ClosureBackedAction<BuildConfigContext>(action))
    }

    @Override
    Class<Task> getBuildTaskType() {
        return buildTaskType
    }

    @Override
    Action<BuildOutputContext> getBuildOutput() {
        return buildOutputAction
    }

    @Override
    void buildOutput(Action<BuildOutputContext> action) {
        buildOutputAction = action
    }

    @Override
    void buildOutput(Closure<Void> action) {
        buildOutput(new ClosureBackedAction<BuildOutputContext>(action))
    }
}
