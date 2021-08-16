/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

//  Native side of the Quartz text pipe, paints on Quartz Surface Datas.
//  Interesting Docs : /Developer/Documentation/Cocoa/TasksAndConcepts/ProgrammingTopics/FontHandling/FontHandling.html

#import "sun_awt_SunHints.h"
#import "sun_lwawt_macosx_CTextPipe.h"
#import "sun_java2d_OSXSurfaceData.h"

#import "CoreTextSupport.h"
#import "QuartzSurfaceData.h"
#include "AWTStrike.h"
#import "JNIUtilities.h"

static const CGAffineTransform sInverseTX = { 1, 0, 0, -1, 0, 0 };


#pragma mark --- CoreText Support ---


// Translates a Unicode into a CGGlyph/CTFontRef pair
// Returns the substituted font, and places the appropriate glyph into "glyphRef"
CTFontRef JavaCT_CopyCTFallbackFontAndGlyphForUnicode
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

// Translates a Java glyph code int (might be a negative unicode value) into a CGGlyph/CTFontRef pair
// Returns the substituted font, and places the appropriate glyph into "glyph"
CTFontRef JavaCT_CopyCTFallbackFontAndGlyphForJavaGlyphCode
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

    UTF16Char character = -glyphCode;
    return JavaCT_CopyCTFallbackFontAndGlyphForUnicode(font, &character, glyphRef, 1);
}

// Breakup a 32 bit unicode value into the component surrogate pairs
void JavaCT_BreakupUnicodeIntoSurrogatePairs(int uniChar, UTF16Char charRef[]) {
    int value = uniChar - 0x10000;
    UTF16Char low_surrogate = (value & 0x3FF) | LO_SURROGATE_START;
    UTF16Char high_surrogate = (((int)(value & 0xFFC00)) >> 10) | HI_SURROGATE_START;
    charRef[0] = high_surrogate;
    charRef[1] = low_surrogate;
}



/*
 * Callback for CoreText which uses the CoreTextProviderStruct to feed CT UniChars
 * We only use it for one-off lines, and don't attempt to fragment our strings
 */
const UniChar *Java_CTProvider
(CFIndex stringIndex, CFIndex *charCount, CFDictionaryRef *attributes, void *refCon)
{
    // if we have a zero length string we can just return NULL for the string
    // or if the index anything other than 0 we are not using core text
    // correctly since we only have one run.
    if (stringIndex != 0)
    {
        return NULL;
    }

    CTS_ProviderStruct *ctps = (CTS_ProviderStruct *)refCon;
    *charCount = ctps->length;
    *attributes = ctps->attributes;
    return ctps->unicodes;
}


/*
 *    Gets a Dictionary filled with common details we want to use for CoreText when we are interacting
 *    with it from Java.
 */
static NSDictionary* ctsDictionaryFor(const NSFont *font, BOOL useFractionalMetrics)
{
    NSNumber *gZeroNumber = [NSNumber numberWithInt:0];
    NSNumber *gOneNumber = [NSNumber numberWithInt:1];

    return [NSDictionary dictionaryWithObjectsAndKeys:
             font, NSFontAttributeName,
             gOneNumber,  (id)kCTForegroundColorFromContextAttributeName,
             useFractionalMetrics ? gZeroNumber : gOneNumber, @"CTIntegerMetrics", // force integer hack in CoreText to help with Java's integer assumptions
             gZeroNumber, NSLigatureAttributeName,
             gZeroNumber, NSKernAttributeName,
             nil];
}

