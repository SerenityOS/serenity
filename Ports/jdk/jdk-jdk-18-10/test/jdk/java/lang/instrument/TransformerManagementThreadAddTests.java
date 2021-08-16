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
 * @summary multi-thread test to exercise sync and contention for adds to transformer registry
 * @author Gabriel Adauto, Wily Technology
 *
 * @run build TransformerManagementThreadAddTests
 * @run shell MakeJAR.sh redefineAgent
 * @run main/othervm -javaagent:redefineAgent.jar TransformerManagementThreadAddTests TransformerManagementThreadAddTests
 */
import java.io.*;
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.*;

public class TransformerManagementThreadAddTests extends ATestCaseScaffold
{
    public static void
    main (String[] args)
        throws Throwable {
        ATestCaseScaffold   test = new TransformerManagementThreadAddTests(args[0]);
        test.runTest();
    }

    protected void
    doRunTest()
        throws Throwable {
        testMultiThreadAdds();
    }


    /**
     * CONFIGURATION FOR TEST
     * ----------------------
     * Set these variables to different values to test the object that
     * manages the transformers.
     *
     * MIN_TRANS: the minimum number of transformers to add by a thread
     * MAX_TRANS: the maximum number of transformers to add by a thread
     *      There will be a total of MAX_TRANS-MIN_TRANS+1 threads created.
     *      Each thread will add between MIN_TRANS and MAX_TRANS transformers
     *      to the manager.
     *
     * REMOVE_THREADS: the number of threads to run that spend their time
     *                  removing transformers
     */
    protected static final int MIN_TRANS = 33;
    protected static final int MAX_TRANS = 45;
    protected static final int REMOVE_THREADS = 5;

    protected static final boolean LOG_TRANSFORMATIONS = false;

    /**
     * Field variables
     */
    protected static final int TOTAL_THREADS = MAX_TRANS - MIN_TRANS + 1;

    private byte[]          fDummyClassBytes;
    // fCheckedTransformers is a Vector that is used to verify
    // that the transform() function is called in the same
    // order in which the transformers were added to the
    // TransformerManager. The test currently verifies that all
    // transformers for a specific worker thread are in
    // increasing order by index value.
    private Vector              fCheckedTransformers;
    private Instrumentation fInstrumentation;
    private int             fFinished;
    private ExecuteTransformersThread fExec;

    // Need to use this for synchronization in subclass
    protected Vector            fAddedTransformers;
    private String          fDummyClassName;

    /**
     * Constructor for TransformerManagementThreadAddTests.
     * @param name  Name for the test
     */
    public TransformerManagementThreadAddTests(String name)
    {
        super(name);

        fCheckedTransformers = new Vector();
        fAddedTransformers = new Vector();

        fDummyClassName = "DummyClass";
        String resourceName = "DummyClass.class";
        File f = new File(System.getProperty("test.classes", "."), resourceName);
        System.out.println("Reading test class from " + f);
        try
        {
            InputStream redefineStream = new FileInputStream(f);
            fDummyClassBytes = NamedBuffer.loadBufferFromStream(redefineStream);
        }
        catch (IOException e)
        {
            fail("Could not load the class: "+resourceName);
        }
    }

    public void
    testMultiThreadAdds()
    {
        TransformerThread[] threads = new TransformerThread[TOTAL_THREADS];
        for (int i = MIN_TRANS; i <= MAX_TRANS; i++)
        {
            int index = i - MIN_TRANS;
            threads[index] = new TransformerThread("Trans"+prettyNum(index,2), i);
        }

        ExecuteTransformersThread exec = new ExecuteTransformersThread();
        exec.start();
        for (int i = threads.length - 1; i >= 0; i--)
        {
            threads[i].start();
        }

        // Effective Java - Item 48: Synchronize access to shared mutable data
        // Don't use a direct field getter.
        while (!exec.isDone())
        {
            // Effective Java - Item 51: Don't depend on the thread scheduler
            // Use sleep() instead of yield().
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
            }
        }
        assertTrue(finalCheck());

