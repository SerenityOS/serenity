/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.compiler.share;

public class Random {
    private int current;

    public Random(int init) {
        this.current =  init;
    }

    //big prime number
    private static long BASE = 1003001;

    public int nextInt() {
        current = (int) ((long) current * current % BASE - 1);
        return current;
    }

    public int nextInt(int n) {
        return nextInt() % n;
    }


    /**
     * for testing purposes
     * @param args
     */
    public static void main(String[] args) {
        Random r = new Random(11);
        int[] a = new int[100];

        for(int i=0; i<1000; i++) {
            a[r.nextInt(100)]++;
        }

        for(int i=0; i<100; i++){
            System.out.println(times(a[i]));
        }

    }

    public static String times(int n) {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<n; i++) {
            sb.append("*");
        }
        return sb.toString();
    }
}