// Itterates though each glyph, and if a transform is present for that glyph, apply it to the CGContext, and strike the glyph.
// If there is no per-glyph transform, just strike the glyph. Advances must also be transformed on-the-spot as well.
void JavaCT_DrawGlyphVector
(const QuartzSDOps *qsdo, const AWTStrike *strike, const BOOL useSubstituion, const int uniChars[], const CGGlyph glyphs[], CGSize advances[], const jint g_gvTXIndicesAsInts[], const jdouble g_gvTransformsAsDoubles[], const CFIndex length)
{
    CGPoint pt = { 0, 0 };

    // get our baseline transform and font
    CGContextRef cgRef = qsdo->cgRef;
    CGAffineTransform ctmText = CGContextGetTextMatrix(cgRef);

    BOOL saved = false;

    CGAffineTransform invTx = CGAffineTransformInvert(strike->fTx);

    NSInteger i;
    for (i = 0; i < length; i++)
    {
        CGGlyph glyph = glyphs[i];
        int uniChar = uniChars[i];
        // if we found a unichar instead of a glyph code, get the fallback font,
        // find the glyph code for the fallback font, and set the font on the current context
        if (uniChar != 0)
        {
            CTFontRef fallback;
            if (uniChar > 0xFFFF) {
                UTF16Char charRef[2];
                JavaCT_BreakupUnicodeIntoSurrogatePairs(uniChar, charRef);
                CGGlyph glyphTmp[2];
                fallback = JavaCT_CopyCTFallbackFontAndGlyphForUnicode(strike->fAWTFont, (const UTF16Char *)&charRef, (CGGlyph *)&glyphTmp, 2);
                glyph = glyphTmp[0];
            } else {
                const UTF16Char u = uniChar;
                fallback = JavaCT_CopyCTFallbackFontAndGlyphForUnicode(strike->fAWTFont, &u, (CGGlyph *)&glyph, 1);
            }
            if (fallback) {
                const CGFontRef cgFallback = CTFontCopyGraphicsFont(fallback, NULL);
                CFRelease(fallback);

                if (cgFallback) {
                    if (!saved) {
                        CGContextSaveGState(cgRef);
                        saved = true;
                    }
                    CGContextSetFont(cgRef, cgFallback);
                    CFRelease(cgFallback);
                }
            }
        } else {
            if (saved) {
                CGContextRestoreGState(cgRef);
                saved = false;
            }
        }

        // if we have per-glyph transformations
        int tin = (g_gvTXIndicesAsInts == NULL) ? -1 : (g_gvTXIndicesAsInts[i] - 1) * 6;
        if (tin < 0)
        {
            CGContextShowGlyphsAtPoint(cgRef, pt.x, pt.y, &glyph, 1);
        }
        else
        {
            CGAffineTransform tx = CGAffineTransformMake(
                                                         (CGFloat)g_gvTransformsAsDoubles[tin + 0], (CGFloat)g_gvTransformsAsDoubles[tin + 2],
                                                         (CGFloat)g_gvTransformsAsDoubles[tin + 1], (CGFloat)g_gvTransformsAsDoubles[tin + 3],
                                                         0, 0);

            CGPoint txOffset = { (CGFloat)g_gvTransformsAsDoubles[tin + 4], (CGFloat)g_gvTransformsAsDoubles[tin + 5] };

            txOffset = CGPointApplyAffineTransform(txOffset, invTx);

            // apply the transform, strike the glyph, can change the transform back
            CGContextSetTextMatrix(cgRef, CGAffineTransformConcat(ctmText, tx));
            CGContextShowGlyphsAtPoint(cgRef, txOffset.x + pt.x, txOffset.y + pt.y, &glyph, 1);
            CGContextSetTextMatrix(cgRef, ctmText);

            // transform the measured advance for this strike
            advances[i] = CGSizeApplyAffineTransform(advances[i], tx);
            advances[i].width += txOffset.x;
            advances[i].height += txOffset.y;
        }

        // move our next x,y
        pt.x += advances[i].width;
        pt.y += advances[i].height;

    }
    // reset the font on the context after striking a unicode with CoreText
    if (saved) {
        CGContextRestoreGState(cgRef);
    }
}

