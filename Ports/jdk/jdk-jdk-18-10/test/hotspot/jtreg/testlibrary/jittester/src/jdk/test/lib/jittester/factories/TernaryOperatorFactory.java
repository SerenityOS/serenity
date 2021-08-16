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

import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.SymbolTable;
import jdk.test.lib.jittester.TernaryOperator;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

class TernaryOperatorFactory extends OperatorFactory<TernaryOperator> {
    private final Type resultType;
    private final TypeKlass ownerClass;

    TernaryOperatorFactory(long complexityLimit, int operatorLimit, TypeKlass ownerClass,
            Type resultType, boolean exceptionSafe, boolean noconsts) {
        super(2, complexityLimit, operatorLimit, exceptionSafe, noconsts);
        this.resultType = resultType;
        this.ownerClass = ownerClass;
    }

    private TernaryOperator generateProduction() throws ProductionFailedException {
        int leftOpLimit = (int) (PseudoRandom.random() * 0.3 * (operatorLimit - 1));
        int rightOpLimit = (int) (PseudoRandom.random() * 0.3 * (operatorLimit - 1));
        int condOpLimit = operatorLimit - 1 - leftOpLimit - rightOpLimit;
        long leftComplLimit = (long) (PseudoRandom.random() * 0.3 * (complexityLimit - 1));
        long rightComplLimit = (long) (PseudoRandom.random() * 0.3 * (complexityLimit - 1));
        long condComplLimit = complexityLimit - 1 - leftComplLimit - rightComplLimit;
        if (leftComplLimit == 0 || rightComplLimit == 0 || condComplLimit == 0
                || leftOpLimit == 0 || rightOpLimit == 0 || condOpLimit == 0) {
            throw new ProductionFailedException();
        }
        IRNodeBuilder builder = new IRNodeBuilder().setOwnerKlass(ownerClass)
                .setExceptionSafe(exceptionSafe);
        IRNode conditionalExp = builder.setComplexityLimit(condComplLimit)
                .setOperatorLimit(condOpLimit)
                .setResultType(TypeList.BOOLEAN)
                .setNoConsts(noconsts)
                .getExpressionFactory()
                .produce();
        // Ignore initializations performed in left and right branches:
        IRNode leftExp;
        SymbolTable.push();
        try {
            leftExp = builder.setComplexityLimit(leftComplLimit)
                    .setOperatorLimit(leftOpLimit)
                    .setResultType(resultType)
                    .setNoConsts(false)
                    .getExpressionFactory()
                    .produce();
        } finally {
            SymbolTable.pop();
        }
        IRNode rightExp;
        SymbolTable.push();
        try {
            rightExp = builder.setComplexityLimit(rightComplLimit)
                    .setOperatorLimit(rightOpLimit)
                    .setResultType(resultType)
                    .setNoConsts(false)
                    .getExpressionFactory()
                    .produce();
        } finally {
            SymbolTable.pop();
        }
        return new TernaryOperator(conditionalExp, leftExp, rightExp);
    }

    @Override
    public TernaryOperator produce() throws ProductionFailedException {
        try {
            SymbolTable.push();
            TernaryOperator result = generateProduction();
            SymbolTable.merge();
            return result;
        } catch (ProductionFailedException e) {
            SymbolTable.pop();
            throw e;
        }
    }
}
