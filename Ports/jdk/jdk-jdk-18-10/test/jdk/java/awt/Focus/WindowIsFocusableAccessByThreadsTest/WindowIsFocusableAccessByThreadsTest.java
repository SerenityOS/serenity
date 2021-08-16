/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

/*
  @test
  @key headful
  @bug      8047288
  @summary  Tests method isFocusable of Window component. It should be accessed only from EDT
  @author   artem.malinko@oracle.com
  @library  ../../regtesthelpers
  @build    Util
  @run      main WindowIsFocusableAccessByThreadsTest
*/

import test.java.awt.regtesthelpers.Util;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class WindowIsFocusableAccessByThreadsTest {
    private static AtomicBoolean testPassed = new AtomicBoolean(true);
    private static volatile TestFrame frame;
    private static volatile TestWindow window;
    private static volatile Button openWindowBtn;

    public static void main(String[] args) {
        frame = new TestFrame("Test EDT access to Window components");
        window = new TestWindow(frame);

        SwingUtilities.invokeLater(WindowIsFocusableAccessByThreadsTest::init);

        Util.waitTillShown(frame);
        Robot robot = Util.createRobot();
        Util.clickOnComp(frame, robot, 100);
        Util.clickOnComp(openWindowBtn, robot, 100);

        Util.waitTillShown(window);

        if (!testPassed.get()) {
            throw new RuntimeException("Window component methods has been accessed not " +
                    "from Event Dispatching Thread");
        }
    }

    private static void init() {
        frame.setSize(400, 400);
        frame.setLayout(new FlowLayout());
        openWindowBtn = new Button("open window");
        openWindowBtn.addActionListener(e -> {
            window.setSize(100, 100);
            window.setLocation(400, 100);
            window.setVisible(true);
        });
        frame.add(openWindowBtn);
        frame.setVisible(true);
    }

    private static void testThread() {
        if (!SwingUtilities.isEventDispatchThread()) {
            testPassed.set(false);
        }
    }

    private static class TestWindow extends Window {
        public TestWindow(Frame owner) {
            super(owner);
        }

        // isFocusable method is final and we can't add this test to it.
        // But it invokes getFocusableWindowState and here we can check
        // if thread is EDT.
        @Override
        public boolean getFocusableWindowState() {
            testThread();
            return super.getFocusableWindowState();
        }
    }

    private static class TestFrame extends Frame {
        private TestFrame(String title) throws HeadlessException {
            super(title);
        }

        // isFocusable method is final and we can't add this test to it.
        // But it invokes getFocusableWindowState and here we can check
        // if thread is EDT.
        @Override
        public boolean getFocusableWindowState() {
            testThread();
            return super.getFocusableWindowState();
        }
    }
}