// Using the Quartz Surface Data context, draw a hot-substituted character run
void JavaCT_DrawTextUsingQSD(JNIEnv *env, const QuartzSDOps *qsdo, const AWTStrike *strike, const jchar *chars, const jsize length)
{
    CGContextRef cgRef = qsdo->cgRef;

    AWTFont *awtFont = strike->fAWTFont;
    CGFloat ptSize = strike->fSize;
    CGAffineTransform tx = strike->fFontTx;

    NSFont *nsFont = [NSFont fontWithName:[awtFont->fFont fontName] size:ptSize];

    if (ptSize != 0) {
        CGFloat invScale = 1 / ptSize;
        tx = CGAffineTransformConcat(tx, CGAffineTransformMakeScale(invScale, invScale));
        CGContextConcatCTM(cgRef, tx);
    }

    CGContextSetTextMatrix(cgRef, CGAffineTransformIdentity); // resets the damage from CoreText

    NSString *string = [NSString stringWithCharacters:chars length:length];
    /*
       The calls below were used previously but for unknown reason did not
       render using the right font (see bug 7183516) when attribString is not
       initialized with font dictionary attributes.  It seems that "options"
       in CTTypesetterCreateWithAttributedStringAndOptions which contains the
       font dictionary is ignored.

    NSAttributedString *attribString = [[NSAttributedString alloc] initWithString:string];

    CTTypesetterRef typeSetterRef = CTTypesetterCreateWithAttributedStringAndOptions((CFAttributedStringRef) attribString, (CFDictionaryRef) ctsDictionaryFor(nsFont, JRSFontStyleUsesFractionalMetrics(strike->fStyle)));
    */
    NSAttributedString *attribString = [[NSAttributedString alloc]
        initWithString:string
        attributes:ctsDictionaryFor(nsFont, JRSFontStyleUsesFractionalMetrics(strike->fStyle))];

    CTTypesetterRef typeSetterRef = CTTypesetterCreateWithAttributedString((CFAttributedStringRef) attribString);

    CFRange range = {0, length};
    CTLineRef lineRef = CTTypesetterCreateLine(typeSetterRef, range);

    CTLineDraw(lineRef, cgRef);

    [attribString release];
    CFRelease(lineRef);
    CFRelease(typeSetterRef);
}


/*----------------------
    DrawTextContext is the funnel for all of our CoreText drawing.
    All three JNI apis call through this method.
 ----------------------*/
static void DrawTextContext
(JNIEnv *env, QuartzSDOps *qsdo, const AWTStrike *strike, const jchar *chars, const jsize length, const jdouble x, const jdouble y)
{
    if (length == 0)
    {
        return;
    }

    qsdo->BeginSurface(env, qsdo, SD_Text);
    if (qsdo->cgRef == NULL)
    {
        qsdo->FinishSurface(env, qsdo);
        return;
    }

    CGContextRef cgRef = qsdo->cgRef;


    CGContextSaveGState(cgRef);
    JRSFontSetRenderingStyleOnContext(cgRef, strike->fStyle);

    // we want to translate before we transform (scale or rotate) <rdar://4042541> (vm)
    CGContextTranslateCTM(cgRef, x, y);

    AWTFont *awtfont = strike->fAWTFont; //(AWTFont *)(qsdo->fontInfo.awtfont);
    NSCharacterSet *charSet = [awtfont->fFont coveredCharacterSet];

    JavaCT_DrawTextUsingQSD(env, qsdo, strike, chars, length);   // Draw with CoreText

    CGContextRestoreGState(cgRef);

    qsdo->FinishSurface(env, qsdo);
}

#pragma mark --- Glyph Vector Pipeline ---

/*-----------------------------------
    Glyph Vector Pipeline

    doDrawGlyphs() has been separated into several pipelined functions to increase performance,
    and improve accountability for JNI resources, malloc'd memory, and error handling.

    Each stage of the pipeline is responsible for doing only one major thing, like allocating buffers,
    aquiring transform arrays from JNI, filling buffers, or striking glyphs. All resources or memory
    acquired at a given stage, must be released in that stage. Any error that occurs (like a failed malloc)
    is to be handled in the stage it occurs in, and is to return immediatly after freeing it's resources.

-----------------------------------*/

static jclass jc_StandardGlyphVector = NULL;
#define GET_SGV_CLASS() GET_CLASS(jc_StandardGlyphVector, "sun/font/StandardGlyphVector");

