/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
 *
 * @summary converted from VM Testbase gc/gctests/gctest01.
 * VM Testbase keywords: [gc]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm gc.gctests.gctest01.gctest01 100 10
 */

package gc.gctests.gctest01;

import nsk.share.test.*;
import nsk.share.log.*;
import nsk.share.gc.*;
import nsk.share.TestBug;

//import RusageStruct;

/*  -- stress testing
 create 20 memory evil threads requesting to allocate
 the object of sizes from 8 to ( 2 ^ 19).
 The live time of objects is very short.
 Memory evil thread exits the first time memory allocation fails.
 */

class ThreadTracker {
        static int threadCount = 0;

        static synchronized int getThreadCount() {
                return threadCount;
        }

        static synchronized void setThreadCount(int count) {
                threadCount = count;
        }

        static synchronized void incr() {
                threadCount++;
        }

        static synchronized void decr() {
                threadCount--;
        }
}

class PopulationException extends Exception {
}

class Person {
        String name;
        int  ssid;
        int  age;
        int  buf[];
        int  bufsz;
        static int populationLimit;
        static int currentPopulation;

        public Person(String n, int ssid, int age, int bufsz) throws PopulationException {
                this.incr();
                if (this.getPopulation() > this.getPopulationLimit()) {
                        throw new PopulationException();
                }
                name = n;
                this.ssid = ssid;
                this.age = age;
                if ( bufsz > 0 ) {
                        this.bufsz = bufsz;
                        this.buf = new int[bufsz];
                }
        }

        static synchronized void incr() {
                currentPopulation++;
        }

        static synchronized int getPopulation() {
                return currentPopulation;
        }

        static synchronized void setPopulation(int census) {
                currentPopulation = census;
        }

        static synchronized void setPopulationLimit(int limit) {
                populationLimit = limit;
        }

        static synchronized int getPopulationLimit() {
                return populationLimit;
        }
}



// create 20 memory evil threads requesting to allocate
// the object of sizes from 8 to ( 2 ^ 19).
// The live time of objects is very short.
public class gctest01 extends TestBase {
        private String[] args;

        public gctest01(String[] args) {
                setArgs(args);
        }

        class memevil extends Thread {
                int sum;
                int bufsz = 64;

                public memevil(int bufsz) {
                        ThreadTracker.incr();
                        sum = 0;
                        this.bufsz = bufsz;

                }

                /* Person object is live short, it will be garbage after
                   control returns
                   */
                private boolean doit() {
                        try {
                                Person p = new Person("Duke", 100, 100, bufsz);
                        } catch (OutOfMemoryError e ) {
                                log.info(getName() + ": Out of Memory");
                                return false; //should free up some memory
                        } catch (PopulationException e) {
                                //we've reached the limit, so stop
                                return false;
                        }
                        return true;
                }

                public void run() {
                        while ( doit() ) {
                                if ( LocalRandom.random() > 0.6668) {
                                        try {
                                                sleep(10);   // to be nice
                                        }
                                        catch (InterruptedException e) {}
                                }
                        }
                        //must be done, decrement the thread count
                        ThreadTracker.decr();
                }
        }

        class escaper extends Thread {
                public void run() {
                        while ( ThreadTracker.getThreadCount() > 0 ) {
                                int buf[] = new int[32];
                                try
                                {
                                        Thread.currentThread().sleep(1000);
                                } catch (InterruptedException e) {
                                }
                                // log.info("Is the sun rising?");
                        }
                }
        }


        public void run() {
                int bufsz = 8;
                int i = 3;
                int peopleLimit = 1000;
                String usage = "usage: gctest01 [NumberOfObjects [Iterations] ] ]";
                int loops;
                int LOOPCOUNT = 10;

                if (args.length > 0) {
                        try {
                                peopleLimit = Integer.valueOf(args[0]).intValue();
                        } catch (NumberFormatException e) {
                                log.info(usage);
                                throw new TestBug("Bad input to gctest01." +
                                                " Expected integer, got: ->" + args[0] + "<-", e);
                        }
                }

                if (args.length > 1 ) {
                        try {
                                LOOPCOUNT = Integer.valueOf(args[1]).intValue();
                        } catch (NumberFormatException e) {
                                log.error(usage);
                                throw new TestBug("Bad input to gctest01." +
                                                " Expected int, got: ->" + args[1] + "<-", e);
                        }

                }

                double before = 0.0;
                double after;

                Person.setPopulationLimit(peopleLimit);

                for (loops = 0; loops < LOOPCOUNT; loops++) {
                        Person.setPopulation(0);
                        escaper you = new escaper();
                        you.setName("Escaper");
                        you.start();
                        i = 3;
                        bufsz = 8;
                        while ( i < 20  )
                        {
                                memevil me = new memevil(bufsz);
                                me.setName("Memevil" + bufsz);
                                bufsz = 2*bufsz;
                                me.start();
                                i++;
                                Thread.currentThread().yield();
                        }
                        try
                        {
                                you.join();
                        }
                        catch (InterruptedException e)
                        {
                                e.printStackTrace();
                        }
                } // end of loops
                log.info("Test passed.");

        }

        public void setArgs(String[] args) {
                this.args = args;
        }

        public static void main(String args[]) {
                GC.runTest(new gctest01(args), args);
        }
}
