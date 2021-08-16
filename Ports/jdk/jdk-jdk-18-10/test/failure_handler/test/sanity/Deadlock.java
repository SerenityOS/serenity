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


/*
 * @test
 * @summary Deadlocked client
 */
public class Deadlock {
    public double e;
    private volatile int i;

    public static void main(String[] args) {
        new Deadlock().test();
    }
    private void test() {
        final Object a = new Object();
        final Object b = new Object();

        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (a) {
                    do {
                        i |= 1;
                    } while (i != 3);

                    synchronized (b) {
                        e = 1;
                    }
                }
            }}).start();

        synchronized (b) {
            do {
                i |= 2;
            } while (i != 3);
            synchronized (a) {
                e = 2;
            }
        }
    }
}
