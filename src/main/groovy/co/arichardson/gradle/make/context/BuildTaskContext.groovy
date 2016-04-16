package co.arichardson.gradle.make.context

import org.gradle.api.Task
import org.gradle.nativeplatform.NativeBinarySpec

class BuildTaskContext extends NativeBinaryContext {
    final List<File> linkedLibraries
    final Task buildTask

    BuildTaskContext(NativeBinarySpec binary, Task task) {
        super(binary)

        linkedLibraries = []
        binary.libs*.linkFiles.each {
            linkedLibraries.addAll(it.files)
        }

        buildTask = task
    }

    @Override
    boolean equals(Object other) {
        other in BuildTaskContext && buildTask == other.buildTask
    }

    Object methodMissing(String name, args) {
        buildTask."$name"(*args)
    }

    Object propertyMissing(String name) {
        return buildTask."$name"
    }

    void propertyMissing(String name, value) {
        buildTask."$name" = value
    }
}
