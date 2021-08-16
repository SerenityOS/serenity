/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Label;
import java.awt.List;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.ScrollPane;
import java.awt.Scrollbar;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.Window;

/**
 * @test
 * @key headful
 * @bug 8211999
 * @run main/othervm SetComponentsBounds
 * @run main/othervm -Dsun.java2d.uiScale=1 SetComponentsBounds
 * @run main/othervm -Dsun.java2d.uiScale=2.25 SetComponentsBounds
 */
public final class SetComponentsBounds {

    private static final int X = 111;
    private static final int Y = 222;
    private static final int WIDTH = 321;
    private static final int HEIGHT = 123;

    public static void main(String[] args) throws Exception {
        var ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (GraphicsDevice gd : ge.getScreenDevices()) {
            test(gd.getDefaultConfiguration(), true);
            test(gd.getDefaultConfiguration(), false);
        }
    }

    private static void test(GraphicsConfiguration gc, boolean visible) throws Exception {
        Rectangle screen = gc.getBounds();
        Window frame = new Frame();
        try {
            frame.setLayout(null); // trigger use the minimum size of
                                   // the peer
            frame.setBounds(screen.x + 100, screen.y + 100, 500, 500);
            frame.add(new Button());
            frame.add(new Canvas());
            frame.add(new Checkbox());
            frame.add(new Choice());
            frame.add(new Label());
            frame.add(new List());
            frame.add(new Scrollbar());
            frame.add(new ScrollPane());
            frame.add(new TextArea());
            frame.add(new TextField());
            for (Component comp : frame.getComponents()) {
                comp.setBounds(X, Y, WIDTH, HEIGHT);
            }
            if (visible) {
                frame.setVisible(true);
            } else {
                frame.pack();
            }
            Robot robot = new Robot();
            robot.waitForIdle();
            checkGC(gc, frame);
            for (Component comp : frame.getComponents()) {
                Rectangle bounds = comp.getBounds();
                if (bounds.x != X || bounds.y != Y || bounds.width != WIDTH) {
                    System.err.println("Screen bounds:" + screen);
                    System.err.println("Component:" + comp);
                    throw new RuntimeException("Wrong bounds:" + bounds);
                }
                if (bounds.height > HEIGHT) {
                    // different check for HEIGHT, it depends on the font
                    throw new RuntimeException("Wrong height:" + bounds.height);
                }
                checkGC(gc, comp);
            }
        } finally {
            frame.dispose();
        }
    }

    private static void checkGC(GraphicsConfiguration gc, Component comp) {
        GraphicsConfiguration compGC = comp.getGraphicsConfiguration();
        if (compGC != gc) {
            System.err.println("Expected GC:" + gc);
            System.err.println("Actual GC:" + compGC);
            throw new RuntimeException();
        }
    }
}
