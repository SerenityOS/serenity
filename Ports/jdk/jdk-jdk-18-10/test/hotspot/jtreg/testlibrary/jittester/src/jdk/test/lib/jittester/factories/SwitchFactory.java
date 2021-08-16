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
import java.util.HashSet;
import java.util.List;

import jdk.test.lib.jittester.BuiltInType;
import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.Literal;
import jdk.test.lib.jittester.Nothing;
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.Rule;
import jdk.test.lib.jittester.Switch;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.utils.TypeUtil;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

class SwitchFactory extends SafeFactory<Switch> {
    private final int statementLimit;
    private final int operatorLimit;
    private final boolean canHaveReturn;
    private final TypeKlass ownerClass;
    private final int level;
    private final long complexityLimit;

    SwitchFactory(TypeKlass ownerClass, long complexityLimit, int statementLimit,
            int operatorLimit, int level, boolean canHaveReturn) {
        this.ownerClass = ownerClass;
        this.complexityLimit = complexityLimit;
        this.statementLimit = statementLimit;
        this.operatorLimit = operatorLimit;
        this.level = level;
        this.canHaveReturn = canHaveReturn;
    }

    @Override
    protected Switch sproduce() throws ProductionFailedException {
        if (statementLimit > 0 && complexityLimit > 0) {
            List<Type> switchTypes = new ArrayList<>();
            switchTypes.add(TypeList.CHAR);
            switchTypes.add(TypeList.BYTE);
            switchTypes.add(TypeList.SHORT);
            switchTypes.add(TypeList.INT);
            PseudoRandom.shuffle(switchTypes);
            IRNodeBuilder builder = new IRNodeBuilder()
                    .setOwnerKlass(ownerClass)
                    .setOperatorLimit(operatorLimit)
                    .setSubBlock(false)
                    .setCanHaveBreaks(true)
                    .setCanHaveContinues(false)
                    .setCanHaveReturn(canHaveReturn);
            MAIN_LOOP:
            for (Type type : switchTypes) {
                List<IRNode> caseConsts = new ArrayList<>();
                List<IRNode> caseBlocks = new ArrayList<>();
                try {
                    int accumulatedStatements = 0;
                    int currentStatementsLimit = 0;
                    long accumulatedComplexity = 0L;
                    long currentComplexityLimit = 0L;
                    currentComplexityLimit = (long) (PseudoRandom.random()
                            * (complexityLimit - accumulatedComplexity));
                    IRNode switchExp = builder.setComplexityLimit(currentComplexityLimit)
                            .setResultType(type)
                            .setExceptionSafe(false)
                            .setNoConsts(true)
                            .getLimitedExpressionFactory()
                            .produce();
                    accumulatedComplexity += currentComplexityLimit;
                    List<Type> caseTypes = new ArrayList<>();
                    caseTypes.add(TypeList.BYTE);
                    caseTypes.add(TypeList.CHAR);
                    caseTypes = new ArrayList<>(TypeUtil.getLessCapaciousOrEqualThan(caseTypes,
                            (BuiltInType) type));
                    if (PseudoRandom.randomBoolean()) { // "default"
                        currentStatementsLimit = (int) (PseudoRandom.random()
                                * (statementLimit - accumulatedStatements));
                        currentComplexityLimit = (long) (PseudoRandom.random()
                                * (complexityLimit - accumulatedComplexity));
                        caseConsts.add(new Nothing());
                        caseBlocks.add(builder.setComplexityLimit(currentComplexityLimit)
                                .setStatementLimit(currentStatementsLimit)
                                .setLevel(level + 1)
                                .setCanHaveReturn(false)
                                .setCanHaveBreaks(false)
                                .getBlockFactory()
                                .produce());
                        builder.setCanHaveBreaks(true)
                                .setCanHaveReturn(canHaveReturn);
                        accumulatedStatements += currentStatementsLimit;
                        accumulatedComplexity += currentComplexityLimit;
                    }
                    HashSet<Integer> cases = new HashSet<>();
                    while (accumulatedStatements < statementLimit) { // "case"s
                        currentStatementsLimit = (int) (PseudoRandom.random()
                                * (statementLimit - accumulatedStatements));
                        currentComplexityLimit = (long) (PseudoRandom.random()
                                * (complexityLimit - accumulatedComplexity));
                        PseudoRandom.shuffle(caseTypes);
                        for (int tryCount = 0; true; tryCount++) {
                            if (tryCount >= 10) {
                                continue MAIN_LOOP;
                            }
                            Literal literal = builder.setResultType(caseTypes.get(0))
                                    .getLiteralFactory().produce();
                            int value = 0;
                            if (literal.value instanceof Integer) {
                                value = (Integer) literal.value;
                            }
                            if (literal.value instanceof Short) {
                                value = (Short) literal.value;
                            }
                            if (literal.value instanceof Byte) {
                                value = (Byte) literal.value;
                            }
                            if (literal.value instanceof Character) {
                                value = (Character) literal.value;
                            }
                            if (!cases.contains(value)) {
                                cases.add(value);
                                caseConsts.add(literal);
                                break;
                            }
                        }
                        Rule<IRNode> rule = new Rule<>("case_block");
                        rule.add("block", builder.setComplexityLimit(currentComplexityLimit)
                                .setStatementLimit(currentStatementsLimit)
                                .setLevel(level)
                                .setCanHaveReturn(false)
                                .setCanHaveBreaks(false)
                                .getBlockFactory());
                        builder.setCanHaveBreaks(true)
                                .setCanHaveReturn(canHaveReturn);
                        rule.add("nothing", builder.getNothingFactory());
                        IRNode choiceResult = rule.produce();
                        caseBlocks.add(choiceResult);
                        if (choiceResult instanceof Nothing) {
                            accumulatedStatements++;
                        } else {
                            accumulatedStatements += currentStatementsLimit;
                            accumulatedComplexity += currentComplexityLimit;
                        }
                    }
                    PseudoRandom.shuffle(caseConsts);
                    List<IRNode> accum = new ArrayList<>();
                    int caseBlockIdx = 1 + caseConsts.size();
                    accum.add(switchExp);
                    for (int i = 1; i < caseBlockIdx; ++i) {
                        accum.add(caseConsts.get(i - 1));
                    }
                    for (int i = caseBlockIdx; i < 1 + caseConsts.size() + caseBlocks.size(); ++i) {
                        accum.add(caseBlocks.get(i - caseBlockIdx));
                    }
                    return new Switch(level, accum, caseBlockIdx);
                } catch (ProductionFailedException e) {
                }
            }
        }
        throw new ProductionFailedException();
    }
}
