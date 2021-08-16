/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AlphaMath_h_Included
#define AlphaMath_h_Included

#include "jni.h"

JNIEXPORT extern unsigned char mul8table[256][256];
JNIEXPORT extern unsigned char div8table[256][256];
extern void initAlphaTables();


/*
 * Multiply and Divide macros for single byte (8-bit) quantities representing
 * the values 0.0 to 1.0 as 0x00 to 0xff.
 * MUL8 multiplies its operands together
 * DIV8 divides the first operand by the second, clipping to 0xff
 *    (Note that since the divisor for DIV8 is likely to be
 *     the alpha quantity which is likely to be the same for
 *     multiple adjacent invocations, the table is designed
 *     with the first index being the divisor to hopefully
 *     improve memory cache hits...)
 */
#define MUL8(a,b) mul8table[a][b]
#define DIV8(a,b) div8table[b][a]

/*
 * Multiply and Divide macros for operations involving a single short (16-bit)
 * quantity and a single byte (8-bit) quantity.  Typically, promoting the
 * 8-bit value to 16 bits would lead to overflow when the operation occurs.
 * These macros have been modified somewhat so that overflow will not occur.
 * MUL8_16 multiplies an 8-bit value by a 16-bit value (the order of operands
 *         is unimportant since multiplication is a commutative operation)
 * DIV16_8 divides the first (16-bit) operand by the second (8-bit) value
 */

#define MUL8_16(a,b) (((a) * (b)) / 255)
#define DIV16_8(a,b) (((a) * 255) / (b))

/*
 * Multiply and Divide macros for single short (16-bit) quantities
 * representing the values 0.0 to 1.0 as 0x0000 to 0xffff.
 * MUL16 multiplies its operands using the standard multiplication operator
 *       and normalizes the result to the appropriate range
 * DIV16 divides the first operand by the second and normalizes the result
 *       to a 16-bit value
 */
#define MUL16(a,b) (((a) * (b)) / 65535)
#define DIV16(a,b) (((a) * 65535) / (b))

/*
 * Macro for the sum of two normalized (16-bit) products.  Refer to the
 * following equation and note that the right side reduces the number of
 * divide operations in the left side and increases the precision of the
 * result:
 *   a*f1 + b*f2     a*f1 + b*f2
 *   ----   ----  =  -----------     (where n in this case will be 65535)
 *     n      n           n
 */
#define AddNormalizedProducts16(a, f1, b, f2) \
    ((((a) * (f1)) + ((b) * (f2))) / 65535)


/*
 * The following macros help to generalize the MaskBlit and MaskFill loops
 * found in AlphaMacros.h.  The appropriate macros will be used based on the
 * strategy of the given loop.  The strategy types take the form:
 *   <number of components per pixel><component data type><colorspace>
 * For example, these are the current strategy types:
 *   3ByteRgb    (currently only used as a glyph list blending strategy where
 *                the alpha value itself is neither blended nor stored)
 *   4ByteArgb   (eg. IntArgb, ThreeByteBgr, Ushort555Rgb, ByteIndexed, etc.)
 *   4ShortArgb  (not used currently; could be used when surface types using
 *                16 bits per component are implemented)
 *   1ByteGray   (eg. ByteGray)
 *   1ShortGray  (eg. UshortGray)
 * Note that the macros which operate on alpha values have the word "Alpha"
 * somewhere in their name.  Those macros that only operate on the color/gray
 * components of a given strategy will have the word "Components" or "Comps"
 * in their name.
 */


/*
 * MaxValFor ## STRATEGY
 */
#define MaxValFor4ByteArgb     0xff
#define MaxValFor1ByteGray     0xff
#define MaxValFor1ShortGray    0xffff


/*
 * AlphaType ## STRATEGY
 */
#define AlphaType3ByteRgb      jint
#define AlphaType4ByteArgb     jint
#define AlphaType1ByteGray     jint
#define AlphaType1ShortGray    juint


/*
 * ComponentType ## STRATEGY
 */
#define ComponentType3ByteRgb      jint
#define ComponentType4ByteArgb     jint
#define ComponentType1ByteGray     jint
#define ComponentType1ShortGray    juint


