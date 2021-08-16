/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Verify type annotation on binding patterns
 * @library /tools/lib
 * @modules java.compiler
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.JavapTask
 * @run main Patterns
 */

import java.lang.annotation.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import toolbox.JavapTask;
import toolbox.Task;
import toolbox.ToolBox;

public class Patterns {

    private ToolBox tb = new ToolBox();

    public static void main(String[] args) throws Exception {
        new Patterns().run();
    }

    public void run() throws Exception {
        String out = new JavapTask(tb)
                .options("-private",
                         "-verbose")
                .classpath(System.getProperty("test.classes"))
                .classes("Patterns$SimpleBindingPattern")
                .run()
                .getOutputLines(Task.OutputKind.DIRECT)
                .stream()
                .collect(Collectors.joining("\n"));

        String constantPool = out.substring(0, out.indexOf('{'));

        out = out.replaceAll("(?ms) *Code:.*?\n( *RuntimeInvisibleTypeAnnotations:)", "$1");
        out = out.substring(out.indexOf('{'));
        out = out.substring(0, out.lastIndexOf('}') + 1);

        String A = snipCPNumber(constantPool, "LPatterns$SimpleBindingPattern$A;");
        String CA = snipCPNumber(constantPool, "LPatterns$SimpleBindingPattern$CA;");
        String value = snipCPNumber(constantPool, "value");

        String expected = """
                          {
                            private static final java.lang.Object o;
                              descriptor: Ljava/lang/Object;
                              flags: (0x001a) ACC_PRIVATE, ACC_STATIC, ACC_FINAL

                            private static final boolean B1s;
                              descriptor: Z
                              flags: (0x001a) ACC_PRIVATE, ACC_STATIC, ACC_FINAL

                            private static final boolean B1m;
                              descriptor: Z
                              flags: (0x001a) ACC_PRIVATE, ACC_STATIC, ACC_FINAL

                            private final boolean B2s;
                              descriptor: Z
                              flags: (0x0012) ACC_PRIVATE, ACC_FINAL

                            private final boolean B2m;
                              descriptor: Z
                              flags: (0x0012) ACC_PRIVATE, ACC_FINAL

                            public Patterns$SimpleBindingPattern();
                              descriptor: ()V
                              flags: (0x0001) ACC_PUBLIC
                                RuntimeInvisibleTypeAnnotations:
                                  0: #_A_(): LOCAL_VARIABLE, {start_pc=206, length=11, index=2}
                                    Patterns$SimpleBindingPattern$A
                                  1: #_CA_(#_value_=[@#_A_(),@#_A_()]): LOCAL_VARIABLE, {start_pc=238, length=11, index=3}
                                    Patterns$SimpleBindingPattern$CA(
                                      value=[@Patterns$SimpleBindingPattern$A,@Patterns$SimpleBindingPattern$A]
                                    )
                                  2: #_A_(): LOCAL_VARIABLE, {start_pc=21, length=11, index=1}
                                    Patterns$SimpleBindingPattern$A
                                  3: #_CA_(#_value_=[@#_A_(),@#_A_()]): LOCAL_VARIABLE, {start_pc=53, length=11, index=1}
                                    Patterns$SimpleBindingPattern$CA(
                                      value=[@Patterns$SimpleBindingPattern$A,@Patterns$SimpleBindingPattern$A]
                                    )
                                  4: #_A_(): LOCAL_VARIABLE, {start_pc=84, length=11, index=2}
                                    Patterns$SimpleBindingPattern$A
                                  5: #_CA_(#_value_=[@#_A_(),@#_A_()]): LOCAL_VARIABLE, {start_pc=116, length=11, index=3}
                                    Patterns$SimpleBindingPattern$CA(
                                      value=[@Patterns$SimpleBindingPattern$A,@Patterns$SimpleBindingPattern$A]
                                    )
                                  6: #_A_(): LOCAL_VARIABLE, {start_pc=145, length=11, index=2}
                                    Patterns$SimpleBindingPattern$A
                                  7: #_CA_(#_value_=[@#_A_(),@#_A_()]): LOCAL_VARIABLE, {start_pc=177, length=11, index=3}
                                    Patterns$SimpleBindingPattern$CA(
                                      value=[@Patterns$SimpleBindingPattern$A,@Patterns$SimpleBindingPattern$A]
                                    )

                            void testPatterns();
                              descriptor: ()V
                              flags: (0x0000)
                                RuntimeInvisibleTypeAnnotations:
                                  0: #_A_(): LOCAL_VARIABLE, {start_pc=16, length=11, index=2}
                                    Patterns$SimpleBindingPattern$A
                                  1: #_CA_(#_value_=[@#_A_(),@#_A_()]): LOCAL_VARIABLE, {start_pc=48, length=11, index=3}
                                    Patterns$SimpleBindingPattern$CA(
                                      value=[@Patterns$SimpleBindingPattern$A,@Patterns$SimpleBindingPattern$A]
                                    )

                            void testPatternsDesugared();
                              descriptor: ()V
                              flags: (0x0000)
                                RuntimeInvisibleTypeAnnotations:
                                  0: #_A_(): LOCAL_VARIABLE, {start_pc=17, length=15, index=1; start_pc=51, length=15, index=1}
                                    Patterns$SimpleBindingPattern$A

                            static {};
                              descriptor: ()V
                              flags: (0x0008) ACC_STATIC
                                RuntimeInvisibleTypeAnnotations:
                                  0: #_A_(): LOCAL_VARIABLE, {start_pc=21, length=11, index=0}
                                    Patterns$SimpleBindingPattern$A
                                  1: #_CA_(#_value_=[@#_A_(),@#_A_()]): LOCAL_VARIABLE, {start_pc=52, length=11, index=0}
                                    Patterns$SimpleBindingPattern$CA(
                                      value=[@Patterns$SimpleBindingPattern$A,@Patterns$SimpleBindingPattern$A]
                                    )
                                  2: #_A_(): LOCAL_VARIABLE, {start_pc=83, length=11, index=1}
                                    Patterns$SimpleBindingPattern$A
                                  3: #_CA_(#_value_=[@#_A_(),@#_A_()]): LOCAL_VARIABLE, {start_pc=112, length=11, index=2}
                                    Patterns$SimpleBindingPattern$CA(
                                      value=[@Patterns$SimpleBindingPattern$A,@Patterns$SimpleBindingPattern$A]
                                    )
                          }""".replace("_A_", A).replace("_CA_", CA).replace("_value_", value);

        if (!expected.equals(out)) {
            throw new AssertionError("Unexpected output:\n" + out + "\nexpected:\n" + expected);
        }
    }

