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

#import "QuartzSurfaceData.h"

#import "java_awt_BasicStroke.h"
#import "java_awt_AlphaComposite.h"
#import "java_awt_geom_PathIterator.h"
#import "java_awt_image_BufferedImage.h"
#import "sun_awt_SunHints.h"
#import "sun_java2d_CRenderer.h"
#import "sun_java2d_OSXSurfaceData.h"
#import "sun_lwawt_macosx_CPrinterSurfaceData.h"
#import "ImageSurfaceData.h"

#import <AppKit/AppKit.h>
#import "ThreadUtilities.h"

//#define DEBUG
#if defined DEBUG
    #define PRINT(msg) {fprintf(stderr, "%s\n", msg);}
#else
    #define PRINT(msg) {}
#endif

#define kOffset (0.5f)

#define JNI_COCOA_THROW_OOME(env, msg) \
    if ([NSThread isMainThread] == NO) { \
         JNU_ThrowOutOfMemoryError(env, msg); \
    } \
    [NSException raise:@"Java Exception" reason:@"Java OutOfMemoryException" userInfo:nil]

BOOL gAdjustForJavaDrawing;

#pragma mark
#pragma mark --- Color Cache ---

// Creating and deleting CGColorRefs can be expensive, therefore we have a color cache.
// The color cache was first introduced with <rdar://problem/3923927>
// With <rdar://problem/4280514>, the hashing function was improved
// With <rdar://problem/4012223>, the color cache became global (per process) instead of per surface.

// Must be power of 2. 1024 is the least power of 2 number that makes SwingSet2 run without any non-empty cache misses
#define gColorCacheSize 1024
struct _ColorCacheInfo
{
    UInt32        keys[gColorCacheSize];
    CGColorRef    values[gColorCacheSize];
};
static struct _ColorCacheInfo colorCacheInfo;

static pthread_mutex_t gColorCacheLock = PTHREAD_MUTEX_INITIALIZER;

// given a UInt32 color, it tries to find that find the corresponding CGColorRef in the hash cache. If the CGColorRef
// doesn't exist or there is a collision, it creates a new one CGColorRef and put's in the cache. Then,
// it sets with current fill/stroke color for the CGContext passed in (qsdo->cgRef).
void setCachedColor(QuartzSDOps *qsdo, UInt32 color)
{
    static const CGFloat kColorConversionMultiplier = 1.0f/255.0f;

    pthread_mutex_lock(&gColorCacheLock);

    static CGColorSpaceRef colorspace = NULL;
    if (colorspace == NULL)
    {
        colorspace = CGColorSpaceCreateWithName(kCGColorSpaceGenericRGB);
    }

    CGColorRef cgColor = NULL;

    // The colors passed have low randomness. That means we need to scramble the bits of the color
    // to produce a good hash key. After some analysis, it looks like Thomas's Wang integer hasing algorithm
    // seems a nice trade off between performance and effectivness.
    UInt32 index = color;
    index += ~(index << 15);
    index ^=  (index >> 10);
    index +=  (index << 3);
    index ^=  (index >> 6);
    index += ~(index << 11);
    index ^=  (index >> 16);
    index = index & (gColorCacheSize - 1);   // The bits are scrambled, we just need to make sure it fits inside our table

    UInt32 key = colorCacheInfo.keys[index];
    CGColorRef value = colorCacheInfo.values[index];
    if ((key == color) && (value != NULL))
    {
        //fprintf(stderr, "+");fflush(stderr);//hit
        cgColor = value;
    }
    else
    {
        if (value != NULL)
        {
            //fprintf(stderr, "!");fflush(stderr);//miss and replace - double ouch
            CGColorRelease(value);
        }
        //fprintf(stderr, "-");fflush(stderr);// miss

        CGFloat alpha = ((color>>24)&0xff)*kColorConversionMultiplier;
        CGFloat red = ((color>>16)&0xff)*kColorConversionMultiplier;
        CGFloat green = ((color>>8)&0xff)*kColorConversionMultiplier;
        CGFloat blue = ((color>>0)&0xff)*kColorConversionMultiplier;
        const CGFloat components[] = {red, green, blue, alpha, 1.0f};
        value = CGColorCreate(colorspace, components);

        colorCacheInfo.keys[index] = color;
        colorCacheInfo.values[index] = value;

        cgColor = value;
    }

    CGContextSetStrokeColorWithColor(qsdo->cgRef, cgColor);
    CGContextSetFillColorWithColor(qsdo->cgRef, cgColor);

    pthread_mutex_unlock(&gColorCacheLock);
}

#pragma mark
#pragma mark --- Gradient ---

// this function MUST NOT be inlined!
void gradientLinearPaintEvaluateFunction(void *info, const CGFloat *in, CGFloat *out)
{
    StateShadingInfo *shadingInfo = (StateShadingInfo *)info;
    CGFloat *colors = shadingInfo->colors;
    CGFloat range = *in;
    CGFloat c1, c2;
    jint k;

//fprintf(stderr, "range=%f\n", range);
    for (k=0; k<4; k++)
    {
        c1 = colors[k];
//fprintf(stderr, "    c1=%f", c1);
        c2 = colors[k+4];
//fprintf(stderr, ", c2=%f", c2);
        if (c1 == c2)
        {
            *out++ = c2;
//fprintf(stderr, ", %f", *(out-1));
        }
        else if (c1 > c2)
        {
            *out++ = c1 - ((c1-c2)*range);
//fprintf(stderr, ", %f", *(out-1));
        }
        else// if (c1 < c2)
        {
            *out++ = c1 + ((c2-c1)*range);
//fprintf(stderr, ", %f", *(out-1));
        }
//fprintf(stderr, "\n");
    }
}

// this function MUST NOT be inlined!
void gradientCyclicPaintEvaluateFunction(void *info, const CGFloat *in, CGFloat *out)
{
    StateShadingInfo *shadingInfo = (StateShadingInfo *)info;
    CGFloat length = shadingInfo->length ;
    CGFloat period = shadingInfo->period;
    CGFloat offset = shadingInfo->offset;
    CGFloat periodLeft = offset;
    CGFloat periodRight = periodLeft+period;
    CGFloat *colors = shadingInfo->colors;
    CGFloat range = *in;
    CGFloat c1, c2;
    jint k;
    jint count = 0;

    range *= length;

    // put the range within the period
    if (range < periodLeft)
    {
        while (range < periodLeft)
        {
            range += period;
            count++;
        }

        range = range-periodLeft;
    }
    else if (range > periodRight)
    {
        count = 1;

        while (range > periodRight)
        {
            range -= period;
            count++;
        }

        range = periodRight-range;
    }
    else
    {
        range = range - offset;
    }
    range = range/period;

    // cycle up or down
    if (count%2 == 0)
    {
        for (k=0; k<4; k++)
        {
            c1 = colors[k];
            c2 = colors[k+4];
            if (c1 == c2)
            {
                *out++ = c2;
            }
            else if (c1 > c2)
            {
                *out++ = c1 - ((c1-c2)*range);
            }
            else// if (c1 < c2)
            {
                *out++ = c1 + ((c2-c1)*range);
            }
        }
    }
    else
    {
        for (k=0; k<4; k++)
        {
            c1 = colors[k+4];
            c2 = colors[k];
            if (c1 == c2)
            {
                *out++ = c2;
            }
            else if (c1 > c2)
            {
                *out++ = c1 - ((c1-c2)*range);
            }
            else// if (c1 < c2)
            {
                *out++ = c1 + ((c2-c1)*range);
            }
        }
    }
 }

