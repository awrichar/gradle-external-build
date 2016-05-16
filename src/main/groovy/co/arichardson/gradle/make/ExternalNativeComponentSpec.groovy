package co.arichardson.gradle.make

import co.arichardson.gradle.make.context.BuildConfigContext
import co.arichardson.gradle.make.context.BuildOutputContext
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.nativeplatform.NativeComponentSpec

interface ExternalNativeComponentSpec extends NativeComponentSpec {
    Action<BuildConfigContext> getBuildConfig()

    void buildConfig(Action<BuildConfigContext> action)
    void buildConfig(Closure<Void> action)
    void buildConfig(Class<Task> actionType, Action<BuildConfigContext> action)
    void buildConfig(Class<Task> actionType, Closure<Void> action)

    void buildConfig(ExternalNativeComponentSpec component)
    void buildConfig(ExternalNativeComponentSpec component, Action<BuildConfigContext> action)
    void buildConfig(ExternalNativeComponentSpec component, Closure<Void> action)

    Class<Task> getBuildTaskType()

    Action<BuildOutputContext> getBuildOutput()
    void buildOutput(Action<BuildOutputContext> action)
    void buildOutput(Closure<Void> action)
}
