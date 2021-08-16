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
package com.sun.swingset3.demos.window;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import com.sun.swingset3.DemoProperties;
import com.sun.swingset3.demos.DemoUtilities;
import java.lang.reflect.InvocationTargetException;

/**
 * @author aim
 */
@DemoProperties(
        value = "JWindow Demo",
        category = "Toplevel Containers",
        description = "Demonstrates JWindow, a toplevel container with no system border.",
        sourceFiles = {
            "com/sun/swingset3/demos/window/WindowDemo.java",
            "com/sun/swingset3/demos/DemoUtilities.java",
            "com/sun/swingset3/demos/window/resources/WindowDemo.html",
            "com/sun/swingset3/demos/window/resources/images/WindowDemo.gif"
        }
)
public final class WindowDemo extends JPanel {

    public static final String SHOW_J_WINDOW = "Show JWindow...";
    public static final String I_HAVE_NO_SYSTEM_BORDER = "I have no system border.";

    private JWindow window;

    private JComponent windowSpaceholder;

    public WindowDemo() {
        initComponents();
    }

    protected void initComponents() {
        window = createWindow();

        setLayout(new BorderLayout());
        add(createControlPanel(), BorderLayout.WEST);
        windowSpaceholder = createWindowSpaceholder(window);
        add(windowSpaceholder, BorderLayout.CENTER);
    }

    protected JComponent createControlPanel() {
        Box controlPanel = Box.createVerticalBox();
        controlPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // Create button to control visibility of frame
        JButton showButton = new JButton(SHOW_J_WINDOW);
        showButton.addActionListener(new ShowActionListener());
        controlPanel.add(showButton);

        return controlPanel;
    }

    private static JComponent createWindowSpaceholder(JWindow window) {
        JPanel windowPlaceholder = new JPanel();
        Dimension prefSize = window.getPreferredSize();
        prefSize.width += 12;
        prefSize.height += 12;
        windowPlaceholder.setPreferredSize(prefSize);

        return windowPlaceholder;
    }

    private static JWindow createWindow() {

        //<snip>Create window
        JWindow window = new JWindow();
        //</snip>

        //<snip>Add a border to the window
        window.getRootPane().setBorder(new LineBorder(Color.BLACK, 1));
        //</snip>

        //<snip>Add window's content
        JLabel label = new JLabel(I_HAVE_NO_SYSTEM_BORDER);
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setPreferredSize(new Dimension(250, 200));
        window.add(label);
        //</snip>

        //<snip>Initialize window's size
        // which will shrink-to-fit its contents
        window.pack();
        //</snip>

        return window;
    }

    public void start() {
        DemoUtilities.setToplevelLocation(window, windowSpaceholder, SwingConstants.CENTER);
        showWindow();
    }

    public void stop() {
        //<snip>Hide window
        window.setVisible(false);
        //</snip>
    }

    public void showWindow() {
        //<snip>Show window
        // if window already visible, then bring to the front
        if (window.isShowing()) {
            window.toFront();
        } else {
            window.setVisible(true);
        }
        //</snip>
    }

    private class ShowActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            showWindow();
        }
    }

    public static void main(String args[]) throws InterruptedException, InvocationTargetException {
        EventQueue.invokeAndWait(() -> {
            JFrame frame = new JFrame();
            WindowDemo demo = new WindowDemo();
            frame.add(demo);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            demo.start();
        });
    }
}
