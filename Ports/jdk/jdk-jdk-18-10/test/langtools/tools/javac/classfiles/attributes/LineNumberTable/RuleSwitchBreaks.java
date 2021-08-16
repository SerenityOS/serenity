/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8262891
 * @summary Verify correct LineNumberTable for rule switches.
 * @library /tools/lib /tools/javac/lib ../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox InMemoryFileManager TestBase
 * @build LineNumberTestBase TestCase
 * @run main RuleSwitchBreaks
 */

import java.util.List;

public class RuleSwitchBreaks extends LineNumberTestBase {
    public static void main(String[] args) throws Exception {
        new RuleSwitchBreaks().test();
    }

    public void test() throws Exception {
        test(List.of(TEST_CASE));
    }

    private static final TestCase[] TEST_CASE = new TestCase[] {
        new TestCase("""
                     public class Test {                                   // 1
                         private void test(int i) {                        // 2
                             switch (i) {                                  // 3
                                 case 0 ->                                 // 4
                                     System.out.println("a");              // 5
                                 case 1 ->                                 // 6
                                     System.out.println("a");              // 7
                                 default ->                                // 8
                                     System.out.println("default");        // 9
                             }                                             //10
                         }                                                 //11
                     }                                                     //12
                     """,
                     List.of(1, 3, 5, 7, 9, 11),
                     true,
                     List.of(),
                     "Test"),
        new TestCase("""
                     public class TestGuards {                             // 1
                         private void test(Object o) {                     // 2
                             switch (o) {                                  // 3
                                 case String s && s.isEmpty() ->           // 4
                                     System.out.println("a");              // 5
                                 case String s ->                          // 6
                                     System.out.println("a");              // 7
                                 default ->                                // 8
                                     System.out.println("default");        // 9
                             }                                             //10
                         }                                                 //11
                     }                                                     //12
                     """,
                     List.of(1, 3, 4, 5, 6, 7, 9, 11),
                     true,
                     List.of("--enable-preview", "-source", System.getProperty("java.specification.version")),
                     "TestGuards")
    };

}
