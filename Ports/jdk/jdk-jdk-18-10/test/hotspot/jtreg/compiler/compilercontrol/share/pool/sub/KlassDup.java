/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.compilercontrol.share.pool.sub;

import compiler.compilercontrol.share.pool.MethodHolder;

/**
 * Simple class with methods to test signatures
 * This class has Dup suffix in the name to test suffix patterns like Klass*.
 * Such patterns should match both Klass and KlassDup
 */
public class KlassDup extends MethodHolder {
    public void method(int a, String[] ss, Integer i, byte[] bb, double[][] dd) { }

    public void method() { }

    public static String smethod() {
        return "ABC";
    }

    public static String smethod(int iarg, int[] aarg) {
        return "ABC";
    }

    public static Integer smethod(Integer arg) {
        Integer var = 1024;
        return arg + var;
    }
}
