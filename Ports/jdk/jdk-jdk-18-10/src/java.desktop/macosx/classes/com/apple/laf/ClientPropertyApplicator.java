/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.apple.laf;

import java.beans.*;
import java.util.*;

import javax.swing.JComponent;

public class ClientPropertyApplicator<T extends JComponent, N> implements PropertyChangeListener {
    private final Map<String, Property<N>> properties = new HashMap<String, Property<N>>();

    @SuppressWarnings("unchecked")
    public ClientPropertyApplicator(final Property<N>... propertyList) {
        for (final Property<N> p : propertyList) {
            properties.put(p.name, p);
        }
    }

    void applyProperty(final N target, final String propName, final Object value) {
        final Property<N> property = properties.get(propName);
        if (property != null) {
            property.applyProperty(target, value);
        }
    }

    public void attachAndApplyClientProperties(final T target) {
        target.addPropertyChangeListener(this);
        final N obj = convertJComponentToTarget(target);
        if (obj == null) {
            return;
        }

        final Set<String> propNames = properties.keySet();
        for (final String propName : propNames) {
            final Object value = target.getClientProperty(propName);
            if (value == null) {
                continue;
            }
            applyProperty(obj, propName, value);
        }
    }

    public void removeFrom(final T target) {
        target.removePropertyChangeListener(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void propertyChange(final PropertyChangeEvent evt) {
        final N obj = convertJComponentToTarget((T)evt.getSource());
        if (obj == null) return;
        applyProperty(obj, evt.getPropertyName(), evt.getNewValue());
    }

    @SuppressWarnings("unchecked")
    public N convertJComponentToTarget(final T component) {
        return (N)component; // naive implementation
    }

    public abstract static class Property<X> {
        final String name;

        public Property(final String name) {
            this.name = name;
        }

        public abstract void applyProperty(final X target, final Object value);
    }
}
