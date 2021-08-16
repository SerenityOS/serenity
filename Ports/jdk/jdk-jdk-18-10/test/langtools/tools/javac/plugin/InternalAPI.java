/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 *  @bug 8235597
 *  @summary Extend support for -XDaccessInternalAPI to plugin modules
 *  @library /tools/lib
 *  @modules jdk.compiler/com.sun.tools.javac.api
 *           jdk.compiler/com.sun.tools.javac.code
 *           jdk.compiler/com.sun.tools.javac.main
 *           jdk.jlink
 *  @build toolbox.ToolBox toolbox.JavacTask toolbox.JarTask
 *  @run main InternalAPI
 */

import java.io.IOException;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;

import toolbox.ExecTask;
import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.TestRunner;
import toolbox.ToolBox;

public class InternalAPI extends TestRunner {
    public static void main(String... args) throws Exception {
        new InternalAPI().run();
    }

    InternalAPI() {
        super(System.out);
    }

    ToolBox tb = new ToolBox();

    Path pluginJar;
    Path mclasses;

    void run() throws Exception {
        Path src = Path.of("src");
        tb.writeJavaFiles(src,
            "package p;\n"
            + "import javax.lang.model.element.*;\n"
            + "import com.sun.source.util.*;\n"
            + "import com.sun.tools.javac.code.Symbol.ClassSymbol;\n"
            + "public class C implements Plugin, TaskListener {\n"
            + "    public String getName() { return \"TestPlugin\"; }\n"
            + "    public boolean autoStart() { return true; }\n"
            + "    public void init(JavacTask task, String... args) {\n"
            + "        System.out.println(\"C.init \" + java.util.Arrays.toString(args));\n"
            + "        task.addTaskListener(this);\n"
            + "    }\n"
            + "    public void started(TaskEvent e) {\n"
            + "        TypeElement te = e.getTypeElement();\n"
            + "        if (te != null) {\n"
            + "            ClassSymbol sym = (ClassSymbol) te;\n"
            + "            System.out.println(e.getKind() + \" \" + te + \" \" + sym.kind);\n"
            + "        }\n"
            + "    }\n"
            + "}\n");

        Path msrc = Path.of("msrc");
        tb.writeJavaFiles(msrc,
            "module m {\n"
            + "    requires jdk.compiler;\n"
            + "    provides com.sun.source.util.Plugin with p.C;\n"
            + "}\n");

        Path classes = Files.createDirectories(Path.of("classes"));
        new JavacTask(tb)
            .outdir(classes)
            .options("--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED")
            .files(tb.findJavaFiles(src))
            .run()
            .writeAll();

        tb.writeFile(classes.resolve("META-INF").resolve("services").resolve("com.sun.source.util.Plugin"),
                "p.C\n");

        pluginJar = Path.of("plugin.jar");
        new JarTask(tb, pluginJar)
                .baseDir(classes)
                .files(".")
                .run();

        mclasses = Files.createDirectories(Path.of("mclasses"));
        new JavacTask(tb)
            .outdir(mclasses)
            .sourcepath(msrc, src)
            .options("--add-exports", "jdk.compiler/com.sun.tools.javac.code=m")
            .files(tb.findJavaFiles(msrc))
            .run()
            .writeAll();

        Path hw = Path.of("hw");
        tb.writeJavaFiles(hw,
                "public class HelloWorld {\n"
                + "    public static void main(String... args) {\n"
                + "        System.out.println(\"Hello World!\");\n"
                + "    }\n"
                + "}\n");

        runTests(m -> new Object[] { Path.of(m.getName()) });
    }

    @Test
    public void testClassPath(Path base) throws Exception {
        List<String> stdout = new JavacTask(tb)
                .options("-XDaccessInternalAPI")
                .classpath(pluginJar)
                .outdir(Files.createDirectories(base.resolve("out")))
                .files(tb.findJavaFiles(Path.of("hw")))
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.STDOUT);
        tb.checkEqual(stdout, List.of("C.init []", "ANALYZE HelloWorld TYP", "GENERATE HelloWorld TYP"));
    }

    // negative control test: access fails without the new option
    @Test
    public void testModuleControl(Path base) throws Exception {
        try {
            new JavacTask(tb)
                    .options("--processor-module-path", mclasses.toString())
                    .outdir(Files.createDirectories(base.resolve("out")))
                    .files(tb.findJavaFiles(Path.of("hw")))
                    .run()
                    .writeAll();
            throw new Exception("expected exception not thrown");
        } catch (Exception e) {
            System.out.println("*** " + e);
            String msg = e.getMessage();
            if (!msg.contains("java.lang.IllegalAccessError: class p.C (in module m) "
                + "cannot access class com.sun.tools.javac.code.Symbol$ClassSymbol (in module jdk.compiler)")) {
                throw new Exception("expected exception not found");
            }
        }
    }

    // positive test: access succeeds with option for modules on modulepath
    @Test
    public void testModulePath(Path base) throws IOException {
        List<String> stdout = new JavacTask(tb)
                .options("--processor-module-path", mclasses.toString(),
                        "-XDaccessInternalAPI")
                .outdir(Files.createDirectories(base.resolve("out")))
                .files(tb.findJavaFiles(Path.of("hw")))
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.STDOUT);
        tb.checkEqual(stdout, List.of("C.init []", "ANALYZE HelloWorld TYP", "GENERATE HelloWorld TYP"));
    }

    // positive test: access succeeds with option for modules in image
    @Test
    public void testImage(Path base) throws Exception {
        Path tmpJDK = base.resolve("tmpJDK");
        ToolProvider jlink = ToolProvider.findFirst("jlink")
                .orElseThrow(() -> new Exception("cannot find jlink"));
        jlink.run(System.out, System.err,
                "--module-path", mclasses.toString(),
                "--add-modules", "jdk.compiler,jdk.zipfs,m",
                "--output", tmpJDK.toString());

        String suffix = tb.isWindows() ? ".exe" : "";
        List<String> stdout = new ExecTask(tb, tmpJDK.resolve("bin").resolve("javac" + suffix))
                .args("-d", Files.createDirectories(base.resolve("out")).toString(),
                        "-XDaccessInternalAPI",
                        Path.of("hw").resolve("HelloWorld.java").toString())
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.STDOUT);
        tb.checkEqual(stdout, List.of("C.init []", "ANALYZE HelloWorld TYP", "GENERATE HelloWorld TYP"));
    }
}

