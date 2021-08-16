/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt;

/**
 * This interface can be implemented on a Graphics object to allow
 * the lightweight component code to permanently install a rectangular
 * maximum clip that cannot be extended with setClip and which works in
 * conjunction with the hit() and getTransform() methods of Graphics2D
 * to make it appear as if there really was a component with these
 * dimensions.
 */
public interface ConstrainableGraphics {
    /**
     * Constrain this graphics object to have a permanent device space
     * origin of (x, y) and a permanent maximum clip of (x,y,w,h).
     * Calling this method is roughly equivalent to:
     *    g.translate(x, y);
     *    g.clipRect(0, 0, w, h);
     * except that the clip can never be extended outside of these
     * bounds, even with setClip() and for the fact that the (x,y)
     * become a new device space coordinate origin.
     *
     * These methods are recursive so that you can further restrict
     * the object by calling the constrain() method more times, but
     * you can never enlarge its restricted maximum clip.
     */
    public void constrain(int x, int y, int w, int h);
}
