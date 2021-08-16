/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5089985
 * @summary Test checks that drawing AA lines don't crash the VM
 */

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;

public class AALineTest {

    /* Pairs of endpoints which cause VM crash */

    public static int xBound [][] = {
        {634,25,640,33},
        {634,57,640,65},
        {634,89,640,97},
        {634,121,640,129},
        {634,153,640,161},
        {634,185,640,193},
        {634,217,640,225},
        {634,249,640,257},
        {634,281,640,289},
        {634,313,640,321},
        {634,345,640,353},
        {634,377,640,385},
        {634,409,640,417},
        {634,441,640,449}
    };

    public static int yBound [][] = {
        {25, 634,33, 640},
        {57, 634,65, 640},
        {89, 634,97, 640},
        {121,634,129,640},
        {153,634,161,640},
        {185,634,193,640},
        {217,634,225,640},
        {249,634,257,640},
        {281,634,289,640},
        {313,634,321,640},
        {345,634,353,640},
        {377,634,385,640},
        {409,634,417,640},
        {441,634,449,640}
    };

    public static void main(String[] args) {
        BufferedImage image =
            new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                  RenderingHints.VALUE_ANTIALIAS_ON);

        GeneralPath path = new GeneralPath();
        for(int i=0; i < xBound.length; i++) {
            path.reset();
            path.moveTo(0, 0);
            path.lineTo(xBound[i][0],xBound[i][1]);
            path.lineTo(xBound[i][2],xBound[i][3]);
            path.closePath();
            graphics.draw(path);
        }

        image = new BufferedImage(480, 640, BufferedImage.TYPE_INT_ARGB);

        graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                  RenderingHints.VALUE_ANTIALIAS_ON);

        for(int i=0; i < yBound.length; i++) {
            path.reset();
            path.moveTo(0, 0);
            path.lineTo(yBound[i][0],yBound[i][1]);
            path.lineTo(yBound[i][2],yBound[i][3]);
            path.closePath();
            graphics.draw(path);
        }
    }
}
