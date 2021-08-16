/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.IOException;

/**
  * @test
  * @bug 8072678
  * @author Prasanta Sadhukhan
  */

public class GetMinMaxValue_ICC_ColorSpace {

    public static void main(String[] a) throws Exception {
        ICC_Profile cmyk_profile = ICC_Profile.getInstance(ColorSpace.CS_sRGB);
        ICC_ColorSpace colorSpace = new ICC_ColorSpace(cmyk_profile);
        String minstr = null;
        String maxstr = null;

        colorSpace.fromRGB(new float[]{4.3f,3.1f,2.2f});
        try {
            System.out.println("minvalue " + colorSpace.getMinValue(3));
        } catch (IllegalArgumentException iae) {
            minstr = iae.toString();
        }
        try {
            System.out.println("maxvalue " + colorSpace.getMaxValue(3));
        } catch (IllegalArgumentException iae) {
            maxstr = iae.toString();
        }

        if (minstr.endsWith("+ component") || maxstr.endsWith("+ component")) {
            System.out.println("Test failed");
            throw new RuntimeException("IllegalArgumentException contains incorrect text message");
        } else {
            System.out.println("Test passed");
        }
    }
}