// this function MUST NOT be inlined!
void gradientPaintReleaseFunction(void *info)
{
PRINT("    gradientPaintReleaseFunction")
    free(info);
}

static inline void contextQuartzLinearGradientPath(QuartzSDOps* qsdo)
{

PRINT("    contextQuartzLinearGradientPath");

    CGContextRef cgRef = qsdo->cgRef;
    StateGradientInfo *gradientInfo = qsdo->gradientInfo;

    CGColorSpaceRef colorspace = CGColorSpaceCreateWithName(kCGColorSpaceGenericRGB);
    size_t num_locations = gradientInfo->fractionsLength;
    CGFloat *locations = (CGFloat *) malloc(sizeof(CGFloat) * num_locations);
    int i = 0;
    size_t component_size = num_locations * 4;
    CGFloat components[component_size];
    CGGradientRef gradient = NULL;

    for (i = 0; i < num_locations; i++) {
        locations[i] = gradientInfo->fractionsdata[i];
    }
    for (i = 0; i < component_size; i++) {
        components[i] = gradientInfo->colordata[i];
    }
    CGContextSaveGState(cgRef);
    gradient = CGGradientCreateWithColorComponents(colorspace, components, locations, num_locations);
    if (qsdo->isEvenOddFill) {
        CGContextEOClip(cgRef);
    } else {
        CGContextClip(cgRef);
    }
    CGContextDrawLinearGradient(cgRef, gradient, gradientInfo->start, gradientInfo->end, kCGGradientDrawsAfterEndLocation);

    CGContextRestoreGState(cgRef);
    CGColorSpaceRelease(colorspace);
    CGGradientRelease(gradient);
    free(locations);
    free(gradientInfo->colordata);
    free(gradientInfo->fractionsdata);
}

static inline void contextQuartzRadialGradientPath(QuartzSDOps* qsdo)
{

PRINT("    contextQuartzRadialGradientPath");

    CGContextRef cgRef = qsdo->cgRef;
    StateGradientInfo *gradientInfo = qsdo->gradientInfo;

    CGColorSpaceRef colorspace = CGColorSpaceCreateWithName(kCGColorSpaceGenericRGB);
    size_t num_locations = gradientInfo->fractionsLength;
    CGFloat *locations = (CGFloat *) malloc(sizeof(CGFloat) * num_locations);
    int i = 0;
    size_t component_size = num_locations * 4;
    CGFloat components[component_size];
    CGGradientRef gradient = NULL;
    CGFloat startRadius = gradientInfo->radius;
    CGFloat endRadius = gradientInfo->radius;

    for (i = 0; i < num_locations; i++) {
        locations[i] = gradientInfo->fractionsdata[i];
    }
    for (i = 0; i < component_size; i++) {
        components[i] = gradientInfo->colordata[i];
    }
    CGContextSaveGState(cgRef);
    gradient = CGGradientCreateWithColorComponents(colorspace, components, locations, num_locations);
    if (qsdo->isEvenOddFill) {
        CGContextEOClip(cgRef);
    } else {
        CGContextClip(cgRef);
    }
    CGContextDrawRadialGradient(cgRef, gradient, gradientInfo->start, 0, gradientInfo->end, endRadius, kCGGradientDrawsAfterEndLocation);

    CGContextRestoreGState(cgRef);
    CGColorSpaceRelease(colorspace);
    CGGradientRelease(gradient);
    free(locations);
    free(gradientInfo->colordata);
    free(gradientInfo->fractionsdata);
}

