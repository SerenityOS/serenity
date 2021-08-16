/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.plaf.FontUIResource;

import sun.awt.FontConfiguration;
import sun.awt.HeadlessToolkit;
import sun.awt.util.ThreadGroupUtils;
import sun.lwawt.macosx.*;

public final class CFontManager extends SunFontManager {
    private static Hashtable<String, Font2D> genericFonts = new Hashtable<String, Font2D>();

    @Override
    protected FontConfiguration createFontConfiguration() {
        FontConfiguration fc = new CFontConfiguration(this);
        fc.init();
        return fc;
    }

    @Override
    public FontConfiguration createFontConfiguration(boolean preferLocaleFonts,
                                                     boolean preferPropFonts)
    {
        return new CFontConfiguration(this, preferLocaleFonts, preferPropFonts);
    }

    /*
     * Returns an array of two strings. The first element is the
     * name of the font. The second element is the file name.
     */
    @Override
    protected String[] getDefaultPlatformFont() {
        return new String[]{"Lucida Grande",
                            "/System/Library/Fonts/LucidaGrande.ttc"};
    }

    // This is a way to register any kind of Font2D, not just files and composites.
    public static Font2D[] getGenericFonts() {
        return genericFonts.values().toArray(new Font2D[0]);
    }

    public Font2D registerGenericFont(Font2D f)
    {
        return registerGenericFont(f, false);
    }
    public Font2D registerGenericFont(Font2D f, boolean logicalFont)
    {
        int rank = 4;

        String fontName = f.fullName;
        String familyName = f.familyName;

        if (fontName == null || fontName.isEmpty()) {
            return null;
        }

        // logical fonts always need to be added to the family
        // plus they never need to be added to the generic font list
        // or the fullNameToFont table since they are covers for
        // already existing fonts in this list
        if (logicalFont || !genericFonts.containsKey(fontName)) {
            if (FontUtilities.debugFonts()) {
                FontUtilities.logInfo("Add to Family " + familyName +
                    ", Font " + fontName + " rank=" + rank);
            }
            FontFamily family = FontFamily.getFamily(familyName);
            if (family == null) {
                family = new FontFamily(familyName, false, rank);
                family.setFont(f, f.style);
            } else if (family.getRank() >= rank) {
                family.setFont(f, f.style);
            }
            if (!logicalFont)
            {
                genericFonts.put(fontName, f);
                fullNameToFont.put(fontName.toLowerCase(Locale.ENGLISH), f);
            }
            return f;
        } else {
            return genericFonts.get(fontName);
        }
    }

    @Override
    public Font2D[] getRegisteredFonts() {
        Font2D[] regFonts = super.getRegisteredFonts();

        // Add in the Mac OS X native fonts
        Font2D[] genericFonts = getGenericFonts();
        Font2D[] allFonts = new Font2D[regFonts.length+genericFonts.length];
        System.arraycopy(regFonts, 0, allFonts, 0, regFonts.length);
        System.arraycopy(genericFonts, 0, allFonts, regFonts.length, genericFonts.length);

        return allFonts;
    }

    @Override
    protected void addNativeFontFamilyNames(TreeMap<String, String> familyNames, Locale requestedLocale) {
        Font2D[] genericfonts = getGenericFonts();
        for (int i=0; i < genericfonts.length; i++) {
            if (!(genericfonts[i] instanceof NativeFont)) {
                String name = genericfonts[i].getFamilyName(requestedLocale);
                familyNames.put(name.toLowerCase(requestedLocale), name);
            }
        }
    }

    protected void registerFontsInDir(final String dirName, boolean useJavaRasterizer,
                                      int fontRank, boolean defer, boolean resolveSymLinks) {

        @SuppressWarnings("removal")
        String[] files = AccessController.doPrivileged((PrivilegedAction<String[]>) () -> {
            return new File(dirName).list(getTrueTypeFilter());
        });

        if (files == null) {
           return;
        } else {
            for (String f : files) {
                loadNativeDirFonts(dirName+File.separator+f);
            }
        }
        super.registerFontsInDir(dirName, useJavaRasterizer, fontRank, defer, resolveSymLinks);
    }

    private native void loadNativeDirFonts(String fontPath);
    private native void loadNativeFonts();

    void registerFont(String fontName, String fontFamilyName) {
        final CFont font = new CFont(fontName, fontFamilyName);

        registerGenericFont(font);
    }

