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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.text.MessageFormat;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.DialogTypeSelection;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

/**
 * @test
 * @bug 8044444 8081491
 * @summary The output's 'Page-n' footer does not show completely
 * @author Alexandr Scherbatiy
 * @run main/manual ImageableAreaTest
 */
public class ImageableAreaTest {

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {


                createAndShowTestDialog(
                        "1. Press the Print Table button\n"
                        + " Java print dialog should appear.\n"
                        + "2. Press the Print button on the Java Print dialog.\n"
                        + "3. Check that the page number is correctly printed.\n"
                        + "4. Check only the visible part of the table is printed.\n"
                        + "If so, press PASS, else press FAIL.",
                        "Page number is not correctly printed!",
                        ImageableAreaTest::printWithJavaPrintDialog);

                createAndShowTestDialog(
                        "1. Press the Print Table button\n"
                        + " The table should be printed without the print dialog.\n"
                        + "2. Check that the page number is correctly printed.\n"
                        + "3. Check only the visible part of the table is printed.\n"
                        + "If so, press PASS, else press FAIL.",
                        "Page number is not correctly printed!",
                        ImageableAreaTest::printWithoutPrintDialog);



                createAndShowTestDialog(
                        "1. Press the Print Table button\n"
                        + " Java print dialog should appear.\n"
                        + "2. Press the Print button on the Java Print dialog.\n"
                        + "3. Check that the table has about half size of the printed page\n"
                        + "4. Check only the visible part of the table is printed.\n"
                        + "If so, press PASS, else press FAIL.",
                        "Custom imageable area is not correctly printed!",
                        ImageableAreaTest::printWithCustomImageareaSize);

                createAndShowTestDialog(
                        "1. Press the Print Table button\n"
                        + " Java print dialog should appear.\n"
                        + "2. Press the Print button on the Java Print dialog.\n"
                        + "3. Check that the rows with different height is printed.\n"
                        + "4. Check only the visible part of the table is printed.\n"
                        + "If so, press PASS, else press FAIL.",
                        "Row with different height is not correctly printed!",
                        ImageableAreaTest::printDifferentRowHeight);

                createAndShowTestDialog(
                        "1. Press the Print Table button\n"
                        + " Java print dialog should appear.\n"
                        + "2. Press the Print button on the Java Print dialog.\n"
                        + "3. Check that the only 1 row is shown & printed.\n"
                        + "If so, press PASS, else press FAIL.",
                        "Only 1 Row is not correctly printed!",
                        ImageableAreaTest::printOneRowWithJavaPrintDialog);
            }
        });
    }

    private static void printWithJavaPrintDialog() {
        final JTable table = createAuthorTable(50);
        Printable printable = table.getPrintable(
                JTable.PrintMode.NORMAL,
                new MessageFormat("Author Table"),
                new MessageFormat("Page - {0}"));

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(printable);

        boolean printAccepted = job.printDialog();
        if (printAccepted) {
            try {
                job.print();
                closeFrame();
            } catch (PrinterException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void printWithoutPrintDialog() {

        final JTable table = createAuthorTable(50);
        PrintRequestAttributeSet pras
                = new HashPrintRequestAttributeSet();
        pras.add(new Copies(1));

        try {

            boolean printAccepted = table.print(JTable.PrintMode.FIT_WIDTH,
                    new MessageFormat("Author Table"),
                    new MessageFormat("Page - {0}"),
                    false, pras, false);

            closeFrame();
            if (!printAccepted) {
                throw new RuntimeException("User cancels the printer job!");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void printDifferentRowHeight() {
        final JTable table = createAuthorTable(50);
        table.setRowHeight(15, table.getRowHeight(15)+10);
        Printable printable = table.getPrintable(
                JTable.PrintMode.NORMAL,
                new MessageFormat("Author Table"),
                new MessageFormat("Page - {0}"));

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(printable);

        boolean printAccepted = job.printDialog();
        if (printAccepted) {
            try {
                job.print();
                closeFrame();
            } catch (PrinterException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private static void printOneRowWithJavaPrintDialog() {
        final JTable table = createAuthorTable(1);
        Printable printable = table.getPrintable(
                JTable.PrintMode.NORMAL,
                new MessageFormat("Author Table"),
                new MessageFormat("Page - {0}"));

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(printable);

        boolean printAccepted = job.printDialog();
        if (printAccepted) {
            try {
                job.print();
                closeFrame();
            } catch (PrinterException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void printWithCustomImageareaSize() {
        final JTable table = createAuthorTable(18);
        PrintRequestAttributeSet printAttributes = new HashPrintRequestAttributeSet();
        printAttributes.add(DialogTypeSelection.NATIVE);
        printAttributes.add(new Copies(1));
        printAttributes.add(new MediaPrintableArea(
                0.25f, 0.25f, 8.0f, 5.0f, MediaPrintableArea.INCH));
        Printable printable = table.getPrintable(
                JTable.PrintMode.NORMAL,
                new MessageFormat("Author Table"),
                new MessageFormat("Page - {0}")
        );

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(printable);

        boolean printAccepted = job.printDialog(printAttributes);
        if (printAccepted) {
            try {
                job.print(printAttributes);
                closeFrame();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("User cancels the printer job!");
        }
    }

    private static JFrame frame;

    private static void closeFrame() {
        if (frame != null) {
            frame.setVisible(false);
            frame.dispose();
        }
    }

    private static JTable createAuthorTable(int rows) {
        final String[] headers = {"Book", "Title"};

        final Object[][] data = new Object[rows][2];
        for (int i = 0; i < rows; i++) {
            int n = i + 1;
            data[i] = new Object[]{"Book: " + n, "Title: " + n};
        }

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
                return false;
            }
        };

        JTable table = new JTable(dataModel);
        table.setGridColor(Color.BLUE);
        table.setBackground(Color.WHITE);
        table.setForeground(Color.BLACK);
        table.setSize(600, 800);

        frame = new JFrame();
        frame.setSize(400, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(table);
        frame.setVisible(true);
        return table;
    }

    private static int testCount;

    private static void createAndShowTestDialog(String description,
            String failMessage, Runnable action) {
        final JDialog dialog = new JDialog();
        dialog.setTitle("Test: " + (++testCount));
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        JTextArea textArea = new JTextArea(description);
        textArea.setEditable(false);
        final JButton testButton = new JButton("Print Table");
        final JButton passButton = new JButton("PASS");
        passButton.setEnabled(false);
        passButton.addActionListener((e) -> {
            dialog.dispose();
        });
        final JButton failButton = new JButton("FAIL");
        failButton.setEnabled(false);
        failButton.addActionListener((e) -> {
            throw new RuntimeException(failMessage);
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
