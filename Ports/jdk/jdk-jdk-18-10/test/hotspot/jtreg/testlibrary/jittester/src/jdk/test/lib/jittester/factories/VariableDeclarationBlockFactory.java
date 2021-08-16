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

import jdk.test.lib.jittester.Declaration;
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.ProductionParams;
import jdk.test.lib.jittester.VariableDeclarationBlock;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

class VariableDeclarationBlockFactory extends Factory<VariableDeclarationBlock> {
    private final long complexityLimit;
    private final int operatorLimit;
    private final boolean exceptionSafe;
    private final int level;
    private final TypeKlass ownerClass;

    VariableDeclarationBlockFactory(TypeKlass ownerClass, long complexityLimit,
            int operatorLimit, int level, boolean exceptionSafe) {
        this.ownerClass = ownerClass;
        this.complexityLimit = complexityLimit;
        this.operatorLimit = operatorLimit;
        this.level = level;
        this.exceptionSafe = exceptionSafe;
    }

    @Override
    public VariableDeclarationBlock produce() throws ProductionFailedException {
        ArrayList<Declaration> content = new ArrayList<>();
        int limit = (int) Math.ceil(PseudoRandom.random() * ProductionParams.dataMemberLimit.value());
        Factory<Declaration> declFactory = new IRNodeBuilder()
                .setOwnerKlass(ownerClass)
                .setComplexityLimit(complexityLimit)
                .setOperatorLimit(operatorLimit)
                .setIsLocal(false)
                .setExceptionSafe(exceptionSafe)
                .getDeclarationFactory();
        for (int i = 0; i < limit; i++) {
            try {
                content.add(declFactory.produce());
            } catch (ProductionFailedException e) {
            }
        }
        return new VariableDeclarationBlock(content, level);
    }
}
