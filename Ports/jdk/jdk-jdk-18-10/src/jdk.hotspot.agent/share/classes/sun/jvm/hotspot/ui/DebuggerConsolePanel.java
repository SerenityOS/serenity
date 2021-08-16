/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
import javax.swing.event.*;
import javax.swing.text.*;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.utilities.*;

/** A JPanel subclass containing a scrollable text area displaying the
    debugger's console, if it has one. This should not be created for
    a debugger which does not have a console. */

public class DebuggerConsolePanel extends JPanel {
  private Debugger debugger;
  private JTextComponent editor;
  private boolean updating;
  private int     mark;
  private String  curText;  // handles multi-line input via '\'
  // Don't run the "main" method of this class unless this flag is set to true first
  private static final boolean DEBUGGING = false;

  public DebuggerConsolePanel(Debugger debugger) {
    this.debugger = debugger;
    if (!DEBUGGING) {
      if (Assert.ASSERTS_ENABLED) {
        Assert.that(debugger.hasConsole(), "should not create a DebuggerConsolePanel for non-console debuggers");
      }
    }

    setLayout(new BorderLayout());

    editor = new JTextArea();
    editor.setDocument(new EditableAtEndDocument());
    editor.setFont(GraphicsUtilities.getMonospacedFont());
    JScrollPane scroller = new JScrollPane();
    scroller.getViewport().add(editor);
    add(scroller, BorderLayout.CENTER);

    editor.getDocument().addDocumentListener(new DocumentListener() {
        public void changedUpdate(DocumentEvent e) {
        }

        public void insertUpdate(DocumentEvent e) {
          if (updating) return;
          beginUpdate();
          editor.setCaretPosition(editor.getDocument().getLength());
          if (insertContains(e, '\n')) {
            String cmd = getMarkedText();
            // Handle multi-line input
            if ((cmd.length() == 0) || (cmd.charAt(cmd.length() - 1) != '\\')) {
              // Trim "\\n" combinations
              cmd = trimContinuations(cmd);
              final String result;
              if (DEBUGGING) {
                System.err.println("Entered command: \"" + cmd + "\"");
                result = "";
              } else {
                result = DebuggerConsolePanel.this.debugger.consoleExecuteCommand(cmd);
              }

              SwingUtilities.invokeLater(new Runnable() {
                  public void run() {
                    print(result);
                    printPrompt();
                    setMark();
                    endUpdate();
                  }
                });
            }
          } else {
            endUpdate();
          }
        }

        public void removeUpdate(DocumentEvent e) {
        }
      });

    // This is a bit of a hack but is probably better than relying on
    // the JEditorPane to update the caret's position precisely the
    // size of the insertion
    editor.addCaretListener(new CaretListener() {
        public void caretUpdate(CaretEvent e) {
          int len = editor.getDocument().getLength();
          if (e.getDot() > len) {
            editor.setCaretPosition(len);
          }
        }
      });

    Box hbox = Box.createHorizontalBox();
    hbox.add(Box.createGlue());
    JButton button = new JButton("Clear Saved Text");
    button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          clear();
        }
      });
    hbox.add(button);
    hbox.add(Box.createGlue());
    add(hbox, BorderLayout.SOUTH);

    clear();
  }

  public void requestFocus() {
    editor.requestFocus();
  }

  public void clear() {
    EditableAtEndDocument d = (EditableAtEndDocument) editor.getDocument();
    d.clear();
    printPrompt();
    setMark();
    editor.requestFocus();
  }

  public void setMark() {
    ((EditableAtEndDocument) editor.getDocument()).setMark();
  }

  public String getMarkedText() {
    try {
      String s = ((EditableAtEndDocument) editor.getDocument()).getMarkedText();
      int i = s.length();
      while ((i > 0) && (s.charAt(i - 1) == '\n')) {
        i--;
      }
      return s.substring(0, i);
    }
    catch (BadLocationException e) {
      e.printStackTrace();
      return null;
    }
  }

  //--------------------------------------------------------------------------------
  // Internals only below this point
  //

  private void beginUpdate() {
    updating = true;
  }

  private void endUpdate() {
    updating = false;
  }

  private void print(String s) {
    Document d = editor.getDocument();
    try {
      d.insertString(d.getLength(), s, null);
    }
    catch (BadLocationException e) {
      e.printStackTrace();
    }
  }

  private void printPrompt() {
    if (DEBUGGING) {
      print("foo> ");
    } else {
      print(debugger.getConsolePrompt());
    }
  }

  private boolean insertContains(DocumentEvent e, char c) {
    String s = null;
    try {
      s = editor.getText(e.getOffset(), e.getLength());
      for (int i = 0; i < e.getLength(); i++) {
        if (s.charAt(i) == c) {
          return true;
        }
      }
    }
    catch (BadLocationException ex) {
      ex.printStackTrace();
    }
    return false;
  }

  private String trimContinuations(String text) {
    int i;
    while ((i = text.indexOf("\\\n")) >= 0) {
      text = text.substring(0, i) + text.substring(i+2, text.length());
    }
    return text;
  }

  public static void main(String[] args) {
    JFrame frame = new JFrame();
    frame.getContentPane().setLayout(new BorderLayout());
    DebuggerConsolePanel panel = new DebuggerConsolePanel(null);
    frame.getContentPane().add(panel, BorderLayout.CENTER);
    frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          System.exit(0);
        }
      });
    frame.setSize(500, 500);
    frame.setVisible(true);
    panel.requestFocus();
  }
}
