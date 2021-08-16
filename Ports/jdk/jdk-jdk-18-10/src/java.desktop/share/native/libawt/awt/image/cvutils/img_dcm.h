/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * with any Java DirectColorModel object.  This implementation will
 * scale the decoded color components to 8-bit quantities if needed.
 * Another file is provided to optimize DCM parsing when the masks
 * are guaranteed to be at least 8-bits wide.  This implementation
 * examines some of the private fields of the DirectColorModel
 * object and decodes the red, green, blue, and possibly alpha values
 * directly rather than calling the getRGB method on the Java object.
 */

/*
 * These definitions vector the standard macro names to the "DCM"
 * versions of those macros only if the "DecodeDeclared" keyword has
 * not yet been defined elsewhere.  The "DecodeDeclared" keyword is
 * also defined here to claim ownership of the primary implementation
 * even though this file does not rely on the definitions in any other
 * files.
 */
#ifndef DecodeDeclared
#define DeclareDecodeVars       DeclareDCMVars
#define InitPixelDecode(CM)     InitPixelDCM(unhand(CM))
#define PixelDecode             PixelDCMDecode
#define DecodeDeclared
#endif

#define DeclareDCMVars                                          \
    IfAlpha(int alpha_mask;                                     \
            int alpha_scale;                                    \
            unsigned int alpha_off;)                            \
    int red_mask, green_mask, blue_mask;                        \
    int red_scale, green_scale, blue_scale;                     \
    unsigned int red_off, green_off, blue_off;                  \
    int scale;

#define InitPixelDCM(CM)                                                \
    do {                                                                \
        Classjava_awt_image_DirectColorModel *dcm =                     \
            (Classjava_awt_image_DirectColorModel *) CM;                \
        red_mask = dcm->red_mask;                                       \
        red_off = dcm->red_offset;                                      \
        red_scale = dcm->red_scale;                                     \
        green_mask = dcm->green_mask;                                   \
        green_off = dcm->green_offset;                                  \
        green_scale = dcm->green_scale;                                 \
        blue_mask = dcm->blue_mask;                                     \
        blue_off = dcm->blue_offset;                                    \
        blue_scale = dcm->blue_scale;                                   \
        IfAlpha(alpha_mask = dcm->alpha_mask;                           \
                alpha_off = dcm->alpha_offset;                          \
                alpha_scale = dcm->alpha_scale;)                        \
        scale = (red_scale | green_scale | blue_scale                   \
                 IfAlpha(| alpha_scale));                               \
    } while (0)

#define PixelDCMDecode(CM, pixel, red, green, blue, alpha)              \
    do {                                                                \
        IfAlpha(alpha = ((alpha_mask == 0)                              \
                         ? 255                                          \
                         : ((pixel & alpha_mask) >> alpha_off));)       \
        red = ((pixel & red_mask) >> red_off);                          \
        green = ((pixel & green_mask) >> green_off);                    \
        blue = ((pixel & blue_mask) >> blue_off);                       \
        if (scale) {                                                    \
            if (red_scale) {                                            \
                red = red * 255 / (red_scale);                          \
            }                                                           \
            if (green_scale) {                                          \
                green = green * 255 / (green_scale);                    \
            }                                                           \
            if (blue_scale) {                                           \
                blue = blue * 255 / (blue_scale);                       \
            }                                                           \
            IfAlpha(if (alpha_scale) {                                  \
                alpha = alpha * 255 / (alpha_scale);                    \
            })                                                          \
        }                                                               \
    } while (0)
