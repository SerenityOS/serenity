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
import jdk.test.lib.jittester.Literal;
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.ProductionParams;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.VariableDeclaration;
import jdk.test.lib.jittester.arrays.ArrayCreation;
import jdk.test.lib.jittester.types.TypeArray;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

class ArrayCreationFactory extends SafeFactory<ArrayCreation> {
    private final long complexityLimit;
    private final int operatorLimit;
    private final Type resultType;
    private final boolean exceptionSafe;
    private final boolean noconsts;
    private final TypeKlass ownerClass;

    ArrayCreationFactory(long complexityLimit, int operatorLimit,
            TypeKlass ownerClass, Type resultType, boolean exceptionSafe, boolean noconsts) {
        this.complexityLimit = complexityLimit;
        this.operatorLimit = operatorLimit;
        this.ownerClass = ownerClass;
        this.resultType = resultType;
        this.exceptionSafe = exceptionSafe;
        this.noconsts = noconsts;
    }

    @Override
    protected ArrayCreation sproduce() throws ProductionFailedException {
        if (resultType instanceof TypeArray) {
            TypeArray arrayResultType = (TypeArray) resultType;
            if (arrayResultType.type.equals(TypeList.VOID)) {
                arrayResultType = arrayResultType.produce();
            }
            IRNodeBuilder builder = new IRNodeBuilder()
                    .setComplexityLimit(complexityLimit)
                    .setOwnerKlass(ownerClass)
                    .setResultType(TypeList.BYTE)
                    .setExceptionSafe(exceptionSafe)
                    .setNoConsts(noconsts);
            double chanceExpression = ProductionParams.chanceExpressionIndex.value() / 100;
            ArrayList<IRNode> dims = new ArrayList<>(arrayResultType.dimensions);
            for (int i = 0; i < arrayResultType.dimensions; i++) {
                if (PseudoRandom.randomBoolean(chanceExpression)) {
                    dims.add(builder.setOperatorLimit((int) (PseudoRandom.random()
                                * operatorLimit / arrayResultType.dimensions))
                            .getExpressionFactory()
                            .produce());
                } else {
                    Literal dimension = builder.getLiteralFactory().produce();
                    while (Integer.valueOf(dimension.getValue().toString()) < 1) {
                        dimension = builder.getLiteralFactory().produce();
                    }
                    dims.add(dimension);
                }
            }
            VariableDeclaration var = builder
                    .setOwnerKlass(ownerClass)
                    .setResultType(arrayResultType)
                    .setIsLocal(true)
                    .setIsStatic(false)
                    .getVariableDeclarationFactory()
                    .produce();
            return new ArrayCreation(var, arrayResultType, dims);
        }
        throw new ProductionFailedException();
    }
}
