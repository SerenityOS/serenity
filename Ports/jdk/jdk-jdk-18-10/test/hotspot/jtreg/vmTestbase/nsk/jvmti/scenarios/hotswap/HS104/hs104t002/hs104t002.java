/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/hotswap/HS104/hs104t002.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine, feature_hotswap]
 * VM Testbase readme:
 * Description :
 * Case Name : Hotswap  hs104t002:
 * class(es) in asynchronous manner within an JVMTI event
 * when all fields (changed initial values), methods (changed bodies),
 * attributes and the constant pool are changed in new class(es).
 * Comments :
 * All the threads would be invoking redefineClass method asynchronously,
 * and redefine should be possible without any errors. Any error while redefining
 * class is considered as test failure.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.hotswap.HS104.hs104t002.hs104t002
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass00
 *
 * @run main/othervm/native
 *      -agentlib:hs104t002=pathToNewByteCode=./bin
 *      nsk.jvmti.scenarios.hotswap.HS104.hs104t002.hs104t002
 */

package nsk.jvmti.scenarios.hotswap.HS104.hs104t002;

import nsk.share.jvmti.RedefineAgent;
import nsk.share.Wicket;

/**
 * This class extends <b>RedefineAgent</b>.
 * @see nsk.share.jvmti.RedefineAgent
 */
public class hs104t002 extends RedefineAgent {

    private static final int size = 30;

    private MyThread[] threadList = new MyThread[size];
    private Wicket wicket;

    /**
     * constructor for hs104t002.
     */
    public hs104t002(String[] arg) {
        super(arg);
        wicket = new Wicket();
        for(int i=0; i < size; i++) {
            threadList[i] = new MyThread(wicket);
        }
    }

    public static void main(String[] arg) {
        arg = nsk.share.jvmti.JVMTITest.commonInit(arg);

        hs104t002 hsCase = new hs104t002(arg);
        System.exit(hsCase.runAgent());
    }

    /**
     * A native method to redefine MyThread class.
     */
    public static native void redefineClasses();

    /**
     * Method is called from RedefineAgent work flow.
     * @return boolean true. Considered true if and only if testcase passes.
     */
    public boolean agentMethod() {
        boolean pass = false;
        try {
            if ( !startAllThreads() ) {
                return pass;
            }
            if ( !waitForAllThreads() ) {
                return pass;
            }
            if ( checkThreads() && redefineAttempted() &&
                 isRedefined()  && agentStatus() ) {
                pass = true;
            }
        } catch(Exception exp) {
            exp.printStackTrace();
            // for any possible exception testcase is failure
            pass=false;
        }
        if ( pass ) {
            log.println(" Testcase hs104t002 :: Passed.");
        } else {
            log.println(" Testcase hs104t002 :: Failed.");
        }
        return pass;
    }

    /**
     * Would start all threads properly.
     * @return boolean. Returns true only if all threads started properly
     *
     */
    public boolean startAllThreads() {
        boolean started= false;
        try {
            for(MyThread thread : threadList) {
                thread.start();
            }
            //notify all the threads to start their jobs.
            wicket.unlock();
            started=true;
            log.println(" startAllThreads :: All threads are running.");
        } catch (IllegalStateException ise) {
            log.complain(" startAllThreads :: Error occured while"
                        +" waiting for threads.");
            ise.printStackTrace();
        }
        return started;
    }

    /**
     * Checks for failure in redefineClass call.
     * @return boolean true iff, all the threads could redefine successfully.
     */
    public boolean checkThreads() {
        boolean passedAll = true;
        int failedThreadCount=0;
        for(MyThread thread : threadList) {
            if (thread.getThreadState() != 100) {
                log.complain(" checkThreads :: Thread name ="+thread.getName()
                     +", Expected state = 100, state = "
                     +thread.getThreadState());
                failedThreadCount++;
                passedAll=false;
            }
        }
        if ( !passedAll )  {
            log.complain(" checkThreads :: Number of threads failed = "
                 +failedThreadCount);
        }

        return passedAll;
    }

    /**
     * @return boolean returns true iff all threads terminate properly.
     */
    private boolean waitForAllThreads() {
        boolean allExited = false;
        try {
            for(MyThread thread : threadList) {
                thread.join();
            }
            allExited= true;
            log.println(" All threads terminated without "
                +"java.lang.InterruptedException.");
        } catch(java.lang.InterruptedException ie ) {
            log.complain(" waitForAllThreads ::"
                 +" Got java.lang.InterruptedException."
                 + "Test would fail.");
            ie.printStackTrace();
        }
        return allExited;
    }
}
