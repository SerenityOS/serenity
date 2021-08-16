/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.X11;

import java.awt.*;
import java.io.*;
import sun.security.action.GetPropertyAction;
import java.security.AccessController;
import sun.awt.OSInfo;

/**
  *
  *  This class contains code that is need to mimic the
  *  Motif Color selection and color defaults code.
  *
  *  Portions of this code have been ported to java from
  *  Motif sources (Color.c) (ColorP.h) etc.
  *
  *  Author: Bino George
  *
  */

class MotifColorUtilities {


    static final float XmRED_LUMINOSITY=0.30f;
    static final float XmGREEN_LUMINOSITY=0.59f;
    static final float XmBLUE_LUMINOSITY=0.11f;
    static final int XmINTENSITY_FACTOR=75;
    static final int XmLIGHT_FACTOR=0;
    static final int XmLUMINOSITY_FACTOR=25;

    static final int XmMAX_SHORT=65535;


    static final int XmCOLOR_PERCENTILE=(XmMAX_SHORT / 100);

    static final int XmDEFAULT_DARK_THRESHOLD=20;
    static final int XmDEFAULT_LIGHT_THRESHOLD=93;
    static final int XmDEFAULT_FOREGROUND_THRESHOLD=70;

    static final int BLACK = 0xFF000000;
    static final int WHITE = 0xFFFFFFFF;
    static final int MOTIF_WINDOW_COLOR= 0xFFDFDFDF;

    static final int DEFAULT_COLOR =  0xFFC4C4C4;

    static final int  XmCOLOR_LITE_THRESHOLD = XmDEFAULT_LIGHT_THRESHOLD * XmCOLOR_PERCENTILE;
    static final int  XmCOLOR_DARK_THRESHOLD = XmDEFAULT_DARK_THRESHOLD * XmCOLOR_PERCENTILE;
    static final int  XmFOREGROUND_THRESHOLD = XmDEFAULT_FOREGROUND_THRESHOLD * XmCOLOR_PERCENTILE;

    /* LITE color model
       percent to interpolate RGB towards black for SEL, BS, TS */

    static final int XmCOLOR_LITE_SEL_FACTOR = 15;
    static final int XmCOLOR_LITE_BS_FACTOR =  40;
    static final int XmCOLOR_LITE_TS_FACTOR =  20;

    /* DARK color model
       percent to interpolate RGB towards white for SEL, BS, TS */

    static final int XmCOLOR_DARK_SEL_FACTOR=  15;
    static final int XmCOLOR_DARK_BS_FACTOR =  30;
    static final int XmCOLOR_DARK_TS_FACTOR =  50;

    /* STD color model
       percent to interpolate RGB towards black for SEL, BS
       percent to interpolate RGB towards white for TS
       HI values used for high brightness (within STD)
       LO values used for low brightness (within STD)
       Interpolate factors between HI & LO values based on brightness */

    static final int XmCOLOR_HI_SEL_FACTOR = 15;
    static final int XmCOLOR_HI_BS_FACTOR =  40;
    static final int XmCOLOR_HI_TS_FACTOR =  60;

    static final int XmCOLOR_LO_SEL_FACTOR=  15;
    static final int XmCOLOR_LO_BS_FACTOR =  60;
    static final int XmCOLOR_LO_TS_FACTOR =  50;

    static int brightness( int red, int green, int blue )
    {
        float brightness;
        float intensity;
        float light;
        float luminosity, maxprimary, minprimary;

        // To mimix Motif logic, we need to convert to 16 bit color values.

        red = red << 8;
        green = green << 8;
        blue = blue << 8;


        intensity = (red + green + blue) / 3;


        /*
         * The casting nonsense below is to try to control the point at
         * the truncation occurs.
         */

        luminosity = (int) ((XmRED_LUMINOSITY * (float) red)
                + (XmGREEN_LUMINOSITY * (float) green)
                + (XmBLUE_LUMINOSITY * (float) blue));

        maxprimary = ( (red > green) ?
                ( (red > blue) ? red : blue ) :
                ( (green > blue) ? green : blue ) );

        minprimary = ( (red < green) ?
                ( (red < blue) ? red : blue ) :
                ( (green < blue) ? green : blue ) );

        light = (minprimary + maxprimary) / 2;

        brightness = ( (intensity * XmINTENSITY_FACTOR) +
                (light * XmLIGHT_FACTOR) +
                (luminosity * XmLUMINOSITY_FACTOR) ) / 100;
        return Math.round(brightness);
    }

