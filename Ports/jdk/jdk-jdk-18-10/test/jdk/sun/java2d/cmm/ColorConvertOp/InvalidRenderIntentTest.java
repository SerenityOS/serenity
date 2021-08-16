/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     7064516
 * @summary Test verifies that incorrect profile rendering intent
 *           does not cause an failure of color conversion op.
 * @run     main InvalidRenderIntentTest
 */

import java.awt.color.CMMException;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.ColorConvertOp;
import java.awt.image.BufferedImage;

import static java.awt.color.ColorSpace.CS_sRGB;
import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;

public class InvalidRenderIntentTest {

    public static void main(String[] args) {
        ICC_Profile pSRGB = ICC_Profile.getInstance(CS_sRGB);

        byte[] raw_data = pSRGB.getData();

        setRenderingIntent(0x1000000, raw_data);

        ICC_Profile p = ICC_Profile.getInstance(raw_data);

        ICC_ColorSpace cs = new ICC_ColorSpace(p);

        // perfrom test color conversion
        ColorConvertOp op = new ColorConvertOp(cs,
                ColorSpace.getInstance(CS_sRGB), null);
        BufferedImage src = new BufferedImage(1, 1, TYPE_3BYTE_BGR);
        BufferedImage dst = new BufferedImage(1, 1, TYPE_3BYTE_BGR);

        try {
            op.filter(src.getRaster(), dst.getRaster());
        } catch (CMMException e) {
            throw new RuntimeException("Test failed.", e);
        }
        System.out.println("Test passed.");
    }

    private static void setRenderingIntent(int intent, byte[] data) {
        final int pos = ICC_Profile.icHdrRenderingIntent;

        data[pos + 0] = (byte) (0xff & (intent >> 24));
        data[pos + 1] = (byte) (0xff & (intent >> 16));
        data[pos + 2] = (byte) (0xff & (intent >> 8));
        data[pos + 3] = (byte) (0xff & (intent));
    }
}
