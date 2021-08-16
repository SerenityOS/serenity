/*
 * Copyright (c) 1995, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt;

import java.io.Serial;

/**
 * The {@code GridLayout} class is a layout manager that
 * lays out a container's components in a rectangular grid.
 * The container is divided into equal-sized rectangles,
 * and one component is placed in each rectangle.
 * For example, the following is an applet that lays out six buttons
 * into three rows and two columns:
 *
 * <hr><blockquote>
 * <pre>
 * import java.awt.*;
 * import java.applet.Applet;
 * public class ButtonGrid extends Applet {
 *     public void init() {
 *         setLayout(new GridLayout(3,2));
 *         add(new Button("1"));
 *         add(new Button("2"));
 *         add(new Button("3"));
 *         add(new Button("4"));
 *         add(new Button("5"));
 *         add(new Button("6"));
 *     }
 * }
 * </pre></blockquote><hr>
 * <p>
 * If the container's {@code ComponentOrientation} property is horizontal
 * and left-to-right, the above example produces the output shown in Figure 1.
 * If the container's {@code ComponentOrientation} property is horizontal
 * and right-to-left, the example produces the output shown in Figure 2.
 *
 * <div style="margin:0 auto;width:600px;text-align:center;font-weight:bold">
 *   <div style="float:left">
 *     <p><img SRC="doc-files/GridLayout-1.gif"
 *        alt="Shows 6 buttons in rows of 2. Row 1 shows buttons 1 then 2.
 *        Row 2 shows buttons 3 then 4. Row 3 shows buttons 5 then 6.">
 *     <p>Figure 1: Horizontal, Left-to-Right
 *   </div>
 *   <div style="float:right">
 *     <p><img SRC="doc-files/GridLayout-2.gif"
 *        alt="Shows 6 buttons in rows of 2. Row 1 shows buttons 2 then 1.
 *        Row 2 shows buttons 4 then 3. Row 3 shows buttons 6 then 5.">
 *     <p>Figure 2: Horizontal, Right-to-Left
 *   </div>
 *   <br style="clear:both;">
 * </div>
 * <p>
 * When both the number of rows and the number of columns have
 * been set to non-zero values, either by a constructor or
 * by the {@code setRows} and {@code setColumns} methods, the number of
 * columns specified is ignored.  Instead, the number of
 * columns is determined from the specified number of rows
 * and the total number of components in the layout. So, for
 * example, if three rows and two columns have been specified
 * and nine components are added to the layout, they will
 * be displayed as three rows of three columns.  Specifying
 * the number of columns affects the layout only when the
 * number of rows is set to zero.
 *
 * @author  Arthur van Hoff
 * @since   1.0
 */
public class GridLayout implements LayoutManager, java.io.Serializable {

    /**
     * Use serialVersionUID from JDK 1.6 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -7411804673224730901L;

    /**
     * This is the horizontal gap (in pixels) which specifies the space
     * between columns.  They can be changed at any time.
     * This should be a non-negative integer.
     *
     * @serial
     * @see #getHgap()
     * @see #setHgap(int)
     */
    int hgap;
    /**
     * This is the vertical gap (in pixels) which specifies the space
     * between rows.  They can be changed at any time.
     * This should be a non negative integer.
     *
     * @serial
     * @see #getVgap()
     * @see #setVgap(int)
     */
    int vgap;
    /**
     * This is the number of rows specified for the grid.  The number
     * of rows can be changed at any time.
     * This should be a non negative integer, where '0' means
     * 'any number' meaning that the number of Rows in that
     * dimension depends on the other dimension.
     *
     * @serial
     * @see #getRows()
     * @see #setRows(int)
     */
    int rows;
    /**
     * This is the number of columns specified for the grid.  The number
     * of columns can be changed at any time.
     * This should be a non negative integer, where '0' means
     * 'any number' meaning that the number of Columns in that
     * dimension depends on the other dimension.
     *
     * @serial
     * @see #getColumns()
     * @see #setColumns(int)
     */
    int cols;

    /**
     * Creates a grid layout with a default of one column per component,
     * in a single row.
     * @since 1.1
     */
    public GridLayout() {
        this(1, 0, 0, 0);
    }

    /**
     * Creates a grid layout with the specified number of rows and
     * columns. All components in the layout are given equal size.
     * <p>
     * One, but not both, of {@code rows} and {@code cols} can
     * be zero, which means that any number of objects can be placed in a
     * row or in a column.
     * @param     rows   the rows, with the value zero meaning
     *                   any number of rows.
     * @param     cols   the columns, with the value zero meaning
     *                   any number of columns.
     */
    public GridLayout(int rows, int cols) {
        this(rows, cols, 0, 0);
    }

