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
package org.netbeans.jemmy.drivers.input;

import java.awt.Component;
import java.awt.event.MouseEvent;
import org.netbeans.jemmy.Timeout;
import org.netbeans.jemmy.drivers.MouseDriver;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.Operator;

/**
 * MouseDriver using event dispatching.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public class MouseEventDriver extends EventDriver implements MouseDriver {

    /**
     * Constructs a MouseEventDriver object.
     *
     * @param supported an array of supported class names
     */
    public MouseEventDriver(String[] supported) {
        super(supported);
    }

    /**
     * Constructs a MouseEventDriver object.
     */
    public MouseEventDriver() {
        super();
    }

    @Override
    public void pressMouse(ComponentOperator oper, int x, int y, int mouseButton, int modifiers) {
        dispatchEvent(oper.getSource(),
                MouseEvent.MOUSE_PRESSED,
                modifiers, x, y, 1,
                mouseButton);
    }

    @Override
    public void releaseMouse(ComponentOperator oper, int x, int y, int mouseButton, int modifiers) {
        dispatchEvent(oper.getSource(),
                MouseEvent.MOUSE_RELEASED,
                modifiers, x, y, 1,
                mouseButton);
    }

    @Override
    public void moveMouse(ComponentOperator oper, int x, int y) {
        dispatchEvent(oper.getSource(),
                MouseEvent.MOUSE_MOVED,
                0, x, y, 0,
                Operator.getDefaultMouseButton());
    }

    @Override
    public void clickMouse(ComponentOperator oper, int x, int y, int clickCount, int mouseButton,
            int modifiers, Timeout mouseClick) {

        moveMouse(oper, x, y);
        dispatchEvent(oper.getSource(),
                MouseEvent.MOUSE_ENTERED,
                0, x, y, 0,
                Operator.getDefaultMouseButton());
        dispatchEvent(oper.getSource(),
                MouseEvent.MOUSE_PRESSED,
                modifiers, x, y, 1,
                mouseButton);

        for (int i = 1; i < clickCount; i++) {
            dispatchEvent(oper.getSource(),
                    MouseEvent.MOUSE_RELEASED,
                    modifiers, x, y, i,
                    mouseButton);
            dispatchEvent(oper.getSource(),
                    MouseEvent.MOUSE_CLICKED,
                    modifiers, x, y, i,
                    mouseButton);
            dispatchEvent(oper.getSource(),
                    MouseEvent.MOUSE_PRESSED,
                    modifiers, x, y, i + 1,
                    mouseButton);
        }

        mouseClick.sleep();
        dispatchEvent(oper.getSource(),
                MouseEvent.MOUSE_RELEASED,
                modifiers, x, y, clickCount,
                mouseButton);
        dispatchEvent(oper.getSource(),
                MouseEvent.MOUSE_CLICKED,
                modifiers, x, y, clickCount,
                mouseButton);
        exitMouse(oper);
    }

    @Override
    public void dragMouse(ComponentOperator oper, int x, int y, int mouseButton, int modifiers) {
        dispatchEvent(oper.getSource(),
                MouseEvent.MOUSE_DRAGGED,
                modifiers, x, y, 1,
                mouseButton);
    }

    @Override
    public void dragNDrop(ComponentOperator oper, int start_x, int start_y, int end_x, int end_y,
            int mouseButton, int modifiers, Timeout before, Timeout after) {

        dispatchEvent(oper.getSource(),
                MouseEvent.MOUSE_ENTERED,
                0, start_x, start_y, 0,
                Operator.getDefaultMouseButton());
        dispatchEvent(oper.getSource(),
                MouseEvent.MOUSE_PRESSED,
                modifiers, start_x, start_y, 1,
                mouseButton);
        before.sleep();
        dragMouse(oper, end_x, end_y, mouseButton, modifiers);
        after.sleep();
        dispatchEvent(oper.getSource(),
                MouseEvent.MOUSE_RELEASED,
                modifiers, end_x, end_y, 1,
                mouseButton);
        exitMouse(oper);
    }

    @Override
    public void enterMouse(ComponentOperator oper) {
        dispatchEvent(oper.getSource(),
                MouseEvent.MOUSE_ENTERED,
                0, oper.getCenterX(), oper.getCenterY(), 0,
                Operator.getDefaultMouseButton());
    }

    @Override
    public void exitMouse(ComponentOperator oper) {
        dispatchEvent(oper.getSource(),
                MouseEvent.MOUSE_EXITED,
                0, oper.getCenterX(), oper.getCenterY(), 0,
                Operator.getDefaultMouseButton());
    }

    /**
     * Dispatches a mouse event to the component.
     *
     * @param comp Component to dispatch events to.
     * @param id an event id.
     * @param modifiers a combination of {@code InputEvent.*_MASK} fields.
     * @param x relative x coordinate of event point
     * @param y relative y coordinate of event point
     * @param clickCount click count
     * @param mouseButton mouse button.
     */
    protected void dispatchEvent(Component comp, int id, int modifiers, int x, int y, int clickCount, int mouseButton) {
        dispatchEvent(comp,
                new MouseEvent(comp,
                        id,
                        System.currentTimeMillis(),
                        modifiers | mouseButton, x, y, clickCount,
                        mouseButton == Operator.getPopupMouseButton()
                        && id == MouseEvent.MOUSE_PRESSED));
    }
}
