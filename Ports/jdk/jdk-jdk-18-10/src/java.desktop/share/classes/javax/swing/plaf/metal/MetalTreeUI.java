/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf.metal;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import javax.swing.plaf.*;
import javax.swing.tree.*;

import javax.swing.plaf.basic.*;

/**
 * The metal look and feel implementation of <code>TreeUI</code>.
 * <p>
 * <code>MetalTreeUI</code> allows for configuring how to
 * visually render the spacing and delineation between nodes. The following
 * hints are supported:
 *
 * <table class="striped">
 * <caption>Descriptions of supported hints: Angled, Horizontal, and None
 * </caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Hint
 *     <th scope="col">Description
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row">Angled
 *     <td>A line is drawn connecting the child to the parent. For handling of
 *     the root node refer to {@link JTree#setRootVisible} and
 *     {@link JTree#setShowsRootHandles}.
 *   <tr>
 *     <th scope="row">Horizontal
 *     <td>A horizontal line is drawn dividing the children of the root node.
 *   <tr>
 *     <th scope="row">None
 *     <td>Do not draw any visual indication between nodes.
 * </tbody>
 * </table>
 * <p>
 * As it is typically impractical to obtain the <code>TreeUI</code> from
 * the <code>JTree</code> and cast to an instance of <code>MetalTreeUI</code>
 * you enable this property via the client property
 * <code>JTree.lineStyle</code>. For example, to switch to
 * <code>Horizontal</code> style you would do:
 * <code>tree.putClientProperty("JTree.lineStyle", "Horizontal");</code>
 * <p>
 * The default is <code>Angled</code>.
 *
 * @author Tom Santos
 * @author Steve Wilson (value add stuff)
 */
public class MetalTreeUI extends BasicTreeUI {

    private static Color lineColor;

    private static final String LINE_STYLE = "JTree.lineStyle";

    private static final String LEG_LINE_STYLE_STRING = "Angled";
    private static final String HORIZ_STYLE_STRING = "Horizontal";
    private static final String NO_STYLE_STRING = "None";

    private static final int LEG_LINE_STYLE = 2;
    private static final int HORIZ_LINE_STYLE = 1;
    private static final int NO_LINE_STYLE = 0;

    private int lineStyle = LEG_LINE_STYLE;
    private PropertyChangeListener lineStyleListener = new LineListener();

    /**
     * Constructs the {@code MetalTreeUI}.
     *
     * @param x a component
     * @return the instance of the {@code MetalTreeUI}
     */
    public static ComponentUI createUI(JComponent x) {
        return new MetalTreeUI();
    }

    /**
     * Constructs the {@code MetalTreeUI}.
     */
    public MetalTreeUI() {
        super();
    }

    protected int getHorizontalLegBuffer() {
        return 3;
    }

    public void installUI( JComponent c ) {
        super.installUI( c );
        lineColor = UIManager.getColor( "Tree.line" );

        Object lineStyleFlag = c.getClientProperty( LINE_STYLE );
        decodeLineStyle(lineStyleFlag);
        c.addPropertyChangeListener(lineStyleListener);

    }

    public void uninstallUI( JComponent c) {
         c.removePropertyChangeListener(lineStyleListener);
         super.uninstallUI(c);
    }

    /**
     * Converts between the string passed into the client property
     * and the internal representation (currently and int)
     *
     * @param lineStyleFlag a flag
     */
    protected void decodeLineStyle(Object lineStyleFlag) {
        if ( lineStyleFlag == null ||
                    lineStyleFlag.equals(LEG_LINE_STYLE_STRING)) {
            lineStyle = LEG_LINE_STYLE; // default case
        } else {
            if ( lineStyleFlag.equals(NO_STYLE_STRING) ) {
                lineStyle = NO_LINE_STYLE;
            } else if ( lineStyleFlag.equals(HORIZ_STYLE_STRING) ) {
                lineStyle = HORIZ_LINE_STYLE;
            }
        }
    }

    /**
     * Returns {@code true} if a point with X coordinate {@code mouseX}
     * and Y coordinate {@code mouseY} is in expanded control.
     *
     * @param row a row
     * @param rowLevel a row level
     * @param mouseX X coordinate
     * @param mouseY Y coordinate
     * @return {@code true} if a point with X coordinate {@code mouseX}
     *         and Y coordinate {@code mouseY} is in expanded control.
     */
    protected boolean isLocationInExpandControl(int row, int rowLevel,
                                                int mouseX, int mouseY) {
        if(tree != null && !isLeaf(row)) {
            int                     boxWidth;

            if(getExpandedIcon() != null)
                boxWidth = getExpandedIcon().getIconWidth() + 6;
            else
                boxWidth = 8;

            Insets i = tree.getInsets();
            int    boxLeftX = (i != null) ? i.left : 0;


            boxLeftX += (((rowLevel + depthOffset - 1) * totalChildIndent) +
                        getLeftChildIndent()) - boxWidth/2;

            int boxRightX = boxLeftX + boxWidth;

            return mouseX >= boxLeftX && mouseX <= boxRightX;
        }
        return false;
    }

    public void paint(Graphics g, JComponent c) {
        super.paint( g, c );


        // Paint the lines
        if (lineStyle == HORIZ_LINE_STYLE && !largeModel) {
            paintHorizontalSeparators(g,c);
        }
    }

    /**
     * Paints the horizontal separators.
     *
     * @param g an instance of {@code Graphics}
     * @param c a component
     */
    protected void paintHorizontalSeparators(Graphics g, JComponent c) {
        g.setColor( lineColor );

        Rectangle clipBounds = g.getClipBounds();

        int beginRow = getRowForPath(tree, getClosestPathForLocation
                                     (tree, 0, clipBounds.y));
        int endRow = getRowForPath(tree, getClosestPathForLocation
                             (tree, 0, clipBounds.y + clipBounds.height - 1));

        if ( beginRow <= -1 || endRow <= -1 ) {
            return;
        }

        for ( int i = beginRow; i <= endRow; ++i ) {
            TreePath        path = getPathForRow(tree, i);

            if(path != null && path.getPathCount() == 2) {
                Rectangle       rowBounds = getPathBounds(tree,getPathForRow
                                                          (tree, i));

                // Draw a line at the top
                if(rowBounds != null)
                    g.drawLine(clipBounds.x, rowBounds.y,
                               clipBounds.x + clipBounds.width, rowBounds.y);
            }
        }

    }

    protected void paintVerticalPartOfLeg(Graphics g, Rectangle clipBounds,
                                          Insets insets, TreePath path) {
        if (lineStyle == LEG_LINE_STYLE) {
            super.paintVerticalPartOfLeg(g, clipBounds, insets, path);
        }
    }

    protected void paintHorizontalPartOfLeg(Graphics g, Rectangle clipBounds,
                                            Insets insets, Rectangle bounds,
                                            TreePath path, int row,
                                            boolean isExpanded,
                                            boolean hasBeenExpanded, boolean
                                            isLeaf) {
        if (lineStyle == LEG_LINE_STYLE) {
            super.paintHorizontalPartOfLeg(g, clipBounds, insets, bounds,
                                           path, row, isExpanded,
                                           hasBeenExpanded, isLeaf);
        }
    }

    /** This class listens for changes in line style */
    class LineListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            String name = e.getPropertyName();
            if ( name.equals( LINE_STYLE ) ) {
                decodeLineStyle(e.getNewValue());
            }
        }
    } // end class PaletteListener

}
