/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.font;

import java.util.Locale;

import sun.awt.SunHints;
import sun.awt.SunToolkit;
import sun.util.logging.PlatformLogger;

/**
 * Small utility class to manage FontConfig.
 */
public class FontConfigManager {

    static boolean fontConfigFailed = false;

    /* This is populated by native */
    private static final FontConfigInfo fcInfo = new FontConfigInfo();

    /* Begin support for GTK Look and Feel - query libfontconfig and
     * return a composite Font to Swing that uses the desktop font(s).
     */

    /* These next three classes are just data structures.
     */
    public static class FontConfigFont {
        public String familyName;        // eg Bitstream Vera Sans
        public String styleStr;          // eg Bold
        public String fullName;          // eg Bitstream Vera Sans Bold
        public String fontFile;          // eg /usr/X11/lib/fonts/foo.ttf
    }

    public static class FcCompFont {
        public String fcName;            // eg sans
        public String fcFamily;          // eg sans
        public String jdkName;           // eg sansserif
        public int style;                // eg 0=PLAIN
        public FontConfigFont firstFont;
        public FontConfigFont[] allFonts;
        //boolean preferBitmaps;    // if embedded bitmaps preferred over AA
        public CompositeFont compFont;   // null if not yet created/known.
    }

    public static class FontConfigInfo {
        public int fcVersion;
        public String[] cacheDirs = new String[4];
    }

    /* fontconfig recognises slants roman, italic, as well as oblique,
     * and a slew of weights, where the ones that matter here are
     * regular and bold.
     * To fully qualify what we want, we can for example ask for (eg)
     * Font.PLAIN             : "serif:regular:roman"
     * Font.BOLD              : "serif:bold:roman"
     * Font.ITALIC            : "serif:regular:italic"
     * Font.BOLD|Font.ITALIC  : "serif:bold:italic"
     */
    private static String[] fontConfigNames = {
        "sans:regular:roman",
        "sans:bold:roman",
        "sans:regular:italic",
        "sans:bold:italic",

        "serif:regular:roman",
        "serif:bold:roman",
        "serif:regular:italic",
        "serif:bold:italic",

        "monospace:regular:roman",
        "monospace:bold:roman",
        "monospace:regular:italic",
        "monospace:bold:italic",
    };

    /* This array has the array elements created in Java code and is
     * passed down to native to be filled in.
     */
    private FcCompFont[] fontConfigFonts;

    /**
     * Instantiates a new FontConfigManager getting the default instance
     * of FontManager from the FontManagerFactory.
     */
    public FontConfigManager() {
    }

    /* Called from code that needs to know what are the AA settings
     * that apps using FC would pick up for the default desktop font.
     * Note apps can change the default desktop font. etc, so this
     * isn't certain to be right but its going to correct for most cases.
     * Native return values map to the text aa values in sun.awt.SunHints.
     * which is used to look up the renderinghint value object.
     */
    public static Object getFontConfigAAHint() {
        return getFontConfigAAHint("sans");
    }

    /* This is public solely so that for debugging purposes it can be called
     * with other names, which might (eg) include a size, eg "sans-24"
     * The return value is a text aa rendering hint value.
     * Normally we should call the no-args version.
     */
    public static Object getFontConfigAAHint(String fcFamily) {
        if (FontUtilities.isWindows) {
            return null;
        } else {
            int hint = getFontConfigAASettings(getFCLocaleStr(), fcFamily);
            if (hint < 0) {
                return null;
            } else {
                return SunHints.Value.get(SunHints.INTKEY_TEXT_ANTIALIASING,
                                          hint);
            }
        }
    }


    private static String getFCLocaleStr() {
        Locale l = SunToolkit.getStartupLocale();
        String localeStr = l.getLanguage();
        String country = l.getCountry();
        if (!country.isEmpty()) {
            localeStr = localeStr + "-" + country;
        }
        return localeStr;
    }

    /* This does cause the native libfontconfig to be loaded and unloaded,
     * but it does not incur the overhead of initialisation of its
     * data structures, so shouldn't have a measurable impact.
     */
    public static native int getFontConfigVersion();

