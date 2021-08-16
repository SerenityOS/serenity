/*
 * Copyright 2009 Red Hat, Inc.  All Rights Reserved.
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6721088
  @summary X11 Window sizes should be what we set them to
  @author Omair Majid <omajid@redhat.com>: area=awt.toplevel
  @run main TestFrameSize
 */

/**
 * TestFrameSize.java
 *
 * Summary: test that X11 Awt windows are drawn with correct sizes
 *
 * Test fails if size of window is wrong
 */

import java.awt.*;

public class TestFrameSize {

    static Dimension desiredDimensions = new Dimension(200, 200);
    static Frame mainWindow;

    private static Dimension getClientSize(Frame window) {
        Dimension size = window.getSize();
        Insets insets = window.getInsets();

        System.out.println("getClientSize() for " + window);
        System.out.println("   size: " + size);
        System.out.println("   insets: " + insets);

        return new Dimension(
                size.width - insets.left - insets.right,
                size.height - insets.top - insets.bottom);
    }

    public static void drawGui() {
        mainWindow = new Frame("");
        mainWindow.setPreferredSize(desiredDimensions);
        mainWindow.pack();

        Dimension actualDimensions = mainWindow.getSize();
        System.out.println("Desired dimensions: " + desiredDimensions.toString());
        System.out.println("Actual dimensions:  " + actualDimensions.toString());
        if (!actualDimensions.equals(desiredDimensions)) {
            throw new RuntimeException("Incorrect widow size");
        }

        // pack() guarantees to preserve the size of the client area after
        // showing the window.
        Dimension clientSize1 = getClientSize(mainWindow);
        System.out.println("Client size before showing: " + clientSize1);

        mainWindow.setVisible(true);

        try {
            Robot robot = new Robot();
            robot.waitForIdle();
        }catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Unexpected failure.");
        }

        Dimension clientSize2 = getClientSize(mainWindow);
        System.out.println("Client size after showing: " + clientSize2);

        if (!clientSize2.equals(clientSize1)) {
            throw new RuntimeException("Incorrect client area size.");
        }
    }

    public static void main(String[] args) {
        try {
            drawGui();
        } finally {
            if (mainWindow != null) {
                mainWindow.dispose();
            }
        }
    }
}
