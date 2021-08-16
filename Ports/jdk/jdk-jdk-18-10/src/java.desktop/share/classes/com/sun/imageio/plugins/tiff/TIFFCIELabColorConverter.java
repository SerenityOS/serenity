/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.imageio.plugins.tiff;

public class TIFFCIELabColorConverter extends TIFFColorConverter {

    // XYZ coordinate or reference white (CIE D65)
    private static final float Xn = 95.047f;
    private static final float Yn = 100.0f;
    private static final float Zn = 108.883f;

    private static final float THRESHOLD = (float)Math.pow(0.008856, 1.0/3.0);

    public TIFFCIELabColorConverter() {}


    private float clamp(float x) {
        if (x < 0.0f) {
            return 0.0f;
        } else if (x > 100.0f) {
            return 255.0f;
        } else {
            return x*(255.0f/100.0f);
        }
    }

    private float clamp2(float x) {
        if (x < 0.0f) {
            return 0.0f;
        } else if (x > 255.0f) {
            return 255.0f;
        } else {
            return x;
        }
    }

    public void fromRGB(float r, float g, float b, float[] result) {
        float X =  0.412453f*r + 0.357580f*g + 0.180423f*b;
        float Y =  0.212671f*r + 0.715160f*g + 0.072169f*b;
        float Z =  0.019334f*r + 0.119193f*g + 0.950227f*b;

        float YYn = Y/Yn;
        float XXn = X/Xn;
        float ZZn = Z/Zn;

        if (YYn < 0.008856f) {
            YYn = 7.787f*YYn + 16.0f/116.0f;
        } else {
            YYn = (float)Math.pow(YYn, 1.0/3.0);
        }

        if (XXn < 0.008856f) {
            XXn = 7.787f*XXn + 16.0f/116.0f;
        } else {
            XXn = (float)Math.pow(XXn, 1.0/3.0);
        }

        if (ZZn < 0.008856f) {
            ZZn = 7.787f*ZZn + 16.0f/116.0f;
        } else {
            ZZn = (float)Math.pow(ZZn, 1.0/3.0);
        }

        float LStar = 116.0f*YYn - 16.0f;
        float aStar = 500.0f*(XXn - YYn);
        float bStar = 200.0f*(YYn - ZZn);

        LStar *= 255.0f/100.0f;
        if (aStar < 0.0f) {
            aStar += 256.0f;
        }
        if (bStar < 0.0f) {
            bStar += 256.0f;
        }

        result[0] = clamp2(LStar);
        result[1] = clamp2(aStar);
        result[2] = clamp2(bStar);
    }

    public void toRGB(float x0, float x1, float x2, float[] rgb) {
        float LStar = x0*100.0f/255.0f;
        float aStar = (x1 > 128.0f) ? (x1 - 256.0f) : x1;
        float bStar = (x2 > 128.0f) ? (x2 - 256.0f) : x2;

        float YYn; // Y/Yn
        float fY; // 'F' value for Y

        if (LStar < 8.0f) {
            YYn = LStar/903.3f;
            fY = 7.787f*YYn + 16.0f/116.0f;
        } else {
            float YYn_cubeRoot = (LStar + 16.0f)/116.0f;
            YYn = YYn_cubeRoot*YYn_cubeRoot*YYn_cubeRoot;
            fY = (float)Math.pow(YYn, 1.0/3.0);
        }
        float Y = YYn*Yn;

        float fX = fY + (aStar/500.0f);
        float X;
        if (fX <= THRESHOLD) {
            X = Xn*(fX - 16.0f/116.0f)/7.787f;
        } else {
            X = Xn*fX*fX*fX;
        }

        float fZ = fY - bStar/200.0f;
        float Z;
        if (fZ <= THRESHOLD) {
            Z = Zn*(fZ - 16.0f/116.0f)/7.787f;
        } else {
            Z = Zn*fZ*fZ*fZ;
        }

        float R =  3.240479f*X - 1.537150f*Y - 0.498535f*Z;
        float G = -0.969256f*X + 1.875992f*Y + 0.041556f*Z;
        float B =  0.055648f*X - 0.204043f*Y + 1.057311f*Z;

        rgb[0] = clamp(R);
        rgb[1] = clamp(G);
        rgb[2] = clamp(B);
    }
}
