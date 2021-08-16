/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 */

/*
 * @test
 * @key headful
 * @bug 8195738
 * @summary scroll position in ScrollPane is reset after calling validate()
 * @run main ScrollPaneValidateTest
 */

import java.awt.ScrollPane;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Button;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Robot;
import java.awt.AWTException;

public class ScrollPaneValidateTest extends Frame {
  ScrollPane pane;

  public ScrollPaneValidateTest() {
    setBounds(300, 300, 300, 300);
    pane = new ScrollPane(ScrollPane.SCROLLBARS_NEVER);
    add(pane, BorderLayout.NORTH);
    pane.add(new InnerPanel());
  }

  public static void main(String[] args) throws AWTException {
    Robot robot = new Robot();
    final ScrollPaneValidateTest obj = new ScrollPaneValidateTest();
    obj.setVisible(true);

    // set to some scroll position
    obj.pane.setScrollPosition(600, 200);

    // get the newly set position
    Point scrollPosition = obj.pane.getScrollPosition();

    // call validate multiple times
    obj.pane.validate();
    robot.delay(1000);
    obj.pane.validate();
    robot.delay(1000);

    // compare position after calling the validate function
    if(!scrollPosition.equals(obj.pane.getScrollPosition())) {
      obj.dispose();
      throw new RuntimeException("Scrolling position is changed in ScrollPane");
    }

    obj.dispose();
    return;
  }

  class InnerPanel extends Panel {
    public InnerPanel() {
      this.setLayout(new GridLayout(2, 4));
      for (int i = 1; i <= 8; i++) {
        this.add(new Button("Button" + i));
      }
    }

    public Dimension getPreferredSize() {
      return new Dimension(980, 200);
    }
  }
}
