/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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


import jdk.test.lib.Platform;
import test.java.awt.regtesthelpers.Sysout;

import java.applet.Applet;
import java.awt.Button;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FileDialogForPackages extends Applet implements ActionListener {
    private static final String APPLICATIONS_FOLDER = "/Applications";

    private volatile Button showBtn;
    private volatile FileDialog fd;

    @Override
    public void init() {
        if (!Platform.isOSX()) {
            Sysout.createDialogWithInstructions(new String[]{
                    "Press PASS, this test is for MacOS X only."});
            return;
        }

        System.setProperty("apple.awt.use-file-dialog-packages", "true");

        setLayout(new GridLayout(1, 1));

        fd = new FileDialog(new Frame(), "Open");
        fd.setDirectory(APPLICATIONS_FOLDER);

        showBtn = new Button("Show File Dialog");
        showBtn.addActionListener(this);
        add(showBtn);
        String[] instructions = {
                "1) Click on 'Show File Dialog' button. A file dialog will come up.",
                "2) Navigate to the Applications folder if not already there",
                "3) Check that the application bundles can be selected and can not be navigated",
                "4) If it's true then the test passed, otherwise it failed."};
        Sysout.createDialogWithInstructions(instructions);
    }

    @Override
    public void start() {
        setSize(200, 200);
        show();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == showBtn) {
            fd.setVisible(true);
            String output = fd.getFile();
            if (output != null) {
                Sysout.println(output + " is selected");
            }
        }
    }
}
