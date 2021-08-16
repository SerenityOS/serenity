/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 4882798
 * @summary multi-thread test to exercise sync and contention for removes to transformer registry
 * @author Gabriel Adauto, Wily Technology
 *
 * @run build TransformerManagementThreadRemoveTests
 * @run shell MakeJAR.sh redefineAgent
 * @run main/othervm -javaagent:redefineAgent.jar TransformerManagementThreadRemoveTests TransformerManagementThreadRemoveTests
 */
import java.util.*;

public class TransformerManagementThreadRemoveTests
    extends TransformerManagementThreadAddTests
{

    /**
     * Constructor for TransformerManagementThreadRemoveTests.
     * @param name
     */
    public TransformerManagementThreadRemoveTests(String name)
    {
        super(name);
    }

    public static void
    main (String[] args)
        throws Throwable {
        ATestCaseScaffold   test = new TransformerManagementThreadRemoveTests(args[0]);
        test.runTest();
    }

    protected final void
    doRunTest()
        throws Throwable {
        testMultiThreadAddsAndRemoves();
    }

    public void
    testMultiThreadAddsAndRemoves()
    {
        int size = TOTAL_THREADS + REMOVE_THREADS;
        ArrayList threadList = new ArrayList(size);
        for (int i = MIN_TRANS; i <= MAX_TRANS; i++)
        {
            int index = i - MIN_TRANS;
            threadList.add(new TransformerThread("Trans"+prettyNum(index,2), i));
        }

        int factor = (int)Math.floor(TOTAL_THREADS / REMOVE_THREADS);
        for (int i = 0; i < REMOVE_THREADS; i++)
        {
            threadList.add(factor * i, new RemoveThread("Remove"+i));
        }

        Thread[] threads = (Thread[])threadList.toArray(new Thread[size]);
        setExecThread(new ExecuteTransformersThread());
        getExecThread().start();
        for (int i = threads.length - 1; i >= 0; i--)
        {
            threads[i].start();
        }

        while (!testCompleted())
        {
            // Effective Java - Item 51: Don't depend on the thread scheduler
            // Use sleep() instead of yield().
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
            }
        }
        assertTrue(finalCheck());

        //printTransformers();
    }

    /**
     * Method removeTransformer.
     */
    private void
    removeTransformer(Thread t)
    {
        ThreadTransformer tt = null;

        synchronized (fAddedTransformers)
        {
            int size = fAddedTransformers.size();
            if (size > 0)
            {
                int choose = (int)Math.floor(Math.random() * size);
                tt = (ThreadTransformer)fAddedTransformers.remove(choose);
            }
            //System.out.println("removed("+tt+") size("+size+") chose("+choose+") by("+t+")");
        }

        if (tt != null)
        {
            getInstrumentation().removeTransformer(tt);
        }
    }

    private class
    RemoveThread
        extends Thread
    {

        RemoveThread(String name)
        {
            super(name);
        }

        public void
        run()
        {
            while (!threadsDone())
            {
                removeTransformer(RemoveThread.this);
            }
        }
    }

}
