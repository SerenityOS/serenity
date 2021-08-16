/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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

import static jdk.test.lib.Asserts.assertTrue;
import static jdk.test.lib.Asserts.fail;

import java.io.File;
import java.lang.management.*;
import java.util.List;

import jdk.test.lib.hprof.HprofParser;
import jdk.test.lib.process.ProcessTools;

import com.sun.management.HotSpotDiagnosticMXBean;

/*
 * @test
 * @bug 6455258
 * @summary Sanity test for com.sun.management.HotSpotDiagnosticMXBean.dumpHeap method
 * @library /test/lib
 * @build jdk.test.lib.hprof.*
 * @build jdk.test.lib.hprof.model.*
 * @build jdk.test.lib.hprof.parser.*
 * @build jdk.test.lib.hprof.util.*
 * @run main DumpHeap
 */
public class DumpHeap {

    public static void main(String[] args) throws Exception {
        List<HotSpotDiagnosticMXBean> list = ManagementFactory.getPlatformMXBeans(HotSpotDiagnosticMXBean.class);
        File dump = new File(ProcessTools.getProcessId() + ".hprof");
        if (dump.exists()) {
            dump.delete();
        }
        System.out.println("Dumping to file: " + dump.getAbsolutePath());
        list.get(0).dumpHeap(dump.getAbsolutePath(), true);

        verifyDumpFile(dump);

        dump.delete();
    }

    private static void verifyDumpFile(File dump) {
        assertTrue(dump.exists() && dump.isFile(), "Could not create dump file");
        try {
            HprofParser.parse(dump);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Could not parse dump file");
        }
    }

}
