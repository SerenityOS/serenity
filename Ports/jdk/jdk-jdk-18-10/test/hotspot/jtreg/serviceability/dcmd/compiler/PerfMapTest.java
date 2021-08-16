/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, Arm Limited. All rights reserved.
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
 * @test PerfMapTest
 * @bug 8254723
 * @requires os.family == "linux"
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @run testng/othervm PerfMapTest
 * @summary Test of diagnostic command Compiler.perfmap
 */

import org.testng.annotations.Test;
import org.testng.Assert;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.JMXExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Call jcmd Compiler.perfmap and check the output file has the expected
 * format.
 */
public class PerfMapTest {

    static final Pattern LINE_PATTERN =
        Pattern.compile("^((?:0x)?\\p{XDigit}+)\\s+((?:0x)?\\p{XDigit}+)\\s+(.*)$");

    public void run(CommandExecutor executor) {
        OutputAnalyzer output = executor.execute("Compiler.perfmap");

        output.stderrShouldBeEmpty();
        output.stdoutShouldBeEmpty();

        final long pid = ProcessHandle.current().pid();
        final Path path = Paths.get(String.format("/tmp/perf-%d.map", pid));

        Assert.assertTrue(Files.exists(path));

        // Sanity check the file contents
        try {
            for (String entry : Files.readAllLines(path)) {
                Matcher m = LINE_PATTERN.matcher(entry);
                Assert.assertTrue(m.matches(), "Invalid file format: " + entry);
            }
        } catch (IOException e) {
            Assert.fail(e.toString());
        }
    }

    @Test
    public void jmx() {
        run(new JMXExecutor());
    }
}
