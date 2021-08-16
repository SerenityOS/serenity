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
 * Defines how to work with windows.
 */
public interface WindowDriver {

    /**
     * Activates a window.
     *
     * @param oper Window operator.
     */
    public void activate(ComponentOperator oper);

    /**
     * Requests a window to close.
     *
     * @param oper Window operator.
     */
    public void requestClose(ComponentOperator oper);

    /**
     * Closes a window by requesting it to close and then hiding it.
     *
     * @param oper Window operator.
     */
    public void requestCloseAndThenHide(ComponentOperator oper);

    /**
     * Closes a window by requesting it to close and then hiding it.
     *
     * @param oper Window operator.
     * @deprecated Use requestClose(ComponentOperator) instead.
     */
    @Deprecated
    public void close(ComponentOperator oper);

    /**
     * Change window location.
     *
     * @param oper Window operator.
     * @param x New x coordinate
     * @param y New y coordinate
     */
    public void move(ComponentOperator oper, int x, int y);

    /**
     * Change window size.
     *
     * @param oper Window operator.
     * @param width New window width.
     * @param height New window height.
     */
    public void resize(ComponentOperator oper, int width, int height);
}
