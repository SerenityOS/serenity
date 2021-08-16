/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include <string.h>
#include <math.h>

#include "jni.h"
#include "jni_util.h"
#include <jlong.h>

#include "j2d_md.h"

#include "PathConsumer2D.h"
#include "SpanIterator.h"

#include "sun_java2d_pipe_ShapeSpanIterator.h"
#include "java_awt_geom_PathIterator.h"

/*
 * This structure holds all of the information needed to trace and
 * manage a single line segment of the shape's outline.
 */
typedef struct {
    jint curx;
    jint cury;
    jint lasty;
    jint error;
    jint bumpx;
    jint bumperr;
    jbyte windDir;
    jbyte pad0;
    jbyte pad1;
    jbyte pad2;
} segmentData;

/*
 * This structure holds all of the information needed to trace out
 * the entire span list of a single Shape object.
 */
typedef struct {
    PathConsumerVec funcs;      /* Native PathConsumer function vector */

    char state;                 /* Path delivery sequence state */
    char evenodd;               /* non-zero if path has EvenOdd winding rule */
    char first;                 /* non-zero if first path segment */
    char adjust;                /* normalize to nearest (0.25, 0.25) */

    jint lox;                   /* clip bbox low X */
    jint loy;                   /* clip bbox low Y */
    jint hix;                   /* clip bbox high X */
    jint hiy;                   /* clip bbox high Y */

    jfloat curx;                /* current path point X coordinate */
    jfloat cury;                /* current path point Y coordinate */
    jfloat movx;                /* last moveto X coordinate */
    jfloat movy;                /* last moveto Y coordinate */

    jfloat adjx;                /* last X coordinate adjustment */
    jfloat adjy;                /* last Y coordinate adjustment */

    jfloat pathlox;             /* lowest X coordinate in path */
    jfloat pathloy;             /* lowest Y coordinate in path */
    jfloat pathhix;             /* highest X coordinate in path */
    jfloat pathhiy;             /* highest Y coordinate in path */

    segmentData *segments;      /* pointer to array of path segments */
    int numSegments;            /* number of segments entries in array */
    int segmentsSize;           /* size of array of path segments */

    int lowSegment;             /* lower limit of segments in active range */
    int curSegment;             /* index of next active segment to return */
    int hiSegment;              /* upper limit of segments in active range */

    segmentData **segmentTable; /* pointers to segments being stepped */
} pathData;

#define STATE_INIT              0
#define STATE_HAVE_CLIP         1
#define STATE_HAVE_RULE         2
#define STATE_PATH_DONE         3
#define STATE_SPAN_STARTED      4

static jboolean subdivideLine(pathData *pd, int level,
                              jfloat x0, jfloat y0,
                              jfloat x1, jfloat y1);
static jboolean subdivideQuad(pathData *pd, int level,
                              jfloat x0, jfloat y0,
                              jfloat x1, jfloat y1,
                              jfloat x2, jfloat y2);
static jboolean subdivideCubic(pathData *pd, int level,
                               jfloat x0, jfloat y0,
                               jfloat x1, jfloat y1,
                               jfloat x2, jfloat y2,
                               jfloat x3, jfloat y3);
static jboolean appendSegment(pathData *pd,
                              jfloat x0, jfloat y0,
                              jfloat x1, jfloat y1);
static jboolean initSegmentTable(pathData *pd);

static void *ShapeSIOpen(JNIEnv *env, jobject iterator);
static void ShapeSIClose(JNIEnv *env, void *private);
static void ShapeSIGetPathBox(JNIEnv *env, void *private, jint pathbox[]);
static void ShapeSIIntersectClipBox(JNIEnv *env, void *private,
                                        jint lox, jint loy, jint hix, jint hiy);
static jboolean ShapeSINextSpan(void *state, jint spanbox[]);
static void ShapeSISkipDownTo(void *private, jint y);

static jfieldID pSpanDataID;

static SpanIteratorFuncs ShapeSIFuncs = {
    ShapeSIOpen,
    ShapeSIClose,
    ShapeSIGetPathBox,
    ShapeSIIntersectClipBox,
    ShapeSINextSpan,
    ShapeSISkipDownTo
};

static LineToFunc PCLineTo;
static MoveToFunc PCMoveTo;
static QuadToFunc PCQuadTo;
static CubicToFunc PCCubicTo;
static ClosePathFunc PCClosePath;
static PathDoneFunc PCPathDone;

#define PDBOXPOINT(pd, x, y)                                    \
    do {                                                        \
        if (pd->first) {                                        \
            pd->pathlox = pd->pathhix = x;                      \
            pd->pathloy = pd->pathhiy = y;                      \
            pd->first = 0;                                      \
        } else {                                                \
            if (pd->pathlox > x) pd->pathlox = x;               \
            if (pd->pathloy > y) pd->pathloy = y;               \
            if (pd->pathhix < x) pd->pathhix = x;               \
            if (pd->pathhiy < y) pd->pathhiy = y;               \
        }                                                       \
    } while (0)

/*
 * _ADJUST is the internal macro used to adjust a new endpoint
 * and then invoke the "additional" code from the callers below
 * which will adjust the control points as needed to match.
 *
 * When the "additional" code is executed, newadj[xy] will
 * contain the adjustment applied to the new endpoint and
 * pd->adj[xy] will still contain the previous adjustment
 * that was applied to the old endpoint.
 */
#define _ADJUST(pd, x, y, additional)                           \
    do {                                                        \
        if (pd->adjust) {                                       \
            jfloat newx = (jfloat) floor(x + 0.25f) + 0.25f;    \
            jfloat newy = (jfloat) floor(y + 0.25f) + 0.25f;    \
            jfloat newadjx = newx - x;                          \
            jfloat newadjy = newy - y;                          \
            x = newx;                                           \
            y = newy;                                           \
            additional;                                         \
            pd->adjx = newadjx;                                 \
            pd->adjy = newadjy;                                 \
        }                                                       \
    } while (0)

