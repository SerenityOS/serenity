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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Thomas Wuerthinger
 */
public abstract class Event<L> {

    private List<L> listener;
    private boolean fireEvents;
    private boolean eventWasFired;

    public Event() {
        listener = new ArrayList<>();
        fireEvents = true;
    }

    public void addListener(L l) {
        listener.add(l);
    }

    /**
     * Remove listener
     * @param l
     */
    public void removeListener(final L l) {
        listener.remove(l);
    }

    public void fire() {
        if(fireEvents) {
            List<L> tmpList = new ArrayList<>(listener);
            for (L l : tmpList) {
                fire(l);
            }
        } else {
            eventWasFired = true;
        }
    }

    public void beginAtomic() {
        assert fireEvents : "endAtomic has to be called before another beginAtomic may be called";
        this.fireEvents = false;
        this.eventWasFired = false;
    }

    public void endAtomic() {
        assert !fireEvents : "beginAtomic has to be called first";
        this.fireEvents = true;
        if(eventWasFired) {
            fire();
        }
    }

    protected abstract void fire(L l);
}
