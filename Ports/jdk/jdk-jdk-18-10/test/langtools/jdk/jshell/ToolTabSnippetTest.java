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
 * @bug 8177076 8185426 8189595 8188072 8221759 8255273
 * @modules
 *     jdk.compiler/com.sun.tools.javac.api
 *     jdk.compiler/com.sun.tools.javac.main
 *     jdk.jshell/jdk.internal.jshell.tool:+open
 *     jdk.jshell/jdk.internal.jshell.tool.resources:open
 *     jdk.jshell/jdk.jshell:open
 * @library /tools/lib
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask
 * @build Compiler UITesting
 * @build ToolTabSnippetTest
 * @run testng/timeout=300 ToolTabSnippetTest
 */

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import jdk.internal.jshell.tool.ConsoleIOContextTestSupport;
import org.testng.annotations.Test;

@Test
public class ToolTabSnippetTest extends UITesting {

    public ToolTabSnippetTest() {
        super(true);
    }

    public void testExpression() throws Exception {
        Path classes = prepareZip();
        doRunTest((inputSink, out) -> {
            inputSink.write("/env -class-path " + classes.toString() + "\n");
            waitOutput(out, resource("jshell.msg.set.restore") + "\n\\u001B\\[\\?2004h" + PROMPT);
            inputSink.write("import jshelltest.*;\n");
            waitOutput(out, "\n\\u001B\\[\\?2004l\\u001B\\[\\?2004h" + PROMPT);

            //-> <tab>
            inputSink.write(TAB);
            waitOutput(out, getMessage("jshell.console.completion.all.completions.number", "[0-9]+"));
            inputSink.write(TAB);
            waitOutput(out, ".*String.*StringBuilder.*" +
                            REDRAW_PROMPT + "");

            //new JShellTes<tab>
            inputSink.write("new JShellTes" + TAB);
            waitOutput(out, "\nJShellTest\\(      JShellTestAux\\(   " +
                            REDRAW_PROMPT + "new JShellTest");

            //new JShellTest<tab>
            inputSink.write(TAB);
            waitOutput(out, "JShellTest\\(      JShellTestAux\\(   \n" +
                            "\n" +
                            resource("jshell.console.completion.current.signatures") + "\n" +
                            "jshelltest.JShellTest\n" +
                            "\n" +
                            resource("jshell.console.see.documentation") +
                            REDRAW_PROMPT + "new JShellTest");
            inputSink.write(TAB);
            waitOutput(out, "\\u001B\\[1mjshelltest.JShellTest\\u001B\\[0m\n" +
                            "JShellTest 0" +
                            REDRAW_PROMPT + "new JShellTest");
            inputSink.write(TAB);
            waitOutput(out, "JShellTest\\(      JShellTestAux\\(   \n" +
                            "\n" +
                            resource("jshell.console.completion.current.signatures") + "\n" +
                            "jshelltest.JShellTest\n" +
                            "\n" +
                            resource("jshell.console.see.documentation") +
                            REDRAW_PROMPT + "new JShellTest");

            //new JShellTest(<tab>
            inputSink.write("(" + TAB);
            waitOutput(out, "\\(\n" +
                            resource("jshell.console.completion.current.signatures") + "\n" +
                            "JShellTest\\(String str\\)\n" +
                            "JShellTest\\(String str, int i\\)\n" +
                            "\n" +
                            resource("jshell.console.see.documentation") +
                            REDRAW_PROMPT + "new JShellTest\\(");
            inputSink.write(TAB);
            waitOutput(out, "\\u001B\\[1mJShellTest\\(String str\\)\\u001B\\[0m\n" +
                            "JShellTest 1\n" +
                            "1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n" +
                            "\n" +
                            resource("jshell.console.see.next.page") +
                            REDRAW_PROMPT + "new JShellTest\\(");
            inputSink.write(TAB);
            waitOutput(out, "1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n" +
                            "\n" +
                            resource("jshell.console.see.next.javadoc") +
                            REDRAW_PROMPT + "new JShellTest\\(");
            inputSink.write(TAB);
            waitOutput(out, "\\u001B\\[1mJShellTest\\(String str, int i\\)\\u001B\\[0m\n" +
                            "JShellTest 2\n" +
                            "\n" +
                            getMessage("jshell.console.completion.all.completions.number", "[0-9]+") +
                            REDRAW_PROMPT + "new JShellTest\\(");
            inputSink.write(TAB);
            waitOutput(out, ".*String.*StringBuilder.*" +
                            REDRAW_PROMPT + "new JShellTest\\(");

            inputSink.write(INTERRUPT + "String str = \"\";\nnew JShellTest(");
            waitOutput(out, PROMPT + "new JShellTest\\(");

            inputSink.write(TAB);
            waitOutput(out, "\n" +
                            "str   \n" +
                            "\n" +
                            resource("jshell.console.completion.current.signatures") + "\n" +
                            "JShellTest\\(String str\\)\n" +
                            "JShellTest\\(String str, int i\\)\n" +
                            "\n" +
                            resource("jshell.console.see.documentation") +
                            REDRAW_PROMPT + "new JShellTest\\(");
            inputSink.write(TAB);
            waitOutput(out, "\\u001B\\[1mJShellTest\\(String str\\)\\u001B\\[0m\n" +
                            "JShellTest 1\n" +
                            "1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n" +
                            "\n" +
                            resource("jshell.console.see.next.page") +
                            REDRAW_PROMPT + "new JShellTest\\(");
            inputSink.write(TAB);
            waitOutput(out, "1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n" +
                            "\n" +
                            resource("jshell.console.see.next.javadoc") +
                            REDRAW_PROMPT + "new JShellTest\\(");
            inputSink.write(TAB);
            waitOutput(out, "\\u001B\\[1mJShellTest\\(String str, int i\\)\\u001B\\[0m\n" +
                            "JShellTest 2\n" +
                            "\n" +
                            getMessage("jshell.console.completion.all.completions.number", "[0-9]+") +
                            REDRAW_PROMPT + "new JShellTest\\(");
            inputSink.write(TAB);
            waitOutput(out, ".*String.*StringBuilder.*" +
                            REDRAW_PROMPT + "new JShellTest\\(");

            inputSink.write(INTERRUPT + "JShellTest t = new JShellTest" + TAB);
            waitOutput(out, PROMPT + "JShellTest t = new JShellTest\n" +
                            "JShellTest\\(   \n" +
                            "\n" +
                            resource("jshell.console.completion.current.signatures") + "\n" +
                            "jshelltest.JShellTest\n" +
                            "\n" +
                            resource("jshell.console.completion.all.completions") +
                            REDRAW_PROMPT + "JShellTest t = new JShellTest");
            inputSink.write(TAB);
            waitOutput(out, "JShellTest\\(      JShellTestAux\\(   \n" +
                            "\n" +
                            resource("jshell.console.see.documentation") +
                            REDRAW_PROMPT + "JShellTest t = new JShellTest");

            inputSink.write(INTERRUPT + "JShellTest t = new " + TAB);
            waitOutput(out, PROMPT + "JShellTest t = new \n" +
                            "JShellTest\\(   \n" +
                            "\n" +
                            getMessage("jshell.console.completion.all.completions.number", "[0-9]+") +
                            REDRAW_PROMPT + "JShellTest t = new ");
            inputSink.write(TAB);
            waitOutput(out, ".*String.*StringBuilder.*" +
                            REDRAW_PROMPT + "JShellTest t = new ");

            inputSink.write(INTERRUPT + "class JShelX{}\n");
            inputSink.write("new JShel" + TAB);
            waitOutput(out, PROMPT + "new JShel\n" +
                            "JShelX\\(\\)         JShellTest\\(      JShellTestAux\\(   " +
                            REDRAW_PROMPT + "new JShel");

            //no crash:
            inputSink.write(INTERRUPT + "new Stringbuil" + TAB);
            waitOutput(out, PROMPT + "new Stringbuil" + BELL);

            //no crash: 8188072
            inputSink.write(INTERRUPT + "for (int:" + TAB);
            waitOutput(out, PROMPT + "for \\(int:\n" +
                            getMessage("jshell.console.completion.all.completions.number", "[0-9]+") +
                            REDRAW_PROMPT + "for \\(int:");
        });
    }

