/*
 * Copyright (c) 2012, Red Hat, Inc.
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7170638
 * @summary Test SDT probes available on GNU/Linux when DTRACE_ENABLED
 * @requires os.family == "linux"
 * @requires vm.flagless
 *
 * @library /test/lib
 * @run driver SDTProbesGNULinuxTest
 */

import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jtreg.SkippedException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SDTProbesGNULinuxTest {
    public static void main(String[] args) throws Throwable {
        {
            var pb = ProcessTools.createJavaProcessBuilder(
                    "-XX:+ExtendedDTraceProbes",
                    "-version");
            var oa = new OutputAnalyzer(pb.start());
            // This test only matters when build with DTRACE_ENABLED.
            if (oa.getExitValue() != 0) {
                throw new SkippedException("Not build using DTRACE_ENABLED");
            }
        }

        try (var libjvms = Files.walk(Paths.get(Utils.TEST_JDK))) {
            libjvms.filter(p -> "libjvm.so".equals(p.getFileName().toString()))
                   .map(Path::toAbsolutePath)
                   .forEach(SDTProbesGNULinuxTest::testLibJvm);
        }
    }

    private static void testLibJvm(Path libjvm) {
        System.out.println("Testing " + libjvm);
        // We could iterate over all SDT probes and test them individually
        // with readelf -n, but older readelf versions don't understand them.
        try {
            ProcessTools.executeCommand("readelf", "-S", libjvm.toString())
                        .shouldHaveExitValue(0)
                        .stdoutShouldContain(".note.stapsd");
        } catch (Throwable t) {
            throw new Error(t);
        }
    }
}
