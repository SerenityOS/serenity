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
import jdk.test.lib.jittester.SymbolTable;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.UnaryOperator;

public abstract class UnaryOperatorFactory extends OperatorFactory<UnaryOperator> {
    protected final OperatorKind opKind;
    protected final Type resultType;
    protected final Type ownerClass;

    protected UnaryOperatorFactory(OperatorKind opKind, long complexityLimit, int operatorLimit,
            Type ownerClass, Type resultType, boolean exceptionSafe, boolean noconsts) {
        super(opKind.priority, complexityLimit, operatorLimit, exceptionSafe, noconsts);
        this.opKind = opKind;
        this.resultType = resultType;
        this.ownerClass = ownerClass;
    }

    protected Type generateType() {
        return resultType;
    }

    protected abstract UnaryOperator generateProduction(Type type) throws ProductionFailedException;

    protected abstract boolean isApplicable(Type resultType);

    @Override
    public UnaryOperator produce() throws ProductionFailedException {
        if (!isApplicable(resultType)) {
            //avoid implicit use of resultType.toString()
            throw new ProductionFailedException("Type " + resultType.getName()
                    + " is not applicable by " + getClass().getName());
        }
        Type type;
        try {
            type = generateType();
        } catch (Exception ex) {
            throw new ProductionFailedException(ex.getMessage());
        }
        try {
            SymbolTable.push();
            UnaryOperator result = generateProduction(type);
            SymbolTable.merge();
            return result;
        } catch (ProductionFailedException e) {
            SymbolTable.pop();
            throw e;
        }
    }
}
