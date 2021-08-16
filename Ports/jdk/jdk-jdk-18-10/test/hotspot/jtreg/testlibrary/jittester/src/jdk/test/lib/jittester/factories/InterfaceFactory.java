/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.jittester.factories;

import java.util.Iterator;
import java.util.LinkedList;
import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.ProductionParams;
import jdk.test.lib.jittester.Symbol;
import jdk.test.lib.jittester.SymbolTable;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.classes.Interface;
import jdk.test.lib.jittester.functions.FunctionInfo;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

class InterfaceFactory extends Factory<Interface> {
    private final String name;
    private final int memberFunctionsLimit;
    private final int memberFunctionsArgLimit;
    private final int level;
    private TypeKlass parent = null;

    InterfaceFactory(String name, int memberFunctionsLimit, int memberFuncArgLimit, int lvl) {
        this.name = name;
        this.memberFunctionsLimit = memberFunctionsLimit;
        this.memberFunctionsArgLimit = memberFuncArgLimit;
        this.level = lvl;
    }

    @Override
    public Interface produce() throws ProductionFailedException {
        TypeKlass thisKlass;
        // Do we want to inherit something?
        if (!ProductionParams.disableInheritance.value()) {
            // Grab all Klasses from the TypeList and select one to be a parent
            LinkedList<Type> types = new LinkedList<>(TypeList.getAll());
            for (Iterator<Type> i = types.iterator(); i.hasNext();) {
                Type klass = i.next();
                if (!(klass instanceof TypeKlass) || !((TypeKlass) klass).isInterface()) {
                    i.remove();
                }
            }
            PseudoRandom.shuffle(types);
            if (!types.isEmpty()) {
                parent = (TypeKlass) types.getFirst();
            }
        }
        thisKlass = new TypeKlass(name, TypeKlass.INTERFACE);
        if (parent != null) {
            thisKlass.addParent(parent.getName());
            thisKlass.setParent(parent);
            parent.addChild(name);
            for (Symbol symbol : SymbolTable.getAllCombined(parent, FunctionInfo.class)) {
                FunctionInfo functionInfo = (FunctionInfo) symbol.deepCopy();
                functionInfo.owner = thisKlass;
                functionInfo.argTypes.get(0).type = thisKlass;
                SymbolTable.add(functionInfo);
            }
        }
        IRNode functionDeclarations = null;
        try {
            functionDeclarations = new IRNodeBuilder().setOwnerKlass(thisKlass)
                    .setMemberFunctionsLimit(memberFunctionsLimit)
                    .setMemberFunctionsArgLimit(memberFunctionsArgLimit)
                    .setLevel(level + 1)
                    .getFunctionDeclarationBlockFactory()
                    .produce();
        } finally {
            TypeList.add(thisKlass);
        }
        return new Interface(parent, name, level, functionDeclarations);
    }
}
