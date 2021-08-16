/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.BadLocationException;

/** Panel supporting loading of and scrolling through source code.
    Contains convenience routines for implementing the Editor
    interface. */

public class SourceCodePanel extends JPanel {
  private JTextArea source;
  private RowHeader header;
  private String filename;
  // Amount of white space between edges, line numbers and icons
  private static final int LINE_NO_SPACE = 4;
  // Size of icons in resources directory
  private static final int ICON_SIZE = 12;
  // Icons used in panel drawing
  private static Icon topFrameCurLine;
  private static Icon lowerFrameCurLine;
  private static Icon breakpoint;
  // State
  private int highlightedLine = -1;
  private Set<Integer> breakpoints = new HashSet<>(); // Zero-based lines internally
  // Parent Editor container and EditorCommands object for setting breakpoints
  private EditorCommands comm;
  private Editor parent;

  /** Support for displaying icons and line numbers in row header of
      scroll pane */
  class RowHeader extends JPanel {
    private JViewport view;
    private boolean   showLineNumbers;
    private int       width;
    private int       rowHeight;
    private boolean   initted;

    public RowHeader() {
      super();
      initted = true;
      addHierarchyBoundsListener(new HierarchyBoundsAdapter() {
          public void ancestorResized(HierarchyEvent e) {
            recomputeSize();
          }
        });
    }

    public void paint(Graphics g) {
      super.paint(g);
      if (getShowLineNumbers()) {
        // Visible region of header panel, in coordinate system of the
        // panel, is provided by clip bounds of Graphics object. This
        // is used to figure out which line numbers to draw.
        Rectangle clip = g.getClipBounds();
        // To avoid missing lines, round down starting line number and
        // round up ending line number
        int start = clip.y / rowHeight;
        int end   = start + (clip.height + (rowHeight - 1)) / rowHeight;
        // Draw these line numbers, right justified to look better
        FontMetrics fm = getFontMetrics(getFont());
        int ascent = fm.getMaxAscent(); // Causes proper alignment -- trial-and-error
        for (int i = start; i <= end; i++) {
          // Line numbers are 1-based
          String str = Integer.toString(i + 1);
          int strWidth = GraphicsUtilities.getStringWidth(str, fm);
          g.drawString(str, width - strWidth - LINE_NO_SPACE, ascent + rowHeight * i);

          // Draw breakpoint if necessary
          if (breakpoints.contains(i)) {
            breakpoint.paintIcon(this, g, LINE_NO_SPACE, rowHeight * i);
          }

          // Draw current line icon if necessary
          if (i == highlightedLine) {
            // FIXME: use correct icon (not always topmost frame)
            topFrameCurLine.paintIcon(this, g, LINE_NO_SPACE, rowHeight * i);
          }
        }
      }
    }

    public boolean getShowLineNumbers() {
      return showLineNumbers;
    }

    public void setShowLineNumbers(boolean val) {
      if (val != showLineNumbers) {
        showLineNumbers = val;
        recomputeSize();
        // Force re-layout
        invalidate();
        validate();
      }
    }

    public void setFont(Font f) {
      super.setFont(f);
      rowHeight = getFontMetrics(f).getHeight();
      recomputeSize();
    }

    void setViewport(JViewport view) {
      this.view = view;
    }

    void recomputeSize() {
      if (!initted) return;
      if (view == null) return;
      width = ICON_SIZE + 2 * LINE_NO_SPACE;
      try {
        int numLines = 1 + source.getLineOfOffset(source.getDocument().getEndPosition().getOffset() - 1);
        String str = Integer.toString(numLines);
        if (getShowLineNumbers()) {
          // Compute width based on whether we are drawing line numbers
          width += GraphicsUtilities.getStringWidth(str, getFontMetrics(getFont())) + LINE_NO_SPACE;
        }
        // FIXME: add on width for all icons (breakpoint, current line,
        // current line in caller frame)
        Dimension d = new Dimension(width, numLines * getFontMetrics(getFont()).getHeight());
        setSize(d);
        setPreferredSize(d);
      } catch (BadLocationException e) {
        e.printStackTrace();
      }
    }
  }

