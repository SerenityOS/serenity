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
 * @bug 8136930
 * @summary Test that the VM ignores explicitly specified module internal properties.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver IgnoreModulePropertiesTest
 */

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

// Test that the VM ignores module related properties such as "jdk.module.addmods"
// and jdk.module.addreads.0" that can only be set using module options.
public class IgnoreModulePropertiesTest {

    // Test that the specified property and its value are ignored.  If the VM accepted
    // the property and value then an exception would be thrown because the value is
    // bogus for that property.  But, since the property is ignored no exception is
    // thrown.
    public static void testProperty(String prop, String value) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-D" + prop + "=" + value, "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain(" version ");
        output.shouldHaveExitValue(0);

        // Ensure that the property and its value aren't available.
        if (System.getProperty(prop) != null) {
            throw new RuntimeException(
                "Unexpected non-null value for property " + prop);
        }
    }

    // For options of the form "option=value", check that an exception gets thrown for
    // the illegal value and then check that its corresponding property is handled
    // correctly.
    public static void testOption(String option, String value,
                                  String prop, String result) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            option + "=" + value, "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain(result);
        testProperty(prop, value);
    }

    public static void main(String[] args) throws Exception {
        testOption("--add-modules", "java.sqlx", "jdk.module.addmods.0", "java.lang.module.FindException");
        testOption("--limit-modules", "java.sqlx", "jdk.module.limitmods", "java.lang.module.FindException");
        testOption("--add-reads", "xyzz=yyzd", "jdk.module.addreads.0", "WARNING: Unknown module: xyzz");
        testOption("--add-exports", "java.base/xyzz=yyzd", "jdk.module.addexports.0",
                   "WARNING: package xyzz not in java.base");
        testOption("--patch-module", "=d", "jdk.module.patch.0", "Unable to parse --patch-module");
    }
}
