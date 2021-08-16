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

package jdk.test.lib.jittester.loops;

import java.util.List;
import jdk.test.lib.jittester.Block;
import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.Statement;
import jdk.test.lib.jittester.visitors.Visitor;

public class For extends IRNode {

    @Override
    public<T> T accept(Visitor<T> v) {
        return v.visit(this);
    }

    public Loop getLoop() {
        return loop;
    }
    public enum ForPart {
        HEADER,
        STATEMENT1,
        STATEMENT2,
        BODY1,
        BODY2,
        BODY3,
    }

    private final Loop loop;
    // header;                       // [subblock]
    // statement1, statement2;       // for (statement; condition; statement) {
    // body1;                        //    [subblock with breaks]
    //    mutate(x);
    // body2;                        //    [subblock with breaks and continues]
    // body3;                        //    [subblock with breaks]
    // }
    private long thisLoopIterLimit = 0;
    public For(int level, Loop loop, long thisLoopIterLimit,
               Block header, Statement statement1,
               Statement statement2, Block body1, Block body2, Block body3) {
        super(body1.getResultType());
        this.level = level;
        this.loop = loop;
        this.thisLoopIterLimit = thisLoopIterLimit;
        resizeUpChildren(ForPart.values().length);
        getChildren().set(ForPart.HEADER.ordinal(), header);
        getChildren().set(ForPart.STATEMENT1.ordinal(), statement1);
        getChildren().set(ForPart.STATEMENT2.ordinal(), statement2);
        getChildren().set(ForPart.BODY1.ordinal(), body1);
        getChildren().set(ForPart.BODY2.ordinal(), body2);
        getChildren().set(ForPart.BODY3.ordinal(), body3);
    }

    @Override
    public long complexity() {
        IRNode header = getChild(ForPart.HEADER.ordinal());
        IRNode statement1 = getChild(ForPart.STATEMENT1.ordinal());
        IRNode statement2 = getChild(ForPart.STATEMENT2.ordinal());
        IRNode body1 = getChild(ForPart.BODY1.ordinal());
        IRNode body2 = getChild(ForPart.BODY2.ordinal());
        IRNode body3 = getChild(ForPart.BODY3.ordinal());
        return loop.initialization.complexity()
                + header.complexity()
                + statement1.complexity()
                + thisLoopIterLimit * (loop.condition.complexity()
                + statement2.complexity()
                + body1.complexity()
                + loop.manipulator.complexity()
                + body2.complexity()
                + body3.complexity());
    }

    @Override
    public long countDepth() {
        return Long.max(level, super.countDepth());
    }

    @Override
    public boolean removeSelf() {
        IRNode header = getChildren().get(ForPart.HEADER.ordinal());
        List<IRNode> siblings = getParent().getChildren();
        int index = siblings.indexOf(this);
        siblings.set(index++, loop.initialization);
        if (header instanceof Block) {
            siblings.addAll(index, header.getChildren());
        } else {
            siblings.add(index, header);
        }
        return true;
    }
}
