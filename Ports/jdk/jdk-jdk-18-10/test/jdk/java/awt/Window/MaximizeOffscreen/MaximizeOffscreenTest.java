/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @test @summary JVM crash if the frame maximized from offscreen
 * @key headful
 * @bug 8020210
 * @author Petr Pchelko
 * @library ../../regtesthelpers
 * @build Util
 * @compile MaximizeOffscreenTest.java
 * @run main/othervm MaximizeOffscreenTest
 */

import test.java.awt.regtesthelpers.Util;

import javax.swing.*;
import java.awt.*;

public class MaximizeOffscreenTest {

    private static JFrame frame;

    public static void main(String[] args) throws Throwable {

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                constructTestUI();
            }
        });

        Util.waitForIdle(null);
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame.setExtendedState(Frame.MAXIMIZED_BOTH);
            }
        });
        Util.waitForIdle(null);
    }

    private static void constructTestUI() {
        frame = new JFrame("Test frame");
        frame.setUndecorated(true);
        frame.setBounds(-1000, -1000, 100, 100);
        frame.setVisible(true);
    }
}
