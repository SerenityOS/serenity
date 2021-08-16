/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 *  @test
 *  @bug 8198552
 *  @summary Check that multiple plugins can be specified when starting javac
 *  @library /tools/lib
 *  @modules jdk.compiler/com.sun.tools.javac.api
 *           jdk.compiler/com.sun.tools.javac.main
 *           jdk.jdeps/com.sun.tools.javap
 *  @build toolbox.ToolBox toolbox.JavacTask toolbox.JarTask
 *  @run main MultiplePlugins
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class MultiplePlugins {
    public static void main(String... args) throws Exception {
        new MultiplePlugins().run();
    }

    final File pluginClasses;
    final File pluginModuleClasses;
    final File pluginJar;
    final JavaCompiler compiler;
    final ToolBox tb = new ToolBox();

    MultiplePlugins() throws Exception {
        pluginClasses = new File("plugin");
        tb.createDirectories(pluginClasses.toPath());
        pluginModuleClasses = new File("plugin-modules");
        pluginJar = new File("plugin.jar");
        compiler = ToolProvider.getSystemJavaCompiler();
    }

    void run() throws Exception {
        // compile the plugins:
        new JavacTask(tb)
          .options("-d", pluginClasses.getPath())
          .sources(PLUGIN1, PLUGIN2)
          .run();

        File plugin = new File(pluginClasses.getPath(), "META-INF/services/com.sun.source.util.Plugin");
        tb.writeFile(plugin.getPath(), "p1.Plugin1\np2.Plugin2\n");
        new JarTask(tb)
          .run("cf", pluginJar.getPath(), "-C", pluginClasses.getPath(), ".");

        testCommandLine(EXPECTED, "--processor-path", pluginJar.toString(), "-Xplugin:plugin1", "-Xplugin:plugin2");
        testAPI(EXPECTED, "--processor-path", pluginJar.toString(), "-Xplugin:plugin1", "-Xplugin:plugin2");

        // compile the plugins as modules:
        File m1 = new File(pluginModuleClasses, "m1");
        tb.createDirectories(m1.toPath());
        new JavacTask(tb)
          .options("-d", m1.getPath())
          .sources(MODULE1, PLUGIN1)
          .run();

        File m2 = new File(pluginModuleClasses, "m2");
        tb.createDirectories(m2.toPath());
        new JavacTask(tb)
          .options("-d", m2.getPath())
          .sources(MODULE2, PLUGIN2)
          .run();

        testCommandLine(EXPECTED, "--processor-module-path", pluginModuleClasses.toString(), "-Xplugin:plugin1", "-Xplugin:plugin2");
        testAPI(EXPECTED, "--processor-module-path", pluginModuleClasses.toString(), "-Xplugin:plugin1", "-Xplugin:plugin2");

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    void testAPI(List<String> ref, String... opts) throws Exception {
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(new File(".")));

            System.err.println("test api: " + List.of(opts));
            Task.Result result = new JavacTask(tb, Task.Mode.API)
                                      .fileManager(fm)
                                      .options(opts)
                                      .sources(TEST)
                                      .run(Task.Expect.SUCCESS)
                                      .writeAll();
            List<String> out = result.getOutputLines(Task.OutputKind.STDERR);
            checkOutput(out, ref);
        }
    }

    void testCommandLine(List<String> ref,String... opt) throws IOException {
        Path testJavaFile = Paths.get("Test.java");

        tb.writeFile(testJavaFile, TEST);

        List<String> args = new ArrayList<>();

        args.add("-d"); args.add(".");
        args.addAll(List.of(opt));

        System.err.println("test command line: " + Arrays.asList(args));
        Task.Result result = new JavacTask(tb, Task.Mode.CMDLINE)
                                  .options(args)
                                  .files(testJavaFile)
                                  .run(Task.Expect.SUCCESS)
                                  .writeAll();
        List<String> out = result.getOutputLines(Task.OutputKind.STDERR);
        checkOutput(out, ref);
    }

    private void checkOutput(List<String> lines, List<String> ref) {
        if (!lines.equals(ref)) {
            error("unexpected output");
        }
    }

    private void error(String msg) {
        System.err.println(msg);
        errors++;
    }

    int errors;

    private static final String MODULE1 =
            "module m1 {\n" +
            "    requires jdk.compiler;\n" +
            "    provides com.sun.source.util.Plugin with p1.Plugin1;\n" +
            "}\n";
    private static final String PLUGIN1 =
            "package p1;\n" +
            "import com.sun.source.util.*;\n" +
            "public class Plugin1 implements Plugin {\n" +
            "    public String getName() {\n" +
            "        return \"plugin1\";\n" +
            "    }\n" +
            "    public void init(JavacTask task, String... args) {\n" +
            "        System.err.println(\"plugin1\");\n" +
            "    }\n" +
            "}";
    private static final String MODULE2 =
            "module m2 {\n" +
            "    requires jdk.compiler;\n" +
            "    provides com.sun.source.util.Plugin with p2.Plugin2;\n" +
            "}\n";
    private static final String PLUGIN2 =
            "package p2;\n" +
            "import com.sun.source.util.*;\n" +
            "public class Plugin2 implements Plugin {\n" +
            "    public String getName() {\n" +
            "        return \"plugin2\";\n" +
            "    }\n" +
            "    public void init(JavacTask task, String... args) {\n" +
            "        System.err.println(\"plugin2\");\n" +
            "    }\n" +
            "}";
    private static final String TEST =
            "public class Test {}";
    private static final List<String> EXPECTED = List.of(
            "plugin1",
            "plugin2"
    );

}
