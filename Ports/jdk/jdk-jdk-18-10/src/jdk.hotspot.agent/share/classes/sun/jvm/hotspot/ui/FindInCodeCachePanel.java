/*
 * Copyright (c) 2004, 2010, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.ui.classbrowser.*;

/** Finds a given (Address) value in the code cache. Only intended for use
    in a debugging system. */

public class FindInCodeCachePanel extends SAPanel {
  private Visitor          iterator;
  private long             usedSize;
  private long             iterated;
  private Address          value;
  private ProgressBarPanel progressBar;
  private HistoryComboBox  addressField;
  private JButton          findButton;
  private SAEditorPane     contentEditor;

  class Visitor implements CodeCacheVisitor {
    Address base;
    StringBuffer result;
    boolean searching;

    public void prologue(Address start, Address end) {
      searching = true;
      base = start;
      usedSize = end.minus(start);
      iterated = 0;
      result = new StringBuffer();
      clearResultWindow();
    }

    public void visit(CodeBlob blob) {
      Address begin = blob.headerBegin();
      Address end = begin.addOffsetTo(blob.getSize());
      long addressSize = VM.getVM().getAddressSize();

      boolean found = false;
      while (!found && begin.lessThan(end)) {
        Address val = begin.getAddressAt(0);
        if (AddressOps.equal(val, value)) {
          reportResult(result, blob);
          found = true;
        }
        begin = begin.addOffsetTo(addressSize);
      }
      iterated = end.minus(base);;
      updateProgressBar(null);
    }

    public void epilogue() {
    }

    public void cleanup() {
      iterated = 0;
      updateProgressBar(result);
      searching = false;
      result = null;
    }

    private void search() {
      // Parse text
      Address val = null;
      try {
        val = VM.getVM().getDebugger().parseAddress(addressField.getText());
      } catch (Exception ex) {
        contentEditor.setText("<b>Error parsing address</b>");
        return;
      }

      // make sure we don't start up multiple search threads in parallel
      synchronized (iterator) {
        if (searching && value.equals(val)) {
          return;
        }

        value = val;
        contentEditor.setText("");
        findButton.setEnabled(false);

        System.out.println("Searching " + value);
        java.lang.Thread t = new java.lang.Thread(new Runnable() {
            public void run() {
              synchronized (iterator) {
                try {
                  VM.getVM().getCodeCache().iterate(iterator);
                } finally {
                  iterator.cleanup();
                }
              }
            }
          });
        t.start();
      }
    }
  }


  public FindInCodeCachePanel() {
    super();

    setLayout(new BorderLayout());

    JPanel topPanel = new JPanel();
    topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    panel.add(new JLabel("Address to search for:"));

    addressField = new HistoryComboBox();
    panel.add(addressField);

    iterator = new Visitor();

    findButton = new JButton("Find");
    ActionListener listener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          iterator.search();
        }
      };
    panel.add(findButton);
    findButton.addActionListener(listener);
    addressField.addActionListener(listener);
    topPanel.add(panel);

    progressBar = new ProgressBarPanel(ProgressBarPanel.HORIZONTAL, "Search progress:");
    topPanel.add(progressBar);

    add(topPanel, BorderLayout.NORTH);

    contentEditor = new SAEditorPane();

    HyperlinkListener hyperListener = new HyperlinkListener() {
        public void hyperlinkUpdate(HyperlinkEvent e) {
          if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            String description = e.getDescription();
            int index = description.indexOf(':');
            if (index != -1) {
              String item = description.substring(0, index);
              if (item.equals("blob")) {
                Address blob = VM.getVM().getDebugger().parseAddress(description.substring(index + 1));
                showCodeViewer(blob);
              }
            }
          }
        }
      };

    contentEditor.addHyperlinkListener(hyperListener);

    JScrollPane scroller = new JScrollPane(contentEditor);
    add(scroller, BorderLayout.CENTER);
  }

  private void reportResult(StringBuffer result, CodeBlob blob) {
    result.append("<a href='blob:");
    result.append(blob.contentBegin().toString());
    result.append("'>");
    result.append(blob.getName());
    result.append("@");
    result.append(blob.contentBegin());
    result.append("</a><br>");
  }

  private void clearResultWindow() {
    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          contentEditor.setText("");
        }
      });
  }

  private void updateProgressBar(final StringBuffer result) {
    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          progressBar.setValue((double) iterated / (double) usedSize);
          if (result != null) {
            String s = "<html> <head> </head> <body>\n" + result + " </body> </html>";
            contentEditor.setText(s);
            findButton.setEnabled(true);
          }
        }
      });
  }
}
