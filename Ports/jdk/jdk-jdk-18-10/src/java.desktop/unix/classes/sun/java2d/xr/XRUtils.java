/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.xr;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.MultipleGradientPaint.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import sun.java2d.loops.*;
import static java.awt.AlphaComposite.*;

/**
 * XRender constants and utility methods.
 *
 * @author Clemens Eisserer
 */

public class XRUtils {
    public static final int None = 0;

    /* Composition Operators */
    public static final byte PictOpClear = 0;
    public static final byte PictOpSrc = 1;
    public static final byte PictOpDst = 2;
    public static final byte PictOpOver = 3;
    public static final byte PictOpOverReverse = 4;
    public static final byte PictOpIn = 5;
    public static final byte PictOpInReverse = 6;
    public static final byte PictOpOut = 7;
    public static final byte PictOpOutReverse = 8;
    public static final byte PictOpAtop = 9;
    public static final byte PictOpAtopReverse = 10;
    public static final byte PictOpXor = 11;
    public static final byte PictOpAdd = 12;
    public static final byte PictOpSaturate = 13;

    /* Repeats */
    public static final int RepeatNone = 0;
    public static final int RepeatNormal = 1;
    public static final int RepeatPad = 2;
    public static final int RepeatReflect = 3;

    /* Interpolation qualities */
    public static final int FAST = 0;
    public static final int GOOD = 1;
    public static final int BEST = 2;
    public static final byte[] FAST_NAME = "fast".getBytes();
    public static final byte[] GOOD_NAME = "good".getBytes();
    public static final byte[] BEST_NAME = "best".getBytes();

    /* PictFormats */
    public static final int PictStandardARGB32 = 0;
    public static final int PictStandardRGB24 = 1;
    public static final int PictStandardA8 = 2;
    public static final int PictStandardA4 = 3;
    public static final int PictStandardA1 = 4;

    /**
     * Maps the specified affineTransformOp to the corresponding XRender image
     * filter.
     */
    public static int ATransOpToXRQuality(int affineTranformOp) {

        switch (affineTranformOp) {
        case AffineTransformOp.TYPE_NEAREST_NEIGHBOR:
            return FAST;

        case AffineTransformOp.TYPE_BILINEAR:
            return GOOD;

        case AffineTransformOp.TYPE_BICUBIC:
            return BEST;
        }

        return -1;
    }

    /**
     * Maps the specified affineTransformOp to the corresponding XRender image
     * filter.
     */
    public static byte[] ATransOpToXRQualityName(int affineTranformOp) {

        switch (affineTranformOp) {
        case AffineTransformOp.TYPE_NEAREST_NEIGHBOR:
            return FAST_NAME;

        case AffineTransformOp.TYPE_BILINEAR:
            return GOOD_NAME;

        case AffineTransformOp.TYPE_BICUBIC:
            return BEST_NAME;
        }

        return null;
    }


    public static byte[] getFilterName(int filterType) {
        switch (filterType) {
        case FAST:
            return FAST_NAME;
        case GOOD:
            return GOOD_NAME;
        case BEST:
            return BEST_NAME;
        }

        return null;
    }


    /**
     * Returns the XRender picture Format which is required to fullfill the
     * Java2D transparency requirement.
     */
    public static int getPictureFormatForTransparency(int transparency) {
        switch (transparency) {
        case Transparency.OPAQUE:
            return PictStandardRGB24;

        case Transparency.BITMASK:
        case Transparency.TRANSLUCENT:
            return PictStandardARGB32;
        }

        return -1;
    }


    public static SurfaceType getXRSurfaceTypeForTransparency(int transparency) {
        if (transparency == Transparency.OPAQUE) {
            return SurfaceType.IntRgb;
        }else {
            return SurfaceType.IntArgbPre;
        }
    }

    /**
     * Maps Java2D CycleMethod to XRender's Repeat property.
     */
    public static int getRepeatForCycleMethod(CycleMethod cycleMethod) {
        if (cycleMethod.equals(CycleMethod.NO_CYCLE)) {
            return RepeatPad;
        } else if (cycleMethod.equals(CycleMethod.REFLECT)) {
            return RepeatReflect;
        } else if (cycleMethod.equals(CycleMethod.REPEAT)) {
            return RepeatNormal;
        }

        return RepeatNone;
    }

    /**
     * Converts a double into an XFixed.
     */
    public static int XDoubleToFixed(double dbl) {
        return (int) (dbl * 65536);
    }

    public static double XFixedToDouble(int fixed) {
        return ((double) fixed) / 65536;
    }

    public static int[] convertFloatsToFixed(float[] values) {
        int[] fixed = new int[values.length];

        for (int i = 0; i < values.length; i++) {
            fixed[i] = XDoubleToFixed(values[i]);
        }

        return fixed;
    }

    public static long intToULong(int signed) {
        if (signed < 0) {
            return ((long) signed) + (((long) Integer.MAX_VALUE) -
                    ((long) Integer.MIN_VALUE) + 1);
        }

        return signed;
    }

    /**
     * Maps the specified Java2D composition rule, to the corresponding XRender
     * composition rule.
     */
    public static byte j2dAlphaCompToXR(int j2dRule) {
        switch (j2dRule) {
        case CLEAR:
            return PictOpClear;

        case SRC:
            return PictOpSrc;

        case DST:
            return PictOpDst;

        case SRC_OVER:
            return PictOpOver;

        case DST_OVER:
            return PictOpOverReverse;

        case SRC_IN:
            return PictOpIn;

        case DST_IN:
            return PictOpInReverse;

        case SRC_OUT:
            return PictOpOut;

        case DST_OUT:
            return PictOpOutReverse;

        case SRC_ATOP:
            return PictOpAtop;

        case DST_ATOP:
            return PictOpAtopReverse;

        case XOR:
            return PictOpXor;
        }

        throw new InternalError("No XRender equivalent available for requested java2d composition rule: "+j2dRule);
    }

    public static short clampToShort(int x) {
        return (short) (x > Short.MAX_VALUE
                           ? Short.MAX_VALUE
                           : (x < Short.MIN_VALUE ? Short.MIN_VALUE : x));
    }

    public static int clampToUShort(int x) {
        return (x > 65535 ? 65535 : (x < 0) ? 0 : x);
    }

    public static boolean isDoubleInShortRange(double dbl) {
         return dbl <= Short.MAX_VALUE && dbl >= Short.MIN_VALUE;
    }

    public static boolean isPointCoordInShortRange(Point2D p) {
        return isDoubleInShortRange(p.getX()) && isDoubleInShortRange(p.getY());
    }


    public static boolean isTransformQuadrantRotated(AffineTransform tr) {
        return ((tr.getType() & (AffineTransform.TYPE_GENERAL_ROTATION |
                 AffineTransform.TYPE_GENERAL_TRANSFORM)) == 0);
    }

    public static boolean isMaskEvaluated(byte xrCompRule) {
        switch (xrCompRule) {
        case PictOpOver:
        case PictOpOverReverse:
        case PictOpAtop:
        case PictOpXor:
            return true;
        }

        return false;
    }
}
