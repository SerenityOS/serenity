/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8048506
 * @summary Tests that popup with null-owner does not throw NPE
 * @author Dmitry Markov
 */

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;

public class bug8048506 {

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(new MetalLookAndFeel());

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                createAndShowGUI();
            }
        });
        System.out.println("The test passed");
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("bug8048506");
        frame.setSize(400, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        PopupFactory factory = PopupFactory.getSharedInstance();

        // Create and show popup with owner
        Popup popup1 = factory.getPopup(frame, new JLabel("Popup with owner"), 100, 100);
        popup1.show();

        //Create and show popup without owner
        Popup popup2 = factory.getPopup(null, new JLabel("Popup without owner"), 200, 200);
        popup2.show();
    }
}

