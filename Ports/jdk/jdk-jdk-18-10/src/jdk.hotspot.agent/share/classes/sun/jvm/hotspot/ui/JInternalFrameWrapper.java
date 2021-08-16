/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

public class JInternalFrameWrapper implements FrameWrapper {
  private JInternalFrame frame;
  private boolean        hasWindowListener;
  private ActionListener closingActionListener;
  private ActionListener activatedActionListener;

  public JInternalFrameWrapper(JInternalFrame frame) {
    this.frame = frame;
  }

  public Component  getComponent()              { return frame;                  }
  public Container  getContentPane()            { return frame.getContentPane(); }
  public void       setVisible(boolean visible) { frame.setVisible(visible);     }
  public void       setSize(int x, int y)       { frame.setSize(x, y);           }
  public void       pack()                      { frame.pack();                  }
  public void       show()                      { frame.show();                  }
  public void       dispose()                   { frame.dispose();               }
  public void       setBackground(Color color)  { frame.setBackground(color);    }
  public void       setResizable(boolean resizable) { frame.setResizable(resizable); }
  public void       setClosable(boolean closable) { frame.setClosable(closable); }

  public void setClosingActionListener(ActionListener l) {
    closingActionListener = l;
    maybeInstallWindowListener();
  }

  public void setActivatedActionListener(ActionListener l) {
    activatedActionListener = l;
    maybeInstallWindowListener();
  }

  public void toFront() {
    frame.toFront();
    try {
      frame.setSelected(true);
    } catch (java.beans.PropertyVetoException e) {
    }
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void maybeInstallWindowListener() {
    if (!hasWindowListener) {
      frame.addInternalFrameListener(new InternalFrameAdapter() {
          public void internalFrameClosing(InternalFrameEvent e) {
            if (closingActionListener != null) {
              closingActionListener.actionPerformed(null);
            }
          }

          public void internalFrameActivated(InternalFrameEvent e) {
            if (activatedActionListener != null) {
              activatedActionListener.actionPerformed(null);
            }
          }
        });
      hasWindowListener = true;
    }
  }
}
