/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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

#ifndef GLXSurfaceData_h_Included
#define GLXSurfaceData_h_Included

#include "J2D_GL/glx.h"
#include "awt_p.h"
#include "OGLSurfaceData.h"

#ifdef HEADLESS
#define GLXSDOps void
#else /* HEADLESS */

/**
 * The GLXSDOps structure contains the GLX-specific information for a given
 * OGLSurfaceData.  It is referenced by the native OGLSDOps structure.
 *
 *     Window window;
 * For onscreen windows, we maintain a reference to that window's associated
 * XWindow handle here.  Offscreen surfaces have no associated Window, so for
 * those surfaces, this value will simply be zero.
 *
 *     Drawable xdrawable;
 * If a GLXDrawable has a corresponding X11 Drawable, it is stored here.  For
 * example, each GLXWindow has an associated Window and each GLXPixmap has an
 * associated Pixmap.  GLXPbuffers have no associated X11 Drawable (they are
 * pure OpenGL surfaces), so for pbuffers, this field is set to zero;
 *
 *     GLXDrawable drawable;
 * The native handle to the GLXDrawable at the core of this surface.  A
 * GLXDrawable can be a Window, GLXWindow, GLXPixmap, or GLXPbuffer.
 *
 *     AwtGraphicsConfigData *configData;
 * A pointer to the AwtGraphicsConfigData under which this surface was
 * created.
 */
typedef struct _GLXSDOps {
    Window      window;
    Drawable    xdrawable;
    GLXDrawable drawable;
    struct _AwtGraphicsConfigData *configData;
} GLXSDOps;

#endif /* HEADLESS */

#endif /* GLXSurfaceData_h_Included */
