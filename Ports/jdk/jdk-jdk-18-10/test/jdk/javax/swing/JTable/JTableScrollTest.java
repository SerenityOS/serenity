/* Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Dialog;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.table.TableModel;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;
import javax.swing.SwingUtilities;

/**
 * @test
 * @bug 8081491
 * @summary Scrolling a JTable creates artifacts
 * @run main/manual JTableScrollTest
 */
public class JTableScrollTest {
    static JFrame frame = new JFrame();
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                doTest(JTableScrollTest::createTable);
            }
        });
    }

    private static void createTable() {
        // final
        final String[] names = {
            new String("first_name"),
            new String("last_name"),
            new String("favorite_color"),
            new String("favorite_food")
        };

        // Create the dummy data (a few rows of names)
        final Object[][] data = {
          {"Mike", "Albers",      "green",      "strawberry"},
          {"Mark", "Andrews",     "blue",       "grapes"},
          {"Brian", "Beck",       "black",      "raspberry"},
          {"Lara", "Bunni",       "red",        "strawberry"},
          {"Roger", "Brinkley",   "blue",       "peach"},
          {"Brent", "Christian",  "black",      "broccoli"},
          {"Mark", "Davidson",    "darkgreen",  "asparagus"},
          {"Jeff", "Dinkins",     "blue",       "kiwi"},
          {"Ewan", "Dinkins",     "yellow",     "strawberry"},
          {"Amy", "Fowler",       "violet",     "raspberry"},
          {"Hania", "Gajewska",   "purple",     "raspberry"},
          {"David", "Geary",      "blue",       "watermelon"},
          {"Ryan", "Gosling",    "pink",       "donut"},
          {"Eric", "Hawkes",      "blue",       "pickle"},
          {"Shannon", "Hickey",   "green",      "grapes"},
          {"Earl", "Johnson",     "green",      "carrot"},
          {"Robi", "Khan",        "green",      "apple"},
          {"Robert", "Kim",       "blue",       "strawberry"},
          {"Janet", "Koenig",     "turquoise",  "peach"},
          {"Jeff", "Kesselman",   "blue",       "pineapple"},
          {"Onno", "Kluyt",       "orange",     "broccoli"},
          {"Peter", "Korn",       "sunpurple",  "sparegrass"},
          {"Rick", "Levenson",    "black",      "raspberry"},
          {"Brian", "Lichtenwalter", "blue", "pear"},
          {"Malini", "Minasandram", "beige",    "corn"},
          {"Michael", "Martak",   "green",      "strawberry"},
          {"David", "Mendenhall", "forestgreen", "peach"},
          {"Phil", "Milne",       "pink", "banana"},
          {"Lynn", "Monsanto",    "cybergreen",  "peach"},
          {"Hans", "Muller",      "rustred",     "pineapple"},
          {"Joshua", "Outwater",  "blue",        "pineapple"},
          {"Tim", "Prinzing",     "blue",        "pepper"},
          {"Raj", "Premkumar",    "blue",    "broccoli"},
          {"Howard", "Rosen",     "green",    "strawberry"},
          {"Ray", "Ryan",         "black",   "banana"},
          {"Georges", "Saab",     "aqua",     "cantaloupe"},
          {"Tom", "Santos",       "blue",       "pepper"},
          {"Rich", "Schiavi",     "blue",       "pepper"},
          {"Nancy", "Schorr",     "green",      "watermelon"},
          {"Keith", "Sprochi",    "darkgreen",   "watermelon"},
          {"Matt", "Tucker",      "eblue",       "broccoli"},
          {"Dmitri", "Trembovetski", "red",      "tomato"},
          {"Scott", "Violet",     "violet",      "banana"},
          {"Kathy", "Walrath",    "darkgreen",   "pear"},
        };

    // Create a model of the data.
        TableModel dataModel = new AbstractTableModel() {
            public int getColumnCount() { return names.length; }
            public int getRowCount() { return data.length;}
            public Object getValueAt(int row, int col) {return data[row][col];}
            public String getColumnName(int column) {return names[column];}
            public Class getColumnClass(int c) {return getValueAt(0, c).getClass();}
            public boolean isCellEditable(int row, int col) {return col != 5;}
            public void setValueAt(Object aValue, int row, int column) { data[row][column] = aValue; }
         };

    // Create the table
        JTable tableView = new JTable(dataModel);
        tableView.setBackground(Color.WHITE);
        tableView.setForeground(Color.BLACK);
        tableView.setSize(600, 800);
        JScrollPane scrollpane = new JScrollPane(tableView);
        frame.add(scrollpane);
        frame.pack();
        frame.setVisible(true);
    }

    private static void doTest(Runnable action) {
        String description =
          "JTable with rows will be displayed along with scrollbar.\n"
           + "Scroll the table. Verify no arifacts are shown and rows.\n"
           + " are correctly displayed.";
        final JDialog dialog = new JDialog();
        dialog.setTitle("ScrollArtifactTest ");
        JTextArea textArea = new JTextArea(description);
        textArea.setEditable(false);
        final JButton testButton = new JButton("Create Table");
        final JButton passButton = new JButton("PASS");
        passButton.setEnabled(false);
        passButton.addActionListener((e) -> {
            dialog.dispose();
            if (frame != null) {
                frame.setVisible(false);
                frame.dispose();
            }
        });
        final JButton failButton = new JButton("FAIL");
        failButton.setEnabled(false);
        failButton.addActionListener((e) -> {
            dialog.dispose();
            if (frame != null) {
                frame.setVisible(false);
                frame.dispose();
            }
            throw new RuntimeException("Scrollbar artifact shown");
        });
        testButton.addActionListener((e) -> {
            testButton.setEnabled(false);
            action.run();
            passButton.setEnabled(true);
            failButton.setEnabled(true);
        });
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(textArea, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(testButton);
        buttonPanel.add(passButton);
        buttonPanel.add(failButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(mainPanel);
        dialog.pack();
        dialog.setVisible(true);
    }


}
