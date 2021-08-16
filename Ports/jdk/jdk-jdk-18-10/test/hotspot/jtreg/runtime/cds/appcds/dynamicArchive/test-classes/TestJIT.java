/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;
import java.util.HashMap;

public class TestJIT {

    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    static void doSomething() {
        HashMap<String,String> map = new HashMap<>();
        for (int i=0; i<400; i++) {
            // Call these methods so that the class/field/method references used
            // by these methods are resolved. This allows C2 to compile more code.
            String x = "Hello" + i;
            map.put(x, x);
            map.get(x);
        }
    }

    static public void main(String[] args) throws NoSuchMethodException {
        Method put_method = HashMap.class.getDeclaredMethod("put", Object.class, Object.class);
        Method get_method = HashMap.class.getDeclaredMethod("get", Object.class);
        Method test_method = TestJIT.class.getDeclaredMethod("doSomething");

        doSomething();

        // 4 == CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION => C2
        WHITE_BOX.enqueueMethodForCompilation(get_method, 4);
        WHITE_BOX.enqueueMethodForCompilation(put_method, 4);
        WHITE_BOX.enqueueMethodForCompilation(test_method, 4);

        // Try to start dynamic dumping while the above compilations are still in progesss
        System.exit(0);
    }
}
