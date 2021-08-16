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
import java.util.List;

import jdk.test.lib.jittester.Block;
import jdk.test.lib.jittester.CatchBlock;
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.TryCatchBlock;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.utils.TypeUtil;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

class TryCatchBlockFactory extends Factory<TryCatchBlock> {
    private final static double CATCH_SELECTION_COEF = 0.1d;
    private final Type returnType;
    private final long complexityLimit;
    private final int statementLimit, operatorLimit;
    private final boolean subBlock;
    private final boolean canHaveBreaks;
    private final boolean canHaveContinues;
    private final boolean canHaveReturn;
    private final int level;
    private final TypeKlass ownerClass;

    TryCatchBlockFactory(TypeKlass ownerClass, Type returnType,
            long complexityLimit, int statementLimit, int operatorLimit,
            int level, boolean subBlock, boolean canHaveBreaks,
            boolean canHaveContinues, boolean canHaveReturn) {
        this.ownerClass = ownerClass;
        this.returnType = returnType;
        this.complexityLimit = complexityLimit;
        this.statementLimit = statementLimit;
        this.operatorLimit = operatorLimit;
        this.level = level;
        this.subBlock = subBlock;
        this.canHaveBreaks = canHaveBreaks;
        this.canHaveContinues = canHaveContinues;
        this.canHaveReturn = canHaveReturn;
    }

    @Override
    public TryCatchBlock produce() throws ProductionFailedException {
        if (complexityLimit < 1 || statementLimit < 1) {
            throw new ProductionFailedException();
        }
        List<Type> uncheckedThrowables = getUncheckedThrowables();
        IRNodeBuilder builder = new IRNodeBuilder().setOwnerKlass(ownerClass)
                .setResultType(returnType)
                .setOperatorLimit(operatorLimit)
                .setLevel(level)
                .setSubBlock(subBlock)
                .setCanHaveReturn(canHaveReturn)
                .setCanHaveContinues(canHaveContinues)
                .setCanHaveBreaks(canHaveBreaks);
        Block body = getBlock(builder, 0.6);
        int catchBlocksCount = (int) (CATCH_SELECTION_COEF
                * PseudoRandom.random() * uncheckedThrowables.size());
        List<CatchBlock> catchBlocks = new ArrayList<>();
        List<Type> caught = new ArrayList<>();
        for (int i = 0; i < catchBlocksCount; i++) {
            List<Type> whatToCatch = new ArrayList<>();
            int throwableLimit = 1 + (int) ((1/(2*CATCH_SELECTION_COEF))
                    * PseudoRandom.random());
            for (int j = 0; j < throwableLimit; j++) {
                whatToCatch.add(selectUniqueThrowable(uncheckedThrowables, caught));
            }
            catchBlocks.add(new CatchBlock(getBlock(builder, 0.3/catchBlocksCount),
                    whatToCatch, level));
        }
        Block finallyBody = PseudoRandom.randomBoolean() || catchBlocksCount == 0 ? getBlock(builder, 0.1) : null;
        return new TryCatchBlock(body, finallyBody, catchBlocks, level);
    }

    private Type selectUniqueThrowable(List<Type> variants, List<Type> caught) {
        Type selected;
        do {
            int randomIndex = PseudoRandom.randomNotZero(variants.size()) - 1;
            selected = variants.get(randomIndex);
        } while (caught.contains(selected));
        caught.add(selected);
        return selected;
    }

    private Block getBlock(IRNodeBuilder builder, double weight)
            throws ProductionFailedException {
        long actualComplexityLim = (long) (weight * PseudoRandom.random()
                * complexityLimit);
        int actualStatementLim = (int) (weight * PseudoRandom.random()
                * statementLimit);
        return builder.setStatementLimit(actualStatementLim)
                .setComplexityLimit(actualComplexityLim)
                .getBlockFactory()
                .produce();
    }

    private List<Type> getUncheckedThrowables() {
        List<Type> result = new ArrayList<>();
        result.addAll(TypeUtil.getImplicitlyCastable(TypeList.getAll(),
                new TypeKlass("java.lang.Error")));
        result.addAll(TypeUtil.getImplicitlyCastable(TypeList.getAll(),
                new TypeKlass("java.lang.RuntimeException")));
        return result;
    }
}
