/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt;

import java.awt.RenderingHints;
import java.lang.annotation.Native;

/**
 * This class contains rendering hints that can be used by the
 * {@link java.awt.Graphics2D} class, and classes that implement
 * {@link java.awt.image.BufferedImageOp} and
 * {@link java.awt.image.Raster}.
 */
public class SunHints {
    /**
     * Defines the type of all keys used to control various
     * aspects of the rendering and imaging pipelines.  Instances
     * of this class are immutable and unique which means that
     * tests for matches can be made using the == operator instead
     * of the more expensive equals() method.
     */
    public static class Key extends RenderingHints.Key {
        String description;

        /**
         * Construct a key using the indicated private key.  Each
         * subclass of Key maintains its own unique domain of integer
         * keys.  No two objects with the same integer key and of the
         * same specific subclass can be constructed.  An exception
         * will be thrown if an attempt is made to construct another
         * object of a given class with the same integer key as a
         * pre-existing instance of that subclass of Key.
         */
        public Key(int privatekey, String description) {
            super(privatekey);
            this.description = description;
        }

        /**
         * Returns the numeric index associated with this Key.  This
         * is useful for use in switch statements and quick lookups
         * of the setting of a particular key.
         */
        public final int getIndex() {
            return intKey();
        }

        /**
         * Returns a string representation of the Key.
         */
        public final String toString() {
            return description;
        }

        /**
         * Returns true if the specified object is a valid value
         * for this Key.
         */
        public boolean isCompatibleValue(Object val) {
            if (val instanceof Value) {
                return ((Value)val).isCompatibleKey(this);
            }
            return false;
        }
    }

    /**
     * Defines the type of all "enumerative" values used to control
     * various aspects of the rendering and imaging pipelines.  Instances
     * of this class are immutable and unique which means that
     * tests for matches can be made using the == operator instead
     * of the more expensive equals() method.
     */
    public static class Value {
        private SunHints.Key myKey;
        private int index;
        private String description;

        private static Value[][] ValueObjects =
            new Value[NUM_KEYS][VALS_PER_KEY];

        private static synchronized void register(SunHints.Key key,
                                                  Value value) {
            int kindex = key.getIndex();
            int vindex = value.getIndex();
            if (ValueObjects[kindex][vindex] != null) {
                throw new InternalError("duplicate index: "+vindex);
            }
            ValueObjects[kindex][vindex] = value;
        }

        public static Value get(int keyindex, int valueindex) {
            return ValueObjects[keyindex][valueindex];
        }

        /**
         * Construct a value using the indicated private index.  Each
         * subclass of Value maintains its own unique domain of integer
         * indices.  Enforcing the uniqueness of the integer indices
         * is left to the subclass.
         */
        public Value(SunHints.Key key, int index, String description) {
            this.myKey = key;
            this.index = index;
            this.description = description;

            register(key, this);
        }

        /**
         * Returns the numeric index associated with this Key.  This
         * is useful for use in switch statements and quick lookups
         * of the setting of a particular key.
         */
        public final int getIndex() {
            return index;
        }

        /**
         * Returns a string representation of this Value.
         */
        public final String toString() {
            return description;
        }

        /**
         * Returns true if the specified object is a valid Key
         * for this Value.
         */
        public final boolean isCompatibleKey(Key k) {
            return myKey == k;
        }

        /**
         * The hash code for all SunHints.Value objects will be the same
         * as the system identity code of the object as defined by the
         * System.identityHashCode() method.
         */
        public final int hashCode() {
            return System.identityHashCode(this);
        }

        /**
         * The equals method for all SunHints.Value objects will return
         * the same result as the equality operator '=='.
         */
        public final boolean equals(Object o) {
            return this == o;
        }
    }

    private static final int NUM_KEYS = 10;
    private static final int VALS_PER_KEY = 8;

    /**
     * Rendering hint key and values
     */
    @Native public static final int INTKEY_RENDERING = 0;
    @Native public static final int INTVAL_RENDER_DEFAULT = 0;
    @Native public static final int INTVAL_RENDER_SPEED = 1;
    @Native public static final int INTVAL_RENDER_QUALITY = 2;

