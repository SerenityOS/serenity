/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test writeable VM Options with ranges.
 * @library /test/lib /runtime/CommandLine/OptionsValidation/common
 * @modules java.base/jdk.internal.misc
 *          jdk.attach/sun.tools.attach
 *          java.management
 * @run main/othervm -XX:MinHeapFreeRatio=0 -XX:MaxHeapFreeRatio=100 -Djdk.attach.allowAttachSelf TestOptionsWithRangesDynamic
 */

import java.util.List;
import jdk.test.lib.Asserts;
import optionsvalidation.JVMOption;
import optionsvalidation.JVMOptionsUtils;

public class TestOptionsWithRangesDynamic {

    private static List<JVMOption> allWriteableOptions;

    private static void excludeTestRange(String optionName) {
        for (JVMOption option: allWriteableOptions) {
            if (option.getName().equals(optionName)) {
                option.excludeTestMinRange();
                option.excludeTestMaxRange();
                break;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int failedTests;

        /* Get only writeable options */
        allWriteableOptions = JVMOptionsUtils.getOptionsWithRange(origin -> (origin.contains("manageable") || origin.contains("rw")));

        /*
         * Exclude SoftMaxHeapSize as its valid range is only known at runtime.
         */
        excludeTestRange("SoftMaxHeapSize");

        Asserts.assertGT(allWriteableOptions.size(), 0, "Options with ranges not found!");

        System.out.println("Test " + allWriteableOptions.size() + " writeable options with ranges. Start test!");

        failedTests = JVMOptionsUtils.runDynamicTests(allWriteableOptions);

        failedTests += JVMOptionsUtils.runJcmdTests(allWriteableOptions);

        failedTests += JVMOptionsUtils.runAttachTests(allWriteableOptions);

        Asserts.assertEQ(failedTests, 0,
                String.format("%d tests failed! %s", failedTests, JVMOptionsUtils.getMessageWithFailures()));
    }
}
