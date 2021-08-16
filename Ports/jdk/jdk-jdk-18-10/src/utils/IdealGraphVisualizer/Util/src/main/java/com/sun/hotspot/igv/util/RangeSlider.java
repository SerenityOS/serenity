/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.hotspot.igv.util;

import com.sun.hotspot.igv.data.ChangedListener;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;
import javax.swing.*;

/**
 *
 * @author Thomas Wuerthinger
 */
public class RangeSlider extends JComponent implements ChangedListener<RangeSliderModel>, MouseListener, MouseMotionListener, Scrollable {

    public static final int HEIGHT = 40;
    public static final float BAR_HEIGHT = 22;
    public static final float BAR_SELECTION_ENDING_HEIGHT = 16;
    public static final float BAR_SELECTION_HEIGHT = 10;
    public static final float BAR_THICKNESS = 2;
    public static final float BAR_CIRCLE_SIZE = 9;
    public static final float BAR_CIRCLE_CONNECTOR_SIZE = 6;
    public static final int MOUSE_ENDING_OFFSET = 3;
    public static final Color BACKGROUND_COLOR = Color.white;
    public static final Color BAR_COLOR = Color.black;
    public static final Color BAR_SELECTION_COLOR = new Color(255, 0, 0, 120);
    public static final Color BAR_SELECTION_COLOR_ROLLOVER = new Color(255, 0, 255, 120);
    public static final Color BAR_SELECTION_COLOR_DRAG = new Color(0, 0, 255, 120);
    private RangeSliderModel model;
    private State state;
    private Point startPoint;
    private RangeSliderModel tempModel;
    private boolean isOverBar;

    private enum State {

        Initial,
        DragBar,
        DragFirstPosition,
        DragSecondPosition
    }

    public RangeSlider() {
        state = State.Initial;
        this.addMouseMotionListener(this);
        this.addMouseListener(this);
    }

    public void setModel(RangeSliderModel newModel) {
        if (model != null) {
            model.getChangedEvent().removeListener(this);
            model.getColorChangedEvent().removeListener(this);
        }
        if (newModel != null) {
            newModel.getChangedEvent().addListener(this);
            newModel.getColorChangedEvent().addListener(this);
        }
        this.model = newModel;
        update();
    }

    private RangeSliderModel getPaintingModel() {
        if (tempModel != null) {
            return tempModel;
        }
        return model;
    }

