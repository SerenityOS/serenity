/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.util.Pair;
import jdk.test.lib.jittester.BinaryOperator;
import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.OperatorKind;
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.SymbolTable;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

abstract class BinaryOperatorFactory extends OperatorFactory<BinaryOperator> {
    protected final OperatorKind opKind;
    protected final Type resultType;
    protected final Type ownerClass;

    protected BinaryOperatorFactory(OperatorKind opKind, long complexityLimit, int operatorLimit,
            Type ownerClass, Type resultType, boolean exceptionSafe, boolean noconsts) {
        super(opKind.priority, complexityLimit, operatorLimit, exceptionSafe, noconsts);
        this.opKind = opKind;
        this.resultType = resultType;
        this.ownerClass = ownerClass;
    }

    protected abstract boolean isApplicable(Type resultType);

    protected abstract Pair<Type, Type> generateTypes();

    protected BinaryOperator generateProduction(Type leftType, Type rightType) throws ProductionFailedException {
        int leftOpLimit = (int) (PseudoRandom.random() * (operatorLimit - 1));
        int rightOpLimit = operatorLimit - 1 - leftOpLimit;
        long leftComplLimit = (long) (PseudoRandom.random() * (complexityLimit - 1));
        long rightComplLimit = complexityLimit - 1 - leftComplLimit;
        if (leftOpLimit == 0 || rightOpLimit == 0 || leftComplLimit == 0 || rightComplLimit == 0) {
            throw new ProductionFailedException();
        }
        boolean swap = PseudoRandom.randomBoolean();
        IRNodeBuilder builder = new IRNodeBuilder().setExceptionSafe(exceptionSafe)
                .setOwnerKlass((TypeKlass) ownerClass)
                .setNoConsts(!swap && noconsts);
        IRNode leftExpr = builder.setComplexityLimit(leftComplLimit)
                .setOperatorLimit(leftOpLimit)
                .setResultType(leftType)
                .getExpressionFactory()
                .produce();
        IRNode rightExpr = builder.setComplexityLimit(rightComplLimit)
                .setOperatorLimit(rightOpLimit)
                .setResultType(rightType)
                .getExpressionFactory()
                .produce();
        return new BinaryOperator(opKind, resultType, leftExpr, rightExpr);
    }

    @Override
    public final BinaryOperator produce() throws ProductionFailedException {
        if (!isApplicable(resultType)) {
            //avoid implicit use of resultType.toString()
            throw new ProductionFailedException("Type " + resultType.getName() + " is not applicable by " + getClass().getName());
        }

        Pair<Type, Type> types;
        try {
            types = generateTypes();
        } catch (RuntimeException ex) {
            throw new ProductionFailedException(ex.getMessage());
        }

        try {
            SymbolTable.push();
            BinaryOperator p = generateProduction(types.first, types.second);
            SymbolTable.merge();
            return p;
        } catch (ProductionFailedException e) {
            SymbolTable.pop();
            throw e;
        }
    }
}