static inline void contextGradientPath(QuartzSDOps* qsdo)
{
PRINT("    ContextGradientPath")

    CGContextRef cgRef = qsdo->cgRef;
    StateShadingInfo* shadingInfo = qsdo->shadingInfo;

    CGRect bounds = CGContextGetClipBoundingBox(cgRef);

    static const CGFloat domain[2] = {0.0f, 1.0f};
    static const CGFloat range[8] = {0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f};
    CGColorSpaceRef colorspace = CGColorSpaceCreateWithName(kCGColorSpaceGenericRGB);
    CGFunctionRef shadingFunc = NULL;
    CGShadingRef shading = NULL;
    if (shadingInfo->cyclic == NO)
    {
        static const CGFunctionCallbacks callbacks = {0, &gradientLinearPaintEvaluateFunction, &gradientPaintReleaseFunction};
        shadingFunc = CGFunctionCreate((void *)shadingInfo, 1, domain, 4, range, &callbacks);
        shading = CGShadingCreateAxial(colorspace, shadingInfo->start, shadingInfo->end, shadingFunc, 1, 1);
    }
    else
    {
//fprintf(stderr, "BOUNDING BOX x1=%f, y1=%f x2=%f, y2=%f\n", bounds.origin.x, bounds.origin.y, bounds.origin.x+bounds.size.width, bounds.origin.y+bounds.size.height);
        // need to extend the line start-end

        CGFloat x1 = shadingInfo->start.x;
        CGFloat y1 = shadingInfo->start.y;
        CGFloat x2 = shadingInfo->end.x;
        CGFloat y2 = shadingInfo->end.y;
//fprintf(stderr, "GIVEN x1=%f, y1=%f      x2=%f, y2=%f\n", x1, y1, x2, y2);

        if (x1 == x2)
        {
            y1 = bounds.origin.y;
            y2 = y1 + bounds.size.height;
        }
        else if (y1 == y2)
        {
            x1 = bounds.origin.x;
            x2 = x1 + bounds.size.width;
        }
        else
        {
            // find the original line function y = mx + c
            CGFloat m1 = (y2-y1)/(x2-x1);
            CGFloat c1 = y1 - m1*x1;
//fprintf(stderr, "         m1=%f, c1=%f\n", m1, c1);

            // a line perpendicular to the original one will have the slope
            CGFloat m2 = -(1/m1);
//fprintf(stderr, "         m2=%f\n", m2);

            // find the only 2 possible lines perpendicular to the original line, passing the two top corners of the bounding box
            CGFloat x1A = bounds.origin.x;
            CGFloat y1A = bounds.origin.y;
            CGFloat c1A = y1A - m2*x1A;
//fprintf(stderr, "         x1A=%f, y1A=%f, c1A=%f\n", x1A, y1A, c1A);
            CGFloat x1B = bounds.origin.x+bounds.size.width;
            CGFloat y1B = bounds.origin.y;
            CGFloat c1B = y1B - m2*x1B;
//fprintf(stderr, "         x1B=%f, y1B=%f, c1B=%f\n", x1B, y1B, c1B);

            // find the crossing points of the original line and the two lines we computed above to find the new possible starting points
            CGFloat x1Anew = (c1A-c1)/(m1-m2);
            CGFloat y1Anew = m2*x1Anew + c1A;
            CGFloat x1Bnew = (c1B-c1)/(m1-m2);
            CGFloat y1Bnew = m2*x1Bnew + c1B;
//fprintf(stderr, "NEW x1Anew=%f, y1Anew=%f      x1Bnew=%f, y1Bnew=%f\n", x1Anew, y1Anew, x1Bnew, y1Bnew);

            // select the new starting point
            if (y1Anew <= y1Bnew)
            {
                x1 = x1Anew;
                y1 = y1Anew;
            }
            else
            {
                x1 = x1Bnew;
                y1 = y1Bnew;
            }
//fprintf(stderr, "--- NEW x1=%f, y1=%f\n", x1, y1);

            // find the only 2 possible lines perpendicular to the original line, passing the two bottom corners of the bounding box
            CGFloat x2A = bounds.origin.x;
            CGFloat y2A = bounds.origin.y+bounds.size.height;
            CGFloat c2A = y2A - m2*x2A;
//fprintf(stderr, "         x2A=%f, y2A=%f, c2A=%f\n", x2A, y2A, c2A);
            CGFloat x2B = bounds.origin.x+bounds.size.width;
            CGFloat y2B = bounds.origin.y+bounds.size.height;
            CGFloat c2B = y2B - m2*x2B;
//fprintf(stderr, "         x2B=%f, y2B=%f, c2B=%f\n", x2B, y2B, c2B);

            // find the crossing points of the original line and the two lines we computed above to find the new possible ending points
            CGFloat x2Anew = (c2A-c1)/(m1-m2);
            CGFloat y2Anew = m2*x2Anew + c2A;
            CGFloat x2Bnew = (c2B-c1)/(m1-m2);
            CGFloat y2Bnew = m2*x2Bnew + c2B;
//fprintf(stderr, "NEW x2Anew=%f, y2Anew=%f      x2Bnew=%f, y2Bnew=%f\n", x2Anew, y2Anew, x2Bnew, y2Bnew);

            // select the new ending point
            if (y2Anew >= y2Bnew)
            {
                x2 = x2Anew;
                y2 = y2Anew;
            }
            else
            {
                x2 = x2Bnew;
                y2 = y2Bnew;
            }
//fprintf(stderr, "--- NEW x2=%f, y2=%f\n", x2, y2);
        }

        qsdo->shadingInfo->period = sqrt(pow(shadingInfo->end.x-shadingInfo->start.x, 2.0) + pow(shadingInfo->end.y-shadingInfo->start.y, 2.0));
        if ((qsdo->shadingInfo->period != 0))
        {
            // compute segment lengths that we will need for the gradient function
            qsdo->shadingInfo->length = sqrt(pow(x2-x1, 2.0) + pow(y2-y1, 2.0));
            qsdo->shadingInfo->offset = sqrt(pow(shadingInfo->start.x-x1, 2.0) + pow(shadingInfo->start.y-y1, 2.0));
//fprintf(stderr, "length=%f, period=%f, offset=%f\n", qsdo->shadingInfo->length, qsdo->shadingInfo->period, qsdo->shadingInfo->offset);

            CGPoint newStart = {x1, y1};
            CGPoint newEnd = {x2, y2};

            static const CGFunctionCallbacks callbacks = {0, &gradientCyclicPaintEvaluateFunction, &gradientPaintReleaseFunction};
            shadingFunc = CGFunctionCreate((void *)shadingInfo, 1, domain, 4, range, &callbacks);
            shading = CGShadingCreateAxial(colorspace, newStart, newEnd, shadingFunc, 0, 0);
        }
    }
    CGColorSpaceRelease(colorspace);

    if (shadingFunc != NULL)
    {
        CGContextSaveGState(cgRef);

        // rdar://problem/5214320
        // Gradient fills of Java GeneralPath don't respect the even odd winding rule (quartz pipeline).
        if (qsdo->isEvenOddFill) {
            CGContextEOClip(cgRef);
        } else {
            CGContextClip(cgRef);
        }
        CGContextDrawShading(cgRef, shading);

        CGContextRestoreGState(cgRef);
        CGShadingRelease(shading);
        CGFunctionRelease(shadingFunc);
        qsdo->shadingInfo = NULL;
    }
}

#pragma mark
#pragma mark --- Texture ---

// this function MUST NOT be inlined!
void texturePaintEvaluateFunction(void *info, CGContextRef cgRef)
{
    JNIEnv* env = [ThreadUtilities getJNIEnvUncached];

    StatePatternInfo* patternInfo = (StatePatternInfo*)info;
    ImageSDOps* isdo = LockImage(env, patternInfo->sdata);

    makeSureImageIsCreated(isdo);
    CGContextDrawImage(cgRef, CGRectMake(0.0f, 0.0f, patternInfo->width, patternInfo->height), isdo->imgRef);

    UnlockImage(env, isdo);
}

// this function MUST NOT be inlined!
void texturePaintReleaseFunction(void *info)
{
    PRINT("    texturePaintReleaseFunction")
    JNIEnv* env = [ThreadUtilities getJNIEnvUncached];

    StatePatternInfo* patternInfo = (StatePatternInfo*)info;
    (*env)->DeleteGlobalRef(env, patternInfo->sdata);

    free(info);
}

