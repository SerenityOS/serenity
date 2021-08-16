/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.event;

import java.awt.event.*;
import java.awt.*;
import java.util.*;

import javax.swing.*;

/**
 * AncestorListener
 *
 * Interface to support notification when changes occur to a JComponent or one
 * of its ancestors.  These include movement and when the component becomes
 * visible or invisible, either by the setVisible() method or by being added
 * or removed from the component hierarchy.
 *
 * @author Dave Moore
 */
public interface AncestorListener extends EventListener {
    /**
     * Called when the source or one of its ancestors is made visible
     * either by setVisible(true) being called or by its being
     * added to the component hierarchy.  The method is only called
     * if the source has actually become visible.  For this to be true
     * all its parents must be visible and it must be in a hierarchy
     * rooted at a Window
     *
     * @param event an {@code AncestorEvent} signifying a change in an
     *              ancestor-component's display-status
     */
    public void ancestorAdded(AncestorEvent event);

    /**
     * Called when the source or one of its ancestors is made invisible
     * either by setVisible(false) being called or by its being
     * removed from the component hierarchy.  The method is only called
     * if the source has actually become invisible.  For this to be true
     * at least one of its parents must by invisible or it is not in
     * a hierarchy rooted at a Window
     *
     * @param event an {@code AncestorEvent} signifying a change in an
     *              ancestor-component's display-status
     */
    public void ancestorRemoved(AncestorEvent event);

    /**
     * Called when either the source or one of its ancestors is moved.
     *
     * @param event an {@code AncestorEvent} signifying a change in an
     *              ancestor-component's display-status
     */
    public void ancestorMoved(AncestorEvent event);

}
