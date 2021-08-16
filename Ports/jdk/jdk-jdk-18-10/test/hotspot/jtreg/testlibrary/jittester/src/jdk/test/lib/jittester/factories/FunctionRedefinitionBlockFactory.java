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

import java.util.ArrayList;
import java.util.Collection;
import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.Symbol;
import jdk.test.lib.jittester.SymbolTable;
import jdk.test.lib.jittester.VariableInfo;
import jdk.test.lib.jittester.functions.FunctionInfo;
import jdk.test.lib.jittester.functions.FunctionRedefinitionBlock;
import jdk.test.lib.jittester.types.TypeKlass;

class FunctionRedefinitionBlockFactory extends Factory<FunctionRedefinitionBlock> {
    private final int statementLimit;
    private final int operatorLimit;
    private final long complexityLimit;
    private final int level;
    private final TypeKlass ownerClass;
    private final Collection<Symbol> functionSet;

    FunctionRedefinitionBlockFactory(Collection<Symbol> functionSet, TypeKlass ownerClass,
            long complexityLimit, int statementLimit, int operatorLimit, int level) {
        this.functionSet = functionSet;
        this.ownerClass = ownerClass;
        this.complexityLimit = complexityLimit;
        this.statementLimit = statementLimit;
        this.operatorLimit = operatorLimit;
        this.level = level;
    }

    @Override
    public FunctionRedefinitionBlock produce() throws ProductionFailedException {
        ArrayList<IRNode> content = new ArrayList<>();
        if (functionSet.size() > 0) {
            long funcComplexity = complexityLimit / functionSet.size();
            IRNodeBuilder builder = new IRNodeBuilder().setOwnerKlass(ownerClass)
                    .setComplexityLimit(funcComplexity)
                    .setStatementLimit(statementLimit)
                    .setOperatorLimit(operatorLimit)
                    .setLevel(level);
            for (Symbol symbol : functionSet) {
                FunctionInfo functionInfo = (FunctionInfo) symbol;
                Symbol thisSymbol = null;
                if ((functionInfo.flags & FunctionInfo.STATIC) > 0) {
                    thisSymbol = SymbolTable.get("this", VariableInfo.class);
                    SymbolTable.remove(thisSymbol);
                }
                try {
                    content.add(builder.setFunctionInfo(functionInfo)
                            .setFlags(functionInfo.flags)
                            .getFunctionRedefinitionFactory()
                            .produce());
                } catch (ProductionFailedException e) {
                    if ((functionInfo.flags & FunctionInfo.STATIC) == 0) {
                        ownerClass.setAbstract();
                    }
                }
                if ((functionInfo.flags & FunctionInfo.STATIC) > 0) {
                    SymbolTable.add(thisSymbol);
                }
            }
        }
        return new FunctionRedefinitionBlock(content, level);
    }
}
