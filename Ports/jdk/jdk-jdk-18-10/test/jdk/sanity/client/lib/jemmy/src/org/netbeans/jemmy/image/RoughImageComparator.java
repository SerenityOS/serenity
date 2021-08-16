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
 * Compares two images roughly (i.e. not all of the pixel colors should match).
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class RoughImageComparator implements ImageComparator {

    double roughness = .0;

    /**
     * Creates a comparator with {@code roughness} allowed roughness.
     *
     * @param roughness Allowed comparision roughness.
     */
    public RoughImageComparator(double roughness) {
        this.roughness = roughness;
    }

    /**
     * Compares two images with allowed roughness.
     *
     * @param image1 an image to compare.
     * @param image2 an image to compare.
     * @return true if images have the same sizes and number of unmatching
     * pixels less or equal to      <code>image1.getWidth() * image1.getHeight() * roughness<code>
     */
    @Override
    public boolean compare(BufferedImage image1, BufferedImage image2) {
        if (image1.getWidth() != image2.getWidth()
                || image1.getHeight() != image2.getHeight()) {
            return false;
        }
        double maxRoughPixels = (double) (image1.getWidth() * image1.getHeight()) * roughness;
        int errorCount = 0;
        for (int x = 0; x < image1.getWidth(); x++) {
            for (int y = 0; y < image1.getHeight(); y++) {
                if (image1.getRGB(x, y) != image2.getRGB(x, y)) {
                    errorCount++;
                    if (errorCount > maxRoughPixels) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
