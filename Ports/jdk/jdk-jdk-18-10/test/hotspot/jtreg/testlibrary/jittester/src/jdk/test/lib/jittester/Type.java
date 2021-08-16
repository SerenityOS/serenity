/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 */

package jdk.test.lib.jittester;

import jdk.test.lib.jittester.visitors.Visitor;

/**
 * Type system's core..
 */
public abstract class Type extends IRNode implements Comparable<Type> {

    private final String typeName;

    protected Type(String typeName) {
        super(null);
        this.typeName = typeName;
    }

    @Override
    public Type getResultType() {
        return this;
    }

    @Override
    public boolean equals(Object t) {
        if (this == t) {
            return true;
        }
        if (t == null || !(t instanceof Type)) {
            return false;
        }
        return typeName.equals(((Type) t).typeName);
    }

    @Override
    public int compareTo(Type t) {
        return typeName.compareTo(t.typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    public abstract boolean canImplicitlyCastTo(Type t);

    public abstract boolean canExplicitlyCastTo(Type t);

    public abstract boolean canCompareTo(Type t);

    public abstract boolean canEquateTo(Type t);

    protected void exportSymbols() {
    }

    @Override
    public<T> T accept(Visitor<T> v) {
        return v.visit(this);
    }

    @Override
    public String getName() {
        return typeName;
    }
}
