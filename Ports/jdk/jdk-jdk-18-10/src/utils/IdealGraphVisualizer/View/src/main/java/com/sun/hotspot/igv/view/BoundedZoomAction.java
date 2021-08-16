/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.hotspot.igv.view;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.action.WidgetAction.State;
import org.netbeans.api.visual.action.WidgetAction.WidgetMouseWheelEvent;
import org.netbeans.api.visual.animator.SceneAnimator;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Thomas Wuerthinger
 */
public class BoundedZoomAction extends WidgetAction.Adapter {

    private double minFactor = 0.0;
    private double maxFactor = Double.MAX_VALUE;
    private double zoomMultiplier;
    private boolean useAnimator;

    public BoundedZoomAction(double zoomMultiplier, boolean useAnimator) {
        assert zoomMultiplier > 1.0;
        this.zoomMultiplier = zoomMultiplier;
        this.useAnimator = useAnimator;
    }

    public double getMinFactor() {
        return minFactor;
    }

    public void setMinFactor(double d) {
        minFactor = d;
    }

    public double getMaxFactor() {
        return maxFactor;
    }

    public void setMaxFactor(double d) {
        maxFactor = d;
    }

    private JScrollPane findScrollPane(JComponent component) {
        for (;;) {
            if (component == null) {
                return null;
            }
            if (component instanceof JScrollPane) {
                return ((JScrollPane) component);
            }
            Container parent = component.getParent();
            if (!(parent instanceof JComponent)) {
                return null;
            }
            component = (JComponent) parent;
        }
    }

    @Override
    public State mouseWheelMoved(Widget widget, WidgetMouseWheelEvent event) {
        final Scene scene = widget.getScene();
        int amount = event.getWheelRotation();
        JScrollPane scrollPane = findScrollPane(scene.getView());
        Point viewPosition = null;
        Point mouseLocation = scene.convertSceneToView(event.getPoint());
        int xOffset = 0;
        int yOffset = 0;
        Rectangle bounds = new Rectangle(scene.getBounds());
        Dimension componentSize = new Dimension(scene.getView().getPreferredSize());
        if (scrollPane != null) {
            viewPosition = new Point(scrollPane.getViewport().getViewPosition());
            xOffset = (mouseLocation.x - viewPosition.x);
            yOffset = (mouseLocation.y - viewPosition.y);
            viewPosition.x += xOffset;
            viewPosition.y += yOffset;
        }

        if (useAnimator) {
            SceneAnimator sceneAnimator = scene.getSceneAnimator();
            synchronized (sceneAnimator) {
                double zoom = sceneAnimator.isAnimatingZoomFactor() ? sceneAnimator.getTargetZoomFactor() : scene.getZoomFactor();
                while (amount > 0 && zoom / zoomMultiplier >= minFactor) {
                    zoom /= zoomMultiplier;
                    if (viewPosition != null) {
                        viewPosition.x /= zoomMultiplier;
                        viewPosition.y /= zoomMultiplier;
                        bounds.width /= zoomMultiplier;
                        bounds.height /= zoomMultiplier;
                        componentSize.width /= zoomMultiplier;
                        componentSize.height /= zoomMultiplier;
                    }
                    amount--;
                }
                while (amount < 0 && zoom * zoomMultiplier <= maxFactor) {
                    zoom *= zoomMultiplier;
                    if (viewPosition != null) {
                        viewPosition.x *= zoomMultiplier;
                        viewPosition.y *= zoomMultiplier;
                        bounds.width *= zoomMultiplier;
                        bounds.height *= zoomMultiplier;
                        componentSize.width *= zoomMultiplier;
                        componentSize.height *= zoomMultiplier;
                    }
                    amount++;
                }
                sceneAnimator.animateZoomFactor(zoom);
            }
        } else {
            double zoom = scene.getZoomFactor();
            while (amount > 0 && zoom / zoomMultiplier >= minFactor) {
                zoom /= zoomMultiplier;
                if (viewPosition != null) {
                    viewPosition.x /= zoomMultiplier;
                    viewPosition.y /= zoomMultiplier;
                    bounds.width /= zoomMultiplier;
                    bounds.height /= zoomMultiplier;
                    componentSize.width /= zoomMultiplier;
                    componentSize.height /= zoomMultiplier;
                }
                amount--;
            }
            while (amount < 0 && zoom * zoomMultiplier <= maxFactor) {
                zoom *= zoomMultiplier;
                if (viewPosition != null) {
                    viewPosition.x *= zoomMultiplier;
                    viewPosition.y *= zoomMultiplier;
                    bounds.width *= zoomMultiplier;
                    bounds.height *= zoomMultiplier;
                    componentSize.width *= zoomMultiplier;
                    componentSize.height *= zoomMultiplier;
                }
                amount++;
            }
            scene.setZoomFactor(zoom);
        }

        if (scrollPane != null) {
            viewPosition.x -= xOffset;
            viewPosition.y -= yOffset;
            scrollPane.getViewport().setViewPosition(viewPosition);
        }


        return WidgetAction.State.CONSUMED;
    }
}
