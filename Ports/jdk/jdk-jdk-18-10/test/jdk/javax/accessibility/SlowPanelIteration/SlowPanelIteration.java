/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * @test
 * @key headful
 * @bug 8202768
 * @summary we should not hang when lots of panels are used
 */
public final class SlowPanelIteration {

    private static JFrame frame;
    private static Point center = new Point();
    private static volatile CountDownLatch go;

    public static void main(final String[] args) throws Exception {
        Robot r = new Robot();
        // accessibility tool will need time to react to our clicks
        r.setAutoDelay(200);
        try {
            EventQueue.invokeAndWait(SlowPanelIteration::showUI);
            for (int i = 0; i < 10; ++i) {
                go = new CountDownLatch(1);
                r.mouseMove(center.x, center.y);
                r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                if (!go.await(10, TimeUnit.SECONDS)) {
                    throw new RuntimeException("Too slow operation");
                }
            }
        } finally {
            EventQueue.invokeAndWait(SlowPanelIteration::dispose);
        }
    }

    private static void showUI() {
        frame = new JFrame();
        frame.setSize(new Dimension(400, 400));
        frame.setLocationRelativeTo(null);

        final Container content = frame.getContentPane();
        content.setLayout(new BorderLayout(0, 0));
        Container lastPanel = content;
        for (int i = 0; i < 500; i++) {
            final JPanel p = new JPanel();
            p.setLayout(new BorderLayout(0, 0));
            lastPanel.add(p);
            lastPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    System.out.println("click");
                    go.countDown();
                }
            });
            lastPanel = p;
        }

        lastPanel.setBackground(Color.GREEN);
        frame.setVisible(true);

        Point loc = frame.getLocationOnScreen();
        center.x = loc.x + frame.getWidth() / 2;
        center.y = loc.y + frame.getHeight() / 2;
    }

    private static void dispose() {
        if (frame != null) {
            frame.dispose();
        }
    }
}
