/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4057345 4120016 4120014
 * @summary try-catch 2:  Verify that overzealous dead-code elimination no
 * longer removes live code.
 * @author dps
 *
 * @run clean DeadCode2
 * @run compile -O DeadCode2.java
 * @run main DeadCode2
 */

class cls
{
   int [] ai = null;
}

public class DeadCode2 extends cls
{
    int [] bi = null;

    static int[] func()  {  return (int[])null; }

    public static void main(String argv[]) {
        int [] ci = null;
        int m = 0;
        int errcnt = 0;

        try { int i = func()[m = 7]; }
        catch(Exception e) {  System.out.println(e + " found "); errcnt++; }
        try { DeadCode2 ox = new DeadCode2(); int i = ox.ai[m = 7]; }
        catch(Exception e) {  System.out.println(e + " found "); errcnt++; }
        try { DeadCode2 ox = new DeadCode2(); int i = ox.bi[m = 7]; }
        catch(Exception e) {  System.out.println(e + " found "); errcnt++; }
        try { int i = ci[m = 7]; }
        catch(Exception e) {  System.out.println(e + " found "); errcnt++; }
        try { int i = ((int[])null)[0]; }
        catch(Exception e) {  System.out.println(e + " found "); errcnt++; }

        if (errcnt != 5)
            throw new RuntimeException("live code accidentally removed");
    }
}
