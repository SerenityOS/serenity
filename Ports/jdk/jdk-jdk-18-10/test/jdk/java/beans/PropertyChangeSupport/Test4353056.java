/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4353056
 * @summary Tests basic IndexPropertyChangeEvent functionality
 * @author Mark Davidson
 */

import java.awt.Color;

import java.beans.IndexedPropertyChangeEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Tests the basic functionality of IndexedPropertyChangeEvent and
 * the fireIndexed... methods on PropertyChangeSupport.
 */
public class Test4353056 implements PropertyChangeListener {
    private static final int COUNT = 100;
    private static final String COLOR = "color";
    private static final String BOOLEAN = "boolean";
    private static final String INTEGER = "integer";

    public static void main(String[] args) throws Exception {
        Test4353056 test = new Test4353056();
        test.addPropertyChangeListener(test);
        for (int i = 0; i < COUNT; i++) {
            boolean even = i % 2 == 0;
            test.setColor(i, i % 3 == 0 ? Color.RED : Color.BLUE);
            test.setBoolean(i, even);
            test.setInteger(i, i);
        }
    }

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private Color color;
    private boolean flag;
    private int value;

    private String name;
    private int index = -1;

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.removePropertyChangeListener(listener);
    }

    /**
     * Setter for Object indexed property.
     *
     * @param index  the property index
     * @param color  new value
     */
    public void setColor(int index, Color color) {
        Color oldColor = this.color;
        this.color = color;

        this.index = index;
        this.name = COLOR;

        this.pcs.fireIndexedPropertyChange(COLOR, index,
                oldColor, color);
    }

    /**
     * Setter for boolean indexed property.
     *
     * @param index  the property index
     * @param flag   new value
     */
    public void setBoolean(int index, boolean flag) {
        boolean oldBool = this.flag;
        this.flag = flag;

        this.index = index;
        this.name = BOOLEAN;

        this.pcs.fireIndexedPropertyChange(BOOLEAN, index,
                oldBool, flag);
    }

    /**
     * Setter for integer indexed property.
     *
     * @param index  the property index
     * @param value  new value
     */
    public void setInteger(int index, int value) {
        int oldInt = this.value;
        this.value = value;

        this.index = index;
        this.name = INTEGER;

        this.pcs.fireIndexedPropertyChange(INTEGER, index,
                oldInt, value);
    }

    public void propertyChange(PropertyChangeEvent event) {
        Object value = event.getNewValue();
        if (value.equals(event.getOldValue())) {
            throw new Error("new value is equal to old one");
        }
        if (!this.name.equals(event.getPropertyName())) {
            throw new Error("unexpected property name");
        } else if (this.name.equals(COLOR)) {
            if (!value.equals(this.color)) {
                throw new Error("unexpected object value");
            }
        } else if (this.name.equals(BOOLEAN)) {
            if (!value.equals(Boolean.valueOf(this.flag))) {
                throw new Error("unexpected boolean value");
            }
        } else if (this.name.equals(INTEGER)) {
            if (!value.equals(Integer.valueOf(this.value))) {
                throw new Error("unexpected integer value");
            }
        } else {
            throw new Error("unexpected property name");
        }
        if (event instanceof IndexedPropertyChangeEvent) {
            IndexedPropertyChangeEvent ipce = (IndexedPropertyChangeEvent) event;
            if (this.index != ipce.getIndex()) {
                throw new Error("unexpected property index");
            }
        } else {
            throw new Error("unexpected event type");
        }
        System.out.println(this.name + " at " + this.index + " is " + value);

        this.name = null;
        this.index = -1;
    }
}