    /**
     * Antialiasing hint key and values
     */
    @Native public static final int INTKEY_ANTIALIASING = 1;
    @Native public static final int INTVAL_ANTIALIAS_DEFAULT = 0;
    @Native public static final int INTVAL_ANTIALIAS_OFF = 1;
    @Native public static final int INTVAL_ANTIALIAS_ON = 2;

    /**
     * Text antialiasing hint key and values
     */
    @Native public static final int INTKEY_TEXT_ANTIALIASING = 2;
    @Native public static final int INTVAL_TEXT_ANTIALIAS_DEFAULT = 0;
    @Native public static final int INTVAL_TEXT_ANTIALIAS_OFF = 1;
    @Native public static final int INTVAL_TEXT_ANTIALIAS_ON = 2;
    @Native public static final int INTVAL_TEXT_ANTIALIAS_GASP = 3;
    @Native public static final int INTVAL_TEXT_ANTIALIAS_LCD_HRGB = 4;
    @Native public static final int INTVAL_TEXT_ANTIALIAS_LCD_HBGR = 5;
    @Native public static final int INTVAL_TEXT_ANTIALIAS_LCD_VRGB = 6;
    @Native public static final int INTVAL_TEXT_ANTIALIAS_LCD_VBGR = 7;

    /**
     * Font fractional metrics hint key and values
     */
    @Native public static final int INTKEY_FRACTIONALMETRICS = 3;
    @Native public static final int INTVAL_FRACTIONALMETRICS_DEFAULT = 0;
    @Native public static final int INTVAL_FRACTIONALMETRICS_OFF = 1;
    @Native public static final int INTVAL_FRACTIONALMETRICS_ON = 2;

    /**
     * Dithering hint key and values
     */
    @Native public static final int INTKEY_DITHERING = 4;
    @Native public static final int INTVAL_DITHER_DEFAULT = 0;
    @Native public static final int INTVAL_DITHER_DISABLE = 1;
    @Native public static final int INTVAL_DITHER_ENABLE = 2;

    /**
     * Interpolation hint key and values
     */
    @Native public static final int INTKEY_INTERPOLATION = 5;
    @Native public static final int INTVAL_INTERPOLATION_NEAREST_NEIGHBOR = 0;
    @Native public static final int INTVAL_INTERPOLATION_BILINEAR = 1;
    @Native public static final int INTVAL_INTERPOLATION_BICUBIC = 2;

    /**
     * Alpha interpolation hint key and values
     */
    @Native public static final int INTKEY_ALPHA_INTERPOLATION = 6;
    @Native public static final int INTVAL_ALPHA_INTERPOLATION_DEFAULT = 0;
    @Native public static final int INTVAL_ALPHA_INTERPOLATION_SPEED = 1;
    @Native public static final int INTVAL_ALPHA_INTERPOLATION_QUALITY = 2;

    /**
     * Color rendering hint key and values
     */
    @Native public static final int INTKEY_COLOR_RENDERING = 7;
    @Native public static final int INTVAL_COLOR_RENDER_DEFAULT = 0;
    @Native public static final int INTVAL_COLOR_RENDER_SPEED = 1;
    @Native public static final int INTVAL_COLOR_RENDER_QUALITY = 2;

    /**
     * Stroke normalization control hint key and values
     */
    @Native public static final int INTKEY_STROKE_CONTROL = 8;
    @Native public static final int INTVAL_STROKE_DEFAULT = 0;
    @Native public static final int INTVAL_STROKE_NORMALIZE = 1;
    @Native public static final int INTVAL_STROKE_PURE = 2;

    /**
     * Image scaling hint key and values
     */
    @Native public static final int INTKEY_RESOLUTION_VARIANT = 9;
    @Native public static final int INTVAL_RESOLUTION_VARIANT_DEFAULT = 0;
    @Native public static final int INTVAL_RESOLUTION_VARIANT_BASE = 1;
    @Native public static final int INTVAL_RESOLUTION_VARIANT_SIZE_FIT = 2;
    @Native public static final int INTVAL_RESOLUTION_VARIANT_DPI_FIT = 3;