/*
 * DeclareAlphaVarFor ## STRATEGY(VAR)
 *
 * jint a;
 */
#define DeclareAlphaVarFor3ByteRgb(VAR) \
    AlphaType3ByteRgb VAR;

#define DeclareAlphaVarFor4ByteArgb(VAR) \
    AlphaType4ByteArgb VAR;

#define DeclareAlphaVarFor1ByteGray(VAR) \
    AlphaType1ByteGray VAR;

#define DeclareAlphaVarFor1ShortGray(VAR) \
    AlphaType1ShortGray VAR;


/*
 * DeclareAndInitAlphaVarFor ## STRATEGY(VAR, initval)
 *
 * jint a = initval;
 */
#define DeclareAndInitAlphaVarFor4ByteArgb(VAR, initval) \
    AlphaType4ByteArgb VAR = initval;

#define DeclareAndInitAlphaVarFor1ByteGray(VAR, initval) \
    AlphaType1ByteGray VAR = initval;

#define DeclareAndInitAlphaVarFor1ShortGray(VAR, initval) \
    AlphaType1ShortGray VAR = initval;


/*
 * DeclareAndClearAlphaVarFor ## STRATEGY(VAR)
 *
 * jint a = 0;
 */
#define DeclareAndClearAlphaVarFor4ByteArgb(VAR) \
    DeclareAndInitAlphaVarFor4ByteArgb(VAR, 0)

#define DeclareAndClearAlphaVarFor1ByteGray(VAR) \
    DeclareAndInitAlphaVarFor1ByteGray(VAR, 0)

#define DeclareAndClearAlphaVarFor1ShortGray(VAR) \
    DeclareAndInitAlphaVarFor1ShortGray(VAR, 0)


/*
 * DeclareAndSetOpaqueAlphaVarFor ## STRATEGY(VAR)
 *
 * jint a = 0xff;
 */
#define DeclareAndSetOpaqueAlphaVarFor4ByteArgb(VAR) \
    DeclareAndInitAlphaVarFor4ByteArgb(VAR, MaxValFor4ByteArgb)

#define DeclareAndSetOpaqueAlphaVarFor1ByteGray(VAR) \
    DeclareAndInitAlphaVarFor1ByteGray(VAR, MaxValFor1ByteGray)

#define DeclareAndSetOpaqueAlphaVarFor1ShortGray(VAR) \
    DeclareAndInitAlphaVarFor1ShortGray(VAR, MaxValFor1ShortGray)


/*
 * DeclareAndInvertAlphaVarFor ## STRATEGY(VAR, invalpha)
 *
 * jint a = 0xff - resA;
 */
#define DeclareAndInvertAlphaVarFor4ByteArgb(VAR, invalpha) \
    DeclareAndInitAlphaVarFor4ByteArgb(VAR, MaxValFor4ByteArgb - invalpha)

#define DeclareAndInvertAlphaVarFor1ByteGray(VAR, invalpha) \
    DeclareAndInitAlphaVarFor1ByteGray(VAR, MaxValFor1ByteGray - invalpha)

#define DeclareAndInvertAlphaVarFor1ShortGray(VAR, invalpha) \
    DeclareAndInitAlphaVarFor1ShortGray(VAR, MaxValFor1ShortGray - invalpha)


/*
 * DeclareCompVarsFor ## STRATEGY(PREFIX)
 *
 * jint c;
 */
#define DeclareCompVarsFor3ByteRgb(PREFIX) \
    ComponentType3ByteRgb PREFIX ## R, PREFIX ## G, PREFIX ## B;

#define DeclareCompVarsFor4ByteArgb(PREFIX) \
    ComponentType4ByteArgb PREFIX ## R, PREFIX ## G, PREFIX ## B;

#define DeclareCompVarsFor1ByteGray(PREFIX) \
    ComponentType1ByteGray PREFIX ## G;

#define DeclareCompVarsFor1ShortGray(PREFIX) \
    ComponentType1ShortGray PREFIX ## G;


/*
 * DeclareAndInitExtraAlphaFor ## STRATEGY(VAR)
 *
 * jint extraA = (int)(pCompInfo->details.extraAlpha * 255.0 + 0.5);
 */
