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
public class Pair<L, R> {

    private L l;
    private R r;

    public Pair() {
    }

    public Pair(L l, R r) {
        this.l = l;
        this.r = r;
    }

    public L getLeft() {
        return l;
    }

    public void setLeft(L l) {
        this.l = l;
    }

    public R getRight() {
        return r;
    }

    public void setRight(R r) {
        this.r = r;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Pair)) {
            return false;
        }
        Pair<?,?> obj = (Pair<?,?>) o;
        boolean b1 = (l == obj.l);
        if (l != null) {
            b1 = l.equals(obj.l);
        }

        boolean b2 = (r == obj.r);
        if (r != null) {
            b2 = r.equals(obj.r);
        }

        return b1 && b2;
    }

    @Override
    public int hashCode() {
        return ((l == null) ? 0 : l.hashCode()) * 71 + ((r == null) ? 0 : r.hashCode());
    }

    @Override
    public String toString() {
        return "[" + l + "/" + r + "]";
    }
}