/*
 * Adjust a single endpoint with no control points.
 * "additional" code is a null statement.
 */
#define ADJUST1(pd, x1, y1)                                     \
    _ADJUST(pd, x1, y1,                                         \
            do {                                                \
            } while (0))

/*
 * Adjust a quadratic curve.  The _ADJUST macro takes care
 * of the new endpoint and the "additional" code adjusts
 * the single quadratic control point by the averge of
 * the prior and the new adjustment amounts.
 */
#define ADJUST2(pd, x1, y1, x2, y2)                             \
    _ADJUST(pd, x2, y2,                                         \
            do {                                                \
                x1 += (pd->adjx + newadjy) / 2;                 \
                y1 += (pd->adjy + newadjy) / 2;                 \
            } while (0))

/*
 * Adjust a cubic curve.  The _ADJUST macro takes care
 * of the new endpoint and the "additional" code adjusts
 * the first of the two cubic control points by the same
 * amount that the prior endpoint was adjusted and then
 * adjusts the second of the two control points by the
 * same amount as the new endpoint was adjusted.  This
 * keeps the tangent lines from xy0 to xy1 and xy3 to xy2
 * parallel before and after the adjustment.
 */
#define ADJUST3(pd, x1, y1, x2, y2, x3, y3)                     \
    _ADJUST(pd, x3, y3,                                         \
            do {                                                \
                x1 += pd->adjx;                                 \
                y1 += pd->adjy;                                 \
                x2 += newadjx;                                  \
                y2 += newadjy;                                  \
            } while (0))

#define HANDLEMOVETO(pd, x0, y0, OOMERR)                        \
    do {                                                        \
        HANDLECLOSE(pd, OOMERR);                                \
        ADJUST1(pd, x0, y0);                                    \
        pd->movx = x0;                                          \
        pd->movy = y0;                                          \
        PDBOXPOINT(pd, x0, y0);                                 \
        pd->curx = x0;                                          \
        pd->cury = y0;                                          \
    } while (0)

#define HANDLELINETO(pd, x1, y1, OOMERR)                        \
    do {                                                        \
        ADJUST1(pd, x1, y1);                                    \
        if (!subdivideLine(pd, 0,                               \
                           pd->curx, pd->cury,                  \
                           x1, y1)) {                           \
            OOMERR;                                             \
            break;                                              \
        }                                                       \
        PDBOXPOINT(pd, x1, y1);                                 \
        pd->curx = x1;                                          \
        pd->cury = y1;                                          \
    } while (0)

#define HANDLEQUADTO(pd, x1, y1, x2, y2, OOMERR)                \
    do {                                                        \
        ADJUST2(pd, x1, y1, x2, y2);                            \
        if (!subdivideQuad(pd, 0,                               \
                           pd->curx, pd->cury,                  \
                           x1, y1, x2, y2)) {                   \
            OOMERR;                                             \
            break;                                              \
        }                                                       \
        PDBOXPOINT(pd, x1, y1);                                 \
        PDBOXPOINT(pd, x2, y2);                                 \
        pd->curx = x2;                                          \
        pd->cury = y2;                                          \
    } while (0)

#define HANDLECUBICTO(pd, x1, y1, x2, y2, x3, y3, OOMERR)       \
    do {                                                        \
        ADJUST3(pd, x1, y1, x2, y2, x3, y3);                    \
        if (!subdivideCubic(pd, 0,                              \
                            pd->curx, pd->cury,                 \
                            x1, y1, x2, y2, x3, y3)) {          \
            OOMERR;                                             \
            break;                                              \
        }                                                       \
        PDBOXPOINT(pd, x1, y1);                                 \
        PDBOXPOINT(pd, x2, y2);                                 \
        PDBOXPOINT(pd, x3, y3);                                 \
        pd->curx = x3;                                          \
        pd->cury = y3;                                          \
    } while (0)

#define HANDLECLOSE(pd, OOMERR)                                 \
    do {                                                        \
        if (pd->curx != pd->movx || pd->cury != pd->movy) {     \
            if (!subdivideLine(pd, 0,                           \
                               pd->curx, pd->cury,              \
                               pd->movx, pd->movy)) {           \
                OOMERR;                                         \
                break;                                          \
            }                                                   \
            pd->curx = pd->movx;                                \
            pd->cury = pd->movy;                                \
        }                                                       \
    } while (0)

#define HANDLEENDPATH(pd, OOMERR)                               \
    do {                                                        \
        HANDLECLOSE(pd, OOMERR);                                \
        pd->state = STATE_PATH_DONE;                            \
    } while (0)

static pathData *
GetSpanData(JNIEnv *env, jobject sr, int minState, int maxState)
{
    pathData *pd = (pathData *) JNU_GetLongFieldAsPtr(env, sr, pSpanDataID);

    if (pd == NULL) {
        JNU_ThrowNullPointerException(env, "private data");
    } else if (pd->state < minState || pd->state > maxState) {
        JNU_ThrowInternalError(env, "bad path delivery sequence");
        pd = NULL;
    }

    return pd;
}

static pathData *
MakeSpanData(JNIEnv *env, jobject sr)
{
    pathData *pd = (pathData *) JNU_GetLongFieldAsPtr(env, sr, pSpanDataID);

    if (pd != NULL) {
        JNU_ThrowInternalError(env, "private data already initialized");
        return NULL;
    }

    pd = calloc(1, sizeof(pathData));

    if (pd == NULL) {
        JNU_ThrowOutOfMemoryError(env, "private data");
    } else {
        /* Initialize PathConsumer2D struct header */
        pd->funcs.moveTo = PCMoveTo;
        pd->funcs.lineTo = PCLineTo;
        pd->funcs.quadTo = PCQuadTo;
        pd->funcs.cubicTo = PCCubicTo;
        pd->funcs.closePath = PCClosePath;
        pd->funcs.pathDone = PCPathDone;

        /* Initialize ShapeSpanIterator fields */
        pd->first = 1;

        (*env)->SetLongField(env, sr, pSpanDataID, ptr_to_jlong(pd));
    }

    return pd;
}