#define DeclareAndInitExtraAlphaFor4ByteArgb(VAR) \
    AlphaType4ByteArgb VAR = \
        (AlphaType4ByteArgb)(pCompInfo->details.extraAlpha * 255.0 + 0.5);

#define DeclareAndInitExtraAlphaFor1ByteGray(VAR) \
    AlphaType1ByteGray VAR = \
        (AlphaType1ByteGray)(pCompInfo->details.extraAlpha * 255.0 + 0.5);

#define DeclareAndInitExtraAlphaFor1ShortGray(VAR) \
    AlphaType1ShortGray VAR = \
        (AlphaType1ShortGray)(pCompInfo->details.extraAlpha * 65535.0 + 0.5);


/*
 * PromoteByteAlphaFor ## STRATEGY(a)
 */
#define PromoteByteAlphaFor4ByteArgb(a)
#define PromoteByteAlphaFor1ByteGray(a)
#define PromoteByteAlphaFor1ShortGray(a) \
    (a) = (((a) << 8) + (a))


/*
 * DeclareAndInitPathAlphaFor ## STRATEGY(VAR)
 *
 * jint pathA = *pMask++;
 */
#define DeclareAndInitPathAlphaFor4ByteArgb(VAR) \
    AlphaType4ByteArgb VAR = *pMask++;

#define DeclareAndInitPathAlphaFor1ByteGray(VAR) \
    AlphaType1ByteGray VAR = *pMask++;

#define DeclareAndInitPathAlphaFor1ShortGray(VAR) \
    AlphaType1ShortGray VAR = *pMask++;


/*
 * MultiplyAlphaFor ## STRATEGY(a, b)
 *
 * a * b
 */
#define MultiplyAlphaFor4ByteArgb(a, b) \
    MUL8(a, b)

#define MultiplyAlphaFor1ByteGray(a, b) \
    MUL8(a, b)

#define MultiplyAlphaFor1ShortGray(a, b) \
    MUL16(a, b)


/*
 * MultiplyAndStore ## STRATEGY ## Comps(PROD_PREFIX, M1, M2_PREFIX)
 *
 * c = m1 * m2;
 */
#define MultiplyAndStore3Components(PROD_PREFIX, M1, M2_PREFIX, PRECISION) \
    do { \
        PROD_PREFIX ## R = MUL ## PRECISION(M1, M2_PREFIX ## R); \
        PROD_PREFIX ## G = MUL ## PRECISION(M1, M2_PREFIX ## G); \
        PROD_PREFIX ## B = MUL ## PRECISION(M1, M2_PREFIX ## B); \
    } while (0)

#define MultiplyAndStore1Component(PROD_PREFIX, M1, M2_PREFIX, PRECISION) \
    PROD_PREFIX ## G = MUL ## PRECISION(M1, M2_PREFIX ## G)

#define MultiplyAndStore4ByteArgbComps(PROD_PREFIX, M1, M2_PREFIX) \
    MultiplyAndStore3Components(PROD_PREFIX, M1, M2_PREFIX, 8)

#define MultiplyAndStore1ByteGrayComps(PROD_PREFIX, M1, M2_PREFIX) \
    MultiplyAndStore1Component(PROD_PREFIX, M1, M2_PREFIX, 8)

#define MultiplyAndStore1ShortGrayComps(PROD_PREFIX, M1, M2_PREFIX) \
    MultiplyAndStore1Component(PROD_PREFIX, M1, M2_PREFIX, 16)


/*
 * DivideAndStore ## STRATEGY ## Comps(QUOT_PREFIX, D1_PREFIX, D2)
 *
 * c = d1 / d2;
 */
#define DivideAndStore3Components(QUOT_PREFIX, D1_PREFIX, D2, PRECISION) \
    do { \
        QUOT_PREFIX ## R = DIV ## PRECISION(D1_PREFIX ## R, D2); \
        QUOT_PREFIX ## G = DIV ## PRECISION(D1_PREFIX ## G, D2); \
        QUOT_PREFIX ## B = DIV ## PRECISION(D1_PREFIX ## B, D2); \
    } while (0)

