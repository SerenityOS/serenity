/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8244778
 * @summary Make sure the archived mirrors of the primitive classes have the proper module (java.base)
 * @requires vm.cds
 * @library /test/lib
 * @run driver PrimitiveClassMirrors
 */

import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.process.OutputAnalyzer;

public class PrimitiveClassMirrors {
    public static void main(String[] args) throws Exception {
        CDSOptions opts = new CDSOptions();

        CDSTestUtils.createArchiveAndCheck(opts);
        opts.setUseVersion(false);
        opts.addSuffix("-Xlog:cds=warning", "PrimitiveClassMirrors$TestApp");
        OutputAnalyzer out = CDSTestUtils.runWithArchive(opts);
        out.shouldHaveExitValue(0);

        // The test should have same results if CDS is turned off
        opts.setXShareMode("off");
        OutputAnalyzer out2 = CDSTestUtils.runWithArchive(opts);
        out2.shouldHaveExitValue(0);
    }

    static class TestApp {
        public static void main(String args[]) throws Exception {
            Class classes[] = {
                int.class,
                float.class,
                double.class,
                byte.class,
                boolean.class,
                char.class,
                long.class,
                short.class,
                void.class,

                int[].class,
                float[].class,
                double[].class,
                byte[].class,
                boolean[].class,
                char[].class,
                long[].class,
                short[].class,
            };

            for (Class c : classes) {
                test(c);
            }
        }
    }

    static void test(Class c) throws Exception {
        Module m = c.getModule();
        boolean unexpected = (m == null || !("java.base".equals(m.getName())));
        System.out.println("Module for " + c + " = " + m + (unexpected ? " *** Error" : ""));
        if (unexpected) {
            throw new RuntimeException("Unexpected: " + m);
        }
    }
}
