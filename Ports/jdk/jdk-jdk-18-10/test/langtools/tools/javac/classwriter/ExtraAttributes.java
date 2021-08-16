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

/*
 * @test
 * @bug 8234689
 * @summary facilitate writing additional custom attributes in a class file
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.jvm
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.JarTask toolbox.JavacTask toolbox.JavapTask toolbox.ToolBox
 * @run main ExtraAttributes
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;

import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.jvm.ClassWriter;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import toolbox.JarTask;
import toolbox.JavapTask;
import toolbox.Task;
import toolbox.ToolBox;


public class ExtraAttributes implements Plugin {
    public static void main(String... args) throws Exception {
        new ExtraAttributes().run();
    }

    void run() throws Exception {
        ToolBox tb = new ToolBox();
        Path pluginClasses = Path.of("plugin-classes");
        tb.writeFile(pluginClasses.resolve("META-INF").resolve("services").resolve(Plugin.class.getName()),
                ExtraAttributes.class.getName() + "\n");
        Files.copy(Path.of(ToolBox.testClasses).resolve("ExtraAttributes.class"),
                pluginClasses.resolve("ExtraAttributes.class"));

        Path pluginJar = Path.of("plugin.jar");
        new JarTask(tb, pluginJar)
                .baseDir(pluginClasses)
                .files(".")
                .run();

        Path src = Path.of("src");
            tb.writeJavaFiles(src,
                    "public class HelloWorld {\n"
                    + "    public static String message = \"Hello World!\";\n"
                    + "    public static void main(String... args) {\n"
                    + "        System.out.println(message);\n"
                    + "    }\n"
                    + "}\n");

        List<String> stdout = new toolbox.JavacTask(tb)
                .classpath(pluginJar)
                .options("-XDaccessInternalAPI")
                .outdir(Files.createDirectories(Path.of("classes")))
                .files(tb.findJavaFiles(src))
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.STDOUT);

        // cannot rely on order of output, so sort it
        stdout.sort(CharSequence::compare);

        tb.checkEqual(stdout,
                List.of(
                        "Add attributes for <clinit>()",
                        "Add attributes for HelloWorld",
                        "Add attributes for HelloWorld()",
                        "Add attributes for main(java.lang.String...)",
                        "Add attributes for message"
                ));

        List<String> lines = new JavapTask(tb)
                .options("-p",
                        "-v",
                        Path.of("classes").resolve("HelloWorld.class").toString())
                .run()
                .getOutputLines(Task.OutputKind.DIRECT);

        long attrs = lines.stream()
                .filter(s -> s.contains("testAttr:"))
                .count();
        if (attrs != 5) {
            throw new Exception("expected attributes not found; expected: 5; found: " + attrs);
        }
    }

    // Plugin impl...

    private ClassWriter classWriter;
    private Names names;

    @Override
    public String getName() { return "ExtraAttributes"; }

    @Override
    public void init(JavacTask task, String... args) {
        Context c = ((BasicJavacTask) task).getContext();
        classWriter = ClassWriter.instance(c);
        names = Names.instance(c);

        // register callback
        classWriter.addExtraAttributes(this::addExtraAttributes);
    }

    @Override
    public boolean autoStart() {
        return true;
    }

    private int addExtraAttributes(Symbol sym) {
        System.out.println("Add attributes for " + sym);
        Name testAttr = names.fromString("testAttr");
        int alenIdx = classWriter.writeAttr(testAttr);
        classWriter.databuf.appendChar(42);
        classWriter.endAttr(alenIdx);
        return 1;
    }
}

