/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import sun.awt.windows.WFontConfiguration;
import sun.font.FontManager;
import sun.font.SunFontManager;
import sun.font.TrueTypeFont;

/**
 * The X11 implementation of {@link FontManager}.
 */
public final class Win32FontManager extends SunFontManager {

    @SuppressWarnings("removal")
    private static final TrueTypeFont eudcFont =
            AccessController.doPrivileged(new PrivilegedAction<TrueTypeFont>() {
                public TrueTypeFont run() {
                    String eudcFile = getEUDCFontFile();
                    if (eudcFile != null) {
                        try {
                            /* Must use Java rasteriser since GDI doesn't
                             * enumerate (allow direct use) of EUDC fonts.
                             */
                            return new TrueTypeFont(eudcFile, null, 0,
                                                        true, false);
                        } catch (FontFormatException e) {
                        }
                    }
                    return null;
                }
            });

    /* Used on Windows to obtain from the windows registry the name
     * of a file containing the system EUFC font. If running in one of
     * the locales for which this applies, and one is defined, the font
     * defined by this file is appended to all composite fonts as a
     * fallback component.
     */
    private static native String getEUDCFontFile();

    public TrueTypeFont getEUDCFont() {
        return eudcFont;
    }

    @SuppressWarnings("removal")
    public Win32FontManager() {
        super();
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {

                    /* Register the JRE fonts so that the native platform can
                     * access them. This is used only on Windows so that when
                     * printing the printer driver can access the fonts.
                     */
                    registerJREFontsWithPlatform(jreFontDirName);
                    return null;
                }
            });
    }

    /**
     * Whether registerFontFile expects absolute or relative
     * font file names.
     */
    protected boolean useAbsoluteFontFileNames() {
        return false;
    }

    /* Unlike the shared code version, this expects a base file name -
     * not a full path name.
     * The font configuration file has base file names and the FontConfiguration
     * class reports these back to the GraphicsEnvironment, so these
     * are the componentFileNames of CompositeFonts.
     */
    protected void registerFontFile(String fontFileName, String[] nativeNames,
                                    int fontRank, boolean defer) {

        // REMIND: case compare depends on platform
        if (registeredFontFiles.contains(fontFileName)) {
            return;
        }
        registeredFontFiles.add(fontFileName);

        int fontFormat;
        if (getTrueTypeFilter().accept(null, fontFileName)) {
            fontFormat = SunFontManager.FONTFORMAT_TRUETYPE;
        } else if (getType1Filter().accept(null, fontFileName)) {
            fontFormat = SunFontManager.FONTFORMAT_TYPE1;
        } else {
            /* on windows we don't use/register native fonts */
            return;
        }

        if (fontPath == null) {
            fontPath = getPlatformFontPath(noType1Font);
        }

        /* Look in the JRE font directory first.
         * This is playing it safe as we would want to find fonts in the
         * JRE font directory ahead of those in the system directory
         */
        String tmpFontPath = jreFontDirName+File.pathSeparator+fontPath;
        StringTokenizer parser = new StringTokenizer(tmpFontPath,
                                                     File.pathSeparator);

        boolean found = false;
        try {
            while (!found && parser.hasMoreTokens()) {
                String newPath = parser.nextToken();
                boolean isJREFont = newPath.equals(jreFontDirName);
                File theFile = new File(newPath, fontFileName);
                if (theFile.canRead()) {
                    found = true;
                    String path = theFile.getAbsolutePath();
                    if (defer) {
                        registerDeferredFont(fontFileName, path,
                                             nativeNames,
                                             fontFormat, isJREFont,
                                             fontRank);
                    } else {
                        registerFontFile(path, nativeNames,
                                         fontFormat, isJREFont,
                                         fontRank);
                    }
                    break;
                }
            }
        } catch (NoSuchElementException e) {
            System.err.println(e);
        }
        if (!found) {
            addToMissingFontFileList(fontFileName);
        }
    }

    @Override
    protected FontConfiguration createFontConfiguration() {

       FontConfiguration fc = new WFontConfiguration(this);
       fc.init();
       return fc;
    }

    @Override
    public FontConfiguration createFontConfiguration(boolean preferLocaleFonts,
            boolean preferPropFonts) {

        return new WFontConfiguration(this,
                                      preferLocaleFonts,preferPropFonts);
    }

    protected void
        populateFontFileNameMap(HashMap<String,String> fontToFileMap,
                                HashMap<String,String> fontToFamilyNameMap,
                                HashMap<String,ArrayList<String>>
                                familyToFontListMap,
                                Locale locale) {

        populateFontFileNameMap0(fontToFileMap, fontToFamilyNameMap,
                                 familyToFontListMap, locale);

    }

    private static native void
        populateFontFileNameMap0(HashMap<String,String> fontToFileMap,
                                 HashMap<String,String> fontToFamilyNameMap,
                                 HashMap<String,ArrayList<String>>
                                     familyToFontListMap,
                                 Locale locale);

    protected synchronized native String getFontPath(boolean noType1Fonts);

    @Override
    protected String[] getDefaultPlatformFont() {
        String[] info = new String[2];
        info[0] = "Arial";
        info[1] = "c:\\windows\\fonts";
        final String[] dirs = getPlatformFontDirs(true);
        if (dirs.length > 1) {
            @SuppressWarnings("removal")
            String dir = (String)
                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                        public Object run() {
                            for (int i=0; i<dirs.length; i++) {
                                String path =
                                    dirs[i] + File.separator + "arial.ttf";
                                File file = new File(path);
                                if (file.exists()) {
                                    return dirs[i];
                                }
                            }
                            return null;
                        }
                    });
            if (dir != null) {
                info[1] = dir;
            }
        } else {
            info[1] = dirs[0];
        }
        info[1] = info[1] + File.separator + "arial.ttf";
        return info;
    }

    /* register only TrueType/OpenType fonts
     * Because these need to be registed just for use when printing,
     * we defer the actual registration and the static initialiser
     * for the printing class makes the call to registerJREFontsForPrinting()
     */
    static String fontsForPrinting = null;
    protected void registerJREFontsWithPlatform(String pathName) {
        fontsForPrinting = pathName;
    }

    @SuppressWarnings("removal")
    public static void registerJREFontsForPrinting() {
        final String pathName;
        synchronized (Win32GraphicsEnvironment.class) {
            GraphicsEnvironment.getLocalGraphicsEnvironment();
            if (fontsForPrinting == null) {
                return;
            }
            pathName = fontsForPrinting;
            fontsForPrinting = null;
        }
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction<Object>() {
                public Object run() {
                    File f1 = new File(pathName);
                    String[] ls = f1.list(SunFontManager.getInstance().
                            getTrueTypeFilter());
                    if (ls == null) {
                        return null;
                    }
                    for (int i=0; i <ls.length; i++ ) {
                        File fontFile = new File(f1, ls[i]);
                        registerFontWithPlatform(fontFile.getAbsolutePath());
                    }
                    return null;
                }
         });
    }

    private static native void registerFontWithPlatform(String fontName);

    private static native void deRegisterFontWithPlatform(String fontName);

    /**
     * populate the map with the most common windows fonts.
     */
    @Override
    public HashMap<String, FamilyDescription> populateHardcodedFileNameMap() {
        HashMap<String, FamilyDescription> platformFontMap
            = new HashMap<String, FamilyDescription>();
        FamilyDescription fd;

        /* Segoe UI is the default UI font for Vista and later, and
         * is used by the Win L&F which is used by FX too.
         * Tahoma is used for the Win L&F on XP.
         * Verdana is used in some FX UI controls.
         */
        fd = new FamilyDescription();
        fd.familyName = "Segoe UI";
        fd.plainFullName = "Segoe UI";
        fd.plainFileName = "segoeui.ttf";
        fd.boldFullName = "Segoe UI Bold";
        fd.boldFileName = "segoeuib.ttf";
        fd.italicFullName = "Segoe UI Italic";
        fd.italicFileName = "segoeuii.ttf";
        fd.boldItalicFullName = "Segoe UI Bold Italic";
        fd.boldItalicFileName = "segoeuiz.ttf";
        platformFontMap.put("segoe", fd);

        fd = new FamilyDescription();
        fd.familyName = "Tahoma";
        fd.plainFullName = "Tahoma";
        fd.plainFileName = "tahoma.ttf";
        fd.boldFullName = "Tahoma Bold";
        fd.boldFileName = "tahomabd.ttf";
        platformFontMap.put("tahoma", fd);

        fd = new FamilyDescription();
        fd.familyName = "Verdana";
        fd.plainFullName = "Verdana";
        fd.plainFileName = "verdana.TTF";
        fd.boldFullName = "Verdana Bold";
        fd.boldFileName = "verdanab.TTF";
        fd.italicFullName = "Verdana Italic";
        fd.italicFileName = "verdanai.TTF";
        fd.boldItalicFullName = "Verdana Bold Italic";
        fd.boldItalicFileName = "verdanaz.TTF";
        platformFontMap.put("verdana", fd);

        /* The following are important because they are the core
         * members of the default "Dialog" font.
         */
        fd = new FamilyDescription();
        fd.familyName = "Arial";
        fd.plainFullName = "Arial";
        fd.plainFileName = "ARIAL.TTF";
        fd.boldFullName = "Arial Bold";
        fd.boldFileName = "ARIALBD.TTF";
        fd.italicFullName = "Arial Italic";
        fd.italicFileName = "ARIALI.TTF";
        fd.boldItalicFullName = "Arial Bold Italic";
        fd.boldItalicFileName = "ARIALBI.TTF";
        platformFontMap.put("arial", fd);

        fd = new FamilyDescription();
        fd.familyName = "Symbol";
        fd.plainFullName = "Symbol";
        fd.plainFileName = "Symbol.TTF";
        platformFontMap.put("symbol", fd);

        fd = new FamilyDescription();
        fd.familyName = "WingDings";
        fd.plainFullName = "WingDings";
        fd.plainFileName = "WINGDING.TTF";
        platformFontMap.put("wingdings", fd);

        return platformFontMap;
    }
}
