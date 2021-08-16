/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8071705
 * @summary  Java application menu misbehaves when running multiple screen stacked vertically
 * @build bug8071705
 * @run main/othervm bug8071705
 */

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class bug8071705 {

    public static void main(String[] args) throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean [] result = new boolean[1];

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = createGUI();
                GraphicsDevice[] devices = checkScreens();

                // check if we have more than one and if they are stacked
                // vertically
                GraphicsDevice device = checkConfigs(devices);
                if (device == null) {
                    // just pass the test
                    frame.dispose();
                    result[0] = true;
                    latch.countDown();
                } else {
                    FrameListener listener =
                            new FrameListener(device, latch, result);
                    frame.addComponentListener(listener);
                    frame.setVisible(true);
                }
            }
        });

        latch.await();

        if (result[0] == false) {
            throw new RuntimeException("popup menu rendered in wrong position");
        }

        System.out.println("OK");
    }

    private static GraphicsDevice[] checkScreens() {
        GraphicsEnvironment ge =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        return ge.getScreenDevices();
    }

    private static JFrame createGUI() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Some menu");
        menuBar.add(menu);

        for (int i = 0; i < 10; i++) {
            menu.add(new JMenuItem("Some menu #" + i));
        }

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(200, 200));
        frame.setJMenuBar(menuBar);
        return frame;
    }

    private static GraphicsDevice checkConfigs(GraphicsDevice[] devices) {

        GraphicsDevice correctDevice = null;
        if (devices.length < 2) {
            return correctDevice;
        }

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Rectangle screenBounds = new Rectangle(toolkit.getScreenSize());
        int halfScreen = screenBounds.height/2;

        for(int i = 0; i < devices.length; i++) {
            if(devices[i].getType() == GraphicsDevice.TYPE_RASTER_SCREEN) {
                GraphicsConfiguration conf =
                        devices[i].getDefaultConfiguration();
                Rectangle bounds = conf.getBounds();
                if (bounds.y >= halfScreen) {
                    // found
                    correctDevice = devices[i];
                    break;
                }
            }
        }
        return correctDevice;
    }

    private static class FrameListener extends ComponentAdapter {

        private GraphicsDevice device;
        private CountDownLatch latch;
        private boolean [] result;
        public FrameListener(GraphicsDevice device,
                             CountDownLatch latch,
                             boolean [] result)
        {
            this.device = device;
            this.latch = latch;
            this.result = result;
        }

        @Override
        public void componentShown(ComponentEvent e) {
            JFrame frame = (JFrame) e.getComponent();

            runActualTest(device, latch, frame, result);

            frame.setVisible(false);
            frame.dispose();
            latch.countDown();
        }
    }

    private static Rectangle setLocation(JFrame frame, GraphicsDevice device) {
        GraphicsConfiguration conf = device.getDefaultConfiguration();
        Rectangle bounds = conf.getBounds();

        // put just below half screen
        int x = bounds.x + bounds.width/2;
        int y = bounds.y + bounds.height/2;
        frame.setLocation(x, y);

        return bounds;
    }

    private static void runActualTest(GraphicsDevice device,
                                      CountDownLatch latch,
                                      JFrame frame,
                                      boolean [] result)
    {
        Rectangle screenBounds = setLocation(frame, device);
        JMenu menu = frame.getJMenuBar().getMenu(0);
        menu.doClick();

        Point location = menu.getLocationOnScreen();
        JPopupMenu pm = menu.getPopupMenu();
        Dimension pmSize = pm.getSize();

        int yOffset = UIManager.getInt("Menu.submenuPopupOffsetY");
        int height = location.y + yOffset + pmSize.height + menu.getHeight();
        int available = screenBounds.y + screenBounds.height - height;
        if (available > 0) {
            Point origin = pm.getLocationOnScreen();
            if (origin.y < location.y) {
                // growing upward, wrong!
                result[0] = false;
            } else {
                // growing downward, ok!
                result[0] = true;
            }
        } else {
            // there is no space, growing upward would be ok, so we pass
            result[0] = true;
        }
    }
}
