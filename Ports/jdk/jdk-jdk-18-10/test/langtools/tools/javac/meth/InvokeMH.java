/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6754038 6979327
 * @summary Generate call sites for method handle
 * @author jrose
 *
 * @compile InvokeMH.java
 */

/*
 * Standalone testing:
 * <code>
 * $ cd $MY_REPO_DIR/langtools
 * $ (cd make; make)
 * $ ./dist/bootstrap/bin/javac -d dist test/tools/javac/meth/InvokeMH.java
 * $ javap -c -classpath dist meth.InvokeMH
 * </code>
 */

package meth;

import java.lang.invoke.MethodHandle;

public class InvokeMH {
    void test(MethodHandle mh_SiO,
              MethodHandle mh_vS,
              MethodHandle mh_vi,
              MethodHandle mh_vv) throws Throwable {
        Object o; String s; int i;  // for return type testing

        // next five must have sig = (String,int)Object
        mh_SiO.invokeExact("world", 123);
        mh_SiO.invokeExact("mundus", 456);
        Object k = "kosmos";
        mh_SiO.invokeExact((String)k, 789);
        o = mh_SiO.invokeExact((String)null, 000);
        o = (Object) mh_SiO.invokeExact("arda", -123);

        // sig = ()String
        s = (String) mh_vS.invokeExact();

        // sig = ()int
        i = (int) mh_vi.invokeExact();
        o = (int) mh_vi.invokeExact();

        // sig = ()void
        mh_vv.invokeExact();
    }

    void testGen(MethodHandle mh_SiO,
                 MethodHandle mh_vS,
                 MethodHandle mh_vi,
                 MethodHandle mh_vv) throws Throwable {
        Object o; String s; int i;  // for return type testing

        // next five must have sig = (*,*)*
        o = mh_SiO.invoke((Object)"world", (Object)123);
        mh_SiO.invoke((Object)"mundus", (Object)456);
        Object k = "kosmos";
        o = mh_SiO.invoke(k, 789);
        o = mh_SiO.invoke(null, 000);
        o = mh_SiO.invoke("arda", -123);

        // sig = ()String
        o = mh_vS.invoke();

        // sig = ()int
        i = (int) mh_vi.invoke();
        o = (int) mh_vi.invoke();
        mh_vi.invoke();

        // sig = ()void
        mh_vv.invoke();
        o = mh_vv.invoke();
    }
}
