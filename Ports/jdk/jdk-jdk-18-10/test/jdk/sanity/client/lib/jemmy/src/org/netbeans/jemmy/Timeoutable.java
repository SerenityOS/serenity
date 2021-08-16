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
package org.netbeans.jemmy;

/**
 *
 * Any class which contains methods requiring waiting or sleeping should
 * implement this interface. Waiting and sleeping operations have time limits
 * that can be set or returned using the methods of this interface.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public interface Timeoutable {

    /**
     * Defines current timeouts.
     *
     * @param t A collection of timeout assignments.
     * @see #getTimeouts
     */
    public void setTimeouts(Timeouts t);

    /**
     * Return current timeouts.
     *
     * @return the collection of current timeout assignments.
     * @see #setTimeouts
     */
    public Timeouts getTimeouts();
}
