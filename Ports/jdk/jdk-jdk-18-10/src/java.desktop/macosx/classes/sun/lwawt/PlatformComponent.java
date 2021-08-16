/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Can be used to store information about native resource related to the
 * lightweight component.
 */
public interface PlatformComponent {

    /**
     * Initializes platform component.
     *
     * @param platformWindow already initialized {@code PlatformWindow}.
     */
    void initialize(PlatformWindow platformWindow);

    /**
     * Moves and resizes this component. The new location of the top-left corner
     * is specified by {@code x} and {@code y}, and the new size is specified by
     * {@code w} and {@code h}. The location is specified relative to the {@code
     * platformWindow}.
     *
     * @param x the X location of the component
     * @param y the Y location of the component
     * @param w the width of the component
     * @param h the height of the component
     */
    void setBounds(int x, int y, int w, int h);

    /**
     * Releases all of the native resources used by this {@code
     * PlatformComponent}.
     */
    void dispose();
}
