/*
 * Copyright (c) 1997, 2011, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */



import javax.swing.*;
import javax.swing.table.*;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Dimension;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager.LookAndFeelInfo;


/**
 * An example showing the JTable with a dataModel that is not derived
 * from a database. We add the optional TableSorter object to give the
 * JTable the ability to sort.
 *
 * @author Philip Milne
 */
public class TableExample3 {

    public TableExample3() {
        JFrame frame = new JFrame("Table");
        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        // Take the dummy data from SwingSet.
        final String[] names = { "First Name", "Last Name", "Favorite Color",
            "Favorite Number", "Vegetarian" };
        final Object[][] data = {
            { "Mark", "Andrews", "Red", Integer.valueOf(2), Boolean.TRUE },
            { "Tom", "Ball", "Blue", Integer.valueOf(99), Boolean.FALSE },
            { "Alan", "Chung", "Green", Integer.valueOf(838), Boolean.FALSE },
            { "Jeff", "Dinkins", "Turquois", Integer.valueOf(8), Boolean.TRUE },
            { "Amy", "Fowler", "Yellow", Integer.valueOf(3), Boolean.FALSE },
            { "Brian", "Gerhold", "Green", Integer.valueOf(0), Boolean.FALSE },
            { "James", "Gosling", "Pink", Integer.valueOf(21), Boolean.FALSE },
            { "David", "Karlton", "Red", Integer.valueOf(1), Boolean.FALSE },
            { "Dave", "Kloba", "Yellow", Integer.valueOf(14), Boolean.FALSE },
            { "Peter", "Korn", "Purple", Integer.valueOf(12), Boolean.FALSE },
            { "Phil", "Milne", "Purple", Integer.valueOf(3), Boolean.FALSE },
            { "Dave", "Moore", "Green", Integer.valueOf(88), Boolean.FALSE },
            { "Hans", "Muller", "Maroon", Integer.valueOf(5), Boolean.FALSE },
            { "Rick", "Levenson", "Blue", Integer.valueOf(2), Boolean.FALSE },
            { "Tim", "Prinzing", "Blue", Integer.valueOf(22), Boolean.FALSE },
            { "Chester", "Rose", "Black", Integer.valueOf(0), Boolean.FALSE },
            { "Ray", "Ryan", "Gray", Integer.valueOf(77), Boolean.FALSE },
            { "Georges", "Saab", "Red", Integer.valueOf(4), Boolean.FALSE },
            { "Willie", "Walker", "Phthalo Blue", Integer.valueOf(4), Boolean.FALSE },
            { "Kathy", "Walrath", "Blue", Integer.valueOf(8), Boolean.FALSE },
            { "Arnaud", "Weber", "Green", Integer.valueOf(44), Boolean.FALSE }
        };

        // Create a model of the data.
        @SuppressWarnings("serial")
        TableModel dataModel = new AbstractTableModel() {
            // These methods always need to be implemented.

            public int getColumnCount() {
                return names.length;
            }

            public int getRowCount() {
                return data.length;
            }

            public Object getValueAt(int row, int col) {
                return data[row][col];
            }

            // The default implementations of these methods in
            // AbstractTableModel would work, but we can refine them.
            @Override
            public String getColumnName(int column) {
                return names[column];
            }

            @Override
            public Class<?> getColumnClass(int col) {
                return getValueAt(0, col).getClass();
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return (col == 4);
            }

            @Override
            public void setValueAt(Object aValue, int row, int column) {
                data[row][column] = aValue;
            }
        };

        // Instead of making the table display the data as it would normally
        // with:
        // JTable tableView = new JTable(dataModel);
        // Add a sorter, by using the following three lines instead of the one
        // above.
        TableSorter sorter = new TableSorter(dataModel);
        JTable tableView = new JTable(sorter);
        sorter.addMouseListenerToHeaderInTable(tableView);

        JScrollPane scrollpane = new JScrollPane(tableView);

        scrollpane.setPreferredSize(new Dimension(700, 300));
        frame.getContentPane().add(scrollpane);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        // Trying to set Nimbus look and feel
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(TableExample3.class.getName()).log(Level.SEVERE,
                    "Failed to apply Nimbus look and feel", ex);
        }
        new TableExample3();
    }
}
