/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Asserts;

/*
 * @test
 * @bug 4784641
 * @summary -Xcheck:jni overly strict in JNI method IsSameObject
 *          Fixed in JDK1.3.1_10
 *          Fixed in JDK1.4.1_07
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run main/othervm/native -Xcheck:jni SameObject
 */
public class SameObject {

    public Object obj = new Object();

    static {
        System.loadLibrary("SameObject");
    }

    public native void createWeakRef(Object obj);

    public native int checkWeakRef();

    public static void main(String[] args) throws Exception {
        SameObject sameObject = new SameObject();

        int result = sameObject.test();
        Asserts.assertEquals(result, 0, "WeakRef still alive");
    }

    public int test() {
        createWeakRef(obj);
        obj = null;
        System.gc();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            System.out.println("Interrupted");
        }

        return checkWeakRef();
    }
}
