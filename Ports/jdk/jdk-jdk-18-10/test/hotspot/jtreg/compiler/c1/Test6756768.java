/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6756768
 * @summary C1 generates invalid code
 *
 * @run main/othervm -Xcomp compiler.c1.Test6756768
 */

package compiler.c1;

class Test6756768a
{
    static boolean var_1 = true;
}

final class Test6756768b
{
    static boolean var_24 = false;
    static int var_25 = 0;

    static boolean var_temp1 = Test6756768a.var_1 = false;
}

public final class Test6756768 extends Test6756768a
{
    final static int var = var_1 ^ (Test6756768b.var_24 ? var_1 : var_1) ? Test6756768b.var_25 : 1;

    static public void main(String[] args) {
        if (var != 0) {
            throw new InternalError("var = " + var);
        }
    }

}
