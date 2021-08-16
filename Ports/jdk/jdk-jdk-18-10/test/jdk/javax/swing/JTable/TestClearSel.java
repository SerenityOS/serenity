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
/*
 * @test
 * @key headful
 * @bug 8202702
 * @summary  Verifies if Jtable clear selction causes disappearance of a row.
 * @run main/manual TestClearSel
 */
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

public class TestClearSel {

    static DefaultTableModel model;

    public static void main(String[] args) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        ClearSelTest test = new ClearSelTest(latch);
        Thread T1 = new Thread(test);
        T1.start();

        // wait for latch to complete
        boolean ret = false;
        try {
            ret = latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            throw ie;
        }
        if (!ret) {
            test.dispose();
            throw new RuntimeException(" User has not executed the test");
        }

        if (test.testResult == false) {
            throw new RuntimeException("Some text were not rendered properly"
                    + " during painting of Jtable rows ");
        }
    }
}

class ClearSelTest implements Runnable {
    static JFrame f;
    static JDialog dialog;
    static DefaultTableModel model;
    public boolean testResult = false;
    private final CountDownLatch latch;
    private static String[] rows = new String[]{
            "Row1", "Row2", "Row3", "Row4", "Row5",
            "Row6", "Row7", "Row8", "Row9", "Row10"};

    public ClearSelTest(CountDownLatch latch) throws Exception {
        this.latch = latch;
    }

    @Override
    public void run() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                createUI();
                clearSelTest();
            });
        } catch (Exception ex) {
            if (f != null) {
                f.dispose();
            }
            latch.countDown();
            throw new RuntimeException("createUI Failed: " + ex.getMessage());
        }

    }

    public void dispose() {
        dialog.dispose();
        f.dispose();
    }

    private static void clearSelTest() {
        final DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Test", rows);
        final JTable table = new JTable(model);
        table.setRowHeight(25);

        final MouseAdapter adapt = new MouseAdapter() {

            @Override
            public void mouseMoved(final MouseEvent pE) {
                final int row = table.rowAtPoint(pE.getPoint());
                if (row > -1) {
                    table.setRowSelectionInterval(row, row);
                } else {
                    table.clearSelection();
                }
            }

            @Override
            public void mouseEntered(final MouseEvent pE) {
                final int row = table.rowAtPoint(pE.getPoint());
                if (row > -1) {
                    table.setRowSelectionInterval(row, row);
                } else {
                    table.clearSelection();
                }
            }

            @Override
            public void mouseExited(final MouseEvent pE) {
                table.clearSelection();
            }
        };
        table.addMouseListener(adapt);
        table.addMouseMotionListener(adapt);

        f = new JFrame();
        f.setSize(300, 300);
        f.setLocationRelativeTo(null);
        f.add(table);
        f.setVisible(true);
    }


    private final void createUI() {
        String description
                = " INSTRUCTIONS:\n"
                + " A JTable will be shown.\n"
                + " Move mouse over different row to select the row.\n "
                + " Please verify if row text disappear "
                + " if mouse is moved out of table.\n"
                + " If any moment any part of the rows will not be\n "
                + " painted properly and if some text are missing in JTable,\n "
                + " then press fail else press pass";

        dialog = new JDialog();
        dialog.setTitle("textselectionTest");
        JTextArea textArea = new JTextArea(description);
        textArea.setEditable(false);
        final JButton passButton = new JButton("PASS");
        passButton.addActionListener((e) -> {
            testResult = true;
            dispose();
            latch.countDown();
        });
        final JButton failButton = new JButton("FAIL");
        failButton.addActionListener((e) -> {
            testResult = false;
            dispose();
            latch.countDown();
        });
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(textArea, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(passButton);
        buttonPanel.add(failButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(mainPanel);
        dialog.pack();
        dialog.setVisible(true);
    }
}