JNIEXPORT void JNICALL
Java_sun_java2d_pipe_ShapeSpanIterator_initIDs
    (JNIEnv *env, jclass src)
{
    pSpanDataID = (*env)->GetFieldID(env, src, "pData", "J");
}

/*
 * Class:     sun_java2d_pipe_ShapeSpanIterator
 * Method:    setNormalize
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_pipe_ShapeSpanIterator_setNormalize
    (JNIEnv *env, jobject sr, jboolean adjust)
{
    pathData *pd;

    pd = MakeSpanData(env, sr);
    if (pd == NULL) {
        return;
    }

    pd->adjust = adjust;
}

JNIEXPORT void JNICALL
Java_sun_java2d_pipe_ShapeSpanIterator_setOutputAreaXYXY
    (JNIEnv *env, jobject sr, jint lox, jint loy, jint hix, jint hiy)
{
    pathData *pd;

    pd = GetSpanData(env, sr, STATE_INIT, STATE_INIT);
    if (pd == NULL) {
        return;
    }

    pd->lox = lox;
    pd->loy = loy;
    pd->hix = hix;
    pd->hiy = hiy;
    pd->state = STATE_HAVE_CLIP;
}

JNIEXPORT void JNICALL
Java_sun_java2d_pipe_ShapeSpanIterator_setRule
    (JNIEnv *env, jobject sr, jint rule)
{
    pathData *pd;

    pd = GetSpanData(env, sr, STATE_HAVE_CLIP, STATE_HAVE_CLIP);
    if (pd == NULL) {
        return;
    }

    pd->evenodd = (rule == java_awt_geom_PathIterator_WIND_EVEN_ODD);
    pd->state = STATE_HAVE_RULE;
}

JNIEXPORT void JNICALL
Java_sun_java2d_pipe_ShapeSpanIterator_addSegment
    (JNIEnv *env, jobject sr, jint type, jfloatArray coordObj)
{
    jfloat coords[6];
    jfloat x1, y1, x2, y2, x3, y3;
    jboolean oom = JNI_FALSE;
    pathData *pd;
    int numpts = 0;

    pd = GetSpanData(env, sr, STATE_HAVE_RULE, STATE_HAVE_RULE);
    if (pd == NULL) {
        return;
    }

    (*env)->GetFloatArrayRegion(env, coordObj, 0, 6, coords);
    if ((*env)->ExceptionCheck(env)) {
        return;
    }

    switch (type) {
    case java_awt_geom_PathIterator_SEG_MOVETO:
        x1 = coords[0]; y1 = coords[1];
        HANDLEMOVETO(pd, x1, y1, {oom = JNI_TRUE;});
        break;
    case java_awt_geom_PathIterator_SEG_LINETO:
        x1 = coords[0]; y1 = coords[1];
        HANDLELINETO(pd, x1, y1, {oom = JNI_TRUE;});
        break;
    case java_awt_geom_PathIterator_SEG_QUADTO:
        x1 = coords[0]; y1 = coords[1];
        x2 = coords[2]; y2 = coords[3];
        HANDLEQUADTO(pd, x1, y1, x2, y2, {oom = JNI_TRUE;});
        break;
    case java_awt_geom_PathIterator_SEG_CUBICTO:
        x1 = coords[0]; y1 = coords[1];
        x2 = coords[2]; y2 = coords[3];
        x3 = coords[4]; y3 = coords[5];
        HANDLECUBICTO(pd, x1, y1, x2, y2, x3, y3, {oom = JNI_TRUE;});
        break;
    case java_awt_geom_PathIterator_SEG_CLOSE:
        HANDLECLOSE(pd, {oom = JNI_TRUE;});
        break;
    default:
        JNU_ThrowInternalError(env, "bad path segment type");
        return;
    }

    if (oom) {
        JNU_ThrowOutOfMemoryError(env, "path segment data");
        return;
    }
}

JNIEXPORT void JNICALL
Java_sun_java2d_pipe_ShapeSpanIterator_getPathBox
    (JNIEnv *env, jobject sr, jintArray spanbox)
{
    pathData *pd;
    jint coords[4];

    pd = GetSpanData(env, sr, STATE_PATH_DONE, STATE_PATH_DONE);
    if (pd == NULL) {
        return;
    }

    ShapeSIGetPathBox(env, pd, coords);

    (*env)->SetIntArrayRegion(env, spanbox, 0, 4, coords);
}

JNIEXPORT void JNICALL
Java_sun_java2d_pipe_ShapeSpanIterator_intersectClipBox
    (JNIEnv *env, jobject ri, jint clox, jint cloy, jint chix, jint chiy)
{
    pathData *pd;

    pd = GetSpanData(env, ri, STATE_PATH_DONE, STATE_PATH_DONE);
    if (pd == NULL) {
        return;
    }

    ShapeSIIntersectClipBox(env, pd, clox, cloy, chix, chiy);
}

JNIEXPORT jboolean JNICALL
Java_sun_java2d_pipe_ShapeSpanIterator_nextSpan
    (JNIEnv *env, jobject sr, jintArray spanbox)
{
    pathData *pd;
    jboolean ret;
    jint coords[4];

    pd = GetSpanData(env, sr, STATE_PATH_DONE, STATE_SPAN_STARTED);
    if (pd == NULL) {
        return JNI_FALSE;
    }

    ret = ShapeSINextSpan(pd, coords);
    if (ret) {
        (*env)->SetIntArrayRegion(env, spanbox, 0, 4, coords);
    }

    return ret;
}

JNIEXPORT void JNICALL
Java_sun_java2d_pipe_ShapeSpanIterator_skipDownTo
    (JNIEnv *env, jobject sr, jint y)
{
    pathData *pd;

    pd = GetSpanData(env, sr, STATE_PATH_DONE, STATE_SPAN_STARTED);
    if (pd == NULL) {
        return;
    }

    ShapeSISkipDownTo(pd, y);
}

JNIEXPORT jlong JNICALL
Java_sun_java2d_pipe_ShapeSpanIterator_getNativeIterator
    (JNIEnv *env, jobject sr)
{
    return ptr_to_jlong(&ShapeSIFuncs);
}

JNIEXPORT void JNICALL
Java_sun_java2d_pipe_ShapeSpanIterator_dispose
    (JNIEnv *env, jobject sr)
{
    pathData *pd = (pathData *) JNU_GetLongFieldAsPtr(env, sr, pSpanDataID);

    if (pd == NULL) {
        return;
    }

    if (pd->segments != NULL) {
        free(pd->segments);
    }
    if (pd->segmentTable != NULL) {
        free(pd->segmentTable);
    }
    free(pd);

    (*env)->SetLongField(env, sr, pSpanDataID, jlong_zero);
}

#define OUT_XLO 1
#define OUT_XHI 2
#define OUT_YLO 4
#define OUT_YHI 8

#define CALCULATE_OUTCODES(pd, outc, x, y) \
    do { \
        if (y <= pd->loy) outc = OUT_YLO; \
        else if (y >= pd->hiy) outc = OUT_YHI; \
        else outc = 0; \
        if (x <= pd->lox) outc |= OUT_XLO; \
        else if (x >= pd->hix) outc |= OUT_XHI; \
    } while (0)

JNIEXPORT void JNICALL
Java_sun_java2d_pipe_ShapeSpanIterator_appendPoly
    (JNIEnv *env, jobject sr,
     jintArray xArray, jintArray yArray, jint nPoints,
     jint ixoff, jint iyoff)
{
    pathData *pd;
    int i;
    jint *xPoints, *yPoints;
    jboolean oom = JNI_FALSE;
    jfloat xoff = (jfloat) ixoff, yoff = (jfloat) iyoff;

    pd = GetSpanData(env, sr, STATE_HAVE_CLIP, STATE_HAVE_CLIP);
    if (pd == NULL) {
        return;
    }

    pd->evenodd = JNI_TRUE;
    pd->state = STATE_HAVE_RULE;
    if (pd->adjust) {
        xoff += 0.25f;
        yoff += 0.25f;
    }

    if (xArray == NULL || yArray == NULL) {
        JNU_ThrowNullPointerException(env, "polygon data arrays");
        return;
    }
    if ((*env)->GetArrayLength(env, xArray) < nPoints ||
        (*env)->GetArrayLength(env, yArray) < nPoints)
    {
        JNU_ThrowArrayIndexOutOfBoundsException(env, "polygon data arrays");
        return;
    }

    if (nPoints > 0) {
        xPoints = (*env)->GetPrimitiveArrayCritical(env, xArray, NULL);
        if (xPoints != NULL) {
            yPoints = (*env)->GetPrimitiveArrayCritical(env, yArray, NULL);
            if (yPoints != NULL) {
                jint outc0;
                jfloat x, y;

                x = xPoints[0] + xoff;
                y = yPoints[0] + yoff;
                CALCULATE_OUTCODES(pd, outc0, x, y);
                pd->movx = pd->curx = x;
                pd->movy = pd->cury = y;
                pd->pathlox = pd->pathhix = x;
                pd->pathloy = pd->pathhiy = y;
                pd->first = 0;
                for (i = 1; !oom && i < nPoints; i++) {
                    jint outc1;

                    x = xPoints[i] + xoff;
                    y = yPoints[i] + yoff;
                    if (y == pd->cury) {
                        /* Horizontal segment - do not append */
                        if (x != pd->curx) {
                            /* Not empty segment - track change in X */
                            CALCULATE_OUTCODES(pd, outc0, x, y);
                            pd->curx = x;
                            if (pd->pathlox > x) pd->pathlox = x;
                            if (pd->pathhix < x) pd->pathhix = x;
                        }
                        continue;
                    }
                    CALCULATE_OUTCODES(pd, outc1, x, y);
                    outc0 &= outc1;
                    if (outc0 == 0) {
                        oom = !appendSegment(pd, pd->curx, pd->cury, x, y);
                    } else if (outc0 == OUT_XLO) {
                        oom = !appendSegment(pd, (jfloat) pd->lox, pd->cury,
                                             (jfloat) pd->lox, y);
                    }
                    if (pd->pathlox > x) pd->pathlox = x;
                    if (pd->pathloy > y) pd->pathloy = y;
                    if (pd->pathhix < x) pd->pathhix = x;
                    if (pd->pathhiy < y) pd->pathhiy = y;
                    outc0 = outc1;
                    pd->curx = x;
                    pd->cury = y;
                }
                (*env)->ReleasePrimitiveArrayCritical(env, yArray,
                                                      yPoints, JNI_ABORT);
            }
            (*env)->ReleasePrimitiveArrayCritical(env, xArray,
                                                  xPoints, JNI_ABORT);
        }
        if (xPoints == NULL || yPoints == NULL) {
            return;
        }
    }
    if (!oom) {
        HANDLEENDPATH(pd, {oom = JNI_TRUE;});
    }
    if (oom) {
        JNU_ThrowOutOfMemoryError(env, "path segment data");
    }
}

