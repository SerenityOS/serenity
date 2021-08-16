/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import static javax.swing.SwingUtilities.invokeAndWait;

/*
 * @test
 * @key headful
 * @bug 7195179
 * @summary Tests that combobox works with generified renderers
 * @author Sergey Malenkov
 */

public class Test7195179 {
    private static JFrame frame;
    public static void main(String[] args) throws Exception {
        invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    Integer[] items = {null, 1, 2, 3};
                    JComboBox<Integer> combo = new JComboBox<>(items);
                    JLabel label = new JLabel("choose:");
                    JPanel panel = new JPanel();
                    GroupLayout layout = new GroupLayout(panel);
                    panel.setLayout(layout);
                    label.setLabelFor(combo);
                    combo.setSelectedIndex(0);
                    combo.setRenderer(new ListCellRenderer<Integer>() {
                        private final BasicComboBoxRenderer renderer = new BasicComboBoxRenderer();

                        @Override
                        public Component getListCellRendererComponent(JList<? extends Integer> list, Integer value, int index, boolean isSelected, boolean cellHasFocus) {
                            return this.renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                        }
                    });
                    layout.setAutoCreateContainerGaps(true);
                    layout.setAutoCreateGaps(true);
                    layout.setHorizontalGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup().addComponent(label))
                            .addGroup(layout.createParallelGroup().addComponent(combo)));
                    layout.setVerticalGroup(layout
                            .createSequentialGroup()
                            .addGroup(layout
                                .createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(label)
                                .addComponent(combo)));

                    frame = new JFrame(getClass().getSimpleName());
                    frame.add(panel);
                    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    frame.pack();
                    frame.setVisible(true);
                } finally {
                    if (frame != null) frame.dispose();
                }
            }
        });
    }
}
