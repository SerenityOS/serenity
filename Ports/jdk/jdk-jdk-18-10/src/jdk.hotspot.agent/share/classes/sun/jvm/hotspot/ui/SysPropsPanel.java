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
import java.util.*;
import javax.swing.*;

import sun.jvm.hotspot.runtime.*;

/** Shows values of Java System properties. */

public class SysPropsPanel extends JPanel {
    private SAEditorPane         flagsPane;

    public SysPropsPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        flagsPane = new SAEditorPane();
        flagsPane.setText(getFlags());

        add(new JScrollPane(flagsPane), BorderLayout.CENTER);
    }

    private String getFlags() {
       final StringBuilder buf = new StringBuilder();
       buf.append("<html><head><title>System Properties</title></head><body>");
       buf.append("<table border='1'>");

       Properties sysProps = VM.getVM().getSystemProperties();
       if (sysProps != null) {
          Enumeration keys = sysProps.keys();
          while (keys.hasMoreElements()) {
             Object key = keys.nextElement();
             buf.append("<tr><td>");
             buf.append(key.toString());
             buf.append("</td><td>");
             buf.append(sysProps.get(key).toString());
             buf.append("</td></tr>");
          }
       } else {
          buf.append("<tr><td>System Properties info not available!</td></tr>");
       }
       buf.append("</table>");
       buf.append("</body></html>");
       return buf.toString();
    }
}