JNIEXPORT void JNICALL
Java_sun_java2d_pipe_ShapeSpanIterator_moveTo
    (JNIEnv *env, jobject sr, jfloat x0, jfloat y0)
{
    pathData *pd;

    pd = GetSpanData(env, sr, STATE_HAVE_RULE, STATE_HAVE_RULE);
    if (pd == NULL) {
        return;
    }

    HANDLEMOVETO(pd, x0, y0,
                 {JNU_ThrowOutOfMemoryError(env, "path segment data");});
}

JNIEXPORT void JNICALL
Java_sun_java2d_pipe_ShapeSpanIterator_lineTo
    (JNIEnv *env, jobject sr, jfloat x1, jfloat y1)
{
    pathData *pd;

    pd = GetSpanData(env, sr, STATE_HAVE_RULE, STATE_HAVE_RULE);
    if (pd == NULL) {
        return;
    }

    HANDLELINETO(pd, x1, y1,
                 {JNU_ThrowOutOfMemoryError(env, "path segment data");});
}

JNIEXPORT void JNICALL
Java_sun_java2d_pipe_ShapeSpanIterator_quadTo
    (JNIEnv *env, jobject sr,
     jfloat xm, jfloat ym, jfloat x1, jfloat y1)
{
    pathData *pd;

    pd = GetSpanData(env, sr, STATE_HAVE_RULE, STATE_HAVE_RULE);
    if (pd == NULL) {
        return;
    }

    HANDLEQUADTO(pd, xm, ym, x1, y1,
                 {JNU_ThrowOutOfMemoryError(env, "path segment data");});
}

