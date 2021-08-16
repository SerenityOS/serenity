/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Polygon;
import java.awt.image.BufferedImage;

/**
 * @test
 * @bug 8004821
 * @summary Verifies that drawPolygon() works with empty arrays.
 * @author Sergey Bylokhov
 */
public final class Test8004821 {

    public static void main(final String[] args) {
        final int[] arrEmpty = {};
        final int[] arr1elem = {150};
        final BufferedImage bi = new BufferedImage(300, 300,
                                                   BufferedImage.TYPE_INT_RGB);
        final Graphics2D g = (Graphics2D) bi.getGraphics();
        test(g, arrEmpty);
        test(g, arr1elem);
        g.translate(2.0, 2.0);
        test(g, arrEmpty);
        test(g, arr1elem);
        g.scale(2.0, 2.0);
        test(g, arrEmpty);
        test(g, arr1elem);
        g.dispose();
    }

    private static void test(final Graphics2D g, final int[] arr) {
        g.drawPolygon(arr, arr, arr.length);
        g.drawPolygon(new Polygon(arr, arr, arr.length));
        g.fillPolygon(arr, arr, arr.length);
        g.fillPolygon(new Polygon(arr, arr, arr.length));
        g.drawPolyline(arr, arr, arr.length);
    }
}
