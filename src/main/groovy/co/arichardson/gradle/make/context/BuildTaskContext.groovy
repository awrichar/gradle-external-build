package co.arichardson.gradle.make.context

import org.gradle.nativeplatform.NativeBinarySpec

class BuildTaskContext extends NativeBinaryContext {
    String executable = "make"
    List<String> args = []
    Map<String, String> environment

    BuildTaskContext(NativeBinarySpec binary) {
        super(binary)
    }

    @Override
    boolean equals(Object other) {
        other in BuildTaskContext &&
            executable == other.executable &&
            args == other.args &&
            environment == other.environment
    }
}
