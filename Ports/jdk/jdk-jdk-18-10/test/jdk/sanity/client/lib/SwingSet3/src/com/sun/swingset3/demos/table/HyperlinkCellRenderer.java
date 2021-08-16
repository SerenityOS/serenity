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

package com.sun.swingset3.demos.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

import com.sun.swingset3.demos.JHyperlink;

/**
 * Table renderer which renders cell value as hyperlink with optional rollover underline.
 *
 * @author aim
 */
public class HyperlinkCellRenderer extends JHyperlink implements TableCellRenderer {
    private JTable table;
    private final ArrayList<Integer> columnModelIndeces = new ArrayList<Integer>();

    private Color rowColors[];
    private Color foreground;
    private Color visitedForeground;
    private Border focusBorder;
    private Border noFocusBorder;

    private boolean underlineOnRollover = true;

    private transient int hitColumnIndex = -1;
    private transient int hitRowIndex = -1;

    private HashMap<Object, int[]> visitedCache;

    public HyperlinkCellRenderer(Action action, boolean underlineOnRollover) {
        setAction(action);
        setHorizontalAlignment(JHyperlink.LEFT);
        rowColors = new Color[1];
        rowColors[0] = UIManager.getColor("Table.background");
        this.underlineOnRollover = underlineOnRollover;
        applyDefaults();
    }

    public void setRowColors(Color[] colors) {
        this.rowColors = colors;
    }

    public void updateUI() {
        super.updateUI();
        applyDefaults();
    }

    protected void applyDefaults() {
        setOpaque(true);
        setBorderPainted(false);
        foreground = UIManager.getColor("Hyperlink.foreground");
        visitedForeground = UIManager.getColor("Hyperlink.visitedForeground");

        // Make sure border used on non-focussed cells is same size as focussed border
        focusBorder = UIManager.getBorder("Table.focusCellHighlightBorder");
        if (focusBorder != null) {
            Insets insets = focusBorder.getBorderInsets(this);
            noFocusBorder = new EmptyBorder(insets.top, insets.left, insets.bottom, insets.right);
        } else {
            focusBorder = noFocusBorder = new EmptyBorder(1, 1, 1, 1);
        }
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        if (this.table == null) {
            this.table = table;
            HyperlinkMouseListener hyperlinkListener = new HyperlinkMouseListener();
            table.addMouseMotionListener(hyperlinkListener);
            table.addMouseListener(hyperlinkListener);
        }
        int columnModelIndex = table.getColumnModel().getColumn(column).getModelIndex();
        if (!columnModelIndeces.contains(columnModelIndex)) {
            columnModelIndeces.add(columnModelIndex);
        }

        if (value instanceof Link) {
            Link link = (Link) value;
            setText(link.getDisplayText());
            setToolTipText(link.getDescription());
        } else {
            setText(value != null ? value.toString() : "");
        }
        setVisited(isCellLinkVisited(value, row, column));
        setDrawUnderline(!underlineOnRollover ||
                (row == hitRowIndex && column == hitColumnIndex));

        if (!isSelected) {
            setBackground(rowColors[row % rowColors.length]);
            //setForeground(isCellLinkVisited(value, row, column)?
            //  visitedForeground : foreground);
            setForeground(foreground);
            setVisitedForeground(visitedForeground);
        } else {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
            setVisitedForeground(table.getSelectionForeground());
        }
        //setBorder(hasFocus? focusBorder : noFocusBorder);
        //System.out.println("border insets="+getBorder().getBorderInsets(this));

        return this;
    }

    protected void setCellLinkVisited(Object value, int row, int column) {
        if (!isCellLinkVisited(value, row, column)) {
            if (value instanceof Link) {
                ((Link) value).setVisited(true);
            } else {
                if (visitedCache == null) {
                    visitedCache = new HashMap<Object, int[]>();
                }
                int position[] = new int[2];
                position[0] = table.convertRowIndexToModel(row);
                position[1] = table.convertColumnIndexToModel(column);
                visitedCache.put(value, position);
            }
        }
    }

    protected boolean isCellLinkVisited(Object value, int row, int column) {
        if (value instanceof Link) {
            return ((Link) value).isVisited();
        }
        if (visitedCache != null) {
            int position[] = visitedCache.get(value);
            if (position != null) {
                return position[0] == table.convertRowIndexToModel(row) &&
                        position[1] == table.convertColumnIndexToModel(column);
            }
        }
        return false;
    }

    public int getActiveHyperlinkRow() {
        return hitRowIndex;
    }

    public int getActiveHyperlinkColumn() {
        return hitColumnIndex;
    }

