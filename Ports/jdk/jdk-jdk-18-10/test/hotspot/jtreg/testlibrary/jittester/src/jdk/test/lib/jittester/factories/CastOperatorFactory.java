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
import jdk.test.lib.jittester.CastOperator;
import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.SymbolTable;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

class CastOperatorFactory extends OperatorFactory<CastOperator> {
    private final Type resultType;
    private final Type ownerClass;

    CastOperatorFactory(long complexityLimit, int operatorLimit, TypeKlass ownerClass,
            Type resultType, boolean exceptionSafe, boolean noconsts) {
        super(13, complexityLimit, operatorLimit, exceptionSafe, noconsts);
        this.resultType = resultType;
        this.ownerClass = ownerClass;
    }

    @Override
    public CastOperator produce() throws ProductionFailedException {
        ArrayList<Type> argType = new ArrayList<>(TypeList.getAll());
        PseudoRandom.shuffle(argType);
        for (Type type : argType) {
            try {
                Factory<IRNode> expressionFactory = new IRNodeBuilder()
                        .setComplexityLimit(complexityLimit - 1)
                        .setOperatorLimit(operatorLimit - 1)
                        .setOwnerKlass((TypeKlass) ownerClass)
                        .setExceptionSafe(exceptionSafe)
                        .setNoConsts(noconsts)
                        .setResultType(type)
                        .getExpressionFactory();
                SymbolTable.push();
                if (type.equals(resultType) ||
                        ((!exceptionSafe || exceptionSafe && !(type instanceof TypeKlass))
                            && type.canExplicitlyCastTo(resultType))) {
                    // In safe mode we cannot explicitly cast an object, because it may throw.
                    CastOperator castOperator = new CastOperator(resultType, expressionFactory.produce());
                    SymbolTable.merge();
                    return castOperator;
                }
                SymbolTable.pop();
            } catch (ProductionFailedException e) {
                SymbolTable.pop();
            }
        }
        throw new ProductionFailedException();
    }
}
