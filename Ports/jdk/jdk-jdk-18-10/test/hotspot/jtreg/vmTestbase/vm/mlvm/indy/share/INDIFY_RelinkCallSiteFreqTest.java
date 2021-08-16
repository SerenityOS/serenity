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
import vm.share.options.Option;

/**
 * The test creates a call site (the call site is supplied by subclass)
 * and relinks it from one thread while calling the current
 * target from the other one. Currently there are 6 targets.

 * The test verifies that target changes in the call site are eventually seen by target calling
 * thread by measuring a frequency of calls for each target and comparing it with theoretical frequency.
 *
 */
public abstract class INDIFY_RelinkCallSiteFreqTest extends MlvmTest {
    private static final int MEASUREMENT_THREADS = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
    private static final double MAX_FREQ_DIFFERENCE = 0.01;
    private static CallSite cs;

    private volatile boolean testDone = false;

    @Option(name = "iterations", default_value = "100000", description = "Iterations, each iteration does call site relinking")
    private int iterations = 100_000;

    /**
     * Provides a call site to test.
     * @param mh MethodHandle to link the new call site to
     * @return CallSite A new call site linked to MethodHandle supplied in the argument
     */
    protected abstract CallSite createCallSite(MethodHandle mh);

    @Override
    public boolean run() throws Throwable {
        // Create targets and call site
        Target[] targets = Target.createTargets(MethodHandles.lookup(), this);
        // TODO: find a way to make cs a non-static field
        cs = createCallSite(targets[0].mh);

        // Call BSM
        indyWrapper();

        // Start call site altering thread
        final FreqMeasurementThread[] csaThread = new FreqMeasurementThread[MEASUREMENT_THREADS];
        final CyclicBarrier startBarrier = new CyclicBarrier(MEASUREMENT_THREADS + 1);
        for (int i = 0; i < MEASUREMENT_THREADS; ++i) {
            csaThread[i] = new FreqMeasurementThread(startBarrier, this, targets.length);
            csaThread[i].start();
        }

        // Start calling invokedynamic
        Stresser stresser = createStresser();
        stresser.start(iterations);
        try {
            int curTarget = 0;
            startBarrier.await();

            while (stresser.continueExecution()) {
                stresser.iteration();

                Env.traceDebug("Setting new target: " + curTarget);
                targets[curTarget].run(cs);
                if (++curTarget >= targets.length) {
                    curTarget = 0;
                }
            }

        } finally {
            stresser.finish();
            testDone = true;
        }

        long totalCalls = 0L;
        long[] callHistogram = new long[targets.length];
        for (int i = 0; i < csaThread.length; ++i) {
            csaThread[i].join();
            totalCalls += csaThread[i].totalCalls;
            long[] threadCallHistogram = csaThread[i].callHistogram;
            assert threadCallHistogram.length == callHistogram.length;
            for (int t = 0; t < callHistogram.length; ++t) {
                callHistogram[t] += threadCallHistogram[t];
            }
        }

        Env.traceNormal("Targets called " + totalCalls + " times");

        for (int i = 0; i < callHistogram.length; ++i) {
            float measuredFreq = (float) callHistogram[i] / totalCalls;
            float theoreticalFreq = (float) targets[i].delay / Target.TOTAL_DELAY;

            boolean freqIsOK =  Math.abs(measuredFreq - theoreticalFreq) < MAX_FREQ_DIFFERENCE;
            String msg = String.format("Target %d: theoretical freq=%f; measured freq=%f; called %d times %s",
                    i, theoreticalFreq, measuredFreq, callHistogram[i], freqIsOK ? " [OK]" : " [BAD, but acceptable: difference is too big]");

            // This test used to fail due to OS scheduler
            // so it was refactored to just a stress test which doesn't fail if the frequency is wrong
            if (!freqIsOK) {
                Env.complain(msg);
            } else {
                Env.traceNormal(msg);
            }
        }

        return true;
    }

    private static class Target {
        // TODO: nanosleep?
        private static final long[] DELAYS = new long[] { 1L, 3L, 5L, 7L, 9L, 11L };
        public static final long TOTAL_DELAY;
        static {
            long total = 0L;
            for (long i : DELAYS) {
                total += i;
            }
            TOTAL_DELAY = total;
        }
        public static Target[] createTargets(MethodHandles.Lookup lookup, INDIFY_RelinkCallSiteFreqTest test) {
            Target[] result = new Target[DELAYS.length];
            MethodHandle targetMH;
            try {
                targetMH = lookup.findVirtual(INDIFY_RelinkCallSiteFreqTest.class, "target", MethodType.methodType(int.class, int.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new Error(e);
            }
            for (int i = 0; i < result.length; ++i) {
                result[i] = new Target(DELAYS[i], MethodHandles.insertArguments(targetMH, 0, test, i));
            }
            return result;
        }

        public final long delay;
        public final MethodHandle mh;
        public Target(long delay, MethodHandle mh) {
            this.delay = delay;
            this.mh = mh;
        }
        public void run(CallSite cs) throws InterruptedException {
            cs.setTarget(mh);
            Thread.sleep(delay);
        }
    }

    private static class FreqMeasurementThread extends Thread {
        final long[] callHistogram;
        long totalCalls = 0L;

        private final CyclicBarrier startBarrier;
        private final INDIFY_RelinkCallSiteFreqTest test;

        public FreqMeasurementThread(CyclicBarrier startBarrier, INDIFY_RelinkCallSiteFreqTest test, int targetCount) {
            setName(getClass().getSimpleName());
            this.startBarrier = startBarrier;
            this.test = test;
            callHistogram = new long[targetCount];
        }

        @Override
        public void run() {
            try {
                startBarrier.await();

                while (!test.testDone) {
                    int n = indyWrapper();
                    ++callHistogram[n];
                    ++totalCalls;
                    Env.traceDebug("Called target: " + n);

                    Thread.yield();
                }
            } catch (BrokenBarrierException e) {
                test.markTestFailed("TEST BUG: start barrier is not working", e);
            } catch (Throwable t) {
                test.markTestFailed("Exception in thread " + getName(), t);
            }
        }
    }

    // BSM + target

    private static MethodType MT_bootstrap() {
        return MethodType.methodType(Object.class, MethodHandles.Lookup.class, String.class, MethodType.class);
    }

    private static MethodHandle MH_bootstrap() throws NoSuchMethodException, IllegalAccessException {
        return MethodHandles.lookup().findStatic(INDIFY_RelinkCallSiteFreqTest.class, "bootstrap", MT_bootstrap());
    }

    private static MethodHandle INDY_call;
    private static MethodHandle INDY_call() throws Throwable {
        if (INDY_call != null) {
          return INDY_call;
        }
        CallSite cs = (CallSite) MH_bootstrap().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MethodType.methodType(int.class));
        return cs.dynamicInvoker();
    }

    public static int indyWrapper() throws Throwable {
        return (int) INDY_call().invokeExact();
    }

    private static Object bootstrap(MethodHandles.Lookup l, String n, MethodType t) throws Throwable {
        Env.traceVerbose("Bootstrap called");
        return INDIFY_RelinkCallSiteFreqTest.cs;
    }

    private int target(int n) {
        return n;
    }

    // End BSM + target
}
