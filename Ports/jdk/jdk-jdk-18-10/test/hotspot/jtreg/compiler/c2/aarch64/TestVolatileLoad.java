/*
 * Copyright (c) 2018, Red Hat, Inc. All rights reserved.
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

package compiler.c2.aarch64;

class TestVolatileLoad
{
    public volatile int f_int = 0;
    public volatile Integer f_obj = Integer.valueOf(0);

    public static void main(String[] args)
    {
        final TestVolatileLoad t = new TestVolatileLoad();
        for (int i = 0; i < 100_000; i++) {
            t.f_int = i;
            int r = t.testInt();
            if (r != i) {
                throw new RuntimeException("bad result!");
            }
        }
        for (int i = 0; i < 100_000; i++) {
            t.f_obj = Integer.valueOf(i);
            int r = t.testObj();
            if (r != i) {
                throw new RuntimeException("bad result!");
            }
        }
    }
    public int testInt()
    {
        return f_int;
    }

    public int testObj()
    {
        return f_obj;
    }
}
