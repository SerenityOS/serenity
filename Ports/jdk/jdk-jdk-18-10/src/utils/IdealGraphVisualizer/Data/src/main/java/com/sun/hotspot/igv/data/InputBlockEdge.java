/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.hotspot.igv.data;

/**
 *
 * @author Thomas Wuerthinger
 */
public class InputBlockEdge {

    public enum State {
        SAME,
        NEW,
        DELETED
    }

    private InputBlock from;
    private InputBlock to;
    private State state = State.SAME;

    public InputBlockEdge(InputBlock from, InputBlock to) {
        assert from != null;
        assert to != null;
        this.from = from;
        this.to = to;
    }

    public InputBlock getFrom() {
        return from;
    }

    public InputBlock getTo() {
        return to;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof InputBlockEdge) {
            InputBlockEdge e = (InputBlockEdge) obj;
            return e.from.equals(from) && e.to.equals(to);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = from.hashCode();
        hash = 59 * hash + to.hashCode();
        return hash;
    }
}
