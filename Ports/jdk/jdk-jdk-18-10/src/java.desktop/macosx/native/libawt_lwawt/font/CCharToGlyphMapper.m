/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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

#import "JNIUtilities.h"

#import "AWTFont.h"
#import "CoreTextSupport.h"

#import "sun_font_CCharToGlyphMapper.h"

/*
 * Class:     sun_font_CCharToGlyphMapper
 * Method:    countGlyphs
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL
Java_sun_font_CCharToGlyphMapper_countGlyphs
    (JNIEnv *env, jclass clazz, jlong awtFontPtr)
{
    jint numGlyphs = 0;

JNI_COCOA_ENTER(env);

    AWTFont *awtFont = (AWTFont *)jlong_to_ptr(awtFontPtr);
    numGlyphs = [awtFont->fFont numberOfGlyphs];

JNI_COCOA_EXIT(env);

    return numGlyphs;
}

static inline void
GetGlyphsFromUnicodes(JNIEnv *env, AWTFont *awtFont,
                      jint count, UniChar *unicodes,
                      CGGlyph *cgGlyphs, jintArray glyphs)
{
    jint *glyphCodeInts = (*env)->GetPrimitiveArrayCritical(env, glyphs, 0);

    CTS_GetGlyphsAsIntsForCharacters(awtFont, unicodes,
                                     cgGlyphs, glyphCodeInts, count);

    // Do not use JNI_COMMIT, as that will not free the buffer copy
    // when +ProtectJavaHeap is on.
    (*env)->ReleasePrimitiveArrayCritical(env, glyphs, glyphCodeInts, 0);
}

static inline void
AllocateGlyphBuffer(JNIEnv *env, AWTFont *awtFont,
                    jint count, UniChar *unicodes, jintArray glyphs)
{
    if (count < MAX_STACK_ALLOC_GLYPH_BUFFER_SIZE) {
        CGGlyph cgGlyphs[count];
        GetGlyphsFromUnicodes(env, awtFont, count, unicodes, cgGlyphs, glyphs);
    } else {
        CGGlyph *cgGlyphs = (CGGlyph *)malloc(count * sizeof(CGGlyph));
        GetGlyphsFromUnicodes(env, awtFont, count, unicodes, cgGlyphs, glyphs);
        free(cgGlyphs);
    }
}

/*
 * Class:     sun_font_CCharToGlyphMapper
 * Method:    nativeCharsToGlyphs
 * Signature: (JI[C[I)V
 */
JNIEXPORT void JNICALL
Java_sun_font_CCharToGlyphMapper_nativeCharsToGlyphs
    (JNIEnv *env, jclass clazz,
     jlong awtFontPtr, jint count, jcharArray unicodes, jintArray glyphs)
{
JNI_COCOA_ENTER(env);

    AWTFont *awtFont = (AWTFont *)jlong_to_ptr(awtFontPtr);

    // check the array size
    jint len = (*env)->GetArrayLength(env, glyphs);
    if (len < count) {
        count = len;
    }

    jchar *unicodesAsChars =
        (*env)->GetPrimitiveArrayCritical(env, unicodes, NULL);

    if (unicodesAsChars != NULL) {
        AllocateGlyphBuffer(env, awtFont, count,
                           (UniChar *)unicodesAsChars, glyphs);

        (*env)->ReleasePrimitiveArrayCritical(env, unicodes,
                                              unicodesAsChars, JNI_ABORT);
    }

JNI_COCOA_EXIT(env);
}
