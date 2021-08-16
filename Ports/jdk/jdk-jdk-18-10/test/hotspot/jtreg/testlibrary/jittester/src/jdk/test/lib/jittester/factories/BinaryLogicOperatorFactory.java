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
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

public class BinaryLogicOperatorFactory extends BinaryOperatorFactory {
    BinaryLogicOperatorFactory(OperatorKind opKind, long complexityLimit, int operatorLimit,
            TypeKlass ownerClass, Type resultType, boolean exceptionSafe, boolean noconsts) {
        super(opKind, complexityLimit, operatorLimit, ownerClass, resultType, exceptionSafe, noconsts);
    }

    @Override
    protected boolean isApplicable(Type resultType) {
        return resultType.equals(TypeList.BOOLEAN);
    }

    @Override
    protected Pair<Type, Type> generateTypes() {
        return new Pair<>(resultType, resultType);
    }

    @Override
    protected BinaryOperator generateProduction(Type leftType, Type rightType) throws ProductionFailedException {
        int leftOpLimit = (int) (PseudoRandom.random() * (operatorLimit - 1));
        int rightOpLimit = operatorLimit - 1 - leftOpLimit;
        long leftComplLimit = (long) (PseudoRandom.random() * (complexityLimit - 1));
        long rightComplLimit = complexityLimit - 1 - leftComplLimit;
        if (leftOpLimit == 0 || rightOpLimit == 0 || leftComplLimit == 0 || rightComplLimit == 0) {
            throw new ProductionFailedException();
        }
        boolean swap = PseudoRandom.randomBoolean();
        IRNodeBuilder builder = new IRNodeBuilder().setOwnerKlass((TypeKlass) ownerClass)
                .setExceptionSafe(exceptionSafe);
        IRNode leftOperand = builder.setComplexityLimit(leftComplLimit)
                .setOperatorLimit(leftOpLimit)
                .setResultType(leftType)
                .setNoConsts(swap && noconsts)
                .getExpressionFactory()
                .produce();
        // Right branch won't necessarily execute. Ignore initalization performed in it.
        SymbolTable.push();
        IRNode rightOperand;
        try {
            rightOperand = builder.setComplexityLimit(rightComplLimit)
                    .setOperatorLimit(rightOpLimit)
                    .setResultType(rightType)
                    .setNoConsts(!swap && noconsts)
                    .getExpressionFactory()
                    .produce();
        } finally {
            SymbolTable.pop();
        }
        return new BinaryOperator(opKind, resultType, leftOperand, rightOperand);
    }
}
