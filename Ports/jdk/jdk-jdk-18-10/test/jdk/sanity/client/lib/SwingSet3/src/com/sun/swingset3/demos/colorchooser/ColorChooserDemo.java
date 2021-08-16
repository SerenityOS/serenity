/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.swingset3.demos.colorchooser;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.sun.swingset3.demos.JGridPanel;
import com.sun.swingset3.demos.ResourceManager;
import com.sun.swingset3.DemoProperties;

/**
 * JColorChooserDemo
 *
 * @author Jeff Dinkins
 * @version 1.1 07/16/99
 */
@DemoProperties(value = "JColorChooser Demo", category = "Choosers", description = "Demonstrates JColorChooser, a component which allows the user to pick a color.", sourceFiles = {
        "com/sun/swingset3/demos/colorchooser/ColorChooserDemo.java",
        "com/sun/swingset3/demos/colorchooser/BezierAnimationPanel.java", "com/sun/swingset3/demos/JGridPanel.java",
        "com/sun/swingset3/demos/ResourceManager.java",
        "com/sun/swingset3/demos/colorchooser/resources/ColorChooserDemo.properties",
        "com/sun/swingset3/demos/colorchooser/resources/images/ColorChooserDemo.gif" })
public class ColorChooserDemo extends JPanel {

    private static final ResourceManager resourceManager = new ResourceManager(ColorChooserDemo.class);

    private final BezierAnimationPanel bezAnim = new BezierAnimationPanel();

    public static final String BACKGROUND = resourceManager.getString("ColorChooserDemo.background");
    public static final String GRADIENT_1 = resourceManager.getString("ColorChooserDemo.grad_a");
    public static final String GRADIENT_2 = resourceManager.getString("ColorChooserDemo.grad_b");
    public static final String PERIMETER = resourceManager.getString("ColorChooserDemo.outer_line");
    public static final String CHOOSER_TITLE = resourceManager.getString("ColorChooserDemo.chooser_title");

    private final JButton outerColorButton = new JButton(PERIMETER);

    private final JButton backgroundColorButton = new JButton(BACKGROUND);

    private final JButton gradientAButton = new JButton(GRADIENT_1);

    private final JButton gradientBButton = new JButton(GRADIENT_2);

    public static final String DEMO_TITLE = ColorChooserDemo.class.getAnnotation(DemoProperties.class).value();

    /**
     * main method allows us to run as a standalone demo.
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame(ColorChooserDemo.class.getAnnotation(DemoProperties.class).value());

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new ColorChooserDemo());
        frame.setPreferredSize(new Dimension(800, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * ColorChooserDemo Constructor
     */
    public ColorChooserDemo() {
        setLayout(new BorderLayout());

        outerColorButton.setIcon(new ColorSwatch(BezierAnimationPanel.BezierColor.OUTER));

        backgroundColorButton.setIcon(new ColorSwatch(BezierAnimationPanel.BezierColor.BACKGROUND));

        gradientAButton.setIcon(new ColorSwatch(BezierAnimationPanel.BezierColor.GRADIENT_A));

        gradientBButton.setIcon(new ColorSwatch(BezierAnimationPanel.BezierColor.GRADIENT_B));

        ActionListener l = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JButton button = (JButton) e.getSource();

                final BezierAnimationPanel.BezierColor bezierColor = ((ColorSwatch) button.getIcon()).getBezierColor();

                Color current = bezAnim.getBezierColor(bezierColor);

                final JColorChooser chooser = new JColorChooser(current != null ? current : Color.WHITE);

                ActionListener colorChooserListener = new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        bezAnim.setBezierColor(bezierColor, chooser.getColor());
                    }
                };

                JDialog dialog = JColorChooser.createDialog(ColorChooserDemo.this, CHOOSER_TITLE, true, chooser,
                        colorChooserListener, null);

                dialog.setVisible(true);
            }
        };

        outerColorButton.addActionListener(l);
        backgroundColorButton.addActionListener(l);
        gradientAButton.addActionListener(l);
        gradientBButton.addActionListener(l);

        // Add control buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 15, 0));

        buttonPanel.add(backgroundColorButton);
        buttonPanel.add(gradientAButton);
        buttonPanel.add(gradientBButton);
        buttonPanel.add(outerColorButton);

        // Add everything to the panel
        JGridPanel pnContent = new JGridPanel(1, 0, 1);

        pnContent.cell(buttonPanel, JGridPanel.Layout.CENTER).cell(bezAnim);

        pnContent.setBorder(new EmptyBorder(10, 0, 0, 0));

        add(pnContent);
    }

    private class ColorSwatch implements Icon {
        private final BezierAnimationPanel.BezierColor bezierColor;

        public ColorSwatch(BezierAnimationPanel.BezierColor bezierColor) {
            this.bezierColor = bezierColor;
        }

        public int getIconWidth() {
            return 11;
        }

        public int getIconHeight() {
            return 11;
        }

        public BezierAnimationPanel.BezierColor getBezierColor() {
            return bezierColor;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.black);
            g.fillRect(x, y, getIconWidth(), getIconHeight());
            g.setColor(bezAnim.getBezierColor(bezierColor));
            g.fillRect(x + 2, y + 2, getIconWidth() - 4, getIconHeight() - 4);
        }
    }
}