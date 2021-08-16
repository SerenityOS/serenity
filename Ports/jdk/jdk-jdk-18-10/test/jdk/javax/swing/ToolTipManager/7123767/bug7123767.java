/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      7123767
 *
 * @summary  Check if a tooltip location in Multi-Monitor
 *           configurations is correct.
 *           If the configurations number per device exceeds 5,
 *           then some 5 random configurations will be checked.
 *           Please Use -Dseed=X to set the random generator seed
 *           (if necessary).
 *
 * @author   Vladislav Karnaukhov
 *
 * @key      headful randomness
 *
 * @modules  java.desktop/sun.awt
 * @library  /test/lib
 *
 * @run      main/timeout=300 bug7123767
 */

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import jdk.test.lib.RandomFactory;


public class bug7123767 extends JFrame {

    // maximum number of GraphicsConfigurations checked per GraphicsDevice
    private static final int MAX_N_CONFIGS = 5;
    private static final List<GraphicsConfiguration> CONFIGS = getConfigs();

    private static List<GraphicsConfiguration> getConfigs() {

        Random rnd = RandomFactory.getRandom();

        List<GraphicsConfiguration> configs = new ArrayList<>();

        GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = ge.getScreenDevices();

        for (GraphicsDevice device : devices) {
            GraphicsConfiguration[] allConfigs = device.getConfigurations();
            int nConfigs = allConfigs.length;
            if (nConfigs <= MAX_N_CONFIGS) {
                Collections.addAll(configs, allConfigs);
            } else { // see JDK-8159454
                System.out.println("check only " + MAX_N_CONFIGS +
                    " configurations for device " + device);
                configs.add(device.getDefaultConfiguration()); // check default
                for (int j = 0; j < MAX_N_CONFIGS - 1; j++) {
                    int k = rnd.nextInt(nConfigs);
                    configs.add(allConfigs[k]);
                }
            }
        }

        return configs;
    }


    private static class TestFactory extends PopupFactory {

        private static TestFactory newFactory = new TestFactory();
        private static PopupFactory oldFactory;

        private TestFactory() {
            super();
        }

        public static void install() {
            if (oldFactory == null) {
                oldFactory = getSharedInstance();
                setSharedInstance(newFactory);
            }
        }

        public static void uninstall() {
            if (oldFactory != null) {
                setSharedInstance(oldFactory);
            }
        }

        // Actual test happens here
        @Override
        public Popup getPopup(Component owner, Component contents, int x, int y) {

            GraphicsConfiguration mouseGC =
                testGC(MouseInfo.getPointerInfo().getLocation());

            if (mouseGC == null) {
                throw new RuntimeException("Can't find GraphicsConfiguration "
                        + "that mouse pointer belongs to");
            }

            GraphicsConfiguration tipGC = testGC(new Point(x, y));
            if (tipGC == null) {
                throw new RuntimeException(
                        "Can't find GraphicsConfiguration that tip belongs to");
            }

            if (!mouseGC.equals(tipGC)) {
                throw new RuntimeException("Mouse and tip GCs are not equal");
            }

            return super.getPopup(owner, contents, x, y);
        }

        private static GraphicsConfiguration testGC(Point pt) {

            for (GraphicsConfiguration config: CONFIGS) {

                Rectangle rect = config.getBounds();
                Insets insets =
                    Toolkit.getDefaultToolkit().getScreenInsets(config);
                adjustInsets(rect, insets);
                if (rect.contains(pt)) { return config; }
            }

            return null;
        }
    }

    private static final int MARGIN = 10;
    private static bug7123767 frame;
    private static Robot robot;

    public static void main(String[] args) throws Exception {

        UIManager.setLookAndFeel(new MetalLookAndFeel());
        setUp();
        testToolTip();
        TestFactory.uninstall();
        if (frame != null) { frame.dispose(); }
    }

    // Creates a window that is stretched across all available monitors
    // and adds itself as ContainerListener to track tooltips drawing
    private bug7123767() {

        super();

        ToolTipManager.sharedInstance().setInitialDelay(0);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        TestFactory.install();

        JLabel label1 = new JLabel("no preferred location");
        label1.setToolTipText("tip");
        add(label1, BorderLayout.WEST);

        JLabel label2 = new JLabel("preferred location (20000, 20000)") {
            public Point getToolTipLocation(MouseEvent event) {
                return new Point(20000, 20000);
            }
        };

        label2.setToolTipText("tip");
        add(label2, BorderLayout.EAST);

        setUndecorated(true);
        pack();

        Rectangle rect = new Rectangle();

        for (GraphicsConfiguration config: CONFIGS) {

            Insets localInsets =
                Toolkit.getDefaultToolkit().getScreenInsets(config);
            Rectangle localRect = config.getBounds();
            adjustInsets(localRect, localInsets);
            rect.add(localRect);
        }

        setBounds(rect);
    }

    private static void setUp() throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame = new bug7123767();
                frame.setVisible(true);
            }
        });
    }

    // Moves mouse pointer to the corners of every GraphicsConfiguration
    private static void testToolTip() throws AWTException {

        robot = new Robot();
        robot.setAutoDelay(20);
        robot.waitForIdle();

        for (GraphicsConfiguration config: CONFIGS) {

            Rectangle rect = config.getBounds();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
            adjustInsets(rect, insets);

            // Upper left
            glide(rect.x + rect.width / 2, rect.y + rect.height / 2,
                    rect.x + MARGIN, rect.y + MARGIN);
            robot.waitForIdle();

            // Lower left
            glide(rect.x + rect.width / 2, rect.y + rect.height / 2,
                    rect.x + MARGIN, rect.y + rect.height - MARGIN);
            robot.waitForIdle();

            // Upper right
            glide(rect.x + rect.width / 2, rect.y + rect.height / 2,
                    rect.x + rect.width - MARGIN, rect.y + MARGIN);
            robot.waitForIdle();

            // Lower right
            glide(rect.x + rect.width / 2, rect.y + rect.height / 2,
                    rect.x + rect.width - MARGIN, rect.y + rect.height - MARGIN);

            robot.waitForIdle();
        }
    }

    private static void glide(int x0, int y0, int x1, int y1) throws AWTException {
        if (robot == null) {
            robot = new Robot();
            robot.setAutoDelay(20);
        }

        float dmax = (float) Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        float dx = (x1 - x0) / dmax;
        float dy = (y1 - y0) / dmax;

        robot.mouseMove(x0, y0);
        for (int i = 1; i <= dmax; i += 10) {
            robot.mouseMove((int) (x0 + dx * i), (int) (y0 + dy * i));
        }
    }

    private static void adjustInsets(Rectangle rect, final Insets insets) {
        rect.x += insets.left;
        rect.y += insets.top;
        rect.width -= (insets.left + insets.right);
        rect.height -= (insets.top + insets.bottom);
    }
}
