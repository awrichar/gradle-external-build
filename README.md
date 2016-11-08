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

This plugin includes 2 component types:

* **ExternalNativeLibrarySpec** - derived from NativeLibrarySpec, for shared and/or static libraries built by an external tool
* **ExternalNativeExecutableSpec** - derived from NativeExecutableSpec, for executables built by an external tool

It includes 5 task types:

* **GnuMake** - for invoking makefiles
* **AutoMake** - for invoking automake projects that use a configure script and a makefile
* **CMake** - for invoking CMake projects
* **QMake** - for invoking qmake projects
* **OutputRedirectingExec** - a variant of Exec that redirects output to a file (the base class for all the above tasks; generally not intended for direct use)

To trigger an external build, define a component using one of the component types:

    import com.cisco.gradle.externalbuild.ExternalNativeLibrarySpec

    model {
        components {
            foo(ExternalNativeLibrarySpec) {
                buildConfig {
                    ...
                }

                buildOutput {
                    ...
                }
            }
        }
    }

## Syntax

The components each have two methods to configure the external build - **buildConfig**
specifies the configuration for invoking the external build tool, and **buildOutput**
specifies the output files from the build to expose to Gradle.

### buildConfig

    void buildConfig(Closure action)
    void buildConfig(Class<Task> taskType, Closure action)
    void buildConfig(ExternalNativeComponentSpec component)
    void buildConfig(ExternalNativeComponentSpec component, Closure action)

For each binary variant of the component, the plugin will generate a new task of the
type specified, and configure it using the given action. If no task type is given, the
default is **OutputRedirectingExec**. If another component is given, this component will
inherit the task type and action from the given component. If a component and action are
given, both the original component's action and the new action will be used to configure
the task.

The delegate and first parameter given to the closure will be an instance of
**BuildConfigContext**. This class exposes properties and methods to help configure the
build based on the binary variant (detailed below). In addition, it will delegate to the
Task being configured for any unknown properties or methods, meaning that you can invoke
any method or property exposed by your Task type as if it were present on
**BuildConfigContext**.

### buildOutput

    void buildOutput(Closure action)

For each binary variant of the component, the plugin will retrieve the file specified by
the closure to serve as the binary's output.

The delegate and first parameter given to the closure will be an instance of
**BuildOutputContext**. This class exposes properties and methods to help Gradle find
the binary outputs of the external build (detailed below).

## API Reference

TODO