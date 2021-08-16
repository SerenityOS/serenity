/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.hotspot.igv.view.widgets;

import com.sun.hotspot.igv.graph.Connection;
import com.sun.hotspot.igv.graph.Figure;
import com.sun.hotspot.igv.graph.InputSlot;
import com.sun.hotspot.igv.graph.OutputSlot;
import com.sun.hotspot.igv.util.StringUtils;
import com.sun.hotspot.igv.view.DiagramScene;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.PopupMenuProvider;
import org.netbeans.api.visual.action.SelectProvider;
import org.netbeans.api.visual.animator.SceneAnimator;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Thomas Wuerthinger
 */
public class LineWidget extends Widget implements PopupMenuProvider {

    public final int BORDER = 5;
    public final int ARROW_SIZE = 6;
    public final int BOLD_ARROW_SIZE = 7;
    public final int HOVER_ARROW_SIZE = 8;
    public final int BOLD_STROKE_WIDTH = 2;
    public final int HOVER_STROKE_WIDTH = 3;
    private static double ZOOM_FACTOR = 0.1;
    private OutputSlot outputSlot;
    private DiagramScene scene;
    private List<Connection> connections;
    private Point from;
    private Point to;
    private Rectangle clientArea;
    private Color color = Color.BLACK;
    private LineWidget predecessor;
    private List<LineWidget> successors;
    private boolean highlighted;
    private boolean popupVisible;
    private boolean isBold;
    private boolean isDashed;

    public LineWidget(DiagramScene scene, OutputSlot s, List<Connection> connections, Point from, Point to, LineWidget predecessor, SceneAnimator animator, boolean isBold, boolean isDashed) {
        super(scene);
        this.scene = scene;
        this.outputSlot = s;
        this.connections = connections;
        this.from = from;
        this.to = to;
        this.predecessor = predecessor;
        this.successors = new ArrayList<>();
        if (predecessor != null) {
            predecessor.addSuccessor(this);
        }

        this.isBold = isBold;
        this.isDashed = isDashed;

        int minX = from.x;
        int minY = from.y;
        int maxX = to.x;
        int maxY = to.y;
        if (minX > maxX) {
            int tmp = minX;
            minX = maxX;
            maxX = tmp;
        }

        if (minY > maxY) {
            int tmp = minY;
            minY = maxY;
            maxY = tmp;
        }

        clientArea = new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
        clientArea.grow(BORDER, BORDER);

        if (connections.size() > 0) {
            color = connections.get(0).getColor();
        }
        this.setToolTipText("<HTML>" + generateToolTipText(this.connections) + "</HTML>");

        this.setCheckClipping(true);

        this.getActions().addAction(ActionFactory.createPopupMenuAction(this));
        if (animator == null) {
            this.setBackground(color);
        } else {
            this.setBackground(Color.WHITE);
            animator.animateBackgroundColor(this, color);
        }

        this.getActions().addAction(ActionFactory.createSelectAction(new SelectProvider() {

            @Override
            public boolean isAimingAllowed(Widget arg0, Point arg1, boolean arg2) {
                return true;
            }

            @Override
            public boolean isSelectionAllowed(Widget arg0, Point arg1, boolean arg2) {
                return true;
            }

            @Override
            public void select(Widget arg0, Point arg1, boolean arg2) {
                Set<Figure> set = new HashSet<>();
                for (Connection c : LineWidget.this.connections) {
                    set.add(c.getInputSlot().getFigure());
                    set.add(c.getOutputSlot().getFigure());
                }
                LineWidget.this.scene.setSelectedObjects(set);
            }
        }));
    }

    private String generateToolTipText(List<Connection> conn) {
        StringBuilder sb = new StringBuilder();
        for (Connection c : conn) {
            sb.append(StringUtils.escapeHTML(c.getToolTipText()));
            sb.append("<br>");
        }
        return sb.toString();
    }

    public Point getFrom() {
        return from;
    }

    public Point getTo() {
        return to;
    }

    private void addSuccessor(LineWidget widget) {
        this.successors.add(widget);
    }

    @Override
    protected Rectangle calculateClientArea() {
        return clientArea;
    }

