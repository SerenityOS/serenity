/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.jittester;

import java.util.List;
import jdk.test.lib.jittester.visitors.Visitor;

public class Switch extends IRNode {
    private final int caseBlockIdx;

    public Switch(int level, List<IRNode> chldrn, int caseBlockIdx) {
        super(chldrn.get(caseBlockIdx).getResultType());
        this.level = level;
        addChildren(chldrn);
        this.caseBlockIdx = caseBlockIdx;
    }

    @Override
    public long complexity() {
        IRNode switchExp = getChild(0);
        long complexity = switchExp != null ? switchExp.complexity() : 0;
        for (int i = caseBlockIdx; i < getChildren().size(); ++i) {
            complexity += getChild(i).complexity();
        }
        return complexity;
    }

    @Override
    public long countDepth() {
        return Long.max(level, super.countDepth());
    }

    @Override
    public<T> T accept(Visitor<T> v) {
        return v.visit(this);
    }

    public int getCaseBlockIndex() {
        return caseBlockIdx;
    }
}