  public SourceCodePanel() {
    maybeLoadIcons();

    // Build user interface
    setLayout(new BorderLayout());
    source = new JTextArea();
    source.setEditable(false);
    source.getCaret().setVisible(true);
    header = new RowHeader();
    header.setShowLineNumbers(true);
    JScrollPane scroller = new JScrollPane(source);
    JViewport rowView = new JViewport();
    rowView.setView(header);
    header.setViewport(rowView);
    rowView.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
    scroller.setRowHeader(rowView);
    add(scroller, BorderLayout.CENTER);
    // Reset font now that header and source are present
    setFont(getFont());

    source.addFocusListener(new FocusAdapter() {
        public void focusGained(FocusEvent e) {
          source.getCaret().setVisible(true);
        }
      });

    source.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          if (e.getKeyCode() == KeyEvent.VK_F9) {
            int lineNo = getCurrentLineNumber();
            // Only the debugger can figure out whether we are setting
            // or clearing a breakpoint, since it has the debug
            // information available and knows whether we're on a
            // valid line
            comm.toggleBreakpointAtLine(parent, lineNo);
          }
        }
      });

  }

  public void setFont(Font f) {
    super.setFont(f);
    if (source != null) {
      source.setFont(f);
    }
    if (header != null) {
      header.setFont(f);
    }
  }

  public boolean getShowLineNumbers() {
    return header.getShowLineNumbers();
  }

  public void setShowLineNumbers(boolean val) {
    header.setShowLineNumbers(val);
  }

  public boolean openFile(String filename) {
    try {
      this.filename = filename;
      File file = new File(filename);
      int len = (int) file.length();
      StringBuilder buf = new StringBuilder(len); // Approximation
      char[] tmp = new char[4096];
      FileReader in = new FileReader(file);
      int res = 0;
      do {
        res = in.read(tmp, 0, tmp.length);
        if (res >= 0) {
          buf.append(tmp, 0, res);
        }
      } while (res != -1);
      in.close();
      String text = buf.toString();
      source.setText(text);
      header.recomputeSize();
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  public String getSourceFileName() {
    return filename;
  }

  /** Line number is one-based */
  public int getCurrentLineNumber() {
    try {
      return 1 + source.getLineOfOffset(source.getCaretPosition());
    } catch (BadLocationException e) {
      return 0;
    }
  }

  /** Line number is one-based */
  public void showLineNumber(int lineNo) {
    try {
      int offset = source.getLineStartOffset(lineNo - 1);
      Rectangle2D rect2d = source.modelToView2D(offset);
      if (rect2d == null) {
        return;
      }
      Rectangle rect = new Rectangle((int) rect2d.getX(), (int) rect2d.getY(),
              (int) rect2d.getWidth(), (int) rect2d.getHeight());
      source.scrollRectToVisible(rect);
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
  }

  /** Line number is one-based */
  public void highlightLineNumber(int lineNo) {
    highlightedLine = lineNo - 1;
  }

  public void showBreakpointAtLine(int lineNo)  { breakpoints.add(lineNo - 1);    repaint(); }
  public boolean hasBreakpointAtLine(int lineNo){ return breakpoints.contains(lineNo - 1);   }
  public void clearBreakpointAtLine(int lineNo) { breakpoints.remove(lineNo - 1); repaint(); }
  public void clearBreakpoints()                { breakpoints.clear();                         repaint(); }

  public void setEditorCommands(EditorCommands comm, Editor parent) {
    this.comm = comm;
    this.parent = parent;
  }

  public void requestFocus() {
    source.requestFocus();
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void maybeLoadIcons() {
    if (topFrameCurLine == null) {
      topFrameCurLine   = loadIcon("resources/arrow.png");
      lowerFrameCurLine = loadIcon("resources/triangle.png");
      breakpoint        = loadIcon("resources/breakpoint.png");
    }
  }

  private Icon loadIcon(String which) {
    URL url = getClass().getResource(which);
    return new ImageIcon(url);
  }
}
