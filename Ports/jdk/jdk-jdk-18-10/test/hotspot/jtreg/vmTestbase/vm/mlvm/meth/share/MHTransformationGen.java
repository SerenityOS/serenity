/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.meth.share;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.management.MemoryUsage;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import jdk.test.lib.Platform;

import nsk.share.test.LazyIntArrayToString;
import nsk.share.test.TestUtils;
import vm.mlvm.meth.share.transform.v2.MHArrayEnvelopeTFPair;
import vm.mlvm.meth.share.transform.v2.MHCall;
import vm.mlvm.meth.share.transform.v2.MHCollectSpreadTF;
import vm.mlvm.meth.share.transform.v2.MHConstantTF;
import vm.mlvm.meth.share.transform.v2.MHDropTF;
import vm.mlvm.meth.share.transform.v2.MHDropTF2;
import vm.mlvm.meth.share.transform.v2.MHFilterRetValTF;
import vm.mlvm.meth.share.transform.v2.MHFilterTF;
import vm.mlvm.meth.share.transform.v2.MHFoldTF;
import vm.mlvm.meth.share.transform.v2.MHIdentityTF;
import vm.mlvm.meth.share.transform.v2.MHInsertTF;
import vm.mlvm.meth.share.transform.v2.MHMacroTF;
import vm.mlvm.meth.share.transform.v2.MHOutboundCallTF;
import vm.mlvm.meth.share.transform.v2.MHOutboundVirtualCallTF;
import vm.mlvm.meth.share.transform.v2.MHPermuteTF;
import vm.mlvm.meth.share.transform.v2.MHTF;
import vm.mlvm.meth.share.transform.v2.MHTFPair;
import vm.mlvm.meth.share.transform.v2.MHThrowCatchTFPair;
import vm.mlvm.meth.share.transform.v2.MHVarargsCollectSpreadTF;
import vm.mlvm.share.Env;

public class MHTransformationGen {

    public static final int MAX_CYCLES = 1000;

    private static final int MAX_ARGUMENTS = 10;

    private static final boolean FILTER_OUT_KNOWN_BUGS = false;

    private static final boolean USE_THROW_CATCH = false; // Test bugs

    /**
     * The class is used for periodical checks if a code-cache consuming operation
     * could be executed (i.e. if code cache has enought free space for a typical operation).
     */
    private static class CodeCacheMonitor {

        private static final Optional<MemoryPoolMXBean> NON_SEGMENTED_CODE_CACHE_POOL;
        private static final Optional<MemoryPoolMXBean> NON_NMETHODS_POOL;
        private static final Optional<MemoryPoolMXBean> PROFILED_NMETHODS_POOL;
        private static final Optional<MemoryPoolMXBean> NON_PROFILED_NMETHODS_POOL;

        // Trial runs show up that maximal increase in code cache consumption between checks (for one
        // cycle/tree build in MHTransformationGen::createSequence), falls within the following intervals:
        //
        // | Threads number | Without Xcomp | With Xcomp |
        // |----------------|---------------|------------|
        // |       1        |   100-200 K   |  400-500 K |
        // |      10        |    1 - 2 M    |    5-6 M   |
        //
        // Those numbers are approximate (since trees are generated randomly and the total consumption
        // between checks depends on how threads are aligned - for example, if all threads finish up their
        // cycles approximately at one time, the consumption increase will be the highest, like with a
        // resonance's amplitude)
        // The 10 threads is chosen as it is a typical number for multi-threaded tests.
        //
        // Based on these numbers, values of 10 M for Xcomp and 5 M for non-Xcomp, were suggested.
        private static final int NON_SEGMENTED_CACHE_ALLOWANCE = Platform.isComp() ? 10_000_000 : 5_000_000;
        private static final int SEGMENTED_CACHE_ALLOWANCE = Platform.isComp() ? 10_000_000 : 5_000_000;

