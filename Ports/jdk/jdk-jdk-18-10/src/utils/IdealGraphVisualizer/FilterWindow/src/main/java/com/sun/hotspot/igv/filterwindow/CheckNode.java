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
package com.sun.hotspot.igv.filterwindow;

import com.sun.hotspot.igv.data.ChangedEvent;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.Lookup;

/**
 *
 * @author Thomas Wuerthinger
 */
public class CheckNode extends AbstractNode {

    private ChangedEvent<CheckNode> selectionChangedEvent;
    public boolean selected;
    public boolean enabled;

    public CheckNode(Children c, Lookup lookup) {
        super(c, lookup);
        selectionChangedEvent = new ChangedEvent<>(this);
        selected = false;
        enabled = true;
    }

    public ChangedEvent<CheckNode> getSelectionChangedEvent() {
        return selectionChangedEvent;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean b) {
        if (b != selected) {
            selected = b;
            selectionChangedEvent.fire();
        }
    }

    public void setEnabled(boolean b) {
        enabled = b;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
