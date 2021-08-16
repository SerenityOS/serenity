/*
 * Copyright (c) 2002, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.geom.AffineTransform;
import javax.swing.plaf.FontUIResource;
import java.util.StringTokenizer;

import sun.font.FontConfigManager;
import sun.font.FontUtilities;

/**
 * @author Shannon Hickey
 * @author Leif Samuelsson
 */
class PangoFonts {

    public static final String CHARS_DIGITS = "0123456789";

    /**
     * Calculate a default scale factor for fonts in this L&F to match
     * the reported resolution of the screen.
     * Java 2D specified a default user-space scale of 72dpi.
     * This is unlikely to correspond to that of the real screen.
     * The Xserver reports a value which may be used to adjust for this.
     * and Java 2D exposes it via a normalizing transform.
     * However many Xservers report a hard-coded 90dpi whilst others report a
     * calculated value based on possibly incorrect data.
     * That is something that must be solved at the X11 level
     * Note that in an X11 multi-screen environment, the default screen
     * is the one used by the JRE so it is safe to use it here.
     */
    private static double fontScale;

    static {
        fontScale = 1.0d;
        GraphicsEnvironment ge =
           GraphicsEnvironment.getLocalGraphicsEnvironment();

        if (!GraphicsEnvironment.isHeadless()) {
            GraphicsConfiguration gc =
                ge.getDefaultScreenDevice().getDefaultConfiguration();
            AffineTransform at = gc.getNormalizingTransform();
            fontScale = at.getScaleY();
        }
    }


    /**
     * Parses a String containing a pango font description and returns
     * a Font object.
     *
     * @param pangoName a String describing a pango font
     *                  e.g. "Sans Italic 10"
     * @return a Font object as a FontUIResource
     *         or null if no suitable font could be created.
     */
    static Font lookupFont(String pangoName) {
        String family = "";
        int style = Font.PLAIN;
        int size = 10;

        StringTokenizer tok = new StringTokenizer(pangoName);

        while (tok.hasMoreTokens()) {
            String word = tok.nextToken();

            if (word.equalsIgnoreCase("italic")) {
                style |= Font.ITALIC;
            } else if (word.equalsIgnoreCase("bold")) {
                style |= Font.BOLD;
            } else if (CHARS_DIGITS.indexOf(word.charAt(0)) != -1) {
                try {
                    size = Integer.parseInt(word);
                } catch (NumberFormatException ex) {
                }
            } else {
                if (family.length() > 0) {
                    family += " ";
                }

                family += word;
            }
        }

        /*
         * Java 2D font point sizes are in a user-space scale of 72dpi.
         * GTK allows a user to configure a "dpi" property used to scale
         * the fonts used to match a user's preference.
         * To match the font size of GTK apps we need to obtain this DPI and
         * adjust as follows:
         * Some versions of GTK use XSETTINGS if available to dynamically
         * monitor user-initiated changes in the DPI to be used by GTK
         * apps. This value is also made available as the Xft.dpi X resource.
         * This is presumably a function of the font preferences API and/or
         * the manner in which it requests the toolkit to update the default
         * for the desktop. This dual approach is probably necessary since
         * other versions of GTK - or perhaps some apps - determine the size
         * to use only at start-up from that X resource.
         * If that resource is not set then GTK scales for the DPI resolution
         * reported by the Xserver using the formula
         * DisplayHeight(dpy, screen) / DisplayHeightMM(dpy, screen) * 25.4
         * (25.4mm == 1 inch).
         * JDK tracks the Xft.dpi XSETTINGS property directly so it can
         * dynamically change font size by tracking just that value.
         * If that resource is not available use the same fall back formula
         * as GTK (see calculation for fontScale).
         *
         * GTK's default setting for Xft.dpi is 96 dpi (and it seems -1
         * apparently also can mean that "default"). However this default
         * isn't used if there's no property set. The real default in the
         * absence of a resource is the Xserver reported dpi.
         * Finally this DPI is used to calculate the nearest Java 2D font
         * 72 dpi font size.
         * There are cases in which JDK behaviour may not exactly mimic
         * GTK native app behaviour :
         * 1) When a GTK app is not able to dynamically track the changes
         * (does not use XSETTINGS), JDK will resize but other apps will
         * not. This is OK as JDK is exhibiting preferred behaviour and
         * this is probably how all later GTK apps will behave
         * 2) When a GTK app does not use XSETTINGS and for some reason
         * the XRDB property is not present. JDK will pick up XSETTINGS
         * and the GTK app will use the Xserver default. Since its
         * impossible for JDK to know that some other GTK app is not
         * using XSETTINGS its impossible to account for this and in any
         * case for it to be a problem the values would have to be different.
         * It also seems unlikely to arise except when a user explicitly
         * deletes the X resource database entry.
         * There also some other issues to be aware of for the future:
         * GTK specifies the Xft.dpi value as server-wide which when used
         * on systems with 2 distinct X screens with different physical DPI
         * the font sizes will inevitably appear different. It would have
         * been a more user-friendly design to further adjust that one
         * setting depending on the screen resolution to achieve perceived
         * equivalent sizes. If such a change were ever to be made in GTK
         * we would need to update for that.
         */
        double dsize = size;
        int dpi = 96;
        Object value =
            Toolkit.getDefaultToolkit().getDesktopProperty("gnome.Xft/DPI");
        if (value instanceof Integer) {
            dpi = ((Integer)value).intValue() / 1024;
            if (dpi == -1) {
              dpi = 96;
            }
            if (dpi < 50) { /* 50 dpi is the minimum value gnome allows */
                dpi = 50;
            }
            /* The Java rasteriser assumes pts are in a user space of
             * 72 dpi, so we need to adjust for that.
             */
            dsize = ((double)(dpi * size)/ 72.0);
        } else {
            /* If there's no property, GTK scales for the resolution
             * reported by the Xserver using the formula listed above.
             * fontScale already accounts for the 72 dpi Java 2D space.
             */
            dsize = size * fontScale;
        }

        /* Round size to nearest integer pt size */
        size = (int)(dsize + 0.5);
        if (size < 1) {
            size = 1;
        }

        String fcFamilyLC = family.toLowerCase();
        if (FontUtilities.mapFcName(fcFamilyLC) != null) {
            /* family is a Fc/Pango logical font which we need to expand. */
            Font font =  FontUtilities.getFontConfigFUIR(fcFamilyLC, style, size);
            font = font.deriveFont(style, (float)dsize);
            return new FontUIResource(font);
        } else {
            /* It's a physical font which we will create with a fallback */
            Font font = new Font(family, style, size);
            /* a roundabout way to set the font size in floating points */
            font = font.deriveFont(style, (float)dsize);
            FontUIResource fuir = new FontUIResource(font);
            return FontUtilities.getCompositeFontUIResource(fuir);
        }
    }

    /**
     * Parses a String containing a pango font description and returns
     * the (unscaled) font size as an integer.
     *
     * @param pangoName a String describing a pango font
     * @return the size of the font described by pangoName (e.g. if
     *         pangoName is "Sans Italic 10", then this method returns 10)
     */
    static int getFontSize(String pangoName) {
        int size = 10;

        StringTokenizer tok = new StringTokenizer(pangoName);
        while (tok.hasMoreTokens()) {
            String word = tok.nextToken();

            if (CHARS_DIGITS.indexOf(word.charAt(0)) != -1) {
                try {
                    size = Integer.parseInt(word);
                } catch (NumberFormatException ex) {
                }
            }
        }

        return size;
    }
}
