/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8044748
 * @summary JVM cannot access constructor though ::new reference although can call it directly
 */

public class MethodRefNewInnerBootstrap {

    interface Constructor {
        public MyTest execute(int i);
    }

    public class MyTest {
        public MyTest(int i) { System.out.println("Constructor executed " + i); }
    }

    public Constructor getConstructor() {
        return MyTest::new;
    }

    public static void main(String argv[]) {
        new MethodRefNewInnerBootstrap().call();
    }

    public void call() {
        MyTest mt = new MyTest(0);

        Constructor c1 = MyTest::new;
        c1.execute(1);

        Constructor c2 = getConstructor();
        c2.execute(2);

        Constructor c3 = new Constructor() {
            public MyTest execute(int i) {
                return new MyTest(3);
            }
        };
        c3.execute(3);

        Constructor c4 = new Constructor() {
            public MyTest execute(int i) {
                Constructor c = MyTest::new;
                return c.execute(i);
            }
        };
        c4.execute(4);
    }
}
