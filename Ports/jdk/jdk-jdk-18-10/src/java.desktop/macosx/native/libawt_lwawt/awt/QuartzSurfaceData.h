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

#import "SurfaceData.h"
#import "BufImgSurfaceData.h"
#import "AWTFont.h"
#import <Cocoa/Cocoa.h>
#import "JNIUtilities.h"

// these flags are not defined on Tiger on PPC, so we need to make them a no-op
#if !defined(kCGBitmapByteOrder32Host)
#define kCGBitmapByteOrder32Host 0
#endif
#if !defined(kCGBitmapByteOrder16Host)
#define kCGBitmapByteOrder16Host 0
#endif

// NOTE : Modify the printSurfaceDataDiagnostics API if you change this enum
enum SDRenderType
{
    SD_Nothing,
    SD_Stroke,
    SD_Fill,
    SD_EOFill,
    SD_Shade,
    SD_LinearGradient,
    SD_RadialGradient,
    SD_Pattern,
    SD_Image,
    SD_Text,
    SD_CopyArea,
    SD_Queue,
    SD_External
};
typedef enum SDRenderType SDRenderType;

struct _stateShadingInfo
{
    CGPoint    start;
    CGPoint    end;
    CGFloat    colors[8];
    BOOL    cyclic;
    CGFloat    length; // of the total segment (used by the cyclic gradient)
    CGFloat    period; // of the cycle (used by the cyclic gradient)
    CGFloat    offset; // of the cycle from the start (used by the cyclic gradient)
};
typedef struct _stateShadingInfo StateShadingInfo;

struct _stateGradientInfo
{
    CGPoint    start;
    CGPoint    end;
    CGFloat  radius;
    CGFloat* colordata;
    CGFloat* fractionsdata;
    jint     fractionsLength;
};
typedef struct _stateGradientInfo StateGradientInfo;

struct _statePatternInfo
{
    CGFloat    tx;
    CGFloat    ty;
    CGFloat    sx;
    CGFloat    sy;
    jint    width;
    jint    height;
    jobject    sdata;
};
typedef struct _statePatternInfo StatePatternInfo;

struct _stateGraphicsInfo
{
    BOOL                adjustedLineWidth;
    BOOL                adjustedAntialias;
    BOOL                antialiased;
    jint                interpolation;
    BOOL                simpleColor;
    BOOL                simpleStroke;
    CGAffineTransform    ctm;
    CGFloat                offsetX;
    CGFloat                offsetY;
    struct CGPoint*        batchedLines;
    UInt32                batchedLinesCount;
};
typedef struct _stateGraphicsInfo StateGraphicsInfo;

typedef struct _QuartzSDOps QuartzSDOps;
typedef void BeginContextFunc(JNIEnv *env, QuartzSDOps *qsdo, SDRenderType renderType);
typedef void FinishContextFunc(JNIEnv *env, QuartzSDOps *qsdo);
struct _QuartzSDOps
{
    BufImgSDOps                sdo; // must be the first entry!

    BeginContextFunc*        BeginSurface;        // used to set graphics states (clip, color, stroke, etc...)
    FinishContextFunc*        FinishSurface;        // used to finish drawing primitives
    BOOL                    newContext;
    CGContextRef            cgRef;

    jint*                    javaGraphicsStates;
    jobject                    javaGraphicsStatesObjects;

    SDRenderType            renderType;

    // rdar://problem/5214320
    // Gradient/Texture fills of Java GeneralPath don't respect the even odd winding rule (quartz pipeline).
    BOOL                    isEvenOddFill;        // Tracks whether the original render type passed into
                                                // SetUpCGContext(...) is SD_EOFILL.
                                                // The reason for this field is because SetUpCGContext(...) can
                                                // change the render type after calling SetUpPaint(...), and right
                                                // after that, the possibly new render type is then assigned into
                                                // qsdo->renderType.  Sigh!!!
                                                // This field is potentially used within CompleteCGContext(...) or
                                                // its callees.

    StateShadingInfo*        shadingInfo;        // tracks shading and its parameters
    StateGradientInfo*       gradientInfo;       // tracks gradient and its parameters
    StatePatternInfo*        patternInfo;        // tracks pattern and its parameters
    StateGraphicsInfo        graphicsStateInfo;    // tracks other graphics state

    BOOL  syncContentsToLayer;    // should changed pixels be synced to a CALayer
    CGRect updateRect;     // used by the layer synchronization code to track update rects.
};

void SetUpCGContext(JNIEnv *env, QuartzSDOps *qsdo, SDRenderType renderType);
SDRenderType DoShapeUsingCG(CGContextRef cgRef, jint *types, jfloat *coords, jint numtypes, BOOL fill, CGFloat offsetX, CGFloat offsetY);
SDRenderType SetUpPaint(JNIEnv *env, QuartzSDOps *qsdo, SDRenderType renderType);
void CompleteCGContext(JNIEnv *env, QuartzSDOps *qsdo);

NSColor* ByteParametersToNSColor(JNIEnv* env, jint *javaGraphicsStates, NSColor* defColor);

#define JNI_COCOA_RENDERER_EXIT(env) \
 JNI_COCOA_EXIT_WITH_ACTION(env, qsdo->FinishSurface(env, qsdo))
