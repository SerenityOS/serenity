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

final class ColorModelHSV extends ColorModel {

    ColorModelHSV() {
        super("hsv", "Hue", "Saturation", "Value", "Transparency"); // NON-NLS: components
    }

    @Override
    void setColor(int color, float[] space) {
        super.setColor(color, space);
        RGBtoHSV(space, space);
        space[3] = 1.0f - space[3];
    }

    @Override
    int getColor(float[] space) {
        space[3] = 1.0f - space[3];
        HSVtoRGB(space, space);
        return super.getColor(space);
    }

    @Override
    int getMaximum(int index) {
        return (index == 0) ? 360 : 100;
    }

    @Override
    float getDefault(int index) {
        return (index == 0) ? -1.0f : 1.0f;
    }

    /**
     * Converts HSV components of a color to a set of RGB components.
     *
     * @param hsv  a float array with length equal to
     *             the number of HSV components
     * @param rgb  a float array with length of at least 3
     *             that contains RGB components of a color
     * @return a float array that contains RGB components
     */
    private static float[] HSVtoRGB(float[] hsv, float[] rgb) {
        if (rgb == null) {
            rgb = new float[3];
        }
        float hue = hsv[0];
        float saturation = hsv[1];
        float value = hsv[2];

        rgb[0] = value;
        rgb[1] = value;
        rgb[2] = value;

        if (saturation > 0.0f) {
            hue = (hue < 1.0f) ? hue * 6.0f : 0.0f;
            int integer = (int) hue;
            float f = hue - (float) integer;
            switch (integer) {
                case 0:
                    rgb[1] *= 1.0f - saturation * (1.0f - f);
                    rgb[2] *= 1.0f - saturation;
                    break;
                case 1:
                    rgb[0] *= 1.0f - saturation * f;
                    rgb[2] *= 1.0f - saturation;
                    break;
                case 2:
                    rgb[0] *= 1.0f - saturation;
                    rgb[2] *= 1.0f - saturation * (1.0f - f);
                    break;
                case 3:
                    rgb[0] *= 1.0f - saturation;
                    rgb[1] *= 1.0f - saturation * f;
                    break;
                case 4:
                    rgb[0] *= 1.0f - saturation * (1.0f - f);
                    rgb[1] *= 1.0f - saturation;
                    break;
                case 5:
                    rgb[1] *= 1.0f - saturation;
                    rgb[2] *= 1.0f - saturation * f;
                    break;
            }
        }
        return rgb;
    }

    /**
     * Converts RGB components of a color to a set of HSV components.
     *
     * @param rgb  a float array with length of at least 3
     *             that contains RGB components of a color
     * @param hsv  a float array with length equal to
     *             the number of HSV components
     * @return a float array that contains HSV components
     */
    private static float[] RGBtoHSV(float[] rgb, float[] hsv) {
        if (hsv == null) {
            hsv = new float[3];
        }
        float max = ColorModelHSL.max(rgb[0], rgb[1], rgb[2]);
        float min = ColorModelHSL.min(rgb[0], rgb[1], rgb[2]);

        float saturation = max - min;
        if (saturation > 0.0f) {
            saturation /= max;
        }
        hsv[0] = ColorModelHSL.getHue(rgb[0], rgb[1], rgb[2], max, min);
        hsv[1] = saturation;
        hsv[2] = max;
        return hsv;
    }
}
