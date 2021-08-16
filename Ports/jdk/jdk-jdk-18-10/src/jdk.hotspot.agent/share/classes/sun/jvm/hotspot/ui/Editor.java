/*
 * Copyright (c) 2001, 2002, Oracle and/or its affiliates. All rights reserved.
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

/** High-level interface describing what the debugger requires from an
    editor component in order to display source code. The
    EditorFactory is used to instantiate Editor objects. */

public interface Editor {
  /** Get name of source file being displayed */
  public String getSourceFileName();

  /** Get (one-based) line number on which cursor is currently placed */
  public int getCurrentLineNumber();

  /** Make a particular line number visible on the screen. NOTE: line
      numbers are numbered starting from 1. */
  public void showLineNumber(int lineNo);

  /** Highlight a particular line number. NOTE: line numbers are
      numbered starting from 1. */
  public void highlightLineNumber(int lineNo);

  /** Show a breakpoint indicator on the current (one-based) line. */
  public void showBreakpointAtLine(int lineNo);

  /** Indicates whether a breakpoint indicator is visible on the
      current (one-based) line. */
  public boolean hasBreakpointAtLine(int lineNo);

  /** Clear a breakpoint indicator on the current (one-based) line. */
  public void clearBreakpointAtLine(int lineNo);

  /** Clear all breakpoint indicators. */
  public void clearBreakpoints();

  /** Set optional object of user data */
  public void setUserData(Object o);

  /** Get optional object of user data */
  public Object getUserData();

  /** Bring the given Editor to the front of all other Editors. This
      must also make this Editor the return value from
      EditorFactory.getCurrentEditor(). */
  public void toFront();
}
