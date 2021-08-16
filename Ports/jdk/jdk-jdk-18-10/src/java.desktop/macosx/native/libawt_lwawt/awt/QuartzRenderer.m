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

#import "java_awt_image_BufferedImage.h"
#import "java_awt_geom_PathIterator.h"
#import "sun_java2d_OSXSurfaceData.h"

#import <stdio.h>

#import "ImageSurfaceData.h"


//#define DEBUG 1
#if defined DEBUG
    #define QUARTZ_RENDERER_INLINE
    #define PRINT(msg) {fprintf(stderr, "%s\n", msg);fflush(stderr);}
#else
    #define QUARTZ_RENDERER_INLINE static inline
    #define PRINT(msg) {}
#endif

// Copied the following from Math.java
#define PI 3.14159265358979323846f

#define BATCHED_POINTS_SIZE 1024

// same value as defined in Sun's own code
#define XOR_ALPHA_CUTOFF 128


static CGFloat gRoundRectCtrlpts[10][12] =
{
    {0.0f, 0.0f, 0.0f, 0.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
    {0.0f, 0.0f, 1.0f, -0.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
    {0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.5f, 1.0f, 0.0f},
    {1.0f, -0.5f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
    {1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, -0.5f},
    {1.0f, 0.0f, 0.0f, 0.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
    {1.0f, 0.0f, 0.0f, 0.0f,  1.0f, 0.0f, 0.0f, 0.0f, 1.0f, -0.5f, 0.0f, 0.0f},
    {0.0f, 0.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
    {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.5f},
    {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f},
};

CG_EXTERN CGRect CGRectApplyAffineTransform(CGRect rect, CGAffineTransform t);


CGRect sanitizedRect(CGFloat x1, CGFloat y1, CGFloat x2, CGFloat y2) {
    CGFloat temp;
    if (x1 > x2) {
        temp = x2;
        x2 = x1;
        x1 = temp;
    }
    if (y1 > y2) {
        temp = y2;
        y2 = y1;
        y1 = temp;
    }
    return CGRectMake(x1, y1, x2-x1, y2-y1);
}

QUARTZ_RENDERER_INLINE SDRenderType doLineUsingCG(CGContextRef cgRef, CGFloat x1, CGFloat y1, CGFloat x2, CGFloat y2, BOOL simple, CGFloat offsetX, CGFloat offsetY)
{
//fprintf(stderr, "doLine start=(%f, %f), end=(%f, %f), linewidth:%f, offsetX:%f, offsetY:%f\n", x1, y1, x2, y2, CGContextGetLineWidth(cgRef), offsetX, offsetY);
    SDRenderType renderType = SD_Nothing;

    if (simple == YES)
    {
        struct CGPoint oneLinePoints[2];

        oneLinePoints[0] = CGPointMake(x1+offsetX, y1+offsetY);
        oneLinePoints[1] = CGPointMake(x2+offsetX, y2+offsetY);

        CGContextStrokeLineSegments(cgRef, oneLinePoints, 2);
        renderType = SD_Nothing;
    }
    else
    {
        CGContextMoveToPoint(cgRef, x1+offsetX, y1+offsetY);
        CGContextAddLineToPoint(cgRef, x2+offsetX, y2+offsetY);
        renderType = SD_Stroke;
    }

    return renderType;
}
QUARTZ_RENDERER_INLINE SDRenderType doLine(QuartzSDOps *qsdo, CGFloat x1, CGFloat y1, CGFloat x2, CGFloat y2)
{
PRINT(" doLine")
    if (YES)
    {
        return doLineUsingCG(qsdo->cgRef, x1, y1, x2, y2,
                                qsdo->graphicsStateInfo.simpleStroke, qsdo->graphicsStateInfo.offsetX, qsdo->graphicsStateInfo.offsetY);
    }
    // here we can add other implementations (ex. using QuickDraw, OpenGL, etc.)
}


QUARTZ_RENDERER_INLINE SDRenderType doRectUsingCG(CGContextRef cgRef, CGFloat x, CGFloat y, CGFloat w, CGFloat h, BOOL fill, BOOL simple, CGFloat offsetX, CGFloat offsetY)
{
//fprintf(stderr, "doRect point=(%f, %f), size=(%f, %f), offsets=(%f, %f) fill=%d simple=%d\n", x, y, w, h, offsetX, offsetY, fill, simple);
//CGRect clip = CGContextGetClipBoundingBox(cgRef);
//fprintf(stderr, "    clip: ((%f, %f), (%f, %f))\n", clip.origin.x, clip.origin.y, clip.size.width, clip.size.height);
//CGAffineTransform ctm = CGContextGetCTM(cgRef);
//fprintf(stderr, "    ctm: (%f, %f, %f, %f, %f, %f)\n", ctm.a, ctm.b, ctm.c, ctm.d, ctm.tx, ctm.ty);
    SDRenderType renderType = SD_Nothing;

    if (fill == YES)
    {
        if (simple == YES)
        {
            CGContextFillRect(cgRef, CGRectMake(x, y, w, h));
            renderType = SD_Nothing;
        }
        else
        {
            CGContextAddRect(cgRef, CGRectMake(x, y, w, h));
            renderType = SD_Fill;
        }
    }
    else
    {
        if (simple == YES)
        {
            CGContextStrokeRect(cgRef, CGRectMake(x+offsetX, y+offsetY, w, h));
            renderType = SD_Nothing;
        }
        else
        {
            CGContextAddRect(cgRef, CGRectMake(x+offsetX, y+offsetY, w, h));
            renderType = SD_Stroke;
        }
    }

    return renderType;
}
QUARTZ_RENDERER_INLINE SDRenderType doRect(QuartzSDOps *qsdo, CGFloat x, CGFloat y, CGFloat w, CGFloat h, BOOL fill)
{
PRINT(" doRect")
    if (YES)
    {
        return doRectUsingCG(qsdo->cgRef, x, y, w, h, fill,
                                qsdo->graphicsStateInfo.simpleStroke, qsdo->graphicsStateInfo.offsetX, qsdo->graphicsStateInfo.offsetY);
    }
    // here we can add other implementations (ex. using QuickDraw, OpenGL, etc.)
}

// from RoundRectIterator.java
QUARTZ_RENDERER_INLINE SDRenderType doRoundRectUsingCG(CGContextRef cgRef, CGFloat x, CGFloat y, CGFloat w, CGFloat h, CGFloat arcWidth, CGFloat arcHeight, BOOL fill, CGFloat offsetX, CGFloat offsetY)
{
    SDRenderType renderType = SD_Nothing;

    if (fill == YES)
    {
        renderType = SD_Fill;
    }
    else
    {
        renderType = SD_Stroke;
    }

    // radr://3593731 RoundRects with corner width/height of 0 don't draw
    arcWidth = (arcWidth > 0.0f) ? arcWidth : 0.0f;
    arcHeight = (arcHeight > 0.0f) ? arcHeight : 0.0f;

    CGFloat aw = (w < arcWidth) ? w : arcWidth;
    CGFloat ah = (h < arcHeight) ? h : arcHeight;

    CGFloat *ctrls, p1, q1, p2, q2, p3, q3;
    ctrls = gRoundRectCtrlpts[0];
    p1 = (x + ctrls[0] * w + ctrls[1] * aw);
    q1 = (y + ctrls[2] * h + ctrls[3] * ah);
    CGContextMoveToPoint(cgRef, p1+offsetX, q1+offsetY);

    ctrls = gRoundRectCtrlpts[1];
    p1 = (x + ctrls[0] * w + ctrls[1] * aw);
    q1 = (y + ctrls[2] * h + ctrls[3] * ah);
    CGContextAddLineToPoint(cgRef, p1+offsetX, q1+offsetY);

    ctrls = gRoundRectCtrlpts[2];
    p1 = (x + ctrls[0] * w + ctrls[1] * aw);
    q1 = (y + ctrls[2] * h + ctrls[3] * ah);
    p2 = (x + ctrls[4] * w + ctrls[5] * aw);
    q2 = (y + ctrls[6] * h + ctrls[7] * ah);
    p3 = (x + ctrls[8] * w + ctrls[9] * aw);
    q3 = (y + ctrls[10] * h + ctrls[11] * ah);
    CGContextAddCurveToPoint(cgRef, p1+offsetX, q1+offsetY, p2+offsetX, q2+offsetY, p3+offsetX, q3+offsetY);

    ctrls = gRoundRectCtrlpts[3];
    p1 = (x + ctrls[0] * w + ctrls[1] * aw);
    q1 = (y + ctrls[2] * h + ctrls[3] * ah);
    CGContextAddLineToPoint(cgRef, p1+offsetX, q1+offsetY);

    ctrls = gRoundRectCtrlpts[4];
    p1 = (x + ctrls[0] * w + ctrls[1] * aw);
    q1 = (y + ctrls[2] * h + ctrls[3] * ah);
    p2 = (x + ctrls[4] * w + ctrls[5] * aw);
    q2 = (y + ctrls[6] * h + ctrls[7] * ah);
    p3 = (x + ctrls[8] * w + ctrls[9] * aw);
    q3 = (y + ctrls[10] * h + ctrls[11] * ah);
    CGContextAddCurveToPoint(cgRef, p1+offsetX, q1+offsetY, p2+offsetX, q2+offsetY, p3+offsetX, q3+offsetY);

    ctrls = gRoundRectCtrlpts[5];
    p1 = (x + ctrls[0] * w + ctrls[1] * aw);
    q1 = (y + ctrls[2] * h + ctrls[3] * ah);
    CGContextAddLineToPoint(cgRef, p1+offsetX, q1+offsetY);

    ctrls = gRoundRectCtrlpts[6];
    p1 = (x + ctrls[0] * w + ctrls[1] * aw);
    q1 = (y + ctrls[2] * h + ctrls[3] * ah);
    p2 = (x + ctrls[4] * w + ctrls[5] * aw);
    q2 = (y + ctrls[6] * h + ctrls[7] * ah);
    p3 = (x + ctrls[8] * w + ctrls[9] * aw);
    q3 = (y + ctrls[10] * h + ctrls[11] * ah);
    CGContextAddCurveToPoint(cgRef, p1+offsetX, q1+offsetY, p2+offsetX, q2+offsetY, p3+offsetX, q3+offsetY);

    ctrls = gRoundRectCtrlpts[7];
    p1 = (x + ctrls[0] * w + ctrls[1] * aw);
    q1 = (y + ctrls[2] * h + ctrls[3] * ah);
    CGContextAddLineToPoint(cgRef, p1+offsetX, q1+offsetY);

    ctrls = gRoundRectCtrlpts[8];
    p1 = (x + ctrls[0] * w + ctrls[1] * aw);
    q1 = (y + ctrls[2] * h + ctrls[3] * ah);
    p2 = (x + ctrls[4] * w + ctrls[5] * aw);
    q2 = (y + ctrls[6] * h + ctrls[7] * ah);
    p3 = (x + ctrls[8] * w + ctrls[9] * aw);
    q3 = (y + ctrls[10] * h + ctrls[11] * ah);
    CGContextAddCurveToPoint(cgRef, p1+offsetX, q1+offsetY, p2+offsetX, q2+offsetY, p3+offsetX, q3+offsetY);

    CGContextClosePath(cgRef);

    return renderType;
}

QUARTZ_RENDERER_INLINE SDRenderType doRoundRect(QuartzSDOps *qsdo, CGFloat x, CGFloat y, CGFloat w, CGFloat h, CGFloat arcWidth, CGFloat arcHeight, BOOL fill)
{
PRINT(" doRoundRect")
    if (YES)
    {
        return doRoundRectUsingCG(qsdo->cgRef, x, y, w, h, arcWidth, arcHeight, fill,
                                    qsdo->graphicsStateInfo.offsetX, qsdo->graphicsStateInfo.offsetY);
    }
    // here we can add other implementations (ex. using QuickDraw, OpenGL, etc.)
}

// from EllipseIterator.java
QUARTZ_RENDERER_INLINE SDRenderType doOvalUsingCG(CGContextRef cgRef, CGFloat x, CGFloat y, CGFloat w, CGFloat h, BOOL fill, BOOL simple, CGFloat offsetX, CGFloat offsetY)
{
    SDRenderType renderType = SD_Nothing;

    if (simple == YES)
    {
        if (fill == YES)
        {
            CGContextFillEllipseInRect(cgRef, CGRectMake(x+offsetX, y+offsetY, w, h));
        }
        else
        {
            CGContextStrokeEllipseInRect(cgRef, CGRectMake(x+offsetX, y+offsetY, w, h));
        }
    }
    else
    {
        if (fill == YES)
        {
            renderType = SD_Fill;
        }
        else
        {
            renderType = SD_Stroke;
        }

        CGContextAddEllipseInRect(cgRef, CGRectMake(x+offsetX, y+offsetY, w, h));
    }

    return renderType;
}
QUARTZ_RENDERER_INLINE SDRenderType doOval(QuartzSDOps *qsdo, CGFloat x, CGFloat y, CGFloat w, CGFloat h, BOOL fill)
{
PRINT(" doOval")
    if (YES)
    {
        return doOvalUsingCG(qsdo->cgRef, x, y, w, h, fill,
                                qsdo->graphicsStateInfo.simpleStroke, qsdo->graphicsStateInfo.offsetX, qsdo->graphicsStateInfo.offsetY);
    }
    // here we can add other implementations (ex. using QuickDraw, OpenGL, etc.)
}

// from ArcIterator.java
QUARTZ_RENDERER_INLINE CGFloat btan(CGFloat increment)
{
    increment /= 2.0f;
    CGFloat a = 1.0f - cos(increment);
    CGFloat b = tan(increment);
    CGFloat c = sqrt(1.0f + b * b) - 1.0f + a;

    return 4.0f / 3.0f * a * b / c;
}
QUARTZ_RENDERER_INLINE SDRenderType doArcUsingCG(CGContextRef cgRef, CGFloat x, CGFloat y, CGFloat w, CGFloat h, CGFloat angleStart, CGFloat angleExtent, jint arcType, BOOL fill, CGFloat offsetX, CGFloat offsetY)
{
//fprintf(stderr, "doArc\n");
    SDRenderType renderType = SD_Nothing;

    if (fill == YES)
    {
        renderType = SD_Fill;
    }
    else
    {
        renderType = SD_Stroke;
    }

    CGFloat angStRad, angExtDeg;
    jint arcSegs;
    jint lineSegs;
    jint index = 1;

    w = w / 2.0f;
    h = h / 2.0f;
    x = x + w;
    y = y + h;
    angStRad = -(angleStart / 180.0f * PI);
    angExtDeg = -angleExtent;
    CGFloat ext = (angExtDeg>0) ? angExtDeg : -angExtDeg;
    if (ext >= 360.0f)
    {
        arcSegs = 4;
    }
    else
    {
        arcSegs = (jint)ceil(ext/90.0f);
    }
    switch (arcType)
    {
        case 0:
            lineSegs = 0;
            break;
        case 1:
            lineSegs = 1;
            break;
        case 2:
            lineSegs = 2;
            break;
    }
    if (w < 0 || h < 0)
    {
        arcSegs = lineSegs = -1;
    }

    CGFloat angle = angStRad;
    CGContextMoveToPoint(cgRef, (x + cos(angle) * w)+offsetX, (y + sin(angle) * h)+offsetY);

    CGFloat increment = angExtDeg;
    if (increment > 360.0f)
    {
        increment = 360.0f;
    }
    else if (increment < -360.0f)
    {
        increment = -360.0f;
    }
    increment /= arcSegs;
    increment = (increment / 180.0f * PI);
    CGFloat z = btan(increment);
    CGFloat angleBase = angle;
    CGFloat p1, q1, p2, q2, p3, q3;
    while (index <= arcSegs)
    {
        angle = angleBase + increment * (index - 1);
        CGFloat relx = cos(angle);
        CGFloat rely = sin(angle);
        p1 = (x + (relx - z * rely) * w);
        q1 = (y + (rely + z * relx) * h);
        angle += increment;
        relx = cos(angle);
        rely = sin(angle);
        p2 = (x + (relx + z * rely) * w);
        q2 = (y + (rely - z * relx) * h);
        p3 = (x + relx * w);
        q3 = (y + rely * h);

        CGContextAddCurveToPoint(cgRef, p1+offsetX, q1+offsetY, p2+offsetX, q2+offsetY, p3+offsetX, q3+offsetY);

        index++;
    }

    switch (arcType)
    {
        case 1:
            CGContextClosePath(cgRef);
            break;
        case 2:
            CGContextAddLineToPoint(cgRef, x+offsetX, y+offsetY);
            CGContextClosePath(cgRef);
            break;
        default:
            break;
    }

    return renderType;
}
QUARTZ_RENDERER_INLINE SDRenderType doArc(QuartzSDOps *qsdo, CGFloat x, CGFloat y, CGFloat w, CGFloat h, CGFloat angleStart, CGFloat angleExtent, jint arcType, BOOL fill)
{
PRINT(" doArc")
    if (YES)
    {
        return doArcUsingCG(qsdo->cgRef, x, y, w, h, angleStart, angleExtent, arcType, fill,
                                qsdo->graphicsStateInfo.offsetX, qsdo->graphicsStateInfo.offsetY);
    }
    // here we can add other implementations (ex. using QuickDraw, OpenGL, etc.)
}

QUARTZ_RENDERER_INLINE SDRenderType doPolyUsingCG(JNIEnv *env, CGContextRef cgRef, jintArray xpointsarray, jintArray ypointsarray, jint npoints, BOOL polygon, BOOL fill, CGFloat offsetX, CGFloat offsetY)
{
    SDRenderType renderType = SD_Nothing;

    if (xpointsarray == NULL || ypointsarray == NULL) {
        return SD_Nothing;
    }
    if (npoints > 1)
    {
        if (fill == YES)
        {
            renderType = SD_Fill;
        }
        else
        {
            renderType = SD_Stroke;
        }

        jint i;

        jint* xpoints = (jint*)(*env)->GetPrimitiveArrayCritical(env, xpointsarray, NULL);
        if (xpoints == NULL) {
            return SD_Nothing;
        }
        jint* ypoints = (jint*)(*env)->GetPrimitiveArrayCritical(env, ypointsarray, NULL);
        if (ypoints == NULL) {
            (*env)->ReleasePrimitiveArrayCritical(env, xpointsarray, xpoints, 0);
            return SD_Nothing;
        }

        CGContextMoveToPoint(cgRef, xpoints[0]+offsetX, ypoints[0]+offsetY);

        for (i=1; i<npoints; i++)
        {
            CGContextAddLineToPoint(cgRef, xpoints[i]+offsetX, ypoints[i]+offsetY);
        }

        if (polygon == YES)
        {
            if ((xpoints[0] != xpoints[npoints-1]) || (ypoints[0] != ypoints[npoints-1])) // according to the specs (only applies to polygons, not polylines)
            {
                CGContextAddLineToPoint(cgRef, xpoints[0]+offsetX, ypoints[0]+offsetY);
            }
        }

        (*env)->ReleasePrimitiveArrayCritical(env, ypointsarray, ypoints, 0);
        (*env)->ReleasePrimitiveArrayCritical(env, xpointsarray, xpoints, 0);
    }

    return renderType;
}
QUARTZ_RENDERER_INLINE SDRenderType doPoly(JNIEnv *env, QuartzSDOps *qsdo, jintArray xpointsarray, jintArray ypointsarray, jint npoints, BOOL polygon, BOOL fill)
{
PRINT(" doPoly")
    if (YES)
    {
        return doPolyUsingCG(env, qsdo->cgRef, xpointsarray, ypointsarray, npoints, polygon, fill,
            qsdo->graphicsStateInfo.offsetX, qsdo->graphicsStateInfo.offsetY);
    }
    // here we can add other implementations (ex. using QuickDraw, OpenGL, etc.)
}

SDRenderType doShape(QuartzSDOps *qsdo, jint *types, jfloat *coords, jint numtypes, BOOL fill, BOOL shouldApplyOffset)
{
PRINT(" doShape")
    if (YES)
    {
        CGFloat offsetX = 0.0f;
        CGFloat offsetY = 0.0f;
        if (shouldApplyOffset)
        {
            offsetX = qsdo->graphicsStateInfo.offsetX;
            offsetY = qsdo->graphicsStateInfo.offsetY;
        }
        return DoShapeUsingCG(qsdo->cgRef, types, coords, numtypes, fill, offsetX, offsetY); // defined in QuartzSurfaceData.m
    }
    // here we can add other implementations (ex. using QuickDraw, OpenGL, etc.)
}



QUARTZ_RENDERER_INLINE void doImageCG(JNIEnv *env, CGContextRef cgRef, jobject imageSurfaceData,
                                        jint interpolation, BOOL fliph, BOOL flipv, jint w, jint h, jint sx, jint sy, jint sw, jint sh, jint dx, jint dy, jint dw, jint dh)
{
//fprintf(stderr, "doImageCG\n");
//fprintf(stderr, "    flip:(%d, %d), size:(%d, %d), src:(%d, %d, %d, %d), dst:(%d, %d, %d, %d)\n", (jint)fliph, (jint)flipv, w, h, sx, sy, sw, sh, dx, dy, dw, dh);
    // gznote: need to handle interpolation
    ImageSDOps* isdo = LockImage(env, imageSurfaceData);

    CGFloat a = 1.0f;
    CGFloat b = 0.0f;
    CGFloat c = 0.0f;
    CGFloat d = -1.0f;
    CGFloat tx = dx;
    CGFloat ty = dy+dh;

    if (flipv == YES)
    {
        d = 1.0f;
        ty -= dh;
    }
    if (fliph == YES)
    {
        a = -1.0f;
        tx += dw;
    }

    makeSureImageIsCreated(isdo);

    CGContextSaveGState(cgRef);
    CGContextConcatCTM(cgRef, CGAffineTransformMake(a, b, c, d, tx, ty));
    jint alphaInfo = isdo->contextInfo.alphaInfo & kCGBitmapAlphaInfoMask;

    if ((sx == 0) && (sy == 0) && (sw == w) && (sh == h)) // no subimages allowed here
    {
        CGContextDrawImage(cgRef, CGRectMake(0, 0, dw, dh), isdo->imgRef);
    }
    else // handle subimages
    {
        CGImageRef subImg = CGImageCreateWithImageInRect(isdo->imgRef, CGRectMake(sx, sy, sw, sh));
        CGContextDrawImage(cgRef, CGRectMake(0.0f, 0.0f, dw, dh), subImg);
        CGImageRelease(subImg);
    }

    CGContextRestoreGState(cgRef);
    UnlockImage(env, isdo);
}

QUARTZ_RENDERER_INLINE void doImage(JNIEnv *env, QuartzSDOps *qsdo, jobject imageSurfaceData,
                                jboolean fliph, jboolean flipv, jint w, jint h, jint sx, jint sy, jint sw, jint sh, jint dx, jint dy, jint dw, jint dh)
{
    if ((w > 0) && (h > 0) && (sw > 0) && (sh > 0) && (dw > 0) && (dh > 0))
    {
       doImageCG(env, qsdo->cgRef, imageSurfaceData,
                            qsdo->graphicsStateInfo.interpolation, (BOOL)fliph, (BOOL)flipv, (jint)w, (jint)h, (jint)sx, (jint)sy, (jint)sw, (jint)sh, (jint)dx, (jint)dy, (jint)dw, (jint)dh);
    }
}



QUARTZ_RENDERER_INLINE void completePath(JNIEnv *env, QuartzSDOps *qsdo, CGContextRef cgRef, jint renderType)
{
    switch (renderType)
    {
        case SD_Stroke:
            if (CGContextIsPathEmpty(cgRef) == 0)
            {
                CGContextStrokePath(cgRef);
            }
            break;
        case SD_Fill:
            if (CGContextIsPathEmpty(cgRef) == 0)
            {
                CGContextFillPath(cgRef);
            }
            break;
        case SD_Image:
            break;
        case SD_Nothing:
                break;
        default:
fprintf(stderr, "completePath unknown renderType=%d\n", (int)renderType);
            break;
    }
}

/*
 * Class:     sun_java2d_CRenderer
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_java2d_CRenderer_init
(JNIEnv *env, jobject jthis)
{
PRINT("Java_sun_java2d_CRenderer_init")
    CGFloat angle = PI / 4.0f;
    CGFloat a = 1.0f - cos(angle);
    CGFloat b = tan(angle);
    CGFloat c = sqrt(1.0f + b * b) - 1.0f + a;
    CGFloat cv = 4.0f / 3.0f * a * b / c;
    CGFloat acv = (1.0f - cv) / 2.0f;

    gRoundRectCtrlpts[2][3] = -acv;
    gRoundRectCtrlpts[2][5] = acv;
    gRoundRectCtrlpts[4][1] = -acv;
    gRoundRectCtrlpts[4][7] = -acv;
    gRoundRectCtrlpts[6][3] = acv;
    gRoundRectCtrlpts[6][5] = -acv;
    gRoundRectCtrlpts[8][1] = acv;
    gRoundRectCtrlpts[8][7] = acv;
}

/*
 * Class:     sun_java2d_CRenderer
 * Method:    doLine
 * Signature: (Lsun/java2d/SurfaceData;Ljava/nio/IntBuffer;Ljava/nio/FloatBuffer;[Ljava/lang/Object;FFFF)V
 */
JNIEXPORT void JNICALL Java_sun_java2d_CRenderer_doLine
(JNIEnv *env, jobject jthis, jobject jsurfacedata, jfloat x1, jfloat y1, jfloat x2, jfloat y2)
{
PRINT("Java_sun_java2d_CRenderer_doLine")
    QuartzSDOps *qsdo = (QuartzSDOps*)SurfaceData_GetOps(env, jsurfacedata);
JNI_COCOA_ENTER(env);
    SDRenderType renderType = SD_Stroke;
    qsdo->BeginSurface(env, qsdo, renderType);
    if (qsdo->cgRef != NULL)
    {
        doLine(qsdo, x1, y1, x2, y2);
    }
    qsdo->FinishSurface(env, qsdo);
JNI_COCOA_RENDERER_EXIT(env);
}

/*
 * Class:     sun_java2d_CRenderer
 * Method:    doRect
 * Signature: (Lsun/java2d/SurfaceData;Ljava/nio/IntBuffer;Ljava/nio/FloatBuffer;[Ljava/lang/Object;FFFF)V
 */
JNIEXPORT void JNICALL Java_sun_java2d_CRenderer_doRect
(JNIEnv *env, jobject jthis, jobject jsurfacedata, jfloat x, jfloat y, jfloat w, jfloat h, jboolean isfill)
{
PRINT("Java_sun_java2d_CRenderer_doRect")
    QuartzSDOps *qsdo = (QuartzSDOps*)SurfaceData_GetOps(env, jsurfacedata);
JNI_COCOA_ENTER(env);
    SDRenderType renderType    = (isfill? SD_Fill : SD_Stroke);
    qsdo->BeginSurface(env, qsdo, renderType);
    if (qsdo->cgRef != NULL)
    {
        doRect(qsdo, x, y, w, h, isfill);
    }
    qsdo->FinishSurface(env, qsdo);
JNI_COCOA_RENDERER_EXIT(env);
}

/*
 * Class:     sun_java2d_CRenderer
 * Method:    doRoundRect
 * Signature: (Lsun/java2d/SurfaceData;Ljava/nio/IntBuffer;Ljava/nio/FloatBuffer;[Ljava/lang/Object;IIIIII)V
 */
JNIEXPORT void JNICALL Java_sun_java2d_CRenderer_doRoundRect
(JNIEnv *env, jobject jthis, jobject jsurfacedata, jfloat x, jfloat y, jfloat w, jfloat h, jfloat arcWidth, jfloat arcHeight, jboolean isfill)
{
PRINT("Java_sun_java2d_CRenderer_doRoundRect")
    QuartzSDOps *qsdo = (QuartzSDOps*)SurfaceData_GetOps(env, jsurfacedata);
JNI_COCOA_ENTER(env);
    SDRenderType renderType    = (isfill? SD_Fill : SD_Stroke);
    qsdo->BeginSurface(env, qsdo, renderType);
    if (qsdo->cgRef != NULL)
    {
        doRoundRect(qsdo, x, y, w, h, arcWidth, arcHeight, isfill);
    }
    qsdo->FinishSurface(env, qsdo);
JNI_COCOA_RENDERER_EXIT(env);
}

/*
 * Class:     sun_java2d_CRenderer
 * Method:    doOval
 * Signature: (Lsun/java2d/SurfaceData;Ljava/nio/IntBuffer;Ljava/nio/FloatBuffer;[Ljava/lang/Object;IIII)V
 */
JNIEXPORT void JNICALL Java_sun_java2d_CRenderer_doOval
(JNIEnv *env, jobject jthis, jobject jsurfacedata, jfloat x, jfloat y, jfloat w, jfloat h, jboolean isfill)
{
PRINT("Java_sun_java2d_CRenderer_doOval")
    QuartzSDOps *qsdo = (QuartzSDOps*)SurfaceData_GetOps(env, jsurfacedata);
JNI_COCOA_ENTER(env);
    SDRenderType renderType    = (isfill? SD_Fill : SD_Stroke);
    qsdo->BeginSurface(env, qsdo, renderType);
    if (qsdo->cgRef != NULL)
    {
        doOval(qsdo, x, y, w, h, isfill);
    }
    qsdo->FinishSurface(env, qsdo);
JNI_COCOA_RENDERER_EXIT(env);
}

/*
 * Class:     sun_java2d_CRenderer
 * Method:    doArc
 * Signature: (Lsun/java2d/SurfaceData;Ljava/nio/IntBuffer;Ljava/nio/FloatBuffer;[Ljava/lang/Object;IIIIII)V
 */
JNIEXPORT void JNICALL Java_sun_java2d_CRenderer_doArc
(JNIEnv *env, jobject jthis, jobject jsurfacedata, jfloat x, jfloat y, jfloat w, jfloat h, jfloat angleStart, jfloat angleExtent, jint arcType, jboolean isfill)
{
PRINT("Java_sun_java2d_CRenderer_doArc")
    QuartzSDOps *qsdo = (QuartzSDOps*)SurfaceData_GetOps(env, jsurfacedata);
JNI_COCOA_ENTER(env);
    SDRenderType renderType    = (isfill? SD_Fill : SD_Stroke);
    qsdo->BeginSurface(env, qsdo, renderType);
    if (qsdo->cgRef != NULL)
    {
        doArc(qsdo, x, y, w, h, angleStart, angleExtent, arcType, isfill);
    }
    qsdo->FinishSurface(env, qsdo);
JNI_COCOA_RENDERER_EXIT(env);
}

/*
 * Class:     sun_java2d_CRenderer
 * Method:    doPoly
 * Signature:
 */
JNIEXPORT void JNICALL Java_sun_java2d_CRenderer_doPoly
(JNIEnv *env, jobject jthis, jobject jsurfacedata, jintArray xpointsarray, jintArray ypointsarray, jint npoints, jboolean ispolygon, jboolean isfill)
{
PRINT("Java_sun_java2d_CRenderer_doPoly")
    QuartzSDOps *qsdo = (QuartzSDOps*)SurfaceData_GetOps(env, jsurfacedata);
JNI_COCOA_ENTER(env);
    BOOL eoFill = YES; // polys are WIND_EVEN_ODD by definition
    SDRenderType renderType    = (isfill? (eoFill ? SD_EOFill : SD_Fill) : SD_Stroke);
    qsdo->BeginSurface(env, qsdo, renderType);
    if (qsdo->cgRef != NULL)
    {
        doPoly(env, qsdo, xpointsarray, ypointsarray, npoints, ispolygon, isfill);
    }
    qsdo->FinishSurface(env, qsdo);
JNI_COCOA_RENDERER_EXIT(env);
}

/*
 * Class:     sun_java2d_CRenderer
 * Method:    doShape
 * Signature: (Lsun/java2d/SurfaceData;Ljava/nio/IntBuffer;Ljava/nio/FloatBuffer;[Ljava/lang/Object;ILjava/nio/FloatBuffer;Ljava/nio/IntBuffer;IZ)V
 */
JNIEXPORT void JNICALL Java_sun_java2d_CRenderer_doShape
(JNIEnv *env, jobject jthis, jobject jsurfacedata, jint length, jobject jFloatCoordinates, jobject jIntTypes, jint windingRule, jboolean isfill, jboolean shouldApplyOffset)
{
PRINT("Java_sun_java2d_CRenderer_doShape")
    QuartzSDOps *qsdo = (QuartzSDOps*)SurfaceData_GetOps(env, jsurfacedata);
JNI_COCOA_ENTER(env);
    BOOL eoFill = (windingRule == java_awt_geom_PathIterator_WIND_EVEN_ODD);
    SDRenderType renderType    = (isfill? (eoFill ? SD_EOFill : SD_Fill) : SD_Stroke);
    qsdo->BeginSurface(env, qsdo, renderType);
    if (qsdo->cgRef != NULL)
    {
        jfloat *coordinates = (jfloat*)((*env)->GetDirectBufferAddress(env, jFloatCoordinates));
        jint *types = (jint*)((*env)->GetDirectBufferAddress(env, jIntTypes));
        doShape(qsdo, types, coordinates, length, isfill, shouldApplyOffset);
    }
    qsdo->FinishSurface(env, qsdo);
JNI_COCOA_RENDERER_EXIT(env);
}

#define invalidContext(c) \
    ((c) == NULL /* || (c)->identifer != CGContextIdentifier */)

/*
 * Class:     sun_java2d_CRenderer
 * Method:    doImage
 * Signature: (Lsun/java2d/SurfaceData;Ljava/nio/IntBuffer;Ljava/nio/FloatBuffer;[Ljava/lang/Object;Lsun/java2d/SurfaceData;ZZIIIIIIII)V
 */
JNIEXPORT void JNICALL Java_sun_java2d_CRenderer_doImage
(JNIEnv *env, jobject jthis, jobject jsurfacedata, jobject imageSurfaceData, jboolean fliph, jboolean flipv, jint w, jint h, jint sx, jint sy, jint sw, jint sh, jint dx, jint dy, jint dw, jint dh)
{
PRINT("Java_sun_java2d_CRenderer_doImage")
    QuartzSDOps *qsdo = (QuartzSDOps*)SurfaceData_GetOps(env, jsurfacedata);
JNI_COCOA_ENTER(env);
    qsdo->BeginSurface(env, qsdo, SD_Image);
    if (qsdo->cgRef != NULL)
    {
        doImage(env, qsdo, imageSurfaceData, fliph, flipv, w, h, sx, sy, sw, sh, dx, dy, dw, dh);
    }
    qsdo->FinishSurface(env, qsdo);
JNI_COCOA_RENDERER_EXIT(env);
}
