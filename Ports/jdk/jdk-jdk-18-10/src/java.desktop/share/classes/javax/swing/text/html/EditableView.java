/*
 * Copyright (c) 1998, 2004, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text.html;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.text.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.util.*;

/**
 * EditableView sets the view it contains to be visible only when the
 * JTextComponent the view is contained in is editable. The min/pref/max
 * size is 0 when not visible.
 *
 * @author  Scott Violet
 */
class EditableView extends ComponentView {

    EditableView(Element e) {
        super(e);
    }

    public float getMinimumSpan(int axis) {
        if (isVisible) {
            return super.getMinimumSpan(axis);
        }
        return 0;
    }

    public float getPreferredSpan(int axis) {
        if (isVisible) {
            return super.getPreferredSpan(axis);
        }
        return 0;
    }

    public float getMaximumSpan(int axis) {
        if (isVisible) {
            return super.getMaximumSpan(axis);
        }
        return 0;
    }

    public void paint(Graphics g, Shape allocation) {
        Component c = getComponent();
        Container host = getContainer();

        if (host instanceof JTextComponent &&
            isVisible != ((JTextComponent)host).isEditable()) {
            isVisible = ((JTextComponent)host).isEditable();
            preferenceChanged(null, true, true);
            host.repaint();
        }
        /*
         * Note: we cannot tweak the visible state of the
         * component in createComponent() even though it
         * gets called after the setParent() call where
         * the value of the boolean is set.  This
         * because, the setComponentParent() in the
         * superclass, always does a setVisible(false)
         * after calling createComponent().   We therefore
         * use this flag in the paint() method to
         * setVisible() to true if required.
         */
        if (isVisible) {
            super.paint(g, allocation);
        }
        else {
            setSize(0, 0);
        }
        if (c != null) {
            c.setFocusable(isVisible);
        }
    }

    public void setParent(View parent) {
        if (parent != null) {
            Container host = parent.getContainer();
            if (host != null) {
                if (host instanceof JTextComponent) {
                    isVisible = ((JTextComponent)host).isEditable();
                } else {
                    isVisible = false;
                }
            }
        }
        super.setParent(parent);
    }

    /**
     * @return true if the Component is visible.
     */
    public boolean isVisible() {
        return isVisible;
    }

    /** Set to true if the component is visible. This is based off the
     * editability of the container. */
    private boolean isVisible;
} // End of EditableView
