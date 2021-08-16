/*
 * Copyright (c) 2001, 2010, Oracle and/or its affiliates. All rights reserved.
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

/** Provides uniform interface for dealing with JInternalFrames and
    JFrames. */

public interface FrameWrapper {
  /** The JInternalFrame or JFrame this wraps */
  public Component  getComponent();

  public Container  getContentPane();
  public void       setVisible(boolean visible);
  public void       setSize(int x, int y);
  public void       pack();
  public void       dispose();
  public void       setBackground(Color color);
  public void       setResizable(boolean resizable);

  /** Largely for use with JInternalFrames but also affects, for
      example, the default close operation for JFrames */
  public void       setClosable(boolean closable);

  /** Set an ActionListener to be invoked when the underlying window
      is closing ("windowClosing" event of a WindowListener). Note:
      the ActionEvent passed to this listener may be null. */
  public void       setClosingActionListener(ActionListener l);

  /** Set an ActionListener to be invoked when the underlying window
      is activated ("windowActivated" event of a
      WindowListener). Note: the ActionEvent passed to this listener
      may be null. */
  public void       setActivatedActionListener(ActionListener l);

  /** Move this frame to the front. Should change focus to the frame
      if possible. */
  public void       toFront();
}
