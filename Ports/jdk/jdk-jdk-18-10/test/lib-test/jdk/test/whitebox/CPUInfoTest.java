/*
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

/**
 * @test
 * @library /test/lib /
 *
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   CPUInfoTest
 */

import java.util.Set;
import java.util.List;
import jdk.test.lib.Platform;
import jdk.test.whitebox.WhiteBox;
import jdk.test.whitebox.cpuinfo.CPUInfo;

import static jdk.test.lib.Asserts.*;

public class CPUInfoTest {
    static final WhiteBox WB = WhiteBox.getWhiteBox();

    private static final Set<String> wellKnownCPUFeatures;

    static {
        if (Platform.isX86() || Platform.isX64()) {
            // @formatter:off
            // Checkstyle: stop
            // See hotspot/cpu/x86/vm_version_x86.hpp for the list of supported features.
            wellKnownCPUFeatures = Set.of(
                    "cx8",          "cmov",             "fxsr",              "ht",
                    "mmx",          "3dnowpref",        "sse",               "sse2",
                    "sse3",         "ssse3",            "sse4a",             "sse4.1",
                    "sse4.2",       "popcnt",           "lzcnt",             "tsc",
                    "tscinvbit",    "tscinv",           "avx",               "avx2",
                    "aes",          "erms",             "clmul",             "bmi1",
                    "bmi2",         "rtm",              "adx",               "avx512f",
                    "avx512dq",     "avx512pf",         "avx512er",          "avx512cd",
                    "avx512bw",     "avx512vl",         "sha",               "fma",
                    "vzeroupper",   "avx512_vpopcntdq", "avx512_vpclmulqdq", "avx512_vaes",
                    "avx512_vnni",  "clflush",          "clflushopt",        "clwb",
                    "avx512_vbmi2", "avx512_vbmi",      "hv"
                    );
            // @formatter:on
            // Checkstyle: resume
        } else {
            wellKnownCPUFeatures = null;
        }
    }

    public static void main(String args[]) throws Throwable {
        System.out.println("WB.getCPUFeatures(): \"" + WB.getCPUFeatures() + "\"");

        String additionalCpuInfo = CPUInfo.getAdditionalCPUInfo();
        assertTrue(additionalCpuInfo != null);
        System.out.println("CPUInfo.getAdditionalCPUInfo(): \"" + additionalCpuInfo + "\"");

        List<String> features = CPUInfo.getFeatures();
        assertTrue(features != null);
        System.out.println("CPUInfo.getFeatures(): " + features);

        for (String feature : features) {
            assertTrue(CPUInfo.hasFeature(feature), feature);
        }

        if (wellKnownCPUFeatures != null) {
            System.out.println("Well-known CPU features: " + wellKnownCPUFeatures);
            assertTrue(wellKnownCPUFeatures.containsAll(features), "not all features are known");
        }

        System.out.println("TEST PASSED");
    }
}
