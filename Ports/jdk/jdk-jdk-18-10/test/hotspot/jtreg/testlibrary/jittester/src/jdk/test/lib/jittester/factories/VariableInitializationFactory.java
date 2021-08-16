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

import java.util.LinkedList;
import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.ProductionParams;
import jdk.test.lib.jittester.Rule;
import jdk.test.lib.jittester.Symbol;
import jdk.test.lib.jittester.SymbolTable;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.VariableInfo;
import jdk.test.lib.jittester.VariableInitialization;
import jdk.test.lib.jittester.types.TypeKlass;
import jdk.test.lib.jittester.utils.PseudoRandom;

class VariableInitializationFactory extends SafeFactory<VariableInitialization> {
    private final int operatorLimit;
    private final long complexityLimit;
    private final boolean constant;
    private final boolean isStatic;
    private final boolean isLocal;
    private final boolean exceptionSafe;
    private final TypeKlass ownerClass;

    VariableInitializationFactory(TypeKlass ownerClass, boolean constant, boolean isStatic,
            boolean isLocal, long complexityLimit, int operatorLimit, boolean exceptionSafe) {
        this.ownerClass = ownerClass;
        this.constant = constant;
        this.isStatic = isStatic;
        this.isLocal = isLocal;
        this.complexityLimit = complexityLimit;
        this.operatorLimit = operatorLimit;
        this.exceptionSafe = exceptionSafe;
    }

    @Override
    protected VariableInitialization sproduce() throws ProductionFailedException {
        LinkedList<Type> types = new LinkedList<>(TypeList.getAll());
        PseudoRandom.shuffle(types);
        if (types.isEmpty()) {
            throw new ProductionFailedException();
        }
        Type resultType = types.getFirst();
        IRNodeBuilder b = new IRNodeBuilder().setComplexityLimit(complexityLimit - 1)
                .setOperatorLimit(operatorLimit - 1)
                .setOwnerKlass(ownerClass)
                .setResultType(resultType)
                .setExceptionSafe(exceptionSafe)
                .setNoConsts(false);
        Rule<IRNode> rule = new Rule<>("initializer");
        rule.add("literal_initializer", b.getLiteralFactory());
        if (!ProductionParams.disableExprInInit.value()) {
            rule.add("expression", b.getLimitedExpressionFactory());
        }
        Symbol thisSymbol = null;
        if (isStatic) {
            thisSymbol = SymbolTable.get("this", VariableInfo.class);
            SymbolTable.remove(thisSymbol);
        }
        IRNode init;
        try {
            init = rule.produce();
        } finally {
            if (isStatic) {
                SymbolTable.add(thisSymbol);
            }
        }
        String resultName = "var_" + SymbolTable.getNextVariableNumber();
        int flags = VariableInfo.INITIALIZED;
        if (constant) {
            flags |= VariableInfo.FINAL;
        }
        if (isStatic) {
            flags |= VariableInfo.STATIC;
        }
        if (isLocal) {
            flags |= VariableInfo.LOCAL;
        }
        VariableInfo varInfo = new VariableInfo(resultName, ownerClass, resultType, flags);
        SymbolTable.add(varInfo);
        return new VariableInitialization(varInfo, init);
    }
}
