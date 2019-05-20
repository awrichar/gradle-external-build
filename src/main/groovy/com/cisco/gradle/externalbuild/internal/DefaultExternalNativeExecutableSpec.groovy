package com.cisco.gradle.externalbuild.internal

import com.cisco.gradle.externalbuild.ExternalNativeComponentSpec
import com.cisco.gradle.externalbuild.ExternalNativeExecutableSpec
import com.cisco.gradle.externalbuild.context.BuildConfigContext
import com.cisco.gradle.externalbuild.context.BuildOutputContext
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.internal.Actions
import org.gradle.nativeplatform.internal.DefaultNativeExecutableSpec

class DefaultExternalNativeExecutableSpec extends DefaultNativeExecutableSpec implements ExternalNativeExecutableSpec {
    private Class<Task> buildTaskType
    private List<Action<BuildConfigContext>> buildConfigActions = []
    private List<Action<BuildOutputContext>> buildOutputActions = []

    private verifyTaskTypeNotSet() {
        if (buildTaskType != null) {
            throw new GradleException("Task type is already set to ${buildTaskType}; cannot set it again!")
        }
    }

    @Override
    Action<BuildConfigContext> getBuildConfig() {
        return Actions.composite(buildConfigActions)
    }

    @Override
    void buildConfig(Action<BuildConfigContext> action) {
        buildConfigActions << action
    }

    @Override
    void buildConfig(Closure<Void> action) {
        buildConfig(new ClosureBackedAction<BuildConfigContext>(action))
    }

    @Override
    void buildConfig(Class<Task> actionType, Action<BuildConfigContext> action) {
        verifyTaskTypeNotSet()
        buildTaskType = actionType
        buildConfigActions << action
    }

    @Override
    void buildConfig(Class<Task> actionType, Closure<Void> action) {
        buildConfig(actionType, new ClosureBackedAction<BuildConfigContext>(action))
    }

    @Override
    void buildConfig(ExternalNativeComponentSpec component) {
        verifyTaskTypeNotSet()
        buildTaskType = component.buildTaskType
        buildConfigActions << component.buildConfig
    }

    @Override
    void buildConfig(ExternalNativeComponentSpec component, Action<BuildConfigContext> action) {
        verifyTaskTypeNotSet()
        buildTaskType = component.buildTaskType
        buildConfigActions << component.buildConfig << action
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
        return Actions.composite(buildOutputActions)
    }

    @Override
    void buildOutput(Action<BuildOutputContext> action) {
        buildOutputActions << action
    }

    @Override
    void buildOutput(Closure<Void> action) {
        buildOutput(new ClosureBackedAction<BuildOutputContext>(action))
    }
}
