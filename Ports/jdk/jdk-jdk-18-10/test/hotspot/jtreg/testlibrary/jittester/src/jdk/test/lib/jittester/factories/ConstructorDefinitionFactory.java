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
import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.Symbol;
import jdk.test.lib.jittester.SymbolTable;
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.VariableInfo;
import jdk.test.lib.jittester.functions.ArgumentDeclaration;
import jdk.test.lib.jittester.functions.ConstructorDefinition;
import jdk.test.lib.jittester.functions.FunctionInfo;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

class ConstructorDefinitionFactory extends Factory<ConstructorDefinition> {
    private final long complexityLimit;
    private final int statementLimit;
    private final int operatorLimit;
    private final int memberFunctionsArgLimit;
    private final int level;
    private final TypeKlass ownerClass;

    ConstructorDefinitionFactory(TypeKlass ownerClass, long complexityLimit, int statementLimit,
            int operatorLimit, int memberFunctionsArgLimit, int level) {
        this.ownerClass = ownerClass;
        this.complexityLimit = complexityLimit;
        this.statementLimit = statementLimit;
        this.operatorLimit = operatorLimit;
        this.memberFunctionsArgLimit = memberFunctionsArgLimit;
        this.level = level;
    }

    @Override
    public ConstructorDefinition produce() throws ProductionFailedException {
        int argNumber = (int) (PseudoRandom.random() * memberFunctionsArgLimit);
        ArrayList<VariableInfo> argumentsInfo = new ArrayList<>(argNumber);
        ArrayList<ArgumentDeclaration> argumentsDeclaration = new ArrayList<>(argNumber);
        SymbolTable.push();
        IRNode body;
        FunctionInfo functionInfo;
        try {
            int i = 0;
            IRNodeBuilder builder = new IRNodeBuilder().setArgumentType(ownerClass).setOwnerKlass(ownerClass);
            for (; i < argNumber; i++) {
                ArgumentDeclaration d = builder.setVariableNumber(i).getArgumentDeclarationFactory()
                        .produce();
                argumentsDeclaration.add(d);
                argumentsInfo.add(d.variableInfo);
            }
            for (boolean dup = true; dup; i++) {
                /* Check if these is a function with a same signature
                (includes original class name) defined. */
                functionInfo = new FunctionInfo(ownerClass.getName(), ownerClass,
                        ownerClass, 0, FunctionInfo.PUBLIC, argumentsInfo);
                dup = false;
                for (Symbol symbol : SymbolTable.get(ownerClass, FunctionInfo.class)) {
                    if (functionInfo.equals(symbol)) {
                        ArgumentDeclaration argDecl = builder.setVariableNumber(i)
                                .getArgumentDeclarationFactory().produce();
                        argumentsDeclaration.add(argDecl);
                        argumentsInfo.add(argDecl.variableInfo);
                        dup = true;
                        break;
                    }
                }
            }
            long blockComplLimit = (long) (PseudoRandom.random() * complexityLimit);
            try {
                body = builder.setResultType(TypeList.VOID)
                        .setComplexityLimit(blockComplLimit)
                        .setStatementLimit(statementLimit)
                        .setOperatorLimit(operatorLimit)
                        .setLevel(level)
                        .setSubBlock(true)
                        .getBlockFactory()
                        .produce();
            } catch (ProductionFailedException e) {
                body = null;
            }
        } finally {
            SymbolTable.pop();
        }
        functionInfo = new FunctionInfo(ownerClass.getName(), ownerClass, ownerClass,
                body != null ? body.complexity() : 0, FunctionInfo.PUBLIC, argumentsInfo);
        // If it's all ok, add the function to the symbol table.
        SymbolTable.add(functionInfo);
        return new ConstructorDefinition(functionInfo, argumentsDeclaration, body);
    }
}
