/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5080830
 * @summary Verify if SheetCollate option is disabled for flavors that do not
 *          support SheetCollate
 * @run main/manual ServiceDlgSheetCollateTest
 */
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.ServiceUI;
import javax.print.attribute.Attribute;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.SheetCollate;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class ServiceDlgSheetCollateTest {
    private static Thread mainThread;
    private static boolean testPassed;
    private static boolean testGeneratedInterrupt;

    /**
     * Starts the application.
     */
    public static void main(java.lang.String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            doTest(ServiceDlgSheetCollateTest::printTest);
        });
        mainThread = Thread.currentThread();
        try {
            Thread.sleep(600000);
        } catch (InterruptedException e) {
            if (!testPassed && testGeneratedInterrupt) {
                throw new RuntimeException("SheetCollate option is not disabled "
                        + "for flavors that do not support sheetCollate");
            }
        }
        if (!testGeneratedInterrupt) {
            throw new RuntimeException("user has not executed the test");
        }
    }

    private static void printTest() {
        ServiceDlgSheetCollateTest pd = new ServiceDlgSheetCollateTest();
        DocFlavor flavor = DocFlavor.INPUT_STREAM.JPEG;
        //DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PRINTABLE;
        PrintService defService = null, service[] = null;
        defService = PrintServiceLookup.lookupDefaultPrintService();
        service = PrintServiceLookup.lookupPrintServices(flavor, null);

        if ((service == null) || (service.length == 0)) {
            throw new RuntimeException("No Printer services found");
        }
        if (defService != null) {
            System.out.println("\nDefault print service: " + service );
            System.out.println("is flavor: "+flavor+" supported? "+
                            defService.isDocFlavorSupported(flavor));
            System.out.println("is SheetCollate category supported? "+
                  defService.isAttributeCategorySupported(SheetCollate.class));
            System.out.println("is SheetCollate.COLLATED value supported ? "+
                  defService.isAttributeValueSupported(SheetCollate.COLLATED,
                                                            flavor, null));
        }
        HashPrintRequestAttributeSet prSet = new HashPrintRequestAttributeSet();
        try {
            PrintService selService = ServiceUI.printDialog(null, 200, 200, service, defService, flavor, prSet);
        } catch (IllegalArgumentException ia) {
            System.out.println("Exception thrown : " + ia);
        }

        System.out.println("\nSelected Values\n");
        Attribute attr[] = prSet.toArray();
        for (int x = 0; x < attr.length; x ++) {
            System.out.println("Attribute: " + attr[x].getName() + " Value: " + attr[x]);
        }

    }

    public static synchronized void pass() {
        testPassed = true;
        testGeneratedInterrupt = true;
        mainThread.interrupt();
    }

    public static synchronized void fail() {
        testPassed = false;
        testGeneratedInterrupt = true;
        mainThread.interrupt();
    }

    private static void doTest(Runnable action) {
        String description
                = " A print dialog would appear.\n"
                + " Increase the no. of copies.\n"
                + " If COLLATE checkbox gets enabled, press FAIL else press PASS.\n"
                + " Press Cancel to close the dialog.";

        final JDialog dialog = new JDialog();
        dialog.setTitle("printSelectionTest");
        JTextArea textArea = new JTextArea(description);
        textArea.setEditable(false);
        final JButton testButton = new JButton("Start Test");
        final JButton passButton = new JButton("PASS");
        passButton.setEnabled(false);
        passButton.addActionListener((e) -> {
            dialog.dispose();
            pass();
        });
        final JButton failButton = new JButton("FAIL");
        failButton.setEnabled(false);
        failButton.addActionListener((e) -> {
            dialog.dispose();
            fail();
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