        if (LOG_TRANSFORMATIONS) {
            printTransformers();
        }
    }

    /**
     * Returns the Instrumentation.
     * @return Instrumentation  the data type with JPLIS calls
     */
    public Instrumentation getInstrumentation()
    {
        return fInstrumentation;
    }

    /**
     * Returns the execution thread
     * @return ExecuteTransformersThread
     */
    protected ExecuteTransformersThread getExecThread()
    {
        return fExec;
    }

    /**
     * Sets the execution thread
     * @param exec The execution thread to set
     */
    protected void setExecThread(ExecuteTransformersThread exec)
    {
        this.fExec = exec;
    }

    // Effective Java - Item 48: Synchronize access to shared mutable data
    // Document a synchronized setter.
    protected synchronized void
    threadFinished(Thread t)
    {
        fFinished++;
    }

    // Effective Java - Item 48: Synchronize access to shared mutable data
    // Provide synchronized getter.
    protected synchronized boolean
    threadsDone()
    {
        return fFinished == TOTAL_THREADS;
    }

    /**
     * Method testCompleted.
     * @return boolean
     */
    protected boolean
    testCompleted()
    {
        // Effective Java - Item 48: Synchronize access to shared mutable data
        // Don't use direct field getter.
        return getExecThread().isDone();
    }

    /**
     *
     */
    protected boolean
    finalCheck()
    {
        if (LOG_TRANSFORMATIONS) {
            // log the list
            for (int x = 0; x < fCheckedTransformers.size(); x++ ) {
                System.out.println(x + "\t\t" + fCheckedTransformers.get(x));
            }
            System.out.println();
            System.out.println();

            // check for multiples
            for (int x = 0; x < fCheckedTransformers.size(); x++ ) {
                Object current = fCheckedTransformers.get(x);
                for ( int y = x + 1; y < fCheckedTransformers.size(); y++) {
                    Object running = fCheckedTransformers.get(y);
                    if ( current.equals(running) ) {
                        System.out.println(x + "\t" + y + " \t" + "FOUND DUPLICATE: " + current);
                    }
                }
            }
        }

        for (int j = 1; j < fCheckedTransformers.size(); j++) {
            ThreadTransformer transformer = (ThreadTransformer)fCheckedTransformers.get(j);
            for (int i = 0; i < j; i++) {
                ThreadTransformer currTrans = (ThreadTransformer)fCheckedTransformers.get(i);
                assertTrue(currTrans + " incorrectly appeared before " +
                           transformer + " i=" + i + " j=" + j + " size=" +
                           fCheckedTransformers.size(),
                           !(
                             currTrans.getThread().equals(transformer.getThread()) &&
                             currTrans.getIndex() > transformer.getIndex()));
            }
        }
        return true;
    }

    /**
     *
     */
    protected void
    setUp()
        throws Exception
    {
        super.setUp();

        fFinished = 0;
        assertTrue(MIN_TRANS < MAX_TRANS);
        fInstrumentation = InstrumentationHandoff.getInstrumentationOrThrow();
    }

    /**
     *
     */
    protected void
    tearDown()
        throws Exception
    {
        super.tearDown();
    }



    /**
     * Method executeTransform.
     */
    private void
    executeTransform()
    {
        try
        {
            ClassDefinition cd = new ClassDefinition(DummyClass.class, fDummyClassBytes);

            // When the ClassDefinition above is created for the first
            // time and every time redefineClasses() below is called,
            // the transform() function is called for each registered
            // transformer. We only want one complete set of calls to
            // be logged in the fCheckedTransformers Vector so we clear
            // any calls logged for ClassDefinition above and just use
            // the ones logged for redefineClasses() below.
            fCheckedTransformers.clear();

            getInstrumentation().redefineClasses(new ClassDefinition[]{ cd });
        }
        catch (ClassNotFoundException e)
        {
            fail("Could not find the class: "+DummyClass.class.getName());
        }
        catch (UnmodifiableClassException e)
        {
            fail("Could not modify the class: "+DummyClass.class.getName());
        }
    }

    /**
     * Method addTransformerToManager.
     * @param threadTransformer
     */
    private void
    addTransformerToManager(ThreadTransformer threadTransformer)
    {
        getInstrumentation().addTransformer(threadTransformer);
        fAddedTransformers.add(threadTransformer);
    }

    /**
     * Method checkInTransformer.
     * @param myClassFileTransformer
     */
    private void
    checkInTransformer(ThreadTransformer transformer)
    {
        fCheckedTransformers.add(transformer);
    }

    /**
     * Method createTransformer.
     * @param transformerThread
     * @param i
     * @return ThreadTransformer
     */
    private ThreadTransformer
    createTransformer(TransformerThread thread, int index)
    {
        ThreadTransformer tt = null;

        tt = new ThreadTransformer(thread, index);

        return tt;
    }


    protected class
    ExecuteTransformersThread
        extends Thread
    {
        private boolean fDone = false;

        // Effective Java - Item 48: Synchronize access to shared mutable data
        // Provide a synchronized getter.
        private synchronized boolean isDone() {
            return fDone;
        }

        // Effective Java - Item 48: Synchronize access to shared mutable data
        // Provide a synchronized setter.
        private synchronized void setIsDone() {
            fDone = true;
        }

        public void
        run()
        {
            while(!threadsDone())
            {
                executeTransform();
            }

            // Do a final check for good measure
            executeTransform();
            // Effective Java - Item 48: Synchronize access to shared mutable data
            // Don't use direct field setter.
            setIsDone();
        }
    }


    protected class
    TransformerThread
        extends Thread
    {
        private final ThreadTransformer[] fThreadTransformers;

        TransformerThread(String name, int numTransformers)
        {
            super(name);

            fThreadTransformers = makeTransformers(numTransformers);
        }

        /**
         * Method makeTransformers.
         * @param numTransformers
         * @return ThreadTransformer[]
         */
        private ThreadTransformer[]
        makeTransformers(int numTransformers)
        {
            ThreadTransformer[] trans = new ThreadTransformer[numTransformers];

            for (int i = 0; i < trans.length; i++)
            {
                trans[i] = createTransformer(TransformerThread.this, i);
            }

            return trans;
        }

        public void
        run()
        {
            for (int i = 0; i < fThreadTransformers.length; i++)
            {
                addTransformerToManager(fThreadTransformers[i]);
            }
            threadFinished(TransformerThread.this);
        }
    }

    /**
     * ClassFileTransformer implementation that knows its thread
     */
    protected class
    ThreadTransformer extends SimpleIdentityTransformer
    {
        private final String    fName;
        private final int       fIndex;
        private final Thread    fThread;

        /**
         * Constructor for ThreadTransformer.
         */
        public ThreadTransformer(Thread thread, int index) {
            super();
            fThread = thread;
            fIndex = index;
            fName = "TT["+fThread.getName()+"]["+prettyNum(fIndex,3)+"]";
        }

        public String toString()
        {
            return fName;
        }

        /**
         *
         */
        public byte[]
        transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain domain,
            byte[] classfileBuffer)
        {
            if ( className.equals(TransformerManagementThreadAddTests.this.fDummyClassName) ) {
                checkInTransformer(ThreadTransformer.this);
            }
            return super.transform(    loader,
                                        className,
                                        classBeingRedefined,
                                        domain,
                                        classfileBuffer);
        }

        /**
         * Returns the index.
         * @return int
         */
        public int getIndex()
        {
            return fIndex;
        }

        /**
         * Returns the thread.
         * @return Thread
         */
        public Thread getThread()
        {
            return fThread;
        }

    }

    /**
     * DEBUG STUFF
     */
    private int NUM_SWITCHES;

    /**
     * Method printTransformers.
     */
    protected void printTransformers()
    {
        NUM_SWITCHES = 0;
        Iterator trans = fCheckedTransformers.iterator();
        ThreadTransformer old = null;
        StringBuffer buf = new StringBuffer();

        for (int i = 1; trans.hasNext(); i++)
        {
            ThreadTransformer t = (ThreadTransformer)trans.next();
            buf.append(t.toString());
            if (old != null)
            {
                if (!old.getThread().equals(t.getThread()))
                {
                    NUM_SWITCHES++;
                    buf.append("*");
                }
                else
                { buf.append(" "); }
            }
            else
            { buf.append(" "); }

            if (i % 5 == 0)
            {
                buf.append("\n");
            }
            else
            { buf.append(" "); }

            old = t;
        }
        System.out.println(buf);
        System.out.println("\nNumber of transitions from one thread to another: "+NUM_SWITCHES);
    }

    protected String
    prettyNum(int n, int numSize)
    {
        StringBuffer num = new StringBuffer(Integer.toString(n));
        int size = num.length();
        for (int i = 0; i < numSize - size; i++)
        {
            num.insert(0, "0");
        }

        return num.toString();
    }
    /**
     * END DEBUG STUFF
     */

}
