/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;
import org.testng.annotations.Test;

/*
 * @test
 * @summary Test of diagnostic command VM.flags
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @run testng/othervm -Xmx129m -XX:+UnlockDiagnosticVMOptions -XX:+IgnoreUnrecognizedVMOptions -XX:+ThereShouldNotBeAnyVMOptionNamedLikeThis_Right FlagsTest
 */
public class FlagsTest {
    public void run(CommandExecutor executor) {
        OutputAnalyzer output = executor.execute("VM.flags");

        /* The following are interpreted by the JVM as actual "flags" */
        output.shouldContain("-XX:+UnlockDiagnosticVMOptions");
        output.shouldContain("-XX:+IgnoreUnrecognizedVMOptions");

        /* The following are not */
        output.shouldNotContain("-Xmx129m");
        output.shouldNotContain("-XX:+ThereShouldNotBeAnyVMOptionNamedLikeThis_Right");
    }

    @Test
    public void jmx() {
        run(new JMXExecutor());
    }
}
