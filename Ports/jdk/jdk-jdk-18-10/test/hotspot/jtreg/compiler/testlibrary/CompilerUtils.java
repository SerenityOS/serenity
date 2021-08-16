/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.testlibrary;

import java.util.Arrays;
import jdk.test.lib.Asserts;
import jdk.test.lib.Platform;
import sun.hotspot.WhiteBox;

import java.util.stream.IntStream;

public class CompilerUtils {

    private CompilerUtils() {
        // to prevent from instantiation
    }

    /**
     * Returns available compilation levels
     *
     * @return int array with compilation levels
     */
    public static int[] getAvailableCompilationLevels() {
        if (!WhiteBox.getWhiteBox().getBooleanVMFlag("UseCompiler")) {
            return new int[0];
        }
        if (WhiteBox.getWhiteBox().getBooleanVMFlag("TieredCompilation")) {
            Long flagValue = WhiteBox.getWhiteBox()
                    .getIntxVMFlag("TieredStopAtLevel");
            int maxLevel = flagValue.intValue();
            Asserts.assertEQ(new Long(maxLevel), flagValue,
                    "TieredStopAtLevel has value out of int capacity");
            return IntStream.rangeClosed(1, maxLevel).toArray();
        } else {
            if (Platform.isServer() && !Platform.isEmulatedClient()) {
                return new int[]{4};
            }
            if (Platform.isClient() || Platform.isMinimal() || Platform.isEmulatedClient()) {
                return new int[]{1};
            }
        }
        return new int[0];
    }

    /**
     * Returns maximum compilation level available
     * @return an int value representing maximum compilation level available
     */
    public static int getMaxCompilationLevel() {
        return Arrays.stream(getAvailableCompilationLevels())
                .max()
                .getAsInt();
    }
}