#define DivideAndStore1Component(QUOT_PREFIX, D1_PREFIX, D2, PRECISION) \
    QUOT_PREFIX ## G = DIV ## PRECISION(D1_PREFIX ## G, D2)

#define DivideAndStore4ByteArgbComps(QUOT_PREFIX, D1_PREFIX, D2) \
    DivideAndStore3Components(QUOT_PREFIX, D1_PREFIX, D2, 8)

#define DivideAndStore1ByteGrayComps(QUOT_PREFIX, D1_PREFIX, D2) \
    DivideAndStore1Component(QUOT_PREFIX, D1_PREFIX, D2, 8)

#define DivideAndStore1ShortGrayComps(QUOT_PREFIX, D1_PREFIX, D2) \
    DivideAndStore1Component(QUOT_PREFIX, D1_PREFIX, D2, 16)


/*
 * MultiplyAddAndStore ## STRATEGY ## Comps(RES_PREFIX, M1, \
 *                                          M2_PREFIX, A_PREFIX)
 *
 * c = (m1 * m2) + a;
 */
#define MultiplyAddAndStore3Components(RES_PREFIX, M1, M2_PREFIX, A_PREFIX, \
                                       PRECISION) \
    do { \
        RES_PREFIX ## R = MUL ## PRECISION(M1, M2_PREFIX ## R) + \
                                                          A_PREFIX ## R; \
        RES_PREFIX ## G = MUL ## PRECISION(M1, M2_PREFIX ## G) + \
                                                          A_PREFIX ## G; \
        RES_PREFIX ## B = MUL ## PRECISION(M1, M2_PREFIX ## B) + \
                                                          A_PREFIX ## B; \
    } while (0)

#define MultiplyAddAndStore1Component(RES_PREFIX, M1, M2_PREFIX, A_PREFIX, \
                                      PRECISION) \
    RES_PREFIX ## G = MUL ## PRECISION(M1, M2_PREFIX ## G) + A_PREFIX ## G

#define MultiplyAddAndStore4ByteArgbComps(RES_PREFIX, M1, M2_PREFIX, \
                                          A_PREFIX) \
    MultiplyAddAndStore3Components(RES_PREFIX, M1, M2_PREFIX, A_PREFIX, 8)

#define MultiplyAddAndStore1ByteGrayComps(RES_PREFIX, M1, M2_PREFIX, \
                                          A_PREFIX) \
    MultiplyAddAndStore1Component(RES_PREFIX, M1, M2_PREFIX, A_PREFIX, 8)

#define MultiplyAddAndStore1ShortGrayComps(RES_PREFIX, M1, M2_PREFIX, \
                                           A_PREFIX) \
    MultiplyAddAndStore1Component(RES_PREFIX, M1, M2_PREFIX, A_PREFIX, 16)


/*
 * MultMultAddAndStore ## STRATEGY ## Comps(RES_PREFIX, M1, M2_PREFIX, \
 *                                          M3, M4_PREFIX)
 *
 * c = (m1 * m2) + (m3 * m4);
 */
#define MultMultAddAndStore3Components(RES_PREFIX, M1, M2_PREFIX, \
                                       M3, M4_PREFIX, PRECISION) \
    do { \
        RES_PREFIX ## R = MUL ## PRECISION(M1, M2_PREFIX ## R) + \
                          MUL ## PRECISION(M3, M4_PREFIX ## R); \
        RES_PREFIX ## G = MUL ## PRECISION(M1, M2_PREFIX ## G) + \
                          MUL ## PRECISION(M3, M4_PREFIX ## G); \
        RES_PREFIX ## B = MUL ## PRECISION(M1, M2_PREFIX ## B) + \
                          MUL ## PRECISION(M3, M4_PREFIX ## B); \
    } while (0)


#define MultMultAddAndStoreLCD3Components(RES_PREFIX, M1, M2_PREFIX, \
                                       M3, M4_PREFIX, PRECISION) \
    do { \
        RES_PREFIX ## R = MUL ## PRECISION(M1 ## R, M2_PREFIX ## R) + \
                          MUL ## PRECISION(M3 ## R, M4_PREFIX ## R); \
        RES_PREFIX ## G = MUL ## PRECISION(M1 ## G, M2_PREFIX ## G) + \
                          MUL ## PRECISION(M3 ## G, M4_PREFIX ## G); \
        RES_PREFIX ## B = MUL ## PRECISION(M1 ## B, M2_PREFIX ## B) + \
                          MUL ## PRECISION(M3 ## B, M4_PREFIX ## B); \
    } while (0)

