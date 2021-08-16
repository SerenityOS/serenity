/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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

#ifndef D3DPaints_h_Included
#define D3DPaints_h_Included

#include "sun_java2d_SunGraphics2D.h"

#include "D3DContext.h"
#include "D3DSurfaceData.h"

HRESULT D3DPaints_ResetPaint(D3DContext *d3dc);
HRESULT D3DPaints_SetColor(D3DContext *d3dc, jint pixel);

/************************* GradientPaint support ****************************/

/**
 * Flags that can be bitwise-or'ed together to control how the shader
 * source code is generated.
 */
#define BASIC_GRAD_IS_CYCLIC (1 << 0)
#define BASIC_GRAD_USE_MASK  (1 << 1)

HRESULT D3DPaints_SetGradientPaint(D3DContext *d3dc,
                                jboolean useMask, jboolean cyclic,
                                jdouble p0, jdouble p1, jdouble p3,
                                jint pixel1, jint pixel2);

/************************** TexturePaint support ****************************/

HRESULT D3DPaints_SetTexturePaint(D3DContext *d3dc,
                               jboolean useMask,
                               jlong pSrcOps, jboolean filter,
                               jdouble xp0, jdouble xp1, jdouble xp3,
                               jdouble yp0, jdouble yp1, jdouble yp3);

/****************** Shared MultipleGradientPaint support ********************/

/**
 * These constants are identical to those defined in the
 * MultipleGradientPaint.CycleMethod enum; they are copied here for
 * convenience (ideally we would pull them directly from the Java level,
 * but that entails more hassle than it is worth).
 */
#define CYCLE_NONE    0
#define CYCLE_REFLECT 1
#define CYCLE_REPEAT  2

/**
 * The following constants are flags that can be bitwise-or'ed together
 * to control how the MultipleGradientPaint shader source code is generated:
 *
 *   MULTI_GRAD_CYCLE_METHOD
 *     Placeholder for the CycleMethod enum constant.
 *
 *   MULTI_GRAD_LARGE
 *     If set, use the (slower) shader that supports a larger number of
 *     gradient colors; otherwise, use the optimized codepath.  See
 *     the MAX_FRACTIONS_SMALL/LARGE constants below for more details.
 *
 *   MULTI_GRAD_USE_MASK
 *     If set, apply the alpha mask value from texture unit 1 to the
 *     final color result (only used in the MaskFill case).
 *
 *   MULTI_GRAD_LINEAR_RGB
 *     If set, convert the linear RGB result back into the sRGB color space.
 */
#define MULTI_GRAD_CYCLE_METHOD (3 << 0)
#define MULTI_GRAD_LARGE        (1 << 2)
#define MULTI_GRAD_USE_MASK     (1 << 3)
#define MULTI_GRAD_LINEAR_RGB   (1 << 4)

/**
 * The maximum number of gradient colors supported by all of the gradient
 * fragment shaders.  Note that this value must be a power of two, as it
 * determines the size of the 1D texture created below.  It also must be
 * greater than or equal to MAX_FRACTIONS (there is no strict requirement
 * that the two values be equal).
 */
#define MAX_MULTI_GRADIENT_COLORS 16

/********************** LinearGradientPaint support *************************/

HRESULT D3DPaints_SetLinearGradientPaint(D3DContext *d3dc, D3DSDOps *dstOps,
                                         jboolean useMask, jboolean linear,
                                         jint cycleMethod, jint numStops,
                                         jfloat p0, jfloat p1, jfloat p3,
                                         void *fractions, void *pixels);

/********************** RadialGradientPaint support *************************/

HRESULT D3DPaints_SetRadialGradientPaint(D3DContext *d3dc, D3DSDOps *dstOps,
                                         jboolean useMask, jboolean linear,
                                         jint cycleMethod, jint numStops,
                                         jfloat m00, jfloat m01, jfloat m02,
                                         jfloat m10, jfloat m11, jfloat m12,
                                         jfloat focusX,
                                         void *fractions, void *pixels);

/************************ SunGraphics2D constants ***************************/

#define PAINT_CUSTOM       sun_java2d_SunGraphics2D_PAINT_CUSTOM
#define PAINT_TEXTURE      sun_java2d_SunGraphics2D_PAINT_TEXTURE
#define PAINT_RAD_GRADIENT sun_java2d_SunGraphics2D_PAINT_RAD_GRADIENT
#define PAINT_LIN_GRADIENT sun_java2d_SunGraphics2D_PAINT_LIN_GRADIENT
#define PAINT_GRADIENT     sun_java2d_SunGraphics2D_PAINT_GRADIENT
#define PAINT_ALPHACOLOR   sun_java2d_SunGraphics2D_PAINT_ALPHACOLOR
#define PAINT_OPAQUECOLOR  sun_java2d_SunGraphics2D_PAINT_OPAQUECOLOR

#endif /* D3DPaints_h_Included */
