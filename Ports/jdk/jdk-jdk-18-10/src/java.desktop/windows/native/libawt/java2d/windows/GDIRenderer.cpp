/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "jni_util.h"
#include "awt.h"
#include "sun_java2d_windows_GDIRenderer.h"
#include "java_awt_geom_PathIterator.h"

#include "GDIWindowSurfaceData.h"
#include "awt_Component.h"
#include "awt_Pen.h"
#include "awt_Brush.h"

#include "GraphicsPrimitiveMgr.h"

#include <math.h>                /* for cos(), sin(), etc */

#define MAX_CLAMP_BND (1<<26)
#define MIN_CLAMP_BND (-MAX_CLAMP_BND)

#define CLAMP(x) (((x) > MAX_CLAMP_BND) ?   \
    MAX_CLAMP_BND : ((x) < MIN_CLAMP_BND) ? \
        MIN_CLAMP_BND : (x))


extern "C" {

#define POLYTEMPSIZE    (512 / sizeof(POINT))

static void AngleToCoord(jint angle, jint w, jint h, jint *x, jint *y)
{
    const double pi = 3.1415926535;
    const double toRadians = 2 * pi / 360;

    *x = (long)(cos((double)angle * toRadians) * w);
    *y = -(long)(sin((double)angle * toRadians) * h);
}

static POINT *TransformPoly(jint *xpoints, jint *ypoints,
                            jint transx, jint transy,
                            POINT *pPoints, jint *pNpoints,
                            BOOL close, BOOL fixend)
{
    int npoints = *pNpoints;
    int outpoints = npoints;
    jint x, y;

    // Fix for 4298688 - draw(Line) and Polygon omit last pixel
    // We will need to add a point if we need to close it off or
    // if we need to fix the endpoint to accommodate the Windows
    // habit of never drawing the last pixel of a Polyline.  Note
    // that if the polyline is already closed then neither fix
    // is needed because the last pixel is also the first pixel
    // and so will be drawn just fine.
    // Clarification for 4298688 - regression bug 4678208 points
    // out that we still need to fix the endpoint if the closed
    // polygon never went anywhere (all vertices on same coordinate).
    jint mx = xpoints[0];
    jint my = ypoints[0];
    BOOL isclosed = (xpoints[npoints-1] == mx && ypoints[npoints-1] == my);
    if ((close && !isclosed) || fixend) {
        outpoints++;
        *pNpoints = outpoints;
    }
    if (outpoints > POLYTEMPSIZE) {
        try {
            pPoints = (POINT *) SAFE_SIZE_ARRAY_ALLOC(safe_Malloc, sizeof(POINT), outpoints);
        } catch (const std::bad_alloc&) {
            return NULL;
        }
    }
    BOOL isempty = fixend;
    for (int i = 0; i < npoints; i++) {
        x = xpoints[i];
        y = ypoints[i];
        isempty = isempty && (x == mx && y == my);
        pPoints[i].x = CLAMP(x + transx);
        pPoints[i].y = CLAMP(y + transy);
    }
    if (close && !isclosed) {
        pPoints[npoints] = pPoints[0];
    } else if (fixend) {
        if (!close || isempty) {
            // Fix for 4298688 - draw(Line) and Polygon omit last pixel
            // Fix up the last segment by adding another segment after
            // it that is only 1 pixel long.  The first pixel of that
            // segment will be drawn, but the second pixel is the one
            // that Windows omits.
            pPoints[npoints] = pPoints[npoints-1];
            pPoints[npoints].x++;
        } else {
            outpoints--;
            *pNpoints = outpoints;
        }
    }

    return pPoints;
}

/*
 * Class:     sun_java2d_windows_GDIRenderer
 * Method:    doDrawLine
 * Signature: (Lsun/java2d/windows/GDIWindowSurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;IIIII)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_windows_GDIRenderer_doDrawLine
    (JNIEnv *env, jobject wr,
     jobject sData,
     jobject clip, jobject comp, jint color,
     jint x1, jint y1, jint x2, jint y2)
{
    J2dTraceLn(J2D_TRACE_INFO, "GDIRenderer_doDrawLine");
    J2dTraceLn5(J2D_TRACE_VERBOSE,
                "  color=0x%x x1=%-4d y1=%-4d x2=%-4d y2=%-4d",
                color, x1, y1, x2, y2);
    GDIWinSDOps *wsdo = GDIWindowSurfaceData_GetOps(env, sData);
    if (wsdo == NULL) {
        return;
    }

    HDC hdc;
    jint patrop;
    if (x1 == x2 || y1 == y2) {
        if (x1 > x2) {
            jint t = x1; x1 = x2; x2 = t;
        }
        if (y1 > y2) {
            jint t = y1; y1 = y2; y2 = t;
        }
        hdc = wsdo->GetDC(env, wsdo, BRUSH, &patrop, clip, comp, color);
        if (hdc == NULL) {
            return;
        }
        ::PatBlt(hdc, x1, y1, x2-x1+1, y2-y1+1, patrop);
    } else {
        hdc = wsdo->GetDC(env, wsdo, PENBRUSH, &patrop, clip, comp, color);
        if (hdc == NULL) {
            return;
        }
        ::MoveToEx(hdc, x1, y1, NULL);
        ::LineTo(hdc, x2, y2);
        ::PatBlt(hdc, x2, y2, 1, 1, patrop);
    }
    wsdo->ReleaseDC(env, wsdo, hdc);
}

/*
 * Class:     sun_java2d_windows_GDIRenderer
 * Method:    doDrawRect
 * Signature: (Lsun/java2d/windows/GDIWindowSurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;IIIII)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_windows_GDIRenderer_doDrawRect
    (JNIEnv *env, jobject wr,
     jobject sData,
     jobject clip, jobject comp, jint color,
     jint x, jint y, jint w, jint h)
{
    J2dTraceLn(J2D_TRACE_INFO, "GDIRenderer_doDrawRect");
    J2dTraceLn5(J2D_TRACE_VERBOSE,
                "  color=0x%x x=%-4d y=%-4d w=%-4d h=%-4d",
                color, x, y, w, h);
    if (w < 0 || h < 0) {
        return;
    }

    GDIWinSDOps *wsdo = GDIWindowSurfaceData_GetOps(env, sData);
    if (wsdo == NULL) {
        return;
    }
    jint patrop;
    HDC hdc = wsdo->GetDC(env, wsdo, BRUSH, &patrop, clip, comp, color);
    if (hdc == NULL) {
        return;
    }
    if (w < 2 || h < 2) {
        // If one dimension is less than 2 then there is no
        // gap in the middle - draw a solid filled rectangle.
        ::PatBlt(hdc, x, y, w+1, h+1, patrop);
    } else {
        // Avoid drawing the endpoints twice.
        // Also prefer including the endpoints in the
        // horizontal sections which draw pixels faster.
        ::PatBlt(hdc,  x,   y,  w+1,  1,  patrop);
        ::PatBlt(hdc,  x,  y+1,  1,  h-1, patrop);
        ::PatBlt(hdc, x+w, y+1,  1,  h-1, patrop);
        ::PatBlt(hdc,  x,  y+h, w+1,  1,  patrop);
    }
    wsdo->ReleaseDC(env, wsdo, hdc);
}

/*
 * Class:     sun_java2d_windows_GDIRenderer
 * Method:    doDrawRoundRect
 * Signature: (Lsun/java2d/windows/GDIWindowSurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;IIIIIII)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_windows_GDIRenderer_doDrawRoundRect
    (JNIEnv *env, jobject wr,
     jobject sData,
     jobject clip, jobject comp, jint color,
     jint x, jint y, jint w, jint h, jint arcW, jint arcH)
{
    J2dTraceLn(J2D_TRACE_INFO, "GDIRenderer_doDrawRoundRect");
    J2dTraceLn5(J2D_TRACE_VERBOSE,
                "  color=0x%x x=%-4d y=%-4d w=%-4d h=%-4d",
                color, x, y, w, h);
    J2dTraceLn2(J2D_TRACE_VERBOSE, "  arcW=%-4d arcH=%-4d",
                arcW, arcH);
    if (w < 2 || h < 2 || arcW <= 0 || arcH <= 0) {
        // Fix for 4524760 - drawRoundRect0 test case fails on Windows 98
        // Thin round rects degenerate into regular rectangles
        // because there is no room for the arc sections.  Also
        // if there is no arc dimension then the roundrect must
        // be a simple rectangle.  Defer to the DrawRect function
        // which handles degenerate sizes better.
        Java_sun_java2d_windows_GDIRenderer_doDrawRect(env, wr,
                                                       sData, clip,
                                                       comp, color,
                                                       x, y, w, h);
        return;
    }

    GDIWinSDOps *wsdo = GDIWindowSurfaceData_GetOps(env, sData);
    if (wsdo == NULL) {
        return;
    }
    HDC hdc = wsdo->GetDC(env, wsdo, PENONLY, NULL, clip, comp, color);
    if (hdc == NULL) {
        return;
    }
    ::RoundRect(hdc, x, y, x+w+1, y+h+1, arcW, arcH);
    wsdo->ReleaseDC(env, wsdo, hdc);
}

/*
 * Class:     sun_java2d_windows_GDIRenderer
 * Method:    doDrawOval
 * Signature: (Lsun/java2d/windows/GDIWindowSurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;IIIII)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_windows_GDIRenderer_doDrawOval
    (JNIEnv *env, jobject wr,
     jobject sData,
     jobject clip, jobject comp, jint color,
     jint x, jint y, jint w, jint h)
{
    J2dTraceLn(J2D_TRACE_INFO, "GDIRenderer_doDrawOval");
    J2dTraceLn5(J2D_TRACE_VERBOSE,
                "  color=0x%x x=%-4d y=%-4d w=%-4d h=%-4d",
                color, x, y, w, h);
    if (w < 2 || h < 2) {
        // Thin enough ovals have no room for curvature.  Defer to
        // the DrawRect method which handles degenerate sizes better.
        Java_sun_java2d_windows_GDIRenderer_doDrawRect(env, wr,
                                                       sData, clip,
                                                       comp, color,
                                                       x, y, w, h);
        return;
    }

    GDIWinSDOps *wsdo = GDIWindowSurfaceData_GetOps(env, sData);
    if (wsdo == NULL) {
        return;
    }
    HDC hdc = wsdo->GetDC(env, wsdo, PENONLY, NULL, clip, comp, color);
    if (hdc == NULL) {
        return;
    }
    ::Ellipse(hdc, x, y, x+w+1, y+h+1);
    wsdo->ReleaseDC(env, wsdo, hdc);
}

/*
 * Class:     sun_java2d_windows_GDIRenderer
 * Method:    doDrawArc
 * Signature: (Lsun/java2d/windows/GDIWindowSurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;IIIIIII)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_windows_GDIRenderer_doDrawArc
    (JNIEnv *env, jobject wr,
     jobject sData,
     jobject clip, jobject comp, jint color,
     jint x, jint y, jint w, jint h,
     jint angleStart, jint angleExtent)
{
    J2dTraceLn(J2D_TRACE_INFO, "GDIRenderer_doDrawArc");
    J2dTraceLn5(J2D_TRACE_VERBOSE,
                "  color=0x%x x=%-4d y=%-4d w=%-4d h=%-4d",
                color, x, y, w, h);
    J2dTraceLn2(J2D_TRACE_VERBOSE,
                "  angleStart=%-4d angleExtent=%-4d",
                angleStart, angleExtent);
    if (w < 0 || h < 0 || angleExtent == 0) {
        return;
    }

    GDIWinSDOps *wsdo = GDIWindowSurfaceData_GetOps(env, sData);
    if (wsdo == NULL) {
        return;
    }

    long sx, sy, ex, ey;
    if (angleExtent >= 360 || angleExtent <= -360) {
        sx = ex = x + w;
        sy = ey = y + h/2;
    } else {
        int angleEnd;
        if (angleExtent < 0) {
            angleEnd = angleStart;
            angleStart += angleExtent;
        } else {
            angleEnd = angleStart + angleExtent;
        }
        AngleToCoord(angleStart, w, h, &sx, &sy);
        sx += x + w/2;
        sy += y + h/2;
        AngleToCoord(angleEnd, w, h, &ex, &ey);
        ex += x + w/2;
        ey += y + h/2;
    }
    HDC hdc = wsdo->GetDC(env, wsdo, PEN, NULL, clip, comp, color);
    if (hdc == NULL) {
        return;
    }
    ::Arc(hdc, x, y, x+w+1, y+h+1, sx, sy, ex, ey);
    wsdo->ReleaseDC(env, wsdo, hdc);
}

/*
 * Class:     sun_java2d_windows_GDIRenderer
 * Method:    doDrawPoly
 * Signature: (Lsun/java2d/windows/GDIWindowSurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;III[I[IIZ)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_windows_GDIRenderer_doDrawPoly
    (JNIEnv *env, jobject wr,
     jobject sData,
     jobject clip, jobject comp, jint color,
     jint transx, jint transy,
     jintArray xpointsarray, jintArray ypointsarray,
     jint npoints, jboolean isclosed)
{
    J2dTraceLn(J2D_TRACE_INFO, "GDIRenderer_doDrawPoly");
    J2dTraceLn5(J2D_TRACE_VERBOSE,
                "  color=0x%x transx=%-4d transy=%-4d "\
                "npoints=%-4d isclosed=%-4d",
                color, transx, transy, npoints, isclosed);
    if (JNU_IsNull(env, xpointsarray) || JNU_IsNull(env, ypointsarray)) {
        JNU_ThrowNullPointerException(env, "coordinate array");
        return;
    }
    if (env->GetArrayLength(xpointsarray) < npoints ||
        env->GetArrayLength(ypointsarray) < npoints)
    {
        JNU_ThrowArrayIndexOutOfBoundsException(env, "coordinate array");
        return;
    }
    if (npoints < 2) {
        // Fix for 4067534 - assertion failure in 1.3.1 for degenerate polys
        // Not enough points for a line.
        // Note that this would be ignored later anyway, but returning
        // here saves us from mistakes in TransformPoly and seeing bad
        // return values from the Windows Polyline function.
        return;
    }

    GDIWinSDOps *wsdo = GDIWindowSurfaceData_GetOps(env, sData);
    if (wsdo == NULL) {
        return;
    }

    POINT tmpPts[POLYTEMPSIZE], *pPoints = NULL;

    jint *xpoints = (jint *) env->GetPrimitiveArrayCritical(xpointsarray, NULL);

    if (xpoints != NULL) {
        jint *ypoints = (jint *) env->GetPrimitiveArrayCritical(ypointsarray, NULL);
        if (ypoints != NULL) {
            pPoints = TransformPoly(xpoints, ypoints, transx, transy,
                                    tmpPts, &npoints, isclosed, TRUE);
            env->ReleasePrimitiveArrayCritical(ypointsarray, ypoints, JNI_ABORT);
        }
        env->ReleasePrimitiveArrayCritical(xpointsarray, xpoints, JNI_ABORT);
    }

    if (pPoints == NULL) {
        return;
    }

    HDC hdc = wsdo->GetDC(env, wsdo, PEN, NULL, clip, comp, color);
    if (hdc == NULL) {
        return;
    }
    ::Polyline(hdc, pPoints, npoints);
    wsdo->ReleaseDC(env, wsdo, hdc);
    if (pPoints != tmpPts) {
        free(pPoints);
    }
}

/*
 * Class:     sun_java2d_windows_GDIRenderer
 * Method:    doFillRect
 * Signature: (Lsun/java2d/windows/GDIWindowSurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;IIIII)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_windows_GDIRenderer_doFillRect
    (JNIEnv *env, jobject wr,
     jobject sData,
     jobject clip, jobject comp, jint color,
     jint x, jint y, jint w, jint h)
{
    J2dTraceLn(J2D_TRACE_INFO, "GDIRenderer_doFillRect");
    J2dTraceLn5(J2D_TRACE_VERBOSE,
                "  color=0x%x x=%-4d y=%-4d w=%-4d h=%-4d",
                color, x, y, w, h);
    if (w <= 0 || h <= 0) {
        return;
    }

    GDIWinSDOps *wsdo = GDIWindowSurfaceData_GetOps(env, sData);
    if (wsdo == NULL) {
        return;
    }
    jint patrop;
    HDC hdc = wsdo->GetDC(env, wsdo, BRUSH, &patrop, clip, comp, color);
    if (hdc == NULL) {
        return;
    }
    ::PatBlt(hdc, x, y, w, h, patrop);
    wsdo->ReleaseDC(env, wsdo, hdc);
}

/*
 * Class:     sun_java2d_windows_GDIRenderer
 * Method:    doFillRoundRect
 * Signature: (Lsun/java2d/windows/GDIWindowSurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;IIIIIII)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_windows_GDIRenderer_doFillRoundRect
    (JNIEnv *env, jobject wr,
     jobject sData,
     jobject clip, jobject comp, jint color,
     jint x, jint y, jint w, jint h, jint arcW, jint arcH)
{
    J2dTraceLn(J2D_TRACE_INFO, "GDIRenderer_doFillRoundRect");
    J2dTraceLn5(J2D_TRACE_VERBOSE,
                "  color=0x%x x=%-4d y=%-4d w=%-4d h=%-4d",
                color, x, y, w, h);
    J2dTraceLn2(J2D_TRACE_VERBOSE, "  arcW=%-4d arcH=%-4d",
                arcW, arcH);
    if (w < 2 || h < 2 || arcW <= 0 || arcH <= 0) {
        // Fix related to 4524760 - drawRoundRect0 fails on Windows 98
        // Thin round rects have no room for curvature.  Also, if
        // the curvature is empty then the primitive has degenerated
        // into a simple rectangle.  Defer to the FillRect method
        // which deals with degenerate sizes better.
        Java_sun_java2d_windows_GDIRenderer_doFillRect(env, wr,
                                                       sData, clip,
                                                       comp, color,
                                                       x, y, w, h);
        return;
    }

    GDIWinSDOps *wsdo = GDIWindowSurfaceData_GetOps(env, sData);
    if (wsdo == NULL) {
        return;
    }
    HDC hdc = wsdo->GetDC(env, wsdo, BRUSHONLY, NULL, clip, comp, color);
    if (hdc == NULL) {
        return;
    }
    ::RoundRect(hdc, x, y, x+w+1, y+h+1, arcW, arcH);
    wsdo->ReleaseDC(env, wsdo, hdc);
}

/*
 * Class:     sun_java2d_windows_GDIRenderer
 * Method:    doFillOval
 * Signature: (Lsun/java2d/windows/GDIWindowSurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;IIIII)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_windows_GDIRenderer_doFillOval
    (JNIEnv *env, jobject wr,
     jobject sData,
     jobject clip, jobject comp, jint color,
     jint x, jint y, jint w, jint h)
{
    J2dTraceLn(J2D_TRACE_INFO, "GDIRenderer_doFillOval");
    J2dTraceLn5(J2D_TRACE_VERBOSE,
                "  color=0x%x x=%-4d y=%-4d w=%-4d h=%-4d",
                color, x, y, w, h);
    if (w < 3 || h < 3) {
        // Fix for 4411814 - small ovals do not draw anything
        // (related to 4205762 on Solaris platform)
        // Most platform graphics packages have poor rendering
        // for thin ellipses and the rendering is most strikingly
        // different from our theoretical arcs.  Ideally we should
        // trap all ovals less than some fairly large size and
        // try to draw aesthetically pleasing ellipses, but that
        // would require considerably more work to get the corresponding
        // drawArc variants to match pixel for pixel.
        // Thin ovals of girth 1 pixel are simple rectangles.
        // Thin ovals of girth 2 pixels are simple rectangles with
        // potentially smaller lengths.  Determine the correct length
        // by calculating .5*.5 + scaledlen*scaledlen == 1.0 which
        // means that scaledlen is the sqrt(0.75).  Scaledlen is
        // relative to the true length (w or h) and needs to be
        // adjusted by half a pixel in different ways for odd or
        // even lengths.
#define SQRT_3_4 0.86602540378443864676
        if (w > 2 && h > 1) {
            int adjw = (int) ((SQRT_3_4 * w - ((w&1)-1)) * 0.5);
            adjw = adjw * 2 + (w&1);
            x += (w-adjw)/2;
            w = adjw;
        } else if (h > 2 && w > 1) {
            int adjh = (int) ((SQRT_3_4 * h - ((h&1)-1)) * 0.5);
            adjh = adjh * 2 + (h&1);
            y += (h-adjh)/2;
            h = adjh;
        }
#undef SQRT_3_4
        if (w > 0 && h > 0) {
            Java_sun_java2d_windows_GDIRenderer_doFillRect(env, wr, sData,
                                                           clip, comp, color,
                                                           x, y, w, h);
        }
        return;
    }

    GDIWinSDOps *wsdo = GDIWindowSurfaceData_GetOps(env, sData);
    if (wsdo == NULL) {
        return;
    }
    HDC hdc = wsdo->GetDC(env, wsdo, BRUSHONLY, NULL, clip, comp, color);
    if (hdc == NULL) {
        return;
    }
    ::Ellipse(hdc, x, y, x+w+1, y+h+1);
    wsdo->ReleaseDC(env, wsdo, hdc);
}

/*
 * Class:     sun_java2d_windows_GDIRenderer
 * Method:    doFillArc
 * Signature: (Lsun/java2d/windows/GDIWindowSurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;IIIIIII)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_windows_GDIRenderer_doFillArc
    (JNIEnv *env, jobject wr,
     jobject sData,
     jobject clip, jobject comp, jint color,
     jint x, jint y, jint w, jint h,
     jint angleStart, jint angleExtent)
{
    J2dTraceLn(J2D_TRACE_INFO, "GDIRenderer_doFillArc");
    J2dTraceLn5(J2D_TRACE_VERBOSE,
                "  color=0x%x x=%-4d y=%-4d w=%-4d h=%-4d",
                color, x, y, w, h);
    J2dTraceLn2(J2D_TRACE_VERBOSE,
                "  angleStart=%-4d angleExtent=%-4d",
                angleStart, angleExtent);
    if (w <= 0 || h <= 0 || angleExtent == 0) {
        return;
    }
    if (angleExtent >= 360 || angleExtent <= -360) {
        // Fix related to 4411814 - small ovals (and arcs) do not draw
        // If the arc is a full circle, let the Oval method handle it
        // since that method can deal with degenerate sizes better.
        Java_sun_java2d_windows_GDIRenderer_doFillOval(env, wr,
                                                       sData, clip,
                                                       comp, color,
                                                       x, y, w, h);
        return;
    }

    GDIWinSDOps *wsdo = GDIWindowSurfaceData_GetOps(env, sData);
    if (wsdo == NULL) {
        return;
    }
    long sx, sy, ex, ey;
    int angleEnd;
    if (angleExtent < 0) {
        angleEnd = angleStart;
        angleStart += angleExtent;
    } else {
        angleEnd = angleStart + angleExtent;
    }
    AngleToCoord(angleStart, w, h, &sx, &sy);
    sx += x + w/2;
    sy += y + h/2;
    AngleToCoord(angleEnd, w, h, &ex, &ey);
    ex += x + w/2;
    ey += y + h/2;
    HDC hdc = wsdo->GetDC(env, wsdo, BRUSHONLY, NULL, clip, comp, color);
    if (hdc == NULL) {
        return;
    }
    ::Pie(hdc, x, y, x+w+1, y+h+1, sx, sy, ex, ey);
    wsdo->ReleaseDC(env, wsdo, hdc);
}

/*
 * Class:     sun_java2d_windows_GDIRenderer
 * Method:    doFillPoly
 * Signature: (Lsun/java2d/windows/GDIWindowSurfaceData;Lsun/java2d/pipe/Region;Ljava/awt/Composite;III[I[II)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_windows_GDIRenderer_doFillPoly
    (JNIEnv *env, jobject wr,
     jobject sData,
     jobject clip, jobject comp, jint color,
     jint transx, jint transy,
     jintArray xpointsarray, jintArray ypointsarray,
     jint npoints)
{
    J2dTraceLn(J2D_TRACE_INFO, "GDIRenderer_doFillPoly");
    J2dTraceLn4(J2D_TRACE_VERBOSE,
                "  color=0x%x transx=%-4d transy=%-4d npoints=%-4d",
                color, transx, transy, npoints);
    if (JNU_IsNull(env, xpointsarray) || JNU_IsNull(env, ypointsarray)) {
        JNU_ThrowNullPointerException(env, "coordinate array");
        return;
    }
    if (env->GetArrayLength(xpointsarray) < npoints ||
        env->GetArrayLength(ypointsarray) < npoints)
    {
        JNU_ThrowArrayIndexOutOfBoundsException(env, "coordinate array");
        return;
    }
    if (npoints < 3) {
        // Fix for 4067534 - assertion failure in 1.3.1 for degenerate polys
        // Not enough points for a triangle.
        // Note that this would be ignored later anyway, but returning
        // here saves us from mistakes in TransformPoly and seeing bad
        // return values from the Windows Polyline function.
        return;
    }

    GDIWinSDOps *wsdo = GDIWindowSurfaceData_GetOps(env, sData);
    if (wsdo == NULL) {
        return;
    }

    POINT tmpPts[POLYTEMPSIZE], *pPoints = NULL;

    jint *xpoints = (jint *) env->GetPrimitiveArrayCritical(xpointsarray, NULL);
    if (xpoints != NULL) {
        jint *ypoints = (jint *) env->GetPrimitiveArrayCritical(ypointsarray, NULL);
        if (ypoints != NULL) {
            pPoints = TransformPoly(xpoints, ypoints, transx, transy,
                                tmpPts, &npoints, FALSE, FALSE);
            env->ReleasePrimitiveArrayCritical(ypointsarray, ypoints, JNI_ABORT);
        }
        env->ReleasePrimitiveArrayCritical(xpointsarray, xpoints, JNI_ABORT);
    }

    if (pPoints == NULL) {
        return;
    }

    HDC hdc = wsdo->GetDC(env, wsdo, BRUSHONLY, NULL, clip, comp, color);
    if (hdc == NULL) {
        return;
    }
    ::SetPolyFillMode(hdc, ALTERNATE);
    ::Polygon(hdc, pPoints, npoints);
    wsdo->ReleaseDC(env, wsdo, hdc);
    if (pPoints != tmpPts) {
        free(pPoints);
    }
}

/*
 * Class:     sun_java2d_windows_GDIRenderer
 * Method:    doShape
 * Signature:  (Lsun/java2d/windows/GDIWindowSurfaceData;Lsun/java2d/pipe/Region;
 *              Ljava/awt/Composite;IIILjava/awt/geom/Path2D.Float;Z)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_windows_GDIRenderer_doShape
    (JNIEnv *env, jobject wr,
     jobject sData,
     jobject clip, jobject comp, jint color,
     jint transX, jint transY,
     jobject p2df, jboolean isfill)
{
    J2dTraceLn(J2D_TRACE_INFO, "GDIRenderer_doShape");
    J2dTraceLn4(J2D_TRACE_VERBOSE,
                "  color=0x%x transx=%-4d transy=%-4d isfill=%-4d",
                color, transX, transY, isfill);
    GDIWinSDOps *wsdo = GDIWindowSurfaceData_GetOps(env, sData);
    if (wsdo == NULL) {
        return;
    }

    jarray typesarray = (jarray) env->GetObjectField(p2df, path2DTypesID);
    jarray coordsarray = (jarray) env->GetObjectField(p2df,
                                                      path2DFloatCoordsID);
    if (coordsarray == NULL) {
        JNU_ThrowNullPointerException(env, "coordinates array");
        return;
    }
    jint numtypes = env->GetIntField(p2df, path2DNumTypesID);
    if (env->GetArrayLength(typesarray) < numtypes) {
        JNU_ThrowArrayIndexOutOfBoundsException(env, "types array");
        return;
    }
    jint maxcoords = env->GetArrayLength(coordsarray);
    jint rule = env->GetIntField(p2df, path2DWindingRuleID);

    HDC hdc = wsdo->GetDC(env, wsdo, (isfill ? BRUSH : PEN), NULL,
                          clip, comp, color);
    if (hdc == NULL) {
        return;
    }

    jbyte *types = (jbyte *) env->GetPrimitiveArrayCritical(typesarray,
                                                            NULL);
    if (types == NULL) {
        wsdo->ReleaseDC(env, wsdo, hdc);
        return;
    }

    jfloat *coords = (jfloat *) env->GetPrimitiveArrayCritical(coordsarray,
                                                               NULL);
    if (coords == NULL) {
        env->ReleasePrimitiveArrayCritical(typesarray, types, JNI_ABORT);
        wsdo->ReleaseDC(env, wsdo, hdc);
        return;
    }

    ::SetPolyFillMode(hdc, (rule == java_awt_geom_PathIterator_WIND_NON_ZERO
                            ? WINDING : ALTERNATE));
    ::BeginPath(hdc);

    int index = 0;
    BOOL ok = TRUE;
    BOOL isempty = TRUE;
    BOOL isapoint = TRUE;
    int mx = 0, my = 0, x1 = 0, y1 = 0;
    POINT ctrlpts[3];
    for (int i = 0; ok && i < numtypes; i++) {
        switch (types[i]) {
        case java_awt_geom_PathIterator_SEG_MOVETO:
            if (!isfill && !isempty) {
                // Fix for 4298688 - draw(Line) omits last pixel
                // Windows omits the last pixel of a path when stroking.
                // Fix up the last segment of the previous subpath by
                // adding another segment after it that is only 1 pixel
                // long.  The first pixel of that segment will be drawn,
                // but the second pixel is the one that Windows omits.
                ::LineTo(hdc, x1+1, y1);
            }
            if (index + 2 <= maxcoords) {
                mx = x1 = transX + (int) floor(coords[index++]);
                my = y1 = transY + (int) floor(coords[index++]);
                ::MoveToEx(hdc, x1, y1, NULL);
                isempty = TRUE;
                isapoint = TRUE;
            } else {
                ok = FALSE;
            }
            break;
        case java_awt_geom_PathIterator_SEG_LINETO:
            if (index + 2 <= maxcoords) {
                x1 = transX + (int) floor(coords[index++]);
                y1 = transY + (int) floor(coords[index++]);
                ::LineTo(hdc, x1, y1);
                isapoint = isapoint && (x1 == mx && y1 == my);
                isempty = FALSE;
            } else {
                ok = FALSE;
            }
            break;
        case java_awt_geom_PathIterator_SEG_QUADTO:
            if (index + 4 <= maxcoords) {
                ctrlpts[0].x = transX + (int) floor(coords[index++]);
                ctrlpts[0].y = transY + (int) floor(coords[index++]);
                ctrlpts[2].x = transX + (int) floor(coords[index++]);
                ctrlpts[2].y = transY + (int) floor(coords[index++]);
                ctrlpts[1].x = (ctrlpts[0].x * 2 + ctrlpts[2].x) / 3;
                ctrlpts[1].y = (ctrlpts[0].y * 2 + ctrlpts[2].y) / 3;
                ctrlpts[0].x = (ctrlpts[0].x * 2 + x1) / 3;
                ctrlpts[0].y = (ctrlpts[0].y * 2 + y1) / 3;
                x1 = ctrlpts[2].x;
                y1 = ctrlpts[2].y;
                ::PolyBezierTo(hdc, ctrlpts, 3);
                isapoint = isapoint && (x1 == mx && y1 == my);
                isempty = FALSE;
            } else {
                ok = FALSE;
            }
            break;
        case java_awt_geom_PathIterator_SEG_CUBICTO:
            if (index + 6 <= maxcoords) {
                ctrlpts[0].x = transX + (int) floor(coords[index++]);
                ctrlpts[0].y = transY + (int) floor(coords[index++]);
                ctrlpts[1].x = transX + (int) floor(coords[index++]);
                ctrlpts[1].y = transY + (int) floor(coords[index++]);
                ctrlpts[2].x = transX + (int) floor(coords[index++]);
                ctrlpts[2].y = transY + (int) floor(coords[index++]);
                x1 = ctrlpts[2].x;
                y1 = ctrlpts[2].y;
                ::PolyBezierTo(hdc, ctrlpts, 3);
                isapoint = isapoint && (x1 == mx && y1 == my);
                isempty = FALSE;
            } else {
                ok = FALSE;
            }
            break;
        case java_awt_geom_PathIterator_SEG_CLOSE:
            ::CloseFigure(hdc);
            if (x1 != mx || y1 != my) {
                x1 = mx;
                y1 = my;
                ::MoveToEx(hdc, x1, y1, NULL);
                isempty = TRUE;
                isapoint = TRUE;
            } else if (!isfill && !isempty && isapoint) {
                ::LineTo(hdc, x1+1, y1);
                ::MoveToEx(hdc, x1, y1, NULL);
                isempty = TRUE;
                isapoint = TRUE;
            }
            break;
        }
    }
    env->ReleasePrimitiveArrayCritical(typesarray, types, JNI_ABORT);
    env->ReleasePrimitiveArrayCritical(coordsarray, coords, JNI_ABORT);
    if (ok) {
        if (!isfill && !isempty) {
            // Fix for 4298688 - draw(Line) omits last pixel
            // Windows omits the last pixel of a path when stroking.
            // Fix up the last segment of the previous subpath by
            // adding another segment after it that is only 1 pixel
            // long.  The first pixel of that segment will be drawn,
            // but the second pixel is the one that Windows omits.
            ::LineTo(hdc, x1+1, y1);
        }
        ::EndPath(hdc);
        if (isfill) {
            ::FillPath(hdc);
        } else {
            ::StrokePath(hdc);
        }
    } else {
        ::AbortPath(hdc);
        JNU_ThrowArrayIndexOutOfBoundsException(env, "coords array");
    }
    wsdo->ReleaseDC(env, wsdo, hdc);
}

} /* extern "C" */

INLINE BOOL RectInMonitorRect(RECT *rCheck, RECT *rContainer)
{
    // Assumption: left <= right, top <= bottom
    if (rCheck->left >= rContainer->left &&
        rCheck->right <= rContainer->right &&
        rCheck->top >= rContainer->top &&
        rCheck->bottom <= rContainer->bottom)
    {
        return TRUE;
    } else {
        return FALSE;
    }
}

/*
 * Class:     sun_java2d_windows_GDIRenderer
 * Method:    devCopyArea
 * Signature: (Lsun/java2d/windows/GDIWindowSurfaceData;IIIIII)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_windows_GDIRenderer_devCopyArea
    (JNIEnv *env, jobject wr,
     jobject wsd,
     jint srcx, jint srcy,
     jint dx, jint dy,
     jint width, jint height)
{
    GDIWinSDOps *wsdo = GDIWindowSurfaceData_GetOps(env, wsd);
    J2dTraceLn(J2D_TRACE_INFO, "GDIWindowSurfaceData_devCopyArea");
    J2dTraceLn4(J2D_TRACE_VERBOSE, "   srcx=%-4d srcy=%-4d dx=%-4d dy=%-4d",
                srcx, srcy, dx, dy);
    J2dTraceLn2(J2D_TRACE_VERBOSE, "     w=%-4d h=%-4d", width, height);
    if (wsdo == NULL) {
        return;
    }
    if (wsdo->invalid) {
        SurfaceData_ThrowInvalidPipeException(env,
            "GDIRenderer_devCopyArea: invalid surface data");
        return;
    }

    HDC hDC = wsdo->GetDC(env, wsdo, 0, NULL, NULL, NULL, 0);
    if (hDC == NULL) {
        return;
    }

    RECT r;
    ::SetRect(&r, srcx, srcy, srcx + width, srcy + height);
    HRGN rgnUpdate = ::CreateRectRgn(0, 0, 0, 0);
    VERIFY(::ScrollDC(hDC, dx, dy, &r, NULL, rgnUpdate, NULL));

    // ScrollDC invalidates the part of the source rectangle that
    // is outside of the destination rectangle on the assumption
    // that you wanted to "move" the pixels from source to dest,
    // and so now you will want to paint new pixels in the source.
    // Since our copyarea operation involves no such semantics we
    // are only interested in the part of the update region that
    // corresponds to unavailable source pixels - i.e. the part
    // that falls within the destination rectangle.

    // The update region will be in client relative coordinates
    // but the destination rect will be in window relative coordinates
    ::OffsetRect(&r, dx-wsdo->insets.left, dy-wsdo->insets.top);
    HRGN rgnDst = ::CreateRectRgnIndirect(&r);
    int result = ::CombineRgn(rgnUpdate, rgnUpdate, rgnDst, RGN_AND);

    // Invalidate the exposed area.
    if (result != NULLREGION) {
        ::InvalidateRgn(wsdo->window, rgnUpdate, TRUE);
    }
    ::DeleteObject(rgnUpdate);
    ::DeleteObject(rgnDst);

    wsdo->ReleaseDC(env, wsdo, hDC);
}
