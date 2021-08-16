/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.swingset3.demos.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.HashMap;
import java.util.List;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

/**
 *
 * @author aim
 */
public class OscarCellRenderers {

    //<snip>Render table rows with alternating colors
    public static class RowRenderer extends DefaultTableCellRenderer {
        private Color rowColors[];

        public RowRenderer() {
            // initialize default colors from look-and-feel
            rowColors = new Color[1];
            rowColors[0] = UIManager.getColor("Table.background");
        }

        public RowRenderer(Color colors[]) {
            super();
            setRowColors(colors);
        }

        public void setRowColors(Color colors[]) {
            rowColors = colors;
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setText(value != null ? value.toString() : "unknown");
            if (!isSelected) {
                setBackground(rowColors[row % rowColors.length]);
            }
            return this;
        }

        public boolean isOpaque() {
            return true;
        }
    }
    //<snip>

    //<snip>Render "year" table column with font representing style of decade
    // currently only used on OS X because fonts are Mac centric.

    public static class YearRenderer extends RowRenderer {
        private HashMap<String, Font> eraFonts;

        public YearRenderer() {
            setHorizontalAlignment(JLabel.CENTER);

            if (System.getProperty("os.name").equals("Mac OS X")) {
                eraFonts = new HashMap<String, Font>();
                eraFonts.put("192"/*1920's*/, new Font("Jazz LET", Font.PLAIN, 12));
                eraFonts.put("193"/*1930's*/, new Font("Mona Lisa Solid ITC TT", Font.BOLD, 18));
                eraFonts.put("194"/*1940's*/, new Font("American Typewriter", Font.BOLD, 12));
                eraFonts.put("195"/*1950's*/, new Font("Britannic Bold", Font.PLAIN, 12));
                eraFonts.put("196"/*1960's*/, new Font("Cooper Black", Font.PLAIN, 14));
                eraFonts.put("197"/*1970's*/, new Font("Syncro LET", Font.PLAIN, 14));
                eraFonts.put("198"/*1980's*/, new Font("Mistral", Font.PLAIN, 18));
                eraFonts.put("199"/*1990's*/, new Font("Papyrus", Font.BOLD, 14));
                eraFonts.put("200"/*2000's*/, new Font("Calisto MT", Font.PLAIN, 14));
            }
        }

        public YearRenderer(Color colors[]) {
            this();
            setRowColors(colors);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String year = table.getValueAt(row,
                    table.convertColumnIndexToView(OscarTableModel.YEAR_COLUMN)).toString();
            if (eraFonts != null && year != null && year.length() == 4) {
                String era = year.substring(0, 3);
                Font eraFont = eraFonts.get(era);
                setFont(eraFont);
            }
            return this;
        }
    }
    //</snip>

    //<snip>Render "nominee" table column with special icon for winners

    public static class NomineeRenderer extends RowRenderer {
        private final ImageIcon winnerIcon;
        private final ImageIcon nomineeIcon; // nice way of saying "loser" :)

        public NomineeRenderer() {
            winnerIcon = new ImageIcon(
                    getClass().getResource("resources/images/goldstar.png"));
            nomineeIcon = new ImageIcon(
                    getClass().getResource("resources/images/nominee.png"));
            setHorizontalTextPosition(JLabel.TRAILING);
        }

        public NomineeRenderer(Color colors[]) {
            this();
            setRowColors(colors);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            TableModel model = table.getModel();
            boolean winner = ((Boolean) model.getValueAt(table.convertRowIndexToModel(row),
                    OscarTableModel.WINNER_COLUMN)).booleanValue();

            List<String> persons = (List<String>) value;
            String text = persons != null && !persons.isEmpty() ? persons.get(0) : "name unknown";
            if (persons != null && persons.size() > 1) {
                int personCount = persons.size();
                setText(text + " + more...");
                StringBuffer winners = new StringBuffer("");
                for (int i = 0; i < personCount; i++) {
                    String person = persons.get(i);
                    winners.append(" " + person + (i < personCount - 1 ? ", " : ""));
                }
                setToolTipText((winner ? "Winners:" : "Nominees:") + winners);
            } else {
                setText(text);
                setToolTipText(winner ? "Winner!" : "Nominee");
            }

            setIcon(winner ? winnerIcon : nomineeIcon);

            return this;
        }
    }
    //</snip>

    public static class MovieRenderer extends HyperlinkCellRenderer {
        public MovieRenderer(Action action, boolean underlineOnRollover, Color rowColors[]) {
            super(action, underlineOnRollover);
            setRowColors(rowColors);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected,
                    hasFocus, row, column);
            if (value != null) {
                setToolTipText("http://www.imdb.com/" + "\"" + value + "\"");
            }
            return this;
        }
    }

}
