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
import jdk.test.lib.jittester.Rule;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.VariableBase;
import jdk.test.lib.jittester.utils.TypeUtil;
import jdk.test.lib.jittester.VariableInfo;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

class AssignmentOperatorImplFactory extends BinaryOperatorFactory {
    AssignmentOperatorImplFactory(long complexityLimit, int operatorLimit, TypeKlass ownerClass,
            Type resultType, boolean exceptionSafe, boolean noconsts) {
        super(OperatorKind.ASSIGN, complexityLimit, operatorLimit, ownerClass, resultType, exceptionSafe, noconsts);
    }

    @Override
    protected boolean isApplicable(Type resultType) {
        return true;
    }

    @Override
    protected Pair<Type, Type> generateTypes() {
        return new Pair<>(resultType, PseudoRandom.randomElement(
                TypeUtil.getImplicitlyCastable(TypeList.getAll(), resultType)));
    }

    @Override
    protected BinaryOperator generateProduction(Type leftOperandType, Type rightOperandType)
            throws ProductionFailedException {
        long leftComplexityLimit = (long) (PseudoRandom.random() * complexityLimit);
        long rightComplexityLimit = complexityLimit - leftComplexityLimit;
        int leftOperatorLimit = (int) (PseudoRandom.random() * operatorLimit);
        int rightOperatorLimit = operatorLimit = leftOperatorLimit;
        IRNodeBuilder builder = new IRNodeBuilder().setOwnerKlass((TypeKlass) ownerClass)
                .setExceptionSafe(exceptionSafe)
                .setNoConsts(noconsts)
                .setComplexityLimit(leftComplexityLimit)
                .setOperatorLimit(leftOperatorLimit)
                .setResultType(leftOperandType)
                .setIsConstant(false);
        Rule<VariableBase> rule = new Rule<>("assignment");
        rule.add("initialized_nonconst_var", builder.setIsInitialized(true).getVariableFactory());
        rule.add("uninitialized_nonconst_var", builder.setIsInitialized(false).getVariableFactory());
        VariableBase leftOperandValue = rule.produce();
        IRNode rightOperandValue = builder.setComplexityLimit(rightComplexityLimit)
                .setOperatorLimit(rightOperatorLimit)
                .setResultType(rightOperandType)
                .getExpressionFactory()
                .produce();
        try {
            if ((leftOperandValue.getVariableInfo().flags & VariableInfo.INITIALIZED) == 0) {
                leftOperandValue.getVariableInfo().flags |= VariableInfo.INITIALIZED;
            }
        } catch (Exception e) {
            throw new ProductionFailedException(e.getMessage());
        }
        return new BinaryOperator(opKind, resultType, leftOperandValue, rightOperandValue);
    }
}
