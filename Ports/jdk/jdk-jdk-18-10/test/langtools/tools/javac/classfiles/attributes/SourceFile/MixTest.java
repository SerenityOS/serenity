/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary sourcefile attribute test for complex structure of nested classes and other types.
 * @bug 8040129
 * @library /tools/lib /tools/javac/lib ../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox InMemoryFileManager TestBase SourceFileTestBase
 * @run main MixTest
 */

public class MixTest extends SourceFileTestBase {
    public static void main(String[] args) throws Exception {
        new InnerClass();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Runnable run = () -> {
                    class Local {
                    }
                };
            }

            class innerInAnonymous {
            }
        };

        new MixTest().run();
    }

    public void run() throws Exception {
        String fileName = getClass().getName() + ".java";
        test("MixTest", fileName);
        test("MixTest$1", fileName);
        test("MixTest$InnerClass", fileName);
        test("MixTest$1$innerInAnonymous", fileName);
        test("MixTest$1$1Local", fileName);
        test("MixTest$InnerClass$innerEnum", fileName);
        test("MixTest$InnerClass$innerInterface", fileName);
        test("MixTest$InnerClass$innerEnum$innerClassInnerEnum", fileName);
        test("MixTest$InnerClass$innerEnum$innerClassInnerEnum$1InnerLocal", fileName);
    }

    static class InnerClass {
        private InnerClass() {
        }

        enum innerEnum {
            E;

            class innerClassInnerEnum {
                void method() {
                    class InnerLocal {
                    }
                }
            }
        }

        @interface innerInterface {
        }
    }
}
