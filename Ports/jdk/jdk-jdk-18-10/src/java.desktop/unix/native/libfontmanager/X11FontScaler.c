/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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

/*
 * Important note : All AWTxxx functions are defined in font.h.
 * These were added to remove the dependency of this file on X11.
 * These functions are used to perform X11 operations and should
 * be "stubbed out" in environments that do not support X11.
 * The implementation of these functions has been moved from this file
 * into X11FontScaler_md.c, which is compiled into another library.
 */
#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <sys/utsname.h>

#include <jni.h>
#include <jni_util.h>

#include "sun_font_NativeFont.h"
#include "sun_font_NativeStrike.h"
#include "sun_font_NativeStrikeDisposer.h"
#include "sunfontids.h"
#include "fontscalerdefs.h"
#include "X11FontScaler.h"

JNIEXPORT void JNICALL
    Java_sun_font_NativeStrikeDisposer_freeNativeScalerContext
    (JNIEnv *env, jobject disposer, jlong pScalerContext) {

    NativeScalerContext *context = (NativeScalerContext*)(uintptr_t)(pScalerContext);

    if (context != NULL) {
        if (context->xFont != NULL) {
            AWTFreeFont(context->xFont);
        }
        free(context);
    }
}

JNIEXPORT jlong JNICALL
Java_sun_font_NativeStrike_createNullScalerContext
    (JNIEnv *env, jobject strike) {

   NativeScalerContext *context =
       (NativeScalerContext*)malloc(sizeof(NativeScalerContext));
   if (context == NULL) {
        return (jlong)(uintptr_t)0L;
   }
   context->xFont = NULL;
   context->minGlyph = 0;
   context->maxGlyph = 0;
   context->numGlyphs = 0;
   context->defaultGlyph = 0;
   context->ptSize = NO_POINTSIZE;
   return (jlong)(uintptr_t)context;
}

JNIEXPORT jlong JNICALL
Java_sun_font_NativeStrike_createScalerContext
    (JNIEnv *env, jobject strike, jbyteArray xlfdBytes,
     jint ptSize, jdouble scale) {

    NativeScalerContext *context;
    int len = (*env)->GetArrayLength(env, xlfdBytes);

    char* xlfd = (char*)malloc(len+1);

    if (xlfd == NULL) {
        return (jlong)(uintptr_t)0L;
    }

    (*env)->GetByteArrayRegion(env, xlfdBytes, 0, len, (jbyte*)xlfd);
    xlfd[len] = '\0';
    context = (NativeScalerContext*)malloc(sizeof(NativeScalerContext));
    if (context == NULL) {
        free(xlfd);
        return (jlong)(uintptr_t)0L;
    }

    AWTLoadFont (xlfd, &(context->xFont));
    free(xlfd);

    if (context->xFont == NULL) {   /* NULL means couldn't find the font */
        free(context);
        context = NULL;
    } else {
        /* numGlyphs is an estimate : X11 doesn't provide a quick way to
         * discover which glyphs are valid: just the range that contains all
         * the valid glyphs, and this range may have holes.
         */
        context->minGlyph = (AWTFontMinByte1(context->xFont) << 8) +
            AWTFontMinCharOrByte2(context->xFont);
        context->maxGlyph = (AWTFontMaxByte1(context->xFont) << 8) +
            AWTFontMaxCharOrByte2(context->xFont);
        context->numGlyphs = context->maxGlyph - context->minGlyph + 1;
        context->defaultGlyph = AWTFontDefaultChar(context->xFont);
        /* Sometimes the default_char field of the XFontStruct isn't
         * initialized to anything, so it can be a large number. So,
         * check to see if its less than the largest possible value
         * and if so, then use it. Otherwise, just use the minGlyph.
         */
        if (context->defaultGlyph < context->minGlyph ||
            context->defaultGlyph > context->maxGlyph) {
            context->defaultGlyph = context->minGlyph;
        }
        context->ptSize = ptSize;
        context->scale = scale;
    }

    /*
     * REMIND: freeing of native resources? XID, XFontStruct etc??
     */
    return (jlong)(uintptr_t)context;
}


/* JNIEXPORT jint JNICALL */
/* Java_sun_font_NativeFont_getItalicAngle */
/*     (JNIEnv *env, jobject font) { */

