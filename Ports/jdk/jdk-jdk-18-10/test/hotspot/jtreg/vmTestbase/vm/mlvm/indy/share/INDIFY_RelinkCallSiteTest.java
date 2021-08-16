/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.indy.share;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import nsk.share.test.Stresser;
import vm.mlvm.share.Env;
import vm.mlvm.share.MlvmTest;

public abstract class INDIFY_RelinkCallSiteTest extends MlvmTest {

    private static final int TARGET_COUNT = 1000000;
    private static final int ARTIFICALLY_LOST_SYNC_PERIOD = 1000;
    private static final CyclicBarrier startBarrier = new CyclicBarrier(2);

    /**
     * This class is used for synchronization between threads during relinking.
     *
     * We don't use volatile, synchronized(), or java.util.concurrent stuff
     * to avoid artificial ordering across threads, except the one introduced by java.lang.invoke
     * package itself.
     *
     * The variable is placed inside an array to make sure that it would occupy
     * a separate CPU cache line than the CallSite.
     *
     * value < 0 and -value == previous-call-site-num: test in call site changing thread
     * value > 0 and value == current-call-site-num:   test in target (invokedynamic) calling thread
     * value == 0: test is done, all threads must exit
     */
    static class Sync {
        int[] sync = new int[64];

        int get() {
            return this.sync[32];
        }

        void put (int v) {
            this.sync[32] = v;
        }
    }

    static Sync syncTargetNum = new Sync();
    static CallSite cs;
    static MethodHandle[] targets = new MethodHandle[TARGET_COUNT];

    protected abstract CallSite createCallSite(MethodHandle mh);

    @Override
    public boolean run() throws Throwable {
        // Create targets
        MethodHandle targetMH = MethodHandles.lookup().findVirtual(INDIFY_RelinkCallSiteTest.class, "target", MethodType.methodType(int.class, int.class));
        for (int i = 0; i < TARGET_COUNT; i++)
            INDIFY_RelinkCallSiteTest.targets[i] = MethodHandles.insertArguments(targetMH, 0, this, i);

        // Set current target number
        INDIFY_RelinkCallSiteTest.syncTargetNum.put(1);

        // Create call site
        INDIFY_RelinkCallSiteTest.cs = createCallSite(INDIFY_RelinkCallSiteTest.targets[INDIFY_RelinkCallSiteTest.syncTargetNum.get()]);

        // Call BSM
        indyWrapper();

        // Start call site altering thread
        CallSiteAlteringThread csaThread = new CallSiteAlteringThread();
        csaThread.setDaemon(true);
        csaThread.start();

        try {
            // Start calling invokedynamic
            Stresser stresser = createStresser();
            stresser.start(1);
            try {
                int lastTargetNum = INDIFY_RelinkCallSiteTest.syncTargetNum.get();
                int curTargetNum;

                INDIFY_RelinkCallSiteTest.startBarrier.await();

                while (stresser.continueExecution()) {
                    stresser.iteration();

                    curTargetNum = indyWrapper();

                    // This test used to fail due to OS scheduler
                    // so it was refactored to just a stress test which doesn't fail if the frequency is wrong
                    boolean artificallyLostSync = lastTargetNum % ARTIFICALLY_LOST_SYNC_PERIOD == 0;
                    if (lastTargetNum == curTargetNum) {
                        Env.traceDebug("Target " + curTargetNum + " called: OK");
                        if (artificallyLostSync) {
                            Env.complain("Test bug: invoked target (" + curTargetNum + ") should not match the one in CallSite (" + lastTargetNum + ")");
                        }
                    } else {
                        if (artificallyLostSync) {
                            // That's OK
                        } else {
                            Env.complain("Invoked target number (" + curTargetNum + ") does not match the one in CallSite (" + lastTargetNum + ")");
                        }

                        // OK, let's continue anyway
                        lastTargetNum = INDIFY_RelinkCallSiteTest.syncTargetNum.get();
                    }

                    // Synchronize without any "special" synchronization means
                    int syncCycles = 0;
                    INDIFY_RelinkCallSiteTest.syncTargetNum.put(-lastTargetNum);
                    while (INDIFY_RelinkCallSiteTest.syncTargetNum.get() < 0) {
                        Thread.yield();
                        curTargetNum = indyWrapper();
                        syncCycles++;

                        if (syncCycles % 100000 == 0) {
                            Env.traceDebug("Waiting for change: target " + curTargetNum + " called " + syncCycles + " times");
                        }

                        if (curTargetNum > lastTargetNum) {
                            Env.traceDebug("Target changed but not yet signalled to me: curTargetNum (" + curTargetNum + ") > lastTargetNum (" + lastTargetNum + ")");
                        } else if (curTargetNum < lastTargetNum && !artificallyLostSync) {
                            Env.complain("Synchronization lost again: curTargetNum (" + curTargetNum + ") < lastTargetNum (" + lastTargetNum + ")");
                        }
                    }

                    lastTargetNum = INDIFY_RelinkCallSiteTest.syncTargetNum.get();
                    if (lastTargetNum == 0) {
                        stresser.forceFinish();
                    }
                }

            } finally {
                stresser.finish();
            }
        } finally {
            INDIFY_RelinkCallSiteTest.syncTargetNum.put(0);
        }

        // Return false
        return true;
        // (Never trust comments :)
    }

