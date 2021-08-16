/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.colorchooser;

final class ColorModelCMYK extends ColorModel {

    ColorModelCMYK() {
        super("cmyk", "Cyan", "Magenta", "Yellow", "Black", "Alpha"); // NON-NLS: components
    }

    @Override
    void setColor(int color, float[] space) {
        super.setColor(color, space);
        space[4] = space[3];
        RGBtoCMYK(space, space);
    }

    @Override
    int getColor(float[] space) {
        CMYKtoRGB(space, space);
        space[3] = space[4];
        return super.getColor(space);
    }

    /**
     * Converts CMYK components of a color to a set of RGB components.
     *
     * @param cmyk  a float array with length equal to
     *              the number of CMYK components
     * @param rgb   a float array with length of at least 3
     *              that contains RGB components of a color
     * @return a float array that contains RGB components
     */
    private static float[] CMYKtoRGB(float[] cmyk, float[] rgb) {
        if (rgb == null) {
            rgb = new float[3];
        }
        rgb[0] = 1.0f + cmyk[0] * cmyk[3] - cmyk[3] - cmyk[0];
        rgb[1] = 1.0f + cmyk[1] * cmyk[3] - cmyk[3] - cmyk[1];
        rgb[2] = 1.0f + cmyk[2] * cmyk[3] - cmyk[3] - cmyk[2];
        return rgb;
    }

    /**
     * Converts RGB components of a color to a set of CMYK components.
     *
     * @param rgb   a float array with length of at least 3
     *              that contains RGB components of a color
     * @param cmyk  a float array with length equal to
     *              the number of CMYK components
     * @return a float array that contains CMYK components
     */
    private static float[] RGBtoCMYK(float[] rgb, float[] cmyk) {
        if (cmyk == null) {
            cmyk = new float[4];
        }
        float max = ColorModelHSL.max(rgb[0], rgb[1], rgb[2]);
        if (max > 0.0f) {
            cmyk[0] = 1.0f - rgb[0] / max;
            cmyk[1] = 1.0f - rgb[1] / max;
            cmyk[2] = 1.0f - rgb[2] / max;
        }
        else {
            cmyk[0] = 0.0f;
            cmyk[1] = 0.0f;
            cmyk[2] = 0.0f;
        }
        cmyk[3] = 1.0f - max;
        return cmyk;
    }
}
