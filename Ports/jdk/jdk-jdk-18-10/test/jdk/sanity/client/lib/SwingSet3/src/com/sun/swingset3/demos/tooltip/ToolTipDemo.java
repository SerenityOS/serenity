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

package com.sun.swingset3.demos.tooltip;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.sun.swingset3.DemoProperties;
import com.sun.swingset3.demos.ResourceManager;

/**
 * ToolTip Demo
 *
 * @version 1.9 11/17/05
 * @author Jeff Dinkins
 */
@DemoProperties(
        value = "ToolTips Demo",
        category = "General",
        description = "Demonstrates how tooltips can be easily added to Swing GUI components",
        sourceFiles = {
                "com/sun/swingset3/demos/tooltip/ToolTipDemo.java",
                "com/sun/swingset3/demos/ResourceManager.java",
                "com/sun/swingset3/demos/tooltip/resources/ToolTipDemo.properties",
                "com/sun/swingset3/demos/tooltip/resources/images/tooltip_background.png",
                "com/sun/swingset3/demos/tooltip/resources/images/ToolTipDemo.gif"
                }
)
public class ToolTipDemo extends JPanel {

    public static final String DEMO_TITLE = ToolTipDemo.class.getAnnotation(DemoProperties.class).value();
    private final static ResourceManager resourceManager = new ResourceManager(ToolTipDemo.class);
    public static final String PLAIN_TOOLTIP_COMP_TITLE = resourceManager.getString("ToolTipDemo.plain");;
    public static final String PLAIN_TOOLTIP_TEXT = "A simple one line tip.";
    public static final String HTML_TOOLTIP_COMP_TITLE = resourceManager.getString("ToolTipDemo.html");;
    public static final String HTML_TOOLTIP_TEXT = "<html><body bgcolor=\"#AABBFF\">In case you thought that tooltips had to be<p>" +
            "boring, one line descriptions, the <font color=blue size=+2>Swing!</font> team<p>" +
            "is happy to shatter your illusions.<p>" +
            "In Swing, you can use HTML to <ul><li>Have Lists<li><b>Bold</b> text<li><em>emphasized</em>" +
            "text<li>text with <font color=red>Color</font><li>text in different <font size=+3>sizes</font>" +
            "<li>and <font face=AvantGarde>Fonts</font></ul>Oh, and they can be multi-line, too.</body></html>";
    public static final String STYLE_TOOLTIP_COMP_TITLE = resourceManager.getString("ToolTipDemo.styled");
    public static final String STYLE_TOOLTIP_TEXT = "<html>Tips can be styled to be" +
            "<br><b>interesting</b> and <i>fun</i></html>";

    /**
     * main method allows us to run as a standalone demo.
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame(ToolTipDemo.class.getAnnotation(DemoProperties.class).value());

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new ToolTipDemo());
        frame.setPreferredSize(new Dimension(800, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * ToolTipDemo Constructor
     */
    public ToolTipDemo() {
        setLayout(new BorderLayout());

        // Create a panel which contains specific tooltip regions.
        Toolbox toolbox = new Toolbox();

        add(toolbox, BorderLayout.CENTER);
    }

    public class Toolbox extends JPanel {
        private final Rectangle plainRect = new Rectangle(44, 0, 186, 128);
        private final Rectangle htmlRect = new Rectangle(240, 134, 186, 186);
        private final Rectangle styledRect = new Rectangle(45, 327, 188, 134);

        private final JLabel background;
        private final JComponent plainToolTipRegion;
        private final JComponent htmlToolTipRegion;
        private final JComponent styledToolTipRegion;

        public Toolbox() {
            setLayout(null);

            background = new JLabel(resourceManager.createImageIcon("tooltip_background.png",
                    resourceManager.getString("ToolTipDemo.toolbox")));

            background.setVerticalAlignment(JLabel.TOP);
            background.setHorizontalAlignment(JLabel.LEFT);

            // Note: tooltip text isn't retrieved from properties file in order
            // to make this code easier to understand

            //<snip>Create region for displaying plain tooltip
            plainToolTipRegion = createToolTipRegion(PLAIN_TOOLTIP_COMP_TITLE);
            plainToolTipRegion.setToolTipText(PLAIN_TOOLTIP_TEXT);
            //</snip>

            //<snip>Create region for displaying HTML tooltip
            htmlToolTipRegion = createToolTipRegion(HTML_TOOLTIP_COMP_TITLE);
            htmlToolTipRegion.setToolTipText(HTML_TOOLTIP_TEXT);
            //</snip>

            //<snip>Create region for displaying styled tooltip
            styledToolTipRegion = createToolTipRegion(STYLE_TOOLTIP_COMP_TITLE);
            styledToolTipRegion.setToolTipText(STYLE_TOOLTIP_TEXT);
            //</snip>

            add(htmlToolTipRegion);
            add(styledToolTipRegion);
            add(plainToolTipRegion);

            add(background);
        }

        public void doLayout() {
            background.setBounds(0, 0, getWidth(), getHeight());
            plainToolTipRegion.setBounds(plainRect);
            htmlToolTipRegion.setBounds(htmlRect);
            styledToolTipRegion.setBounds(styledRect);
        }

        private JComponent createToolTipRegion(String text) {
            JLabel region = new JLabel(text);
            region.setForeground(Color.white);
            region.setFont(getFont().deriveFont(18f));
            region.setHorizontalAlignment(JLabel.CENTER);
            region.setVerticalAlignment(JLabel.CENTER);
            return region;
        }
    }
}
