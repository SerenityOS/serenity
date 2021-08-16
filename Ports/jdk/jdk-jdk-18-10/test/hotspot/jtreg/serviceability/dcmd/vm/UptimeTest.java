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

import org.testng.annotations.Test;
import org.testng.Assert;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;

import java.text.NumberFormat;
import java.text.ParseException;

/*
 * @test
 * @summary Test of diagnostic command VM.uptime
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @run testng UptimeTest
 */
public class UptimeTest {
    public void run(CommandExecutor executor) {
        double someUptime = 1.0;
        long startTime = System.currentTimeMillis();
        try {
            synchronized (this) {
                /* Loop to guard against spurious wake ups */
                while (System.currentTimeMillis() < (startTime + someUptime * 1000)) {
                    wait((int) someUptime * 1000);
                }
            }
        } catch (InterruptedException e) {
            Assert.fail("Test error: Exception caught when sleeping:", e);
        }

        OutputAnalyzer output = executor.execute("VM.uptime");

        output.stderrShouldBeEmpty();

        /*
         * Output should be:
         * [pid]:
         * xx.yyy s
         *
         * If there is only one line in output there is no "[pid]:" printout;
         * skip first line, split on whitespace and grab first half
         */
        int index = output.asLines().size() == 1 ? 0 : 1;
        String uptimeString = output.asLines().get(index).split("\\s+")[0];

        try {
            double uptime = NumberFormat.getNumberInstance().parse(uptimeString).doubleValue();
            if (uptime < someUptime) {
                Assert.fail(String.format(
                        "Test failure: Uptime was less than intended sleep time: %.3f s < %.3f s",
                        uptime, someUptime));
            }
        } catch (ParseException e) {
            Assert.fail("Test failure: Could not parse uptime string: " +
                    uptimeString, e);
        }
    }

    @Test
    public void jmx() {
        run(new JMXExecutor());
    }
}
