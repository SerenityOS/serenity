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
import jdk.test.lib.jittester.OperatorKind;
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.ProductionLimiter;
import jdk.test.lib.jittester.ProductionParams;
import jdk.test.lib.jittester.Rule;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.types.TypeKlass;

class ExpressionFactory extends SafeFactory<IRNode> {
    private final Rule<IRNode> rule;

    ExpressionFactory(long complexityLimit, int operatorLimit, TypeKlass ownerClass, Type resultType,
            boolean exceptionSafe, boolean noconsts) throws ProductionFailedException {
        IRNodeBuilder builder = new IRNodeBuilder()
                .setComplexityLimit(complexityLimit)
                .setOperatorLimit(operatorLimit)
                .setOwnerKlass(ownerClass)
                .setResultType(resultType)
                .setExceptionSafe(exceptionSafe)
                .setNoConsts(noconsts);
        rule = new Rule<>("expression");
        if (!noconsts) {
            rule.add("literal", builder.getLiteralFactory());
            rule.add("constant", builder.setIsConstant(true)
                    .setIsInitialized(true)
                    //.setVariableType(resultType)
                    .getVariableFactory());
        }
        rule.add("variable", builder.setIsConstant(false).setIsInitialized(true).getVariableFactory());
        if (operatorLimit > 0 && complexityLimit > 0) {
            rule.add("cast", builder.getCastOperatorFactory(), 0.1);
            rule.add("arithmetic", builder.getArithmeticOperatorFactory());
            rule.add("logic", builder.getLogicOperatorFactory());
            rule.add("bitwise", new BitwiseOperatorFactory(complexityLimit, operatorLimit, ownerClass,
                    resultType, exceptionSafe, noconsts));
            rule.add("assignment", builder.getAssignmentOperatorFactory());
            rule.add("ternary", builder.getTernaryOperatorFactory());
            rule.add("function", builder.getFunctionFactory(), 0.1);
            rule.add("str_plus", builder.setOperatorKind(OperatorKind.STRADD).getBinaryOperatorFactory());
            if (!ProductionParams.disableArrays.value() && !exceptionSafe) {
                //rule.add("array_creation", builder.getArrayCreationFactory());
                rule.add("array_element", builder.getArrayElementFactory());
                rule.add("array_extraction", builder.getArrayExtractionFactory());
            }
        }
    }

    @Override
    protected IRNode sproduce() throws ProductionFailedException {
        ProductionLimiter.limitProduction();
        return rule.produce();
    }
}
