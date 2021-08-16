/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Verify that plugins inside modules works
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main PluginsInModulesTest
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import toolbox.JavacTask;
import toolbox.Task;

public class PluginsInModulesTest extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new PluginsInModulesTest().runTests();
    }

    private static final String pluginModule1 =
            """
                module pluginMod1x {
                    requires jdk.compiler;

                    provides com.sun.source.util.Plugin
                      with mypkg1.SimplePlugin1;
                }""";

    private static final String plugin1 =
            """
                package mypkg1;
                import com.sun.source.util.JavacTask;
                import com.sun.source.util.Plugin;
                import com.sun.source.util.TaskEvent;
                import com.sun.source.util.TaskListener;

                public class SimplePlugin1 implements Plugin {

                    @Override
                    public String getName() {
                        return "simpleplugin1";
                    }

                    @Override
                    public void init(JavacTask task, String... args) {
                        task.addTaskListener(new PostAnalyzeTaskListener());
                    }

                    private static class PostAnalyzeTaskListener implements TaskListener {
                        @Override
                        public void started(TaskEvent taskEvent) {\s
                            if (taskEvent.getKind().equals(TaskEvent.Kind.COMPILATION)) {
                                System.out.println("simpleplugin1 started for event " + taskEvent.getKind());
                            }
                        }

                        @Override
                        public void finished(TaskEvent taskEvent) {
                            if (taskEvent.getKind().equals(TaskEvent.Kind.COMPILATION)) {
                                System.out.println("simpleplugin1 finished for event " + taskEvent.getKind());
                            }
                        }
                    }
                }""";

    private static final String testClass = "class Test { }";

    void initialization(Path base) throws Exception {
        moduleSrc = base.resolve("plugin_mods_src");
        Path pluginMod1 = moduleSrc.resolve("pluginMod1x");

        processorCompiledModules = base.resolve("mods");

        Files.createDirectories(processorCompiledModules);

        tb.writeJavaFiles(
                pluginMod1,
                pluginModule1,
                plugin1);

        String log = new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString())
                .outdir(processorCompiledModules)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.isEmpty()) {
            throw new AssertionError("Unexpected output: " + log);
        }

        classes = base.resolve("classes");
        Files.createDirectories(classes);
    }

    Path processorCompiledModules;
    Path moduleSrc;
    Path classes;

    @Test
    public void testUseOnlyOneProcessor(Path base) throws Exception {
        initialization(base);
        List<String> log = new JavacTask(tb)
                .options("--processor-module-path", processorCompiledModules.toString(),
                        "-Xplugin:simpleplugin1")
                .outdir(classes)
                .sources(testClass)
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.STDOUT);
        if (!log.equals(Arrays.asList("simpleplugin1 started for event COMPILATION",
                                      "simpleplugin1 finished for event COMPILATION"))) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }
}
