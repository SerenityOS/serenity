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
import javax.swing.*;

/**
 * An event reported to a child component that originated from an
 * ancestor in the component hierarchy.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @author Dave Moore
 */
@SuppressWarnings("serial")
public class AncestorEvent extends AWTEvent {
    /**
     * An ancestor-component was added to the hierarchy of
     * visible objects (made visible), and is currently being displayed.
     */
    public static final int ANCESTOR_ADDED = 1;
    /**
     * An ancestor-component was removed from the hierarchy
     * of visible objects (hidden) and is no longer being displayed.
     */
    public static final int ANCESTOR_REMOVED = 2;
    /** An ancestor-component changed its position on the screen. */
    public static final int ANCESTOR_MOVED = 3;

    Container ancestor;
    Container ancestorParent;

    /**
     * Constructs an AncestorEvent object to identify a change
     * in an ancestor-component's display-status.
     *
     * @param source          the JComponent that originated the event
     *                        (typically <code>this</code>)
     * @param id              an int specifying {@link #ANCESTOR_ADDED},
     *                        {@link #ANCESTOR_REMOVED} or {@link #ANCESTOR_MOVED}
     * @param ancestor        a Container object specifying the ancestor-component
     *                        whose display-status changed
     * @param ancestorParent  a Container object specifying the ancestor's parent
     */
    public AncestorEvent(JComponent source, int id, Container ancestor, Container ancestorParent) {
        super(source, id);
        this.ancestor = ancestor;
        this.ancestorParent = ancestorParent;
    }

    /**
     * Returns the ancestor that the event actually occurred on.
     *
     * @return the {@code Container} object specifying the ancestor component
     */
    public Container getAncestor() {
        return ancestor;
    }

    /**
     * Returns the parent of the ancestor the event actually occurred on.
     * This is most interesting in an ANCESTOR_REMOVED event, as
     * the ancestor may no longer be in the component hierarchy.
     *
     * @return the {@code Container} object specifying the ancestor's parent
     */
    public Container getAncestorParent() {
        return ancestorParent;
    }

    /**
     * Returns the component that the listener was added to.
     *
     * @return the {@code JComponent} on which the event occurred
     */
    public JComponent getComponent() {
        return (JComponent)getSource();
    }
}
