/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

#import <AppKit/AppKit.h>
#import "CoreTextSupport.h"


/*
 * Callback for CoreText which uses the CoreTextProviderStruct to
 * feed CT UniChars.  We only use it for one-off lines, and don't
 * attempt to fragment our strings.
 */
const UniChar *
CTS_Provider(CFIndex stringIndex, CFIndex *charCount,
             CFDictionaryRef *attributes, void *refCon)
{
    // if we have a zero length string we can just return NULL for the string
    // or if the index anything other than 0 we are not using core text
    // correctly since we only have one run.
    if (stringIndex != 0) {
        return NULL;
    }

    CTS_ProviderStruct *ctps = (CTS_ProviderStruct *)refCon;
    *charCount = ctps->length;
    *attributes = ctps->attributes;
    return ctps->unicodes;
}


#pragma mark --- Retain/Release CoreText State Dictionary ---

/*
 * Gets a Dictionary filled with common details we want to use for CoreText
 * when we are interacting with it from Java.
 */
static inline CFMutableDictionaryRef
GetCTStateDictionaryFor(const NSFont *font, BOOL useFractionalMetrics)
{
    NSNumber *gZeroNumber = [NSNumber numberWithInt:0];
    NSNumber *gOneNumber = [NSNumber numberWithInt:1];

    CFMutableDictionaryRef dictRef = (CFMutableDictionaryRef)
        [[NSMutableDictionary alloc] initWithObjectsAndKeys:
        font, NSFontAttributeName,
        // TODO(cpc): following attribute is private...
        //gOneNumber,  (id)kCTForegroundColorFromContextAttributeName,
        // force integer hack in CoreText to help with Java integer assumptions
        useFractionalMetrics ? gZeroNumber : gOneNumber, @"CTIntegerMetrics",
        gZeroNumber, NSLigatureAttributeName,
        gZeroNumber, NSKernAttributeName,
        NULL];
    CFRetain(dictRef); // GC
    [(id)dictRef release];

    return dictRef;
}

/*
 * Releases the CoreText Dictionary - in the future we should hold on
 * to these to improve performance.
 */
static inline void
ReleaseCTStateDictionary(CFDictionaryRef ctStateDict)
{
    CFRelease(ctStateDict); // GC
}

/*
 *    Transform Unicode characters into glyphs.
 *
 *    Fills the "glyphsAsInts" array with the glyph codes for the current font,
 *    or the negative unicode value if we know the character can be hot-substituted.
 *
 *    This is the heart of "Universal Font Substitution" in Java.
 */
void CTS_GetGlyphsAsIntsForCharacters
(const AWTFont *font, const UniChar unicodes[], CGGlyph glyphs[], jint glyphsAsInts[], const size_t count)
{
    CTFontGetGlyphsForCharacters((CTFontRef)font->fFont, unicodes, glyphs, count);

    size_t i;
    for (i = 0; i < count; i++) {
        UniChar unicode = unicodes[i];
        UniChar nextUnicode = (i+1) < count ? unicodes[i+1] : 0;
        bool surrogatePair = unicode >= HI_SURROGATE_START && unicode <= HI_SURROGATE_END
                             && nextUnicode >= LO_SURROGATE_START && nextUnicode <= LO_SURROGATE_END;

        CGGlyph glyph = glyphs[i];
        if (glyph > 0) {
            glyphsAsInts[i] = glyph;
            if (surrogatePair) i++;
            continue;
        }

        const CTFontRef fallback = JRSFontCreateFallbackFontForCharacters((CTFontRef)font->fFont, &unicodes[i],
                                                                          surrogatePair ? 2 : 1);
        if (fallback) {
            CTFontGetGlyphsForCharacters(fallback, &unicodes[i], &glyphs[i], surrogatePair ? 2 : 1);
            glyph = glyphs[i];
            CFRelease(fallback);
        }

        if (glyph > 0) {
            int codePoint = surrogatePair ? (((int)(unicode - HI_SURROGATE_START)) << 10)
                                            + nextUnicode - LO_SURROGATE_START + 0x10000 : unicode;
            glyphsAsInts[i] = -codePoint; // set the glyph code to the negative unicode value
        } else {
            glyphsAsInts[i] = 0; // CoreText couldn't find a glyph for this character either
        }
        if (surrogatePair) i++;
    }
}

/*
 * Translates a Unicode into a CGGlyph/CTFontRef pair
 * Returns the substituted font, and places the appropriate glyph into "glyphRef"
 */
CTFontRef CTS_CopyCTFallbackFontAndGlyphForUnicode
(const AWTFont *font, const UTF16Char *charRef, CGGlyph *glyphRef, int count) {
    CTFontRef fallback = JRSFontCreateFallbackFontForCharacters((CTFontRef)font->fFont, charRef, count);
    if (fallback == NULL)
    {
        // use the original font if we somehow got duped into trying to fallback something we can't
        fallback = (CTFontRef)font->fFont;
        CFRetain(fallback);
    }

    CTFontGetGlyphsForCharacters(fallback, charRef, glyphRef, count);
    return fallback;
}

/*
 * Translates a Java glyph code int (might be a negative unicode value) into a CGGlyph/CTFontRef pair
 * Returns the substituted font, and places the appropriate glyph into "glyphRef"
 */
CTFontRef CTS_CopyCTFallbackFontAndGlyphForJavaGlyphCode
(const AWTFont *font, const jint glyphCode, CGGlyph *glyphRef)
{
    // negative glyph codes are really unicodes, which were placed there by the mapper
    // to indicate we should use CoreText to substitute the character
    if (glyphCode >= 0)
    {
        *glyphRef = glyphCode;
        CFRetain(font->fFont);
        return (CTFontRef)font->fFont;
    }

    int codePoint = -glyphCode;
    if (codePoint >= 0x10000) {
        UTF16Char chars[2];
        CGGlyph glyphs[2];
        CTS_BreakupUnicodeIntoSurrogatePairs(codePoint, chars);
        CTFontRef result = CTS_CopyCTFallbackFontAndGlyphForUnicode(font, chars, glyphs, 2);
        *glyphRef = glyphs[0];
        return result;
    } else {
        UTF16Char character = codePoint;
        return CTS_CopyCTFallbackFontAndGlyphForUnicode(font, &character, glyphRef, 1);
    }
}

// Breakup a 32 bit unicode value into the component surrogate pairs
void CTS_BreakupUnicodeIntoSurrogatePairs(int uniChar, UTF16Char charRef[]) {
    int value = uniChar - 0x10000;
    UTF16Char low_surrogate = (value & 0x3FF) | LO_SURROGATE_START;
    UTF16Char high_surrogate = (((int)(value & 0xFFC00)) >> 10) | HI_SURROGATE_START;
    charRef[0] = high_surrogate;
    charRef[1] = low_surrogate;
}
