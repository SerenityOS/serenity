/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.hotspot.tools.compiler;

import java.io.PrintStream;

/**
 * The interface of an event from a HotSpot compilation log. Events can have a
 * duration, e.g., a compiler {@link Phase} is an event, and so is an entire
 * {@link Compilation}.
 */
public interface LogEvent {

    /**
     * The event's start time.
     */
    public double getStart();

    /**
     * The event's duration in milliseconds.
     */
    public double getElapsedTime();

    /**
     * The compilation during which this event was signalled.
     */
    public Compilation getCompilation();

    /**
     * Print the event to the given stream.
     */
    public void print(PrintStream stream, boolean printID);
}