    /**
     * LCD text contrast control hint key.
     * Value is "100" to make discontiguous with the others which
     * are all enumerative and are of a different class.
     */
    @Native public static final int INTKEY_AATEXT_LCD_CONTRAST = 100;

    /**
     * Rendering hint key and value objects
     */
    public static final Key KEY_RENDERING =
        new SunHints.Key(SunHints.INTKEY_RENDERING,
                         "Global rendering quality key");
    public static final Object VALUE_RENDER_SPEED =
        new SunHints.Value(KEY_RENDERING,
                           SunHints.INTVAL_RENDER_SPEED,
                           "Fastest rendering methods");
    public static final Object VALUE_RENDER_QUALITY =
        new SunHints.Value(KEY_RENDERING,
                           SunHints.INTVAL_RENDER_QUALITY,
                           "Highest quality rendering methods");
    public static final Object VALUE_RENDER_DEFAULT =
        new SunHints.Value(KEY_RENDERING,
                           SunHints.INTVAL_RENDER_DEFAULT,
                           "Default rendering methods");

    /**
     * Antialiasing hint key and value objects
     */
    public static final Key KEY_ANTIALIASING =
        new SunHints.Key(SunHints.INTKEY_ANTIALIASING,
                         "Global antialiasing enable key");
    public static final Object VALUE_ANTIALIAS_ON =
        new SunHints.Value(KEY_ANTIALIASING,
                           SunHints.INTVAL_ANTIALIAS_ON,
                           "Antialiased rendering mode");
    public static final Object VALUE_ANTIALIAS_OFF =
        new SunHints.Value(KEY_ANTIALIASING,
                           SunHints.INTVAL_ANTIALIAS_OFF,
                           "Nonantialiased rendering mode");
    public static final Object VALUE_ANTIALIAS_DEFAULT =
        new SunHints.Value(KEY_ANTIALIASING,
                           SunHints.INTVAL_ANTIALIAS_DEFAULT,
                           "Default antialiasing rendering mode");

