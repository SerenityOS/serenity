/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.windows;

import java.awt.Color;
import java.awt.Font;
import static java.awt.RenderingHints.*;
import java.awt.RenderingHints;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import sun.util.logging.PlatformLogger;

import sun.awt.SunToolkit;

/*
 * Class encapsulating Windows desktop properties.;
 * This class exposes Windows user configuration values
 * for things like:
 *      Window metrics
 *      Accessibility, display settings
 *      Animation effects
 *      Colors
 *      Etc, etc etc.
 *
 * It's primary use is so that Windows specific Java code;
 * like the Windows Pluggable Look-and-Feel can better adapt
 * itself when running on a Windows platform.
 */
final class WDesktopProperties {
    private static final PlatformLogger log = PlatformLogger.getLogger("sun.awt.windows.WDesktopProperties");
    private static final String PREFIX = "win.";
    private static final String FILE_PREFIX = "awt.file.";
    private static final String PROP_NAMES = "win.propNames";

    private long pData;

    static {
        initIDs();
    }

    private WToolkit wToolkit;

    private HashMap<String, Object> map = new HashMap<String, Object>();

    /**
     * Initialize JNI field and method IDs
     */
    private static native void initIDs();

    static boolean isWindowsProperty(String name) {
        return name.startsWith(PREFIX) || name.startsWith(FILE_PREFIX) ||
            name.equals(SunToolkit.DESKTOPFONTHINTS);
    }

    WDesktopProperties(WToolkit wToolkit) {
        this.wToolkit = wToolkit;
        init();
    }

    private native void init();

    /*
     * Returns String[] containing available property names
     */
    private String [] getKeyNames() {
        String[] sortedKeys = map.keySet().toArray(new String[0]);
        Arrays.sort(sortedKeys);
        return sortedKeys;
    }

    /*
     * Reads Win32 configuration information and
     * updates hashmap values
     */
    private native void getWindowsParameters();

