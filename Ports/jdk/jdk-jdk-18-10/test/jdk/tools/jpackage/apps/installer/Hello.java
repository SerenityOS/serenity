/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.awt.Desktop;
import java.awt.desktop.OpenFilesEvent;
import java.awt.desktop.OpenFilesHandler;
import java.util.List;

public class Hello implements OpenFilesHandler {

    private static final String MSG = "jpackage test application";
    private static final int EXPECTED_NUM_OF_PARAMS = 3; // Starts at 1
    private static List<File> files;

    public static void main(String[] args) {
        if(Desktop.getDesktop().isSupported(Desktop.Action.APP_OPEN_FILE)) {
            Desktop.getDesktop().setOpenFileHandler(new Hello());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        printToStdout(args);
        if (args.length == 1 || (files != null && files.size() == 1)) { // Called via file association
            printToFile(args);
        }
    }

    private static void printToStdout(String[] args) {
        System.out.println(MSG);

        System.out.println("args.length: " + (files == null ? args.length : args.length + files.size()));

        for (String arg : args) {
            System.out.println(arg);
        }

        if (files != null) {
            for (File file : files) {
                System.out.println(file.getAbsolutePath());
            }
        }

        for (int index = 1; index <= EXPECTED_NUM_OF_PARAMS; index++) {
            String value = System.getProperty("param" + index);
            if (value != null) {
                System.out.println("-Dparam" + index + "=" + value);
            }
        }
    }

    private static void printToFile(String[] args) {
        File inputFile = files == null ? new File(args[0]) : files.get(0);
        String outputFile = inputFile.getParent() + File.separator + "appOutput.txt";
        File file = new File(outputFile);

        try (PrintWriter out
                = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            out.println(MSG);

            out.println("args.length: " + (files == null ? args.length : args.length + files.size()));

            for (String arg : args) {
                out.println(arg);
            }

            if (files != null) {
                for (File f : files) {
                    out.println(f.getAbsolutePath());
                }
            }

            for (int index = 1; index <= EXPECTED_NUM_OF_PARAMS; index++) {
                String value = System.getProperty("param" + index);
                if (value != null) {
                    out.println("-Dparam" + index + "=" + value);
                }
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    @Override
    public void openFiles(OpenFilesEvent e) {
        files = e.getFiles();
    }
}
