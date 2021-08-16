/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package build.tools.icondata.awt;

import java.io.*;
import java.awt.image.*;
import javax.imageio.*;
import java.awt.*;

public class ToBin {
    public static void main(String[] args) throws Exception {
        BufferedImage im = ImageIO.read(System.in);
        BufferedImage bi = null;
        int iconWidth = im.getWidth(null);
        int iconHeight = im.getHeight(null);
        if (im != null && iconHeight != 0 && iconWidth != 0) {
            bi = new BufferedImage(iconWidth, iconHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics g = bi.getGraphics();
            try {
                g.drawImage(im, 0, 0, iconWidth, iconHeight, null);
            } finally {
                g.dispose();
            }
        }
        DataBuffer srcBuf = bi.getData().getDataBuffer();
        int[] buf = ((DataBufferInt)srcBuf).getData();
        System.out.print(iconWidth + ",");
        System.out.println(iconHeight + ",");
        for (int i = 0; i < buf.length; i++) {
            System.out.print("0x" + Integer.toHexString(buf[i]) + ", ");
            if (i % 10 == 0) {
                System.out.println();
            }
        }
    }
}
