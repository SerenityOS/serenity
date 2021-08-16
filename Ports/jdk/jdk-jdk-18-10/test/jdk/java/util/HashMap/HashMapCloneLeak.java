/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7042126
 * @summary Verify that we do not leak contents when we clone a HashMap
 * @author david.buck@oracle.com
 * @run main/othervm HashMapCloneLeak
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+EliminateAutoBox -XX:AutoBoxCacheMax=20000 HashMapCloneLeak
 */

import java.util.HashMap;
import java.lang.ref.WeakReference;

public class HashMapCloneLeak {

    static WeakReference<Object> wr = null;

    // helper method to keep testObject and map out of main method's scope
    private static HashMap<Integer, Object> makeMap() {
        HashMap<Integer, Object> map = new HashMap<Integer, Object>();
        Object testObject = new Object();
        wr = new WeakReference<Object>(testObject);
        map.put(42, testObject);
        return map;
    }

    public static void main(String[] args) throws Exception {
        HashMap<Integer, Object> hm = makeMap();
        hm = (HashMap<Integer, Object>)hm.clone();
        hm.clear();
        // There should no longer be a strong reference to testObject
        // the WeakReference should be nulled out by GC. If not,
        // we will hang here until timed out by the test harness.
        Object[] chain = null;
        while (wr.get() != null) {
            try {
                Object[] allocate = new Object[1000000];
                allocate[0] = chain;
                chain = allocate;
            } catch (OutOfMemoryError oome) {
                chain = null;
            }
            System.gc();
            Thread.sleep(100);
        }
    }

}
