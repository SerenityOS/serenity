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

/* @test
   @bug 8203796
   @run main/manual DialogOwnerTest
   @summary Test DialogOwner API
*/

import java.util.ArrayList;
import java.util.List;
import java.awt.GraphicsConfiguration;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.print.PrinterJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.ServiceUI;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.DialogOwner;
import javax.print.attribute.standard.DialogTypeSelection;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class DialogOwnerTest extends JPanel {

    static final int NONE      =     0x0;
    static final int PRINT     =     0x1;
    static final int PAGE      =     0x2;
    static final int SWING2D   =     0x4;
    static final int NATIVE2D  =     0x8;
    static final int SERVICEUI =    0x10;

    static final int ONTOP      =   0x20;
    static final int OWNED      =   0x40;

    static PrintService[] services =
        PrintServiceLookup.lookupPrintServices(null, null);

    public static void main(String[] args) {
        if (services.length == 0) {
            System.out.println("No printers, exiting");
            return;
        } else {
           service = PrinterJob.getPrinterJob().getPrintService();
        }
        SwingUtilities.invokeLater(() -> {
            createUI();
        });
        while (!testFinished) {
           try {
                Thread.sleep(1000);
           } catch (InterruptedException e){
           }
        }
        if (!testPassed) {
            throw new RuntimeException("TEST FAILED.");
        }
    }


    static final String otherText =
       "This window is used to test on top behaviour\n" +
       "For tests that are 'Owned' or 'On Top' the dialog\n" +
       "must always stay above this window. Verify this\n " +
       "by moving the dialog so that it partially obscures\n" +
       "this window and then trying to raise this window.";

    static final String instructions =
       " Instructions\n" +
       "This tests that a print dialog stays on top of either another\n" +
       "window, or on top of all windows.\n" +
       "For Owned tests the window titled 'Owner Window' should always \n" +
       "stay behind the print dialog.\n" +
       "For On Top tests all windows should stay behind the print dialog \n" +
       "This test tracks if you have checked all the scenarios and will\n" +
       "not allow the test to pass unless you have visited them all.\n";

    static PrintService service;

    public DialogOwnerTest() {
       super();
       //setLayout(new GridLayout(24, 1));
    }

    static boolean isNative(int flags) {
        return (flags & NATIVE2D) != 0;
    }

    static boolean isCommon(int flags) {
        return (flags & SWING2D) != 0;
    }

    static boolean is2D(int flags) {
        return (flags & SWING2D|NATIVE2D) != 0;
    }

    static boolean isPage(int flags) {
        return (flags & PAGE ) != 0;
    }

    static JFrame frame;
    static JFrame other;
    static JButton pass;
    static ArrayList<JPanel> panelList = new ArrayList<JPanel>();
    static volatile boolean testPassed, testFinished;

    int testCount = 0;
    List<String> testList = new ArrayList<String>();

    static void createUI() {
        other = new JFrame("Owner Window");
        JTextArea otherTextArea = new JTextArea(otherText, 10, 40);
        other.add(otherTextArea);
        other.pack();
        other.setVisible(true);
        other.setLocation(800, 100);

        frame = new JFrame("Test Dialog Owner");
        frame.pack();
        JTextArea instructionsPanel = new JTextArea(instructions, 10, 50);
        instructionsPanel.setEditable(false);
        frame.add("North", instructionsPanel);
        DialogOwnerTest test = new DialogOwnerTest();

        test.addTest("Owned Swing Print", OWNED, frame, PRINT|SWING2D);
        test.addTest("On Top Swing Print", ONTOP, null, PRINT|SWING2D);

        test.addTest("Owned Swing Page", OWNED, frame, PAGE|SWING2D);
        test.addTest("On Top Swing Page", ONTOP, null, PAGE|SWING2D);

        test.addTest("Owned javax.print", OWNED, frame, PRINT|SERVICEUI);
        test.addTest("On Top javax.print", OWNED, null, PRINT|SERVICEUI);

        test.addTest("Owned Native Print", OWNED, frame, PRINT|NATIVE2D);
        test.addTest("On Top Native Print", OWNED, null, PRINT|NATIVE2D);

        test.addTest("Owned Native Page", OWNED, frame, PAGE|NATIVE2D);
        test.addTest("On Top Native Page", OWNED, null, PAGE|NATIVE2D);

        test.setLayout(new GridLayout(panelList.size()+2, 1));

        pass = new JButton("Pass");
        pass.setEnabled(false);
        pass.addActionListener((ActionEvent e) -> {
            if (test.testList.size() > 0) {
                return;
            }
            frame.dispose();
            other.dispose();
            System.out.println("User says test passed.");
            testPassed = true;
            testFinished = true;
        });

        JButton fail = new JButton("Fail");
        fail.addActionListener((ActionEvent e) -> {
            frame.dispose();
            other.dispose();
            System.out.println("User says test failed.");
            testPassed = false;
            testFinished = true;
        });

        JPanel p = new JPanel();
        p.add(pass);
        p.add(fail);
        test.add(p);


        for (JPanel panel : panelList) {
            test.add(panel);
        }

        frame.add("Center", test);
        frame.pack();
        frame.setLocation(0,0);
        frame.setVisible(true);
     }

   boolean isSupported(PrintRequestAttributeSet aset,
                     int ownerFlags, Window owner, int dlgFlags) {

       boolean supported = true;
       DialogOwner ownerAttr = null;
       if (ownerFlags != NONE) {
           if (ownerFlags == ONTOP) {
               ownerAttr = new DialogOwner();
           } else if (ownerFlags == OWNED) {
               ownerAttr = new DialogOwner(owner);
           }
           aset.add(ownerAttr);
        }
        if (is2D(dlgFlags)) {
           DialogTypeSelection dst = null;
           if (isNative(dlgFlags)) {
               dst = DialogTypeSelection.NATIVE;
           } else if (isCommon(dlgFlags)) {
               dst = DialogTypeSelection.COMMON;
           }
           if (dst != null &&
               !service.isAttributeValueSupported(dst, null, aset)) {
               //System.out.println("This DialogType not supported");
               supported = false;
           }
           if (dst != null) {
               aset.add(dst);
           }
           if (ownerAttr != null &&
               !service.isAttributeValueSupported(ownerAttr, null, aset)) {
               //System.out.println("This DialogOwner not supported");
               supported = false;
           }
        }
        return supported;
   }

   void addTest(String title, int ownerFlags, Window owner, int dlgFlags) {

        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        if (!isSupported(aset, ownerFlags, owner, dlgFlags)) {
            return;
        }

        // if we are here then this is supportable and worth testing
        // and the attribute set is configured.

        String label = title + " Dialog";
        JButton button = new JButton(label);
        JCheckBox tested = new JCheckBox("Tested");
        tested.setEnabled(false);
        JPanel panel = new JPanel();
        panel.add(tested);
        panel.add(button);
        panelList.add(panel);
        //add(panel);
        testList.add(title);
        if (++testCount != testList.size()) {
            throw new RuntimeException("Test titles must be unique");
        }

        button.addActionListener((ActionEvent e) -> {
            tested.setSelected(true);
            testList.remove(title);
            if (testList.isEmpty()) {
              pass.setEnabled(true);
            }

            if (is2D(dlgFlags)) {
               PrinterJob job = PrinterJob.getPrinterJob();
               if (isPage(dlgFlags)) {
                   job.pageDialog(aset);
               } else {
                   job.printDialog(aset);
               }
            } else {
               GraphicsConfiguration gc = null;
               int x = 0, y = 0;
               ServiceUI.printDialog(gc, x, y, services, services[0], null,aset);
            }
        });
    }
}
