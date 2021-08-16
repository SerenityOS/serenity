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
 * @summary Basic functional test of remove method for InheritableThreadLocal
 * @author Seetharama Avadhanam
 */

public class ITLRemoveTest {
    private static final int INITIAL_VALUE = Integer.MIN_VALUE;
    private static final int REMOVE_SET_VALUE = Integer.MAX_VALUE;

    static InheritableThreadLocal<Integer> n = new InheritableThreadLocal<Integer>() {
        protected Integer initialValue() {
            return INITIAL_VALUE;
        }

        protected Integer childValue(Integer parentValue) {
            return(parentValue + 1);
        }
    };

    static int threadCount = 100;
    static int x[];
    static Throwable exceptions[];
    static final int[] removeNode = {10,20,45,38,88};
    /* ThreadLocal values will be removed for these threads. */
    static final int[] removeAndSet = {12,34,10};
    /* ThreadLocal values will be removed and sets new values */

    public static void main(String args[]) throws Throwable {
        x = new int[threadCount];
        exceptions = new Throwable[threadCount];

        Thread progenitor = new MyThread();
        progenitor.start();

        // Wait for *all* threads to complete
        progenitor.join();

        for(int i = 0; i<threadCount; i++){
            int checkValue = i+INITIAL_VALUE;

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
    private static class MyThread extends Thread {
        public void run() {

            Thread child = null;
            int threadId=0;
            try{
                threadId = n.get();
                // Creating child thread...
                if (threadId < (threadCount-1+INITIAL_VALUE)) {
                    child = new MyThread();
                    child.start();
                }

                for (int j = 0; j<threadId; j++)
                    Thread.currentThread().yield();


                // To remove the ThreadLocal value...
                for(int removeId  : removeNode)
                   if((threadId-INITIAL_VALUE) == removeId){
                       n.remove();
                       break;
                   }

                 // To remove the ThreadLocal value and set new value ...
                 for(int removeId  : removeAndSet)
                    if((threadId-INITIAL_VALUE) == removeId){
                        n.remove();
                        n.set(REMOVE_SET_VALUE);
                        break;
                    }
                x[threadId-INITIAL_VALUE] =  n.get();
            }catch(Throwable ex){
                exceptions[threadId-INITIAL_VALUE] = ex;
            }
             // Wait for child (if any)
            if (child != null) {
                try {
                     child.join();
                } catch(InterruptedException e) {
                     throw(new RuntimeException("Interrupted"));
                }
            }
        }
    }
}
