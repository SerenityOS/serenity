/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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


import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/** Provides an editable text field with history. */

public class HistoryComboBox extends JComboBox<String> {
  static final int HISTORY_LENGTH = 15;

  public HistoryComboBox() {
    setEditable(true);
    addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Object text = getSelectedItem();
          if (text != null) {
            setText((String)text);
          }
        }
      });
  }

  public String getText() {
    Object text = getSelectedItem();
    if (text == null) {
      return "";
    }
    return (String)text;
  }

  public void setText(String text) {
    removeItem(text);
    insertItemAt(text, 0);
    setSelectedItem(text);
    int length = getModel().getSize();
    while (length > HISTORY_LENGTH) {
      removeItemAt(--length);
    }
  }
}