    /* This can be made public if it's needed to force a re-read
     * rather than using the cached values. The re-read would be needed
     * only if some event signalled that the fontconfig has changed.
     * In that event this method would need to return directly the array
     * to be used by the caller in case it subsequently changed.
     */
    public synchronized void initFontConfigFonts(boolean includeFallbacks) {

        if (fontConfigFonts != null) {
            if (!includeFallbacks || (fontConfigFonts[0].allFonts != null)) {
                return;
            }
        }

        if (FontUtilities.isWindows || fontConfigFailed) {
            return;
        }

        long t0 = 0;
        if (FontUtilities.isLogging()) {
            t0 = System.nanoTime();
        }

        FcCompFont[] fontArr = new FcCompFont[fontConfigNames.length];

        for (int i = 0; i< fontArr.length; i++) {
            fontArr[i] = new FcCompFont();
            fontArr[i].fcName = fontConfigNames[i];
            int colonPos = fontArr[i].fcName.indexOf(':');
            fontArr[i].fcFamily = fontArr[i].fcName.substring(0, colonPos);
            fontArr[i].jdkName = FontUtilities.mapFcName(fontArr[i].fcFamily);
            fontArr[i].style = i % 4; // depends on array order.
        }
        getFontConfig(getFCLocaleStr(), fcInfo, fontArr, includeFallbacks);
        FontConfigFont anyFont = null;
        /* If don't find anything (eg no libfontconfig), then just return */
        for (int i = 0; i< fontArr.length; i++) {
            FcCompFont fci = fontArr[i];
            if (fci.firstFont == null) {
                if (FontUtilities.isLogging()) {
                    FontUtilities.logInfo("Fontconfig returned no font for " + fontArr[i].fcName);
                }
                fontConfigFailed = true;
            } else if (anyFont == null) {
                anyFont = fci.firstFont;
            }
        }

        if (anyFont == null) {
            if (FontUtilities.isLogging()) {
                FontUtilities.logInfo("Fontconfig returned no fonts at all.");
            }
            fontConfigFailed = true;
            return;
        } else if (fontConfigFailed) {
            for (int i = 0; i< fontArr.length; i++) {
                if (fontArr[i].firstFont == null) {
                    fontArr[i].firstFont = anyFont;
                }
            }
        }

        fontConfigFonts = fontArr;

        if (FontUtilities.isLogging()) {
            long t1 = System.nanoTime();
            FontUtilities.logInfo("Time spent accessing fontconfig="
                        + ((t1 - t0) / 1000000) + "ms.");

            for (int i = 0; i< fontConfigFonts.length; i++) {
                FcCompFont fci = fontConfigFonts[i];
                FontUtilities.logInfo("FC font " + fci.fcName+" maps to family " +
                            fci.firstFont.familyName +
                            " in file " + fci.firstFont.fontFile);
                if (fci.allFonts != null) {
                    for (int f=0;f<fci.allFonts.length;f++) {
                        FontConfigFont fcf = fci.allFonts[f];
                        FontUtilities.logInfo("Family=" + fcf.familyName +
                                    " Style="+ fcf.styleStr +
                                    " Fullname="+fcf.fullName +
                                    " File="+fcf.fontFile);
                    }
                }
            }
        }
    }

    public PhysicalFont registerFromFcInfo(FcCompFont fcInfo) {

        SunFontManager fm = SunFontManager.getInstance();

        /* If it's a TTC file we need to know that as we will need to
         * make sure we return the right font */
        String fontFile = fcInfo.firstFont.fontFile;
        int offset = fontFile.length()-4;
        if (offset <= 0) {
            return null;
        }
        String ext = fontFile.substring(offset).toLowerCase();
        boolean isTTC = ext.equals(".ttc");

        /* If this file is already registered, can just return its font.
         * However we do need to check in case it's a TTC as we need
         * a specific font, so rather than directly returning it, let
         * findFont2D resolve that.
         */
        PhysicalFont physFont = fm.getRegisteredFontFile(fontFile);
        if (physFont != null) {
            if (isTTC) {
                Font2D f2d = fm.findFont2D(fcInfo.firstFont.familyName,
                                           fcInfo.style,
                                           FontManager.NO_FALLBACK);
                if (f2d instanceof PhysicalFont) { /* paranoia */
                    return (PhysicalFont)f2d;
                } else {
                    return null;
                }
            } else {
                return physFont;
            }
        }

        /* If the font may hide a JRE font, we want to use the JRE version,
         * so make it point to the JRE font.
         */
        physFont = fm.findJREDeferredFont(fcInfo.firstFont.familyName,
                                          fcInfo.style);

        /* It is also possible the font file is on the "deferred" list,
         * in which case we can just initialise it now.
         */
        if (physFont == null &&
            fm.isDeferredFont(fontFile) == true) {
            physFont = fm.initialiseDeferredFont(fcInfo.firstFont.fontFile);
            /* use findFont2D to get the right font from TTC's */
            if (physFont != null) {
                if (isTTC) {
                    Font2D f2d = fm.findFont2D(fcInfo.firstFont.familyName,
                                               fcInfo.style,
                                               FontManager.NO_FALLBACK);
                    if (f2d instanceof PhysicalFont) { /* paranoia */
                        return (PhysicalFont)f2d;
                    } else {
                        return null;
                    }
                } else {
                    return physFont;
                }
            }
        }

        /* In the majority of cases we reach here, and need to determine
         * the type and rank to register the font.
         */
        if (physFont == null) {
            int fontFormat = SunFontManager.FONTFORMAT_NONE;
            int fontRank = Font2D.UNKNOWN_RANK;

            if (ext.equals(".ttf") || isTTC) {
                fontFormat = SunFontManager.FONTFORMAT_TRUETYPE;
                fontRank = Font2D.TTF_RANK;
            } else if (ext.equals(".pfa") || ext.equals(".pfb")) {
                fontFormat = SunFontManager.FONTFORMAT_TYPE1;
                fontRank = Font2D.TYPE1_RANK;
            }
            physFont = fm.registerFontFile(fcInfo.firstFont.fontFile, null,
                                      fontFormat, true, fontRank);
        }
        return physFont;
    }

