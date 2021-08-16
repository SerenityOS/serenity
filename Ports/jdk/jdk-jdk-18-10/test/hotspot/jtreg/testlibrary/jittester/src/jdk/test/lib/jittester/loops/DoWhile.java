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

public class DoWhile extends IRNode {

    @Override
    public<T> T accept(Visitor<T> v) {
        return v.visit(this);
    }

    public Loop getLoop() {
        return loop;
    }
    public enum DoWhilePart {
        HEADER,
        BODY1,
        BODY2,
    }
    private final Loop loop;
    // header;                  [subblock]
    // do {
    //      body1;              [subblock with breaks]
    //      mutate(counter);
    //      body2;              [subblock with breaks]
    // } while(condition);
    private long thisLoopIterLimit = 0;

    public DoWhile(int level, Loop loop, long thisLoopIterLimit, Block header,
                   Block body1, Block body2) {
        super(body1.getResultType());
        this.level = level;
        this.loop = loop;
        this.thisLoopIterLimit = thisLoopIterLimit;
        addChild(header);
        addChild(body1);
        addChild(body2);
    }

    @Override
    public long complexity() {
        IRNode header = getChild(DoWhilePart.HEADER.ordinal());
        IRNode body1 = getChild(DoWhilePart.BODY1.ordinal());
        IRNode body2 = getChild(DoWhilePart.BODY2.ordinal());
        return loop.initialization.complexity()
                + header.complexity()
                + thisLoopIterLimit * (body1.complexity()
                + loop.manipulator.complexity()
                + body2.complexity()
                + loop.condition.complexity());
    }

    @Override
    public long countDepth() {
        return Long.max(level, super.countDepth());
    }

    @Override
    public boolean removeSelf() {
        IRNode header = getChildren().get(DoWhilePart.HEADER.ordinal());
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
