/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6724218
 * @summary Fix raise_LCA_above_marks() early termination
 *
 * @run main/othervm -Xbatch
 *      -XX:CompileCommand=exclude,compiler.c2.Test6724218::update
 *      compiler.c2.Test6724218
 */

package compiler.c2;

public class Test6724218 {
    Test6724218 next  = null;
    Object value = null;

    static boolean _closed = false;
    static int size = 0;
    static Test6724218 list  = null;
    static int cache_size = 0;
    static Test6724218 cache = null;

    Object get(int i) {
        Test6724218 t = list;
        list = t.next;
        size -= 1;
        Object o = t.value;
        if (i > 0) {
            t.next = cache;
            t.value = null;
            cache = t;
            cache_size = +1;
        }
        return o;
    }

    void update() {
        // Exclude compilation of this one.
        if (size == 0) {
            Test6724218 t;
            if (cache_size > 0) {
                t = cache;
                cache = t.next;
                cache_size = -1;
            } else {
                t = new Test6724218();
            }
            t.value = new Object();
            t.next = list;
            list = t;
            size += 1;
        }
    }

    synchronized Object test(int i) {
        while (true) {
            if (_closed) {
                return null;
            } else if (size > 0) {
                return get(i);
            }
            update();
        }
    }

    public static void main(String argv[]) throws Exception {
        Test6724218 t = new Test6724218();
        int lim = 500000;
        Object o;
        for (int j = 0; j < lim; j++) {
            o = t.test(j&1);
            if (o == null) {
              throw new Exception("*** Failed on iteration " + j);
            }
            if ((j&1) == 0) {
              t.update();
            }
        }
    }
}
