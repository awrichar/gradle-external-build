package com.cisco.gradle.externalbuild

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

class ExternalBuildTest extends Specification {
    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
    @Shared File stubLibrary
    @Shared File stubExecutable
    File buildFile

    static final String pluginInit = """
        plugins {
            id 'com.cisco.external-build'
        }

        import com.cisco.gradle.externalbuild.ExternalNativeExecutableSpec
        import com.cisco.gradle.externalbuild.ExternalNativeLibrarySpec
        import com.cisco.gradle.externalbuild.ExternalNativeTestExecutableSpec
        import com.cisco.gradle.externalbuild.tasks.AutoMake
        import com.cisco.gradle.externalbuild.tasks.CMake
        import com.cisco.gradle.externalbuild.tasks.GnuMake
        import com.cisco.gradle.externalbuild.tasks.QMake
    """

    def setupSpec() {
        GradleRunner.create()
                .withProjectDir(new File('testStub'))
                .withArguments('build')
                .build()

        stubLibrary = firstFile('testStub/build/libs/stubLibrary/shared')
        stubExecutable = firstFile('testStub/build/exe/stubExecutable')
    }

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
    }

    File firstFile(String folderName) {
        return new File(folderName).listFiles().first()
    }

    BuildResult runBuild(boolean succeed=true) {
        GradleRunner runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir.root)
                .withArguments('build')

        return succeed ? runner.build() : runner.buildAndFail()
    }

    String outputText(String component, String buildType=null) {
        File outputFolder = new File(testProjectDir.root, "build/tmp/externalBuild${component.capitalize()}")
        if (buildType == null) {
            return new File(outputFolder, 'output.txt').text.trim()
        } else {
            return new File(outputFolder, "${buildType}-output.txt").text.trim()
        }
    }

    List<String> folderContents(File folder, String subfolder='') {
        List<String> filenames = new File(folder, subfolder).listFiles()*.name
        if (filenames != null) {
            Collections.sort(filenames)
        }
        return filenames
    }

    def "no output"() {
        given:
        buildFile << """
            $pluginInit

            model {
                components {
                    foo(ExternalNativeExecutableSpec) {
                        buildConfig(GnuMake) {
                            executable 'echo'
                        }
                    }
                }
            }
        """

        when:
        def result = runBuild()

        then:
        result.task(":build").outcome == UP_TO_DATE
        folderContents(testProjectDir.root, 'build/exe/foo') == null
    }

    def "basic make"() {
        given:
        buildFile << """
            $pluginInit

            model {
                components {
                    foo(ExternalNativeExecutableSpec) {
                        buildConfig(GnuMake) {
                            executable 'echo'
                            jobs 1
                            targets 'all', 'install'
                            args 'make-arg-1'
                        }

                        buildOutput {
                            outputFile = file('${stubExecutable.absolutePath}')
                        }
                    }
                }
            }
        """

        when:
        def result = runBuild()

        then:
        result.task(":build").outcome == SUCCESS
        outputText('fooExecutable', 'makeAll') == 'make-arg-1 -j 1 all'
        outputText('fooExecutable', 'makeInstall') == 'make-arg-1 -j 1 install'
        folderContents(testProjectDir.root, 'build/exe/foo').size() > 0
    }

    def "basic cmake"() {
        given:
        def cmakeFolder = testProjectDir.newFolder('cmake-folder')
        buildFile << """
            $pluginInit

            model {
                components {
                    foo(ExternalNativeExecutableSpec) {
                        buildConfig(CMake) {
                            executable 'echo'
                            jobs 1
                            targets 'all'
                            cmakeExecutable 'echo'
                            cmakeArgs 'cmake-arg-1'
                            cmakeRoot 'cmake-folder'
                        }

                        buildOutput {
                            outputFile = file('${stubExecutable.absolutePath}')
                        }
                    }
                }
            }
        """

        when:
        def result = runBuild()

        then:
        result.task(":build").outcome == SUCCESS
        outputText('fooExecutable', 'cmake') == "cmake-arg-1 ${cmakeFolder}"
        outputText('fooExecutable', 'makeAll') == '-j 1 all'
        folderContents(testProjectDir.root, 'build/exe/foo').size() > 0
    }

    def "basic qmake"() {
        given:
        def projectFile = testProjectDir.newFile('test.pro')
        buildFile << """
            $pluginInit

            model {
                components {
                    foo(ExternalNativeExecutableSpec) {
                        buildConfig(QMake) {
                            executable 'echo'
                            jobs 1
                            targets 'all'
                            qmakeExecutable 'echo'
                            qmakeArgs 'qmake-arg-1'
                            qmakeProject 'test.pro'
                        }

                        buildOutput {
                            outputFile = file('${stubExecutable.absolutePath}')
                        }
                    }
                }
            }
        """

        when:
        def result = runBuild()

        then:
        result.task(":build").outcome == SUCCESS
        outputText('fooExecutable', 'qmake') == "qmake-arg-1 ${projectFile}"
        outputText('fooExecutable', 'makeAll') == '-j 1 all'
        folderContents(testProjectDir.root, 'build/exe/foo').size() > 0
    }

    def "basic automake"() {
        given:
        def installPrefix = testProjectDir.newFolder('install')
        buildFile << """
            $pluginInit

            model {
                components {
                    foo(ExternalNativeExecutableSpec) {
                        buildConfig(AutoMake) {
                            executable 'echo'
                            jobs 1
                            targets 'all'
                            configureExecutable 'echo'
                            configureArgs 'configure-arg-1'
                            crossCompileHost 'test-host'
                            installPrefix '${installPrefix}'
                        }

                        buildOutput {
                            outputFile = file('${stubExecutable.absolutePath}')
                        }
                    }
                }
            }
        """

        when:
        def result = runBuild()

        then:
        result.task(":build").outcome == SUCCESS
        outputText('fooExecutable', 'configure') == "--host=test-host --prefix=${installPrefix} configure-arg-1"
        outputText('fooExecutable', 'makeAll') == '-j 1 all'
        folderContents(testProjectDir.root, 'build/exe/foo').size() > 0
    }

    def "two config blocks"() {
        given:
        buildFile << """
            $pluginInit

            model {
                components {
                    foo(ExternalNativeExecutableSpec) {
                        buildConfig(GnuMake) {
                            executable 'echo'
                            jobs 1
                            targets 'all'
                            args 'make-arg-1'
                        }

                        buildConfig {
                            args 'make-arg-2'
                        }

                        buildOutput {
                            outputFile = file('${stubExecutable.absolutePath}')
                        }
                    }
                }
            }
        """

        when:
        def result = runBuild()

        then:
        result.task(":build").outcome == SUCCESS
        outputText('fooExecutable', 'makeAll') == 'make-arg-1 make-arg-2 -j 1 all'
        folderContents(testProjectDir.root, 'build/exe/foo').size() > 0
    }

    def "cannot set task type twice"() {
        given:
        buildFile << """
            $pluginInit

            model {
                components {
                    foo(ExternalNativeExecutableSpec) {
                        buildConfig(GnuMake) {
                            executable 'echo'
                            jobs 1
                            targets 'all'
                            args 'make-arg-1'
                        }

                        buildConfig(CMake) {
                            args 'make-arg-2'
                        }

                        buildOutput {
                            outputFile = file('${stubExecutable.absolutePath}')
                        }
                    }
                }
            }
        """

        when:
        def result = runBuild(false)

        then:
        result.tasks == []
    }

    def "inherited config"() {
        given:
        buildFile << """
            $pluginInit

            model {
                components {
                    foo(ExternalNativeExecutableSpec) {
                        buildConfig(GnuMake) {
                            executable 'echo'
                            jobs 1
                            targets 'all'
                            args 'make-arg-1'
                        }

                        buildOutput {
                            outputFile = file('${stubExecutable.absolutePath}')
                        }
                    }

                    bar(ExternalNativeExecutableSpec) {
                        buildConfig(\$.components.foo) {
                            args 'make-arg-2'
                        }

                        buildOutput {
                            outputFile = file('${stubExecutable.absolutePath}')
                        }
                    }
                }
            }
        """

        when:
        def result = runBuild()

        then:
        result.task(":build").outcome == SUCCESS
        outputText('fooExecutable', 'makeAll') == 'make-arg-1 -j 1 all'
        outputText('barExecutable', 'makeAll') == 'make-arg-1 make-arg-2 -j 1 all'
        folderContents(testProjectDir.root, 'build/exe/foo').size() > 0
        folderContents(testProjectDir.root, 'build/exe/bar').size() > 0
    }

    def "external executable depends on Gradle library"() {
        given:
        buildFile << """
            $pluginInit

            model {
                components {
                    foo(ExternalNativeExecutableSpec) {
                        buildConfig(GnuMake) {
                            lib library: 'bar'

                            executable 'echo'
                            jobs 1
                            args requiredLibraries*.canonicalPath
                        }

                        buildOutput {
                            outputFile = file('${stubExecutable.absolutePath}')
                        }
                    }

                    bar(NativeLibrarySpec) {
                        sources {
                            cpp {
                                source {
                                    srcDir '.'
                                    include 'lib.cpp'
                                }
                            }
                        }
                    }
                }
            }
        """

        File srcFile = testProjectDir.newFile('lib.cpp')
        srcFile.text = "int bar() { return 0; }"

        when:
        def result = runBuild()
        File barLibrary = firstFile("${testProjectDir.root}/build/libs/bar/shared")

        then:
        result.task(":build").outcome == SUCCESS
        outputText('fooExecutable') == "${barLibrary.canonicalPath} -j 1"
        folderContents(testProjectDir.root, 'build/exe/foo').size() > 0
    }

    def "Gradle executable depends on external library"() {
        given:
        buildFile << """
            $pluginInit

            model {
                components {
                    foo(ExternalNativeLibrarySpec) {
                        buildConfig(GnuMake) {
                            executable 'echo'
                        }

                        buildOutput {
                            outputFile = file('${stubLibrary.absolutePath}')
                        }
                    }

                    bar(NativeExecutableSpec) {
                        sources {
                            cpp {
                                source {
                                    srcDir '.'
                                    include 'main.cpp'
                                }

                                lib library: 'foo'
                            }
                        }
                    }
                }
            }
        """

        File srcFile = testProjectDir.newFile('main.cpp')
        srcFile.text = "int main() { return 0; }"

        when:
        def result = runBuild()
        File fooLibrary = firstFile("${testProjectDir.root}/build/libs/foo/shared")
        File optionsFile = new File(testProjectDir.root, 'build/tmp/linkBarExecutable/options.txt')

        then:
        result.task(":build").outcome == SUCCESS
        optionsFile.text.contains(fooLibrary.canonicalPath)
        folderContents(testProjectDir.root, 'build/libs/foo/shared').size() > 0
    }

    def "Task de-duplication"() {
        given:
        buildFile << """
            $pluginInit

            model {
                components {
                    foo(ExternalNativeLibrarySpec) {
                        buildConfig(GnuMake) {
                            executable 'echo'
                            doFirst {
                                println "Running build"
                            }
                        }

                        buildOutput {
                            outputFile = file('${stubLibrary.absolutePath}')
                        }
                    }

                    bar(ExternalNativeLibrarySpec) {
                        buildConfig(\$.components.foo)
                        buildOutput {
                            outputFile = file('${stubLibrary.absolutePath}')
                        }
                    }
                }
            }
        """

        when:
        def result = runBuild()

        then:
        result.task(":build").outcome == SUCCESS
        result.task(":externalBuildFooSharedLibrary").outcome == SUCCESS
        result.task(":externalBuildFooStaticLibrary").outcome == SUCCESS
        result.task(":externalBuildBarSharedLibrary").outcome == SUCCESS
        result.task(":externalBuildBarStaticLibrary").outcome == SUCCESS
        result.output.count("Running build") == 1
    }

    def "build tests"() {
        given:
        buildFile << """
            $pluginInit

            model {
                components {
                    foo(ExternalNativeTestExecutableSpec) {
                        buildConfig(GnuMake) {
                            executable 'echo'
                            jobs 1
                            targets 'all', 'install'
                            args 'make-arg-1'
                        }

                        buildOutput {
                            outputFile = file('${stubExecutable.absolutePath}')
                        }
                    }
                }
            }
        """

        when:
        def result = runBuild()

        then:
        result.task(":build").outcome == SUCCESS
        result.task(":installFooExecutable").outcome == SUCCESS
        result.task(":runFooExecutable").outcome == SUCCESS
        outputText('fooExecutable', 'makeAll') == 'make-arg-1 -j 1 all'
        outputText('fooExecutable', 'makeInstall') == 'make-arg-1 -j 1 install'
        folderContents(testProjectDir.root, 'build/exe/foo').size() > 0
    }

    def "Cross-project dependencies"() {
        given:
        def project1Folder = testProjectDir.newFolder('project1')
        def project2Folder = testProjectDir.newFolder('project2')
        def settingsFile = testProjectDir.newFile('settings.gradle')
        settingsFile.text = "include(':project1', ':project2')"

        def project1BuildFile = new File(project1Folder, 'build.gradle')
        project1BuildFile << """
            $pluginInit

            model {
                components {
                    foo(ExternalNativeLibrarySpec) {
                        buildConfig(GnuMake) {
                            executable 'echo'
                        }

                        buildOutput {
                            outputFile = file('${stubLibrary.absolutePath}')
                        }
                    }

                    foo2(ExternalNativeLibrarySpec) {
                        buildConfig(\$.components.foo)
                        buildOutput {
                            outputFile = file('${stubLibrary.absolutePath}')
                        }
                    }
                }
            }
        """

        def project2BuildFile = new File(project2Folder, 'build.gradle')
        project2BuildFile << """
            $pluginInit

            model {
                components {
                    bar(NativeExecutableSpec) {
                        sources {
                            cpp {
                                source {
                                    srcDir '.'
                                    include 'main.cpp'
                                }

                                lib project: ':project1', library: 'foo2'
                            }
                        }
                    }
                }
            }
        """

        File srcFile = new File(project2Folder, 'main.cpp')
        srcFile.text = "int main() { return 0; }"

        when:
        def result = runBuild()
        File fooLibrary = firstFile("${project1Folder}/build/libs/foo2/shared")
        File optionsFile = new File(project2Folder, 'build/tmp/linkBarExecutable/options.txt')

        then:
        result.task(":project1:build").outcome == SUCCESS
        result.task(":project2:build").outcome == SUCCESS
        optionsFile.text.contains(fooLibrary.canonicalPath)
        folderContents(project1Folder, 'build/libs/foo/shared').size() > 0
        folderContents(project1Folder, 'build/libs/foo2/shared').size() > 0
    }
}