    /**
     * Returns the preferred size of the viewport for a view component.
     * For example, the preferred size of a <code>JList</code> component
     * is the size required to accommodate all of the cells in its list.
     * However, the value of <code>preferredScrollableViewportSize</code>
     * is the size required for <code>JList.getVisibleRowCount</code> rows.
     * A component without any properties that would affect the viewport
     * size should just return <code>getPreferredSize</code> here.
     *
     * @return the preferredSize of a <code>JViewport</code> whose view
     *    is this <code>Scrollable</code>
     * @see JViewport#getPreferredSize
     */
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            return 1;
        }

        return (int)(BAR_CIRCLE_SIZE + BAR_CIRCLE_CONNECTOR_SIZE);
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? visibleRect.height / 2 : visibleRect.width / 2;
    }

    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    public boolean getScrollableTracksViewportHeight() {
        return true;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.height = HEIGHT;
        d.width = Math.max(d.width, (int)(2 * BAR_CIRCLE_CONNECTOR_SIZE + getPaintingModel().getPositions().size() * (BAR_CIRCLE_SIZE + BAR_CIRCLE_CONNECTOR_SIZE)));
        return d;
    }

    @Override
    public void changed(RangeSliderModel source) {
        revalidate();

        float barStartY = getBarStartY();
        int circleCenterY = (int)(barStartY + BAR_HEIGHT / 2);
        int startX = (int)getStartXPosition(model.getFirstPosition());
        int endX = (int)getEndXPosition(model.getSecondPosition());
        Rectangle r = new Rectangle(startX, circleCenterY, endX - startX, 1);
        scrollRectToVisible(r);
        update();
    }

    private void update() {
        this.repaint();
    }

    private float getXPosition(int index) {
        assert index >= 0 && index < getPaintingModel().getPositions().size();
        return getXOffset() * (index + 1);
    }

    private float getXOffset() {
        int size = getPaintingModel().getPositions().size();
        float width = (float)getWidth();
        return (width / (size + 1));
    }

    private float getEndXPosition(int index) {
        return getXPosition(index) + getXOffset() / 2;
    }

    private float getStartXPosition(int index) {
        return getXPosition(index) - getXOffset() / 2;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        int width = getWidth();
        int height = getHeight();

        g2.setColor(BACKGROUND_COLOR);
        g2.fill(new Rectangle2D.Float(0, 0, width, height));

        // Nothing to paint?
        if (getPaintingModel() == null || getPaintingModel().getPositions().size() == 0) {
            return;
        }

        int firstPos = getPaintingModel().getFirstPosition();
        int secondPos = getPaintingModel().getSecondPosition();

        paintSelected(g2, firstPos, secondPos);
        paintBar(g2);

    }

    private float getBarStartY() {
        return getHeight() / 2 - BAR_HEIGHT / 2;
    }

    private void paintBar(Graphics2D g) {
        List<String> list = getPaintingModel().getPositions();
        float barStartY = getBarStartY();

        g.setColor(BAR_COLOR);
        g.fill(new Rectangle2D.Float(getXPosition(0), barStartY + BAR_HEIGHT / 2 - BAR_THICKNESS / 2, getXPosition(list.size() - 1) - getXPosition(0), BAR_THICKNESS));

        float circleCenterY = barStartY + BAR_HEIGHT / 2;
        for (int i = 0; i < list.size(); i++) {
            float curX = getXPosition(i);
            g.setColor(getPaintingModel().getColors().get(i));
            g.fill(new Ellipse2D.Float(curX - BAR_CIRCLE_SIZE / 2, circleCenterY - BAR_CIRCLE_SIZE / 2, BAR_CIRCLE_SIZE, BAR_CIRCLE_SIZE));
            g.setColor(Color.black);
            g.draw(new Ellipse2D.Float(curX - BAR_CIRCLE_SIZE / 2, circleCenterY - BAR_CIRCLE_SIZE / 2, BAR_CIRCLE_SIZE, BAR_CIRCLE_SIZE));


            String curS = list.get(i);
            if (curS != null && curS.length() > 0) {
                float startX = getStartXPosition(i);
                float endX = getEndXPosition(i);
                FontMetrics metrics = g.getFontMetrics();
                Rectangle bounds = metrics.getStringBounds(curS, g).getBounds();
                if (bounds.width < endX - startX && bounds.height < barStartY) {
                    g.setColor(Color.black);
                    g.drawString(curS, startX + (endX - startX) / 2 - bounds.width / 2, barStartY / 2 + bounds.height / 2);
                }
            }
        }

    }

    private void paintSelected(Graphics2D g, int start, int end) {

        float startX = getStartXPosition(start);
        float endX = getEndXPosition(end);
        float barStartY = getBarStartY();
        float barSelectionEndingStartY = barStartY + BAR_HEIGHT / 2 - BAR_SELECTION_ENDING_HEIGHT / 2;
        paintSelectedEnding(g, startX, barSelectionEndingStartY);
        paintSelectedEnding(g, endX, barSelectionEndingStartY);

        g.setColor(BAR_SELECTION_COLOR);
        if (state == State.DragBar) {
            g.setColor(BAR_SELECTION_COLOR_DRAG);
        } else if (isOverBar) {
            g.setColor(BAR_SELECTION_COLOR_ROLLOVER);
        }
        g.fill(new Rectangle2D.Float(startX, barStartY + BAR_HEIGHT / 2 - BAR_SELECTION_HEIGHT / 2, endX - startX, BAR_SELECTION_HEIGHT));
    }

    private void paintSelectedEnding(Graphics2D g, float x, float y) {
        g.setColor(BAR_COLOR);
        g.fill(new Rectangle2D.Float(x - BAR_THICKNESS / 2, y, BAR_THICKNESS, BAR_SELECTION_ENDING_HEIGHT));
    }

    private boolean isOverSecondPosition(Point p) {
        if (p.y >= getBarStartY()) {
            float destX = getEndXPosition(getPaintingModel().getSecondPosition());
            float off = Math.abs(destX - p.x);
            return off <= MOUSE_ENDING_OFFSET;
        }
        return false;
    }

    private boolean isOverFirstPosition(Point p) {
        if (p.y >= getBarStartY()) {
            float destX = getStartXPosition(getPaintingModel().getFirstPosition());
            float off = Math.abs(destX - p.x);
            return off <= MOUSE_ENDING_OFFSET;
        }
        return false;
    }

    private boolean isOverSelection(Point p) {
        if (p.y >= getBarStartY() && !isOverFirstPosition(p) && !isOverSecondPosition(p)) {
            return p.x > getStartXPosition(getPaintingModel().getFirstPosition()) && p.x < getEndXPosition(getPaintingModel().getSecondPosition());
        }
        return false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Rectangle r = new Rectangle(e.getX(), e.getY(), 1, 1);
        scrollRectToVisible(r);

        if (state == State.DragBar) {
            float firstX = this.getStartXPosition(model.getFirstPosition());
            float newFirstX = firstX + e.getPoint().x - startPoint.x;
            int newIndex = getIndexFromPosition(newFirstX) + 1;
            if (newIndex + model.getSecondPosition() - model.getFirstPosition() >= model.getPositions().size()) {
                newIndex = model.getPositions().size() - (model.getSecondPosition() - model.getFirstPosition()) - 1;
            }
            int secondPosition = newIndex + model.getSecondPosition() - model.getFirstPosition();
            tempModel.setPositions(newIndex, secondPosition);
            update();
        } else if (state == State.DragFirstPosition) {
            int firstPosition = getIndexFromPosition(e.getPoint().x) + 1;
            int secondPosition = model.getSecondPosition();
            if (firstPosition > secondPosition) {
                firstPosition--;
            }
            tempModel.setPositions(firstPosition, secondPosition);
            update();
        } else if (state == State.DragSecondPosition) {
            int firstPosition = model.getFirstPosition();
            int secondPosition = getIndexFromPosition(e.getPoint().x);
            if (secondPosition < firstPosition) {
                secondPosition++;
            }
            tempModel.setPositions(firstPosition, secondPosition);
            update();
        }
    }

    private int getIndexFromPosition(float x) {
        if (x < getXPosition(0)) {
            return -1;
        }
        for (int i = 0; i < getPaintingModel().getPositions().size() - 1; i++) {
            float startX = getXPosition(i);
            float endX = getXPosition(i + 1);
            if (x >= startX && x <= endX) {
                return i;
            }
        }
        return getPaintingModel().getPositions().size() - 1;
    }

    private int getCircleIndexFromPosition(int x) {
        int result = 0;
        for (int i = 1; i < getPaintingModel().getPositions().size(); i++) {
            if (x > getStartXPosition(i)) {
                result = i;
            }
        }
        return result;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        isOverBar = false;
        if (model == null) {
            return;
        }


        Point p = e.getPoint();
        if (isOverFirstPosition(p) || isOverSecondPosition(p)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
        } else if (isOverSelection(p)) {
            isOverBar = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            this.setCursor(Cursor.getDefaultCursor());
        }
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {
            // Double click
            int index = getCircleIndexFromPosition(e.getPoint().x);
            model.setPositions(index, index);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (model == null) {
            return;
        }

        Point p = e.getPoint();
        if (isOverFirstPosition(p)) {
            state = State.DragFirstPosition;
        } else if (isOverSecondPosition(p)) {
            state = State.DragSecondPosition;
        } else if (isOverSelection(p)) {
            state = State.DragBar;
        } else {
            return;
        }

        startPoint = e.getPoint();
        tempModel = model.copy();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (model == null || tempModel == null) {
            return;
        }
        state = State.Initial;
        model.setPositions(tempModel.getFirstPosition(), tempModel.getSecondPosition());
        tempModel = null;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
        isOverBar = false;
        repaint();
    }
}
