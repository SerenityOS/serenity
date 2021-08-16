/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.peer.MouseInfoPeer;
import sun.awt.ComponentFactory;

/**
 * @test
 * @key headful
 * @bug 8143316
 * @modules java.desktop/java.awt.peer
 *          java.desktop/sun.awt
 * @summary Crash Trend in 1.9.0-ea-b93 (sun.awt.DefaultMouseInfoPeer.fillPointWithCoords)
 */
public class PointerInfoCrashTest {

    public static void main(String[] args) {
        testMouseInfo();
        testMouseInfoPeer();
    }

    private static void testMouseInfo() {
        // call the getPointerInfo() before graphics devices initialization
        MouseInfo.getPointerInfo();
    }

    private static void testMouseInfoPeer() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        if (toolkit instanceof ComponentFactory) {
            ComponentFactory componentFactory = (ComponentFactory) toolkit;

            MouseInfoPeer mouseInfoPeer = componentFactory.getMouseInfoPeer();
            mouseInfoPeer.fillPointWithCoords(new Point());

            Window win = new Window(null);
            win.setSize(300, 300);
            win.setVisible(true);

            mouseInfoPeer.isWindowUnderMouse(win);
            win.dispose();
        }
    }
}
