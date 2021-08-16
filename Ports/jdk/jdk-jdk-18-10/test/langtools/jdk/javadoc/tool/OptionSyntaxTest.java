/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8166144
 * @summary support new-style options
 * @modules jdk.compiler/com.sun.tools.javac.api
 * @modules jdk.compiler/com.sun.tools.javac.main
 * @modules jdk.javadoc/jdk.javadoc.internal.api
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @library /tools/lib
 * @build toolbox.JavacTask toolbox.JavadocTask toolbox.ModuleBuilder toolbox.TestRunner toolbox.ToolBox
 * @run main OptionSyntaxTest
 */
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import toolbox.JavadocTask;
import toolbox.ModuleBuilder;
import toolbox.Task;
import toolbox.TestRunner;
import toolbox.ToolBox;


public class OptionSyntaxTest extends TestRunner {
    public static class TestDoclet implements Doclet {
        @Override
        public boolean run(DocletEnvironment root) {
            System.out.println("TestDoclet.run");
            return true;
        }

        @Override
        public String getName() {
            return "Test";
        }

        @Override
        public Set<Option> getSupportedOptions() {
            return options;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

        @Override
        public void init(Locale locale, Reporter reporter) {
        }

        private final Set<Doclet.Option> options = new HashSet<>(Arrays.asList(
                new DOption("-old", 0),
                new DOption("-oldWithArg", 1),
                new DOption("-oldWithArgs", 2),
                new DOption("--new", 0),
                new DOption("--newWithArg", 1),
                new DOption("--newWithArgs", 2)
        ));

    }

    static class DOption implements Doclet.Option {
        private final List<String> names = new ArrayList<>();
        private final int argCount;

        DOption(String name, int argCount) {
            this.names.add(name);
            this.argCount = argCount;
        }

        @Override
        public int getArgumentCount() {
            return argCount;
        }

        @Override
        public String getDescription() {
            return "description[" + names.get(0) + "]";
        }

        @Override
        public Kind getKind() {
            return Doclet.Option.Kind.STANDARD;
        }

        @Override
        public List<String> getNames() {
            return names;
        }

        @Override
        public String getParameters() {
            return argCount > 0 ? "parameters[" + names.get(0) + "," + argCount + "]" : null;
        }

        @Override
        public boolean process(String option, List<String> arguments) {
            List<String> args = new ArrayList<>();
            for (int i = 0; i < argCount && i < arguments.size(); i++) {
                args.add(arguments.get(i));
            }
            System.out.println("process " + option + " " + args);
            return args.stream().filter(s -> s.startsWith("arg")).count() == argCount;
        }
    }

    public static void main(String... args) throws Exception {
        OptionSyntaxTest t = new OptionSyntaxTest();
        t.runTests();
    }

    private final ToolBox tb = new ToolBox();
    private final Path src = Paths.get("src");
    private final Path modules = Paths.get("modules");

    OptionSyntaxTest() throws IOException {
        super(System.err);
        initModules();
    }

    void initModules() throws IOException {
        new ModuleBuilder(tb, "m1")
                .exports("p1")
                .classes("package p1; public class C1 { }")
                .write(src);

        new ModuleBuilder(tb, "m2")
                .exports("p2")
                .classes("package p2; public class C2 { }")
                .build(modules);

    }

    @Test
    public void testBasic() {
        new JavadocTask(tb, Task.Mode.CMDLINE)
                .options("-docletpath", System.getProperty("test.classes"),
                       "-doclet", TestDoclet.class.getName(),
                       "-sourcepath", "src/m1",
                       "p1")
                .run()
                .writeAll();
    }

    @Test
    public void testNewSourcePath() {
        new JavadocTask(tb, Task.Mode.CMDLINE)
                .options("-docletpath", System.getProperty("test.classes"),
                       "-doclet", TestDoclet.class.getName(),
                       "--source-path", "src/m1",
                       "p1")
                .run()
                .writeAll();
    }

    @Test
    public void testNewSourcePathEquals() {
        new JavadocTask(tb, Task.Mode.CMDLINE)
                .options("-docletpath", System.getProperty("test.classes"),
                       "-doclet", TestDoclet.class.getName(),
                       "--source-path=src/m1",
                       "p1")
                .run()
                .writeAll();
    }

    @Test
    public void testOldDocletArgs() {
        new JavadocTask(tb, Task.Mode.CMDLINE)
                .options("-docletpath", System.getProperty("test.classes"),
                       "-doclet", TestDoclet.class.getName(),
                       "-sourcepath", "src/m1",
                       "-old",
                       "-oldWithArg", "arg",
                       "-oldWithArgs", "arg1", "arg2",
                       "p1")
                .run()
                .writeAll();
    }

    @Test
    public void testNewDocletArgs() {
        new JavadocTask(tb, Task.Mode.CMDLINE)
                .options("-docletpath", System.getProperty("test.classes"),
                       "-doclet", TestDoclet.class.getName(),
                       "-sourcepath", "src/m1",
                       "--new",
                       "--newWithArg", "arg",
                       "--newWithArgs", "arg1", "arg2",
                       "p1")
                .run()
                .writeAll();
    }

    @Test
    public void testNewDocletArgsEquals() {
        new JavadocTask(tb, Task.Mode.CMDLINE)
                .options("-docletpath", System.getProperty("test.classes"),
                       "-doclet", TestDoclet.class.getName(),
                       "-sourcepath", "src/m1",
                       "--new", "--newWithArg=arg",
                       "p1")
                .run()
                .writeAll();
    }

    @Test
    public void testNewDocletArgsMissingArgs() throws Exception {
        String log = new JavadocTask(tb, Task.Mode.CMDLINE)
                .options("-docletpath", System.getProperty("test.classes"),
                       "-doclet", TestDoclet.class.getName(),
                       "-sourcepath", "src/m1",
                       "--newWithArg")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
        if (!log.contains("option --newWithArg requires an argument"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testNewDocletArgsExtraArgs() throws Exception {
        String log = new JavadocTask(tb, Task.Mode.CMDLINE)
                .options("-docletpath", System.getProperty("test.classes"),
                       "-doclet", TestDoclet.class.getName(),
                       "-sourcepath", "src/m1",
                       "--new=arg",
                       "p1")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
        if (!log.contains("option --new does not require an argument"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testNewDocletArgsExtraArgs2() throws Exception {
        String log = new JavadocTask(tb, Task.Mode.CMDLINE)
                .options("-docletpath", System.getProperty("test.classes"),
                       "-doclet", TestDoclet.class.getName(),
                       "-sourcepath", "src/m1",
                       "--newWithArgs=arg1 arg2",
                       "p1")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
        if (!log.contains("cannot use '=' syntax for options that require multiple arguments"))
            throw new Exception("expected output not found");
    }

}
