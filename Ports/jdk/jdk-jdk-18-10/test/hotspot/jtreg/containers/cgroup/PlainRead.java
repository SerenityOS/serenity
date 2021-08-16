/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test PlainRead
 * @key cgroups
 * @requires os.family == "linux"
 * @requires vm.flagless
 * @library /testlibrary /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI PlainRead
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Platform;
import sun.hotspot.WhiteBox;

public class PlainRead {

    static public void match(OutputAnalyzer oa, String what, String value) {
       oa.shouldMatch("^.*" + what + " *" + value + ".*$");
    }

    static public void noMatch(OutputAnalyzer oa, String what, String value) {
       oa.shouldNotMatch("^.*" + what + " *" + value + ".*$");
    }

    static final String good_value = "(\\d+|-1|-2|Unlimited)";
    static final String bad_value = "(failed)";

    static final String[] variables = {"Memory Limit is:", "CPU Shares is:", "CPU Quota is:", "CPU Period is:", "active_processor_count:"};

    static public void isContainer(OutputAnalyzer oa) {
        for (String v: variables) {
            match(oa, v, good_value);
        }
        for (String v: variables) {
            noMatch(oa, v, bad_value);
        }
    }

    static public void isNotContainer(OutputAnalyzer oa) {
       oa.shouldMatch("^.*Can't open /proc/self/mountinfo.*$");
    }

    public static void main(String[] args) throws Exception {
        WhiteBox wb = WhiteBox.getWhiteBox();
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-Xlog:os+container=trace", "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        if (wb.isContainerized()) {
            System.out.println("Inside a cgroup, testing...");
            isContainer(output);
        }
    }
}
