/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7167142
 * @summary Warn if unused .hotspot_rc file is present
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver ConfigFileWarning
 */

import java.io.PrintWriter;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Platform;

public class ConfigFileWarning {
    public static void main(String[] args) throws Exception {
        PrintWriter pw;
        ProcessBuilder pb;
        OutputAnalyzer output;

        pw = new PrintWriter("hs_flags.txt");
        pw.println("aaa");
        pw.close();

        pb = ProcessTools.createJavaProcessBuilder("-XX:Flags=hs_flags.txt","-version");
        output = new OutputAnalyzer(pb.start());
        output.shouldContain("Unrecognized VM option 'aaa'");
        output.shouldHaveExitValue(1);

        // Skip on debug builds since we'll always read the file there
        if (!Platform.isDebugBuild()) {
            pw = new PrintWriter(".hotspotrc");
            pw.println("aa");
            pw.close();

            pb = ProcessTools.createJavaProcessBuilder("-version");
            output = new OutputAnalyzer(pb.start());
            output.shouldContain("warning: .hotspotrc file is present but has been ignored.  Run with -XX:Flags=.hotspotrc to load the file.");
        }
    }
}
