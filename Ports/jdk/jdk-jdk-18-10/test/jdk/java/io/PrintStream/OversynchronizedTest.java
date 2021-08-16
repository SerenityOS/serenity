/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4905777
   @summary PrintStream.println(Object) oversynchronized, can deadlock
   @key randomness
*/

import java.io.PrintStream;

public class OversynchronizedTest extends Thread {
    private static TestObj testObj = new TestObj("This is a test.");
    private static int loopNum = 100;

    public void run() {
        for(int i=0; i<loopNum; i++) {
            testObj.test();

            //passing an object to System.out.println might cause deadlock
            //if the object has a synchronized toString() method.
            //using System.out.println(testObj.toString()) won't have a problem
           System.out.println(testObj);
        }
    }

    public static void main(String args[]) throws Exception {
        // should no NullPointerException
        System.out.println((Object)null);

        // over synch test
        int num = 5;

        OversynchronizedTest[] t = new OversynchronizedTest[num];
        for(int i=0; i<num; i++) {
            t[i] = new OversynchronizedTest();
            t[i].start();
        }

        for(int i=0; i <num; i++) {
            t[i].join();
        }

        System.out.println("Test completed.");
    }
}

class TestObj {
    String mStr;

    TestObj(String str) {
        mStr = str;
    }

    synchronized void test() {
        try {
            long t = Math.round(Math.random()*10);
            Thread.currentThread().sleep(t);
        } catch (InterruptedException e) {
            // jtreg timeout?
            // Only jtreg will interrupt this thread so it knows what to do:
            e.printStackTrace();
        }

        //the following line might cause hang if there is System.out.println(testObj)
        //called by other threads.
        System.out.println("In test().");
    }

    public synchronized String toString() {
        System.out.println("Calling toString\n");
        return mStr;
    }
}
