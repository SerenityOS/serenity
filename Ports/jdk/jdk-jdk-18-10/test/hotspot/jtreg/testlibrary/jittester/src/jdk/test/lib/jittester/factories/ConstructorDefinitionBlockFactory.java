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
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.ProductionParams;
import jdk.test.lib.jittester.functions.ConstructorDefinitionBlock;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

class ConstructorDefinitionBlockFactory extends Factory<ConstructorDefinitionBlock> {
    private final long complexityLimit;
    private final int statementLimit;
    private final int operatorLimit;
    private final int memberFunctionsLimit;
    private final int memberFunctionsArgLimit;
    private final TypeKlass ownerClass;
    private final int level;

    ConstructorDefinitionBlockFactory(TypeKlass ownerClass, int memberFunctionsLimit,
            int memberFunctionsArgLimit, long complexityLimit, int statementLimit,
            int operatorLimit, int level) {
        this.ownerClass = ownerClass;
        this.memberFunctionsLimit = memberFunctionsLimit;
        this.memberFunctionsArgLimit = memberFunctionsArgLimit;
        this.complexityLimit = complexityLimit;
        this.statementLimit = statementLimit;
        this.operatorLimit = operatorLimit;
        this.level = level;
    }

    @Override
    public ConstructorDefinitionBlock produce() throws ProductionFailedException {
        IRNodeBuilder builder = new IRNodeBuilder()
                .setOwnerKlass(ownerClass)
                .setStatementLimit(statementLimit)
                .setOperatorLimit(operatorLimit)
                .setLevel(level);
        ArrayList<IRNode> content = new ArrayList<>();
        int memFunLimit = PseudoRandom.randomNotZero(memberFunctionsLimit);
        builder.setComplexityLimit(complexityLimit / memFunLimit);
        if (!ProductionParams.disableStatic.value() && PseudoRandom.randomBoolean()) {
            // Generate static constructor
            content.add(builder.getStaticConstructorDefinitionFactory().produce());
            // take static constructor into account
            --memFunLimit;
        }
        // No matter what, generate default constructor first.
        // This would guarantee a way to initialize a data member in case,
        // when arguments to a non-default constructor cannot be generated.
        content.add(builder.setMemberFunctionsArgLimit(0)
                .getConstructorDefinitionFactory()
                .produce());
        if (--memFunLimit > 0) {
            for (int i = 0; i < memFunLimit; i++) {
                try {
                    content.add(builder.setMemberFunctionsArgLimit(memberFunctionsArgLimit)
                            .getConstructorDefinitionFactory()
                            .produce());
                } catch (ProductionFailedException e) {
                }
            }
        }
        return new ConstructorDefinitionBlock(content, level);
    }
}
