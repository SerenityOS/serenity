/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6186134
 * @summary Server virtual machine produces/exeutes incorrect code.
 *
 * @run main compiler.c2.Test6186134 100000
 */

package compiler.c2;

import java.util.ArrayList;

public class Test6186134 {

    int num = 0;

    public Test6186134(int n) {
        num = n;
    }

    public boolean more() {
        return num-- > 0;
    }

    public ArrayList test1() {
        ArrayList res = new ArrayList();
        int maxResults = Integer.MAX_VALUE;
        int n = 0;
        boolean more = more();
        while ((n++ < maxResults) && more) {
            res.add(new Object());
            more = more();
        }
        return res;
    }

    public static void main(String[] pars) {
        int n = Integer.parseInt(pars[0]);
        for (int i = 0; i < n; i++) {
            Test6186134 t = new Test6186134(10);
            int size = t.test1().size();
            if (size != 10) {
                System.out.println("wrong size: " + size + ", should be 10");
                System.exit(97);
            }
        }
        System.out.println("Passed");
    }
}