    static int calculateForegroundFromBackground(int r, int g, int b) {

        int foreground = WHITE;
        int  brightness = brightness(r,g,b);

        if (brightness >  XmFOREGROUND_THRESHOLD) {
            foreground = BLACK;
        }
        else foreground = WHITE;

        return foreground;
    }

    static int calculateTopShadowFromBackground(int r, int g, int b) {

        float color_value,f;

        int br = r << 8;
        int bg = g << 8;
        int bb = b << 8;

        int brightness = brightness(r,g,b);

        float red;
        float green;
        float blue;

        if (brightness < XmCOLOR_DARK_THRESHOLD) {
            // dark background

            color_value = br;
            color_value += XmCOLOR_DARK_TS_FACTOR *
                (XmMAX_SHORT - color_value) / 100;
            red = color_value;

            color_value = bg;
            color_value += XmCOLOR_DARK_TS_FACTOR *
                (XmMAX_SHORT - color_value) / 100;
            green = color_value;

            color_value = bb;
            color_value += XmCOLOR_DARK_TS_FACTOR *
                (XmMAX_SHORT - color_value) / 100;
            blue = color_value;
        }
        else if (brightness > XmCOLOR_LITE_THRESHOLD) {
            // lite background

            color_value = br;
            color_value -= (color_value * XmCOLOR_LITE_TS_FACTOR) / 100;
            red = color_value;

            color_value = bg;
            color_value -= (color_value * XmCOLOR_LITE_TS_FACTOR) / 100;
            green = color_value;

            color_value = bb;
            color_value -= (color_value * XmCOLOR_LITE_TS_FACTOR) / 100;
            blue = color_value;

        }
        else {
            // medium
            f = XmCOLOR_LO_TS_FACTOR + (brightness
                    * ( XmCOLOR_HI_TS_FACTOR - XmCOLOR_LO_TS_FACTOR )
                    / XmMAX_SHORT);

            color_value = br;
            color_value += f * ( XmMAX_SHORT - color_value ) / 100;
            red = color_value;

            color_value = bg;
            color_value += f * ( XmMAX_SHORT - color_value ) / 100;
            green = color_value;

            color_value = bb;
            color_value += f * ( XmMAX_SHORT - color_value ) / 100;
            blue = color_value;


        }


        int ired = ((int)red) >> 8;
        int igreen = ((int)green) >> 8;
        int iblue = ((int)blue) >> 8;

        int ret = 0xff000000 | ired <<16 | igreen<<8 | iblue;

        return ret;
    }


    static int calculateBottomShadowFromBackground(int r, int g, int b) {

        float color_value,f;

        int br = r << 8;
        int bg = g << 8;
        int bb = b << 8;

        int brightness = brightness(r,g,b);

        float red;
        float green;
        float blue;

        if (brightness < XmCOLOR_DARK_THRESHOLD) {
            // dark background
            color_value = br;
            color_value += XmCOLOR_DARK_BS_FACTOR *
                (XmMAX_SHORT - color_value) / 100;
            red = color_value;

            color_value = bg;
            color_value += XmCOLOR_DARK_BS_FACTOR *
                (XmMAX_SHORT - color_value) / 100;
            green = color_value;

            color_value = bb;
            color_value += XmCOLOR_DARK_BS_FACTOR *
                (XmMAX_SHORT - color_value) / 100;
            blue = color_value;

        }
        else if (brightness > XmCOLOR_LITE_THRESHOLD) {
            // lite background
            color_value = br;
            color_value -= (color_value * XmCOLOR_LITE_BS_FACTOR) / 100;
            red = color_value;

            color_value = bg;
            color_value -= (color_value * XmCOLOR_LITE_BS_FACTOR) / 100;
            green = color_value;

            color_value = bb;
            color_value -= (color_value * XmCOLOR_LITE_BS_FACTOR) / 100;
            blue = color_value;

        }
        else {
            // medium
            f = XmCOLOR_LO_BS_FACTOR + (brightness
                    * ( XmCOLOR_HI_BS_FACTOR - XmCOLOR_LO_BS_FACTOR )
                    / XmMAX_SHORT);

            color_value = br;
            color_value -= (color_value * f) / 100;
            red = color_value;

            color_value = bg;
            color_value -= (color_value * f) / 100;
            green = color_value;

            color_value = bb;
            color_value -= (color_value * f) / 100;
            blue = color_value;
        }


        int ired = ((int)red) >> 8;
        int igreen = ((int)green) >> 8;
        int iblue = ((int)blue) >> 8;

        int ret = 0xff000000 | ired <<16 | igreen<<8 | iblue;

        return ret;
    }