static inline void contextTexturePath(JNIEnv* env, QuartzSDOps* qsdo)
{
    PRINT("    ContextTexturePath")
    CGContextRef cgRef = qsdo->cgRef;
    StatePatternInfo* patternInfo = qsdo->patternInfo;

    CGAffineTransform ctm = CGContextGetCTM(cgRef);
    CGAffineTransform ptm = {patternInfo->sx, 0.0f, 0.0f, -patternInfo->sy, patternInfo->tx, patternInfo->ty};
    CGAffineTransform tm = CGAffineTransformConcat(ptm, ctm);
    CGFloat xStep = (CGFloat)qsdo->patternInfo->width;
    CGFloat yStep = (CGFloat)qsdo->patternInfo->height;
    CGPatternTiling tiling = kCGPatternTilingNoDistortion;
    BOOL isColored = YES;
    static const CGPatternCallbacks callbacks = {0, &texturePaintEvaluateFunction, &texturePaintReleaseFunction};
    CGPatternRef pattern = CGPatternCreate((void*)patternInfo, CGRectMake(0.0f, 0.0f, xStep, yStep), tm, xStep, yStep, tiling, isColored, &callbacks);

    CGColorSpaceRef colorspace = CGColorSpaceCreatePattern(NULL);
    static const CGFloat alpha = 1.0f;

    CGContextSaveGState(cgRef);

    CGContextSetFillColorSpace(cgRef, colorspace);
    CGContextSetFillPattern(cgRef, pattern, &alpha);
    CGContextSetRGBStrokeColor(cgRef, 0.0f, 0.0f, 0.0f, 1.0f);
    CGContextSetPatternPhase(cgRef, CGSizeMake(0.0f, 0.0f));
    // rdar://problem/5214320
    // Gradient fills of Java GeneralPath don't respect the even odd winding rule (quartz pipeline).
    if (qsdo->isEvenOddFill) {
        CGContextEOFillPath(cgRef);
    } else {
        CGContextFillPath(cgRef);
    }

    CGContextRestoreGState(cgRef);

    CGColorSpaceRelease(colorspace);
    CGPatternRelease(pattern);

    qsdo->patternInfo = NULL;
}

#pragma mark
#pragma mark --- Context Setup ---

static inline void setDefaultColorSpace(CGContextRef cgRef)
{
    static CGColorSpaceRef colorspace = NULL;
    if (colorspace == NULL)
    {
        colorspace = CGColorSpaceCreateWithName(kCGColorSpaceGenericRGB);
    }
    CGContextSetStrokeColorSpace(cgRef, colorspace);
    CGContextSetFillColorSpace(cgRef, colorspace);
}

void SetUpCGContext(JNIEnv *env, QuartzSDOps *qsdo, SDRenderType renderType)
{
PRINT(" SetUpCGContext")
    CGContextRef cgRef = qsdo->cgRef;
//fprintf(stderr, "%p ", cgRef);
    jint *javaGraphicsStates = qsdo->javaGraphicsStates;
    jfloat *javaFloatGraphicsStates = (jfloat*)(qsdo->javaGraphicsStates);

    jint changeFlags            = javaGraphicsStates[sun_java2d_OSXSurfaceData_kChangeFlagIndex];
    BOOL everyThingChanged        = qsdo->newContext || (changeFlags == sun_java2d_OSXSurfaceData_kEverythingChangedFlag);
    BOOL clipChanged            = everyThingChanged || ((changeFlags&sun_java2d_OSXSurfaceData_kClipChangedBit) != 0);
    BOOL transformChanged        = everyThingChanged || ((changeFlags&sun_java2d_OSXSurfaceData_kCTMChangedBit) != 0);
    BOOL paintChanged            = everyThingChanged || ((changeFlags&sun_java2d_OSXSurfaceData_kColorChangedBit) != 0);
    BOOL compositeChanged        = everyThingChanged || ((changeFlags&sun_java2d_OSXSurfaceData_kCompositeChangedBit) != 0);
    BOOL strokeChanged            = everyThingChanged || ((changeFlags&sun_java2d_OSXSurfaceData_kStrokeChangedBit) != 0);
//    BOOL fontChanged            = everyThingChanged || ((changeFlags&sun_java2d_OSXSurfaceData_kFontChangedBit) != 0);
    BOOL renderingHintsChanged  = everyThingChanged || ((changeFlags&sun_java2d_OSXSurfaceData_kHintsChangedBit) != 0);

//fprintf(stderr, "SetUpCGContext cgRef=%p new=%d changeFlags=%d, everyThingChanged=%d clipChanged=%d transformChanged=%d\n",
//                    cgRef, qsdo->newContext, changeFlags, everyThingChanged, clipChanged, transformChanged);

    if ((everyThingChanged == YES) || (clipChanged == YES) || (transformChanged == YES))
    {
        everyThingChanged = YES; // in case clipChanged or transformChanged

        CGContextRestoreGState(cgRef);  // restore to the original state

        CGContextSaveGState(cgRef);        // make our local copy of the state

        setDefaultColorSpace(cgRef);
    }

    if ((everyThingChanged == YES) || (clipChanged == YES))
    {
        if (javaGraphicsStates[sun_java2d_OSXSurfaceData_kClipStateIndex] == sun_java2d_OSXSurfaceData_kClipRect)
        {
            CGFloat x = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kClipXIndex];
            CGFloat y = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kClipYIndex];
            CGFloat w = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kClipWidthIndex];
            CGFloat h = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kClipHeightIndex];
            CGContextClipToRect(cgRef, CGRectMake(x, y, w, h));
        }
        else
        {
            BOOL eoFill = (javaGraphicsStates[sun_java2d_OSXSurfaceData_kClipWindingRuleIndex] == java_awt_geom_PathIterator_WIND_EVEN_ODD);
            jint numtypes = javaGraphicsStates[sun_java2d_OSXSurfaceData_kClipNumTypesIndex];

            jobject coordsarray = (jobject)((*env)->GetObjectArrayElement(env, qsdo->javaGraphicsStatesObjects, sun_java2d_OSXSurfaceData_kClipCoordinatesIndex));
            jobject typesarray = (jobject)((*env)->GetObjectArrayElement(env, qsdo->javaGraphicsStatesObjects, sun_java2d_OSXSurfaceData_kClipTypesIndex));

            jfloat* coords = (jfloat*)(*env)->GetDirectBufferAddress(env, coordsarray);
            jint* types = (jint*)(*env)->GetDirectBufferAddress(env, typesarray);

            DoShapeUsingCG(cgRef, types, coords, numtypes, NO, qsdo->graphicsStateInfo.offsetX, qsdo->graphicsStateInfo.offsetY);

            if (CGContextIsPathEmpty(cgRef) == 0)
            {
                if (eoFill)
                {
                    CGContextEOClip(cgRef);
                }
                else
                {
                    CGContextClip(cgRef);
                }
            }
            else
            {
                CGContextClipToRect(cgRef, CGRectZero);
            }
        }
    }
