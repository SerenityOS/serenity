/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7020499
 * @summary Verify that the close resource code works properly in all cases
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox TwrClose
 * @run main TwrClose
 */

import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class TwrClose {

    private static final int MAX_RESOURCES = 5;
    public static void main(String... args) throws Exception {
        for (int i = 1; i < MAX_RESOURCES * 2; i++) {
            new TwrClose().compile(i);
        }
    }

    ToolBox tb = new ToolBox();
    JavaFileManager fm = ToolProvider.getSystemJavaCompiler()
                                     .getStandardFileManager(null, null, null);

    void compile(int trysCount) throws Exception {
        StringBuilder testInvocations = new StringBuilder();
        StringBuilder testMethods = new StringBuilder();

        for (int i = 0; i < trysCount; i++) {
            testInvocations.append(TEST_INVOCATIONS_TEMPLATE.replace("#N", Integer.toString(i)));
            testMethods.append(TEST_METHOD_TEMPLATE.replace("#N", Integer.toString(i)));
        }

        String sourceCode = FILE_TEMPLATE.replace("#TEST_INVOCATIONS", testInvocations.toString())
                                         .replace("#TEST_METHODS", testMethods.toString());

        System.err.println("analyzing:");
        System.err.println(sourceCode);

        try (ToolBox.MemoryFileManager mfm = new ToolBox.MemoryFileManager()) {
            new JavacTask(tb).fileManager(mfm)
                             .sources(sourceCode)
                             .run()
                             .writeAll();
            ClassLoader cl = new ClassLoader(TwrClose.class.getClassLoader()) {
                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    byte[] data = mfm.getFileBytes(StandardLocation.CLASS_OUTPUT, name);
                    if (data != null) {
                        return defineClass(name, data, 0, data.length);
                    }
                    return super.findClass(name);
                }
            };

            ((Runnable) cl.loadClass("Test").newInstance()).run();
        }
    }

    final String TEST_INVOCATIONS_TEMPLATE =
        "        test#N(false, false, Arrays.asList(\"close\"));\n" +
        "        test#N(false, true, Arrays.asList(\"close\", \"close-exception\"));\n" +
        "        test#N(true, false, Arrays.asList(\"close\", \"inTwr\"));\n" +
        "        test#N(true, true, Arrays.asList(\"close\", \"inTwr\", \"close-exception\"));\n";

    final String TEST_METHOD_TEMPLATE =
        "    private void test#N(boolean failInTwr, boolean failOnClose,\n" +
        "                        List<String> expectedMessages) {\n" +
        "        List<String> messages = new ArrayList<>();\n" +
        "        try {\n" +
        "            try (CloseableImpl c = new CloseableImpl(messages, failOnClose)) {\n" +
        "                if (failInTwr)\n" +
        "                    throw new IllegalStateException(\"inTwr\");\n" +
        "            }\n" +
        "        } catch (IllegalStateException ex) {\n" +
        "            messages.add(ex.getMessage());\n" +
        "            for (Throwable t : ex.getSuppressed()) {\n" +
        "                messages.add(t.getMessage());\n" +
        "            }\n" +
        "        }\n" +
        "        if (!expectedMessages.equals(messages))\n" +
        "            throw new AssertionError(\"Expected and actual messages differ; expectedMessages=\" +\n" +
        "                                     expectedMessages + \"; actual=\" + messages);\n" +
        "    }\n";

    final String FILE_TEMPLATE =
        "import java.util.*;\n" +
        "public class Test implements Runnable {\n" +
        "    public void run() {\n" +
        "#TEST_INVOCATIONS" +
        "    }\n" +
        "#TEST_METHODS" +
        "    static class CloseableImpl implements AutoCloseable {\n" +
        "        private final List<String> messages;\n" +
        "        private final boolean failOnClose;\n" +
        "        public CloseableImpl(List<String> messages, boolean failOnClose) {\n" +
        "            this.messages = messages;\n" +
        "            this.failOnClose = failOnClose;\n" +
        "        }\n" +
        "        @Override\n" +
        "        public void close() {\n" +
        "            messages.add(\"close\");\n" +
        "            if (failOnClose)\n" +
        "                throw new IllegalStateException(\"close-exception\");\n" +
        "        }\n" +
        "    }\n" +
        "}\n";
}
