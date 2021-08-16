/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Basic functional test of remove method for ThreadLocal
 * @author Seetharama Avadhanam
 */

public class TLRemoveTest {
    private static final int INITIAL_VALUE = 101;
    private static final int REMOVE_SET_VALUE = 102;

    static ThreadLocal<Integer> n = new ThreadLocal<Integer>() {
        protected synchronized Integer initialValue() {
            return INITIAL_VALUE;
        }
    };

    public static void main(String args[]) throws Throwable {
        int threadCount = 100;
        final int[] removeNode = {10,20,45,38};
        // ThreadLocal values will be removed for these threads.
        final int[] removeAndSet = {12,34,10};
        // ThreadLocal values will be removed and sets new values..

        Thread th[] = new Thread[threadCount];
        final int x[] = new int[threadCount];
        final Throwable exceptions[] = new Throwable[threadCount];

        for(int i = 0; i<threadCount; i++) {
            final int threadId = i;
            th[i] = new Thread() {
                public void run() {
                    try{
                        n.set(threadId); // Sets threadId as threadlocal value...
                        for (int j = 0; j<threadId; j++)
                            Thread.currentThread().yield();

                        // To remove the ThreadLocal ....
                        for(int removeId  : removeNode)
                            if(threadId == removeId){
                               n.remove(); // Removes ThreadLocal values..
                               break;
                            }

                        // To remove the ThreadLocal value and set new value ...
                        for(int removeId  : removeAndSet)
                            if(threadId == removeId){
                               n.remove(); // Removes the ThreadLocal Value...
                               n.set(REMOVE_SET_VALUE); /* Setting new Values to
                                                          ThreadLocal */
                               break;
                            }
                            /* Storing the threadlocal values in 'x'
                               ...so that it can be used for checking results... */
                        x[threadId] = n.get();
                    }
                    catch(Throwable ex){
                        exceptions[threadId] = ex;
                    }
                }
            };
            th[i].start();
        }

        // Wait for the threads to finish
        for(int i = 0; i<threadCount; i++)
            th[i].join();

        // Check results
        for(int i = 0; i<threadCount; i++){
            int checkValue = i;

            /* If the remove method is called then the ThreadLocal value will
             * be its initial value */
            for(int removeId : removeNode)
                if(removeId == i){
                    checkValue = INITIAL_VALUE;
                    break;
                }

            for(int removeId : removeAndSet)
                if(removeId == i){
                    checkValue = REMOVE_SET_VALUE;
                    break;
                }

            if(exceptions[i] != null)
                throw(exceptions[i]);
            if(x[i] != checkValue)
                throw(new Throwable("x[" + i + "] =" + x[i]));
        }
    }
}
