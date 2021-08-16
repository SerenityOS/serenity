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
import java.util.List;

import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.Nothing;
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.Symbol;
import jdk.test.lib.jittester.SymbolTable;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.VariableInfo;
import jdk.test.lib.jittester.functions.ArgumentDeclaration;
import jdk.test.lib.jittester.functions.FunctionDefinition;
import jdk.test.lib.jittester.functions.FunctionInfo;
import jdk.test.lib.jittester.functions.Return;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

class FunctionDefinitionFactory extends Factory<FunctionDefinition> {
    private final Type resultType;
    private final String name;
    private final long complexityLimit;
    private final int statementLimit;
    private final int operatorLimit;
    private final int memberFunctionsArgLimit;
    private final int flags;
    private final int level;
    private final TypeKlass ownerClass;

    FunctionDefinitionFactory(String name, TypeKlass ownerClass, Type resultType,
            long complexityLimit, int statementLimit, int operatorLimit,
            int memberFunctionsArgLimit, int level, int flags) {
        this.name = name;
        this.ownerClass = ownerClass;
        this.resultType = resultType;
        this.complexityLimit = complexityLimit;
        this.statementLimit = statementLimit;
        this.operatorLimit = operatorLimit;
        this.memberFunctionsArgLimit = memberFunctionsArgLimit;
        this.level = level;
        this.flags = flags;
    }

    @Override
    public FunctionDefinition produce() throws ProductionFailedException {
        Type resType = resultType;
        if (resType == null) {
            List<Type> types = new ArrayList<>(TypeList.getAll());
            types.add(TypeList.VOID);
            resType = PseudoRandom.randomElement(types);
        }
        int argNumber = (int) (PseudoRandom.random() * memberFunctionsArgLimit);
        ArrayList<VariableInfo> argumentsInfo;
        if ((flags & FunctionInfo.STATIC) > 0) {
            argumentsInfo = new ArrayList<>(argNumber);
        } else {
            argumentsInfo = new ArrayList<>(argNumber + 1);
            argumentsInfo.add(new VariableInfo("this", ownerClass, ownerClass,
                    VariableInfo.FINAL | VariableInfo.LOCAL | VariableInfo.INITIALIZED));
        }
        ArrayList<ArgumentDeclaration> argumentsDeclaration = new ArrayList<>(argNumber);
        SymbolTable.push();
        IRNode body;
        Return returnNode;
        FunctionInfo functionInfo;
        try {
            IRNodeBuilder builder = new IRNodeBuilder().setArgumentType(ownerClass);
            int i = 0;
            for (; i < argNumber; i++) {
                ArgumentDeclaration d = builder.setVariableNumber(i).getArgumentDeclarationFactory()
                        .produce();
                argumentsDeclaration.add(d);
                argumentsInfo.add(d.variableInfo);
            }
            Collection<Symbol> thisKlassFuncs = SymbolTable.getAllCombined(ownerClass,
                    FunctionInfo.class);
            Collection<Symbol> parentFuncs = FunctionDefinition.getFuncsFromParents(ownerClass);
            while (true) {
                functionInfo = new FunctionInfo(name, ownerClass, resType, 0, flags,
                        argumentsInfo);
                if (thisKlassFuncs.contains(functionInfo)
                        || FunctionDefinition.isInvalidOverride(functionInfo, parentFuncs)) {
                    // try changing the signature, and go checking again.
                    ArgumentDeclaration argDecl = builder.setVariableNumber(i++)
                            .getArgumentDeclarationFactory().produce();
                    argumentsDeclaration.add(argDecl);
                    argumentsInfo.add(argDecl.variableInfo);
                } else {
                    break;
                }
            }
            long blockComplLimit = (long) (PseudoRandom.random() * complexityLimit);
            body = builder.setOwnerKlass(ownerClass)
                    .setResultType(resType)
                    .setComplexityLimit(blockComplLimit)
                    .setStatementLimit(statementLimit)
                    .setOperatorLimit(operatorLimit)
                    .setLevel(level)
                    .setSubBlock(true)
                    .setCanHaveBreaks(false)
                    .setCanHaveContinues(false)
                    .setCanHaveReturn(true)
                    .getBlockFactory()
                    .produce();
            if (!resType.equals(TypeList.VOID)) {
                returnNode = builder.setComplexityLimit(complexityLimit - blockComplLimit)
                        .setExceptionSafe(false)
                        .getReturnFactory()
                        .produce();
            } else {
                returnNode = new Return(new Nothing());
            }
        } finally {
            SymbolTable.pop();
        }
        // addChildren(argumentsDeclaration); // not neccessary while complexity() doesn't use it
        functionInfo = new FunctionInfo(name, ownerClass, resType, body == null ? 0 : body.complexity(),
                flags, argumentsInfo);
        // If it's all ok, add the function to the symbol table.
        SymbolTable.add(functionInfo);
        return new FunctionDefinition(functionInfo, argumentsDeclaration, body, returnNode);
    }
}