    /**
     * Creates a grid layout with the specified number of rows and
     * columns. All components in the layout are given equal size.
     * <p>
     * In addition, the horizontal and vertical gaps are set to the
     * specified values. Horizontal gaps are placed between each
     * of the columns. Vertical gaps are placed between each of
     * the rows.
     * <p>
     * One, but not both, of {@code rows} and {@code cols} can
     * be zero, which means that any number of objects can be placed in a
     * row or in a column.
     * <p>
     * All {@code GridLayout} constructors defer to this one.
     * @param     rows   the rows, with the value zero meaning
     *                   any number of rows
     * @param     cols   the columns, with the value zero meaning
     *                   any number of columns
     * @param     hgap   the horizontal gap
     * @param     vgap   the vertical gap
     * @exception   IllegalArgumentException  if the value of both
     *                  {@code rows} and {@code cols} is
     *                  set to zero
     */
    public GridLayout(int rows, int cols, int hgap, int vgap) {
        if ((rows == 0) && (cols == 0)) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.rows = rows;
        this.cols = cols;
        this.hgap = hgap;
        this.vgap = vgap;
    }

    /**
     * Gets the number of rows in this layout.
     * @return    the number of rows in this layout
     * @since     1.1
     */
    public int getRows() {
        return rows;
    }

    /**
     * Sets the number of rows in this layout to the specified value.
     * @param        rows   the number of rows in this layout
     * @exception    IllegalArgumentException  if the value of both
     *               {@code rows} and {@code cols} is set to zero
     * @since        1.1
     */
    public void setRows(int rows) {
        if ((rows == 0) && (this.cols == 0)) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.rows = rows;
    }

    /**
     * Gets the number of columns in this layout.
     * @return     the number of columns in this layout
     * @since      1.1
     */
    public int getColumns() {
        return cols;
    }

    /**
     * Sets the number of columns in this layout to the specified value.
     * Setting the number of columns has no affect on the layout
     * if the number of rows specified by a constructor or by
     * the {@code setRows} method is non-zero. In that case, the number
     * of columns displayed in the layout is determined by the total
     * number of components and the number of rows specified.
     * @param        cols   the number of columns in this layout
     * @exception    IllegalArgumentException  if the value of both
     *               {@code rows} and {@code cols} is set to zero
     * @since        1.1
     */
    public void setColumns(int cols) {
        if ((cols == 0) && (this.rows == 0)) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.cols = cols;
    }

    /**
     * Gets the horizontal gap between components.
     * @return       the horizontal gap between components
     * @since        1.1
     */
    public int getHgap() {
        return hgap;
    }

    /**
     * Sets the horizontal gap between components to the specified value.
     * @param        hgap   the horizontal gap between components
     * @since        1.1
     */
    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    /**
     * Gets the vertical gap between components.
     * @return       the vertical gap between components
     * @since        1.1
     */
    public int getVgap() {
        return vgap;
    }

    /**
     * Sets the vertical gap between components to the specified value.
     * @param         vgap  the vertical gap between components
     * @since        1.1
     */
    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    /**
     * Adds the specified component with the specified name to the layout.
     * @param name the name of the component
     * @param comp the component to be added
     */
    public void addLayoutComponent(String name, Component comp) {
    }

    /**
     * Removes the specified component from the layout.
     * @param comp the component to be removed
     */
    public void removeLayoutComponent(Component comp) {
    }

    /**
     * Determines the preferred size of the container argument using
     * this grid layout.
     * <p>
     * The preferred width of a grid layout is the largest preferred
     * width of all of the components in the container times the number of
     * columns, plus the horizontal padding times the number of columns
     * minus one, plus the left and right insets of the target container.
     * <p>
     * The preferred height of a grid layout is the largest preferred
     * height of all of the components in the container times the number of
     * rows, plus the vertical padding times the number of rows minus one,
     * plus the top and bottom insets of the target container.
     *
     * @param     parent   the container in which to do the layout
     * @return    the preferred dimensions to lay out the
     *                      subcomponents of the specified container
     * @see       java.awt.GridLayout#minimumLayoutSize
     * @see       java.awt.Container#getPreferredSize()
     */
    public Dimension preferredLayoutSize(Container parent) {
      synchronized (parent.getTreeLock()) {
        Insets insets = parent.getInsets();
        int ncomponents = parent.getComponentCount();
        int nrows = rows;
        int ncols = cols;

        if (nrows > 0) {
            ncols = (ncomponents + nrows - 1) / nrows;
        } else {
            nrows = (ncomponents + ncols - 1) / ncols;
        }
        int w = 0;
        int h = 0;
        for (int i = 0 ; i < ncomponents ; i++) {
            Component comp = parent.getComponent(i);
            Dimension d = comp.getPreferredSize();
            if (w < d.width) {
                w = d.width;
            }
            if (h < d.height) {
                h = d.height;
            }
        }
        return new Dimension(insets.left + insets.right + ncols*w + (ncols-1)*hgap,
                             insets.top + insets.bottom + nrows*h + (nrows-1)*vgap);
      }
    }

