/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase jit/escape/AdaptiveBlocking/AdaptiveBlocking001.
 * VM Testbase keywords: [jit, quick]
 * VM Testbase readme:
 * This is a basic JIT compiler adaptive blocking test. The test runs 2 threads
 * contending for a shared resource. In the first round, the lock is held for
 * a short period of time, provoking spinlock-style locking. In the second round,
 * the lock is held for a long period of time, provoking blocked locking. The
 * described scenario is repeated numRounds (specified by the -numRounds
 * parameter) times.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build jit.escape.AdaptiveBlocking.AdaptiveBlocking001.AdaptiveBlocking001
 * @run driver/timeout=300 ExecDriver --java -server -Xcomp -XX:+DoEscapeAnalysis
 *             jit.escape.AdaptiveBlocking.AdaptiveBlocking001.AdaptiveBlocking001 -numRounds 10
 */

package jit.escape.AdaptiveBlocking.AdaptiveBlocking001;

import nsk.share.TestFailure;

class AdaptiveBlocking001
{
        public static int numRounds = 10;

        private static Object sharedLock = new Object();
        private static boolean lockEntered1;
        private static boolean lockEntered2;

        private static boolean latentLocks;

        private static boolean done;

        private static boolean failed;

        public static void main( String[] args )
        {
                System.out.println( "Adaptive blocking test" );

                parseArgs( args );

                for( int i=0; i < numRounds; ++i )
                {
                        doRound(i, false);
                        doRound(i, true);
                }

                if( !failed )
                        System.out.println( "TEST PASSED" );
                else
                        throw new TestFailure( "TEST FAILED" );
        }

        private static void parseArgs( String[] args )
        {
                for( int i=0; i < args.length; ++i )
                {
                        String arg = args[i];
                        String val;

                        if( arg.equals( "-numRounds" ) )
                        {
                                if( ++i < args.length )
                                        val = args[i];
                                else
                                        throw new TestFailure( "Need value for '" + arg + "' parameter" );

                                try {
                                        numRounds = Integer.parseInt( val );
                                } catch( NumberFormatException e ) {
                                        throw new TestFailure( "Invalid value for '" + arg + "' parameter: " + val );
                                }
                        }
                        else
                        {
                                throw new TestFailure( "Invalid argument: " + args );
                        }
                }
        }

        private static void doRound(int ord, boolean latent_locks)
        {
                System.out.println( "round #" + ord + ", latent locks: " + (latent_locks ? "yes" : "no") + "..." );

                latentLocks = latent_locks;

                Thread_1 t1 = new Thread_1();
                Thread_2 t2 = new Thread_2();

                done = false;

                t1.start();
                t2.start();

                for( int i=0; i < 10; ++i )
                {
                        try {
                                Thread.sleep( 1000 );
                        } catch( InterruptedException e ) {
                        }

                        if( done )
                                break;
                }

                done = true;

                try {
                        t1.join();
                        t2.join();
                } catch( InterruptedException e ) {
                }
        }

        private static class Thread_1 extends Thread
        {
                public void run()
                {
                        while( !done )
                        {
                                synchronized( sharedLock )
                                {
                                        lockEntered1 = true;
                                        if( lockEntered2 )
                                        {
                                                Fail();
                                                done = true;
                                                break;
                                        }

                                        holdLock();

                                        lockEntered1 = false;
                                }
                        }
                }
        }

        private static class Thread_2 extends Thread
        {
                public void run()
                {
                        while( !done )
                        {
                                synchronized( sharedLock )
                                {
                                        lockEntered2 = true;
                                        if( lockEntered1 )
                                        {
                                                Fail();
                                                done = true;
                                                break;
                                        }

                                        holdLock();

                                        lockEntered2 = false;
                                }
                        }
                }
        }

        private static void holdLock()
        {
                if( latentLocks )
                {
                        try {
                                Thread.sleep( 500 );
                        } catch( InterruptedException e ) {
                        }
                }
        }

        private static void Fail()
        {
                failed = true;
        }
}
