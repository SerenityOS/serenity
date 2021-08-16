/*
 * Copyright (c) 2000, 2002, Oracle and/or its affiliates. All rights reserved.
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

#include "AlphaMacros.h"

/*
 * The following equation is used to blend each pixel in a compositing
 * operation between two images (a and b).  If we have Ca (Component of a)
 * and Cb (Component of b) representing the alpha and color components
 * of a given pair of corresponding pixels in the two source images,
 * then Porter & Duff have defined blending factors Fa (Factor for a)
 * and Fb (Factor for b) to represent the contribution of the pixel
 * from the corresponding image to the pixel in the result.
 *
 *    Cresult = Fa * Ca + Fb * Cb
 *
 * The blending factors Fa and Fb are computed from the alpha value of
 * the pixel from the "other" source image.  Thus, Fa is computed from
 * the alpha of Cb and vice versa on a per-pixel basis.
 *
 * A given factor (Fa or Fb) is computed from the other alpha using
 * one of the following blending factor equations depending on the
 * blending rule and depending on whether we are computing Fa or Fb:
 *
 *    Fblend = 0
 *    Fblend = ONE
 *    Fblend = alpha
 *    Fblend = (ONE - alpha)
 *
 * The value ONE in these equations represents the same numeric value
 * as is used to represent "full coverage" in the alpha component.  For
 * example it is the value 0xff for 8-bit alpha channels and the value
 * 0xffff for 16-bit alpha channels.
 *
 * Each Porter-Duff blending rule thus defines a pair of the above Fblend
 * equations to define Fa and Fb independently and thus to control
 * the contributions of the two source pixels to the destination pixel.
 *
 * Rather than use conditional tests per pixel in the inner loop,
 * we note that the following 3 logical and mathematical operations
 * can be applied to any alpha value to produce the result of one
 * of the 4 Fblend equations:
 *
 *    Fcomp = ((alpha AND Fk1) XOR Fk2) PLUS Fk3
 *
 * Through appropriate choices for the 3 Fk values we can cause
 * the result of this Fcomp equation to always match one of the
 * defined Fblend equations.  More importantly, the Fcomp equation
 * involves no conditional tests which can stall pipelined processor
 * execution and typically compiles very tightly into 3 machine
 * instructions.
 *
 * For each of the 4 Fblend equations the desired Fk values are
 * as follows:
 *
 *       Fblend            Fk1        Fk2       Fk3
 *       ------            ---        ---       ---
 *          0               0          0         0
 *         ONE              0          0        ONE
 *        alpha            ONE         0         0
 *      ONE-alpha          ONE        -1       ONE+1
 *
 * This gives us the following derivations for Fcomp.  Note that
 * the derivation of the last equation is less obvious so it is
 * broken down into steps and uses the well-known equality for
 * two's-complement arithmetic "((n XOR -1) PLUS 1) == -n":
 *
 *     ((alpha AND  0 ) XOR  0) PLUS   0        == 0
 *
 *     ((alpha AND  0 ) XOR  0) PLUS  ONE       == ONE
 *
 *     ((alpha AND ONE) XOR  0) PLUS   0        == alpha
 *
 *     ((alpha AND ONE) XOR -1) PLUS ONE+1      ==
 *         ((alpha XOR -1) PLUS 1) PLUS ONE     ==
 *         (-alpha) PLUS ONE                    == ONE - alpha
 *
 * We have assigned each Porter-Duff rule an implicit index for
 * simplicity of referring to the rule in parameter lists.  For
 * a given blending operation which uses a specific rule, we simply
 * use the index of that rule to index into a table and load values
 * from that table which help us construct the 2 sets of 3 Fk values
 * needed for applying that blending rule (one set for Fa and the
 * other set for Fb).  Since these Fk values depend only on the
 * rule we can set them up at the start of the outer loop and only
 * need to do the 3 operations in the Fcomp equation twice per
 * pixel (once for Fa and again for Fb).
 * -------------------------------------------------------------
 */

