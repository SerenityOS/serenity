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

package jdk.test.lib.jittester.functions;

import java.util.ArrayList;
import java.util.Arrays;

import jdk.test.lib.jittester.Symbol;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.VariableInfo;
import jdk.test.lib.jittester.types.TypeKlass;

public class FunctionInfo extends Symbol {
    public ArrayList<VariableInfo> argTypes;
    public long complexity = 0;
    public static final int ABSTRACT = 0x40;
    public static final int NONRECURSIVE = 0x80;
    public static final int SYNCHRONIZED = 0x100;

    public FunctionInfo() {
    }

    public FunctionInfo(String name, TypeKlass ownerClass, Type returnType,
            long complexity, int flags, VariableInfo... args) {
        super(name, ownerClass, returnType, flags);
        argTypes = new ArrayList<>();
        argTypes.addAll(Arrays.asList(args));
        this.complexity = complexity;
    }

    public FunctionInfo(String name, TypeKlass ownerClass, Type returnType,
            long complexity, int flags, ArrayList<VariableInfo> args) {
        super(name, ownerClass, returnType, flags);
        argTypes = args;
        this.complexity = complexity;
    }

    public FunctionInfo(FunctionInfo value) {
        super(value);
        argTypes = new ArrayList<>();
        for (VariableInfo i : value.argTypes) {
            argTypes.add(new VariableInfo(i));
        }
        complexity = value.complexity;
    }

    public boolean isSynchronized() {
        return (flags & SYNCHRONIZED) > 0;
    }

    @Override
    protected Symbol copy() {
        return this;
    }

    @Override
    public Symbol deepCopy() {
        return new FunctionInfo(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof FunctionInfo)) {
            return false;
        }

        try {
            FunctionInfo f = (FunctionInfo) o;
            return owner.equals(f.owner) && hasEqualSignature(o);
        } catch (Exception e) {
        }
        return false;
    }

    protected boolean hasEqualSignature(Object o) {
        try {
            FunctionInfo f = (FunctionInfo) o;
            if (name.equals(f.name)) {
                int i = (flags & STATIC) > 0 ? 0 : 1;
                int j = (f.flags & STATIC) > 0 ? 0 : 1;

                if (argTypes.size() - i == f.argTypes.size() - j) {
                    while (i < argTypes.size() && j < f.argTypes.size()) {
                        if (!argTypes.get(i++).type.equals(f.argTypes.get(j++).type)) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    public boolean isConstructor() {
        return name.equals(owner.getName());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean isStatic() {
        return (flags & STATIC) > 0;
    }
}
