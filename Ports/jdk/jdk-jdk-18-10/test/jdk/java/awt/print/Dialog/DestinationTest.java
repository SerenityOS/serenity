/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4846344 4851365 4851321 4851316 4863656 5046198 6293139
 * @summary Confirm that cancelling the dialog will not prompt for file.
 * @run main/manual DestinationTest
 */
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

public class DestinationTest extends Frame implements ActionListener {
    //Declare things used in the test, like buttons and labels here

    DisplayImages images;
    Button nativeDlg, nativeDlg2, commonSelectionDlg, commonRangeDlg, fileDlg;

    public DestinationTest() {

        images = new DisplayImages();
        images.setSize(530, 480);
        add(images, "Center");

        Panel printpanel = new Panel();

        nativeDlg = new Button("Native");
        nativeDlg.addActionListener(this);
        printpanel.add(nativeDlg);

        nativeDlg2 = new Button("Native 2");
        nativeDlg2.addActionListener(this);
        printpanel.add(nativeDlg2);

        commonSelectionDlg = new Button("Common Selection");
        commonSelectionDlg.addActionListener(this);
        printpanel.add(commonSelectionDlg);

        commonRangeDlg = new Button("Common Range");
        commonRangeDlg.addActionListener(this);
        printpanel.add(commonRangeDlg);

        fileDlg = new Button("Print To File - Common Dialog");
        fileDlg.addActionListener(this);
        printpanel.add(fileDlg);

        add(printpanel, "South");
        setSize(900, 300);
        setVisible(true);
    }

    public static void main (String args[]) {
        DestinationTest test = new DestinationTest();
    }


    public void actionPerformed(ActionEvent e) {

        JobAttributes  ja = new JobAttributes();
        PageAttributes pa = new PageAttributes();
        ja.setDestination(JobAttributes.DestinationType.FILE);
        ja.setFileName("test_file_name.prn");

        if(e.getSource()== nativeDlg) {
            ja.setDefaultSelection(JobAttributes.DefaultSelectionType.SELECTION);
            ja.setPageRanges(new int[][] {new int[] {2,3}, new int[] {5,6}});
            ja.setDialog(JobAttributes.DialogType.NATIVE);
        }

        if(e.getSource()== nativeDlg2) {
            ja.setFileName("");
            ja.setDialog(JobAttributes.DialogType.NATIVE);
        }

        if(e.getSource()== commonRangeDlg) {
            ja = new JobAttributes();
            ja.setDefaultSelection(JobAttributes.DefaultSelectionType.RANGE);
            ja.setPageRanges(new int[][] {new int[] {1,3}, new int[] {5,6}});
            ja.setDialog(JobAttributes.DialogType.COMMON);
        }

        if (e.getSource() == fileDlg) {
            ja = new JobAttributes();
            ja.setDestination(JobAttributes.DestinationType.FILE);
            ja.setDialog(JobAttributes.DialogType.COMMON);
        }

        if(e.getSource()== commonSelectionDlg) {
            ja.setDefaultSelection(JobAttributes.DefaultSelectionType.SELECTION);
            ja.setDialog(JobAttributes.DialogType.COMMON);
        }

        PrintJob pjob = getToolkit().getPrintJob(this,"Printing Test",ja,pa);
        System.out.println("6293139: Chosen printer is: "+ja.getPrinter());
        if(pjob != null) {

            Graphics pg = pjob.getGraphics();

            if(pg != null) {
                //images.printAll(pg);
                this.printAll(pg);
                pg.dispose();
            }
            pjob.end();
        }
    }
}

class DisplayImages extends Canvas {

    public void paint(Graphics g) {

        g.setFont(new Font("Helvetica", Font.BOLD, 12));
        g.drawString("PRINTING TEST", 1, 10);
        g.drawString(" 4846344: Confirm that cancelling the native dialog will not prompt for file.", 1, 25);
        g.drawString(" 4851365: Confirm that printing in native dialog shows test_file_name.prn as default.", 1, 40);
        g.drawString(" 4851321: Confirm that in the Common Range dialog, page ranges is set to 1-6.", 1, 55);
        g.drawString(" 4851316: Confirm that NPE is not thrown upon selecting Common Selection dialog.", 1, 70);
        g.drawString(" 4863656: Confirm that no IAE is thrown when printing in native dialog.", 1, 85);
        g.drawString(" 4864444: Confirm that the default directory in Native 2 is same as current one with no filename set.", 1, 100);
        g.drawString(" 5046198: Confirm that the default filename in Common Range dialog when printing to a file is same as that of PrintToFile dialog.", 1, 115);
        g.drawString(" 6293139: In Common Range dialog, change printer before printing then confirm the chosen printer.", 1, 130);
    }
}
