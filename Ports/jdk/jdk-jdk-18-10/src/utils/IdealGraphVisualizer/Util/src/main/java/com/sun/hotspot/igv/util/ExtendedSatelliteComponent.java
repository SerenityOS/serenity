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
package com.sun.hotspot.igv.util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.JComponent;
import org.netbeans.api.visual.widget.Scene;

/**
 * @author David Kaspar
 * @author Thomas Wuerthinger
 */
public class ExtendedSatelliteComponent extends JComponent implements MouseListener, MouseMotionListener, Scene.SceneListener, ComponentListener {

    private Scene scene;
    private Image image;
    private int imageWidth;
    private int imageHeight;

    public ExtendedSatelliteComponent(Scene scene) {
        this.scene = scene;
        setDoubleBuffered(true);
        setPreferredSize(new Dimension(128, 128));
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        scene.addSceneListener(this);
        JComponent viewComponent = scene.getView();
        if (viewComponent == null) {
            viewComponent = scene.createView();
        }
        viewComponent.addComponentListener(this);
        repaint();
    }

    @Override
    public void removeNotify() {
        scene.getView().removeComponentListener(this);
        scene.removeSceneListener(this);
        super.removeNotify();
    }

    public void update() {
        this.image = null;
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D gr = (Graphics2D) g;
        super.paint(g);
        Rectangle bounds = scene.getBounds();
        Dimension size = getSize();

        double sx = bounds.width > 0 ? (double) size.width / bounds.width : 0.0;
        double sy = bounds.width > 0 ? (double) size.height / bounds.height : 0.0;
        double scale = Math.min(sx, sy);

        int vw = (int) (scale * bounds.width);
        int vh = (int) (scale * bounds.height);
        int vx = (size.width - vw) / 2;
        int vy = (size.height - vh) / 2;


        if (image == null || vw != imageWidth || vh != imageHeight) {

            imageWidth = vw;
            imageHeight = vh;
            image = this.createImage(imageWidth, imageHeight);
            Graphics2D ig = (Graphics2D) image.getGraphics();
            ig.scale(scale, scale);
            scene.paint(ig);
        }

        gr.drawImage(image, vx, vy, this);

        JComponent component = scene.getView();
        double zoomFactor = scene.getZoomFactor();
        Rectangle viewRectangle = component != null ? component.getVisibleRect() : null;
        if (viewRectangle != null) {
            Rectangle window = new Rectangle(
                    (int) ((double) viewRectangle.x * scale / zoomFactor),
                    (int) ((double) viewRectangle.y * scale / zoomFactor),
                    (int) ((double) viewRectangle.width * scale / zoomFactor),
                    (int) ((double) viewRectangle.height * scale / zoomFactor));
            window.translate(vx, vy);
            gr.setColor(new Color(200, 200, 200, 128));
            gr.fill(window);
            gr.setColor(Color.BLACK);
            gr.drawRect(window.x, window.y, window.width - 1, window.height - 1);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        moveVisibleRect(e.getPoint());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        moveVisibleRect(e.getPoint());
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        moveVisibleRect(e.getPoint());
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    private void moveVisibleRect(Point center) {
        JComponent component = scene.getView();
        if (component == null) {
            return;
        }
        double zoomFactor = scene.getZoomFactor();
        Rectangle bounds = scene.getBounds();
        Dimension size = getSize();

        double sx = bounds.width > 0 ? (double) size.width / bounds.width : 0.0;
        double sy = bounds.width > 0 ? (double) size.height / bounds.height : 0.0;
        double scale = Math.min(sx, sy);

        int vw = (int) (scale * bounds.width);
        int vh = (int) (scale * bounds.height);
        int vx = (size.width - vw) / 2;
        int vy = (size.height - vh) / 2;

        int cx = (int) ((double) (center.x - vx) / scale * zoomFactor);
        int cy = (int) ((double) (center.y - vy) / scale * zoomFactor);

        Rectangle visibleRect = component.getVisibleRect();
        visibleRect.x = cx - visibleRect.width / 2;
        visibleRect.y = cy - visibleRect.height / 2;
        component.scrollRectToVisible(visibleRect);

    }

    @Override
    public void sceneRepaint() {
    }

    @Override
    public void sceneValidating() {
    }

    @Override
    public void sceneValidated() {
    }

    @Override
    public void componentResized(ComponentEvent e) {
        repaint();
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        repaint();
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }

    @Override
    public void componentHidden(ComponentEvent e) {
    }
}
