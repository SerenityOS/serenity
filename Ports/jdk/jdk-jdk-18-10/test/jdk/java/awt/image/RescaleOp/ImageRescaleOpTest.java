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

/*
 * @test
 * @bug 8177393
 * @summary Verify RescaleOp applied to BufferedImages.
 * @run main ImageRescaleOpTest
 */

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import static java.awt.image.BufferedImage.*;
import java.awt.image.RescaleOp;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageRescaleOpTest {

    int w = 10, h = 10;
    float scaleFactor = 0.5f;
    float offset = 0.0f;
    static boolean saveImage = false;

    public static void main(String[] args) throws Exception {
        saveImage = args.length > 0;
        ImageRescaleOpTest test = new ImageRescaleOpTest();
        test.startTest();
    }

    String getFileName(int s, int d) {
        return textFor(s)+"_to_"+textFor(d)+".png";
    }

    String getMsgText(int s, int d) {
        return textFor(s)+"->"+textFor(d)+": ";
    }

    String textFor(int t) {
       switch (t) {
           case TYPE_INT_ARGB        : return "ARGB";
           case TYPE_INT_RGB         : return "RGB";
           case TYPE_4BYTE_ABGR      : return "4BYTEABGR";
           case TYPE_3BYTE_BGR       : return "3BYTEBGR";
           case TYPE_USHORT_555_RGB  : return "USHORT_555_RGB";
           case TYPE_USHORT_565_RGB  : return "USHORT_565_RGB";
           case TYPE_USHORT_GRAY     : return "USHORT_GRAY";
           default                   : return "OTHER";
       }
    }

    private void startTest() throws Exception {

        int expect = 0xff7f7f7f;
        runTest(TYPE_INT_RGB, TYPE_INT_RGB, expect);
        runTest(TYPE_INT_ARGB, TYPE_INT_ARGB, expect);
        runTest(TYPE_INT_ARGB, TYPE_INT_RGB, expect);
        runTest(TYPE_INT_RGB, TYPE_INT_ARGB, expect);

        runTest(TYPE_3BYTE_BGR, TYPE_3BYTE_BGR, expect);
        runTest(TYPE_3BYTE_BGR, TYPE_4BYTE_ABGR, expect);
        runTest(TYPE_4BYTE_ABGR, TYPE_3BYTE_BGR, expect);
        runTest(TYPE_4BYTE_ABGR, TYPE_4BYTE_ABGR, expect);

        /* Slightly different values here due to limited precision */
        runTest(TYPE_USHORT_555_RGB, TYPE_USHORT_555_RGB, 0xff7b7b7b);
        runTest(TYPE_USHORT_565_RGB, TYPE_USHORT_565_RGB, 0xff7b7d7b);

        /* 565->555 and 555->565 results are wrong as the slow code
         * path used is not accounting for the difference in the range.
         */
        //runTest(TYPE_USHORT_555_RGB, TYPE_USHORT_565_RGB, expect);
        //runTest(TYPE_USHORT_565_RGB, TYPE_USHORT_555_RGB, expect);

        runTest(TYPE_USHORT_GRAY, TYPE_USHORT_GRAY, 0xffbcbcbc);

    }

   private void check(BufferedImage bi, int expect, String msg) {
        int argb = bi.getRGB(w-1, h-1);
        System.out.println(msg + Integer.toHexString(argb));
        if (argb != expect) {
            throw new RuntimeException(msg +
                   " expected " + Integer.toHexString(expect) +
                   " but got " + Integer.toHexString(argb));
        }
    }

    private void runTest(int sType, int dType, int expect) {

        BufferedImage src  = new BufferedImage(w, h, sType);
        BufferedImage dst  = new BufferedImage(w, h, dType);
        String msg = getMsgText(sType, dType);

        Graphics2D g2d = src.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, w, h);
        RescaleOp res = new RescaleOp(scaleFactor, offset, null);
        res.filter(src, dst);
        if (saveImage) {
            try {
               String fname = getFileName(sType, dType);
               ImageIO.write(dst, "png", new File(fname));
            } catch (IOException e) {
            }
        }
        check(dst, expect, msg);
   }
}
