/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8139564 8203960
 * @summary defaultmethods=debug should have logging from each of the statements in the code
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver DefaultMethodsTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class DefaultMethodsTest {
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xlog:defaultmethods=debug",
                                                                  InnerClass.class.getName());
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("Slots that need filling:");
        output.shouldContain("requires default method processing");
        output.shouldContain("Looking for default methods for slot ");
        output.shouldContain("Creating defaults and overpasses...");
        output.shouldContain("for slot: ");
        output.shouldContain("Default method processing complete");
        output.shouldContain("overpass methods");
        output.shouldContain("default methods");
        output.shouldHaveExitValue(0);
    }

    interface TestInterface {
        default void doSomething() {
            System.out.println("Default TestInterface");
        }
    }

    public static class InnerClass implements TestInterface {
        // InnerClass implements TestInterface with a default method.
        // Loading of InnerClass will trigger default method processing.
        public static void main(String[] args) throws Exception {
            System.out.println("Inner Class");
        }
    }
}

