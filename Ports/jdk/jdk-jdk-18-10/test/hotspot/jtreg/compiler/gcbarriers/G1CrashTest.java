/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8023472
 * @summary C2 optimization breaks with G1
 *
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:-TieredCompilation
 *      -Dcount=100000 compiler.gcbarriers.G1CrashTest
 *
 * @author pbiswal@palantir.com
 */

package compiler.gcbarriers;

public class G1CrashTest {
    static Object[] set = new Object[11];

    public static void main(String[] args) throws InterruptedException {
        for (int j = 0; j < Integer.getInteger("count"); j++) {
            Object key = new Object();
            insertKey(key);
            if (j > set.length / 2) {
                Object[] oldKeys = set;
                set = new Object[2 * set.length - 1];
                for (Object o : oldKeys) {
                    if (o != null)
                        insertKey(o);
                }
            }
        }
    }

    static void insertKey(Object key) {
        int hash = key.hashCode() & 0x7fffffff;
        int index = hash % set.length;
        Object cur = set[index];
        if (cur == null)
            set[index] = key;
        else
            insertKeyRehash(key, index, hash, cur);
    }

    static void insertKeyRehash(Object key, int index, int hash, Object cur) {
        int loopIndex = index;
        int firstRemoved = -1;
        do {
            if (cur == "dead")
                firstRemoved = 1;
            index--;
            if (index < 0)
                index += set.length;
            cur = set[index];
            if (cur == null) {
                if (firstRemoved != -1)
                    set[firstRemoved] = "dead";
                else
                    set[index] = key;
                return;
            }
        } while (index != loopIndex);
        if (firstRemoved != -1)
            set[firstRemoved] = null;
    }
}
