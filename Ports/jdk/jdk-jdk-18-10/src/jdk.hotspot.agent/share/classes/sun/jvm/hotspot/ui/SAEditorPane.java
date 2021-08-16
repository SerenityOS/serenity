/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.event.*;
import java.awt.im.InputContext;
import java.awt.datatransfer.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

/** A simple subclass of JEditorPane for displaying uneditable html
 */

public class SAEditorPane extends JEditorPane {

  public SAEditorPane() {
    setEditable(false);
    setContentType("text/html");
  }

  /**
      Override getSelectedText so that <br> elements produce \n when
      text is copied out of the window.
   */

  public String getSelectedText() {
    StringBuilder result = new StringBuilder();
    Document doc = getDocument();

    int start = getSelectionStart();
    int end = getSelectionEnd();

    try {
      // Create an iterator using the root element
      ElementIterator it = new ElementIterator(doc.getDefaultRootElement());

      // Iterate all content elements (which are leaves)
      Element e;
      String separator = System.getProperty("line.separator");
      while ((e = it.next()) != null) {
        if (e.isLeaf()) {
          int rangeStart = e.getStartOffset();
          int rangeEnd = e.getEndOffset();

          if (rangeEnd < start || rangeStart > end) continue;
          if (end < rangeEnd) rangeEnd = end;
          if (start > rangeStart) rangeStart = start;
          try {
            String line = getText(rangeStart, rangeEnd-rangeStart);
            if (e.getName().equals("br"))
              result.append(separator);
            else
              result.append(line);
          } catch (BadLocationException ex) {
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return result.toString();
  }

  public void setText(String text) {
    super.setText(text);
    // put the cursor at the top instead of leaving it at the end.
    setCaretPosition(0);
  }
}
