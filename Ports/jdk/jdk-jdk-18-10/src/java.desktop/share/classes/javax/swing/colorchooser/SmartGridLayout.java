/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package javax.swing.colorchooser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.io.Serializable;


/**
  * A better GridLayout class
  *
  * @author Steve Wilson
  */
@SuppressWarnings("serial") // Same-version serialization only
class SmartGridLayout implements LayoutManager, Serializable {

  int rows = 2;
  int columns = 2;
  int xGap = 2;
  int yGap = 2;
  int componentCount = 0;
  Component[][] layoutGrid;


  public SmartGridLayout(int numColumns, int numRows) {
    rows = numRows;
    columns = numColumns;
    layoutGrid = new Component[numColumns][numRows];

  }


  public void layoutContainer(Container c) {

    buildLayoutGrid(c);

    int[] rowHeights = new int[rows];
    int[] columnWidths = new int[columns];

    for (int row = 0; row < rows; row++) {
        rowHeights[row] = computeRowHeight(row);
    }

    for (int column = 0; column < columns; column++) {
        columnWidths[column] = computeColumnWidth(column);
    }


    Insets insets = c.getInsets();

    if (c.getComponentOrientation().isLeftToRight()) {
        int horizLoc = insets.left;
        for (int column = 0; column < columns; column++) {
          int vertLoc = insets.top;

          for (int row = 0; row < rows; row++) {
            Component current = layoutGrid[column][row];

            current.setBounds(horizLoc, vertLoc, columnWidths[column], rowHeights[row]);
            //  System.out.println(current.getBounds());
            vertLoc += (rowHeights[row] + yGap);
          }
          horizLoc += (columnWidths[column] + xGap );
        }
    } else {
        int horizLoc = c.getWidth() - insets.right;
        for (int column = 0; column < columns; column++) {
          int vertLoc = insets.top;
          horizLoc -= columnWidths[column];

          for (int row = 0; row < rows; row++) {
            Component current = layoutGrid[column][row];

            current.setBounds(horizLoc, vertLoc, columnWidths[column], rowHeights[row]);
            //  System.out.println(current.getBounds());
            vertLoc += (rowHeights[row] + yGap);
          }
          horizLoc -= xGap;
        }
    }



  }

  public Dimension minimumLayoutSize(Container c) {

    buildLayoutGrid(c);
    Insets insets = c.getInsets();



    int height = 0;
    int width = 0;

    for (int row = 0; row < rows; row++) {
        height += computeRowHeight(row);
    }

    for (int column = 0; column < columns; column++) {
        width += computeColumnWidth(column);
    }

    height += (yGap * (rows - 1)) + insets.top + insets.bottom;
    width += (xGap * (columns - 1)) + insets.right + insets.left;

    return new Dimension(width, height);


  }

  public Dimension preferredLayoutSize(Container c) {
      return minimumLayoutSize(c);
  }


  public void addLayoutComponent(String s, Component c) {}

  public void removeLayoutComponent(Component c) {}


  private void buildLayoutGrid(Container c) {

      Component[] children = c.getComponents();

      for (int componentCount = 0; componentCount < children.length; componentCount++) {
        //      System.out.println("Children: " +componentCount);
        int row = 0;
        int column = 0;

        if (componentCount != 0) {
          column = componentCount % columns;
          row = (componentCount - column) / columns;
        }

        //      System.out.println("inserting into: "+ column +  " " + row);

        layoutGrid[column][row] = children[componentCount];
      }
  }

  private int computeColumnWidth(int columnNum) {
    int maxWidth = 1;
    for (int row = 0; row < rows; row++) {
      int width = layoutGrid[columnNum][row].getPreferredSize().width;
      if (width > maxWidth) {
        maxWidth = width;
      }
    }
    return maxWidth;
  }

  private int computeRowHeight(int rowNum) {
    int maxHeight = 1;
    for (int column = 0; column < columns; column++) {
      int height = layoutGrid[column][rowNum].getPreferredSize().height;
      if (height > maxHeight) {
        maxHeight = height;
      }
    }
    return maxHeight;
  }

}
