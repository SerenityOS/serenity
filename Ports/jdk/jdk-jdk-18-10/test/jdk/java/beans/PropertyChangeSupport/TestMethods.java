/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5004188
 * @summary Tests sequence of methods
 * @author Sergey Malenkov
 */

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

enum Fire {
    PropertyChangeEvent,
    PropertyObject,
    PropertyInteger,
    PropertyBoolean,
    IndexedPropertyObject,
    IndexedPropertyInteger,
    IndexedPropertyBoolean,
}

public class TestMethods extends PropertyChangeSupport implements PropertyChangeListener {
    private static final String NAME = "property";

    public static void main(String[] args) {
        Object source = new Object();
        new TestMethods(source).firePropertyChange(new PropertyChangeEvent(source, NAME, null, null));
        new TestMethods(source).firePropertyChange(NAME, null, null);
        new TestMethods(source).firePropertyChange(NAME, 0, 1);
        new TestMethods(source).firePropertyChange(NAME, true, false);
        new TestMethods(source).fireIndexedPropertyChange(NAME, 0, null, null);
        new TestMethods(source).fireIndexedPropertyChange(NAME, 0, 0, 1);
        new TestMethods(source).fireIndexedPropertyChange(NAME, 0, true, false);
    }

    private Fire state;

    public TestMethods(Object source) {
        super(source);
        addPropertyChangeListener(this);
    }

    public void propertyChange(PropertyChangeEvent event) {
        if (this.state != Fire.PropertyChangeEvent)
            throw new Error("Illegal state: " + this.state);
    }

    @Override
    public void firePropertyChange(String property, Object oldValue, Object newValue) {
        if ((this.state != null) && (this.state != Fire.PropertyBoolean) && (this.state != Fire.PropertyInteger))
            throw new Error("Illegal state: " + this.state);

        this.state = Fire.PropertyObject;
        super.firePropertyChange(property, oldValue, newValue);
    }

    @Override
    public void firePropertyChange(String property, int oldValue, int newValue) {
        if (this.state != null)
            throw new Error("Illegal state: " + this.state);

        this.state = Fire.PropertyInteger;
        super.firePropertyChange(property, oldValue, newValue);
    }

    @Override
    public void firePropertyChange(String property, boolean oldValue, boolean newValue) {
        if (this.state != null)
            throw new Error("Illegal state: " + this.state);

        this.state = Fire.PropertyBoolean;
        super.firePropertyChange(property, oldValue, newValue);
    }

    @Override
    public void firePropertyChange(PropertyChangeEvent event) {
        if ((this.state != null) && (this.state != Fire.PropertyObject) && (this.state != Fire.IndexedPropertyObject))
            throw new Error("Illegal state: " + this.state);

        this.state = Fire.PropertyChangeEvent;
        super.firePropertyChange(event);
    }

    @Override
    public void fireIndexedPropertyChange(String property, int index, Object oldValue, Object newValue) {
        if ((this.state != null) && (this.state != Fire.IndexedPropertyBoolean) && (this.state != Fire.IndexedPropertyInteger))
            throw new Error("Illegal state: " + this.state);

        this.state = Fire.IndexedPropertyObject;
        super.fireIndexedPropertyChange(property, index, oldValue, newValue);
    }

    @Override
    public void fireIndexedPropertyChange(String property, int index, int oldValue, int newValue) {
        if (this.state != null)
            throw new Error("Illegal state: " + this.state);

        this.state = Fire.IndexedPropertyInteger;
        super.fireIndexedPropertyChange(property, index, oldValue, newValue);
    }

    @Override
    public void fireIndexedPropertyChange(String property, int index, boolean oldValue, boolean newValue) {
        if (this.state != null)
            throw new Error("Illegal state: " + this.state);

        this.state = Fire.IndexedPropertyBoolean;
        super.fireIndexedPropertyChange(property, index, oldValue, newValue);
    }
}
