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

import org.netbeans.jemmy.operators.ComponentOperator;

/**
 * Defines how to work with text components.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public interface TextDriver {

    /**
     * Moves caret.
     *
     * @param oper Text component operator.
     * @param position Position to move caret to.
     */
    public void changeCaretPosition(ComponentOperator oper, int position);

    /**
     * Selects text.
     *
     * @param oper Text component operator.
     * @param startPosition a posistion of selction start
     * @param finalPosition a posistion of selction end
     */
    public void selectText(ComponentOperator oper, int startPosition, int finalPosition);

    /**
     * Clears component text.
     *
     * @param oper Text component operator.
     */
    public void clearText(ComponentOperator oper);

    /**
     * Types new text.
     *
     * @param oper Text component operator.
     * @param text New text to type.
     * @param caretPosition Type text at that position.
     */
    public void typeText(ComponentOperator oper, String text, int caretPosition);

    /**
     * Replace component text.
     *
     * @param oper Text component operator.
     * @param text New text to type.
     */
    public void changeText(ComponentOperator oper, String text);

    /**
     * Type text and push enter.
     *
     * @param oper Text component operator.
     * @param text New text to type.
     */
    public void enterText(ComponentOperator oper, String text);
}