        static {
            var pools = ManagementFactory.getMemoryPoolMXBeans();
            NON_SEGMENTED_CODE_CACHE_POOL = pools.stream()
                .filter(pool -> pool.getName().equals("CodeCache")).findFirst();
            NON_NMETHODS_POOL = pools.stream()
                .filter(pool -> pool.getName().equals("CodeHeap 'non-nmethods'")).findFirst();
            PROFILED_NMETHODS_POOL = pools.stream()
                .filter(pool -> pool.getName().equals("CodeHeap 'profiled nmethods'")).findFirst();
            NON_PROFILED_NMETHODS_POOL = pools.stream()
                .filter(pool -> pool.getName().equals("CodeHeap 'non-profiled nmethods'")).findFirst();
        }

        public static final boolean isCodeCacheEffectivelyFull() {
            var result = new Object() { boolean value = false; };

            BiConsumer<MemoryPoolMXBean, Integer> check = (pool, limit) -> {
                var usage = pool.getUsage();
                result.value |= usage.getMax() - usage.getUsed() < limit;
            };

            NON_SEGMENTED_CODE_CACHE_POOL.ifPresent(pool -> check.accept(pool, NON_SEGMENTED_CACHE_ALLOWANCE));
            NON_NMETHODS_POOL.ifPresent(pool -> check.accept(pool, SEGMENTED_CACHE_ALLOWANCE));
            PROFILED_NMETHODS_POOL.ifPresent(pool -> check.accept(pool, SEGMENTED_CACHE_ALLOWANCE));
            NON_PROFILED_NMETHODS_POOL.ifPresent(pool -> check.accept(pool, SEGMENTED_CACHE_ALLOWANCE));

            return result.value;
        }
    };

    public static class ThrowCatchTestException extends Throwable {
        private static final long serialVersionUID = -6749961303738648241L;
    }

