/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1;

/**
 * @test
 * @bug 8169703
 * @summary Verifies that dumping and loading a CDS archive succeeds with AlwaysPreTouch
 * @requires vm.gc.G1
 * @requires vm.cds
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver gc.g1.TestSharedArchiveWithPreTouch
 */

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestSharedArchiveWithPreTouch {
    public static void main(String[] args) throws Exception {
        final String ArchiveFileName = "./SharedArchiveWithPreTouch.jsa";

        final List<String> BaseOptions = Arrays.asList(new String[] {"-XX:+UseG1GC", "-XX:+AlwaysPreTouch",
            "-XX:+UnlockDiagnosticVMOptions", "-XX:SharedArchiveFile=" + ArchiveFileName });

        ProcessBuilder pb;

        List<String> dump_args = new ArrayList<String>(BaseOptions);

        if (Platform.is64bit()) {
          dump_args.addAll(0, Arrays.asList(new String[] { "-XX:+UseCompressedClassPointers", "-XX:+UseCompressedOops" }));
        }
        dump_args.addAll(Arrays.asList(new String[] { "-Xshare:dump", "-Xlog:cds" }));

        pb = ProcessTools.createJavaProcessBuilder(dump_args);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        try {
            output.shouldContain("Loading classes to share");
            output.shouldHaveExitValue(0);

            List<String> load_args = new ArrayList<String>(BaseOptions);

            if (Platform.is64bit()) {
                load_args.addAll(0, Arrays.asList(new String[] { "-XX:+UseCompressedClassPointers", "-XX:+UseCompressedOops" }));
            }
            load_args.addAll(Arrays.asList(new String[] { "-Xshare:on", "-version" }));

            pb = ProcessTools.createJavaProcessBuilder(load_args.toArray(new String[0]));
            output = new OutputAnalyzer(pb.start());
            output.shouldContain("sharing");
            output.shouldHaveExitValue(0);
        } catch (RuntimeException e) {
            // Report 'passed' if CDS was turned off.
            output.shouldContain("Unable to use shared archive");
            output.shouldHaveExitValue(1);
        }
    }
}
