
/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8139581
 * @summary AWT components are not drawn after removal and addition to a container
 * @author Anton Litvinov
 */

import java.awt.Button;
import java.awt.Color;
import java.awt.Canvas;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Panel;
import java.util.ArrayList;

public class ComponentIsNotDrawnAfterRemoveAddTest {
    private final Frame frame;
    private final Panel panel;
    private final ArrayList<Testable> compList = new ArrayList<Testable>();

    public ComponentIsNotDrawnAfterRemoveAddTest() {
        frame = new Frame("ComponentIsNotDrawnAfterRemoveAddTest");
        frame.setSize(500, 500);
        frame.setLocation(200, 200);
        frame.setLayout(null);
        frame.setBackground(Color.RED);

        panel = new Panel();
        panel.setLayout(null);
        panel.setBounds(25, 100, 455, 295);
        panel.setBackground(Color.GREEN);

        for (int i = 0; i < 10; i++) {
            TestCanvas canv1 = new TestCanvas();
            canv1.setBounds(i * 45 + 5, 15, 30 + i, 30 + i);
            panel.add(canv1);
            compList.add(canv1);

            TestButton btn1 = new TestButton();
            btn1.setBounds(i * 45 + 5, 60, 30 + i, 30 + i);
            panel.add(btn1);
            compList.add(btn1);

            TestCanvas canv2 = new TestCanvas();
            canv2.setBounds(i * 45 + 5, 105, 30 + i, 30 + i);
            panel.add(canv2);
            compList.add(canv2);

            TestButton btn2 = new TestButton();
            btn2.setBounds(i * 45 + 5, 150, 30 + i, 30 + i);
            panel.add(btn2);
            compList.add(btn2);

            TestCanvas canv3 = new TestCanvas();
            canv3.setBounds(i * 45 + 5, 195, 30 + i, 30 + i);
            panel.add(canv3);
            compList.add(canv3);

            TestButton btn3 = new TestButton();
            btn3.setBounds(i * 45 + 5, 240, 30 + i, 30 + i);
            panel.add(btn3);
            compList.add(btn3);
        }

        frame.add(panel);
        frame.setVisible(true);
    }

    private void runTest() {
        try {
            doSleep(1500);
            checkTestableComponents();

            for (int i = 0; i < 5; i++) {
                System.err.println(String.format("Test iteration #%d:", i));

                frame.remove(panel);
                frame.invalidate();
                frame.validate();
                frame.add(panel);

                doSleep(1500);
                checkTestableComponents();
            }
        } finally {
            frame.dispose();
        }
    }

    private void doSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    private void checkTestableComponents() throws RuntimeException {
        int notDrawnCompsCount = 0;
        for (Testable comp : compList) {
            if (!comp.wasPaintCalled()) {
                notDrawnCompsCount++;
            } else {
                comp.resetPaintCalledFlag();
            }
        }
        if (notDrawnCompsCount > 0) {
            throw new RuntimeException(String.format(
                "'paint' method of %d components was not called.", notDrawnCompsCount));
        }
    }

    private interface Testable {
        boolean wasPaintCalled();
        void resetPaintCalledFlag();
    }

    private static class TestCanvas extends Canvas implements Testable {
        private volatile boolean paintWasCalled = false;

        @Override
        public void paint(Graphics g) {
            paintWasCalled = true;
            super.paint(g);
            g.setColor(Color.BLUE);
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        @Override
        public boolean wasPaintCalled() {
            return paintWasCalled;
        }

        @Override
        public void resetPaintCalledFlag() {
            paintWasCalled = false;
        }
    }

    private static class TestButton extends Button implements Testable {
        private volatile boolean paintWasCalled = false;

        @Override
        public void paint(Graphics g) {
            paintWasCalled = true;
            super.paint(g);
            g.setColor(Color.YELLOW);
            g.fillRect(0, 0, 15, 15);
        }

        @Override
        public boolean wasPaintCalled() {
            return paintWasCalled;
        }

        @Override
        public void resetPaintCalledFlag() {
            paintWasCalled = false;
        }
    }

    public static void main(String[] args) {
        new ComponentIsNotDrawnAfterRemoveAddTest().runTest();
    }
}
