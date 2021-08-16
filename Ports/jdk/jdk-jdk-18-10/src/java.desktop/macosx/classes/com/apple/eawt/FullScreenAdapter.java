/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.apple.eawt;

import java.awt.Window;

import com.apple.eawt.event.FullScreenEvent;

/**
 * Abstract adapter class for receiving fullscreen events. This class is provided
 * as a convenience for creating listeners.
 *
 * Subclasses registered with {@link FullScreenUtilities#addFullScreenListenerTo(Window, FullScreenListener)}
 * will receive all entering/entered/exiting/exited full screen events.
 *
 * @see FullScreenUtilities
 *
 * @since Java for Mac OS X 10.7 Update 1
 */
public abstract class FullScreenAdapter implements FullScreenListener {
        public void windowEnteringFullScreen(final FullScreenEvent e) {}
        public void windowEnteredFullScreen(final FullScreenEvent e) {}
        public void windowExitingFullScreen(final FullScreenEvent e) {}
        public void windowExitedFullScreen(final FullScreenEvent e) {}
}
