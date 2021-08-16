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

/*
 * @test
 * @bug 8007722
 * @summary GetAndSetP's MachNode should capture bottom type
 *
 * @run main/othervm -XX:-UseOnStackReplacement -XX:-BackgroundCompilation
 *      compiler.c2.Test8007722
 */

package compiler.c2;

import java.util.concurrent.atomic.AtomicReference;

public class Test8007722 {

    int i;
    static AtomicReference<Test8007722> ref;

    static int test(Test8007722 new_obj) {
        Test8007722 o = ref.getAndSet(new_obj);
        int ret = o.i;
        o.i = 5;
        return ret;
    }

    static public void main(String[] args) {
        Test8007722 obj = new Test8007722();
        ref = new AtomicReference<Test8007722>(obj);

        for (int i = 0; i < 20000; i++) {
            test(obj);
        }

        System.out.println("PASSED");
    }
}
