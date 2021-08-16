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

#ifndef D3DBufImgOps_h_Included
#define D3DBufImgOps_h_Included

#include "D3DContext.h"

/**************************** ConvolveOp support ****************************/

/**
 * Flags that can be bitwise-or'ed together to control how the shader
 * source code is generated.
 */
#define CONVOLVE_EDGE_ZERO_FILL (1 << 0)
#define CONVOLVE_5X5            (1 << 1)
#define MAX_CONVOLVE            (1 << 2)

HRESULT D3DBufImgOps_EnableConvolveOp(D3DContext *oglc, jlong pSrcOps,
                                      jboolean edgeZeroFill,
                                      jint kernelWidth, jint KernelHeight,
                                      unsigned char *kernelVals);
HRESULT D3DBufImgOps_DisableConvolveOp(D3DContext *oglc);

/**************************** RescaleOp support *****************************/

/**
 * Flags that can be bitwise-or'ed together to control how the shader
 * source code is generated.
 */
#define RESCALE_NON_PREMULT (1 << 0)
#define MAX_RESCALE         (1 << 1)

HRESULT D3DBufImgOps_EnableRescaleOp(D3DContext *oglc,
                                     jboolean nonPremult,
                                     unsigned char *scaleFactors,
                                     unsigned char *offsets);
HRESULT D3DBufImgOps_DisableRescaleOp(D3DContext *oglc);

/**************************** LookupOp support ******************************/

/**
 * Flags that can be bitwise-or'ed together to control how the shader
 * source code is generated.
 */
#define LOOKUP_USE_SRC_ALPHA (1 << 0)
#define LOOKUP_NON_PREMULT   (1 << 1)
#define MAX_LOOKUP           (1 << 2)

HRESULT D3DBufImgOps_EnableLookupOp(D3DContext *oglc,
                                    jboolean nonPremult, jboolean shortData,
                                    jint numBands, jint bandLength, jint offset,
                                    void *tableValues);
HRESULT D3DBufImgOps_DisableLookupOp(D3DContext *oglc);

#endif /* D3DBufImgOps_h_Included */