JNIEXPORT void JNICALL
Java_sun_java2d_pipe_ShapeSpanIterator_curveTo
    (JNIEnv *env, jobject sr,
     jfloat xm, jfloat ym,
     jfloat xn, jfloat yn,
     jfloat x1, jfloat y1)
{
    pathData *pd;

    pd = GetSpanData(env, sr, STATE_HAVE_RULE, STATE_HAVE_RULE);
    if (pd == NULL) {
        return;
    }

    HANDLECUBICTO(pd, xm, ym, xn, yn, x1, y1,
                  {JNU_ThrowOutOfMemoryError(env, "path segment data");});
}

JNIEXPORT void JNICALL
Java_sun_java2d_pipe_ShapeSpanIterator_closePath
    (JNIEnv *env, jobject sr)
{
    pathData *pd;

    pd = GetSpanData(env, sr, STATE_HAVE_RULE, STATE_HAVE_RULE);
    if (pd == NULL) {
        return;
    }

    HANDLECLOSE(pd, {JNU_ThrowOutOfMemoryError(env, "path segment data");});
}

JNIEXPORT void JNICALL
Java_sun_java2d_pipe_ShapeSpanIterator_pathDone
    (JNIEnv *env, jobject sr)
{
    pathData *pd;

    pd = GetSpanData(env, sr, STATE_HAVE_RULE, STATE_HAVE_RULE);
    if (pd == NULL) {
        return;
    }

    HANDLEENDPATH(pd, {JNU_ThrowOutOfMemoryError(env, "path segment data");});
}

JNIEXPORT jlong JNICALL
Java_sun_java2d_pipe_ShapeSpanIterator_getNativeConsumer
    (JNIEnv *env, jobject sr)
{
    pathData *pd = GetSpanData(env, sr, STATE_HAVE_RULE, STATE_HAVE_RULE);

    if (pd == NULL) {
        return jlong_zero;
    }

    return ptr_to_jlong(&(pd->funcs));
}

static jboolean
PCMoveTo(PathConsumerVec *consumer,
         jfloat x0, jfloat y0)
{
    pathData *pd = (pathData *) consumer;
    jboolean oom = JNI_FALSE;

    HANDLEMOVETO(pd, x0, y0, {oom = JNI_TRUE;});

    return oom;
}

static jboolean
PCLineTo(PathConsumerVec *consumer,
         jfloat x1, jfloat y1)
{
    pathData *pd = (pathData *) consumer;
    jboolean oom = JNI_FALSE;

    HANDLELINETO(pd, x1, y1, {oom = JNI_TRUE;});

    return oom;
}

static jboolean
PCQuadTo(PathConsumerVec *consumer,
         jfloat x1, jfloat y1,
         jfloat x2, jfloat y2)
{
    pathData *pd = (pathData *) consumer;
    jboolean oom = JNI_FALSE;

    HANDLEQUADTO(pd, x1, y1, x2, y2, {oom = JNI_TRUE;});

    return oom;
}

static jboolean
PCCubicTo(PathConsumerVec *consumer,
          jfloat x1, jfloat y1,
          jfloat x2, jfloat y2,
          jfloat x3, jfloat y3)
{
    pathData *pd = (pathData *) consumer;
    jboolean oom = JNI_FALSE;

    HANDLECUBICTO(pd, x1, y1, x2, y2, x3, y3, {oom = JNI_TRUE;});

    return oom;
}

static jboolean
PCClosePath(PathConsumerVec *consumer)
{
    pathData *pd = (pathData *) consumer;
    jboolean oom = JNI_FALSE;

    HANDLECLOSE(pd, {oom = JNI_TRUE;});

    return oom;
}

static jboolean
PCPathDone(PathConsumerVec *consumer)
{
    pathData *pd = (pathData *) consumer;
    jboolean oom = JNI_FALSE;

    HANDLEENDPATH(pd, {oom = JNI_TRUE;});

    return oom;
}

/*
 * REMIND: CDECL needed for WIN32 "qsort"
 */

#ifdef _WIN32
#define CDECL __cdecl
#else
#define CDECL
#endif

#define SUBDIVIDE_MAX   10
#define MAX_FLAT_SQ     (1.0 * 1.0)
#define GROW_SIZE       20
#define ERRSTEP_MAX     (0x7fffffff)
#define FRACTTOJINT(f)  ((jint) ((f) * (double) ERRSTEP_MAX))

#define minmax2(v1, v2, min, max)       \
do {                                    \
    if (v1 < v2) {                      \
        min = v1;                       \
        max = v2;                       \
    } else {                            \
        min = v2;                       \
        max = v1;                       \
    }                                   \
} while(0)

#define minmax3(v1, v2, v3, min, max)   \
do {                                    \
    if (v1 < v2) {                      \
        if (v1 < v3) {                  \
            min = v1;                   \
            max = (v2 < v3) ? v3 : v2;  \
        } else {                        \
            max = v2;                   \
            min = v3;                   \
        }                               \
    } else {                            \
        if (v1 < v3) {                  \
            max = v3;                   \
            min = v2;                   \
        } else {                        \
            max = v1;                   \
            min = (v2 < v3) ? v2 : v3;  \
        }                               \
    }                                   \
} while (0)

