/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.marlin;

import java.security.AccessController;
import static sun.java2d.marlin.MarlinUtils.logInfo;
import sun.security.action.GetPropertyAction;

public final class MarlinProperties {

    private MarlinProperties() {
        // no-op
    }

    // marlin system properties

    public static boolean isUseThreadLocal() {
        return getBoolean("sun.java2d.renderer.useThreadLocal", "true");
    }

    /**
     * Return the initial edge capacity used to define initial arrays
     * (edges, polystack, crossings)
     *
     * @return 256 < initial edges < 65536 (4096 by default)
     */
    public static int getInitialEdges() {
        return align(
            getInteger("sun.java2d.renderer.edges", 4096, 64, 64 * 1024),
            64);
    }

    /**
     * Return the initial pixel width used to define initial arrays
     * (tile AA chunk, alpha line)
     *
     * @return 64 < initial pixel size < 32768 (4096 by default)
     */
    public static int getInitialPixelWidth() {
        return align(
            getInteger("sun.java2d.renderer.pixelWidth", 4096, 64, 32 * 1024),
            64);
    }

    /**
     * Return the initial pixel height used to define initial arrays
     * (buckets)
     *
     * @return 64 < initial pixel size < 32768 (2176 by default)
     */
    public static int getInitialPixelHeight() {
        return align(
            getInteger("sun.java2d.renderer.pixelHeight", 2176, 64, 32 * 1024),
            64);
    }

    /**
     * Return the log(2) corresponding to subpixel on x-axis
     *
     * @return 0 (1 subpixels) < initial pixel size < 8 (256 subpixels)
     * (8 by default ie 256 subpixels)
     */
    public static int getSubPixel_Log2_X() {
        return getInteger("sun.java2d.renderer.subPixel_log2_X", 8, 0, 8);
    }

    /**
     * Return the log(2) corresponding to subpixel on y-axis
     *
     * @return 0 (1 subpixels) < initial pixel size < 8 (256 subpixels)
     * (3 by default ie 8 subpixels)
     */
    public static int getSubPixel_Log2_Y() {
        return getInteger("sun.java2d.renderer.subPixel_log2_Y", 3, 0, 8);
    }

    /**
     * Return the log(2) corresponding to the square tile size in pixels
     *
     * @return 3 (8x8 pixels) < tile size < 10 (1024x1024 pixels)
     * (5 by default ie 32x32 pixels)
     */
    public static int getTileSize_Log2() {
        return getInteger("sun.java2d.renderer.tileSize_log2", 5, 3, 10);
    }

    /**
     * Return the log(2) corresponding to the tile width in pixels
     *
     * @return 3 (8 pixels) < tile width < 10 (1024 pixels)
     * (5 by default ie 32x32 pixels)
     */
    public static int getTileWidth_Log2() {
        return getInteger("sun.java2d.renderer.tileWidth_log2", 5, 3, 10);
    }

    /**
     * Return the log(2) corresponding to the block size in pixels
     *
     * @return 3 (8 pixels) < block size < 8 (256 pixels)
     * (5 by default ie 32 pixels)
     */
    public static int getBlockSize_Log2() {
        return getInteger("sun.java2d.renderer.blockSize_log2", 5, 3, 8);
    }

    // RLE / blockFlags settings

    public static boolean isForceRLE() {
        return getBoolean("sun.java2d.renderer.forceRLE", "false");
    }

    public static boolean isForceNoRLE() {
        return getBoolean("sun.java2d.renderer.forceNoRLE", "false");
    }

    public static boolean isUseTileFlags() {
        return getBoolean("sun.java2d.renderer.useTileFlags", "true");
    }

    public static boolean isUseTileFlagsWithHeuristics() {
        return isUseTileFlags()
        && getBoolean("sun.java2d.renderer.useTileFlags.useHeuristics", "true");
    }

    public static int getRLEMinWidth() {
        return getInteger("sun.java2d.renderer.rleMinWidth", 64, 0, Integer.MAX_VALUE);
    }

    // optimisation parameters

    public static boolean isUseSimplifier() {
        return getBoolean("sun.java2d.renderer.useSimplifier", "false");
    }

    public static boolean isUsePathSimplifier() {
        return getBoolean("sun.java2d.renderer.usePathSimplifier", "false");
    }

