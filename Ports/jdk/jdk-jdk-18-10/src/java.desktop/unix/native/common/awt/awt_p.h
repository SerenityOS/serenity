/*
 * Copyright (c) 1995, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * Motif-specific data structures for AWT Java objects.
 *
 */
#ifndef _AWT_P_H_
#define _AWT_P_H_

#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#ifndef HEADLESS
#include <X11/extensions/Xrender.h>
#endif /* !HEADLESS */
#include "awt.h"
#include "awt_util.h"
#include "color.h"
#include "colordata.h"
#include "gdefs.h"

#ifndef HEADLESS
#ifndef min
#define min(a,b) ((a) <= (b)? (a):(b))
#endif
#ifndef max
#define max(a,b) ((a) >= (b)? (a):(b))
#endif
#endif /* !HEADLESS */

#define LOOKUPSIZE 32

#ifndef HEADLESS

typedef XRenderPictFormat *
XRenderFindVisualFormatFunc (Display *dpy, _Xconst Visual *visual);

typedef struct _AwtGraphicsConfigData  {
    int         awt_depth;
    Colormap    awt_cmap;
    XVisualInfo awt_visInfo;
    int         awt_num_colors;
    awtImageData *awtImage;
    int         (*AwtColorMatch)(int, int, int,
                                 struct _AwtGraphicsConfigData *);
    XImage      *monoImage;
    Pixmap      monoPixmap;      /* Used in X11TextRenderer_md.c */
    int         monoPixmapWidth; /* Used in X11TextRenderer_md.c */
    int         monoPixmapHeight;/* Used in X11TextRenderer_md.c */
    GC          monoPixmapGC;    /* Used in X11TextRenderer_md.c */
    int         pixelStride;     /* Used in X11SurfaceData.c */
    ColorData      *color_data;
    struct _GLXGraphicsConfigInfo *glxInfo;
    int         isTranslucencySupported; /* Uses Xrender to find this out. */
    XRenderPictFormat renderPictFormat; /*Used only if translucency supported*/
} AwtGraphicsConfigData;

typedef AwtGraphicsConfigData* AwtGraphicsConfigDataPtr;

typedef struct _AwtScreenData {
    int numConfigs;
    Window root;
    unsigned long whitepixel;
    unsigned long blackpixel;
    AwtGraphicsConfigDataPtr defaultConfig;
    AwtGraphicsConfigDataPtr *configs;
} AwtScreenData;

typedef AwtScreenData* AwtScreenDataPtr;

extern AwtGraphicsConfigDataPtr getDefaultConfig(int screen);
#endif /* !HEADLESS */

/* allocated and initialize a structure */
#define ZALLOC(T)       ((struct T *)calloc(1, sizeof(struct T)))

#ifndef HEADLESS

extern int awt_allocate_colors(AwtGraphicsConfigDataPtr);
extern void awt_allocate_systemrgbcolors(jint *, int, AwtGraphicsConfigDataPtr);

extern jobject awtJNI_GetColorModel(JNIEnv *, AwtGraphicsConfigDataPtr);
extern void awtJNI_CreateColorData (JNIEnv *, AwtGraphicsConfigDataPtr, int lock);

#endif /* !HEADLESS */
#endif           /* _AWT_P_H_ */
