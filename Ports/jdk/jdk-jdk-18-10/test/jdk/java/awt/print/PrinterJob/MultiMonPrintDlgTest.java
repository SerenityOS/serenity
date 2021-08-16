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
import java.awt.Button;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * @test
 * @bug 8138749
 * @summary PrinterJob.printDialog() does not support multi-mon,
 *           always displayed on primary
 * @run main/manual MultiMonPrintDlgTest
 */
public class MultiMonPrintDlgTest implements ActionListener {

    private static boolean testPassed;
    private static Thread mainThread;
    private static boolean testGeneratedInterrupt;
    private static int sleepTime = 30000;
    private static String message = "User has not executed the test";

    static Frame primaryFrame = null;
    static Frame secFrame = null;
    static GraphicsDevice gd[] = GraphicsEnvironment.getLocalGraphicsEnvironment().
                            getScreenDevices();

    private static void init() throws Exception {

        String[] instructions =
            {
             " This test should be running on a dual-monitor setup.",
             "A frame will be created on each of the 2 monitor. ",
             "Click the Print button on the frame displayed in the non-default monitor.",
             "Please verify that page dialog followed by print dialog ",
             " is displayed in the same screen",
             "where the frame is located ie, in the non-default monitor.",
            };

        SwingUtilities.invokeAndWait(() -> {
            JOptionPane.showMessageDialog(
                    (Component) null,
                    instructions,
                    "information", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void executeTest() {

        GraphicsDevice defDev = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int x = 0;
        Frame f = null;
        for (x = 0; x < gd.length; x ++) {
            if (gd[x] != defDev) {
                secFrame = new Frame("Screen " + x + " - secondary", gd[x].getDefaultConfiguration());
                f = secFrame;
            } else {
                primaryFrame = new Frame("Screen " + x + " - primary", gd[x].getDefaultConfiguration());
                f = primaryFrame;
            }
            Button b = new Button("Print");
            b.addActionListener(this);
            f.add("South", b);
            f.addWindowListener (new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    ((Window) we.getSource()).dispose();
                }
            });
            f.setSize(200, 200);
            f.setVisible(true);
        }
    }

    public void actionPerformed (ActionEvent ae) {
        javax.print.attribute.PrintRequestAttributeSet prSet =
              new javax.print.attribute.HashPrintRequestAttributeSet();
        java.awt.print.PrinterJob.getPrinterJob().pageDialog(prSet);
        Window w = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        int dialogButton = JOptionPane.showConfirmDialog (w,
                        "Did the pageDialog shown in non-default monitor?",
                        null, JOptionPane.YES_NO_OPTION);
        if(dialogButton == JOptionPane.NO_OPTION) {
            fail("PageDialog is shown in wrong monitor");
        } else {
            java.awt.print.PrinterJob.getPrinterJob().printDialog(prSet);
            dialogButton = JOptionPane.showConfirmDialog (w,
                        "Did the printDialog shown in non-default monitor?",
                        null, JOptionPane.YES_NO_OPTION);
            if(dialogButton == JOptionPane.NO_OPTION) {
                fail("PrintDialog is shown in wrong monitor");
            } else {
                pass();
            }
        }
    }

    private static void dispose() {
        primaryFrame.dispose();
        secFrame.dispose();
    }

    public static synchronized void pass() {
        testPassed = true;
        testGeneratedInterrupt = true;
        mainThread.interrupt();
    }

    public static synchronized void fail(String msg) {
        testPassed = false;
        message = msg;
        testGeneratedInterrupt = true;
        mainThread.interrupt();
    }

    public static void main (String args[]) throws Exception {
        if (gd.length <= 1) {
            System.out.println("This test should be run only on dual-monitor systems. Aborted!!");
            return;
        }
        init();
        MultiMonPrintDlgTest test = new MultiMonPrintDlgTest();
        test.executeTest();
        mainThread = Thread.currentThread();

        try {
            mainThread.sleep(sleepTime);
        } catch (InterruptedException ex) {
            dispose();
            if (!testPassed && testGeneratedInterrupt) {
                throw new RuntimeException(message);
            }
        }
        if (!testGeneratedInterrupt) {
            dispose();
            throw new RuntimeException(message);
        }
    }
}
