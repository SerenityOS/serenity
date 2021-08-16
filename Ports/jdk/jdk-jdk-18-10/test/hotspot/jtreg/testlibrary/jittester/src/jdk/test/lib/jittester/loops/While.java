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
import jdk.test.lib.jittester.visitors.Visitor;

public class While extends IRNode {

    public Loop getLoop() {
        return loop;
    }
    public enum WhilePart {
        HEADER,
        BODY1,
        BODY2,
        BODY3,
    }

    private final Loop loop;
    // int counter = x;
    // header;                                // [subblock]
    // while (condition) {
    //      body1;                                 //    [subblock with breaks]
    //      mutate(counter);
    //      body2;                                 //    [subblock with breaks and continues]
    //      body3;                                 //    [subblock with breaks]
    // }
    private final long thisLoopIterLimit;

    public While(int level, Loop loop, long thisLoopIterLimit, Block header,
                 Block body1, Block body2, Block body3) {
        super(body1.getResultType());
        this.loop = loop;
        this.level = level;
        this.thisLoopIterLimit = thisLoopIterLimit;
        resizeUpChildren(WhilePart.values().length);
        getChildren().set(WhilePart.HEADER.ordinal(), header);
        getChildren().set(WhilePart.BODY1.ordinal(), body1);
        getChildren().set(WhilePart.BODY2.ordinal(), body2);
        getChildren().set(WhilePart.BODY3.ordinal(), body3);
    }

    @Override
    public long complexity() {
        IRNode header = getChildren().get(WhilePart.HEADER.ordinal());
        IRNode body1 = getChildren().get(WhilePart.BODY1.ordinal());
        IRNode body2 = getChildren().get(WhilePart.BODY2.ordinal());
        IRNode body3 = getChildren().get(WhilePart.BODY3.ordinal());
        return loop.initialization.complexity()
                + header.complexity()
                + thisLoopIterLimit * (loop.condition.complexity()
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
        IRNode header = getChildren().get(WhilePart.HEADER.ordinal());
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

    @Override
    public<T> T accept(Visitor<T> v) {
        return v.visit(this);
    }
}
