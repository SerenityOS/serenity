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
 * Compares two images with color mapping defined by {@code ColorModel}
 * implementation.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class ColorImageComparator extends StrictImageComparator {

    ColorMap leftMap, rightMap;
    ImageComparator comparator = null;

    /**
     * Creates a comparator with a color maps. Object created by this
     * constructor behaves like {@code StrictImageComparator}. Object
     * created works faster because it does not create intermediate images for
     * another comparator.
     *
     * @param map Map applied to both left and right images during comparision.
     */
    public ColorImageComparator(ColorMap map) {
        leftMap = map;
        rightMap = map;
    }

    /**
     * Creates a comparator with {@code map} color mapping. Actual
     * comparision perfomed by {@code comparator} parameter.
     *
     * @param map Map applied to both left and right images during comparision.
     * @param subComparator comporator to perform a comparision of to images
     * with mapped colors.
     */
    public ColorImageComparator(ColorMap map, ImageComparator subComparator) {
        this(map);
        this.comparator = subComparator;
    }

    /**
     * Creates a comparator with two color maps. Object created by this
     * constructor behaves like {@code StrictImageComparator}. Object
     * created works faster because it does not create intermediate images for
     * another comparator.
     *
     * @param leftMap Map applied to the left image during comparision.
     * @param rightMap Map applied to the right image during comparision.
     */
    public ColorImageComparator(ColorMap leftMap, ColorMap rightMap) {
        this.leftMap = leftMap;
        this.rightMap = rightMap;
    }

    /**
     * Creates a comparator with two color maps. Actual comparision perfomed by
     * {@code comparator} parameter.
     *
     * @param leftMap Map applied to the left image during comparision.
     * @param rightMap Map applied to the right image during comparision.
     * @param subComparator comporator to perform a comparision of to images
     * with mapped colors.
     */
    public ColorImageComparator(ColorMap leftMap, ColorMap rightMap, ImageComparator subComparator) {
        this(leftMap, rightMap);
        this.comparator = subComparator;
    }

    /**
     * Compares images by {@code ImageComparator} passed into constructor,
     * or itself if no {@code ImageComparator} was passed, processing both
     * images by {@code ColorMap} instance before comparision.
     */
    @Override
    public boolean compare(BufferedImage image1, BufferedImage image2) {
        if (comparator != null) {
            return comparator.compare(recolor(image1, leftMap), recolor(image2, rightMap));
        } else {
            return super.compare(image1, image2);
        }
    }

    private BufferedImage recolor(BufferedImage src, ColorMap map) {
        BufferedImage result = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        for (int x = 0; x < src.getWidth(); x++) {
            for (int y = 0; y < src.getWidth(); y++) {
                result.setRGB(x, y, map.mapColor(src.getRGB(x, y)));
            }
        }
        return result;
    }

    @Override
    protected final boolean compareColors(int rgb1, int rgb2) {
        return leftMap.mapColor(rgb1) == rightMap.mapColor(rgb2);
    }

    /**
     * Interface to map colors during the comparision.
     */
    public static interface ColorMap {

        /**
         * Maps one color into another.
         *
         * @param rgb an original color.
         * @return a converted color.
         */
        public int mapColor(int rgb);
    }

    /**
     * Turns {@code foreground} color to white, other - to black.
     */
    public static class ForegroundColorMap implements ColorMap {

        int foreground;

        /**
         * Constructs a ColorImageComparator$ForegroundColorMap object.
         *
         * @param foreground Foreground color.
         */
        public ForegroundColorMap(int foreground) {
            this.foreground = foreground;
        }

        @Override
        public int mapColor(int rgb) {
            return (rgb == foreground) ? 0xffffff : 0;
        }
    }

    /**
     * Turns {@code background} color to black, left others unchanged.
     */
    public static class BackgroundColorMap implements ColorMap {

        int background;

        /**
         * Constructs a ColorImageComparator$BackgroundColorMap object.
         *
         * @param background Background color.
         */
        public BackgroundColorMap(int background) {
            this.background = background;
        }

        @Override
        public int mapColor(int rgb) {
            return (rgb == background) ? 0 : rgb;
        }
    }

}