    @SuppressWarnings("unused")
    public static MHMacroTF createSequence(Argument finalRetVal, Object boundObj, MethodHandle finalMH, Argument[] finalArgs) throws Throwable {
        Env.traceDebug("Generating sequence.");

        MHMacroTF graph = new MHMacroTF("sequence");

        MHTF outTF;
        if ( boundObj != null )
            outTF = new MHOutboundVirtualCallTF(finalRetVal, boundObj, finalMH, finalArgs);
        else
            outTF = new MHOutboundCallTF(finalRetVal, finalMH, finalArgs);

        Env.traceDebug("Outbound call=%s", outTF);
        graph.addTransformation(outTF);

        if ( MAX_CYCLES == 0 )
            return graph;

        List<MHTFPair> pendingPWTFs = new LinkedList<MHTFPair>();

        final int cyclesToBuild = nextInt(MAX_CYCLES);
        for ( int i = 0; i < cyclesToBuild; i++) {
            if (CodeCacheMonitor.isCodeCacheEffectivelyFull()) {
                Env.traceNormal("Not enought code cache to build up MH sequences anymore. " +
                        " Has only been able to achieve " + i + " out of " + cyclesToBuild);
                break;
            }

            MHCall lastCall = graph.computeInboundCall();
            Argument[] lastArgs = lastCall.getArgs();
            MethodType type = lastCall.getTargetMH().type();
            Argument lastRetVal = lastCall.getRetVal();

            int lastArgsSlots = computeVmSlotCount(lastArgs);
            if ( boundObj != null )
                lastArgsSlots += TestTypes.getSlotsCount(boundObj.getClass());

            Env.traceDebug("Current last call: %s", lastCall);

            MHTF tf = null;
            MHTFPair tfPair = null;

            int nextCase = nextInt(11);

            Env.traceDebug("Adding case #" + nextCase);
            try {
                switch ( nextCase ) {
                    case 0: { // add next pending TF
                        if ( pendingPWTFs.size() > 0 ) {
                            MHTFPair pwtf = pendingPWTFs.remove(0);
                            tf = pwtf.getInboundTF(lastCall);
                            Env.traceDebug("(adding pending inbound transformation)");
                        }
                    }
                    break;

                    case 1: { // Drop arguments
                        int pos = nextInt(lastArgs.length);
                        int nArgs = nextInt(MAX_ARGUMENTS);
                        if ( nArgs == 0 )
                            break;

                        Argument[] values = new Argument[nArgs];
                        for ( int j = 0; j < nArgs; j++ )
                            values[j] = RandomArgumentGen.next();

                        int valuesSlots = computeVmSlotCount(values);

                        int newValuesCount = nArgs;
                        while ( valuesSlots + lastArgsSlots > MAX_ARGUMENTS ) {
                            valuesSlots -= TestTypes.getSlotsCount(values[newValuesCount - 1].getType());
                            --newValuesCount;
                        }

                        if ( newValuesCount != nArgs )
                            values = Arrays.copyOf(values, newValuesCount);

                        if ( Env.getRNG().nextBoolean() )
                            tf = new MHDropTF(lastCall, pos, values);
                        else
                            tf = new MHDropTF2(lastCall, pos, values);
                    }
                    break;

                    case 2: { // Insert arguments
                        if ( lastArgs.length == 0 )
                            break;

                        int pos = nextInt(lastArgs.length);

                        int p;
                        for ( p = pos; p < pos + lastArgs.length; p++ ) {
                            if ( ! lastArgs[p % lastArgs.length].isPreserved() )
                                break;
                        }

                        pos = p % lastArgs.length;
                        if ( lastArgs[pos].isPreserved() )
                            break;

                        int nArgs = 1 + nextInt(lastArgs.length - pos - 1);

                        for ( p = pos + 1; p < pos + nArgs; p++ ) {
                            if ( lastArgs[p].isPreserved() )
                                break;
                        }

                        nArgs = p - pos;

                        Argument[] values = Arrays.copyOfRange(lastArgs, pos, pos + nArgs);

                        tf = new MHInsertTF(lastCall, pos, values, false);
                    }
                    break;

                    case 3: { // Throw + catch
                        if ( ! USE_THROW_CATCH )
                            break;

                        if ( lastArgsSlots + 1 >= MAX_ARGUMENTS )
                            break;

                        Argument testArg = RandomArgumentGen.next();
                        Env.traceDebug("testArgument=%s", testArg);

                        Object testValue2;
                        boolean eqTest = (Boolean) RandomValueGen.next(Boolean.class);
                        if ( eqTest ) {
                            testValue2 = testArg.getValue();
                        } else {
                            testValue2 = RandomValueGen.nextDistinct(testArg.getType(), testArg.getValue());
                        }

                        tfPair = new MHThrowCatchTFPair(lastCall, testArg, testValue2, eqTest, new ThrowCatchTestException());
                    }
                    break;

                    case 4: { // Permute arguments

                        List<Integer> targetArgNumbers = new LinkedList<Integer>();
                        for ( int n = 0;  n < lastArgs.length; n++ )
                            targetArgNumbers.add(n);
                        Collections.shuffle(targetArgNumbers, Env.getRNG());

                        Argument[] sourceArgs = new Argument[lastArgs.length];
                        for ( int n = 0; n < lastArgs.length; n++ ) {
                            sourceArgs[targetArgNumbers.get(n)] = lastArgs[n];
                        }

                        MethodType newMT = MethodType.methodType(type.returnType(), Arguments.typesArray(sourceArgs));

                        // Java has no other means for converting Integer[] to int[]
                        int[] permuteArray = new int[targetArgNumbers.size()];
                        for ( int n = 0; n < permuteArray.length; n++ )
                            permuteArray[n] = targetArgNumbers.get(n);

                        Env.traceDebug("Permute: permuteArray=%s; newMT=%s; oldMT=%s", new LazyIntArrayToString(permuteArray), newMT, lastCall.getTargetMH());

                        tf = new MHPermuteTF(lastCall, newMT, permuteArray);
                    }
                    break;

                    case 5: { // Fold arguments
                        if ( lastArgs.length == 0 )
                            break;

                        Argument arg = lastArgs[0];
                        if ( arg.isPreserved() )
                            break;

                        Argument[] restOfArgs = TestUtils.cdr(lastArgs);

                        MHMacroTF mTF = new MHMacroTF("fold arguments");
                        mTF.addOutboundCall(lastCall);

                        MHCall combinerCall = mTF.addTransformation(new MHDropTF(
                                mTF.addTransformation(new MHConstantTF(arg)),
                                0, restOfArgs
                        ));

                        Env.traceDebug("combinerCall=%s", combinerCall);
                        Env.traceDebug("targetCall=%s", lastCall);

                        mTF.addTransformation(new MHFoldTF(
                                lastCall,
                                combinerCall
                        ));

                        tf = mTF;
                    }
                    break;

                    case 6: { // Filter arguments
                        if ( lastArgs.length == 0 )
                            break;

                        int pos = nextInt(lastArgs.length);
                        int nArgs = 1 + nextInt(lastArgs.length - pos - 1);

                        MHMacroTF mTF = new MHMacroTF("identity filter arguments");
                        mTF.addOutboundCall(lastCall);

                        MHCall[] filters = new MHCall[nArgs];
                        for ( int n = 0; n < filters.length; n++ ) {
                            if ( nextInt(5) != 0 ) {
                                filters[n] = mTF.addTransformation(new MHIdentityTF(lastArgs[n + pos]));
                            }
                        }

                        mTF.addTransformation(new MHFilterTF(lastCall, pos, filters));

                        tf = mTF;
                    }
                    break;

                    case 7: { // filter
                        if ( lastArgs.length <= 1 )
                            break;

                        int pos = nextInt(lastArgs.length);
                        int nArgs;
                        if ( pos == lastArgs.length - 1 )
                            nArgs = 1;
                        else
                            nArgs = 1 + nextInt(lastArgs.length - pos - 1);

                        MHMacroTF mTF = new MHMacroTF("replace filter arguments");
                        mTF.addOutboundCall(lastCall);

                        MHCall[] filters = new MHCall[nArgs];

                        loop:
                        for ( int n = 0; n < nArgs; n++ ) {
                            Argument arg = lastArgs[pos + n];
                            if ( nextInt(5) == 0 || arg.isPreserved() )
                                continue;

                            Class<?> argType = arg.getType();
                            Object value = RandomValueGen.next(argType);
                            Argument newArg = new Argument(argType, value);

                            filters[n] = mTF.addTransformation(new MHDropTF(
                                             mTF.addTransformation(new MHConstantTF(arg)),
                                             0, new Argument[] { newArg }
                                         ));
                        }

                        mTF.addTransformation(new MHFilterTF(lastCall, pos, filters));

                        tf = mTF;
                    }
                    break;

                    case 8: { // Filter return value
                        if ( lastRetVal.isPreserved() )
                            break;

                        Class<?> lastRetType = lastRetVal.getType();
                        if ( lastRetType.equals(void.class) )
                            break;

                        Argument newRetVal = new Argument(lastRetType, RandomValueGen.next(lastRetType));

                        MHMacroTF mTF = new MHMacroTF("filter retval");
                        mTF.addOutboundCall(lastCall);
                        mTF.addTransformation(new MHFilterRetValTF(lastCall,
                                mTF.addTransformation(new MHDropTF(
                                            mTF.addTransformation(new MHConstantTF(newRetVal)),
                                            0,
                                            new Argument[] { lastRetVal }
                                ))
                        ));

                        tf = mTF;
                    }
                    break;

                    case 9: { // Envelope argument into array
                        if ( lastArgs.length >= 0 )
                            break;

                        int arraySize = 1 + nextInt(0xffff);
                        int arrayIdx = nextInt(arraySize);
                        int argNum = nextInt(lastArgs.length);
                        tfPair = new MHArrayEnvelopeTFPair(lastCall, argNum, arrayIdx, arraySize);
                    }
                    break;

                    case 10: { // Collect + spread
                        if ( nextInt(1) == 0 )
                            tf = new MHCollectSpreadTF(lastCall);
                        else
                            tf = new MHVarargsCollectSpreadTF(lastCall);
                    }
                    break;
                }

                if ( FILTER_OUT_KNOWN_BUGS ) {
                    if ( tfPair != null ) {
                        Env.traceDebug("Checking transformation pair %s", tfPair);

                        tfPair.getInboundTF(tfPair.getOutboundTF().computeInboundCall()).computeInboundCall();
                    } else if ( tf != null ) {
                        Env.traceDebug("Checking transformation %s", tf);
                        tf.computeInboundCall();
                    }
                }

            } catch ( Throwable e ) {
                if ( ! FILTER_OUT_KNOWN_BUGS )
                    throw e;

                String msg = e.getMessage();
                for ( Throwable t = e.getCause(); t != null; t = t.getCause() ) {
                    msg += " ";
                    msg += t.getMessage();
                }

                if ( msg != null
                  && ! msg.contains("NONE SO FAR 2011-07-10")
                   ) {
                    throw e;
                }

                Env.traceDebug("Failed to add transformation %s; Error: %s", tf, msg);
                tfPair = null;
                tf = null;
            }

            if ( tfPair != null ) {
                MHTF oTF = tfPair.getOutboundTF();
                Env.traceDebug("Adding outbound transformation %s", oTF);
                graph.addTransformation(oTF);
                pendingPWTFs.add(tfPair);
            } else if ( tf != null ) {
                Env.traceDebug("Adding transformation %s", tf);
                graph.addTransformation(tf);
            } else {
                Env.traceDebug("Skipping transformation");
            }
        }

        while ( pendingPWTFs.size() > 0 ) {
            MHTFPair pwtf = pendingPWTFs.remove(0);
            MHTF tf = pwtf.getInboundTF(graph.computeInboundCall());

            Env.traceDebug("Adding pending inbound transformation: %s", tf);
            graph.addTransformation(tf);
        }

        Env.traceVerbose("MHTransformationGen produced graph: %s", graph);

        return graph;
    }