    public void testCleaningCompletionTODO() throws Exception {
        doRunTest((inputSink, out) -> {
            CountDownLatch testCompleteComputationStarted = new CountDownLatch(1);
            CountDownLatch testCompleteComputationContinue = new CountDownLatch(1);
            ConsoleIOContextTestSupport.IMPL = new ConsoleIOContextTestSupport() {
                @Override
                protected void willComputeCompletionCallback() {
                    if (testCompleteComputationStarted != null) {
                        testCompleteComputationStarted.countDown();
                    }
                    if (testCompleteComputationContinue != null) {
                        try {
                            testCompleteComputationContinue.await();
                        } catch (InterruptedException ex) {
                            throw new IllegalStateException(ex);
                        }
                    }
                }
            };
            //-> <tab>
            inputSink.write(TAB);
            testCompleteComputationStarted.await();
            //-> <tab><tab>
            inputSink.write(TAB + TAB);
            testCompleteComputationContinue.countDown();
            waitOutput(out, PROMPT);
            //-> <tab>
            inputSink.write(TAB);
            waitOutput(out, PROMPT);
            ConsoleIOContextTestSupport.IMPL = null;
        });
    }

    public void testNoRepeat() throws Exception {
        doRunTest((inputSink, out) -> {
            inputSink.write("String xyzAA;\n");
            waitOutput(out, PROMPT);

            //xyz<tab>
            inputSink.write("String s = xyz" + TAB);
            waitOutput(out, "^String s = xyzAA");
            inputSink.write(".");
            waitOutput(out, "^\\.");

            inputSink.write(INTERRUPT);
            waitOutput(out, PROMPT);

            inputSink.write("double xyzAB;\n");
            waitOutput(out, PROMPT);

            //xyz<tab>
            inputSink.write("String s = xyz" + TAB);
            String allCompletions =
                    resource("jshell.console.completion.all.completions");
            waitOutput(out, ".*xyzAA.*" + allCompletions + ".*\u0005String s = xyzA");
        });
    }

