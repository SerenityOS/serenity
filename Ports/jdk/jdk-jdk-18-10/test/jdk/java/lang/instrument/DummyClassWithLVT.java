/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

public class DummyClassWithLVT
{
    public static void main(String[] args) {
        boolean b  = true;
        byte    by = 0x42;
        char    c  = 'X';
        double  d  = 1.1;
        float   f  = (float) 1.2;
        int     i  = 42;
        long    l  = 0xCAFEBABE;
        short   s  = 88;

        System.out.println("b=" + b);
        System.out.println("by=" + by);
        System.out.println("c=" + c);
        System.out.println("d=" + d);
        System.out.println("f=" + f);
        System.out.println("i=" + i);
        System.out.println("l=" + l);
        System.out.println("s=" + s);
    }
}
