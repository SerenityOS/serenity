/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.*;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.utilities.*;

/** This can be extended, along with AnnotatedMemoryPanel, to be an
    arbitrarily complex mechanism, supporting user interaction,
    etc. */

public class Annotation {
  private Interval interval;
  private java.util.List<String> strings;
  private java.util.List<Integer> heights;
  private Color baseColor;
  private int width;
  private int height;
  private int x;
  private int y;

  /** The Annotation can handle the sense of lowAddress and
      highAddress being swapped. */
  public Annotation(Address lowAddress,
                    Address highAddress,
                    String s) {
    strings = new ArrayList<>();
    heights = new ArrayList<>();
    for (StringTokenizer tok = new StringTokenizer(s, "\n"); tok.hasMoreTokens(); ) {
      strings.add(tok.nextToken());
    }
    if (AddressOps.lessThan(highAddress, lowAddress)) {
      Address temp = lowAddress;
      lowAddress = highAddress;
      highAddress = temp;
    }
    interval = new Interval(lowAddress, highAddress);
  }

  public Interval getInterval() {
    return interval;
  }

  public Address getLowAddress() {
    return (Address) getInterval().getLowEndpoint();
  }

  public Address getHighAddress() {
    return (Address) getInterval().getHighEndpoint();
  }

  /** Draw the annotation at its current (x, y) position with its
      current color */
  public void draw(Graphics g) {
    g.setColor(baseColor);
    int tmpY = y;
    for (int i = 0; i < strings.size(); i++) {
      String s = (String) strings.get(i);
      Integer h = (Integer) heights.get(i);
      g.drawString(s, x, tmpY);
      tmpY += h.intValue();
    }
  }

  /** Sets the base color of this annotation. The annotation may
      render sub-portions in a different color if desired. */
  public void setColor(Color c) {
    this.baseColor = c;
  }

  /** Returns the base color of this annotation. */
  public Color getColor() {
    return baseColor;
  }

  /** Computes width and height for this Annotation. Retrieve the
      computed information using getWidth() and getHeight(). Separated
      because the width and height only need to be recomputed if the
      font changes. */
  public void computeWidthAndHeight(Graphics g) {
    width = 0;
    height = 0;
    heights.clear();
    for (Iterator iter = strings.iterator(); iter.hasNext(); ) {
      String s = (String) iter.next();
      Rectangle2D bounds = GraphicsUtilities.getStringBounds(s, g);
      width  =  Math.max(width, (int) bounds.getWidth());
      height += (int) bounds.getHeight();
      heights.add((int) bounds.getHeight());
    }
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  /** Set the x and y position of this annotation */
  public void setXAndY(int x, int y) {
    this.x = x; this.y = y;
  }

  public void setX(int x) {
    this.x = x;
  }

  public int getX() {
    return x;
  }

  public void setY(int y) {
    this.y = y;
  }

  public int getY() {
    return y;
  }

  public Rectangle getBounds() {
    return new Rectangle(x, y, width, height);
  }
  public String toString() {
    String result = "Annotation: lowAddr: " + getLowAddress() + " highAddr: " + getHighAddress() + " strings: "  + strings.size();
    for (int i = 0; i < strings.size(); i++) {
      result += "\n" + (String) strings.get(i);
    }
    return result;
  }
}
