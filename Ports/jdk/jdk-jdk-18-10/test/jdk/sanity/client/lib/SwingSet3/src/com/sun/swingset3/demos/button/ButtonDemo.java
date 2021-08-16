/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.swingset3.demos.button;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.net.URISyntaxException;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.sun.swingset3.DemoProperties;
import com.sun.swingset3.demos.JHyperlink;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author aim
 */
@DemoProperties(
        value = "JButton Demo",
        category = "Controls",
        description = "Demonstrates the many uses of JButton, Swing's push button component.",
        sourceFiles = {
            "com/sun/swingset3/demos/button/ButtonDemo.java",
            "com/sun/swingset3/demos/JHyperlink.java",
            "com/sun/swingset3/demos/button/resources/ButtonDemo.html",
            "com/sun/swingset3/demos/button/resources/images/blogs.png",
            "com/sun/swingset3/demos/button/resources/images/ButtonDemo.gif",
            "com/sun/swingset3/demos/button/resources/images/document-print.png",
            "com/sun/swingset3/demos/button/resources/images/earth_day.gif",
            "com/sun/swingset3/demos/button/resources/images/earth_night.gif",
            "com/sun/swingset3/demos/button/resources/images/edit-find.png",
            "com/sun/swingset3/demos/button/resources/images/redbutton.png",
            "com/sun/swingset3/demos/button/resources/images/redbutton_dark.png",
            "com/sun/swingset3/demos/button/resources/images/redbutton_glow.png"
        }
)
public final class ButtonDemo extends JPanel {

    public static final String DEMO_TITLE = ButtonDemo.class.getAnnotation(DemoProperties.class).value();
    public static final String DO_IT_AGAIN = "Do it again";
    public static final String DO_IT = "Do it";
    public static final String BUTTON_WITH_TEXT_AND_IMAGE = "button with text and image";
    public static final String BUTTON_WITH_BACKGROUND_COLOR = "button with background color";
    public static final String GO = "Go";
    public static final String FIND = "Find";
    public static final String IMAGE_BUTTON = "image button";
    public static final String SIMPLE_BUTTON = "simple button";
    public static final String GET_MORE_INFO = "Get More Info";
    public static final String JAVA_BLOGS_URL = "https://blogs.oracle.com/java/";
    public static final String JAVA_SE_URL = "http://www.oracle.com/technetwork/java/javase/overview/index.html";
    public static final String BUTTON_WITH_ROLLOVER_IMAGE = "button with rollover image";
    public static final String BUTTON_WITH_NO_BORDER = "button with no border";
    public static final String CONNECT = "Connect";

    public ButtonDemo() {
        setToolTipText("Demonstrates JButton, Swing's push button component.");
        initComponents();
        setOpaque(false);
    }

    protected void initComponents() {
        setLayout(new GridLayout(0, 1));

        add(createSimpleButtonPanel());
        add(createCreativeButtonPanel());
    }

    protected JPanel createSimpleButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 8));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                "Simple Buttons"));

        //<snip>Create simple button
        final JButton simpleButton = new JButton(DO_IT);
        simpleButton.setToolTipText(SIMPLE_BUTTON);
        //</snip>
        //<snip>Add action listener using anonymous inner class
        // This style is useful when the action code is tied to a
        // single button instance and it's useful for simplicity
        // sake to keep the action code located near the button.
        // More global application actions should be implemented
        // using Action classes instead.
        simpleButton.addActionListener((ActionEvent event) -> {
            simpleButton.setText(DO_IT_AGAIN);
            // Need to force toplevel to relayout to accommodate new button size
            SwingUtilities.getWindowAncestor(simpleButton).validate();
        });
        //</snip>
        simpleButton.putClientProperty("snippetKey", "Create simple button");
        panel.add(simpleButton);

        //<snip>Create image button
        // Image is from the Java Look and Feel Graphics Repository
        JButton button = new JButton(new ImageIcon(getClass().
                getResource("resources/images/document-print.png")));
        button.setToolTipText(IMAGE_BUTTON);
        //</snip>
        button.putClientProperty("snippetKey", "Create image button");
        panel.add(button);

        //<snip>Create button with text and image
        // Image is from the Java Look and Feel Graphics Repository
        button = new JButton(FIND,
                new ImageIcon(getClass().
                        getResource("resources/images/edit-find.png")));
        button.setToolTipText(BUTTON_WITH_TEXT_AND_IMAGE);
        button.setHorizontalTextPosition(JButton.LEADING);
        button.setIconTextGap(6);
        //</snip>
        button.putClientProperty("snippetKey", "Create button with text and image");
        panel.add(button);

        //<snip>Create button with background color
        button = new JButton(GO);
        button.setBackground(Color.green);
        button.setContentAreaFilled(true);
        button.setOpaque(false);
        button.setToolTipText(BUTTON_WITH_BACKGROUND_COLOR);
        //</snip>
        button.putClientProperty("snippetKey", "Create button with background color");
        panel.add(button);

        return panel;
    }

    protected JPanel createCreativeButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 16, 8));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
                "More Interesting Buttons"));

        //<snip>Create button with no border
        JButton button = new JButton();
        button.setText(CONNECT);
        button.setIcon(new ImageIcon(getClass().getResource("resources/images/earth_day.gif")));
        button.setPressedIcon(new ImageIcon(getClass().getResource("resources/images/earth_night.gif")));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setVerticalTextPosition(JButton.BOTTOM);
        button.setHorizontalTextPosition(JButton.CENTER);
        button.setIconTextGap(0);
        button.setToolTipText(BUTTON_WITH_NO_BORDER);
        //</snip>
        button.putClientProperty("snippetKey", "Create button with no border");
        panel.add(button);

        //<snip>Create image button with rollover image
        button = new JButton();
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setIcon(new ImageIcon(getClass().getResource("resources/images/redbutton.png")));
        button.setRolloverEnabled(true);
        button.setRolloverIcon(new ImageIcon(getClass().getResource("resources/images/redbutton_glow.png")));
        button.setPressedIcon(new ImageIcon(getClass().getResource("resources/images/redbutton_dark.png")));
        button.setToolTipText(BUTTON_WITH_ROLLOVER_IMAGE);
        //</snip>
        button.putClientProperty("snippetKey", "Create image button with rollover image");
        panel.add(button);

        //<snip>Create HTML hyperlink
        JHyperlink hyperlink;
        try {
            hyperlink = new JHyperlink(GET_MORE_INFO, JAVA_SE_URL);
        } catch (URISyntaxException use) {
            use.printStackTrace();
            hyperlink = new JHyperlink(GET_MORE_INFO);
        }
        //</snip>
        hyperlink.putClientProperty("snippetKey", "Create HTML hyperlink");
        panel.add(hyperlink);

        //<snip>Create HTML image hyperlink
        try {
            hyperlink = new JHyperlink(
                    new ImageIcon(getClass().getResource("resources/images/blogs.png")), JAVA_BLOGS_URL);
        } catch (URISyntaxException use) {
            use.printStackTrace();
        }
        //</snip>
        button.putClientProperty("snippetKey", "Create HTML image hyperlink");
        panel.add(hyperlink);

        return panel;
    }

    public static void main(String args[]) throws InterruptedException, InvocationTargetException {
        final ButtonDemo buttonDemo = new ButtonDemo();

        javax.swing.SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame(DEMO_TITLE);
            frame.add(buttonDemo);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
