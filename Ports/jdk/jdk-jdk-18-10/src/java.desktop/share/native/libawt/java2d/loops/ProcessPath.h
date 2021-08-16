/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

#ifndef ProcessPath_h_Included
#define ProcessPath_h_Included

#include <float.h>
#include "jni_util.h"

#define UPPER_BND (FLT_MAX/4.0f)
#define LOWER_BND (-UPPER_BND)

/* Precision (in bits) used in forward differencing */
#define FWD_PREC    7

/* Precision (in bits) used for the rounding in the midpoint test */
#define MDP_PREC    10

#define MDP_MULT        (1<<MDP_PREC)
#define MDP_HALF_MULT   (MDP_MULT >> 1)

/* Bit mask used to separate whole part from the fraction part of the
 * number
 */
#define MDP_W_MASK      (-MDP_MULT)

/* Bit mask used to separate fractional part from the whole part of the
 * number
 */
#define MDP_F_MASK      (MDP_MULT-1)

typedef struct _DrawHandler {
    void (*pDrawLine)(struct _DrawHandler* hnd,
                      jint x0, jint y0, jint x1, jint y1);

    void (*pDrawPixel)(struct _DrawHandler* hnd, jint x0, jint y0);

    void (*pDrawScanline)(struct _DrawHandler* hnd, jint x0, jint x1, jint y0);
    // TODO Change following names to smth like outXMin
    jint xMin, yMin, xMax, yMax;

    /* Boundary values with stroke control rendering hint applied */
    jfloat xMinf, yMinf, xMaxf, yMaxf;

    void* pData;
} DrawHandler;

typedef enum {
    PH_MODE_DRAW_CLIP,
    PH_MODE_FILL_CLIP
} PHClip;

/* Constants representing KEY_STROKE_CONTROL rendering hints */
typedef enum {
    PH_STROKE_PURE,   /* RenderingHints.VALUE_STROKE_PURE    */
    PH_STROKE_DEFAULT /* RenderingHints.VALUE_STROKE_DEFAULT */
} PHStroke;

typedef struct _ProcessHandler {
    void (*pProcessFixedLine)(struct _ProcessHandler* hnd,
                              jint x1,jint y1,
                              jint x2,jint y2, jint* pixelInfo,
                              jboolean checkBounds,
                              jboolean endSubPath);
    void (*pProcessEndSubPath)(struct _ProcessHandler* hnd);
    DrawHandler* dhnd;
    PHStroke stroke;
    PHClip clipMode;
    void* pData;
} ProcessHandler;


JNIEXPORT jboolean JNICALL
doDrawPath(DrawHandler* hnd,
           void (*pProcessEndSubPath)(ProcessHandler* hnd),
           jint transX, jint transY,
           jfloat* coords, jint maxCoords,
           jbyte* types, jint numTypes,
           PHStroke stroke);

JNIEXPORT jboolean JNICALL
doFillPath(DrawHandler* hnd,
           jint transX, jint transY,
           jfloat* coords, jint maxCoords,
           jbyte* types, jint numTypes,
           PHStroke stroke,
           jint fillRule);

#endif
