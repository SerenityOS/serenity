/*
 * Copyright (c) 2002, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.java.swing.plaf.gtk;

import javax.swing.plaf.synth.ColorType;
import java.awt.Color;
import javax.swing.plaf.ColorUIResource;

/**
 * @author Scott Violet
 */
public class GTKColorType extends ColorType {
    // GTK allows you to specify the foreground and background in a
    // gtkrc, the rest (dark, mid, light) are calculated from these
    // values.
    public static final ColorType LIGHT = new GTKColorType("Light");
    public static final ColorType DARK = new GTKColorType("Dark");
    public static final ColorType MID = new GTKColorType("Mid");
    public static final ColorType BLACK = new GTKColorType("Black");
    public static final ColorType WHITE = new GTKColorType("White");

    public static final int MAX_COUNT;

    private static final float[] HLS_COLORS = new float[3];
    private static final Object HLS_COLOR_LOCK = new Object();

    static {
        MAX_COUNT = WHITE.getID() + 1;
    }

    private static int hlsToRGB(float h, float l, float s) {
        float m2 = (l <= .5f) ? (l * (1 + s)) : (l + s - l * s);
        float m1 = 2.0f * l - m2;
        float r, g, b;

        if (s == 0.0) {
            if (h == 0.0) {
                r = g = b = l;
            }
            else {
                r = g = b = 0;
            }
        }
        else {
            r = hlsValue(m1, m2, h + 120);
            g = hlsValue(m1, m2, h);
            b = hlsValue(m1, m2, h - 120);
        }
        return (((int)(r * 255)) << 16) | (((int)(g * 255.0)) << 8) |
               ((int)(b * 255));
    }

    private static float hlsValue(float n1, float n2, float h) {
        if (h > 360) {
            h -= 360;
        }
        else if (h < 0) {
            h += 360;
        }
        if (h < 60) {
            return n1 + (n2 - n1) * h / 60.0f;
        }
        else if (h < 180) {
            return n2;
        }
        else if (h < 240) {
            return n1 + (n2 - n1) * (240.0f - h) / 60.0f;
        }
        return n1;
    }

    /**
     * Converts from RGB color space to HLS colorspace.
     */
    private static float[] rgbToHLS(int rgb, float[] hls) {
        float r = ((rgb & 0xFF0000) >> 16) / 255.0f;
        float g = ((rgb & 0xFF00) >> 8) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;

        /* calculate lightness */
        float max = Math.max(Math.max(r, g), b);
        float min = Math.min(Math.min(r, g), b);
        float l = (max + min) / 2.0f;
        float s = 0;
        float h = 0;

        if (max != min) {
            float delta = max - min;
            s = (l <= .5f) ? (delta / (max + min)) : (delta / (2.0f - max -min));
            if (r == max) {
                h = (g - b) / delta;
            }
            else if (g == max) {
                h = 2.0f + (b - r) / delta;
            }
            else {
                h = 4.0f + (r - g) / delta;
            }
            h *= 60.0f;
            if (h < 0) {
                h += 360.0f;
            }
        }
        if (hls == null) {
            hls = new float[3];
        }
        hls[0] = h;
        hls[1] = l;
        hls[2] = s;
        return hls;
    }

    /**
     * Creates and returns a new color derived from the passed in color.
     * The transformation is done in the HLS color space using the specified
     * arguments to scale.
     *
     * @param color Color to alter
     * @param hFactor Amount to scale the hue
     * @param lFactor Amount to scale the lightness
     * @param sFactor Amount to sacle saturation
     * @return newly created color
     */
    static Color adjustColor(Color color, float hFactor, float lFactor,
                             float sFactor) {
        float h;
        float l;
        float s;

        synchronized(HLS_COLOR_LOCK) {
            float[] hls = rgbToHLS(color.getRGB(), HLS_COLORS);
            h = hls[0];
            l = hls[1];
            s = hls[2];
        }
        h = Math.min(360, hFactor * h);
        l = Math.min(1, lFactor * l);
        s = Math.min(1, sFactor * s);
        return new ColorUIResource(hlsToRGB(h, l, s));
    }

    protected GTKColorType(String name) {
        super(name);
    }
}
