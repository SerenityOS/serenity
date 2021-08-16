/*
 * Copyright (c) 1996, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This file contains macro definitions for the Decoding category of
 * the macros used by the generic scaleloop function.
 *
 * This implementation can decode the pixel information associated
 * with any valid Java ColorModel object by dynamically invoking the
 * getRGB method on that object.  The implementation will also
 * optimally handle pixel data coming from IndexColorModel and
 * DirectColorModel objects so that it can be used as the default
 * fallback implementation for corner cases without imposing the
 * enormous performance penalty required for handling the custom
 * ColorModel objects in those cases.
 *
 * This file can be used to provide the default implementation of the
 * Decoding macros, handling all color conversion cases.
 */

/*
 * These definitions vector the standard macro names to the "Any"
 * versions of those macros.  The "DecodeDeclared" keyword is also
 * defined to indicate to the other include files that they are not
 * defining the primary implementation.  All other include files
 * will check for the existance of the "DecodeDeclared" keyword
 * and define their implementations of the Decoding macros using
 * more specific names without overriding the standard names.
 * This is done so that the other files can be included here to
 * reuse their implementations for the specific optimization cases.
 */
#define DecodeDeclared
#define DeclareDecodeVars       DeclareAnyVars
#define InitPixelDecode         InitPixelAny
#define PixelDecode             PixelAnyDecode

/* Include the optimal implementations for Index and Direct ColorModels */
#include "img_icm.h"
#include "img_dcm.h"

#define ICMTYPE         0
#define DCMTYPE         1
#define OCMTYPE         2

#define DeclareAnyVars                                          \
    DeclareICMVars                                              \
    DeclareDCMVars                                              \
    struct execenv *ee;                                         \
    struct methodblock *mb = 0;                                 \
    int CMtype;

#define InitPixelAny(CM)                                                \
    do {                                                                \
        Classjava_awt_image_ColorModel *cm =                            \
            (Classjava_awt_image_ColorModel *) unhand(CM);              \
        ImgCMData *icmd = (ImgCMData *) cm->pData;                      \
        if ((icmd->type & IMGCV_CMBITS) == IMGCV_ICM) {                 \
            CMtype = ICMTYPE;                                           \
            InitPixelICM(cm);                                           \
        } else if (((icmd->type & IMGCV_CMBITS) == IMGCV_DCM)           \
                   || ((icmd->type & IMGCV_CMBITS) == IMGCV_DCM8)) {    \
            CMtype = DCMTYPE;                                           \
            InitPixelDCM(cm);                                           \
        } else {                                                        \
            CMtype = OCMTYPE;                                           \
            ee = EE();                                                  \
            mb = icmd->mb;                                              \
        }                                                               \
    } while (0)

#define PixelAnyDecode(CM, pixel, red, green, blue, alpha)              \
    do {                                                                \
        switch (CMtype) {                                               \
        case ICMTYPE:                                                   \
            PixelICMDecode(CM, pixel, red, green, blue, alpha);         \
            break;                                                      \
        case DCMTYPE:                                                   \
            PixelDCMDecode(CM, pixel, red, green, blue, alpha);         \
            break;                                                      \
        case OCMTYPE:                                                   \
            pixel = do_execute_java_method(ee, (void *) CM,             \
                                           "getRGB","(I)I", mb,         \
                                           FALSE, pixel);               \
            if (exceptionOccurred(ee)) {                                \
                return SCALEFAILURE;                                    \
            }                                                           \
            IfAlpha(alpha = pixel >> ALPHASHIFT;)                       \
            red = (pixel >> REDSHIFT) & 0xff;                           \
            green = (pixel >> GREENSHIFT) & 0xff;                       \
            blue = (pixel >> BLUESHIFT) & 0xff;                         \
            break;                                                      \
        }                                                               \
    } while (0)