// for debugging
//CGContextResetClip(cgRef);

    if ((everyThingChanged == YES) || (transformChanged == YES))
    {
        CGFloat a = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kCTMaIndex];
        CGFloat b = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kCTMbIndex];
        CGFloat c = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kCTMcIndex];
        CGFloat d = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kCTMdIndex];
        CGFloat tx = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kCTMtxIndex];
        CGFloat ty = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kCTMtyIndex];

        CGContextConcatCTM(cgRef, CGAffineTransformMake(a, b, c, d, tx, ty));

        if (gAdjustForJavaDrawing == YES)
        {
            // find the offsets in the device corrdinate system
            CGAffineTransform ctm = CGContextGetCTM(cgRef);
            if ((qsdo->graphicsStateInfo.ctm.a != ctm.a) ||
                    (qsdo->graphicsStateInfo.ctm.b != ctm.b) ||
                        (qsdo->graphicsStateInfo.ctm.c != ctm.c) ||
                            (qsdo->graphicsStateInfo.ctm.d != ctm.d))
            {
                qsdo->graphicsStateInfo.ctm = ctm;
                // In CG affine xforms y' = bx+dy+ty
                // We need to flip both y coefficeints to flip the offset point into the java coordinate system.
                ctm.b = -ctm.b; ctm.d = -ctm.d; ctm.tx = 0.0f; ctm.ty = 0.0f;
                CGPoint offsets = {kOffset, kOffset};
                CGAffineTransform inverse = CGAffineTransformInvert(ctm);
                offsets = CGPointApplyAffineTransform(offsets, inverse);
                qsdo->graphicsStateInfo.offsetX = offsets.x;
                qsdo->graphicsStateInfo.offsetY = offsets.y;
            }
        }
        else
        {
            qsdo->graphicsStateInfo.offsetX = 0.0f;
            qsdo->graphicsStateInfo.offsetY = 0.0f;
        }
    }

// for debugging
//CGContextResetCTM(cgRef);

    if ((everyThingChanged == YES) || (compositeChanged == YES))
    {
        jint alphaCompositeRule = javaGraphicsStates[sun_java2d_OSXSurfaceData_kCompositeRuleIndex];
        CGFloat alphaCompositeValue = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kCompositeValueIndex];

        NSCompositingOperation op;
        switch (alphaCompositeRule)
        {
                case java_awt_AlphaComposite_CLEAR:
                op = NSCompositeClear;
                break;
            case java_awt_AlphaComposite_SRC:
                op = NSCompositeCopy;
                break;
            case java_awt_AlphaComposite_SRC_OVER:
                op = NSCompositeSourceOver;
                break;
            case java_awt_AlphaComposite_DST_OVER:
                op = NSCompositeDestinationOver;
                break;
            case java_awt_AlphaComposite_SRC_IN:
                op = NSCompositeSourceIn;
                break;
            case java_awt_AlphaComposite_DST_IN:
                op = NSCompositeDestinationIn;
                break;
            case java_awt_AlphaComposite_SRC_OUT:
                op = NSCompositeSourceOut;
                break;
            case java_awt_AlphaComposite_DST_OUT:
                op = NSCompositeDestinationOut;
                break;
            case java_awt_AlphaComposite_DST:
                // Alpha must be set to 0 because we're using the kCGCompositeSover rule
                op = NSCompositeSourceOver;
                alphaCompositeValue = 0.0f;
                break;
            case java_awt_AlphaComposite_SRC_ATOP:
                op = NSCompositeSourceAtop;
                break;
            case java_awt_AlphaComposite_DST_ATOP:
                op = NSCompositeDestinationAtop;
                break;
            case java_awt_AlphaComposite_XOR:
                op = NSCompositeXOR;
                break;
            default:
                op = NSCompositeSourceOver;
                alphaCompositeValue = 1.0f;
                break;
        }

        NSGraphicsContext *context = [NSGraphicsContext graphicsContextWithGraphicsPort:cgRef flipped:NO];
        //CGContextSetCompositeOperation(cgRef, op);
        [context setCompositingOperation:op];
        CGContextSetAlpha(cgRef, alphaCompositeValue);
    }

    if ((everyThingChanged == YES) || (renderingHintsChanged == YES))
    {
        jint antialiasHint = javaGraphicsStates[sun_java2d_OSXSurfaceData_kHintsAntialiasIndex];
//        jint textAntialiasHint = javaGraphicsStates[sun_java2d_OSXSurfaceData_kHintsTextAntialiasIndex];
        jint renderingHint = javaGraphicsStates[sun_java2d_OSXSurfaceData_kHintsRenderingIndex];
        jint interpolationHint = javaGraphicsStates[sun_java2d_OSXSurfaceData_kHintsInterpolationIndex];
//        jint textFractionalMetricsHint = javaGraphicsStates[sun_java2d_OSXSurfaceData_kHintsFractionalMetricsIndex];

        // 10-10-02 VL: since CoreGraphics supports only an interpolation quality attribute we have to map
        // both interpolationHint and renderingHint to an attribute value that best represents their combination.
        // (See Radar 3071704.) We'll go for the best quality. CG maps interpolation quality values as follows:
        // kCGInterpolationNone - nearest_neighbor
        // kCGInterpolationLow - bilinear
        // kCGInterpolationHigh - Lanczos (better than bicubic)
        CGInterpolationQuality interpolationQuality = kCGInterpolationDefault;
        // First check if the interpolation hint is suggesting to turn off interpolation:
        if (interpolationHint == sun_awt_SunHints_INTVAL_INTERPOLATION_NEAREST_NEIGHBOR)
        {
            interpolationQuality = kCGInterpolationNone;
        }
        else if ((interpolationHint >= sun_awt_SunHints_INTVAL_INTERPOLATION_BICUBIC) || (renderingHint >= sun_awt_SunHints_INTVAL_RENDER_QUALITY))
        {
            // Use >= just in case Sun adds some hint values in the future - this check wouldn't fall apart then:
            interpolationQuality = kCGInterpolationHigh;
        }
        else if (interpolationHint == sun_awt_SunHints_INTVAL_INTERPOLATION_BILINEAR)
        {
            interpolationQuality = kCGInterpolationLow;
        }
        else if (renderingHint == sun_awt_SunHints_INTVAL_RENDER_SPEED)
        {
            interpolationQuality = kCGInterpolationNone;
        }
        // else interpolationHint == -1 || renderingHint == sun_awt_SunHints_INTVAL_CSURFACE_DEFAULT --> kCGInterpolationDefault
        CGContextSetInterpolationQuality(cgRef, interpolationQuality);
        qsdo->graphicsStateInfo.interpolation = interpolationQuality;

        // antialiasing
        BOOL antialiased = (antialiasHint == sun_awt_SunHints_INTVAL_ANTIALIAS_ON);
        CGContextSetShouldAntialias(cgRef, antialiased);
        qsdo->graphicsStateInfo.antialiased = antialiased;
    }

    if ((everyThingChanged == YES) || (strokeChanged == YES))
    {
        qsdo->graphicsStateInfo.simpleStroke = YES;

        CGFloat linewidth = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kStrokeWidthIndex];
        jint linejoin = javaGraphicsStates[sun_java2d_OSXSurfaceData_kStrokeJoinIndex];
        jint linecap = javaGraphicsStates[sun_java2d_OSXSurfaceData_kStrokeCapIndex];
        CGFloat miterlimit = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kStrokeLimitIndex];
        jobject dasharray = ((*env)->GetObjectArrayElement(env, qsdo->javaGraphicsStatesObjects, sun_java2d_OSXSurfaceData_kStrokeDashArrayIndex));
        CGFloat dashphase = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kStrokeDashPhaseIndex];

        if (linewidth == 0.0f)
        {
            linewidth = (CGFloat)-109.05473e+14; // Don't ask !
        }
        CGContextSetLineWidth(cgRef, linewidth);

        CGLineCap cap;
        switch (linecap)
        {
            case java_awt_BasicStroke_CAP_BUTT:
                qsdo->graphicsStateInfo.simpleStroke = NO;
                cap = kCGLineCapButt;
                break;
            case java_awt_BasicStroke_CAP_ROUND:
                qsdo->graphicsStateInfo.simpleStroke = NO;
                cap = kCGLineCapRound;
                break;
            case java_awt_BasicStroke_CAP_SQUARE:
            default:
                cap = kCGLineCapSquare;
                break;
        }
        CGContextSetLineCap(cgRef, cap);

        CGLineJoin join;
        switch (linejoin)
        {
            case java_awt_BasicStroke_JOIN_ROUND:
                qsdo->graphicsStateInfo.simpleStroke = NO;
                join = kCGLineJoinRound;
                break;
            case java_awt_BasicStroke_JOIN_BEVEL:
                qsdo->graphicsStateInfo.simpleStroke = NO;
                join = kCGLineJoinBevel;
                break;
            case java_awt_BasicStroke_JOIN_MITER:
            default:
                join = kCGLineJoinMiter;
                break;
        }
        CGContextSetLineJoin(cgRef, join);
        CGContextSetMiterLimit(cgRef, miterlimit);

        if (dasharray != NULL)
        {
            qsdo->graphicsStateInfo.simpleStroke = NO;
            jint length = (*env)->GetArrayLength(env, dasharray);
            jfloat* jdashes = (jfloat*)(*env)->GetPrimitiveArrayCritical(env, dasharray, NULL);
            if (jdashes == NULL) {
                CGContextSetLineDash(cgRef, 0, NULL, 0);
                return;
            }
            CGFloat* dashes = (CGFloat*)malloc(sizeof(CGFloat)*length);
            if (dashes != NULL)
            {
                jint i;
                for (i=0; i<length; i++)
                {
                    dashes[i] = (CGFloat)jdashes[i];
                }
            }
            else
            {
                dashphase = 0;
                length = 0;
            }
            CGContextSetLineDash(cgRef, dashphase, dashes, length);
            if (dashes != NULL)
            {
                free(dashes);
            }
            (*env)->ReleasePrimitiveArrayCritical(env, dasharray, jdashes, 0);
        }
        else
        {
            CGContextSetLineDash(cgRef, 0, NULL, 0);
        }
    }

    BOOL cocoaPaint = (javaGraphicsStates[sun_java2d_OSXSurfaceData_kColorStateIndex] == sun_java2d_OSXSurfaceData_kColorSystem);
    BOOL complexPaint = (javaGraphicsStates[sun_java2d_OSXSurfaceData_kColorStateIndex] == sun_java2d_OSXSurfaceData_kColorGradient) ||
                        (javaGraphicsStates[sun_java2d_OSXSurfaceData_kColorStateIndex] == sun_java2d_OSXSurfaceData_kColorTexture);
    if ((everyThingChanged == YES) || (paintChanged == YES) || (cocoaPaint == YES) || (complexPaint == YES))
    {
        // rdar://problem/5214320
        // Gradient fills of Java GeneralPath don't respect the even odd winding rule (quartz pipeline).
        // Notice the side effect of the stmt after this if-block.
        if (renderType == SD_EOFill) {
            qsdo->isEvenOddFill = YES;
        }

        renderType = SetUpPaint(env, qsdo, renderType);
    }

    qsdo->renderType = renderType;
}

