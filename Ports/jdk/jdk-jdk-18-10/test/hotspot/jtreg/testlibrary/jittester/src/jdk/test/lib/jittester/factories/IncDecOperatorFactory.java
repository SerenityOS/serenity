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

import jdk.test.lib.jittester.OperatorKind;
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.UnaryOperator;
import jdk.test.lib.jittester.types.TypeKlass;

class IncDecOperatorFactory extends UnaryOperatorFactory {
    IncDecOperatorFactory(OperatorKind opKind, long complexityLimit, int operatorLimit,
                          Type klass, Type resultType, boolean safe, boolean noconsts) {
        super(opKind, complexityLimit, operatorLimit, klass, resultType, safe, noconsts);
    }

    @Override
    protected boolean isApplicable(Type resultType) {
        return TypeList.isBuiltInInt(resultType) && !resultType.equals(TypeList.BOOLEAN);
    }

    @Override
    protected UnaryOperator generateProduction(Type l) throws ProductionFailedException {
        return new UnaryOperator(opKind, new IRNodeBuilder().setComplexityLimit(complexityLimit - 1)
                .setOperatorLimit(operatorLimit - 1)
                .setOwnerKlass((TypeKlass) ownerClass)
                .setResultType(l)
                .setIsConstant(false)
                .setIsInitialized(true)
                .setExceptionSafe(exceptionSafe)
                .setNoConsts(exceptionSafe)
                .getVariableFactory()
                .produce());
    }
}