#define minmax4(v1, v2, v3, v4, min, max)       \
do {                                            \
    if (v1 < v2) {                              \
        if (v3 < v4) {                          \
            max = (v2 < v4) ? v4 : v2;          \
            min = (v1 < v3) ? v1 : v3;          \
        } else {                                \
            max = (v2 < v3) ? v3 : v2;          \
            min = (v1 < v4) ? v1 : v4;          \
        }                                       \
    } else {                                    \
        if (v3 < v4) {                          \
            max = (v1 < v4) ? v4 : v1;          \
            min = (v2 < v3) ? v2 : v3;          \
        } else {                                \
            max = (v1 < v3) ? v3 : v1;          \
            min = (v2 < v4) ? v2 : v4;          \
        }                                       \
    }                                           \
} while(0)

static jfloat
ptSegDistSq(jfloat x0, jfloat y0,
            jfloat x1, jfloat y1,
            jfloat px, jfloat py)
{
    jfloat dotprod, projlenSq;

    /* Adjust vectors relative to x0,y0 */
    /* x1,y1 becomes relative vector from x0,y0 to end of segment */
    x1 -= x0;
    y1 -= y0;
    /* px,py becomes relative vector from x0,y0 to test point */
    px -= x0;
    py -= y0;
    dotprod = px * x1 + py * y1;
    if (dotprod <= 0.0) {
        /* px,py is on the side of x0,y0 away from x1,y1 */
        /* distance to segment is length of px,py vector */
        /* "length of its (clipped) projection" is now 0.0 */
        projlenSq = 0.0;
    } else {
        /* switch to backwards vectors relative to x1,y1 */
        /* x1,y1 are already the negative of x0,y0=>x1,y1 */
        /* to get px,py to be the negative of px,py=>x1,y1 */
        /* the dot product of two negated vectors is the same */
        /* as the dot product of the two normal vectors */
        px = x1 - px;
        py = y1 - py;
        dotprod = px * x1 + py * y1;
        if (dotprod <= 0.0) {
            /* px,py is on the side of x1,y1 away from x0,y0 */
            /* distance to segment is length of (backwards) px,py vector */
            /* "length of its (clipped) projection" is now 0.0 */
            projlenSq = 0.0;
        } else {
            /* px,py is between x0,y0 and x1,y1 */
            /* dotprod is the length of the px,py vector */
            /* projected on the x1,y1=>x0,y0 vector times the */
            /* length of the x1,y1=>x0,y0 vector */
            projlenSq = dotprod * dotprod / (x1 * x1 + y1 * y1);
        }
    }
    /* Distance to line is now the length of the relative point */
    /* vector minus the length of its projection onto the line */
    /* (which is zero if the projection falls outside the range */
    /*  of the line segment). */
    return px * px + py * py - projlenSq;
}

static jboolean
appendSegment(pathData *pd,
              jfloat x0, jfloat y0,
              jfloat x1, jfloat y1)
{
    jbyte windDir;
    jint istartx, istarty, ilasty;
    jfloat dx, dy, slope;
    jfloat ystartbump;
    jint bumpx, bumperr, error;
    segmentData *seg;

    if (y0 > y1) {
        jfloat t;
        t = x0; x0 = x1; x1 = t;
        t = y0; y0 = y1; y1 = t;
        windDir = -1;
    } else {
        windDir = 1;
    }
    /* We want to iterate at every horizontal pixel center (HPC) crossing. */
    /* First calculate next highest HPC we will cross at the start. */
    istarty = (jint) ceil(y0 - 0.5f);
    /* Then calculate next highest HPC we would cross at the end. */
    ilasty  = (jint) ceil(y1 - 0.5f);
    /* Ignore if we start and end outside clip, or on the same scanline. */
    if (istarty >= ilasty || istarty >= pd->hiy || ilasty <= pd->loy) {
        return JNI_TRUE;
    }

    /* We will need to insert this segment, check for room. */
    if (pd->numSegments >= pd->segmentsSize) {
        segmentData *newSegs;
        int newSize = pd->segmentsSize + GROW_SIZE;
        newSegs = (segmentData *) calloc(newSize, sizeof(segmentData));
        if (newSegs == NULL) {
            return JNI_FALSE;
        }
        if (pd->segments != NULL) {
            memcpy(newSegs, pd->segments,
                   sizeof(segmentData) * pd->segmentsSize);
            free(pd->segments);
        }
        pd->segments = newSegs;
        pd->segmentsSize = newSize;
    }

    dx = x1 - x0;
    dy = y1 - y0;
    slope = dx / dy;

    /*
     * The Y coordinate of the first HPC was calculated as istarty.  We
     * now need to calculate the corresponding X coordinate (both integer
     * version for span start coordinate and float version for sub-pixel
     * error calculation).
     */
    /* First, how far does y bump to get to next HPC? */
    ystartbump = istarty + 0.5f - y0;
    /* Now, bump the float x coordinate to get X sample at that HPC. */
    x0 += ystartbump * dx / dy;
    /* Now calculate the integer coordinate that such a span starts at. */
    /* NOTE: Span inclusion is based on vertical pixel centers (VPC). */
    istartx = (jint) ceil(x0 - 0.5f);
    /* What is the lower bound of the per-scanline change in the X coord? */
    bumpx = (jint) floor(slope);
    /* What is the subpixel amount by which the bumpx is off? */
    bumperr = FRACTTOJINT(slope - floor(slope));
    /* Finally, find out how far the x coordinate can go before next VPC. */
    error = FRACTTOJINT(x0 - (istartx - 0.5f));

    seg = &pd->segments[pd->numSegments++];
    seg->curx = istartx;
    seg->cury = istarty;
    seg->lasty = ilasty;
    seg->error = error;
    seg->bumpx = bumpx;
    seg->bumperr = bumperr;
    seg->windDir = windDir;
    return JNI_TRUE;
}

