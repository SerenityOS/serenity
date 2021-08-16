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
import java.awt.font.*;
import java.awt.geom.*;
import java.util.Random;
import javax.swing.*;
import javax.swing.border.*;

/** Useful utilities for drawing graphics */

public class GraphicsUtilities {
  private static final int FONT_SIZE = 12;

  public static Font getMonospacedFont() {
    return new Font(Font.MONOSPACED, Font.PLAIN, FONT_SIZE);
  }

  /** Compute the width and height of given string given the current
      font context in the Graphics object */
  public static Rectangle2D getStringBounds(String s, Graphics g) {
    FontMetrics fm = g.getFontMetrics();
    return fm.getStringBounds(s, 0, s.length(), g);
  }

  /** Compute just the width of the given string with the given
      FontMetrics. This is less accurate then getStringBounds(),
      above, since the graphics context is not taken into account. */
  public static int getStringWidth(String s, FontMetrics fm) {
    return fm.stringWidth(s);
  }

  public static void reshapeToAspectRatio(Component component,
                                          float aspectRatio,
                                          float fillRatio,
                                          Dimension containerDimension) {
    int x = containerDimension.width;
    int y = containerDimension.height;

    int desiredX;
    int desiredY;

    if (((float) x / (float) y) > aspectRatio) {
      desiredY = (int) (fillRatio * y);
      desiredX = (int) (desiredY * aspectRatio);
    } else {
      desiredX = (int) (fillRatio * x);
      desiredY = (int) (desiredX / aspectRatio);
    }
    component.setSize(desiredX, desiredY);
  }

  public static void constrainToSize(Component component, Dimension containerDimension) {
    Dimension d = component.getSize();
    int x = d.width;
    int y = d.height;
    boolean changed = false;

    if (x > containerDimension.width) {
      x = containerDimension.width;
      changed = true;
    }
    if (y > containerDimension.height) {
      y = containerDimension.height;
      changed = true;
    }

    if (changed) {
      component.setSize(x, y);
    }
  }

  public static void centerInContainer(Component c) {
    centerInContainer(c, c.getParent().getSize());
  }

  public static void centerInContainer(Component component,
                                       Dimension containerDimension) {
    Dimension sz = component.getSize();
    int x = ((containerDimension.width - sz.width) / 2);
    int y = ((containerDimension.height - sz.height) / 2);
    component.setLocation(x, y);
  }

  public static void moveToInContainer(Component component,
                                       float relativeX,
                                       float relativeY,
                                       int minX,
                                       int minY) {
    Dimension d = component.getParent().getSize();
    // Move the center of this component to the relative position in
    // the parent. Don't clip this component, however.
    Dimension sz = component.getSize();
    int xPos = Math.min(d.width - sz.width,
                        (int) ((d.width * relativeX) - (sz.width / 2)));
    int yPos = Math.min(d.height - sz.height,
                        (int) ((d.height * relativeY) - (sz.height / 2)));
    xPos = Math.max(xPos, minX);
    yPos = Math.max(yPos, minY);
    component.setLocation(xPos, yPos);
  }

  static Random random = new Random();

  public static void randomLocation(Component c) {
    randomLocation(c, c.getParent().getSize());
  }

  public static void randomLocation(Component component,
                                    Dimension containerDimension) {
    Dimension sz = component.getSize();
    int x = (int)((containerDimension.width - sz.width) * random.nextFloat());
    int y = (int)((containerDimension.height - sz.height) * random.nextFloat());
    component.setLocation(x, y);
  }


  public static Border newBorder(int size) {
    return BorderFactory.createEmptyBorder(size, size, size, size);
  }
}
