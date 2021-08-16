/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.swingset3.demos.gridbaglayout;

import java.awt.*;
import javax.swing.*;

import com.sun.swingset3.demos.JGridPanel;
import com.sun.swingset3.demos.ResourceManager;
import com.sun.swingset3.DemoProperties;

/**
 * GridBagLayout Demo
 *
 * @author Pavel Porvatov
 */
@DemoProperties(
        value = "GridBagLayout Demo",
        category = "Containers",
        description = "Demonstrates GridBagLayout, a layout which allows to arrange components in containers.",
        sourceFiles = {
            "com/sun/swingset3/demos/gridbaglayout/GridBagLayoutDemo.java",
            "com/sun/swingset3/demos/gridbaglayout/Calculator.java",
            "com/sun/swingset3/demos/JGridPanel.java",
            "com/sun/swingset3/demos/ResourceManager.java",
            "com/sun/swingset3/demos/gridbaglayout/resources/GridBagLayoutDemo.properties",
            "com/sun/swingset3/demos/gridbaglayout/resources/images/GridBagLayoutDemo.gif"
        }
)
public class GridBagLayoutDemo extends JPanel {

    private final ResourceManager resourceManager = new ResourceManager(this.getClass());

    private final JLabel lbCaption = new JLabel("<html>"
            + resourceManager.getString("GridBagLayoutDemo.caption.text") + "</html>");

    private final Calculator calculator = new Calculator();

    public static final String GRID_BAG_LAYOUT_DEMO_TITLE = GridBagLayoutDemo.class
            .getAnnotation(DemoProperties.class).value();

    /**
     * main method allows us to run as a standalone demo.
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame(GRID_BAG_LAYOUT_DEMO_TITLE);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new GridBagLayoutDemo());
        frame.setPreferredSize(new Dimension(800, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public GridBagLayoutDemo() {
        setLayout(new BorderLayout());

        initUI();
    }

    private void initUI() {
        JGridPanel pnContent = new JGridPanel(1, 0, 2);

        pnContent.setBorderEqual(10);

        pnContent.cell(lbCaption, JGridPanel.Layout.FILL).
                cell().
                cell(calculator, JGridPanel.Layout.CENTER, JGridPanel.Layout.FIRST).
                cell();

        add(pnContent);
    }
}
