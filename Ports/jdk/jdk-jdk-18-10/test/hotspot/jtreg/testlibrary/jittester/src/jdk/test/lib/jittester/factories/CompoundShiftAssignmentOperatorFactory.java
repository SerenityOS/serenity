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
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.utils.TypeUtil;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

class CompoundShiftAssignmentOperatorFactory extends BinaryOperatorFactory {
    CompoundShiftAssignmentOperatorFactory(OperatorKind opKind, long complexityLimit,
            int operatorLimit, TypeKlass ownerClass, Type resultType, boolean exceptionSafe, boolean noconsts) {
        super(opKind, complexityLimit, operatorLimit, ownerClass, resultType, exceptionSafe, noconsts);
    }

    @Override
    protected boolean isApplicable(Type resultType) {
        return TypeList.isBuiltInInt(resultType) && !resultType.equals(TypeList.BOOLEAN);
    }

    @Override
    protected Pair<Type, Type> generateTypes() {
        return new Pair<>(resultType, PseudoRandom.randomElement(
                TypeUtil.getExplicitlyCastable(TypeList.getBuiltInInt(), resultType)));
    }

    @Override
    protected BinaryOperator generateProduction(Type leftType, Type rightType) throws ProductionFailedException {
        long leftComplexityLimit = (long) (PseudoRandom.random() * complexityLimit);
        long rightComplexityLimit = complexityLimit - leftComplexityLimit;
        int leftOperatorLimit = (int) (PseudoRandom.random() * operatorLimit);
        int rightOperatorLimit = operatorLimit = leftOperatorLimit;
        IRNodeBuilder builder = new IRNodeBuilder().setOwnerKlass((TypeKlass) ownerClass)
                .setExceptionSafe(exceptionSafe)
                .setNoConsts(noconsts);
        IRNode leftExpr = builder.setComplexityLimit(leftComplexityLimit)
                .setOperatorLimit(leftOperatorLimit)
                .setResultType(leftType)
                .setIsConstant(false)
                .setIsInitialized(true)
                .getVariableFactory()
                .produce();
        IRNode rightExpr = builder.setComplexityLimit(rightComplexityLimit)
                .setOperatorLimit(rightOperatorLimit)
                .setResultType(rightType)
                .getExpressionFactory()
                .produce();
        return new BinaryOperator(opKind, resultType, leftExpr, rightExpr);
    }
}
