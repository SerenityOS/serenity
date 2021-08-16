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

import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.Rule;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.VariableBase;
import jdk.test.lib.jittester.VariableInfo;
import jdk.test.lib.jittester.types.TypeKlass;

class VariableFactory extends Factory<VariableBase> {
    private final Rule<VariableBase> rule;

    VariableFactory(long complexityLimit, int operatorLimit, TypeKlass ownerClass, Type resultType,
            boolean constant, boolean initialized, boolean exceptionSafe, boolean noconsts) {
        int flags = VariableInfo.NONE;
        if (constant) {
            flags |= VariableInfo.FINAL;
        }
        if (initialized) {
            flags |= VariableInfo.INITIALIZED;
        }
        rule = new Rule<>("variable");
        IRNodeBuilder b = new IRNodeBuilder().setResultType(resultType)
                .setFlags(flags)
                .setComplexityLimit(complexityLimit)
                .setOperatorLimit(operatorLimit)
                .setOwnerKlass(ownerClass)
                .setExceptionSafe(exceptionSafe);
        rule.add("non_static_member_variable", b.getNonStaticMemberVariableFactory());
        rule.add("static_member_variable", b.getStaticMemberVariableFactory());
        rule.add("local_variable", b.getLocalVariableFactory());
    }

    @Override
    public VariableBase produce() throws ProductionFailedException {
        return rule.produce();
    }
}
