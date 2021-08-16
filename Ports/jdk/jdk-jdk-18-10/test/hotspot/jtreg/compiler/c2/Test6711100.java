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

/*
 * @test
 * @bug 6711100
 * @summary 64bit fastdebug server vm crashes with assert(_base == Int,"Not an Int")
 *
 * @run main/othervm -Xcomp
 *      -XX:CompileCommand=compileonly,compiler.c2.Test6711100::<init>
 *      compiler.c2.Test6711100
 */

package compiler.c2;

public class Test6711100 {

    static byte b;

    // The server compiler chokes on compiling
    // this method when f() is not inlined
    public Test6711100() {
        b = (new byte[1])[(new byte[f()])[-1]];
    }

    protected static int f() {
      return 1;
    }

    public static void main(String[] args) {
      try {
        Test6711100 t = new Test6711100();
      } catch (ArrayIndexOutOfBoundsException e) {
      }
    }
}


