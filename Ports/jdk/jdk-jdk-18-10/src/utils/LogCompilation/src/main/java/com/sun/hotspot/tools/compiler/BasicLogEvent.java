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
 * Provide basic data structures and behaviour for {@link LogEvent}s.
 */
public abstract class BasicLogEvent implements LogEvent {

    /**
     * The event's ID. This is a number; we represent it as a string for
     * convenience.
     */
    protected final String id;

    /**
     * The event's start time.
     */
    protected final double start;

    /**
     * The event's end time.
     */
    protected double end;

    /**
     * The compilation during which this event was signalled.
     */
    protected Compilation compilation;

    BasicLogEvent(double start, String id) {
        this.start = start;
        this.end = start;
        this.id = id;
    }

    public final double getStart() {
        return start;
    }

    public final double getEnd() {
        return end;
    }

    public final void setEnd(double end) {
        this.end = end;
    }

    public final double getElapsedTime() {
        return ((int) ((getEnd() - getStart()) * 1000)) / 1000.0;
    }

    public final String getId() {
        return id;
    }

    public final Compilation getCompilation() {
        return compilation;
    }

    /**
     * Set the compilation for this event. This is not a {@code final} method
     * as it is overridden in {@link UncommonTrapEvent}.
     */
    public void setCompilation(Compilation compilation) {
        this.compilation = compilation;
    }

    abstract public void print(PrintStream stream, boolean printID);
}
