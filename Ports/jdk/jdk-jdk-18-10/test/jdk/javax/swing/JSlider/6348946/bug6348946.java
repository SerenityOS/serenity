/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6348946
 * @summary Tests that JSlider's thumb moves in the right direction
 *          when it is used as a JTable cell editor.
 * @author Mikhail Lapshin
*/

import java.awt.*;
import java.awt.event.InputEvent;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class bug6348946 {

    private static JFrame frame;

    private static JPanel panel;
    private static Robot robot;

    private static volatile boolean passed = false;

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        robot.setAutoDelay(10);

        String lf = "javax.swing.plaf.metal.MetalLookAndFeel";
        UIManager.setLookAndFeel(lf);

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    setupUI();
                }
            });
            robot.waitForIdle();
            clickOnSlider();
            robot.waitForIdle();
            checkResult();
        } finally {
            stopEDT();
        }
    }

    private static void setupUI() {
        frame = new JFrame();

        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new ParameterTable(), BorderLayout.CENTER);
        frame.getContentPane().add(panel);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void clickOnSlider() throws Exception {
        Rectangle rect = getPanelRectangle();

        double clickX = rect.getX() + rect.getWidth() / 4;
        double clickY = rect.getY() + rect.getHeight() / 2;
        robot.mouseMove((int) clickX, (int) clickY);

        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
    }

    private static void checkResult(){
        if (passed) {
            System.out.println("Test passed");
        } else {
            throw new RuntimeException("The thumb moved " +
                    "to the right instead of the left!");
        }
    }

    private static void stopEDT() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                frame.dispose();
            }
        });
    }

    private static class ParameterTable extends JTable {
        public ParameterTable() {
            super(new Object[][]{{5}}, new String[]{"Value"});
            getColumnModel().getColumn(0).setCellRenderer(new Renderer());
            getColumnModel().getColumn(0).setCellEditor(new Editor());
        }
    }

    private static class Renderer implements TableCellRenderer {
        private JSlider slider = new JSlider(0, 10);

        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row, int col) {
            int val = (Integer) value;
            slider.setValue(val);
            return slider;
        }
    }

    private static class Editor extends AbstractCellEditor implements TableCellEditor {
        private JSlider slider = new JSlider(0, 10);

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected,
                                                     int row, int col) {
            int val = (Integer) value;
            slider.setValue(val);
            return slider;
        }

        public Editor() {
            slider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    if (!slider.getValueIsAdjusting()) {
                        passed = slider.getValue() <= 5;
                    }
                }
            });
        }

        public Object getCellEditorValue() {
            return slider.getValue();
        }
    }

    private static Rectangle getPanelRectangle() throws Exception{
        final Rectangle[] result = new Rectangle[1];

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                result[0] = new Rectangle(panel.getLocationOnScreen(), panel.getSize());
            }
        });

        return result[0];
    }
}
