/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 6937798
   @summary Nimbus: Issues with JTable grid
   @author Alexander Potochkin
   @run main bug6937798
*/

import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.image.BufferedImage;

public class bug6937798 {

    public static void main(String... args) throws Exception {
        UIManager.setLookAndFeel(new NimbusLookAndFeel());
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                new bug6937798();
            }
        });
    }

    public bug6937798() {
        final JTable table = createCountryTable();
        table.setShowGrid(true);
        table.setSize(100, 100);

        BufferedImage im = new BufferedImage(table.getWidth(), table.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = im.getGraphics();
        table.print(g);
        g.dispose();

        for (int i = 0; i < im.getHeight(); i++) {
            for (int j = 0; j < im.getWidth(); j++) {
                if (im.getRGB(i, j) == table.getGridColor().getRGB()) {
                    System.out.println("got it!");
                    return;
                }
            }
        }
        throw new RuntimeException("no table's grid detected....");
    }

    protected JTable createCountryTable() {
        // Column headers
        final String
                [] headers = {
                "Name", "Capital City", "Language(s)", "Monetary Unit(s)", "EC Member"
        };

        // Table data
        final Object[][] data = {
                {"Albania", "Tirane", "Albanian, Greek", "Lek", new Boolean(false)},
                {"Andorra", "Andorra la Vella", "Catalan, French, Spanish", "French Franc, Spanish Peseta", new Boolean(false)},
                {"Austria", "Vienna", "German, Slovenian, Croatian", "Schilling", new Boolean(false)},
                {"Belarus", "Minsk", "Byelorussian, Russian", "Belarusian Rubel", new Boolean(false)},
                {"Belgium", "Brussels", "French, Flemish, German", "Belgian Franc", new Boolean(true)},
                {"Bosnia & Herzegovina", "Sarajevo", "Serbo-Croatian", "Dinar", new Boolean(false)},
                {"Bulgaria", "Sofia", "Bulgarian, Turkish", "Lev", new Boolean(false)},
                {"Croatia", "Zagreb", "Serbo-Croatian", "Croatian Kuna", new Boolean(false)},
                {"Czech Republic", "Prague", "Czech, Slovak", "Koruna", new Boolean(false)},
                {"Denmark", "Copenhagen", "Danish", "Krone", new Boolean(true)},
                {"Estonia", "Tallinn", "Estonian, Latvian, Lithuanian, Russian", "Estonian Kroon", new Boolean(false)},
                {"Finland", "Helsinki", "Finnish, Swedish, Lappish", "Markka", new Boolean(false)},
                {"France", "Paris", "French", "Franc", new Boolean(true)},
                {"Germany", "Berlin", "German", "Deutsche Mark", new Boolean(true)},
                {"Greece", "Athens", "Greek, English, French", "Drachma", new Boolean(true)},
                {"Hungary", "Budapest", "Hungarian", "Forint", new Boolean(false)},
                {"Iceland", "Reykjavik", "Icelandic", "Icelandic Krona", new Boolean(false)},
                {"Ireland", "Dublin", "Irish, English", "Pound", new Boolean(true)},
                {"Italy", "Rome", "Italian", "Lira", new Boolean(true)},
                {"Latvia", "Riga", "Lettish, Lithuanian, Russian", "Lat", new Boolean(false)},
                {"Liechtenstein", "Vaduz", "German", "Swiss Franc", new Boolean(false)},
                {"Lithuania", "Vilnius", "Lithuanian, Polish, Russian", "Lita", new Boolean(false)},
                {"Luxembourg", "Luxembourg", "French, German, Letzeburgesch", "Luxembourg Franc", new Boolean(true)},
                {"Macedonia", "Skopje", "Macedonian, Albanian, Turkish, Serbo-Croatian", "Denar", new Boolean(false)},
                {"Malta", "Valletta", "Maltese, English", "Maltese Lira", new Boolean(false)},
                {"Moldova", "Chisinau", "Moldovan, Russian", "Leu", new Boolean(false)},
                {"Monaco", "Monaco", "French, English, Italian", "French Franc", new Boolean(false)},
                {"the Netherlands", "Amsterdam", "Dutch", "Guilder", new Boolean(true)},
                {"Norway", "Oslo", "Norwegian", "Krone", new Boolean(false)},
                {"Poland", "Warsaw", "Polish", "Zloty", new Boolean(false)},
                {"Portugal", "Lisbon", "Portuguese", "Escudo", new Boolean(true)},
                {"Romania", "Bucharest", "Romanian", "Leu", new Boolean(false)},
                {"Russia", "Moscow", "Russian", "Ruble", new Boolean(false)},
                {"San Marino", "San Marino", "Italian", "Italian Lira", new Boolean(false)},
                {"Slovakia", "Bratislava", "Slovak, Hungarian", "Koruna", new Boolean(false)},
                {"Slovenia", "Ljubljana", "Slovenian, Serbo-Croatian", "Tolar", new Boolean(false)},
                {"Spain", "Madrid", "Spanish", "Peseta", new Boolean(true)},
                {"Sweden", "Stockholm", "Swedish", "Krona", new Boolean(false)},
                {"Switzerland", "Bern", "German, French, Italian", "Swiss Franc", new Boolean(false)},
                {"Turkey", "Ankara", "Turkish", "Turkish Lira", new Boolean(false)},
                {"Ukraine", "Kiev", "Ukranian, Russian, Romanian, Polish, Hungarian", "Hryvnia", new Boolean(false)},
                {"United Kingdom", "London", "English, Welsh", "British Pound", new Boolean(true)},
                {"Yugoslavia", "Belgrade", "Serbo-Croatian, Slovenian, Macedonian", "Dinar", new Boolean(false)},
        };

        // Table model
        TableModel dataModel = new AbstractTableModel() {

            public int getColumnCount() {
                return headers.length;
            }

            public int getRowCount() {
                return data.length;
            }

            public Object getValueAt(int row, int col) {
                return data[row][col];
            }

            public String getColumnName(int column) {
                return headers[column];
            }

            public Class getColumnClass(int col) {
                return getValueAt(0, col).getClass();
            }

            public void setValueAt(Object aValue, int row, int column) {
                data[row][column] = aValue;
            }

            public boolean isCellEditable(int row, int col) {
                return (col == 4);
            }
        };

        // Create table with table model
        JTable countryTable = new JTable(dataModel);
        countryTable.setGridColor(Color.pink);
        countryTable.setBackground(Color.white);
        countryTable.setForeground(Color.black);
        return countryTable;
    }
}
