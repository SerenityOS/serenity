/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "sun_java2d_x11_X11Renderer.h"

#include "X11SurfaceData.h"
#include "SpanIterator.h"
#include "Trace.h"
#include "ProcessPath.h"
#include "GraphicsPrimitiveMgr.h"


#include <jlong.h>

#ifndef HEADLESS
#define POLYTEMPSIZE    (int)(256 / sizeof(XPoint))
#define ABS(n)          (((n) < 0) ? -(n) : (n))

#define MAX_SHORT 32767
#define MIN_SHORT (-32768)

#define CLAMP_TO_SHORT(x) (((x) > MAX_SHORT)                            \
                           ? MAX_SHORT                                  \
                           : ((x) < MIN_SHORT)                          \
                               ? MIN_SHORT                              \
                               : (x))

#define CLAMP_TO_USHORT(x)  (((x) > 65535) ? 65535 : ((x) < 0) ? 0 : (x))

#define DF_MAX_XPNTS 256

typedef struct {
    Drawable drawable;
    GC      gc;
    XPoint  *pPoints;
    XPoint  dfPoints[DF_MAX_XPNTS];
    jint    npoints;
    jint    maxpoints;
} XDrawHandlerData;

#define XDHD_INIT(PTR, _GC, DRAWABLE)                                       \
    do {                                                                    \
        (PTR)->pPoints = (PTR)->dfPoints;                                   \
        (PTR)->npoints = 0;                                                 \
        (PTR)->maxpoints = DF_MAX_XPNTS;                                    \
        (PTR)->gc = (_GC);                                                    \
        (PTR)->drawable = (DRAWABLE);                                         \
    } while(0)

#define XDHD_RESET(PTR)                                                     \
    do {                                                                    \
        (PTR)->npoints = 0;                                                 \
    } while(0)


#define XDHD_ADD_POINT(PTR, X, Y)                                           \
    do {                                                                    \
        XPoint* _pnts = (PTR)->pPoints;                                     \
        jint _npnts = (PTR)->npoints;                                       \
        if (_npnts >= (PTR)->maxpoints) {                                   \
            jint newMax = (PTR)->maxpoints*2;                               \
            if ((PTR)->pPoints == (PTR)->dfPoints) {                        \
                (PTR)->pPoints = (XPoint*)malloc(newMax*sizeof(XPoint));    \
                memcpy((PTR)->pPoints, _pnts, _npnts*sizeof(XPoint));       \
            } else {                                                        \
                (PTR)->pPoints = (XPoint*)realloc(                          \
                    _pnts, newMax*sizeof(XPoint));                          \
            }                                                               \
            _pnts = (PTR)->pPoints;                                         \
            (PTR)->maxpoints = newMax;                                      \
        }                                                                   \
        _pnts += _npnts;                                                    \
        _pnts->x = X;                                                       \
        _pnts->y = Y;                                                       \
        (PTR)->npoints = _npnts + 1;                                        \
    } while(0)

#define XDHD_FREE_POINTS(PTR)                                               \
    do {                                                                    \
        if ((PTR)->pPoints != (PTR)->dfPoints) {                            \
            free((PTR)->pPoints);                                           \
        }                                                                   \
    } while(0)


static void
awt_drawArc(JNIEnv * env, jint drawable, GC xgc,
            int x, int y, int w, int h,
            int startAngle, int endAngle,
            int filled)
{
    int s, e;

    if (w < 0 || h < 0) {
        return;
    }
    if (endAngle >= 360 || endAngle <= -360) {
        s = 0;
        e = 360 * 64;
    } else {
        s = (startAngle % 360) * 64;
        e = endAngle * 64;
    }
    if (filled == 0) {
        XDrawArc(awt_display, drawable, xgc, x, y, w, h, s, e);
    } else {
        XFillArc(awt_display, drawable, xgc, x, y, w, h, s, e);
    }
}

/*
 * Copy vertices from xcoordsArray and ycoordsArray to a buffer
 * of XPoint structures, translating by transx and transy and
 * collapsing empty segments out of the list as we go.
 * The number of points to be converted should be guaranteed
 * to be more than 2 by the caller and is stored at *pNpoints.
 * The resulting number of uncollapsed unique translated vertices
 * will be stored back into the location *pNpoints.
 * The points pointer is guaranteed to be pointing to an area of
 * memory large enough for POLYTEMPSIZE points and a larger
 * area of memory is allocated (and returned) if that is not enough.
 */
