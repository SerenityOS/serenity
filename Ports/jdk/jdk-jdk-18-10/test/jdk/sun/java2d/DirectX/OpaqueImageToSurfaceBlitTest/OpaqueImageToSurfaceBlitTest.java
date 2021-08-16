/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 6764257 8198613
 * @summary Tests that the alpha in opaque images doesn't affect result of alpha
 * compositing
 * @author Dmitri.Trembovetski@sun.com: area=Graphics
 * @run main/othervm OpaqueImageToSurfaceBlitTest
 * @run main/othervm -Dsun.java2d.noddraw=true OpaqueImageToSurfaceBlitTest
 */

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.VolatileImage;

public class OpaqueImageToSurfaceBlitTest {

    public static void main(String[] args) {

        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        VolatileImage vi = gc.createCompatibleVolatileImage(16, 16);
        vi.validate(gc);

        BufferedImage bi =
            new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        int data[] = ((DataBufferInt)bi.getRaster().getDataBuffer()).getData();
        data[0] = 0x0000007f;
        data[1] = 0x0000007f;
        data[2] = 0xff00007f;
        data[3] = 0xff00007f;
        Graphics2D g = vi.createGraphics();
        g.setComposite(AlphaComposite.SrcOver.derive(0.999f));
        g.drawImage(bi, 0, 0, null);

        bi = vi.getSnapshot();
        if (bi.getRGB(0, 0) != bi.getRGB(1, 1)) {
            throw new RuntimeException("Test FAILED: color at 0x0 ="+
                Integer.toHexString(bi.getRGB(0, 0))+" differs from 1x1 ="+
                Integer.toHexString(bi.getRGB(1,1)));
        }

        System.out.println("Test PASSED.");
    }
}
