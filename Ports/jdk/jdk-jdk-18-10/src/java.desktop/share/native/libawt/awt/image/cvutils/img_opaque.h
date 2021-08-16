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
 * This file contains macro definitions for the Alpha category of the
 * macros used by the generic scaleloop function.
 *
 * This implementation of the Alpha macros will ignore all alpha
 * information.  It also provides an empty expansion of the IfAlpha
 * macro which keeps the other macro sets in the image package from
 * wasting time and space on code to fetch or store the alpha
 * information.  This file is only applicable when the incoming
 * data is known to be entirely opaque and there is not yet any
 * image mask or alpha buffer associated with the output data.
 */

/*
 * The macro IfAlpha is used by the varous pixel conversion macros
 * to conditionally compile code that is only needed if alpha values
 * are going to be used.
 */
#define IfAlpha(statements)     /* Omit alpha handling code */

#define DeclareAlphaVars

#define InitAlpha(cvdata, dstY, dstX1, dstX2)                   \
    do {} while (0)

#define StartAlphaRow(cvdata, DSTX1, DSTY)                      \
    do {} while (0)

#define ApplyAlpha(cvdata, dstX, dstY, alpha)                   \
    do {} while (0)

#define EndMaskLine()                                           \
    do {} while (0)
