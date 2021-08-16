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

package gc.arguments;

/*
 * @test TestShrinkHeapInSteps
 * @summary Verify that -XX:-ShrinkHeapInSteps works properly.
 * @requires vm.gc != "Z" & vm.gc != "Shenandoah"
 * @library /test/lib
 * @library /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver/timeout=240 gc.arguments.TestShrinkHeapInSteps
 */

import java.util.LinkedList;
import java.util.Arrays;
import jdk.test.lib.Utils;

public class TestShrinkHeapInSteps {
    public static void main(String args[]) throws Exception {
        LinkedList<String> options = new LinkedList<>(
                Arrays.asList(Utils.getFilteredTestJavaOpts("-XX:[^ ]*HeapFreeRatio","-XX:\\+ExplicitGCInvokesConcurrent"))
        );

        // Leverage the existing TestMaxMinHeapFreeRatioFlags test, but pass
        // "false" for the shrinkHeapInSteps argument. This will cause it to
        // run with -XX:-ShrinkHeapInSteps, and only do 1 full GC instead of 10.
        TestMaxMinHeapFreeRatioFlags.positiveTest(10, false, 90, false, false, options);
        TestMaxMinHeapFreeRatioFlags.positiveTest(10, true, 80, false, false, options);
        TestMaxMinHeapFreeRatioFlags.positiveTest(20, false, 70, true, false, options);
        TestMaxMinHeapFreeRatioFlags.positiveTest(25, true, 65, true, false, options);
        TestMaxMinHeapFreeRatioFlags.positiveTest(40, false, 50, false, false, options);
    }
}