    /*
     * We need to return a Composite font which has as the font in
     * its first slot one obtained from fontconfig.
     */
    public CompositeFont getFontConfigFont(String name, int style) {

        name = name.toLowerCase();

        initFontConfigFonts(false);
        if (fontConfigFonts == null) {
            // This avoids an immediate NPE if fontconfig look up failed
            // but doesn't guarantee this is a recoverable situation.
            return null;
        }

        FcCompFont fcInfo = null;
        for (int i=0; i<fontConfigFonts.length; i++) {
            if (name.equals(fontConfigFonts[i].fcFamily) &&
                style == fontConfigFonts[i].style) {
                fcInfo = fontConfigFonts[i];
                break;
            }
        }
        if (fcInfo == null) {
            fcInfo = fontConfigFonts[0];
        }

        if (FontUtilities.isLogging()) {
            FontUtilities.logInfo("FC name=" + name + " style=" + style +
                                  " uses " + fcInfo.firstFont.familyName +
                                  " in file: " + fcInfo.firstFont.fontFile);
        }

        if (fcInfo.compFont != null) {
            return fcInfo.compFont;
        }

        /* jdkFont is going to be used for slots 1..N and as a fallback.
         * Slot 0 will be the physical font from fontconfig.
         */
        FontManager fm = FontManagerFactory.getInstance();
        CompositeFont jdkFont = (CompositeFont)
            fm.findFont2D(fcInfo.jdkName, style, FontManager.LOGICAL_FALLBACK);

        if (fcInfo.firstFont.familyName == null ||
            fcInfo.firstFont.fontFile == null) {
            return (fcInfo.compFont = jdkFont);
        }

        /* First, see if the family and exact style is already registered.
         * If it is, use it. If it's not, then try to register it.
         * If that registration fails (signalled by null) just return the
         * regular JDK composite.
         * Algorithmically styled fonts won't match on exact style, so
         * will fall through this code, but the regisration code will
         * find that file already registered and return its font.
         */
        FontFamily family = FontFamily.getFamily(fcInfo.firstFont.familyName);
        PhysicalFont physFont = null;
        if (family != null) {
            Font2D f2D = family.getFontWithExactStyleMatch(fcInfo.style);
            if (f2D instanceof PhysicalFont) {
                physFont = (PhysicalFont)f2D;
            }
        }

        if (physFont == null ||
            !fcInfo.firstFont.fontFile.equals(physFont.platName)) {
            physFont = registerFromFcInfo(fcInfo);
            if (physFont == null) {
                return (fcInfo.compFont = jdkFont);
            }
            family = FontFamily.getFamily(physFont.getFamilyName(null));
        }

        /* Now register the fonts in the family (the other styles) after
         * checking that they aren't already registered and are actually in
         * a different file. They may be the same file in CJK cases.
         * For cases where they are different font files - eg as is common for
         * Latin fonts, then we rely on fontconfig to report these correctly.
         * Assume that all styles of this font are found by fontconfig,
         * so we can find all the family members which must be registered
         * together to prevent synthetic styling.
         */
        for (int i=0; i<fontConfigFonts.length; i++) {
            FcCompFont fc = fontConfigFonts[i];
            if (fc != fcInfo &&
                physFont.getFamilyName(null).equals(fc.firstFont.familyName) &&
                !fc.firstFont.fontFile.equals(physFont.platName) &&
                family.getFontWithExactStyleMatch(fc.style) == null) {

                registerFromFcInfo(fontConfigFonts[i]);
            }
        }

        /* Now we have a physical font. We will back this up with the JDK
         * logical font (sansserif, serif, or monospaced) that corresponds
         * to the Pango/GTK/FC logical font name.
         */
        return (fcInfo.compFont = new CompositeFont(physFont, jdkFont));
    }

    public FcCompFont[] getFontConfigFonts() {
        return fontConfigFonts;
    }

    /* Return an array of FcCompFont structs describing the primary
     * font located for each of fontconfig/GTK/Pango's logical font names.
     */
    private static native void getFontConfig(String locale,
                                             FontConfigInfo fcInfo,
                                             FcCompFont[] fonts,
                                             boolean includeFallbacks);

    void populateFontConfig(FcCompFont[] fcInfo) {
        fontConfigFonts = fcInfo;
    }

    FcCompFont[] loadFontConfig() {
        initFontConfigFonts(true);
        return fontConfigFonts;
    }

    FontConfigInfo getFontConfigInfo() {
        initFontConfigFonts(true);
        return fcInfo;
    }

    private static native int
    getFontConfigAASettings(String locale, String fcFamily);
}