void setupGradient(JNIEnv *env, QuartzSDOps* qsdo, jfloat* javaFloatGraphicsStates)
{
    static const CGFloat kColorConversionMultiplier = 1.0f/255.0f;
    qsdo->gradientInfo = (StateGradientInfo*)malloc(sizeof(StateGradientInfo));
    if (qsdo->gradientInfo == NULL)
    {
        JNI_COCOA_THROW_OOME(env, "Failed to malloc memory for gradient paint");
    }

    qsdo->graphicsStateInfo.simpleStroke = NO;
    qsdo->graphicsStateInfo.simpleColor = NO;

    qsdo->gradientInfo->start.x    = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kColorx1Index];
    qsdo->gradientInfo->start.y    = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kColory1Index];
    qsdo->gradientInfo->end.x    = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kColorx2Index];
    qsdo->gradientInfo->end.y    = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kColory2Index];

    jobject colorArray  = ((*env)->GetObjectArrayElement(env, qsdo->javaGraphicsStatesObjects, sun_java2d_OSXSurfaceData_kColorArrayIndex));
    if (colorArray != NULL)
    {
        jint length = (*env)->GetArrayLength(env, colorArray);

        jint* jcolorData = (jint*)(*env)->GetPrimitiveArrayCritical(env, colorArray, NULL);
        qsdo->gradientInfo->colordata = (CGFloat*)malloc(sizeof(CGFloat)*4*length);
        memset(qsdo->gradientInfo->colordata, 0, sizeof(CGFloat)*4*length);
        if (jcolorData != NULL)
        {
            int i;
            for (i=0; i<length; i++)
            {
                qsdo->gradientInfo->colordata[i*4] = ((jcolorData[i]>>16)&0xff)*kColorConversionMultiplier;

                qsdo->gradientInfo->colordata[i*4+1] = ((jcolorData[i]>>8)&0xff)*kColorConversionMultiplier;

                qsdo->gradientInfo->colordata[i*4+2] = ((jcolorData[i]>>0)&0xff)*kColorConversionMultiplier;

                qsdo->gradientInfo->colordata[i*4+3] = ((jcolorData[i]>>24)&0xff)*kColorConversionMultiplier;
            }
        }
        (*env)->ReleasePrimitiveArrayCritical(env, colorArray, jcolorData, 0);
    }
    jobject fractionsArray  = ((*env)->GetObjectArrayElement(env, qsdo->javaGraphicsStatesObjects, sun_java2d_OSXSurfaceData_kFractionsArrayIndex));
    if (fractionsArray != NULL)
    {
        jint length = (*env)->GetArrayLength(env, fractionsArray);
        qsdo->gradientInfo->fractionsLength = length;

        jfloat* jfractionsData = (jfloat*)(*env)->GetPrimitiveArrayCritical(env, fractionsArray, NULL);
        if (jfractionsData != NULL)
        {
            int i;
            qsdo->gradientInfo->fractionsdata = (CGFloat *)malloc(sizeof(CGFloat) *length);
            memset(qsdo->gradientInfo->fractionsdata, 0, sizeof(CGFloat)*length);
            for (i=0; i<length; i++)
            {
                qsdo->gradientInfo->fractionsdata[i] = jfractionsData[i];
            }
            (*env)->ReleasePrimitiveArrayCritical(env, fractionsArray, jfractionsData, 0);
        }
    }
}

