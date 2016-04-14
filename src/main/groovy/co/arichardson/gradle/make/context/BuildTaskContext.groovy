package co.arichardson.gradle.make.context

import org.gradle.nativeplatform.NativeBinarySpec

class BuildTaskContext extends NativeBinaryContext {
    File outputDir

    String executable = "make"
    List<String> args = []
    Map<String, String> environment

    BuildTaskContext(NativeBinarySpec binary) {
        super(binary)
    }

    @Override
    boolean equals(Object other) {
        other in BuildTaskContext &&
            outputDir == other.outputDir &&
            executable == other.executable &&
            args == other.args &&
            environment == other.environment
    }
}
