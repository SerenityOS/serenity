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


import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Shape;

/**
 * @test
 * @bug 6206189
 * @summary Verifies passing null to Graphics2D.clip(Shape) throws NPE.
 */
public class TestNullClip {

    public static void main(String[] argv) {
        BufferedImage bi = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D)bi.getGraphics();

        g2d.clip(null); // silently return, no NPE should be thrown

        g2d.setClip(0, 0, 100, 100);
        g2d.setClip(null);
        Shape clip1 = g2d.getClip();
        if (clip1 != null) {
            throw new RuntimeException("Clip is not cleared");
        }
        g2d.setClip(0, 0, 100, 100);
        try {
            g2d.clip(null);
            throw new RuntimeException("NPE is expected");
        } catch (NullPointerException e) {
            //expected
            System.out.println("NPE is thrown");
        }
    }
}
