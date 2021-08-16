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

import java.awt.AWTEvent;
import java.awt.Component;

import org.netbeans.jemmy.ComponentIsNotVisibleException;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.drivers.LightSupportiveDriver;

/**
 * Superclass for all drivers using event dispatching.
 *
 * @author Alexandre Iline(alexandre.iline@oracle.com)
 */
public class EventDriver extends LightSupportiveDriver {

    /**
     * Constructs an EventDriver object.
     *
     * @param supported an array of supported class names
     */
    public EventDriver(String[] supported) {
        super(supported);
    }

    /**
     * Constructs an EventDriver object suporting ComponentOperator.
     */
    public EventDriver() {
        this(new String[]{"org.netbeans.jemmy.operators.ComponentOperator"});
    }

    /**
     * Dispatches an event to the component.
     *
     * @param comp Component to dispatch events to.
     * @param event an event to dispatch.
     */
    public void dispatchEvent(Component comp, AWTEvent event) {
//        checkVisibility(comp);
        QueueTool.processEvent(event);
    }

    /**
     * Checks component visibility.
     *
     * @param component a component.
     */
    protected void checkVisibility(Component component) {
        if (!component.isVisible()) {
            throw (new ComponentIsNotVisibleException(component));
        }
    }

    /**
     * Class used fot execution of an event through the dispatching thread.
     */
    protected class Dispatcher extends QueueTool.QueueAction<Void> {

        AWTEvent event;
        Component component;

        /**
         * Constructs an EventDriver$Dispatcher object.
         *
         * @param component a component to dispatch event to.
         * @param e an event to dispatch.
         */
        public Dispatcher(Component component, AWTEvent e) {
            super(e.getClass().getName() + " event dispatching");
            this.component = component;
            event = e;
        }

        @Override
        public Void launch() {
            checkVisibility(component);
            component.dispatchEvent(event);
            return null;
        }
    }
}