/*
 * Lines don't really need to be subdivided, but this function performs
 * the same trivial rejections and reductions that the curve subdivision
 * functions perform before it hands the coordinates off to the appendSegment
 * function.
 */
static jboolean
subdivideLine(pathData *pd, int level,
              jfloat x0, jfloat y0,
              jfloat x1, jfloat y1)
{
    jfloat miny, maxy;
    jfloat minx, maxx;

    minmax2(x0, x1, minx, maxx);
    minmax2(y0, y1, miny, maxy);

    if (maxy <= pd->loy || miny >= pd->hiy || minx >= pd->hix) {
        return JNI_TRUE;
    }
    if (maxx <= pd->lox) {
        return appendSegment(pd, maxx, y0, maxx, y1);
    }

    return appendSegment(pd, x0, y0, x1, y1);
}

static jboolean
subdivideQuad(pathData *pd, int level,
              jfloat x0, jfloat y0,
              jfloat x1, jfloat y1,
              jfloat x2, jfloat y2)
{
    jfloat miny, maxy;
    jfloat minx, maxx;

    minmax3(x0, x1, x2, minx, maxx);
    minmax3(y0, y1, y2, miny, maxy);

    if (maxy <= pd->loy || miny >= pd->hiy || minx >= pd->hix) {
        return JNI_TRUE;
    }
    if (maxx <= pd->lox) {
        return appendSegment(pd, maxx, y0, maxx, y2);
    }

    if (level < SUBDIVIDE_MAX) {
        /* Test if the curve is flat enough for insertion. */
        if (ptSegDistSq(x0, y0, x2, y2, x1, y1) > MAX_FLAT_SQ) {
            jfloat cx1, cx2;
            jfloat cy1, cy2;

            cx1 = (x0 + x1) / 2.0f;
            cx2 = (x1 + x2) / 2.0f;
            x1 = (cx1 + cx2) / 2.0f;

            cy1 = (y0 + y1) / 2.0f;
            cy2 = (y1 + y2) / 2.0f;
            y1 = (cy1 + cy2) / 2.0f;

            level++;
            return (subdivideQuad(pd, level, x0, y0, cx1, cy1, x1, y1) &&
                    subdivideQuad(pd, level, x1, y1, cx2, cy2, x2, y2));
        }
    }

    return appendSegment(pd, x0, y0, x2, y2);
}

static jboolean
subdivideCubic(pathData *pd, int level,
               jfloat x0, jfloat y0,
               jfloat x1, jfloat y1,
               jfloat x2, jfloat y2,
               jfloat x3, jfloat y3)
{
    jfloat miny, maxy;
    jfloat minx, maxx;

    minmax4(x0, x1, x2, x3, minx, maxx);
    minmax4(y0, y1, y2, y3, miny, maxy);

    if (maxy <= pd->loy || miny >= pd->hiy || minx >= pd->hix) {
        return JNI_TRUE;
    }
    if (maxx <= pd->lox) {
        return appendSegment(pd, maxx, y0, maxx, y3);
    }

    if (level < SUBDIVIDE_MAX) {
        /* Test if the curve is flat enough for insertion. */
        if (ptSegDistSq(x0, y0, x3, y3, x1, y1) > MAX_FLAT_SQ ||
            ptSegDistSq(x0, y0, x3, y3, x2, y2) > MAX_FLAT_SQ)
        {
            jfloat ctrx, cx12, cx21;
            jfloat ctry, cy12, cy21;

            ctrx = (x1 + x2) / 2.0f;
            x1 = (x0 + x1) / 2.0f;
            x2 = (x2 + x3) / 2.0f;
            cx12 = (x1 + ctrx) / 2.0f;
            cx21 = (ctrx + x2) / 2.0f;
            ctrx = (cx12 + cx21) / 2.0f;

            ctry = (y1 + y2) / 2.0f;
            y1 = (y0 + y1) / 2.0f;
            y2 = (y2 + y3) / 2.0f;
            cy12 = (y1 + ctry) / 2.0f;
            cy21 = (ctry + y2) / 2.0f;
            ctry = (cy12 + cy21) / 2.0f;

            level++;
            return (subdivideCubic(pd, level, x0, y0, x1, y1,
                                   cx12, cy12, ctrx, ctry) &&
                    subdivideCubic(pd, level, ctrx, ctry, cx21, cy21,
                                   x2, y2, x3, y3));
        }
    }

    return appendSegment(pd, x0, y0, x3, y3);
}

static int CDECL
sortSegmentsByLeadingY(const void *elem1, const void *elem2)
{
    segmentData *seg1 = *(segmentData **)elem1;
    segmentData *seg2 = *(segmentData **)elem2;

    if (seg1->cury < seg2->cury) {
        return -1;
    }
    if (seg1->cury > seg2->cury) {
        return 1;
    }
    if (seg1->curx < seg2->curx) {
        return -1;
    }
    if (seg1->curx > seg2->curx) {
        return 1;
    }
    if (seg1->lasty < seg2->lasty) {
        return -1;
    }
    if (seg1->lasty > seg2->lasty) {
        return 1;
    }
    return 0;
}

static void *
ShapeSIOpen(JNIEnv *env, jobject iterator)
{
    return GetSpanData(env, iterator, STATE_PATH_DONE, STATE_PATH_DONE);
}

static void
ShapeSIClose(JNIEnv *env, void *private)
{
}

static void
ShapeSIGetPathBox(JNIEnv *env, void *private, jint pathbox[])
{
    pathData *pd = (pathData *)private;

    pathbox[0] = (jint) floor(pd->pathlox);
    pathbox[1] = (jint) floor(pd->pathloy);
    pathbox[2] = (jint) ceil(pd->pathhix);
    pathbox[3] = (jint) ceil(pd->pathhiy);
}

