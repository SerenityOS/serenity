/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.hotspot.igv.selectioncoordinator;

import com.sun.hotspot.igv.data.ChangedEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Thomas
 */
public class SelectionCoordinator {

    private static SelectionCoordinator singleInstance = new SelectionCoordinator();
    private Set<Object> selectedObjects;
    private Set<Object> highlightedObjects;
    private ChangedEvent<SelectionCoordinator> selectedChangedEvent;
    private ChangedEvent<SelectionCoordinator> highlightedChangedEvent;

    public static SelectionCoordinator getInstance() {
        return singleInstance;
    }

    private SelectionCoordinator() {
        selectedChangedEvent = new ChangedEvent<>(this);
        highlightedChangedEvent = new ChangedEvent<>(this);
        selectedObjects = new HashSet<>();
        highlightedObjects = new HashSet<>();
    }

    public Set<Object> getSelectedObjects() {
        return Collections.unmodifiableSet(selectedObjects);
    }

    public Set<Object> getHighlightedObjects() {
        return Collections.unmodifiableSet(highlightedObjects);
    }

    public ChangedEvent<SelectionCoordinator> getHighlightedChangedEvent() {
        return highlightedChangedEvent;
    }

    public ChangedEvent<SelectionCoordinator> getSelectedChangedEvent() {
        return selectedChangedEvent;
    }

    public void addHighlighted(Object o) {
        if (!highlightedObjects.contains(o)) {
            highlightedObjects.add(o);
            highlightedObjectsChanged();
        }
    }

    public void removeHighlighted(Object o) {
        if (highlightedObjects.contains(o)) {
            highlightedObjects.remove(o);
            highlightedObjectsChanged();
        }
    }

    public void addAllHighlighted(Set<? extends Object> s) {
        int oldSize = highlightedObjects.size();
        highlightedObjects.addAll(s);
        if (oldSize != highlightedObjects.size()) {
            highlightedObjectsChanged();
        }
    }

    public void removeAllHighlighted(Set<? extends Object> s) {
        int oldSize = highlightedObjects.size();
        highlightedObjects.removeAll(s);
        if (oldSize != highlightedObjects.size()) {
            highlightedObjectsChanged();
        }
    }

    private void highlightedObjectsChanged() {
        highlightedChangedEvent.fire();

    }

    public void addAllSelected(Set<? extends Object> s) {
        int oldSize = selectedObjects.size();
        selectedObjects.addAll(s);
        if (oldSize != selectedObjects.size()) {
            selectedObjectsChanged();
        }
    }

    public void removeAllSelected(Set<? extends Object> s) {
        int oldSize = selectedObjects.size();
        selectedObjects.removeAll(s);
        if (oldSize != selectedObjects.size()) {
            selectedObjectsChanged();
        }
    }

    public void setSelectedObjects(Set<? extends Object> s) {
        assert s != null;
        selectedObjects.clear();
        selectedObjects.addAll(s);
        selectedObjectsChanged();
    }

    private void selectedObjectsChanged() {
        selectedChangedEvent.fire();
    }

    public void setHighlightedObjects(Set<? extends Object> s) {
        assert s != null;
        this.highlightedObjects.clear();
        this.highlightedObjects.addAll(s);
        highlightedObjectsChanged();
    }
}