    private static int computeVmSlotCount(Argument[] values) {
        int count = 0;
        for ( Argument v : values )
            count += TestTypes.getSlotsCount(v.getType());
        return count;
    }

    public static Object callSequence(MHMacroTF seq, boolean checkRetVal) throws Throwable {
        Env.traceVerbose("Calling sequence...");
        MHCall call = seq.computeInboundCall();
        Object result;
        try {
            if ( checkRetVal ) {
                result = call.callAndCheckRetVal();
            } else {
                result = call.call();
            }
        } catch ( Throwable t ) {
            Env.traceNormal(t, "Exception during calling a sequence");
            throw (Exception) (new Exception("Exception in sequence " + seq.toString()).initCause(t));
        }
        Env.traceVerbose("Sequence result = %s", result);
        return result;
    }

    public static Object createAndCallSequence(Argument finalRetVal, Object boundObj, MethodHandle finalMH, Argument[] finalArgs, boolean checkRetVal) throws Throwable {
        return callSequence(createSequence(finalRetVal, boundObj, finalMH, finalArgs), checkRetVal);
    }

    public static void transformToMatchArgsNum(MHMacroTF graph, int argNumMin, int argNumMax) throws Throwable {
        MHCall lastCall = graph.computeInboundCall();
        Argument[] lastArgs = lastCall.getArgs();

        if ( lastArgs.length > argNumMax ) {

            // TODO: preserved args
            MHTF tf = new MHInsertTF(lastCall, argNumMax, Arrays.copyOfRange(lastArgs, argNumMax, lastArgs.length), false);
            Env.traceVerbose("Adding transformation to match %d limit: %s", argNumMax, tf);
            graph.addTransformation(tf);

        } else if ( lastArgs.length < argNumMin ) {

            int argsToInsert = argNumMin - lastArgs.length;

            Argument[] values = new Argument[argsToInsert];
            for ( int j = 0; j < argsToInsert; j++ )
                values[j] = RandomArgumentGen.next();

            int pos = 0;

            MHTF tf = new MHDropTF(lastCall, pos, values);
            Env.traceVerbose("Adding transformation to match %d arguments: %s", argNumMin, tf);
            graph.addTransformation(tf);

        }
    }

    private static int nextInt(int i) {
        if ( i == 0 )
            return 0;
        else
            return Env.getRNG().nextInt(i);
    }
}
