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
 * @bug 8013566
 * @summary Failure of GroupLayout in combination of addPreferredGap and addGroup's
 * last row
 * @author Semyon Sadetsky
 */

import javax.swing.*;

public class bug8013566 {

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                final JFrame frame = new JFrame();
                try {
                    frame.setUndecorated(true);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    test(frame);


                } finally {
                    frame.dispose();
                }
            }
        });

        System.out.println("ok");
    }

    static void test(JFrame frame) {
        JComponent c1 = new JButton("Label1");
        JComponent c2 = new JButton("Label22");
        JComponent c3 = new JButton("Label333");

        JPanel panel = new JPanel();
        GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateContainerGaps(true);
        layout.setAutoCreateGaps(true);
        panel.setLayout(layout);

        layout.setHorizontalGroup(layout.createSequentialGroup().addGroup(
                layout.createParallelGroup().addGroup(
                        layout.createSequentialGroup().addComponent(c1)
                                .addPreferredGap(
                                        LayoutStyle.ComponentPlacement.RELATED,
                                        50, 200))
                        .addComponent(c2)).addComponent(c3));

        layout.setVerticalGroup(layout.createSequentialGroup()
                        .addComponent(c1).addComponent(c2).addComponent(c3)
        );

        frame.setContentPane(panel);
        frame.pack();
        frame.setVisible(true);

        if (c3.getX() != c1.getX() + c1.getWidth() + 50) {
            throw new RuntimeException(
                    "Gap between 1st and 3rd component is wrong");
        }

    }
}
