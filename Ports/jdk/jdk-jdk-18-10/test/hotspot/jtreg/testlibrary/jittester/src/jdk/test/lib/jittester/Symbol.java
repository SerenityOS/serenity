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

import jdk.test.lib.jittester.types.TypeKlass;

public class Symbol {

    public String name;
    public Type type;
    public TypeKlass owner;
    public static final int NONE = 0x00;
    public static final int PRIVATE = 0x01;
    public static final int DEFAULT = 0x02;
    public static final int PROTECTED = 0x04;
    public static final int PUBLIC = 0x08;
    public static final int ACCESS_ATTRS_MASK = PRIVATE + PROTECTED + DEFAULT + PUBLIC;
    public static final int STATIC = 0x10;
    public static final int FINAL = 0x20;
    public int flags = NONE;

    protected Symbol() {
    }

    protected Symbol(String name) {
        this.name = name;
    }

    public Symbol(String name, TypeKlass owner, Type type, int flags) {
        this.name = name;
        this.owner = owner;
        this.type = type;
        this.flags = flags;
    }

    protected Symbol(Symbol value) {
        this.name = value.name;
        this.owner = value.owner;
        this.type = value.type;
        this.flags = value.flags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof Symbol)) {
            return false;
        }
        try {
            Symbol s = (Symbol) o;
            return owner.equals(s.owner) && name.equals(s.name);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }


    public boolean isStatic() {
        return (flags & STATIC) > 0;
    }

    public boolean isFinal() {
        return (flags & FINAL) > 0;
    }

    public boolean isPublic() {
        return (flags & PUBLIC) > 0;
    }

    public boolean isProtected() {
        return (flags & PROTECTED) > 0;
    }

    public boolean isPrivate() {
        return (flags & PRIVATE) > 0;
    }

    protected Symbol copy() {
        return new Symbol(this);
    }

    public Symbol deepCopy() {
        return new Symbol(this);
    }


    public TypeKlass getOwner() {
        return owner;
    }
}
