/*
 * Copyright (c) 2021 SAP SE. All rights reserved.
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

import org.testng.annotations.Test;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;
import jdk.test.lib.process.OutputAnalyzer;

/*
 * @test
 * @summary Test of diagnostic command VM.trim_libc_heap
 * @library /test/lib
 * @requires os.family == "linux"
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @run testng TrimLibcHeapTest
 */
public class TrimLibcHeapTest {
    public void run(CommandExecutor executor) {
        OutputAnalyzer output = executor.execute("System.trim_native_heap");
        output.reportDiagnosticSummary();
        output.shouldMatch("(Done|Not available)"); // Not available could happen on Linux + non-glibc (eg. muslc)
        if (output.firstMatch("Done") != null) {
            output.shouldMatch("(Virtual size before|RSS before|Swap before|No details available)");
        }
    }

    @Test
    public void jmx() {
        run(new JMXExecutor());
    }
}