// Checks the GlyphVector Java object for any transforms that were applied to individual characters. If none are present,
// strike the glyphs immediately in Core Graphics. Otherwise, obtain the arrays, and defer to above.
static inline void doDrawGlyphsPipe_checkForPerGlyphTransforms
(JNIEnv *env, QuartzSDOps *qsdo, const AWTStrike *strike, jobject gVector, BOOL useSubstituion, int *uniChars, CGGlyph *glyphs, CGSize *advances, size_t length)
{
    // if we have no character substitution, and no per-glyph transformations - strike now!
    GET_SGV_CLASS();
    DECLARE_FIELD(jm_StandardGlyphVector_gti, jc_StandardGlyphVector, "gti", "Lsun/font/StandardGlyphVector$GlyphTransformInfo;");
    jobject gti = (*env)->GetObjectField(env, gVector, jm_StandardGlyphVector_gti);
    if (gti == 0)
    {
        if (useSubstituion)
        {
            // quasi-simple case, substitution, but no per-glyph transforms
            JavaCT_DrawGlyphVector(qsdo, strike, TRUE, uniChars, glyphs, advances, NULL, NULL, length);
        }
        else
        {
            // fast path, straight to CG without per-glyph transforms
            CGContextShowGlyphsWithAdvances(qsdo->cgRef, glyphs, advances, length);
        }
        return;
    }

    DECLARE_CLASS(jc_StandardGlyphVector_GlyphTransformInfo, "sun/font/StandardGlyphVector$GlyphTransformInfo");
    DECLARE_FIELD(jm_StandardGlyphVector_GlyphTransformInfo_transforms, jc_StandardGlyphVector_GlyphTransformInfo, "transforms", "[D");
    jdoubleArray g_gtiTransformsArray = (*env)->GetObjectField(env, gti, jm_StandardGlyphVector_GlyphTransformInfo_transforms); //(*env)->GetObjectField(env, gti, g_gtiTransforms);
    if (g_gtiTransformsArray == NULL) {
        return;
    }
    jdouble *g_gvTransformsAsDoubles = (*env)->GetPrimitiveArrayCritical(env, g_gtiTransformsArray, NULL);
    if (g_gvTransformsAsDoubles == NULL) {
        (*env)->DeleteLocalRef(env, g_gtiTransformsArray);
        return;
    }

    DECLARE_FIELD(jm_StandardGlyphVector_GlyphTransformInfo_indices, jc_StandardGlyphVector_GlyphTransformInfo, "indices", "[I");
    jintArray g_gtiTXIndicesArray = (*env)->GetObjectField(env, gti, jm_StandardGlyphVector_GlyphTransformInfo_indices);
    jint *g_gvTXIndicesAsInts = (*env)->GetPrimitiveArrayCritical(env, g_gtiTXIndicesArray, NULL);
    if (g_gvTXIndicesAsInts == NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, g_gtiTransformsArray, g_gvTransformsAsDoubles, JNI_ABORT);
        (*env)->DeleteLocalRef(env, g_gtiTransformsArray);
        (*env)->DeleteLocalRef(env, g_gtiTXIndicesArray);
        return;
    }
    // slowest case, we have per-glyph transforms, and possibly glyph substitution as well
    JavaCT_DrawGlyphVector(qsdo, strike, useSubstituion, uniChars, glyphs, advances, g_gvTXIndicesAsInts, g_gvTransformsAsDoubles, length);

    (*env)->ReleasePrimitiveArrayCritical(env, g_gtiTransformsArray, g_gvTransformsAsDoubles, JNI_ABORT);
    (*env)->ReleasePrimitiveArrayCritical(env, g_gtiTXIndicesArray, g_gvTXIndicesAsInts, JNI_ABORT);

    (*env)->DeleteLocalRef(env, g_gtiTransformsArray);
    (*env)->DeleteLocalRef(env, g_gtiTXIndicesArray);
}

// Retrieves advances for translated unicodes
// Uses "glyphs" as a temporary buffer for the glyph-to-unicode translation
void JavaCT_GetAdvancesForUnichars
(const NSFont *font, const int uniChars[], CGGlyph glyphs[], const size_t length, CGSize advances[])
{
    // cycle over each spot, and if we discovered a unicode to substitute, we have to calculate the advance for it
    size_t i;
    for (i = 0; i < length; i++)
    {
        UniChar uniChar = uniChars[i];
        if (uniChar == 0) continue;

        CGGlyph glyph = 0;
        const CTFontRef fallback = JRSFontCreateFallbackFontForCharacters((CTFontRef)font, &uniChar, 1);
        if (fallback) {
            CTFontGetGlyphsForCharacters(fallback, &uniChar, &glyph, 1);
            CTFontGetAdvancesForGlyphs(fallback, kCTFontDefaultOrientation, &glyph, &(advances[i]), 1);
            CFRelease(fallback);
        }

        glyphs[i] = glyph;
    }
}

