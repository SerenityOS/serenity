/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8182450
 * @summary Bad classfiles should not abort compilations
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.code
 *      jdk.compiler/com.sun.tools.javac.comp
 *      jdk.compiler/com.sun.tools.javac.jvm
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.compiler/com.sun.tools.javac.processing
 *      jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main NoAbortForBadClassFile
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.code.DeferredCompletionFailureHandler;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Context.Factory;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Options;
import toolbox.Task;
import toolbox.Task.Expect;

import toolbox.TestRunner;
import toolbox.ToolBox;

public class NoAbortForBadClassFile extends TestRunner {

    private ToolBox tb = new ToolBox();

    public NoAbortForBadClassFile() {
        super(System.out);
    }

    public static void main(String... args) throws Exception {
        new NoAbortForBadClassFile().runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    @Test
    public void testBrokenClassFile(Path base) throws Exception {
        Path classes = base.resolve("classes");
        Path brokenClassFile = classes.resolve("test").resolve("Broken.class");

        Files.createDirectories(brokenClassFile.getParent());
        Files.newOutputStream(brokenClassFile).close();

        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "package test; public class Test { private void test() { Broken b; String.unknown(); } }");
        Path out = base.resolve("out");
        tb.createDirectories(out);

        List<String> log = new toolbox.JavacTask(tb)
                .options("-classpath", classes.toString(),
                         "-XDrawDiagnostics")
                .outdir(out)
                .files(tb.findJavaFiles(src))
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expectedOut = Arrays.asList(
                "Test.java:1:57: compiler.err.cant.access: test.Broken, (compiler.misc.bad.class.file.header: Broken.class, (compiler.misc.class.file.wrong.class: java.lang.AutoCloseable))",
                 "Test.java:1:73: compiler.err.cant.resolve.location.args: kindname.method, unknown, , , (compiler.misc.location: kindname.class, java.lang.String, null)",
                 "2 errors"
        );

        if (!expectedOut.equals(log))
            throw new Exception("expected output not found: " + log);
    }

    @Test
    public void testLoading(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "public class Test { static { new Object() {}; } public static class I { public static class II { } } }");
        Path out = base.resolve("out");
        tb.createDirectories(out);

        new toolbox.JavacTask(tb)
                .outdir(out)
                .options("-source", "10", "-target", "10")
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<Path> files;
        try (Stream<Path> dir = Files.list(out)) {
            files = dir.collect(Collectors.toList());
        }

        List<List<Path>> result = new ArrayList<>();

        permutations(files, Collections.emptyList(), result);

        int testNum = 0;

        for (List<Path> order : result) {
            for (Path missing : order) {
                Path test = base.resolve(String.valueOf(testNum++)).resolve("test");

                tb.createDirectories(test);

                for (Path p : order) {
                    Files.copy(p, test.resolve(p.getFileName()));
                }

                List<String> actual = complete(test, order, missing, true);

                Files.delete(test.resolve(missing.getFileName()));

                List<String> expected = complete(test, order, missing, false);

                if (!actual.equals(expected)) {
                    throw new AssertionError("Unexpected state, actual=\n" + actual + "\nexpected=\n" + expected + "\norder=" + order + "\nmissing=" + missing);
                }
            }
        }
    }

    private static void permutations(List<Path> todo, List<Path> currentList, List<List<Path>> result) {
        if (todo.isEmpty()) {
            result.add(currentList);
            return ;
        }

        for (Path p : todo) {
            List<Path> nextTODO = new ArrayList<>(todo);

            nextTODO.remove(p);

            List<Path> nextList = new ArrayList<>(currentList);

            nextList.add(p);

            permutations(nextTODO, nextList, result);
        }
    }

    private List<String> complete(Path test, List<Path> order, Path missing, boolean badClassFile) {
        Context context = new Context();
        if (badClassFile) {
            TestClassReader.preRegister(context);
        }
        JavacTool tool = JavacTool.create();
        JavacTaskImpl task = (JavacTaskImpl) tool.getTask(null, null, null, List.of("-classpath", test.toString(), "-XDblockClass=" + flatName(missing)), null, null, context);
        Symtab syms = Symtab.instance(context);
        Names names = Names.instance(context);

        DeferredCompletionFailureHandler dcfh = DeferredCompletionFailureHandler.instance(context);

        dcfh.setHandler(dcfh.javacCodeHandler);

        task.getElements().getTypeElement("java.lang.Object");

        if (!badClassFile) {
            //to ensure the same paths taken in ClassFinder.completeEnclosing in case the file is missing:
            syms.enterClass(syms.unnamedModule, names.fromString(flatName(missing)));
        }

        List<String> result = new ArrayList<>();

        for (Path toCheck : order) {
            ClassSymbol sym = syms.enterClass(syms.unnamedModule, names.fromString(flatName(toCheck)));

            try {
                sym.complete();
            } catch (CompletionFailure ignore) {
            }

            long flags = sym.flags_field;

            flags &= ~(Flags.CLASS_SEEN | Flags.SOURCE_SEEN);

            result.add("sym: " + sym.flatname + ", " + sym.owner.flatName() +
                       ", " + sym.type + ", " + sym.members_field + ", " + flags);
        }

        return result;
    }

    private String flatName(Path p) {
        return p.getFileName().toString().replace(".class", "");
    }

    private static class TestClassReader extends ClassReader {
        public static void preRegister(Context ctx) {
            ctx.put(classReaderKey, (Factory<ClassReader>) c -> new TestClassReader(ctx));
        }

        private final String block;

        public TestClassReader(Context context) {
            super(context);
            block = Options.instance(context).get("blockClass");
        }

        @Override
        public void readClassFile(ClassSymbol c) {
            super.readClassFile(c);

            if (c.flatname.contentEquals(block)) {
                throw badClassFile("blocked");
            }
        }

    }

}