    // overridden because the AbstractButton's version forces the source of the event
    // to be the AbstractButton and we want a little more freedom to configure the
    // event
    @Override
    protected void fireActionPerformed(ActionEvent event) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                ((ActionListener) listeners[i + 1]).actionPerformed(event);
            }
        }
    }

    public void invalidate() {
    }

    public void validate() {
    }

    public void revalidate() {
    }

    public void repaint(long tm, int x, int y, int width, int height) {
    }

    public void repaint(Rectangle r) {
    }

    public void repaint() {
    }

    private class HyperlinkMouseListener extends MouseAdapter {
        private transient Rectangle cellRect;
        private final transient Rectangle iconRect = new Rectangle();
        private final transient Rectangle textRect = new Rectangle();
        private transient Cursor tableCursor;

        @Override
        public void mouseMoved(MouseEvent event) {
            // This should only be called if underlineOnRollover is true
            JTable table = (JTable) event.getSource();

            // Locate the table cell under the event location
            int oldHitColumnIndex = hitColumnIndex;
            int oldHitRowIndex = hitRowIndex;

            checkIfPointInsideHyperlink(event.getPoint());

            if (hitRowIndex != oldHitRowIndex ||
                    hitColumnIndex != oldHitColumnIndex) {
                if (hitRowIndex != -1) {
                    if (tableCursor == null) {
                        tableCursor = table.getCursor();
                    }
                    table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    table.setCursor(tableCursor);
                }

                // repaint the cells affected by rollover
                Rectangle repaintRect;
                if (hitRowIndex != -1 && hitColumnIndex != -1) {
                    // we need to repaint new cell with rollover underline
                    // cellRect already contains rect of hit cell
                    if (oldHitRowIndex != -1 && oldHitColumnIndex != -1) {
                        // we also need to repaint previously underlined hyperlink cell
                        // to remove the underline
                        repaintRect = cellRect.union(
                                table.getCellRect(oldHitRowIndex, oldHitColumnIndex, false));
                    } else {
                        // we don't have a previously underlined hyperlink, so just repaint new one'
                        repaintRect = table.getCellRect(hitRowIndex, hitColumnIndex, false);
                    }
                } else {
                    // we just need to repaint previously underlined hyperlink cell
                    //to remove the underline
                    repaintRect = table.getCellRect(oldHitRowIndex, oldHitColumnIndex, false);
                }
                table.repaint(repaintRect);
            }

        }

        @Override
        public void mouseClicked(MouseEvent event) {
            if (checkIfPointInsideHyperlink(event.getPoint())) {

                ActionEvent actionEvent = new ActionEvent(new Integer(hitRowIndex),
                        ActionEvent.ACTION_PERFORMED,
                        "hyperlink");

                HyperlinkCellRenderer.this.fireActionPerformed(actionEvent);

                setCellLinkVisited(table.getValueAt(hitRowIndex, hitColumnIndex),
                        hitRowIndex, hitColumnIndex);

            }
        }

        protected boolean checkIfPointInsideHyperlink(Point p) {
            hitColumnIndex = table.columnAtPoint(p);
            hitRowIndex = table.rowAtPoint(p);

            if (hitColumnIndex != -1 && hitRowIndex != -1 &&
                    columnModelIndeces.contains(table.getColumnModel().
                            getColumn(hitColumnIndex).getModelIndex())) {
                // We know point is within a hyperlink column, however we do further hit testing
                // to see if point is within the text bounds on the hyperlink
                TableCellRenderer renderer = table.getCellRenderer(hitRowIndex, hitColumnIndex);
                JHyperlink hyperlink = (JHyperlink) table.prepareRenderer(renderer, hitRowIndex, hitColumnIndex);

                // Convert the event to the renderer's coordinate system
                cellRect = table.getCellRect(hitRowIndex, hitColumnIndex, false);
                hyperlink.setSize(cellRect.width, cellRect.height);
                p.translate(-cellRect.x, -cellRect.y);
                cellRect.x = cellRect.y = 0;
                iconRect.x = iconRect.y = iconRect.width = iconRect.height = 0;
                textRect.x = textRect.y = textRect.width = textRect.height = 0;
                SwingUtilities.layoutCompoundLabel(
                        hyperlink.getFontMetrics(hyperlink.getFont()),
                        hyperlink.getText(), hyperlink.getIcon(),
                        hyperlink.getVerticalAlignment(),
                        hyperlink.getHorizontalAlignment(),
                        hyperlink.getVerticalTextPosition(),
                        hyperlink.getHorizontalTextPosition(),
                        cellRect, iconRect, textRect, hyperlink.getIconTextGap());

                if (textRect.contains(p)) {
                    // point is within hyperlink text bounds
                    return true;
                }
            }
            // point is not within a hyperlink's text bounds
            hitRowIndex = -1;
            hitColumnIndex = -1;
            return false;
        }
    }
}