/*     UInt32 angle; */
/*     AWTGetFontItalicAngle(xFont, &angle); */
/*X11 reports italic angle as 1/64ths of a degree, relative to 3 o'clock
 * with anti-clockwise being the +ve rotation direction.
 * We return
XGetFontProperty(xFont,XA_ITALIC_ANGLE, &angle);
*/

/*     return (jint)angle; */
/* } */

JNIEXPORT jboolean JNICALL
Java_sun_font_NativeFont_fontExists
    (JNIEnv *env, jclass fontClass, jbyteArray xlfdBytes) {

    int count = 0;
    int len = (*env)->GetArrayLength(env, xlfdBytes);
    char* xlfd = (char*)malloc(len+1);

    if (xlfd == NULL) {
        return JNI_FALSE;
    }

    (*env)->GetByteArrayRegion(env, xlfdBytes, 0, len, (jbyte*)xlfd);
    xlfd[len] = '\0';

    count = AWTCountFonts(xlfd);
    free(xlfd);
    if (count > 0) {
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}

JNIEXPORT jboolean JNICALL
Java_sun_font_NativeFont_haveBitmapFonts
    (JNIEnv *env, jclass fontClass, jbyteArray xlfdBytes) {

    int count = 0;
    int len = (*env)->GetArrayLength(env, xlfdBytes);
    char* xlfd = (char*)malloc(len+1);

    if (xlfd == NULL) {
        return JNI_FALSE;
    }

    (*env)->GetByteArrayRegion(env, xlfdBytes, 0, len, (jbyte*)xlfd);
    xlfd[len] = '\0';

    count = AWTCountFonts(xlfd);
    free(xlfd);
    if (count > 2) {
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}

// CountGlyphs doubles as way of getting a native font reference
// and telling if its valid. So far as I can tell GenerateImage etc
// just return if this "initialisation method" hasn't been called.
// So clients of this class need to call CountGlyphs() right after
// construction to be safe.
JNIEXPORT jint JNICALL
Java_sun_font_NativeFont_countGlyphs
    (JNIEnv *env, jobject font, jbyteArray xlfdBytes, jint ptSize) {

    NativeScalerContext *context = (NativeScalerContext*)
        (uintptr_t)(Java_sun_font_NativeStrike_createScalerContext
        (env, NULL, xlfdBytes, ptSize, 1));

    if (context == NULL) {
        return 0;
    } else {
        int numGlyphs = context->numGlyphs;
        AWTFreeFont(context->xFont);
        free(context);
        return numGlyphs;
    }
}

JNIEXPORT jint JNICALL
Java_sun_font_NativeStrike_getMaxGlyph
    (JNIEnv *env, jobject strike, jlong pScalerContext) {

    NativeScalerContext *context = (NativeScalerContext*)(uintptr_t)(pScalerContext);
    if (context == NULL) {
        return (jint)0;
    } else {
        return (jint)context->maxGlyph+1;
    }
}

JNIEXPORT jfloat JNICALL
Java_sun_font_NativeFont_getGlyphAdvance
   (JNIEnv *env, jobject font2D, jlong pScalerContext, jint glyphCode) {

    AWTChar xcs = NULL;
    jfloat advance = 0.0f;
    AWTFont xFont;
    NativeScalerContext *context = (NativeScalerContext*)(uintptr_t)(pScalerContext);
    if (context == NULL) {
        return advance;
    } else {
        xFont = (AWTFont)context->xFont;
    }

    if (xFont == NULL || context->ptSize == NO_POINTSIZE) {
        return advance;
    }

    if (glyphCode < context->minGlyph || glyphCode > context->maxGlyph) {
        glyphCode = context->defaultGlyph;
    }

    /* If number of glyphs is 256 or less, the metrics are
     * stored correctly in the XFontStruct for each
     * character. If the # characters is more (double byte
     * case), then these metrics seem flaky and there's no
     * way to determine if they have been set or not.
     */
    if ((context->maxGlyph <= 256) && (AWTFontPerChar(xFont, 0) != NULL)) {
        xcs = AWTFontPerChar(xFont, glyphCode - context->minGlyph);
        advance = AWTCharAdvance(xcs);
    } else {
        int direction, ascent, descent;
        AWTChar2b xChar;

        xChar.byte1 = (unsigned char) (glyphCode >> 8);
        xChar.byte2 = (unsigned char) glyphCode;
        AWTFontTextExtents16(xFont, &xChar, &xcs);
        advance = AWTCharAdvance(xcs);
        AWTFreeChar(xcs);
    }
    return (jfloat)(advance/context->scale);
}

JNIEXPORT jlong JNICALL
Java_sun_font_NativeFont_getGlyphImageNoDefault
    (JNIEnv *env, jobject font2D, jlong pScalerContext, jint glyphCode) {

    AWTChar2b xChar;
    AWTFont xFont;
    NativeScalerContext *context = (NativeScalerContext*)(uintptr_t)(pScalerContext);
    if (context == NULL) {
        return (jlong)0;
    } else {
        xFont = (AWTFont)context->xFont;
    }

    if (xFont == NULL || context->ptSize == NO_POINTSIZE) {
        return (jlong)0;
    }

    if (glyphCode < context->minGlyph || glyphCode > context->maxGlyph) {
        return (jlong)0;
    }

    xChar.byte1 = (unsigned char)(glyphCode >> 8);
    xChar.byte2 = (unsigned char)glyphCode;
    return AWTFontGenerateImage(xFont, &xChar);
}

JNIEXPORT jlong JNICALL
Java_sun_font_NativeFont_getGlyphImage
    (JNIEnv *env, jobject font2D, jlong pScalerContext, jint glyphCode) {

    AWTChar2b xChar;
    AWTFont xFont;
    NativeScalerContext *context = (NativeScalerContext*)(uintptr_t)(pScalerContext);
    if (context == NULL) {
        return (jlong)0;
    } else {
        xFont = (AWTFont)context->xFont;
    }

    if (xFont == NULL || context->ptSize == NO_POINTSIZE) {
        return (jlong)0;
    }

    if (glyphCode < context->minGlyph || glyphCode > context->maxGlyph) {
        glyphCode = context->defaultGlyph;
    }

    xChar.byte1 = (unsigned char)(glyphCode >> 8);
    xChar.byte2 = (unsigned char)glyphCode;
    return AWTFontGenerateImage(xFont, &xChar);
}

JNIEXPORT jobject JNICALL
  Java_sun_font_NativeFont_getFontMetrics
    (JNIEnv *env, jobject font2D, jlong pScalerContext) {

    jfloat j0=0, j1=1, ay=j0, dy=j0, mx=j0;
    jobject metrics;
    AWTFont xFont;
    NativeScalerContext *context = (NativeScalerContext*)(uintptr_t)(pScalerContext);
    if (context == NULL) {
        return NULL;
    } else {
        xFont = (AWTFont)context->xFont;
    }

    if (xFont == NULL) {
        return NULL;
    }

    /* the commented out lines are the old 1.4.x behaviour which used max
     * bounds instead of the font's designed ascent/descent */
/*   ay =  (jfloat)-AWTCharAscent(AWTFontMaxBounds(xFont)); */
/*   dy =  (jfloat)AWTCharDescent(AWTFontMaxBounds(xFont)); */

    ay = (jfloat)-AWTFontAscent(xFont);
    dy = (jfloat)AWTFontDescent(xFont);
    mx = (jfloat)AWTCharAdvance(AWTFontMaxBounds(xFont));

    /* ascent : no need to set ascentX - it will be zero
     * descent : no need to set descentX - it will be zero
     * baseline :  old releases "made up" a number and also seemed to
     * make it up for "X" and set "Y" to 0.
     * leadingX : no need to set leadingX - it will be zero.
     * leadingY : made-up number, but being compatible with what 1.4.x did
     * advance : no need to set yMaxLinearAdvanceWidth - it will be zero.
     */
    metrics = (*env)->NewObject(env, sunFontIDs.strikeMetricsClass,
                                sunFontIDs.strikeMetricsCtr,
                                j0, ay, j0, dy, j1, j0, j0, j1, mx, j0);
/*      printf("X11 asc=%f dsc=%f adv=%f scale=%f\n", */
/*          ay, dy, mx, (float)context->scale); */
    return metrics;
}
