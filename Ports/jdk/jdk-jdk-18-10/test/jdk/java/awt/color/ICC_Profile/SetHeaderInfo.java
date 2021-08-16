/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.util.Arrays;

/**
 * @test
 * @bug 8263622
 * @summary The ICC_Profile#setData invert the order of bytes for the "head" tag
 */
public final class SetHeaderInfo {

    public static void main(String[] args) {
        int[] cspaces = {ColorSpace.CS_sRGB, ColorSpace.CS_LINEAR_RGB,
                         ColorSpace.CS_CIEXYZ, ColorSpace.CS_PYCC,
                         ColorSpace.CS_GRAY};
        for (int cspace : cspaces) {
            ICC_Profile icc = ICC_Profile.getInstance(cspace);
            testSame(icc);
            testCustom(icc);
            // some corner cases
            negative(icc, null);
            negative(icc, new byte[0]);
            negative(icc, new byte[1]);
            byte[] header = icc.getData(ICC_Profile.icSigHead);
            negative(icc, new byte[header.length - 1]);
        }
    }

    private static void testSame(ICC_Profile icc) {
        byte[] expected = icc.getData(ICC_Profile.icSigHead);
        icc.setData(ICC_Profile.icSigHead, expected);
        byte[] actual = icc.getData(ICC_Profile.icSigHead);
        if (!Arrays.equals(expected, actual)) {
            System.err.println("Expected: " + Arrays.toString(expected));
            System.err.println("Actual:   " + Arrays.toString(actual));
            throw new RuntimeException();
        }
    }

    private static void testCustom(ICC_Profile icc) {
        byte[] expected = icc.getData(ICC_Profile.icSigHead);
        // small modification of the default profile
        expected[ICC_Profile.icHdrFlags + 3] = 1;
        expected[ICC_Profile.icHdrModel + 3] = 1;
        icc.setData(ICC_Profile.icSigHead, expected);
        byte[] actual = icc.getData(ICC_Profile.icSigHead);
        if (!Arrays.equals(expected, actual)) {
            System.err.println("Expected: " + Arrays.toString(expected));
            System.err.println("Actual:   " + Arrays.toString(actual));
            throw new RuntimeException();
        }
    }

    private static void negative(ICC_Profile icc, byte[] tagData) {
        try {
            icc.setData(ICC_Profile.icSigHead, tagData);
            throw new RuntimeException("IllegalArgumentException expected");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }
}