    static int calculateSelectFromBackground(int r, int g, int b) {

        float color_value,f;

        int br = r << 8;
        int bg = g << 8;
        int bb = b << 8;

        int brightness = brightness(r,g,b);

        float red;
        float green;
        float blue;

        if (brightness < XmCOLOR_DARK_THRESHOLD) {
            // dark background
            color_value = br;
            color_value += XmCOLOR_DARK_SEL_FACTOR *
                (XmMAX_SHORT - color_value) / 100;
            red = color_value;

            color_value = bg;
            color_value += XmCOLOR_DARK_SEL_FACTOR *
                (XmMAX_SHORT - color_value) / 100;
            green = color_value;

            color_value = bb;
            color_value += XmCOLOR_DARK_SEL_FACTOR *
                (XmMAX_SHORT - color_value) / 100;
            blue = color_value;

        }
        else if (brightness > XmCOLOR_LITE_THRESHOLD) {
            // lite background
            color_value = br;
            color_value -= (color_value * XmCOLOR_LITE_SEL_FACTOR) / 100;
            red = color_value;

            color_value = bg;
            color_value -= (color_value * XmCOLOR_LITE_SEL_FACTOR) / 100;
            green = color_value;

            color_value = bb;
            color_value -= (color_value * XmCOLOR_LITE_SEL_FACTOR) / 100;
            blue = color_value;

        }
        else {
            // medium
            f = XmCOLOR_LO_SEL_FACTOR + (brightness
                    * ( XmCOLOR_HI_SEL_FACTOR - XmCOLOR_LO_SEL_FACTOR )
                    / XmMAX_SHORT);

            color_value = br;
            color_value -= (color_value * f) / 100;
            red = color_value;

            color_value = bg;
            color_value -= (color_value * f) / 100;
            green = color_value;

            color_value = bb;
            color_value -= (color_value * f) / 100;
            blue = color_value;
        }


        int ired = ((int)red) >> 8;
        int igreen = ((int)green) >> 8;
        int iblue = ((int)blue) >> 8;

        int ret = 0xff000000 | ired <<16 | igreen<<8 | iblue;

        return ret;
    }

