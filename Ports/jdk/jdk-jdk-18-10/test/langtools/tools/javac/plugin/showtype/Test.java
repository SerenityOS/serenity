/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
 *  @bug 8001098 8004961 8004082
 *  @library /tools/lib
 *  @modules jdk.compiler/com.sun.tools.javac.api
 *           jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 *  @build toolbox.ToolBox toolbox.JavacTask toolbox.JarTask
 *  @run main Test
 *  @summary Provide a simple light-weight "plug-in" mechanism for javac
 */

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class Test {
    public static void main(String... args) throws Exception {
        new Test().run();
    }

    final File testSrc;
    final File pluginSrc;
    final File pluginClasses ;
    final File pluginJar;
    final List<String> ref1;
    final List<String> ref2;
    final JavaCompiler compiler;
    final StandardJavaFileManager fm;
    ToolBox tb = new ToolBox();

    Test() throws Exception {
        testSrc = new File(tb.testSrc);
        pluginSrc = new File(testSrc, "ShowTypePlugin.java");
        pluginClasses = new File("plugin");
        tb.createDirectories(pluginClasses.toPath());
        pluginJar = new File("plugin.jar");
        ref1 = tb.readAllLines((new File(testSrc,"Identifiers.out")).toPath());
        ref2 = tb.readAllLines((new File(testSrc,"Identifiers_PI.out")).toPath());
        compiler = ToolProvider.getSystemJavaCompiler();
        fm = compiler.getStandardFileManager(null, null, null);
    }

    void run() throws Exception {
        try {
            // compile the plugin explicitly, to a non-standard directory
            // so that we don't find it on the wrong path by accident
            new JavacTask(tb)
              .options("-d", pluginClasses.getPath())
              .files(pluginSrc.getPath())
              .run();

            File plugin = new File(pluginClasses.getPath(), "META-INF/services/com.sun.source.util.Plugin");
            tb.writeFile(plugin.getPath(), "ShowTypePlugin\n");
            new JarTask(tb)
              .run("cf", pluginJar.getPath(), "-C", pluginClasses.getPath(), ".");

            testCommandLine("-Xplugin:showtype", ref1);
            testCommandLine("-Xplugin:showtype PI", ref2);
            testAPI("-Xplugin:showtype", ref1);
            testAPI("-Xplugin:showtype PI", ref2);

            if (errors > 0)
                throw new Exception(errors + " errors occurred");
        } finally {
            fm.close();
        }
    }

    void testAPI(String opt, List<String> ref) throws Exception {
        File identifiers = new File(testSrc, "Identifiers.java");
        fm.setLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH, Arrays.asList(pluginJar));
        fm.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(new File(".")));
        List<String> options = Arrays.asList(opt);
        Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(identifiers);

        System.err.println("test api: " + options + " " + files);
        Task.Result result = new JavacTask(tb, Task.Mode.API)
                                  .fileManager(fm)
                                  .options(opt)
                                  .files(identifiers.toPath())
                                  .run(Task.Expect.SUCCESS)
                                  .writeAll();
        String out = result.getOutput(Task.OutputKind.DIRECT);
        checkOutput(out, ref);
    }

    void testCommandLine(String opt, List<String> ref) {
        File identifiers = new File(testSrc, "Identifiers.java");
        String[] args = {
            "-d", ".",
            "-processorpath", pluginJar.getPath(),
            opt,
            identifiers.getPath() };

        System.err.println("test command line: " + Arrays.asList(args));
        Task.Result result = new JavacTask(tb, Task.Mode.CMDLINE)
                                  .options(args)
                                  .run(Task.Expect.SUCCESS)
                                  .writeAll();
        String out = result.getOutput(Task.OutputKind.DIRECT);
        checkOutput(out, ref);
    }

    private void checkOutput(String out, List<String> ref) {
        List<String> lines = Arrays.asList(out
                .replaceAll(".*?([A-Za-z.]+:[0-9]+: .*)", "$1") // remove file directory
                .split("[\r\n]+"));                             // allow for newline formats
        if (!lines.equals(ref)) {
            error("unexpected output");
        }
    }

    private void error(String msg) {
        System.err.println(msg);
        errors++;
    }

    int errors;
}
