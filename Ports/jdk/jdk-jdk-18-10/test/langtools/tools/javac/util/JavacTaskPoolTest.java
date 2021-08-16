/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8186694
 * @summary Check that JavacTaskPool reuses JavacTask internals when it should
 * @modules jdk.compiler/com.sun.tools.javac.api
 * @run main JavacTaskPoolTest
 */

import java.util.List;

import javax.lang.model.util.Types;

import com.sun.tools.javac.api.JavacTaskPool;

public class JavacTaskPoolTest {
    public static void main(String... args) throws Exception {
        new JavacTaskPoolTest().run();
    }

    void run() throws Exception {
        JavacTaskPool pool = new JavacTaskPool(2);
        Types tps1 = pool.getTask(null, null, null, List.of("-XDone"), null, null, task -> {
            task.getElements(); //initialize
            return task.getTypes();
        });
        Types tps2  = pool.getTask(null, null, null, List.of("-XDone"), null, null, task -> {
            task.getElements(); //initialize
            return task.getTypes();
        });

        assertSame(tps1, tps2);

        Types tps3 = pool.getTask(null, null, null, List.of("-XDtwo"), null, null, task -> {
            task.getElements(); //initialize
            return task.getTypes();
        });

        assertNotSame(tps1, tps3);

        Types tps4 = pool.getTask(null, null, null, List.of("-XDthree"), null, null, task -> {
            task.getElements(); //initialize
            return task.getTypes();
        });

        assertNotSame(tps1, tps4);
        assertNotSame(tps3, tps4);

        Types tps5 = pool.getTask(null, null, null, List.of("-XDone"), null, null, task -> {
            task.getElements(); //initialize
            return task.getTypes();
        });

        assertNotSame(tps1, tps5);
    }

    void assertSame(Object expected, Object actual) {
        if (expected != actual) {
            throw new IllegalStateException("expected=" + expected + "; actual=" + actual);
        }
    }

    void assertNotSame(Object expected, Object actual) {
        if (expected == actual) {
            throw new IllegalStateException("expected=" + expected + "; actual=" + actual);
        }
    }

}
