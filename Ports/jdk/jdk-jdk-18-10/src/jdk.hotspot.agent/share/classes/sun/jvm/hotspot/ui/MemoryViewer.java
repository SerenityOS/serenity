/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.event.*;
import javax.swing.*;
import sun.jvm.hotspot.debugger.*;

/** Wraps a MemoryPanel with a field allowing the user to type in an
    address. */

public class MemoryViewer extends JPanel {
  public MemoryViewer(final Debugger debugger, boolean is64Bit) {
    super();
    final MemoryPanel memory = new MemoryPanel(debugger, is64Bit);
    memory.setBorder(GraphicsUtilities.newBorder(5));
    JPanel addressPanel = new JPanel();
    addressPanel.setLayout(new BoxLayout(addressPanel, BoxLayout.X_AXIS));
    addressPanel.add(new JLabel("Address: "));
    final JTextField addressField = new JTextField(20);
    addressPanel.add(addressField);
    addressField.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          try {
            memory.makeVisible(debugger.parseAddress(addressField.getText()));
          } catch (NumberFormatException ex) {
          }
        }
      });
    setLayout(new BorderLayout());
    add(addressPanel, BorderLayout.NORTH);
    add(memory, BorderLayout.CENTER);
  }
}
