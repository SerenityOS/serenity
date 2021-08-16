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

import javax.swing.undo.*;

/**
 * An event indicating that an operation which can be undone has occurred.
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
 * @author Ray Ryan
 */
@SuppressWarnings("serial") // Same-version serialization only
public class UndoableEditEvent extends java.util.EventObject {
    private UndoableEdit myEdit;

    /**
     * Constructs an UndoableEditEvent object.
     *
     * @param source  the Object that originated the event
     *                (typically <code>this</code>)
     * @param edit    an UndoableEdit object
     */
    public UndoableEditEvent(Object source, UndoableEdit edit) {
        super(source);
        myEdit = edit;
    }

    /**
     * Returns the edit value.
     *
     * @return the UndoableEdit object encapsulating the edit
     */
    public UndoableEdit getEdit() {
        return myEdit;
    }
}
