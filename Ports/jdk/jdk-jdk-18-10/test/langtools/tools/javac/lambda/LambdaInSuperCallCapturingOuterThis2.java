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

/*
 * @test
 * @bug 8184989
 * @summary Incorrect class file created when passing lambda in inner class constructor and outer is subclass
 * @run main LambdaInSuperCallCapturingOuterThis2
 */

class A8184989_2 {
    public boolean test() {
        return true;
    }
    class AA {
        public AA(Condition8184989_2<AA> condition) {
            if (condition.check(this) != true) {
                throw new AssertionError("Incorrect output");
            }
        }
    }
}

interface Condition8184989_2<T> {
    boolean check(T t);
}

public class LambdaInSuperCallCapturingOuterThis2 extends A8184989_2 {
    public boolean test() {return false;}
    public void b() {}

    class C extends A8184989_2 {
        public class BA extends AA {
            public BA() {
                super(o -> {b(); return test();});
            }
        }
    }
    public static void main(String[] args) {
        new LambdaInSuperCallCapturingOuterThis2().new C().new BA();
    }
}
