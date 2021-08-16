/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.swingset3.demos.tabbedpane;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;
import javax.swing.*;
import javax.swing.event.ChangeEvent;

import com.sun.swingset3.DemoProperties;
import com.sun.swingset3.demos.ResourceManager;

/**
 * JTabbedPane Demo
 *
 * @version 1.11 11/17/05
 * @author Jeff Dinkins
 */
@DemoProperties(
        value = "JTabbedPane Demo",
        category = "Containers",
        description = "Demonstrates JTabbedPane, a container which allows tabbed navigation of components",
        sourceFiles = {
            "com/sun/swingset3/demos/tabbedpane/TabbedPaneDemo.java",
            "com/sun/swingset3/demos/ResourceManager.java",
            "com/sun/swingset3/demos/tabbedpane/resources/TabbedPaneDemo.properties",
            "com/sun/swingset3/demos/tabbedpane/resources/images/blake.gif",
            "com/sun/swingset3/demos/tabbedpane/resources/images/brooke.gif",
            "com/sun/swingset3/demos/tabbedpane/resources/images/camille.jpg",
            "com/sun/swingset3/demos/tabbedpane/resources/images/david.gif",
            "com/sun/swingset3/demos/tabbedpane/resources/images/ewan.gif",
            "com/sun/swingset3/demos/tabbedpane/resources/images/ewan.jpg",
            "com/sun/swingset3/demos/tabbedpane/resources/images/miranda.jpg",
            "com/sun/swingset3/demos/tabbedpane/resources/images/matthew.gif",
            "com/sun/swingset3/demos/tabbedpane/resources/images/stephen.gif",
            "com/sun/swingset3/demos/tabbedpane/resources/images/TabbedPaneDemo.gif"
        }
)
public class TabbedPaneDemo extends JPanel implements ActionListener {

    private static final ResourceManager resourceManager = new ResourceManager(TabbedPaneDemo.class);
    public static final String BOUNCE = resourceManager.getString("TabbedPaneDemo.bounce");
    public static final String EWAN = resourceManager.getString("TabbedPaneDemo.ewan");
    public static final String MIRANDA = resourceManager.getString("TabbedPaneDemo.miranda");
    public static final String CAMILLE = resourceManager.getString("TabbedPaneDemo.camille");
    public static final String TAB_PLACEMENT = resourceManager.getString("TabbedPaneDemo.label");
    public static final String RIGHT = resourceManager.getString("TabbedPaneDemo.right");
    public static final String BOTTOM = resourceManager.getString("TabbedPaneDemo.bottom");
    public static final String LEFT = resourceManager.getString("TabbedPaneDemo.left");
    public static final String TOP = resourceManager.getString("TabbedPaneDemo.top");
    public static final String DEMO_TITLE = TabbedPaneDemo.class.getAnnotation(DemoProperties.class).value();

    private final HeadSpin spin;

    private final JTabbedPane tabbedpane;

    private final ButtonGroup group;

    private final JRadioButton top;
    private final JRadioButton bottom;
    private final JRadioButton left;
    private final JRadioButton right;