    /**
     * Determines the minimum size of the container argument using this
     * grid layout.
     * <p>
     * The minimum width of a grid layout is the largest minimum width
     * of all of the components in the container times the number of columns,
     * plus the horizontal padding times the number of columns minus one,
     * plus the left and right insets of the target container.
     * <p>
     * The minimum height of a grid layout is the largest minimum height
     * of all of the components in the container times the number of rows,
     * plus the vertical padding times the number of rows minus one, plus
     * the top and bottom insets of the target container.
     *
     * @param       parent   the container in which to do the layout
     * @return      the minimum dimensions needed to lay out the
     *                      subcomponents of the specified container
     * @see         java.awt.GridLayout#preferredLayoutSize
     * @see         java.awt.Container#doLayout
     */
    public Dimension minimumLayoutSize(Container parent) {
      synchronized (parent.getTreeLock()) {
        Insets insets = parent.getInsets();
        int ncomponents = parent.getComponentCount();
        int nrows = rows;
        int ncols = cols;

        if (nrows > 0) {
            ncols = (ncomponents + nrows - 1) / nrows;
        } else {
            nrows = (ncomponents + ncols - 1) / ncols;
        }
        int w = 0;
        int h = 0;
        for (int i = 0 ; i < ncomponents ; i++) {
            Component comp = parent.getComponent(i);
            Dimension d = comp.getMinimumSize();
            if (w < d.width) {
                w = d.width;
            }
            if (h < d.height) {
                h = d.height;
            }
        }
        return new Dimension(insets.left + insets.right + ncols*w + (ncols-1)*hgap,
                             insets.top + insets.bottom + nrows*h + (nrows-1)*vgap);
      }
    }

    /**
     * Lays out the specified container using this layout.
     * <p>
     * This method reshapes the components in the specified target
     * container in order to satisfy the constraints of the
     * {@code GridLayout} object.
     * <p>
     * The grid layout manager determines the size of individual
     * components by dividing the free space in the container into
     * equal-sized portions according to the number of rows and columns
     * in the layout. The container's free space equals the container's
     * size minus any insets and any specified horizontal or vertical
     * gap. All components in a grid layout are given the same size.
     *
     * @param      parent   the container in which to do the layout
     * @see        java.awt.Container
     * @see        java.awt.Container#doLayout
     */
    public void layoutContainer(Container parent) {
      synchronized (parent.getTreeLock()) {
        Insets insets = parent.getInsets();
        int ncomponents = parent.getComponentCount();
        int nrows = rows;
        int ncols = cols;
        boolean ltr = parent.getComponentOrientation().isLeftToRight();

        if (ncomponents == 0) {
            return;
        }
        if (nrows > 0) {
            ncols = (ncomponents + nrows - 1) / nrows;
        } else {
            nrows = (ncomponents + ncols - 1) / ncols;
        }
        // 4370316. To position components in the center we should:
        // 1. get an amount of extra space within Container
        // 2. incorporate half of that value to the left/top position
        // Note that we use trancating division for widthOnComponent
        // The reminder goes to extraWidthAvailable
        int totalGapsWidth = (ncols - 1) * hgap;
        int widthWOInsets = parent.width - (insets.left + insets.right);
        int widthOnComponent = (widthWOInsets - totalGapsWidth) / ncols;
        int extraWidthAvailable = (widthWOInsets - (widthOnComponent * ncols + totalGapsWidth)) / 2;

        int totalGapsHeight = (nrows - 1) * vgap;
        int heightWOInsets = parent.height - (insets.top + insets.bottom);
        int heightOnComponent = (heightWOInsets - totalGapsHeight) / nrows;
        int extraHeightAvailable = (heightWOInsets - (heightOnComponent * nrows + totalGapsHeight)) / 2;
        if (ltr) {
            for (int c = 0, x = insets.left + extraWidthAvailable; c < ncols ; c++, x += widthOnComponent + hgap) {
                for (int r = 0, y = insets.top + extraHeightAvailable; r < nrows ; r++, y += heightOnComponent + vgap) {
                    int i = r * ncols + c;
                    if (i < ncomponents) {
                        parent.getComponent(i).setBounds(x, y, widthOnComponent, heightOnComponent);
                    }
                }
            }
        } else {
            for (int c = 0, x = (parent.width - insets.right - widthOnComponent) - extraWidthAvailable; c < ncols ; c++, x -= widthOnComponent + hgap) {
                for (int r = 0, y = insets.top + extraHeightAvailable; r < nrows ; r++, y += heightOnComponent + vgap) {
                    int i = r * ncols + c;
                    if (i < ncomponents) {
                        parent.getComponent(i).setBounds(x, y, widthOnComponent, heightOnComponent);
                    }
                }
            }
        }
      }
    }

    /**
     * Returns the string representation of this grid layout's values.
     * @return     a string representation of this grid layout
     */
    public String toString() {
        return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap +
                                       ",rows=" + rows + ",cols=" + cols + "]";
    }
}
