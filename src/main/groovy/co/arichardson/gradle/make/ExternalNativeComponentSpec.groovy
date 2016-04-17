package co.arichardson.gradle.make

import org.gradle.api.Task
import org.gradle.nativeplatform.NativeComponentSpec

interface ExternalNativeComponentSpec extends NativeComponentSpec {
    Closure<Void> getBuildConfig()
    void buildConfig(Closure<Void> action)
    void buildConfig(Class<Task> actionType, Closure<Void> action)

    Class<Task> getBuildTaskType()

    Closure<Void> getBuildOutput()
    void buildOutput(Closure<Void> action)
}
