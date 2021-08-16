/*
 * Copyright (c) 1998, 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4146462
 * @summary Priority inversion prevents call to the genSeed method from
 *      returning
 *
 * if the test returns, then it passed.
 * if the test never returns (hangs forever), then it failed.
 * @key randomness
 */

import java.security.SecureRandom;

public class Priority_Inversion {
    public static void main(String args[]) {
        int deltaPriority = 1;

        if (args.length == 1) {
            try {
                deltaPriority = Integer.parseInt(args[0]);
            }
            catch (NumberFormatException nfe) {
                System.err.println
                        ("Sorry, \"" + args[0] + "\" is not a number");
                System.exit(1);
            }
        }

        RandomTest rand = new RandomTest();
        InvertTest invert = new InvertTest(deltaPriority, rand);
        rand.start();
        invert.start();
    }
}

class RandomTest extends Thread {
    public synchronized void run() {
        System.out.println("Start priority " + getPriority());
        // The following should take over a second
        SecureRandom rand = new SecureRandom();
        rand.nextBytes(new byte[5]);
    }

    void invertPriority() {
        System.out.println("Waiting ..., priority " +
                Thread.currentThread().getPriority());
        synchronized(this) {
        }
        System.out.println("Released Lock");
    }
}
class InvertTest extends Thread {
    private int delta;
    private RandomTest rand;

    InvertTest(int delta, RandomTest rand) {
        this.delta = delta;
        this.rand = rand;
    }

    public void run() {
        setPriority(getPriority() + delta);
        try {
            sleep(500);
        }
        catch (InterruptedException ie) { }
        rand.invertPriority();
    }
}