static XPoint *
transformPoints(JNIEnv * env,
                jintArray xcoordsArray, jintArray ycoordsArray,
                jint transx, jint transy,
                XPoint * points, int *pNpoints, int close)
{
    int npoints = *pNpoints;
    jint *xcoords, *ycoords;

    xcoords = (jint *)
        (*env)->GetPrimitiveArrayCritical(env, xcoordsArray, NULL);
    if (xcoords == NULL) {
        return 0;
    }

    ycoords = (jint *)
        (*env)->GetPrimitiveArrayCritical(env, ycoordsArray, NULL);
    if (ycoords == NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, xcoordsArray, xcoords,
                                              JNI_ABORT);
        return 0;
    }

    if (close) {
        close = (xcoords[npoints - 1] != xcoords[0] ||
                 ycoords[npoints - 1] != ycoords[0]);
        if (close) {
            npoints++;
        }
    }
    if (npoints > POLYTEMPSIZE) {
        points = (XPoint *) malloc(sizeof(XPoint) * npoints);
    }
    if (points != NULL) {
        int in, out;
        int oldx = CLAMP_TO_SHORT(xcoords[0] + transx);
        int oldy = CLAMP_TO_SHORT(ycoords[0] + transy);
        points[0].x = oldx;
        points[0].y = oldy;
        if (close) {
            npoints--;
        }
        for (in = 1, out = 1; in < npoints; in++) {
            int newx = CLAMP_TO_SHORT(xcoords[in] + transx);
            int newy = CLAMP_TO_SHORT(ycoords[in] + transy);
            if (newx != oldx || newy != oldy) {
                points[out].x = newx;
                points[out].y = newy;
                out++;
                oldx = newx;
                oldy = newy;
            }
        }
        if (out == 1) {
            points[1].x = oldx;
            points[1].y = oldy;
            out = 2;
        } else if (close) {
            points[out++] = points[0];
        }
        *pNpoints = out;
    }

    (*env)->ReleasePrimitiveArrayCritical(env, xcoordsArray, xcoords,
                                          JNI_ABORT);
    (*env)->ReleasePrimitiveArrayCritical(env, ycoordsArray, ycoords,
                                          JNI_ABORT);

    return points;
}
#endif /* !HEADLESS */

/*
 * Class:     sun_java2d_x11_X11Renderer
 * Method:    XDrawLine
 * Signature: (IJIIII)V
 */
