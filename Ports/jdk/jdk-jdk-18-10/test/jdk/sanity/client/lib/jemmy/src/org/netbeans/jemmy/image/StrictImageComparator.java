/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
package org.netbeans.jemmy.image;

import java.awt.image.BufferedImage;

/**
 * Compares two images strictly (i.e. all the pixel colors should match).
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class StrictImageComparator implements ImageComparator {

    /**
     * Checks images sizes and pixels. Compares one pixel after another untill
     * one will be different.
     *
     * @param image1 an image to compare.
     * @param image2 an image to compare.
     * @return True if all the pixels match, false otherwise.
     */
    @Override
    public boolean compare(BufferedImage image1, BufferedImage image2) {
        if (image1.getWidth() != image2.getWidth()
                || image1.getHeight() != image2.getHeight()) {
            return false;
        }
        for (int x = 0; x < image1.getWidth(); x++) {
            for (int y = 0; y < image1.getHeight(); y++) {
                if (!compareColors(image1.getRGB(x, y), image2.getRGB(x, y))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Could be used to override the way of comparing colors.
     *
     * @param rgb1 a color to compare.
     * @param rgb2 a color to compare.
     * @return true if colors are equal.
     */
    protected boolean compareColors(int rgb1, int rgb2) {
        return rgb1 == rgb2;
    }
}
