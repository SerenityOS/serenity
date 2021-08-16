/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt.macosx;

final class CWrapper {
    private CWrapper() { }

    static final class NSWindow {
        // NSWindowOrderingMode
        static final int NSWindowAbove = 1;
        static final int NSWindowBelow = -1;
        static final int NSWindowOut = 0;

        // Window level constants
        // The number of supported levels: (we'll use more in the future)
        static final int MAX_WINDOW_LEVELS = 3;
        // The levels: (these are NOT real constants, these are keys. See native code.)
        static final int NSNormalWindowLevel = 0;
        static final int NSFloatingWindowLevel = 1;
        static final int NSPopUpMenuWindowLevel = 2;

        // 'level' is one of the keys defined above
        static native void setLevel(long window, int level);

        static native void makeKeyAndOrderFront(long window);
        static native void makeKeyWindow(long window);
        static native void makeMainWindow(long window);
        static native boolean canBecomeMainWindow(long window);
        static native boolean isKeyWindow(long window);

        static native void orderFront(long window);
        static native void orderFrontRegardless(long window);
        static native void orderWindow(long window, int ordered, long relativeTo);

        /**
         * Removes the window from the screen.
         *
         * @param window the pointer of the NSWindow
         */
        static native void orderOut(long window);

        /**
         * Removes the window from the screen and releases it. According to
         * documentation this method should be similar to {@link #orderOut},
         * because we use ReleasedWhenClosed:NO, so the window shouldn't be
         * released. But the close method works differently, for example it
         * close the space if the window was in the full screen via
         * {@link CPlatformWindow#toggleFullScreen()}.
         *
         * @param window the pointer of the NSWindow
         */
        static native void close(long window);

        static native void addChildWindow(long parent, long child, int ordered);
        static native void removeChildWindow(long parent, long child);

        static native void setAlphaValue(long window, float alpha);
        static native void setOpaque(long window, boolean opaque);

        /**
         * Sets background color of the NSWindow.
         *
         * @param window the pointer of the NSWindow
         * @param color the color in argb format
         */
        static native void setBackgroundColor(long window, int color);

        static native void miniaturize(long window);
        static native void deminiaturize(long window);
        static native boolean isZoomed(long window);
        static native void zoom(long window);

        static native void makeFirstResponder(long window, long responder);
    }

    static final class NSView {
        static native void addSubview(long view, long subview);
        static native void removeFromSuperview(long view);

        static native void setFrame(long view, int x, int y, int w, int h);
        static native long window(long view);

        static native void setHidden(long view, boolean hidden);

        static native void setToolTip(long view, String msg);
    }
}
