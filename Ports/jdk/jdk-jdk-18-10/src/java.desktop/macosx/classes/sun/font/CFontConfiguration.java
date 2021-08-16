/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.charset.Charset;
import java.util.HashMap;
import sun.awt.FontConfiguration;
import sun.font.CompositeFontDescriptor;
import sun.font.SunFontManager;

class CFontConfiguration extends FontConfiguration {

    private static CompositeFontDescriptor[] emptyDescriptors =
        new CompositeFontDescriptor[0];
    private static String[] emptyStrings = new String[0];

    public CFontConfiguration(SunFontManager fm) {
        super(fm);
    }

    public CFontConfiguration(SunFontManager fm,
                              boolean preferLocaleFonts,
                              boolean preferPropFonts)
    {
        super(fm, preferLocaleFonts, preferPropFonts);
    }

    /*
     * On Mac OS X we essentially ignore the font.properties file, and do
     * it all programatically.  The intention is end users will use things
     * like the Font Book to manage fonts. Plus our fonts automatically do
     * unicode substitution, so a localized font is not required.
     *
     * The following methods therefore act like stubs and return empty values.
     */

    @Override
    public int getNumberCoreFonts() {
        return 0;
    }

    @Override
    public String[] getPlatformFontNames() {
        return emptyStrings;
    }

    @Override
    public CompositeFontDescriptor[] get2DCompositeFontInfo() {
        return emptyDescriptors;
    }

    @Override
    protected String mapFileName(String fileName) {
        return "";
    }

    @Override
    protected Charset getDefaultFontCharset(String fontName) {
        return Charset.forName("ISO8859_1");
    }

    @Override
    protected String getEncoding(String awtFontName, String charSubsetName) {
        return "default";
    }

    @Override
    protected String getFaceNameFromComponentFontName(String compFontName) {
        return compFontName;
    }

    @Override
    protected String getFileNameFromComponentFontName(String compFontName) {
        return compFontName;
    }

    @Override
    public String getFallbackFamilyName(String fontName,
                                        String defaultFallback)
    {
        return defaultFallback;
    }

    @Override
    protected void initReorderMap() {
        reorderMap = new HashMap<>();
    }
}
