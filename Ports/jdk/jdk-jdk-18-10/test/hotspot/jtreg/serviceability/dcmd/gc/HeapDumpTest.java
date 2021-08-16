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

import java.io.File;
import java.nio.file.Files;
import java.io.IOException;
import java.util.List;

import jdk.test.lib.hprof.HprofParser;
import jdk.test.lib.hprof.model.Snapshot;

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.PidJcmdExecutor;

/*
 * @test
 * @summary Test of diagnostic command GC.heap_dump
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @run testng HeapDumpTest
 */
public class HeapDumpTest {
    protected String heapDumpArgs = "";

    public void run(CommandExecutor executor, boolean overwrite) throws IOException {
        File dump = new File("jcmd.gc.heap_dump." + System.currentTimeMillis() + ".hprof");
        if (!overwrite && dump.exists()) {
            dump.delete();
        } else if (overwrite) {
            dump.createNewFile();
        }

        String cmd = "GC.heap_dump " + (overwrite ? "-overwrite " : "") + heapDumpArgs + " " + dump.getAbsolutePath();
        executor.execute(cmd);

        verifyHeapDump(dump);
        dump.delete();
    }

    private void verifyHeapDump(File dump) {
        Assert.assertTrue(dump.exists() && dump.isFile(), "Could not create dump file " + dump.getAbsolutePath());
        try {
            File out = HprofParser.parse(dump);

            Assert.assertTrue(out != null && out.exists() && out.isFile(), "Could not find hprof parser output file");
            List<String> lines = Files.readAllLines(out.toPath());
            Assert.assertTrue(lines.size() > 0, "hprof parser output file is empty");
            for (String line : lines) {
                Assert.assertFalse(line.matches(".*WARNING(?!.*Failed to resolve object.*constantPoolOop.*).*"));
            }

            out.delete();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Could not parse dump file " + dump.getAbsolutePath());
        }
    }

    /* GC.heap_dump is not available over JMX, running jcmd pid executor instead */
    @Test
    public void pid() throws IOException {
        run(new PidJcmdExecutor(), false);
    }

    @Test
    public void pidRewrite() throws IOException {
        run(new PidJcmdExecutor(), true);
    }
}

