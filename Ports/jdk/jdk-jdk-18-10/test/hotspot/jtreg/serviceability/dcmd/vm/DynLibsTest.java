/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.Test;
import org.testng.Assert;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Platform;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;

/*
 * @test
 * @summary Test of VM.dynlib diagnostic command via MBean
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @run testng DynLibsTest
 */

public class DynLibsTest {

    public void run(CommandExecutor executor) {
        OutputAnalyzer output = executor.execute("VM.dynlibs");

        String osDependentBaseString = null;
        if (Platform.isAix()) {
            osDependentBaseString = "lib%s.so";
        } else if (Platform.isLinux()) {
            osDependentBaseString = "lib%s.so";
        } else if (Platform.isOSX()) {
            osDependentBaseString = "lib%s.dylib";
        } else if (Platform.isWindows()) {
            osDependentBaseString = "%s.dll";
        }

        if (osDependentBaseString == null) {
            Assert.fail("Unsupported OS");
        }

        output.shouldContain(String.format(osDependentBaseString, "jvm"));
        output.shouldContain(String.format(osDependentBaseString, "java"));
        output.shouldContain(String.format(osDependentBaseString, "management"));
    }

    @Test
    public void jmx() {
        run(new JMXExecutor());
    }
}