    /**
     * Text antialiasing hint key and value objects
     */
    public static final Key KEY_TEXT_ANTIALIASING =
        new SunHints.Key(SunHints.INTKEY_TEXT_ANTIALIASING,
                         "Text-specific antialiasing enable key");
    public static final Object VALUE_TEXT_ANTIALIAS_ON =
        new SunHints.Value(KEY_TEXT_ANTIALIASING,
                           SunHints.INTVAL_TEXT_ANTIALIAS_ON,
                           "Antialiased text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_OFF =
        new SunHints.Value(KEY_TEXT_ANTIALIASING,
                           SunHints.INTVAL_TEXT_ANTIALIAS_OFF,
                           "Nonantialiased text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_DEFAULT =
        new SunHints.Value(KEY_TEXT_ANTIALIASING,
                           SunHints.INTVAL_TEXT_ANTIALIAS_DEFAULT,
                           "Default antialiasing text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_GASP =
        new SunHints.Value(KEY_TEXT_ANTIALIASING,
                           SunHints.INTVAL_TEXT_ANTIALIAS_GASP,
                           "gasp antialiasing text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_LCD_HRGB =
        new SunHints.Value(KEY_TEXT_ANTIALIASING,
                           SunHints.INTVAL_TEXT_ANTIALIAS_LCD_HRGB,
                           "LCD HRGB antialiasing text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_LCD_HBGR =
        new SunHints.Value(KEY_TEXT_ANTIALIASING,
                           SunHints.INTVAL_TEXT_ANTIALIAS_LCD_HBGR,
                           "LCD HBGR antialiasing text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_LCD_VRGB =
        new SunHints.Value(KEY_TEXT_ANTIALIASING,
                           SunHints.INTVAL_TEXT_ANTIALIAS_LCD_VRGB,
                           "LCD VRGB antialiasing text mode");
    public static final Object VALUE_TEXT_ANTIALIAS_LCD_VBGR =
        new SunHints.Value(KEY_TEXT_ANTIALIASING,
                           SunHints.INTVAL_TEXT_ANTIALIAS_LCD_VBGR,
                           "LCD VBGR antialiasing text mode");

    /**
     * Font fractional metrics hint key and value objects
     */
    public static final Key KEY_FRACTIONALMETRICS =
        new SunHints.Key(SunHints.INTKEY_FRACTIONALMETRICS,
                         "Fractional metrics enable key");
    public static final Object VALUE_FRACTIONALMETRICS_ON =
        new SunHints.Value(KEY_FRACTIONALMETRICS,
                           SunHints.INTVAL_FRACTIONALMETRICS_ON,
                           "Fractional text metrics mode");
    public static final Object VALUE_FRACTIONALMETRICS_OFF =
        new SunHints.Value(KEY_FRACTIONALMETRICS,
                           SunHints.INTVAL_FRACTIONALMETRICS_OFF,
                           "Integer text metrics mode");
    public static final Object VALUE_FRACTIONALMETRICS_DEFAULT =
        new SunHints.Value(KEY_FRACTIONALMETRICS,
                           SunHints.INTVAL_FRACTIONALMETRICS_DEFAULT,
                           "Default fractional text metrics mode");

    /**
     * Dithering hint key and value objects
     */
    public static final Key KEY_DITHERING =
        new SunHints.Key(SunHints.INTKEY_DITHERING,
                         "Dithering quality key");
    public static final Object VALUE_DITHER_ENABLE =
        new SunHints.Value(KEY_DITHERING,
                           SunHints.INTVAL_DITHER_ENABLE,
                           "Dithered rendering mode");
    public static final Object VALUE_DITHER_DISABLE =
        new SunHints.Value(KEY_DITHERING,
                           SunHints.INTVAL_DITHER_DISABLE,
                           "Nondithered rendering mode");
    public static final Object VALUE_DITHER_DEFAULT =
        new SunHints.Value(KEY_DITHERING,
                           SunHints.INTVAL_DITHER_DEFAULT,
                           "Default dithering mode");

    /**
     * Interpolation hint key and value objects
     */
    public static final Key KEY_INTERPOLATION =
        new SunHints.Key(SunHints.INTKEY_INTERPOLATION,
                         "Image interpolation method key");
    public static final Object VALUE_INTERPOLATION_NEAREST_NEIGHBOR =
        new SunHints.Value(KEY_INTERPOLATION,
                           SunHints.INTVAL_INTERPOLATION_NEAREST_NEIGHBOR,
                           "Nearest Neighbor image interpolation mode");
    public static final Object VALUE_INTERPOLATION_BILINEAR =
        new SunHints.Value(KEY_INTERPOLATION,
                           SunHints.INTVAL_INTERPOLATION_BILINEAR,
                           "Bilinear image interpolation mode");
    public static final Object VALUE_INTERPOLATION_BICUBIC =
        new SunHints.Value(KEY_INTERPOLATION,
                           SunHints.INTVAL_INTERPOLATION_BICUBIC,
                           "Bicubic image interpolation mode");

    /**
     * Alpha interpolation hint key and value objects
     */
    public static final Key KEY_ALPHA_INTERPOLATION =
        new SunHints.Key(SunHints.INTKEY_ALPHA_INTERPOLATION,
                         "Alpha blending interpolation method key");
    public static final Object VALUE_ALPHA_INTERPOLATION_SPEED =
        new SunHints.Value(KEY_ALPHA_INTERPOLATION,
                           SunHints.INTVAL_ALPHA_INTERPOLATION_SPEED,
                           "Fastest alpha blending methods");
    public static final Object VALUE_ALPHA_INTERPOLATION_QUALITY =
        new SunHints.Value(KEY_ALPHA_INTERPOLATION,
                           SunHints.INTVAL_ALPHA_INTERPOLATION_QUALITY,
                           "Highest quality alpha blending methods");
    public static final Object VALUE_ALPHA_INTERPOLATION_DEFAULT =
        new SunHints.Value(KEY_ALPHA_INTERPOLATION,
                           SunHints.INTVAL_ALPHA_INTERPOLATION_DEFAULT,
                           "Default alpha blending methods");

    /**
     * Color rendering hint key and value objects
     */
    public static final Key KEY_COLOR_RENDERING =
        new SunHints.Key(SunHints.INTKEY_COLOR_RENDERING,
                         "Color rendering quality key");
    public static final Object VALUE_COLOR_RENDER_SPEED =
        new SunHints.Value(KEY_COLOR_RENDERING,
                           SunHints.INTVAL_COLOR_RENDER_SPEED,
                           "Fastest color rendering mode");
    public static final Object VALUE_COLOR_RENDER_QUALITY =
        new SunHints.Value(KEY_COLOR_RENDERING,
                           SunHints.INTVAL_COLOR_RENDER_QUALITY,
                           "Highest quality color rendering mode");
    public static final Object VALUE_COLOR_RENDER_DEFAULT =
        new SunHints.Value(KEY_COLOR_RENDERING,
                           SunHints.INTVAL_COLOR_RENDER_DEFAULT,
                           "Default color rendering mode");

    /**
     * Stroke normalization control hint key and value objects
     */
    public static final Key KEY_STROKE_CONTROL =
        new SunHints.Key(SunHints.INTKEY_STROKE_CONTROL,
                         "Stroke normalization control key");
    public static final Object VALUE_STROKE_DEFAULT =
        new SunHints.Value(KEY_STROKE_CONTROL,
                           SunHints.INTVAL_STROKE_DEFAULT,
                           "Default stroke normalization");
    public static final Object VALUE_STROKE_NORMALIZE =
        new SunHints.Value(KEY_STROKE_CONTROL,
                           SunHints.INTVAL_STROKE_NORMALIZE,
                           "Normalize strokes for consistent rendering");
    public static final Object VALUE_STROKE_PURE =
        new SunHints.Value(KEY_STROKE_CONTROL,
                           SunHints.INTVAL_STROKE_PURE,
                           "Pure stroke conversion for accurate paths");

    /**
     * Image resolution variant hint key and value objects
     */
    public static final Key KEY_RESOLUTION_VARIANT =
        new SunHints.Key(SunHints.INTKEY_RESOLUTION_VARIANT,
                         "Global image resolution variant key");
    public static final Object VALUE_RESOLUTION_VARIANT_DEFAULT =
        new SunHints.Value(KEY_RESOLUTION_VARIANT,
                           SunHints.INTVAL_RESOLUTION_VARIANT_DEFAULT,
                           "Choose image resolutions based on a default"
                                   + "heuristic");
    public static final Object VALUE_RESOLUTION_VARIANT_BASE =
        new SunHints.Value(KEY_RESOLUTION_VARIANT,
                           SunHints.INTVAL_RESOLUTION_VARIANT_BASE,
                           "Use only the standard resolution of an image");
    public static final Object VALUE_RESOLUTION_VARIANT_SIZE_FIT =
        new SunHints.Value(KEY_RESOLUTION_VARIANT,
                           SunHints.INTVAL_RESOLUTION_VARIANT_SIZE_FIT,
                           "Choose image resolutions based on the DPI"
                                   + "of the screen and transform"
                                   + "in the Graphics2D context");
    public static final Object VALUE_RESOLUTION_VARIANT_DPI_FIT =
        new SunHints.Value(KEY_RESOLUTION_VARIANT,
                           SunHints.INTVAL_RESOLUTION_VARIANT_DPI_FIT,
                           "Choose image resolutions based only on the DPI"
                                   + " of the screen");

    public static class LCDContrastKey extends Key {

        public LCDContrastKey(int privatekey, String description) {
            super(privatekey, description);
        }

        /**
         * Returns true if the specified object is a valid value
         * for this Key. The allowable range is 100 to 250.
         */
        public final boolean isCompatibleValue(Object val) {
            if (val instanceof Integer) {
                int ival = ((Integer)val).intValue();
                return ival >= 100 && ival <= 250;
            }
            return false;
        }

    }

    /**
     * LCD text contrast hint key
     */
    public static final RenderingHints.Key
        KEY_TEXT_ANTIALIAS_LCD_CONTRAST =
        new LCDContrastKey(SunHints.INTKEY_AATEXT_LCD_CONTRAST,
                           "Text-specific LCD contrast key");
}
