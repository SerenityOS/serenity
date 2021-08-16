/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.hotspot.igv.data;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Thomas Wuerthinger
 */
public class GraphDocument extends Properties.Entity implements ChangedEventProvider<GraphDocument>, Folder {

    private List<FolderElement> elements;
    private ChangedEvent<GraphDocument> changedEvent;

    public GraphDocument() {
        elements = new ArrayList<>();
        changedEvent = new ChangedEvent<>(this);
    }

    public void clear() {
        elements.clear();
        getChangedEvent().fire();
    }

    @Override
    public ChangedEvent<GraphDocument> getChangedEvent() {
        return changedEvent;
    }

    public void addGraphDocument(GraphDocument document) {
        if (document != this) {
            for (FolderElement e : document.elements) {
                e.setParent(this);
                this.addElement(e);
            }
            document.clear();
        }
        getChangedEvent().fire();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("GraphDocument: ").append(getProperties().toString()).append(" \n\n");
        for (FolderElement g : getElements()) {
            sb.append(g.toString());
            sb.append("\n\n");
        }

        return sb.toString();
    }

    @Override
    public List<? extends FolderElement> getElements() {
        return elements;
    }

    @Override
    public void removeElement(FolderElement element) {
        if (elements.remove(element)) {
            getChangedEvent().fire();
        }
    }

    @Override
    public void addElement(FolderElement element) {
        elements.add(element);
        getChangedEvent().fire();
    }
}
