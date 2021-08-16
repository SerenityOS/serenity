/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.Hashtable;
import sun.awt.FontDescriptor;
import sun.awt.FontConfiguration;
import sun.font.SunFontManager;
import java.nio.charset.*;

public final class WFontConfiguration extends FontConfiguration {

    // whether compatibility fallbacks for TimesRoman and Co. are used
    private boolean useCompatibilityFallbacks;

    public WFontConfiguration(SunFontManager fm) {
        super(fm);
        useCompatibilityFallbacks = "windows-1252".equals(encoding);
        initTables(encoding);
    }

    public WFontConfiguration(SunFontManager fm,
                              boolean preferLocaleFonts,
                              boolean preferPropFonts) {
        super(fm, preferLocaleFonts, preferPropFonts);
        useCompatibilityFallbacks = "windows-1252".equals(encoding);
    }

    @Override
    protected void initReorderMap() {
        if (encoding.equalsIgnoreCase("windows-31j")) {
            localeMap = new Hashtable<>();
            /* Substitute Mincho for Gothic in this one case.
             * Note the windows fontconfig files already contain the mapping:
             * filename.MS_Mincho=MSMINCHO.TTC
             * which isn't essential to this usage but avoids a call
             * to loadfonts in the event MSMINCHO.TTC has not otherwise
             * been opened and its fonts loaded.
             * Also note this usage is only enabled if a private flag is set.
             */
            localeMap.put("dialoginput.plain.japanese", "MS Mincho");
            localeMap.put("dialoginput.bold.japanese", "MS Mincho");
            localeMap.put("dialoginput.italic.japanese", "MS Mincho");
            localeMap.put("dialoginput.bolditalic.japanese", "MS Mincho");
        }
        reorderMap = new HashMap<>();
        reorderMap.put("UTF-8.hi", "devanagari");
        reorderMap.put("windows-1255", "hebrew");
        reorderMap.put("x-windows-874", "thai");
        reorderMap.put("windows-31j", "japanese");
        reorderMap.put("x-windows-949", "korean");
        reorderMap.put("GBK", "chinese-ms936");
        reorderMap.put("GB18030", "chinese-gb18030");
        reorderMap.put("x-windows-950", "chinese-ms950");
        reorderMap.put("x-MS950-HKSCS", split("chinese-ms950,chinese-hkscs"));
//      reorderMap.put("windows-1252", "alphabetic");
    }

    @Override
    protected void setOsNameAndVersion(){
        super.setOsNameAndVersion();
        if (osName.startsWith("Windows")){
            int p, q;
            p = osName.indexOf(' ');
            if (p == -1){
                osName = null;
            }
            else{
                q = osName.indexOf(' ', p + 1);
                if (q == -1){
                    osName = osName.substring(p + 1);
                }
                else{
                    osName = osName.substring(p + 1, q);
                }
            }
            osVersion = null;
        }
    }

    // overrides FontConfiguration.getFallbackFamilyName
    @Override
    public String getFallbackFamilyName(String fontName, String defaultFallback) {
        // maintain compatibility with old font.properties files, where
        // default file had aliases for timesroman & Co, while others didn't.
        if (useCompatibilityFallbacks) {
            String compatibilityName = getCompatibilityFamilyName(fontName);
            if (compatibilityName != null) {
                return compatibilityName;
            }
        }
        return defaultFallback;
    }

    @Override
    protected String makeAWTFontName(String platformFontName, String characterSubsetName) {
        String windowsCharset = subsetCharsetMap.get(characterSubsetName);
        if (windowsCharset == null) {
            windowsCharset = "DEFAULT_CHARSET";
        }
        return platformFontName + "," + windowsCharset;
    }

    @Override
    protected String getEncoding(String awtFontName, String characterSubsetName) {
        String encoding = subsetEncodingMap.get(characterSubsetName);
        if (encoding == null) {
            encoding = "default";
        }
        return encoding;
    }

    @Override
    protected Charset getDefaultFontCharset(String fontName) {
        return new WDefaultFontCharset(fontName);
    }

    @Override
    public String getFaceNameFromComponentFontName(String componentFontName) {
        // for Windows, the platform name is the face name
        return componentFontName;
    }

    @Override
    protected String getFileNameFromComponentFontName(String componentFontName) {
        return getFileNameFromPlatformName(componentFontName);
    }

    /**
     * Returns the component font name (face name plus charset) of the
     * font that should be used for AWT text components.
     */
    public String getTextComponentFontName(String familyName, int style) {
        FontDescriptor[] fontDescriptors = getFontDescriptors(familyName, style);
        String fontName = findFontWithCharset(fontDescriptors, textInputCharset);
        if ((fontName == null) && !textInputCharset.equals("DEFAULT_CHARSET")) {
            fontName = findFontWithCharset(fontDescriptors, "DEFAULT_CHARSET");
        }
        if (fontName == null) {
            fontName = (fontDescriptors.length > 0) ? fontDescriptors[0].getNativeName()
                                                    : "Arial,ANSI_CHARSET";
        }
        return fontName;
    }

