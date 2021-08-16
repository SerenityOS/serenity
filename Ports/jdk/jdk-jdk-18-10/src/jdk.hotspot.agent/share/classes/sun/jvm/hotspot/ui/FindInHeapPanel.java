/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.utilities.*;

/** Finds a given (Address) value in the heap. Only intended for use
    in a debugging system. */

public class FindInHeapPanel extends JPanel {
  private RawHeapVisitor   iterator;
  private long             addressSize;
  private long             usedSize;
  private long             iterated;
  private Address          value;
  private ProgressBarPanel progressBar;
  private HistoryComboBox  addressField;
  private JButton          findButton;
  private JTextArea        textArea;
  private ArrayList<String> updates;
  private double           lastFrac;

  static final double minUpdateFraction = 0.05;

  public FindInHeapPanel() {
    super();

    setLayout(new BorderLayout());

    JPanel topPanel = new JPanel();
    topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    panel.add(new JLabel("Address to search for:"));

    addressField = new HistoryComboBox();
    panel.add(addressField);

    addressSize = VM.getVM().getAddressSize();

    iterator = new RawHeapVisitor() {
        boolean error = false;

        public void prologue(long used) {
          usedSize = used;
          iterated = 0;
          lastFrac = 0;
          error = false;
          updates = new ArrayList<>();
        }

        public void visitAddress(Address addr) {
          if (error) return;

          Address val = addr.getAddressAt(0);
          if (AddressOps.equal(val, value)) {
            error = reportResult(addr);
          }
          iterated += addressSize;
          updateProgressBar();
        }
        public void visitCompOopAddress(Address addr) {
          if (error) return;

          Address val = addr.getCompOopAddressAt(0);
          if (AddressOps.equal(val, value)) {
            error = reportResult(addr);
          }
          iterated += addressSize;
          updateProgressBar();

        }
        public void epilogue() {
          iterated = 0;
          updateProgressBar();
          findButton.setEnabled(true);
        }
      };

    findButton = new JButton("Find");
    ActionListener listener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          clearResultWindow();
          // Parse text
          try {
            Address val = VM.getVM().getDebugger().parseAddress(addressField.getText());
            value = val;

            findButton.setEnabled(false);

            java.lang.Thread t = new java.lang.Thread(new Runnable() {
                public void run() {
                  try {
                    VM.getVM().getObjectHeap().iterateRaw(iterator);
                  } finally {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                          findButton.setEnabled(true);
                        }
                      });
                  }
                }
              });
            t.start();
          } catch (Exception ex) {
            textArea.setText("Error parsing address");
          }
        }
      };
    panel.add(findButton);
    findButton.addActionListener(listener);
    addressField.addActionListener(listener);
    topPanel.add(panel);

    progressBar = new ProgressBarPanel(ProgressBarPanel.HORIZONTAL, "Search progress:");
    topPanel.add(progressBar);

    add(topPanel, BorderLayout.NORTH);

    textArea = new JTextArea();
    JScrollPane scroller = new JScrollPane(textArea);
    add(scroller, BorderLayout.CENTER);
  }

  private boolean pendingUpdate = false;

  private boolean reportResult(final Address addr) {
    synchronized (this) {
      try {
        updates.add("found at " + addr + "\n");
        if (!pendingUpdate) {
          pendingUpdate = true;
          SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                updateResultWindow();
              }
            });
        }
      } catch (Throwable t) {
        t.printStackTrace();
        return true;
      }
    }

    return false;
  }

  private void clearResultWindow() {
    SwingUtilities.invokeLater(new Runnable() {
        public void run() {

          Document d = textArea.getDocument();
          try {
            d.remove(0, d.getLength());
          } catch (BadLocationException e) {
          }
        }
      });
  }

  private synchronized void updateResultWindow() {
    if (updates.size() > 0) {
      Iterator i = updates.iterator();
      while (i.hasNext()) {
        textArea.append((String)i.next());
      }
      updates = new ArrayList<>();;
    }
    pendingUpdate = false;
  }

  private void invokeInDispatchThread(Runnable runnable) {
    if (EventQueue.isDispatchThread()) {
      runnable.run();
    } else {
      SwingUtilities.invokeLater(runnable);
    }
  }

  private void updateProgressBar() {
    final double frac = (double) iterated / (double) usedSize;
    if (frac == 0.0 || (frac - lastFrac > minUpdateFraction)) {
      lastFrac = frac;
      if (iterated > usedSize) {
        System.out.println("iterated " + iterated + " usedSize " + usedSize);
      }
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            progressBar.setValue(frac);
          }
        });
    }
  }
}