   static void loadSystemColorsForCDE(int[] systemColors) throws Exception  {
        // System.out.println("loadSystemColorsForCDE");
        XAtom resourceManager = XAtom.get("RESOURCE_MANAGER");

        String resourceString = resourceManager.getProperty(XToolkit.getDefaultRootWindow());

        int index = resourceString.indexOf("ColorPalette:");
        int len = resourceString.length();
        while ( (index < len) && (resourceString.charAt(index) != ':')) index++;
        index++; // skip :
        if (resourceString.charAt(index) == '\t') index++; // skip \t

        String paletteFile = resourceString.substring(index,resourceString.indexOf("\n",index));

        //System.out.println("Palette File = " + paletteFile);

        // Check if palette is a user palette.

        String  paletteFilePath = System.getProperty("user.home") + "/.dt/palettes/" + paletteFile;

        File pFile = new File(paletteFilePath);
        if (!pFile.exists())
        {
            // Must be a system palette
            paletteFilePath = "/usr/dt/palettes/" + paletteFile;
            pFile = new File(paletteFilePath);
            if (!pFile.exists())
            {
                throw new FileNotFoundException("Could not open : "+ paletteFilePath);
            }
        }
        BufferedReader bfr = new BufferedReader(new FileReader(pFile));

        int[] colors = new int[8];
        int r,g,b;
        String temp,color;

        for (int i=0;i<8;i++) {
            temp = bfr.readLine();
            color = temp.substring(1,temp.length());
            r = Integer.valueOf(color.substring(0,4),16).intValue() >> 8;
            g = Integer.valueOf(color.substring(4,8),16).intValue() >> 8;
            b = Integer.valueOf(color.substring(8,12),16).intValue() >> 8;
            colors[i] = 0xff000000 | r<<16 | g<<8 | b;
            //  System.out.println("color["+i+"]="+Integer.toHexString(colors[i]) + "r = " +r + "g="+g+"b="+b);
        }

        // Solaris's default color is MEDIUM_COLOR (4)
        // AIX's default color is HIGH_COLOR (8)
        int numOfColor = OSInfo.getOSType() == OSInfo.OSType.AIX ? 8 : 4;

        int idx = resourceString.indexOf("ColorUse:");
        if (idx > -1) {
            while ( (idx < len) && (resourceString.charAt(idx) != ':')) idx++;
            idx++; // skip :
            if (resourceString.charAt(idx) == '\t') idx++; // skip \t
            String colorUse = resourceString.substring(idx,resourceString.indexOf("\n",idx));
            if ("HIGH_COLOR".equalsIgnoreCase(colorUse)) {
                numOfColor = 8;
            } else if ("MEDIUM_COLOR".equalsIgnoreCase(colorUse)) {
                numOfColor = 4;
            }
        }

        if (4 == numOfColor)
            loadSystemColorsForCDE4(systemColors, colors);
        else
            loadSystemColorsForCDE8(systemColors, colors);
   }

   private static void loadSystemColorsForCDE4(int[] systemColors, int[] colors) throws Exception {
        int r,g,b;
        systemColors[SystemColor.ACTIVE_CAPTION] = colors[0];
        systemColors[SystemColor.ACTIVE_CAPTION_BORDER] = colors[0];

        systemColors[SystemColor.INACTIVE_CAPTION] = colors[1];
        systemColors[SystemColor.INACTIVE_CAPTION_BORDER] = colors[1];

        systemColors[SystemColor.WINDOW] = colors[1];

        systemColors[SystemColor.WINDOW_BORDER] = colors[1];
        systemColors[SystemColor.MENU] = colors[1];

        systemColors[SystemColor.TEXT] = colors[3];

        systemColors[SystemColor.SCROLLBAR] = colors[1];
        systemColors[SystemColor.CONTROL] = colors[1];

        int activeFore;
        int inactiveFore;
        int textFore;


        r = (colors[0] & 0x00FF0000) >> 16;
        g = (colors[0] & 0x0000FF00) >> 8;
        b = (colors[0] & 0x000000FF);

        activeFore = MotifColorUtilities.calculateForegroundFromBackground(r,g,b);

        r = (colors[1] & 0x00FF0000) >> 16;
        g = (colors[1] & 0x0000FF00) >> 8;
        b = (colors[1] & 0x000000FF);

        inactiveFore = MotifColorUtilities.calculateForegroundFromBackground(r,g,b);

        int top_shadow = MotifColorUtilities.calculateTopShadowFromBackground(r,g,b);
        int bottom_shadow = MotifColorUtilities.calculateBottomShadowFromBackground(r,g,b);


        r = (colors[3] & 0x00FF0000) >> 16;
        g = (colors[3] & 0x0000FF00) >> 8;
        b = (colors[3] & 0x000000FF);

        textFore = MotifColorUtilities.calculateForegroundFromBackground(r,g,b);


        systemColors[SystemColor.ACTIVE_CAPTION_TEXT] = activeFore;
        systemColors[SystemColor.INACTIVE_CAPTION_TEXT] = inactiveFore;
        systemColors[SystemColor.WINDOW_TEXT] = inactiveFore;
        systemColors[SystemColor.MENU_TEXT] = inactiveFore;
        systemColors[SystemColor.TEXT_TEXT] = textFore;
        systemColors[SystemColor.TEXT_HIGHLIGHT] = MotifColorUtilities.BLACK;
        systemColors[SystemColor.TEXT_HIGHLIGHT_TEXT] = MotifColorUtilities.DEFAULT_COLOR;
        systemColors[SystemColor.CONTROL_TEXT] = inactiveFore;
        Color tmp = new Color(top_shadow);
        systemColors[SystemColor.CONTROL_HIGHLIGHT] =  top_shadow;
        systemColors[SystemColor.CONTROL_LT_HIGHLIGHT] =  tmp.brighter().getRGB();

        tmp = new Color(bottom_shadow);
        systemColors[SystemColor.CONTROL_SHADOW] =  bottom_shadow;
        systemColors[SystemColor.CONTROL_DK_SHADOW] = tmp.darker().getRGB();

    }

