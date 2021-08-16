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

import jdk.test.lib.jittester.Operator;
import jdk.test.lib.jittester.OperatorKind;
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.Rule;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.types.TypeKlass;

class BitwiseOperatorFactory extends Factory<Operator> {
    private final Rule<Operator> rule;

    BitwiseOperatorFactory(long complexityLimit, int operatorLimit, TypeKlass ownerClass,
            Type resultType, boolean exceptionSafe, boolean noconsts) throws ProductionFailedException {
        IRNodeBuilder builder = new IRNodeBuilder()
                .setComplexityLimit(complexityLimit)
                .setOperatorLimit(operatorLimit)
                .setOwnerKlass(ownerClass)
                .setResultType(resultType)
                .setExceptionSafe(exceptionSafe)
                .setNoConsts(noconsts);
        rule = new Rule<>("bitwise");
        rule.add("and", builder.setOperatorKind(OperatorKind.BIT_AND).getBinaryOperatorFactory());
        rule.add("or", builder.setOperatorKind(OperatorKind.BIT_OR).getBinaryOperatorFactory());
        rule.add("xor", builder.setOperatorKind(OperatorKind.BIT_XOR).getBinaryOperatorFactory());
        rule.add("not", builder.setOperatorKind(OperatorKind.BIT_NOT).getUnaryOperatorFactory());
        rule.add("shl", builder.setOperatorKind(OperatorKind.SHL).getBinaryOperatorFactory());
        rule.add("shr", builder.setOperatorKind(OperatorKind.SHR).getBinaryOperatorFactory());
        rule.add("sar", builder.setOperatorKind(OperatorKind.SAR).getBinaryOperatorFactory());
    }

    @Override
    public Operator produce() throws ProductionFailedException {
        return rule.produce();
    }
}
