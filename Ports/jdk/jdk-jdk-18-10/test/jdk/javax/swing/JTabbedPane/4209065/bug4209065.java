/*
 * Copyright (c) 1999, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.InvocationTargetException;

import javax.swing.JApplet;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;

/**
 * @test
 * @bug 4209065
 * @author Georges Saab
 * @run applet/manual=yesno bug4209065.html
 */
public final class bug4209065 extends JApplet {

    @Override
    public void init() {
        try {
            EventQueue.invokeAndWait(this::createTabbedPane);
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private void createTabbedPane() {
        JTabbedPane tp = new JTabbedPane();
        getContentPane().add(tp);
        String text = "<html><center>If the style of the text on the tabs matches"
                      + "<br>the descriptions, press <em><b>PASS</b></em></center></html>";
        tp.addTab("<html><center><font size=+3>big</font></center></html>", new JLabel(text));
        tp.addTab("<html><center><font color=red>red</font></center></html>", new JLabel(text));
        tp.addTab("<html><center><em><b>Bold Italic!</b></em></center></html>", new JLabel(text));
    }
}
