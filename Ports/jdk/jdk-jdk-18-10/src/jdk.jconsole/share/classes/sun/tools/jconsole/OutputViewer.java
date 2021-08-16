/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.tools.jconsole;

import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;

import javax.swing.*;

/**
 * A simple console window to display messages sent to System.out and
 * System.err.
 *
 * A stop-gap solution until an error dialog is implemented.
 */
public class OutputViewer {
    private static JFrame frame;
    private static JTextArea ta;

    static {
        System.setOut(PipeListener.create("System.out"));
        System.setErr(PipeListener.create("System.err"));
    }

    // Dummy to cause class to be loaded
    public static void init() { }

    private static void append(String s) {
        if (frame == null) {
            // FIXME: The frame title should be a localized string.
            frame = new JFrame("JConsole: Output");
            ta = new JTextArea();
            ta.setEditable(false);
            frame.getContentPane().add(new JScrollPane(ta));
            ta.setFont(new Font("Monospaced", Font.BOLD, 14));
            frame.setSize(500, 600);
            frame.setLocation(1024-500, 768-600);
            // Exit JConsole if no window remains.
            // e.g. jconsole -version only creates the OutputViewer
            // but no other window.
            frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    if (JFrame.getFrames().length == 1) {
                        System.exit(0);
                    }
                }
            });
        }
        ta.append(s);
        ta.setCaretPosition(ta.getText().length());
        frame.setVisible(true);
    }

    private static void appendln(String s) {
        append(s+"\n");
    }

    private static class PipeListener extends Thread {
        public PrintStream ps;
        private String name;
        private PipedInputStream inPipe;
        private BufferedReader br;

        public static PrintStream create(String name) {
            return new PipeListener(name).ps;
        }

        private PipeListener(String name) {
            this.name = name;

            try {
                inPipe = new PipedInputStream();
                ps = new PrintStream(new PipedOutputStream(inPipe));
                br = new BufferedReader(new InputStreamReader(inPipe));
            } catch (IOException e) {
                appendln("PipeListener<init>("+name+"): " + e);
            }
            start();
        }

        public void run() {
            try {
                String str;
                while ((str = br.readLine()) != null) {
                    appendln(str);

                    // Hack: Turn off thread check in PipedInputStream.
                    // Any thread should be allowed to write except this one
                    // but we just use this one to keep the pipe alive.
                    try {
                        java.lang.reflect.Field f =
                            PipedInputStream.class.getDeclaredField("writeSide");
                        f.setAccessible(true);
                        f.set(inPipe, this);
                    } catch (Exception e) {
                        appendln("PipeListener("+name+").run: "+e);
                    }
                }
                appendln("-- "+name+" closed --");
                br.close();
            } catch (IOException e) {
                appendln("PipeListener("+name+").run: "+e);
            }
        }
    }
}
