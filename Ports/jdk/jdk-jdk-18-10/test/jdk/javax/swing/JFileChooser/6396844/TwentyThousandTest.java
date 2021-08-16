/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6396844
 * @summary Tests memory leak for 20000 files
 * @author Sergey Malenkov
 * @library ../../regtesthelpers
 * @modules java.desktop/sun.java2d
 * @build Util
 * @run main/othervm/timeout=1000 -mx128m TwentyThousandTest
 */

import sun.java2d.Disposer;
import sun.java2d.DisposerRecord;

import javax.swing.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.File;
import java.io.FileWriter;

public class TwentyThousandTest {

    private static final int FILES = 20000;
    private static final int ATTEMPTS = 20;
    private static final int INTERVAL = 100;

    private static String tmpDir;

    private static volatile boolean disposerComplete;

    public static void main(String[] args) throws Exception {
        tmpDir = System.getProperty("java.io.tmpdir");

        if (tmpDir.length() == 0) { //'java.io.tmpdir' isn't guaranteed to be defined
            tmpDir = System.getProperty("user.home");
        }

        System.out.println("Temp directory: " + tmpDir);

        System.out.println("Creating " + FILES + " files");

        for (int i = 0; i < FILES; i++) {
            File file = getTempFile(i);

            FileWriter writer = new FileWriter(file);
            writer.write("File " + i);
            writer.close();
        }

        for (UIManager.LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
            if (laf.getClassName().contains("Motif")) {
                continue;
            }

            UIManager.setLookAndFeel(laf.getClassName());

            System.out.println("Do " + ATTEMPTS + " attempts for " + laf.getClassName());

            for (int i = 0; i < ATTEMPTS; i++) {
                System.out.print(i + " ");

                doAttempt();
            }

            System.out.println();
        }

        System.out.println("Removing " + FILES + " files");

        for (int i = 0; i < FILES; i++) {
            getTempFile(i).delete();
        }

        System.out.println("Test passed successfully");
    }

    private static File getTempFile(int i) {
        return new File(tmpDir, "temp" + i + ".txt");
    }

    private static void doAttempt() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                final JFileChooser chooser = new JFileChooser(tmpDir);

                chooser.updateUI();

                // Postpone JFileChooser closing until it becomes visible
                chooser.addHierarchyListener(new HierarchyListener() {
                    @Override
                    public void hierarchyChanged(HierarchyEvent e) {
                        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                            if (chooser.isShowing()) {
                                Thread thread = new Thread(new Runnable() {
                                    public void run() {
                                        try {
                                            Thread.sleep(INTERVAL);

                                            // Close JFileChooser
                                            SwingUtilities.invokeLater(new Runnable() {
                                                public void run() {
                                                    chooser.cancelSelection();
                                                }
                                            });
                                        } catch (InterruptedException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                });

                                thread.start();
                            }
                        }
                    }
                });

                chooser.showOpenDialog(null);
            }
        });

        DisposerRecord disposerRecord = new DisposerRecord() {
            public void dispose() {
                disposerComplete = true;
            }
        };

        disposerComplete = false;

        Disposer.addRecord(new Object(), disposerRecord);

        while (!disposerComplete) {
            Util.generateOOME();
        }
    }
}
