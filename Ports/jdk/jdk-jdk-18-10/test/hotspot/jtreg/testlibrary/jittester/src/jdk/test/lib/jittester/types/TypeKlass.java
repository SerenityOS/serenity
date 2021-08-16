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

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.TreeSet;
import jdk.test.lib.jittester.ProductionParams;
import jdk.test.lib.jittester.Symbol;
import jdk.test.lib.jittester.SymbolTable;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.TypeList;

public class TypeKlass extends Type {
    private TypeKlass parentKlass;
    private final HashSet<String> parentsList;
    private final HashSet<String> childrenList;
    private final HashSet<Symbol> symbolsSet;
    private int flags;

    public static final int NONE = 0x00;
    public static final int FINAL = 0x01;
    public static final int INTERFACE = 0x02;
    public static final int ABSTRACT = 0x04;


    public TypeKlass(String name) {
        this(name, NONE);
    }

    public TypeKlass(String name, int flags) {
        super(name);
        this.flags = flags;
        parentsList = new HashSet<>();
        childrenList = new HashSet<>();
        symbolsSet = new HashSet<>();
    }

    public boolean addSymbol(Symbol s) {
        return symbolsSet.add(s);
    }

    public boolean addAllSymbols(Collection<? extends Symbol> symbols) {
        return symbolsSet.addAll(symbols);
    }

    public boolean containsSymbol(Symbol s) {
        return symbolsSet.contains(s);
    }

    public boolean removeSymbol(Symbol s) {
        return symbolsSet.remove(s);
    }

    public boolean removeAllSymbols(Collection<? extends Symbol> symbols) {
        return symbolsSet.removeAll(symbols);
    }

    @Override
    protected void exportSymbols() {
        symbolsSet.stream().forEach(SymbolTable::add);
    }

    public void setParent(TypeKlass p) {
        parentKlass = p;
    }

    public void addParent(String p) {
        parentsList.add(p);
    }

    public void addChild(String c) {
        childrenList.add(c);
    }

    protected void removeParent(String p) {
        parentsList.remove(p);
    }

    protected void removeChild(String c) {
        childrenList.remove(c);
    }

    public HashSet<String> getParentsNames() {
        return parentsList;
    }

    public HashSet<String> getChildrenNames() {
        return childrenList;
    }

    @Override
    public boolean canCompareTo(Type t) {
        return false;
    }

    @Override
    public boolean canEquateTo(Type t) {
        return true;
    }

    public TreeSet<TypeKlass> getAllParents() {
        TreeSet<TypeKlass> result = new TreeSet<>();
        parentsList.stream()
                .map(TypeList::find)
                .filter(Objects::nonNull)
                .map(k -> (TypeKlass) k)
                .forEach(k -> {
                    if (result.add(k)) {
                        result.addAll(k.getAllParents());
                    }
        });
        return result;
    }

    public TreeSet<TypeKlass> getAllChildren() {
        TreeSet<TypeKlass> result = new TreeSet<>();
        childrenList.stream()
                .map(TypeList::find)
                .filter(Objects::nonNull)
                .map(k -> (TypeKlass) k)
                .forEach(k -> {
                    if (result.add(k)) {
                        result.addAll(k.getAllChildren());
                    }
        });
        return result;
    }

    @Override
    public boolean canImplicitlyCastTo(Type t) {
        // We can implicitly cast to anything up the hierarchy and to self
        if (t instanceof TypeKlass) {
            return equals(t) || getAllParents().contains(t);
        }
        return false;
    }

    // If canExplicitlyCastTo() returns true in this case it doesn't mean that
    // it would really be successful. Since explicit casts are inherintly dynamic
    // we cannot guarantee that no exception will occur.
    @Override
    public boolean canExplicitlyCastTo(Type t) {
        if (equals(t)) {
            return true;
        }
        if (t instanceof TypeKlass && !ProductionParams.disableDowncasts.value()) {
            return getAllChildren().contains(t);
        }

        return false;
    }

    public boolean isFinal() {
        return (flags & FINAL) > 0;
    }

    public void setFinal() {
        flags |= FINAL;
    }

    public boolean isAbstract() {
        return (flags & ABSTRACT) > 0;
    }

    public void setAbstract() {
        flags |= ABSTRACT;
    }

    public boolean isInterface() {
        return (flags & INTERFACE) > 0;
    }

    public TypeKlass getParent() {
        return parentKlass;
    }
}
