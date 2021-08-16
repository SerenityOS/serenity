/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4931668
 * @summary Tests XEmbed server/client functionality
 * @author denis mikhalkin: area=awt.xembed
 * @requires (!(os.family=="mac") & !(os.family=="windows"))
 * @modules java.desktop/sun.awt
 * @compile JavaClient.java TesterClient.java TestXEmbedServer.java
 * @run main/manual TestXEmbedServerJava
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;

public class TestXEmbedServerJava extends TestXEmbedServer {
    public static void main(String[] args) {

        // Enabled XEmbed
        System.setProperty("sun.awt.xembedserver", "true");

        String instruction =
            "This is a manual test for XEmbed server functionality. \n" +
            "You may start XEmbed client by pressing 'Add client' button.\n" +
            "Check that focus transfer with mouse works, that focus traversal with Tab/Shift-Tab works.\n" +
            "Check that XEmbed server client's growing and shrinking.\n" +
            "Check that Drag&Drop works in all combinations.\n" +
            "Check the keyboard input works in both text fields.\n";
        Frame f = new Frame("Instructions");
        f.setLayout(new BorderLayout());
        f.add(new TextArea(instruction), BorderLayout.CENTER);
        f.pack();
        f.setLocation(0, 400);
        f.setVisible(true);

        TestXEmbedServerJava lock = new TestXEmbedServerJava();
        try {
            synchronized(lock) {
                lock.wait();
            }
        } catch (InterruptedException e) {
        }
        if (!lock.isPassed()) {
            throw new RuntimeException("Test failed");
        }
    }

    public TestXEmbedServerJava() {
        super(true);
    }

    public Process startClient(Rectangle[] bounds, long window) {
        try {
            String java_home = System.getProperty("java.home");
            boolean hasModules = true;
            try {
                Class.class.getMethod("getModule");
            }catch(Exception hasModulesEx) {
                hasModules = false;
            }
            if (hasModules) {
                System.out.println(java_home +
                               "/bin/java --add-exports java.desktop/sun.awt.X11=ALL-UNNAMED "+
                               "--add-exports java.desktop/sun.awt=ALL-UNNAMED  JavaClient " + window);
                return Runtime.getRuntime().exec(java_home +
                               "/bin/java --add-exports java.desktop/sun.awt.X11=ALL-UNNAMED "+
                               "--add-exports java.desktop/sun.awt=ALL-UNNAMED  JavaClient " + window);
            }else{
                System.out.println(java_home + "/bin/java JavaClient " + window);
                return Runtime.getRuntime().exec(java_home + "/bin/java JavaClient " + window);
            }
        } catch (IOException ex1) {
            ex1.printStackTrace();
        }
        return null;
    }
}