/*
 * The following definitions represent terms in the Fblend
 * equations described above.  One "term name" is chosen from
 * each of the following 3 pairs of names to define the table
 * values for the Fa or the Fb of a given Porter-Duff rule.
 *
 *    AROP_ZERO     the first operand is the constant zero
 *    AROP_ONE      the first operand is the constant one
 *
 *    AROP_PLUS     the two operands are added together
 *    AROP_MINUS    the second operand is subtracted from the first
 *
 *    AROP_NAUGHT   there is no second operand
 *    AROP_ALPHA    the indicated alpha is used for the second operand
 *
 * These names expand to numeric values which can be conveniently
 * combined to produce the 3 Fk values needed for the Fcomp equation.
 *
 * Note that the numeric values used here are most convenient for
 * generating the 3 specific Fk values needed for manipulating images
 * with 8-bits of alpha precision.  But Fk values for manipulating
 * images with other alpha precisions (such as 16-bits) can also be
 * derived from these same values using a small amount of bit
 * shifting and replication.
 */
#define AROP_ZERO       0x00
#define AROP_ONE        0xff
#define AROP_PLUS       0
#define AROP_MINUS      -1
#define AROP_NAUGHT     0x00
#define AROP_ALPHA      0xff

/*
 * This macro constructs a single Fcomp equation table entry from the
 * term names for the 3 terms in the corresponding Fblend equation.
 */
#define MAKE_AROPS(add, xor, and)  { AROP_ ## add, AROP_ ## and, AROP_ ## xor }

/*
 * These macros define the Fcomp equation table entries for each
 * of the 4 Fblend equations described above.
 *
 *    AROPS_ZERO      Fblend = 0
 *    AROPS_ONE       Fblend = 1
 *    AROPS_ALPHA     Fblend = alpha
 *    AROPS_INVALPHA  Fblend = (1 - alpha)
 */
#define AROPS_ZERO      MAKE_AROPS( ZERO, PLUS,  NAUGHT )
#define AROPS_ONE       MAKE_AROPS( ONE,  PLUS,  NAUGHT )
#define AROPS_ALPHA     MAKE_AROPS( ZERO, PLUS,  ALPHA  )
#define AROPS_INVALPHA  MAKE_AROPS( ONE,  MINUS, ALPHA  )

/*
 * This table maps a given Porter-Duff blending rule index to a
 * pair of Fcomp equation table entries, one for computing the
 * 3 Fk values needed for Fa and another for computing the 3
 * Fk values needed for Fb.
 */
AlphaFunc AlphaRules[] = {
    {   {0, 0, 0},      {0, 0, 0}       },      /* 0 - Nothing */
    {   AROPS_ZERO,     AROPS_ZERO      },      /* 1 - RULE_Clear */
    {   AROPS_ONE,      AROPS_ZERO      },      /* 2 - RULE_Src */
    {   AROPS_ONE,      AROPS_INVALPHA  },      /* 3 - RULE_SrcOver */
    {   AROPS_INVALPHA, AROPS_ONE       },      /* 4 - RULE_DstOver */
    {   AROPS_ALPHA,    AROPS_ZERO      },      /* 5 - RULE_SrcIn */
    {   AROPS_ZERO,     AROPS_ALPHA     },      /* 6 - RULE_DstIn */
    {   AROPS_INVALPHA, AROPS_ZERO      },      /* 7 - RULE_SrcOut */
    {   AROPS_ZERO,     AROPS_INVALPHA  },      /* 8 - RULE_DstOut */
    {   AROPS_ZERO,     AROPS_ONE       },      /* 9 - RULE_Dst */
    {   AROPS_ALPHA,    AROPS_INVALPHA  },      /*10 - RULE_SrcAtop */
    {   AROPS_INVALPHA, AROPS_ALPHA     },      /*11 - RULE_DstAtop */
    {   AROPS_INVALPHA, AROPS_INVALPHA  },      /*12 - RULE_Xor */
};
