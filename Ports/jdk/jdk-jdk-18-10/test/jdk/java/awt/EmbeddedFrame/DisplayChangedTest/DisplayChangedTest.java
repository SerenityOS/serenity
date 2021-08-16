/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 4980592 8171363
 * @summary   switching user in XP causes an NPE in
 *            sun.awt.windows.WWindowPeer.displayChanged
 * @requires (os.family == "windows")
 * @modules java.desktop/java.awt.peer
 * @modules java.desktop/sun.awt.windows:open
 * @modules java.desktop/sun.awt
 * @author son@sparc.spb.su: area=embedded
 * @run main DisplayChangedTest
 */

import java.awt.Frame;
import java.awt.Dialog;
import java.awt.TextArea;
import java.awt.peer.ComponentPeer;
import java.awt.peer.FramePeer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

import sun.awt.AWTAccessor;

public class DisplayChangedTest {

    /**
     * Test fails if it throws any exception.
     *
     * @throws Exception
     */
    private void init() throws Exception {

        if (!System.getProperty("os.name").startsWith("Windows")) {
            System.out.println("This is Windows only test.");
            return;
        }

        Frame frame = new Frame("AWT Frame");
        frame.pack();

        FramePeer frame_peer = AWTAccessor.getComponentAccessor()
                .getPeer(frame);
        Class comp_peer_class = Class.forName("sun.awt.windows.WComponentPeer");
        Field hwnd_field = comp_peer_class.getDeclaredField("hwnd");
        hwnd_field.setAccessible(true);
        long hwnd = hwnd_field.getLong(frame_peer);

        Class clazz = Class.forName("sun.awt.windows.WEmbeddedFrame");
        Constructor constructor = clazz
                .getConstructor(new Class[]{long.class});
        Frame embedded_frame = (Frame) constructor
                .newInstance(new Object[]{new Long(hwnd)});
        frame.setVisible(true);

        ComponentPeer peer = AWTAccessor.getComponentAccessor().getPeer(
                embedded_frame);
        Class peerClass = peer.getClass();
        Method displayChangedM = peerClass.getMethod("displayChanged",
                new Class[0]);
        displayChangedM.invoke(peer, null);
        embedded_frame.dispose();
        frame.dispose();

    }

    public static void main(String args[]) throws Exception {
        new DisplayChangedTest().init();
    }

}
