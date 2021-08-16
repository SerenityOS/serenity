/*
 * Copyright 2011 Red Hat, Inc.  All Rights Reserved.
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @bug 7049339
  @summary Copying images with a non-rectangular clip and a custom composite
           fails
  @author Denis Lila <dlila@redhat.com>
  @run main Bug7049339
 */

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

public class Bug7049339 {
    public static void main(String[] argv) {
        int x = 100, y = 100;
        BufferedImage src = new BufferedImage(x, y, BufferedImage.TYPE_INT_ARGB);
        BufferedImage dst = new BufferedImage(x, y, BufferedImage.TYPE_3BYTE_BGR);

        Graphics2D dstg2d = dst.createGraphics();
        dstg2d.setComposite(new Composite() {
            @Override
            public CompositeContext createContext(
                    ColorModel srcColorModel,
                    ColorModel dstColorModel,
                    RenderingHints hints)
            {
                return new CompositeContext() {
                    @Override
                    public void compose(Raster src, Raster dstIn,
                            WritableRaster dstOut)
                    {
                        // do nothing
                    }
                    @Override
                    public void dispose() {
                    }
                };
            }
        });
        Shape clip = new Ellipse2D.Double(x/4, y/4, x/2, y/2);
        dstg2d.setClip(clip);
        // This will throw a RasterFormatException if the bug is present.
        dstg2d.drawImage(src, 0, 0, null);
    }
}
