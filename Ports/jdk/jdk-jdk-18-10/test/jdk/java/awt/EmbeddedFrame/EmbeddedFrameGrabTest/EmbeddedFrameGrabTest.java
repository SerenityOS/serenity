/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6345003 8171363 8211999
 * @summary grab problems with EmbeddedFrame
 * @requires (os.family == "windows")
 * @modules java.desktop/java.awt.peer
 * @modules java.desktop/sun.awt
 * @modules java.desktop/sun.awt.windows:open
 * @author Oleg.Semenov@sun.com area=EmbeddedFrame
 * @run main EmbeddedFrameGrabTest
 */

import java.awt.Frame;
import java.awt.peer.FramePeer;
import javax.swing.JComboBox;
import java.awt.Panel;
import java.awt.BorderLayout;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.Rectangle;
import java.awt.TextArea;
import java.awt.Dialog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import sun.awt.AWTAccessor;

public class EmbeddedFrameGrabTest {

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

        final Frame frame = new Frame("AWT Frame");
        frame.pack();
        frame.setSize(200, 200);
        frame.setLocationRelativeTo(null);
        FramePeer frame_peer = AWTAccessor.getComponentAccessor()
                                    .getPeer(frame);
        Class comp_peer_class
                = Class.forName("sun.awt.windows.WComponentPeer");
        Field hwnd_field = comp_peer_class.getDeclaredField("hwnd");
        hwnd_field.setAccessible(true);
        long hwnd = hwnd_field.getLong(frame_peer);

        Class clazz = Class.forName("sun.awt.windows.WEmbeddedFrame");
        Constructor constructor
                = clazz.getConstructor(new Class[]{long.class});
        final Frame embedded_frame
                = (Frame) constructor.newInstance(new Object[]{
                    new Long(hwnd)});;
        final JComboBox<String> combo = new JComboBox<>(new String[]{
            "Item 1", "Item 2"
        });
        combo.setSelectedIndex(1);
        final Panel p = new Panel();
        p.setLayout(new BorderLayout());
        embedded_frame.add(p, BorderLayout.CENTER);
        embedded_frame.setBounds(0, 0, 150, 150);
        embedded_frame.validate();
        p.add(combo);
        p.validate();
        frame.setVisible(true);
        Robot robot = new Robot();
        robot.delay(2000);
        Rectangle clos = new Rectangle(
                combo.getLocationOnScreen(), combo.getSize());
        robot.mouseMove(clos.x + clos.width / 2, clos.y + clos.height / 2);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.delay(1000);
        if (!combo.isPopupVisible()) {
            throw new RuntimeException("Combobox popup is not visible!");
        }
        robot.mouseMove(clos.x + clos.width / 2, clos.y + clos.height + 3);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.delay(1000);
        if (combo.getSelectedIndex() != 0) {
            throw new RuntimeException("Combobox selection has not changed!");
        }
        embedded_frame.remove(p);
        embedded_frame.dispose();
        frame.dispose();

    }

    public static void main(String args[]) throws Exception {
        new EmbeddedFrameGrabTest().init();
    }

}
