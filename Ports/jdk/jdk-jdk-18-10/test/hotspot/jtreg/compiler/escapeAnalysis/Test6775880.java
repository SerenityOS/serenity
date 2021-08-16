/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6775880
 * @summary EA +DeoptimizeALot: assert(mon_info->owner()->is_locked(),"object must be locked now")
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -Xbatch -XX:+DoEscapeAnalysis -XX:+DeoptimizeALot
 *    -XX:CompileCommand=exclude,java.lang.AbstractStringBuilder::append
 *    compiler.escapeAnalysis.Test6775880
 */

package compiler.escapeAnalysis;

public class Test6775880 {

    int cnt;
    int b[];
    String s;

    String test() {
        String res = "";
        for (int i = 0; i < cnt; i++) {
            if (i != 0) {
                res = res + ".";
            }
            res = res + b[i];
        }
        return res;
    }

    public static void main(String[] args) {
        Test6775880 t = new Test6775880();
        t.cnt = 3;
        t.b = new int[3];
        t.b[0] = 0;
        t.b[1] = 1;
        t.b[2] = 2;
        int j = 0;
        t.s = "";
        for (int i = 0; i < 10001; i++) {
            t.s = "c";
            t.s = t.test();
        }
        System.out.println("After s=" + t.s);
    }
}


