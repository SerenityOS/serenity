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

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;
import jdk.test.lib.dcmd.PidJcmdExecutor;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * @test
 * @bug 8054890
 * @summary Test of JVMTI.data_dump diagnostic command
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run testng DataDumpDcmdTest
 */

/**
 * This test issues the "JVMTI.data_dump" command which will dump the related JVMTI
 * data.
 *
 */
public class DataDumpDcmdTest {
    public void run(CommandExecutor executor) {
        OutputAnalyzer out = executor.execute("JVMTI.data_dump");

        // stderr should be empty except for VM warnings.
        if (!out.getStderr().isEmpty()) {
            List<String> lines = Arrays.asList(out.getStderr().split("(\\r\\n|\\n|\\r)"));
            Pattern p = Pattern.compile(".*VM warning.*");
            for (String line : lines) {
                Matcher m = p.matcher(line);
                if (!m.matches()) {
                    throw new RuntimeException("Stderr has output other than VM warnings");
                }
            }
        }
    }

    @Test
    public void jmx() throws Throwable {
        run(new JMXExecutor());
    }

    @Test
    public void cli() throws Throwable {
        run(new PidJcmdExecutor());
    }
}
