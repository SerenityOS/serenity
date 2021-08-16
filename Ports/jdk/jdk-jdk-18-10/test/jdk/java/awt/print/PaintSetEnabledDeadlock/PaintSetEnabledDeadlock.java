/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7108598 8172009
 * @summary Container.paint/KeyboardFocusManager.clearMostRecentFocusOwner methods deadlock
 * @library ../../regtesthelpers
 * @author Oleg Pekhovskiy
 * @build Util
 * @run main PaintSetEnabledDeadlock
 */

import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import test.java.awt.regtesthelpers.Util;

public class PaintSetEnabledDeadlock extends Frame {

    final TestPanel panel;
    final Button button;

    public static void main(String[] args) {
        PaintSetEnabledDeadlock frame = new PaintSetEnabledDeadlock();
        frame.setSize(200, 200);
        frame.setVisible(true);

        Robot robot = Util.createRobot();
        robot.setAutoDelay(100);
        robot.setAutoWaitForIdle(true);

        for (int i = 0; i < 20; ++i) {
            Util.clickOnComp(frame.panel, robot);
            Util.clickOnComp(frame.button, robot);
        }

        boolean ret = frame.panel.stop();
        frame.dispose();

        if (!ret) {
            throw new RuntimeException("Test failed!");
        }
        System.out.println("Test passed.");
    }

    public PaintSetEnabledDeadlock() {
        super("7108598 test");
        setLayout(new GridLayout(1, 2));
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                panel.stop();
                System.exit(0);
            }
        });
        panel = new TestPanel();
        add(panel);
        button = new Button("Enable");
        button.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                panel.setEnabled(true);
                panel.sync();
                panel.repaint();
            }
        });
        add(button);
    }
}

class TestPanel extends Panel implements Runnable {

    Image image = null;
    Thread thread = null;
    volatile boolean active = true;
    final Object sync = new Object();
    Panel panel = this;

    public TestPanel() {
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                synchronized (panel) {
                    sync();
                    panel.setEnabled(false);
                }
                panel.repaint();
            }
        });
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void paint(Graphics paramGraphics) {
        synchronized (getTreeLock()) {
            Rectangle rect = getBounds();
            if (image == null) {
                image = createImage(rect.width, rect.height);
            }

            if (image != null) {
                paramGraphics.drawImage(image, 0, 0, this);
            }
        }
    }

    @Override
    public void run() {
        while (active) {
            try {
                synchronized (sync) {
                    sync.wait();
                }
            } catch (InterruptedException ex) {
            }
            if (active) {
                draw();
            }
        }
    }

    public boolean stop() {
        active = false;
        try {
            sync();
            thread.join(1000);
            if (thread.isAlive()) {
                thread.interrupt();
                return false;
            }
        } catch (InterruptedException ex) {
            return false;
        }
        return true;
    }

    public void draw() {
        synchronized (getTreeLock()) {
            if (image != null) {
                Graphics localGraphics = image.getGraphics();
                Dimension size = getSize();
                localGraphics.setColor(isEnabled() ? Color.green : Color.red);
                localGraphics.fillRect(0, 0, size.width, size.height);
                super.paint(localGraphics);
                localGraphics.dispose();
                getTreeLock().notifyAll();
            }
        }
    }

    public void sync() {
        synchronized (sync) {
            sync.notify();
        }
    }
}
