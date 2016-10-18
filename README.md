# gradle-external-build
A Gradle plugin for seamlessly integrating with other build tools, including GNU make, CMake, qmake, and more.

## Example Usage

    apply plugin: 'co.arichardson.external-build'
    import co.arichardson.gradle.make.ExternalNativeLibrarySpec
    import co.arichardson.gradle.make.tasks.GnuMake

    def getOutputDir = { file("${buildDir}/external-foo/${it.targetPlatform.name}") }

    model {
        components {
            foo(ExternalNativeLibrarySpec) {
                sources.c {
                    lib project: ':bar', library: 'bar'
                }
                
                buildConfig(GnuMake) {
                    environment = [
                        'PATH': [files(toolChain.path).asPath, System.env.PATH].join(File.pathSeparator),
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
