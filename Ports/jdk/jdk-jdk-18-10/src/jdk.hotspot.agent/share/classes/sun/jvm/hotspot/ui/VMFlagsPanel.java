/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.ui;

import java.awt.*;
import javax.swing.*;

import sun.jvm.hotspot.runtime.*;

/** Shows values of Java command line flags */

public class VMFlagsPanel extends JPanel {
    private JEditorPane         flagsPane;

    public VMFlagsPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        flagsPane = new JEditorPane();
        flagsPane.setContentType("text/html");
        flagsPane.setEditable(false);
        flagsPane.setText(getFlags());

        add(new JScrollPane(flagsPane), BorderLayout.CENTER);
    }

    private String getFlags() {
       VM.Flag[] flags = VM.getVM().getCommandLineFlags();
       StringBuilder buf = new StringBuilder();
       buf.append("<html><head><title>VM Command Line Flags</title></head><body>");
       if (flags == null) {
          buf.append("<b>Command Flag info not available (use 1.4.1_03 or later)!</b>");
       } else {
          buf.append("<table border='1'>");
          for (int f = 0; f < flags.length; f++) {
             buf.append("<tr><td>");
             buf.append(flags[f].getName());
             buf.append("</td><td>");
             buf.append(flags[f].getValue());
             buf.append("</td>");
          }
          buf.append("</table>");
       }

       buf.append("</body></html>");
       return buf.toString();
    }
}
