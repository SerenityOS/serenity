/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6895383
 * @summary JCK test throws NPE for method compiled with Escape Analysis
 *
 * @run main/othervm -Xcomp compiler.escapeAnalysis.Test6895383
 */

package compiler.escapeAnalysis;

import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;

public class Test6895383 {
    public static void main(String argv[]) {
        Test6895383 test = new Test6895383();
        test.testRemove1_IndexOutOfBounds();
        test.testAddAll1_IndexOutOfBoundsException();
    }

    public void testRemove1_IndexOutOfBounds() {
        CopyOnWriteArrayList c = new CopyOnWriteArrayList();
    }

    public void testAddAll1_IndexOutOfBoundsException() {
        try {
            CopyOnWriteArrayList c = new CopyOnWriteArrayList();
            c.addAll(-1, new LinkedList()); // should throw IndexOutOfBoundsException
        } catch (IndexOutOfBoundsException e) {
        }
    }
}
