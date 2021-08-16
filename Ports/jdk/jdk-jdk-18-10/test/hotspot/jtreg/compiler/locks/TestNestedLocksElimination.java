/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @bug 8268347
 * @summary Nested locks optimization may create unbalanced monitor enter/exit code
 *
 * @run main/othervm -XX:-BackgroundCompilation
 *                   -XX:CompileCommand=dontinline,TestNestedLocksElimination::foo
 *                   -XX:CompileCommand=dontinline,TestNestedLocksElimination::getNext
 *                   -XX:CompileCommand=dontinline,TestNestedLocksElimination::getHolder
 *                   TestNestedLocksElimination
 */

import java.util.LinkedList;

public class TestNestedLocksElimination {

    private LinkedList<char[]> buffers = new LinkedList<>();
    private boolean complete = false;
    private int bufferSize;

    void foo(char[] ca) {
        // Don't inline dummy method
    }

    // Don't inline
    char[] getNext(int length, int count) {
        if (this.buffers.isEmpty()) {
            return new char[100];
        }
        char[] b = (char[]) this.buffers.getFirst();
        if (count >= 100) {
            this.complete = true;
            this.buffers.clear(); // empty
        }
        return b;
    }

    synchronized boolean isComplete() {
        return this.complete;
    }

    synchronized boolean availableSegment() {
        return (buffers.isEmpty() == false);
    }

    // Don't inline
    TestNestedLocksElimination getHolder(TestNestedLocksElimination s1, TestNestedLocksElimination s2, int count) {
        return (count & 7) == 0 ? s2 : s1;
    }

    int test(TestNestedLocksElimination s1, TestNestedLocksElimination s2, int maxToSend) {
        boolean isComplete = true;
        boolean availableSegment = false;
        int size = 0;
        int count = 0;
        do {
            TestNestedLocksElimination s = getHolder(s1, s2, count++);

            synchronized(s) {
                isComplete = s.isComplete();
                availableSegment = s.availableSegment();
            }

            synchronized (this) {
                size = 0;
                while (size < maxToSend) {
                    char[] b = null;
                    // This is outer Lock region for object 's'.
                    // Locks from following inlined methods are "nested"
                    // because they reference the same object.
                    synchronized(s) {
                        b = s.getNext(maxToSend - size, count);

                        // The next is bi-morphic call with both calls inlined.
                        // But one is synchronized and the other is not.
                        // Class check for bi-morphic call is loop invariant
                        // and will trigger loop unswitching.
                        // Loop unswitching will create two versions of loop
                        // with gollowing calls inlinined in both versions.

                        isComplete = s.isComplete();

                        // The next synchronized method availableSegment() is
                        // inlined and its Lock will be "coarsened" with Unlock
                        // in version of loop with inlined synchronized method
                        // isComplete().
                        // Nested Lock Optimization will mark only this Unlock
                        // as nested (as part of "nested" pair lock/unlock).
                        // Locks elimination will remove "coarsened" Lock from
                        // availableSegment() method leaving unmatched unlock.

                        availableSegment = s.availableSegment();
                    }
                    foo(b);
                    size += b.length;
                }
            }
      } while (availableSegment == true || isComplete == false);
        return size;
    }

    public static void main(String[] args) {
        int count = 0;
        int n = 0;

        TestNestedLocksElimination t = new TestNestedLocksElimination();
        TestNestedLocksElimination s1 = new TestNestedLocksElimination();
        TestNestedLocksElimination s2 = new TestNestedLocksEliminationSub();

        char[] c = new char[100];
        while (n++ < 20_000) {
            s1.buffers.add(c);
            s2.buffers.add(c);
            count += t.test(s1, s2, 10000);
        }

        System.out.println(" count: " + count);
    }
}

class TestNestedLocksEliminationSub extends TestNestedLocksElimination {
    public boolean isComplete() {
        return true;
    }
}

