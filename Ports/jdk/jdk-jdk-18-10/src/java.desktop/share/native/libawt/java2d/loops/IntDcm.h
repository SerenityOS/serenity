/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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

#ifndef IntDcm_h_Included
#define IntDcm_h_Included

typedef jint    IntDcmPixelType;
typedef jint    IntDcmElemType;

#define SwapIntDcmComponentsX123ToX321(pixel) \
    (((pixel) << 16) | \
     ((pixel) & 0xff00) | \
     (((pixel) >> 16) & 0xff))

#define SwapIntDcmComponentsX123ToC321(pixel) \
    (((pixel & 0xff) << 16) | \
     ((pixel) & 0xff00) | \
     (((pixel) >> 16) & 0xff))

#define SwapIntDcmComponentsX123ToS321(pixel) \
    (0xff000000 | \
     ((pixel) << 16) | \
     ((pixel) & 0xff00) | \
     (((pixel) >> 16) & 0xff))

#define SwapIntDcmComponents4123To4321(pixel) \
    ((((pixel) & 0xff) << 16) | \
     ((pixel) & 0xff00ff00) | \
     (((pixel) >> 16) & 0xff))

#define ExtractIntDcmComponentsX123(pixel, c1, c2, c3) \
    do { \
        (c3) = (pixel) & 0xff; \
        (c2) = ((pixel) >> 8) & 0xff; \
        (c1) = ((pixel) >> 16) & 0xff; \
    } while (0)

#define ExtractIntDcmComponents123X(pixel, c1, c2, c3) \
    do { \
        (c3) = ((pixel) >> 8) & 0xff; \
        (c2) = ((pixel) >> 16) & 0xff; \
        (c1) = ((pixel) >> 24) & 0xff; \
    } while (0)

#define ExtractIntDcmComponents1234(pixel, c1, c2, c3, c4) \
    do { \
        (c4) = (pixel) & 0xff; \
        (c3) = ((pixel) >> 8) & 0xff; \
        (c2) = ((pixel) >> 16) & 0xff; \
        (c1) = ((pixel) >> 24) & 0xff; \
    } while (0)

#define ComposeIntDcmComponentsX123(c1, c2, c3) \
    (((((c1) << 8) | (c2)) << 8) | (c3))

#define ComposeIntDcmComponents123X(c1, c2, c3) \
    ((((((c1) << 8) | (c2)) << 8) | (c3)) << 8)

#define ComposeIntDcmComponents1234(c1, c2, c3, c4) \
    (((((((c1) << 8) | (c2)) << 8) | (c3)) << 8) | (c4))

#endif /* IntDcm_h_Included */