SDRenderType SetUpPaint(JNIEnv *env, QuartzSDOps *qsdo, SDRenderType renderType)
{
    CGContextRef cgRef = qsdo->cgRef;

    jint *javaGraphicsStates = qsdo->javaGraphicsStates;
    jfloat *javaFloatGraphicsStates = (jfloat*)(qsdo->javaGraphicsStates);

    static const CGFloat kColorConversionMultiplier = 1.0f/255.0f;
    jint colorState = javaGraphicsStates[sun_java2d_OSXSurfaceData_kColorStateIndex];

    switch (colorState)
    {
        case sun_java2d_OSXSurfaceData_kColorSimple:
        {
            if (qsdo->graphicsStateInfo.simpleColor == NO)
            {
                setDefaultColorSpace(cgRef);
            }
            qsdo->graphicsStateInfo.simpleColor = YES;

            // sets the color on the CGContextRef (CGContextSetStrokeColorWithColor/CGContextSetFillColorWithColor)
            setCachedColor(qsdo, javaGraphicsStates[sun_java2d_OSXSurfaceData_kColorRGBValueIndex]);

            break;
        }
        case sun_java2d_OSXSurfaceData_kColorSystem:
        {
            qsdo->graphicsStateInfo.simpleStroke = NO;
            // All our custom Colors are NSPatternColorSpace so we are complex colors!
            qsdo->graphicsStateInfo.simpleColor = NO;

            NSColor *color = nil;
            /* TODO:BG
            {
                color = getColor(javaGraphicsStates[sun_java2d_OSXSurfaceData_kColorIndexValueIndex]);
            }
            */
            [color set];
            break;
        }
        case sun_java2d_OSXSurfaceData_kColorGradient:
        {
            qsdo->shadingInfo = (StateShadingInfo*)malloc(sizeof(StateShadingInfo));
            if (qsdo->shadingInfo == NULL)
            {
                JNI_COCOA_THROW_OOME(env, "Failed to malloc memory for gradient paint");
            }

            qsdo->graphicsStateInfo.simpleStroke = NO;
            qsdo->graphicsStateInfo.simpleColor = NO;

            renderType = SD_Shade;

            qsdo->shadingInfo->start.x    = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kColorx1Index];
            qsdo->shadingInfo->start.y    = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kColory1Index];
            qsdo->shadingInfo->end.x    = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kColorx2Index];
            qsdo->shadingInfo->end.y    = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kColory2Index];
            jint c1 = javaGraphicsStates[sun_java2d_OSXSurfaceData_kColorRGBValue1Index];
            qsdo->shadingInfo->colors[0] = ((c1>>16)&0xff)*kColorConversionMultiplier;
            qsdo->shadingInfo->colors[1] = ((c1>>8)&0xff)*kColorConversionMultiplier;
            qsdo->shadingInfo->colors[2] = ((c1>>0)&0xff)*kColorConversionMultiplier;
            qsdo->shadingInfo->colors[3] = ((c1>>24)&0xff)*kColorConversionMultiplier;
            jint c2 = javaGraphicsStates[sun_java2d_OSXSurfaceData_kColorRGBValue2Index];
            qsdo->shadingInfo->colors[4] = ((c2>>16)&0xff)*kColorConversionMultiplier;
            qsdo->shadingInfo->colors[5] = ((c2>>8)&0xff)*kColorConversionMultiplier;
            qsdo->shadingInfo->colors[6] = ((c2>>0)&0xff)*kColorConversionMultiplier;
            qsdo->shadingInfo->colors[7] = ((c2>>24)&0xff)*kColorConversionMultiplier;
            qsdo->shadingInfo->cyclic    = (javaGraphicsStates[sun_java2d_OSXSurfaceData_kColorIsCyclicIndex] == sun_java2d_OSXSurfaceData_kColorCyclic);

            break;
        }
        case sun_java2d_OSXSurfaceData_kColorLinearGradient:
        {
            renderType = SD_LinearGradient;
            setupGradient(env, qsdo, javaFloatGraphicsStates);
            break;
        }

        case sun_java2d_OSXSurfaceData_kColorRadialGradient:
        {
            renderType = SD_RadialGradient;
            setupGradient(env, qsdo, javaFloatGraphicsStates);
            qsdo->gradientInfo->radius = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kRadiusIndex];
            break;
        }

        case sun_java2d_OSXSurfaceData_kColorTexture:
        {
            qsdo->patternInfo = (StatePatternInfo*)malloc(sizeof(StatePatternInfo));
            if (qsdo->patternInfo == NULL)
            {
                JNI_COCOA_THROW_OOME(env, "Failed to malloc memory for texture paint");
            }

            qsdo->graphicsStateInfo.simpleStroke = NO;
            qsdo->graphicsStateInfo.simpleColor = NO;

            renderType = SD_Pattern;

            qsdo->patternInfo->tx        = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kColortxIndex];
            qsdo->patternInfo->ty        = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kColortyIndex];
            qsdo->patternInfo->sx        = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kColorsxIndex];
            if (qsdo->patternInfo->sx == 0.0f)
            {
                return SD_Fill; // 0 is an invalid value, fill argb rect
            }
            qsdo->patternInfo->sy        = javaFloatGraphicsStates[sun_java2d_OSXSurfaceData_kColorsyIndex];
            if (qsdo->patternInfo->sy == 0.0f)
            {
                return SD_Fill; // 0 is an invalid value, fill argb rect
            }
            qsdo->patternInfo->width    = javaGraphicsStates[sun_java2d_OSXSurfaceData_kColorWidthIndex];
            qsdo->patternInfo->height    = javaGraphicsStates[sun_java2d_OSXSurfaceData_kColorHeightIndex];

            jobject sData = ((*env)->GetObjectArrayElement(env, qsdo->javaGraphicsStatesObjects, sun_java2d_OSXSurfaceData_kTextureImageIndex)); //deleted next time through SetUpPaint and not before ( radr://3913190 )
            if (sData != NULL)
            {
                qsdo->patternInfo->sdata = (*env)->NewGlobalRef(env, sData);
                if (qsdo->patternInfo->sdata == NULL)
                {
                    renderType = SD_Fill;
                }
            }
            else
            {
                renderType = SD_Fill;
            }

            break;
        }
    }

    return renderType;
}