    private String snipCPNumber(String constantPool, String expectedConstant) {
        Matcher m = Pattern.compile("#([0-9]+).*" + Pattern.quote(expectedConstant))
                           .matcher(constantPool);
        if (!m.find()) {
            throw new AssertionError("Cannot find constant pool item");
        }

        return m.group(1);
    }

    /*********************** Test class *************************/
    static class SimpleBindingPattern {
        @Target(ElementType.TYPE_USE)
        @Repeatable(CA.class)
        @interface A {}
        @Target(ElementType.TYPE_USE)
        @interface CA {
            public A[] value();
        }

        private static final Object o = "";
        private static final boolean B1s = o instanceof @A String s && s.isEmpty();
        private static final boolean B1m = o instanceof @A @A String s && s.isEmpty();
        private final boolean B2s = o instanceof @A String s && s.isEmpty();
        private final boolean B2m = o instanceof @A @A String s && s.isEmpty();

        static {
            boolean B3s = o instanceof @A String s && s.isEmpty();
            boolean B3m = o instanceof @A @A String s && s.isEmpty();
        }

        {
            boolean B4s = o instanceof @A String s && s.isEmpty();
            boolean B4m = o instanceof @A @A String s && s.isEmpty();
        }

        {
            boolean B5s = o instanceof @A String s && s.isEmpty();
            boolean B5m = o instanceof @A @A String s && s.isEmpty();
        }

        public SimpleBindingPattern() {
            boolean B6s = o instanceof @A String s && s.isEmpty();
            boolean B6m = o instanceof @A @A String s && s.isEmpty();
        }

        void testPatterns() {
            boolean B7s = o instanceof @A String s && s.isEmpty();
            boolean B7m = o instanceof @A @A String s && s.isEmpty();
        }

        void testPatternsDesugared() {
            @A String s;
            boolean B8s = o instanceof String && (s = (String) o) == s && s.isEmpty();
            boolean B8sx = o instanceof String && (s = (String) o) == s && s.isEmpty();
        }
    }
}