#define MultMultAddAndStore1Component(RES_PREFIX, M1, M2_PREFIX, \
                                      M3, M4_PREFIX, PRECISION) \
    RES_PREFIX ## G = MUL ## PRECISION(M1, M2_PREFIX ## G) + \
                      MUL ## PRECISION(M3, M4_PREFIX ## G)

#define MultMultAddAndStore3ByteRgbComps(RES_PREFIX, M1, M2_PREFIX, \
                                         M3, M4_PREFIX) \
    MultMultAddAndStore3Components(RES_PREFIX, M1, M2_PREFIX, \
                                   M3, M4_PREFIX, 8)

#define MultMultAddAndStoreLCD3ByteRgbComps(RES_PREFIX, M1, M2_PREFIX, \
                                         M3, M4_PREFIX) \
    MultMultAddAndStoreLCD3Components(RES_PREFIX, M1, M2_PREFIX, \
                                   M3, M4_PREFIX, 8)

#define MultMultAddAndStore4ByteArgbComps(RES_PREFIX, M1, M2_PREFIX, \
                                          M3, M4_PREFIX) \
    MultMultAddAndStore3Components(RES_PREFIX, M1, M2_PREFIX, \
                                   M3, M4_PREFIX, 8)

#define MultMultAddAndStoreLCD4ByteArgbComps(RES_PREFIX, M1, M2_PREFIX, \
                                          M3, M4_PREFIX) \
    MultMultAddAndStoreLCD3Components(RES_PREFIX, M1, M2_PREFIX, \
                                      M3, M4_PREFIX, 8)

#define MultMultAddAndStore1ByteGrayComps(RES_PREFIX, M1, M2_PREFIX, \
                                          M3, M4_PREFIX) \
    MultMultAddAndStore1Component(RES_PREFIX, M1, M2_PREFIX, \
                                  M3, M4_PREFIX, 8)

#define MultMultAddAndStore1ShortGrayComps(RES_PREFIX, M1, M2_PREFIX, \
                                           M3, M4_PREFIX) \
    RES_PREFIX ## G = AddNormalizedProducts16(M1, M2_PREFIX ## G, \
                                              M3, M4_PREFIX ## G)


/*
 * Store ## STRATEGY ## CompsUsingOp(L_PREFIX, OP, R_PREFIX)
 *
 * l op r;  // where op can be something like = or +=
 */
#define Store3ComponentsUsingOp(L_PREFIX, OP, R_PREFIX) \
    do { \
        L_PREFIX ## R OP R_PREFIX ## R; \
        L_PREFIX ## G OP R_PREFIX ## G; \
        L_PREFIX ## B OP R_PREFIX ## B; \
    } while (0)

#define Store1ComponentUsingOp(L_PREFIX, OP, R_PREFIX) \
    L_PREFIX ## G OP R_PREFIX ## G

#define Store4ByteArgbCompsUsingOp(L_PREFIX, OP, R_PREFIX) \
    Store3ComponentsUsingOp(L_PREFIX, OP, R_PREFIX)

#define Store1ByteGrayCompsUsingOp(L_PREFIX, OP, R_PREFIX) \
    Store1ComponentUsingOp(L_PREFIX, OP, R_PREFIX)

#define Store1ShortGrayCompsUsingOp(L_PREFIX, OP, R_PREFIX) \
    Store1ComponentUsingOp(L_PREFIX, OP, R_PREFIX)


/*
 * Set ## STRATEGY ## CompsToZero(PREFIX)
 *
 * c = 0;
 */
#define Set4ByteArgbCompsToZero(PREFIX) \
    PREFIX ## R = PREFIX ## G = PREFIX ## B = 0

#define Set1ByteGrayCompsToZero(PREFIX) \
    PREFIX ## G = 0

#define Set1ShortGrayCompsToZero(PREFIX) \
    PREFIX ## G = 0

#endif /* AlphaMath_h_Included */