#pragma mark
#pragma mark --- Shape Drawing Code ---

SDRenderType DoShapeUsingCG(CGContextRef cgRef, jint *types, jfloat *coords, jint numtypes, BOOL fill, CGFloat offsetX, CGFloat offsetY)
{
//fprintf(stderr, "DoShapeUsingCG fill=%d\n", (jint)fill);
    SDRenderType renderType = SD_Nothing;

    if (gAdjustForJavaDrawing != YES)
    {
        offsetX = 0.0f;
        offsetY = 0.0f;
    }

    if (fill == YES)
    {
        renderType = SD_Fill;
    }
    else
    {
        renderType = SD_Stroke;
    }

    if (numtypes > 0)
    {
        BOOL needNewSubpath = NO;

        CGContextBeginPath(cgRef); // create new path
//fprintf(stderr, "    CGContextBeginPath\n");

        jint index = 0;
        CGFloat mx = 0.0f, my = 0.0f, x1 = 0.0f, y1 = 0.0f, cpx1 = 0.0f, cpy1 = 0.0f, cpx2 = 0.0f, cpy2 = 0.0f;
        jint i;

        mx = (CGFloat)coords[index++] + offsetX;
        my = (CGFloat)coords[index++] + offsetY;
        CGContextMoveToPoint(cgRef, mx, my);

        for (i=1; i<numtypes; i++)
        {
            jint pathType = types[i];

            if (needNewSubpath == YES)
            {
                needNewSubpath = NO;
                switch (pathType)
                {
                    case java_awt_geom_PathIterator_SEG_LINETO:
                    case java_awt_geom_PathIterator_SEG_QUADTO:
                    case java_awt_geom_PathIterator_SEG_CUBICTO:
//fprintf(stderr, "    forced CGContextMoveToPoint (%f, %f)\n", mx, my);
                        CGContextMoveToPoint(cgRef, mx, my); // force new subpath
                        break;
                }
            }

            switch (pathType)
            {
                case java_awt_geom_PathIterator_SEG_MOVETO:
                    mx = x1 = (CGFloat)coords[index++] + offsetX;
                    my = y1 = (CGFloat)coords[index++] + offsetY;
                    CGContextMoveToPoint(cgRef, x1, y1); // start new subpath
//fprintf(stderr, "    SEG_MOVETO CGContextMoveToPoint (%f, %f)\n", x1, y1);
                    break;
                case java_awt_geom_PathIterator_SEG_LINETO:
                    x1 = (CGFloat)coords[index++] + offsetX;
                    y1 = (CGFloat)coords[index++] + offsetY;
                    CGContextAddLineToPoint(cgRef, x1, y1);
//fprintf(stderr, "    SEG_LINETO CGContextAddLineToPoint (%f, %f)\n", x1, y1);
                    break;
                case java_awt_geom_PathIterator_SEG_QUADTO:
                    cpx1 = (CGFloat)coords[index++] + offsetX;
                    cpy1 = (CGFloat)coords[index++] + offsetY;
                    x1 = (CGFloat)coords[index++] + offsetX;
                    y1 = (CGFloat)coords[index++]+ offsetY;
                    CGContextAddQuadCurveToPoint(cgRef, cpx1, cpy1, x1, y1);
//fprintf(stderr, "    SEG_QUADTO CGContextAddQuadCurveToPoint (%f, %f), (%f, %f)\n", cpx1, cpy1, x1, y1);
                    break;
                case java_awt_geom_PathIterator_SEG_CUBICTO:
                    cpx1 = (CGFloat)coords[index++] + offsetX;
                    cpy1 = (CGFloat)coords[index++] + offsetY;
                    cpx2 = (CGFloat)coords[index++] + offsetX;
                    cpy2 = (CGFloat)coords[index++] + offsetY;
                    x1 = (CGFloat)coords[index++] + offsetX;
                    y1 = (CGFloat)coords[index++] + offsetY;
                    CGContextAddCurveToPoint(cgRef, cpx1, cpy1, cpx2, cpy2, x1, y1);
//fprintf(stderr, "    SEG_CUBICTO CGContextAddCurveToPoint (%f, %f), (%f, %f), (%f, %f)\n", cpx1, cpy1, cpx2, cpy2, x1, y1);
                    break;
                case java_awt_geom_PathIterator_SEG_CLOSE:
                    CGContextClosePath(cgRef); // close subpath
                    needNewSubpath = YES;
//fprintf(stderr, "    SEG_CLOSE CGContextClosePath\n");
                    break;
            }
        }
    }

    return renderType;
}

void CompleteCGContext(JNIEnv *env, QuartzSDOps *qsdo)
{
PRINT(" CompleteCGContext")
    switch (qsdo->renderType)
    {
        case SD_Nothing:
            break;

        case SD_Stroke:
            if (CGContextIsPathEmpty(qsdo->cgRef) == 0)
            {
                CGContextStrokePath(qsdo->cgRef);
            }
            break;

        case SD_Fill:
            if (CGContextIsPathEmpty(qsdo->cgRef) == 0)
            {
                CGContextFillPath(qsdo->cgRef);
            }
            break;

        case SD_Shade:
            if (CGContextIsPathEmpty(qsdo->cgRef) == 0)
            {
                contextGradientPath(qsdo);
            }
            break;

        case SD_LinearGradient:
            if (CGContextIsPathEmpty(qsdo->cgRef) == 0)
            {
                contextQuartzLinearGradientPath(qsdo);
            }
            break;

        case SD_RadialGradient:
            if (CGContextIsPathEmpty(qsdo->cgRef) == 0)
            {
                contextQuartzRadialGradientPath(qsdo);
            }
            break;

        case SD_Pattern:
            if (CGContextIsPathEmpty(qsdo->cgRef) == 0)
            {
                contextTexturePath(env, qsdo);
            }
            break;

        case SD_EOFill:
            if (CGContextIsPathEmpty(qsdo->cgRef) == 0)
            {
                CGContextEOFillPath(qsdo->cgRef);
            }
            break;

        case SD_Image:
            break;

        case SD_Text:
            break;

        case SD_CopyArea:
            break;

        case SD_Queue:
            break;

        case SD_External:
            break;
    }

    if (qsdo->shadingInfo != NULL) {
        gradientPaintReleaseFunction(qsdo->shadingInfo);
        qsdo->shadingInfo = NULL;
    }
    if (qsdo->gradientInfo != NULL) {
        gradientPaintReleaseFunction(qsdo->gradientInfo);
        qsdo->gradientInfo = NULL;
    }
}
