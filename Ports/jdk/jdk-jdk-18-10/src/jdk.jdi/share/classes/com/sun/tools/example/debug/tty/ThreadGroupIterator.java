/*
 * Copyright (c) 1998, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

/*
 * This source code is provided to illustrate the usage of a given feature
 * or technique and has been deliberately simplified. Additional steps
 * required for a production-quality application, such as security checks,
 * input validation and proper error handling, might not be present in
 * this sample code.
 */


package com.sun.tools.example.debug.tty;

import com.sun.jdi.ThreadGroupReference;
import java.util.List;
import java.util.Stack;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Descend the tree of thread groups.
 * @author Robert G. Field
 */
class ThreadGroupIterator implements Iterator<ThreadGroupReference> {
    private final Stack<Iterator<ThreadGroupReference>> stack = new Stack<Iterator<ThreadGroupReference>>();

    ThreadGroupIterator(List<ThreadGroupReference> tgl) {
        push(tgl);
    }

    ThreadGroupIterator(ThreadGroupReference tg) {
        List<ThreadGroupReference> tgl = new ArrayList<ThreadGroupReference>();
        tgl.add(tg);
        push(tgl);
    }

    ThreadGroupIterator() {
        this(Env.vm().topLevelThreadGroups());
    }

    private Iterator<ThreadGroupReference> top() {
        return stack.peek();
    }

    /**
     * The invariant in this class is that the top iterator
     * on the stack has more elements.  If the stack is
     * empty, there is no top.  This method assures
     * this invariant.
     */
    private void push(List<ThreadGroupReference> tgl) {
        stack.push(tgl.iterator());
        while (!stack.isEmpty() && !top().hasNext()) {
            stack.pop();
        }
    }

    @Override
    public boolean hasNext() {
        return !stack.isEmpty();
    }

    @Override
    public ThreadGroupReference next() {
        return nextThreadGroup();
    }

    public ThreadGroupReference nextThreadGroup() {
        ThreadGroupReference tg = top().next();
        push(tg.threadGroups());
        return tg;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    static ThreadGroupReference find(String name) {
        ThreadGroupIterator tgi = new ThreadGroupIterator();
        while (tgi.hasNext()) {
            ThreadGroupReference tg = tgi.nextThreadGroup();
            if (tg.name().equals(name)) {
                return tg;
            }
        }
        return null;
    }

}
