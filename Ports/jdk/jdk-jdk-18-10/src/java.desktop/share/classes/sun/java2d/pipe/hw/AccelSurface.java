/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.pipe.hw;

import java.awt.Rectangle;
import sun.java2d.Surface;

import java.lang.annotation.Native;

/**
 * Abstraction for a hardware accelerated surface.
 */
public interface AccelSurface extends BufferedContextProvider, Surface {
    /**
     * Undefined
     */
    @Native public static final int UNDEFINED       = 0;
    /**
     * Window (or window substitute) surface
     */
    @Native public static final int WINDOW          = 1;
    /**
     * Render-To Plain surface (Render Target surface for Direct3D)
     */
    @Native public static final int RT_PLAIN        = 2;
    /**
     * Texture surface
     */
    @Native public static final int TEXTURE         = 3;
    /**
     * A back-buffer surface (SwapChain surface for Direct3D, backbuffer for
     * OpenGL)
     */
    @Native public static final int FLIP_BACKBUFFER = 4;
    /**
     * Render-To Texture surface (fbobject for OpenGL, texture with render-to
     * attribute for Direct3D)
     */
    @Native public static final int RT_TEXTURE      = 5;

    /**
     * Returns {@code int} representing surface's type as defined by constants
     * in this interface.
     *
     * @return an integer representing this surface's type
     * @see AccelSurface#UNDEFINED
     * @see AccelSurface#WINDOW
     * @see AccelSurface#RT_PLAIN
     * @see AccelSurface#TEXTURE
     * @see AccelSurface#FLIP_BACKBUFFER
     * @see AccelSurface#RT_TEXTURE
     */
    public int getType();

    /**
     * Returns a pointer to the native surface data associated with this
     * surface.
     * Note: this pointer is only valid on the rendering thread.
     *
     * @return pointer to the native surface's data
     */
    public long getNativeOps();

    /**
     * Returns a pointer to the real native resource
     * of the specified type associated with this AccelSurface.
     * Note: this pointer is only valid on the rendering thread.
     *
     * @param resType the type of the requested resource
     * @return a long containing a pointer to the native resource of the
     * specified type or 0L if such resource doesn't exist for this surface
     */
    public long getNativeResource(int resType);

    /**
     * Marks this surface dirty.
     */
    public void markDirty();

    /**
     * Returns whether the pipeline considers this surface valid. A surface
     * may become invalid if it is disposed of, or resized.
     *
     * @return true if valid, false otherwise
     */
    public boolean isValid();

    /**
     * Returns whether this surface is lost. The return value is only valid
     * on the render thread, meaning that even if this method returns
     * {@code true} it could be lost in the next moment unless it is called
     * on the rendering thread.
     *
     * @return true if the surface is known to be lost, false otherwise
     */
    public boolean isSurfaceLost();

    /**
     * Returns the requested bounds of the destination surface. The real bounds
     * of the native accelerated surface may differ. Use
     * {@link #getNativeBounds} to get the bounds of the native surface.
     *
     * @return Rectangle representing java surface's bounds
     */
    public Rectangle getBounds();

    /**
     * Returns real bounds of the native surface, which may differ from those
     * returned by {@link #getBounds}.
     *
     * @return Rectangle representing native surface's bounds
     */
    public Rectangle getNativeBounds();
}
