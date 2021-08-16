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

/*
 * @test
 * @key headful
 * @bug 8236953
 * @summary JavaFX SwingNode is not rendered on macOS
 * @modules java.desktop/sun.swing
 */

import java.awt.EventQueue;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComponent;
import javax.swing.JPanel;

import sun.swing.JLightweightFrame;
import sun.swing.LightweightContent;

public final class ResizedMovedEvents {

    private static final AtomicInteger resized = new AtomicInteger();
    private static final AtomicInteger moved = new AtomicInteger();
    private static JLightweightFrame jLightweightFrame;

    public static void main(String[] args) throws Exception {
        emulateSwingNode(true);  // emulate visible node
        emulateSwingNode(false); // emulate invisible node
    }

    private static void emulateSwingNode(boolean visible) throws Exception {
        try {
            EventQueue.invokeAndWait(() -> {
                jLightweightFrame = new JLightweightFrame();
                jLightweightFrame.setContent(new XLightweightContent());
                jLightweightFrame.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        resized.incrementAndGet();
                    }

                    @Override
                    public void componentMoved(ComponentEvent e) {
                        moved.incrementAndGet();
                    }
                });
                jLightweightFrame.setVisible(visible);
            });

            // Some dummy initial location
            setBounds(10, 10, 10, 10);

            // resize and move
            resetFlags();
            setBounds(100, 100, 100, 100);
            checkFlags(1, 1);

            // resize only
            resetFlags();
            setBounds(100, 100, 200, 200);
            checkFlags(1, 0);

            // move only
            resetFlags();
            setBounds(200, 200, 200, 200);
            checkFlags(0, 1);
        } finally {
            if (jLightweightFrame != null) jLightweightFrame.dispose();
        }
    }

    private static void setBounds(int x, int y, int w, int h) throws Exception {
        EventQueue.invokeAndWait(() -> {
            jLightweightFrame.setBounds(x, y, w, h);
        });
        EventQueue.invokeAndWait(() -> {
            // dummy event to flush the EventQueue
        });
    }

    private static void resetFlags() {
        resized.set(0);
        moved.set(0);
    }

    private static void checkFlags(int expectedR, int expectedM) {
        int actualR = resized.get();
        int actualM = moved.get();
        //
        if (actualR < expectedR) {
            System.err.println("Expected: " + expectedR);
            System.err.println("Actual: " + actualR);
            throw new RuntimeException("Wrong number of COMPONENT_RESIZED");
        }
        if (actualM < expectedM) {
            System.err.println("Expected: " + expectedM);
            System.err.println("Actual: " + actualM);
            throw new RuntimeException("Wrong number of COMPONENT_MOVED");
        }
    }

    static final class XLightweightContent implements LightweightContent {
        @Override
        public JComponent getComponent() {
            return new JPanel();
        }

        @Override
        public void paintLock() {
        }

        @Override
        public void paintUnlock() {
        }

        @Override
        public void imageBufferReset(int[] data, int x, int y, int width,
                                     int height, int linestride,
                                     double scaleX,
                                     double scaleY) {
        }

        @Override
        public void imageReshaped(int x, int y, int width, int height) {
        }

        @Override
        public void imageUpdated(int dirtyX, int dirtyY, int dirtyWidth,
                                 int dirtyHeight) {
        }

        @Override
        public void focusGrabbed() {
        }

        @Override
        public void focusUngrabbed() {
        }

        @Override
        public void preferredSizeChanged(int width, int height) {
        }

        @Override
        public void maximumSizeChanged(int width, int height) {
        }

        @Override
        public void minimumSizeChanged(int width, int height) {
        }
    }
}
