/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8152561
 * @summary Test verifies whether all boundary conditions are checked
 *          properly in TIFFField.createArrayForType().
 * @run     main TIFFCreateArrayForTypeTest
 */

import javax.imageio.plugins.tiff.TIFFField;
import javax.imageio.plugins.tiff.TIFFTag;

public class TIFFCreateArrayForTypeTest {

    static int count = 0;
    static boolean unknownDataType, negativeCount, zeroCount, countNotOne;
    static String errorMsg = "";

    private static void testCase1() {
        // check passing unknown data type to createArrayForType()
        count = 2;
        int dataType = 15;
        try {
            TIFFField.createArrayForType(dataType, count);
        } catch (IllegalArgumentException e) {
            unknownDataType = true;
        } catch (Exception e) {
            // just consume if it throws any other exception.
        }
        if (!unknownDataType) {
            errorMsg = errorMsg + "testCase1 ";
        }
    }

    private static void testCase2() {
        // check passing negative count value for createArrayForType()
        count = -1;
        try {
            TIFFField.createArrayForType(TIFFTag.TIFF_LONG, count);
        } catch (IllegalArgumentException e) {
            negativeCount = true;
        } catch (Exception e) {
            // just consume if it throws any other exception.
        }
        if (!negativeCount) {
            errorMsg = errorMsg + "testCase2 ";
        }
    }

    private static void testCase3() {
        /*
         * check passing zero count value for createArrayForType() with
         * TIFFTag.TIFF_RATIONAL or TIFFTag.TIFF_SRATIONAL data type.
         */
        count = 0;
        try {
            TIFFField.createArrayForType(TIFFTag.TIFF_RATIONAL, count);
        } catch (IllegalArgumentException e) {
            zeroCount = true;
        } catch (Exception e) {
            // just consume if it throws any other exception.
        }
        if (!zeroCount) {
            errorMsg = errorMsg + "testCase3 ";
        }
    }

    private static void testCase4() {
        /*
         * check passing count value other than 1 for createArrayForType() with
         * TIFFTag.TIFF_IFD_POINTER data type.
         */
        count = 2;
        try {
            TIFFField.createArrayForType(TIFFTag.TIFF_IFD_POINTER, count);
        } catch (IllegalArgumentException e) {
            countNotOne = true;
        } catch (Exception e) {
            // just consume if it throws any other exception.
        }
        if (!countNotOne) {
            errorMsg = errorMsg + "testCase4 ";
        }
    }

    public static void main(String[] args) {
        /*
         * test different scenarios where TIFFField.createArrayForType()
         * is required to throw IllegalArgumentException.
         */
        testCase1();
        testCase2();
        testCase3();
        testCase4();
        if ((!unknownDataType) ||
            (!negativeCount) ||
            (!zeroCount) ||
            (!countNotOne))
        {
            throw new RuntimeException(errorMsg + "is/are not throwing"
                    + " required IllegalArgumentException");
        }
    }
}