    static class CallSiteAlteringThread extends Thread {
        @Override
        public void run() {
            int curTargetNum = INDIFY_RelinkCallSiteTest.syncTargetNum.get();

            try {
                INDIFY_RelinkCallSiteTest.startBarrier.await();
            } catch ( BrokenBarrierException e ) {
                Env.complain(e, "Test bug: start barrier is not working");
            } catch ( InterruptedException leave ) {
                return;
            }

            while ( INDIFY_RelinkCallSiteTest.syncTargetNum.get() != 0 ) {
                while ( INDIFY_RelinkCallSiteTest.syncTargetNum.get() != -curTargetNum )
                    Thread.yield();

                curTargetNum++;
                if ( curTargetNum >= TARGET_COUNT ) {
                    INDIFY_RelinkCallSiteTest.syncTargetNum.put(0);
                    break;
                }

                Env.traceDebug("Setting new target: " + curTargetNum);
                // TODO: Check this and next line ordering in JMM
                if ( curTargetNum % ARTIFICALLY_LOST_SYNC_PERIOD != 0 )
                    INDIFY_RelinkCallSiteTest.cs.setTarget(INDIFY_RelinkCallSiteTest.targets[curTargetNum]);
                INDIFY_RelinkCallSiteTest.syncTargetNum.put(curTargetNum);
            }
        }
    }

    // BSM + target

    private static MethodType MT_bootstrap() {
        return MethodType.methodType(Object.class, Object.class, Object.class, Object.class);
    }

    private static MethodHandle MH_bootstrap() throws NoSuchMethodException, IllegalAccessException {
        return MethodHandles.lookup().findStatic(INDIFY_RelinkCallSiteTest.class, "bootstrap", MT_bootstrap());
    }

    private static MethodHandle INDY_call;
    private static MethodHandle INDY_call() throws Throwable {
        if (INDY_call != null) {
            return INDY_call;
        }
        CallSite cs = (CallSite) MH_bootstrap ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MethodType.methodType(int.class));
        return cs.dynamicInvoker();
    }

    public static int indyWrapper() throws Throwable {
        return (int) INDY_call().invokeExact();
    }

    private static Object bootstrap (Object l, Object n, Object t) throws Throwable {
        Env.traceVerbose("Bootstrap called");
        return INDIFY_RelinkCallSiteTest.cs;
    }

    private int target(int n) {
        return n;
    }

    // End BSM + target
}
