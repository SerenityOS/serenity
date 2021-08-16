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
import jdk.test.lib.jittester.ProductionParams;
import jdk.test.lib.jittester.Symbol;
import jdk.test.lib.jittester.SymbolTable;
import jdk.test.lib.jittester.VariableInfo;
import jdk.test.lib.jittester.functions.FunctionDefinitionBlock;
import jdk.test.lib.jittester.functions.FunctionInfo;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

class FunctionDefinitionBlockFactory extends Factory<FunctionDefinitionBlock> {
    private final long complexityLimit;
    private final int statementLimit;
    private final int operatorLimit;
    private final int memberFunctionsLimit;
    private final int memberFunctionsArgLimit;
    private final int initialFlags;
    private final int level;
    private final TypeKlass ownerClass;

    FunctionDefinitionBlockFactory(TypeKlass ownerClass, int memberFunctionsLimit,
            int memberFunctionsArgLimit, long complexityLimit, int statementLimit,
            int operatorLimit, int level, int initialFlags) {
        this.ownerClass = ownerClass;
        this.memberFunctionsLimit = memberFunctionsLimit;
        this.memberFunctionsArgLimit = memberFunctionsArgLimit;
        this.complexityLimit = complexityLimit;
        this.statementLimit = statementLimit;
        this.operatorLimit = operatorLimit;
        this.level = level;
        this.initialFlags = initialFlags;
    }

    @Override
    public FunctionDefinitionBlock produce() throws ProductionFailedException {
        ArrayList<IRNode> content = new ArrayList<>();
        int memFunLimit = (int) (PseudoRandom.random() * memberFunctionsLimit);
        if (memFunLimit > 0) {
            long memFunCompl = complexityLimit / memFunLimit;
            IRNodeBuilder builder = new IRNodeBuilder().setOwnerKlass(ownerClass)
                    .setComplexityLimit(memFunCompl)
                    .setStatementLimit(statementLimit)
                    .setOperatorLimit(operatorLimit)
                    .setMemberFunctionsArgLimit(memberFunctionsArgLimit)
                    .setLevel(level);
            for (int i = 0; i < memFunLimit; i++) {
                int flags = initialFlags;
                if (PseudoRandom.randomBoolean()) {
                    flags |= FunctionInfo.STATIC;
                }
                if (!ProductionParams.disableFinalMethods.value() && PseudoRandom.randomBoolean()) {
                    flags |= FunctionInfo.FINAL;
                }
                if (PseudoRandom.randomBoolean()) {
                    flags |= FunctionInfo.NONRECURSIVE;
                }
                if (PseudoRandom.randomBoolean()) {
                    flags |= FunctionInfo.SYNCHRONIZED;
                }
                switch ((int) (PseudoRandom.random() * 4)) {
                    case 0:
                        flags |= FunctionInfo.PRIVATE;
                        break;
                    case 1:
                        flags |= FunctionInfo.PROTECTED;
                        break;
                    case 2:
                        flags |= FunctionInfo.DEFAULT;
                        break;
                    case 3:
                        flags |= FunctionInfo.PUBLIC;
                        break;
                }
                Symbol thisSymbol = null;
                if ((flags & FunctionInfo.STATIC) > 0) {
                    thisSymbol = SymbolTable.get("this", VariableInfo.class);
                    SymbolTable.remove(thisSymbol);
                }
                try {
                    content.add(builder.setName("func_" + i)
                            .setFlags(flags)
                            .getFunctionDefinitionFactory()
                            .produce());
                } catch (ProductionFailedException e) {
                }
                if ((flags & FunctionInfo.STATIC) > 0) {
                    SymbolTable.add(thisSymbol);
                }
            }
        }
        return new FunctionDefinitionBlock(content, level, ownerClass);
    }
}
