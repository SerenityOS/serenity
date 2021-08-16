/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 6396526
 * @summary Verify below-the-spot IM in the swing L&F JFrame.
 *          Although the swing component is decorated with L&F
 *          the IM window should have no decoration.
 * @author yuriko.yamasaki
 */

import java.awt.*;
import javax.swing.*;

public class IMLookAndFeel {
    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Suggest that the L&F (rather than the system)
        //decorate all windows.  This must be invoked before
        //creating the JFrame.  Native look and feels will
        //ignore this hint.
        //Create and set up the window.

        JFrame.setDefaultLookAndFeelDecorated(true);
        JFrame frame = new JFrame("IM with L&F");
        //frame.setUndecorated( true );

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JTextArea description = new JTextArea("Please try typing using below-the-spot IM.\n\n eg. Use City IM with the following arguement:\n    -Djava.awt.im.style=below-the-spot");
        description.setPreferredSize(new Dimension(250, 70));
        description.setEditable(false);
        frame.getContentPane().add(description, BorderLayout.NORTH);

        JTextField textField = new JTextField();
        textField.setPreferredSize(new Dimension(275, 50));
        frame.getContentPane().add(textField, BorderLayout.CENTER);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
