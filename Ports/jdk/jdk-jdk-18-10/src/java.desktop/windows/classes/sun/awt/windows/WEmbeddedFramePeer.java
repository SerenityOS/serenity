/*
 * Copyright (c) 1996, 2015, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.windows;

import java.awt.Dialog;
import java.awt.Graphics;
import java.awt.Rectangle;

import sun.awt.EmbeddedFrame;
import sun.awt.Win32GraphicsEnvironment;

public class WEmbeddedFramePeer extends WFramePeer {

    public WEmbeddedFramePeer(EmbeddedFrame target) {
        super(target);
    }

    @Override
    native void create(WComponentPeer parent);

    // suppress printing of an embedded frame.
    @Override
    public void print(Graphics g) {}

    // supress calling native setMinSize()
    @Override
    public void updateMinimumSize() {}

    @Override
    public void modalDisable(Dialog blocker, long blockerHWnd)
    {
        super.modalDisable(blocker, blockerHWnd);
        ((EmbeddedFrame)target).notifyModalBlocked(blocker, true);
    }
    @Override
    public void modalEnable(Dialog blocker)
    {
        super.modalEnable(blocker);
        ((EmbeddedFrame)target).notifyModalBlocked(blocker, false);
    }

    @Override
    public void setBoundsPrivate(int x, int y, int width, int height) {
        setBounds(x, y, width, height, SET_BOUNDS | NO_EMBEDDED_CHECK);
    }

    @Override
    public native Rectangle getBoundsPrivate();

    @Override
    public boolean isAccelCapable() {
        // REMIND: Temp workaround for issues with using HW acceleration
        // in the browser on Vista when DWM is enabled
        // Note: isDWMCompositionEnabled is only relevant on Vista, returns
        // false on other systems.
        return !Win32GraphicsEnvironment.isDWMCompositionEnabled();
    }

}
