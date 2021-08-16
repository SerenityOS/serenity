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

package jdk.test.lib.jittester.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.test.lib.jittester.ProductionParams;
import jdk.test.lib.jittester.SymbolTable;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.VariableInfo;
import jdk.test.lib.jittester.visitors.Visitor;
import jdk.test.lib.jittester.utils.PseudoRandom;

public class TypeArray extends TypeKlass {

    public List<Byte> getDims() {
        return dims;
    }

    public void setDimentions(List<Byte> dims) {
        this.dims = dims;
    }
    public final Type type;
    public final int dimensions;
    private List<Byte> dims = new ArrayList<>();

    public TypeArray(Type type, int dimensions) {
        super("Array", TypeKlass.FINAL);
        addParent(TypeList.OBJECT.getName());
        setParent(TypeList.OBJECT);
        this.type = type;
        this.dimensions = dimensions;
    }

    public String getName() {
        String dimString = Stream.generate(() -> "[]")
                .limit(dimensions)
                .collect(Collectors.joining());
        return type.getName() + dimString;
    }

    @Override
    protected void exportSymbols() {
        SymbolTable.add(new VariableInfo("length", this, TypeList.INT, VariableInfo.PUBLIC | VariableInfo.FINAL));
    }

    @Override
    public boolean equals(Object t) {
        if (this == t) {
            return true;
        }
        if (t == null || !(t instanceof TypeArray)) {
            return false;
        }

        if (super.equals(t)) { // make sure we're compating to an array
            try {
                TypeArray a = (TypeArray) t;
                return a.type.equals(type) && (a.dimensions == dimensions
                        || a.dimensions == -1
                        || dimensions == -1);
            } catch (Exception e) {
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + Objects.hashCode(this.type);
        hash = 313 * hash + this.dimensions;
        return hash;
    }

    @Override
    public int compareTo(Type t) {
        int r = super.compareTo(t);
        if (r == 0) {
            try {
                TypeArray a = (TypeArray) t;
                r = type.compareTo(t);
                if (r == 0) {
                    r = dimensions - a.dimensions;
                }
            } catch (Exception e) {
            }
        }

        return r;
    }

    public TypeArray produce() {
        ArrayList<Type> all = new ArrayList<>(TypeList.getAll());
        PseudoRandom.shuffle(all);
        for (Type t : all) {
            if (t instanceof TypeArray) {
                continue;
            }
            int dims = PseudoRandom.randomNotZero(ProductionParams.dimensionsLimit.value());
            return new TypeArray(t, dims);
        }
        throw new Error("Shouldn't happen");
    }

    @Override
    public<T> T accept(Visitor<T> v) {
        return v.visit(this);
    }

    public Type getType() {
        return type;
    }

    public int getDimensions() {
        return dimensions;
    }
}
