/*
 * Copyright (c) 2000, 2011, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

/**
 * Capabilities and properties of images.
 * @author Michael Martak
 * @since 1.4
 */
public class ImageCapabilities implements Cloneable {

    private boolean accelerated = false;

    /**
     * Creates a new object for specifying image capabilities.
     * @param accelerated whether or not an accelerated image is desired
     */
    public ImageCapabilities(boolean accelerated) {
        this.accelerated = accelerated;
    }

    /**
     * Returns {@code true} if the object whose capabilities are
     * encapsulated in this {@code ImageCapabilities} can be or is
     * accelerated.
     * @return whether or not an image can be, or is, accelerated.  There are
     * various platform-specific ways to accelerate an image, including
     * pixmaps, VRAM, AGP.  This is the general acceleration method (as
     * opposed to residing in system memory).
     */
    public boolean isAccelerated() {
        return accelerated;
    }

    /**
     * Returns {@code true} if the {@code VolatileImage}
     * described by this {@code ImageCapabilities} can lose
     * its surfaces.
     * @return whether or not a volatile image is subject to losing its surfaces
     * at the whim of the operating system.
     */
    public boolean isTrueVolatile() {
        return false;
    }

    /**
     * @return a copy of this ImageCapabilities object.
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // Since we implement Cloneable, this should never happen
            throw new InternalError(e);
        }
    }

}
