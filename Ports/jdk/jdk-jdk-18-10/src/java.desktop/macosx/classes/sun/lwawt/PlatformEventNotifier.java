/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Rectangle;

public interface PlatformEventNotifier {
    void notifyIconify(boolean iconify);

    void notifyZoom(boolean isZoomed);

    void notifyExpose(Rectangle r);

    void notifyReshape(int x, int y, int w, int h);

    void notifyUpdateCursor();

    void notifyActivation(boolean activation, LWWindowPeer opposite);

    // MouseDown in non-client area
    void notifyNCMouseDown();

    /*
     * Called by the delegate to dispatch the event to Java. Event
     * coordinates are relative to non-client window are, i.e. the top-left
     * point of the client area is (insets.top, insets.left).
     */
    void notifyMouseEvent(int id, long when, int button,
                          int x, int y, int absX, int absY,
                          int modifiers, int clickCount, boolean popupTrigger,
                          byte[] bdata);

    void notifyMouseWheelEvent(long when, int x, int y, final int absX,
                               final int absY, int modifiers, int scrollType,
                               int scrollAmount, int wheelRotation,
                               double preciseWheelRotation, byte[] bdata);
    /*
     * Called by the delegate when a key is pressed.
     */
    void notifyKeyEvent(int id, long when, int modifiers,
                        int keyCode, char keyChar, int keyLocation);
}
