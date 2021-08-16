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

import java.util.Collection;
import java.util.List;
import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.Symbol;
import jdk.test.lib.jittester.SymbolTable;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.visitors.Visitor;


public class Function extends IRNode {
    private FunctionInfo functionInfo = new FunctionInfo();

    public Function(TypeKlass ownerClass, FunctionInfo functionInfo, List<IRNode> args) {
        super(functionInfo.type);
        setOwner(ownerClass);
        this.functionInfo = functionInfo;
        addChildren(args);
    }

    @Override
    public long complexity() {
        int argsComplexity = 0;
        for (IRNode child : getChildren()) {
            argsComplexity += child.complexity();
        }
        long funcComplexity = functionInfo.complexity;
        TypeKlass typeKlass = this.owner;
        if (functionInfo.isConstructor()) {
            // Sum complexities of all default constructors of parent classes
            for (TypeKlass parent : typeKlass.getAllParents()) {
                Collection<Symbol> parentFuncs = SymbolTable.getAllCombined(parent, FunctionInfo.class);
                for (Symbol f : parentFuncs) {
                    FunctionInfo c = (FunctionInfo) f;
                    if (c.name.equals(c.owner.getName()) && c.argTypes.isEmpty()) {
                        funcComplexity += c.complexity;
                    }
                }
            }
            // TODO: Complexities of all non-static initializers should be also added..
        } else {
            // Perform the CHA and find the highest complexity
            for (TypeKlass child : typeKlass.getAllChildren()) {
                Collection<Symbol> childFuncs = SymbolTable.getAllCombined(child, FunctionInfo.class);
                for (Symbol childFunc : childFuncs) {
                    if (childFunc.equals(functionInfo)) {
                        funcComplexity = Math.max(funcComplexity, ((FunctionInfo) childFunc).complexity);
                    }
                }
            }
        }
        return argsComplexity + funcComplexity;
    }

    @Override
    public<T> T accept(Visitor<T> v) {
        return v.visit(this);
    }

    public FunctionInfo getValue() {
        return functionInfo;
    }
}
