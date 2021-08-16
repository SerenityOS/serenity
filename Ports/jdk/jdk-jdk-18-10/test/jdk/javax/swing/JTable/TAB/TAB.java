/*
 * Copyright (c) 1999, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.DefaultCellEditor;
import javax.swing.JApplet;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * @test
 * @bug 4128521
 * @summary
 *     Tabbing test
 * @author milne
 * @run applet/manual=yesno TAB.html
 */
public class TAB extends JApplet
{
    static void initTest(Container contentPane)
    {
        // Take the dummy data from SwingSet.
        final String[] names = {"First Name", "Last Name", "Favorite Color",
                "Favorite Number", "Vegetarian"};
        final Object[][] data = {
                {"Mark", "Andrews", "Red", new Integer(2), new Boolean(true)},
                {"Tom", "Ball", "Blue", new Integer(99), new Boolean(false)},
                {"Alan", "Chung", "Green", new Integer(838), new Boolean(false)},
                {"Jeff", "Dinkins", "Turquois", new Integer(8), new Boolean(true)},
                {"Amy", "Fowler", "Yellow", new Integer(3), new Boolean(false)},
                {"Brian", "Gerhold", "Green", new Integer(0), new Boolean(false)},
                {"James", "Gosling", "Pink", new Integer(21), new Boolean(false)},
                {"David", "Karlton", "Red", new Integer(1), new Boolean(false)},
                {"Dave", "Kloba", "Yellow", new Integer(14), new Boolean(false)},
                {"Peter", "Korn", "Purple", new Integer(12), new Boolean(false)},
                {"Phil", "Milne", "Purple", new Integer(3), new Boolean(false)},
                {"Dave", "Moore", "Green", new Integer(88), new Boolean(false)},
                {"Hans", "Muller", "Maroon", new Integer(5), new Boolean(false)},
                {"Rick", "Levenson", "Blue", new Integer(2), new Boolean(false)},
                {"Tim", "Prinzing", "Blue", new Integer(22), new Boolean(false)},
                {"Chester", "Rose", "Black", new Integer(0), new Boolean(false)},
                {"Ray", "Ryan", "Gray", new Integer(77), new Boolean(false)},
                {"Georges", "Saab", "Red", new Integer(4), new Boolean(false)},
                {"Willie", "Walker", "Phthalo Blue", new Integer(4), new Boolean(false)},
                {"Kathy", "Walrath", "Blue", new Integer(8), new Boolean(false)},
                {"Arnaud", "Weber", "Green", new Integer(44), new Boolean(false)}
        };

        // Create a model of the data.
        TableModel dataModel = new AbstractTableModel() {
            // These methods always need to be implemented.
            public int getColumnCount() { return names.length; }
            public int getRowCount() { return data.length;}
            public Object getValueAt(int row, int col) {return data[row][col];}

            // The default implementations of these methods in
            // AbstractTableModel would work, but we can refine them.
            public String getColumnName(int column) {return names[column];}
            public Class getColumnClass(int c) {return getValueAt(0, c).getClass();}
            public boolean isCellEditable(int row, int col) {return true;}
            public void setValueAt(Object aValue, int row, int column) {
                System.out.println("Setting value to: " + aValue);
                data[row][column] = aValue;
            }
        };

        // Create the table
        JTable tableView = new JTable(dataModel);
        // Turn off auto-resizing so that we can set column sizes programmatically.
        // In this mode, all columns will get their preferred widths, as set blow.
        tableView.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Create a combo box to show that you can use one in a table.
        JComboBox comboBox = new JComboBox();
        comboBox.addItem("Red");
        comboBox.addItem("Orange");
        comboBox.addItem("Yellow");
        comboBox.addItem("Green");
        comboBox.addItem("Blue");
        comboBox.addItem("Indigo");
        comboBox.addItem("Violet");

        TableColumn colorColumn = tableView.getColumn("Favorite Color");
        // Use the combo box as the editor in the "Favorite Color" column.
        colorColumn.setCellEditor(new DefaultCellEditor(comboBox));

        // Set a pink background and tooltip for the Color column renderer.
        DefaultTableCellRenderer colorColumnRenderer = new DefaultTableCellRenderer();
        colorColumnRenderer.setBackground(Color.pink);
        colorColumnRenderer.setToolTipText("Click for combo box");
        colorColumn.setCellRenderer(colorColumnRenderer);

        // Set a tooltip for the header of the colors column.
        TableCellRenderer headerRenderer = colorColumn.getHeaderRenderer();
        if (headerRenderer instanceof DefaultTableCellRenderer)
            ((DefaultTableCellRenderer)headerRenderer).setToolTipText("Hi Mom!");

        // Set the width of the "Vegetarian" column.
        TableColumn vegetarianColumn = tableView.getColumn("Vegetarian");
        vegetarianColumn.setPreferredWidth(100);

        // Show the values in the "Favorite Number" column in different colors.
        TableColumn numbersColumn = tableView.getColumn("Favorite Number");
        DefaultTableCellRenderer numberColumnRenderer = new DefaultTableCellRenderer() {
            public void setValue(Object value) {
                int cellValue = (value instanceof Number) ? ((Number)value).intValue() : 0;
                setForeground((cellValue > 30) ? Color.black : Color.red);
                setText((value == null) ? "" : value.toString());
            }
        };
        numberColumnRenderer.setHorizontalAlignment(JLabel.RIGHT);
        numbersColumn.setCellRenderer(numberColumnRenderer);
        numbersColumn.setPreferredWidth(110);

        // Finish setting up the table.
        JScrollPane scrollpane = new JScrollPane(tableView);
        scrollpane.setBorder(new BevelBorder(BevelBorder.LOWERED));
        scrollpane.setPreferredSize(new Dimension(430, 200));

        contentPane.add(scrollpane);
    }


    public void init() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        "javax.swing.plaf.metal.MetalLookAndFeel");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            initTest(getContentPane());
        });
    }
}
