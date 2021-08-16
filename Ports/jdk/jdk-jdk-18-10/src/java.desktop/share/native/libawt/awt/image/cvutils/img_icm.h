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
 * with any Java IndexColorModel object.  This implementation examines
 * some of the private fields of the IndexColorModel object and decodes
 * the red, green, blue, and possibly alpha values directly rather than
 * calling the getRGB method on the Java object.
 */

/*
 * These definitions vector the standard macro names to the "ICM"
 * versions of those macros only if the "DecodeDeclared" keyword has
 * not yet been defined elsewhere.  The "DecodeDeclared" keyword is
 * also defined here to claim ownership of the primary implementation
 * even though this file does not rely on the definitions in any other
 * files.
 */
#ifndef DecodeDeclared
#define DeclareDecodeVars       DeclareICMVars
#define InitPixelDecode(CM)     InitPixelICM(unhand(CM))
#define PixelDecode             PixelICMDecode
#define DecodeDeclared
#endif

#include "java_awt_image_IndexColorModel.h"

#define DeclareICMVars                                  \
    unsigned int mapsize;                               \
    unsigned int *cmrgb;

#define InitPixelICM(CM)                                        \
    do {                                                        \
        Classjava_awt_image_IndexColorModel *icm =              \
            (Classjava_awt_image_IndexColorModel *) CM;         \
        cmrgb = (unsigned int *) unhand(icm->rgb);              \
        mapsize = obj_length(icm->rgb);                         \
    } while (0)

#define PixelICMDecode(CM, pixel, red, green, blue, alpha)      \
    do {                                                        \
        VerifyPixelRange(pixel, mapsize);                       \
        pixel = cmrgb[pixel];                                   \
        IfAlpha(alpha = (pixel >> ALPHASHIFT) & 0xff;)          \
        red = (pixel >> REDSHIFT) & 0xff;                       \
        green = (pixel >> GREENSHIFT) & 0xff;                   \
        blue = (pixel >> BLUESHIFT) & 0xff;                     \
    } while (0)
