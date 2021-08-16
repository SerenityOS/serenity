/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Dialog;
import java.awt.Frame;
import java.io.*;
import javax.swing.*;
import sun.awt.SunToolkit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @test
 * @key headful
 * @bug 8043705
 * @summary Can't exit color chooser dialog when running as an applet
 * @modules java.desktop/sun.awt
 * @run main CloseDialogTest
 */

public class CloseDialogTest {

    private static volatile Frame frame;
    private static volatile Dialog dialog;
    private static volatile InputStream testErrorStream;
    private static final PrintStream systemErrStream = System.err;
    private static final AtomicReference<Exception> caughtException
            = new AtomicReference<>();

    public static void main(String[] args) throws Exception {

        // redirect System err
        PipedOutputStream errorOutputStream = new PipedOutputStream();
        testErrorStream = new PipedInputStream(errorOutputStream);
        System.setErr(new PrintStream(errorOutputStream));

        ThreadGroup swingTG = new ThreadGroup(getRootThreadGroup(), "SwingTG");
        try {
            new Thread(swingTG, () -> {
                SunToolkit.createNewAppContext();
                SwingUtilities.invokeLater(() -> {
                    frame = new Frame();
                    frame.setSize(300, 300);
                    frame.setVisible(true);

                    dialog = new Dialog(frame);
                    dialog.setSize(200, 200);
                    dialog.setModal(true);
                    dialog.setVisible(true);
                });
            }).start();

            Thread.sleep(400);

            Thread disposeThread = new Thread(swingTG, () ->
                    SwingUtilities.invokeLater(() -> {
                try {
                    while (dialog == null || !dialog.isVisible()) {
                        Thread.sleep(100);
                    }
                    dialog.setVisible(false);
                    dialog.dispose();
                    frame.dispose();
                } catch (Exception e) {
                    caughtException.set(e);
                }
            }));
            disposeThread.start();
            disposeThread.join();
            Thread.sleep(500);

            // read System err
            final char[] buffer = new char[2048];
            System.err.print("END");
            System.setErr(systemErrStream);
            try (Reader in = new InputStreamReader(testErrorStream, "UTF-8")) {
                int size = in.read(buffer, 0, buffer.length);
                String errorString = new String(buffer, 0, size);
                if (!errorString.startsWith("END")) {
                    System.err.println(errorString.
                            substring(0, errorString.length() - 4));
                    throw new RuntimeException("Error output is not empty!");
                }
            }
        } finally {
            if (caughtException.get() != null) {
                throw new RuntimeException("Failed. Caught exception!",
                        caughtException.get());
            }
        }
    }

    private static ThreadGroup getRootThreadGroup() {
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        while (threadGroup.getParent() != null) {
            threadGroup = threadGroup.getParent();
        }
        return threadGroup;
    }
}
