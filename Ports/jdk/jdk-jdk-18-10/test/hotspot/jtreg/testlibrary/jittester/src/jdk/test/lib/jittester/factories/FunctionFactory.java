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
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.Symbol;
import jdk.test.lib.jittester.SymbolTable;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.VariableInfo;
import jdk.test.lib.jittester.functions.Function;
import jdk.test.lib.jittester.functions.FunctionInfo;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

class FunctionFactory extends SafeFactory<Function> {
    private final FunctionInfo functionInfo;
    private final int operatorLimit;
    private final long complexityLimit;
    private final boolean exceptionSafe;
    private final TypeKlass ownerClass;

    FunctionFactory(long complexityLimit, int operatorLimit, TypeKlass ownerClass,
            Type resultType, boolean exceptionSafe) {
        functionInfo = new FunctionInfo();
        this.complexityLimit = complexityLimit;
        this.operatorLimit = operatorLimit;
        this.ownerClass = ownerClass;
        this.functionInfo.type = resultType;
        this.exceptionSafe = exceptionSafe;
    }

    @Override
    protected Function sproduce() throws ProductionFailedException {
        // Currently no function is exception-safe
        if (exceptionSafe) {
            throw new ProductionFailedException();
        }
        ArrayList<Symbol> allFunctions;
        if (functionInfo.type == null) {
            allFunctions = new ArrayList<>(SymbolTable.getAllCombined(FunctionInfo.class));
        } else {
            allFunctions = new ArrayList<>(SymbolTable.get(functionInfo.type, FunctionInfo.class));
        }
        if (!allFunctions.isEmpty()) {
            PseudoRandom.shuffle(allFunctions);
            Collection<TypeKlass> klassHierarchy = ownerClass.getAllParents();
            for (Symbol function : allFunctions) {
                FunctionInfo functionInfo = (FunctionInfo) function;
                // Don't try to construct abstract classes.
                if (functionInfo.isConstructor() && functionInfo.owner.isAbstract()) {
                    continue;
                }
                // We don't call methods from the same class which are not final, because if we
                // do this may produce an infinite recursion. Simple example:
                // class  A
                // {
                //     f1() { }
                //     f2() { f1(); }
                // }
                //
                // class B : A
                // {
                //    f1() { f2(); }
                // }
                //
                // However the same example is obviously safe for static and final functions
                // Also we introduce a special flag NONRECURSIVE to mark functions that
                // are not overrided. We may also call such functions.

                // If it's a local call.. or it's a call using some variable to some object of some type in our hierarchy
                boolean inHierarchy = false;
                if (ownerClass.equals(functionInfo.owner) || (inHierarchy = klassHierarchy.contains(functionInfo.owner))) {
                    if ((functionInfo.flags & FunctionInfo.FINAL) == 0 && (functionInfo.flags & FunctionInfo.STATIC) == 0
                            && (functionInfo.flags & FunctionInfo.NONRECURSIVE) == 0) {
                        continue;
                    }
                    if (inHierarchy && (functionInfo.flags & FunctionInfo.PRIVATE) > 0) {
                        continue;
                    }
                } else {
                    if ((functionInfo.flags & FunctionInfo.PUBLIC) == 0
                            && (functionInfo.flags & FunctionInfo.DEFAULT) == 0) {
                        continue;
                    }
                }
                if (functionInfo.complexity < complexityLimit - 1) {
                    try {
                        List<IRNode> accum = new ArrayList<>();
                        if (!functionInfo.argTypes.isEmpty()) {
                            // Here we should do some analysis here to determine if
                            // there are any conflicting functions due to possible
                            // constant folding.

                            // For example the following can be done:
                            // Scan all the hieirachy where the class is declared.
                            // If there are function with a same name and same number of args,
                            // then disable usage of foldable expressions in the args.
                            boolean noconsts = false;
                            Collection<Symbol> allFuncsInKlass = SymbolTable.getAllCombined(functionInfo.owner,
                                    FunctionInfo.class);
                            for (Symbol s2 : allFuncsInKlass) {
                                FunctionInfo i2 = (FunctionInfo) function;
                                if (!i2.equals(functionInfo) && i2.name.equals(functionInfo.name)
                                        && i2.argTypes.size() == functionInfo.argTypes.size()) {
                                    noconsts = true;
                                    break;
                                }
                            }
                            long argComp = (complexityLimit - 1 - functionInfo.complexity) / functionInfo.argTypes.size();
                            int argumentOperatorLimit = (operatorLimit - 1) / functionInfo.argTypes.size();
                            IRNodeBuilder b = new IRNodeBuilder().setOwnerKlass(ownerClass)
                                    .setComplexityLimit(argComp)
                                    .setOperatorLimit(argumentOperatorLimit)
                                    .setExceptionSafe(exceptionSafe)
                                    .setNoConsts(noconsts);
                            for (VariableInfo argType : functionInfo.argTypes) {
                                accum.add(b.setResultType(argType.type)
                                        .getExpressionFactory()
                                        .produce());
                            }
                        }
                        return new Function(ownerClass, functionInfo, accum);
                    } catch (ProductionFailedException e) {
                        // removeAllChildren();
                    }
                }
            }
        }
        throw new ProductionFailedException();
    }
}