    @Override
    protected void paintWidget() {
        if (scene.getZoomFactor() < ZOOM_FACTOR) {
            return;
        }

        Graphics2D g = getScene().getGraphics();
        g.setPaint(this.getBackground());
        float width = 1.0f;

        if (isBold) {
            width = BOLD_STROKE_WIDTH;
        }

        if (highlighted || popupVisible) {
            width = HOVER_STROKE_WIDTH;
        }

        Stroke oldStroke = g.getStroke();
        if (isDashed) {
            float[] dashPattern = {5, 5, 5, 5};
            g.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 10,
                    dashPattern, 0));
        } else {
            g.setStroke(new BasicStroke(width));
        }

        g.drawLine(from.x, from.y, to.x, to.y);

        boolean sameFrom = false;
        boolean sameTo = successors.isEmpty();
        for (LineWidget w : successors) {
            if (w.getFrom().equals(getTo())) {
                sameTo = true;
            }
        }

        if (predecessor == null || predecessor.getTo().equals(getFrom())) {
            sameFrom = true;
        }


        int size = ARROW_SIZE;
        if (isBold) {
            size = BOLD_ARROW_SIZE;
        }
        if (highlighted || popupVisible) {
            size = HOVER_ARROW_SIZE;
        }
        if (!sameFrom) {
            g.fillPolygon(
                    new int[]{from.x - size / 2, from.x + size / 2, from.x},
                    new int[]{from.y - size / 2, from.y - size / 2, from.y + size / 2},
                    3);
        }
        if (!sameTo) {
            g.fillPolygon(
                    new int[]{to.x - size / 2, to.x + size / 2, to.x},
                    new int[]{to.y - size / 2, to.y - size / 2, to.y + size / 2},
                    3);
        }
        g.setStroke(oldStroke);
    }

    private void setHighlighted(boolean b) {
        this.highlighted = b;
        Set<Object> highlightedObjects = new HashSet<>(scene.getHighlightedObjects());
        Set<Object> highlightedObjectsChange = new HashSet<>();
        for (Connection c : connections) {
            highlightedObjectsChange.add(c.getInputSlot().getFigure());
            highlightedObjectsChange.add(c.getInputSlot());
            highlightedObjectsChange.add(c.getOutputSlot().getFigure());
            highlightedObjectsChange.add(c.getOutputSlot());
        }
        if(b) {
            highlightedObjects.addAll(highlightedObjectsChange);
        } else {
            highlightedObjects.removeAll(highlightedObjectsChange);
        }
        scene.setHighlightedObjects(highlightedObjects);
        this.revalidate(true);
    }

    private void setPopupVisible(boolean b) {
        this.popupVisible = b;
        this.revalidate(true);
    }

    @Override
    public boolean isHitAt(Point localPoint) {
        return Line2D.ptLineDistSq(from.x, from.y, to.x, to.y, localPoint.x, localPoint.y) <= BORDER * BORDER;
    }

    @Override
    protected void notifyStateChanged(ObjectState previousState, ObjectState state) {
        if (previousState.isHovered() != state.isHovered()) {
            setRecursiveHighlighted(state.isHovered());
        }
    }

    private void setRecursiveHighlighted(boolean b) {
        LineWidget cur = predecessor;
        while (cur != null) {
            cur.setHighlighted(b);
            cur = cur.predecessor;
        }

        highlightSuccessors(b);
        this.setHighlighted(b);
    }

    private void highlightSuccessors(boolean b) {
        for (LineWidget s : successors) {
            s.setHighlighted(b);
            s.highlightSuccessors(b);
        }
    }

    private void setRecursivePopupVisible(boolean b) {
        LineWidget cur = predecessor;
        while (cur != null) {
            cur.setPopupVisible(b);
            cur = cur.predecessor;
        }

        popupVisibleSuccessors(b);
        setPopupVisible(b);
    }

    private void popupVisibleSuccessors(boolean b) {
        for (LineWidget s : successors) {
            s.setPopupVisible(b);
            s.popupVisibleSuccessors(b);
        }
    }

    @Override
    public JPopupMenu getPopupMenu(Widget widget, Point localLocation) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(scene.createGotoAction(outputSlot.getFigure()));
        menu.addSeparator();

        for (Connection c : connections) {
            InputSlot s = c.getInputSlot();
            menu.add(scene.createGotoAction(s.getFigure()));
        }

        final LineWidget w = this;
        menu.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                w.setRecursivePopupVisible(true);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                w.setRecursivePopupVisible(false);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        return menu;
    }
}
