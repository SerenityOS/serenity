/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8006582 8008658
 * @summary javac should generate method parameters correctly.
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @build MethodParametersTester ClassFileVisitor ReflectionVisitor
 * @compile -parameters MemberClassTest.java
 * @run main MethodParametersTester MemberClassTest MemberClassTest.out
 */

class MemberClassTest {

    interface I {
        Long m();
        Long m(Long x, Long yx);
    }

    public class Member implements I {
        public class Member_Member {
            public Member_Member() {}
            public Member_Member(String x, String yx) {}
        }

        public Member()  { }
        public Member(Long a, Long ba)  { }
        public Long m() { return 0L; }
        public Long m(Long s, Long ts) { return 0L; }
    }

    static class Static_Member implements I {
        public class Static_Member_Member {
            public Static_Member_Member() {}
            public Static_Member_Member(String x, String yx) {}
        }

        public static class Static_Member_Static_Member {
            public Static_Member_Static_Member() {}
            public Static_Member_Static_Member(String x, String yx) {}
        }
        public Static_Member()  { }
        public Static_Member(Long arg, Long barg)  { }
        public Long m() { return 0L; }
        public Long m(Long s, Long ts) { return s + ts; }
    }

    public MemberClassTest() {
    }
    public MemberClassTest(final Long a, Long ba) {
    }

    public void foo() {

        new I() {

            class Anonymous_Member {
                public Anonymous_Member() {}
                public Anonymous_Member(String x, String yx) {}
            }

            public Long m() { return 0L; }
            public Long m(Long s, Long ts) { return s + ts; }
        }.m();
    }
}
