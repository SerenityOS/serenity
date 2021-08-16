/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 6195901 6195923 6195928 6195933 6491273 6888734
 * @summary No SecurityException should be thrown when printing to a file
            using the given policy.
            Print to file option should be selected.
 * @run main/othervm/policy=policy SecurityDialogTest
 */
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;


public class SecurityDialogTest extends Frame implements ActionListener {
    // Declare things used in the test, like buttons and labels here

    Button nativeDlg, setSecurity;
        boolean isNative = true;

    public SecurityDialogTest() {

        nativeDlg = new Button("Print Dialog");
        nativeDlg.addActionListener(this);
        setSecurity = new Button("Toggle Dialog");
        setSecurity.addActionListener(this);
        add("South", nativeDlg);
        add("North", setSecurity);
        setSize(300, 300);
        setVisible(true);
    }

    public static void main(String args[]) {
        System.out.println("Native dialog is the default");
        SecurityDialogTest test = new SecurityDialogTest();
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == setSecurity) {
            if (isNative) {
                isNative = false;
                System.out.println("Common dialog is the default");

            } else {
                isNative = true;
                System.out.println("Native dialog is the default");
            }
            return;
        }

        JobAttributes  ja = new JobAttributes();
        PageAttributes pa = new PageAttributes();

        if (isNative) {
            ja.setDialog(JobAttributes.DialogType.NATIVE);
        } else {
            ja.setDialog(JobAttributes.DialogType.COMMON);
        }
        ja.setDestination(JobAttributes.DestinationType.FILE);
        ja.setFileName("mohan.ps");


        PrintJob pjob = getToolkit().getPrintJob(this, null, ja, pa);

        if (pjob != null) {
            Graphics pg = pjob.getGraphics();
            System.out.println("PJOB: " + pjob);
            if (pg != null) {
                System.out.println("Printer Graphics: " + pg);
                this.printAll(pg);
                pg.dispose();
            } else {
                System.out.println("Printer Graphics is null");
            }
            pjob.end();
            System.out.println("DONE");
        } else {
            System.out.println("PJOB is null");
        }
    }
}