    /*
     * Called from native code to set a boolean property
     */
    private synchronized void setBooleanProperty(String key, boolean value) {
        assert( key != null );
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine(key + "=" + String.valueOf(value));
        }
        map.put(key, Boolean.valueOf(value));
    }

    /*
     * Called from native code to set an integer property
     */
    private synchronized void setIntegerProperty(String key, int value) {
        assert( key != null );
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine(key + "=" + String.valueOf(value));
        }
        map.put(key, Integer.valueOf(value));
    }

    /*
     * Called from native code to set a string property
     */
    private synchronized void setStringProperty(String key, String value) {
        assert( key != null );
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine(key + "=" + value);
        }
        map.put(key, value);
    }

    /*
     * Called from native code to set a color property
     */
    private synchronized void setColorProperty(String key, int r, int g, int b) {
        assert( key != null && r <= 255 && g <=255 && b <= 255 );
        Color color = new Color(r, g, b);
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine(key + "=" + color);
        }
        map.put(key, color);
    }

    /* Map of known windows font aliases to the preferred JDK name */
    static HashMap<String,String> fontNameMap;
    static {
        fontNameMap = new HashMap<String,String>();
        fontNameMap.put("Courier", Font.MONOSPACED);
        fontNameMap.put("MS Serif", "Microsoft Serif");
        fontNameMap.put("MS Sans Serif", "Microsoft Sans Serif");
        fontNameMap.put("Terminal", Font.DIALOG);
        fontNameMap.put("FixedSys", Font.MONOSPACED);
        fontNameMap.put("System", Font.DIALOG);
    }
    /*
     * Called from native code to set a font property
     */
    private synchronized void setFontProperty(String key, String name, int style, int size) {
        assert( key != null && style <= (Font.BOLD|Font.ITALIC)  && size >= 0 );

        String mappedName = fontNameMap.get(name);
        if (mappedName != null) {
            name = mappedName;
        }
        Font    font = new Font(name, style, size);
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine(key + "=" + font);
        }
        map.put(key, font);

        String sizeKey = key + ".height";
        Integer iSize = Integer.valueOf(size);
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine(sizeKey + "=" + iSize);
        }
        map.put(sizeKey, iSize);
    }

    /*
     * Called from native code to set a sound event property
     */
    private synchronized void setSoundProperty(String key, String winEventName) {
        assert( key != null && winEventName != null );

        Runnable soundRunnable = new WinPlaySound(winEventName);
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine(key + "=" + soundRunnable);
        }
        map.put(key, soundRunnable);
    }

    /*
     * Plays Windows sound event
     */
    private native void playWindowsSound(String winEventName);

    class WinPlaySound implements Runnable {
        String  winEventName;

        WinPlaySound(String winEventName) {
            this.winEventName = winEventName;
        }

        @Override
        public void run() {
            WDesktopProperties.this.playWindowsSound(winEventName);
        }

        public String toString() {
            return "WinPlaySound("+winEventName+")";
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            try {
                return winEventName.equals(((WinPlaySound)o).winEventName);
            } catch (Exception e) {
                return false;
            }
        }

        public int hashCode() {
            return winEventName.hashCode();
        }
    }

    /*
     * Called by WToolkit when Windows settings change-- we (re)load properties and
     * set new values.
     */
    @SuppressWarnings("unchecked")
    synchronized Map<String, Object> getProperties() {
        ThemeReader.flush();

        // load the changed properties into a new hashmap
        map = new HashMap<String, Object>();
        getWindowsParameters();
        map.put(SunToolkit.DESKTOPFONTHINTS, SunToolkit.getDesktopFontHints());
        map.put(PROP_NAMES, getKeyNames());
        // DnD uses one value for x and y drag diff, but Windows provides
        // separate ones.  For now, just use the x value - rnk
        map.put("DnD.Autoscroll.cursorHysteresis", map.get("win.drag.x"));

        return (Map<String, Object>) map.clone();
    }

    /*
     * This returns the value for the desktop property "awt.font.desktophints"
     * It builds this using the Windows desktop properties to return
     * them as platform independent hints.
     * This requires that the Windows properties have already been gathered
     * and placed in "map"
     */
    synchronized RenderingHints getDesktopAAHints() {

        /* Equate "DEFAULT" to "OFF", which it is in our implementation.
         * Doing this prevents unnecessary pipeline revalidation where
         * the value OFF is detected as a distinct value by SunGraphics2D
         */
        Object fontSmoothingHint = VALUE_TEXT_ANTIALIAS_DEFAULT;
        Integer fontSmoothingContrast = null;

        Boolean smoothingOn = (Boolean)map.get("win.text.fontSmoothingOn");

        if (smoothingOn != null && smoothingOn.equals(Boolean.TRUE)) {
            Integer typeID = (Integer)map.get("win.text.fontSmoothingType");
            /* "1" is GASP/Standard but we'll also use that if the return
             * value is anything other than "2" for LCD.
             */
            if (typeID == null || typeID.intValue() <= 1 ||
                typeID.intValue() > 2) {
                fontSmoothingHint = VALUE_TEXT_ANTIALIAS_GASP;
            } else {
                /* Recognise 0 as BGR and everything else as RGB - note
                 * that 1 is the expected value for RGB.
                 */
                Integer orientID = (Integer)
                    map.get("win.text.fontSmoothingOrientation");
                /* 0 is BGR, 1 is RGB. Other values, assume RGB */
                if (orientID == null || orientID.intValue() != 0) {
                    fontSmoothingHint = VALUE_TEXT_ANTIALIAS_LCD_HRGB;
                } else {
                    fontSmoothingHint = VALUE_TEXT_ANTIALIAS_LCD_HBGR;
                }

                fontSmoothingContrast = (Integer)
                    map.get("win.text.fontSmoothingContrast");
                if (fontSmoothingContrast == null) {
                    fontSmoothingContrast = Integer.valueOf(140);
                } else {
                    /* Windows values are scaled 10x those of Java 2D */
                    fontSmoothingContrast =
                        Integer.valueOf(fontSmoothingContrast.intValue()/10);
                }
            }
        }

        RenderingHints hints = new RenderingHints(null);
        hints.put(KEY_TEXT_ANTIALIASING, fontSmoothingHint);
        if (fontSmoothingContrast != null) {
            hints.put(KEY_TEXT_LCD_CONTRAST, fontSmoothingContrast);
        }
        return hints;
    }
}
