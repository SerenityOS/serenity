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
 * @bug 8006582
 * @summary javac should generate method parameters correctly.
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @build MethodParametersTester ClassFileVisitor ReflectionVisitor
 * @compile -parameters AnonymousClass.java
 * @run main MethodParametersTester AnonymousClass AnonymousClass.out
 */

class AnonymousClass {

    interface I<T> {
        T m();
        T m(T x, T yx);
    }

    private class Inner implements I<String> {
        public Inner()  { }
        public Inner(String arg, String barg)  { }
        public String m() { return "0"; }
        public String m(String s, String ts) { return "0"; }
    }

    public static class Sinner implements I<Long> {
        public Sinner()  { }
        public Sinner(Long arg, Long barg)  { }
        public Long m() { return 0L; }
        public Long m(Long s, Long ts) { return s + ts; }
    }

    /** Inner class in constructor context */
    public AnonymousClass(final Long a, Long ba) {
        new I<Long>() {
            public Long m() { return null; }
            public Long m(Long i, Long ji) { return i + ji; }
        }.m(a, ba);
        new Inner() {
            public String m() { return null; }
            public String m(String i, String ji) { return i + ji; }
        }.m(a.toString(), ba.toString());
        new Inner(a.toString(), ba.toString()) {
            public String m() { return null; }
            public String m(String i, String ji) { return i + ji; }
        }.m(a.toString(), ba.toString());
        new Sinner() {
            public Long m() { return null; }
            public Long m(Long i, Long ji) { return i + ji; }
        }.m(a, ba);
        new Sinner(a, ba) {
            public Long m() { return null; }
            public Long m(Long i, Long ji) { return i + ji; }
        }.m(a, ba);
    }

    /** Inner class in method context */
    public void foo(final Long a, Long ba) {
        new I<Long>() {
            public Long m() { return null; }
            public Long m(Long i, Long ji) { return i + ji; }
        }.m(a, ba);
        new Inner() {
            public String m() { return null; }
            public String m(String i, String ji) { return i + ji; }
        }.m(a.toString(), ba.toString());
        new Inner(a.toString(), ba.toString()) {
            public String m() { return null; }
            public String m(String i, String ji) { return i + ji; }
        }.m(a.toString(), ba.toString());
        new Sinner() {
            public Long m() { return null; }
            public Long m(Long i, Long ji) { return i + ji; }
        }.m(a, ba);
        new Sinner(a, ba) {
            public Long m() { return null; }
            public Long m(Long i, Long ji) { return i + ji; }
        }.m(a, ba);
    }
}



