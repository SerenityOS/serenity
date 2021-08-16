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
 * @compile -parameters StaticMethods.java
 * @run main MethodParametersTester StaticMethods StaticMethods.out
 */

public class StaticMethods {
    static public void empty() {}
    static final void def(Object a, final Object ba, final String... cba) { }
    static final public void pub(Object d, final Object ed, final String... fed) { }
    static protected boolean prot(Object g, final Object hg, final String... ihg) { return true; }
    static private boolean priv(Object j, final Object kj, final String... lkj) { return true; }
    static void def(int a, Object ba, final Object cba, final String... dcba) { }
    static public void pub(int a, Object ba, final Object cba , final String... dcba) { }
    static final protected boolean prot(int aa, Object baa, final Object cbaa, final String... dcbaa) { return true; }
    static final private boolean priv(int abc, Object babc, final Object cbabc, final String... dcbabc) { return true; }
}



