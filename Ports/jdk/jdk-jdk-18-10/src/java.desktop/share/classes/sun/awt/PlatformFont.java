/*
 * Copyright (c) 1996, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.peer.FontPeer;
import java.util.Locale;
import java.util.Vector;
import sun.font.SunFontManager;
import sun.java2d.FontSupport;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;

public abstract class PlatformFont implements FontPeer {

    static {
        NativeLibLoader.loadLibraries();
        initIDs();
    }

    protected FontDescriptor[] componentFonts;
    protected char defaultChar;
    protected FontConfiguration fontConfig;

    protected FontDescriptor defaultFont;

    protected String familyName;

    private Object[] fontCache;

    // Maybe this should be a property that is set based
    // on the locale?
    protected static int FONTCACHESIZE = 256;
    protected static int FONTCACHEMASK = PlatformFont.FONTCACHESIZE - 1;
    protected static String osVersion;

    public PlatformFont(String name, int style){
        SunFontManager sfm = SunFontManager.getInstance();
        if (sfm instanceof FontSupport) {
            fontConfig = ((FontSupport)sfm).getFontConfiguration();
        }
        if (fontConfig == null) {
            return;
        }

        // map given font name to a valid logical font family name
        familyName = name.toLowerCase(Locale.ENGLISH);
        if (!FontConfiguration.isLogicalFontFamilyName(familyName)) {
            familyName = fontConfig.getFallbackFamilyName(familyName, "sansserif");
        }

        componentFonts = fontConfig.getFontDescriptors(familyName, style);

        // search default character
        //
        char missingGlyphCharacter = getMissingGlyphCharacter();

        defaultChar = '?';
        if (componentFonts.length > 0)
            defaultFont = componentFonts[0];

        for (int i = 0; i < componentFonts.length; i++){
            if (componentFonts[i].isExcluded(missingGlyphCharacter)) {
                continue;
            }

            if (componentFonts[i].encoder.canEncode(missingGlyphCharacter)) {
                defaultFont = componentFonts[i];
                defaultChar = missingGlyphCharacter;
                break;
            }
        }
    }

    /**
     * Returns the character that should be rendered when a glyph
     * is missing.
     */
    protected abstract char getMissingGlyphCharacter();

    /**
     * make a array of CharsetString with given String.
     */
    public CharsetString[] makeMultiCharsetString(String str){
        return makeMultiCharsetString(str.toCharArray(), 0, str.length(), true);
    }

    /**
     * make a array of CharsetString with given String.
     */
    public CharsetString[] makeMultiCharsetString(String str, boolean allowdefault){
        return makeMultiCharsetString(str.toCharArray(), 0, str.length(), allowdefault);
    }

    /**
     * make a array of CharsetString with given char array.
     * @param str The char array to convert.
     * @param offset offset of first character of interest
     * @param len number of characters to convert
     */
    public CharsetString[] makeMultiCharsetString(char[] str, int offset, int len) {
        return makeMultiCharsetString(str, offset, len, true);
    }

    /**
     * make a array of CharsetString with given char array.
     * @param str The char array to convert.
     * @param offset offset of first character of interest
     * @param len number of characters to convert
     * @param allowDefault whether to allow the default char.
     * Setting this to true overloads the meaning of this method to
     * return non-null only if all chars can be converted.
     * @return array of CharsetString or if allowDefault is false and any
     * of the returned chars would have been converted to a default char,
     * then return null.
     * This is used to choose alternative means of displaying the text.
     */
    public CharsetString[] makeMultiCharsetString(char[] str, int offset, int len,
                                                  boolean allowDefault) {

        if (len < 1) {
            return new CharsetString[0];
        }
        Vector<CharsetString> mcs = null;
        char[] tmpStr = new char[len];
        char tmpChar = defaultChar;
        boolean encoded = false;

        FontDescriptor currentFont = defaultFont;


        for (int i = 0; i < componentFonts.length; i++) {
            if (componentFonts[i].isExcluded(str[offset])){
                continue;
            }

            /* Need "encoded" variable to distinguish the case when
             * the default char is the same as the encoded char.
             * The defaultChar on Linux is '?' so it is needed there.
             */
            if (componentFonts[i].encoder.canEncode(str[offset])){
                currentFont = componentFonts[i];
                tmpChar = str[offset];
                encoded = true;
                break;
            }
        }
        if (!allowDefault && !encoded) {
            return null;
        } else {
            tmpStr[0] = tmpChar;
        }

        int lastIndex = 0;
        for (int i = 1; i < len; i++){
            char ch = str[offset + i];
            FontDescriptor fd = defaultFont;
            tmpChar = defaultChar;
            encoded = false;
            for (int j = 0; j < componentFonts.length; j++){
                if (componentFonts[j].isExcluded(ch)){
                    continue;
                }

                if (componentFonts[j].encoder.canEncode(ch)){
                    fd = componentFonts[j];
                    tmpChar = ch;
                    encoded = true;
                    break;
                }
            }
            if (!allowDefault && !encoded) {
                return null;
            } else {
                tmpStr[i] = tmpChar;
            }
            if (currentFont != fd){
                if (mcs == null) {
                    mcs = new Vector<>(3);
                }
                mcs.addElement(new CharsetString(tmpStr, lastIndex,
                                                 i-lastIndex, currentFont));
                currentFont = fd;
                fd = defaultFont;
                lastIndex = i;
            }
        }
        CharsetString[] result;
        CharsetString cs = new CharsetString(tmpStr, lastIndex,
                                             len-lastIndex, currentFont);
        if (mcs == null) {
            result = new CharsetString[1];
            result[0] = cs;
        } else {
            mcs.addElement(cs);
            result = mcs.toArray(new CharsetString[mcs.size()]);
        }
        return result;
    }

    /**
     * Is it possible that this font's metrics require the multi-font calls?
     * This might be true, for example, if the font supports kerning.
    **/
    public boolean mightHaveMultiFontMetrics() {
        return fontConfig != null;
    }

    /**
     * Specialized fast path string conversion for AWT.
     */
    public Object[] makeConvertedMultiFontString(String str)
    {
        return makeConvertedMultiFontChars(str.toCharArray(),0,str.length());
    }

    public Object[] makeConvertedMultiFontChars(char[] data,
                                                int start, int len)
    {
        Object[] result = new Object[2];
        Object[] workingCache;
        byte[] convertedData = null;
        int stringIndex = start;
        int convertedDataIndex = 0;
        int resultIndex = 0;
        int cacheIndex;
        FontDescriptor currentFontDescriptor = null;
        FontDescriptor lastFontDescriptor = null;
        char currentDefaultChar;
        PlatformFontCache theChar;

        // Simple bounds check
        int end = start + len;
        if (start < 0 || end > data.length) {
            throw new ArrayIndexOutOfBoundsException();
        }

        if(stringIndex >= end) {
            return null;
        }

        // coversion loop
        while(stringIndex < end)
        {
            currentDefaultChar = data[stringIndex];

            // Note that cache sizes must be a power of two!
            cacheIndex = (currentDefaultChar & PlatformFont.FONTCACHEMASK);

            theChar = (PlatformFontCache)getFontCache()[cacheIndex];

            // Is the unicode char we want cached?
            if(theChar == null || theChar.uniChar != currentDefaultChar)
            {
                /* find a converter that can convert the current character */
                currentFontDescriptor = defaultFont;
                currentDefaultChar = defaultChar;
                char ch = data[stringIndex];
                int componentCount = componentFonts.length;

                for (int j = 0; j < componentCount; j++) {
                    FontDescriptor fontDescriptor = componentFonts[j];

                    fontDescriptor.encoder.reset();
                    //fontDescriptor.encoder.onUnmappleCharacterAction(...);

                    if (fontDescriptor.isExcluded(ch)) {
                        continue;
                    }
                    if (fontDescriptor.encoder.canEncode(ch)) {
                        currentFontDescriptor = fontDescriptor;
                        currentDefaultChar = ch;
                        break;
                    }
                }
                try {
                    char[] input = new char[1];
                    input[0] = currentDefaultChar;

                    theChar = new PlatformFontCache();
                    if (currentFontDescriptor.useUnicode()) {
                        /*
                        currentFontDescriptor.unicodeEncoder.encode(CharBuffer.wrap(input),
                                                                    theChar.bb,
                                                                    true);
                        */
                        if (FontDescriptor.isLE) {
                            theChar.bb.put((byte)(input[0] & 0xff));
                            theChar.bb.put((byte)(input[0] >>8));
                        } else {
                            theChar.bb.put((byte)(input[0] >> 8));
                            theChar.bb.put((byte)(input[0] & 0xff));
                        }
                    }
                    else  {
                        currentFontDescriptor.encoder.encode(CharBuffer.wrap(input),
                                                             theChar.bb,
                                                             true);
                    }
                    theChar.fontDescriptor = currentFontDescriptor;
                    theChar.uniChar = data[stringIndex];
                    getFontCache()[cacheIndex] = theChar;
                } catch(Exception e){
                    // Should never happen!
                    System.err.println(e);
                    e.printStackTrace();
                    return null;
                }
            }

            // Check to see if we've changed fonts.
            if(lastFontDescriptor != theChar.fontDescriptor) {
                if(lastFontDescriptor != null) {
                    result[resultIndex++] = lastFontDescriptor;
                    result[resultIndex++] = convertedData;
                    //  Add the size to the converted data field.
                    if(convertedData != null) {
                        convertedDataIndex -= 4;
                        convertedData[0] = (byte)(convertedDataIndex >> 24);
                        convertedData[1] = (byte)(convertedDataIndex >> 16);
                        convertedData[2] = (byte)(convertedDataIndex >> 8);
                        convertedData[3] = (byte)convertedDataIndex;
                    }

                    if(resultIndex >= result.length) {
                        Object[] newResult = new Object[result.length * 2];

                        System.arraycopy(result, 0, newResult, 0,
                                         result.length);
                        result = newResult;
                    }
                }

                if (theChar.fontDescriptor.useUnicode()) {
                    convertedData = new byte[(end - stringIndex + 1) *
                                        (int)theChar.fontDescriptor.unicodeEncoder.maxBytesPerChar()
                                        + 4];
                }
                else  {
                    convertedData = new byte[(end - stringIndex + 1) *
                                        (int)theChar.fontDescriptor.encoder.maxBytesPerChar()
                                        + 4];
                }

                convertedDataIndex = 4;

                lastFontDescriptor = theChar.fontDescriptor;
            }

            byte[] ba = theChar.bb.array();
            int size = theChar.bb.position();
            if(size == 1) {
                convertedData[convertedDataIndex++] = ba[0];
            }
            else if(size == 2) {
                convertedData[convertedDataIndex++] = ba[0];
                convertedData[convertedDataIndex++] = ba[1];
            } else if(size == 3) {
                convertedData[convertedDataIndex++] = ba[0];
                convertedData[convertedDataIndex++] = ba[1];
                convertedData[convertedDataIndex++] = ba[2];
            } else if(size == 4) {
                convertedData[convertedDataIndex++] = ba[0];
                convertedData[convertedDataIndex++] = ba[1];
                convertedData[convertedDataIndex++] = ba[2];
                convertedData[convertedDataIndex++] = ba[3];
            }
            stringIndex++;
        }

        result[resultIndex++] = lastFontDescriptor;
        result[resultIndex] = convertedData;

        //  Add the size to the converted data field.
        if(convertedData != null) {
            convertedDataIndex -= 4;
            convertedData[0] = (byte)(convertedDataIndex >> 24);
            convertedData[1] = (byte)(convertedDataIndex >> 16);
            convertedData[2] = (byte)(convertedDataIndex >> 8);
            convertedData[3] = (byte)convertedDataIndex;
        }
        return result;
    }

    /*
     * Create fontCache on demand instead of during construction to
     * reduce overall memory consumption.
     *
     * This method is declared final so that its code can be inlined
     * by the compiler.
     */
    protected final Object[] getFontCache() {
        // This method is not MT-safe by design. Since this is just a
        // cache anyways, it's okay if we occasionally allocate the array
        // twice or return an array which will be dereferenced and gced
        // right away.
        if (fontCache == null) {
            fontCache = new Object[PlatformFont.FONTCACHESIZE];
        }

        return fontCache;
    }

    /**
     * Initialize JNI field and method IDs
     */
    private static native void initIDs();

    class PlatformFontCache
    {
        char uniChar;
        FontDescriptor fontDescriptor;
        ByteBuffer bb = ByteBuffer.allocate(4);
    }
}