// Fills the glyph buffer with glyphs from the GlyphVector object. Also checks to see if the glyph's positions have been
// already caculated from GlyphVector, or we simply ask Core Graphics to make some advances for us. Pre-calculated positions
// are translated into advances, since CG only understands advances.
static inline void doDrawGlyphsPipe_fillGlyphAndAdvanceBuffers
(JNIEnv *env, QuartzSDOps *qsdo, const AWTStrike *strike, jobject gVector, CGGlyph *glyphs, int *uniChars, CGSize *advances, size_t length, jintArray glyphsArray)
{
    // fill the glyph buffer
    jint *glyphsAsInts = (*env)->GetPrimitiveArrayCritical(env, glyphsArray, NULL);
    if (glyphsAsInts == NULL) {
        return;
    }

    // if a glyph code from Java is negative, that means it is really a unicode value
    // which we can use in CoreText to strike the character in another font
    size_t i;
    BOOL complex = NO;
    for (i = 0; i < length; i++)
    {
        jint code = glyphsAsInts[i];
        if (code < 0)
        {
            complex = YES;
            uniChars[i] = -code;
            glyphs[i] = 0;
        }
        else
        {
            uniChars[i] = 0;
            glyphs[i] = code;
        }
    }

    (*env)->ReleasePrimitiveArrayCritical(env, glyphsArray, glyphsAsInts, JNI_ABORT);

    // fill the advance buffer
    GET_SGV_CLASS();
    DECLARE_FIELD(jm_StandardGlyphVector_positions, jc_StandardGlyphVector, "positions", "[F");
    jfloatArray posArray = (*env)->GetObjectField(env, gVector, jm_StandardGlyphVector_positions);
    jfloat *positions = NULL;
    if (posArray != NULL) {
        // in this case, the positions have already been pre-calculated for us on the Java side
        positions = (*env)->GetPrimitiveArrayCritical(env, posArray, NULL);
        if (positions == NULL) {
            (*env)->DeleteLocalRef(env, posArray);
        }
    }
    if (positions != NULL) {
        CGPoint prev;
        prev.x = positions[0];
        prev.y = positions[1];

        // <rdar://problem/4294061> take the first point, and move the context to that location
        CGContextTranslateCTM(qsdo->cgRef, prev.x, prev.y);

        CGAffineTransform invTx = CGAffineTransformInvert(strike->fFontTx);

        // for each position, figure out the advance (since CG won't take positions directly)
        size_t i;
        for (i = 0; i < length - 1; i++)
        {
            size_t i2 = (i+1) * 2;
            CGPoint pt;
            pt.x = positions[i2];
            pt.y = positions[i2+1];
            pt = CGPointApplyAffineTransform(pt, invTx);
            advances[i].width = pt.x - prev.x;
            advances[i].height = -(pt.y - prev.y); // negative to translate to device space
            prev.x = pt.x;
            prev.y = pt.y;
        }

        (*env)->ReleasePrimitiveArrayCritical(env, posArray, positions, JNI_ABORT);
        (*env)->DeleteLocalRef(env, posArray);
    }
    else
    {
        // in this case, we have to go and calculate the positions ourselves
        // there were no pre-calculated positions from the glyph buffer on the Java side
        AWTFont *awtFont = strike->fAWTFont;
        CTFontGetAdvancesForGlyphs((CTFontRef)awtFont->fFont, kCTFontDefaultOrientation, glyphs, advances, length);

        if (complex)
        {
            JavaCT_GetAdvancesForUnichars(awtFont->fFont, uniChars, glyphs, length, advances);
        }
    }

    // continue on to the next stage of the pipe
    doDrawGlyphsPipe_checkForPerGlyphTransforms(env, qsdo, strike, gVector, complex, uniChars, glyphs, advances, length);
}

