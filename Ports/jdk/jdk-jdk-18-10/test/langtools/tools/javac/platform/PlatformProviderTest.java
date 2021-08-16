/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @bug 8072480
 * @summary Ensure all methods of PlatformProvider are called correctly, and their result is used
 *          correctly.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.platform
 *          jdk.compiler/com.sun.tools.javac.util:+open
 * @build toolbox.ToolBox PlatformProviderTest
 * @run main/othervm PlatformProviderTest
 */

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

// import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.tools.javac.platform.PlatformDescription;
import com.sun.tools.javac.platform.PlatformProvider;
import com.sun.tools.javac.util.Log;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class PlatformProviderTest implements PlatformProvider {

    public static void main(String... args) throws IOException {
        new PlatformProviderTest().run();
    }

    void run() throws IOException {
        Path registration = Paths.get(System.getProperty("test.classes"),
                                      "META-INF",
                                      "services",
                                      "com.sun.tools.javac.platform.PlatformProvider");
        Files.createDirectories(registration.getParent());
        try (Writer metaInf = Files.newBufferedWriter(registration)) {
            metaInf.write(PlatformProviderTest.class.getName());
        }

        doTest("name", "");
        doTest("name:param", "param");
        doTestFailure();
    }

    void doTest(String platformSpec, String expectedParameter) {
        ToolBox tb = new ToolBox();
        Task.Result result =
                new JavacTask(tb, Task.Mode.EXEC)
                  .outdir(".")
                  .options("-J--class-path=" + System.getProperty("test.classes"),
                           "-J--add-exports=jdk.compiler/com.sun.tools.javac.platform=ALL-UNNAMED",
                           "-J--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                           "-XDrawDiagnostics",
                           "--release",
                           platformSpec,
                           System.getProperty("test.src") + "/PlatformProviderTestSource.java")
                  .run();

        List<String> expectedOutput =
                Arrays.asList("getSupportedPlatformNames",
                              "getPlatform(name, " + expectedParameter + ")",
                              "getSourceVersion",
                              "getTargetVersion",
                              "getFileManager",
                              "testPlugin: [testPluginKey=testPluginValue]",
                              "process: {testAPKey=testAPValue}",
                              "process: {testAPKey=testAPValue}",
                              "PlatformProviderTestSource.java:4:49: compiler.warn.raw.class.use: java.util.ArrayList, java.util.ArrayList<E>",
                              "compiler.misc.count.warn",
                              "close");
        List<String> actualOutput = result.getOutputLines(Task.OutputKind.STDERR);
        actualOutput = actualOutput.stream().filter(s->!s.matches("^Picked up .*JAVA.*OPTIONS:.*")).collect(Collectors.toList());
        if (!expectedOutput.equals(actualOutput)) {
            throw new AssertionError(  "Expected output: " + expectedOutput +
                                     "; actual output: " + actualOutput);
        }
        result.writeAll();
    }

    void doTestFailure() {
        ToolBox tb = new ToolBox();
        Task.Result result =
                new JavacTask(tb, Task.Mode.EXEC)
                  .outdir(".")
                  .options("-J--class-path=" + System.getProperty("test.classes"),
                           "-J--add-exports=jdk.compiler/com.sun.tools.javac.platform=ALL-UNNAMED",
                           "-J--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                           "--release",
                           "fail",
                           System.getProperty("test.src") + "/PlatformProviderTestSource.java")
                  .run(Task.Expect.FAIL);

        List<String> expectedOutput =
                Arrays.asList("getSupportedPlatformNames",
                              "getPlatform(fail, )",
                              "error: release version fail not supported",
                              "javac.msg.usage");
        List<String> actualOutput = result.getOutputLines(Task.OutputKind.STDERR);
        actualOutput = actualOutput.stream().filter(s->!s.matches("^Picked up .*JAVA.*OPTIONS:.*")).collect(Collectors.toList());
        tb.checkEqual(expectedOutput, actualOutput);
    }

    @Override
    public Iterable<String> getSupportedPlatformNames() {
        System.err.println("getSupportedPlatformNames");
        return Arrays.asList("name", "fail");
    }

    @Override
    public PlatformDescription getPlatform(String platformName, String options) throws PlatformNotSupported {
        System.err.println("getPlatform(" + platformName + ", " + options + ")");

        if ("fail".equals(platformName)) {
            throw new PlatformNotSupported();
        }

        return new DescriptionImpl();
    }

    static {
        try {
            Field useRawMessages = Log.class.getDeclaredField("useRawMessages");

            useRawMessages.setAccessible(true);
            useRawMessages.set(null, true);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    class DescriptionImpl implements PlatformDescription {

        private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        private final StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null);

        @Override
        public JavaFileManager getFileManager() {
            System.err.println("getFileManager");
            return fm;
        }

        @Override
        public String getSourceVersion() {
            System.err.println("getSourceVersion");
            return "8";
        }

        @Override
        public String getTargetVersion() {
            System.err.println("getTargetVersion");
            return "8";
        }

        @Override
        public List<PluginInfo<Processor>> getAnnotationProcessors() {
            return Arrays.asList(new PluginInfo<Processor>() {
                @Override
                public String getName() {
                    return "test";
                }
                @Override
                public Map<String, String> getOptions() {
                    return Collections.singletonMap("testAPKey", "testAPValue");
                }
                @Override
                public Processor getPlugin() {
                    return new ProcessorImpl();
                }
            });
        }

        @Override
        public List<PluginInfo<Plugin>> getPlugins() {
            return Arrays.asList(new PluginInfo<Plugin>() {
                @Override
                public String getName() {
                    return "testPlugin";
                }
                @Override
                public Map<String, String> getOptions() {
                    return Collections.singletonMap("testPluginKey", "testPluginValue");
                }
                @Override
                public Plugin getPlugin() {
                    return new PluginImpl();
                }
            });
        }

        @Override
        public List<String> getAdditionalOptions() {
            return Arrays.asList("-Xlint:rawtypes", "-XDrawDiagnostics");
        }

        @Override
        public void close() throws IOException {
            System.err.println("close");
            fm.close();
        }

    }

    @SupportedAnnotationTypes("*")
    @SupportedOptions("testAPKey")
    class ProcessorImpl extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            System.err.println("process: " + processingEnv.getOptions());
            return true;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }

    class PluginImpl implements Plugin {

        @Override
        public String getName() {
            return "testPluginName";
        }

        @Override
        public void init(com.sun.source.util.JavacTask task, String... args) {
            System.err.println("testPlugin: " + Arrays.toString(args));
        }

    }
}