    void registerItalicDerived() {
        FontFamily[] famArr = FontFamily.getAllFontFamilies();
        for (int i=0; i<famArr.length; i++) {
            FontFamily family = famArr[i];

            Font2D f2dPlain = family.getFont(Font.PLAIN);
            if (f2dPlain != null && !(f2dPlain instanceof CFont)) continue;
            Font2D f2dBold = family.getFont(Font.BOLD);
            if (f2dBold != null && !(f2dBold instanceof CFont)) continue;
            Font2D f2dItalic = family.getFont(Font.ITALIC);
            if (f2dItalic != null && !(f2dItalic instanceof CFont)) continue;
            Font2D f2dBoldItalic = family.getFont(Font.BOLD|Font.ITALIC);
            if (f2dBoldItalic != null && !(f2dBoldItalic instanceof CFont)) continue;

            CFont plain = (CFont)f2dPlain;
            CFont bold = (CFont)f2dBold;
            CFont italic = (CFont)f2dItalic;
            CFont boldItalic = (CFont)f2dBoldItalic;

            if (bold == null) bold = plain;
            if (plain == null && bold == null) continue;
            if (italic != null && boldItalic != null) continue;
            if (plain != null && italic == null) {
               registerGenericFont(plain.createItalicVariant(), true);
            }
            if (bold != null && boldItalic == null) {
               registerGenericFont(bold.createItalicVariant(), true);
            }
        }
    }

    Object waitForFontsToBeLoaded  = new Object();
    private boolean loadedAllFonts = false;

    @SuppressWarnings("removal")
    public void loadFonts()
    {
        synchronized(waitForFontsToBeLoaded)
        {
            super.loadFonts();
            java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction<Object>() {
                    public Object run() {
                        if (!loadedAllFonts) {
                           loadNativeFonts();
                           registerItalicDerived();
                           loadedAllFonts = true;
                        }
                        return null;
                    }
                }
            );

            String defaultFont = "Lucida Grande";
            String defaultFallback = "Lucida Grande";

            setupLogicalFonts("Dialog", defaultFont, defaultFallback);
            setupLogicalFonts("Serif", "Times", "Times");
            setupLogicalFonts("SansSerif", defaultFont, defaultFallback);
            setupLogicalFonts("Monospaced", "Menlo", "Courier");
            setupLogicalFonts("DialogInput", defaultFont, defaultFallback);
        }
    }

    protected void setupLogicalFonts(String logicalName, String realName, String fallbackName) {
        FontFamily realFamily = getFontFamilyWithExtraTry(logicalName, realName, fallbackName);

        cloneStyledFont(realFamily, logicalName, Font.PLAIN);
        cloneStyledFont(realFamily, logicalName, Font.BOLD);
        cloneStyledFont(realFamily, logicalName, Font.ITALIC);
        cloneStyledFont(realFamily, logicalName, Font.BOLD | Font.ITALIC);
    }

    protected FontFamily getFontFamilyWithExtraTry(String logicalName, String realName, String fallbackName){
        FontFamily family = getFontFamily(realName, fallbackName);
        if (family != null) return family;

        // at this point, we recognize that we probably needed a fallback font
        super.loadFonts();

        family = getFontFamily(realName, fallbackName);
        if (family != null) return family;

        System.err.println("Warning: the fonts \"" + realName + "\" and \"" + fallbackName + "\" are not available for the Java logical font \"" + logicalName + "\", which may have unexpected appearance or behavior. Re-enable the \""+ realName +"\" font to remove this warning.");
        return null;
    }

    protected FontFamily getFontFamily(String realName, String fallbackName){
        FontFamily family = FontFamily.getFamily(realName);
        if (family != null) return family;

        family = FontFamily.getFamily(fallbackName);
        if (family != null){
            System.err.println("Warning: the font \"" + realName + "\" is not available, so \"" + fallbackName + "\" has been substituted, but may have unexpected appearance or behavor. Re-enable the \""+ realName +"\" font to remove this warning.");
            return family;
        }

        return null;
    }

    protected boolean cloneStyledFont(FontFamily realFamily, String logicalFamilyName, int style) {
        if (realFamily == null) return false;

        Font2D realFont = realFamily.getFontWithExactStyleMatch(style);
        if (realFont == null || !(realFont instanceof CFont)) return false;

        CFont newFont = new CFont((CFont)realFont, logicalFamilyName);
        registerGenericFont(newFont, true);

        return true;
    }

    @Override
    public String getFontPath(boolean noType1Fonts) {
        // In the case of the Cocoa toolkit, since we go through NSFont, we don't need to register /Library/Fonts
        Toolkit tk = Toolkit.getDefaultToolkit();
        if (tk instanceof HeadlessToolkit) {
            tk = ((HeadlessToolkit)tk).getUnderlyingToolkit();
        }
        if (tk instanceof LWCToolkit) {
            return "";
        }

        // X11 case
        return "/Library/Fonts";
    }

    @Override
    protected FontUIResource getFontConfigFUIR(
            String family, int style, int size)
    {
        String mappedName = FontUtilities.mapFcName(family);
        if (mappedName == null) {
            mappedName = "sansserif";
        }
        return new FontUIResource(mappedName, style, size);
    }

    // Only implemented on Windows
    @Override
    protected void populateFontFileNameMap(HashMap<String, String> fontToFileMap, HashMap<String, String> fontToFamilyNameMap,
            HashMap<String, ArrayList<String>> familyToFontListMap, Locale locale) {}
}
