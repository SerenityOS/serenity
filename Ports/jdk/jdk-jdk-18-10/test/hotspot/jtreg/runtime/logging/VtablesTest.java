/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8141564
 * @summary vtables=trace should have logging from each of the statements in the code
 * @library /test/lib
 * @requires vm.debug
 * @compile ClassB.java
 *          p1/A.java
 *          p2/B.jcod
 *          p1/C.java
 *          p2/D.java
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver VtablesTest
 */

import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class VtablesTest {
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xlog:vtables=trace", "ClassB");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("copy vtable from ClassA to ClassB");
        output.shouldContain("Initializing: ClassB");
        output.shouldContain("adding ClassB.Method1()V");
        output.shouldContain("] overriding with ClassB.Method2()V");
        output.shouldContain("invokevirtual resolved method: caller-class:ClassB");
        output.shouldContain("invokevirtual selected method: receiver-class:ClassB");
        output.shouldContain("NOT overriding with p2.D.nooverride()V");
        output.shouldHaveExitValue(0);

        pb = ProcessTools.createJavaProcessBuilder("-Xlog:vtables=trace", "p1/C");
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("transitive overriding superclass ");
        output.shouldHaveExitValue(0);
    }
}

