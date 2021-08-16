/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package test;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.EventListener;
import java.util.TooManyListenersException;

public class Accessor {

    public static Class<?> getBeanType() {
        return Bean.class;
    }

    public static Class<?> getListenerType() {
        return TestListener.class;
    }
}

interface TestEvent {
}

interface TestListener extends EventListener {
    void process(TestEvent event);
}

class Bean {

    private boolean b;
    private int[] indexed;
    private TestListener listener;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }

    public void addTestListener(TestListener listener) throws TooManyListenersException {
        if (listener != null) {
            if (this.listener != null) {
                throw new TooManyListenersException();
            }
            this.listener = listener;
        }
    }

    public void removeTestListener(TestListener listener) {
        if (this.listener == listener) {
            this.listener = null;
        }
    }

    public TestListener[] getTestListeners() {
        return (this.listener != null)
                ? new TestListener[] { this.listener }
                : new TestListener[0];
    }

    public boolean isBoolean() {
        return this.b;
    }

    public void setBoolean(boolean b) {
        this.b = b;
    }

    public int[] getIndexed() {
        return this.indexed;
    }

    public void setIndexed(int[] values) {
        this.indexed = values;
    }

    public int getIndexed(int index) {
        return this.indexed[index];
    }

    public void setIndexed(int index, int value) {
        this.indexed[index] = value;
    }
}