// Obtains the glyph array to determine the number of glyphs we are dealing with. If we are dealing a large number of glyphs,
// we malloc a buffer to hold the glyphs and their advances, otherwise we use stack allocated buffers.
static inline void doDrawGlyphsPipe_getGlyphVectorLengthAndAlloc
(JNIEnv *env, QuartzSDOps *qsdo, const AWTStrike *strike, jobject gVector)
{
    GET_SGV_CLASS();
    DECLARE_FIELD(jm_StandardGlyphVector_glyphs, jc_StandardGlyphVector, "glyphs", "[I");
    jintArray glyphsArray = (*env)->GetObjectField(env, gVector, jm_StandardGlyphVector_glyphs);
    jsize length = (*env)->GetArrayLength(env, glyphsArray);

    if (length == 0)
    {
        // nothing to draw
        (*env)->DeleteLocalRef(env, glyphsArray);
        return;
    }

    if (length < MAX_STACK_ALLOC_GLYPH_BUFFER_SIZE)
    {
        // if we are small enough, fit everything onto the stack
        CGGlyph glyphs[length];
        int uniChars[length];
        CGSize advances[length];
        doDrawGlyphsPipe_fillGlyphAndAdvanceBuffers(env, qsdo, strike, gVector, glyphs, uniChars, advances, length, glyphsArray);
    }
    else
    {
        // otherwise, we should malloc and free buffers for this large run
        CGGlyph *glyphs = (CGGlyph *)malloc(sizeof(CGGlyph) * length);
        int *uniChars = (int *)malloc(sizeof(int) * length);
        CGSize *advances = (CGSize *)malloc(sizeof(CGSize) * length);

        if (glyphs == NULL || uniChars == NULL || advances == NULL)
        {
            (*env)->DeleteLocalRef(env, glyphsArray);
            [NSException raise:NSMallocException format:@"%s-%s:%d", __FILE__, __FUNCTION__, __LINE__];
            if (glyphs)
            {
                free(glyphs);
            }
            if (uniChars)
            {
                free(uniChars);
            }
            if (advances)
            {
                free(advances);
            }
            return;
        }

        doDrawGlyphsPipe_fillGlyphAndAdvanceBuffers(env, qsdo, strike, gVector, glyphs, uniChars, advances, length, glyphsArray);

        free(glyphs);
        free(uniChars);
        free(advances);
    }

    (*env)->DeleteLocalRef(env, glyphsArray);
}

// Setup and save the state of the CGContext, and apply any java.awt.Font transforms to the context.
static inline void doDrawGlyphsPipe_applyFontTransforms
(JNIEnv *env, QuartzSDOps *qsdo, const AWTStrike *strike, jobject gVector, const jfloat x, const jfloat y)
{
    CGContextRef cgRef = qsdo->cgRef;
    CGContextSetFontSize(cgRef, 1.0);
    CGContextSetFont(cgRef, strike->fAWTFont->fNativeCGFont);
    CGContextSetTextMatrix(cgRef, CGAffineTransformIdentity);

    CGAffineTransform tx = strike->fFontTx;
    tx.tx += x;
    tx.ty += y;
    CGContextConcatCTM(cgRef, tx);

    doDrawGlyphsPipe_getGlyphVectorLengthAndAlloc(env, qsdo, strike, gVector);
}


#pragma mark --- CTextPipe JNI ---


