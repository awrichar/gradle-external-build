# gradle-external-build
A Gradle plugin for seamlessly integrating with other build tools, including GNU make, CMake, qmake, and more.

## Example Usage

    apply plugin: 'com.cisco.external-build'
    import com.cisco.gradle.externalbuild.ExternalNativeLibrarySpec
    import com.cisco.gradle.externalbuild.GnuMake

    def getOutputDir = { file("${buildDir}/external-foo/${it.targetPlatform.name}") }

    model {
        components {
            foo(ExternalNativeLibrarySpec) {
                sources.c {
                    lib project: ':bar', library: 'bar'
                }
                
                buildConfig(GnuMake) {
                    targets 'all', 'install'
                    environment = [
                        'PATH': toolChainPath,
                        'OUTPUT_DIR': getOutputDir(it),
                        'EXTERNAL_LIBRARIES': requiredLibraries.join(' '),
                        'EXTERNAL_INCLUDES': requiredIncludes.join(' '),
                    ]
                }

                buildOutput {
                    def outputDir = getOutputDir(it)
                    outputFile = file("${outputDir}/libs/libfoo.so")

                    exportedHeaders {
                        srcDir "${outputDir}/include"
                    }
                }
            }
        }
    }
    
## Overview

TODO
