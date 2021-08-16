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
 * @bug 8023112
 * @summary Mixing up the method type argument with the class type for method
 *          reference ClassType<Q>::<T>new
 * @compile SkipLazyConstantCreationForMethodRefTest.java
 */

public class SkipLazyConstantCreationForMethodRefTest<T> {
    SkipLazyConstantCreationForMethodRefTest(int a, boolean b) {}
    SkipLazyConstantCreationForMethodRefTest() {}
}

class SubClass<T> extends SkipLazyConstantCreationForMethodRefTest {
    SubClass(int a, boolean b) {}
}

interface SAM {
    SubClass<SkipLazyConstantCreationForMethodRefTest> m(int a, boolean b);
}

interface Tester1 {
    SAM s11 = SubClass<SkipLazyConstantCreationForMethodRefTest>::<Object>new;
    SAM s12 = (SubClass<SkipLazyConstantCreationForMethodRefTest>::<Object>new);
    SAM s13 = (SAM)SubClass<SkipLazyConstantCreationForMethodRefTest>::<Object>new;
    SAM s14 = true ? s11 : s12;
    SAM s15 = true ? s11 : (SAM)SubClass<SkipLazyConstantCreationForMethodRefTest>::<Object>new;
    SAM s16 = true ? (SAM)SubClass<SkipLazyConstantCreationForMethodRefTest>::<Object>new : s12;
}

interface Tester2 {
    SAM s21 = Tester1.s11;
}
