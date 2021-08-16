/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
package org.netbeans.jemmy.util;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Point;

import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.drivers.input.MouseRobotDriver;
import org.netbeans.jemmy.operators.Operator;
import org.netbeans.jemmy.operators.WindowOperator;

/**
 *
 * Does
 * {@code super.activate(org.netbeans.jemmy.operators.WindowOperator)}.
 * Then, if java version is appropriate (1.3 or later) activates windows by
 * robot mouse click on border.
 *
 * @see
 * org.netbeans.jemmy.operators.Operator#setVisualizer(Operator.ComponentVisualizer)
 * @see org.netbeans.jemmy.operators.Operator.ComponentVisualizer
 *
 * <BR><BR>Timeouts used: <BR>
 * MouseVisualiser.BeforeClickTimeout - time to let a window manager to move a
 * window as it wants<BR>
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class MouseVisualizer extends DefaultVisualizer {

    private static final long BEFORE_CLICK = 100;

    /**
     * A constant used to inform that window activating click needs to performed
     * on the <b>top</b> side of frame.
     *
     * @see #MouseVisualizer()
     */
    public static final int TOP = 0;

    /**
     * A constant used to inform that window activating click needs to performed
     * on the <b>botton</b> side of frame.
     *
     * @see #MouseVisualizer()
     */
    public static final int BOTTOM = 1;

    /**
     * A constant used to inform that window activating click needs to performed
     * on the <b>left</b> side of frame.
     *
     * @see #MouseVisualizer()
     */
    public static final int LEFT = 2;

    /**
     * A constant used to inform that window activating click needs to performed
     * on the <b>right</b> side of frame.
     *
     * @see #MouseVisualizer()
     */
    public static final int RIGHT = 3;

    private int place = 0;
    private double pointLocation = 0;
    private int depth = 0;

    /**
     * Creates a visualizer which clicks on (0, 0) window coords.
     */
    public MouseVisualizer() {
    }

    /**
     * Creates a visualizer which clicks on window border. In case if
     * {@code place == BOTTOM}, for example clicks on (width *
     * pointLocation, height - depth) coordinates.
     *
     * @param place One of the predefined value: TOP, BOTTOM, LEFT, RIGHT
     * @param pointLocation Proportional coordinates to click.
     * @param depth Distance from the border.
     * @param checkMouse Check if there is any java component under mouse
     * (currently ignored)
     */
    public MouseVisualizer(int place, double pointLocation, int depth, boolean checkMouse) {
        this.place = place;
        this.pointLocation = pointLocation;
        this.depth = depth;
    }

    static {
        Timeouts.initDefault("MouseVisualiser.BeforeClickTimeout", BEFORE_CLICK);
    }

    @Override
    protected boolean isWindowActive(WindowOperator winOper) {
        return (super.isWindowActive(winOper)
                && (winOper.getSource() instanceof Frame
                || winOper.getSource() instanceof Dialog));
    }

    @Override
    protected void makeWindowActive(WindowOperator winOper) {
        JemmyProperties.getCurrentTimeouts().
                create("MouseVisualiser.BeforeClickTimeout").sleep();
        super.makeWindowActive(winOper);
        if (!System.getProperty("java.version").startsWith("1.2")) {
            Point p = getClickPoint(winOper);
            new MouseRobotDriver(winOper.getTimeouts().create("EventDispatcher.RobotAutoDelay")).
                    clickMouse(winOper, p.x, p.y,
                            1, Operator.getDefaultMouseButton(),
                            0,
                            winOper.getTimeouts().create("ComponentOperator.MouseClickTimeout"));
        }
    }

    private Point getClickPoint(WindowOperator win) {
        int x, y;
        if (place == LEFT
                || place == RIGHT) {
            y = ((int) (win.getHeight() * pointLocation - 1));
            if (place == RIGHT) {
                x = win.getWidth() - 1 - depth;
            } else {
                x = depth;
            }
        } else {
            x = ((int) (win.getWidth() * pointLocation - 1));
            if (place == BOTTOM) {
                y = win.getHeight() - 1 - depth;
            } else {
                y = depth;
            }
        }
        if (x < 0) {
            x = 0;
        }
        if (x >= win.getWidth()) {
            x = win.getWidth() - 1;
        }
        if (y < 0) {
            y = 0;
        }
        if (y >= win.getHeight()) {
            y = win.getHeight() - 1;
        }
        return new Point(x, y);
    }
}