    private String findFontWithCharset(FontDescriptor[] fontDescriptors, String charset) {
        String fontName = null;
        for (int i = 0; i < fontDescriptors.length; i++) {
            String componentFontName = fontDescriptors[i].getNativeName();
            if (componentFontName.endsWith(charset)) {
                fontName = componentFontName;
            }
        }
        return fontName;
    }

    private static HashMap<String, String> subsetCharsetMap = new HashMap<>();
    private static HashMap<String, String> subsetEncodingMap = new HashMap<>();
    private static String textInputCharset;

    private void initTables(String defaultEncoding) {
        subsetCharsetMap.put("alphabetic", "ANSI_CHARSET");
        subsetCharsetMap.put("alphabetic/1252", "ANSI_CHARSET");
        subsetCharsetMap.put("alphabetic/default", "DEFAULT_CHARSET");
        subsetCharsetMap.put("arabic", "ARABIC_CHARSET");
        subsetCharsetMap.put("chinese-ms936", "GB2312_CHARSET");
        subsetCharsetMap.put("chinese-gb18030", "GB2312_CHARSET");
        subsetCharsetMap.put("chinese-ms950", "CHINESEBIG5_CHARSET");
        subsetCharsetMap.put("chinese-hkscs", "CHINESEBIG5_CHARSET");
        subsetCharsetMap.put("cyrillic", "RUSSIAN_CHARSET");
        subsetCharsetMap.put("devanagari", "DEFAULT_CHARSET");
        subsetCharsetMap.put("dingbats", "SYMBOL_CHARSET");
        subsetCharsetMap.put("greek", "GREEK_CHARSET");
        subsetCharsetMap.put("hebrew", "HEBREW_CHARSET");
        subsetCharsetMap.put("japanese", "SHIFTJIS_CHARSET");
        subsetCharsetMap.put("korean", "HANGEUL_CHARSET");
        subsetCharsetMap.put("latin", "ANSI_CHARSET");
        subsetCharsetMap.put("symbol", "SYMBOL_CHARSET");
        subsetCharsetMap.put("thai", "THAI_CHARSET");

        subsetEncodingMap.put("alphabetic", "default");
        subsetEncodingMap.put("alphabetic/1252", "windows-1252");
        subsetEncodingMap.put("alphabetic/default", defaultEncoding);
        subsetEncodingMap.put("arabic", "windows-1256");
        subsetEncodingMap.put("chinese-ms936", "GBK");
        subsetEncodingMap.put("chinese-gb18030", "GB18030");
        if ("x-MS950-HKSCS".equals(defaultEncoding)) {
            subsetEncodingMap.put("chinese-ms950", "x-MS950-HKSCS");
        } else {
            subsetEncodingMap.put("chinese-ms950", "x-windows-950"); //MS950
        }
        subsetEncodingMap.put("chinese-hkscs", "sun.awt.HKSCS");
        subsetEncodingMap.put("cyrillic", "windows-1251");
        subsetEncodingMap.put("devanagari", "UTF-16LE");
        subsetEncodingMap.put("dingbats", "sun.awt.windows.WingDings");
        subsetEncodingMap.put("greek", "windows-1253");
        subsetEncodingMap.put("hebrew", "windows-1255");
        subsetEncodingMap.put("japanese", "windows-31j");
        subsetEncodingMap.put("korean", "x-windows-949");
        subsetEncodingMap.put("latin", "windows-1252");
        subsetEncodingMap.put("symbol", "sun.awt.Symbol");
        subsetEncodingMap.put("thai", "x-windows-874");

        if ("windows-1256".equals(defaultEncoding)) {
            textInputCharset = "ARABIC_CHARSET";
        } else if ("GBK".equals(defaultEncoding)) {
            textInputCharset = "GB2312_CHARSET";
        } else if ("GB18030".equals(defaultEncoding)) {
            textInputCharset = "GB2312_CHARSET";
        } else if ("x-windows-950".equals(defaultEncoding)) {
            textInputCharset = "CHINESEBIG5_CHARSET";
        } else if ("x-MS950-HKSCS".equals(defaultEncoding)) {
            textInputCharset = "CHINESEBIG5_CHARSET";
        } else if ("windows-1251".equals(defaultEncoding)) {
            textInputCharset = "RUSSIAN_CHARSET";
        } else if ("UTF-8".equals(defaultEncoding)) {
            textInputCharset = "DEFAULT_CHARSET";
        } else if ("windows-1253".equals(defaultEncoding)) {
            textInputCharset = "GREEK_CHARSET";
        } else if ("windows-1255".equals(defaultEncoding)) {
            textInputCharset = "HEBREW_CHARSET";
        } else if ("windows-31j".equals(defaultEncoding)) {
            textInputCharset = "SHIFTJIS_CHARSET";
        } else if ("x-windows-949".equals(defaultEncoding)) {
            textInputCharset = "HANGEUL_CHARSET";
        } else if ("x-windows-874".equals(defaultEncoding)) {
            textInputCharset = "THAI_CHARSET";
        } else {
            textInputCharset = "DEFAULT_CHARSET";
        }
    }
}
