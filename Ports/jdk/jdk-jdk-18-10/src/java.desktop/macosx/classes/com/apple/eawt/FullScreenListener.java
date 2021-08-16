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

import com.apple.eawt.event.FullScreenEvent;
import java.util.EventListener;


/**
 *
 *
 * @since Java for Mac OS X 10.7 Update 1
 */
public interface FullScreenListener extends EventListener {
        /**
     * Invoked when a window has started to enter full screen.
     * @param e containing the specific window entering full screen.
     */
        public void windowEnteringFullScreen(final FullScreenEvent e);

        /**
     * Invoked when a window has fully entered full screen.
     * @param e containing the specific window which has entered full screen.
     */
        public void windowEnteredFullScreen(final FullScreenEvent e);

        /**
     * Invoked when a window has started to exit full screen.
     * @param e containing the specific window exiting full screen.
     */
        public void windowExitingFullScreen(final FullScreenEvent e);

        /**
     * Invoked when a window has fully exited full screen.
     * @param e containing the specific window which has exited full screen.
     */
        public void windowExitedFullScreen(final FullScreenEvent e);
}