    private static void loadSystemColorsForCDE8(int[] systemColors, int[] colors) throws Exception {
        int r,g,b;
        systemColors[SystemColor.ACTIVE_CAPTION] = colors[0];
        systemColors[SystemColor.ACTIVE_CAPTION_BORDER] = colors[0];

        systemColors[SystemColor.INACTIVE_CAPTION] = colors[1];
        systemColors[SystemColor.INACTIVE_CAPTION_BORDER] = colors[1];

        systemColors[SystemColor.WINDOW] = colors[4];

        systemColors[SystemColor.MENU] = colors[5];

        systemColors[SystemColor.TEXT] = colors[3];
        systemColors[SystemColor.TEXT_HIGHLIGHT_TEXT] = colors[3];

        systemColors[SystemColor.SCROLLBAR] = colors[4];
        systemColors[SystemColor.CONTROL] = colors[4];
        systemColors[SystemColor.INFO] =  colors[4];

        int activeFore;
        int inactiveFore;
        int textFore;


        r = (colors[0] & 0x00FF0000) >> 16;
        g = (colors[0] & 0x0000FF00) >> 8;
        b = (colors[0] & 0x000000FF);

        activeFore = MotifColorUtilities.calculateForegroundFromBackground(r,g,b);

        r = (colors[1] & 0x00FF0000) >> 16;
        g = (colors[1] & 0x0000FF00) >> 8;
        b = (colors[1] & 0x000000FF);

        inactiveFore = MotifColorUtilities.calculateForegroundFromBackground(r,g,b);

        r = (colors[3] & 0x00FF0000) >> 16;
        g = (colors[3] & 0x0000FF00) >> 8;
        b = (colors[3] & 0x000000FF);

        textFore = MotifColorUtilities.calculateForegroundFromBackground(r,g,b);

        r = (colors[4] & 0x00FF0000) >> 16;
        g = (colors[4] & 0x0000FF00) >> 8;
        b = (colors[4] & 0x000000FF);

        int windowFore = MotifColorUtilities.calculateForegroundFromBackground(r,g,b);

        int top_shadow = MotifColorUtilities.calculateTopShadowFromBackground(r,g,b);
        int bottom_shadow = MotifColorUtilities.calculateBottomShadowFromBackground(r,g,b);


        r = (colors[5] & 0x00FF0000) >> 16;
        g = (colors[5] & 0x0000FF00) >> 8;
        b = (colors[5] & 0x000000FF);

        int menuFore = MotifColorUtilities.calculateForegroundFromBackground(r,g,b);

        systemColors[SystemColor.ACTIVE_CAPTION_TEXT] = activeFore;
        systemColors[SystemColor.INACTIVE_CAPTION_TEXT] = inactiveFore;
        systemColors[SystemColor.WINDOW_BORDER] = MotifColorUtilities.BLACK;
        systemColors[SystemColor.WINDOW_TEXT] = windowFore;
        systemColors[SystemColor.MENU_TEXT] = menuFore;
        systemColors[SystemColor.TEXT_TEXT] = textFore;
        systemColors[SystemColor.TEXT_HIGHLIGHT] = textFore;
        systemColors[SystemColor.CONTROL_TEXT] = windowFore;
        Color tmp = new Color(top_shadow);
        systemColors[SystemColor.CONTROL_HIGHLIGHT] =  top_shadow;
        systemColors[SystemColor.CONTROL_LT_HIGHLIGHT] =  tmp.brighter().getRGB();

        tmp = new Color(bottom_shadow);
        systemColors[SystemColor.CONTROL_SHADOW] =  bottom_shadow;
        systemColors[SystemColor.CONTROL_DK_SHADOW] = tmp.darker().getRGB();

        systemColors[SystemColor.INFO_TEXT] = windowFore;

    }

