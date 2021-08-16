/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt;

import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.Component;
import java.awt.Image;

/**
 * As lwawt can be used on different platforms with different graphic
 * configurations, the general set of methods is necessary. This interface
 * collects the methods that should be provided by GraphicsConfiguration,
 * simplifying use by the LWAWT.
 *
 * @author Sergey Bylokhov
 */
public interface LWGraphicsConfig {

    /*
     * A GraphicsConfiguration must implements following methods to indicate
     * that it imposes certain limitations on the maximum size of supported
     * textures.
     */

    /**
     * Returns the maximum width of any texture image. By default return {@code
     * Integer.MAX_VALUE}.
     */
    int getMaxTextureWidth();

    /**
     * Returns the maximum height of any texture image. By default return {@code
     * Integer.MAX_VALUE}.
     */
    int getMaxTextureHeight();

    /*
     * The following methods correspond to the multi-buffering methods in
     * LWComponentPeer.java.
     */

    /**
     * Checks that the requested configuration is natively supported; if not, an
     * AWTException is thrown.
     */
    void assertOperationSupported(int numBuffers, BufferCapabilities caps)
            throws AWTException;

    /**
     * Creates a back buffer for the given peer and returns the image wrapper.
     */
    Image createBackBuffer(LWComponentPeer<?, ?> peer);

    /**
     * Destroys the back buffer object.
     */
    void destroyBackBuffer(Image backBuffer);

    /**
     * Performs the native flip operation for the given target Component. Our
     * flip is implemented through normal drawImage() to the graphic object,
     * because of our components uses a graphic object of the container(in this
     * case we also apply necessary constrains)
     */
    void flip(LWComponentPeer<?, ?> peer, Image backBuffer, int x1, int y1,
              int x2, int y2, BufferCapabilities.FlipContents flipAction);

    /**
     * Creates a new hidden-acceleration image of the given width and height
     * that is associated with the target Component.
     */
    Image createAcceleratedImage(Component target, int width, int height);
}
