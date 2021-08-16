/*
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @bug 6941130
 * @summary Numeric overflow/underflow of permits causes Error throw
 */

import java.util.concurrent.Semaphore;

public class PermitOverflow {

    public static void main(String[] args) throws Throwable {
        for (boolean fair : new boolean[] { true, false }) {
            Semaphore sem = new Semaphore(Integer.MAX_VALUE - 1, fair);
            if (sem.availablePermits() != Integer.MAX_VALUE - 1)
                throw new RuntimeException();
            try {
                sem.release(2);
                throw new RuntimeException();
            } catch (Error expected) {
            }
            sem.release(1);
            if (sem.availablePermits() != Integer.MAX_VALUE)
                throw new RuntimeException();
            try {
                sem.release(1);
                throw new RuntimeException();
            } catch (Error expected) {
            }
            try {
                sem.release(Integer.MAX_VALUE);
                throw new RuntimeException();
            } catch (Error expected) {
            }
        }

        class Sem extends Semaphore {
            public Sem(int permits, boolean fair) {
                super(permits, fair);
            }
            public void reducePermits(int reduction) {
                super.reducePermits(reduction);
            }
        }

        for (boolean fair : new boolean[] { true, false }) {
            Sem sem = new Sem(Integer.MIN_VALUE + 1, fair);
            if (sem.availablePermits() != Integer.MIN_VALUE + 1)
                throw new RuntimeException();
            try {
                sem.reducePermits(2);
                throw new RuntimeException();
            } catch (Error expected) {
            }
            sem.reducePermits(1);
            if (sem.availablePermits() != Integer.MIN_VALUE)
                throw new RuntimeException();
            try {
                sem.reducePermits(1);
                throw new RuntimeException();
            } catch (Error expected) {
            }
            try {
                sem.reducePermits(Integer.MAX_VALUE);
                throw new RuntimeException();
            } catch (Error expected) {
            }
        }
    }
}