    static void loadMotifDefaultColors(int[] systemColors) {
        //fix for 5092883. WINDOW should be light gray and TEXT should be WHITE to look similar to Motif
        systemColors[SystemColor.WINDOW] = MotifColorUtilities.MOTIF_WINDOW_COLOR;
        systemColors[SystemColor.TEXT] = MotifColorUtilities.WHITE;
        systemColors[SystemColor.WINDOW_TEXT] = MotifColorUtilities.BLACK;
        systemColors[SystemColor.MENU_TEXT] = MotifColorUtilities.BLACK;
        systemColors[SystemColor.ACTIVE_CAPTION_TEXT] = MotifColorUtilities.BLACK;
        systemColors[SystemColor.INACTIVE_CAPTION_TEXT] = MotifColorUtilities.BLACK;
        systemColors[SystemColor.TEXT_TEXT] = MotifColorUtilities.BLACK;
        systemColors[SystemColor.TEXT_HIGHLIGHT] = MotifColorUtilities.BLACK;
        systemColors[SystemColor.TEXT_HIGHLIGHT_TEXT] = MotifColorUtilities.DEFAULT_COLOR;
        systemColors[SystemColor.CONTROL_TEXT] = MotifColorUtilities.BLACK;
        systemColors[SystemColor.WINDOW_BORDER] = MotifColorUtilities.DEFAULT_COLOR;
        systemColors[SystemColor.MENU] = MotifColorUtilities.DEFAULT_COLOR;
        systemColors[SystemColor.SCROLLBAR] = MotifColorUtilities.DEFAULT_COLOR;
        systemColors[SystemColor.CONTROL] = MotifColorUtilities.MOTIF_WINDOW_COLOR;

        int r = (MotifColorUtilities.DEFAULT_COLOR & 0x00FF0000) >> 16;
        int g = (MotifColorUtilities.DEFAULT_COLOR & 0x0000FF00) >> 8;
        int b = (MotifColorUtilities.DEFAULT_COLOR & 0x000000FF);


        int top_shadow = MotifColorUtilities.calculateTopShadowFromBackground(r,g,b);
        int bottom_shadow = MotifColorUtilities.calculateBottomShadowFromBackground(r,g,b);

        Color tmp = new Color(top_shadow);
        systemColors[SystemColor.CONTROL_HIGHLIGHT] =  top_shadow;
        systemColors[SystemColor.CONTROL_LT_HIGHLIGHT] =  tmp.brighter().getRGB();

        tmp = new Color(bottom_shadow);
        systemColors[SystemColor.CONTROL_SHADOW] =  bottom_shadow;
        systemColors[SystemColor.CONTROL_DK_SHADOW] = tmp.darker().getRGB();

    }


    @SuppressWarnings("removal")
    static void loadSystemColors(int[] systemColors) {
        if ("Linux".equals(AccessController.doPrivileged(new GetPropertyAction("os.name")))) { // Load motif default colors on Linux.
            loadMotifDefaultColors(systemColors);
        }
        else
        {
            try {
                loadSystemColorsForCDE(systemColors);
            }
            catch (Exception e) // Failure to load CDE colors.
            {
                loadMotifDefaultColors(systemColors);
            }
        }
    }
}