    /**
     * main method allows us to run as a standalone demo.
     *
     * @param args
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame(DEMO_TITLE);

        frame.getContentPane().add(new TabbedPaneDemo());
        frame.setPreferredSize(new Dimension(800, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * TabbedPaneDemo Constructor
     */
    public TabbedPaneDemo() {
        setLayout(new BorderLayout());

        // create tab position controls
        JPanel tabControls = new JPanel();
        tabControls.add(new JLabel(TAB_PLACEMENT));
        top = (JRadioButton) tabControls.add(new JRadioButton(TOP));
        left = (JRadioButton) tabControls.add(new JRadioButton(LEFT));
        bottom = (JRadioButton) tabControls.add(new JRadioButton(BOTTOM));
        right = (JRadioButton) tabControls.add(new JRadioButton(RIGHT));
        add(tabControls, BorderLayout.NORTH);

        group = new ButtonGroup();
        group.add(top);
        group.add(bottom);
        group.add(left);
        group.add(right);

        top.setSelected(true);

        top.addActionListener(this);
        bottom.addActionListener(this);
        left.addActionListener(this);
        right.addActionListener(this);

        // create tab
        tabbedpane = new JTabbedPane();
        add(tabbedpane, BorderLayout.CENTER);

        String name = CAMILLE;
        JLabel pix = new JLabel(resourceManager.createImageIcon("camille.jpg", name));
        tabbedpane.add(name, pix);

        name = MIRANDA;
        pix = new JLabel(resourceManager.createImageIcon("miranda.jpg", name));
        pix.setToolTipText(resourceManager.getString("TabbedPaneDemo.miranda.tooltip"));
        tabbedpane.add(name, pix);

        name = EWAN;
        pix = new JLabel(resourceManager.createImageIcon("ewan.jpg", name));
        tabbedpane.add(name, pix);

        name = BOUNCE;
        spin = new HeadSpin();
        tabbedpane.add(name, spin);

        tabbedpane.getModel().addChangeListener((ChangeEvent e) -> {
            SingleSelectionModel model = (SingleSelectionModel) e.getSource();
            if (model.getSelectedIndex() == tabbedpane.getTabCount() - 1) {
                spin.go();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == top) {
            tabbedpane.setTabPlacement(JTabbedPane.TOP);
        } else if (e.getSource() == left) {
            tabbedpane.setTabPlacement(JTabbedPane.LEFT);
        } else if (e.getSource() == bottom) {
            tabbedpane.setTabPlacement(JTabbedPane.BOTTOM);
        } else if (e.getSource() == right) {
            tabbedpane.setTabPlacement(JTabbedPane.RIGHT);
        }
    }

    private class HeadSpin extends JComponent implements ActionListener {

        private javax.swing.Timer animator;

        private final ImageIcon[] icon = new ImageIcon[6];

        private final static int numImages = 6;

        private final double[] x = new double[numImages];
        private final double[] y = new double[numImages];

        private final int[] xh = new int[numImages];
        private final int[] yh = new int[numImages];

        private final double[] scale = new double[numImages];

        private final Random rand = new Random();

        public HeadSpin() {
            setBackground(Color.black);
            icon[0] = resourceManager.createImageIcon("ewan.gif", resourceManager.getString("TabbedPaneDemo.ewan"));
            icon[1] = resourceManager.createImageIcon("stephen.gif", resourceManager.getString("TabbedPaneDemo.stephen"));
            icon[2] = resourceManager.createImageIcon("david.gif", resourceManager.getString("TabbedPaneDemo.david"));
            icon[3] = resourceManager.createImageIcon("matthew.gif", resourceManager.getString("TabbedPaneDemo.matthew"));
            icon[4] = resourceManager.createImageIcon("blake.gif", resourceManager.getString("TabbedPaneDemo.blake"));
            icon[5] = resourceManager.createImageIcon("brooke.gif", resourceManager.getString("TabbedPaneDemo.brooke"));

            /*
             for(int i = 0; i < 6; i++) {
                 x[i] = (double) rand.nextInt(500);
                 y[i] = (double) rand.nextInt(500);
             }
             */
        }

        public void go() {
            if (animator == null) {
                animator = new javax.swing.Timer(22 + 22 + 22, this);
            }
            animator.start();
        }

        @Override
        public void paint(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());

            for (int i = 0; i < numImages; i++) {
                if (x[i] > 3 * i) {
                    nudge(i);
                    squish(g, icon[i], xh[i], yh[i], scale[i]);
                } else {
                    x[i] += .05;
                    y[i] += .05;
                }
            }
        }

        public void nudge(int i) {
            x[i] += (double) rand.nextInt(1000) / 8756;
            y[i] += (double) rand.nextInt(1000) / 5432;
            int tmpScale = (int) (Math.abs(Math.sin(x[i])) * 10);
            scale[i] = (double) tmpScale / 10;
            int nudgeX = (int) (((double) getWidth() / 2) * .8);
            int nudgeY = (int) (((double) getHeight() / 2) * .60);
            xh[i] = (int) (Math.sin(x[i]) * nudgeX) + nudgeX;
            yh[i] = (int) (Math.sin(y[i]) * nudgeY) + nudgeY;
        }

        public void squish(Graphics g, ImageIcon icon, int x, int y, double scale) {
            if (isVisible()) {
                g.drawImage(icon.getImage(), x, y,
                        (int) (icon.getIconWidth() * scale),
                        (int) (icon.getIconHeight() * scale),
                        this);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (isShowing()) {
                repaint();
            } else {
                animator.stop();
            }
        }
    }
}
