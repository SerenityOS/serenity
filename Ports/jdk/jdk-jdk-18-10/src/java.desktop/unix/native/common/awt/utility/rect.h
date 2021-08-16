/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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

/* platform-dependent definitions */

#ifndef _AWT_RECT_H
#define _AWT_RECT_H

#ifndef MACOSX
#include <X11/Xlib.h>
typedef XRectangle RECT_T;
#else
// OSX still needs this for BitmapToYXBandedRectangles
typedef struct {
    int x;
    int y;
    int width;
    int height;
} RECT_T;
#endif /* !MACOSX */

#define RECT_EQ_X(r1,r2)        ((r1).x==(r2).x && (r1).width==(r2).width)

#define RECT_SET(r,xx,yy,ww,hh)  \
    do {                         \
        (r).x=(xx);              \
        (r).y=(yy);              \
        (r).width=(ww);          \
        (r).height=(hh);         \
    } while (0)

#define RECT_INC_HEIGHT(r)      (r).height++

#if defined(__cplusplus)
extern "C" {
#endif

int BitmapToYXBandedRectangles(int bitsPerPixel, int width, int height,
        unsigned char * buf, RECT_T * outBuf);

#if defined(__cplusplus)
}
#endif

#endif // _AWT_RECT_H
