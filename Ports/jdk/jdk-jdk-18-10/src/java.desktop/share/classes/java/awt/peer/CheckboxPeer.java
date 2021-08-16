/*
 * Copyright (c) 1995, 2014, Oracle and/or its affiliates. All rights reserved.
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
package java.awt.peer;

import java.awt.Checkbox;
import java.awt.CheckboxGroup;

/**
 * The peer interface for {@link Checkbox}.
 *
 * The peer interfaces are intended only for use in porting
 * the AWT. They are not intended for use by application
 * developers, and developers should not implement peers
 * nor invoke any of the peer methods directly on the peer
 * instances.
 */
public interface CheckboxPeer extends ComponentPeer {

    /**
     * Sets the state of the checkbox to be checked {@code true} or
     * unchecked {@code false}.
     *
     * @param state the state to set on the checkbox
     *
     * @see Checkbox#setState(boolean)
     */
    void setState(boolean state);

    /**
     * Sets the checkbox group for this checkbox. Checkboxes in one checkbox
     * group can only be selected exclusively (like radio buttons). A value
     * of {@code null} removes this checkbox from any checkbox group.
     *
     * @param g the checkbox group to set, or {@code null} when this
     *          checkbox should not be placed in any group
     *
     * @see Checkbox#setCheckboxGroup(CheckboxGroup)
     */
    void setCheckboxGroup(CheckboxGroup g);

    /**
     * Sets the label that should be displayed on the checkbox. A value of
     * {@code null} means that no label should be displayed.
     *
     * @param label the label to be displayed on the checkbox, or
     *              {@code null} when no label should be displayed.
     */
    void setLabel(String label);

}
