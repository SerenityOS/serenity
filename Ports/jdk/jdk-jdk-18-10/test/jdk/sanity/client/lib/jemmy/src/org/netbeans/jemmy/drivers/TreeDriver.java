/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
package org.netbeans.jemmy.drivers;

import org.netbeans.jemmy.Timeout;
import org.netbeans.jemmy.operators.ComponentOperator;

/**
 * Defines how to work with trees.
 */
public interface TreeDriver extends MultiSelListDriver {

    /**
     * Expandes a node.
     *
     * @param oper Tree operator.
     * @param index Node index.
     */
    public void expandItem(ComponentOperator oper, int index);

    /**
     * Collapses a node.
     *
     * @param oper Tree operator.
     * @param index Node index.
     */
    public void collapseItem(ComponentOperator oper, int index);

    /**
     * Edits a node.
     *
     * @param oper Tree operator.
     * @param index Node index.
     * @param newValue New node value
     * @param waitEditorTime Time to wait node editor.
     */
    public void editItem(ComponentOperator oper, int index, Object newValue, Timeout waitEditorTime);

    /**
     * Starts node editing.
     *
     * @param oper Tree operator.
     * @param index Node index.
     * @param waitEditorTime Time to wait node editor.
     */
    public void startEditing(ComponentOperator oper, int index, Timeout waitEditorTime);
}
