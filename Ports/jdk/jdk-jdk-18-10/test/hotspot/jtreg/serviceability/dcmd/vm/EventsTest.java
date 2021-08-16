/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;
import jdk.test.lib.process.OutputAnalyzer;
import org.testng.annotations.Test;

/*
 * @test
 * @summary Test of diagnostic command VM.events
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @run testng  EventsTest
 */
public class EventsTest {

    static String buildHeaderPattern(String logname) {
        return "^" + logname + ".*\\(\\d+ events\\):";
    }

    public void run_all(CommandExecutor executor) {
        OutputAnalyzer output = executor.execute("VM.events");
        // This tests for the output to contain the event log header line (e.g. "Classes unloaded (0 events):").
        // Those are always printed even if the corresponding event log is empty.
        output.stdoutShouldMatch(buildHeaderPattern("Events"));
        output.stdoutShouldMatch(buildHeaderPattern("Compilation"));
        output.stdoutShouldMatch(buildHeaderPattern("GC Heap History"));
        output.stdoutShouldMatch(buildHeaderPattern("Deoptimization"));
        output.stdoutShouldMatch(buildHeaderPattern("Classes unloaded"));
    }

    public void run_selected(CommandExecutor executor) {
        OutputAnalyzer output = executor.execute("VM.events log=deopt");
        // We only expect one log to be printed here.
        output.stdoutShouldMatch(buildHeaderPattern("Deoptimization"));

        output.stdoutShouldNotMatch(buildHeaderPattern("Events"));
        output.stdoutShouldNotMatch(buildHeaderPattern("Compilation"));
        output.stdoutShouldNotMatch(buildHeaderPattern("GC Heap History"));
        output.stdoutShouldNotMatch(buildHeaderPattern("Classes unloaded"));
    }

    @Test
    public void jmx() {
        run_all(new JMXExecutor());
        run_selected(new JMXExecutor());
    }
}
