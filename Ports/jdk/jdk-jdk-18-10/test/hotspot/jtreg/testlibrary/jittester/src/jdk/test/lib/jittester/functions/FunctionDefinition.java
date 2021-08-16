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
import java.util.LinkedList;
import java.util.List;

import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.Symbol;
import jdk.test.lib.jittester.SymbolTable;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.visitors.Visitor;

public class FunctionDefinition extends IRNode {
    private final FunctionInfo functionInfo;

    public FunctionDefinition(FunctionInfo functionInfo,
                              List<? extends ArgumentDeclaration> argumentsDeclaration, IRNode body, Return ret) {
        super(functionInfo.type);
        this.functionInfo = functionInfo;
        this.owner = functionInfo.owner;
        addChild(body);
        addChild(ret);
        addChildren(argumentsDeclaration);
    }

    // get the list of all functions from all parents of the given class.
    public static Collection<Symbol> getFuncsFromParents(TypeKlass typeKlass) {
        LinkedList<Symbol> result = new LinkedList<>();
        for (TypeKlass parent : typeKlass.getAllParents()) {
            result.addAll(SymbolTable.getAllCombined(parent, FunctionInfo.class));
        }
        return result;
    }

    // Check if the given function prototype f1 is a valid overload of
    // prototypes in collection S.
    // The override is invalid if function f1 has the same signature as
    // function f2 in S, but has different return type.
    public static boolean isInvalidOverride(FunctionInfo f1, Collection<Symbol> symbols) {
        for (Symbol symbol : symbols) {
            FunctionInfo f2 = (FunctionInfo) symbol;
            if (f1.hasEqualSignature(f2)) {
                if (!f1.type.equals(f2.type)) {
                    return true;
                }
                if ((f2.flags & FunctionInfo.NONRECURSIVE) > 0
                        || ((f1.flags & FunctionInfo.ABSTRACT) > 0 && (f2.flags & FunctionInfo.ABSTRACT) == 0)
                        || (f1.flags & FunctionInfo.STATIC) != (f2.flags & FunctionInfo.STATIC)
                        || (f2.flags & FunctionInfo.FINAL) > 0
                        || (f1.flags & FunctionInfo.ACCESS_ATTRS_MASK) < (f2.flags & FunctionInfo.ACCESS_ATTRS_MASK)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public long complexity() {
        IRNode body = getChild(0);
        IRNode ret = getChild(1);
        return body.complexity() + (ret != null ? ret.complexity() : 0);
    }

    @Override
    public<T> T accept(Visitor<T> v) {
        return v.visit(this);
    }

    public FunctionInfo getFunctionInfo() {
        return functionInfo;
    }
}