/*  Adjust the clip box from the given bounds. Used to constrain
    the output to a device clip
*/
static void
ShapeSIIntersectClipBox(JNIEnv *env, void *private,
                            jint clox, jint cloy, jint chix, jint chiy)
{
    pathData *pd = (pathData *)private;

    if (clox > pd->lox) {
        pd->lox = clox;
    }
    if (cloy > pd->loy) {
        pd->loy = cloy;
    }
    if (chix < pd->hix) {
        pd->hix = chix;
    }
    if (chiy < pd->hiy) {
        pd->hiy = chiy;
    }
}

static jboolean
ShapeSINextSpan(void *state, jint spanbox[])
{
    pathData *pd = (pathData *)state;
    int lo, cur, new, hi;
    int num = pd->numSegments;
    jint x0, x1, y0, err;
    jint loy;
    int ret = JNI_FALSE;
    segmentData **segmentTable;
    segmentData *seg;

    if (pd->state != STATE_SPAN_STARTED) {
        if (!initSegmentTable(pd)) {
            /* REMIND: - throw exception? */
            pd->lowSegment = num;
            return JNI_FALSE;
        }
    }

    lo = pd->lowSegment;
    cur = pd->curSegment;
    hi = pd->hiSegment;
    num = pd->numSegments;
    loy = pd->loy;
    segmentTable = pd->segmentTable;

    while (lo < num) {
        if (cur < hi) {
            seg = segmentTable[cur];
            x0 = seg->curx;
            if (x0 >= pd->hix) {
                cur = hi;
                continue;
            }
            if (x0 < pd->lox) {
                x0 = pd->lox;
            }

            if (pd->evenodd) {
                cur += 2;
                if (cur <= hi) {
                    x1 = segmentTable[cur - 1]->curx;
                } else {
                    x1 = pd->hix;
                }
            } else {
                int wind = seg->windDir;
                cur++;

                while (JNI_TRUE) {
                    if (cur >= hi) {
                        x1 = pd->hix;
                        break;
                    }
                    seg = segmentTable[cur++];
                    wind += seg->windDir;
                    if (wind == 0) {
                        x1 = seg->curx;
                        break;
                    }
                }
            }

            if (x1 > pd->hix) {
                x1 = pd->hix;
            }
            if (x1 <= x0) {
                continue;
            }
            spanbox[0] = x0;
            spanbox[1] = loy;
            spanbox[2] = x1;
            spanbox[3] = loy + 1;
            ret = JNI_TRUE;
            break;
        }

        if (++loy >= pd->hiy) {
            lo = cur = hi = num;
            break;
        }

        /* Go through active segments and toss which end "above" loy */
        cur = new = hi;
        while (--cur >= lo) {
            seg = segmentTable[cur];
            if (seg->lasty > loy) {
                segmentTable[--new] = seg;
            }
        }

        lo = new;
        if (lo == hi && lo < num) {
            /* The current list of segments is empty so we need to
             * jump to the beginning of the next set of segments.
             * Since the segments are not clipped to the output
             * area we need to make sure we don't jump "backwards"
             */
            seg = segmentTable[lo];
            if (loy < seg->cury) {
                loy = seg->cury;
            }
        }

        /* Go through new segments and accept any which start "above" loy */
        while (hi < num && segmentTable[hi]->cury <= loy) {
            hi++;
        }

        /* Update and sort the active segments by x0 */
        for (cur = lo; cur < hi; cur++) {
            seg = segmentTable[cur];

            /* First update the x0, y0 of the segment */
            x0 = seg->curx;
            y0 = seg->cury;
            err = seg->error;
            if (++y0 == loy) {
                x0 += seg->bumpx;
                err += seg->bumperr;
                x0 -= (err >> 31);
                err &= ERRSTEP_MAX;
            } else {
                jlong steps = loy;
                steps -= y0 - 1;
                y0 = loy;
                x0 += (jint) (steps * seg->bumpx);
                steps = err + (steps * seg->bumperr);
                x0 += (jint) (steps >> 31);
                err = ((jint) steps) & ERRSTEP_MAX;
            }
            seg->curx = x0;
            seg->cury = y0;
            seg->error = err;

            /* Then make sure the segment is sorted by x0 */
            for (new = cur; new > lo; new--) {
                segmentData *seg2 = segmentTable[new - 1];
                if (seg2->curx <= x0) {
                    break;
                }
                segmentTable[new] = seg2;
            }
            segmentTable[new] = seg;
        }
        cur = lo;
    }

    pd->lowSegment = lo;
    pd->hiSegment = hi;
    pd->curSegment = cur;
    pd->loy = loy;
    return ret;
}

static void
ShapeSISkipDownTo(void *private, jint y)
{
    pathData *pd = (pathData *)private;

    if (pd->state != STATE_SPAN_STARTED) {
        if (!initSegmentTable(pd)) {
            /* REMIND: - throw exception? */
            pd->lowSegment = pd->numSegments;
            return;
        }
    }

    /* Make sure we are jumping forward */
    if (pd->loy < y) {
        /* Pretend like we just finished with the span line y-1... */
        pd->loy = y - 1;
        pd->curSegment = pd->hiSegment; /* no more segments on that line */
    }
}

static jboolean
initSegmentTable(pathData *pd)
{
    int i, cur, num, loy;
    segmentData **segmentTable;
    segmentTable = malloc(pd->numSegments * sizeof(segmentData *));
    if (segmentTable == NULL) {
        return JNI_FALSE;
    }
    pd->state = STATE_SPAN_STARTED;
    for (i = 0; i < pd->numSegments; i++) {
        segmentTable[i] = &pd->segments[i];
    }
    qsort(segmentTable, pd->numSegments, sizeof(segmentData *),
          sortSegmentsByLeadingY);

    pd->segmentTable = segmentTable;

    /* Skip to the first segment that ends below the top clip edge */
    cur = 0;
    num = pd->numSegments;
    loy = pd->loy;
    while (cur < num && segmentTable[cur]->lasty <= loy) {
        cur++;
    }
    pd->lowSegment = pd->curSegment = pd->hiSegment = cur;

    /* Prepare for next action to increment loy and prepare new segments */
    pd->loy--;

    return JNI_TRUE;
}