    public static float getPathSimplifierPixelTolerance() {
        // default: MIN_PEN_SIZE or less ?
        return getFloat("sun.java2d.renderer.pathSimplifier.pixTol",
                (1.0f / MarlinConst.MIN_SUBPIXELS),
                1e-3f,
                10.0f);
    }

    public static boolean isDoClip() {
        return getBoolean("sun.java2d.renderer.clip", "true");
    }

    public static boolean isDoClipRuntimeFlag() {
        return getBoolean("sun.java2d.renderer.clip.runtime.enable", "false");
    }

    public static boolean isDoClipAtRuntime() {
        return getBoolean("sun.java2d.renderer.clip.runtime", "true");
    }

    public static boolean isDoClipSubdivider() {
        return getBoolean("sun.java2d.renderer.clip.subdivider", "true");
    }

    public static float getSubdividerMinLength() {
        return getFloat("sun.java2d.renderer.clip.subdivider.minLength", 100.0f, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
    }

    // debugging parameters

    public static boolean isDoStats() {
        return getBoolean("sun.java2d.renderer.doStats", "false");
    }

    public static boolean isDoMonitors() {
        return getBoolean("sun.java2d.renderer.doMonitors", "false");
    }

    public static boolean isDoChecks() {
        return getBoolean("sun.java2d.renderer.doChecks", "false");
    }

    // logging parameters

    public static boolean isLoggingEnabled() {
        return getBoolean("sun.java2d.renderer.log", "false");
    }

    public static boolean isUseLogger() {
        return getBoolean("sun.java2d.renderer.useLogger", "false");
    }

    public static boolean isLogCreateContext() {
        return getBoolean("sun.java2d.renderer.logCreateContext", "false");
    }

    public static boolean isLogUnsafeMalloc() {
        return getBoolean("sun.java2d.renderer.logUnsafeMalloc", "false");
    }

    // quality settings

    public static float getCurveLengthError() {
        return getFloat("sun.java2d.renderer.curve_len_err", 0.01f, 1e-6f, 1.0f);
    }

    public static float getCubicDecD2() {
        return getFloat("sun.java2d.renderer.cubic_dec_d2", 1.0f, 1e-5f, 4.0f);
    }

    public static float getCubicIncD1() {
        return getFloat("sun.java2d.renderer.cubic_inc_d1", 0.2f, 1e-6f, 1.0f);
    }

    public static float getQuadDecD2() {
        return getFloat("sun.java2d.renderer.quad_dec_d2", 0.5f, 1e-5f, 4.0f);
    }

    // system property utilities
    @SuppressWarnings("removal")
    static boolean getBoolean(final String key, final String def) {
        return Boolean.valueOf(AccessController.doPrivileged(
                  new GetPropertyAction(key, def)));
    }

    static int getInteger(final String key, final int def,
                                 final int min, final int max)
    {
        @SuppressWarnings("removal")
        final String property = AccessController.doPrivileged(
                                    new GetPropertyAction(key));

        int value = def;
        if (property != null) {
            try {
                value = Integer.decode(property);
            } catch (NumberFormatException e) {
                logInfo("Invalid integer value for " + key + " = " + property);
            }
        }

        // check for invalid values
        if ((value < min) || (value > max)) {
            logInfo("Invalid value for " + key + " = " + value
                    + "; expected value in range[" + min + ", " + max + "] !");
            value = def;
        }
        return value;
    }

    static int align(final int val, final int norm) {
        final int ceil = FloatMath.ceil_int( ((float) val) / norm);
        return ceil * norm;
    }

    public static double getDouble(final String key, final double def,
                                   final double min, final double max)
    {
        double value = def;
        @SuppressWarnings("removal")
        final String property = AccessController.doPrivileged(
                                    new GetPropertyAction(key));

        if (property != null) {
            try {
                value = Double.parseDouble(property);
            } catch (NumberFormatException nfe) {
                logInfo("Invalid value for " + key + " = " + property + " !");
            }
        }
        // check for invalid values
        if (value < min || value > max) {
            logInfo("Invalid value for " + key + " = " + value
                    + "; expect value in range[" + min + ", " + max + "] !");
            value = def;
        }
        return value;
    }

    public static float getFloat(final String key, final float def,
                                 final float min, final float max)
    {
        return (float)getDouble(key, def, min, max);
    }
}
