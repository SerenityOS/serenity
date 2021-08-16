/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6773022
 * @summary Test verifies that SampleModel.getDataElements() throws an appropriate
 *           exception if coordinates are not in bounds.
 *
 * @run     main GetDataElementsTest
 */

import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;

public class GetDataElementsTest {

    public static int width = 100;
    public static int height = 100;
    public static int dataType = DataBuffer.TYPE_BYTE;
    public static int numBands = 4;

    public static void main(String[] args) {
        SampleModel sm = new ComponentSampleModel(dataType, width, height, 4, width * 4, new int[] { 0, 1, 2, 3 } );

        DataBuffer db = sm.createDataBuffer();
        Object o = null;

        boolean testPassed = false;
        try {
            o = sm.getDataElements(Integer.MAX_VALUE, 0, 1, 1, o, db);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println(e.getMessage());
            testPassed = true;
        }

        if (!testPassed) {
            throw new RuntimeException("Excpected excprion was not thrown.");
        }
    }
}
