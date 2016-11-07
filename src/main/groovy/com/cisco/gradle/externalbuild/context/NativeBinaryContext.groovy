package com.cisco.gradle.externalbuild.context

import org.gradle.nativeplatform.BuildType
import org.gradle.nativeplatform.Flavor
import org.gradle.nativeplatform.NativeBinarySpec
import org.gradle.nativeplatform.platform.NativePlatform
import org.gradle.nativeplatform.toolchain.NativeToolChain

class NativeBinaryContext {
    final NativeBinarySpec binary
    final NativePlatform targetPlatform
    final NativeToolChain toolChain
    final BuildType buildType
    final Flavor flavor

    NativeBinaryContext(NativeBinarySpec binary) {
        this.binary = binary
        targetPlatform = binary.targetPlatform
        toolChain = binary.toolChain
        buildType = binary.buildType
        flavor = binary.flavor
    }
}