    public void testCrash8221759() throws Exception {
        doRunTest((inputSink, out) -> {
            inputSink.write("java.io.File.path" + TAB);
            waitOutput(out, "java.io.File.path\n" +
                            "pathSeparator       pathSeparatorChar   " +
                            REDRAW_PROMPT + "java.io.File.pathSeparator");
        });
    }

    private Path prepareZip() {
        String clazz1 =
                "package jshelltest;\n" +
                "/**JShellTest 0" +
                " */\n" +
                "public class JShellTest {\n" +
                "    /**JShellTest 1\n" +
                "     * <p>1 <p>1 <p>1 <p>1 <p>1 <p>1 <p>1 <p>1 <p>1 <p>1\n" +
                "     * <p>1 <p>1 <p>1 <p>1 <p>1 <p>1 <p>1 <p>1 <p>1 <p>1\n" +
                "     * <p>1 <p>1 <p>1 <p>1 <p>1 <p>1 <p>1 <p>1 <p>1 <p>1\n" +
                "     */\n" +
                "    public JShellTest(String str) {}\n" +
                "    /**JShellTest 2" +
                "     */\n" +
                "    public JShellTest(String str, int i) {}\n" +
                "}\n";

        String clazz2 =
                "package jshelltest;\n" +
                "/**JShellTestAux 0" +
                " */\n" +
                "public class JShellTestAux {\n" +
                "    /**JShellTest 1" +
                "     */\n" +
                "    public JShellTestAux(String str) { }\n" +
                "    /**JShellTest 2" +
                "     */\n" +
                "    public JShellTestAux(String str, int i) { }\n" +
                "}\n";

        Path srcZip = Paths.get("src.zip");

        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(srcZip))) {
            out.putNextEntry(new JarEntry("jshelltest/JShellTest.java"));
            out.write(clazz1.getBytes());
            out.putNextEntry(new JarEntry("jshelltest/JShellTestAux.java"));
            out.write(clazz2.getBytes());
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        compiler.compile(clazz1, clazz2);

        try {
            Field availableSources = Class.forName("jdk.jshell.SourceCodeAnalysisImpl").getDeclaredField("availableSourcesOverride");
            availableSources.setAccessible(true);
            availableSources.set(null, Arrays.asList(srcZip));
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException | ClassNotFoundException ex) {
            throw new IllegalStateException(ex);
        }

        return compiler.getClassDir();
    }
    //where:
        private final Compiler compiler = new Compiler();

    public void testDocumentationAfterInsert() throws Exception {
        doRunTest((inputSink, out) -> {
            inputSink.write("import java.time.*\n");
            waitOutput(out, PROMPT);

            inputSink.write("new Instant" + TAB);
            waitOutput(out, PROMPT + "new InstantiationE");
        });
    }
}
