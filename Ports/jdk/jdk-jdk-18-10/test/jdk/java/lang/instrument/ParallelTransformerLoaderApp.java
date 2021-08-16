/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 *      Test Java Program
 *
 *      @author Daryl Puryear
 *      @copyright 1999-2004 Wily Technology, Inc.  All rights reserved.
 */
public class ParallelTransformerLoaderApp
{
        private static final int kNumIterations = 1000;

        public static void
        main(   String[] args)
                throws Exception
        {
                System.out.println();
                System.out.print("Starting test with " + kNumIterations + " iterations");
                for (int i = 0; i < kNumIterations; i++)
                {
                        // load some classes from multiple threads (this thread and one other)
                        Thread testThread = new TestThread(2);
                        testThread.start();
                        loadClasses(1);

                        // log that it completed and reset for the next iteration
                        testThread.join();
                        System.out.print(".");
                        ParallelTransformerLoaderAgent.generateNewClassLoader();
                }

                System.out.println();
                System.out.println("Test completed successfully");
        }

        private static class TestThread
                extends Thread
        {
                private final int fIndex;

                public
                TestThread(     int index)
                {
                        super("TestThread");

                        fIndex = index;
                }

                public void
                run()
                {
                        loadClasses(fIndex);
                }
        }

        public static void
        loadClasses( int index)
        {
                ClassLoader loader = ParallelTransformerLoaderAgent.getClassLoader();
                try
                {
                        Class.forName("TestClass" + index, true, loader);
                }
                catch (Exception e)
                {
                        e.printStackTrace();
                }
        }
}
