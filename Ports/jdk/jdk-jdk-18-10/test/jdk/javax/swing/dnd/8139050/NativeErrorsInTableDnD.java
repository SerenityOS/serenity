/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import static javax.swing.UIManager.getInstalledLookAndFeels;

/**
 * @test
 * @bug 8139050 8153871
 * @key headful
 * @library /lib/client
 * @build ExtendedRobot
 * @run main/othervm/timeout=360 -Xcheck:jni NativeErrorsInTableDnD
 */

public final class NativeErrorsInTableDnD {

    private static JFrame frame;

    private static volatile Rectangle bounds;

    public static void main(final String[] args) throws Exception {
        if (args.length == 0) {
            createChildProcess();
            return;
        }
        for (final UIManager.LookAndFeelInfo laf : getInstalledLookAndFeels()) {
            SwingUtilities.invokeAndWait(() -> setLookAndFeel(laf));

            SwingUtilities.invokeAndWait(() -> {
                final JTable table = new JTable(10, 10);
                frame = new JFrame();
                frame.setUndecorated(true);
                table.setDragEnabled(true);
                table.selectAll();
                frame.add(table);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            });
            final ExtendedRobot r = new ExtendedRobot();
            r.waitForIdle();
            SwingUtilities.invokeAndWait(() -> {
                bounds = frame.getBounds();
            });
            for (int i = 0; i < 5; ++i) {
                int x1 = bounds.x + bounds.width / 4;
                int y1 = bounds.y + bounds.height / 4;
                r.setAutoDelay(50);
                // Special sequence of clicks which reproduce the problem
                r.mouseMove(bounds.x + bounds.width / 7, y1);
                r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                r.mouseMove(x1, y1);
                r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                r.setAutoDelay(0);
                r.glide(x1, y1, x1 + bounds.width / 4, y1 + bounds.height / 4);
                r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            }
            SwingUtilities.invokeAndWait(() -> {
                frame.dispose();
            });
            r.waitForIdle();
        }
    }

    static void createChildProcess() throws Exception {
        final String javaPath = System.getProperty("java.home");
        final String classPathDir = System.getProperty("java.class.path");
        doExec(javaPath + File.separator + "bin" + File.separator + "java",
               "-cp", classPathDir, "NativeErrorsInTableDnD", "start");
    }

    static void doExec(final String... cmds) throws Exception {
        Process p;
        final ProcessBuilder pb = new ProcessBuilder(cmds);
        for (final String cmd : cmds) {
            System.out.print(cmd + " ");
        }
        System.out.println();
        BufferedReader rdr;
        final List<String> errorList = new ArrayList<>();
        final List<String> outputList = new ArrayList<>();
        p = pb.start();
        rdr = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String in = rdr.readLine();
        while (in != null) {
            outputList.add(in);
            in = rdr.readLine();
            System.out.println(in);
        }
        rdr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        in = rdr.readLine();
        while (in != null) {
            errorList.add(in);
            in = rdr.readLine();
            System.err.println(in);
        }
        p.waitFor();
        p.destroy();

        if (!errorList.isEmpty()) {
            throw new RuntimeException("Error log is not empty");
        }
        final int exit = p.exitValue();
        if (exit != 0) {
            throw new RuntimeException("Exit status = " + exit);
        }
    }

    private static void setLookAndFeel(final UIManager.LookAndFeelInfo laf) {
        try {
            UIManager.setLookAndFeel(laf.getClassName());
            System.out.println("LookAndFeel: " + laf.getClassName());
        } catch (ClassNotFoundException | InstantiationException |
                UnsupportedLookAndFeelException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
