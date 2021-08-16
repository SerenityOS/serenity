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

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * @test
 * @key headful
 * @bug 8072775
 * @run main/othervm -Xmx80m TextViewOOM
 */
public class TextViewOOM {

    private static JFrame frame;
    private static JTextArea ta;
    private static final String STRING = "\uDC00\uD802\uDFFF";
    private static final int N = 5000;

    private static void createAndShowGUI() {
        frame = new JFrame();
        final JScrollPane jScrollPane1 = new JScrollPane();
        ta = new JTextArea();

        ta.setEditable(false);
        ta.setColumns(20);
        ta.setRows(5);
        jScrollPane1.setViewportView(ta);
        frame.add(ta);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(final String[] args) throws Exception {
        /* Create and display the form */
        EventQueue.invokeAndWait(TextViewOOM::createAndShowGUI);
        for (int i = 0; i < 10; i++) {
            System.gc();
            Thread.sleep(1000);
        }
        long mem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.err.println("Memory before creating the text: "+mem);
        final StringBuilder sb = new StringBuilder(N * STRING.length());
        for (int i = 0; i < N; i++) {
            sb.append(STRING);
        }
        for (int i = 0; i < 10; i++) {
            System.gc();
            Thread.sleep(1000);
        }
        mem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.err.println("Memory after  creating the text: "+mem);

        EventQueue.invokeAndWait(() -> {
            ta.setText(sb.toString());
            for (int i = 0; i < 10; i++) {
                System.gc();
                try {Thread.sleep(200);} catch (InterruptedException iex) {}
            }
            long mem1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            System.err.println("Memory after  setting the text: " + mem1);
        });
        for (int i = 0; i < 10; i++) {
            System.gc();
            Thread.sleep(1000);
        }
        mem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.err.println("Final memory  after everything: " + mem);
        EventQueue.invokeAndWait(frame::dispose);
    }
}
