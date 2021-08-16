/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestEmptyBootstrapMethodsAttr
 * @bug 8041918
 * @library /test/lib
 * @summary Test empty bootstrap_methods table within BootstrapMethods attribute
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @compile emptynumbootstrapmethods1.jcod emptynumbootstrapmethods2.jcod
 * @run driver TestEmptyBootstrapMethodsAttr
 */

import java.io.File;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestEmptyBootstrapMethodsAttr {

    public static void main(String args[]) throws Throwable {
        System.out.println("Regression test for bug 8041918");

        // Test case #1:
        // Try loading class with empty bootstrap_methods table where no
        // other attributes are following BootstrapMethods in attribute table.
        String className = "emptynumbootstrapmethods1";

        // ======= execute test case #1
        // Expect a lack of main method, this implies that the class loaded correctly
        // with an empty bootstrap_methods and did not generate a ClassFormatError.
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(className);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldNotContain("java.lang.ClassFormatError");
        output.shouldContain("Main method not found in class " + className);
        output.shouldHaveExitValue(1);

        // Test case #2:
        // Try loading class with empty bootstrap_methods table where an
        // AnnotationDefault attribute follows the BootstrapMethods in the attribute table.
        className = "emptynumbootstrapmethods2";

        // ======= execute test case #2
        // Expect a lack of main method, this implies that the class loaded correctly
        // with an empty bootstrap_methods and did not generate ClassFormatError.
        pb = ProcessTools.createJavaProcessBuilder(className);
        output = new OutputAnalyzer(pb.start());
        output.shouldNotContain("java.lang.ClassFormatError");
        output.shouldContain("Main method not found in class " + className);
        output.shouldHaveExitValue(1);
    }
}
