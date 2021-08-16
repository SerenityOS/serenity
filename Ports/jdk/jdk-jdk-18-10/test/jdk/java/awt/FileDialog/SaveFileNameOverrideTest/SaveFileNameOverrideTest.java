/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
  test
  @bug 6998877 8022531
  @summary After double-click on the folder names, FileNameOverrideTest FAILED
  @author Sergey.Bylokhov@oracle.com area=awt.filedialog
  @library ../../regtesthelpers
  @build Sysout
  @run applet/manual=yesno SaveFileNameOverrideTest.html
*/

import test.java.awt.regtesthelpers.Sysout;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class SaveFileNameOverrideTest extends Applet implements ActionListener {
    private final static String clickDirName = "Directory for double click";
    private final static String dirPath = ".";
    private Button showBtn;
    private FileDialog fd;

    public void init() {
        this.setLayout(new GridLayout(1, 1));

        fd = new FileDialog(new Frame(), "Save", FileDialog.SAVE);

        showBtn = new Button("Show File Dialog");
        showBtn.addActionListener(this);
        add(showBtn);

        File tmpDir = new File(dirPath + File.separator + clickDirName);
        tmpDir.mkdir();

        String[] instructions = {
                "1) Click on 'Show File Dialog' button. A file dialog will come up.",
                "2) Double-click on '" + clickDirName + "' and click a confirmation",
                "   button, it can be 'OK', 'Save' or any other platform-dependent name.",
                "3) See result of the test below"
        };

        Sysout.createDialogWithInstructions(instructions);

    }//End  init()

    public void start() {
        setSize(200, 200);
        show();
    }// start()

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == showBtn) {
            fd.setFile("input");
            fd.setDirectory(dirPath);
            fd.setVisible(true);
            String output = fd.getFile();
            if ("input".equals(output)) {
                Sysout.println("TEST PASSED");
            } else {
                Sysout.println("TEST FAILED (output file - " + output + ")");
            }
        }
    }
}// class ManualYesNoTest