JNIEXPORT void JNICALL Java_sun_java2d_x11_X11Renderer_XDrawLine
    (JNIEnv *env, jobject xr,
     jlong pXSData, jlong xgc,
     jint x1, jint y1, jint x2, jint y2)
{
#ifndef HEADLESS
    X11SDOps *xsdo = (X11SDOps *) pXSData;

    if (xsdo == NULL) {
        return;
    }

    XDrawLine(awt_display, xsdo->drawable, (GC) xgc,
              CLAMP_TO_SHORT(x1), CLAMP_TO_SHORT(y1),
              CLAMP_TO_SHORT(x2), CLAMP_TO_SHORT(y2));
    X11SD_DirectRenderNotify(env, xsdo);
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11Renderer
 * Method:    XDrawRect
 * Signature: (IJIIII)V
 */
JNIEXPORT void JNICALL Java_sun_java2d_x11_X11Renderer_XDrawRect
    (JNIEnv *env, jobject xr,
     jlong pXSData, jlong xgc,
     jint x, jint y, jint w, jint h)
{
#ifndef HEADLESS
    X11SDOps *xsdo = (X11SDOps *) pXSData;

    if (xsdo == NULL || w < 0 || h < 0) {
        return;
    }

    if (w < 2 || h < 2) {
        /* REMIND: This optimization assumes thin lines. */
        /*
         * This optimization not only simplifies the processing
         * of a particular degenerate case, but it protects against
         * the anomalies of various X11 implementations that draw
         * nothing for degenerate Polygons and Rectangles.
         */
        XFillRectangle(awt_display, xsdo->drawable, (GC) xgc,
                       CLAMP_TO_SHORT(x),  CLAMP_TO_SHORT(y),
                       CLAMP_TO_USHORT(w+1), CLAMP_TO_USHORT(h+1));
    } else {
        XDrawRectangle(awt_display, xsdo->drawable, (GC) xgc,
                       CLAMP_TO_SHORT(x),  CLAMP_TO_SHORT(y),
                       CLAMP_TO_USHORT(w), CLAMP_TO_USHORT(h));
    }
    X11SD_DirectRenderNotify(env, xsdo);
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11Renderer
 * Method:    XDrawRoundRect
 * Signature: (IJIIIIII)V
 */
JNIEXPORT void JNICALL Java_sun_java2d_x11_X11Renderer_XDrawRoundRect
    (JNIEnv *env, jobject xr,
     jlong pXSData, jlong xgc,
     jint x, jint y, jint w, jint h,
     jint arcW, jint arcH)
{
#ifndef HEADLESS
    long ty1, ty2, tx1, tx2, cx, cy, cxw, cyh,
         halfW, halfH, leftW, rightW, topH, bottomH;
    X11SDOps *xsdo = (X11SDOps *) pXSData;

    if (xsdo == NULL || w < 0 || h < 0) {
        return;
    }

    arcW = ABS(arcW);
    arcH = ABS(arcH);
    if (arcW > w) {
        arcW = w;
    }
    if (arcH > h) {
        arcH = h;
    }

    if (arcW == 0 || arcH == 0) {
        Java_sun_java2d_x11_X11Renderer_XDrawRect(env, xr, pXSData, xgc,
                                                  x, y, w, h);
        return;
    }

    halfW = (arcW / 2);
    halfH = (arcH / 2);

    /* clamp to short bounding box of round rectangle */
    cx = CLAMP_TO_SHORT(x);
    cy = CLAMP_TO_SHORT(y);
    cxw = CLAMP_TO_SHORT(x + w);
    cyh = CLAMP_TO_SHORT(y + h);

    /* clamp to short coordinates of lines */
    tx1 = CLAMP_TO_SHORT(x + halfW + 1);
    tx2 = CLAMP_TO_SHORT(x + w - halfW - 1);
    ty1 = CLAMP_TO_SHORT(y + halfH + 1);
    ty2 = CLAMP_TO_SHORT(y + h - halfH - 1);

    /*
     * recalculate heightes and widthes of round parts
     * to minimize distortions in visible area
     */
    leftW = (tx1 - cx) * 2;
    rightW = (cxw - tx2) * 2;
    topH = (ty1 - cy) * 2;
    bottomH = (cyh - ty2) * 2;

    awt_drawArc(env, xsdo->drawable, (GC) xgc,
                cx, cy, leftW, topH,
                90, 90, JNI_FALSE);
    awt_drawArc(env, xsdo->drawable, (GC) xgc,
                cxw - rightW, cy, rightW, topH,
                0, 90, JNI_FALSE);
    awt_drawArc(env, xsdo->drawable, (GC) xgc,
                cx, cyh - bottomH, leftW, bottomH,
                180, 90, JNI_FALSE);
    awt_drawArc(env, xsdo->drawable, (GC) xgc,
                cxw - rightW, cyh - bottomH, rightW, bottomH,
                270, 90, JNI_FALSE);

    if (tx1 <= tx2) {
        XDrawLine(awt_display, xsdo->drawable, (GC) xgc,
                  tx1, cy, tx2, cy);
        if (h > 0) {
            XDrawLine(awt_display, xsdo->drawable, (GC) xgc,
                      tx1, cyh, tx2, cyh);
        }
    }
    if (ty1 <= ty2) {
        XDrawLine(awt_display, xsdo->drawable, (GC) xgc,
                  cx, ty1, cx, ty2);
        if (w > 0) {
            XDrawLine(awt_display, xsdo->drawable, (GC) xgc,
                      cxw, ty1, cxw, ty2);
        }
    }
    X11SD_DirectRenderNotify(env, xsdo);
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11Renderer
 * Method:    XDrawOval
 * Signature: (IJIIII)V
 */
JNIEXPORT void JNICALL Java_sun_java2d_x11_X11Renderer_XDrawOval
    (JNIEnv *env, jobject xr,
     jlong pXSData, jlong xgc,
     jint x, jint y, jint w, jint h)
{
#ifndef HEADLESS
    X11SDOps *xsdo = (X11SDOps *) pXSData;

    if (xsdo == NULL) {
        return;
    }

    if (w < 2 || h < 2) {
        /*
         * Fix for 4205762 - 1x1 ovals do not draw on Ultra1, Creator3d
         * (related to 4411814 on Windows platform)
         * Really small ovals degenerate to simple rectangles as they
         * have no curvature or enclosed area.  Use XFillRectangle
         * for speed and to deal better with degenerate sizes.
         */
        if (w >= 0 && h >= 0) {
            XFillRectangle(awt_display, xsdo->drawable, (GC) xgc,
                           x, y, w+1, h+1);
        }
    } else {
        awt_drawArc(env, xsdo->drawable, (GC) xgc,
                    x, y, w, h, 0, 360, JNI_FALSE);
    }
    X11SD_DirectRenderNotify(env, xsdo);
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11Renderer
 * Method:    XDrawArc
 * Signature: (IJIIIIII)V
 */
JNIEXPORT void JNICALL Java_sun_java2d_x11_X11Renderer_XDrawArc
    (JNIEnv *env, jobject xr,
     jlong pXSData, jlong xgc,
     jint x, jint y, jint w, jint h,
     jint angleStart, jint angleExtent)
{
#ifndef HEADLESS
    X11SDOps *xsdo = (X11SDOps *) pXSData;

    if (xsdo == NULL) {
        return;
    }

    awt_drawArc(env, xsdo->drawable, (GC) xgc,
                x, y, w, h, angleStart, angleExtent, JNI_FALSE);
    X11SD_DirectRenderNotify(env, xsdo);
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11Renderer
 * Method:    XDrawPoly
 * Signature: (IJII[I[IIZ)V
 */
JNIEXPORT void JNICALL Java_sun_java2d_x11_X11Renderer_XDrawPoly
    (JNIEnv *env, jobject xr,
     jlong pXSData, jlong xgc,
     jint transx, jint transy,
     jintArray xcoordsArray, jintArray ycoordsArray, jint npoints,
     jboolean isclosed)
{
#ifndef HEADLESS
    XPoint pTmp[POLYTEMPSIZE], *points;
    X11SDOps *xsdo = (X11SDOps *) pXSData;

    if (xsdo == NULL) {
        return;
    }

    if (JNU_IsNull(env, xcoordsArray) || JNU_IsNull(env, ycoordsArray)) {
        JNU_ThrowNullPointerException(env, "coordinate array");
        return;
    }
    if ((*env)->GetArrayLength(env, ycoordsArray) < npoints ||
        (*env)->GetArrayLength(env, xcoordsArray) < npoints)
    {
        JNU_ThrowArrayIndexOutOfBoundsException(env, "coordinate array");
        return;
    }

    if (npoints < 2) {
        return;
    }

    points = transformPoints(env, xcoordsArray, ycoordsArray, transx, transy,
                             pTmp, (int *)&npoints, isclosed);
    if (points != 0) {
        if (npoints == 2) {
            /*
             * Some X11 implementations fail to draw anything for
             * simple 2 point polygons where the vertices are the
             * same point even though this violates the X11
             * specification.  For simplicity we will dispatch all
             * 2 point polygons through XDrawLine even if they are
             * non-degenerate as this may invoke less processing
             * down the line than a Poly primitive anyway.
             */
            XDrawLine(awt_display, xsdo->drawable, (GC) xgc,
                      points[0].x, points[0].y,
                      points[1].x, points[1].y);
        } else {
            XDrawLines(awt_display, xsdo->drawable, (GC) xgc,
                       points, npoints, CoordModeOrigin);
        }
        if (points != pTmp) {
            free(points);
        }
        X11SD_DirectRenderNotify(env, xsdo);
    }
#endif /* !HEADLESS */
}

static void storeLine(DrawHandler* hnd,
                      jint x0, jint y0, jint x1, jint y1)
{
#ifndef HEADLESS
    XDrawHandlerData* dhnd = (XDrawHandlerData*)(hnd->pData);

    XDHD_ADD_POINT(dhnd, x0, y0);
    XDHD_ADD_POINT(dhnd, x1, y1);
#endif /* !HEADLESS */
}

static void storePoint(DrawHandler* hnd, jint x0, jint y0) {
#ifndef HEADLESS
    XDrawHandlerData* dhnd = (XDrawHandlerData*)(hnd->pData);

    XDHD_ADD_POINT(dhnd, x0, y0);
#endif /* !HEADLESS */
}

static void drawSubPath(ProcessHandler* hnd) {
#ifndef HEADLESS
    XDrawHandlerData* dhnd = (XDrawHandlerData*)(hnd->dhnd->pData);
    XPoint *points = dhnd->pPoints;

    switch (dhnd->npoints) {
    case 0:
        /* No-op */
        break;
    case 1:
        /* Draw the single pixel */
        XFillRectangle(awt_display, dhnd->drawable, dhnd->gc,
                       points[0].x, points[0].y, 1, 1);
        break;
    case 2:
        /*
         * The XDrawLines method for some X11 implementations
         * fails to draw anything for simple 2 point polygons
         * where the vertices are the same point even though
         * this violates the X11 specification.  For simplicity
         * we will dispatch all 2 point polygons through XDrawLine
         * even if they are non-degenerate as this may invoke
         * less processing down the line than a poly primitive anyway.
         */
        XDrawLine(awt_display, dhnd->drawable, dhnd->gc,
                  points[0].x, points[0].y,
                  points[1].x, points[1].y);
        break;
    default:
        /* Draw the entire polyline */
        XDrawLines(awt_display, dhnd->drawable, dhnd->gc, points,
                   dhnd->npoints, CoordModeOrigin);
        break;
    }

    XDHD_RESET(dhnd);
#endif /* !HEADLESS */
}

static void drawScanline(DrawHandler* hnd, jint x0, jint x1, jint y0)
{
#ifndef HEADLESS
    XDrawHandlerData* dhnd = (XDrawHandlerData*)(hnd->pData);

    XDrawLine(awt_display, dhnd->drawable, dhnd->gc, x0, y0, x1, y0);
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11Renderer
 * Method:    XDoPath
 * Signature: (Lsun/java2d/SunGraphics2D;JJIILjava/awt/geom/Path2D/Float;Z)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_x11_X11Renderer_XDoPath
    (JNIEnv *env, jobject self, jobject sg2d, jlong pXSData, jlong xgc,
     jint transX, jint transY, jobject p2df, jboolean isFill)
{
#ifndef HEADLESS
    X11SDOps *xsdo = (X11SDOps *) pXSData;
    jarray typesArray;
    jobject pointArray;
    jarray coordsArray;
    jint numTypes;
    jint fillRule;
    jint maxCoords;
    jbyte *types;
    jfloat *coords;
    XDrawHandlerData dHData;
    DrawHandler drawHandler = {
        NULL, NULL, NULL,
        MIN_SHORT, MIN_SHORT, MAX_SHORT, MAX_SHORT,
        0, 0, 0, 0,
        NULL
    };
    PHStroke stroke;
    jboolean ok = JNI_TRUE;

    if (xsdo == NULL) {
        return;
    }

    if (isFill) {
        fillRule = (*env)->GetIntField(env, p2df, path2DWindingRuleID);
    }

    typesArray = (jarray)(*env)->GetObjectField(env, p2df, path2DTypesID);
    coordsArray = (jarray)(*env)->GetObjectField(env, p2df,
                                                 path2DFloatCoordsID);
    if (coordsArray == NULL) {
        JNU_ThrowNullPointerException(env, "coordinates array");
        return;
    }
    numTypes = (*env)->GetIntField(env, p2df, path2DNumTypesID);
    if ((*env)->GetArrayLength(env, typesArray) < numTypes) {
        JNU_ThrowArrayIndexOutOfBoundsException(env, "types array");
        return;
    }

    XDHD_INIT(&dHData, (GC)xgc, xsdo->drawable);
    drawHandler.pData = &dHData;

    stroke = (((*env)->GetIntField(env, sg2d, sg2dStrokeHintID) ==
               sunHints_INTVAL_STROKE_PURE)
              ? PH_STROKE_PURE
              : PH_STROKE_DEFAULT);

    maxCoords = (*env)->GetArrayLength(env, coordsArray);
    coords = (jfloat*)
        (*env)->GetPrimitiveArrayCritical(env, coordsArray, NULL);
    if (coords != NULL) {
        types = (jbyte*)
            (*env)->GetPrimitiveArrayCritical(env, typesArray, NULL);
        if (types != NULL) {
            if (isFill) {
                drawHandler.pDrawScanline = &drawScanline;
                ok = doFillPath(&drawHandler,
                                transX, transY,
                                coords, maxCoords,
                                types, numTypes,
                                stroke, fillRule);
            } else {
                drawHandler.pDrawLine = &storeLine;
                drawHandler.pDrawPixel = &storePoint;
                ok = doDrawPath(&drawHandler, &drawSubPath,
                                transX, transY,
                                coords, maxCoords,
                                types, numTypes,
                                stroke);
            }
            (*env)->ReleasePrimitiveArrayCritical(env, typesArray, types,
                                                  JNI_ABORT);
        }
        (*env)->ReleasePrimitiveArrayCritical(env, coordsArray, coords,
                                              JNI_ABORT);
        if (!ok) {
            JNU_ThrowArrayIndexOutOfBoundsException(env, "coords array");
        }
    }

    XDHD_FREE_POINTS(&dHData);
    X11SD_DirectRenderNotify(env, xsdo);
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11Renderer
 * Method:    XFillRect
 * Signature: (IJIIII)V
 */
JNIEXPORT void JNICALL Java_sun_java2d_x11_X11Renderer_XFillRect
    (JNIEnv *env, jobject xr,
     jlong pXSData, jlong xgc,
     jint x, jint y, jint w, jint h)
{
#ifndef HEADLESS
    X11SDOps *xsdo = (X11SDOps *) pXSData;

    if (xsdo == NULL) {
        return;
    }

    XFillRectangle(awt_display, xsdo->drawable, (GC) xgc,
                   CLAMP_TO_SHORT(x),  CLAMP_TO_SHORT(y),
                   CLAMP_TO_USHORT(w), CLAMP_TO_USHORT(h));
    X11SD_DirectRenderNotify(env, xsdo);
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11Renderer
 * Method:    XFillRoundRect
 * Signature: (IJIIIIII)V
 */
JNIEXPORT void JNICALL Java_sun_java2d_x11_X11Renderer_XFillRoundRect
    (JNIEnv *env, jobject xr,
     jlong pXSData, jlong xgc,
     jint x, jint y, jint w, jint h,
     jint arcW, jint arcH)
{
#ifndef HEADLESS
    long ty1, ty2, tx1, tx2, cx, cy, cxw, cyh,
         halfW, halfH, leftW, rightW, topH, bottomH;
    X11SDOps *xsdo = (X11SDOps *) pXSData;

    if (xsdo == NULL || w <= 0 || h <= 0) {
        return;
    }

    arcW = ABS(arcW);
    arcH = ABS(arcH);
    if (arcW > w) {
        arcW = w;
    }
    if (arcH > h) {
        arcH = h;
    }

    if (arcW == 0 || arcH == 0) {
        Java_sun_java2d_x11_X11Renderer_XFillRect(env, xr, pXSData, xgc,
                                                  x, y, w, h);
        return;
    }

    halfW = (arcW / 2);
    halfH = (arcH / 2);

    /* clamp to short bounding box of round rectangle */
    cx = CLAMP_TO_SHORT(x);
    cy = CLAMP_TO_SHORT(y);
    cxw = CLAMP_TO_SHORT(x + w);
    cyh = CLAMP_TO_SHORT(y + h);

    /* clamp to short coordinates of lines */
    tx1 = CLAMP_TO_SHORT(x + halfW + 1);
    tx2 = CLAMP_TO_SHORT(x + w - halfW - 1);
    ty1 = CLAMP_TO_SHORT(y + halfH + 1);
    ty2 = CLAMP_TO_SHORT(y + h - halfH - 1);

    /*
     * recalculate heightes and widthes of round parts
     * to minimize distortions in visible area
     */
    leftW = (tx1 - cx) * 2;
    rightW = (cxw - tx2) * 2;
    topH = (ty1 - cy) * 2;
    bottomH = (cyh - ty2) * 2;

    awt_drawArc(env, xsdo->drawable, (GC) xgc,
                cx, cy, leftW, topH,
                90, 90, JNI_TRUE);
    awt_drawArc(env, xsdo->drawable, (GC) xgc,
                cxw - rightW, cy, rightW, topH,
                0, 90, JNI_TRUE);
    awt_drawArc(env, xsdo->drawable, (GC) xgc,
                cx, cyh - bottomH, leftW, bottomH,
                180, 90, JNI_TRUE);
    awt_drawArc(env, xsdo->drawable, (GC) xgc,
                cxw - rightW, cyh - bottomH, rightW, bottomH,
                270, 90, JNI_TRUE);

    if (tx1 < tx2) {
        if (cy < ty1) {
            XFillRectangle(awt_display, xsdo->drawable, (GC) xgc,
                           tx1, cy, tx2 - tx1, ty1 - cy);
        }
        if (ty2 < cyh) {
            XFillRectangle(awt_display, xsdo->drawable, (GC) xgc,
                           tx1, ty2, tx2 - tx1, cyh - ty2);
        }
    }
    if (ty1 < ty2) {
        XFillRectangle(awt_display, xsdo->drawable, (GC) xgc,
                       cx, ty1, cxw - cx, ty2 - ty1);
    }
    X11SD_DirectRenderNotify(env, xsdo);
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11Renderer
 * Method:    XFillOval
 * Signature: (IJIIII)V
 */
JNIEXPORT void JNICALL Java_sun_java2d_x11_X11Renderer_XFillOval
    (JNIEnv *env, jobject xr,
     jlong pXSData, jlong xgc,
     jint x, jint y, jint w, jint h)
{
#ifndef HEADLESS
    X11SDOps *xsdo = (X11SDOps *) pXSData;

    if (xsdo == NULL) {
        return;
    }

    if (w < 3 || h < 3) {
        /*
         * Fix for 4205762 - 1x1 ovals do not draw on Ultra1, Creator3d
         * (related to 4411814 on Windows platform)
         * Most X11 servers drivers have poor rendering
         * for thin ellipses and the rendering is most strikingly
         * different from our theoretical arcs.  Ideally we should
         * trap all ovals less than some fairly large size and
         * try to draw aesthetically pleasing ellipses, but that
         * would require considerably more work to get the corresponding
         * drawArc variants to match pixel for pixel.
         * Thin ovals of girth 1 pixel are simple rectangles.
         * Thin ovals of girth 2 pixels are simple rectangles with
         * potentially smaller lengths.  Determine the correct length
         * by calculating .5*.5 + scaledlen*scaledlen == 1.0 which
         * means that scaledlen is the sqrt(0.75).  Scaledlen is
         * relative to the true length (w or h) and needs to be
         * adjusted by half a pixel in different ways for odd or
         * even lengths.
         */
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
            XFillRectangle(awt_display, xsdo->drawable, (GC) xgc, x, y, w, h);
        }
    } else {
        awt_drawArc(env, xsdo->drawable, (GC) xgc,
                    x, y, w, h, 0, 360, JNI_TRUE);
    }
    X11SD_DirectRenderNotify(env, xsdo);
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11Renderer
 * Method:    XFillArc
 * Signature: (IJIIIIII)V
 */
JNIEXPORT void JNICALL Java_sun_java2d_x11_X11Renderer_XFillArc
    (JNIEnv *env, jobject xr,
     jlong pXSData, jlong xgc,
     jint x, jint y, jint w, jint h,
     jint angleStart, jint angleExtent)
{
#ifndef HEADLESS
    X11SDOps *xsdo = (X11SDOps *) pXSData;

    if (xsdo == NULL) {
        return;
    }

    awt_drawArc(env, xsdo->drawable, (GC) xgc,
                x, y, w, h, angleStart, angleExtent, JNI_TRUE);
    X11SD_DirectRenderNotify(env, xsdo);
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11Renderer
 * Method:    XFillPoly
 * Signature: (IJII[I[II)V
 */
JNIEXPORT void JNICALL Java_sun_java2d_x11_X11Renderer_XFillPoly
    (JNIEnv *env, jobject xr,
     jlong pXSData, jlong xgc,
     jint transx, jint transy,
     jintArray xcoordsArray, jintArray ycoordsArray, jint npoints)
{
#ifndef HEADLESS
    XPoint pTmp[POLYTEMPSIZE], *points;
    X11SDOps *xsdo = (X11SDOps *) pXSData;

    if (xsdo == NULL) {
        return;
    }

    if (JNU_IsNull(env, xcoordsArray) || JNU_IsNull(env, ycoordsArray)) {
        JNU_ThrowNullPointerException(env, "coordinate array");
        return;
    }
    if ((*env)->GetArrayLength(env, ycoordsArray) < npoints ||
        (*env)->GetArrayLength(env, xcoordsArray) < npoints)
    {
        JNU_ThrowArrayIndexOutOfBoundsException(env, "coordinate array");
        return;
    }

    if (npoints < 3) {
        return;
    }

    points = transformPoints(env, xcoordsArray, ycoordsArray, transx, transy,
                             pTmp, (int *)&npoints, JNI_FALSE);
    if (points != 0) {
        if (npoints > 2) {
            XFillPolygon(awt_display, xsdo->drawable, (GC) xgc,
                         points, npoints, Complex, CoordModeOrigin);
            X11SD_DirectRenderNotify(env, xsdo);
        }
        if (points != pTmp) {
            free(points);
        }
    }
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11Renderer
 * Method:    XFillSpans
 * Signature: (IJLsun/java2d/pipe/SpanIterator;JII)V
 */
JNIEXPORT void JNICALL Java_sun_java2d_x11_X11Renderer_XFillSpans
    (JNIEnv *env, jobject xr,
     jlong pXSData, jlong xgc,
     jobject si, jlong pIterator,
     jint transx, jint transy)
{
#ifndef HEADLESS
    SpanIteratorFuncs *pFuncs = (SpanIteratorFuncs *) jlong_to_ptr(pIterator);
    void *srData;
    jint x, y, w, h;
    jint spanbox[4];
    X11SDOps *xsdo = (X11SDOps *) pXSData;

    if (xsdo == NULL) {
        return;
    }

    if (JNU_IsNull(env, si)) {
        JNU_ThrowNullPointerException(env, "span iterator");
        return;
    }
    if (pFuncs == NULL) {
        JNU_ThrowNullPointerException(env, "native iterator not supplied");
        return;
    }

    srData = (*pFuncs->open)(env, si);
    while ((*pFuncs->nextSpan)(srData, spanbox)) {
        x = spanbox[0] + transx;
        y = spanbox[1] + transy;
        w = spanbox[2] - spanbox[0];
        h = spanbox[3] - spanbox[1];
        XFillRectangle(awt_display, xsdo->drawable, (GC) xgc,
                       CLAMP_TO_SHORT(x),  CLAMP_TO_SHORT(y),
                       CLAMP_TO_USHORT(w), CLAMP_TO_USHORT(h));
    }
    (*pFuncs->close)(env, srData);
    X11SD_DirectRenderNotify(env, xsdo);
#endif /* !HEADLESS */
}

/*
 * Class:     sun_java2d_x11_X11Renderer
 * Method:    devCopyArea
 * Signature: (Lsun/java2d/SurfaceData;IIIIII)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_x11_X11Renderer_devCopyArea
    (JNIEnv *env, jobject xr,
     jlong xsd, jlong gc,
     jint srcx, jint srcy,
     jint dstx, jint dsty,
     jint width, jint height)
{
#ifndef HEADLESS
    X11SDOps *xsdo;
    GC xgc;

    xsdo = (X11SDOps *)jlong_to_ptr(xsd);
    if (xsdo == NULL) {
        return;
    }

    xgc = (GC)gc;
    if (xgc == NULL) {
        return;
    }

    XCopyArea(awt_display, xsdo->drawable, xsdo->drawable, xgc,
              srcx, srcy, width, height, dstx, dsty);

    X11SD_DirectRenderNotify(env, xsdo);
#endif /* !HEADLESS */
}
