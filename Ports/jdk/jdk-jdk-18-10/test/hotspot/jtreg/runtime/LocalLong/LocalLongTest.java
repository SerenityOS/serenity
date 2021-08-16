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
 * @test LocalLongTest
 * @bug 8163014
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @requires vm.bits == 64
 * @compile LocalLongHelper.java
 * @run driver LocalLongTest
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class LocalLongTest {
    public static void main(String... args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xint",
                                                                  "--add-opens",
                                                                  "java.base/java.lang=ALL-UNNAMED",
                                                                  "--add-opens",
                                                                  "java.base/java.lang.invoke=ALL-UNNAMED",
                                                                  "LocalLongHelper");
        OutputAnalyzer o = new OutputAnalyzer(pb.start());
        o.shouldHaveExitValue(0);
    };
}