/*
 * Class:     sun_lwawt_macosx_CTextPipe
 * Method:    doDrawString
 * Signature: (Lsun/java2d/SurfaceData;JLjava/lang/String;DD)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CTextPipe_doDrawString
(JNIEnv *env, jobject jthis, jobject jsurfacedata, jlong awtStrikePtr, jstring str, jdouble x, jdouble y)
{
    if (str == NULL) {
        return;
    }
    QuartzSDOps *qsdo = (QuartzSDOps *)SurfaceData_GetOps(env, jsurfacedata);
    AWTStrike *awtStrike = (AWTStrike *)jlong_to_ptr(awtStrikePtr);

JNI_COCOA_ENTER(env);

    jsize len = (*env)->GetStringLength(env, str);

    if (len < MAX_STACK_ALLOC_GLYPH_BUFFER_SIZE) // optimized for stack allocation <rdar://problem/4285041>
    {
        jchar unichars[len];
        (*env)->GetStringRegion(env, str, 0, len, unichars);
        CHECK_EXCEPTION();

        // Draw the text context
        DrawTextContext(env, qsdo, awtStrike, unichars, len, x, y);
    }
    else
    {
        // Get string to draw and the length
        const jchar *unichars = (*env)->GetStringChars(env, str, NULL);
        if (unichars == NULL) {
            JNU_ThrowOutOfMemoryError(env, "Could not get string chars");
            return;
        }

        // Draw the text context
        DrawTextContext(env, qsdo, awtStrike, unichars, len, x, y);

        (*env)->ReleaseStringChars(env, str, unichars);
    }

JNI_COCOA_RENDERER_EXIT(env);
}


/*
 * Class:     sun_lwawt_macosx_CTextPipe
 * Method:    doUnicodes
 * Signature: (Lsun/java2d/SurfaceData;J[CIIFF)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CTextPipe_doUnicodes
(JNIEnv *env, jobject jthis, jobject jsurfacedata, jlong awtStrikePtr, jcharArray unicodes, jint offset, jint length, jfloat x, jfloat y)
{
    QuartzSDOps *qsdo = (QuartzSDOps *)SurfaceData_GetOps(env, jsurfacedata);
    AWTStrike *awtStrike = (AWTStrike *)jlong_to_ptr(awtStrikePtr);

JNI_COCOA_ENTER(env);

    // Setup the text context
    if (length < MAX_STACK_ALLOC_GLYPH_BUFFER_SIZE) // optimized for stack allocation
    {
        jchar copyUnichars[length];
        (*env)->GetCharArrayRegion(env, unicodes, offset, length, copyUnichars);
        CHECK_EXCEPTION();
        DrawTextContext(env, qsdo, awtStrike, copyUnichars, length, x, y);
    }
    else
    {
        jchar *copyUnichars = malloc(length * sizeof(jchar));
        if (!copyUnichars) {
            JNU_ThrowOutOfMemoryError(env, "Failed to malloc memory to create the glyphs for string drawing");
            return;
        }

        @try {
            (*env)->GetCharArrayRegion(env, unicodes, offset, length, copyUnichars);
            CHECK_EXCEPTION();
            DrawTextContext(env, qsdo, awtStrike, copyUnichars, length, x, y);
        } @finally {
            free(copyUnichars);
        }
    }

JNI_COCOA_RENDERER_EXIT(env);
}

/*
 * Class:     sun_lwawt_macosx_CTextPipe
 * Method:    doOneUnicode
 * Signature: (Lsun/java2d/SurfaceData;JCFF)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CTextPipe_doOneUnicode
(JNIEnv *env, jobject jthis, jobject jsurfacedata, jlong awtStrikePtr, jchar aUnicode, jfloat x, jfloat y)
{
    QuartzSDOps *qsdo = (QuartzSDOps *)SurfaceData_GetOps(env, jsurfacedata);
    AWTStrike *awtStrike = (AWTStrike *)jlong_to_ptr(awtStrikePtr);

JNI_COCOA_ENTER(env);

    DrawTextContext(env, qsdo, awtStrike, &aUnicode, 1, x, y);

JNI_COCOA_RENDERER_EXIT(env);
}

/*
 * Class: sun_lwawt_macosx_CTextPipe
 * Method: doDrawGlyphs
 * Signature: (Lsun/java2d/SurfaceData;JLjava/awt/font/GlyphVector;FF)V
 */
JNIEXPORT void JNICALL Java_sun_lwawt_macosx_CTextPipe_doDrawGlyphs
(JNIEnv *env, jobject jthis, jobject jsurfacedata, jlong awtStrikePtr, jobject gVector, jfloat x, jfloat y)
{
    QuartzSDOps *qsdo = (QuartzSDOps *)SurfaceData_GetOps(env, jsurfacedata);
    AWTStrike *awtStrike = (AWTStrike *)jlong_to_ptr(awtStrikePtr);

JNI_COCOA_ENTER(env);

    qsdo->BeginSurface(env, qsdo, SD_Text);
    if (qsdo->cgRef == NULL)
    {
        qsdo->FinishSurface(env, qsdo);
        return;
    }

    CGContextSaveGState(qsdo->cgRef);
    JRSFontSetRenderingStyleOnContext(qsdo->cgRef, JRSFontGetRenderingStyleForHints(sun_awt_SunHints_INTVAL_FRACTIONALMETRICS_ON, sun_awt_SunHints_INTVAL_TEXT_ANTIALIAS_ON));

    doDrawGlyphsPipe_applyFontTransforms(env, qsdo, awtStrike, gVector, x, y);

    CGContextRestoreGState(qsdo->cgRef);

    qsdo->FinishSurface(env, qsdo);

JNI_COCOA_RENDERER_EXIT(env);
}
