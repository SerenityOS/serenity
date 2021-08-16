/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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

// generated from vm/mlvm/mixed/stress/java/findDeadlock/INDIFY_Test.jmpp

package vm.mlvm.mixed.stress.java.findDeadlock;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantLock;

import nsk.share.test.Stresser;
import vm.mlvm.share.Env;
import vm.mlvm.share.MlvmTest;

public class INDIFY_Test extends MlvmTest {

    public static final int THREAD_NUM = 999;
    public static final int ITERATIONS = 1000;

    static ThreadMXBean _threadMXBean = ManagementFactory.getThreadMXBean();

    static Thread[] _threads = new Thread[THREAD_NUM];
    static ReentrantLock[] _locks = new ReentrantLock[THREAD_NUM];
    static MethodHandle[] _mh = new MethodHandle[THREAD_NUM];
    static MutableCallSite[] _cs = new MutableCallSite[THREAD_NUM];

    static CyclicBarrier _threadRaceStartBarrier;
    static CountDownLatch _threadsRunningLatch;
    static volatile boolean _testFailed;
    static volatile boolean _testDone;
    static volatile int _iteration;

    private static int nextLock(int n) { return (n + 1) % THREAD_NUM; }

    private static boolean lock(String place, int n, boolean lockInterruptible) throws Throwable {
        boolean locked = false;
        place =  Thread.currentThread().getName() + ": " + place;
        if ( ! lockInterruptible ) {
            Env.traceVerbose("Iteration " + _iteration + " " + place + ": Locking " + n);
            _locks[n].lock();
            locked = true;
        } else {
            try {
                Env.traceVerbose("Iteration " + _iteration + " " + place + ": Locking interruptibly " + n);
                _locks[n].lockInterruptibly();
                locked = true;

                if ( ! _testDone )
                    throw new Exception(place + ": LOCKED " + n);
                else
                    Env.traceVerbose("Iteration " + _iteration + " " + place + ": LOCKED " + n);

            } catch ( InterruptedException swallow ) {
                Env.traceVerbose("Iteration " + _iteration + " " + place + ": interrupted while locking " + n);
            }
        }

        return locked;
    }

    private static boolean unlock(String place, int n) throws Throwable {
        place =  Thread.currentThread().getName() + ": " + place;
        Env.traceVerbose("Iteration " + _iteration + " " + place + ": Unlocking " + n);
        _locks[n].unlock();
        Env.traceVerbose("Iteration " + _iteration + " " + place + ": UNLOCKED " + n);
        return false;
    }

    static Object bsmt(int lockNum, Object l, Object n, Object m) throws Throwable {
        DeadlockedThread thread = (DeadlockedThread) Thread.currentThread();

        if ( l instanceof MethodHandles.Lookup ) {
            // Method is used as BSM
            Env.traceVerbose("Iteration " + _iteration + " " + thread.getName() + ": Entered BSM. Lock=" + lockNum);

            if ( _iteration > 0 )
                throw new Exception("BSM called twice!");

            switch ( lockNum % 3 ) {
            case 0:
                thread._lockedCurrent = lock("BSM", lockNum, false);
                _threadRaceStartBarrier.await();
                _threadsRunningLatch.countDown();
                thread._lockedNext = lock("BSM", nextLock(lockNum), true);
                break;

            case 1:
                thread._lockedCurrent = lock("BSM", lockNum, false);
                break;

            case 2:
                // Do everything in target method
                break;
            }

            return (_cs[lockNum] = new MutableCallSite(_mh[lockNum]));

        } else {
            // Method is used as target
            Env.traceVerbose("Iteration " + _iteration + " " + thread.getName() + ": Entered target method. Lock=" + lockNum);

            try {
                if ( _iteration > 0 ) {

                    switch ( lockNum % 3 ) {
                    case 0:
                        thread._lockedCurrent = lock("Target", lockNum, false);
                        _threadRaceStartBarrier.await();
                        _threadsRunningLatch.countDown();
                        thread._lockedNext = lock("Target", nextLock(lockNum), true);
                        break;

                    case 1:
                        thread._lockedCurrent = lock("Target", lockNum, false);
                        _threadRaceStartBarrier.await();
                        _threadsRunningLatch.countDown();
                        Env.traceVerbose("Iteration " + _iteration + " " + thread.getName() + ": Entering synchronize ( " + lockNum + " )");
                        synchronized ( _locks[nextLock(lockNum)] ) {
                        }
                        Env.traceVerbose("Iteration " + _iteration + " " + thread.getName() + ": Exited synchronize ( " + lockNum + " )");
                        break;

                    case 2:
                        Env.traceVerbose("Iteration " + _iteration + " " + thread.getName() + ": Entering synchronize ( " + lockNum + " )");
                        synchronized ( _locks[lockNum] ) {
                            _threadRaceStartBarrier.await();
                            _threadsRunningLatch.countDown();
                            thread._lockedNext = lock("Target", nextLock(lockNum), true);
                            thread._lockedNext = unlock("Target", nextLock(lockNum));
                        }
                        Env.traceVerbose("Iteration " + _iteration + " " + thread.getName() + ": Exited synchronize ( " + lockNum + " )");
                        break;
                    }

                } else {
                    switch ( lockNum % 3 ) {
                    case 0:
                        // Everything is done in BSM
                        break;

                    case 1:
                        _threadRaceStartBarrier.await();
                        _threadsRunningLatch.countDown();
                        thread._lockedNext = lock("Target", nextLock(lockNum), true);
                        break;

                    case 2:
                        thread._lockedCurrent = lock("Target", lockNum, false);
                        _threadRaceStartBarrier.await();
                        _threadsRunningLatch.countDown();
                        thread._lockedNext = lock("Target", nextLock(lockNum), true);
                        break;
                    }

                }

                return null;
            } finally {
                if ( thread._lockedNext )
                    thread._lockedNext = unlock("Target", nextLock(lockNum));
                if ( thread._lockedCurrent )
                    thread._lockedCurrent = unlock("Target", lockNum);
            }
        }
    }

    // BSM + Indy pairs
    // 0
    private static MethodType MT_bootstrap0 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap0 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap0", MT_bootstrap0 ());
    }

    private static MethodHandle INDY_call0;
    private static MethodHandle INDY_call0 () throws Throwable {
        if (INDY_call0 != null) return INDY_call0;
        CallSite cs = (CallSite) MH_bootstrap0 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap0 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper0 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call0 ().invokeExact(o1, o2, o3); }

    static Object bootstrap0 (Object l, Object n, Object t) throws Throwable { return _mh[ 0 ].invokeExact(l, n, t); }

    // 1
    private static MethodType MT_bootstrap1 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap1 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap1", MT_bootstrap1 ());
    }

    private static MethodHandle INDY_call1;
    private static MethodHandle INDY_call1 () throws Throwable {
        if (INDY_call1 != null) return INDY_call1;
        CallSite cs = (CallSite) MH_bootstrap1 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap1 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper1 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call1 ().invokeExact(o1, o2, o3); }

    static Object bootstrap1 (Object l, Object n, Object t) throws Throwable { return _mh[ 1 ].invokeExact(l, n, t); }

    // 2
    private static MethodType MT_bootstrap2 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap2 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap2", MT_bootstrap2 ());
    }

    private static MethodHandle INDY_call2;
    private static MethodHandle INDY_call2 () throws Throwable {
        if (INDY_call2 != null) return INDY_call2;
        CallSite cs = (CallSite) MH_bootstrap2 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap2 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper2 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call2 ().invokeExact(o1, o2, o3); }

    static Object bootstrap2 (Object l, Object n, Object t) throws Throwable { return _mh[ 2 ].invokeExact(l, n, t); }

    // 3
    private static MethodType MT_bootstrap3 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap3 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap3", MT_bootstrap3 ());
    }

    private static MethodHandle INDY_call3;
    private static MethodHandle INDY_call3 () throws Throwable {
        if (INDY_call3 != null) return INDY_call3;
        CallSite cs = (CallSite) MH_bootstrap3 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap3 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper3 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call3 ().invokeExact(o1, o2, o3); }

    static Object bootstrap3 (Object l, Object n, Object t) throws Throwable { return _mh[ 3 ].invokeExact(l, n, t); }

    // 4
    private static MethodType MT_bootstrap4 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap4 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap4", MT_bootstrap4 ());
    }

    private static MethodHandle INDY_call4;
    private static MethodHandle INDY_call4 () throws Throwable {
        if (INDY_call4 != null) return INDY_call4;
        CallSite cs = (CallSite) MH_bootstrap4 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap4 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper4 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call4 ().invokeExact(o1, o2, o3); }

    static Object bootstrap4 (Object l, Object n, Object t) throws Throwable { return _mh[ 4 ].invokeExact(l, n, t); }

    // 5
    private static MethodType MT_bootstrap5 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap5 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap5", MT_bootstrap5 ());
    }

    private static MethodHandle INDY_call5;
    private static MethodHandle INDY_call5 () throws Throwable {
        if (INDY_call5 != null) return INDY_call5;
        CallSite cs = (CallSite) MH_bootstrap5 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap5 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper5 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call5 ().invokeExact(o1, o2, o3); }

    static Object bootstrap5 (Object l, Object n, Object t) throws Throwable { return _mh[ 5 ].invokeExact(l, n, t); }

    // 6
    private static MethodType MT_bootstrap6 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap6 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap6", MT_bootstrap6 ());
    }

    private static MethodHandle INDY_call6;
    private static MethodHandle INDY_call6 () throws Throwable {
        if (INDY_call6 != null) return INDY_call6;
        CallSite cs = (CallSite) MH_bootstrap6 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap6 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper6 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call6 ().invokeExact(o1, o2, o3); }

    static Object bootstrap6 (Object l, Object n, Object t) throws Throwable { return _mh[ 6 ].invokeExact(l, n, t); }

    // 7
    private static MethodType MT_bootstrap7 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap7 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap7", MT_bootstrap7 ());
    }

    private static MethodHandle INDY_call7;
    private static MethodHandle INDY_call7 () throws Throwable {
        if (INDY_call7 != null) return INDY_call7;
        CallSite cs = (CallSite) MH_bootstrap7 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap7 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper7 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call7 ().invokeExact(o1, o2, o3); }

    static Object bootstrap7 (Object l, Object n, Object t) throws Throwable { return _mh[ 7 ].invokeExact(l, n, t); }

    // 8
    private static MethodType MT_bootstrap8 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap8 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap8", MT_bootstrap8 ());
    }

    private static MethodHandle INDY_call8;
    private static MethodHandle INDY_call8 () throws Throwable {
        if (INDY_call8 != null) return INDY_call8;
        CallSite cs = (CallSite) MH_bootstrap8 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap8 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper8 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call8 ().invokeExact(o1, o2, o3); }

    static Object bootstrap8 (Object l, Object n, Object t) throws Throwable { return _mh[ 8 ].invokeExact(l, n, t); }

    // 9
    private static MethodType MT_bootstrap9 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap9 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap9", MT_bootstrap9 ());
    }

    private static MethodHandle INDY_call9;
    private static MethodHandle INDY_call9 () throws Throwable {
        if (INDY_call9 != null) return INDY_call9;
        CallSite cs = (CallSite) MH_bootstrap9 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap9 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper9 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call9 ().invokeExact(o1, o2, o3); }

    static Object bootstrap9 (Object l, Object n, Object t) throws Throwable { return _mh[ 9 ].invokeExact(l, n, t); }

    // 10
    private static MethodType MT_bootstrap10 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap10 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap10", MT_bootstrap10 ());
    }

    private static MethodHandle INDY_call10;
    private static MethodHandle INDY_call10 () throws Throwable {
        if (INDY_call10 != null) return INDY_call10;
        CallSite cs = (CallSite) MH_bootstrap10 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap10 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper10 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call10 ().invokeExact(o1, o2, o3); }

    static Object bootstrap10 (Object l, Object n, Object t) throws Throwable { return _mh[ 10 ].invokeExact(l, n, t); }

    // 11
    private static MethodType MT_bootstrap11 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap11 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap11", MT_bootstrap11 ());
    }

    private static MethodHandle INDY_call11;
    private static MethodHandle INDY_call11 () throws Throwable {
        if (INDY_call11 != null) return INDY_call11;
        CallSite cs = (CallSite) MH_bootstrap11 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap11 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper11 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call11 ().invokeExact(o1, o2, o3); }

    static Object bootstrap11 (Object l, Object n, Object t) throws Throwable { return _mh[ 11 ].invokeExact(l, n, t); }

    // 12
    private static MethodType MT_bootstrap12 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap12 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap12", MT_bootstrap12 ());
    }

    private static MethodHandle INDY_call12;
    private static MethodHandle INDY_call12 () throws Throwable {
        if (INDY_call12 != null) return INDY_call12;
        CallSite cs = (CallSite) MH_bootstrap12 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap12 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper12 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call12 ().invokeExact(o1, o2, o3); }

    static Object bootstrap12 (Object l, Object n, Object t) throws Throwable { return _mh[ 12 ].invokeExact(l, n, t); }

    // 13
    private static MethodType MT_bootstrap13 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap13 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap13", MT_bootstrap13 ());
    }

    private static MethodHandle INDY_call13;
    private static MethodHandle INDY_call13 () throws Throwable {
        if (INDY_call13 != null) return INDY_call13;
        CallSite cs = (CallSite) MH_bootstrap13 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap13 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper13 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call13 ().invokeExact(o1, o2, o3); }

    static Object bootstrap13 (Object l, Object n, Object t) throws Throwable { return _mh[ 13 ].invokeExact(l, n, t); }

    // 14
    private static MethodType MT_bootstrap14 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap14 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap14", MT_bootstrap14 ());
    }

    private static MethodHandle INDY_call14;
    private static MethodHandle INDY_call14 () throws Throwable {
        if (INDY_call14 != null) return INDY_call14;
        CallSite cs = (CallSite) MH_bootstrap14 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap14 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper14 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call14 ().invokeExact(o1, o2, o3); }

    static Object bootstrap14 (Object l, Object n, Object t) throws Throwable { return _mh[ 14 ].invokeExact(l, n, t); }

    // 15
    private static MethodType MT_bootstrap15 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap15 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap15", MT_bootstrap15 ());
    }

    private static MethodHandle INDY_call15;
    private static MethodHandle INDY_call15 () throws Throwable {
        if (INDY_call15 != null) return INDY_call15;
        CallSite cs = (CallSite) MH_bootstrap15 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap15 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper15 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call15 ().invokeExact(o1, o2, o3); }

    static Object bootstrap15 (Object l, Object n, Object t) throws Throwable { return _mh[ 15 ].invokeExact(l, n, t); }

    // 16
    private static MethodType MT_bootstrap16 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap16 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap16", MT_bootstrap16 ());
    }

    private static MethodHandle INDY_call16;
    private static MethodHandle INDY_call16 () throws Throwable {
        if (INDY_call16 != null) return INDY_call16;
        CallSite cs = (CallSite) MH_bootstrap16 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap16 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper16 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call16 ().invokeExact(o1, o2, o3); }

    static Object bootstrap16 (Object l, Object n, Object t) throws Throwable { return _mh[ 16 ].invokeExact(l, n, t); }

    // 17
    private static MethodType MT_bootstrap17 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap17 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap17", MT_bootstrap17 ());
    }

    private static MethodHandle INDY_call17;
    private static MethodHandle INDY_call17 () throws Throwable {
        if (INDY_call17 != null) return INDY_call17;
        CallSite cs = (CallSite) MH_bootstrap17 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap17 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper17 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call17 ().invokeExact(o1, o2, o3); }

    static Object bootstrap17 (Object l, Object n, Object t) throws Throwable { return _mh[ 17 ].invokeExact(l, n, t); }

    // 18
    private static MethodType MT_bootstrap18 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap18 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap18", MT_bootstrap18 ());
    }

    private static MethodHandle INDY_call18;
    private static MethodHandle INDY_call18 () throws Throwable {
        if (INDY_call18 != null) return INDY_call18;
        CallSite cs = (CallSite) MH_bootstrap18 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap18 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper18 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call18 ().invokeExact(o1, o2, o3); }

    static Object bootstrap18 (Object l, Object n, Object t) throws Throwable { return _mh[ 18 ].invokeExact(l, n, t); }

    // 19
    private static MethodType MT_bootstrap19 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap19 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap19", MT_bootstrap19 ());
    }

    private static MethodHandle INDY_call19;
    private static MethodHandle INDY_call19 () throws Throwable {
        if (INDY_call19 != null) return INDY_call19;
        CallSite cs = (CallSite) MH_bootstrap19 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap19 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper19 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call19 ().invokeExact(o1, o2, o3); }

    static Object bootstrap19 (Object l, Object n, Object t) throws Throwable { return _mh[ 19 ].invokeExact(l, n, t); }

    // 20
    private static MethodType MT_bootstrap20 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap20 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap20", MT_bootstrap20 ());
    }

    private static MethodHandle INDY_call20;
    private static MethodHandle INDY_call20 () throws Throwable {
        if (INDY_call20 != null) return INDY_call20;
        CallSite cs = (CallSite) MH_bootstrap20 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap20 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper20 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call20 ().invokeExact(o1, o2, o3); }

    static Object bootstrap20 (Object l, Object n, Object t) throws Throwable { return _mh[ 20 ].invokeExact(l, n, t); }

    // 21
    private static MethodType MT_bootstrap21 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap21 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap21", MT_bootstrap21 ());
    }

    private static MethodHandle INDY_call21;
    private static MethodHandle INDY_call21 () throws Throwable {
        if (INDY_call21 != null) return INDY_call21;
        CallSite cs = (CallSite) MH_bootstrap21 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap21 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper21 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call21 ().invokeExact(o1, o2, o3); }

    static Object bootstrap21 (Object l, Object n, Object t) throws Throwable { return _mh[ 21 ].invokeExact(l, n, t); }

    // 22
    private static MethodType MT_bootstrap22 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap22 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap22", MT_bootstrap22 ());
    }

    private static MethodHandle INDY_call22;
    private static MethodHandle INDY_call22 () throws Throwable {
        if (INDY_call22 != null) return INDY_call22;
        CallSite cs = (CallSite) MH_bootstrap22 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap22 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper22 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call22 ().invokeExact(o1, o2, o3); }

    static Object bootstrap22 (Object l, Object n, Object t) throws Throwable { return _mh[ 22 ].invokeExact(l, n, t); }

    // 23
    private static MethodType MT_bootstrap23 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap23 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap23", MT_bootstrap23 ());
    }

    private static MethodHandle INDY_call23;
    private static MethodHandle INDY_call23 () throws Throwable {
        if (INDY_call23 != null) return INDY_call23;
        CallSite cs = (CallSite) MH_bootstrap23 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap23 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper23 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call23 ().invokeExact(o1, o2, o3); }

    static Object bootstrap23 (Object l, Object n, Object t) throws Throwable { return _mh[ 23 ].invokeExact(l, n, t); }

    // 24
    private static MethodType MT_bootstrap24 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap24 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap24", MT_bootstrap24 ());
    }

    private static MethodHandle INDY_call24;
    private static MethodHandle INDY_call24 () throws Throwable {
        if (INDY_call24 != null) return INDY_call24;
        CallSite cs = (CallSite) MH_bootstrap24 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap24 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper24 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call24 ().invokeExact(o1, o2, o3); }

    static Object bootstrap24 (Object l, Object n, Object t) throws Throwable { return _mh[ 24 ].invokeExact(l, n, t); }

    // 25
    private static MethodType MT_bootstrap25 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap25 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap25", MT_bootstrap25 ());
    }

    private static MethodHandle INDY_call25;
    private static MethodHandle INDY_call25 () throws Throwable {
        if (INDY_call25 != null) return INDY_call25;
        CallSite cs = (CallSite) MH_bootstrap25 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap25 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper25 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call25 ().invokeExact(o1, o2, o3); }

    static Object bootstrap25 (Object l, Object n, Object t) throws Throwable { return _mh[ 25 ].invokeExact(l, n, t); }

    // 26
    private static MethodType MT_bootstrap26 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap26 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap26", MT_bootstrap26 ());
    }

    private static MethodHandle INDY_call26;
    private static MethodHandle INDY_call26 () throws Throwable {
        if (INDY_call26 != null) return INDY_call26;
        CallSite cs = (CallSite) MH_bootstrap26 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap26 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper26 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call26 ().invokeExact(o1, o2, o3); }

    static Object bootstrap26 (Object l, Object n, Object t) throws Throwable { return _mh[ 26 ].invokeExact(l, n, t); }

    // 27
    private static MethodType MT_bootstrap27 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap27 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap27", MT_bootstrap27 ());
    }

    private static MethodHandle INDY_call27;
    private static MethodHandle INDY_call27 () throws Throwable {
        if (INDY_call27 != null) return INDY_call27;
        CallSite cs = (CallSite) MH_bootstrap27 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap27 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper27 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call27 ().invokeExact(o1, o2, o3); }

    static Object bootstrap27 (Object l, Object n, Object t) throws Throwable { return _mh[ 27 ].invokeExact(l, n, t); }

    // 28
    private static MethodType MT_bootstrap28 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap28 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap28", MT_bootstrap28 ());
    }

    private static MethodHandle INDY_call28;
    private static MethodHandle INDY_call28 () throws Throwable {
        if (INDY_call28 != null) return INDY_call28;
        CallSite cs = (CallSite) MH_bootstrap28 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap28 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper28 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call28 ().invokeExact(o1, o2, o3); }

    static Object bootstrap28 (Object l, Object n, Object t) throws Throwable { return _mh[ 28 ].invokeExact(l, n, t); }

    // 29
    private static MethodType MT_bootstrap29 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap29 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap29", MT_bootstrap29 ());
    }

    private static MethodHandle INDY_call29;
    private static MethodHandle INDY_call29 () throws Throwable {
        if (INDY_call29 != null) return INDY_call29;
        CallSite cs = (CallSite) MH_bootstrap29 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap29 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper29 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call29 ().invokeExact(o1, o2, o3); }

    static Object bootstrap29 (Object l, Object n, Object t) throws Throwable { return _mh[ 29 ].invokeExact(l, n, t); }

    // 30
    private static MethodType MT_bootstrap30 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap30 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap30", MT_bootstrap30 ());
    }

    private static MethodHandle INDY_call30;
    private static MethodHandle INDY_call30 () throws Throwable {
        if (INDY_call30 != null) return INDY_call30;
        CallSite cs = (CallSite) MH_bootstrap30 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap30 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper30 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call30 ().invokeExact(o1, o2, o3); }

    static Object bootstrap30 (Object l, Object n, Object t) throws Throwable { return _mh[ 30 ].invokeExact(l, n, t); }

    // 31
    private static MethodType MT_bootstrap31 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap31 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap31", MT_bootstrap31 ());
    }

    private static MethodHandle INDY_call31;
    private static MethodHandle INDY_call31 () throws Throwable {
        if (INDY_call31 != null) return INDY_call31;
        CallSite cs = (CallSite) MH_bootstrap31 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap31 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper31 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call31 ().invokeExact(o1, o2, o3); }

    static Object bootstrap31 (Object l, Object n, Object t) throws Throwable { return _mh[ 31 ].invokeExact(l, n, t); }

    // 32
    private static MethodType MT_bootstrap32 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap32 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap32", MT_bootstrap32 ());
    }

    private static MethodHandle INDY_call32;
    private static MethodHandle INDY_call32 () throws Throwable {
        if (INDY_call32 != null) return INDY_call32;
        CallSite cs = (CallSite) MH_bootstrap32 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap32 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper32 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call32 ().invokeExact(o1, o2, o3); }

    static Object bootstrap32 (Object l, Object n, Object t) throws Throwable { return _mh[ 32 ].invokeExact(l, n, t); }

    // 33
    private static MethodType MT_bootstrap33 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap33 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap33", MT_bootstrap33 ());
    }

    private static MethodHandle INDY_call33;
    private static MethodHandle INDY_call33 () throws Throwable {
        if (INDY_call33 != null) return INDY_call33;
        CallSite cs = (CallSite) MH_bootstrap33 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap33 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper33 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call33 ().invokeExact(o1, o2, o3); }

    static Object bootstrap33 (Object l, Object n, Object t) throws Throwable { return _mh[ 33 ].invokeExact(l, n, t); }

    // 34
    private static MethodType MT_bootstrap34 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap34 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap34", MT_bootstrap34 ());
    }

    private static MethodHandle INDY_call34;
    private static MethodHandle INDY_call34 () throws Throwable {
        if (INDY_call34 != null) return INDY_call34;
        CallSite cs = (CallSite) MH_bootstrap34 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap34 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper34 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call34 ().invokeExact(o1, o2, o3); }

    static Object bootstrap34 (Object l, Object n, Object t) throws Throwable { return _mh[ 34 ].invokeExact(l, n, t); }

    // 35
    private static MethodType MT_bootstrap35 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap35 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap35", MT_bootstrap35 ());
    }

    private static MethodHandle INDY_call35;
    private static MethodHandle INDY_call35 () throws Throwable {
        if (INDY_call35 != null) return INDY_call35;
        CallSite cs = (CallSite) MH_bootstrap35 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap35 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper35 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call35 ().invokeExact(o1, o2, o3); }

    static Object bootstrap35 (Object l, Object n, Object t) throws Throwable { return _mh[ 35 ].invokeExact(l, n, t); }

    // 36
    private static MethodType MT_bootstrap36 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap36 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap36", MT_bootstrap36 ());
    }

    private static MethodHandle INDY_call36;
    private static MethodHandle INDY_call36 () throws Throwable {
        if (INDY_call36 != null) return INDY_call36;
        CallSite cs = (CallSite) MH_bootstrap36 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap36 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper36 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call36 ().invokeExact(o1, o2, o3); }

    static Object bootstrap36 (Object l, Object n, Object t) throws Throwable { return _mh[ 36 ].invokeExact(l, n, t); }

    // 37
    private static MethodType MT_bootstrap37 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap37 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap37", MT_bootstrap37 ());
    }

    private static MethodHandle INDY_call37;
    private static MethodHandle INDY_call37 () throws Throwable {
        if (INDY_call37 != null) return INDY_call37;
        CallSite cs = (CallSite) MH_bootstrap37 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap37 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper37 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call37 ().invokeExact(o1, o2, o3); }

    static Object bootstrap37 (Object l, Object n, Object t) throws Throwable { return _mh[ 37 ].invokeExact(l, n, t); }

    // 38
    private static MethodType MT_bootstrap38 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap38 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap38", MT_bootstrap38 ());
    }

    private static MethodHandle INDY_call38;
    private static MethodHandle INDY_call38 () throws Throwable {
        if (INDY_call38 != null) return INDY_call38;
        CallSite cs = (CallSite) MH_bootstrap38 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap38 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper38 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call38 ().invokeExact(o1, o2, o3); }

    static Object bootstrap38 (Object l, Object n, Object t) throws Throwable { return _mh[ 38 ].invokeExact(l, n, t); }

    // 39
    private static MethodType MT_bootstrap39 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap39 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap39", MT_bootstrap39 ());
    }

    private static MethodHandle INDY_call39;
    private static MethodHandle INDY_call39 () throws Throwable {
        if (INDY_call39 != null) return INDY_call39;
        CallSite cs = (CallSite) MH_bootstrap39 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap39 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper39 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call39 ().invokeExact(o1, o2, o3); }

    static Object bootstrap39 (Object l, Object n, Object t) throws Throwable { return _mh[ 39 ].invokeExact(l, n, t); }

    // 40
    private static MethodType MT_bootstrap40 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap40 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap40", MT_bootstrap40 ());
    }

    private static MethodHandle INDY_call40;
    private static MethodHandle INDY_call40 () throws Throwable {
        if (INDY_call40 != null) return INDY_call40;
        CallSite cs = (CallSite) MH_bootstrap40 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap40 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper40 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call40 ().invokeExact(o1, o2, o3); }

    static Object bootstrap40 (Object l, Object n, Object t) throws Throwable { return _mh[ 40 ].invokeExact(l, n, t); }

    // 41
    private static MethodType MT_bootstrap41 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap41 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap41", MT_bootstrap41 ());
    }

    private static MethodHandle INDY_call41;
    private static MethodHandle INDY_call41 () throws Throwable {
        if (INDY_call41 != null) return INDY_call41;
        CallSite cs = (CallSite) MH_bootstrap41 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap41 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper41 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call41 ().invokeExact(o1, o2, o3); }

    static Object bootstrap41 (Object l, Object n, Object t) throws Throwable { return _mh[ 41 ].invokeExact(l, n, t); }

    // 42
    private static MethodType MT_bootstrap42 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap42 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap42", MT_bootstrap42 ());
    }

    private static MethodHandle INDY_call42;
    private static MethodHandle INDY_call42 () throws Throwable {
        if (INDY_call42 != null) return INDY_call42;
        CallSite cs = (CallSite) MH_bootstrap42 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap42 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper42 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call42 ().invokeExact(o1, o2, o3); }

    static Object bootstrap42 (Object l, Object n, Object t) throws Throwable { return _mh[ 42 ].invokeExact(l, n, t); }

    // 43
    private static MethodType MT_bootstrap43 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap43 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap43", MT_bootstrap43 ());
    }

    private static MethodHandle INDY_call43;
    private static MethodHandle INDY_call43 () throws Throwable {
        if (INDY_call43 != null) return INDY_call43;
        CallSite cs = (CallSite) MH_bootstrap43 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap43 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper43 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call43 ().invokeExact(o1, o2, o3); }

    static Object bootstrap43 (Object l, Object n, Object t) throws Throwable { return _mh[ 43 ].invokeExact(l, n, t); }

    // 44
    private static MethodType MT_bootstrap44 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap44 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap44", MT_bootstrap44 ());
    }

    private static MethodHandle INDY_call44;
    private static MethodHandle INDY_call44 () throws Throwable {
        if (INDY_call44 != null) return INDY_call44;
        CallSite cs = (CallSite) MH_bootstrap44 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap44 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper44 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call44 ().invokeExact(o1, o2, o3); }

    static Object bootstrap44 (Object l, Object n, Object t) throws Throwable { return _mh[ 44 ].invokeExact(l, n, t); }

    // 45
    private static MethodType MT_bootstrap45 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap45 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap45", MT_bootstrap45 ());
    }

    private static MethodHandle INDY_call45;
    private static MethodHandle INDY_call45 () throws Throwable {
        if (INDY_call45 != null) return INDY_call45;
        CallSite cs = (CallSite) MH_bootstrap45 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap45 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper45 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call45 ().invokeExact(o1, o2, o3); }

    static Object bootstrap45 (Object l, Object n, Object t) throws Throwable { return _mh[ 45 ].invokeExact(l, n, t); }

    // 46
    private static MethodType MT_bootstrap46 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap46 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap46", MT_bootstrap46 ());
    }

    private static MethodHandle INDY_call46;
    private static MethodHandle INDY_call46 () throws Throwable {
        if (INDY_call46 != null) return INDY_call46;
        CallSite cs = (CallSite) MH_bootstrap46 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap46 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper46 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call46 ().invokeExact(o1, o2, o3); }

    static Object bootstrap46 (Object l, Object n, Object t) throws Throwable { return _mh[ 46 ].invokeExact(l, n, t); }

    // 47
    private static MethodType MT_bootstrap47 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap47 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap47", MT_bootstrap47 ());
    }

    private static MethodHandle INDY_call47;
    private static MethodHandle INDY_call47 () throws Throwable {
        if (INDY_call47 != null) return INDY_call47;
        CallSite cs = (CallSite) MH_bootstrap47 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap47 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper47 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call47 ().invokeExact(o1, o2, o3); }

    static Object bootstrap47 (Object l, Object n, Object t) throws Throwable { return _mh[ 47 ].invokeExact(l, n, t); }

    // 48
    private static MethodType MT_bootstrap48 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap48 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap48", MT_bootstrap48 ());
    }

    private static MethodHandle INDY_call48;
    private static MethodHandle INDY_call48 () throws Throwable {
        if (INDY_call48 != null) return INDY_call48;
        CallSite cs = (CallSite) MH_bootstrap48 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap48 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper48 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call48 ().invokeExact(o1, o2, o3); }

    static Object bootstrap48 (Object l, Object n, Object t) throws Throwable { return _mh[ 48 ].invokeExact(l, n, t); }

    // 49
    private static MethodType MT_bootstrap49 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap49 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap49", MT_bootstrap49 ());
    }

    private static MethodHandle INDY_call49;
    private static MethodHandle INDY_call49 () throws Throwable {
        if (INDY_call49 != null) return INDY_call49;
        CallSite cs = (CallSite) MH_bootstrap49 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap49 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper49 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call49 ().invokeExact(o1, o2, o3); }

    static Object bootstrap49 (Object l, Object n, Object t) throws Throwable { return _mh[ 49 ].invokeExact(l, n, t); }

    // 50
    private static MethodType MT_bootstrap50 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap50 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap50", MT_bootstrap50 ());
    }

    private static MethodHandle INDY_call50;
    private static MethodHandle INDY_call50 () throws Throwable {
        if (INDY_call50 != null) return INDY_call50;
        CallSite cs = (CallSite) MH_bootstrap50 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap50 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper50 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call50 ().invokeExact(o1, o2, o3); }

    static Object bootstrap50 (Object l, Object n, Object t) throws Throwable { return _mh[ 50 ].invokeExact(l, n, t); }

    // 51
    private static MethodType MT_bootstrap51 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap51 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap51", MT_bootstrap51 ());
    }

    private static MethodHandle INDY_call51;
    private static MethodHandle INDY_call51 () throws Throwable {
        if (INDY_call51 != null) return INDY_call51;
        CallSite cs = (CallSite) MH_bootstrap51 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap51 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper51 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call51 ().invokeExact(o1, o2, o3); }

    static Object bootstrap51 (Object l, Object n, Object t) throws Throwable { return _mh[ 51 ].invokeExact(l, n, t); }

    // 52
    private static MethodType MT_bootstrap52 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap52 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap52", MT_bootstrap52 ());
    }

    private static MethodHandle INDY_call52;
    private static MethodHandle INDY_call52 () throws Throwable {
        if (INDY_call52 != null) return INDY_call52;
        CallSite cs = (CallSite) MH_bootstrap52 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap52 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper52 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call52 ().invokeExact(o1, o2, o3); }

    static Object bootstrap52 (Object l, Object n, Object t) throws Throwable { return _mh[ 52 ].invokeExact(l, n, t); }

    // 53
    private static MethodType MT_bootstrap53 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap53 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap53", MT_bootstrap53 ());
    }

    private static MethodHandle INDY_call53;
    private static MethodHandle INDY_call53 () throws Throwable {
        if (INDY_call53 != null) return INDY_call53;
        CallSite cs = (CallSite) MH_bootstrap53 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap53 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper53 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call53 ().invokeExact(o1, o2, o3); }

    static Object bootstrap53 (Object l, Object n, Object t) throws Throwable { return _mh[ 53 ].invokeExact(l, n, t); }

    // 54
    private static MethodType MT_bootstrap54 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap54 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap54", MT_bootstrap54 ());
    }

    private static MethodHandle INDY_call54;
    private static MethodHandle INDY_call54 () throws Throwable {
        if (INDY_call54 != null) return INDY_call54;
        CallSite cs = (CallSite) MH_bootstrap54 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap54 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper54 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call54 ().invokeExact(o1, o2, o3); }

    static Object bootstrap54 (Object l, Object n, Object t) throws Throwable { return _mh[ 54 ].invokeExact(l, n, t); }

    // 55
    private static MethodType MT_bootstrap55 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap55 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap55", MT_bootstrap55 ());
    }

    private static MethodHandle INDY_call55;
    private static MethodHandle INDY_call55 () throws Throwable {
        if (INDY_call55 != null) return INDY_call55;
        CallSite cs = (CallSite) MH_bootstrap55 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap55 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper55 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call55 ().invokeExact(o1, o2, o3); }

    static Object bootstrap55 (Object l, Object n, Object t) throws Throwable { return _mh[ 55 ].invokeExact(l, n, t); }

    // 56
    private static MethodType MT_bootstrap56 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap56 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap56", MT_bootstrap56 ());
    }

    private static MethodHandle INDY_call56;
    private static MethodHandle INDY_call56 () throws Throwable {
        if (INDY_call56 != null) return INDY_call56;
        CallSite cs = (CallSite) MH_bootstrap56 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap56 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper56 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call56 ().invokeExact(o1, o2, o3); }

    static Object bootstrap56 (Object l, Object n, Object t) throws Throwable { return _mh[ 56 ].invokeExact(l, n, t); }

    // 57
    private static MethodType MT_bootstrap57 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap57 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap57", MT_bootstrap57 ());
    }

    private static MethodHandle INDY_call57;
    private static MethodHandle INDY_call57 () throws Throwable {
        if (INDY_call57 != null) return INDY_call57;
        CallSite cs = (CallSite) MH_bootstrap57 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap57 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper57 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call57 ().invokeExact(o1, o2, o3); }

    static Object bootstrap57 (Object l, Object n, Object t) throws Throwable { return _mh[ 57 ].invokeExact(l, n, t); }

    // 58
    private static MethodType MT_bootstrap58 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap58 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap58", MT_bootstrap58 ());
    }

    private static MethodHandle INDY_call58;
    private static MethodHandle INDY_call58 () throws Throwable {
        if (INDY_call58 != null) return INDY_call58;
        CallSite cs = (CallSite) MH_bootstrap58 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap58 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper58 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call58 ().invokeExact(o1, o2, o3); }

    static Object bootstrap58 (Object l, Object n, Object t) throws Throwable { return _mh[ 58 ].invokeExact(l, n, t); }

    // 59
    private static MethodType MT_bootstrap59 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap59 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap59", MT_bootstrap59 ());
    }

    private static MethodHandle INDY_call59;
    private static MethodHandle INDY_call59 () throws Throwable {
        if (INDY_call59 != null) return INDY_call59;
        CallSite cs = (CallSite) MH_bootstrap59 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap59 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper59 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call59 ().invokeExact(o1, o2, o3); }

    static Object bootstrap59 (Object l, Object n, Object t) throws Throwable { return _mh[ 59 ].invokeExact(l, n, t); }

    // 60
    private static MethodType MT_bootstrap60 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap60 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap60", MT_bootstrap60 ());
    }

    private static MethodHandle INDY_call60;
    private static MethodHandle INDY_call60 () throws Throwable {
        if (INDY_call60 != null) return INDY_call60;
        CallSite cs = (CallSite) MH_bootstrap60 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap60 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper60 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call60 ().invokeExact(o1, o2, o3); }

    static Object bootstrap60 (Object l, Object n, Object t) throws Throwable { return _mh[ 60 ].invokeExact(l, n, t); }

    // 61
    private static MethodType MT_bootstrap61 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap61 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap61", MT_bootstrap61 ());
    }

    private static MethodHandle INDY_call61;
    private static MethodHandle INDY_call61 () throws Throwable {
        if (INDY_call61 != null) return INDY_call61;
        CallSite cs = (CallSite) MH_bootstrap61 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap61 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper61 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call61 ().invokeExact(o1, o2, o3); }

    static Object bootstrap61 (Object l, Object n, Object t) throws Throwable { return _mh[ 61 ].invokeExact(l, n, t); }

    // 62
    private static MethodType MT_bootstrap62 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap62 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap62", MT_bootstrap62 ());
    }

    private static MethodHandle INDY_call62;
    private static MethodHandle INDY_call62 () throws Throwable {
        if (INDY_call62 != null) return INDY_call62;
        CallSite cs = (CallSite) MH_bootstrap62 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap62 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper62 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call62 ().invokeExact(o1, o2, o3); }

    static Object bootstrap62 (Object l, Object n, Object t) throws Throwable { return _mh[ 62 ].invokeExact(l, n, t); }

    // 63
    private static MethodType MT_bootstrap63 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap63 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap63", MT_bootstrap63 ());
    }

    private static MethodHandle INDY_call63;
    private static MethodHandle INDY_call63 () throws Throwable {
        if (INDY_call63 != null) return INDY_call63;
        CallSite cs = (CallSite) MH_bootstrap63 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap63 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper63 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call63 ().invokeExact(o1, o2, o3); }

    static Object bootstrap63 (Object l, Object n, Object t) throws Throwable { return _mh[ 63 ].invokeExact(l, n, t); }

    // 64
    private static MethodType MT_bootstrap64 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap64 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap64", MT_bootstrap64 ());
    }

    private static MethodHandle INDY_call64;
    private static MethodHandle INDY_call64 () throws Throwable {
        if (INDY_call64 != null) return INDY_call64;
        CallSite cs = (CallSite) MH_bootstrap64 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap64 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper64 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call64 ().invokeExact(o1, o2, o3); }

    static Object bootstrap64 (Object l, Object n, Object t) throws Throwable { return _mh[ 64 ].invokeExact(l, n, t); }

    // 65
    private static MethodType MT_bootstrap65 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap65 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap65", MT_bootstrap65 ());
    }

    private static MethodHandle INDY_call65;
    private static MethodHandle INDY_call65 () throws Throwable {
        if (INDY_call65 != null) return INDY_call65;
        CallSite cs = (CallSite) MH_bootstrap65 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap65 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper65 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call65 ().invokeExact(o1, o2, o3); }

    static Object bootstrap65 (Object l, Object n, Object t) throws Throwable { return _mh[ 65 ].invokeExact(l, n, t); }

    // 66
    private static MethodType MT_bootstrap66 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap66 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap66", MT_bootstrap66 ());
    }

    private static MethodHandle INDY_call66;
    private static MethodHandle INDY_call66 () throws Throwable {
        if (INDY_call66 != null) return INDY_call66;
        CallSite cs = (CallSite) MH_bootstrap66 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap66 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper66 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call66 ().invokeExact(o1, o2, o3); }

    static Object bootstrap66 (Object l, Object n, Object t) throws Throwable { return _mh[ 66 ].invokeExact(l, n, t); }

    // 67
    private static MethodType MT_bootstrap67 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap67 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap67", MT_bootstrap67 ());
    }

    private static MethodHandle INDY_call67;
    private static MethodHandle INDY_call67 () throws Throwable {
        if (INDY_call67 != null) return INDY_call67;
        CallSite cs = (CallSite) MH_bootstrap67 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap67 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper67 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call67 ().invokeExact(o1, o2, o3); }

    static Object bootstrap67 (Object l, Object n, Object t) throws Throwable { return _mh[ 67 ].invokeExact(l, n, t); }

    // 68
    private static MethodType MT_bootstrap68 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap68 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap68", MT_bootstrap68 ());
    }

    private static MethodHandle INDY_call68;
    private static MethodHandle INDY_call68 () throws Throwable {
        if (INDY_call68 != null) return INDY_call68;
        CallSite cs = (CallSite) MH_bootstrap68 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap68 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper68 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call68 ().invokeExact(o1, o2, o3); }

    static Object bootstrap68 (Object l, Object n, Object t) throws Throwable { return _mh[ 68 ].invokeExact(l, n, t); }

    // 69
    private static MethodType MT_bootstrap69 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap69 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap69", MT_bootstrap69 ());
    }

    private static MethodHandle INDY_call69;
    private static MethodHandle INDY_call69 () throws Throwable {
        if (INDY_call69 != null) return INDY_call69;
        CallSite cs = (CallSite) MH_bootstrap69 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap69 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper69 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call69 ().invokeExact(o1, o2, o3); }

    static Object bootstrap69 (Object l, Object n, Object t) throws Throwable { return _mh[ 69 ].invokeExact(l, n, t); }

    // 70
    private static MethodType MT_bootstrap70 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap70 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap70", MT_bootstrap70 ());
    }

    private static MethodHandle INDY_call70;
    private static MethodHandle INDY_call70 () throws Throwable {
        if (INDY_call70 != null) return INDY_call70;
        CallSite cs = (CallSite) MH_bootstrap70 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap70 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper70 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call70 ().invokeExact(o1, o2, o3); }

    static Object bootstrap70 (Object l, Object n, Object t) throws Throwable { return _mh[ 70 ].invokeExact(l, n, t); }

    // 71
    private static MethodType MT_bootstrap71 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap71 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap71", MT_bootstrap71 ());
    }

    private static MethodHandle INDY_call71;
    private static MethodHandle INDY_call71 () throws Throwable {
        if (INDY_call71 != null) return INDY_call71;
        CallSite cs = (CallSite) MH_bootstrap71 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap71 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper71 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call71 ().invokeExact(o1, o2, o3); }

    static Object bootstrap71 (Object l, Object n, Object t) throws Throwable { return _mh[ 71 ].invokeExact(l, n, t); }

    // 72
    private static MethodType MT_bootstrap72 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap72 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap72", MT_bootstrap72 ());
    }

    private static MethodHandle INDY_call72;
    private static MethodHandle INDY_call72 () throws Throwable {
        if (INDY_call72 != null) return INDY_call72;
        CallSite cs = (CallSite) MH_bootstrap72 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap72 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper72 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call72 ().invokeExact(o1, o2, o3); }

    static Object bootstrap72 (Object l, Object n, Object t) throws Throwable { return _mh[ 72 ].invokeExact(l, n, t); }

    // 73
    private static MethodType MT_bootstrap73 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap73 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap73", MT_bootstrap73 ());
    }

    private static MethodHandle INDY_call73;
    private static MethodHandle INDY_call73 () throws Throwable {
        if (INDY_call73 != null) return INDY_call73;
        CallSite cs = (CallSite) MH_bootstrap73 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap73 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper73 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call73 ().invokeExact(o1, o2, o3); }

    static Object bootstrap73 (Object l, Object n, Object t) throws Throwable { return _mh[ 73 ].invokeExact(l, n, t); }

    // 74
    private static MethodType MT_bootstrap74 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap74 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap74", MT_bootstrap74 ());
    }

    private static MethodHandle INDY_call74;
    private static MethodHandle INDY_call74 () throws Throwable {
        if (INDY_call74 != null) return INDY_call74;
        CallSite cs = (CallSite) MH_bootstrap74 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap74 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper74 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call74 ().invokeExact(o1, o2, o3); }

    static Object bootstrap74 (Object l, Object n, Object t) throws Throwable { return _mh[ 74 ].invokeExact(l, n, t); }

    // 75
    private static MethodType MT_bootstrap75 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap75 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap75", MT_bootstrap75 ());
    }

    private static MethodHandle INDY_call75;
    private static MethodHandle INDY_call75 () throws Throwable {
        if (INDY_call75 != null) return INDY_call75;
        CallSite cs = (CallSite) MH_bootstrap75 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap75 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper75 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call75 ().invokeExact(o1, o2, o3); }

    static Object bootstrap75 (Object l, Object n, Object t) throws Throwable { return _mh[ 75 ].invokeExact(l, n, t); }

    // 76
    private static MethodType MT_bootstrap76 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap76 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap76", MT_bootstrap76 ());
    }

    private static MethodHandle INDY_call76;
    private static MethodHandle INDY_call76 () throws Throwable {
        if (INDY_call76 != null) return INDY_call76;
        CallSite cs = (CallSite) MH_bootstrap76 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap76 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper76 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call76 ().invokeExact(o1, o2, o3); }

    static Object bootstrap76 (Object l, Object n, Object t) throws Throwable { return _mh[ 76 ].invokeExact(l, n, t); }

    // 77
    private static MethodType MT_bootstrap77 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap77 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap77", MT_bootstrap77 ());
    }

    private static MethodHandle INDY_call77;
    private static MethodHandle INDY_call77 () throws Throwable {
        if (INDY_call77 != null) return INDY_call77;
        CallSite cs = (CallSite) MH_bootstrap77 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap77 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper77 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call77 ().invokeExact(o1, o2, o3); }

    static Object bootstrap77 (Object l, Object n, Object t) throws Throwable { return _mh[ 77 ].invokeExact(l, n, t); }

    // 78
    private static MethodType MT_bootstrap78 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap78 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap78", MT_bootstrap78 ());
    }

    private static MethodHandle INDY_call78;
    private static MethodHandle INDY_call78 () throws Throwable {
        if (INDY_call78 != null) return INDY_call78;
        CallSite cs = (CallSite) MH_bootstrap78 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap78 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper78 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call78 ().invokeExact(o1, o2, o3); }

    static Object bootstrap78 (Object l, Object n, Object t) throws Throwable { return _mh[ 78 ].invokeExact(l, n, t); }

    // 79
    private static MethodType MT_bootstrap79 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap79 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap79", MT_bootstrap79 ());
    }

    private static MethodHandle INDY_call79;
    private static MethodHandle INDY_call79 () throws Throwable {
        if (INDY_call79 != null) return INDY_call79;
        CallSite cs = (CallSite) MH_bootstrap79 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap79 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper79 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call79 ().invokeExact(o1, o2, o3); }

    static Object bootstrap79 (Object l, Object n, Object t) throws Throwable { return _mh[ 79 ].invokeExact(l, n, t); }

    // 80
    private static MethodType MT_bootstrap80 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap80 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap80", MT_bootstrap80 ());
    }

    private static MethodHandle INDY_call80;
    private static MethodHandle INDY_call80 () throws Throwable {
        if (INDY_call80 != null) return INDY_call80;
        CallSite cs = (CallSite) MH_bootstrap80 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap80 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper80 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call80 ().invokeExact(o1, o2, o3); }

    static Object bootstrap80 (Object l, Object n, Object t) throws Throwable { return _mh[ 80 ].invokeExact(l, n, t); }

    // 81
    private static MethodType MT_bootstrap81 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap81 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap81", MT_bootstrap81 ());
    }

    private static MethodHandle INDY_call81;
    private static MethodHandle INDY_call81 () throws Throwable {
        if (INDY_call81 != null) return INDY_call81;
        CallSite cs = (CallSite) MH_bootstrap81 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap81 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper81 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call81 ().invokeExact(o1, o2, o3); }

    static Object bootstrap81 (Object l, Object n, Object t) throws Throwable { return _mh[ 81 ].invokeExact(l, n, t); }

    // 82
    private static MethodType MT_bootstrap82 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap82 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap82", MT_bootstrap82 ());
    }

    private static MethodHandle INDY_call82;
    private static MethodHandle INDY_call82 () throws Throwable {
        if (INDY_call82 != null) return INDY_call82;
        CallSite cs = (CallSite) MH_bootstrap82 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap82 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper82 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call82 ().invokeExact(o1, o2, o3); }

    static Object bootstrap82 (Object l, Object n, Object t) throws Throwable { return _mh[ 82 ].invokeExact(l, n, t); }

    // 83
    private static MethodType MT_bootstrap83 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap83 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap83", MT_bootstrap83 ());
    }

    private static MethodHandle INDY_call83;
    private static MethodHandle INDY_call83 () throws Throwable {
        if (INDY_call83 != null) return INDY_call83;
        CallSite cs = (CallSite) MH_bootstrap83 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap83 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper83 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call83 ().invokeExact(o1, o2, o3); }

    static Object bootstrap83 (Object l, Object n, Object t) throws Throwable { return _mh[ 83 ].invokeExact(l, n, t); }

    // 84
    private static MethodType MT_bootstrap84 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap84 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap84", MT_bootstrap84 ());
    }

    private static MethodHandle INDY_call84;
    private static MethodHandle INDY_call84 () throws Throwable {
        if (INDY_call84 != null) return INDY_call84;
        CallSite cs = (CallSite) MH_bootstrap84 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap84 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper84 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call84 ().invokeExact(o1, o2, o3); }

    static Object bootstrap84 (Object l, Object n, Object t) throws Throwable { return _mh[ 84 ].invokeExact(l, n, t); }

    // 85
    private static MethodType MT_bootstrap85 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap85 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap85", MT_bootstrap85 ());
    }

    private static MethodHandle INDY_call85;
    private static MethodHandle INDY_call85 () throws Throwable {
        if (INDY_call85 != null) return INDY_call85;
        CallSite cs = (CallSite) MH_bootstrap85 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap85 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper85 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call85 ().invokeExact(o1, o2, o3); }

    static Object bootstrap85 (Object l, Object n, Object t) throws Throwable { return _mh[ 85 ].invokeExact(l, n, t); }

    // 86
    private static MethodType MT_bootstrap86 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap86 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap86", MT_bootstrap86 ());
    }

    private static MethodHandle INDY_call86;
    private static MethodHandle INDY_call86 () throws Throwable {
        if (INDY_call86 != null) return INDY_call86;
        CallSite cs = (CallSite) MH_bootstrap86 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap86 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper86 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call86 ().invokeExact(o1, o2, o3); }

    static Object bootstrap86 (Object l, Object n, Object t) throws Throwable { return _mh[ 86 ].invokeExact(l, n, t); }

    // 87
    private static MethodType MT_bootstrap87 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap87 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap87", MT_bootstrap87 ());
    }

    private static MethodHandle INDY_call87;
    private static MethodHandle INDY_call87 () throws Throwable {
        if (INDY_call87 != null) return INDY_call87;
        CallSite cs = (CallSite) MH_bootstrap87 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap87 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper87 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call87 ().invokeExact(o1, o2, o3); }

    static Object bootstrap87 (Object l, Object n, Object t) throws Throwable { return _mh[ 87 ].invokeExact(l, n, t); }

    // 88
    private static MethodType MT_bootstrap88 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap88 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap88", MT_bootstrap88 ());
    }

    private static MethodHandle INDY_call88;
    private static MethodHandle INDY_call88 () throws Throwable {
        if (INDY_call88 != null) return INDY_call88;
        CallSite cs = (CallSite) MH_bootstrap88 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap88 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper88 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call88 ().invokeExact(o1, o2, o3); }

    static Object bootstrap88 (Object l, Object n, Object t) throws Throwable { return _mh[ 88 ].invokeExact(l, n, t); }

    // 89
    private static MethodType MT_bootstrap89 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap89 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap89", MT_bootstrap89 ());
    }

    private static MethodHandle INDY_call89;
    private static MethodHandle INDY_call89 () throws Throwable {
        if (INDY_call89 != null) return INDY_call89;
        CallSite cs = (CallSite) MH_bootstrap89 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap89 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper89 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call89 ().invokeExact(o1, o2, o3); }

    static Object bootstrap89 (Object l, Object n, Object t) throws Throwable { return _mh[ 89 ].invokeExact(l, n, t); }

    // 90
    private static MethodType MT_bootstrap90 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap90 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap90", MT_bootstrap90 ());
    }

    private static MethodHandle INDY_call90;
    private static MethodHandle INDY_call90 () throws Throwable {
        if (INDY_call90 != null) return INDY_call90;
        CallSite cs = (CallSite) MH_bootstrap90 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap90 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper90 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call90 ().invokeExact(o1, o2, o3); }

    static Object bootstrap90 (Object l, Object n, Object t) throws Throwable { return _mh[ 90 ].invokeExact(l, n, t); }

    // 91
    private static MethodType MT_bootstrap91 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap91 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap91", MT_bootstrap91 ());
    }

    private static MethodHandle INDY_call91;
    private static MethodHandle INDY_call91 () throws Throwable {
        if (INDY_call91 != null) return INDY_call91;
        CallSite cs = (CallSite) MH_bootstrap91 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap91 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper91 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call91 ().invokeExact(o1, o2, o3); }

    static Object bootstrap91 (Object l, Object n, Object t) throws Throwable { return _mh[ 91 ].invokeExact(l, n, t); }

    // 92
    private static MethodType MT_bootstrap92 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap92 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap92", MT_bootstrap92 ());
    }

    private static MethodHandle INDY_call92;
    private static MethodHandle INDY_call92 () throws Throwable {
        if (INDY_call92 != null) return INDY_call92;
        CallSite cs = (CallSite) MH_bootstrap92 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap92 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper92 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call92 ().invokeExact(o1, o2, o3); }

    static Object bootstrap92 (Object l, Object n, Object t) throws Throwable { return _mh[ 92 ].invokeExact(l, n, t); }

    // 93
    private static MethodType MT_bootstrap93 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap93 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap93", MT_bootstrap93 ());
    }

    private static MethodHandle INDY_call93;
    private static MethodHandle INDY_call93 () throws Throwable {
        if (INDY_call93 != null) return INDY_call93;
        CallSite cs = (CallSite) MH_bootstrap93 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap93 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper93 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call93 ().invokeExact(o1, o2, o3); }

    static Object bootstrap93 (Object l, Object n, Object t) throws Throwable { return _mh[ 93 ].invokeExact(l, n, t); }

    // 94
    private static MethodType MT_bootstrap94 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap94 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap94", MT_bootstrap94 ());
    }

    private static MethodHandle INDY_call94;
    private static MethodHandle INDY_call94 () throws Throwable {
        if (INDY_call94 != null) return INDY_call94;
        CallSite cs = (CallSite) MH_bootstrap94 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap94 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper94 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call94 ().invokeExact(o1, o2, o3); }

    static Object bootstrap94 (Object l, Object n, Object t) throws Throwable { return _mh[ 94 ].invokeExact(l, n, t); }

    // 95
    private static MethodType MT_bootstrap95 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap95 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap95", MT_bootstrap95 ());
    }

    private static MethodHandle INDY_call95;
    private static MethodHandle INDY_call95 () throws Throwable {
        if (INDY_call95 != null) return INDY_call95;
        CallSite cs = (CallSite) MH_bootstrap95 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap95 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper95 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call95 ().invokeExact(o1, o2, o3); }

    static Object bootstrap95 (Object l, Object n, Object t) throws Throwable { return _mh[ 95 ].invokeExact(l, n, t); }

    // 96
    private static MethodType MT_bootstrap96 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap96 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap96", MT_bootstrap96 ());
    }

    private static MethodHandle INDY_call96;
    private static MethodHandle INDY_call96 () throws Throwable {
        if (INDY_call96 != null) return INDY_call96;
        CallSite cs = (CallSite) MH_bootstrap96 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap96 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper96 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call96 ().invokeExact(o1, o2, o3); }

    static Object bootstrap96 (Object l, Object n, Object t) throws Throwable { return _mh[ 96 ].invokeExact(l, n, t); }

    // 97
    private static MethodType MT_bootstrap97 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap97 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap97", MT_bootstrap97 ());
    }

    private static MethodHandle INDY_call97;
    private static MethodHandle INDY_call97 () throws Throwable {
        if (INDY_call97 != null) return INDY_call97;
        CallSite cs = (CallSite) MH_bootstrap97 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap97 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper97 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call97 ().invokeExact(o1, o2, o3); }

    static Object bootstrap97 (Object l, Object n, Object t) throws Throwable { return _mh[ 97 ].invokeExact(l, n, t); }

    // 98
    private static MethodType MT_bootstrap98 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap98 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap98", MT_bootstrap98 ());
    }

    private static MethodHandle INDY_call98;
    private static MethodHandle INDY_call98 () throws Throwable {
        if (INDY_call98 != null) return INDY_call98;
        CallSite cs = (CallSite) MH_bootstrap98 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap98 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper98 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call98 ().invokeExact(o1, o2, o3); }

    static Object bootstrap98 (Object l, Object n, Object t) throws Throwable { return _mh[ 98 ].invokeExact(l, n, t); }

    // 99
    private static MethodType MT_bootstrap99 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap99 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap99", MT_bootstrap99 ());
    }

    private static MethodHandle INDY_call99;
    private static MethodHandle INDY_call99 () throws Throwable {
        if (INDY_call99 != null) return INDY_call99;
        CallSite cs = (CallSite) MH_bootstrap99 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap99 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper99 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call99 ().invokeExact(o1, o2, o3); }

    static Object bootstrap99 (Object l, Object n, Object t) throws Throwable { return _mh[ 99 ].invokeExact(l, n, t); }

    // 100
    private static MethodType MT_bootstrap100 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap100 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap100", MT_bootstrap100 ());
    }

    private static MethodHandle INDY_call100;
    private static MethodHandle INDY_call100 () throws Throwable {
        if (INDY_call100 != null) return INDY_call100;
        CallSite cs = (CallSite) MH_bootstrap100 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap100 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper100 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call100 ().invokeExact(o1, o2, o3); }

    static Object bootstrap100 (Object l, Object n, Object t) throws Throwable { return _mh[ 100 ].invokeExact(l, n, t); }

    // 101
    private static MethodType MT_bootstrap101 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap101 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap101", MT_bootstrap101 ());
    }

    private static MethodHandle INDY_call101;
    private static MethodHandle INDY_call101 () throws Throwable {
        if (INDY_call101 != null) return INDY_call101;
        CallSite cs = (CallSite) MH_bootstrap101 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap101 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper101 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call101 ().invokeExact(o1, o2, o3); }

    static Object bootstrap101 (Object l, Object n, Object t) throws Throwable { return _mh[ 101 ].invokeExact(l, n, t); }

    // 102
    private static MethodType MT_bootstrap102 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap102 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap102", MT_bootstrap102 ());
    }

    private static MethodHandle INDY_call102;
    private static MethodHandle INDY_call102 () throws Throwable {
        if (INDY_call102 != null) return INDY_call102;
        CallSite cs = (CallSite) MH_bootstrap102 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap102 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper102 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call102 ().invokeExact(o1, o2, o3); }

    static Object bootstrap102 (Object l, Object n, Object t) throws Throwable { return _mh[ 102 ].invokeExact(l, n, t); }

    // 103
    private static MethodType MT_bootstrap103 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap103 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap103", MT_bootstrap103 ());
    }

    private static MethodHandle INDY_call103;
    private static MethodHandle INDY_call103 () throws Throwable {
        if (INDY_call103 != null) return INDY_call103;
        CallSite cs = (CallSite) MH_bootstrap103 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap103 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper103 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call103 ().invokeExact(o1, o2, o3); }

    static Object bootstrap103 (Object l, Object n, Object t) throws Throwable { return _mh[ 103 ].invokeExact(l, n, t); }

    // 104
    private static MethodType MT_bootstrap104 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap104 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap104", MT_bootstrap104 ());
    }

    private static MethodHandle INDY_call104;
    private static MethodHandle INDY_call104 () throws Throwable {
        if (INDY_call104 != null) return INDY_call104;
        CallSite cs = (CallSite) MH_bootstrap104 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap104 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper104 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call104 ().invokeExact(o1, o2, o3); }

    static Object bootstrap104 (Object l, Object n, Object t) throws Throwable { return _mh[ 104 ].invokeExact(l, n, t); }

    // 105
    private static MethodType MT_bootstrap105 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap105 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap105", MT_bootstrap105 ());
    }

    private static MethodHandle INDY_call105;
    private static MethodHandle INDY_call105 () throws Throwable {
        if (INDY_call105 != null) return INDY_call105;
        CallSite cs = (CallSite) MH_bootstrap105 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap105 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper105 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call105 ().invokeExact(o1, o2, o3); }

    static Object bootstrap105 (Object l, Object n, Object t) throws Throwable { return _mh[ 105 ].invokeExact(l, n, t); }

    // 106
    private static MethodType MT_bootstrap106 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap106 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap106", MT_bootstrap106 ());
    }

    private static MethodHandle INDY_call106;
    private static MethodHandle INDY_call106 () throws Throwable {
        if (INDY_call106 != null) return INDY_call106;
        CallSite cs = (CallSite) MH_bootstrap106 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap106 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper106 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call106 ().invokeExact(o1, o2, o3); }

    static Object bootstrap106 (Object l, Object n, Object t) throws Throwable { return _mh[ 106 ].invokeExact(l, n, t); }

    // 107
    private static MethodType MT_bootstrap107 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap107 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap107", MT_bootstrap107 ());
    }

    private static MethodHandle INDY_call107;
    private static MethodHandle INDY_call107 () throws Throwable {
        if (INDY_call107 != null) return INDY_call107;
        CallSite cs = (CallSite) MH_bootstrap107 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap107 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper107 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call107 ().invokeExact(o1, o2, o3); }

    static Object bootstrap107 (Object l, Object n, Object t) throws Throwable { return _mh[ 107 ].invokeExact(l, n, t); }

    // 108
    private static MethodType MT_bootstrap108 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap108 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap108", MT_bootstrap108 ());
    }

    private static MethodHandle INDY_call108;
    private static MethodHandle INDY_call108 () throws Throwable {
        if (INDY_call108 != null) return INDY_call108;
        CallSite cs = (CallSite) MH_bootstrap108 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap108 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper108 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call108 ().invokeExact(o1, o2, o3); }

    static Object bootstrap108 (Object l, Object n, Object t) throws Throwable { return _mh[ 108 ].invokeExact(l, n, t); }

    // 109
    private static MethodType MT_bootstrap109 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap109 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap109", MT_bootstrap109 ());
    }

    private static MethodHandle INDY_call109;
    private static MethodHandle INDY_call109 () throws Throwable {
        if (INDY_call109 != null) return INDY_call109;
        CallSite cs = (CallSite) MH_bootstrap109 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap109 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper109 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call109 ().invokeExact(o1, o2, o3); }

    static Object bootstrap109 (Object l, Object n, Object t) throws Throwable { return _mh[ 109 ].invokeExact(l, n, t); }

    // 110
    private static MethodType MT_bootstrap110 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap110 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap110", MT_bootstrap110 ());
    }

    private static MethodHandle INDY_call110;
    private static MethodHandle INDY_call110 () throws Throwable {
        if (INDY_call110 != null) return INDY_call110;
        CallSite cs = (CallSite) MH_bootstrap110 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap110 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper110 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call110 ().invokeExact(o1, o2, o3); }

    static Object bootstrap110 (Object l, Object n, Object t) throws Throwable { return _mh[ 110 ].invokeExact(l, n, t); }

    // 111
    private static MethodType MT_bootstrap111 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap111 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap111", MT_bootstrap111 ());
    }

    private static MethodHandle INDY_call111;
    private static MethodHandle INDY_call111 () throws Throwable {
        if (INDY_call111 != null) return INDY_call111;
        CallSite cs = (CallSite) MH_bootstrap111 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap111 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper111 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call111 ().invokeExact(o1, o2, o3); }

    static Object bootstrap111 (Object l, Object n, Object t) throws Throwable { return _mh[ 111 ].invokeExact(l, n, t); }

    // 112
    private static MethodType MT_bootstrap112 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap112 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap112", MT_bootstrap112 ());
    }

    private static MethodHandle INDY_call112;
    private static MethodHandle INDY_call112 () throws Throwable {
        if (INDY_call112 != null) return INDY_call112;
        CallSite cs = (CallSite) MH_bootstrap112 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap112 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper112 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call112 ().invokeExact(o1, o2, o3); }

    static Object bootstrap112 (Object l, Object n, Object t) throws Throwable { return _mh[ 112 ].invokeExact(l, n, t); }

    // 113
    private static MethodType MT_bootstrap113 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap113 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap113", MT_bootstrap113 ());
    }

    private static MethodHandle INDY_call113;
    private static MethodHandle INDY_call113 () throws Throwable {
        if (INDY_call113 != null) return INDY_call113;
        CallSite cs = (CallSite) MH_bootstrap113 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap113 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper113 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call113 ().invokeExact(o1, o2, o3); }

    static Object bootstrap113 (Object l, Object n, Object t) throws Throwable { return _mh[ 113 ].invokeExact(l, n, t); }

    // 114
    private static MethodType MT_bootstrap114 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap114 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap114", MT_bootstrap114 ());
    }

    private static MethodHandle INDY_call114;
    private static MethodHandle INDY_call114 () throws Throwable {
        if (INDY_call114 != null) return INDY_call114;
        CallSite cs = (CallSite) MH_bootstrap114 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap114 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper114 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call114 ().invokeExact(o1, o2, o3); }

    static Object bootstrap114 (Object l, Object n, Object t) throws Throwable { return _mh[ 114 ].invokeExact(l, n, t); }

    // 115
    private static MethodType MT_bootstrap115 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap115 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap115", MT_bootstrap115 ());
    }

    private static MethodHandle INDY_call115;
    private static MethodHandle INDY_call115 () throws Throwable {
        if (INDY_call115 != null) return INDY_call115;
        CallSite cs = (CallSite) MH_bootstrap115 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap115 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper115 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call115 ().invokeExact(o1, o2, o3); }

    static Object bootstrap115 (Object l, Object n, Object t) throws Throwable { return _mh[ 115 ].invokeExact(l, n, t); }

    // 116
    private static MethodType MT_bootstrap116 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap116 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap116", MT_bootstrap116 ());
    }

    private static MethodHandle INDY_call116;
    private static MethodHandle INDY_call116 () throws Throwable {
        if (INDY_call116 != null) return INDY_call116;
        CallSite cs = (CallSite) MH_bootstrap116 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap116 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper116 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call116 ().invokeExact(o1, o2, o3); }

    static Object bootstrap116 (Object l, Object n, Object t) throws Throwable { return _mh[ 116 ].invokeExact(l, n, t); }

    // 117
    private static MethodType MT_bootstrap117 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap117 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap117", MT_bootstrap117 ());
    }

    private static MethodHandle INDY_call117;
    private static MethodHandle INDY_call117 () throws Throwable {
        if (INDY_call117 != null) return INDY_call117;
        CallSite cs = (CallSite) MH_bootstrap117 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap117 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper117 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call117 ().invokeExact(o1, o2, o3); }

    static Object bootstrap117 (Object l, Object n, Object t) throws Throwable { return _mh[ 117 ].invokeExact(l, n, t); }

    // 118
    private static MethodType MT_bootstrap118 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap118 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap118", MT_bootstrap118 ());
    }

    private static MethodHandle INDY_call118;
    private static MethodHandle INDY_call118 () throws Throwable {
        if (INDY_call118 != null) return INDY_call118;
        CallSite cs = (CallSite) MH_bootstrap118 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap118 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper118 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call118 ().invokeExact(o1, o2, o3); }

    static Object bootstrap118 (Object l, Object n, Object t) throws Throwable { return _mh[ 118 ].invokeExact(l, n, t); }

    // 119
    private static MethodType MT_bootstrap119 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap119 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap119", MT_bootstrap119 ());
    }

    private static MethodHandle INDY_call119;
    private static MethodHandle INDY_call119 () throws Throwable {
        if (INDY_call119 != null) return INDY_call119;
        CallSite cs = (CallSite) MH_bootstrap119 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap119 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper119 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call119 ().invokeExact(o1, o2, o3); }

    static Object bootstrap119 (Object l, Object n, Object t) throws Throwable { return _mh[ 119 ].invokeExact(l, n, t); }

    // 120
    private static MethodType MT_bootstrap120 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap120 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap120", MT_bootstrap120 ());
    }

    private static MethodHandle INDY_call120;
    private static MethodHandle INDY_call120 () throws Throwable {
        if (INDY_call120 != null) return INDY_call120;
        CallSite cs = (CallSite) MH_bootstrap120 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap120 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper120 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call120 ().invokeExact(o1, o2, o3); }

    static Object bootstrap120 (Object l, Object n, Object t) throws Throwable { return _mh[ 120 ].invokeExact(l, n, t); }

    // 121
    private static MethodType MT_bootstrap121 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap121 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap121", MT_bootstrap121 ());
    }

    private static MethodHandle INDY_call121;
    private static MethodHandle INDY_call121 () throws Throwable {
        if (INDY_call121 != null) return INDY_call121;
        CallSite cs = (CallSite) MH_bootstrap121 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap121 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper121 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call121 ().invokeExact(o1, o2, o3); }

    static Object bootstrap121 (Object l, Object n, Object t) throws Throwable { return _mh[ 121 ].invokeExact(l, n, t); }

    // 122
    private static MethodType MT_bootstrap122 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap122 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap122", MT_bootstrap122 ());
    }

    private static MethodHandle INDY_call122;
    private static MethodHandle INDY_call122 () throws Throwable {
        if (INDY_call122 != null) return INDY_call122;
        CallSite cs = (CallSite) MH_bootstrap122 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap122 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper122 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call122 ().invokeExact(o1, o2, o3); }

    static Object bootstrap122 (Object l, Object n, Object t) throws Throwable { return _mh[ 122 ].invokeExact(l, n, t); }

    // 123
    private static MethodType MT_bootstrap123 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap123 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap123", MT_bootstrap123 ());
    }

    private static MethodHandle INDY_call123;
    private static MethodHandle INDY_call123 () throws Throwable {
        if (INDY_call123 != null) return INDY_call123;
        CallSite cs = (CallSite) MH_bootstrap123 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap123 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper123 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call123 ().invokeExact(o1, o2, o3); }

    static Object bootstrap123 (Object l, Object n, Object t) throws Throwable { return _mh[ 123 ].invokeExact(l, n, t); }

    // 124
    private static MethodType MT_bootstrap124 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap124 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap124", MT_bootstrap124 ());
    }

    private static MethodHandle INDY_call124;
    private static MethodHandle INDY_call124 () throws Throwable {
        if (INDY_call124 != null) return INDY_call124;
        CallSite cs = (CallSite) MH_bootstrap124 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap124 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper124 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call124 ().invokeExact(o1, o2, o3); }

    static Object bootstrap124 (Object l, Object n, Object t) throws Throwable { return _mh[ 124 ].invokeExact(l, n, t); }

    // 125
    private static MethodType MT_bootstrap125 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap125 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap125", MT_bootstrap125 ());
    }

    private static MethodHandle INDY_call125;
    private static MethodHandle INDY_call125 () throws Throwable {
        if (INDY_call125 != null) return INDY_call125;
        CallSite cs = (CallSite) MH_bootstrap125 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap125 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper125 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call125 ().invokeExact(o1, o2, o3); }

    static Object bootstrap125 (Object l, Object n, Object t) throws Throwable { return _mh[ 125 ].invokeExact(l, n, t); }

    // 126
    private static MethodType MT_bootstrap126 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap126 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap126", MT_bootstrap126 ());
    }

    private static MethodHandle INDY_call126;
    private static MethodHandle INDY_call126 () throws Throwable {
        if (INDY_call126 != null) return INDY_call126;
        CallSite cs = (CallSite) MH_bootstrap126 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap126 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper126 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call126 ().invokeExact(o1, o2, o3); }

    static Object bootstrap126 (Object l, Object n, Object t) throws Throwable { return _mh[ 126 ].invokeExact(l, n, t); }

    // 127
    private static MethodType MT_bootstrap127 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap127 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap127", MT_bootstrap127 ());
    }

    private static MethodHandle INDY_call127;
    private static MethodHandle INDY_call127 () throws Throwable {
        if (INDY_call127 != null) return INDY_call127;
        CallSite cs = (CallSite) MH_bootstrap127 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap127 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper127 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call127 ().invokeExact(o1, o2, o3); }

    static Object bootstrap127 (Object l, Object n, Object t) throws Throwable { return _mh[ 127 ].invokeExact(l, n, t); }

    // 128
    private static MethodType MT_bootstrap128 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap128 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap128", MT_bootstrap128 ());
    }

    private static MethodHandle INDY_call128;
    private static MethodHandle INDY_call128 () throws Throwable {
        if (INDY_call128 != null) return INDY_call128;
        CallSite cs = (CallSite) MH_bootstrap128 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap128 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper128 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call128 ().invokeExact(o1, o2, o3); }

    static Object bootstrap128 (Object l, Object n, Object t) throws Throwable { return _mh[ 128 ].invokeExact(l, n, t); }

    // 129
    private static MethodType MT_bootstrap129 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap129 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap129", MT_bootstrap129 ());
    }

    private static MethodHandle INDY_call129;
    private static MethodHandle INDY_call129 () throws Throwable {
        if (INDY_call129 != null) return INDY_call129;
        CallSite cs = (CallSite) MH_bootstrap129 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap129 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper129 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call129 ().invokeExact(o1, o2, o3); }

    static Object bootstrap129 (Object l, Object n, Object t) throws Throwable { return _mh[ 129 ].invokeExact(l, n, t); }

    // 130
    private static MethodType MT_bootstrap130 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap130 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap130", MT_bootstrap130 ());
    }

    private static MethodHandle INDY_call130;
    private static MethodHandle INDY_call130 () throws Throwable {
        if (INDY_call130 != null) return INDY_call130;
        CallSite cs = (CallSite) MH_bootstrap130 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap130 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper130 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call130 ().invokeExact(o1, o2, o3); }

    static Object bootstrap130 (Object l, Object n, Object t) throws Throwable { return _mh[ 130 ].invokeExact(l, n, t); }

    // 131
    private static MethodType MT_bootstrap131 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap131 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap131", MT_bootstrap131 ());
    }

    private static MethodHandle INDY_call131;
    private static MethodHandle INDY_call131 () throws Throwable {
        if (INDY_call131 != null) return INDY_call131;
        CallSite cs = (CallSite) MH_bootstrap131 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap131 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper131 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call131 ().invokeExact(o1, o2, o3); }

    static Object bootstrap131 (Object l, Object n, Object t) throws Throwable { return _mh[ 131 ].invokeExact(l, n, t); }

    // 132
    private static MethodType MT_bootstrap132 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap132 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap132", MT_bootstrap132 ());
    }

    private static MethodHandle INDY_call132;
    private static MethodHandle INDY_call132 () throws Throwable {
        if (INDY_call132 != null) return INDY_call132;
        CallSite cs = (CallSite) MH_bootstrap132 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap132 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper132 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call132 ().invokeExact(o1, o2, o3); }

    static Object bootstrap132 (Object l, Object n, Object t) throws Throwable { return _mh[ 132 ].invokeExact(l, n, t); }

    // 133
    private static MethodType MT_bootstrap133 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap133 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap133", MT_bootstrap133 ());
    }

    private static MethodHandle INDY_call133;
    private static MethodHandle INDY_call133 () throws Throwable {
        if (INDY_call133 != null) return INDY_call133;
        CallSite cs = (CallSite) MH_bootstrap133 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap133 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper133 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call133 ().invokeExact(o1, o2, o3); }

    static Object bootstrap133 (Object l, Object n, Object t) throws Throwable { return _mh[ 133 ].invokeExact(l, n, t); }

    // 134
    private static MethodType MT_bootstrap134 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap134 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap134", MT_bootstrap134 ());
    }

    private static MethodHandle INDY_call134;
    private static MethodHandle INDY_call134 () throws Throwable {
        if (INDY_call134 != null) return INDY_call134;
        CallSite cs = (CallSite) MH_bootstrap134 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap134 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper134 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call134 ().invokeExact(o1, o2, o3); }

    static Object bootstrap134 (Object l, Object n, Object t) throws Throwable { return _mh[ 134 ].invokeExact(l, n, t); }

    // 135
    private static MethodType MT_bootstrap135 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap135 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap135", MT_bootstrap135 ());
    }

    private static MethodHandle INDY_call135;
    private static MethodHandle INDY_call135 () throws Throwable {
        if (INDY_call135 != null) return INDY_call135;
        CallSite cs = (CallSite) MH_bootstrap135 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap135 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper135 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call135 ().invokeExact(o1, o2, o3); }

    static Object bootstrap135 (Object l, Object n, Object t) throws Throwable { return _mh[ 135 ].invokeExact(l, n, t); }

    // 136
    private static MethodType MT_bootstrap136 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap136 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap136", MT_bootstrap136 ());
    }

    private static MethodHandle INDY_call136;
    private static MethodHandle INDY_call136 () throws Throwable {
        if (INDY_call136 != null) return INDY_call136;
        CallSite cs = (CallSite) MH_bootstrap136 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap136 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper136 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call136 ().invokeExact(o1, o2, o3); }

    static Object bootstrap136 (Object l, Object n, Object t) throws Throwable { return _mh[ 136 ].invokeExact(l, n, t); }

    // 137
    private static MethodType MT_bootstrap137 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap137 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap137", MT_bootstrap137 ());
    }

    private static MethodHandle INDY_call137;
    private static MethodHandle INDY_call137 () throws Throwable {
        if (INDY_call137 != null) return INDY_call137;
        CallSite cs = (CallSite) MH_bootstrap137 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap137 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper137 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call137 ().invokeExact(o1, o2, o3); }

    static Object bootstrap137 (Object l, Object n, Object t) throws Throwable { return _mh[ 137 ].invokeExact(l, n, t); }

    // 138
    private static MethodType MT_bootstrap138 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap138 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap138", MT_bootstrap138 ());
    }

    private static MethodHandle INDY_call138;
    private static MethodHandle INDY_call138 () throws Throwable {
        if (INDY_call138 != null) return INDY_call138;
        CallSite cs = (CallSite) MH_bootstrap138 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap138 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper138 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call138 ().invokeExact(o1, o2, o3); }

    static Object bootstrap138 (Object l, Object n, Object t) throws Throwable { return _mh[ 138 ].invokeExact(l, n, t); }

    // 139
    private static MethodType MT_bootstrap139 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap139 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap139", MT_bootstrap139 ());
    }

    private static MethodHandle INDY_call139;
    private static MethodHandle INDY_call139 () throws Throwable {
        if (INDY_call139 != null) return INDY_call139;
        CallSite cs = (CallSite) MH_bootstrap139 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap139 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper139 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call139 ().invokeExact(o1, o2, o3); }

    static Object bootstrap139 (Object l, Object n, Object t) throws Throwable { return _mh[ 139 ].invokeExact(l, n, t); }

    // 140
    private static MethodType MT_bootstrap140 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap140 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap140", MT_bootstrap140 ());
    }

    private static MethodHandle INDY_call140;
    private static MethodHandle INDY_call140 () throws Throwable {
        if (INDY_call140 != null) return INDY_call140;
        CallSite cs = (CallSite) MH_bootstrap140 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap140 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper140 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call140 ().invokeExact(o1, o2, o3); }

    static Object bootstrap140 (Object l, Object n, Object t) throws Throwable { return _mh[ 140 ].invokeExact(l, n, t); }

    // 141
    private static MethodType MT_bootstrap141 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap141 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap141", MT_bootstrap141 ());
    }

    private static MethodHandle INDY_call141;
    private static MethodHandle INDY_call141 () throws Throwable {
        if (INDY_call141 != null) return INDY_call141;
        CallSite cs = (CallSite) MH_bootstrap141 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap141 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper141 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call141 ().invokeExact(o1, o2, o3); }

    static Object bootstrap141 (Object l, Object n, Object t) throws Throwable { return _mh[ 141 ].invokeExact(l, n, t); }

    // 142
    private static MethodType MT_bootstrap142 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap142 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap142", MT_bootstrap142 ());
    }

    private static MethodHandle INDY_call142;
    private static MethodHandle INDY_call142 () throws Throwable {
        if (INDY_call142 != null) return INDY_call142;
        CallSite cs = (CallSite) MH_bootstrap142 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap142 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper142 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call142 ().invokeExact(o1, o2, o3); }

    static Object bootstrap142 (Object l, Object n, Object t) throws Throwable { return _mh[ 142 ].invokeExact(l, n, t); }

    // 143
    private static MethodType MT_bootstrap143 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap143 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap143", MT_bootstrap143 ());
    }

    private static MethodHandle INDY_call143;
    private static MethodHandle INDY_call143 () throws Throwable {
        if (INDY_call143 != null) return INDY_call143;
        CallSite cs = (CallSite) MH_bootstrap143 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap143 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper143 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call143 ().invokeExact(o1, o2, o3); }

    static Object bootstrap143 (Object l, Object n, Object t) throws Throwable { return _mh[ 143 ].invokeExact(l, n, t); }

    // 144
    private static MethodType MT_bootstrap144 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap144 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap144", MT_bootstrap144 ());
    }

    private static MethodHandle INDY_call144;
    private static MethodHandle INDY_call144 () throws Throwable {
        if (INDY_call144 != null) return INDY_call144;
        CallSite cs = (CallSite) MH_bootstrap144 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap144 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper144 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call144 ().invokeExact(o1, o2, o3); }

    static Object bootstrap144 (Object l, Object n, Object t) throws Throwable { return _mh[ 144 ].invokeExact(l, n, t); }

    // 145
    private static MethodType MT_bootstrap145 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap145 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap145", MT_bootstrap145 ());
    }

    private static MethodHandle INDY_call145;
    private static MethodHandle INDY_call145 () throws Throwable {
        if (INDY_call145 != null) return INDY_call145;
        CallSite cs = (CallSite) MH_bootstrap145 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap145 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper145 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call145 ().invokeExact(o1, o2, o3); }

    static Object bootstrap145 (Object l, Object n, Object t) throws Throwable { return _mh[ 145 ].invokeExact(l, n, t); }

    // 146
    private static MethodType MT_bootstrap146 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap146 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap146", MT_bootstrap146 ());
    }

    private static MethodHandle INDY_call146;
    private static MethodHandle INDY_call146 () throws Throwable {
        if (INDY_call146 != null) return INDY_call146;
        CallSite cs = (CallSite) MH_bootstrap146 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap146 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper146 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call146 ().invokeExact(o1, o2, o3); }

    static Object bootstrap146 (Object l, Object n, Object t) throws Throwable { return _mh[ 146 ].invokeExact(l, n, t); }

    // 147
    private static MethodType MT_bootstrap147 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap147 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap147", MT_bootstrap147 ());
    }

    private static MethodHandle INDY_call147;
    private static MethodHandle INDY_call147 () throws Throwable {
        if (INDY_call147 != null) return INDY_call147;
        CallSite cs = (CallSite) MH_bootstrap147 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap147 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper147 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call147 ().invokeExact(o1, o2, o3); }

    static Object bootstrap147 (Object l, Object n, Object t) throws Throwable { return _mh[ 147 ].invokeExact(l, n, t); }

    // 148
    private static MethodType MT_bootstrap148 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap148 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap148", MT_bootstrap148 ());
    }

    private static MethodHandle INDY_call148;
    private static MethodHandle INDY_call148 () throws Throwable {
        if (INDY_call148 != null) return INDY_call148;
        CallSite cs = (CallSite) MH_bootstrap148 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap148 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper148 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call148 ().invokeExact(o1, o2, o3); }

    static Object bootstrap148 (Object l, Object n, Object t) throws Throwable { return _mh[ 148 ].invokeExact(l, n, t); }

    // 149
    private static MethodType MT_bootstrap149 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap149 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap149", MT_bootstrap149 ());
    }

    private static MethodHandle INDY_call149;
    private static MethodHandle INDY_call149 () throws Throwable {
        if (INDY_call149 != null) return INDY_call149;
        CallSite cs = (CallSite) MH_bootstrap149 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap149 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper149 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call149 ().invokeExact(o1, o2, o3); }

    static Object bootstrap149 (Object l, Object n, Object t) throws Throwable { return _mh[ 149 ].invokeExact(l, n, t); }

    // 150
    private static MethodType MT_bootstrap150 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap150 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap150", MT_bootstrap150 ());
    }

    private static MethodHandle INDY_call150;
    private static MethodHandle INDY_call150 () throws Throwable {
        if (INDY_call150 != null) return INDY_call150;
        CallSite cs = (CallSite) MH_bootstrap150 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap150 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper150 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call150 ().invokeExact(o1, o2, o3); }

    static Object bootstrap150 (Object l, Object n, Object t) throws Throwable { return _mh[ 150 ].invokeExact(l, n, t); }

    // 151
    private static MethodType MT_bootstrap151 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap151 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap151", MT_bootstrap151 ());
    }

    private static MethodHandle INDY_call151;
    private static MethodHandle INDY_call151 () throws Throwable {
        if (INDY_call151 != null) return INDY_call151;
        CallSite cs = (CallSite) MH_bootstrap151 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap151 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper151 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call151 ().invokeExact(o1, o2, o3); }

    static Object bootstrap151 (Object l, Object n, Object t) throws Throwable { return _mh[ 151 ].invokeExact(l, n, t); }

    // 152
    private static MethodType MT_bootstrap152 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap152 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap152", MT_bootstrap152 ());
    }

    private static MethodHandle INDY_call152;
    private static MethodHandle INDY_call152 () throws Throwable {
        if (INDY_call152 != null) return INDY_call152;
        CallSite cs = (CallSite) MH_bootstrap152 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap152 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper152 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call152 ().invokeExact(o1, o2, o3); }

    static Object bootstrap152 (Object l, Object n, Object t) throws Throwable { return _mh[ 152 ].invokeExact(l, n, t); }

    // 153
    private static MethodType MT_bootstrap153 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap153 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap153", MT_bootstrap153 ());
    }

    private static MethodHandle INDY_call153;
    private static MethodHandle INDY_call153 () throws Throwable {
        if (INDY_call153 != null) return INDY_call153;
        CallSite cs = (CallSite) MH_bootstrap153 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap153 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper153 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call153 ().invokeExact(o1, o2, o3); }

    static Object bootstrap153 (Object l, Object n, Object t) throws Throwable { return _mh[ 153 ].invokeExact(l, n, t); }

    // 154
    private static MethodType MT_bootstrap154 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap154 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap154", MT_bootstrap154 ());
    }

    private static MethodHandle INDY_call154;
    private static MethodHandle INDY_call154 () throws Throwable {
        if (INDY_call154 != null) return INDY_call154;
        CallSite cs = (CallSite) MH_bootstrap154 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap154 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper154 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call154 ().invokeExact(o1, o2, o3); }

    static Object bootstrap154 (Object l, Object n, Object t) throws Throwable { return _mh[ 154 ].invokeExact(l, n, t); }

    // 155
    private static MethodType MT_bootstrap155 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap155 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap155", MT_bootstrap155 ());
    }

    private static MethodHandle INDY_call155;
    private static MethodHandle INDY_call155 () throws Throwable {
        if (INDY_call155 != null) return INDY_call155;
        CallSite cs = (CallSite) MH_bootstrap155 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap155 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper155 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call155 ().invokeExact(o1, o2, o3); }

    static Object bootstrap155 (Object l, Object n, Object t) throws Throwable { return _mh[ 155 ].invokeExact(l, n, t); }

    // 156
    private static MethodType MT_bootstrap156 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap156 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap156", MT_bootstrap156 ());
    }

    private static MethodHandle INDY_call156;
    private static MethodHandle INDY_call156 () throws Throwable {
        if (INDY_call156 != null) return INDY_call156;
        CallSite cs = (CallSite) MH_bootstrap156 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap156 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper156 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call156 ().invokeExact(o1, o2, o3); }

    static Object bootstrap156 (Object l, Object n, Object t) throws Throwable { return _mh[ 156 ].invokeExact(l, n, t); }

    // 157
    private static MethodType MT_bootstrap157 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap157 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap157", MT_bootstrap157 ());
    }

    private static MethodHandle INDY_call157;
    private static MethodHandle INDY_call157 () throws Throwable {
        if (INDY_call157 != null) return INDY_call157;
        CallSite cs = (CallSite) MH_bootstrap157 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap157 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper157 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call157 ().invokeExact(o1, o2, o3); }

    static Object bootstrap157 (Object l, Object n, Object t) throws Throwable { return _mh[ 157 ].invokeExact(l, n, t); }

    // 158
    private static MethodType MT_bootstrap158 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap158 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap158", MT_bootstrap158 ());
    }

    private static MethodHandle INDY_call158;
    private static MethodHandle INDY_call158 () throws Throwable {
        if (INDY_call158 != null) return INDY_call158;
        CallSite cs = (CallSite) MH_bootstrap158 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap158 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper158 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call158 ().invokeExact(o1, o2, o3); }

    static Object bootstrap158 (Object l, Object n, Object t) throws Throwable { return _mh[ 158 ].invokeExact(l, n, t); }

    // 159
    private static MethodType MT_bootstrap159 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap159 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap159", MT_bootstrap159 ());
    }

    private static MethodHandle INDY_call159;
    private static MethodHandle INDY_call159 () throws Throwable {
        if (INDY_call159 != null) return INDY_call159;
        CallSite cs = (CallSite) MH_bootstrap159 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap159 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper159 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call159 ().invokeExact(o1, o2, o3); }

    static Object bootstrap159 (Object l, Object n, Object t) throws Throwable { return _mh[ 159 ].invokeExact(l, n, t); }

    // 160
    private static MethodType MT_bootstrap160 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap160 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap160", MT_bootstrap160 ());
    }

    private static MethodHandle INDY_call160;
    private static MethodHandle INDY_call160 () throws Throwable {
        if (INDY_call160 != null) return INDY_call160;
        CallSite cs = (CallSite) MH_bootstrap160 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap160 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper160 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call160 ().invokeExact(o1, o2, o3); }

    static Object bootstrap160 (Object l, Object n, Object t) throws Throwable { return _mh[ 160 ].invokeExact(l, n, t); }

    // 161
    private static MethodType MT_bootstrap161 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap161 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap161", MT_bootstrap161 ());
    }

    private static MethodHandle INDY_call161;
    private static MethodHandle INDY_call161 () throws Throwable {
        if (INDY_call161 != null) return INDY_call161;
        CallSite cs = (CallSite) MH_bootstrap161 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap161 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper161 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call161 ().invokeExact(o1, o2, o3); }

    static Object bootstrap161 (Object l, Object n, Object t) throws Throwable { return _mh[ 161 ].invokeExact(l, n, t); }

    // 162
    private static MethodType MT_bootstrap162 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap162 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap162", MT_bootstrap162 ());
    }

    private static MethodHandle INDY_call162;
    private static MethodHandle INDY_call162 () throws Throwable {
        if (INDY_call162 != null) return INDY_call162;
        CallSite cs = (CallSite) MH_bootstrap162 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap162 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper162 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call162 ().invokeExact(o1, o2, o3); }

    static Object bootstrap162 (Object l, Object n, Object t) throws Throwable { return _mh[ 162 ].invokeExact(l, n, t); }

    // 163
    private static MethodType MT_bootstrap163 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap163 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap163", MT_bootstrap163 ());
    }

    private static MethodHandle INDY_call163;
    private static MethodHandle INDY_call163 () throws Throwable {
        if (INDY_call163 != null) return INDY_call163;
        CallSite cs = (CallSite) MH_bootstrap163 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap163 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper163 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call163 ().invokeExact(o1, o2, o3); }

    static Object bootstrap163 (Object l, Object n, Object t) throws Throwable { return _mh[ 163 ].invokeExact(l, n, t); }

    // 164
    private static MethodType MT_bootstrap164 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap164 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap164", MT_bootstrap164 ());
    }

    private static MethodHandle INDY_call164;
    private static MethodHandle INDY_call164 () throws Throwable {
        if (INDY_call164 != null) return INDY_call164;
        CallSite cs = (CallSite) MH_bootstrap164 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap164 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper164 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call164 ().invokeExact(o1, o2, o3); }

    static Object bootstrap164 (Object l, Object n, Object t) throws Throwable { return _mh[ 164 ].invokeExact(l, n, t); }

    // 165
    private static MethodType MT_bootstrap165 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap165 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap165", MT_bootstrap165 ());
    }

    private static MethodHandle INDY_call165;
    private static MethodHandle INDY_call165 () throws Throwable {
        if (INDY_call165 != null) return INDY_call165;
        CallSite cs = (CallSite) MH_bootstrap165 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap165 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper165 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call165 ().invokeExact(o1, o2, o3); }

    static Object bootstrap165 (Object l, Object n, Object t) throws Throwable { return _mh[ 165 ].invokeExact(l, n, t); }

    // 166
    private static MethodType MT_bootstrap166 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap166 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap166", MT_bootstrap166 ());
    }

    private static MethodHandle INDY_call166;
    private static MethodHandle INDY_call166 () throws Throwable {
        if (INDY_call166 != null) return INDY_call166;
        CallSite cs = (CallSite) MH_bootstrap166 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap166 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper166 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call166 ().invokeExact(o1, o2, o3); }

    static Object bootstrap166 (Object l, Object n, Object t) throws Throwable { return _mh[ 166 ].invokeExact(l, n, t); }

    // 167
    private static MethodType MT_bootstrap167 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap167 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap167", MT_bootstrap167 ());
    }

    private static MethodHandle INDY_call167;
    private static MethodHandle INDY_call167 () throws Throwable {
        if (INDY_call167 != null) return INDY_call167;
        CallSite cs = (CallSite) MH_bootstrap167 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap167 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper167 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call167 ().invokeExact(o1, o2, o3); }

    static Object bootstrap167 (Object l, Object n, Object t) throws Throwable { return _mh[ 167 ].invokeExact(l, n, t); }

    // 168
    private static MethodType MT_bootstrap168 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap168 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap168", MT_bootstrap168 ());
    }

    private static MethodHandle INDY_call168;
    private static MethodHandle INDY_call168 () throws Throwable {
        if (INDY_call168 != null) return INDY_call168;
        CallSite cs = (CallSite) MH_bootstrap168 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap168 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper168 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call168 ().invokeExact(o1, o2, o3); }

    static Object bootstrap168 (Object l, Object n, Object t) throws Throwable { return _mh[ 168 ].invokeExact(l, n, t); }

    // 169
    private static MethodType MT_bootstrap169 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap169 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap169", MT_bootstrap169 ());
    }

    private static MethodHandle INDY_call169;
    private static MethodHandle INDY_call169 () throws Throwable {
        if (INDY_call169 != null) return INDY_call169;
        CallSite cs = (CallSite) MH_bootstrap169 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap169 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper169 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call169 ().invokeExact(o1, o2, o3); }

    static Object bootstrap169 (Object l, Object n, Object t) throws Throwable { return _mh[ 169 ].invokeExact(l, n, t); }

    // 170
    private static MethodType MT_bootstrap170 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap170 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap170", MT_bootstrap170 ());
    }

    private static MethodHandle INDY_call170;
    private static MethodHandle INDY_call170 () throws Throwable {
        if (INDY_call170 != null) return INDY_call170;
        CallSite cs = (CallSite) MH_bootstrap170 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap170 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper170 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call170 ().invokeExact(o1, o2, o3); }

    static Object bootstrap170 (Object l, Object n, Object t) throws Throwable { return _mh[ 170 ].invokeExact(l, n, t); }

    // 171
    private static MethodType MT_bootstrap171 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap171 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap171", MT_bootstrap171 ());
    }

    private static MethodHandle INDY_call171;
    private static MethodHandle INDY_call171 () throws Throwable {
        if (INDY_call171 != null) return INDY_call171;
        CallSite cs = (CallSite) MH_bootstrap171 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap171 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper171 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call171 ().invokeExact(o1, o2, o3); }

    static Object bootstrap171 (Object l, Object n, Object t) throws Throwable { return _mh[ 171 ].invokeExact(l, n, t); }

    // 172
    private static MethodType MT_bootstrap172 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap172 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap172", MT_bootstrap172 ());
    }

    private static MethodHandle INDY_call172;
    private static MethodHandle INDY_call172 () throws Throwable {
        if (INDY_call172 != null) return INDY_call172;
        CallSite cs = (CallSite) MH_bootstrap172 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap172 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper172 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call172 ().invokeExact(o1, o2, o3); }

    static Object bootstrap172 (Object l, Object n, Object t) throws Throwable { return _mh[ 172 ].invokeExact(l, n, t); }

    // 173
    private static MethodType MT_bootstrap173 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap173 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap173", MT_bootstrap173 ());
    }

    private static MethodHandle INDY_call173;
    private static MethodHandle INDY_call173 () throws Throwable {
        if (INDY_call173 != null) return INDY_call173;
        CallSite cs = (CallSite) MH_bootstrap173 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap173 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper173 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call173 ().invokeExact(o1, o2, o3); }

    static Object bootstrap173 (Object l, Object n, Object t) throws Throwable { return _mh[ 173 ].invokeExact(l, n, t); }

    // 174
    private static MethodType MT_bootstrap174 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap174 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap174", MT_bootstrap174 ());
    }

    private static MethodHandle INDY_call174;
    private static MethodHandle INDY_call174 () throws Throwable {
        if (INDY_call174 != null) return INDY_call174;
        CallSite cs = (CallSite) MH_bootstrap174 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap174 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper174 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call174 ().invokeExact(o1, o2, o3); }

    static Object bootstrap174 (Object l, Object n, Object t) throws Throwable { return _mh[ 174 ].invokeExact(l, n, t); }

    // 175
    private static MethodType MT_bootstrap175 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap175 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap175", MT_bootstrap175 ());
    }

    private static MethodHandle INDY_call175;
    private static MethodHandle INDY_call175 () throws Throwable {
        if (INDY_call175 != null) return INDY_call175;
        CallSite cs = (CallSite) MH_bootstrap175 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap175 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper175 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call175 ().invokeExact(o1, o2, o3); }

    static Object bootstrap175 (Object l, Object n, Object t) throws Throwable { return _mh[ 175 ].invokeExact(l, n, t); }

    // 176
    private static MethodType MT_bootstrap176 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap176 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap176", MT_bootstrap176 ());
    }

    private static MethodHandle INDY_call176;
    private static MethodHandle INDY_call176 () throws Throwable {
        if (INDY_call176 != null) return INDY_call176;
        CallSite cs = (CallSite) MH_bootstrap176 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap176 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper176 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call176 ().invokeExact(o1, o2, o3); }

    static Object bootstrap176 (Object l, Object n, Object t) throws Throwable { return _mh[ 176 ].invokeExact(l, n, t); }

    // 177
    private static MethodType MT_bootstrap177 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap177 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap177", MT_bootstrap177 ());
    }

    private static MethodHandle INDY_call177;
    private static MethodHandle INDY_call177 () throws Throwable {
        if (INDY_call177 != null) return INDY_call177;
        CallSite cs = (CallSite) MH_bootstrap177 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap177 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper177 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call177 ().invokeExact(o1, o2, o3); }

    static Object bootstrap177 (Object l, Object n, Object t) throws Throwable { return _mh[ 177 ].invokeExact(l, n, t); }

    // 178
    private static MethodType MT_bootstrap178 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap178 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap178", MT_bootstrap178 ());
    }

    private static MethodHandle INDY_call178;
    private static MethodHandle INDY_call178 () throws Throwable {
        if (INDY_call178 != null) return INDY_call178;
        CallSite cs = (CallSite) MH_bootstrap178 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap178 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper178 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call178 ().invokeExact(o1, o2, o3); }

    static Object bootstrap178 (Object l, Object n, Object t) throws Throwable { return _mh[ 178 ].invokeExact(l, n, t); }

    // 179
    private static MethodType MT_bootstrap179 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap179 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap179", MT_bootstrap179 ());
    }

    private static MethodHandle INDY_call179;
    private static MethodHandle INDY_call179 () throws Throwable {
        if (INDY_call179 != null) return INDY_call179;
        CallSite cs = (CallSite) MH_bootstrap179 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap179 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper179 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call179 ().invokeExact(o1, o2, o3); }

    static Object bootstrap179 (Object l, Object n, Object t) throws Throwable { return _mh[ 179 ].invokeExact(l, n, t); }

    // 180
    private static MethodType MT_bootstrap180 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap180 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap180", MT_bootstrap180 ());
    }

    private static MethodHandle INDY_call180;
    private static MethodHandle INDY_call180 () throws Throwable {
        if (INDY_call180 != null) return INDY_call180;
        CallSite cs = (CallSite) MH_bootstrap180 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap180 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper180 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call180 ().invokeExact(o1, o2, o3); }

    static Object bootstrap180 (Object l, Object n, Object t) throws Throwable { return _mh[ 180 ].invokeExact(l, n, t); }

    // 181
    private static MethodType MT_bootstrap181 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap181 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap181", MT_bootstrap181 ());
    }

    private static MethodHandle INDY_call181;
    private static MethodHandle INDY_call181 () throws Throwable {
        if (INDY_call181 != null) return INDY_call181;
        CallSite cs = (CallSite) MH_bootstrap181 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap181 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper181 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call181 ().invokeExact(o1, o2, o3); }

    static Object bootstrap181 (Object l, Object n, Object t) throws Throwable { return _mh[ 181 ].invokeExact(l, n, t); }

    // 182
    private static MethodType MT_bootstrap182 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap182 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap182", MT_bootstrap182 ());
    }

    private static MethodHandle INDY_call182;
    private static MethodHandle INDY_call182 () throws Throwable {
        if (INDY_call182 != null) return INDY_call182;
        CallSite cs = (CallSite) MH_bootstrap182 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap182 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper182 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call182 ().invokeExact(o1, o2, o3); }

    static Object bootstrap182 (Object l, Object n, Object t) throws Throwable { return _mh[ 182 ].invokeExact(l, n, t); }

    // 183
    private static MethodType MT_bootstrap183 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap183 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap183", MT_bootstrap183 ());
    }

    private static MethodHandle INDY_call183;
    private static MethodHandle INDY_call183 () throws Throwable {
        if (INDY_call183 != null) return INDY_call183;
        CallSite cs = (CallSite) MH_bootstrap183 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap183 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper183 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call183 ().invokeExact(o1, o2, o3); }

    static Object bootstrap183 (Object l, Object n, Object t) throws Throwable { return _mh[ 183 ].invokeExact(l, n, t); }

    // 184
    private static MethodType MT_bootstrap184 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap184 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap184", MT_bootstrap184 ());
    }

    private static MethodHandle INDY_call184;
    private static MethodHandle INDY_call184 () throws Throwable {
        if (INDY_call184 != null) return INDY_call184;
        CallSite cs = (CallSite) MH_bootstrap184 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap184 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper184 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call184 ().invokeExact(o1, o2, o3); }

    static Object bootstrap184 (Object l, Object n, Object t) throws Throwable { return _mh[ 184 ].invokeExact(l, n, t); }

    // 185
    private static MethodType MT_bootstrap185 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap185 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap185", MT_bootstrap185 ());
    }

    private static MethodHandle INDY_call185;
    private static MethodHandle INDY_call185 () throws Throwable {
        if (INDY_call185 != null) return INDY_call185;
        CallSite cs = (CallSite) MH_bootstrap185 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap185 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper185 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call185 ().invokeExact(o1, o2, o3); }

    static Object bootstrap185 (Object l, Object n, Object t) throws Throwable { return _mh[ 185 ].invokeExact(l, n, t); }

    // 186
    private static MethodType MT_bootstrap186 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap186 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap186", MT_bootstrap186 ());
    }

    private static MethodHandle INDY_call186;
    private static MethodHandle INDY_call186 () throws Throwable {
        if (INDY_call186 != null) return INDY_call186;
        CallSite cs = (CallSite) MH_bootstrap186 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap186 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper186 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call186 ().invokeExact(o1, o2, o3); }

    static Object bootstrap186 (Object l, Object n, Object t) throws Throwable { return _mh[ 186 ].invokeExact(l, n, t); }

    // 187
    private static MethodType MT_bootstrap187 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap187 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap187", MT_bootstrap187 ());
    }

    private static MethodHandle INDY_call187;
    private static MethodHandle INDY_call187 () throws Throwable {
        if (INDY_call187 != null) return INDY_call187;
        CallSite cs = (CallSite) MH_bootstrap187 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap187 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper187 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call187 ().invokeExact(o1, o2, o3); }

    static Object bootstrap187 (Object l, Object n, Object t) throws Throwable { return _mh[ 187 ].invokeExact(l, n, t); }

    // 188
    private static MethodType MT_bootstrap188 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap188 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap188", MT_bootstrap188 ());
    }

    private static MethodHandle INDY_call188;
    private static MethodHandle INDY_call188 () throws Throwable {
        if (INDY_call188 != null) return INDY_call188;
        CallSite cs = (CallSite) MH_bootstrap188 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap188 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper188 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call188 ().invokeExact(o1, o2, o3); }

    static Object bootstrap188 (Object l, Object n, Object t) throws Throwable { return _mh[ 188 ].invokeExact(l, n, t); }

    // 189
    private static MethodType MT_bootstrap189 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap189 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap189", MT_bootstrap189 ());
    }

    private static MethodHandle INDY_call189;
    private static MethodHandle INDY_call189 () throws Throwable {
        if (INDY_call189 != null) return INDY_call189;
        CallSite cs = (CallSite) MH_bootstrap189 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap189 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper189 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call189 ().invokeExact(o1, o2, o3); }

    static Object bootstrap189 (Object l, Object n, Object t) throws Throwable { return _mh[ 189 ].invokeExact(l, n, t); }

    // 190
    private static MethodType MT_bootstrap190 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap190 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap190", MT_bootstrap190 ());
    }

    private static MethodHandle INDY_call190;
    private static MethodHandle INDY_call190 () throws Throwable {
        if (INDY_call190 != null) return INDY_call190;
        CallSite cs = (CallSite) MH_bootstrap190 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap190 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper190 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call190 ().invokeExact(o1, o2, o3); }

    static Object bootstrap190 (Object l, Object n, Object t) throws Throwable { return _mh[ 190 ].invokeExact(l, n, t); }

    // 191
    private static MethodType MT_bootstrap191 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap191 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap191", MT_bootstrap191 ());
    }

    private static MethodHandle INDY_call191;
    private static MethodHandle INDY_call191 () throws Throwable {
        if (INDY_call191 != null) return INDY_call191;
        CallSite cs = (CallSite) MH_bootstrap191 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap191 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper191 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call191 ().invokeExact(o1, o2, o3); }

    static Object bootstrap191 (Object l, Object n, Object t) throws Throwable { return _mh[ 191 ].invokeExact(l, n, t); }

    // 192
    private static MethodType MT_bootstrap192 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap192 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap192", MT_bootstrap192 ());
    }

    private static MethodHandle INDY_call192;
    private static MethodHandle INDY_call192 () throws Throwable {
        if (INDY_call192 != null) return INDY_call192;
        CallSite cs = (CallSite) MH_bootstrap192 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap192 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper192 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call192 ().invokeExact(o1, o2, o3); }

    static Object bootstrap192 (Object l, Object n, Object t) throws Throwable { return _mh[ 192 ].invokeExact(l, n, t); }

    // 193
    private static MethodType MT_bootstrap193 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap193 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap193", MT_bootstrap193 ());
    }

    private static MethodHandle INDY_call193;
    private static MethodHandle INDY_call193 () throws Throwable {
        if (INDY_call193 != null) return INDY_call193;
        CallSite cs = (CallSite) MH_bootstrap193 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap193 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper193 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call193 ().invokeExact(o1, o2, o3); }

    static Object bootstrap193 (Object l, Object n, Object t) throws Throwable { return _mh[ 193 ].invokeExact(l, n, t); }

    // 194
    private static MethodType MT_bootstrap194 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap194 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap194", MT_bootstrap194 ());
    }

    private static MethodHandle INDY_call194;
    private static MethodHandle INDY_call194 () throws Throwable {
        if (INDY_call194 != null) return INDY_call194;
        CallSite cs = (CallSite) MH_bootstrap194 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap194 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper194 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call194 ().invokeExact(o1, o2, o3); }

    static Object bootstrap194 (Object l, Object n, Object t) throws Throwable { return _mh[ 194 ].invokeExact(l, n, t); }

    // 195
    private static MethodType MT_bootstrap195 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap195 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap195", MT_bootstrap195 ());
    }

    private static MethodHandle INDY_call195;
    private static MethodHandle INDY_call195 () throws Throwable {
        if (INDY_call195 != null) return INDY_call195;
        CallSite cs = (CallSite) MH_bootstrap195 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap195 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper195 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call195 ().invokeExact(o1, o2, o3); }

    static Object bootstrap195 (Object l, Object n, Object t) throws Throwable { return _mh[ 195 ].invokeExact(l, n, t); }

    // 196
    private static MethodType MT_bootstrap196 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap196 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap196", MT_bootstrap196 ());
    }

    private static MethodHandle INDY_call196;
    private static MethodHandle INDY_call196 () throws Throwable {
        if (INDY_call196 != null) return INDY_call196;
        CallSite cs = (CallSite) MH_bootstrap196 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap196 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper196 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call196 ().invokeExact(o1, o2, o3); }

    static Object bootstrap196 (Object l, Object n, Object t) throws Throwable { return _mh[ 196 ].invokeExact(l, n, t); }

    // 197
    private static MethodType MT_bootstrap197 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap197 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap197", MT_bootstrap197 ());
    }

    private static MethodHandle INDY_call197;
    private static MethodHandle INDY_call197 () throws Throwable {
        if (INDY_call197 != null) return INDY_call197;
        CallSite cs = (CallSite) MH_bootstrap197 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap197 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper197 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call197 ().invokeExact(o1, o2, o3); }

    static Object bootstrap197 (Object l, Object n, Object t) throws Throwable { return _mh[ 197 ].invokeExact(l, n, t); }

    // 198
    private static MethodType MT_bootstrap198 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap198 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap198", MT_bootstrap198 ());
    }

    private static MethodHandle INDY_call198;
    private static MethodHandle INDY_call198 () throws Throwable {
        if (INDY_call198 != null) return INDY_call198;
        CallSite cs = (CallSite) MH_bootstrap198 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap198 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper198 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call198 ().invokeExact(o1, o2, o3); }

    static Object bootstrap198 (Object l, Object n, Object t) throws Throwable { return _mh[ 198 ].invokeExact(l, n, t); }

    // 199
    private static MethodType MT_bootstrap199 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap199 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap199", MT_bootstrap199 ());
    }

    private static MethodHandle INDY_call199;
    private static MethodHandle INDY_call199 () throws Throwable {
        if (INDY_call199 != null) return INDY_call199;
        CallSite cs = (CallSite) MH_bootstrap199 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap199 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper199 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call199 ().invokeExact(o1, o2, o3); }

    static Object bootstrap199 (Object l, Object n, Object t) throws Throwable { return _mh[ 199 ].invokeExact(l, n, t); }

    // 200
    private static MethodType MT_bootstrap200 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap200 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap200", MT_bootstrap200 ());
    }

    private static MethodHandle INDY_call200;
    private static MethodHandle INDY_call200 () throws Throwable {
        if (INDY_call200 != null) return INDY_call200;
        CallSite cs = (CallSite) MH_bootstrap200 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap200 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper200 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call200 ().invokeExact(o1, o2, o3); }

    static Object bootstrap200 (Object l, Object n, Object t) throws Throwable { return _mh[ 200 ].invokeExact(l, n, t); }

    // 201
    private static MethodType MT_bootstrap201 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap201 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap201", MT_bootstrap201 ());
    }

    private static MethodHandle INDY_call201;
    private static MethodHandle INDY_call201 () throws Throwable {
        if (INDY_call201 != null) return INDY_call201;
        CallSite cs = (CallSite) MH_bootstrap201 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap201 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper201 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call201 ().invokeExact(o1, o2, o3); }

    static Object bootstrap201 (Object l, Object n, Object t) throws Throwable { return _mh[ 201 ].invokeExact(l, n, t); }

    // 202
    private static MethodType MT_bootstrap202 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap202 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap202", MT_bootstrap202 ());
    }

    private static MethodHandle INDY_call202;
    private static MethodHandle INDY_call202 () throws Throwable {
        if (INDY_call202 != null) return INDY_call202;
        CallSite cs = (CallSite) MH_bootstrap202 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap202 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper202 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call202 ().invokeExact(o1, o2, o3); }

    static Object bootstrap202 (Object l, Object n, Object t) throws Throwable { return _mh[ 202 ].invokeExact(l, n, t); }

    // 203
    private static MethodType MT_bootstrap203 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap203 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap203", MT_bootstrap203 ());
    }

    private static MethodHandle INDY_call203;
    private static MethodHandle INDY_call203 () throws Throwable {
        if (INDY_call203 != null) return INDY_call203;
        CallSite cs = (CallSite) MH_bootstrap203 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap203 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper203 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call203 ().invokeExact(o1, o2, o3); }

    static Object bootstrap203 (Object l, Object n, Object t) throws Throwable { return _mh[ 203 ].invokeExact(l, n, t); }

    // 204
    private static MethodType MT_bootstrap204 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap204 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap204", MT_bootstrap204 ());
    }

    private static MethodHandle INDY_call204;
    private static MethodHandle INDY_call204 () throws Throwable {
        if (INDY_call204 != null) return INDY_call204;
        CallSite cs = (CallSite) MH_bootstrap204 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap204 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper204 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call204 ().invokeExact(o1, o2, o3); }

    static Object bootstrap204 (Object l, Object n, Object t) throws Throwable { return _mh[ 204 ].invokeExact(l, n, t); }

    // 205
    private static MethodType MT_bootstrap205 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap205 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap205", MT_bootstrap205 ());
    }

    private static MethodHandle INDY_call205;
    private static MethodHandle INDY_call205 () throws Throwable {
        if (INDY_call205 != null) return INDY_call205;
        CallSite cs = (CallSite) MH_bootstrap205 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap205 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper205 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call205 ().invokeExact(o1, o2, o3); }

    static Object bootstrap205 (Object l, Object n, Object t) throws Throwable { return _mh[ 205 ].invokeExact(l, n, t); }

    // 206
    private static MethodType MT_bootstrap206 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap206 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap206", MT_bootstrap206 ());
    }

    private static MethodHandle INDY_call206;
    private static MethodHandle INDY_call206 () throws Throwable {
        if (INDY_call206 != null) return INDY_call206;
        CallSite cs = (CallSite) MH_bootstrap206 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap206 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper206 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call206 ().invokeExact(o1, o2, o3); }

    static Object bootstrap206 (Object l, Object n, Object t) throws Throwable { return _mh[ 206 ].invokeExact(l, n, t); }

    // 207
    private static MethodType MT_bootstrap207 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap207 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap207", MT_bootstrap207 ());
    }

    private static MethodHandle INDY_call207;
    private static MethodHandle INDY_call207 () throws Throwable {
        if (INDY_call207 != null) return INDY_call207;
        CallSite cs = (CallSite) MH_bootstrap207 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap207 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper207 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call207 ().invokeExact(o1, o2, o3); }

    static Object bootstrap207 (Object l, Object n, Object t) throws Throwable { return _mh[ 207 ].invokeExact(l, n, t); }

    // 208
    private static MethodType MT_bootstrap208 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap208 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap208", MT_bootstrap208 ());
    }

    private static MethodHandle INDY_call208;
    private static MethodHandle INDY_call208 () throws Throwable {
        if (INDY_call208 != null) return INDY_call208;
        CallSite cs = (CallSite) MH_bootstrap208 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap208 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper208 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call208 ().invokeExact(o1, o2, o3); }

    static Object bootstrap208 (Object l, Object n, Object t) throws Throwable { return _mh[ 208 ].invokeExact(l, n, t); }

    // 209
    private static MethodType MT_bootstrap209 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap209 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap209", MT_bootstrap209 ());
    }

    private static MethodHandle INDY_call209;
    private static MethodHandle INDY_call209 () throws Throwable {
        if (INDY_call209 != null) return INDY_call209;
        CallSite cs = (CallSite) MH_bootstrap209 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap209 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper209 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call209 ().invokeExact(o1, o2, o3); }

    static Object bootstrap209 (Object l, Object n, Object t) throws Throwable { return _mh[ 209 ].invokeExact(l, n, t); }

    // 210
    private static MethodType MT_bootstrap210 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap210 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap210", MT_bootstrap210 ());
    }

    private static MethodHandle INDY_call210;
    private static MethodHandle INDY_call210 () throws Throwable {
        if (INDY_call210 != null) return INDY_call210;
        CallSite cs = (CallSite) MH_bootstrap210 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap210 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper210 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call210 ().invokeExact(o1, o2, o3); }

    static Object bootstrap210 (Object l, Object n, Object t) throws Throwable { return _mh[ 210 ].invokeExact(l, n, t); }

    // 211
    private static MethodType MT_bootstrap211 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap211 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap211", MT_bootstrap211 ());
    }

    private static MethodHandle INDY_call211;
    private static MethodHandle INDY_call211 () throws Throwable {
        if (INDY_call211 != null) return INDY_call211;
        CallSite cs = (CallSite) MH_bootstrap211 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap211 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper211 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call211 ().invokeExact(o1, o2, o3); }

    static Object bootstrap211 (Object l, Object n, Object t) throws Throwable { return _mh[ 211 ].invokeExact(l, n, t); }

    // 212
    private static MethodType MT_bootstrap212 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap212 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap212", MT_bootstrap212 ());
    }

    private static MethodHandle INDY_call212;
    private static MethodHandle INDY_call212 () throws Throwable {
        if (INDY_call212 != null) return INDY_call212;
        CallSite cs = (CallSite) MH_bootstrap212 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap212 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper212 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call212 ().invokeExact(o1, o2, o3); }

    static Object bootstrap212 (Object l, Object n, Object t) throws Throwable { return _mh[ 212 ].invokeExact(l, n, t); }

    // 213
    private static MethodType MT_bootstrap213 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap213 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap213", MT_bootstrap213 ());
    }

    private static MethodHandle INDY_call213;
    private static MethodHandle INDY_call213 () throws Throwable {
        if (INDY_call213 != null) return INDY_call213;
        CallSite cs = (CallSite) MH_bootstrap213 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap213 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper213 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call213 ().invokeExact(o1, o2, o3); }

    static Object bootstrap213 (Object l, Object n, Object t) throws Throwable { return _mh[ 213 ].invokeExact(l, n, t); }

    // 214
    private static MethodType MT_bootstrap214 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap214 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap214", MT_bootstrap214 ());
    }

    private static MethodHandle INDY_call214;
    private static MethodHandle INDY_call214 () throws Throwable {
        if (INDY_call214 != null) return INDY_call214;
        CallSite cs = (CallSite) MH_bootstrap214 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap214 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper214 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call214 ().invokeExact(o1, o2, o3); }

    static Object bootstrap214 (Object l, Object n, Object t) throws Throwable { return _mh[ 214 ].invokeExact(l, n, t); }

    // 215
    private static MethodType MT_bootstrap215 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap215 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap215", MT_bootstrap215 ());
    }

    private static MethodHandle INDY_call215;
    private static MethodHandle INDY_call215 () throws Throwable {
        if (INDY_call215 != null) return INDY_call215;
        CallSite cs = (CallSite) MH_bootstrap215 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap215 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper215 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call215 ().invokeExact(o1, o2, o3); }

    static Object bootstrap215 (Object l, Object n, Object t) throws Throwable { return _mh[ 215 ].invokeExact(l, n, t); }

    // 216
    private static MethodType MT_bootstrap216 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap216 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap216", MT_bootstrap216 ());
    }

    private static MethodHandle INDY_call216;
    private static MethodHandle INDY_call216 () throws Throwable {
        if (INDY_call216 != null) return INDY_call216;
        CallSite cs = (CallSite) MH_bootstrap216 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap216 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper216 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call216 ().invokeExact(o1, o2, o3); }

    static Object bootstrap216 (Object l, Object n, Object t) throws Throwable { return _mh[ 216 ].invokeExact(l, n, t); }

    // 217
    private static MethodType MT_bootstrap217 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap217 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap217", MT_bootstrap217 ());
    }

    private static MethodHandle INDY_call217;
    private static MethodHandle INDY_call217 () throws Throwable {
        if (INDY_call217 != null) return INDY_call217;
        CallSite cs = (CallSite) MH_bootstrap217 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap217 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper217 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call217 ().invokeExact(o1, o2, o3); }

    static Object bootstrap217 (Object l, Object n, Object t) throws Throwable { return _mh[ 217 ].invokeExact(l, n, t); }

    // 218
    private static MethodType MT_bootstrap218 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap218 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap218", MT_bootstrap218 ());
    }

    private static MethodHandle INDY_call218;
    private static MethodHandle INDY_call218 () throws Throwable {
        if (INDY_call218 != null) return INDY_call218;
        CallSite cs = (CallSite) MH_bootstrap218 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap218 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper218 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call218 ().invokeExact(o1, o2, o3); }

    static Object bootstrap218 (Object l, Object n, Object t) throws Throwable { return _mh[ 218 ].invokeExact(l, n, t); }

    // 219
    private static MethodType MT_bootstrap219 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap219 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap219", MT_bootstrap219 ());
    }

    private static MethodHandle INDY_call219;
    private static MethodHandle INDY_call219 () throws Throwable {
        if (INDY_call219 != null) return INDY_call219;
        CallSite cs = (CallSite) MH_bootstrap219 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap219 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper219 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call219 ().invokeExact(o1, o2, o3); }

    static Object bootstrap219 (Object l, Object n, Object t) throws Throwable { return _mh[ 219 ].invokeExact(l, n, t); }

    // 220
    private static MethodType MT_bootstrap220 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap220 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap220", MT_bootstrap220 ());
    }

    private static MethodHandle INDY_call220;
    private static MethodHandle INDY_call220 () throws Throwable {
        if (INDY_call220 != null) return INDY_call220;
        CallSite cs = (CallSite) MH_bootstrap220 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap220 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper220 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call220 ().invokeExact(o1, o2, o3); }

    static Object bootstrap220 (Object l, Object n, Object t) throws Throwable { return _mh[ 220 ].invokeExact(l, n, t); }

    // 221
    private static MethodType MT_bootstrap221 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap221 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap221", MT_bootstrap221 ());
    }

    private static MethodHandle INDY_call221;
    private static MethodHandle INDY_call221 () throws Throwable {
        if (INDY_call221 != null) return INDY_call221;
        CallSite cs = (CallSite) MH_bootstrap221 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap221 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper221 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call221 ().invokeExact(o1, o2, o3); }

    static Object bootstrap221 (Object l, Object n, Object t) throws Throwable { return _mh[ 221 ].invokeExact(l, n, t); }

    // 222
    private static MethodType MT_bootstrap222 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap222 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap222", MT_bootstrap222 ());
    }

    private static MethodHandle INDY_call222;
    private static MethodHandle INDY_call222 () throws Throwable {
        if (INDY_call222 != null) return INDY_call222;
        CallSite cs = (CallSite) MH_bootstrap222 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap222 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper222 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call222 ().invokeExact(o1, o2, o3); }

    static Object bootstrap222 (Object l, Object n, Object t) throws Throwable { return _mh[ 222 ].invokeExact(l, n, t); }

    // 223
    private static MethodType MT_bootstrap223 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap223 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap223", MT_bootstrap223 ());
    }

    private static MethodHandle INDY_call223;
    private static MethodHandle INDY_call223 () throws Throwable {
        if (INDY_call223 != null) return INDY_call223;
        CallSite cs = (CallSite) MH_bootstrap223 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap223 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper223 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call223 ().invokeExact(o1, o2, o3); }

    static Object bootstrap223 (Object l, Object n, Object t) throws Throwable { return _mh[ 223 ].invokeExact(l, n, t); }

    // 224
    private static MethodType MT_bootstrap224 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap224 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap224", MT_bootstrap224 ());
    }

    private static MethodHandle INDY_call224;
    private static MethodHandle INDY_call224 () throws Throwable {
        if (INDY_call224 != null) return INDY_call224;
        CallSite cs = (CallSite) MH_bootstrap224 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap224 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper224 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call224 ().invokeExact(o1, o2, o3); }

    static Object bootstrap224 (Object l, Object n, Object t) throws Throwable { return _mh[ 224 ].invokeExact(l, n, t); }

    // 225
    private static MethodType MT_bootstrap225 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap225 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap225", MT_bootstrap225 ());
    }

    private static MethodHandle INDY_call225;
    private static MethodHandle INDY_call225 () throws Throwable {
        if (INDY_call225 != null) return INDY_call225;
        CallSite cs = (CallSite) MH_bootstrap225 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap225 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper225 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call225 ().invokeExact(o1, o2, o3); }

    static Object bootstrap225 (Object l, Object n, Object t) throws Throwable { return _mh[ 225 ].invokeExact(l, n, t); }

    // 226
    private static MethodType MT_bootstrap226 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap226 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap226", MT_bootstrap226 ());
    }

    private static MethodHandle INDY_call226;
    private static MethodHandle INDY_call226 () throws Throwable {
        if (INDY_call226 != null) return INDY_call226;
        CallSite cs = (CallSite) MH_bootstrap226 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap226 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper226 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call226 ().invokeExact(o1, o2, o3); }

    static Object bootstrap226 (Object l, Object n, Object t) throws Throwable { return _mh[ 226 ].invokeExact(l, n, t); }

    // 227
    private static MethodType MT_bootstrap227 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap227 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap227", MT_bootstrap227 ());
    }

    private static MethodHandle INDY_call227;
    private static MethodHandle INDY_call227 () throws Throwable {
        if (INDY_call227 != null) return INDY_call227;
        CallSite cs = (CallSite) MH_bootstrap227 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap227 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper227 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call227 ().invokeExact(o1, o2, o3); }

    static Object bootstrap227 (Object l, Object n, Object t) throws Throwable { return _mh[ 227 ].invokeExact(l, n, t); }

    // 228
    private static MethodType MT_bootstrap228 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap228 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap228", MT_bootstrap228 ());
    }

    private static MethodHandle INDY_call228;
    private static MethodHandle INDY_call228 () throws Throwable {
        if (INDY_call228 != null) return INDY_call228;
        CallSite cs = (CallSite) MH_bootstrap228 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap228 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper228 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call228 ().invokeExact(o1, o2, o3); }

    static Object bootstrap228 (Object l, Object n, Object t) throws Throwable { return _mh[ 228 ].invokeExact(l, n, t); }

    // 229
    private static MethodType MT_bootstrap229 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap229 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap229", MT_bootstrap229 ());
    }

    private static MethodHandle INDY_call229;
    private static MethodHandle INDY_call229 () throws Throwable {
        if (INDY_call229 != null) return INDY_call229;
        CallSite cs = (CallSite) MH_bootstrap229 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap229 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper229 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call229 ().invokeExact(o1, o2, o3); }

    static Object bootstrap229 (Object l, Object n, Object t) throws Throwable { return _mh[ 229 ].invokeExact(l, n, t); }

    // 230
    private static MethodType MT_bootstrap230 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap230 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap230", MT_bootstrap230 ());
    }

    private static MethodHandle INDY_call230;
    private static MethodHandle INDY_call230 () throws Throwable {
        if (INDY_call230 != null) return INDY_call230;
        CallSite cs = (CallSite) MH_bootstrap230 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap230 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper230 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call230 ().invokeExact(o1, o2, o3); }

    static Object bootstrap230 (Object l, Object n, Object t) throws Throwable { return _mh[ 230 ].invokeExact(l, n, t); }

    // 231
    private static MethodType MT_bootstrap231 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap231 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap231", MT_bootstrap231 ());
    }

    private static MethodHandle INDY_call231;
    private static MethodHandle INDY_call231 () throws Throwable {
        if (INDY_call231 != null) return INDY_call231;
        CallSite cs = (CallSite) MH_bootstrap231 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap231 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper231 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call231 ().invokeExact(o1, o2, o3); }

    static Object bootstrap231 (Object l, Object n, Object t) throws Throwable { return _mh[ 231 ].invokeExact(l, n, t); }

    // 232
    private static MethodType MT_bootstrap232 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap232 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap232", MT_bootstrap232 ());
    }

    private static MethodHandle INDY_call232;
    private static MethodHandle INDY_call232 () throws Throwable {
        if (INDY_call232 != null) return INDY_call232;
        CallSite cs = (CallSite) MH_bootstrap232 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap232 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper232 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call232 ().invokeExact(o1, o2, o3); }

    static Object bootstrap232 (Object l, Object n, Object t) throws Throwable { return _mh[ 232 ].invokeExact(l, n, t); }

    // 233
    private static MethodType MT_bootstrap233 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap233 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap233", MT_bootstrap233 ());
    }

    private static MethodHandle INDY_call233;
    private static MethodHandle INDY_call233 () throws Throwable {
        if (INDY_call233 != null) return INDY_call233;
        CallSite cs = (CallSite) MH_bootstrap233 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap233 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper233 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call233 ().invokeExact(o1, o2, o3); }

    static Object bootstrap233 (Object l, Object n, Object t) throws Throwable { return _mh[ 233 ].invokeExact(l, n, t); }

    // 234
    private static MethodType MT_bootstrap234 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap234 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap234", MT_bootstrap234 ());
    }

    private static MethodHandle INDY_call234;
    private static MethodHandle INDY_call234 () throws Throwable {
        if (INDY_call234 != null) return INDY_call234;
        CallSite cs = (CallSite) MH_bootstrap234 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap234 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper234 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call234 ().invokeExact(o1, o2, o3); }

    static Object bootstrap234 (Object l, Object n, Object t) throws Throwable { return _mh[ 234 ].invokeExact(l, n, t); }

    // 235
    private static MethodType MT_bootstrap235 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap235 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap235", MT_bootstrap235 ());
    }

    private static MethodHandle INDY_call235;
    private static MethodHandle INDY_call235 () throws Throwable {
        if (INDY_call235 != null) return INDY_call235;
        CallSite cs = (CallSite) MH_bootstrap235 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap235 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper235 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call235 ().invokeExact(o1, o2, o3); }

    static Object bootstrap235 (Object l, Object n, Object t) throws Throwable { return _mh[ 235 ].invokeExact(l, n, t); }

    // 236
    private static MethodType MT_bootstrap236 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap236 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap236", MT_bootstrap236 ());
    }

    private static MethodHandle INDY_call236;
    private static MethodHandle INDY_call236 () throws Throwable {
        if (INDY_call236 != null) return INDY_call236;
        CallSite cs = (CallSite) MH_bootstrap236 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap236 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper236 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call236 ().invokeExact(o1, o2, o3); }

    static Object bootstrap236 (Object l, Object n, Object t) throws Throwable { return _mh[ 236 ].invokeExact(l, n, t); }

    // 237
    private static MethodType MT_bootstrap237 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap237 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap237", MT_bootstrap237 ());
    }

    private static MethodHandle INDY_call237;
    private static MethodHandle INDY_call237 () throws Throwable {
        if (INDY_call237 != null) return INDY_call237;
        CallSite cs = (CallSite) MH_bootstrap237 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap237 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper237 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call237 ().invokeExact(o1, o2, o3); }

    static Object bootstrap237 (Object l, Object n, Object t) throws Throwable { return _mh[ 237 ].invokeExact(l, n, t); }

    // 238
    private static MethodType MT_bootstrap238 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap238 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap238", MT_bootstrap238 ());
    }

    private static MethodHandle INDY_call238;
    private static MethodHandle INDY_call238 () throws Throwable {
        if (INDY_call238 != null) return INDY_call238;
        CallSite cs = (CallSite) MH_bootstrap238 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap238 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper238 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call238 ().invokeExact(o1, o2, o3); }

    static Object bootstrap238 (Object l, Object n, Object t) throws Throwable { return _mh[ 238 ].invokeExact(l, n, t); }

    // 239
    private static MethodType MT_bootstrap239 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap239 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap239", MT_bootstrap239 ());
    }

    private static MethodHandle INDY_call239;
    private static MethodHandle INDY_call239 () throws Throwable {
        if (INDY_call239 != null) return INDY_call239;
        CallSite cs = (CallSite) MH_bootstrap239 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap239 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper239 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call239 ().invokeExact(o1, o2, o3); }

    static Object bootstrap239 (Object l, Object n, Object t) throws Throwable { return _mh[ 239 ].invokeExact(l, n, t); }

    // 240
    private static MethodType MT_bootstrap240 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap240 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap240", MT_bootstrap240 ());
    }

    private static MethodHandle INDY_call240;
    private static MethodHandle INDY_call240 () throws Throwable {
        if (INDY_call240 != null) return INDY_call240;
        CallSite cs = (CallSite) MH_bootstrap240 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap240 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper240 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call240 ().invokeExact(o1, o2, o3); }

    static Object bootstrap240 (Object l, Object n, Object t) throws Throwable { return _mh[ 240 ].invokeExact(l, n, t); }

    // 241
    private static MethodType MT_bootstrap241 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap241 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap241", MT_bootstrap241 ());
    }

    private static MethodHandle INDY_call241;
    private static MethodHandle INDY_call241 () throws Throwable {
        if (INDY_call241 != null) return INDY_call241;
        CallSite cs = (CallSite) MH_bootstrap241 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap241 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper241 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call241 ().invokeExact(o1, o2, o3); }

    static Object bootstrap241 (Object l, Object n, Object t) throws Throwable { return _mh[ 241 ].invokeExact(l, n, t); }

    // 242
    private static MethodType MT_bootstrap242 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap242 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap242", MT_bootstrap242 ());
    }

    private static MethodHandle INDY_call242;
    private static MethodHandle INDY_call242 () throws Throwable {
        if (INDY_call242 != null) return INDY_call242;
        CallSite cs = (CallSite) MH_bootstrap242 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap242 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper242 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call242 ().invokeExact(o1, o2, o3); }

    static Object bootstrap242 (Object l, Object n, Object t) throws Throwable { return _mh[ 242 ].invokeExact(l, n, t); }

    // 243
    private static MethodType MT_bootstrap243 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap243 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap243", MT_bootstrap243 ());
    }

    private static MethodHandle INDY_call243;
    private static MethodHandle INDY_call243 () throws Throwable {
        if (INDY_call243 != null) return INDY_call243;
        CallSite cs = (CallSite) MH_bootstrap243 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap243 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper243 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call243 ().invokeExact(o1, o2, o3); }

    static Object bootstrap243 (Object l, Object n, Object t) throws Throwable { return _mh[ 243 ].invokeExact(l, n, t); }

    // 244
    private static MethodType MT_bootstrap244 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap244 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap244", MT_bootstrap244 ());
    }

    private static MethodHandle INDY_call244;
    private static MethodHandle INDY_call244 () throws Throwable {
        if (INDY_call244 != null) return INDY_call244;
        CallSite cs = (CallSite) MH_bootstrap244 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap244 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper244 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call244 ().invokeExact(o1, o2, o3); }

    static Object bootstrap244 (Object l, Object n, Object t) throws Throwable { return _mh[ 244 ].invokeExact(l, n, t); }

    // 245
    private static MethodType MT_bootstrap245 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap245 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap245", MT_bootstrap245 ());
    }

    private static MethodHandle INDY_call245;
    private static MethodHandle INDY_call245 () throws Throwable {
        if (INDY_call245 != null) return INDY_call245;
        CallSite cs = (CallSite) MH_bootstrap245 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap245 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper245 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call245 ().invokeExact(o1, o2, o3); }

    static Object bootstrap245 (Object l, Object n, Object t) throws Throwable { return _mh[ 245 ].invokeExact(l, n, t); }

    // 246
    private static MethodType MT_bootstrap246 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap246 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap246", MT_bootstrap246 ());
    }

    private static MethodHandle INDY_call246;
    private static MethodHandle INDY_call246 () throws Throwable {
        if (INDY_call246 != null) return INDY_call246;
        CallSite cs = (CallSite) MH_bootstrap246 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap246 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper246 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call246 ().invokeExact(o1, o2, o3); }

    static Object bootstrap246 (Object l, Object n, Object t) throws Throwable { return _mh[ 246 ].invokeExact(l, n, t); }

    // 247
    private static MethodType MT_bootstrap247 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap247 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap247", MT_bootstrap247 ());
    }

    private static MethodHandle INDY_call247;
    private static MethodHandle INDY_call247 () throws Throwable {
        if (INDY_call247 != null) return INDY_call247;
        CallSite cs = (CallSite) MH_bootstrap247 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap247 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper247 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call247 ().invokeExact(o1, o2, o3); }

    static Object bootstrap247 (Object l, Object n, Object t) throws Throwable { return _mh[ 247 ].invokeExact(l, n, t); }

    // 248
    private static MethodType MT_bootstrap248 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap248 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap248", MT_bootstrap248 ());
    }

    private static MethodHandle INDY_call248;
    private static MethodHandle INDY_call248 () throws Throwable {
        if (INDY_call248 != null) return INDY_call248;
        CallSite cs = (CallSite) MH_bootstrap248 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap248 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper248 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call248 ().invokeExact(o1, o2, o3); }

    static Object bootstrap248 (Object l, Object n, Object t) throws Throwable { return _mh[ 248 ].invokeExact(l, n, t); }

    // 249
    private static MethodType MT_bootstrap249 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap249 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap249", MT_bootstrap249 ());
    }

    private static MethodHandle INDY_call249;
    private static MethodHandle INDY_call249 () throws Throwable {
        if (INDY_call249 != null) return INDY_call249;
        CallSite cs = (CallSite) MH_bootstrap249 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap249 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper249 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call249 ().invokeExact(o1, o2, o3); }

    static Object bootstrap249 (Object l, Object n, Object t) throws Throwable { return _mh[ 249 ].invokeExact(l, n, t); }

    // 250
    private static MethodType MT_bootstrap250 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap250 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap250", MT_bootstrap250 ());
    }

    private static MethodHandle INDY_call250;
    private static MethodHandle INDY_call250 () throws Throwable {
        if (INDY_call250 != null) return INDY_call250;
        CallSite cs = (CallSite) MH_bootstrap250 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap250 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper250 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call250 ().invokeExact(o1, o2, o3); }

    static Object bootstrap250 (Object l, Object n, Object t) throws Throwable { return _mh[ 250 ].invokeExact(l, n, t); }

    // 251
    private static MethodType MT_bootstrap251 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap251 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap251", MT_bootstrap251 ());
    }

    private static MethodHandle INDY_call251;
    private static MethodHandle INDY_call251 () throws Throwable {
        if (INDY_call251 != null) return INDY_call251;
        CallSite cs = (CallSite) MH_bootstrap251 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap251 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper251 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call251 ().invokeExact(o1, o2, o3); }

    static Object bootstrap251 (Object l, Object n, Object t) throws Throwable { return _mh[ 251 ].invokeExact(l, n, t); }

    // 252
    private static MethodType MT_bootstrap252 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap252 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap252", MT_bootstrap252 ());
    }

    private static MethodHandle INDY_call252;
    private static MethodHandle INDY_call252 () throws Throwable {
        if (INDY_call252 != null) return INDY_call252;
        CallSite cs = (CallSite) MH_bootstrap252 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap252 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper252 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call252 ().invokeExact(o1, o2, o3); }

    static Object bootstrap252 (Object l, Object n, Object t) throws Throwable { return _mh[ 252 ].invokeExact(l, n, t); }

    // 253
    private static MethodType MT_bootstrap253 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap253 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap253", MT_bootstrap253 ());
    }

    private static MethodHandle INDY_call253;
    private static MethodHandle INDY_call253 () throws Throwable {
        if (INDY_call253 != null) return INDY_call253;
        CallSite cs = (CallSite) MH_bootstrap253 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap253 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper253 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call253 ().invokeExact(o1, o2, o3); }

    static Object bootstrap253 (Object l, Object n, Object t) throws Throwable { return _mh[ 253 ].invokeExact(l, n, t); }

    // 254
    private static MethodType MT_bootstrap254 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap254 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap254", MT_bootstrap254 ());
    }

    private static MethodHandle INDY_call254;
    private static MethodHandle INDY_call254 () throws Throwable {
        if (INDY_call254 != null) return INDY_call254;
        CallSite cs = (CallSite) MH_bootstrap254 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap254 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper254 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call254 ().invokeExact(o1, o2, o3); }

    static Object bootstrap254 (Object l, Object n, Object t) throws Throwable { return _mh[ 254 ].invokeExact(l, n, t); }

    // 255
    private static MethodType MT_bootstrap255 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap255 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap255", MT_bootstrap255 ());
    }

    private static MethodHandle INDY_call255;
    private static MethodHandle INDY_call255 () throws Throwable {
        if (INDY_call255 != null) return INDY_call255;
        CallSite cs = (CallSite) MH_bootstrap255 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap255 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper255 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call255 ().invokeExact(o1, o2, o3); }

    static Object bootstrap255 (Object l, Object n, Object t) throws Throwable { return _mh[ 255 ].invokeExact(l, n, t); }

    // 256
    private static MethodType MT_bootstrap256 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap256 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap256", MT_bootstrap256 ());
    }

    private static MethodHandle INDY_call256;
    private static MethodHandle INDY_call256 () throws Throwable {
        if (INDY_call256 != null) return INDY_call256;
        CallSite cs = (CallSite) MH_bootstrap256 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap256 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper256 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call256 ().invokeExact(o1, o2, o3); }

    static Object bootstrap256 (Object l, Object n, Object t) throws Throwable { return _mh[ 256 ].invokeExact(l, n, t); }

    // 257
    private static MethodType MT_bootstrap257 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap257 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap257", MT_bootstrap257 ());
    }

    private static MethodHandle INDY_call257;
    private static MethodHandle INDY_call257 () throws Throwable {
        if (INDY_call257 != null) return INDY_call257;
        CallSite cs = (CallSite) MH_bootstrap257 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap257 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper257 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call257 ().invokeExact(o1, o2, o3); }

    static Object bootstrap257 (Object l, Object n, Object t) throws Throwable { return _mh[ 257 ].invokeExact(l, n, t); }

    // 258
    private static MethodType MT_bootstrap258 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap258 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap258", MT_bootstrap258 ());
    }

    private static MethodHandle INDY_call258;
    private static MethodHandle INDY_call258 () throws Throwable {
        if (INDY_call258 != null) return INDY_call258;
        CallSite cs = (CallSite) MH_bootstrap258 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap258 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper258 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call258 ().invokeExact(o1, o2, o3); }

    static Object bootstrap258 (Object l, Object n, Object t) throws Throwable { return _mh[ 258 ].invokeExact(l, n, t); }

    // 259
    private static MethodType MT_bootstrap259 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap259 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap259", MT_bootstrap259 ());
    }

    private static MethodHandle INDY_call259;
    private static MethodHandle INDY_call259 () throws Throwable {
        if (INDY_call259 != null) return INDY_call259;
        CallSite cs = (CallSite) MH_bootstrap259 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap259 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper259 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call259 ().invokeExact(o1, o2, o3); }

    static Object bootstrap259 (Object l, Object n, Object t) throws Throwable { return _mh[ 259 ].invokeExact(l, n, t); }

    // 260
    private static MethodType MT_bootstrap260 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap260 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap260", MT_bootstrap260 ());
    }

    private static MethodHandle INDY_call260;
    private static MethodHandle INDY_call260 () throws Throwable {
        if (INDY_call260 != null) return INDY_call260;
        CallSite cs = (CallSite) MH_bootstrap260 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap260 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper260 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call260 ().invokeExact(o1, o2, o3); }

    static Object bootstrap260 (Object l, Object n, Object t) throws Throwable { return _mh[ 260 ].invokeExact(l, n, t); }

    // 261
    private static MethodType MT_bootstrap261 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap261 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap261", MT_bootstrap261 ());
    }

    private static MethodHandle INDY_call261;
    private static MethodHandle INDY_call261 () throws Throwable {
        if (INDY_call261 != null) return INDY_call261;
        CallSite cs = (CallSite) MH_bootstrap261 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap261 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper261 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call261 ().invokeExact(o1, o2, o3); }

    static Object bootstrap261 (Object l, Object n, Object t) throws Throwable { return _mh[ 261 ].invokeExact(l, n, t); }

    // 262
    private static MethodType MT_bootstrap262 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap262 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap262", MT_bootstrap262 ());
    }

    private static MethodHandle INDY_call262;
    private static MethodHandle INDY_call262 () throws Throwable {
        if (INDY_call262 != null) return INDY_call262;
        CallSite cs = (CallSite) MH_bootstrap262 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap262 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper262 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call262 ().invokeExact(o1, o2, o3); }

    static Object bootstrap262 (Object l, Object n, Object t) throws Throwable { return _mh[ 262 ].invokeExact(l, n, t); }

    // 263
    private static MethodType MT_bootstrap263 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap263 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap263", MT_bootstrap263 ());
    }

    private static MethodHandle INDY_call263;
    private static MethodHandle INDY_call263 () throws Throwable {
        if (INDY_call263 != null) return INDY_call263;
        CallSite cs = (CallSite) MH_bootstrap263 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap263 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper263 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call263 ().invokeExact(o1, o2, o3); }

    static Object bootstrap263 (Object l, Object n, Object t) throws Throwable { return _mh[ 263 ].invokeExact(l, n, t); }

    // 264
    private static MethodType MT_bootstrap264 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap264 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap264", MT_bootstrap264 ());
    }

    private static MethodHandle INDY_call264;
    private static MethodHandle INDY_call264 () throws Throwable {
        if (INDY_call264 != null) return INDY_call264;
        CallSite cs = (CallSite) MH_bootstrap264 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap264 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper264 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call264 ().invokeExact(o1, o2, o3); }

    static Object bootstrap264 (Object l, Object n, Object t) throws Throwable { return _mh[ 264 ].invokeExact(l, n, t); }

    // 265
    private static MethodType MT_bootstrap265 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap265 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap265", MT_bootstrap265 ());
    }

    private static MethodHandle INDY_call265;
    private static MethodHandle INDY_call265 () throws Throwable {
        if (INDY_call265 != null) return INDY_call265;
        CallSite cs = (CallSite) MH_bootstrap265 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap265 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper265 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call265 ().invokeExact(o1, o2, o3); }

    static Object bootstrap265 (Object l, Object n, Object t) throws Throwable { return _mh[ 265 ].invokeExact(l, n, t); }

    // 266
    private static MethodType MT_bootstrap266 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap266 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap266", MT_bootstrap266 ());
    }

    private static MethodHandle INDY_call266;
    private static MethodHandle INDY_call266 () throws Throwable {
        if (INDY_call266 != null) return INDY_call266;
        CallSite cs = (CallSite) MH_bootstrap266 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap266 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper266 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call266 ().invokeExact(o1, o2, o3); }

    static Object bootstrap266 (Object l, Object n, Object t) throws Throwable { return _mh[ 266 ].invokeExact(l, n, t); }

    // 267
    private static MethodType MT_bootstrap267 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap267 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap267", MT_bootstrap267 ());
    }

    private static MethodHandle INDY_call267;
    private static MethodHandle INDY_call267 () throws Throwable {
        if (INDY_call267 != null) return INDY_call267;
        CallSite cs = (CallSite) MH_bootstrap267 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap267 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper267 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call267 ().invokeExact(o1, o2, o3); }

    static Object bootstrap267 (Object l, Object n, Object t) throws Throwable { return _mh[ 267 ].invokeExact(l, n, t); }

    // 268
    private static MethodType MT_bootstrap268 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap268 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap268", MT_bootstrap268 ());
    }

    private static MethodHandle INDY_call268;
    private static MethodHandle INDY_call268 () throws Throwable {
        if (INDY_call268 != null) return INDY_call268;
        CallSite cs = (CallSite) MH_bootstrap268 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap268 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper268 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call268 ().invokeExact(o1, o2, o3); }

    static Object bootstrap268 (Object l, Object n, Object t) throws Throwable { return _mh[ 268 ].invokeExact(l, n, t); }

    // 269
    private static MethodType MT_bootstrap269 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap269 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap269", MT_bootstrap269 ());
    }

    private static MethodHandle INDY_call269;
    private static MethodHandle INDY_call269 () throws Throwable {
        if (INDY_call269 != null) return INDY_call269;
        CallSite cs = (CallSite) MH_bootstrap269 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap269 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper269 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call269 ().invokeExact(o1, o2, o3); }

    static Object bootstrap269 (Object l, Object n, Object t) throws Throwable { return _mh[ 269 ].invokeExact(l, n, t); }

    // 270
    private static MethodType MT_bootstrap270 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap270 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap270", MT_bootstrap270 ());
    }

    private static MethodHandle INDY_call270;
    private static MethodHandle INDY_call270 () throws Throwable {
        if (INDY_call270 != null) return INDY_call270;
        CallSite cs = (CallSite) MH_bootstrap270 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap270 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper270 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call270 ().invokeExact(o1, o2, o3); }

    static Object bootstrap270 (Object l, Object n, Object t) throws Throwable { return _mh[ 270 ].invokeExact(l, n, t); }

    // 271
    private static MethodType MT_bootstrap271 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap271 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap271", MT_bootstrap271 ());
    }

    private static MethodHandle INDY_call271;
    private static MethodHandle INDY_call271 () throws Throwable {
        if (INDY_call271 != null) return INDY_call271;
        CallSite cs = (CallSite) MH_bootstrap271 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap271 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper271 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call271 ().invokeExact(o1, o2, o3); }

    static Object bootstrap271 (Object l, Object n, Object t) throws Throwable { return _mh[ 271 ].invokeExact(l, n, t); }

    // 272
    private static MethodType MT_bootstrap272 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap272 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap272", MT_bootstrap272 ());
    }

    private static MethodHandle INDY_call272;
    private static MethodHandle INDY_call272 () throws Throwable {
        if (INDY_call272 != null) return INDY_call272;
        CallSite cs = (CallSite) MH_bootstrap272 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap272 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper272 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call272 ().invokeExact(o1, o2, o3); }

    static Object bootstrap272 (Object l, Object n, Object t) throws Throwable { return _mh[ 272 ].invokeExact(l, n, t); }

    // 273
    private static MethodType MT_bootstrap273 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap273 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap273", MT_bootstrap273 ());
    }

    private static MethodHandle INDY_call273;
    private static MethodHandle INDY_call273 () throws Throwable {
        if (INDY_call273 != null) return INDY_call273;
        CallSite cs = (CallSite) MH_bootstrap273 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap273 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper273 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call273 ().invokeExact(o1, o2, o3); }

    static Object bootstrap273 (Object l, Object n, Object t) throws Throwable { return _mh[ 273 ].invokeExact(l, n, t); }

    // 274
    private static MethodType MT_bootstrap274 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap274 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap274", MT_bootstrap274 ());
    }

    private static MethodHandle INDY_call274;
    private static MethodHandle INDY_call274 () throws Throwable {
        if (INDY_call274 != null) return INDY_call274;
        CallSite cs = (CallSite) MH_bootstrap274 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap274 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper274 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call274 ().invokeExact(o1, o2, o3); }

    static Object bootstrap274 (Object l, Object n, Object t) throws Throwable { return _mh[ 274 ].invokeExact(l, n, t); }

    // 275
    private static MethodType MT_bootstrap275 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap275 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap275", MT_bootstrap275 ());
    }

    private static MethodHandle INDY_call275;
    private static MethodHandle INDY_call275 () throws Throwable {
        if (INDY_call275 != null) return INDY_call275;
        CallSite cs = (CallSite) MH_bootstrap275 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap275 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper275 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call275 ().invokeExact(o1, o2, o3); }

    static Object bootstrap275 (Object l, Object n, Object t) throws Throwable { return _mh[ 275 ].invokeExact(l, n, t); }

    // 276
    private static MethodType MT_bootstrap276 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap276 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap276", MT_bootstrap276 ());
    }

    private static MethodHandle INDY_call276;
    private static MethodHandle INDY_call276 () throws Throwable {
        if (INDY_call276 != null) return INDY_call276;
        CallSite cs = (CallSite) MH_bootstrap276 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap276 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper276 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call276 ().invokeExact(o1, o2, o3); }

    static Object bootstrap276 (Object l, Object n, Object t) throws Throwable { return _mh[ 276 ].invokeExact(l, n, t); }

    // 277
    private static MethodType MT_bootstrap277 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap277 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap277", MT_bootstrap277 ());
    }

    private static MethodHandle INDY_call277;
    private static MethodHandle INDY_call277 () throws Throwable {
        if (INDY_call277 != null) return INDY_call277;
        CallSite cs = (CallSite) MH_bootstrap277 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap277 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper277 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call277 ().invokeExact(o1, o2, o3); }

    static Object bootstrap277 (Object l, Object n, Object t) throws Throwable { return _mh[ 277 ].invokeExact(l, n, t); }

    // 278
    private static MethodType MT_bootstrap278 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap278 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap278", MT_bootstrap278 ());
    }

    private static MethodHandle INDY_call278;
    private static MethodHandle INDY_call278 () throws Throwable {
        if (INDY_call278 != null) return INDY_call278;
        CallSite cs = (CallSite) MH_bootstrap278 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap278 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper278 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call278 ().invokeExact(o1, o2, o3); }

    static Object bootstrap278 (Object l, Object n, Object t) throws Throwable { return _mh[ 278 ].invokeExact(l, n, t); }

    // 279
    private static MethodType MT_bootstrap279 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap279 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap279", MT_bootstrap279 ());
    }

    private static MethodHandle INDY_call279;
    private static MethodHandle INDY_call279 () throws Throwable {
        if (INDY_call279 != null) return INDY_call279;
        CallSite cs = (CallSite) MH_bootstrap279 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap279 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper279 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call279 ().invokeExact(o1, o2, o3); }

    static Object bootstrap279 (Object l, Object n, Object t) throws Throwable { return _mh[ 279 ].invokeExact(l, n, t); }

    // 280
    private static MethodType MT_bootstrap280 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap280 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap280", MT_bootstrap280 ());
    }

    private static MethodHandle INDY_call280;
    private static MethodHandle INDY_call280 () throws Throwable {
        if (INDY_call280 != null) return INDY_call280;
        CallSite cs = (CallSite) MH_bootstrap280 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap280 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper280 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call280 ().invokeExact(o1, o2, o3); }

    static Object bootstrap280 (Object l, Object n, Object t) throws Throwable { return _mh[ 280 ].invokeExact(l, n, t); }

    // 281
    private static MethodType MT_bootstrap281 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap281 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap281", MT_bootstrap281 ());
    }

    private static MethodHandle INDY_call281;
    private static MethodHandle INDY_call281 () throws Throwable {
        if (INDY_call281 != null) return INDY_call281;
        CallSite cs = (CallSite) MH_bootstrap281 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap281 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper281 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call281 ().invokeExact(o1, o2, o3); }

    static Object bootstrap281 (Object l, Object n, Object t) throws Throwable { return _mh[ 281 ].invokeExact(l, n, t); }

    // 282
    private static MethodType MT_bootstrap282 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap282 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap282", MT_bootstrap282 ());
    }

    private static MethodHandle INDY_call282;
    private static MethodHandle INDY_call282 () throws Throwable {
        if (INDY_call282 != null) return INDY_call282;
        CallSite cs = (CallSite) MH_bootstrap282 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap282 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper282 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call282 ().invokeExact(o1, o2, o3); }

    static Object bootstrap282 (Object l, Object n, Object t) throws Throwable { return _mh[ 282 ].invokeExact(l, n, t); }

    // 283
    private static MethodType MT_bootstrap283 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap283 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap283", MT_bootstrap283 ());
    }

    private static MethodHandle INDY_call283;
    private static MethodHandle INDY_call283 () throws Throwable {
        if (INDY_call283 != null) return INDY_call283;
        CallSite cs = (CallSite) MH_bootstrap283 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap283 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper283 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call283 ().invokeExact(o1, o2, o3); }

    static Object bootstrap283 (Object l, Object n, Object t) throws Throwable { return _mh[ 283 ].invokeExact(l, n, t); }

    // 284
    private static MethodType MT_bootstrap284 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap284 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap284", MT_bootstrap284 ());
    }

    private static MethodHandle INDY_call284;
    private static MethodHandle INDY_call284 () throws Throwable {
        if (INDY_call284 != null) return INDY_call284;
        CallSite cs = (CallSite) MH_bootstrap284 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap284 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper284 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call284 ().invokeExact(o1, o2, o3); }

    static Object bootstrap284 (Object l, Object n, Object t) throws Throwable { return _mh[ 284 ].invokeExact(l, n, t); }

    // 285
    private static MethodType MT_bootstrap285 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap285 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap285", MT_bootstrap285 ());
    }

    private static MethodHandle INDY_call285;
    private static MethodHandle INDY_call285 () throws Throwable {
        if (INDY_call285 != null) return INDY_call285;
        CallSite cs = (CallSite) MH_bootstrap285 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap285 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper285 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call285 ().invokeExact(o1, o2, o3); }

    static Object bootstrap285 (Object l, Object n, Object t) throws Throwable { return _mh[ 285 ].invokeExact(l, n, t); }

    // 286
    private static MethodType MT_bootstrap286 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap286 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap286", MT_bootstrap286 ());
    }

    private static MethodHandle INDY_call286;
    private static MethodHandle INDY_call286 () throws Throwable {
        if (INDY_call286 != null) return INDY_call286;
        CallSite cs = (CallSite) MH_bootstrap286 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap286 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper286 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call286 ().invokeExact(o1, o2, o3); }

    static Object bootstrap286 (Object l, Object n, Object t) throws Throwable { return _mh[ 286 ].invokeExact(l, n, t); }

    // 287
    private static MethodType MT_bootstrap287 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap287 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap287", MT_bootstrap287 ());
    }

    private static MethodHandle INDY_call287;
    private static MethodHandle INDY_call287 () throws Throwable {
        if (INDY_call287 != null) return INDY_call287;
        CallSite cs = (CallSite) MH_bootstrap287 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap287 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper287 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call287 ().invokeExact(o1, o2, o3); }

    static Object bootstrap287 (Object l, Object n, Object t) throws Throwable { return _mh[ 287 ].invokeExact(l, n, t); }

    // 288
    private static MethodType MT_bootstrap288 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap288 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap288", MT_bootstrap288 ());
    }

    private static MethodHandle INDY_call288;
    private static MethodHandle INDY_call288 () throws Throwable {
        if (INDY_call288 != null) return INDY_call288;
        CallSite cs = (CallSite) MH_bootstrap288 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap288 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper288 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call288 ().invokeExact(o1, o2, o3); }

    static Object bootstrap288 (Object l, Object n, Object t) throws Throwable { return _mh[ 288 ].invokeExact(l, n, t); }

    // 289
    private static MethodType MT_bootstrap289 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap289 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap289", MT_bootstrap289 ());
    }

    private static MethodHandle INDY_call289;
    private static MethodHandle INDY_call289 () throws Throwable {
        if (INDY_call289 != null) return INDY_call289;
        CallSite cs = (CallSite) MH_bootstrap289 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap289 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper289 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call289 ().invokeExact(o1, o2, o3); }

    static Object bootstrap289 (Object l, Object n, Object t) throws Throwable { return _mh[ 289 ].invokeExact(l, n, t); }

    // 290
    private static MethodType MT_bootstrap290 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap290 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap290", MT_bootstrap290 ());
    }

    private static MethodHandle INDY_call290;
    private static MethodHandle INDY_call290 () throws Throwable {
        if (INDY_call290 != null) return INDY_call290;
        CallSite cs = (CallSite) MH_bootstrap290 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap290 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper290 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call290 ().invokeExact(o1, o2, o3); }

    static Object bootstrap290 (Object l, Object n, Object t) throws Throwable { return _mh[ 290 ].invokeExact(l, n, t); }

    // 291
    private static MethodType MT_bootstrap291 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap291 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap291", MT_bootstrap291 ());
    }

    private static MethodHandle INDY_call291;
    private static MethodHandle INDY_call291 () throws Throwable {
        if (INDY_call291 != null) return INDY_call291;
        CallSite cs = (CallSite) MH_bootstrap291 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap291 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper291 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call291 ().invokeExact(o1, o2, o3); }

    static Object bootstrap291 (Object l, Object n, Object t) throws Throwable { return _mh[ 291 ].invokeExact(l, n, t); }

    // 292
    private static MethodType MT_bootstrap292 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap292 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap292", MT_bootstrap292 ());
    }

    private static MethodHandle INDY_call292;
    private static MethodHandle INDY_call292 () throws Throwable {
        if (INDY_call292 != null) return INDY_call292;
        CallSite cs = (CallSite) MH_bootstrap292 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap292 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper292 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call292 ().invokeExact(o1, o2, o3); }

    static Object bootstrap292 (Object l, Object n, Object t) throws Throwable { return _mh[ 292 ].invokeExact(l, n, t); }

    // 293
    private static MethodType MT_bootstrap293 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap293 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap293", MT_bootstrap293 ());
    }

    private static MethodHandle INDY_call293;
    private static MethodHandle INDY_call293 () throws Throwable {
        if (INDY_call293 != null) return INDY_call293;
        CallSite cs = (CallSite) MH_bootstrap293 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap293 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper293 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call293 ().invokeExact(o1, o2, o3); }

    static Object bootstrap293 (Object l, Object n, Object t) throws Throwable { return _mh[ 293 ].invokeExact(l, n, t); }

    // 294
    private static MethodType MT_bootstrap294 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap294 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap294", MT_bootstrap294 ());
    }

    private static MethodHandle INDY_call294;
    private static MethodHandle INDY_call294 () throws Throwable {
        if (INDY_call294 != null) return INDY_call294;
        CallSite cs = (CallSite) MH_bootstrap294 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap294 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper294 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call294 ().invokeExact(o1, o2, o3); }

    static Object bootstrap294 (Object l, Object n, Object t) throws Throwable { return _mh[ 294 ].invokeExact(l, n, t); }

    // 295
    private static MethodType MT_bootstrap295 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap295 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap295", MT_bootstrap295 ());
    }

    private static MethodHandle INDY_call295;
    private static MethodHandle INDY_call295 () throws Throwable {
        if (INDY_call295 != null) return INDY_call295;
        CallSite cs = (CallSite) MH_bootstrap295 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap295 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper295 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call295 ().invokeExact(o1, o2, o3); }

    static Object bootstrap295 (Object l, Object n, Object t) throws Throwable { return _mh[ 295 ].invokeExact(l, n, t); }

    // 296
    private static MethodType MT_bootstrap296 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap296 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap296", MT_bootstrap296 ());
    }

    private static MethodHandle INDY_call296;
    private static MethodHandle INDY_call296 () throws Throwable {
        if (INDY_call296 != null) return INDY_call296;
        CallSite cs = (CallSite) MH_bootstrap296 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap296 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper296 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call296 ().invokeExact(o1, o2, o3); }

    static Object bootstrap296 (Object l, Object n, Object t) throws Throwable { return _mh[ 296 ].invokeExact(l, n, t); }

    // 297
    private static MethodType MT_bootstrap297 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap297 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap297", MT_bootstrap297 ());
    }

    private static MethodHandle INDY_call297;
    private static MethodHandle INDY_call297 () throws Throwable {
        if (INDY_call297 != null) return INDY_call297;
        CallSite cs = (CallSite) MH_bootstrap297 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap297 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper297 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call297 ().invokeExact(o1, o2, o3); }

    static Object bootstrap297 (Object l, Object n, Object t) throws Throwable { return _mh[ 297 ].invokeExact(l, n, t); }

    // 298
    private static MethodType MT_bootstrap298 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap298 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap298", MT_bootstrap298 ());
    }

    private static MethodHandle INDY_call298;
    private static MethodHandle INDY_call298 () throws Throwable {
        if (INDY_call298 != null) return INDY_call298;
        CallSite cs = (CallSite) MH_bootstrap298 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap298 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper298 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call298 ().invokeExact(o1, o2, o3); }

    static Object bootstrap298 (Object l, Object n, Object t) throws Throwable { return _mh[ 298 ].invokeExact(l, n, t); }

    // 299
    private static MethodType MT_bootstrap299 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap299 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap299", MT_bootstrap299 ());
    }

    private static MethodHandle INDY_call299;
    private static MethodHandle INDY_call299 () throws Throwable {
        if (INDY_call299 != null) return INDY_call299;
        CallSite cs = (CallSite) MH_bootstrap299 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap299 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper299 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call299 ().invokeExact(o1, o2, o3); }

    static Object bootstrap299 (Object l, Object n, Object t) throws Throwable { return _mh[ 299 ].invokeExact(l, n, t); }

    // 300
    private static MethodType MT_bootstrap300 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap300 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap300", MT_bootstrap300 ());
    }

    private static MethodHandle INDY_call300;
    private static MethodHandle INDY_call300 () throws Throwable {
        if (INDY_call300 != null) return INDY_call300;
        CallSite cs = (CallSite) MH_bootstrap300 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap300 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper300 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call300 ().invokeExact(o1, o2, o3); }

    static Object bootstrap300 (Object l, Object n, Object t) throws Throwable { return _mh[ 300 ].invokeExact(l, n, t); }

    // 301
    private static MethodType MT_bootstrap301 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap301 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap301", MT_bootstrap301 ());
    }

    private static MethodHandle INDY_call301;
    private static MethodHandle INDY_call301 () throws Throwable {
        if (INDY_call301 != null) return INDY_call301;
        CallSite cs = (CallSite) MH_bootstrap301 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap301 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper301 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call301 ().invokeExact(o1, o2, o3); }

    static Object bootstrap301 (Object l, Object n, Object t) throws Throwable { return _mh[ 301 ].invokeExact(l, n, t); }

    // 302
    private static MethodType MT_bootstrap302 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap302 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap302", MT_bootstrap302 ());
    }

    private static MethodHandle INDY_call302;
    private static MethodHandle INDY_call302 () throws Throwable {
        if (INDY_call302 != null) return INDY_call302;
        CallSite cs = (CallSite) MH_bootstrap302 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap302 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper302 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call302 ().invokeExact(o1, o2, o3); }

    static Object bootstrap302 (Object l, Object n, Object t) throws Throwable { return _mh[ 302 ].invokeExact(l, n, t); }

    // 303
    private static MethodType MT_bootstrap303 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap303 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap303", MT_bootstrap303 ());
    }

    private static MethodHandle INDY_call303;
    private static MethodHandle INDY_call303 () throws Throwable {
        if (INDY_call303 != null) return INDY_call303;
        CallSite cs = (CallSite) MH_bootstrap303 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap303 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper303 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call303 ().invokeExact(o1, o2, o3); }

    static Object bootstrap303 (Object l, Object n, Object t) throws Throwable { return _mh[ 303 ].invokeExact(l, n, t); }

    // 304
    private static MethodType MT_bootstrap304 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap304 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap304", MT_bootstrap304 ());
    }

    private static MethodHandle INDY_call304;
    private static MethodHandle INDY_call304 () throws Throwable {
        if (INDY_call304 != null) return INDY_call304;
        CallSite cs = (CallSite) MH_bootstrap304 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap304 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper304 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call304 ().invokeExact(o1, o2, o3); }

    static Object bootstrap304 (Object l, Object n, Object t) throws Throwable { return _mh[ 304 ].invokeExact(l, n, t); }

    // 305
    private static MethodType MT_bootstrap305 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap305 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap305", MT_bootstrap305 ());
    }

    private static MethodHandle INDY_call305;
    private static MethodHandle INDY_call305 () throws Throwable {
        if (INDY_call305 != null) return INDY_call305;
        CallSite cs = (CallSite) MH_bootstrap305 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap305 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper305 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call305 ().invokeExact(o1, o2, o3); }

    static Object bootstrap305 (Object l, Object n, Object t) throws Throwable { return _mh[ 305 ].invokeExact(l, n, t); }

    // 306
    private static MethodType MT_bootstrap306 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap306 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap306", MT_bootstrap306 ());
    }

    private static MethodHandle INDY_call306;
    private static MethodHandle INDY_call306 () throws Throwable {
        if (INDY_call306 != null) return INDY_call306;
        CallSite cs = (CallSite) MH_bootstrap306 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap306 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper306 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call306 ().invokeExact(o1, o2, o3); }

    static Object bootstrap306 (Object l, Object n, Object t) throws Throwable { return _mh[ 306 ].invokeExact(l, n, t); }

    // 307
    private static MethodType MT_bootstrap307 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap307 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap307", MT_bootstrap307 ());
    }

    private static MethodHandle INDY_call307;
    private static MethodHandle INDY_call307 () throws Throwable {
        if (INDY_call307 != null) return INDY_call307;
        CallSite cs = (CallSite) MH_bootstrap307 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap307 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper307 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call307 ().invokeExact(o1, o2, o3); }

    static Object bootstrap307 (Object l, Object n, Object t) throws Throwable { return _mh[ 307 ].invokeExact(l, n, t); }

    // 308
    private static MethodType MT_bootstrap308 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap308 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap308", MT_bootstrap308 ());
    }

    private static MethodHandle INDY_call308;
    private static MethodHandle INDY_call308 () throws Throwable {
        if (INDY_call308 != null) return INDY_call308;
        CallSite cs = (CallSite) MH_bootstrap308 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap308 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper308 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call308 ().invokeExact(o1, o2, o3); }

    static Object bootstrap308 (Object l, Object n, Object t) throws Throwable { return _mh[ 308 ].invokeExact(l, n, t); }

    // 309
    private static MethodType MT_bootstrap309 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap309 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap309", MT_bootstrap309 ());
    }

    private static MethodHandle INDY_call309;
    private static MethodHandle INDY_call309 () throws Throwable {
        if (INDY_call309 != null) return INDY_call309;
        CallSite cs = (CallSite) MH_bootstrap309 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap309 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper309 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call309 ().invokeExact(o1, o2, o3); }

    static Object bootstrap309 (Object l, Object n, Object t) throws Throwable { return _mh[ 309 ].invokeExact(l, n, t); }

    // 310
    private static MethodType MT_bootstrap310 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap310 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap310", MT_bootstrap310 ());
    }

    private static MethodHandle INDY_call310;
    private static MethodHandle INDY_call310 () throws Throwable {
        if (INDY_call310 != null) return INDY_call310;
        CallSite cs = (CallSite) MH_bootstrap310 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap310 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper310 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call310 ().invokeExact(o1, o2, o3); }

    static Object bootstrap310 (Object l, Object n, Object t) throws Throwable { return _mh[ 310 ].invokeExact(l, n, t); }

    // 311
    private static MethodType MT_bootstrap311 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap311 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap311", MT_bootstrap311 ());
    }

    private static MethodHandle INDY_call311;
    private static MethodHandle INDY_call311 () throws Throwable {
        if (INDY_call311 != null) return INDY_call311;
        CallSite cs = (CallSite) MH_bootstrap311 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap311 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper311 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call311 ().invokeExact(o1, o2, o3); }

    static Object bootstrap311 (Object l, Object n, Object t) throws Throwable { return _mh[ 311 ].invokeExact(l, n, t); }

    // 312
    private static MethodType MT_bootstrap312 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap312 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap312", MT_bootstrap312 ());
    }

    private static MethodHandle INDY_call312;
    private static MethodHandle INDY_call312 () throws Throwable {
        if (INDY_call312 != null) return INDY_call312;
        CallSite cs = (CallSite) MH_bootstrap312 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap312 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper312 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call312 ().invokeExact(o1, o2, o3); }

    static Object bootstrap312 (Object l, Object n, Object t) throws Throwable { return _mh[ 312 ].invokeExact(l, n, t); }

    // 313
    private static MethodType MT_bootstrap313 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap313 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap313", MT_bootstrap313 ());
    }

    private static MethodHandle INDY_call313;
    private static MethodHandle INDY_call313 () throws Throwable {
        if (INDY_call313 != null) return INDY_call313;
        CallSite cs = (CallSite) MH_bootstrap313 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap313 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper313 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call313 ().invokeExact(o1, o2, o3); }

    static Object bootstrap313 (Object l, Object n, Object t) throws Throwable { return _mh[ 313 ].invokeExact(l, n, t); }

    // 314
    private static MethodType MT_bootstrap314 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap314 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap314", MT_bootstrap314 ());
    }

    private static MethodHandle INDY_call314;
    private static MethodHandle INDY_call314 () throws Throwable {
        if (INDY_call314 != null) return INDY_call314;
        CallSite cs = (CallSite) MH_bootstrap314 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap314 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper314 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call314 ().invokeExact(o1, o2, o3); }

    static Object bootstrap314 (Object l, Object n, Object t) throws Throwable { return _mh[ 314 ].invokeExact(l, n, t); }

    // 315
    private static MethodType MT_bootstrap315 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap315 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap315", MT_bootstrap315 ());
    }

    private static MethodHandle INDY_call315;
    private static MethodHandle INDY_call315 () throws Throwable {
        if (INDY_call315 != null) return INDY_call315;
        CallSite cs = (CallSite) MH_bootstrap315 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap315 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper315 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call315 ().invokeExact(o1, o2, o3); }

    static Object bootstrap315 (Object l, Object n, Object t) throws Throwable { return _mh[ 315 ].invokeExact(l, n, t); }

    // 316
    private static MethodType MT_bootstrap316 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap316 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap316", MT_bootstrap316 ());
    }

    private static MethodHandle INDY_call316;
    private static MethodHandle INDY_call316 () throws Throwable {
        if (INDY_call316 != null) return INDY_call316;
        CallSite cs = (CallSite) MH_bootstrap316 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap316 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper316 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call316 ().invokeExact(o1, o2, o3); }

    static Object bootstrap316 (Object l, Object n, Object t) throws Throwable { return _mh[ 316 ].invokeExact(l, n, t); }

    // 317
    private static MethodType MT_bootstrap317 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap317 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap317", MT_bootstrap317 ());
    }

    private static MethodHandle INDY_call317;
    private static MethodHandle INDY_call317 () throws Throwable {
        if (INDY_call317 != null) return INDY_call317;
        CallSite cs = (CallSite) MH_bootstrap317 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap317 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper317 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call317 ().invokeExact(o1, o2, o3); }

    static Object bootstrap317 (Object l, Object n, Object t) throws Throwable { return _mh[ 317 ].invokeExact(l, n, t); }

    // 318
    private static MethodType MT_bootstrap318 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap318 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap318", MT_bootstrap318 ());
    }

    private static MethodHandle INDY_call318;
    private static MethodHandle INDY_call318 () throws Throwable {
        if (INDY_call318 != null) return INDY_call318;
        CallSite cs = (CallSite) MH_bootstrap318 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap318 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper318 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call318 ().invokeExact(o1, o2, o3); }

    static Object bootstrap318 (Object l, Object n, Object t) throws Throwable { return _mh[ 318 ].invokeExact(l, n, t); }

    // 319
    private static MethodType MT_bootstrap319 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap319 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap319", MT_bootstrap319 ());
    }

    private static MethodHandle INDY_call319;
    private static MethodHandle INDY_call319 () throws Throwable {
        if (INDY_call319 != null) return INDY_call319;
        CallSite cs = (CallSite) MH_bootstrap319 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap319 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper319 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call319 ().invokeExact(o1, o2, o3); }

    static Object bootstrap319 (Object l, Object n, Object t) throws Throwable { return _mh[ 319 ].invokeExact(l, n, t); }

    // 320
    private static MethodType MT_bootstrap320 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap320 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap320", MT_bootstrap320 ());
    }

    private static MethodHandle INDY_call320;
    private static MethodHandle INDY_call320 () throws Throwable {
        if (INDY_call320 != null) return INDY_call320;
        CallSite cs = (CallSite) MH_bootstrap320 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap320 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper320 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call320 ().invokeExact(o1, o2, o3); }

    static Object bootstrap320 (Object l, Object n, Object t) throws Throwable { return _mh[ 320 ].invokeExact(l, n, t); }

    // 321
    private static MethodType MT_bootstrap321 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap321 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap321", MT_bootstrap321 ());
    }

    private static MethodHandle INDY_call321;
    private static MethodHandle INDY_call321 () throws Throwable {
        if (INDY_call321 != null) return INDY_call321;
        CallSite cs = (CallSite) MH_bootstrap321 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap321 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper321 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call321 ().invokeExact(o1, o2, o3); }

    static Object bootstrap321 (Object l, Object n, Object t) throws Throwable { return _mh[ 321 ].invokeExact(l, n, t); }

    // 322
    private static MethodType MT_bootstrap322 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap322 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap322", MT_bootstrap322 ());
    }

    private static MethodHandle INDY_call322;
    private static MethodHandle INDY_call322 () throws Throwable {
        if (INDY_call322 != null) return INDY_call322;
        CallSite cs = (CallSite) MH_bootstrap322 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap322 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper322 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call322 ().invokeExact(o1, o2, o3); }

    static Object bootstrap322 (Object l, Object n, Object t) throws Throwable { return _mh[ 322 ].invokeExact(l, n, t); }

    // 323
    private static MethodType MT_bootstrap323 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap323 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap323", MT_bootstrap323 ());
    }

    private static MethodHandle INDY_call323;
    private static MethodHandle INDY_call323 () throws Throwable {
        if (INDY_call323 != null) return INDY_call323;
        CallSite cs = (CallSite) MH_bootstrap323 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap323 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper323 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call323 ().invokeExact(o1, o2, o3); }

    static Object bootstrap323 (Object l, Object n, Object t) throws Throwable { return _mh[ 323 ].invokeExact(l, n, t); }

    // 324
    private static MethodType MT_bootstrap324 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap324 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap324", MT_bootstrap324 ());
    }

    private static MethodHandle INDY_call324;
    private static MethodHandle INDY_call324 () throws Throwable {
        if (INDY_call324 != null) return INDY_call324;
        CallSite cs = (CallSite) MH_bootstrap324 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap324 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper324 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call324 ().invokeExact(o1, o2, o3); }

    static Object bootstrap324 (Object l, Object n, Object t) throws Throwable { return _mh[ 324 ].invokeExact(l, n, t); }

    // 325
    private static MethodType MT_bootstrap325 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap325 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap325", MT_bootstrap325 ());
    }

    private static MethodHandle INDY_call325;
    private static MethodHandle INDY_call325 () throws Throwable {
        if (INDY_call325 != null) return INDY_call325;
        CallSite cs = (CallSite) MH_bootstrap325 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap325 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper325 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call325 ().invokeExact(o1, o2, o3); }

    static Object bootstrap325 (Object l, Object n, Object t) throws Throwable { return _mh[ 325 ].invokeExact(l, n, t); }

    // 326
    private static MethodType MT_bootstrap326 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap326 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap326", MT_bootstrap326 ());
    }

    private static MethodHandle INDY_call326;
    private static MethodHandle INDY_call326 () throws Throwable {
        if (INDY_call326 != null) return INDY_call326;
        CallSite cs = (CallSite) MH_bootstrap326 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap326 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper326 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call326 ().invokeExact(o1, o2, o3); }

    static Object bootstrap326 (Object l, Object n, Object t) throws Throwable { return _mh[ 326 ].invokeExact(l, n, t); }

    // 327
    private static MethodType MT_bootstrap327 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap327 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap327", MT_bootstrap327 ());
    }

    private static MethodHandle INDY_call327;
    private static MethodHandle INDY_call327 () throws Throwable {
        if (INDY_call327 != null) return INDY_call327;
        CallSite cs = (CallSite) MH_bootstrap327 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap327 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper327 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call327 ().invokeExact(o1, o2, o3); }

    static Object bootstrap327 (Object l, Object n, Object t) throws Throwable { return _mh[ 327 ].invokeExact(l, n, t); }

    // 328
    private static MethodType MT_bootstrap328 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap328 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap328", MT_bootstrap328 ());
    }

    private static MethodHandle INDY_call328;
    private static MethodHandle INDY_call328 () throws Throwable {
        if (INDY_call328 != null) return INDY_call328;
        CallSite cs = (CallSite) MH_bootstrap328 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap328 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper328 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call328 ().invokeExact(o1, o2, o3); }

    static Object bootstrap328 (Object l, Object n, Object t) throws Throwable { return _mh[ 328 ].invokeExact(l, n, t); }

    // 329
    private static MethodType MT_bootstrap329 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap329 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap329", MT_bootstrap329 ());
    }

    private static MethodHandle INDY_call329;
    private static MethodHandle INDY_call329 () throws Throwable {
        if (INDY_call329 != null) return INDY_call329;
        CallSite cs = (CallSite) MH_bootstrap329 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap329 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper329 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call329 ().invokeExact(o1, o2, o3); }

    static Object bootstrap329 (Object l, Object n, Object t) throws Throwable { return _mh[ 329 ].invokeExact(l, n, t); }

    // 330
    private static MethodType MT_bootstrap330 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap330 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap330", MT_bootstrap330 ());
    }

    private static MethodHandle INDY_call330;
    private static MethodHandle INDY_call330 () throws Throwable {
        if (INDY_call330 != null) return INDY_call330;
        CallSite cs = (CallSite) MH_bootstrap330 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap330 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper330 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call330 ().invokeExact(o1, o2, o3); }

    static Object bootstrap330 (Object l, Object n, Object t) throws Throwable { return _mh[ 330 ].invokeExact(l, n, t); }

    // 331
    private static MethodType MT_bootstrap331 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap331 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap331", MT_bootstrap331 ());
    }

    private static MethodHandle INDY_call331;
    private static MethodHandle INDY_call331 () throws Throwable {
        if (INDY_call331 != null) return INDY_call331;
        CallSite cs = (CallSite) MH_bootstrap331 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap331 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper331 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call331 ().invokeExact(o1, o2, o3); }

    static Object bootstrap331 (Object l, Object n, Object t) throws Throwable { return _mh[ 331 ].invokeExact(l, n, t); }

    // 332
    private static MethodType MT_bootstrap332 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap332 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap332", MT_bootstrap332 ());
    }

    private static MethodHandle INDY_call332;
    private static MethodHandle INDY_call332 () throws Throwable {
        if (INDY_call332 != null) return INDY_call332;
        CallSite cs = (CallSite) MH_bootstrap332 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap332 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper332 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call332 ().invokeExact(o1, o2, o3); }

    static Object bootstrap332 (Object l, Object n, Object t) throws Throwable { return _mh[ 332 ].invokeExact(l, n, t); }

    // 333
    private static MethodType MT_bootstrap333 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap333 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap333", MT_bootstrap333 ());
    }

    private static MethodHandle INDY_call333;
    private static MethodHandle INDY_call333 () throws Throwable {
        if (INDY_call333 != null) return INDY_call333;
        CallSite cs = (CallSite) MH_bootstrap333 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap333 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper333 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call333 ().invokeExact(o1, o2, o3); }

    static Object bootstrap333 (Object l, Object n, Object t) throws Throwable { return _mh[ 333 ].invokeExact(l, n, t); }

    // 334
    private static MethodType MT_bootstrap334 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap334 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap334", MT_bootstrap334 ());
    }

    private static MethodHandle INDY_call334;
    private static MethodHandle INDY_call334 () throws Throwable {
        if (INDY_call334 != null) return INDY_call334;
        CallSite cs = (CallSite) MH_bootstrap334 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap334 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper334 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call334 ().invokeExact(o1, o2, o3); }

    static Object bootstrap334 (Object l, Object n, Object t) throws Throwable { return _mh[ 334 ].invokeExact(l, n, t); }

    // 335
    private static MethodType MT_bootstrap335 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap335 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap335", MT_bootstrap335 ());
    }

    private static MethodHandle INDY_call335;
    private static MethodHandle INDY_call335 () throws Throwable {
        if (INDY_call335 != null) return INDY_call335;
        CallSite cs = (CallSite) MH_bootstrap335 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap335 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper335 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call335 ().invokeExact(o1, o2, o3); }

    static Object bootstrap335 (Object l, Object n, Object t) throws Throwable { return _mh[ 335 ].invokeExact(l, n, t); }

    // 336
    private static MethodType MT_bootstrap336 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap336 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap336", MT_bootstrap336 ());
    }

    private static MethodHandle INDY_call336;
    private static MethodHandle INDY_call336 () throws Throwable {
        if (INDY_call336 != null) return INDY_call336;
        CallSite cs = (CallSite) MH_bootstrap336 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap336 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper336 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call336 ().invokeExact(o1, o2, o3); }

    static Object bootstrap336 (Object l, Object n, Object t) throws Throwable { return _mh[ 336 ].invokeExact(l, n, t); }

    // 337
    private static MethodType MT_bootstrap337 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap337 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap337", MT_bootstrap337 ());
    }

    private static MethodHandle INDY_call337;
    private static MethodHandle INDY_call337 () throws Throwable {
        if (INDY_call337 != null) return INDY_call337;
        CallSite cs = (CallSite) MH_bootstrap337 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap337 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper337 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call337 ().invokeExact(o1, o2, o3); }

    static Object bootstrap337 (Object l, Object n, Object t) throws Throwable { return _mh[ 337 ].invokeExact(l, n, t); }

    // 338
    private static MethodType MT_bootstrap338 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap338 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap338", MT_bootstrap338 ());
    }

    private static MethodHandle INDY_call338;
    private static MethodHandle INDY_call338 () throws Throwable {
        if (INDY_call338 != null) return INDY_call338;
        CallSite cs = (CallSite) MH_bootstrap338 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap338 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper338 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call338 ().invokeExact(o1, o2, o3); }

    static Object bootstrap338 (Object l, Object n, Object t) throws Throwable { return _mh[ 338 ].invokeExact(l, n, t); }

    // 339
    private static MethodType MT_bootstrap339 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap339 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap339", MT_bootstrap339 ());
    }

    private static MethodHandle INDY_call339;
    private static MethodHandle INDY_call339 () throws Throwable {
        if (INDY_call339 != null) return INDY_call339;
        CallSite cs = (CallSite) MH_bootstrap339 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap339 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper339 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call339 ().invokeExact(o1, o2, o3); }

    static Object bootstrap339 (Object l, Object n, Object t) throws Throwable { return _mh[ 339 ].invokeExact(l, n, t); }

    // 340
    private static MethodType MT_bootstrap340 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap340 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap340", MT_bootstrap340 ());
    }

    private static MethodHandle INDY_call340;
    private static MethodHandle INDY_call340 () throws Throwable {
        if (INDY_call340 != null) return INDY_call340;
        CallSite cs = (CallSite) MH_bootstrap340 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap340 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper340 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call340 ().invokeExact(o1, o2, o3); }

    static Object bootstrap340 (Object l, Object n, Object t) throws Throwable { return _mh[ 340 ].invokeExact(l, n, t); }

    // 341
    private static MethodType MT_bootstrap341 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap341 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap341", MT_bootstrap341 ());
    }

    private static MethodHandle INDY_call341;
    private static MethodHandle INDY_call341 () throws Throwable {
        if (INDY_call341 != null) return INDY_call341;
        CallSite cs = (CallSite) MH_bootstrap341 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap341 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper341 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call341 ().invokeExact(o1, o2, o3); }

    static Object bootstrap341 (Object l, Object n, Object t) throws Throwable { return _mh[ 341 ].invokeExact(l, n, t); }

    // 342
    private static MethodType MT_bootstrap342 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap342 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap342", MT_bootstrap342 ());
    }

    private static MethodHandle INDY_call342;
    private static MethodHandle INDY_call342 () throws Throwable {
        if (INDY_call342 != null) return INDY_call342;
        CallSite cs = (CallSite) MH_bootstrap342 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap342 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper342 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call342 ().invokeExact(o1, o2, o3); }

    static Object bootstrap342 (Object l, Object n, Object t) throws Throwable { return _mh[ 342 ].invokeExact(l, n, t); }

    // 343
    private static MethodType MT_bootstrap343 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap343 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap343", MT_bootstrap343 ());
    }

    private static MethodHandle INDY_call343;
    private static MethodHandle INDY_call343 () throws Throwable {
        if (INDY_call343 != null) return INDY_call343;
        CallSite cs = (CallSite) MH_bootstrap343 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap343 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper343 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call343 ().invokeExact(o1, o2, o3); }

    static Object bootstrap343 (Object l, Object n, Object t) throws Throwable { return _mh[ 343 ].invokeExact(l, n, t); }

    // 344
    private static MethodType MT_bootstrap344 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap344 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap344", MT_bootstrap344 ());
    }

    private static MethodHandle INDY_call344;
    private static MethodHandle INDY_call344 () throws Throwable {
        if (INDY_call344 != null) return INDY_call344;
        CallSite cs = (CallSite) MH_bootstrap344 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap344 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper344 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call344 ().invokeExact(o1, o2, o3); }

    static Object bootstrap344 (Object l, Object n, Object t) throws Throwable { return _mh[ 344 ].invokeExact(l, n, t); }

    // 345
    private static MethodType MT_bootstrap345 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap345 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap345", MT_bootstrap345 ());
    }

    private static MethodHandle INDY_call345;
    private static MethodHandle INDY_call345 () throws Throwable {
        if (INDY_call345 != null) return INDY_call345;
        CallSite cs = (CallSite) MH_bootstrap345 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap345 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper345 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call345 ().invokeExact(o1, o2, o3); }

    static Object bootstrap345 (Object l, Object n, Object t) throws Throwable { return _mh[ 345 ].invokeExact(l, n, t); }

    // 346
    private static MethodType MT_bootstrap346 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap346 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap346", MT_bootstrap346 ());
    }

    private static MethodHandle INDY_call346;
    private static MethodHandle INDY_call346 () throws Throwable {
        if (INDY_call346 != null) return INDY_call346;
        CallSite cs = (CallSite) MH_bootstrap346 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap346 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper346 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call346 ().invokeExact(o1, o2, o3); }

    static Object bootstrap346 (Object l, Object n, Object t) throws Throwable { return _mh[ 346 ].invokeExact(l, n, t); }

    // 347
    private static MethodType MT_bootstrap347 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap347 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap347", MT_bootstrap347 ());
    }

    private static MethodHandle INDY_call347;
    private static MethodHandle INDY_call347 () throws Throwable {
        if (INDY_call347 != null) return INDY_call347;
        CallSite cs = (CallSite) MH_bootstrap347 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap347 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper347 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call347 ().invokeExact(o1, o2, o3); }

    static Object bootstrap347 (Object l, Object n, Object t) throws Throwable { return _mh[ 347 ].invokeExact(l, n, t); }

    // 348
    private static MethodType MT_bootstrap348 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap348 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap348", MT_bootstrap348 ());
    }

    private static MethodHandle INDY_call348;
    private static MethodHandle INDY_call348 () throws Throwable {
        if (INDY_call348 != null) return INDY_call348;
        CallSite cs = (CallSite) MH_bootstrap348 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap348 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper348 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call348 ().invokeExact(o1, o2, o3); }

    static Object bootstrap348 (Object l, Object n, Object t) throws Throwable { return _mh[ 348 ].invokeExact(l, n, t); }

    // 349
    private static MethodType MT_bootstrap349 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap349 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap349", MT_bootstrap349 ());
    }

    private static MethodHandle INDY_call349;
    private static MethodHandle INDY_call349 () throws Throwable {
        if (INDY_call349 != null) return INDY_call349;
        CallSite cs = (CallSite) MH_bootstrap349 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap349 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper349 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call349 ().invokeExact(o1, o2, o3); }

    static Object bootstrap349 (Object l, Object n, Object t) throws Throwable { return _mh[ 349 ].invokeExact(l, n, t); }

    // 350
    private static MethodType MT_bootstrap350 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap350 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap350", MT_bootstrap350 ());
    }

    private static MethodHandle INDY_call350;
    private static MethodHandle INDY_call350 () throws Throwable {
        if (INDY_call350 != null) return INDY_call350;
        CallSite cs = (CallSite) MH_bootstrap350 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap350 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper350 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call350 ().invokeExact(o1, o2, o3); }

    static Object bootstrap350 (Object l, Object n, Object t) throws Throwable { return _mh[ 350 ].invokeExact(l, n, t); }

    // 351
    private static MethodType MT_bootstrap351 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap351 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap351", MT_bootstrap351 ());
    }

    private static MethodHandle INDY_call351;
    private static MethodHandle INDY_call351 () throws Throwable {
        if (INDY_call351 != null) return INDY_call351;
        CallSite cs = (CallSite) MH_bootstrap351 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap351 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper351 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call351 ().invokeExact(o1, o2, o3); }

    static Object bootstrap351 (Object l, Object n, Object t) throws Throwable { return _mh[ 351 ].invokeExact(l, n, t); }

    // 352
    private static MethodType MT_bootstrap352 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap352 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap352", MT_bootstrap352 ());
    }

    private static MethodHandle INDY_call352;
    private static MethodHandle INDY_call352 () throws Throwable {
        if (INDY_call352 != null) return INDY_call352;
        CallSite cs = (CallSite) MH_bootstrap352 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap352 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper352 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call352 ().invokeExact(o1, o2, o3); }

    static Object bootstrap352 (Object l, Object n, Object t) throws Throwable { return _mh[ 352 ].invokeExact(l, n, t); }

    // 353
    private static MethodType MT_bootstrap353 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap353 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap353", MT_bootstrap353 ());
    }

    private static MethodHandle INDY_call353;
    private static MethodHandle INDY_call353 () throws Throwable {
        if (INDY_call353 != null) return INDY_call353;
        CallSite cs = (CallSite) MH_bootstrap353 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap353 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper353 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call353 ().invokeExact(o1, o2, o3); }

    static Object bootstrap353 (Object l, Object n, Object t) throws Throwable { return _mh[ 353 ].invokeExact(l, n, t); }

    // 354
    private static MethodType MT_bootstrap354 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap354 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap354", MT_bootstrap354 ());
    }

    private static MethodHandle INDY_call354;
    private static MethodHandle INDY_call354 () throws Throwable {
        if (INDY_call354 != null) return INDY_call354;
        CallSite cs = (CallSite) MH_bootstrap354 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap354 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper354 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call354 ().invokeExact(o1, o2, o3); }

    static Object bootstrap354 (Object l, Object n, Object t) throws Throwable { return _mh[ 354 ].invokeExact(l, n, t); }

    // 355
    private static MethodType MT_bootstrap355 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap355 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap355", MT_bootstrap355 ());
    }

    private static MethodHandle INDY_call355;
    private static MethodHandle INDY_call355 () throws Throwable {
        if (INDY_call355 != null) return INDY_call355;
        CallSite cs = (CallSite) MH_bootstrap355 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap355 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper355 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call355 ().invokeExact(o1, o2, o3); }

    static Object bootstrap355 (Object l, Object n, Object t) throws Throwable { return _mh[ 355 ].invokeExact(l, n, t); }

    // 356
    private static MethodType MT_bootstrap356 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap356 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap356", MT_bootstrap356 ());
    }

    private static MethodHandle INDY_call356;
    private static MethodHandle INDY_call356 () throws Throwable {
        if (INDY_call356 != null) return INDY_call356;
        CallSite cs = (CallSite) MH_bootstrap356 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap356 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper356 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call356 ().invokeExact(o1, o2, o3); }

    static Object bootstrap356 (Object l, Object n, Object t) throws Throwable { return _mh[ 356 ].invokeExact(l, n, t); }

    // 357
    private static MethodType MT_bootstrap357 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap357 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap357", MT_bootstrap357 ());
    }

    private static MethodHandle INDY_call357;
    private static MethodHandle INDY_call357 () throws Throwable {
        if (INDY_call357 != null) return INDY_call357;
        CallSite cs = (CallSite) MH_bootstrap357 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap357 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper357 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call357 ().invokeExact(o1, o2, o3); }

    static Object bootstrap357 (Object l, Object n, Object t) throws Throwable { return _mh[ 357 ].invokeExact(l, n, t); }

    // 358
    private static MethodType MT_bootstrap358 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap358 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap358", MT_bootstrap358 ());
    }

    private static MethodHandle INDY_call358;
    private static MethodHandle INDY_call358 () throws Throwable {
        if (INDY_call358 != null) return INDY_call358;
        CallSite cs = (CallSite) MH_bootstrap358 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap358 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper358 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call358 ().invokeExact(o1, o2, o3); }

    static Object bootstrap358 (Object l, Object n, Object t) throws Throwable { return _mh[ 358 ].invokeExact(l, n, t); }

    // 359
    private static MethodType MT_bootstrap359 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap359 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap359", MT_bootstrap359 ());
    }

    private static MethodHandle INDY_call359;
    private static MethodHandle INDY_call359 () throws Throwable {
        if (INDY_call359 != null) return INDY_call359;
        CallSite cs = (CallSite) MH_bootstrap359 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap359 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper359 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call359 ().invokeExact(o1, o2, o3); }

    static Object bootstrap359 (Object l, Object n, Object t) throws Throwable { return _mh[ 359 ].invokeExact(l, n, t); }

    // 360
    private static MethodType MT_bootstrap360 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap360 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap360", MT_bootstrap360 ());
    }

    private static MethodHandle INDY_call360;
    private static MethodHandle INDY_call360 () throws Throwable {
        if (INDY_call360 != null) return INDY_call360;
        CallSite cs = (CallSite) MH_bootstrap360 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap360 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper360 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call360 ().invokeExact(o1, o2, o3); }

    static Object bootstrap360 (Object l, Object n, Object t) throws Throwable { return _mh[ 360 ].invokeExact(l, n, t); }

    // 361
    private static MethodType MT_bootstrap361 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap361 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap361", MT_bootstrap361 ());
    }

    private static MethodHandle INDY_call361;
    private static MethodHandle INDY_call361 () throws Throwable {
        if (INDY_call361 != null) return INDY_call361;
        CallSite cs = (CallSite) MH_bootstrap361 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap361 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper361 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call361 ().invokeExact(o1, o2, o3); }

    static Object bootstrap361 (Object l, Object n, Object t) throws Throwable { return _mh[ 361 ].invokeExact(l, n, t); }

    // 362
    private static MethodType MT_bootstrap362 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap362 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap362", MT_bootstrap362 ());
    }

    private static MethodHandle INDY_call362;
    private static MethodHandle INDY_call362 () throws Throwable {
        if (INDY_call362 != null) return INDY_call362;
        CallSite cs = (CallSite) MH_bootstrap362 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap362 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper362 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call362 ().invokeExact(o1, o2, o3); }

    static Object bootstrap362 (Object l, Object n, Object t) throws Throwable { return _mh[ 362 ].invokeExact(l, n, t); }

    // 363
    private static MethodType MT_bootstrap363 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap363 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap363", MT_bootstrap363 ());
    }

    private static MethodHandle INDY_call363;
    private static MethodHandle INDY_call363 () throws Throwable {
        if (INDY_call363 != null) return INDY_call363;
        CallSite cs = (CallSite) MH_bootstrap363 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap363 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper363 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call363 ().invokeExact(o1, o2, o3); }

    static Object bootstrap363 (Object l, Object n, Object t) throws Throwable { return _mh[ 363 ].invokeExact(l, n, t); }

    // 364
    private static MethodType MT_bootstrap364 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap364 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap364", MT_bootstrap364 ());
    }

    private static MethodHandle INDY_call364;
    private static MethodHandle INDY_call364 () throws Throwable {
        if (INDY_call364 != null) return INDY_call364;
        CallSite cs = (CallSite) MH_bootstrap364 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap364 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper364 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call364 ().invokeExact(o1, o2, o3); }

    static Object bootstrap364 (Object l, Object n, Object t) throws Throwable { return _mh[ 364 ].invokeExact(l, n, t); }

    // 365
    private static MethodType MT_bootstrap365 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap365 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap365", MT_bootstrap365 ());
    }

    private static MethodHandle INDY_call365;
    private static MethodHandle INDY_call365 () throws Throwable {
        if (INDY_call365 != null) return INDY_call365;
        CallSite cs = (CallSite) MH_bootstrap365 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap365 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper365 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call365 ().invokeExact(o1, o2, o3); }

    static Object bootstrap365 (Object l, Object n, Object t) throws Throwable { return _mh[ 365 ].invokeExact(l, n, t); }

    // 366
    private static MethodType MT_bootstrap366 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap366 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap366", MT_bootstrap366 ());
    }

    private static MethodHandle INDY_call366;
    private static MethodHandle INDY_call366 () throws Throwable {
        if (INDY_call366 != null) return INDY_call366;
        CallSite cs = (CallSite) MH_bootstrap366 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap366 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper366 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call366 ().invokeExact(o1, o2, o3); }

    static Object bootstrap366 (Object l, Object n, Object t) throws Throwable { return _mh[ 366 ].invokeExact(l, n, t); }

    // 367
    private static MethodType MT_bootstrap367 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap367 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap367", MT_bootstrap367 ());
    }

    private static MethodHandle INDY_call367;
    private static MethodHandle INDY_call367 () throws Throwable {
        if (INDY_call367 != null) return INDY_call367;
        CallSite cs = (CallSite) MH_bootstrap367 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap367 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper367 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call367 ().invokeExact(o1, o2, o3); }

    static Object bootstrap367 (Object l, Object n, Object t) throws Throwable { return _mh[ 367 ].invokeExact(l, n, t); }

    // 368
    private static MethodType MT_bootstrap368 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap368 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap368", MT_bootstrap368 ());
    }

    private static MethodHandle INDY_call368;
    private static MethodHandle INDY_call368 () throws Throwable {
        if (INDY_call368 != null) return INDY_call368;
        CallSite cs = (CallSite) MH_bootstrap368 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap368 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper368 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call368 ().invokeExact(o1, o2, o3); }

    static Object bootstrap368 (Object l, Object n, Object t) throws Throwable { return _mh[ 368 ].invokeExact(l, n, t); }

    // 369
    private static MethodType MT_bootstrap369 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap369 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap369", MT_bootstrap369 ());
    }

    private static MethodHandle INDY_call369;
    private static MethodHandle INDY_call369 () throws Throwable {
        if (INDY_call369 != null) return INDY_call369;
        CallSite cs = (CallSite) MH_bootstrap369 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap369 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper369 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call369 ().invokeExact(o1, o2, o3); }

    static Object bootstrap369 (Object l, Object n, Object t) throws Throwable { return _mh[ 369 ].invokeExact(l, n, t); }

    // 370
    private static MethodType MT_bootstrap370 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap370 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap370", MT_bootstrap370 ());
    }

    private static MethodHandle INDY_call370;
    private static MethodHandle INDY_call370 () throws Throwable {
        if (INDY_call370 != null) return INDY_call370;
        CallSite cs = (CallSite) MH_bootstrap370 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap370 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper370 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call370 ().invokeExact(o1, o2, o3); }

    static Object bootstrap370 (Object l, Object n, Object t) throws Throwable { return _mh[ 370 ].invokeExact(l, n, t); }

    // 371
    private static MethodType MT_bootstrap371 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap371 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap371", MT_bootstrap371 ());
    }

    private static MethodHandle INDY_call371;
    private static MethodHandle INDY_call371 () throws Throwable {
        if (INDY_call371 != null) return INDY_call371;
        CallSite cs = (CallSite) MH_bootstrap371 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap371 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper371 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call371 ().invokeExact(o1, o2, o3); }

    static Object bootstrap371 (Object l, Object n, Object t) throws Throwable { return _mh[ 371 ].invokeExact(l, n, t); }

    // 372
    private static MethodType MT_bootstrap372 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap372 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap372", MT_bootstrap372 ());
    }

    private static MethodHandle INDY_call372;
    private static MethodHandle INDY_call372 () throws Throwable {
        if (INDY_call372 != null) return INDY_call372;
        CallSite cs = (CallSite) MH_bootstrap372 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap372 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper372 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call372 ().invokeExact(o1, o2, o3); }

    static Object bootstrap372 (Object l, Object n, Object t) throws Throwable { return _mh[ 372 ].invokeExact(l, n, t); }

    // 373
    private static MethodType MT_bootstrap373 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap373 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap373", MT_bootstrap373 ());
    }

    private static MethodHandle INDY_call373;
    private static MethodHandle INDY_call373 () throws Throwable {
        if (INDY_call373 != null) return INDY_call373;
        CallSite cs = (CallSite) MH_bootstrap373 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap373 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper373 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call373 ().invokeExact(o1, o2, o3); }

    static Object bootstrap373 (Object l, Object n, Object t) throws Throwable { return _mh[ 373 ].invokeExact(l, n, t); }

    // 374
    private static MethodType MT_bootstrap374 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap374 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap374", MT_bootstrap374 ());
    }

    private static MethodHandle INDY_call374;
    private static MethodHandle INDY_call374 () throws Throwable {
        if (INDY_call374 != null) return INDY_call374;
        CallSite cs = (CallSite) MH_bootstrap374 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap374 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper374 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call374 ().invokeExact(o1, o2, o3); }

    static Object bootstrap374 (Object l, Object n, Object t) throws Throwable { return _mh[ 374 ].invokeExact(l, n, t); }

    // 375
    private static MethodType MT_bootstrap375 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap375 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap375", MT_bootstrap375 ());
    }

    private static MethodHandle INDY_call375;
    private static MethodHandle INDY_call375 () throws Throwable {
        if (INDY_call375 != null) return INDY_call375;
        CallSite cs = (CallSite) MH_bootstrap375 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap375 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper375 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call375 ().invokeExact(o1, o2, o3); }

    static Object bootstrap375 (Object l, Object n, Object t) throws Throwable { return _mh[ 375 ].invokeExact(l, n, t); }

    // 376
    private static MethodType MT_bootstrap376 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap376 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap376", MT_bootstrap376 ());
    }

    private static MethodHandle INDY_call376;
    private static MethodHandle INDY_call376 () throws Throwable {
        if (INDY_call376 != null) return INDY_call376;
        CallSite cs = (CallSite) MH_bootstrap376 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap376 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper376 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call376 ().invokeExact(o1, o2, o3); }

    static Object bootstrap376 (Object l, Object n, Object t) throws Throwable { return _mh[ 376 ].invokeExact(l, n, t); }

    // 377
    private static MethodType MT_bootstrap377 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap377 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap377", MT_bootstrap377 ());
    }

    private static MethodHandle INDY_call377;
    private static MethodHandle INDY_call377 () throws Throwable {
        if (INDY_call377 != null) return INDY_call377;
        CallSite cs = (CallSite) MH_bootstrap377 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap377 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper377 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call377 ().invokeExact(o1, o2, o3); }

    static Object bootstrap377 (Object l, Object n, Object t) throws Throwable { return _mh[ 377 ].invokeExact(l, n, t); }

    // 378
    private static MethodType MT_bootstrap378 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap378 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap378", MT_bootstrap378 ());
    }

    private static MethodHandle INDY_call378;
    private static MethodHandle INDY_call378 () throws Throwable {
        if (INDY_call378 != null) return INDY_call378;
        CallSite cs = (CallSite) MH_bootstrap378 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap378 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper378 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call378 ().invokeExact(o1, o2, o3); }

    static Object bootstrap378 (Object l, Object n, Object t) throws Throwable { return _mh[ 378 ].invokeExact(l, n, t); }

    // 379
    private static MethodType MT_bootstrap379 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap379 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap379", MT_bootstrap379 ());
    }

    private static MethodHandle INDY_call379;
    private static MethodHandle INDY_call379 () throws Throwable {
        if (INDY_call379 != null) return INDY_call379;
        CallSite cs = (CallSite) MH_bootstrap379 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap379 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper379 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call379 ().invokeExact(o1, o2, o3); }

    static Object bootstrap379 (Object l, Object n, Object t) throws Throwable { return _mh[ 379 ].invokeExact(l, n, t); }

    // 380
    private static MethodType MT_bootstrap380 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap380 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap380", MT_bootstrap380 ());
    }

    private static MethodHandle INDY_call380;
    private static MethodHandle INDY_call380 () throws Throwable {
        if (INDY_call380 != null) return INDY_call380;
        CallSite cs = (CallSite) MH_bootstrap380 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap380 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper380 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call380 ().invokeExact(o1, o2, o3); }

    static Object bootstrap380 (Object l, Object n, Object t) throws Throwable { return _mh[ 380 ].invokeExact(l, n, t); }

    // 381
    private static MethodType MT_bootstrap381 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap381 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap381", MT_bootstrap381 ());
    }

    private static MethodHandle INDY_call381;
    private static MethodHandle INDY_call381 () throws Throwable {
        if (INDY_call381 != null) return INDY_call381;
        CallSite cs = (CallSite) MH_bootstrap381 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap381 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper381 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call381 ().invokeExact(o1, o2, o3); }

    static Object bootstrap381 (Object l, Object n, Object t) throws Throwable { return _mh[ 381 ].invokeExact(l, n, t); }

    // 382
    private static MethodType MT_bootstrap382 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap382 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap382", MT_bootstrap382 ());
    }

    private static MethodHandle INDY_call382;
    private static MethodHandle INDY_call382 () throws Throwable {
        if (INDY_call382 != null) return INDY_call382;
        CallSite cs = (CallSite) MH_bootstrap382 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap382 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper382 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call382 ().invokeExact(o1, o2, o3); }

    static Object bootstrap382 (Object l, Object n, Object t) throws Throwable { return _mh[ 382 ].invokeExact(l, n, t); }

    // 383
    private static MethodType MT_bootstrap383 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap383 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap383", MT_bootstrap383 ());
    }

    private static MethodHandle INDY_call383;
    private static MethodHandle INDY_call383 () throws Throwable {
        if (INDY_call383 != null) return INDY_call383;
        CallSite cs = (CallSite) MH_bootstrap383 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap383 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper383 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call383 ().invokeExact(o1, o2, o3); }

    static Object bootstrap383 (Object l, Object n, Object t) throws Throwable { return _mh[ 383 ].invokeExact(l, n, t); }

    // 384
    private static MethodType MT_bootstrap384 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap384 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap384", MT_bootstrap384 ());
    }

    private static MethodHandle INDY_call384;
    private static MethodHandle INDY_call384 () throws Throwable {
        if (INDY_call384 != null) return INDY_call384;
        CallSite cs = (CallSite) MH_bootstrap384 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap384 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper384 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call384 ().invokeExact(o1, o2, o3); }

    static Object bootstrap384 (Object l, Object n, Object t) throws Throwable { return _mh[ 384 ].invokeExact(l, n, t); }

    // 385
    private static MethodType MT_bootstrap385 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap385 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap385", MT_bootstrap385 ());
    }

    private static MethodHandle INDY_call385;
    private static MethodHandle INDY_call385 () throws Throwable {
        if (INDY_call385 != null) return INDY_call385;
        CallSite cs = (CallSite) MH_bootstrap385 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap385 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper385 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call385 ().invokeExact(o1, o2, o3); }

    static Object bootstrap385 (Object l, Object n, Object t) throws Throwable { return _mh[ 385 ].invokeExact(l, n, t); }

    // 386
    private static MethodType MT_bootstrap386 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap386 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap386", MT_bootstrap386 ());
    }

    private static MethodHandle INDY_call386;
    private static MethodHandle INDY_call386 () throws Throwable {
        if (INDY_call386 != null) return INDY_call386;
        CallSite cs = (CallSite) MH_bootstrap386 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap386 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper386 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call386 ().invokeExact(o1, o2, o3); }

    static Object bootstrap386 (Object l, Object n, Object t) throws Throwable { return _mh[ 386 ].invokeExact(l, n, t); }

    // 387
    private static MethodType MT_bootstrap387 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap387 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap387", MT_bootstrap387 ());
    }

    private static MethodHandle INDY_call387;
    private static MethodHandle INDY_call387 () throws Throwable {
        if (INDY_call387 != null) return INDY_call387;
        CallSite cs = (CallSite) MH_bootstrap387 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap387 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper387 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call387 ().invokeExact(o1, o2, o3); }

    static Object bootstrap387 (Object l, Object n, Object t) throws Throwable { return _mh[ 387 ].invokeExact(l, n, t); }

    // 388
    private static MethodType MT_bootstrap388 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap388 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap388", MT_bootstrap388 ());
    }

    private static MethodHandle INDY_call388;
    private static MethodHandle INDY_call388 () throws Throwable {
        if (INDY_call388 != null) return INDY_call388;
        CallSite cs = (CallSite) MH_bootstrap388 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap388 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper388 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call388 ().invokeExact(o1, o2, o3); }

    static Object bootstrap388 (Object l, Object n, Object t) throws Throwable { return _mh[ 388 ].invokeExact(l, n, t); }

    // 389
    private static MethodType MT_bootstrap389 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap389 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap389", MT_bootstrap389 ());
    }

    private static MethodHandle INDY_call389;
    private static MethodHandle INDY_call389 () throws Throwable {
        if (INDY_call389 != null) return INDY_call389;
        CallSite cs = (CallSite) MH_bootstrap389 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap389 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper389 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call389 ().invokeExact(o1, o2, o3); }

    static Object bootstrap389 (Object l, Object n, Object t) throws Throwable { return _mh[ 389 ].invokeExact(l, n, t); }

    // 390
    private static MethodType MT_bootstrap390 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap390 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap390", MT_bootstrap390 ());
    }

    private static MethodHandle INDY_call390;
    private static MethodHandle INDY_call390 () throws Throwable {
        if (INDY_call390 != null) return INDY_call390;
        CallSite cs = (CallSite) MH_bootstrap390 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap390 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper390 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call390 ().invokeExact(o1, o2, o3); }

    static Object bootstrap390 (Object l, Object n, Object t) throws Throwable { return _mh[ 390 ].invokeExact(l, n, t); }

    // 391
    private static MethodType MT_bootstrap391 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap391 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap391", MT_bootstrap391 ());
    }

    private static MethodHandle INDY_call391;
    private static MethodHandle INDY_call391 () throws Throwable {
        if (INDY_call391 != null) return INDY_call391;
        CallSite cs = (CallSite) MH_bootstrap391 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap391 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper391 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call391 ().invokeExact(o1, o2, o3); }

    static Object bootstrap391 (Object l, Object n, Object t) throws Throwable { return _mh[ 391 ].invokeExact(l, n, t); }

    // 392
    private static MethodType MT_bootstrap392 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap392 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap392", MT_bootstrap392 ());
    }

    private static MethodHandle INDY_call392;
    private static MethodHandle INDY_call392 () throws Throwable {
        if (INDY_call392 != null) return INDY_call392;
        CallSite cs = (CallSite) MH_bootstrap392 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap392 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper392 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call392 ().invokeExact(o1, o2, o3); }

    static Object bootstrap392 (Object l, Object n, Object t) throws Throwable { return _mh[ 392 ].invokeExact(l, n, t); }

    // 393
    private static MethodType MT_bootstrap393 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap393 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap393", MT_bootstrap393 ());
    }

    private static MethodHandle INDY_call393;
    private static MethodHandle INDY_call393 () throws Throwable {
        if (INDY_call393 != null) return INDY_call393;
        CallSite cs = (CallSite) MH_bootstrap393 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap393 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper393 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call393 ().invokeExact(o1, o2, o3); }

    static Object bootstrap393 (Object l, Object n, Object t) throws Throwable { return _mh[ 393 ].invokeExact(l, n, t); }

    // 394
    private static MethodType MT_bootstrap394 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap394 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap394", MT_bootstrap394 ());
    }

    private static MethodHandle INDY_call394;
    private static MethodHandle INDY_call394 () throws Throwable {
        if (INDY_call394 != null) return INDY_call394;
        CallSite cs = (CallSite) MH_bootstrap394 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap394 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper394 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call394 ().invokeExact(o1, o2, o3); }

    static Object bootstrap394 (Object l, Object n, Object t) throws Throwable { return _mh[ 394 ].invokeExact(l, n, t); }

    // 395
    private static MethodType MT_bootstrap395 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap395 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap395", MT_bootstrap395 ());
    }

    private static MethodHandle INDY_call395;
    private static MethodHandle INDY_call395 () throws Throwable {
        if (INDY_call395 != null) return INDY_call395;
        CallSite cs = (CallSite) MH_bootstrap395 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap395 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper395 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call395 ().invokeExact(o1, o2, o3); }

    static Object bootstrap395 (Object l, Object n, Object t) throws Throwable { return _mh[ 395 ].invokeExact(l, n, t); }

    // 396
    private static MethodType MT_bootstrap396 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap396 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap396", MT_bootstrap396 ());
    }

    private static MethodHandle INDY_call396;
    private static MethodHandle INDY_call396 () throws Throwable {
        if (INDY_call396 != null) return INDY_call396;
        CallSite cs = (CallSite) MH_bootstrap396 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap396 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper396 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call396 ().invokeExact(o1, o2, o3); }

    static Object bootstrap396 (Object l, Object n, Object t) throws Throwable { return _mh[ 396 ].invokeExact(l, n, t); }

    // 397
    private static MethodType MT_bootstrap397 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap397 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap397", MT_bootstrap397 ());
    }

    private static MethodHandle INDY_call397;
    private static MethodHandle INDY_call397 () throws Throwable {
        if (INDY_call397 != null) return INDY_call397;
        CallSite cs = (CallSite) MH_bootstrap397 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap397 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper397 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call397 ().invokeExact(o1, o2, o3); }

    static Object bootstrap397 (Object l, Object n, Object t) throws Throwable { return _mh[ 397 ].invokeExact(l, n, t); }

    // 398
    private static MethodType MT_bootstrap398 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap398 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap398", MT_bootstrap398 ());
    }

    private static MethodHandle INDY_call398;
    private static MethodHandle INDY_call398 () throws Throwable {
        if (INDY_call398 != null) return INDY_call398;
        CallSite cs = (CallSite) MH_bootstrap398 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap398 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper398 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call398 ().invokeExact(o1, o2, o3); }

    static Object bootstrap398 (Object l, Object n, Object t) throws Throwable { return _mh[ 398 ].invokeExact(l, n, t); }

    // 399
    private static MethodType MT_bootstrap399 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap399 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap399", MT_bootstrap399 ());
    }

    private static MethodHandle INDY_call399;
    private static MethodHandle INDY_call399 () throws Throwable {
        if (INDY_call399 != null) return INDY_call399;
        CallSite cs = (CallSite) MH_bootstrap399 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap399 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper399 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call399 ().invokeExact(o1, o2, o3); }

    static Object bootstrap399 (Object l, Object n, Object t) throws Throwable { return _mh[ 399 ].invokeExact(l, n, t); }

    // 400
    private static MethodType MT_bootstrap400 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap400 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap400", MT_bootstrap400 ());
    }

    private static MethodHandle INDY_call400;
    private static MethodHandle INDY_call400 () throws Throwable {
        if (INDY_call400 != null) return INDY_call400;
        CallSite cs = (CallSite) MH_bootstrap400 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap400 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper400 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call400 ().invokeExact(o1, o2, o3); }

    static Object bootstrap400 (Object l, Object n, Object t) throws Throwable { return _mh[ 400 ].invokeExact(l, n, t); }

    // 401
    private static MethodType MT_bootstrap401 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap401 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap401", MT_bootstrap401 ());
    }

    private static MethodHandle INDY_call401;
    private static MethodHandle INDY_call401 () throws Throwable {
        if (INDY_call401 != null) return INDY_call401;
        CallSite cs = (CallSite) MH_bootstrap401 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap401 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper401 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call401 ().invokeExact(o1, o2, o3); }

    static Object bootstrap401 (Object l, Object n, Object t) throws Throwable { return _mh[ 401 ].invokeExact(l, n, t); }

    // 402
    private static MethodType MT_bootstrap402 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap402 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap402", MT_bootstrap402 ());
    }

    private static MethodHandle INDY_call402;
    private static MethodHandle INDY_call402 () throws Throwable {
        if (INDY_call402 != null) return INDY_call402;
        CallSite cs = (CallSite) MH_bootstrap402 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap402 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper402 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call402 ().invokeExact(o1, o2, o3); }

    static Object bootstrap402 (Object l, Object n, Object t) throws Throwable { return _mh[ 402 ].invokeExact(l, n, t); }

    // 403
    private static MethodType MT_bootstrap403 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap403 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap403", MT_bootstrap403 ());
    }

    private static MethodHandle INDY_call403;
    private static MethodHandle INDY_call403 () throws Throwable {
        if (INDY_call403 != null) return INDY_call403;
        CallSite cs = (CallSite) MH_bootstrap403 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap403 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper403 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call403 ().invokeExact(o1, o2, o3); }

    static Object bootstrap403 (Object l, Object n, Object t) throws Throwable { return _mh[ 403 ].invokeExact(l, n, t); }

    // 404
    private static MethodType MT_bootstrap404 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap404 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap404", MT_bootstrap404 ());
    }

    private static MethodHandle INDY_call404;
    private static MethodHandle INDY_call404 () throws Throwable {
        if (INDY_call404 != null) return INDY_call404;
        CallSite cs = (CallSite) MH_bootstrap404 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap404 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper404 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call404 ().invokeExact(o1, o2, o3); }

    static Object bootstrap404 (Object l, Object n, Object t) throws Throwable { return _mh[ 404 ].invokeExact(l, n, t); }

    // 405
    private static MethodType MT_bootstrap405 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap405 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap405", MT_bootstrap405 ());
    }

    private static MethodHandle INDY_call405;
    private static MethodHandle INDY_call405 () throws Throwable {
        if (INDY_call405 != null) return INDY_call405;
        CallSite cs = (CallSite) MH_bootstrap405 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap405 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper405 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call405 ().invokeExact(o1, o2, o3); }

    static Object bootstrap405 (Object l, Object n, Object t) throws Throwable { return _mh[ 405 ].invokeExact(l, n, t); }

    // 406
    private static MethodType MT_bootstrap406 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap406 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap406", MT_bootstrap406 ());
    }

    private static MethodHandle INDY_call406;
    private static MethodHandle INDY_call406 () throws Throwable {
        if (INDY_call406 != null) return INDY_call406;
        CallSite cs = (CallSite) MH_bootstrap406 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap406 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper406 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call406 ().invokeExact(o1, o2, o3); }

    static Object bootstrap406 (Object l, Object n, Object t) throws Throwable { return _mh[ 406 ].invokeExact(l, n, t); }

    // 407
    private static MethodType MT_bootstrap407 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap407 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap407", MT_bootstrap407 ());
    }

    private static MethodHandle INDY_call407;
    private static MethodHandle INDY_call407 () throws Throwable {
        if (INDY_call407 != null) return INDY_call407;
        CallSite cs = (CallSite) MH_bootstrap407 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap407 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper407 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call407 ().invokeExact(o1, o2, o3); }

    static Object bootstrap407 (Object l, Object n, Object t) throws Throwable { return _mh[ 407 ].invokeExact(l, n, t); }

    // 408
    private static MethodType MT_bootstrap408 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap408 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap408", MT_bootstrap408 ());
    }

    private static MethodHandle INDY_call408;
    private static MethodHandle INDY_call408 () throws Throwable {
        if (INDY_call408 != null) return INDY_call408;
        CallSite cs = (CallSite) MH_bootstrap408 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap408 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper408 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call408 ().invokeExact(o1, o2, o3); }

    static Object bootstrap408 (Object l, Object n, Object t) throws Throwable { return _mh[ 408 ].invokeExact(l, n, t); }

    // 409
    private static MethodType MT_bootstrap409 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap409 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap409", MT_bootstrap409 ());
    }

    private static MethodHandle INDY_call409;
    private static MethodHandle INDY_call409 () throws Throwable {
        if (INDY_call409 != null) return INDY_call409;
        CallSite cs = (CallSite) MH_bootstrap409 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap409 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper409 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call409 ().invokeExact(o1, o2, o3); }

    static Object bootstrap409 (Object l, Object n, Object t) throws Throwable { return _mh[ 409 ].invokeExact(l, n, t); }

    // 410
    private static MethodType MT_bootstrap410 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap410 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap410", MT_bootstrap410 ());
    }

    private static MethodHandle INDY_call410;
    private static MethodHandle INDY_call410 () throws Throwable {
        if (INDY_call410 != null) return INDY_call410;
        CallSite cs = (CallSite) MH_bootstrap410 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap410 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper410 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call410 ().invokeExact(o1, o2, o3); }

    static Object bootstrap410 (Object l, Object n, Object t) throws Throwable { return _mh[ 410 ].invokeExact(l, n, t); }

    // 411
    private static MethodType MT_bootstrap411 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap411 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap411", MT_bootstrap411 ());
    }

    private static MethodHandle INDY_call411;
    private static MethodHandle INDY_call411 () throws Throwable {
        if (INDY_call411 != null) return INDY_call411;
        CallSite cs = (CallSite) MH_bootstrap411 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap411 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper411 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call411 ().invokeExact(o1, o2, o3); }

    static Object bootstrap411 (Object l, Object n, Object t) throws Throwable { return _mh[ 411 ].invokeExact(l, n, t); }

    // 412
    private static MethodType MT_bootstrap412 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap412 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap412", MT_bootstrap412 ());
    }

    private static MethodHandle INDY_call412;
    private static MethodHandle INDY_call412 () throws Throwable {
        if (INDY_call412 != null) return INDY_call412;
        CallSite cs = (CallSite) MH_bootstrap412 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap412 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper412 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call412 ().invokeExact(o1, o2, o3); }

    static Object bootstrap412 (Object l, Object n, Object t) throws Throwable { return _mh[ 412 ].invokeExact(l, n, t); }

    // 413
    private static MethodType MT_bootstrap413 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap413 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap413", MT_bootstrap413 ());
    }

    private static MethodHandle INDY_call413;
    private static MethodHandle INDY_call413 () throws Throwable {
        if (INDY_call413 != null) return INDY_call413;
        CallSite cs = (CallSite) MH_bootstrap413 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap413 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper413 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call413 ().invokeExact(o1, o2, o3); }

    static Object bootstrap413 (Object l, Object n, Object t) throws Throwable { return _mh[ 413 ].invokeExact(l, n, t); }

    // 414
    private static MethodType MT_bootstrap414 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap414 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap414", MT_bootstrap414 ());
    }

    private static MethodHandle INDY_call414;
    private static MethodHandle INDY_call414 () throws Throwable {
        if (INDY_call414 != null) return INDY_call414;
        CallSite cs = (CallSite) MH_bootstrap414 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap414 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper414 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call414 ().invokeExact(o1, o2, o3); }

    static Object bootstrap414 (Object l, Object n, Object t) throws Throwable { return _mh[ 414 ].invokeExact(l, n, t); }

    // 415
    private static MethodType MT_bootstrap415 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap415 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap415", MT_bootstrap415 ());
    }

    private static MethodHandle INDY_call415;
    private static MethodHandle INDY_call415 () throws Throwable {
        if (INDY_call415 != null) return INDY_call415;
        CallSite cs = (CallSite) MH_bootstrap415 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap415 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper415 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call415 ().invokeExact(o1, o2, o3); }

    static Object bootstrap415 (Object l, Object n, Object t) throws Throwable { return _mh[ 415 ].invokeExact(l, n, t); }

    // 416
    private static MethodType MT_bootstrap416 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap416 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap416", MT_bootstrap416 ());
    }

    private static MethodHandle INDY_call416;
    private static MethodHandle INDY_call416 () throws Throwable {
        if (INDY_call416 != null) return INDY_call416;
        CallSite cs = (CallSite) MH_bootstrap416 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap416 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper416 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call416 ().invokeExact(o1, o2, o3); }

    static Object bootstrap416 (Object l, Object n, Object t) throws Throwable { return _mh[ 416 ].invokeExact(l, n, t); }

    // 417
    private static MethodType MT_bootstrap417 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap417 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap417", MT_bootstrap417 ());
    }

    private static MethodHandle INDY_call417;
    private static MethodHandle INDY_call417 () throws Throwable {
        if (INDY_call417 != null) return INDY_call417;
        CallSite cs = (CallSite) MH_bootstrap417 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap417 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper417 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call417 ().invokeExact(o1, o2, o3); }

    static Object bootstrap417 (Object l, Object n, Object t) throws Throwable { return _mh[ 417 ].invokeExact(l, n, t); }

    // 418
    private static MethodType MT_bootstrap418 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap418 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap418", MT_bootstrap418 ());
    }

    private static MethodHandle INDY_call418;
    private static MethodHandle INDY_call418 () throws Throwable {
        if (INDY_call418 != null) return INDY_call418;
        CallSite cs = (CallSite) MH_bootstrap418 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap418 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper418 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call418 ().invokeExact(o1, o2, o3); }

    static Object bootstrap418 (Object l, Object n, Object t) throws Throwable { return _mh[ 418 ].invokeExact(l, n, t); }

    // 419
    private static MethodType MT_bootstrap419 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap419 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap419", MT_bootstrap419 ());
    }

    private static MethodHandle INDY_call419;
    private static MethodHandle INDY_call419 () throws Throwable {
        if (INDY_call419 != null) return INDY_call419;
        CallSite cs = (CallSite) MH_bootstrap419 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap419 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper419 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call419 ().invokeExact(o1, o2, o3); }

    static Object bootstrap419 (Object l, Object n, Object t) throws Throwable { return _mh[ 419 ].invokeExact(l, n, t); }

    // 420
    private static MethodType MT_bootstrap420 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap420 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap420", MT_bootstrap420 ());
    }

    private static MethodHandle INDY_call420;
    private static MethodHandle INDY_call420 () throws Throwable {
        if (INDY_call420 != null) return INDY_call420;
        CallSite cs = (CallSite) MH_bootstrap420 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap420 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper420 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call420 ().invokeExact(o1, o2, o3); }

    static Object bootstrap420 (Object l, Object n, Object t) throws Throwable { return _mh[ 420 ].invokeExact(l, n, t); }

    // 421
    private static MethodType MT_bootstrap421 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap421 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap421", MT_bootstrap421 ());
    }

    private static MethodHandle INDY_call421;
    private static MethodHandle INDY_call421 () throws Throwable {
        if (INDY_call421 != null) return INDY_call421;
        CallSite cs = (CallSite) MH_bootstrap421 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap421 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper421 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call421 ().invokeExact(o1, o2, o3); }

    static Object bootstrap421 (Object l, Object n, Object t) throws Throwable { return _mh[ 421 ].invokeExact(l, n, t); }

    // 422
    private static MethodType MT_bootstrap422 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap422 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap422", MT_bootstrap422 ());
    }

    private static MethodHandle INDY_call422;
    private static MethodHandle INDY_call422 () throws Throwable {
        if (INDY_call422 != null) return INDY_call422;
        CallSite cs = (CallSite) MH_bootstrap422 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap422 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper422 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call422 ().invokeExact(o1, o2, o3); }

    static Object bootstrap422 (Object l, Object n, Object t) throws Throwable { return _mh[ 422 ].invokeExact(l, n, t); }

    // 423
    private static MethodType MT_bootstrap423 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap423 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap423", MT_bootstrap423 ());
    }

    private static MethodHandle INDY_call423;
    private static MethodHandle INDY_call423 () throws Throwable {
        if (INDY_call423 != null) return INDY_call423;
        CallSite cs = (CallSite) MH_bootstrap423 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap423 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper423 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call423 ().invokeExact(o1, o2, o3); }

    static Object bootstrap423 (Object l, Object n, Object t) throws Throwable { return _mh[ 423 ].invokeExact(l, n, t); }

    // 424
    private static MethodType MT_bootstrap424 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap424 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap424", MT_bootstrap424 ());
    }

    private static MethodHandle INDY_call424;
    private static MethodHandle INDY_call424 () throws Throwable {
        if (INDY_call424 != null) return INDY_call424;
        CallSite cs = (CallSite) MH_bootstrap424 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap424 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper424 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call424 ().invokeExact(o1, o2, o3); }

    static Object bootstrap424 (Object l, Object n, Object t) throws Throwable { return _mh[ 424 ].invokeExact(l, n, t); }

    // 425
    private static MethodType MT_bootstrap425 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap425 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap425", MT_bootstrap425 ());
    }

    private static MethodHandle INDY_call425;
    private static MethodHandle INDY_call425 () throws Throwable {
        if (INDY_call425 != null) return INDY_call425;
        CallSite cs = (CallSite) MH_bootstrap425 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap425 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper425 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call425 ().invokeExact(o1, o2, o3); }

    static Object bootstrap425 (Object l, Object n, Object t) throws Throwable { return _mh[ 425 ].invokeExact(l, n, t); }

    // 426
    private static MethodType MT_bootstrap426 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap426 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap426", MT_bootstrap426 ());
    }

    private static MethodHandle INDY_call426;
    private static MethodHandle INDY_call426 () throws Throwable {
        if (INDY_call426 != null) return INDY_call426;
        CallSite cs = (CallSite) MH_bootstrap426 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap426 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper426 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call426 ().invokeExact(o1, o2, o3); }

    static Object bootstrap426 (Object l, Object n, Object t) throws Throwable { return _mh[ 426 ].invokeExact(l, n, t); }

    // 427
    private static MethodType MT_bootstrap427 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap427 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap427", MT_bootstrap427 ());
    }

    private static MethodHandle INDY_call427;
    private static MethodHandle INDY_call427 () throws Throwable {
        if (INDY_call427 != null) return INDY_call427;
        CallSite cs = (CallSite) MH_bootstrap427 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap427 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper427 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call427 ().invokeExact(o1, o2, o3); }

    static Object bootstrap427 (Object l, Object n, Object t) throws Throwable { return _mh[ 427 ].invokeExact(l, n, t); }

    // 428
    private static MethodType MT_bootstrap428 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap428 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap428", MT_bootstrap428 ());
    }

    private static MethodHandle INDY_call428;
    private static MethodHandle INDY_call428 () throws Throwable {
        if (INDY_call428 != null) return INDY_call428;
        CallSite cs = (CallSite) MH_bootstrap428 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap428 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper428 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call428 ().invokeExact(o1, o2, o3); }

    static Object bootstrap428 (Object l, Object n, Object t) throws Throwable { return _mh[ 428 ].invokeExact(l, n, t); }

    // 429
    private static MethodType MT_bootstrap429 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap429 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap429", MT_bootstrap429 ());
    }

    private static MethodHandle INDY_call429;
    private static MethodHandle INDY_call429 () throws Throwable {
        if (INDY_call429 != null) return INDY_call429;
        CallSite cs = (CallSite) MH_bootstrap429 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap429 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper429 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call429 ().invokeExact(o1, o2, o3); }

    static Object bootstrap429 (Object l, Object n, Object t) throws Throwable { return _mh[ 429 ].invokeExact(l, n, t); }

    // 430
    private static MethodType MT_bootstrap430 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap430 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap430", MT_bootstrap430 ());
    }

    private static MethodHandle INDY_call430;
    private static MethodHandle INDY_call430 () throws Throwable {
        if (INDY_call430 != null) return INDY_call430;
        CallSite cs = (CallSite) MH_bootstrap430 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap430 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper430 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call430 ().invokeExact(o1, o2, o3); }

    static Object bootstrap430 (Object l, Object n, Object t) throws Throwable { return _mh[ 430 ].invokeExact(l, n, t); }

    // 431
    private static MethodType MT_bootstrap431 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap431 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap431", MT_bootstrap431 ());
    }

    private static MethodHandle INDY_call431;
    private static MethodHandle INDY_call431 () throws Throwable {
        if (INDY_call431 != null) return INDY_call431;
        CallSite cs = (CallSite) MH_bootstrap431 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap431 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper431 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call431 ().invokeExact(o1, o2, o3); }

    static Object bootstrap431 (Object l, Object n, Object t) throws Throwable { return _mh[ 431 ].invokeExact(l, n, t); }

    // 432
    private static MethodType MT_bootstrap432 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap432 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap432", MT_bootstrap432 ());
    }

    private static MethodHandle INDY_call432;
    private static MethodHandle INDY_call432 () throws Throwable {
        if (INDY_call432 != null) return INDY_call432;
        CallSite cs = (CallSite) MH_bootstrap432 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap432 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper432 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call432 ().invokeExact(o1, o2, o3); }

    static Object bootstrap432 (Object l, Object n, Object t) throws Throwable { return _mh[ 432 ].invokeExact(l, n, t); }

    // 433
    private static MethodType MT_bootstrap433 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap433 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap433", MT_bootstrap433 ());
    }

    private static MethodHandle INDY_call433;
    private static MethodHandle INDY_call433 () throws Throwable {
        if (INDY_call433 != null) return INDY_call433;
        CallSite cs = (CallSite) MH_bootstrap433 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap433 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper433 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call433 ().invokeExact(o1, o2, o3); }

    static Object bootstrap433 (Object l, Object n, Object t) throws Throwable { return _mh[ 433 ].invokeExact(l, n, t); }

    // 434
    private static MethodType MT_bootstrap434 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap434 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap434", MT_bootstrap434 ());
    }

    private static MethodHandle INDY_call434;
    private static MethodHandle INDY_call434 () throws Throwable {
        if (INDY_call434 != null) return INDY_call434;
        CallSite cs = (CallSite) MH_bootstrap434 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap434 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper434 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call434 ().invokeExact(o1, o2, o3); }

    static Object bootstrap434 (Object l, Object n, Object t) throws Throwable { return _mh[ 434 ].invokeExact(l, n, t); }

    // 435
    private static MethodType MT_bootstrap435 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap435 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap435", MT_bootstrap435 ());
    }

    private static MethodHandle INDY_call435;
    private static MethodHandle INDY_call435 () throws Throwable {
        if (INDY_call435 != null) return INDY_call435;
        CallSite cs = (CallSite) MH_bootstrap435 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap435 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper435 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call435 ().invokeExact(o1, o2, o3); }

    static Object bootstrap435 (Object l, Object n, Object t) throws Throwable { return _mh[ 435 ].invokeExact(l, n, t); }

    // 436
    private static MethodType MT_bootstrap436 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap436 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap436", MT_bootstrap436 ());
    }

    private static MethodHandle INDY_call436;
    private static MethodHandle INDY_call436 () throws Throwable {
        if (INDY_call436 != null) return INDY_call436;
        CallSite cs = (CallSite) MH_bootstrap436 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap436 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper436 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call436 ().invokeExact(o1, o2, o3); }

    static Object bootstrap436 (Object l, Object n, Object t) throws Throwable { return _mh[ 436 ].invokeExact(l, n, t); }

    // 437
    private static MethodType MT_bootstrap437 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap437 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap437", MT_bootstrap437 ());
    }

    private static MethodHandle INDY_call437;
    private static MethodHandle INDY_call437 () throws Throwable {
        if (INDY_call437 != null) return INDY_call437;
        CallSite cs = (CallSite) MH_bootstrap437 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap437 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper437 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call437 ().invokeExact(o1, o2, o3); }

    static Object bootstrap437 (Object l, Object n, Object t) throws Throwable { return _mh[ 437 ].invokeExact(l, n, t); }

    // 438
    private static MethodType MT_bootstrap438 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap438 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap438", MT_bootstrap438 ());
    }

    private static MethodHandle INDY_call438;
    private static MethodHandle INDY_call438 () throws Throwable {
        if (INDY_call438 != null) return INDY_call438;
        CallSite cs = (CallSite) MH_bootstrap438 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap438 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper438 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call438 ().invokeExact(o1, o2, o3); }

    static Object bootstrap438 (Object l, Object n, Object t) throws Throwable { return _mh[ 438 ].invokeExact(l, n, t); }

    // 439
    private static MethodType MT_bootstrap439 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap439 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap439", MT_bootstrap439 ());
    }

    private static MethodHandle INDY_call439;
    private static MethodHandle INDY_call439 () throws Throwable {
        if (INDY_call439 != null) return INDY_call439;
        CallSite cs = (CallSite) MH_bootstrap439 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap439 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper439 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call439 ().invokeExact(o1, o2, o3); }

    static Object bootstrap439 (Object l, Object n, Object t) throws Throwable { return _mh[ 439 ].invokeExact(l, n, t); }

    // 440
    private static MethodType MT_bootstrap440 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap440 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap440", MT_bootstrap440 ());
    }

    private static MethodHandle INDY_call440;
    private static MethodHandle INDY_call440 () throws Throwable {
        if (INDY_call440 != null) return INDY_call440;
        CallSite cs = (CallSite) MH_bootstrap440 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap440 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper440 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call440 ().invokeExact(o1, o2, o3); }

    static Object bootstrap440 (Object l, Object n, Object t) throws Throwable { return _mh[ 440 ].invokeExact(l, n, t); }

    // 441
    private static MethodType MT_bootstrap441 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap441 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap441", MT_bootstrap441 ());
    }

    private static MethodHandle INDY_call441;
    private static MethodHandle INDY_call441 () throws Throwable {
        if (INDY_call441 != null) return INDY_call441;
        CallSite cs = (CallSite) MH_bootstrap441 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap441 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper441 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call441 ().invokeExact(o1, o2, o3); }

    static Object bootstrap441 (Object l, Object n, Object t) throws Throwable { return _mh[ 441 ].invokeExact(l, n, t); }

    // 442
    private static MethodType MT_bootstrap442 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap442 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap442", MT_bootstrap442 ());
    }

    private static MethodHandle INDY_call442;
    private static MethodHandle INDY_call442 () throws Throwable {
        if (INDY_call442 != null) return INDY_call442;
        CallSite cs = (CallSite) MH_bootstrap442 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap442 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper442 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call442 ().invokeExact(o1, o2, o3); }

    static Object bootstrap442 (Object l, Object n, Object t) throws Throwable { return _mh[ 442 ].invokeExact(l, n, t); }

    // 443
    private static MethodType MT_bootstrap443 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap443 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap443", MT_bootstrap443 ());
    }

    private static MethodHandle INDY_call443;
    private static MethodHandle INDY_call443 () throws Throwable {
        if (INDY_call443 != null) return INDY_call443;
        CallSite cs = (CallSite) MH_bootstrap443 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap443 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper443 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call443 ().invokeExact(o1, o2, o3); }

    static Object bootstrap443 (Object l, Object n, Object t) throws Throwable { return _mh[ 443 ].invokeExact(l, n, t); }

    // 444
    private static MethodType MT_bootstrap444 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap444 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap444", MT_bootstrap444 ());
    }

    private static MethodHandle INDY_call444;
    private static MethodHandle INDY_call444 () throws Throwable {
        if (INDY_call444 != null) return INDY_call444;
        CallSite cs = (CallSite) MH_bootstrap444 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap444 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper444 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call444 ().invokeExact(o1, o2, o3); }

    static Object bootstrap444 (Object l, Object n, Object t) throws Throwable { return _mh[ 444 ].invokeExact(l, n, t); }

    // 445
    private static MethodType MT_bootstrap445 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap445 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap445", MT_bootstrap445 ());
    }

    private static MethodHandle INDY_call445;
    private static MethodHandle INDY_call445 () throws Throwable {
        if (INDY_call445 != null) return INDY_call445;
        CallSite cs = (CallSite) MH_bootstrap445 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap445 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper445 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call445 ().invokeExact(o1, o2, o3); }

    static Object bootstrap445 (Object l, Object n, Object t) throws Throwable { return _mh[ 445 ].invokeExact(l, n, t); }

    // 446
    private static MethodType MT_bootstrap446 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap446 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap446", MT_bootstrap446 ());
    }

    private static MethodHandle INDY_call446;
    private static MethodHandle INDY_call446 () throws Throwable {
        if (INDY_call446 != null) return INDY_call446;
        CallSite cs = (CallSite) MH_bootstrap446 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap446 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper446 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call446 ().invokeExact(o1, o2, o3); }

    static Object bootstrap446 (Object l, Object n, Object t) throws Throwable { return _mh[ 446 ].invokeExact(l, n, t); }

    // 447
    private static MethodType MT_bootstrap447 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap447 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap447", MT_bootstrap447 ());
    }

    private static MethodHandle INDY_call447;
    private static MethodHandle INDY_call447 () throws Throwable {
        if (INDY_call447 != null) return INDY_call447;
        CallSite cs = (CallSite) MH_bootstrap447 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap447 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper447 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call447 ().invokeExact(o1, o2, o3); }

    static Object bootstrap447 (Object l, Object n, Object t) throws Throwable { return _mh[ 447 ].invokeExact(l, n, t); }

    // 448
    private static MethodType MT_bootstrap448 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap448 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap448", MT_bootstrap448 ());
    }

    private static MethodHandle INDY_call448;
    private static MethodHandle INDY_call448 () throws Throwable {
        if (INDY_call448 != null) return INDY_call448;
        CallSite cs = (CallSite) MH_bootstrap448 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap448 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper448 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call448 ().invokeExact(o1, o2, o3); }

    static Object bootstrap448 (Object l, Object n, Object t) throws Throwable { return _mh[ 448 ].invokeExact(l, n, t); }

    // 449
    private static MethodType MT_bootstrap449 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap449 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap449", MT_bootstrap449 ());
    }

    private static MethodHandle INDY_call449;
    private static MethodHandle INDY_call449 () throws Throwable {
        if (INDY_call449 != null) return INDY_call449;
        CallSite cs = (CallSite) MH_bootstrap449 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap449 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper449 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call449 ().invokeExact(o1, o2, o3); }

    static Object bootstrap449 (Object l, Object n, Object t) throws Throwable { return _mh[ 449 ].invokeExact(l, n, t); }

    // 450
    private static MethodType MT_bootstrap450 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap450 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap450", MT_bootstrap450 ());
    }

    private static MethodHandle INDY_call450;
    private static MethodHandle INDY_call450 () throws Throwable {
        if (INDY_call450 != null) return INDY_call450;
        CallSite cs = (CallSite) MH_bootstrap450 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap450 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper450 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call450 ().invokeExact(o1, o2, o3); }

    static Object bootstrap450 (Object l, Object n, Object t) throws Throwable { return _mh[ 450 ].invokeExact(l, n, t); }

    // 451
    private static MethodType MT_bootstrap451 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap451 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap451", MT_bootstrap451 ());
    }

    private static MethodHandle INDY_call451;
    private static MethodHandle INDY_call451 () throws Throwable {
        if (INDY_call451 != null) return INDY_call451;
        CallSite cs = (CallSite) MH_bootstrap451 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap451 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper451 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call451 ().invokeExact(o1, o2, o3); }

    static Object bootstrap451 (Object l, Object n, Object t) throws Throwable { return _mh[ 451 ].invokeExact(l, n, t); }

    // 452
    private static MethodType MT_bootstrap452 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap452 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap452", MT_bootstrap452 ());
    }

    private static MethodHandle INDY_call452;
    private static MethodHandle INDY_call452 () throws Throwable {
        if (INDY_call452 != null) return INDY_call452;
        CallSite cs = (CallSite) MH_bootstrap452 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap452 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper452 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call452 ().invokeExact(o1, o2, o3); }

    static Object bootstrap452 (Object l, Object n, Object t) throws Throwable { return _mh[ 452 ].invokeExact(l, n, t); }

    // 453
    private static MethodType MT_bootstrap453 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap453 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap453", MT_bootstrap453 ());
    }

    private static MethodHandle INDY_call453;
    private static MethodHandle INDY_call453 () throws Throwable {
        if (INDY_call453 != null) return INDY_call453;
        CallSite cs = (CallSite) MH_bootstrap453 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap453 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper453 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call453 ().invokeExact(o1, o2, o3); }

    static Object bootstrap453 (Object l, Object n, Object t) throws Throwable { return _mh[ 453 ].invokeExact(l, n, t); }

    // 454
    private static MethodType MT_bootstrap454 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap454 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap454", MT_bootstrap454 ());
    }

    private static MethodHandle INDY_call454;
    private static MethodHandle INDY_call454 () throws Throwable {
        if (INDY_call454 != null) return INDY_call454;
        CallSite cs = (CallSite) MH_bootstrap454 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap454 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper454 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call454 ().invokeExact(o1, o2, o3); }

    static Object bootstrap454 (Object l, Object n, Object t) throws Throwable { return _mh[ 454 ].invokeExact(l, n, t); }

    // 455
    private static MethodType MT_bootstrap455 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap455 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap455", MT_bootstrap455 ());
    }

    private static MethodHandle INDY_call455;
    private static MethodHandle INDY_call455 () throws Throwable {
        if (INDY_call455 != null) return INDY_call455;
        CallSite cs = (CallSite) MH_bootstrap455 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap455 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper455 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call455 ().invokeExact(o1, o2, o3); }

    static Object bootstrap455 (Object l, Object n, Object t) throws Throwable { return _mh[ 455 ].invokeExact(l, n, t); }

    // 456
    private static MethodType MT_bootstrap456 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap456 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap456", MT_bootstrap456 ());
    }

    private static MethodHandle INDY_call456;
    private static MethodHandle INDY_call456 () throws Throwable {
        if (INDY_call456 != null) return INDY_call456;
        CallSite cs = (CallSite) MH_bootstrap456 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap456 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper456 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call456 ().invokeExact(o1, o2, o3); }

    static Object bootstrap456 (Object l, Object n, Object t) throws Throwable { return _mh[ 456 ].invokeExact(l, n, t); }

    // 457
    private static MethodType MT_bootstrap457 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap457 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap457", MT_bootstrap457 ());
    }

    private static MethodHandle INDY_call457;
    private static MethodHandle INDY_call457 () throws Throwable {
        if (INDY_call457 != null) return INDY_call457;
        CallSite cs = (CallSite) MH_bootstrap457 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap457 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper457 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call457 ().invokeExact(o1, o2, o3); }

    static Object bootstrap457 (Object l, Object n, Object t) throws Throwable { return _mh[ 457 ].invokeExact(l, n, t); }

    // 458
    private static MethodType MT_bootstrap458 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap458 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap458", MT_bootstrap458 ());
    }

    private static MethodHandle INDY_call458;
    private static MethodHandle INDY_call458 () throws Throwable {
        if (INDY_call458 != null) return INDY_call458;
        CallSite cs = (CallSite) MH_bootstrap458 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap458 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper458 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call458 ().invokeExact(o1, o2, o3); }

    static Object bootstrap458 (Object l, Object n, Object t) throws Throwable { return _mh[ 458 ].invokeExact(l, n, t); }

    // 459
    private static MethodType MT_bootstrap459 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap459 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap459", MT_bootstrap459 ());
    }

    private static MethodHandle INDY_call459;
    private static MethodHandle INDY_call459 () throws Throwable {
        if (INDY_call459 != null) return INDY_call459;
        CallSite cs = (CallSite) MH_bootstrap459 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap459 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper459 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call459 ().invokeExact(o1, o2, o3); }

    static Object bootstrap459 (Object l, Object n, Object t) throws Throwable { return _mh[ 459 ].invokeExact(l, n, t); }

    // 460
    private static MethodType MT_bootstrap460 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap460 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap460", MT_bootstrap460 ());
    }

    private static MethodHandle INDY_call460;
    private static MethodHandle INDY_call460 () throws Throwable {
        if (INDY_call460 != null) return INDY_call460;
        CallSite cs = (CallSite) MH_bootstrap460 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap460 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper460 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call460 ().invokeExact(o1, o2, o3); }

    static Object bootstrap460 (Object l, Object n, Object t) throws Throwable { return _mh[ 460 ].invokeExact(l, n, t); }

    // 461
    private static MethodType MT_bootstrap461 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap461 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap461", MT_bootstrap461 ());
    }

    private static MethodHandle INDY_call461;
    private static MethodHandle INDY_call461 () throws Throwable {
        if (INDY_call461 != null) return INDY_call461;
        CallSite cs = (CallSite) MH_bootstrap461 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap461 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper461 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call461 ().invokeExact(o1, o2, o3); }

    static Object bootstrap461 (Object l, Object n, Object t) throws Throwable { return _mh[ 461 ].invokeExact(l, n, t); }

    // 462
    private static MethodType MT_bootstrap462 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap462 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap462", MT_bootstrap462 ());
    }

    private static MethodHandle INDY_call462;
    private static MethodHandle INDY_call462 () throws Throwable {
        if (INDY_call462 != null) return INDY_call462;
        CallSite cs = (CallSite) MH_bootstrap462 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap462 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper462 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call462 ().invokeExact(o1, o2, o3); }

    static Object bootstrap462 (Object l, Object n, Object t) throws Throwable { return _mh[ 462 ].invokeExact(l, n, t); }

    // 463
    private static MethodType MT_bootstrap463 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap463 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap463", MT_bootstrap463 ());
    }

    private static MethodHandle INDY_call463;
    private static MethodHandle INDY_call463 () throws Throwable {
        if (INDY_call463 != null) return INDY_call463;
        CallSite cs = (CallSite) MH_bootstrap463 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap463 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper463 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call463 ().invokeExact(o1, o2, o3); }

    static Object bootstrap463 (Object l, Object n, Object t) throws Throwable { return _mh[ 463 ].invokeExact(l, n, t); }

    // 464
    private static MethodType MT_bootstrap464 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap464 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap464", MT_bootstrap464 ());
    }

    private static MethodHandle INDY_call464;
    private static MethodHandle INDY_call464 () throws Throwable {
        if (INDY_call464 != null) return INDY_call464;
        CallSite cs = (CallSite) MH_bootstrap464 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap464 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper464 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call464 ().invokeExact(o1, o2, o3); }

    static Object bootstrap464 (Object l, Object n, Object t) throws Throwable { return _mh[ 464 ].invokeExact(l, n, t); }

    // 465
    private static MethodType MT_bootstrap465 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap465 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap465", MT_bootstrap465 ());
    }

    private static MethodHandle INDY_call465;
    private static MethodHandle INDY_call465 () throws Throwable {
        if (INDY_call465 != null) return INDY_call465;
        CallSite cs = (CallSite) MH_bootstrap465 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap465 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper465 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call465 ().invokeExact(o1, o2, o3); }

    static Object bootstrap465 (Object l, Object n, Object t) throws Throwable { return _mh[ 465 ].invokeExact(l, n, t); }

    // 466
    private static MethodType MT_bootstrap466 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap466 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap466", MT_bootstrap466 ());
    }

    private static MethodHandle INDY_call466;
    private static MethodHandle INDY_call466 () throws Throwable {
        if (INDY_call466 != null) return INDY_call466;
        CallSite cs = (CallSite) MH_bootstrap466 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap466 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper466 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call466 ().invokeExact(o1, o2, o3); }

    static Object bootstrap466 (Object l, Object n, Object t) throws Throwable { return _mh[ 466 ].invokeExact(l, n, t); }

    // 467
    private static MethodType MT_bootstrap467 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap467 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap467", MT_bootstrap467 ());
    }

    private static MethodHandle INDY_call467;
    private static MethodHandle INDY_call467 () throws Throwable {
        if (INDY_call467 != null) return INDY_call467;
        CallSite cs = (CallSite) MH_bootstrap467 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap467 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper467 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call467 ().invokeExact(o1, o2, o3); }

    static Object bootstrap467 (Object l, Object n, Object t) throws Throwable { return _mh[ 467 ].invokeExact(l, n, t); }

    // 468
    private static MethodType MT_bootstrap468 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap468 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap468", MT_bootstrap468 ());
    }

    private static MethodHandle INDY_call468;
    private static MethodHandle INDY_call468 () throws Throwable {
        if (INDY_call468 != null) return INDY_call468;
        CallSite cs = (CallSite) MH_bootstrap468 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap468 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper468 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call468 ().invokeExact(o1, o2, o3); }

    static Object bootstrap468 (Object l, Object n, Object t) throws Throwable { return _mh[ 468 ].invokeExact(l, n, t); }

    // 469
    private static MethodType MT_bootstrap469 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap469 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap469", MT_bootstrap469 ());
    }

    private static MethodHandle INDY_call469;
    private static MethodHandle INDY_call469 () throws Throwable {
        if (INDY_call469 != null) return INDY_call469;
        CallSite cs = (CallSite) MH_bootstrap469 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap469 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper469 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call469 ().invokeExact(o1, o2, o3); }

    static Object bootstrap469 (Object l, Object n, Object t) throws Throwable { return _mh[ 469 ].invokeExact(l, n, t); }

    // 470
    private static MethodType MT_bootstrap470 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap470 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap470", MT_bootstrap470 ());
    }

    private static MethodHandle INDY_call470;
    private static MethodHandle INDY_call470 () throws Throwable {
        if (INDY_call470 != null) return INDY_call470;
        CallSite cs = (CallSite) MH_bootstrap470 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap470 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper470 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call470 ().invokeExact(o1, o2, o3); }

    static Object bootstrap470 (Object l, Object n, Object t) throws Throwable { return _mh[ 470 ].invokeExact(l, n, t); }

    // 471
    private static MethodType MT_bootstrap471 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap471 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap471", MT_bootstrap471 ());
    }

    private static MethodHandle INDY_call471;
    private static MethodHandle INDY_call471 () throws Throwable {
        if (INDY_call471 != null) return INDY_call471;
        CallSite cs = (CallSite) MH_bootstrap471 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap471 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper471 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call471 ().invokeExact(o1, o2, o3); }

    static Object bootstrap471 (Object l, Object n, Object t) throws Throwable { return _mh[ 471 ].invokeExact(l, n, t); }

    // 472
    private static MethodType MT_bootstrap472 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap472 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap472", MT_bootstrap472 ());
    }

    private static MethodHandle INDY_call472;
    private static MethodHandle INDY_call472 () throws Throwable {
        if (INDY_call472 != null) return INDY_call472;
        CallSite cs = (CallSite) MH_bootstrap472 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap472 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper472 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call472 ().invokeExact(o1, o2, o3); }

    static Object bootstrap472 (Object l, Object n, Object t) throws Throwable { return _mh[ 472 ].invokeExact(l, n, t); }

    // 473
    private static MethodType MT_bootstrap473 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap473 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap473", MT_bootstrap473 ());
    }

    private static MethodHandle INDY_call473;
    private static MethodHandle INDY_call473 () throws Throwable {
        if (INDY_call473 != null) return INDY_call473;
        CallSite cs = (CallSite) MH_bootstrap473 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap473 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper473 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call473 ().invokeExact(o1, o2, o3); }

    static Object bootstrap473 (Object l, Object n, Object t) throws Throwable { return _mh[ 473 ].invokeExact(l, n, t); }

    // 474
    private static MethodType MT_bootstrap474 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap474 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap474", MT_bootstrap474 ());
    }

    private static MethodHandle INDY_call474;
    private static MethodHandle INDY_call474 () throws Throwable {
        if (INDY_call474 != null) return INDY_call474;
        CallSite cs = (CallSite) MH_bootstrap474 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap474 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper474 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call474 ().invokeExact(o1, o2, o3); }

    static Object bootstrap474 (Object l, Object n, Object t) throws Throwable { return _mh[ 474 ].invokeExact(l, n, t); }

    // 475
    private static MethodType MT_bootstrap475 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap475 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap475", MT_bootstrap475 ());
    }

    private static MethodHandle INDY_call475;
    private static MethodHandle INDY_call475 () throws Throwable {
        if (INDY_call475 != null) return INDY_call475;
        CallSite cs = (CallSite) MH_bootstrap475 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap475 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper475 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call475 ().invokeExact(o1, o2, o3); }

    static Object bootstrap475 (Object l, Object n, Object t) throws Throwable { return _mh[ 475 ].invokeExact(l, n, t); }

    // 476
    private static MethodType MT_bootstrap476 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap476 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap476", MT_bootstrap476 ());
    }

    private static MethodHandle INDY_call476;
    private static MethodHandle INDY_call476 () throws Throwable {
        if (INDY_call476 != null) return INDY_call476;
        CallSite cs = (CallSite) MH_bootstrap476 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap476 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper476 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call476 ().invokeExact(o1, o2, o3); }

    static Object bootstrap476 (Object l, Object n, Object t) throws Throwable { return _mh[ 476 ].invokeExact(l, n, t); }

    // 477
    private static MethodType MT_bootstrap477 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap477 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap477", MT_bootstrap477 ());
    }

    private static MethodHandle INDY_call477;
    private static MethodHandle INDY_call477 () throws Throwable {
        if (INDY_call477 != null) return INDY_call477;
        CallSite cs = (CallSite) MH_bootstrap477 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap477 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper477 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call477 ().invokeExact(o1, o2, o3); }

    static Object bootstrap477 (Object l, Object n, Object t) throws Throwable { return _mh[ 477 ].invokeExact(l, n, t); }

    // 478
    private static MethodType MT_bootstrap478 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap478 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap478", MT_bootstrap478 ());
    }

    private static MethodHandle INDY_call478;
    private static MethodHandle INDY_call478 () throws Throwable {
        if (INDY_call478 != null) return INDY_call478;
        CallSite cs = (CallSite) MH_bootstrap478 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap478 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper478 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call478 ().invokeExact(o1, o2, o3); }

    static Object bootstrap478 (Object l, Object n, Object t) throws Throwable { return _mh[ 478 ].invokeExact(l, n, t); }

    // 479
    private static MethodType MT_bootstrap479 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap479 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap479", MT_bootstrap479 ());
    }

    private static MethodHandle INDY_call479;
    private static MethodHandle INDY_call479 () throws Throwable {
        if (INDY_call479 != null) return INDY_call479;
        CallSite cs = (CallSite) MH_bootstrap479 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap479 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper479 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call479 ().invokeExact(o1, o2, o3); }

    static Object bootstrap479 (Object l, Object n, Object t) throws Throwable { return _mh[ 479 ].invokeExact(l, n, t); }

    // 480
    private static MethodType MT_bootstrap480 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap480 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap480", MT_bootstrap480 ());
    }

    private static MethodHandle INDY_call480;
    private static MethodHandle INDY_call480 () throws Throwable {
        if (INDY_call480 != null) return INDY_call480;
        CallSite cs = (CallSite) MH_bootstrap480 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap480 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper480 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call480 ().invokeExact(o1, o2, o3); }

    static Object bootstrap480 (Object l, Object n, Object t) throws Throwable { return _mh[ 480 ].invokeExact(l, n, t); }

    // 481
    private static MethodType MT_bootstrap481 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap481 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap481", MT_bootstrap481 ());
    }

    private static MethodHandle INDY_call481;
    private static MethodHandle INDY_call481 () throws Throwable {
        if (INDY_call481 != null) return INDY_call481;
        CallSite cs = (CallSite) MH_bootstrap481 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap481 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper481 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call481 ().invokeExact(o1, o2, o3); }

    static Object bootstrap481 (Object l, Object n, Object t) throws Throwable { return _mh[ 481 ].invokeExact(l, n, t); }

    // 482
    private static MethodType MT_bootstrap482 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap482 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap482", MT_bootstrap482 ());
    }

    private static MethodHandle INDY_call482;
    private static MethodHandle INDY_call482 () throws Throwable {
        if (INDY_call482 != null) return INDY_call482;
        CallSite cs = (CallSite) MH_bootstrap482 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap482 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper482 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call482 ().invokeExact(o1, o2, o3); }

    static Object bootstrap482 (Object l, Object n, Object t) throws Throwable { return _mh[ 482 ].invokeExact(l, n, t); }

    // 483
    private static MethodType MT_bootstrap483 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap483 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap483", MT_bootstrap483 ());
    }

    private static MethodHandle INDY_call483;
    private static MethodHandle INDY_call483 () throws Throwable {
        if (INDY_call483 != null) return INDY_call483;
        CallSite cs = (CallSite) MH_bootstrap483 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap483 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper483 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call483 ().invokeExact(o1, o2, o3); }

    static Object bootstrap483 (Object l, Object n, Object t) throws Throwable { return _mh[ 483 ].invokeExact(l, n, t); }

    // 484
    private static MethodType MT_bootstrap484 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap484 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap484", MT_bootstrap484 ());
    }

    private static MethodHandle INDY_call484;
    private static MethodHandle INDY_call484 () throws Throwable {
        if (INDY_call484 != null) return INDY_call484;
        CallSite cs = (CallSite) MH_bootstrap484 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap484 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper484 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call484 ().invokeExact(o1, o2, o3); }

    static Object bootstrap484 (Object l, Object n, Object t) throws Throwable { return _mh[ 484 ].invokeExact(l, n, t); }

    // 485
    private static MethodType MT_bootstrap485 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap485 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap485", MT_bootstrap485 ());
    }

    private static MethodHandle INDY_call485;
    private static MethodHandle INDY_call485 () throws Throwable {
        if (INDY_call485 != null) return INDY_call485;
        CallSite cs = (CallSite) MH_bootstrap485 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap485 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper485 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call485 ().invokeExact(o1, o2, o3); }

    static Object bootstrap485 (Object l, Object n, Object t) throws Throwable { return _mh[ 485 ].invokeExact(l, n, t); }

    // 486
    private static MethodType MT_bootstrap486 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap486 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap486", MT_bootstrap486 ());
    }

    private static MethodHandle INDY_call486;
    private static MethodHandle INDY_call486 () throws Throwable {
        if (INDY_call486 != null) return INDY_call486;
        CallSite cs = (CallSite) MH_bootstrap486 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap486 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper486 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call486 ().invokeExact(o1, o2, o3); }

    static Object bootstrap486 (Object l, Object n, Object t) throws Throwable { return _mh[ 486 ].invokeExact(l, n, t); }

    // 487
    private static MethodType MT_bootstrap487 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap487 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap487", MT_bootstrap487 ());
    }

    private static MethodHandle INDY_call487;
    private static MethodHandle INDY_call487 () throws Throwable {
        if (INDY_call487 != null) return INDY_call487;
        CallSite cs = (CallSite) MH_bootstrap487 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap487 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper487 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call487 ().invokeExact(o1, o2, o3); }

    static Object bootstrap487 (Object l, Object n, Object t) throws Throwable { return _mh[ 487 ].invokeExact(l, n, t); }

    // 488
    private static MethodType MT_bootstrap488 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap488 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap488", MT_bootstrap488 ());
    }

    private static MethodHandle INDY_call488;
    private static MethodHandle INDY_call488 () throws Throwable {
        if (INDY_call488 != null) return INDY_call488;
        CallSite cs = (CallSite) MH_bootstrap488 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap488 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper488 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call488 ().invokeExact(o1, o2, o3); }

    static Object bootstrap488 (Object l, Object n, Object t) throws Throwable { return _mh[ 488 ].invokeExact(l, n, t); }

    // 489
    private static MethodType MT_bootstrap489 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap489 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap489", MT_bootstrap489 ());
    }

    private static MethodHandle INDY_call489;
    private static MethodHandle INDY_call489 () throws Throwable {
        if (INDY_call489 != null) return INDY_call489;
        CallSite cs = (CallSite) MH_bootstrap489 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap489 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper489 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call489 ().invokeExact(o1, o2, o3); }

    static Object bootstrap489 (Object l, Object n, Object t) throws Throwable { return _mh[ 489 ].invokeExact(l, n, t); }

    // 490
    private static MethodType MT_bootstrap490 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap490 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap490", MT_bootstrap490 ());
    }

    private static MethodHandle INDY_call490;
    private static MethodHandle INDY_call490 () throws Throwable {
        if (INDY_call490 != null) return INDY_call490;
        CallSite cs = (CallSite) MH_bootstrap490 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap490 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper490 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call490 ().invokeExact(o1, o2, o3); }

    static Object bootstrap490 (Object l, Object n, Object t) throws Throwable { return _mh[ 490 ].invokeExact(l, n, t); }

    // 491
    private static MethodType MT_bootstrap491 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap491 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap491", MT_bootstrap491 ());
    }

    private static MethodHandle INDY_call491;
    private static MethodHandle INDY_call491 () throws Throwable {
        if (INDY_call491 != null) return INDY_call491;
        CallSite cs = (CallSite) MH_bootstrap491 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap491 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper491 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call491 ().invokeExact(o1, o2, o3); }

    static Object bootstrap491 (Object l, Object n, Object t) throws Throwable { return _mh[ 491 ].invokeExact(l, n, t); }

    // 492
    private static MethodType MT_bootstrap492 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap492 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap492", MT_bootstrap492 ());
    }

    private static MethodHandle INDY_call492;
    private static MethodHandle INDY_call492 () throws Throwable {
        if (INDY_call492 != null) return INDY_call492;
        CallSite cs = (CallSite) MH_bootstrap492 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap492 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper492 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call492 ().invokeExact(o1, o2, o3); }

    static Object bootstrap492 (Object l, Object n, Object t) throws Throwable { return _mh[ 492 ].invokeExact(l, n, t); }

    // 493
    private static MethodType MT_bootstrap493 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap493 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap493", MT_bootstrap493 ());
    }

    private static MethodHandle INDY_call493;
    private static MethodHandle INDY_call493 () throws Throwable {
        if (INDY_call493 != null) return INDY_call493;
        CallSite cs = (CallSite) MH_bootstrap493 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap493 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper493 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call493 ().invokeExact(o1, o2, o3); }

    static Object bootstrap493 (Object l, Object n, Object t) throws Throwable { return _mh[ 493 ].invokeExact(l, n, t); }

    // 494
    private static MethodType MT_bootstrap494 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap494 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap494", MT_bootstrap494 ());
    }

    private static MethodHandle INDY_call494;
    private static MethodHandle INDY_call494 () throws Throwable {
        if (INDY_call494 != null) return INDY_call494;
        CallSite cs = (CallSite) MH_bootstrap494 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap494 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper494 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call494 ().invokeExact(o1, o2, o3); }

    static Object bootstrap494 (Object l, Object n, Object t) throws Throwable { return _mh[ 494 ].invokeExact(l, n, t); }

    // 495
    private static MethodType MT_bootstrap495 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap495 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap495", MT_bootstrap495 ());
    }

    private static MethodHandle INDY_call495;
    private static MethodHandle INDY_call495 () throws Throwable {
        if (INDY_call495 != null) return INDY_call495;
        CallSite cs = (CallSite) MH_bootstrap495 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap495 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper495 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call495 ().invokeExact(o1, o2, o3); }

    static Object bootstrap495 (Object l, Object n, Object t) throws Throwable { return _mh[ 495 ].invokeExact(l, n, t); }

    // 496
    private static MethodType MT_bootstrap496 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap496 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap496", MT_bootstrap496 ());
    }

    private static MethodHandle INDY_call496;
    private static MethodHandle INDY_call496 () throws Throwable {
        if (INDY_call496 != null) return INDY_call496;
        CallSite cs = (CallSite) MH_bootstrap496 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap496 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper496 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call496 ().invokeExact(o1, o2, o3); }

    static Object bootstrap496 (Object l, Object n, Object t) throws Throwable { return _mh[ 496 ].invokeExact(l, n, t); }

    // 497
    private static MethodType MT_bootstrap497 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap497 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap497", MT_bootstrap497 ());
    }

    private static MethodHandle INDY_call497;
    private static MethodHandle INDY_call497 () throws Throwable {
        if (INDY_call497 != null) return INDY_call497;
        CallSite cs = (CallSite) MH_bootstrap497 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap497 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper497 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call497 ().invokeExact(o1, o2, o3); }

    static Object bootstrap497 (Object l, Object n, Object t) throws Throwable { return _mh[ 497 ].invokeExact(l, n, t); }

    // 498
    private static MethodType MT_bootstrap498 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap498 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap498", MT_bootstrap498 ());
    }

    private static MethodHandle INDY_call498;
    private static MethodHandle INDY_call498 () throws Throwable {
        if (INDY_call498 != null) return INDY_call498;
        CallSite cs = (CallSite) MH_bootstrap498 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap498 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper498 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call498 ().invokeExact(o1, o2, o3); }

    static Object bootstrap498 (Object l, Object n, Object t) throws Throwable { return _mh[ 498 ].invokeExact(l, n, t); }

    // 499
    private static MethodType MT_bootstrap499 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap499 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap499", MT_bootstrap499 ());
    }

    private static MethodHandle INDY_call499;
    private static MethodHandle INDY_call499 () throws Throwable {
        if (INDY_call499 != null) return INDY_call499;
        CallSite cs = (CallSite) MH_bootstrap499 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap499 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper499 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call499 ().invokeExact(o1, o2, o3); }

    static Object bootstrap499 (Object l, Object n, Object t) throws Throwable { return _mh[ 499 ].invokeExact(l, n, t); }

    // 500
    private static MethodType MT_bootstrap500 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap500 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap500", MT_bootstrap500 ());
    }

    private static MethodHandle INDY_call500;
    private static MethodHandle INDY_call500 () throws Throwable {
        if (INDY_call500 != null) return INDY_call500;
        CallSite cs = (CallSite) MH_bootstrap500 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap500 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper500 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call500 ().invokeExact(o1, o2, o3); }

    static Object bootstrap500 (Object l, Object n, Object t) throws Throwable { return _mh[ 500 ].invokeExact(l, n, t); }

    // 501
    private static MethodType MT_bootstrap501 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap501 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap501", MT_bootstrap501 ());
    }

    private static MethodHandle INDY_call501;
    private static MethodHandle INDY_call501 () throws Throwable {
        if (INDY_call501 != null) return INDY_call501;
        CallSite cs = (CallSite) MH_bootstrap501 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap501 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper501 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call501 ().invokeExact(o1, o2, o3); }

    static Object bootstrap501 (Object l, Object n, Object t) throws Throwable { return _mh[ 501 ].invokeExact(l, n, t); }

    // 502
    private static MethodType MT_bootstrap502 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap502 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap502", MT_bootstrap502 ());
    }

    private static MethodHandle INDY_call502;
    private static MethodHandle INDY_call502 () throws Throwable {
        if (INDY_call502 != null) return INDY_call502;
        CallSite cs = (CallSite) MH_bootstrap502 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap502 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper502 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call502 ().invokeExact(o1, o2, o3); }

    static Object bootstrap502 (Object l, Object n, Object t) throws Throwable { return _mh[ 502 ].invokeExact(l, n, t); }

    // 503
    private static MethodType MT_bootstrap503 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap503 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap503", MT_bootstrap503 ());
    }

    private static MethodHandle INDY_call503;
    private static MethodHandle INDY_call503 () throws Throwable {
        if (INDY_call503 != null) return INDY_call503;
        CallSite cs = (CallSite) MH_bootstrap503 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap503 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper503 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call503 ().invokeExact(o1, o2, o3); }

    static Object bootstrap503 (Object l, Object n, Object t) throws Throwable { return _mh[ 503 ].invokeExact(l, n, t); }

    // 504
    private static MethodType MT_bootstrap504 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap504 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap504", MT_bootstrap504 ());
    }

    private static MethodHandle INDY_call504;
    private static MethodHandle INDY_call504 () throws Throwable {
        if (INDY_call504 != null) return INDY_call504;
        CallSite cs = (CallSite) MH_bootstrap504 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap504 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper504 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call504 ().invokeExact(o1, o2, o3); }

    static Object bootstrap504 (Object l, Object n, Object t) throws Throwable { return _mh[ 504 ].invokeExact(l, n, t); }

    // 505
    private static MethodType MT_bootstrap505 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap505 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap505", MT_bootstrap505 ());
    }

    private static MethodHandle INDY_call505;
    private static MethodHandle INDY_call505 () throws Throwable {
        if (INDY_call505 != null) return INDY_call505;
        CallSite cs = (CallSite) MH_bootstrap505 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap505 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper505 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call505 ().invokeExact(o1, o2, o3); }

    static Object bootstrap505 (Object l, Object n, Object t) throws Throwable { return _mh[ 505 ].invokeExact(l, n, t); }

    // 506
    private static MethodType MT_bootstrap506 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap506 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap506", MT_bootstrap506 ());
    }

    private static MethodHandle INDY_call506;
    private static MethodHandle INDY_call506 () throws Throwable {
        if (INDY_call506 != null) return INDY_call506;
        CallSite cs = (CallSite) MH_bootstrap506 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap506 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper506 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call506 ().invokeExact(o1, o2, o3); }

    static Object bootstrap506 (Object l, Object n, Object t) throws Throwable { return _mh[ 506 ].invokeExact(l, n, t); }

    // 507
    private static MethodType MT_bootstrap507 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap507 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap507", MT_bootstrap507 ());
    }

    private static MethodHandle INDY_call507;
    private static MethodHandle INDY_call507 () throws Throwable {
        if (INDY_call507 != null) return INDY_call507;
        CallSite cs = (CallSite) MH_bootstrap507 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap507 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper507 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call507 ().invokeExact(o1, o2, o3); }

    static Object bootstrap507 (Object l, Object n, Object t) throws Throwable { return _mh[ 507 ].invokeExact(l, n, t); }

    // 508
    private static MethodType MT_bootstrap508 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap508 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap508", MT_bootstrap508 ());
    }

    private static MethodHandle INDY_call508;
    private static MethodHandle INDY_call508 () throws Throwable {
        if (INDY_call508 != null) return INDY_call508;
        CallSite cs = (CallSite) MH_bootstrap508 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap508 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper508 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call508 ().invokeExact(o1, o2, o3); }

    static Object bootstrap508 (Object l, Object n, Object t) throws Throwable { return _mh[ 508 ].invokeExact(l, n, t); }

    // 509
    private static MethodType MT_bootstrap509 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap509 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap509", MT_bootstrap509 ());
    }

    private static MethodHandle INDY_call509;
    private static MethodHandle INDY_call509 () throws Throwable {
        if (INDY_call509 != null) return INDY_call509;
        CallSite cs = (CallSite) MH_bootstrap509 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap509 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper509 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call509 ().invokeExact(o1, o2, o3); }

    static Object bootstrap509 (Object l, Object n, Object t) throws Throwable { return _mh[ 509 ].invokeExact(l, n, t); }

    // 510
    private static MethodType MT_bootstrap510 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap510 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap510", MT_bootstrap510 ());
    }

    private static MethodHandle INDY_call510;
    private static MethodHandle INDY_call510 () throws Throwable {
        if (INDY_call510 != null) return INDY_call510;
        CallSite cs = (CallSite) MH_bootstrap510 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap510 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper510 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call510 ().invokeExact(o1, o2, o3); }

    static Object bootstrap510 (Object l, Object n, Object t) throws Throwable { return _mh[ 510 ].invokeExact(l, n, t); }

    // 511
    private static MethodType MT_bootstrap511 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap511 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap511", MT_bootstrap511 ());
    }

    private static MethodHandle INDY_call511;
    private static MethodHandle INDY_call511 () throws Throwable {
        if (INDY_call511 != null) return INDY_call511;
        CallSite cs = (CallSite) MH_bootstrap511 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap511 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper511 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call511 ().invokeExact(o1, o2, o3); }

    static Object bootstrap511 (Object l, Object n, Object t) throws Throwable { return _mh[ 511 ].invokeExact(l, n, t); }

    // 512
    private static MethodType MT_bootstrap512 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap512 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap512", MT_bootstrap512 ());
    }

    private static MethodHandle INDY_call512;
    private static MethodHandle INDY_call512 () throws Throwable {
        if (INDY_call512 != null) return INDY_call512;
        CallSite cs = (CallSite) MH_bootstrap512 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap512 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper512 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call512 ().invokeExact(o1, o2, o3); }

    static Object bootstrap512 (Object l, Object n, Object t) throws Throwable { return _mh[ 512 ].invokeExact(l, n, t); }

    // 513
    private static MethodType MT_bootstrap513 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap513 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap513", MT_bootstrap513 ());
    }

    private static MethodHandle INDY_call513;
    private static MethodHandle INDY_call513 () throws Throwable {
        if (INDY_call513 != null) return INDY_call513;
        CallSite cs = (CallSite) MH_bootstrap513 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap513 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper513 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call513 ().invokeExact(o1, o2, o3); }

    static Object bootstrap513 (Object l, Object n, Object t) throws Throwable { return _mh[ 513 ].invokeExact(l, n, t); }

    // 514
    private static MethodType MT_bootstrap514 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap514 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap514", MT_bootstrap514 ());
    }

    private static MethodHandle INDY_call514;
    private static MethodHandle INDY_call514 () throws Throwable {
        if (INDY_call514 != null) return INDY_call514;
        CallSite cs = (CallSite) MH_bootstrap514 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap514 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper514 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call514 ().invokeExact(o1, o2, o3); }

    static Object bootstrap514 (Object l, Object n, Object t) throws Throwable { return _mh[ 514 ].invokeExact(l, n, t); }

    // 515
    private static MethodType MT_bootstrap515 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap515 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap515", MT_bootstrap515 ());
    }

    private static MethodHandle INDY_call515;
    private static MethodHandle INDY_call515 () throws Throwable {
        if (INDY_call515 != null) return INDY_call515;
        CallSite cs = (CallSite) MH_bootstrap515 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap515 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper515 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call515 ().invokeExact(o1, o2, o3); }

    static Object bootstrap515 (Object l, Object n, Object t) throws Throwable { return _mh[ 515 ].invokeExact(l, n, t); }

    // 516
    private static MethodType MT_bootstrap516 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap516 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap516", MT_bootstrap516 ());
    }

    private static MethodHandle INDY_call516;
    private static MethodHandle INDY_call516 () throws Throwable {
        if (INDY_call516 != null) return INDY_call516;
        CallSite cs = (CallSite) MH_bootstrap516 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap516 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper516 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call516 ().invokeExact(o1, o2, o3); }

    static Object bootstrap516 (Object l, Object n, Object t) throws Throwable { return _mh[ 516 ].invokeExact(l, n, t); }

    // 517
    private static MethodType MT_bootstrap517 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap517 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap517", MT_bootstrap517 ());
    }

    private static MethodHandle INDY_call517;
    private static MethodHandle INDY_call517 () throws Throwable {
        if (INDY_call517 != null) return INDY_call517;
        CallSite cs = (CallSite) MH_bootstrap517 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap517 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper517 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call517 ().invokeExact(o1, o2, o3); }

    static Object bootstrap517 (Object l, Object n, Object t) throws Throwable { return _mh[ 517 ].invokeExact(l, n, t); }

    // 518
    private static MethodType MT_bootstrap518 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap518 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap518", MT_bootstrap518 ());
    }

    private static MethodHandle INDY_call518;
    private static MethodHandle INDY_call518 () throws Throwable {
        if (INDY_call518 != null) return INDY_call518;
        CallSite cs = (CallSite) MH_bootstrap518 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap518 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper518 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call518 ().invokeExact(o1, o2, o3); }

    static Object bootstrap518 (Object l, Object n, Object t) throws Throwable { return _mh[ 518 ].invokeExact(l, n, t); }

    // 519
    private static MethodType MT_bootstrap519 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap519 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap519", MT_bootstrap519 ());
    }

    private static MethodHandle INDY_call519;
    private static MethodHandle INDY_call519 () throws Throwable {
        if (INDY_call519 != null) return INDY_call519;
        CallSite cs = (CallSite) MH_bootstrap519 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap519 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper519 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call519 ().invokeExact(o1, o2, o3); }

    static Object bootstrap519 (Object l, Object n, Object t) throws Throwable { return _mh[ 519 ].invokeExact(l, n, t); }

    // 520
    private static MethodType MT_bootstrap520 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap520 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap520", MT_bootstrap520 ());
    }

    private static MethodHandle INDY_call520;
    private static MethodHandle INDY_call520 () throws Throwable {
        if (INDY_call520 != null) return INDY_call520;
        CallSite cs = (CallSite) MH_bootstrap520 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap520 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper520 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call520 ().invokeExact(o1, o2, o3); }

    static Object bootstrap520 (Object l, Object n, Object t) throws Throwable { return _mh[ 520 ].invokeExact(l, n, t); }

    // 521
    private static MethodType MT_bootstrap521 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap521 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap521", MT_bootstrap521 ());
    }

    private static MethodHandle INDY_call521;
    private static MethodHandle INDY_call521 () throws Throwable {
        if (INDY_call521 != null) return INDY_call521;
        CallSite cs = (CallSite) MH_bootstrap521 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap521 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper521 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call521 ().invokeExact(o1, o2, o3); }

    static Object bootstrap521 (Object l, Object n, Object t) throws Throwable { return _mh[ 521 ].invokeExact(l, n, t); }

    // 522
    private static MethodType MT_bootstrap522 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap522 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap522", MT_bootstrap522 ());
    }

    private static MethodHandle INDY_call522;
    private static MethodHandle INDY_call522 () throws Throwable {
        if (INDY_call522 != null) return INDY_call522;
        CallSite cs = (CallSite) MH_bootstrap522 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap522 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper522 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call522 ().invokeExact(o1, o2, o3); }

    static Object bootstrap522 (Object l, Object n, Object t) throws Throwable { return _mh[ 522 ].invokeExact(l, n, t); }

    // 523
    private static MethodType MT_bootstrap523 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap523 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap523", MT_bootstrap523 ());
    }

    private static MethodHandle INDY_call523;
    private static MethodHandle INDY_call523 () throws Throwable {
        if (INDY_call523 != null) return INDY_call523;
        CallSite cs = (CallSite) MH_bootstrap523 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap523 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper523 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call523 ().invokeExact(o1, o2, o3); }

    static Object bootstrap523 (Object l, Object n, Object t) throws Throwable { return _mh[ 523 ].invokeExact(l, n, t); }

    // 524
    private static MethodType MT_bootstrap524 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap524 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap524", MT_bootstrap524 ());
    }

    private static MethodHandle INDY_call524;
    private static MethodHandle INDY_call524 () throws Throwable {
        if (INDY_call524 != null) return INDY_call524;
        CallSite cs = (CallSite) MH_bootstrap524 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap524 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper524 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call524 ().invokeExact(o1, o2, o3); }

    static Object bootstrap524 (Object l, Object n, Object t) throws Throwable { return _mh[ 524 ].invokeExact(l, n, t); }

    // 525
    private static MethodType MT_bootstrap525 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap525 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap525", MT_bootstrap525 ());
    }

    private static MethodHandle INDY_call525;
    private static MethodHandle INDY_call525 () throws Throwable {
        if (INDY_call525 != null) return INDY_call525;
        CallSite cs = (CallSite) MH_bootstrap525 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap525 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper525 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call525 ().invokeExact(o1, o2, o3); }

    static Object bootstrap525 (Object l, Object n, Object t) throws Throwable { return _mh[ 525 ].invokeExact(l, n, t); }

    // 526
    private static MethodType MT_bootstrap526 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap526 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap526", MT_bootstrap526 ());
    }

    private static MethodHandle INDY_call526;
    private static MethodHandle INDY_call526 () throws Throwable {
        if (INDY_call526 != null) return INDY_call526;
        CallSite cs = (CallSite) MH_bootstrap526 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap526 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper526 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call526 ().invokeExact(o1, o2, o3); }

    static Object bootstrap526 (Object l, Object n, Object t) throws Throwable { return _mh[ 526 ].invokeExact(l, n, t); }

    // 527
    private static MethodType MT_bootstrap527 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap527 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap527", MT_bootstrap527 ());
    }

    private static MethodHandle INDY_call527;
    private static MethodHandle INDY_call527 () throws Throwable {
        if (INDY_call527 != null) return INDY_call527;
        CallSite cs = (CallSite) MH_bootstrap527 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap527 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper527 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call527 ().invokeExact(o1, o2, o3); }

    static Object bootstrap527 (Object l, Object n, Object t) throws Throwable { return _mh[ 527 ].invokeExact(l, n, t); }

    // 528
    private static MethodType MT_bootstrap528 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap528 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap528", MT_bootstrap528 ());
    }

    private static MethodHandle INDY_call528;
    private static MethodHandle INDY_call528 () throws Throwable {
        if (INDY_call528 != null) return INDY_call528;
        CallSite cs = (CallSite) MH_bootstrap528 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap528 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper528 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call528 ().invokeExact(o1, o2, o3); }

    static Object bootstrap528 (Object l, Object n, Object t) throws Throwable { return _mh[ 528 ].invokeExact(l, n, t); }

    // 529
    private static MethodType MT_bootstrap529 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap529 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap529", MT_bootstrap529 ());
    }

    private static MethodHandle INDY_call529;
    private static MethodHandle INDY_call529 () throws Throwable {
        if (INDY_call529 != null) return INDY_call529;
        CallSite cs = (CallSite) MH_bootstrap529 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap529 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper529 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call529 ().invokeExact(o1, o2, o3); }

    static Object bootstrap529 (Object l, Object n, Object t) throws Throwable { return _mh[ 529 ].invokeExact(l, n, t); }

    // 530
    private static MethodType MT_bootstrap530 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap530 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap530", MT_bootstrap530 ());
    }

    private static MethodHandle INDY_call530;
    private static MethodHandle INDY_call530 () throws Throwable {
        if (INDY_call530 != null) return INDY_call530;
        CallSite cs = (CallSite) MH_bootstrap530 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap530 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper530 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call530 ().invokeExact(o1, o2, o3); }

    static Object bootstrap530 (Object l, Object n, Object t) throws Throwable { return _mh[ 530 ].invokeExact(l, n, t); }

    // 531
    private static MethodType MT_bootstrap531 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap531 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap531", MT_bootstrap531 ());
    }

    private static MethodHandle INDY_call531;
    private static MethodHandle INDY_call531 () throws Throwable {
        if (INDY_call531 != null) return INDY_call531;
        CallSite cs = (CallSite) MH_bootstrap531 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap531 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper531 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call531 ().invokeExact(o1, o2, o3); }

    static Object bootstrap531 (Object l, Object n, Object t) throws Throwable { return _mh[ 531 ].invokeExact(l, n, t); }

    // 532
    private static MethodType MT_bootstrap532 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap532 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap532", MT_bootstrap532 ());
    }

    private static MethodHandle INDY_call532;
    private static MethodHandle INDY_call532 () throws Throwable {
        if (INDY_call532 != null) return INDY_call532;
        CallSite cs = (CallSite) MH_bootstrap532 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap532 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper532 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call532 ().invokeExact(o1, o2, o3); }

    static Object bootstrap532 (Object l, Object n, Object t) throws Throwable { return _mh[ 532 ].invokeExact(l, n, t); }

    // 533
    private static MethodType MT_bootstrap533 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap533 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap533", MT_bootstrap533 ());
    }

    private static MethodHandle INDY_call533;
    private static MethodHandle INDY_call533 () throws Throwable {
        if (INDY_call533 != null) return INDY_call533;
        CallSite cs = (CallSite) MH_bootstrap533 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap533 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper533 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call533 ().invokeExact(o1, o2, o3); }

    static Object bootstrap533 (Object l, Object n, Object t) throws Throwable { return _mh[ 533 ].invokeExact(l, n, t); }

    // 534
    private static MethodType MT_bootstrap534 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap534 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap534", MT_bootstrap534 ());
    }

    private static MethodHandle INDY_call534;
    private static MethodHandle INDY_call534 () throws Throwable {
        if (INDY_call534 != null) return INDY_call534;
        CallSite cs = (CallSite) MH_bootstrap534 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap534 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper534 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call534 ().invokeExact(o1, o2, o3); }

    static Object bootstrap534 (Object l, Object n, Object t) throws Throwable { return _mh[ 534 ].invokeExact(l, n, t); }

    // 535
    private static MethodType MT_bootstrap535 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap535 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap535", MT_bootstrap535 ());
    }

    private static MethodHandle INDY_call535;
    private static MethodHandle INDY_call535 () throws Throwable {
        if (INDY_call535 != null) return INDY_call535;
        CallSite cs = (CallSite) MH_bootstrap535 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap535 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper535 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call535 ().invokeExact(o1, o2, o3); }

    static Object bootstrap535 (Object l, Object n, Object t) throws Throwable { return _mh[ 535 ].invokeExact(l, n, t); }

    // 536
    private static MethodType MT_bootstrap536 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap536 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap536", MT_bootstrap536 ());
    }

    private static MethodHandle INDY_call536;
    private static MethodHandle INDY_call536 () throws Throwable {
        if (INDY_call536 != null) return INDY_call536;
        CallSite cs = (CallSite) MH_bootstrap536 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap536 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper536 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call536 ().invokeExact(o1, o2, o3); }

    static Object bootstrap536 (Object l, Object n, Object t) throws Throwable { return _mh[ 536 ].invokeExact(l, n, t); }

    // 537
    private static MethodType MT_bootstrap537 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap537 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap537", MT_bootstrap537 ());
    }

    private static MethodHandle INDY_call537;
    private static MethodHandle INDY_call537 () throws Throwable {
        if (INDY_call537 != null) return INDY_call537;
        CallSite cs = (CallSite) MH_bootstrap537 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap537 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper537 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call537 ().invokeExact(o1, o2, o3); }

    static Object bootstrap537 (Object l, Object n, Object t) throws Throwable { return _mh[ 537 ].invokeExact(l, n, t); }

    // 538
    private static MethodType MT_bootstrap538 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap538 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap538", MT_bootstrap538 ());
    }

    private static MethodHandle INDY_call538;
    private static MethodHandle INDY_call538 () throws Throwable {
        if (INDY_call538 != null) return INDY_call538;
        CallSite cs = (CallSite) MH_bootstrap538 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap538 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper538 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call538 ().invokeExact(o1, o2, o3); }

    static Object bootstrap538 (Object l, Object n, Object t) throws Throwable { return _mh[ 538 ].invokeExact(l, n, t); }

    // 539
    private static MethodType MT_bootstrap539 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap539 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap539", MT_bootstrap539 ());
    }

    private static MethodHandle INDY_call539;
    private static MethodHandle INDY_call539 () throws Throwable {
        if (INDY_call539 != null) return INDY_call539;
        CallSite cs = (CallSite) MH_bootstrap539 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap539 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper539 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call539 ().invokeExact(o1, o2, o3); }

    static Object bootstrap539 (Object l, Object n, Object t) throws Throwable { return _mh[ 539 ].invokeExact(l, n, t); }

    // 540
    private static MethodType MT_bootstrap540 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap540 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap540", MT_bootstrap540 ());
    }

    private static MethodHandle INDY_call540;
    private static MethodHandle INDY_call540 () throws Throwable {
        if (INDY_call540 != null) return INDY_call540;
        CallSite cs = (CallSite) MH_bootstrap540 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap540 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper540 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call540 ().invokeExact(o1, o2, o3); }

    static Object bootstrap540 (Object l, Object n, Object t) throws Throwable { return _mh[ 540 ].invokeExact(l, n, t); }

    // 541
    private static MethodType MT_bootstrap541 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap541 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap541", MT_bootstrap541 ());
    }

    private static MethodHandle INDY_call541;
    private static MethodHandle INDY_call541 () throws Throwable {
        if (INDY_call541 != null) return INDY_call541;
        CallSite cs = (CallSite) MH_bootstrap541 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap541 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper541 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call541 ().invokeExact(o1, o2, o3); }

    static Object bootstrap541 (Object l, Object n, Object t) throws Throwable { return _mh[ 541 ].invokeExact(l, n, t); }

    // 542
    private static MethodType MT_bootstrap542 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap542 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap542", MT_bootstrap542 ());
    }

    private static MethodHandle INDY_call542;
    private static MethodHandle INDY_call542 () throws Throwable {
        if (INDY_call542 != null) return INDY_call542;
        CallSite cs = (CallSite) MH_bootstrap542 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap542 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper542 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call542 ().invokeExact(o1, o2, o3); }

    static Object bootstrap542 (Object l, Object n, Object t) throws Throwable { return _mh[ 542 ].invokeExact(l, n, t); }

    // 543
    private static MethodType MT_bootstrap543 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap543 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap543", MT_bootstrap543 ());
    }

    private static MethodHandle INDY_call543;
    private static MethodHandle INDY_call543 () throws Throwable {
        if (INDY_call543 != null) return INDY_call543;
        CallSite cs = (CallSite) MH_bootstrap543 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap543 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper543 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call543 ().invokeExact(o1, o2, o3); }

    static Object bootstrap543 (Object l, Object n, Object t) throws Throwable { return _mh[ 543 ].invokeExact(l, n, t); }

    // 544
    private static MethodType MT_bootstrap544 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap544 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap544", MT_bootstrap544 ());
    }

    private static MethodHandle INDY_call544;
    private static MethodHandle INDY_call544 () throws Throwable {
        if (INDY_call544 != null) return INDY_call544;
        CallSite cs = (CallSite) MH_bootstrap544 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap544 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper544 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call544 ().invokeExact(o1, o2, o3); }

    static Object bootstrap544 (Object l, Object n, Object t) throws Throwable { return _mh[ 544 ].invokeExact(l, n, t); }

    // 545
    private static MethodType MT_bootstrap545 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap545 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap545", MT_bootstrap545 ());
    }

    private static MethodHandle INDY_call545;
    private static MethodHandle INDY_call545 () throws Throwable {
        if (INDY_call545 != null) return INDY_call545;
        CallSite cs = (CallSite) MH_bootstrap545 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap545 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper545 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call545 ().invokeExact(o1, o2, o3); }

    static Object bootstrap545 (Object l, Object n, Object t) throws Throwable { return _mh[ 545 ].invokeExact(l, n, t); }

    // 546
    private static MethodType MT_bootstrap546 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap546 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap546", MT_bootstrap546 ());
    }

    private static MethodHandle INDY_call546;
    private static MethodHandle INDY_call546 () throws Throwable {
        if (INDY_call546 != null) return INDY_call546;
        CallSite cs = (CallSite) MH_bootstrap546 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap546 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper546 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call546 ().invokeExact(o1, o2, o3); }

    static Object bootstrap546 (Object l, Object n, Object t) throws Throwable { return _mh[ 546 ].invokeExact(l, n, t); }

    // 547
    private static MethodType MT_bootstrap547 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap547 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap547", MT_bootstrap547 ());
    }

    private static MethodHandle INDY_call547;
    private static MethodHandle INDY_call547 () throws Throwable {
        if (INDY_call547 != null) return INDY_call547;
        CallSite cs = (CallSite) MH_bootstrap547 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap547 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper547 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call547 ().invokeExact(o1, o2, o3); }

    static Object bootstrap547 (Object l, Object n, Object t) throws Throwable { return _mh[ 547 ].invokeExact(l, n, t); }

    // 548
    private static MethodType MT_bootstrap548 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap548 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap548", MT_bootstrap548 ());
    }

    private static MethodHandle INDY_call548;
    private static MethodHandle INDY_call548 () throws Throwable {
        if (INDY_call548 != null) return INDY_call548;
        CallSite cs = (CallSite) MH_bootstrap548 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap548 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper548 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call548 ().invokeExact(o1, o2, o3); }

    static Object bootstrap548 (Object l, Object n, Object t) throws Throwable { return _mh[ 548 ].invokeExact(l, n, t); }

    // 549
    private static MethodType MT_bootstrap549 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap549 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap549", MT_bootstrap549 ());
    }

    private static MethodHandle INDY_call549;
    private static MethodHandle INDY_call549 () throws Throwable {
        if (INDY_call549 != null) return INDY_call549;
        CallSite cs = (CallSite) MH_bootstrap549 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap549 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper549 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call549 ().invokeExact(o1, o2, o3); }

    static Object bootstrap549 (Object l, Object n, Object t) throws Throwable { return _mh[ 549 ].invokeExact(l, n, t); }

    // 550
    private static MethodType MT_bootstrap550 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap550 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap550", MT_bootstrap550 ());
    }

    private static MethodHandle INDY_call550;
    private static MethodHandle INDY_call550 () throws Throwable {
        if (INDY_call550 != null) return INDY_call550;
        CallSite cs = (CallSite) MH_bootstrap550 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap550 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper550 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call550 ().invokeExact(o1, o2, o3); }

    static Object bootstrap550 (Object l, Object n, Object t) throws Throwable { return _mh[ 550 ].invokeExact(l, n, t); }

    // 551
    private static MethodType MT_bootstrap551 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap551 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap551", MT_bootstrap551 ());
    }

    private static MethodHandle INDY_call551;
    private static MethodHandle INDY_call551 () throws Throwable {
        if (INDY_call551 != null) return INDY_call551;
        CallSite cs = (CallSite) MH_bootstrap551 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap551 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper551 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call551 ().invokeExact(o1, o2, o3); }

    static Object bootstrap551 (Object l, Object n, Object t) throws Throwable { return _mh[ 551 ].invokeExact(l, n, t); }

    // 552
    private static MethodType MT_bootstrap552 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap552 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap552", MT_bootstrap552 ());
    }

    private static MethodHandle INDY_call552;
    private static MethodHandle INDY_call552 () throws Throwable {
        if (INDY_call552 != null) return INDY_call552;
        CallSite cs = (CallSite) MH_bootstrap552 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap552 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper552 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call552 ().invokeExact(o1, o2, o3); }

    static Object bootstrap552 (Object l, Object n, Object t) throws Throwable { return _mh[ 552 ].invokeExact(l, n, t); }

    // 553
    private static MethodType MT_bootstrap553 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap553 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap553", MT_bootstrap553 ());
    }

    private static MethodHandle INDY_call553;
    private static MethodHandle INDY_call553 () throws Throwable {
        if (INDY_call553 != null) return INDY_call553;
        CallSite cs = (CallSite) MH_bootstrap553 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap553 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper553 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call553 ().invokeExact(o1, o2, o3); }

    static Object bootstrap553 (Object l, Object n, Object t) throws Throwable { return _mh[ 553 ].invokeExact(l, n, t); }

    // 554
    private static MethodType MT_bootstrap554 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap554 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap554", MT_bootstrap554 ());
    }

    private static MethodHandle INDY_call554;
    private static MethodHandle INDY_call554 () throws Throwable {
        if (INDY_call554 != null) return INDY_call554;
        CallSite cs = (CallSite) MH_bootstrap554 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap554 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper554 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call554 ().invokeExact(o1, o2, o3); }

    static Object bootstrap554 (Object l, Object n, Object t) throws Throwable { return _mh[ 554 ].invokeExact(l, n, t); }

    // 555
    private static MethodType MT_bootstrap555 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap555 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap555", MT_bootstrap555 ());
    }

    private static MethodHandle INDY_call555;
    private static MethodHandle INDY_call555 () throws Throwable {
        if (INDY_call555 != null) return INDY_call555;
        CallSite cs = (CallSite) MH_bootstrap555 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap555 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper555 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call555 ().invokeExact(o1, o2, o3); }

    static Object bootstrap555 (Object l, Object n, Object t) throws Throwable { return _mh[ 555 ].invokeExact(l, n, t); }

    // 556
    private static MethodType MT_bootstrap556 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap556 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap556", MT_bootstrap556 ());
    }

    private static MethodHandle INDY_call556;
    private static MethodHandle INDY_call556 () throws Throwable {
        if (INDY_call556 != null) return INDY_call556;
        CallSite cs = (CallSite) MH_bootstrap556 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap556 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper556 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call556 ().invokeExact(o1, o2, o3); }

    static Object bootstrap556 (Object l, Object n, Object t) throws Throwable { return _mh[ 556 ].invokeExact(l, n, t); }

    // 557
    private static MethodType MT_bootstrap557 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap557 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap557", MT_bootstrap557 ());
    }

    private static MethodHandle INDY_call557;
    private static MethodHandle INDY_call557 () throws Throwable {
        if (INDY_call557 != null) return INDY_call557;
        CallSite cs = (CallSite) MH_bootstrap557 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap557 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper557 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call557 ().invokeExact(o1, o2, o3); }

    static Object bootstrap557 (Object l, Object n, Object t) throws Throwable { return _mh[ 557 ].invokeExact(l, n, t); }

    // 558
    private static MethodType MT_bootstrap558 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap558 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap558", MT_bootstrap558 ());
    }

    private static MethodHandle INDY_call558;
    private static MethodHandle INDY_call558 () throws Throwable {
        if (INDY_call558 != null) return INDY_call558;
        CallSite cs = (CallSite) MH_bootstrap558 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap558 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper558 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call558 ().invokeExact(o1, o2, o3); }

    static Object bootstrap558 (Object l, Object n, Object t) throws Throwable { return _mh[ 558 ].invokeExact(l, n, t); }

    // 559
    private static MethodType MT_bootstrap559 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap559 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap559", MT_bootstrap559 ());
    }

    private static MethodHandle INDY_call559;
    private static MethodHandle INDY_call559 () throws Throwable {
        if (INDY_call559 != null) return INDY_call559;
        CallSite cs = (CallSite) MH_bootstrap559 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap559 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper559 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call559 ().invokeExact(o1, o2, o3); }

    static Object bootstrap559 (Object l, Object n, Object t) throws Throwable { return _mh[ 559 ].invokeExact(l, n, t); }

    // 560
    private static MethodType MT_bootstrap560 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap560 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap560", MT_bootstrap560 ());
    }

    private static MethodHandle INDY_call560;
    private static MethodHandle INDY_call560 () throws Throwable {
        if (INDY_call560 != null) return INDY_call560;
        CallSite cs = (CallSite) MH_bootstrap560 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap560 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper560 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call560 ().invokeExact(o1, o2, o3); }

    static Object bootstrap560 (Object l, Object n, Object t) throws Throwable { return _mh[ 560 ].invokeExact(l, n, t); }

    // 561
    private static MethodType MT_bootstrap561 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap561 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap561", MT_bootstrap561 ());
    }

    private static MethodHandle INDY_call561;
    private static MethodHandle INDY_call561 () throws Throwable {
        if (INDY_call561 != null) return INDY_call561;
        CallSite cs = (CallSite) MH_bootstrap561 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap561 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper561 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call561 ().invokeExact(o1, o2, o3); }

    static Object bootstrap561 (Object l, Object n, Object t) throws Throwable { return _mh[ 561 ].invokeExact(l, n, t); }

    // 562
    private static MethodType MT_bootstrap562 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap562 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap562", MT_bootstrap562 ());
    }

    private static MethodHandle INDY_call562;
    private static MethodHandle INDY_call562 () throws Throwable {
        if (INDY_call562 != null) return INDY_call562;
        CallSite cs = (CallSite) MH_bootstrap562 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap562 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper562 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call562 ().invokeExact(o1, o2, o3); }

    static Object bootstrap562 (Object l, Object n, Object t) throws Throwable { return _mh[ 562 ].invokeExact(l, n, t); }

    // 563
    private static MethodType MT_bootstrap563 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap563 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap563", MT_bootstrap563 ());
    }

    private static MethodHandle INDY_call563;
    private static MethodHandle INDY_call563 () throws Throwable {
        if (INDY_call563 != null) return INDY_call563;
        CallSite cs = (CallSite) MH_bootstrap563 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap563 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper563 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call563 ().invokeExact(o1, o2, o3); }

    static Object bootstrap563 (Object l, Object n, Object t) throws Throwable { return _mh[ 563 ].invokeExact(l, n, t); }

    // 564
    private static MethodType MT_bootstrap564 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap564 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap564", MT_bootstrap564 ());
    }

    private static MethodHandle INDY_call564;
    private static MethodHandle INDY_call564 () throws Throwable {
        if (INDY_call564 != null) return INDY_call564;
        CallSite cs = (CallSite) MH_bootstrap564 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap564 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper564 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call564 ().invokeExact(o1, o2, o3); }

    static Object bootstrap564 (Object l, Object n, Object t) throws Throwable { return _mh[ 564 ].invokeExact(l, n, t); }

    // 565
    private static MethodType MT_bootstrap565 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap565 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap565", MT_bootstrap565 ());
    }

    private static MethodHandle INDY_call565;
    private static MethodHandle INDY_call565 () throws Throwable {
        if (INDY_call565 != null) return INDY_call565;
        CallSite cs = (CallSite) MH_bootstrap565 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap565 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper565 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call565 ().invokeExact(o1, o2, o3); }

    static Object bootstrap565 (Object l, Object n, Object t) throws Throwable { return _mh[ 565 ].invokeExact(l, n, t); }

    // 566
    private static MethodType MT_bootstrap566 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap566 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap566", MT_bootstrap566 ());
    }

    private static MethodHandle INDY_call566;
    private static MethodHandle INDY_call566 () throws Throwable {
        if (INDY_call566 != null) return INDY_call566;
        CallSite cs = (CallSite) MH_bootstrap566 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap566 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper566 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call566 ().invokeExact(o1, o2, o3); }

    static Object bootstrap566 (Object l, Object n, Object t) throws Throwable { return _mh[ 566 ].invokeExact(l, n, t); }

    // 567
    private static MethodType MT_bootstrap567 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap567 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap567", MT_bootstrap567 ());
    }

    private static MethodHandle INDY_call567;
    private static MethodHandle INDY_call567 () throws Throwable {
        if (INDY_call567 != null) return INDY_call567;
        CallSite cs = (CallSite) MH_bootstrap567 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap567 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper567 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call567 ().invokeExact(o1, o2, o3); }

    static Object bootstrap567 (Object l, Object n, Object t) throws Throwable { return _mh[ 567 ].invokeExact(l, n, t); }

    // 568
    private static MethodType MT_bootstrap568 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap568 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap568", MT_bootstrap568 ());
    }

    private static MethodHandle INDY_call568;
    private static MethodHandle INDY_call568 () throws Throwable {
        if (INDY_call568 != null) return INDY_call568;
        CallSite cs = (CallSite) MH_bootstrap568 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap568 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper568 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call568 ().invokeExact(o1, o2, o3); }

    static Object bootstrap568 (Object l, Object n, Object t) throws Throwable { return _mh[ 568 ].invokeExact(l, n, t); }

    // 569
    private static MethodType MT_bootstrap569 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap569 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap569", MT_bootstrap569 ());
    }

    private static MethodHandle INDY_call569;
    private static MethodHandle INDY_call569 () throws Throwable {
        if (INDY_call569 != null) return INDY_call569;
        CallSite cs = (CallSite) MH_bootstrap569 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap569 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper569 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call569 ().invokeExact(o1, o2, o3); }

    static Object bootstrap569 (Object l, Object n, Object t) throws Throwable { return _mh[ 569 ].invokeExact(l, n, t); }

    // 570
    private static MethodType MT_bootstrap570 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap570 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap570", MT_bootstrap570 ());
    }

    private static MethodHandle INDY_call570;
    private static MethodHandle INDY_call570 () throws Throwable {
        if (INDY_call570 != null) return INDY_call570;
        CallSite cs = (CallSite) MH_bootstrap570 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap570 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper570 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call570 ().invokeExact(o1, o2, o3); }

    static Object bootstrap570 (Object l, Object n, Object t) throws Throwable { return _mh[ 570 ].invokeExact(l, n, t); }

    // 571
    private static MethodType MT_bootstrap571 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap571 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap571", MT_bootstrap571 ());
    }

    private static MethodHandle INDY_call571;
    private static MethodHandle INDY_call571 () throws Throwable {
        if (INDY_call571 != null) return INDY_call571;
        CallSite cs = (CallSite) MH_bootstrap571 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap571 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper571 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call571 ().invokeExact(o1, o2, o3); }

    static Object bootstrap571 (Object l, Object n, Object t) throws Throwable { return _mh[ 571 ].invokeExact(l, n, t); }

    // 572
    private static MethodType MT_bootstrap572 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap572 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap572", MT_bootstrap572 ());
    }

    private static MethodHandle INDY_call572;
    private static MethodHandle INDY_call572 () throws Throwable {
        if (INDY_call572 != null) return INDY_call572;
        CallSite cs = (CallSite) MH_bootstrap572 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap572 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper572 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call572 ().invokeExact(o1, o2, o3); }

    static Object bootstrap572 (Object l, Object n, Object t) throws Throwable { return _mh[ 572 ].invokeExact(l, n, t); }

    // 573
    private static MethodType MT_bootstrap573 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap573 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap573", MT_bootstrap573 ());
    }

    private static MethodHandle INDY_call573;
    private static MethodHandle INDY_call573 () throws Throwable {
        if (INDY_call573 != null) return INDY_call573;
        CallSite cs = (CallSite) MH_bootstrap573 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap573 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper573 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call573 ().invokeExact(o1, o2, o3); }

    static Object bootstrap573 (Object l, Object n, Object t) throws Throwable { return _mh[ 573 ].invokeExact(l, n, t); }

    // 574
    private static MethodType MT_bootstrap574 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap574 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap574", MT_bootstrap574 ());
    }

    private static MethodHandle INDY_call574;
    private static MethodHandle INDY_call574 () throws Throwable {
        if (INDY_call574 != null) return INDY_call574;
        CallSite cs = (CallSite) MH_bootstrap574 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap574 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper574 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call574 ().invokeExact(o1, o2, o3); }

    static Object bootstrap574 (Object l, Object n, Object t) throws Throwable { return _mh[ 574 ].invokeExact(l, n, t); }

    // 575
    private static MethodType MT_bootstrap575 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap575 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap575", MT_bootstrap575 ());
    }

    private static MethodHandle INDY_call575;
    private static MethodHandle INDY_call575 () throws Throwable {
        if (INDY_call575 != null) return INDY_call575;
        CallSite cs = (CallSite) MH_bootstrap575 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap575 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper575 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call575 ().invokeExact(o1, o2, o3); }

    static Object bootstrap575 (Object l, Object n, Object t) throws Throwable { return _mh[ 575 ].invokeExact(l, n, t); }

    // 576
    private static MethodType MT_bootstrap576 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap576 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap576", MT_bootstrap576 ());
    }

    private static MethodHandle INDY_call576;
    private static MethodHandle INDY_call576 () throws Throwable {
        if (INDY_call576 != null) return INDY_call576;
        CallSite cs = (CallSite) MH_bootstrap576 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap576 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper576 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call576 ().invokeExact(o1, o2, o3); }

    static Object bootstrap576 (Object l, Object n, Object t) throws Throwable { return _mh[ 576 ].invokeExact(l, n, t); }

    // 577
    private static MethodType MT_bootstrap577 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap577 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap577", MT_bootstrap577 ());
    }

    private static MethodHandle INDY_call577;
    private static MethodHandle INDY_call577 () throws Throwable {
        if (INDY_call577 != null) return INDY_call577;
        CallSite cs = (CallSite) MH_bootstrap577 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap577 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper577 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call577 ().invokeExact(o1, o2, o3); }

    static Object bootstrap577 (Object l, Object n, Object t) throws Throwable { return _mh[ 577 ].invokeExact(l, n, t); }

    // 578
    private static MethodType MT_bootstrap578 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap578 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap578", MT_bootstrap578 ());
    }

    private static MethodHandle INDY_call578;
    private static MethodHandle INDY_call578 () throws Throwable {
        if (INDY_call578 != null) return INDY_call578;
        CallSite cs = (CallSite) MH_bootstrap578 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap578 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper578 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call578 ().invokeExact(o1, o2, o3); }

    static Object bootstrap578 (Object l, Object n, Object t) throws Throwable { return _mh[ 578 ].invokeExact(l, n, t); }

    // 579
    private static MethodType MT_bootstrap579 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap579 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap579", MT_bootstrap579 ());
    }

    private static MethodHandle INDY_call579;
    private static MethodHandle INDY_call579 () throws Throwable {
        if (INDY_call579 != null) return INDY_call579;
        CallSite cs = (CallSite) MH_bootstrap579 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap579 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper579 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call579 ().invokeExact(o1, o2, o3); }

    static Object bootstrap579 (Object l, Object n, Object t) throws Throwable { return _mh[ 579 ].invokeExact(l, n, t); }

    // 580
    private static MethodType MT_bootstrap580 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap580 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap580", MT_bootstrap580 ());
    }

    private static MethodHandle INDY_call580;
    private static MethodHandle INDY_call580 () throws Throwable {
        if (INDY_call580 != null) return INDY_call580;
        CallSite cs = (CallSite) MH_bootstrap580 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap580 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper580 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call580 ().invokeExact(o1, o2, o3); }

    static Object bootstrap580 (Object l, Object n, Object t) throws Throwable { return _mh[ 580 ].invokeExact(l, n, t); }

    // 581
    private static MethodType MT_bootstrap581 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap581 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap581", MT_bootstrap581 ());
    }

    private static MethodHandle INDY_call581;
    private static MethodHandle INDY_call581 () throws Throwable {
        if (INDY_call581 != null) return INDY_call581;
        CallSite cs = (CallSite) MH_bootstrap581 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap581 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper581 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call581 ().invokeExact(o1, o2, o3); }

    static Object bootstrap581 (Object l, Object n, Object t) throws Throwable { return _mh[ 581 ].invokeExact(l, n, t); }

    // 582
    private static MethodType MT_bootstrap582 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap582 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap582", MT_bootstrap582 ());
    }

    private static MethodHandle INDY_call582;
    private static MethodHandle INDY_call582 () throws Throwable {
        if (INDY_call582 != null) return INDY_call582;
        CallSite cs = (CallSite) MH_bootstrap582 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap582 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper582 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call582 ().invokeExact(o1, o2, o3); }

    static Object bootstrap582 (Object l, Object n, Object t) throws Throwable { return _mh[ 582 ].invokeExact(l, n, t); }

    // 583
    private static MethodType MT_bootstrap583 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap583 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap583", MT_bootstrap583 ());
    }

    private static MethodHandle INDY_call583;
    private static MethodHandle INDY_call583 () throws Throwable {
        if (INDY_call583 != null) return INDY_call583;
        CallSite cs = (CallSite) MH_bootstrap583 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap583 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper583 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call583 ().invokeExact(o1, o2, o3); }

    static Object bootstrap583 (Object l, Object n, Object t) throws Throwable { return _mh[ 583 ].invokeExact(l, n, t); }

    // 584
    private static MethodType MT_bootstrap584 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap584 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap584", MT_bootstrap584 ());
    }

    private static MethodHandle INDY_call584;
    private static MethodHandle INDY_call584 () throws Throwable {
        if (INDY_call584 != null) return INDY_call584;
        CallSite cs = (CallSite) MH_bootstrap584 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap584 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper584 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call584 ().invokeExact(o1, o2, o3); }

    static Object bootstrap584 (Object l, Object n, Object t) throws Throwable { return _mh[ 584 ].invokeExact(l, n, t); }

    // 585
    private static MethodType MT_bootstrap585 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap585 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap585", MT_bootstrap585 ());
    }

    private static MethodHandle INDY_call585;
    private static MethodHandle INDY_call585 () throws Throwable {
        if (INDY_call585 != null) return INDY_call585;
        CallSite cs = (CallSite) MH_bootstrap585 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap585 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper585 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call585 ().invokeExact(o1, o2, o3); }

    static Object bootstrap585 (Object l, Object n, Object t) throws Throwable { return _mh[ 585 ].invokeExact(l, n, t); }

    // 586
    private static MethodType MT_bootstrap586 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap586 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap586", MT_bootstrap586 ());
    }

    private static MethodHandle INDY_call586;
    private static MethodHandle INDY_call586 () throws Throwable {
        if (INDY_call586 != null) return INDY_call586;
        CallSite cs = (CallSite) MH_bootstrap586 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap586 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper586 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call586 ().invokeExact(o1, o2, o3); }

    static Object bootstrap586 (Object l, Object n, Object t) throws Throwable { return _mh[ 586 ].invokeExact(l, n, t); }

    // 587
    private static MethodType MT_bootstrap587 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap587 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap587", MT_bootstrap587 ());
    }

    private static MethodHandle INDY_call587;
    private static MethodHandle INDY_call587 () throws Throwable {
        if (INDY_call587 != null) return INDY_call587;
        CallSite cs = (CallSite) MH_bootstrap587 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap587 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper587 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call587 ().invokeExact(o1, o2, o3); }

    static Object bootstrap587 (Object l, Object n, Object t) throws Throwable { return _mh[ 587 ].invokeExact(l, n, t); }

    // 588
    private static MethodType MT_bootstrap588 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap588 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap588", MT_bootstrap588 ());
    }

    private static MethodHandle INDY_call588;
    private static MethodHandle INDY_call588 () throws Throwable {
        if (INDY_call588 != null) return INDY_call588;
        CallSite cs = (CallSite) MH_bootstrap588 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap588 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper588 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call588 ().invokeExact(o1, o2, o3); }

    static Object bootstrap588 (Object l, Object n, Object t) throws Throwable { return _mh[ 588 ].invokeExact(l, n, t); }

    // 589
    private static MethodType MT_bootstrap589 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap589 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap589", MT_bootstrap589 ());
    }

    private static MethodHandle INDY_call589;
    private static MethodHandle INDY_call589 () throws Throwable {
        if (INDY_call589 != null) return INDY_call589;
        CallSite cs = (CallSite) MH_bootstrap589 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap589 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper589 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call589 ().invokeExact(o1, o2, o3); }

    static Object bootstrap589 (Object l, Object n, Object t) throws Throwable { return _mh[ 589 ].invokeExact(l, n, t); }

    // 590
    private static MethodType MT_bootstrap590 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap590 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap590", MT_bootstrap590 ());
    }

    private static MethodHandle INDY_call590;
    private static MethodHandle INDY_call590 () throws Throwable {
        if (INDY_call590 != null) return INDY_call590;
        CallSite cs = (CallSite) MH_bootstrap590 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap590 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper590 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call590 ().invokeExact(o1, o2, o3); }

    static Object bootstrap590 (Object l, Object n, Object t) throws Throwable { return _mh[ 590 ].invokeExact(l, n, t); }

    // 591
    private static MethodType MT_bootstrap591 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap591 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap591", MT_bootstrap591 ());
    }

    private static MethodHandle INDY_call591;
    private static MethodHandle INDY_call591 () throws Throwable {
        if (INDY_call591 != null) return INDY_call591;
        CallSite cs = (CallSite) MH_bootstrap591 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap591 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper591 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call591 ().invokeExact(o1, o2, o3); }

    static Object bootstrap591 (Object l, Object n, Object t) throws Throwable { return _mh[ 591 ].invokeExact(l, n, t); }

    // 592
    private static MethodType MT_bootstrap592 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap592 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap592", MT_bootstrap592 ());
    }

    private static MethodHandle INDY_call592;
    private static MethodHandle INDY_call592 () throws Throwable {
        if (INDY_call592 != null) return INDY_call592;
        CallSite cs = (CallSite) MH_bootstrap592 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap592 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper592 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call592 ().invokeExact(o1, o2, o3); }

    static Object bootstrap592 (Object l, Object n, Object t) throws Throwable { return _mh[ 592 ].invokeExact(l, n, t); }

    // 593
    private static MethodType MT_bootstrap593 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap593 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap593", MT_bootstrap593 ());
    }

    private static MethodHandle INDY_call593;
    private static MethodHandle INDY_call593 () throws Throwable {
        if (INDY_call593 != null) return INDY_call593;
        CallSite cs = (CallSite) MH_bootstrap593 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap593 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper593 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call593 ().invokeExact(o1, o2, o3); }

    static Object bootstrap593 (Object l, Object n, Object t) throws Throwable { return _mh[ 593 ].invokeExact(l, n, t); }

    // 594
    private static MethodType MT_bootstrap594 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap594 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap594", MT_bootstrap594 ());
    }

    private static MethodHandle INDY_call594;
    private static MethodHandle INDY_call594 () throws Throwable {
        if (INDY_call594 != null) return INDY_call594;
        CallSite cs = (CallSite) MH_bootstrap594 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap594 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper594 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call594 ().invokeExact(o1, o2, o3); }

    static Object bootstrap594 (Object l, Object n, Object t) throws Throwable { return _mh[ 594 ].invokeExact(l, n, t); }

    // 595
    private static MethodType MT_bootstrap595 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap595 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap595", MT_bootstrap595 ());
    }

    private static MethodHandle INDY_call595;
    private static MethodHandle INDY_call595 () throws Throwable {
        if (INDY_call595 != null) return INDY_call595;
        CallSite cs = (CallSite) MH_bootstrap595 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap595 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper595 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call595 ().invokeExact(o1, o2, o3); }

    static Object bootstrap595 (Object l, Object n, Object t) throws Throwable { return _mh[ 595 ].invokeExact(l, n, t); }

    // 596
    private static MethodType MT_bootstrap596 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap596 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap596", MT_bootstrap596 ());
    }

    private static MethodHandle INDY_call596;
    private static MethodHandle INDY_call596 () throws Throwable {
        if (INDY_call596 != null) return INDY_call596;
        CallSite cs = (CallSite) MH_bootstrap596 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap596 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper596 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call596 ().invokeExact(o1, o2, o3); }

    static Object bootstrap596 (Object l, Object n, Object t) throws Throwable { return _mh[ 596 ].invokeExact(l, n, t); }

    // 597
    private static MethodType MT_bootstrap597 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap597 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap597", MT_bootstrap597 ());
    }

    private static MethodHandle INDY_call597;
    private static MethodHandle INDY_call597 () throws Throwable {
        if (INDY_call597 != null) return INDY_call597;
        CallSite cs = (CallSite) MH_bootstrap597 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap597 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper597 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call597 ().invokeExact(o1, o2, o3); }

    static Object bootstrap597 (Object l, Object n, Object t) throws Throwable { return _mh[ 597 ].invokeExact(l, n, t); }

    // 598
    private static MethodType MT_bootstrap598 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap598 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap598", MT_bootstrap598 ());
    }

    private static MethodHandle INDY_call598;
    private static MethodHandle INDY_call598 () throws Throwable {
        if (INDY_call598 != null) return INDY_call598;
        CallSite cs = (CallSite) MH_bootstrap598 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap598 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper598 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call598 ().invokeExact(o1, o2, o3); }

    static Object bootstrap598 (Object l, Object n, Object t) throws Throwable { return _mh[ 598 ].invokeExact(l, n, t); }

    // 599
    private static MethodType MT_bootstrap599 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap599 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap599", MT_bootstrap599 ());
    }

    private static MethodHandle INDY_call599;
    private static MethodHandle INDY_call599 () throws Throwable {
        if (INDY_call599 != null) return INDY_call599;
        CallSite cs = (CallSite) MH_bootstrap599 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap599 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper599 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call599 ().invokeExact(o1, o2, o3); }

    static Object bootstrap599 (Object l, Object n, Object t) throws Throwable { return _mh[ 599 ].invokeExact(l, n, t); }

    // 600
    private static MethodType MT_bootstrap600 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap600 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap600", MT_bootstrap600 ());
    }

    private static MethodHandle INDY_call600;
    private static MethodHandle INDY_call600 () throws Throwable {
        if (INDY_call600 != null) return INDY_call600;
        CallSite cs = (CallSite) MH_bootstrap600 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap600 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper600 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call600 ().invokeExact(o1, o2, o3); }

    static Object bootstrap600 (Object l, Object n, Object t) throws Throwable { return _mh[ 600 ].invokeExact(l, n, t); }

    // 601
    private static MethodType MT_bootstrap601 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap601 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap601", MT_bootstrap601 ());
    }

    private static MethodHandle INDY_call601;
    private static MethodHandle INDY_call601 () throws Throwable {
        if (INDY_call601 != null) return INDY_call601;
        CallSite cs = (CallSite) MH_bootstrap601 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap601 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper601 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call601 ().invokeExact(o1, o2, o3); }

    static Object bootstrap601 (Object l, Object n, Object t) throws Throwable { return _mh[ 601 ].invokeExact(l, n, t); }

    // 602
    private static MethodType MT_bootstrap602 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap602 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap602", MT_bootstrap602 ());
    }

    private static MethodHandle INDY_call602;
    private static MethodHandle INDY_call602 () throws Throwable {
        if (INDY_call602 != null) return INDY_call602;
        CallSite cs = (CallSite) MH_bootstrap602 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap602 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper602 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call602 ().invokeExact(o1, o2, o3); }

    static Object bootstrap602 (Object l, Object n, Object t) throws Throwable { return _mh[ 602 ].invokeExact(l, n, t); }

    // 603
    private static MethodType MT_bootstrap603 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap603 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap603", MT_bootstrap603 ());
    }

    private static MethodHandle INDY_call603;
    private static MethodHandle INDY_call603 () throws Throwable {
        if (INDY_call603 != null) return INDY_call603;
        CallSite cs = (CallSite) MH_bootstrap603 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap603 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper603 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call603 ().invokeExact(o1, o2, o3); }

    static Object bootstrap603 (Object l, Object n, Object t) throws Throwable { return _mh[ 603 ].invokeExact(l, n, t); }

    // 604
    private static MethodType MT_bootstrap604 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap604 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap604", MT_bootstrap604 ());
    }

    private static MethodHandle INDY_call604;
    private static MethodHandle INDY_call604 () throws Throwable {
        if (INDY_call604 != null) return INDY_call604;
        CallSite cs = (CallSite) MH_bootstrap604 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap604 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper604 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call604 ().invokeExact(o1, o2, o3); }

    static Object bootstrap604 (Object l, Object n, Object t) throws Throwable { return _mh[ 604 ].invokeExact(l, n, t); }

    // 605
    private static MethodType MT_bootstrap605 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap605 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap605", MT_bootstrap605 ());
    }

    private static MethodHandle INDY_call605;
    private static MethodHandle INDY_call605 () throws Throwable {
        if (INDY_call605 != null) return INDY_call605;
        CallSite cs = (CallSite) MH_bootstrap605 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap605 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper605 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call605 ().invokeExact(o1, o2, o3); }

    static Object bootstrap605 (Object l, Object n, Object t) throws Throwable { return _mh[ 605 ].invokeExact(l, n, t); }

    // 606
    private static MethodType MT_bootstrap606 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap606 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap606", MT_bootstrap606 ());
    }

    private static MethodHandle INDY_call606;
    private static MethodHandle INDY_call606 () throws Throwable {
        if (INDY_call606 != null) return INDY_call606;
        CallSite cs = (CallSite) MH_bootstrap606 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap606 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper606 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call606 ().invokeExact(o1, o2, o3); }

    static Object bootstrap606 (Object l, Object n, Object t) throws Throwable { return _mh[ 606 ].invokeExact(l, n, t); }

    // 607
    private static MethodType MT_bootstrap607 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap607 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap607", MT_bootstrap607 ());
    }

    private static MethodHandle INDY_call607;
    private static MethodHandle INDY_call607 () throws Throwable {
        if (INDY_call607 != null) return INDY_call607;
        CallSite cs = (CallSite) MH_bootstrap607 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap607 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper607 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call607 ().invokeExact(o1, o2, o3); }

    static Object bootstrap607 (Object l, Object n, Object t) throws Throwable { return _mh[ 607 ].invokeExact(l, n, t); }

    // 608
    private static MethodType MT_bootstrap608 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap608 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap608", MT_bootstrap608 ());
    }

    private static MethodHandle INDY_call608;
    private static MethodHandle INDY_call608 () throws Throwable {
        if (INDY_call608 != null) return INDY_call608;
        CallSite cs = (CallSite) MH_bootstrap608 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap608 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper608 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call608 ().invokeExact(o1, o2, o3); }

    static Object bootstrap608 (Object l, Object n, Object t) throws Throwable { return _mh[ 608 ].invokeExact(l, n, t); }

    // 609
    private static MethodType MT_bootstrap609 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap609 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap609", MT_bootstrap609 ());
    }

    private static MethodHandle INDY_call609;
    private static MethodHandle INDY_call609 () throws Throwable {
        if (INDY_call609 != null) return INDY_call609;
        CallSite cs = (CallSite) MH_bootstrap609 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap609 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper609 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call609 ().invokeExact(o1, o2, o3); }

    static Object bootstrap609 (Object l, Object n, Object t) throws Throwable { return _mh[ 609 ].invokeExact(l, n, t); }

    // 610
    private static MethodType MT_bootstrap610 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap610 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap610", MT_bootstrap610 ());
    }

    private static MethodHandle INDY_call610;
    private static MethodHandle INDY_call610 () throws Throwable {
        if (INDY_call610 != null) return INDY_call610;
        CallSite cs = (CallSite) MH_bootstrap610 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap610 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper610 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call610 ().invokeExact(o1, o2, o3); }

    static Object bootstrap610 (Object l, Object n, Object t) throws Throwable { return _mh[ 610 ].invokeExact(l, n, t); }

    // 611
    private static MethodType MT_bootstrap611 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap611 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap611", MT_bootstrap611 ());
    }

    private static MethodHandle INDY_call611;
    private static MethodHandle INDY_call611 () throws Throwable {
        if (INDY_call611 != null) return INDY_call611;
        CallSite cs = (CallSite) MH_bootstrap611 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap611 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper611 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call611 ().invokeExact(o1, o2, o3); }

    static Object bootstrap611 (Object l, Object n, Object t) throws Throwable { return _mh[ 611 ].invokeExact(l, n, t); }

    // 612
    private static MethodType MT_bootstrap612 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap612 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap612", MT_bootstrap612 ());
    }

    private static MethodHandle INDY_call612;
    private static MethodHandle INDY_call612 () throws Throwable {
        if (INDY_call612 != null) return INDY_call612;
        CallSite cs = (CallSite) MH_bootstrap612 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap612 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper612 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call612 ().invokeExact(o1, o2, o3); }

    static Object bootstrap612 (Object l, Object n, Object t) throws Throwable { return _mh[ 612 ].invokeExact(l, n, t); }

    // 613
    private static MethodType MT_bootstrap613 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap613 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap613", MT_bootstrap613 ());
    }

    private static MethodHandle INDY_call613;
    private static MethodHandle INDY_call613 () throws Throwable {
        if (INDY_call613 != null) return INDY_call613;
        CallSite cs = (CallSite) MH_bootstrap613 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap613 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper613 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call613 ().invokeExact(o1, o2, o3); }

    static Object bootstrap613 (Object l, Object n, Object t) throws Throwable { return _mh[ 613 ].invokeExact(l, n, t); }

    // 614
    private static MethodType MT_bootstrap614 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap614 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap614", MT_bootstrap614 ());
    }

    private static MethodHandle INDY_call614;
    private static MethodHandle INDY_call614 () throws Throwable {
        if (INDY_call614 != null) return INDY_call614;
        CallSite cs = (CallSite) MH_bootstrap614 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap614 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper614 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call614 ().invokeExact(o1, o2, o3); }

    static Object bootstrap614 (Object l, Object n, Object t) throws Throwable { return _mh[ 614 ].invokeExact(l, n, t); }

    // 615
    private static MethodType MT_bootstrap615 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap615 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap615", MT_bootstrap615 ());
    }

    private static MethodHandle INDY_call615;
    private static MethodHandle INDY_call615 () throws Throwable {
        if (INDY_call615 != null) return INDY_call615;
        CallSite cs = (CallSite) MH_bootstrap615 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap615 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper615 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call615 ().invokeExact(o1, o2, o3); }

    static Object bootstrap615 (Object l, Object n, Object t) throws Throwable { return _mh[ 615 ].invokeExact(l, n, t); }

    // 616
    private static MethodType MT_bootstrap616 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap616 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap616", MT_bootstrap616 ());
    }

    private static MethodHandle INDY_call616;
    private static MethodHandle INDY_call616 () throws Throwable {
        if (INDY_call616 != null) return INDY_call616;
        CallSite cs = (CallSite) MH_bootstrap616 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap616 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper616 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call616 ().invokeExact(o1, o2, o3); }

    static Object bootstrap616 (Object l, Object n, Object t) throws Throwable { return _mh[ 616 ].invokeExact(l, n, t); }

    // 617
    private static MethodType MT_bootstrap617 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap617 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap617", MT_bootstrap617 ());
    }

    private static MethodHandle INDY_call617;
    private static MethodHandle INDY_call617 () throws Throwable {
        if (INDY_call617 != null) return INDY_call617;
        CallSite cs = (CallSite) MH_bootstrap617 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap617 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper617 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call617 ().invokeExact(o1, o2, o3); }

    static Object bootstrap617 (Object l, Object n, Object t) throws Throwable { return _mh[ 617 ].invokeExact(l, n, t); }

    // 618
    private static MethodType MT_bootstrap618 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap618 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap618", MT_bootstrap618 ());
    }

    private static MethodHandle INDY_call618;
    private static MethodHandle INDY_call618 () throws Throwable {
        if (INDY_call618 != null) return INDY_call618;
        CallSite cs = (CallSite) MH_bootstrap618 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap618 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper618 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call618 ().invokeExact(o1, o2, o3); }

    static Object bootstrap618 (Object l, Object n, Object t) throws Throwable { return _mh[ 618 ].invokeExact(l, n, t); }

    // 619
    private static MethodType MT_bootstrap619 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap619 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap619", MT_bootstrap619 ());
    }

    private static MethodHandle INDY_call619;
    private static MethodHandle INDY_call619 () throws Throwable {
        if (INDY_call619 != null) return INDY_call619;
        CallSite cs = (CallSite) MH_bootstrap619 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap619 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper619 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call619 ().invokeExact(o1, o2, o3); }

    static Object bootstrap619 (Object l, Object n, Object t) throws Throwable { return _mh[ 619 ].invokeExact(l, n, t); }

    // 620
    private static MethodType MT_bootstrap620 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap620 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap620", MT_bootstrap620 ());
    }

    private static MethodHandle INDY_call620;
    private static MethodHandle INDY_call620 () throws Throwable {
        if (INDY_call620 != null) return INDY_call620;
        CallSite cs = (CallSite) MH_bootstrap620 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap620 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper620 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call620 ().invokeExact(o1, o2, o3); }

    static Object bootstrap620 (Object l, Object n, Object t) throws Throwable { return _mh[ 620 ].invokeExact(l, n, t); }

    // 621
    private static MethodType MT_bootstrap621 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap621 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap621", MT_bootstrap621 ());
    }

    private static MethodHandle INDY_call621;
    private static MethodHandle INDY_call621 () throws Throwable {
        if (INDY_call621 != null) return INDY_call621;
        CallSite cs = (CallSite) MH_bootstrap621 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap621 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper621 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call621 ().invokeExact(o1, o2, o3); }

    static Object bootstrap621 (Object l, Object n, Object t) throws Throwable { return _mh[ 621 ].invokeExact(l, n, t); }

    // 622
    private static MethodType MT_bootstrap622 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap622 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap622", MT_bootstrap622 ());
    }

    private static MethodHandle INDY_call622;
    private static MethodHandle INDY_call622 () throws Throwable {
        if (INDY_call622 != null) return INDY_call622;
        CallSite cs = (CallSite) MH_bootstrap622 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap622 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper622 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call622 ().invokeExact(o1, o2, o3); }

    static Object bootstrap622 (Object l, Object n, Object t) throws Throwable { return _mh[ 622 ].invokeExact(l, n, t); }

    // 623
    private static MethodType MT_bootstrap623 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap623 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap623", MT_bootstrap623 ());
    }

    private static MethodHandle INDY_call623;
    private static MethodHandle INDY_call623 () throws Throwable {
        if (INDY_call623 != null) return INDY_call623;
        CallSite cs = (CallSite) MH_bootstrap623 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap623 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper623 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call623 ().invokeExact(o1, o2, o3); }

    static Object bootstrap623 (Object l, Object n, Object t) throws Throwable { return _mh[ 623 ].invokeExact(l, n, t); }

    // 624
    private static MethodType MT_bootstrap624 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap624 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap624", MT_bootstrap624 ());
    }

    private static MethodHandle INDY_call624;
    private static MethodHandle INDY_call624 () throws Throwable {
        if (INDY_call624 != null) return INDY_call624;
        CallSite cs = (CallSite) MH_bootstrap624 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap624 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper624 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call624 ().invokeExact(o1, o2, o3); }

    static Object bootstrap624 (Object l, Object n, Object t) throws Throwable { return _mh[ 624 ].invokeExact(l, n, t); }

    // 625
    private static MethodType MT_bootstrap625 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap625 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap625", MT_bootstrap625 ());
    }

    private static MethodHandle INDY_call625;
    private static MethodHandle INDY_call625 () throws Throwable {
        if (INDY_call625 != null) return INDY_call625;
        CallSite cs = (CallSite) MH_bootstrap625 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap625 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper625 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call625 ().invokeExact(o1, o2, o3); }

    static Object bootstrap625 (Object l, Object n, Object t) throws Throwable { return _mh[ 625 ].invokeExact(l, n, t); }

    // 626
    private static MethodType MT_bootstrap626 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap626 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap626", MT_bootstrap626 ());
    }

    private static MethodHandle INDY_call626;
    private static MethodHandle INDY_call626 () throws Throwable {
        if (INDY_call626 != null) return INDY_call626;
        CallSite cs = (CallSite) MH_bootstrap626 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap626 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper626 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call626 ().invokeExact(o1, o2, o3); }

    static Object bootstrap626 (Object l, Object n, Object t) throws Throwable { return _mh[ 626 ].invokeExact(l, n, t); }

    // 627
    private static MethodType MT_bootstrap627 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap627 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap627", MT_bootstrap627 ());
    }

    private static MethodHandle INDY_call627;
    private static MethodHandle INDY_call627 () throws Throwable {
        if (INDY_call627 != null) return INDY_call627;
        CallSite cs = (CallSite) MH_bootstrap627 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap627 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper627 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call627 ().invokeExact(o1, o2, o3); }

    static Object bootstrap627 (Object l, Object n, Object t) throws Throwable { return _mh[ 627 ].invokeExact(l, n, t); }

    // 628
    private static MethodType MT_bootstrap628 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap628 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap628", MT_bootstrap628 ());
    }

    private static MethodHandle INDY_call628;
    private static MethodHandle INDY_call628 () throws Throwable {
        if (INDY_call628 != null) return INDY_call628;
        CallSite cs = (CallSite) MH_bootstrap628 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap628 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper628 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call628 ().invokeExact(o1, o2, o3); }

    static Object bootstrap628 (Object l, Object n, Object t) throws Throwable { return _mh[ 628 ].invokeExact(l, n, t); }

    // 629
    private static MethodType MT_bootstrap629 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap629 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap629", MT_bootstrap629 ());
    }

    private static MethodHandle INDY_call629;
    private static MethodHandle INDY_call629 () throws Throwable {
        if (INDY_call629 != null) return INDY_call629;
        CallSite cs = (CallSite) MH_bootstrap629 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap629 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper629 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call629 ().invokeExact(o1, o2, o3); }

    static Object bootstrap629 (Object l, Object n, Object t) throws Throwable { return _mh[ 629 ].invokeExact(l, n, t); }

    // 630
    private static MethodType MT_bootstrap630 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap630 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap630", MT_bootstrap630 ());
    }

    private static MethodHandle INDY_call630;
    private static MethodHandle INDY_call630 () throws Throwable {
        if (INDY_call630 != null) return INDY_call630;
        CallSite cs = (CallSite) MH_bootstrap630 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap630 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper630 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call630 ().invokeExact(o1, o2, o3); }

    static Object bootstrap630 (Object l, Object n, Object t) throws Throwable { return _mh[ 630 ].invokeExact(l, n, t); }

    // 631
    private static MethodType MT_bootstrap631 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap631 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap631", MT_bootstrap631 ());
    }

    private static MethodHandle INDY_call631;
    private static MethodHandle INDY_call631 () throws Throwable {
        if (INDY_call631 != null) return INDY_call631;
        CallSite cs = (CallSite) MH_bootstrap631 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap631 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper631 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call631 ().invokeExact(o1, o2, o3); }

    static Object bootstrap631 (Object l, Object n, Object t) throws Throwable { return _mh[ 631 ].invokeExact(l, n, t); }

    // 632
    private static MethodType MT_bootstrap632 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap632 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap632", MT_bootstrap632 ());
    }

    private static MethodHandle INDY_call632;
    private static MethodHandle INDY_call632 () throws Throwable {
        if (INDY_call632 != null) return INDY_call632;
        CallSite cs = (CallSite) MH_bootstrap632 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap632 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper632 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call632 ().invokeExact(o1, o2, o3); }

    static Object bootstrap632 (Object l, Object n, Object t) throws Throwable { return _mh[ 632 ].invokeExact(l, n, t); }

    // 633
    private static MethodType MT_bootstrap633 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap633 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap633", MT_bootstrap633 ());
    }

    private static MethodHandle INDY_call633;
    private static MethodHandle INDY_call633 () throws Throwable {
        if (INDY_call633 != null) return INDY_call633;
        CallSite cs = (CallSite) MH_bootstrap633 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap633 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper633 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call633 ().invokeExact(o1, o2, o3); }

    static Object bootstrap633 (Object l, Object n, Object t) throws Throwable { return _mh[ 633 ].invokeExact(l, n, t); }

    // 634
    private static MethodType MT_bootstrap634 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap634 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap634", MT_bootstrap634 ());
    }

    private static MethodHandle INDY_call634;
    private static MethodHandle INDY_call634 () throws Throwable {
        if (INDY_call634 != null) return INDY_call634;
        CallSite cs = (CallSite) MH_bootstrap634 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap634 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper634 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call634 ().invokeExact(o1, o2, o3); }

    static Object bootstrap634 (Object l, Object n, Object t) throws Throwable { return _mh[ 634 ].invokeExact(l, n, t); }

    // 635
    private static MethodType MT_bootstrap635 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap635 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap635", MT_bootstrap635 ());
    }

    private static MethodHandle INDY_call635;
    private static MethodHandle INDY_call635 () throws Throwable {
        if (INDY_call635 != null) return INDY_call635;
        CallSite cs = (CallSite) MH_bootstrap635 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap635 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper635 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call635 ().invokeExact(o1, o2, o3); }

    static Object bootstrap635 (Object l, Object n, Object t) throws Throwable { return _mh[ 635 ].invokeExact(l, n, t); }

    // 636
    private static MethodType MT_bootstrap636 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap636 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap636", MT_bootstrap636 ());
    }

    private static MethodHandle INDY_call636;
    private static MethodHandle INDY_call636 () throws Throwable {
        if (INDY_call636 != null) return INDY_call636;
        CallSite cs = (CallSite) MH_bootstrap636 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap636 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper636 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call636 ().invokeExact(o1, o2, o3); }

    static Object bootstrap636 (Object l, Object n, Object t) throws Throwable { return _mh[ 636 ].invokeExact(l, n, t); }

    // 637
    private static MethodType MT_bootstrap637 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap637 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap637", MT_bootstrap637 ());
    }

    private static MethodHandle INDY_call637;
    private static MethodHandle INDY_call637 () throws Throwable {
        if (INDY_call637 != null) return INDY_call637;
        CallSite cs = (CallSite) MH_bootstrap637 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap637 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper637 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call637 ().invokeExact(o1, o2, o3); }

    static Object bootstrap637 (Object l, Object n, Object t) throws Throwable { return _mh[ 637 ].invokeExact(l, n, t); }

    // 638
    private static MethodType MT_bootstrap638 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap638 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap638", MT_bootstrap638 ());
    }

    private static MethodHandle INDY_call638;
    private static MethodHandle INDY_call638 () throws Throwable {
        if (INDY_call638 != null) return INDY_call638;
        CallSite cs = (CallSite) MH_bootstrap638 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap638 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper638 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call638 ().invokeExact(o1, o2, o3); }

    static Object bootstrap638 (Object l, Object n, Object t) throws Throwable { return _mh[ 638 ].invokeExact(l, n, t); }

    // 639
    private static MethodType MT_bootstrap639 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap639 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap639", MT_bootstrap639 ());
    }

    private static MethodHandle INDY_call639;
    private static MethodHandle INDY_call639 () throws Throwable {
        if (INDY_call639 != null) return INDY_call639;
        CallSite cs = (CallSite) MH_bootstrap639 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap639 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper639 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call639 ().invokeExact(o1, o2, o3); }

    static Object bootstrap639 (Object l, Object n, Object t) throws Throwable { return _mh[ 639 ].invokeExact(l, n, t); }

    // 640
    private static MethodType MT_bootstrap640 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap640 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap640", MT_bootstrap640 ());
    }

    private static MethodHandle INDY_call640;
    private static MethodHandle INDY_call640 () throws Throwable {
        if (INDY_call640 != null) return INDY_call640;
        CallSite cs = (CallSite) MH_bootstrap640 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap640 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper640 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call640 ().invokeExact(o1, o2, o3); }

    static Object bootstrap640 (Object l, Object n, Object t) throws Throwable { return _mh[ 640 ].invokeExact(l, n, t); }

    // 641
    private static MethodType MT_bootstrap641 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap641 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap641", MT_bootstrap641 ());
    }

    private static MethodHandle INDY_call641;
    private static MethodHandle INDY_call641 () throws Throwable {
        if (INDY_call641 != null) return INDY_call641;
        CallSite cs = (CallSite) MH_bootstrap641 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap641 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper641 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call641 ().invokeExact(o1, o2, o3); }

    static Object bootstrap641 (Object l, Object n, Object t) throws Throwable { return _mh[ 641 ].invokeExact(l, n, t); }

    // 642
    private static MethodType MT_bootstrap642 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap642 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap642", MT_bootstrap642 ());
    }

    private static MethodHandle INDY_call642;
    private static MethodHandle INDY_call642 () throws Throwable {
        if (INDY_call642 != null) return INDY_call642;
        CallSite cs = (CallSite) MH_bootstrap642 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap642 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper642 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call642 ().invokeExact(o1, o2, o3); }

    static Object bootstrap642 (Object l, Object n, Object t) throws Throwable { return _mh[ 642 ].invokeExact(l, n, t); }

    // 643
    private static MethodType MT_bootstrap643 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap643 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap643", MT_bootstrap643 ());
    }

    private static MethodHandle INDY_call643;
    private static MethodHandle INDY_call643 () throws Throwable {
        if (INDY_call643 != null) return INDY_call643;
        CallSite cs = (CallSite) MH_bootstrap643 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap643 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper643 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call643 ().invokeExact(o1, o2, o3); }

    static Object bootstrap643 (Object l, Object n, Object t) throws Throwable { return _mh[ 643 ].invokeExact(l, n, t); }

    // 644
    private static MethodType MT_bootstrap644 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap644 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap644", MT_bootstrap644 ());
    }

    private static MethodHandle INDY_call644;
    private static MethodHandle INDY_call644 () throws Throwable {
        if (INDY_call644 != null) return INDY_call644;
        CallSite cs = (CallSite) MH_bootstrap644 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap644 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper644 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call644 ().invokeExact(o1, o2, o3); }

    static Object bootstrap644 (Object l, Object n, Object t) throws Throwable { return _mh[ 644 ].invokeExact(l, n, t); }

    // 645
    private static MethodType MT_bootstrap645 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap645 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap645", MT_bootstrap645 ());
    }

    private static MethodHandle INDY_call645;
    private static MethodHandle INDY_call645 () throws Throwable {
        if (INDY_call645 != null) return INDY_call645;
        CallSite cs = (CallSite) MH_bootstrap645 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap645 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper645 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call645 ().invokeExact(o1, o2, o3); }

    static Object bootstrap645 (Object l, Object n, Object t) throws Throwable { return _mh[ 645 ].invokeExact(l, n, t); }

    // 646
    private static MethodType MT_bootstrap646 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap646 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap646", MT_bootstrap646 ());
    }

    private static MethodHandle INDY_call646;
    private static MethodHandle INDY_call646 () throws Throwable {
        if (INDY_call646 != null) return INDY_call646;
        CallSite cs = (CallSite) MH_bootstrap646 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap646 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper646 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call646 ().invokeExact(o1, o2, o3); }

    static Object bootstrap646 (Object l, Object n, Object t) throws Throwable { return _mh[ 646 ].invokeExact(l, n, t); }

    // 647
    private static MethodType MT_bootstrap647 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap647 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap647", MT_bootstrap647 ());
    }

    private static MethodHandle INDY_call647;
    private static MethodHandle INDY_call647 () throws Throwable {
        if (INDY_call647 != null) return INDY_call647;
        CallSite cs = (CallSite) MH_bootstrap647 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap647 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper647 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call647 ().invokeExact(o1, o2, o3); }

    static Object bootstrap647 (Object l, Object n, Object t) throws Throwable { return _mh[ 647 ].invokeExact(l, n, t); }

    // 648
    private static MethodType MT_bootstrap648 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap648 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap648", MT_bootstrap648 ());
    }

    private static MethodHandle INDY_call648;
    private static MethodHandle INDY_call648 () throws Throwable {
        if (INDY_call648 != null) return INDY_call648;
        CallSite cs = (CallSite) MH_bootstrap648 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap648 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper648 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call648 ().invokeExact(o1, o2, o3); }

    static Object bootstrap648 (Object l, Object n, Object t) throws Throwable { return _mh[ 648 ].invokeExact(l, n, t); }

    // 649
    private static MethodType MT_bootstrap649 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap649 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap649", MT_bootstrap649 ());
    }

    private static MethodHandle INDY_call649;
    private static MethodHandle INDY_call649 () throws Throwable {
        if (INDY_call649 != null) return INDY_call649;
        CallSite cs = (CallSite) MH_bootstrap649 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap649 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper649 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call649 ().invokeExact(o1, o2, o3); }

    static Object bootstrap649 (Object l, Object n, Object t) throws Throwable { return _mh[ 649 ].invokeExact(l, n, t); }

    // 650
    private static MethodType MT_bootstrap650 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap650 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap650", MT_bootstrap650 ());
    }

    private static MethodHandle INDY_call650;
    private static MethodHandle INDY_call650 () throws Throwable {
        if (INDY_call650 != null) return INDY_call650;
        CallSite cs = (CallSite) MH_bootstrap650 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap650 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper650 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call650 ().invokeExact(o1, o2, o3); }

    static Object bootstrap650 (Object l, Object n, Object t) throws Throwable { return _mh[ 650 ].invokeExact(l, n, t); }

    // 651
    private static MethodType MT_bootstrap651 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap651 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap651", MT_bootstrap651 ());
    }

    private static MethodHandle INDY_call651;
    private static MethodHandle INDY_call651 () throws Throwable {
        if (INDY_call651 != null) return INDY_call651;
        CallSite cs = (CallSite) MH_bootstrap651 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap651 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper651 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call651 ().invokeExact(o1, o2, o3); }

    static Object bootstrap651 (Object l, Object n, Object t) throws Throwable { return _mh[ 651 ].invokeExact(l, n, t); }

    // 652
    private static MethodType MT_bootstrap652 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap652 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap652", MT_bootstrap652 ());
    }

    private static MethodHandle INDY_call652;
    private static MethodHandle INDY_call652 () throws Throwable {
        if (INDY_call652 != null) return INDY_call652;
        CallSite cs = (CallSite) MH_bootstrap652 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap652 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper652 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call652 ().invokeExact(o1, o2, o3); }

    static Object bootstrap652 (Object l, Object n, Object t) throws Throwable { return _mh[ 652 ].invokeExact(l, n, t); }

    // 653
    private static MethodType MT_bootstrap653 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap653 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap653", MT_bootstrap653 ());
    }

    private static MethodHandle INDY_call653;
    private static MethodHandle INDY_call653 () throws Throwable {
        if (INDY_call653 != null) return INDY_call653;
        CallSite cs = (CallSite) MH_bootstrap653 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap653 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper653 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call653 ().invokeExact(o1, o2, o3); }

    static Object bootstrap653 (Object l, Object n, Object t) throws Throwable { return _mh[ 653 ].invokeExact(l, n, t); }

    // 654
    private static MethodType MT_bootstrap654 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap654 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap654", MT_bootstrap654 ());
    }

    private static MethodHandle INDY_call654;
    private static MethodHandle INDY_call654 () throws Throwable {
        if (INDY_call654 != null) return INDY_call654;
        CallSite cs = (CallSite) MH_bootstrap654 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap654 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper654 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call654 ().invokeExact(o1, o2, o3); }

    static Object bootstrap654 (Object l, Object n, Object t) throws Throwable { return _mh[ 654 ].invokeExact(l, n, t); }

    // 655
    private static MethodType MT_bootstrap655 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap655 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap655", MT_bootstrap655 ());
    }

    private static MethodHandle INDY_call655;
    private static MethodHandle INDY_call655 () throws Throwable {
        if (INDY_call655 != null) return INDY_call655;
        CallSite cs = (CallSite) MH_bootstrap655 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap655 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper655 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call655 ().invokeExact(o1, o2, o3); }

    static Object bootstrap655 (Object l, Object n, Object t) throws Throwable { return _mh[ 655 ].invokeExact(l, n, t); }

    // 656
    private static MethodType MT_bootstrap656 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap656 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap656", MT_bootstrap656 ());
    }

    private static MethodHandle INDY_call656;
    private static MethodHandle INDY_call656 () throws Throwable {
        if (INDY_call656 != null) return INDY_call656;
        CallSite cs = (CallSite) MH_bootstrap656 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap656 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper656 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call656 ().invokeExact(o1, o2, o3); }

    static Object bootstrap656 (Object l, Object n, Object t) throws Throwable { return _mh[ 656 ].invokeExact(l, n, t); }

    // 657
    private static MethodType MT_bootstrap657 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap657 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap657", MT_bootstrap657 ());
    }

    private static MethodHandle INDY_call657;
    private static MethodHandle INDY_call657 () throws Throwable {
        if (INDY_call657 != null) return INDY_call657;
        CallSite cs = (CallSite) MH_bootstrap657 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap657 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper657 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call657 ().invokeExact(o1, o2, o3); }

    static Object bootstrap657 (Object l, Object n, Object t) throws Throwable { return _mh[ 657 ].invokeExact(l, n, t); }

    // 658
    private static MethodType MT_bootstrap658 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap658 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap658", MT_bootstrap658 ());
    }

    private static MethodHandle INDY_call658;
    private static MethodHandle INDY_call658 () throws Throwable {
        if (INDY_call658 != null) return INDY_call658;
        CallSite cs = (CallSite) MH_bootstrap658 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap658 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper658 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call658 ().invokeExact(o1, o2, o3); }

    static Object bootstrap658 (Object l, Object n, Object t) throws Throwable { return _mh[ 658 ].invokeExact(l, n, t); }

    // 659
    private static MethodType MT_bootstrap659 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap659 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap659", MT_bootstrap659 ());
    }

    private static MethodHandle INDY_call659;
    private static MethodHandle INDY_call659 () throws Throwable {
        if (INDY_call659 != null) return INDY_call659;
        CallSite cs = (CallSite) MH_bootstrap659 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap659 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper659 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call659 ().invokeExact(o1, o2, o3); }

    static Object bootstrap659 (Object l, Object n, Object t) throws Throwable { return _mh[ 659 ].invokeExact(l, n, t); }

    // 660
    private static MethodType MT_bootstrap660 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap660 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap660", MT_bootstrap660 ());
    }

    private static MethodHandle INDY_call660;
    private static MethodHandle INDY_call660 () throws Throwable {
        if (INDY_call660 != null) return INDY_call660;
        CallSite cs = (CallSite) MH_bootstrap660 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap660 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper660 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call660 ().invokeExact(o1, o2, o3); }

    static Object bootstrap660 (Object l, Object n, Object t) throws Throwable { return _mh[ 660 ].invokeExact(l, n, t); }

    // 661
    private static MethodType MT_bootstrap661 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap661 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap661", MT_bootstrap661 ());
    }

    private static MethodHandle INDY_call661;
    private static MethodHandle INDY_call661 () throws Throwable {
        if (INDY_call661 != null) return INDY_call661;
        CallSite cs = (CallSite) MH_bootstrap661 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap661 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper661 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call661 ().invokeExact(o1, o2, o3); }

    static Object bootstrap661 (Object l, Object n, Object t) throws Throwable { return _mh[ 661 ].invokeExact(l, n, t); }

    // 662
    private static MethodType MT_bootstrap662 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap662 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap662", MT_bootstrap662 ());
    }

    private static MethodHandle INDY_call662;
    private static MethodHandle INDY_call662 () throws Throwable {
        if (INDY_call662 != null) return INDY_call662;
        CallSite cs = (CallSite) MH_bootstrap662 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap662 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper662 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call662 ().invokeExact(o1, o2, o3); }

    static Object bootstrap662 (Object l, Object n, Object t) throws Throwable { return _mh[ 662 ].invokeExact(l, n, t); }

    // 663
    private static MethodType MT_bootstrap663 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap663 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap663", MT_bootstrap663 ());
    }

    private static MethodHandle INDY_call663;
    private static MethodHandle INDY_call663 () throws Throwable {
        if (INDY_call663 != null) return INDY_call663;
        CallSite cs = (CallSite) MH_bootstrap663 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap663 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper663 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call663 ().invokeExact(o1, o2, o3); }

    static Object bootstrap663 (Object l, Object n, Object t) throws Throwable { return _mh[ 663 ].invokeExact(l, n, t); }

    // 664
    private static MethodType MT_bootstrap664 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap664 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap664", MT_bootstrap664 ());
    }

    private static MethodHandle INDY_call664;
    private static MethodHandle INDY_call664 () throws Throwable {
        if (INDY_call664 != null) return INDY_call664;
        CallSite cs = (CallSite) MH_bootstrap664 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap664 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper664 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call664 ().invokeExact(o1, o2, o3); }

    static Object bootstrap664 (Object l, Object n, Object t) throws Throwable { return _mh[ 664 ].invokeExact(l, n, t); }

    // 665
    private static MethodType MT_bootstrap665 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap665 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap665", MT_bootstrap665 ());
    }

    private static MethodHandle INDY_call665;
    private static MethodHandle INDY_call665 () throws Throwable {
        if (INDY_call665 != null) return INDY_call665;
        CallSite cs = (CallSite) MH_bootstrap665 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap665 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper665 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call665 ().invokeExact(o1, o2, o3); }

    static Object bootstrap665 (Object l, Object n, Object t) throws Throwable { return _mh[ 665 ].invokeExact(l, n, t); }

    // 666
    private static MethodType MT_bootstrap666 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap666 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap666", MT_bootstrap666 ());
    }

    private static MethodHandle INDY_call666;
    private static MethodHandle INDY_call666 () throws Throwable {
        if (INDY_call666 != null) return INDY_call666;
        CallSite cs = (CallSite) MH_bootstrap666 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap666 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper666 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call666 ().invokeExact(o1, o2, o3); }

    static Object bootstrap666 (Object l, Object n, Object t) throws Throwable { return _mh[ 666 ].invokeExact(l, n, t); }

    // 667
    private static MethodType MT_bootstrap667 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap667 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap667", MT_bootstrap667 ());
    }

    private static MethodHandle INDY_call667;
    private static MethodHandle INDY_call667 () throws Throwable {
        if (INDY_call667 != null) return INDY_call667;
        CallSite cs = (CallSite) MH_bootstrap667 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap667 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper667 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call667 ().invokeExact(o1, o2, o3); }

    static Object bootstrap667 (Object l, Object n, Object t) throws Throwable { return _mh[ 667 ].invokeExact(l, n, t); }

    // 668
    private static MethodType MT_bootstrap668 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap668 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap668", MT_bootstrap668 ());
    }

    private static MethodHandle INDY_call668;
    private static MethodHandle INDY_call668 () throws Throwable {
        if (INDY_call668 != null) return INDY_call668;
        CallSite cs = (CallSite) MH_bootstrap668 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap668 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper668 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call668 ().invokeExact(o1, o2, o3); }

    static Object bootstrap668 (Object l, Object n, Object t) throws Throwable { return _mh[ 668 ].invokeExact(l, n, t); }

    // 669
    private static MethodType MT_bootstrap669 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap669 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap669", MT_bootstrap669 ());
    }

    private static MethodHandle INDY_call669;
    private static MethodHandle INDY_call669 () throws Throwable {
        if (INDY_call669 != null) return INDY_call669;
        CallSite cs = (CallSite) MH_bootstrap669 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap669 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper669 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call669 ().invokeExact(o1, o2, o3); }

    static Object bootstrap669 (Object l, Object n, Object t) throws Throwable { return _mh[ 669 ].invokeExact(l, n, t); }

    // 670
    private static MethodType MT_bootstrap670 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap670 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap670", MT_bootstrap670 ());
    }

    private static MethodHandle INDY_call670;
    private static MethodHandle INDY_call670 () throws Throwable {
        if (INDY_call670 != null) return INDY_call670;
        CallSite cs = (CallSite) MH_bootstrap670 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap670 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper670 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call670 ().invokeExact(o1, o2, o3); }

    static Object bootstrap670 (Object l, Object n, Object t) throws Throwable { return _mh[ 670 ].invokeExact(l, n, t); }

    // 671
    private static MethodType MT_bootstrap671 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap671 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap671", MT_bootstrap671 ());
    }

    private static MethodHandle INDY_call671;
    private static MethodHandle INDY_call671 () throws Throwable {
        if (INDY_call671 != null) return INDY_call671;
        CallSite cs = (CallSite) MH_bootstrap671 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap671 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper671 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call671 ().invokeExact(o1, o2, o3); }

    static Object bootstrap671 (Object l, Object n, Object t) throws Throwable { return _mh[ 671 ].invokeExact(l, n, t); }

    // 672
    private static MethodType MT_bootstrap672 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap672 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap672", MT_bootstrap672 ());
    }

    private static MethodHandle INDY_call672;
    private static MethodHandle INDY_call672 () throws Throwable {
        if (INDY_call672 != null) return INDY_call672;
        CallSite cs = (CallSite) MH_bootstrap672 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap672 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper672 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call672 ().invokeExact(o1, o2, o3); }

    static Object bootstrap672 (Object l, Object n, Object t) throws Throwable { return _mh[ 672 ].invokeExact(l, n, t); }

    // 673
    private static MethodType MT_bootstrap673 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap673 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap673", MT_bootstrap673 ());
    }

    private static MethodHandle INDY_call673;
    private static MethodHandle INDY_call673 () throws Throwable {
        if (INDY_call673 != null) return INDY_call673;
        CallSite cs = (CallSite) MH_bootstrap673 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap673 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper673 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call673 ().invokeExact(o1, o2, o3); }

    static Object bootstrap673 (Object l, Object n, Object t) throws Throwable { return _mh[ 673 ].invokeExact(l, n, t); }

    // 674
    private static MethodType MT_bootstrap674 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap674 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap674", MT_bootstrap674 ());
    }

    private static MethodHandle INDY_call674;
    private static MethodHandle INDY_call674 () throws Throwable {
        if (INDY_call674 != null) return INDY_call674;
        CallSite cs = (CallSite) MH_bootstrap674 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap674 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper674 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call674 ().invokeExact(o1, o2, o3); }

    static Object bootstrap674 (Object l, Object n, Object t) throws Throwable { return _mh[ 674 ].invokeExact(l, n, t); }

    // 675
    private static MethodType MT_bootstrap675 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap675 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap675", MT_bootstrap675 ());
    }

    private static MethodHandle INDY_call675;
    private static MethodHandle INDY_call675 () throws Throwable {
        if (INDY_call675 != null) return INDY_call675;
        CallSite cs = (CallSite) MH_bootstrap675 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap675 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper675 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call675 ().invokeExact(o1, o2, o3); }

    static Object bootstrap675 (Object l, Object n, Object t) throws Throwable { return _mh[ 675 ].invokeExact(l, n, t); }

    // 676
    private static MethodType MT_bootstrap676 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap676 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap676", MT_bootstrap676 ());
    }

    private static MethodHandle INDY_call676;
    private static MethodHandle INDY_call676 () throws Throwable {
        if (INDY_call676 != null) return INDY_call676;
        CallSite cs = (CallSite) MH_bootstrap676 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap676 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper676 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call676 ().invokeExact(o1, o2, o3); }

    static Object bootstrap676 (Object l, Object n, Object t) throws Throwable { return _mh[ 676 ].invokeExact(l, n, t); }

    // 677
    private static MethodType MT_bootstrap677 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap677 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap677", MT_bootstrap677 ());
    }

    private static MethodHandle INDY_call677;
    private static MethodHandle INDY_call677 () throws Throwable {
        if (INDY_call677 != null) return INDY_call677;
        CallSite cs = (CallSite) MH_bootstrap677 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap677 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper677 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call677 ().invokeExact(o1, o2, o3); }

    static Object bootstrap677 (Object l, Object n, Object t) throws Throwable { return _mh[ 677 ].invokeExact(l, n, t); }

    // 678
    private static MethodType MT_bootstrap678 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap678 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap678", MT_bootstrap678 ());
    }

    private static MethodHandle INDY_call678;
    private static MethodHandle INDY_call678 () throws Throwable {
        if (INDY_call678 != null) return INDY_call678;
        CallSite cs = (CallSite) MH_bootstrap678 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap678 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper678 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call678 ().invokeExact(o1, o2, o3); }

    static Object bootstrap678 (Object l, Object n, Object t) throws Throwable { return _mh[ 678 ].invokeExact(l, n, t); }

    // 679
    private static MethodType MT_bootstrap679 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap679 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap679", MT_bootstrap679 ());
    }

    private static MethodHandle INDY_call679;
    private static MethodHandle INDY_call679 () throws Throwable {
        if (INDY_call679 != null) return INDY_call679;
        CallSite cs = (CallSite) MH_bootstrap679 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap679 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper679 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call679 ().invokeExact(o1, o2, o3); }

    static Object bootstrap679 (Object l, Object n, Object t) throws Throwable { return _mh[ 679 ].invokeExact(l, n, t); }

    // 680
    private static MethodType MT_bootstrap680 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap680 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap680", MT_bootstrap680 ());
    }

    private static MethodHandle INDY_call680;
    private static MethodHandle INDY_call680 () throws Throwable {
        if (INDY_call680 != null) return INDY_call680;
        CallSite cs = (CallSite) MH_bootstrap680 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap680 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper680 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call680 ().invokeExact(o1, o2, o3); }

    static Object bootstrap680 (Object l, Object n, Object t) throws Throwable { return _mh[ 680 ].invokeExact(l, n, t); }

    // 681
    private static MethodType MT_bootstrap681 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap681 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap681", MT_bootstrap681 ());
    }

    private static MethodHandle INDY_call681;
    private static MethodHandle INDY_call681 () throws Throwable {
        if (INDY_call681 != null) return INDY_call681;
        CallSite cs = (CallSite) MH_bootstrap681 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap681 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper681 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call681 ().invokeExact(o1, o2, o3); }

    static Object bootstrap681 (Object l, Object n, Object t) throws Throwable { return _mh[ 681 ].invokeExact(l, n, t); }

    // 682
    private static MethodType MT_bootstrap682 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap682 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap682", MT_bootstrap682 ());
    }

    private static MethodHandle INDY_call682;
    private static MethodHandle INDY_call682 () throws Throwable {
        if (INDY_call682 != null) return INDY_call682;
        CallSite cs = (CallSite) MH_bootstrap682 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap682 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper682 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call682 ().invokeExact(o1, o2, o3); }

    static Object bootstrap682 (Object l, Object n, Object t) throws Throwable { return _mh[ 682 ].invokeExact(l, n, t); }

    // 683
    private static MethodType MT_bootstrap683 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap683 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap683", MT_bootstrap683 ());
    }

    private static MethodHandle INDY_call683;
    private static MethodHandle INDY_call683 () throws Throwable {
        if (INDY_call683 != null) return INDY_call683;
        CallSite cs = (CallSite) MH_bootstrap683 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap683 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper683 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call683 ().invokeExact(o1, o2, o3); }

    static Object bootstrap683 (Object l, Object n, Object t) throws Throwable { return _mh[ 683 ].invokeExact(l, n, t); }

    // 684
    private static MethodType MT_bootstrap684 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap684 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap684", MT_bootstrap684 ());
    }

    private static MethodHandle INDY_call684;
    private static MethodHandle INDY_call684 () throws Throwable {
        if (INDY_call684 != null) return INDY_call684;
        CallSite cs = (CallSite) MH_bootstrap684 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap684 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper684 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call684 ().invokeExact(o1, o2, o3); }

    static Object bootstrap684 (Object l, Object n, Object t) throws Throwable { return _mh[ 684 ].invokeExact(l, n, t); }

    // 685
    private static MethodType MT_bootstrap685 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap685 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap685", MT_bootstrap685 ());
    }

    private static MethodHandle INDY_call685;
    private static MethodHandle INDY_call685 () throws Throwable {
        if (INDY_call685 != null) return INDY_call685;
        CallSite cs = (CallSite) MH_bootstrap685 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap685 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper685 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call685 ().invokeExact(o1, o2, o3); }

    static Object bootstrap685 (Object l, Object n, Object t) throws Throwable { return _mh[ 685 ].invokeExact(l, n, t); }

    // 686
    private static MethodType MT_bootstrap686 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap686 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap686", MT_bootstrap686 ());
    }

    private static MethodHandle INDY_call686;
    private static MethodHandle INDY_call686 () throws Throwable {
        if (INDY_call686 != null) return INDY_call686;
        CallSite cs = (CallSite) MH_bootstrap686 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap686 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper686 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call686 ().invokeExact(o1, o2, o3); }

    static Object bootstrap686 (Object l, Object n, Object t) throws Throwable { return _mh[ 686 ].invokeExact(l, n, t); }

    // 687
    private static MethodType MT_bootstrap687 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap687 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap687", MT_bootstrap687 ());
    }

    private static MethodHandle INDY_call687;
    private static MethodHandle INDY_call687 () throws Throwable {
        if (INDY_call687 != null) return INDY_call687;
        CallSite cs = (CallSite) MH_bootstrap687 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap687 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper687 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call687 ().invokeExact(o1, o2, o3); }

    static Object bootstrap687 (Object l, Object n, Object t) throws Throwable { return _mh[ 687 ].invokeExact(l, n, t); }

    // 688
    private static MethodType MT_bootstrap688 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap688 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap688", MT_bootstrap688 ());
    }

    private static MethodHandle INDY_call688;
    private static MethodHandle INDY_call688 () throws Throwable {
        if (INDY_call688 != null) return INDY_call688;
        CallSite cs = (CallSite) MH_bootstrap688 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap688 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper688 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call688 ().invokeExact(o1, o2, o3); }

    static Object bootstrap688 (Object l, Object n, Object t) throws Throwable { return _mh[ 688 ].invokeExact(l, n, t); }

    // 689
    private static MethodType MT_bootstrap689 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap689 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap689", MT_bootstrap689 ());
    }

    private static MethodHandle INDY_call689;
    private static MethodHandle INDY_call689 () throws Throwable {
        if (INDY_call689 != null) return INDY_call689;
        CallSite cs = (CallSite) MH_bootstrap689 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap689 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper689 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call689 ().invokeExact(o1, o2, o3); }

    static Object bootstrap689 (Object l, Object n, Object t) throws Throwable { return _mh[ 689 ].invokeExact(l, n, t); }

    // 690
    private static MethodType MT_bootstrap690 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap690 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap690", MT_bootstrap690 ());
    }

    private static MethodHandle INDY_call690;
    private static MethodHandle INDY_call690 () throws Throwable {
        if (INDY_call690 != null) return INDY_call690;
        CallSite cs = (CallSite) MH_bootstrap690 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap690 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper690 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call690 ().invokeExact(o1, o2, o3); }

    static Object bootstrap690 (Object l, Object n, Object t) throws Throwable { return _mh[ 690 ].invokeExact(l, n, t); }

    // 691
    private static MethodType MT_bootstrap691 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap691 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap691", MT_bootstrap691 ());
    }

    private static MethodHandle INDY_call691;
    private static MethodHandle INDY_call691 () throws Throwable {
        if (INDY_call691 != null) return INDY_call691;
        CallSite cs = (CallSite) MH_bootstrap691 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap691 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper691 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call691 ().invokeExact(o1, o2, o3); }

    static Object bootstrap691 (Object l, Object n, Object t) throws Throwable { return _mh[ 691 ].invokeExact(l, n, t); }

    // 692
    private static MethodType MT_bootstrap692 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap692 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap692", MT_bootstrap692 ());
    }

    private static MethodHandle INDY_call692;
    private static MethodHandle INDY_call692 () throws Throwable {
        if (INDY_call692 != null) return INDY_call692;
        CallSite cs = (CallSite) MH_bootstrap692 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap692 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper692 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call692 ().invokeExact(o1, o2, o3); }

    static Object bootstrap692 (Object l, Object n, Object t) throws Throwable { return _mh[ 692 ].invokeExact(l, n, t); }

    // 693
    private static MethodType MT_bootstrap693 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap693 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap693", MT_bootstrap693 ());
    }

    private static MethodHandle INDY_call693;
    private static MethodHandle INDY_call693 () throws Throwable {
        if (INDY_call693 != null) return INDY_call693;
        CallSite cs = (CallSite) MH_bootstrap693 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap693 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper693 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call693 ().invokeExact(o1, o2, o3); }

    static Object bootstrap693 (Object l, Object n, Object t) throws Throwable { return _mh[ 693 ].invokeExact(l, n, t); }

    // 694
    private static MethodType MT_bootstrap694 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap694 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap694", MT_bootstrap694 ());
    }

    private static MethodHandle INDY_call694;
    private static MethodHandle INDY_call694 () throws Throwable {
        if (INDY_call694 != null) return INDY_call694;
        CallSite cs = (CallSite) MH_bootstrap694 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap694 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper694 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call694 ().invokeExact(o1, o2, o3); }

    static Object bootstrap694 (Object l, Object n, Object t) throws Throwable { return _mh[ 694 ].invokeExact(l, n, t); }

    // 695
    private static MethodType MT_bootstrap695 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap695 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap695", MT_bootstrap695 ());
    }

    private static MethodHandle INDY_call695;
    private static MethodHandle INDY_call695 () throws Throwable {
        if (INDY_call695 != null) return INDY_call695;
        CallSite cs = (CallSite) MH_bootstrap695 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap695 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper695 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call695 ().invokeExact(o1, o2, o3); }

    static Object bootstrap695 (Object l, Object n, Object t) throws Throwable { return _mh[ 695 ].invokeExact(l, n, t); }

    // 696
    private static MethodType MT_bootstrap696 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap696 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap696", MT_bootstrap696 ());
    }

    private static MethodHandle INDY_call696;
    private static MethodHandle INDY_call696 () throws Throwable {
        if (INDY_call696 != null) return INDY_call696;
        CallSite cs = (CallSite) MH_bootstrap696 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap696 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper696 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call696 ().invokeExact(o1, o2, o3); }

    static Object bootstrap696 (Object l, Object n, Object t) throws Throwable { return _mh[ 696 ].invokeExact(l, n, t); }

    // 697
    private static MethodType MT_bootstrap697 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap697 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap697", MT_bootstrap697 ());
    }

    private static MethodHandle INDY_call697;
    private static MethodHandle INDY_call697 () throws Throwable {
        if (INDY_call697 != null) return INDY_call697;
        CallSite cs = (CallSite) MH_bootstrap697 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap697 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper697 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call697 ().invokeExact(o1, o2, o3); }

    static Object bootstrap697 (Object l, Object n, Object t) throws Throwable { return _mh[ 697 ].invokeExact(l, n, t); }

    // 698
    private static MethodType MT_bootstrap698 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap698 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap698", MT_bootstrap698 ());
    }

    private static MethodHandle INDY_call698;
    private static MethodHandle INDY_call698 () throws Throwable {
        if (INDY_call698 != null) return INDY_call698;
        CallSite cs = (CallSite) MH_bootstrap698 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap698 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper698 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call698 ().invokeExact(o1, o2, o3); }

    static Object bootstrap698 (Object l, Object n, Object t) throws Throwable { return _mh[ 698 ].invokeExact(l, n, t); }

    // 699
    private static MethodType MT_bootstrap699 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap699 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap699", MT_bootstrap699 ());
    }

    private static MethodHandle INDY_call699;
    private static MethodHandle INDY_call699 () throws Throwable {
        if (INDY_call699 != null) return INDY_call699;
        CallSite cs = (CallSite) MH_bootstrap699 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap699 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper699 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call699 ().invokeExact(o1, o2, o3); }

    static Object bootstrap699 (Object l, Object n, Object t) throws Throwable { return _mh[ 699 ].invokeExact(l, n, t); }

    // 700
    private static MethodType MT_bootstrap700 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap700 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap700", MT_bootstrap700 ());
    }

    private static MethodHandle INDY_call700;
    private static MethodHandle INDY_call700 () throws Throwable {
        if (INDY_call700 != null) return INDY_call700;
        CallSite cs = (CallSite) MH_bootstrap700 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap700 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper700 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call700 ().invokeExact(o1, o2, o3); }

    static Object bootstrap700 (Object l, Object n, Object t) throws Throwable { return _mh[ 700 ].invokeExact(l, n, t); }

    // 701
    private static MethodType MT_bootstrap701 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap701 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap701", MT_bootstrap701 ());
    }

    private static MethodHandle INDY_call701;
    private static MethodHandle INDY_call701 () throws Throwable {
        if (INDY_call701 != null) return INDY_call701;
        CallSite cs = (CallSite) MH_bootstrap701 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap701 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper701 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call701 ().invokeExact(o1, o2, o3); }

    static Object bootstrap701 (Object l, Object n, Object t) throws Throwable { return _mh[ 701 ].invokeExact(l, n, t); }

    // 702
    private static MethodType MT_bootstrap702 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap702 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap702", MT_bootstrap702 ());
    }

    private static MethodHandle INDY_call702;
    private static MethodHandle INDY_call702 () throws Throwable {
        if (INDY_call702 != null) return INDY_call702;
        CallSite cs = (CallSite) MH_bootstrap702 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap702 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper702 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call702 ().invokeExact(o1, o2, o3); }

    static Object bootstrap702 (Object l, Object n, Object t) throws Throwable { return _mh[ 702 ].invokeExact(l, n, t); }

    // 703
    private static MethodType MT_bootstrap703 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap703 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap703", MT_bootstrap703 ());
    }

    private static MethodHandle INDY_call703;
    private static MethodHandle INDY_call703 () throws Throwable {
        if (INDY_call703 != null) return INDY_call703;
        CallSite cs = (CallSite) MH_bootstrap703 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap703 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper703 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call703 ().invokeExact(o1, o2, o3); }

    static Object bootstrap703 (Object l, Object n, Object t) throws Throwable { return _mh[ 703 ].invokeExact(l, n, t); }

    // 704
    private static MethodType MT_bootstrap704 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap704 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap704", MT_bootstrap704 ());
    }

    private static MethodHandle INDY_call704;
    private static MethodHandle INDY_call704 () throws Throwable {
        if (INDY_call704 != null) return INDY_call704;
        CallSite cs = (CallSite) MH_bootstrap704 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap704 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper704 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call704 ().invokeExact(o1, o2, o3); }

    static Object bootstrap704 (Object l, Object n, Object t) throws Throwable { return _mh[ 704 ].invokeExact(l, n, t); }

    // 705
    private static MethodType MT_bootstrap705 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap705 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap705", MT_bootstrap705 ());
    }

    private static MethodHandle INDY_call705;
    private static MethodHandle INDY_call705 () throws Throwable {
        if (INDY_call705 != null) return INDY_call705;
        CallSite cs = (CallSite) MH_bootstrap705 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap705 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper705 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call705 ().invokeExact(o1, o2, o3); }

    static Object bootstrap705 (Object l, Object n, Object t) throws Throwable { return _mh[ 705 ].invokeExact(l, n, t); }

    // 706
    private static MethodType MT_bootstrap706 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap706 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap706", MT_bootstrap706 ());
    }

    private static MethodHandle INDY_call706;
    private static MethodHandle INDY_call706 () throws Throwable {
        if (INDY_call706 != null) return INDY_call706;
        CallSite cs = (CallSite) MH_bootstrap706 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap706 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper706 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call706 ().invokeExact(o1, o2, o3); }

    static Object bootstrap706 (Object l, Object n, Object t) throws Throwable { return _mh[ 706 ].invokeExact(l, n, t); }

    // 707
    private static MethodType MT_bootstrap707 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap707 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap707", MT_bootstrap707 ());
    }

    private static MethodHandle INDY_call707;
    private static MethodHandle INDY_call707 () throws Throwable {
        if (INDY_call707 != null) return INDY_call707;
        CallSite cs = (CallSite) MH_bootstrap707 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap707 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper707 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call707 ().invokeExact(o1, o2, o3); }

    static Object bootstrap707 (Object l, Object n, Object t) throws Throwable { return _mh[ 707 ].invokeExact(l, n, t); }

    // 708
    private static MethodType MT_bootstrap708 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap708 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap708", MT_bootstrap708 ());
    }

    private static MethodHandle INDY_call708;
    private static MethodHandle INDY_call708 () throws Throwable {
        if (INDY_call708 != null) return INDY_call708;
        CallSite cs = (CallSite) MH_bootstrap708 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap708 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper708 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call708 ().invokeExact(o1, o2, o3); }

    static Object bootstrap708 (Object l, Object n, Object t) throws Throwable { return _mh[ 708 ].invokeExact(l, n, t); }

    // 709
    private static MethodType MT_bootstrap709 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap709 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap709", MT_bootstrap709 ());
    }

    private static MethodHandle INDY_call709;
    private static MethodHandle INDY_call709 () throws Throwable {
        if (INDY_call709 != null) return INDY_call709;
        CallSite cs = (CallSite) MH_bootstrap709 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap709 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper709 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call709 ().invokeExact(o1, o2, o3); }

    static Object bootstrap709 (Object l, Object n, Object t) throws Throwable { return _mh[ 709 ].invokeExact(l, n, t); }

    // 710
    private static MethodType MT_bootstrap710 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap710 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap710", MT_bootstrap710 ());
    }

    private static MethodHandle INDY_call710;
    private static MethodHandle INDY_call710 () throws Throwable {
        if (INDY_call710 != null) return INDY_call710;
        CallSite cs = (CallSite) MH_bootstrap710 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap710 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper710 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call710 ().invokeExact(o1, o2, o3); }

    static Object bootstrap710 (Object l, Object n, Object t) throws Throwable { return _mh[ 710 ].invokeExact(l, n, t); }

    // 711
    private static MethodType MT_bootstrap711 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap711 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap711", MT_bootstrap711 ());
    }

    private static MethodHandle INDY_call711;
    private static MethodHandle INDY_call711 () throws Throwable {
        if (INDY_call711 != null) return INDY_call711;
        CallSite cs = (CallSite) MH_bootstrap711 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap711 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper711 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call711 ().invokeExact(o1, o2, o3); }

    static Object bootstrap711 (Object l, Object n, Object t) throws Throwable { return _mh[ 711 ].invokeExact(l, n, t); }

    // 712
    private static MethodType MT_bootstrap712 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap712 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap712", MT_bootstrap712 ());
    }

    private static MethodHandle INDY_call712;
    private static MethodHandle INDY_call712 () throws Throwable {
        if (INDY_call712 != null) return INDY_call712;
        CallSite cs = (CallSite) MH_bootstrap712 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap712 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper712 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call712 ().invokeExact(o1, o2, o3); }

    static Object bootstrap712 (Object l, Object n, Object t) throws Throwable { return _mh[ 712 ].invokeExact(l, n, t); }

    // 713
    private static MethodType MT_bootstrap713 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap713 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap713", MT_bootstrap713 ());
    }

    private static MethodHandle INDY_call713;
    private static MethodHandle INDY_call713 () throws Throwable {
        if (INDY_call713 != null) return INDY_call713;
        CallSite cs = (CallSite) MH_bootstrap713 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap713 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper713 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call713 ().invokeExact(o1, o2, o3); }

    static Object bootstrap713 (Object l, Object n, Object t) throws Throwable { return _mh[ 713 ].invokeExact(l, n, t); }

    // 714
    private static MethodType MT_bootstrap714 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap714 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap714", MT_bootstrap714 ());
    }

    private static MethodHandle INDY_call714;
    private static MethodHandle INDY_call714 () throws Throwable {
        if (INDY_call714 != null) return INDY_call714;
        CallSite cs = (CallSite) MH_bootstrap714 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap714 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper714 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call714 ().invokeExact(o1, o2, o3); }

    static Object bootstrap714 (Object l, Object n, Object t) throws Throwable { return _mh[ 714 ].invokeExact(l, n, t); }

    // 715
    private static MethodType MT_bootstrap715 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap715 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap715", MT_bootstrap715 ());
    }

    private static MethodHandle INDY_call715;
    private static MethodHandle INDY_call715 () throws Throwable {
        if (INDY_call715 != null) return INDY_call715;
        CallSite cs = (CallSite) MH_bootstrap715 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap715 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper715 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call715 ().invokeExact(o1, o2, o3); }

    static Object bootstrap715 (Object l, Object n, Object t) throws Throwable { return _mh[ 715 ].invokeExact(l, n, t); }

    // 716
    private static MethodType MT_bootstrap716 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap716 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap716", MT_bootstrap716 ());
    }

    private static MethodHandle INDY_call716;
    private static MethodHandle INDY_call716 () throws Throwable {
        if (INDY_call716 != null) return INDY_call716;
        CallSite cs = (CallSite) MH_bootstrap716 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap716 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper716 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call716 ().invokeExact(o1, o2, o3); }

    static Object bootstrap716 (Object l, Object n, Object t) throws Throwable { return _mh[ 716 ].invokeExact(l, n, t); }

    // 717
    private static MethodType MT_bootstrap717 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap717 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap717", MT_bootstrap717 ());
    }

    private static MethodHandle INDY_call717;
    private static MethodHandle INDY_call717 () throws Throwable {
        if (INDY_call717 != null) return INDY_call717;
        CallSite cs = (CallSite) MH_bootstrap717 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap717 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper717 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call717 ().invokeExact(o1, o2, o3); }

    static Object bootstrap717 (Object l, Object n, Object t) throws Throwable { return _mh[ 717 ].invokeExact(l, n, t); }

    // 718
    private static MethodType MT_bootstrap718 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap718 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap718", MT_bootstrap718 ());
    }

    private static MethodHandle INDY_call718;
    private static MethodHandle INDY_call718 () throws Throwable {
        if (INDY_call718 != null) return INDY_call718;
        CallSite cs = (CallSite) MH_bootstrap718 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap718 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper718 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call718 ().invokeExact(o1, o2, o3); }

    static Object bootstrap718 (Object l, Object n, Object t) throws Throwable { return _mh[ 718 ].invokeExact(l, n, t); }

    // 719
    private static MethodType MT_bootstrap719 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap719 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap719", MT_bootstrap719 ());
    }

    private static MethodHandle INDY_call719;
    private static MethodHandle INDY_call719 () throws Throwable {
        if (INDY_call719 != null) return INDY_call719;
        CallSite cs = (CallSite) MH_bootstrap719 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap719 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper719 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call719 ().invokeExact(o1, o2, o3); }

    static Object bootstrap719 (Object l, Object n, Object t) throws Throwable { return _mh[ 719 ].invokeExact(l, n, t); }

    // 720
    private static MethodType MT_bootstrap720 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap720 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap720", MT_bootstrap720 ());
    }

    private static MethodHandle INDY_call720;
    private static MethodHandle INDY_call720 () throws Throwable {
        if (INDY_call720 != null) return INDY_call720;
        CallSite cs = (CallSite) MH_bootstrap720 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap720 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper720 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call720 ().invokeExact(o1, o2, o3); }

    static Object bootstrap720 (Object l, Object n, Object t) throws Throwable { return _mh[ 720 ].invokeExact(l, n, t); }

    // 721
    private static MethodType MT_bootstrap721 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap721 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap721", MT_bootstrap721 ());
    }

    private static MethodHandle INDY_call721;
    private static MethodHandle INDY_call721 () throws Throwable {
        if (INDY_call721 != null) return INDY_call721;
        CallSite cs = (CallSite) MH_bootstrap721 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap721 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper721 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call721 ().invokeExact(o1, o2, o3); }

    static Object bootstrap721 (Object l, Object n, Object t) throws Throwable { return _mh[ 721 ].invokeExact(l, n, t); }

    // 722
    private static MethodType MT_bootstrap722 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap722 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap722", MT_bootstrap722 ());
    }

    private static MethodHandle INDY_call722;
    private static MethodHandle INDY_call722 () throws Throwable {
        if (INDY_call722 != null) return INDY_call722;
        CallSite cs = (CallSite) MH_bootstrap722 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap722 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper722 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call722 ().invokeExact(o1, o2, o3); }

    static Object bootstrap722 (Object l, Object n, Object t) throws Throwable { return _mh[ 722 ].invokeExact(l, n, t); }

    // 723
    private static MethodType MT_bootstrap723 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap723 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap723", MT_bootstrap723 ());
    }

    private static MethodHandle INDY_call723;
    private static MethodHandle INDY_call723 () throws Throwable {
        if (INDY_call723 != null) return INDY_call723;
        CallSite cs = (CallSite) MH_bootstrap723 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap723 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper723 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call723 ().invokeExact(o1, o2, o3); }

    static Object bootstrap723 (Object l, Object n, Object t) throws Throwable { return _mh[ 723 ].invokeExact(l, n, t); }

    // 724
    private static MethodType MT_bootstrap724 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap724 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap724", MT_bootstrap724 ());
    }

    private static MethodHandle INDY_call724;
    private static MethodHandle INDY_call724 () throws Throwable {
        if (INDY_call724 != null) return INDY_call724;
        CallSite cs = (CallSite) MH_bootstrap724 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap724 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper724 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call724 ().invokeExact(o1, o2, o3); }

    static Object bootstrap724 (Object l, Object n, Object t) throws Throwable { return _mh[ 724 ].invokeExact(l, n, t); }

    // 725
    private static MethodType MT_bootstrap725 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap725 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap725", MT_bootstrap725 ());
    }

    private static MethodHandle INDY_call725;
    private static MethodHandle INDY_call725 () throws Throwable {
        if (INDY_call725 != null) return INDY_call725;
        CallSite cs = (CallSite) MH_bootstrap725 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap725 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper725 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call725 ().invokeExact(o1, o2, o3); }

    static Object bootstrap725 (Object l, Object n, Object t) throws Throwable { return _mh[ 725 ].invokeExact(l, n, t); }

    // 726
    private static MethodType MT_bootstrap726 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap726 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap726", MT_bootstrap726 ());
    }

    private static MethodHandle INDY_call726;
    private static MethodHandle INDY_call726 () throws Throwable {
        if (INDY_call726 != null) return INDY_call726;
        CallSite cs = (CallSite) MH_bootstrap726 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap726 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper726 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call726 ().invokeExact(o1, o2, o3); }

    static Object bootstrap726 (Object l, Object n, Object t) throws Throwable { return _mh[ 726 ].invokeExact(l, n, t); }

    // 727
    private static MethodType MT_bootstrap727 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap727 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap727", MT_bootstrap727 ());
    }

    private static MethodHandle INDY_call727;
    private static MethodHandle INDY_call727 () throws Throwable {
        if (INDY_call727 != null) return INDY_call727;
        CallSite cs = (CallSite) MH_bootstrap727 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap727 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper727 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call727 ().invokeExact(o1, o2, o3); }

    static Object bootstrap727 (Object l, Object n, Object t) throws Throwable { return _mh[ 727 ].invokeExact(l, n, t); }

    // 728
    private static MethodType MT_bootstrap728 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap728 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap728", MT_bootstrap728 ());
    }

    private static MethodHandle INDY_call728;
    private static MethodHandle INDY_call728 () throws Throwable {
        if (INDY_call728 != null) return INDY_call728;
        CallSite cs = (CallSite) MH_bootstrap728 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap728 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper728 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call728 ().invokeExact(o1, o2, o3); }

    static Object bootstrap728 (Object l, Object n, Object t) throws Throwable { return _mh[ 728 ].invokeExact(l, n, t); }

    // 729
    private static MethodType MT_bootstrap729 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap729 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap729", MT_bootstrap729 ());
    }

    private static MethodHandle INDY_call729;
    private static MethodHandle INDY_call729 () throws Throwable {
        if (INDY_call729 != null) return INDY_call729;
        CallSite cs = (CallSite) MH_bootstrap729 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap729 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper729 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call729 ().invokeExact(o1, o2, o3); }

    static Object bootstrap729 (Object l, Object n, Object t) throws Throwable { return _mh[ 729 ].invokeExact(l, n, t); }

    // 730
    private static MethodType MT_bootstrap730 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap730 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap730", MT_bootstrap730 ());
    }

    private static MethodHandle INDY_call730;
    private static MethodHandle INDY_call730 () throws Throwable {
        if (INDY_call730 != null) return INDY_call730;
        CallSite cs = (CallSite) MH_bootstrap730 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap730 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper730 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call730 ().invokeExact(o1, o2, o3); }

    static Object bootstrap730 (Object l, Object n, Object t) throws Throwable { return _mh[ 730 ].invokeExact(l, n, t); }

    // 731
    private static MethodType MT_bootstrap731 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap731 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap731", MT_bootstrap731 ());
    }

    private static MethodHandle INDY_call731;
    private static MethodHandle INDY_call731 () throws Throwable {
        if (INDY_call731 != null) return INDY_call731;
        CallSite cs = (CallSite) MH_bootstrap731 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap731 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper731 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call731 ().invokeExact(o1, o2, o3); }

    static Object bootstrap731 (Object l, Object n, Object t) throws Throwable { return _mh[ 731 ].invokeExact(l, n, t); }

    // 732
    private static MethodType MT_bootstrap732 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap732 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap732", MT_bootstrap732 ());
    }

    private static MethodHandle INDY_call732;
    private static MethodHandle INDY_call732 () throws Throwable {
        if (INDY_call732 != null) return INDY_call732;
        CallSite cs = (CallSite) MH_bootstrap732 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap732 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper732 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call732 ().invokeExact(o1, o2, o3); }

    static Object bootstrap732 (Object l, Object n, Object t) throws Throwable { return _mh[ 732 ].invokeExact(l, n, t); }

    // 733
    private static MethodType MT_bootstrap733 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap733 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap733", MT_bootstrap733 ());
    }

    private static MethodHandle INDY_call733;
    private static MethodHandle INDY_call733 () throws Throwable {
        if (INDY_call733 != null) return INDY_call733;
        CallSite cs = (CallSite) MH_bootstrap733 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap733 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper733 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call733 ().invokeExact(o1, o2, o3); }

    static Object bootstrap733 (Object l, Object n, Object t) throws Throwable { return _mh[ 733 ].invokeExact(l, n, t); }

    // 734
    private static MethodType MT_bootstrap734 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap734 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap734", MT_bootstrap734 ());
    }

    private static MethodHandle INDY_call734;
    private static MethodHandle INDY_call734 () throws Throwable {
        if (INDY_call734 != null) return INDY_call734;
        CallSite cs = (CallSite) MH_bootstrap734 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap734 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper734 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call734 ().invokeExact(o1, o2, o3); }

    static Object bootstrap734 (Object l, Object n, Object t) throws Throwable { return _mh[ 734 ].invokeExact(l, n, t); }

    // 735
    private static MethodType MT_bootstrap735 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap735 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap735", MT_bootstrap735 ());
    }

    private static MethodHandle INDY_call735;
    private static MethodHandle INDY_call735 () throws Throwable {
        if (INDY_call735 != null) return INDY_call735;
        CallSite cs = (CallSite) MH_bootstrap735 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap735 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper735 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call735 ().invokeExact(o1, o2, o3); }

    static Object bootstrap735 (Object l, Object n, Object t) throws Throwable { return _mh[ 735 ].invokeExact(l, n, t); }

    // 736
    private static MethodType MT_bootstrap736 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap736 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap736", MT_bootstrap736 ());
    }

    private static MethodHandle INDY_call736;
    private static MethodHandle INDY_call736 () throws Throwable {
        if (INDY_call736 != null) return INDY_call736;
        CallSite cs = (CallSite) MH_bootstrap736 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap736 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper736 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call736 ().invokeExact(o1, o2, o3); }

    static Object bootstrap736 (Object l, Object n, Object t) throws Throwable { return _mh[ 736 ].invokeExact(l, n, t); }

    // 737
    private static MethodType MT_bootstrap737 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap737 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap737", MT_bootstrap737 ());
    }

    private static MethodHandle INDY_call737;
    private static MethodHandle INDY_call737 () throws Throwable {
        if (INDY_call737 != null) return INDY_call737;
        CallSite cs = (CallSite) MH_bootstrap737 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap737 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper737 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call737 ().invokeExact(o1, o2, o3); }

    static Object bootstrap737 (Object l, Object n, Object t) throws Throwable { return _mh[ 737 ].invokeExact(l, n, t); }

    // 738
    private static MethodType MT_bootstrap738 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap738 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap738", MT_bootstrap738 ());
    }

    private static MethodHandle INDY_call738;
    private static MethodHandle INDY_call738 () throws Throwable {
        if (INDY_call738 != null) return INDY_call738;
        CallSite cs = (CallSite) MH_bootstrap738 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap738 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper738 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call738 ().invokeExact(o1, o2, o3); }

    static Object bootstrap738 (Object l, Object n, Object t) throws Throwable { return _mh[ 738 ].invokeExact(l, n, t); }

    // 739
    private static MethodType MT_bootstrap739 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap739 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap739", MT_bootstrap739 ());
    }

    private static MethodHandle INDY_call739;
    private static MethodHandle INDY_call739 () throws Throwable {
        if (INDY_call739 != null) return INDY_call739;
        CallSite cs = (CallSite) MH_bootstrap739 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap739 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper739 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call739 ().invokeExact(o1, o2, o3); }

    static Object bootstrap739 (Object l, Object n, Object t) throws Throwable { return _mh[ 739 ].invokeExact(l, n, t); }

    // 740
    private static MethodType MT_bootstrap740 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap740 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap740", MT_bootstrap740 ());
    }

    private static MethodHandle INDY_call740;
    private static MethodHandle INDY_call740 () throws Throwable {
        if (INDY_call740 != null) return INDY_call740;
        CallSite cs = (CallSite) MH_bootstrap740 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap740 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper740 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call740 ().invokeExact(o1, o2, o3); }

    static Object bootstrap740 (Object l, Object n, Object t) throws Throwable { return _mh[ 740 ].invokeExact(l, n, t); }

    // 741
    private static MethodType MT_bootstrap741 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap741 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap741", MT_bootstrap741 ());
    }

    private static MethodHandle INDY_call741;
    private static MethodHandle INDY_call741 () throws Throwable {
        if (INDY_call741 != null) return INDY_call741;
        CallSite cs = (CallSite) MH_bootstrap741 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap741 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper741 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call741 ().invokeExact(o1, o2, o3); }

    static Object bootstrap741 (Object l, Object n, Object t) throws Throwable { return _mh[ 741 ].invokeExact(l, n, t); }

    // 742
    private static MethodType MT_bootstrap742 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap742 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap742", MT_bootstrap742 ());
    }

    private static MethodHandle INDY_call742;
    private static MethodHandle INDY_call742 () throws Throwable {
        if (INDY_call742 != null) return INDY_call742;
        CallSite cs = (CallSite) MH_bootstrap742 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap742 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper742 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call742 ().invokeExact(o1, o2, o3); }

    static Object bootstrap742 (Object l, Object n, Object t) throws Throwable { return _mh[ 742 ].invokeExact(l, n, t); }

    // 743
    private static MethodType MT_bootstrap743 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap743 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap743", MT_bootstrap743 ());
    }

    private static MethodHandle INDY_call743;
    private static MethodHandle INDY_call743 () throws Throwable {
        if (INDY_call743 != null) return INDY_call743;
        CallSite cs = (CallSite) MH_bootstrap743 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap743 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper743 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call743 ().invokeExact(o1, o2, o3); }

    static Object bootstrap743 (Object l, Object n, Object t) throws Throwable { return _mh[ 743 ].invokeExact(l, n, t); }

    // 744
    private static MethodType MT_bootstrap744 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap744 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap744", MT_bootstrap744 ());
    }

    private static MethodHandle INDY_call744;
    private static MethodHandle INDY_call744 () throws Throwable {
        if (INDY_call744 != null) return INDY_call744;
        CallSite cs = (CallSite) MH_bootstrap744 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap744 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper744 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call744 ().invokeExact(o1, o2, o3); }

    static Object bootstrap744 (Object l, Object n, Object t) throws Throwable { return _mh[ 744 ].invokeExact(l, n, t); }

    // 745
    private static MethodType MT_bootstrap745 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap745 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap745", MT_bootstrap745 ());
    }

    private static MethodHandle INDY_call745;
    private static MethodHandle INDY_call745 () throws Throwable {
        if (INDY_call745 != null) return INDY_call745;
        CallSite cs = (CallSite) MH_bootstrap745 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap745 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper745 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call745 ().invokeExact(o1, o2, o3); }

    static Object bootstrap745 (Object l, Object n, Object t) throws Throwable { return _mh[ 745 ].invokeExact(l, n, t); }

    // 746
    private static MethodType MT_bootstrap746 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap746 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap746", MT_bootstrap746 ());
    }

    private static MethodHandle INDY_call746;
    private static MethodHandle INDY_call746 () throws Throwable {
        if (INDY_call746 != null) return INDY_call746;
        CallSite cs = (CallSite) MH_bootstrap746 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap746 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper746 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call746 ().invokeExact(o1, o2, o3); }

    static Object bootstrap746 (Object l, Object n, Object t) throws Throwable { return _mh[ 746 ].invokeExact(l, n, t); }

    // 747
    private static MethodType MT_bootstrap747 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap747 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap747", MT_bootstrap747 ());
    }

    private static MethodHandle INDY_call747;
    private static MethodHandle INDY_call747 () throws Throwable {
        if (INDY_call747 != null) return INDY_call747;
        CallSite cs = (CallSite) MH_bootstrap747 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap747 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper747 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call747 ().invokeExact(o1, o2, o3); }

    static Object bootstrap747 (Object l, Object n, Object t) throws Throwable { return _mh[ 747 ].invokeExact(l, n, t); }

    // 748
    private static MethodType MT_bootstrap748 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap748 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap748", MT_bootstrap748 ());
    }

    private static MethodHandle INDY_call748;
    private static MethodHandle INDY_call748 () throws Throwable {
        if (INDY_call748 != null) return INDY_call748;
        CallSite cs = (CallSite) MH_bootstrap748 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap748 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper748 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call748 ().invokeExact(o1, o2, o3); }

    static Object bootstrap748 (Object l, Object n, Object t) throws Throwable { return _mh[ 748 ].invokeExact(l, n, t); }

    // 749
    private static MethodType MT_bootstrap749 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap749 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap749", MT_bootstrap749 ());
    }

    private static MethodHandle INDY_call749;
    private static MethodHandle INDY_call749 () throws Throwable {
        if (INDY_call749 != null) return INDY_call749;
        CallSite cs = (CallSite) MH_bootstrap749 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap749 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper749 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call749 ().invokeExact(o1, o2, o3); }

    static Object bootstrap749 (Object l, Object n, Object t) throws Throwable { return _mh[ 749 ].invokeExact(l, n, t); }

    // 750
    private static MethodType MT_bootstrap750 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap750 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap750", MT_bootstrap750 ());
    }

    private static MethodHandle INDY_call750;
    private static MethodHandle INDY_call750 () throws Throwable {
        if (INDY_call750 != null) return INDY_call750;
        CallSite cs = (CallSite) MH_bootstrap750 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap750 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper750 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call750 ().invokeExact(o1, o2, o3); }

    static Object bootstrap750 (Object l, Object n, Object t) throws Throwable { return _mh[ 750 ].invokeExact(l, n, t); }

    // 751
    private static MethodType MT_bootstrap751 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap751 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap751", MT_bootstrap751 ());
    }

    private static MethodHandle INDY_call751;
    private static MethodHandle INDY_call751 () throws Throwable {
        if (INDY_call751 != null) return INDY_call751;
        CallSite cs = (CallSite) MH_bootstrap751 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap751 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper751 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call751 ().invokeExact(o1, o2, o3); }

    static Object bootstrap751 (Object l, Object n, Object t) throws Throwable { return _mh[ 751 ].invokeExact(l, n, t); }

    // 752
    private static MethodType MT_bootstrap752 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap752 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap752", MT_bootstrap752 ());
    }

    private static MethodHandle INDY_call752;
    private static MethodHandle INDY_call752 () throws Throwable {
        if (INDY_call752 != null) return INDY_call752;
        CallSite cs = (CallSite) MH_bootstrap752 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap752 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper752 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call752 ().invokeExact(o1, o2, o3); }

    static Object bootstrap752 (Object l, Object n, Object t) throws Throwable { return _mh[ 752 ].invokeExact(l, n, t); }

    // 753
    private static MethodType MT_bootstrap753 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap753 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap753", MT_bootstrap753 ());
    }

    private static MethodHandle INDY_call753;
    private static MethodHandle INDY_call753 () throws Throwable {
        if (INDY_call753 != null) return INDY_call753;
        CallSite cs = (CallSite) MH_bootstrap753 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap753 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper753 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call753 ().invokeExact(o1, o2, o3); }

    static Object bootstrap753 (Object l, Object n, Object t) throws Throwable { return _mh[ 753 ].invokeExact(l, n, t); }

    // 754
    private static MethodType MT_bootstrap754 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap754 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap754", MT_bootstrap754 ());
    }

    private static MethodHandle INDY_call754;
    private static MethodHandle INDY_call754 () throws Throwable {
        if (INDY_call754 != null) return INDY_call754;
        CallSite cs = (CallSite) MH_bootstrap754 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap754 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper754 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call754 ().invokeExact(o1, o2, o3); }

    static Object bootstrap754 (Object l, Object n, Object t) throws Throwable { return _mh[ 754 ].invokeExact(l, n, t); }

    // 755
    private static MethodType MT_bootstrap755 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap755 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap755", MT_bootstrap755 ());
    }

    private static MethodHandle INDY_call755;
    private static MethodHandle INDY_call755 () throws Throwable {
        if (INDY_call755 != null) return INDY_call755;
        CallSite cs = (CallSite) MH_bootstrap755 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap755 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper755 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call755 ().invokeExact(o1, o2, o3); }

    static Object bootstrap755 (Object l, Object n, Object t) throws Throwable { return _mh[ 755 ].invokeExact(l, n, t); }

    // 756
    private static MethodType MT_bootstrap756 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap756 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap756", MT_bootstrap756 ());
    }

    private static MethodHandle INDY_call756;
    private static MethodHandle INDY_call756 () throws Throwable {
        if (INDY_call756 != null) return INDY_call756;
        CallSite cs = (CallSite) MH_bootstrap756 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap756 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper756 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call756 ().invokeExact(o1, o2, o3); }

    static Object bootstrap756 (Object l, Object n, Object t) throws Throwable { return _mh[ 756 ].invokeExact(l, n, t); }

    // 757
    private static MethodType MT_bootstrap757 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap757 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap757", MT_bootstrap757 ());
    }

    private static MethodHandle INDY_call757;
    private static MethodHandle INDY_call757 () throws Throwable {
        if (INDY_call757 != null) return INDY_call757;
        CallSite cs = (CallSite) MH_bootstrap757 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap757 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper757 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call757 ().invokeExact(o1, o2, o3); }

    static Object bootstrap757 (Object l, Object n, Object t) throws Throwable { return _mh[ 757 ].invokeExact(l, n, t); }

    // 758
    private static MethodType MT_bootstrap758 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap758 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap758", MT_bootstrap758 ());
    }

    private static MethodHandle INDY_call758;
    private static MethodHandle INDY_call758 () throws Throwable {
        if (INDY_call758 != null) return INDY_call758;
        CallSite cs = (CallSite) MH_bootstrap758 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap758 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper758 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call758 ().invokeExact(o1, o2, o3); }

    static Object bootstrap758 (Object l, Object n, Object t) throws Throwable { return _mh[ 758 ].invokeExact(l, n, t); }

    // 759
    private static MethodType MT_bootstrap759 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap759 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap759", MT_bootstrap759 ());
    }

    private static MethodHandle INDY_call759;
    private static MethodHandle INDY_call759 () throws Throwable {
        if (INDY_call759 != null) return INDY_call759;
        CallSite cs = (CallSite) MH_bootstrap759 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap759 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper759 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call759 ().invokeExact(o1, o2, o3); }

    static Object bootstrap759 (Object l, Object n, Object t) throws Throwable { return _mh[ 759 ].invokeExact(l, n, t); }

    // 760
    private static MethodType MT_bootstrap760 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap760 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap760", MT_bootstrap760 ());
    }

    private static MethodHandle INDY_call760;
    private static MethodHandle INDY_call760 () throws Throwable {
        if (INDY_call760 != null) return INDY_call760;
        CallSite cs = (CallSite) MH_bootstrap760 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap760 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper760 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call760 ().invokeExact(o1, o2, o3); }

    static Object bootstrap760 (Object l, Object n, Object t) throws Throwable { return _mh[ 760 ].invokeExact(l, n, t); }

    // 761
    private static MethodType MT_bootstrap761 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap761 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap761", MT_bootstrap761 ());
    }

    private static MethodHandle INDY_call761;
    private static MethodHandle INDY_call761 () throws Throwable {
        if (INDY_call761 != null) return INDY_call761;
        CallSite cs = (CallSite) MH_bootstrap761 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap761 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper761 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call761 ().invokeExact(o1, o2, o3); }

    static Object bootstrap761 (Object l, Object n, Object t) throws Throwable { return _mh[ 761 ].invokeExact(l, n, t); }

    // 762
    private static MethodType MT_bootstrap762 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap762 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap762", MT_bootstrap762 ());
    }

    private static MethodHandle INDY_call762;
    private static MethodHandle INDY_call762 () throws Throwable {
        if (INDY_call762 != null) return INDY_call762;
        CallSite cs = (CallSite) MH_bootstrap762 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap762 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper762 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call762 ().invokeExact(o1, o2, o3); }

    static Object bootstrap762 (Object l, Object n, Object t) throws Throwable { return _mh[ 762 ].invokeExact(l, n, t); }

    // 763
    private static MethodType MT_bootstrap763 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap763 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap763", MT_bootstrap763 ());
    }

    private static MethodHandle INDY_call763;
    private static MethodHandle INDY_call763 () throws Throwable {
        if (INDY_call763 != null) return INDY_call763;
        CallSite cs = (CallSite) MH_bootstrap763 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap763 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper763 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call763 ().invokeExact(o1, o2, o3); }

    static Object bootstrap763 (Object l, Object n, Object t) throws Throwable { return _mh[ 763 ].invokeExact(l, n, t); }

    // 764
    private static MethodType MT_bootstrap764 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap764 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap764", MT_bootstrap764 ());
    }

    private static MethodHandle INDY_call764;
    private static MethodHandle INDY_call764 () throws Throwable {
        if (INDY_call764 != null) return INDY_call764;
        CallSite cs = (CallSite) MH_bootstrap764 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap764 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper764 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call764 ().invokeExact(o1, o2, o3); }

    static Object bootstrap764 (Object l, Object n, Object t) throws Throwable { return _mh[ 764 ].invokeExact(l, n, t); }

    // 765
    private static MethodType MT_bootstrap765 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap765 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap765", MT_bootstrap765 ());
    }

    private static MethodHandle INDY_call765;
    private static MethodHandle INDY_call765 () throws Throwable {
        if (INDY_call765 != null) return INDY_call765;
        CallSite cs = (CallSite) MH_bootstrap765 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap765 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper765 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call765 ().invokeExact(o1, o2, o3); }

    static Object bootstrap765 (Object l, Object n, Object t) throws Throwable { return _mh[ 765 ].invokeExact(l, n, t); }

    // 766
    private static MethodType MT_bootstrap766 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap766 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap766", MT_bootstrap766 ());
    }

    private static MethodHandle INDY_call766;
    private static MethodHandle INDY_call766 () throws Throwable {
        if (INDY_call766 != null) return INDY_call766;
        CallSite cs = (CallSite) MH_bootstrap766 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap766 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper766 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call766 ().invokeExact(o1, o2, o3); }

    static Object bootstrap766 (Object l, Object n, Object t) throws Throwable { return _mh[ 766 ].invokeExact(l, n, t); }

    // 767
    private static MethodType MT_bootstrap767 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap767 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap767", MT_bootstrap767 ());
    }

    private static MethodHandle INDY_call767;
    private static MethodHandle INDY_call767 () throws Throwable {
        if (INDY_call767 != null) return INDY_call767;
        CallSite cs = (CallSite) MH_bootstrap767 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap767 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper767 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call767 ().invokeExact(o1, o2, o3); }

    static Object bootstrap767 (Object l, Object n, Object t) throws Throwable { return _mh[ 767 ].invokeExact(l, n, t); }

    // 768
    private static MethodType MT_bootstrap768 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap768 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap768", MT_bootstrap768 ());
    }

    private static MethodHandle INDY_call768;
    private static MethodHandle INDY_call768 () throws Throwable {
        if (INDY_call768 != null) return INDY_call768;
        CallSite cs = (CallSite) MH_bootstrap768 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap768 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper768 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call768 ().invokeExact(o1, o2, o3); }

    static Object bootstrap768 (Object l, Object n, Object t) throws Throwable { return _mh[ 768 ].invokeExact(l, n, t); }

    // 769
    private static MethodType MT_bootstrap769 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap769 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap769", MT_bootstrap769 ());
    }

    private static MethodHandle INDY_call769;
    private static MethodHandle INDY_call769 () throws Throwable {
        if (INDY_call769 != null) return INDY_call769;
        CallSite cs = (CallSite) MH_bootstrap769 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap769 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper769 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call769 ().invokeExact(o1, o2, o3); }

    static Object bootstrap769 (Object l, Object n, Object t) throws Throwable { return _mh[ 769 ].invokeExact(l, n, t); }

    // 770
    private static MethodType MT_bootstrap770 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap770 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap770", MT_bootstrap770 ());
    }

    private static MethodHandle INDY_call770;
    private static MethodHandle INDY_call770 () throws Throwable {
        if (INDY_call770 != null) return INDY_call770;
        CallSite cs = (CallSite) MH_bootstrap770 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap770 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper770 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call770 ().invokeExact(o1, o2, o3); }

    static Object bootstrap770 (Object l, Object n, Object t) throws Throwable { return _mh[ 770 ].invokeExact(l, n, t); }

    // 771
    private static MethodType MT_bootstrap771 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap771 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap771", MT_bootstrap771 ());
    }

    private static MethodHandle INDY_call771;
    private static MethodHandle INDY_call771 () throws Throwable {
        if (INDY_call771 != null) return INDY_call771;
        CallSite cs = (CallSite) MH_bootstrap771 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap771 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper771 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call771 ().invokeExact(o1, o2, o3); }

    static Object bootstrap771 (Object l, Object n, Object t) throws Throwable { return _mh[ 771 ].invokeExact(l, n, t); }

    // 772
    private static MethodType MT_bootstrap772 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap772 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap772", MT_bootstrap772 ());
    }

    private static MethodHandle INDY_call772;
    private static MethodHandle INDY_call772 () throws Throwable {
        if (INDY_call772 != null) return INDY_call772;
        CallSite cs = (CallSite) MH_bootstrap772 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap772 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper772 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call772 ().invokeExact(o1, o2, o3); }

    static Object bootstrap772 (Object l, Object n, Object t) throws Throwable { return _mh[ 772 ].invokeExact(l, n, t); }

    // 773
    private static MethodType MT_bootstrap773 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap773 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap773", MT_bootstrap773 ());
    }

    private static MethodHandle INDY_call773;
    private static MethodHandle INDY_call773 () throws Throwable {
        if (INDY_call773 != null) return INDY_call773;
        CallSite cs = (CallSite) MH_bootstrap773 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap773 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper773 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call773 ().invokeExact(o1, o2, o3); }

    static Object bootstrap773 (Object l, Object n, Object t) throws Throwable { return _mh[ 773 ].invokeExact(l, n, t); }

    // 774
    private static MethodType MT_bootstrap774 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap774 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap774", MT_bootstrap774 ());
    }

    private static MethodHandle INDY_call774;
    private static MethodHandle INDY_call774 () throws Throwable {
        if (INDY_call774 != null) return INDY_call774;
        CallSite cs = (CallSite) MH_bootstrap774 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap774 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper774 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call774 ().invokeExact(o1, o2, o3); }

    static Object bootstrap774 (Object l, Object n, Object t) throws Throwable { return _mh[ 774 ].invokeExact(l, n, t); }

    // 775
    private static MethodType MT_bootstrap775 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap775 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap775", MT_bootstrap775 ());
    }

    private static MethodHandle INDY_call775;
    private static MethodHandle INDY_call775 () throws Throwable {
        if (INDY_call775 != null) return INDY_call775;
        CallSite cs = (CallSite) MH_bootstrap775 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap775 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper775 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call775 ().invokeExact(o1, o2, o3); }

    static Object bootstrap775 (Object l, Object n, Object t) throws Throwable { return _mh[ 775 ].invokeExact(l, n, t); }

    // 776
    private static MethodType MT_bootstrap776 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap776 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap776", MT_bootstrap776 ());
    }

    private static MethodHandle INDY_call776;
    private static MethodHandle INDY_call776 () throws Throwable {
        if (INDY_call776 != null) return INDY_call776;
        CallSite cs = (CallSite) MH_bootstrap776 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap776 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper776 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call776 ().invokeExact(o1, o2, o3); }

    static Object bootstrap776 (Object l, Object n, Object t) throws Throwable { return _mh[ 776 ].invokeExact(l, n, t); }

    // 777
    private static MethodType MT_bootstrap777 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap777 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap777", MT_bootstrap777 ());
    }

    private static MethodHandle INDY_call777;
    private static MethodHandle INDY_call777 () throws Throwable {
        if (INDY_call777 != null) return INDY_call777;
        CallSite cs = (CallSite) MH_bootstrap777 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap777 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper777 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call777 ().invokeExact(o1, o2, o3); }

    static Object bootstrap777 (Object l, Object n, Object t) throws Throwable { return _mh[ 777 ].invokeExact(l, n, t); }

    // 778
    private static MethodType MT_bootstrap778 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap778 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap778", MT_bootstrap778 ());
    }

    private static MethodHandle INDY_call778;
    private static MethodHandle INDY_call778 () throws Throwable {
        if (INDY_call778 != null) return INDY_call778;
        CallSite cs = (CallSite) MH_bootstrap778 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap778 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper778 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call778 ().invokeExact(o1, o2, o3); }

    static Object bootstrap778 (Object l, Object n, Object t) throws Throwable { return _mh[ 778 ].invokeExact(l, n, t); }

    // 779
    private static MethodType MT_bootstrap779 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap779 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap779", MT_bootstrap779 ());
    }

    private static MethodHandle INDY_call779;
    private static MethodHandle INDY_call779 () throws Throwable {
        if (INDY_call779 != null) return INDY_call779;
        CallSite cs = (CallSite) MH_bootstrap779 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap779 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper779 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call779 ().invokeExact(o1, o2, o3); }

    static Object bootstrap779 (Object l, Object n, Object t) throws Throwable { return _mh[ 779 ].invokeExact(l, n, t); }

    // 780
    private static MethodType MT_bootstrap780 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap780 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap780", MT_bootstrap780 ());
    }

    private static MethodHandle INDY_call780;
    private static MethodHandle INDY_call780 () throws Throwable {
        if (INDY_call780 != null) return INDY_call780;
        CallSite cs = (CallSite) MH_bootstrap780 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap780 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper780 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call780 ().invokeExact(o1, o2, o3); }

    static Object bootstrap780 (Object l, Object n, Object t) throws Throwable { return _mh[ 780 ].invokeExact(l, n, t); }

    // 781
    private static MethodType MT_bootstrap781 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap781 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap781", MT_bootstrap781 ());
    }

    private static MethodHandle INDY_call781;
    private static MethodHandle INDY_call781 () throws Throwable {
        if (INDY_call781 != null) return INDY_call781;
        CallSite cs = (CallSite) MH_bootstrap781 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap781 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper781 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call781 ().invokeExact(o1, o2, o3); }

    static Object bootstrap781 (Object l, Object n, Object t) throws Throwable { return _mh[ 781 ].invokeExact(l, n, t); }

    // 782
    private static MethodType MT_bootstrap782 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap782 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap782", MT_bootstrap782 ());
    }

    private static MethodHandle INDY_call782;
    private static MethodHandle INDY_call782 () throws Throwable {
        if (INDY_call782 != null) return INDY_call782;
        CallSite cs = (CallSite) MH_bootstrap782 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap782 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper782 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call782 ().invokeExact(o1, o2, o3); }

    static Object bootstrap782 (Object l, Object n, Object t) throws Throwable { return _mh[ 782 ].invokeExact(l, n, t); }

    // 783
    private static MethodType MT_bootstrap783 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap783 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap783", MT_bootstrap783 ());
    }

    private static MethodHandle INDY_call783;
    private static MethodHandle INDY_call783 () throws Throwable {
        if (INDY_call783 != null) return INDY_call783;
        CallSite cs = (CallSite) MH_bootstrap783 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap783 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper783 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call783 ().invokeExact(o1, o2, o3); }

    static Object bootstrap783 (Object l, Object n, Object t) throws Throwable { return _mh[ 783 ].invokeExact(l, n, t); }

    // 784
    private static MethodType MT_bootstrap784 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap784 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap784", MT_bootstrap784 ());
    }

    private static MethodHandle INDY_call784;
    private static MethodHandle INDY_call784 () throws Throwable {
        if (INDY_call784 != null) return INDY_call784;
        CallSite cs = (CallSite) MH_bootstrap784 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap784 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper784 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call784 ().invokeExact(o1, o2, o3); }

    static Object bootstrap784 (Object l, Object n, Object t) throws Throwable { return _mh[ 784 ].invokeExact(l, n, t); }

    // 785
    private static MethodType MT_bootstrap785 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap785 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap785", MT_bootstrap785 ());
    }

    private static MethodHandle INDY_call785;
    private static MethodHandle INDY_call785 () throws Throwable {
        if (INDY_call785 != null) return INDY_call785;
        CallSite cs = (CallSite) MH_bootstrap785 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap785 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper785 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call785 ().invokeExact(o1, o2, o3); }

    static Object bootstrap785 (Object l, Object n, Object t) throws Throwable { return _mh[ 785 ].invokeExact(l, n, t); }

    // 786
    private static MethodType MT_bootstrap786 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap786 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap786", MT_bootstrap786 ());
    }

    private static MethodHandle INDY_call786;
    private static MethodHandle INDY_call786 () throws Throwable {
        if (INDY_call786 != null) return INDY_call786;
        CallSite cs = (CallSite) MH_bootstrap786 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap786 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper786 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call786 ().invokeExact(o1, o2, o3); }

    static Object bootstrap786 (Object l, Object n, Object t) throws Throwable { return _mh[ 786 ].invokeExact(l, n, t); }

    // 787
    private static MethodType MT_bootstrap787 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap787 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap787", MT_bootstrap787 ());
    }

    private static MethodHandle INDY_call787;
    private static MethodHandle INDY_call787 () throws Throwable {
        if (INDY_call787 != null) return INDY_call787;
        CallSite cs = (CallSite) MH_bootstrap787 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap787 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper787 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call787 ().invokeExact(o1, o2, o3); }

    static Object bootstrap787 (Object l, Object n, Object t) throws Throwable { return _mh[ 787 ].invokeExact(l, n, t); }

    // 788
    private static MethodType MT_bootstrap788 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap788 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap788", MT_bootstrap788 ());
    }

    private static MethodHandle INDY_call788;
    private static MethodHandle INDY_call788 () throws Throwable {
        if (INDY_call788 != null) return INDY_call788;
        CallSite cs = (CallSite) MH_bootstrap788 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap788 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper788 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call788 ().invokeExact(o1, o2, o3); }

    static Object bootstrap788 (Object l, Object n, Object t) throws Throwable { return _mh[ 788 ].invokeExact(l, n, t); }

    // 789
    private static MethodType MT_bootstrap789 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap789 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap789", MT_bootstrap789 ());
    }

    private static MethodHandle INDY_call789;
    private static MethodHandle INDY_call789 () throws Throwable {
        if (INDY_call789 != null) return INDY_call789;
        CallSite cs = (CallSite) MH_bootstrap789 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap789 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper789 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call789 ().invokeExact(o1, o2, o3); }

    static Object bootstrap789 (Object l, Object n, Object t) throws Throwable { return _mh[ 789 ].invokeExact(l, n, t); }

    // 790
    private static MethodType MT_bootstrap790 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap790 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap790", MT_bootstrap790 ());
    }

    private static MethodHandle INDY_call790;
    private static MethodHandle INDY_call790 () throws Throwable {
        if (INDY_call790 != null) return INDY_call790;
        CallSite cs = (CallSite) MH_bootstrap790 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap790 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper790 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call790 ().invokeExact(o1, o2, o3); }

    static Object bootstrap790 (Object l, Object n, Object t) throws Throwable { return _mh[ 790 ].invokeExact(l, n, t); }

    // 791
    private static MethodType MT_bootstrap791 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap791 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap791", MT_bootstrap791 ());
    }

    private static MethodHandle INDY_call791;
    private static MethodHandle INDY_call791 () throws Throwable {
        if (INDY_call791 != null) return INDY_call791;
        CallSite cs = (CallSite) MH_bootstrap791 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap791 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper791 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call791 ().invokeExact(o1, o2, o3); }

    static Object bootstrap791 (Object l, Object n, Object t) throws Throwable { return _mh[ 791 ].invokeExact(l, n, t); }

    // 792
    private static MethodType MT_bootstrap792 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap792 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap792", MT_bootstrap792 ());
    }

    private static MethodHandle INDY_call792;
    private static MethodHandle INDY_call792 () throws Throwable {
        if (INDY_call792 != null) return INDY_call792;
        CallSite cs = (CallSite) MH_bootstrap792 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap792 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper792 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call792 ().invokeExact(o1, o2, o3); }

    static Object bootstrap792 (Object l, Object n, Object t) throws Throwable { return _mh[ 792 ].invokeExact(l, n, t); }

    // 793
    private static MethodType MT_bootstrap793 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap793 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap793", MT_bootstrap793 ());
    }

    private static MethodHandle INDY_call793;
    private static MethodHandle INDY_call793 () throws Throwable {
        if (INDY_call793 != null) return INDY_call793;
        CallSite cs = (CallSite) MH_bootstrap793 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap793 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper793 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call793 ().invokeExact(o1, o2, o3); }

    static Object bootstrap793 (Object l, Object n, Object t) throws Throwable { return _mh[ 793 ].invokeExact(l, n, t); }

    // 794
    private static MethodType MT_bootstrap794 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap794 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap794", MT_bootstrap794 ());
    }

    private static MethodHandle INDY_call794;
    private static MethodHandle INDY_call794 () throws Throwable {
        if (INDY_call794 != null) return INDY_call794;
        CallSite cs = (CallSite) MH_bootstrap794 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap794 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper794 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call794 ().invokeExact(o1, o2, o3); }

    static Object bootstrap794 (Object l, Object n, Object t) throws Throwable { return _mh[ 794 ].invokeExact(l, n, t); }

    // 795
    private static MethodType MT_bootstrap795 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap795 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap795", MT_bootstrap795 ());
    }

    private static MethodHandle INDY_call795;
    private static MethodHandle INDY_call795 () throws Throwable {
        if (INDY_call795 != null) return INDY_call795;
        CallSite cs = (CallSite) MH_bootstrap795 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap795 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper795 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call795 ().invokeExact(o1, o2, o3); }

    static Object bootstrap795 (Object l, Object n, Object t) throws Throwable { return _mh[ 795 ].invokeExact(l, n, t); }

    // 796
    private static MethodType MT_bootstrap796 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap796 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap796", MT_bootstrap796 ());
    }

    private static MethodHandle INDY_call796;
    private static MethodHandle INDY_call796 () throws Throwable {
        if (INDY_call796 != null) return INDY_call796;
        CallSite cs = (CallSite) MH_bootstrap796 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap796 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper796 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call796 ().invokeExact(o1, o2, o3); }

    static Object bootstrap796 (Object l, Object n, Object t) throws Throwable { return _mh[ 796 ].invokeExact(l, n, t); }

    // 797
    private static MethodType MT_bootstrap797 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap797 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap797", MT_bootstrap797 ());
    }

    private static MethodHandle INDY_call797;
    private static MethodHandle INDY_call797 () throws Throwable {
        if (INDY_call797 != null) return INDY_call797;
        CallSite cs = (CallSite) MH_bootstrap797 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap797 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper797 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call797 ().invokeExact(o1, o2, o3); }

    static Object bootstrap797 (Object l, Object n, Object t) throws Throwable { return _mh[ 797 ].invokeExact(l, n, t); }

    // 798
    private static MethodType MT_bootstrap798 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap798 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap798", MT_bootstrap798 ());
    }

    private static MethodHandle INDY_call798;
    private static MethodHandle INDY_call798 () throws Throwable {
        if (INDY_call798 != null) return INDY_call798;
        CallSite cs = (CallSite) MH_bootstrap798 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap798 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper798 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call798 ().invokeExact(o1, o2, o3); }

    static Object bootstrap798 (Object l, Object n, Object t) throws Throwable { return _mh[ 798 ].invokeExact(l, n, t); }

    // 799
    private static MethodType MT_bootstrap799 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap799 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap799", MT_bootstrap799 ());
    }

    private static MethodHandle INDY_call799;
    private static MethodHandle INDY_call799 () throws Throwable {
        if (INDY_call799 != null) return INDY_call799;
        CallSite cs = (CallSite) MH_bootstrap799 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap799 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper799 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call799 ().invokeExact(o1, o2, o3); }

    static Object bootstrap799 (Object l, Object n, Object t) throws Throwable { return _mh[ 799 ].invokeExact(l, n, t); }

    // 800
    private static MethodType MT_bootstrap800 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap800 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap800", MT_bootstrap800 ());
    }

    private static MethodHandle INDY_call800;
    private static MethodHandle INDY_call800 () throws Throwable {
        if (INDY_call800 != null) return INDY_call800;
        CallSite cs = (CallSite) MH_bootstrap800 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap800 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper800 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call800 ().invokeExact(o1, o2, o3); }

    static Object bootstrap800 (Object l, Object n, Object t) throws Throwable { return _mh[ 800 ].invokeExact(l, n, t); }

    // 801
    private static MethodType MT_bootstrap801 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap801 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap801", MT_bootstrap801 ());
    }

    private static MethodHandle INDY_call801;
    private static MethodHandle INDY_call801 () throws Throwable {
        if (INDY_call801 != null) return INDY_call801;
        CallSite cs = (CallSite) MH_bootstrap801 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap801 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper801 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call801 ().invokeExact(o1, o2, o3); }

    static Object bootstrap801 (Object l, Object n, Object t) throws Throwable { return _mh[ 801 ].invokeExact(l, n, t); }

    // 802
    private static MethodType MT_bootstrap802 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap802 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap802", MT_bootstrap802 ());
    }

    private static MethodHandle INDY_call802;
    private static MethodHandle INDY_call802 () throws Throwable {
        if (INDY_call802 != null) return INDY_call802;
        CallSite cs = (CallSite) MH_bootstrap802 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap802 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper802 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call802 ().invokeExact(o1, o2, o3); }

    static Object bootstrap802 (Object l, Object n, Object t) throws Throwable { return _mh[ 802 ].invokeExact(l, n, t); }

    // 803
    private static MethodType MT_bootstrap803 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap803 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap803", MT_bootstrap803 ());
    }

    private static MethodHandle INDY_call803;
    private static MethodHandle INDY_call803 () throws Throwable {
        if (INDY_call803 != null) return INDY_call803;
        CallSite cs = (CallSite) MH_bootstrap803 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap803 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper803 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call803 ().invokeExact(o1, o2, o3); }

    static Object bootstrap803 (Object l, Object n, Object t) throws Throwable { return _mh[ 803 ].invokeExact(l, n, t); }

    // 804
    private static MethodType MT_bootstrap804 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap804 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap804", MT_bootstrap804 ());
    }

    private static MethodHandle INDY_call804;
    private static MethodHandle INDY_call804 () throws Throwable {
        if (INDY_call804 != null) return INDY_call804;
        CallSite cs = (CallSite) MH_bootstrap804 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap804 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper804 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call804 ().invokeExact(o1, o2, o3); }

    static Object bootstrap804 (Object l, Object n, Object t) throws Throwable { return _mh[ 804 ].invokeExact(l, n, t); }

    // 805
    private static MethodType MT_bootstrap805 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap805 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap805", MT_bootstrap805 ());
    }

    private static MethodHandle INDY_call805;
    private static MethodHandle INDY_call805 () throws Throwable {
        if (INDY_call805 != null) return INDY_call805;
        CallSite cs = (CallSite) MH_bootstrap805 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap805 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper805 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call805 ().invokeExact(o1, o2, o3); }

    static Object bootstrap805 (Object l, Object n, Object t) throws Throwable { return _mh[ 805 ].invokeExact(l, n, t); }

    // 806
    private static MethodType MT_bootstrap806 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap806 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap806", MT_bootstrap806 ());
    }

    private static MethodHandle INDY_call806;
    private static MethodHandle INDY_call806 () throws Throwable {
        if (INDY_call806 != null) return INDY_call806;
        CallSite cs = (CallSite) MH_bootstrap806 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap806 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper806 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call806 ().invokeExact(o1, o2, o3); }

    static Object bootstrap806 (Object l, Object n, Object t) throws Throwable { return _mh[ 806 ].invokeExact(l, n, t); }

    // 807
    private static MethodType MT_bootstrap807 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap807 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap807", MT_bootstrap807 ());
    }

    private static MethodHandle INDY_call807;
    private static MethodHandle INDY_call807 () throws Throwable {
        if (INDY_call807 != null) return INDY_call807;
        CallSite cs = (CallSite) MH_bootstrap807 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap807 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper807 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call807 ().invokeExact(o1, o2, o3); }

    static Object bootstrap807 (Object l, Object n, Object t) throws Throwable { return _mh[ 807 ].invokeExact(l, n, t); }

    // 808
    private static MethodType MT_bootstrap808 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap808 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap808", MT_bootstrap808 ());
    }

    private static MethodHandle INDY_call808;
    private static MethodHandle INDY_call808 () throws Throwable {
        if (INDY_call808 != null) return INDY_call808;
        CallSite cs = (CallSite) MH_bootstrap808 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap808 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper808 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call808 ().invokeExact(o1, o2, o3); }

    static Object bootstrap808 (Object l, Object n, Object t) throws Throwable { return _mh[ 808 ].invokeExact(l, n, t); }

    // 809
    private static MethodType MT_bootstrap809 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap809 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap809", MT_bootstrap809 ());
    }

    private static MethodHandle INDY_call809;
    private static MethodHandle INDY_call809 () throws Throwable {
        if (INDY_call809 != null) return INDY_call809;
        CallSite cs = (CallSite) MH_bootstrap809 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap809 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper809 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call809 ().invokeExact(o1, o2, o3); }

    static Object bootstrap809 (Object l, Object n, Object t) throws Throwable { return _mh[ 809 ].invokeExact(l, n, t); }

    // 810
    private static MethodType MT_bootstrap810 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap810 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap810", MT_bootstrap810 ());
    }

    private static MethodHandle INDY_call810;
    private static MethodHandle INDY_call810 () throws Throwable {
        if (INDY_call810 != null) return INDY_call810;
        CallSite cs = (CallSite) MH_bootstrap810 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap810 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper810 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call810 ().invokeExact(o1, o2, o3); }

    static Object bootstrap810 (Object l, Object n, Object t) throws Throwable { return _mh[ 810 ].invokeExact(l, n, t); }

    // 811
    private static MethodType MT_bootstrap811 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap811 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap811", MT_bootstrap811 ());
    }

    private static MethodHandle INDY_call811;
    private static MethodHandle INDY_call811 () throws Throwable {
        if (INDY_call811 != null) return INDY_call811;
        CallSite cs = (CallSite) MH_bootstrap811 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap811 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper811 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call811 ().invokeExact(o1, o2, o3); }

    static Object bootstrap811 (Object l, Object n, Object t) throws Throwable { return _mh[ 811 ].invokeExact(l, n, t); }

    // 812
    private static MethodType MT_bootstrap812 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap812 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap812", MT_bootstrap812 ());
    }

    private static MethodHandle INDY_call812;
    private static MethodHandle INDY_call812 () throws Throwable {
        if (INDY_call812 != null) return INDY_call812;
        CallSite cs = (CallSite) MH_bootstrap812 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap812 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper812 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call812 ().invokeExact(o1, o2, o3); }

    static Object bootstrap812 (Object l, Object n, Object t) throws Throwable { return _mh[ 812 ].invokeExact(l, n, t); }

    // 813
    private static MethodType MT_bootstrap813 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap813 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap813", MT_bootstrap813 ());
    }

    private static MethodHandle INDY_call813;
    private static MethodHandle INDY_call813 () throws Throwable {
        if (INDY_call813 != null) return INDY_call813;
        CallSite cs = (CallSite) MH_bootstrap813 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap813 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper813 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call813 ().invokeExact(o1, o2, o3); }

    static Object bootstrap813 (Object l, Object n, Object t) throws Throwable { return _mh[ 813 ].invokeExact(l, n, t); }

    // 814
    private static MethodType MT_bootstrap814 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap814 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap814", MT_bootstrap814 ());
    }

    private static MethodHandle INDY_call814;
    private static MethodHandle INDY_call814 () throws Throwable {
        if (INDY_call814 != null) return INDY_call814;
        CallSite cs = (CallSite) MH_bootstrap814 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap814 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper814 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call814 ().invokeExact(o1, o2, o3); }

    static Object bootstrap814 (Object l, Object n, Object t) throws Throwable { return _mh[ 814 ].invokeExact(l, n, t); }

    // 815
    private static MethodType MT_bootstrap815 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap815 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap815", MT_bootstrap815 ());
    }

    private static MethodHandle INDY_call815;
    private static MethodHandle INDY_call815 () throws Throwable {
        if (INDY_call815 != null) return INDY_call815;
        CallSite cs = (CallSite) MH_bootstrap815 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap815 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper815 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call815 ().invokeExact(o1, o2, o3); }

    static Object bootstrap815 (Object l, Object n, Object t) throws Throwable { return _mh[ 815 ].invokeExact(l, n, t); }

    // 816
    private static MethodType MT_bootstrap816 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap816 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap816", MT_bootstrap816 ());
    }

    private static MethodHandle INDY_call816;
    private static MethodHandle INDY_call816 () throws Throwable {
        if (INDY_call816 != null) return INDY_call816;
        CallSite cs = (CallSite) MH_bootstrap816 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap816 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper816 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call816 ().invokeExact(o1, o2, o3); }

    static Object bootstrap816 (Object l, Object n, Object t) throws Throwable { return _mh[ 816 ].invokeExact(l, n, t); }

    // 817
    private static MethodType MT_bootstrap817 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap817 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap817", MT_bootstrap817 ());
    }

    private static MethodHandle INDY_call817;
    private static MethodHandle INDY_call817 () throws Throwable {
        if (INDY_call817 != null) return INDY_call817;
        CallSite cs = (CallSite) MH_bootstrap817 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap817 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper817 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call817 ().invokeExact(o1, o2, o3); }

    static Object bootstrap817 (Object l, Object n, Object t) throws Throwable { return _mh[ 817 ].invokeExact(l, n, t); }

    // 818
    private static MethodType MT_bootstrap818 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap818 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap818", MT_bootstrap818 ());
    }

    private static MethodHandle INDY_call818;
    private static MethodHandle INDY_call818 () throws Throwable {
        if (INDY_call818 != null) return INDY_call818;
        CallSite cs = (CallSite) MH_bootstrap818 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap818 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper818 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call818 ().invokeExact(o1, o2, o3); }

    static Object bootstrap818 (Object l, Object n, Object t) throws Throwable { return _mh[ 818 ].invokeExact(l, n, t); }

    // 819
    private static MethodType MT_bootstrap819 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap819 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap819", MT_bootstrap819 ());
    }

    private static MethodHandle INDY_call819;
    private static MethodHandle INDY_call819 () throws Throwable {
        if (INDY_call819 != null) return INDY_call819;
        CallSite cs = (CallSite) MH_bootstrap819 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap819 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper819 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call819 ().invokeExact(o1, o2, o3); }

    static Object bootstrap819 (Object l, Object n, Object t) throws Throwable { return _mh[ 819 ].invokeExact(l, n, t); }

    // 820
    private static MethodType MT_bootstrap820 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap820 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap820", MT_bootstrap820 ());
    }

    private static MethodHandle INDY_call820;
    private static MethodHandle INDY_call820 () throws Throwable {
        if (INDY_call820 != null) return INDY_call820;
        CallSite cs = (CallSite) MH_bootstrap820 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap820 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper820 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call820 ().invokeExact(o1, o2, o3); }

    static Object bootstrap820 (Object l, Object n, Object t) throws Throwable { return _mh[ 820 ].invokeExact(l, n, t); }

    // 821
    private static MethodType MT_bootstrap821 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap821 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap821", MT_bootstrap821 ());
    }

    private static MethodHandle INDY_call821;
    private static MethodHandle INDY_call821 () throws Throwable {
        if (INDY_call821 != null) return INDY_call821;
        CallSite cs = (CallSite) MH_bootstrap821 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap821 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper821 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call821 ().invokeExact(o1, o2, o3); }

    static Object bootstrap821 (Object l, Object n, Object t) throws Throwable { return _mh[ 821 ].invokeExact(l, n, t); }

    // 822
    private static MethodType MT_bootstrap822 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap822 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap822", MT_bootstrap822 ());
    }

    private static MethodHandle INDY_call822;
    private static MethodHandle INDY_call822 () throws Throwable {
        if (INDY_call822 != null) return INDY_call822;
        CallSite cs = (CallSite) MH_bootstrap822 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap822 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper822 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call822 ().invokeExact(o1, o2, o3); }

    static Object bootstrap822 (Object l, Object n, Object t) throws Throwable { return _mh[ 822 ].invokeExact(l, n, t); }

    // 823
    private static MethodType MT_bootstrap823 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap823 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap823", MT_bootstrap823 ());
    }

    private static MethodHandle INDY_call823;
    private static MethodHandle INDY_call823 () throws Throwable {
        if (INDY_call823 != null) return INDY_call823;
        CallSite cs = (CallSite) MH_bootstrap823 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap823 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper823 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call823 ().invokeExact(o1, o2, o3); }

    static Object bootstrap823 (Object l, Object n, Object t) throws Throwable { return _mh[ 823 ].invokeExact(l, n, t); }

    // 824
    private static MethodType MT_bootstrap824 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap824 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap824", MT_bootstrap824 ());
    }

    private static MethodHandle INDY_call824;
    private static MethodHandle INDY_call824 () throws Throwable {
        if (INDY_call824 != null) return INDY_call824;
        CallSite cs = (CallSite) MH_bootstrap824 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap824 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper824 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call824 ().invokeExact(o1, o2, o3); }

    static Object bootstrap824 (Object l, Object n, Object t) throws Throwable { return _mh[ 824 ].invokeExact(l, n, t); }

    // 825
    private static MethodType MT_bootstrap825 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap825 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap825", MT_bootstrap825 ());
    }

    private static MethodHandle INDY_call825;
    private static MethodHandle INDY_call825 () throws Throwable {
        if (INDY_call825 != null) return INDY_call825;
        CallSite cs = (CallSite) MH_bootstrap825 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap825 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper825 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call825 ().invokeExact(o1, o2, o3); }

    static Object bootstrap825 (Object l, Object n, Object t) throws Throwable { return _mh[ 825 ].invokeExact(l, n, t); }

    // 826
    private static MethodType MT_bootstrap826 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap826 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap826", MT_bootstrap826 ());
    }

    private static MethodHandle INDY_call826;
    private static MethodHandle INDY_call826 () throws Throwable {
        if (INDY_call826 != null) return INDY_call826;
        CallSite cs = (CallSite) MH_bootstrap826 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap826 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper826 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call826 ().invokeExact(o1, o2, o3); }

    static Object bootstrap826 (Object l, Object n, Object t) throws Throwable { return _mh[ 826 ].invokeExact(l, n, t); }

    // 827
    private static MethodType MT_bootstrap827 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap827 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap827", MT_bootstrap827 ());
    }

    private static MethodHandle INDY_call827;
    private static MethodHandle INDY_call827 () throws Throwable {
        if (INDY_call827 != null) return INDY_call827;
        CallSite cs = (CallSite) MH_bootstrap827 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap827 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper827 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call827 ().invokeExact(o1, o2, o3); }

    static Object bootstrap827 (Object l, Object n, Object t) throws Throwable { return _mh[ 827 ].invokeExact(l, n, t); }

    // 828
    private static MethodType MT_bootstrap828 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap828 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap828", MT_bootstrap828 ());
    }

    private static MethodHandle INDY_call828;
    private static MethodHandle INDY_call828 () throws Throwable {
        if (INDY_call828 != null) return INDY_call828;
        CallSite cs = (CallSite) MH_bootstrap828 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap828 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper828 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call828 ().invokeExact(o1, o2, o3); }

    static Object bootstrap828 (Object l, Object n, Object t) throws Throwable { return _mh[ 828 ].invokeExact(l, n, t); }

    // 829
    private static MethodType MT_bootstrap829 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap829 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap829", MT_bootstrap829 ());
    }

    private static MethodHandle INDY_call829;
    private static MethodHandle INDY_call829 () throws Throwable {
        if (INDY_call829 != null) return INDY_call829;
        CallSite cs = (CallSite) MH_bootstrap829 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap829 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper829 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call829 ().invokeExact(o1, o2, o3); }

    static Object bootstrap829 (Object l, Object n, Object t) throws Throwable { return _mh[ 829 ].invokeExact(l, n, t); }

    // 830
    private static MethodType MT_bootstrap830 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap830 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap830", MT_bootstrap830 ());
    }

    private static MethodHandle INDY_call830;
    private static MethodHandle INDY_call830 () throws Throwable {
        if (INDY_call830 != null) return INDY_call830;
        CallSite cs = (CallSite) MH_bootstrap830 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap830 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper830 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call830 ().invokeExact(o1, o2, o3); }

    static Object bootstrap830 (Object l, Object n, Object t) throws Throwable { return _mh[ 830 ].invokeExact(l, n, t); }

    // 831
    private static MethodType MT_bootstrap831 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap831 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap831", MT_bootstrap831 ());
    }

    private static MethodHandle INDY_call831;
    private static MethodHandle INDY_call831 () throws Throwable {
        if (INDY_call831 != null) return INDY_call831;
        CallSite cs = (CallSite) MH_bootstrap831 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap831 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper831 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call831 ().invokeExact(o1, o2, o3); }

    static Object bootstrap831 (Object l, Object n, Object t) throws Throwable { return _mh[ 831 ].invokeExact(l, n, t); }

    // 832
    private static MethodType MT_bootstrap832 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap832 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap832", MT_bootstrap832 ());
    }

    private static MethodHandle INDY_call832;
    private static MethodHandle INDY_call832 () throws Throwable {
        if (INDY_call832 != null) return INDY_call832;
        CallSite cs = (CallSite) MH_bootstrap832 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap832 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper832 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call832 ().invokeExact(o1, o2, o3); }

    static Object bootstrap832 (Object l, Object n, Object t) throws Throwable { return _mh[ 832 ].invokeExact(l, n, t); }

    // 833
    private static MethodType MT_bootstrap833 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap833 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap833", MT_bootstrap833 ());
    }

    private static MethodHandle INDY_call833;
    private static MethodHandle INDY_call833 () throws Throwable {
        if (INDY_call833 != null) return INDY_call833;
        CallSite cs = (CallSite) MH_bootstrap833 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap833 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper833 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call833 ().invokeExact(o1, o2, o3); }

    static Object bootstrap833 (Object l, Object n, Object t) throws Throwable { return _mh[ 833 ].invokeExact(l, n, t); }

    // 834
    private static MethodType MT_bootstrap834 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap834 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap834", MT_bootstrap834 ());
    }

    private static MethodHandle INDY_call834;
    private static MethodHandle INDY_call834 () throws Throwable {
        if (INDY_call834 != null) return INDY_call834;
        CallSite cs = (CallSite) MH_bootstrap834 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap834 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper834 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call834 ().invokeExact(o1, o2, o3); }

    static Object bootstrap834 (Object l, Object n, Object t) throws Throwable { return _mh[ 834 ].invokeExact(l, n, t); }

    // 835
    private static MethodType MT_bootstrap835 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap835 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap835", MT_bootstrap835 ());
    }

    private static MethodHandle INDY_call835;
    private static MethodHandle INDY_call835 () throws Throwable {
        if (INDY_call835 != null) return INDY_call835;
        CallSite cs = (CallSite) MH_bootstrap835 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap835 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper835 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call835 ().invokeExact(o1, o2, o3); }

    static Object bootstrap835 (Object l, Object n, Object t) throws Throwable { return _mh[ 835 ].invokeExact(l, n, t); }

    // 836
    private static MethodType MT_bootstrap836 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap836 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap836", MT_bootstrap836 ());
    }

    private static MethodHandle INDY_call836;
    private static MethodHandle INDY_call836 () throws Throwable {
        if (INDY_call836 != null) return INDY_call836;
        CallSite cs = (CallSite) MH_bootstrap836 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap836 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper836 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call836 ().invokeExact(o1, o2, o3); }

    static Object bootstrap836 (Object l, Object n, Object t) throws Throwable { return _mh[ 836 ].invokeExact(l, n, t); }

    // 837
    private static MethodType MT_bootstrap837 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap837 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap837", MT_bootstrap837 ());
    }

    private static MethodHandle INDY_call837;
    private static MethodHandle INDY_call837 () throws Throwable {
        if (INDY_call837 != null) return INDY_call837;
        CallSite cs = (CallSite) MH_bootstrap837 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap837 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper837 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call837 ().invokeExact(o1, o2, o3); }

    static Object bootstrap837 (Object l, Object n, Object t) throws Throwable { return _mh[ 837 ].invokeExact(l, n, t); }

    // 838
    private static MethodType MT_bootstrap838 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap838 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap838", MT_bootstrap838 ());
    }

    private static MethodHandle INDY_call838;
    private static MethodHandle INDY_call838 () throws Throwable {
        if (INDY_call838 != null) return INDY_call838;
        CallSite cs = (CallSite) MH_bootstrap838 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap838 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper838 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call838 ().invokeExact(o1, o2, o3); }

    static Object bootstrap838 (Object l, Object n, Object t) throws Throwable { return _mh[ 838 ].invokeExact(l, n, t); }

    // 839
    private static MethodType MT_bootstrap839 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap839 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap839", MT_bootstrap839 ());
    }

    private static MethodHandle INDY_call839;
    private static MethodHandle INDY_call839 () throws Throwable {
        if (INDY_call839 != null) return INDY_call839;
        CallSite cs = (CallSite) MH_bootstrap839 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap839 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper839 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call839 ().invokeExact(o1, o2, o3); }

    static Object bootstrap839 (Object l, Object n, Object t) throws Throwable { return _mh[ 839 ].invokeExact(l, n, t); }

    // 840
    private static MethodType MT_bootstrap840 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap840 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap840", MT_bootstrap840 ());
    }

    private static MethodHandle INDY_call840;
    private static MethodHandle INDY_call840 () throws Throwable {
        if (INDY_call840 != null) return INDY_call840;
        CallSite cs = (CallSite) MH_bootstrap840 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap840 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper840 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call840 ().invokeExact(o1, o2, o3); }

    static Object bootstrap840 (Object l, Object n, Object t) throws Throwable { return _mh[ 840 ].invokeExact(l, n, t); }

    // 841
    private static MethodType MT_bootstrap841 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap841 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap841", MT_bootstrap841 ());
    }

    private static MethodHandle INDY_call841;
    private static MethodHandle INDY_call841 () throws Throwable {
        if (INDY_call841 != null) return INDY_call841;
        CallSite cs = (CallSite) MH_bootstrap841 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap841 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper841 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call841 ().invokeExact(o1, o2, o3); }

    static Object bootstrap841 (Object l, Object n, Object t) throws Throwable { return _mh[ 841 ].invokeExact(l, n, t); }

    // 842
    private static MethodType MT_bootstrap842 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap842 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap842", MT_bootstrap842 ());
    }

    private static MethodHandle INDY_call842;
    private static MethodHandle INDY_call842 () throws Throwable {
        if (INDY_call842 != null) return INDY_call842;
        CallSite cs = (CallSite) MH_bootstrap842 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap842 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper842 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call842 ().invokeExact(o1, o2, o3); }

    static Object bootstrap842 (Object l, Object n, Object t) throws Throwable { return _mh[ 842 ].invokeExact(l, n, t); }

    // 843
    private static MethodType MT_bootstrap843 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap843 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap843", MT_bootstrap843 ());
    }

    private static MethodHandle INDY_call843;
    private static MethodHandle INDY_call843 () throws Throwable {
        if (INDY_call843 != null) return INDY_call843;
        CallSite cs = (CallSite) MH_bootstrap843 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap843 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper843 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call843 ().invokeExact(o1, o2, o3); }

    static Object bootstrap843 (Object l, Object n, Object t) throws Throwable { return _mh[ 843 ].invokeExact(l, n, t); }

    // 844
    private static MethodType MT_bootstrap844 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap844 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap844", MT_bootstrap844 ());
    }

    private static MethodHandle INDY_call844;
    private static MethodHandle INDY_call844 () throws Throwable {
        if (INDY_call844 != null) return INDY_call844;
        CallSite cs = (CallSite) MH_bootstrap844 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap844 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper844 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call844 ().invokeExact(o1, o2, o3); }

    static Object bootstrap844 (Object l, Object n, Object t) throws Throwable { return _mh[ 844 ].invokeExact(l, n, t); }

    // 845
    private static MethodType MT_bootstrap845 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap845 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap845", MT_bootstrap845 ());
    }

    private static MethodHandle INDY_call845;
    private static MethodHandle INDY_call845 () throws Throwable {
        if (INDY_call845 != null) return INDY_call845;
        CallSite cs = (CallSite) MH_bootstrap845 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap845 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper845 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call845 ().invokeExact(o1, o2, o3); }

    static Object bootstrap845 (Object l, Object n, Object t) throws Throwable { return _mh[ 845 ].invokeExact(l, n, t); }

    // 846
    private static MethodType MT_bootstrap846 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap846 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap846", MT_bootstrap846 ());
    }

    private static MethodHandle INDY_call846;
    private static MethodHandle INDY_call846 () throws Throwable {
        if (INDY_call846 != null) return INDY_call846;
        CallSite cs = (CallSite) MH_bootstrap846 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap846 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper846 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call846 ().invokeExact(o1, o2, o3); }

    static Object bootstrap846 (Object l, Object n, Object t) throws Throwable { return _mh[ 846 ].invokeExact(l, n, t); }

    // 847
    private static MethodType MT_bootstrap847 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap847 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap847", MT_bootstrap847 ());
    }

    private static MethodHandle INDY_call847;
    private static MethodHandle INDY_call847 () throws Throwable {
        if (INDY_call847 != null) return INDY_call847;
        CallSite cs = (CallSite) MH_bootstrap847 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap847 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper847 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call847 ().invokeExact(o1, o2, o3); }

    static Object bootstrap847 (Object l, Object n, Object t) throws Throwable { return _mh[ 847 ].invokeExact(l, n, t); }

    // 848
    private static MethodType MT_bootstrap848 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap848 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap848", MT_bootstrap848 ());
    }

    private static MethodHandle INDY_call848;
    private static MethodHandle INDY_call848 () throws Throwable {
        if (INDY_call848 != null) return INDY_call848;
        CallSite cs = (CallSite) MH_bootstrap848 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap848 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper848 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call848 ().invokeExact(o1, o2, o3); }

    static Object bootstrap848 (Object l, Object n, Object t) throws Throwable { return _mh[ 848 ].invokeExact(l, n, t); }

    // 849
    private static MethodType MT_bootstrap849 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap849 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap849", MT_bootstrap849 ());
    }

    private static MethodHandle INDY_call849;
    private static MethodHandle INDY_call849 () throws Throwable {
        if (INDY_call849 != null) return INDY_call849;
        CallSite cs = (CallSite) MH_bootstrap849 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap849 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper849 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call849 ().invokeExact(o1, o2, o3); }

    static Object bootstrap849 (Object l, Object n, Object t) throws Throwable { return _mh[ 849 ].invokeExact(l, n, t); }

    // 850
    private static MethodType MT_bootstrap850 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap850 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap850", MT_bootstrap850 ());
    }

    private static MethodHandle INDY_call850;
    private static MethodHandle INDY_call850 () throws Throwable {
        if (INDY_call850 != null) return INDY_call850;
        CallSite cs = (CallSite) MH_bootstrap850 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap850 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper850 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call850 ().invokeExact(o1, o2, o3); }

    static Object bootstrap850 (Object l, Object n, Object t) throws Throwable { return _mh[ 850 ].invokeExact(l, n, t); }

    // 851
    private static MethodType MT_bootstrap851 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap851 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap851", MT_bootstrap851 ());
    }

    private static MethodHandle INDY_call851;
    private static MethodHandle INDY_call851 () throws Throwable {
        if (INDY_call851 != null) return INDY_call851;
        CallSite cs = (CallSite) MH_bootstrap851 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap851 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper851 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call851 ().invokeExact(o1, o2, o3); }

    static Object bootstrap851 (Object l, Object n, Object t) throws Throwable { return _mh[ 851 ].invokeExact(l, n, t); }

    // 852
    private static MethodType MT_bootstrap852 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap852 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap852", MT_bootstrap852 ());
    }

    private static MethodHandle INDY_call852;
    private static MethodHandle INDY_call852 () throws Throwable {
        if (INDY_call852 != null) return INDY_call852;
        CallSite cs = (CallSite) MH_bootstrap852 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap852 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper852 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call852 ().invokeExact(o1, o2, o3); }

    static Object bootstrap852 (Object l, Object n, Object t) throws Throwable { return _mh[ 852 ].invokeExact(l, n, t); }

    // 853
    private static MethodType MT_bootstrap853 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap853 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap853", MT_bootstrap853 ());
    }

    private static MethodHandle INDY_call853;
    private static MethodHandle INDY_call853 () throws Throwable {
        if (INDY_call853 != null) return INDY_call853;
        CallSite cs = (CallSite) MH_bootstrap853 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap853 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper853 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call853 ().invokeExact(o1, o2, o3); }

    static Object bootstrap853 (Object l, Object n, Object t) throws Throwable { return _mh[ 853 ].invokeExact(l, n, t); }

    // 854
    private static MethodType MT_bootstrap854 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap854 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap854", MT_bootstrap854 ());
    }

    private static MethodHandle INDY_call854;
    private static MethodHandle INDY_call854 () throws Throwable {
        if (INDY_call854 != null) return INDY_call854;
        CallSite cs = (CallSite) MH_bootstrap854 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap854 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper854 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call854 ().invokeExact(o1, o2, o3); }

    static Object bootstrap854 (Object l, Object n, Object t) throws Throwable { return _mh[ 854 ].invokeExact(l, n, t); }

    // 855
    private static MethodType MT_bootstrap855 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap855 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap855", MT_bootstrap855 ());
    }

    private static MethodHandle INDY_call855;
    private static MethodHandle INDY_call855 () throws Throwable {
        if (INDY_call855 != null) return INDY_call855;
        CallSite cs = (CallSite) MH_bootstrap855 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap855 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper855 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call855 ().invokeExact(o1, o2, o3); }

    static Object bootstrap855 (Object l, Object n, Object t) throws Throwable { return _mh[ 855 ].invokeExact(l, n, t); }

    // 856
    private static MethodType MT_bootstrap856 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap856 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap856", MT_bootstrap856 ());
    }

    private static MethodHandle INDY_call856;
    private static MethodHandle INDY_call856 () throws Throwable {
        if (INDY_call856 != null) return INDY_call856;
        CallSite cs = (CallSite) MH_bootstrap856 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap856 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper856 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call856 ().invokeExact(o1, o2, o3); }

    static Object bootstrap856 (Object l, Object n, Object t) throws Throwable { return _mh[ 856 ].invokeExact(l, n, t); }

    // 857
    private static MethodType MT_bootstrap857 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap857 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap857", MT_bootstrap857 ());
    }

    private static MethodHandle INDY_call857;
    private static MethodHandle INDY_call857 () throws Throwable {
        if (INDY_call857 != null) return INDY_call857;
        CallSite cs = (CallSite) MH_bootstrap857 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap857 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper857 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call857 ().invokeExact(o1, o2, o3); }

    static Object bootstrap857 (Object l, Object n, Object t) throws Throwable { return _mh[ 857 ].invokeExact(l, n, t); }

    // 858
    private static MethodType MT_bootstrap858 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap858 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap858", MT_bootstrap858 ());
    }

    private static MethodHandle INDY_call858;
    private static MethodHandle INDY_call858 () throws Throwable {
        if (INDY_call858 != null) return INDY_call858;
        CallSite cs = (CallSite) MH_bootstrap858 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap858 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper858 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call858 ().invokeExact(o1, o2, o3); }

    static Object bootstrap858 (Object l, Object n, Object t) throws Throwable { return _mh[ 858 ].invokeExact(l, n, t); }

    // 859
    private static MethodType MT_bootstrap859 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap859 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap859", MT_bootstrap859 ());
    }

    private static MethodHandle INDY_call859;
    private static MethodHandle INDY_call859 () throws Throwable {
        if (INDY_call859 != null) return INDY_call859;
        CallSite cs = (CallSite) MH_bootstrap859 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap859 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper859 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call859 ().invokeExact(o1, o2, o3); }

    static Object bootstrap859 (Object l, Object n, Object t) throws Throwable { return _mh[ 859 ].invokeExact(l, n, t); }

    // 860
    private static MethodType MT_bootstrap860 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap860 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap860", MT_bootstrap860 ());
    }

    private static MethodHandle INDY_call860;
    private static MethodHandle INDY_call860 () throws Throwable {
        if (INDY_call860 != null) return INDY_call860;
        CallSite cs = (CallSite) MH_bootstrap860 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap860 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper860 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call860 ().invokeExact(o1, o2, o3); }

    static Object bootstrap860 (Object l, Object n, Object t) throws Throwable { return _mh[ 860 ].invokeExact(l, n, t); }

    // 861
    private static MethodType MT_bootstrap861 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap861 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap861", MT_bootstrap861 ());
    }

    private static MethodHandle INDY_call861;
    private static MethodHandle INDY_call861 () throws Throwable {
        if (INDY_call861 != null) return INDY_call861;
        CallSite cs = (CallSite) MH_bootstrap861 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap861 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper861 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call861 ().invokeExact(o1, o2, o3); }

    static Object bootstrap861 (Object l, Object n, Object t) throws Throwable { return _mh[ 861 ].invokeExact(l, n, t); }

    // 862
    private static MethodType MT_bootstrap862 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap862 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap862", MT_bootstrap862 ());
    }

    private static MethodHandle INDY_call862;
    private static MethodHandle INDY_call862 () throws Throwable {
        if (INDY_call862 != null) return INDY_call862;
        CallSite cs = (CallSite) MH_bootstrap862 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap862 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper862 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call862 ().invokeExact(o1, o2, o3); }

    static Object bootstrap862 (Object l, Object n, Object t) throws Throwable { return _mh[ 862 ].invokeExact(l, n, t); }

    // 863
    private static MethodType MT_bootstrap863 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap863 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap863", MT_bootstrap863 ());
    }

    private static MethodHandle INDY_call863;
    private static MethodHandle INDY_call863 () throws Throwable {
        if (INDY_call863 != null) return INDY_call863;
        CallSite cs = (CallSite) MH_bootstrap863 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap863 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper863 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call863 ().invokeExact(o1, o2, o3); }

    static Object bootstrap863 (Object l, Object n, Object t) throws Throwable { return _mh[ 863 ].invokeExact(l, n, t); }

    // 864
    private static MethodType MT_bootstrap864 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap864 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap864", MT_bootstrap864 ());
    }

    private static MethodHandle INDY_call864;
    private static MethodHandle INDY_call864 () throws Throwable {
        if (INDY_call864 != null) return INDY_call864;
        CallSite cs = (CallSite) MH_bootstrap864 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap864 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper864 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call864 ().invokeExact(o1, o2, o3); }

    static Object bootstrap864 (Object l, Object n, Object t) throws Throwable { return _mh[ 864 ].invokeExact(l, n, t); }

    // 865
    private static MethodType MT_bootstrap865 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap865 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap865", MT_bootstrap865 ());
    }

    private static MethodHandle INDY_call865;
    private static MethodHandle INDY_call865 () throws Throwable {
        if (INDY_call865 != null) return INDY_call865;
        CallSite cs = (CallSite) MH_bootstrap865 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap865 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper865 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call865 ().invokeExact(o1, o2, o3); }

    static Object bootstrap865 (Object l, Object n, Object t) throws Throwable { return _mh[ 865 ].invokeExact(l, n, t); }

    // 866
    private static MethodType MT_bootstrap866 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap866 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap866", MT_bootstrap866 ());
    }

    private static MethodHandle INDY_call866;
    private static MethodHandle INDY_call866 () throws Throwable {
        if (INDY_call866 != null) return INDY_call866;
        CallSite cs = (CallSite) MH_bootstrap866 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap866 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper866 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call866 ().invokeExact(o1, o2, o3); }

    static Object bootstrap866 (Object l, Object n, Object t) throws Throwable { return _mh[ 866 ].invokeExact(l, n, t); }

    // 867
    private static MethodType MT_bootstrap867 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap867 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap867", MT_bootstrap867 ());
    }

    private static MethodHandle INDY_call867;
    private static MethodHandle INDY_call867 () throws Throwable {
        if (INDY_call867 != null) return INDY_call867;
        CallSite cs = (CallSite) MH_bootstrap867 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap867 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper867 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call867 ().invokeExact(o1, o2, o3); }

    static Object bootstrap867 (Object l, Object n, Object t) throws Throwable { return _mh[ 867 ].invokeExact(l, n, t); }

    // 868
    private static MethodType MT_bootstrap868 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap868 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap868", MT_bootstrap868 ());
    }

    private static MethodHandle INDY_call868;
    private static MethodHandle INDY_call868 () throws Throwable {
        if (INDY_call868 != null) return INDY_call868;
        CallSite cs = (CallSite) MH_bootstrap868 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap868 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper868 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call868 ().invokeExact(o1, o2, o3); }

    static Object bootstrap868 (Object l, Object n, Object t) throws Throwable { return _mh[ 868 ].invokeExact(l, n, t); }

    // 869
    private static MethodType MT_bootstrap869 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap869 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap869", MT_bootstrap869 ());
    }

    private static MethodHandle INDY_call869;
    private static MethodHandle INDY_call869 () throws Throwable {
        if (INDY_call869 != null) return INDY_call869;
        CallSite cs = (CallSite) MH_bootstrap869 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap869 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper869 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call869 ().invokeExact(o1, o2, o3); }

    static Object bootstrap869 (Object l, Object n, Object t) throws Throwable { return _mh[ 869 ].invokeExact(l, n, t); }

    // 870
    private static MethodType MT_bootstrap870 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap870 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap870", MT_bootstrap870 ());
    }

    private static MethodHandle INDY_call870;
    private static MethodHandle INDY_call870 () throws Throwable {
        if (INDY_call870 != null) return INDY_call870;
        CallSite cs = (CallSite) MH_bootstrap870 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap870 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper870 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call870 ().invokeExact(o1, o2, o3); }

    static Object bootstrap870 (Object l, Object n, Object t) throws Throwable { return _mh[ 870 ].invokeExact(l, n, t); }

    // 871
    private static MethodType MT_bootstrap871 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap871 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap871", MT_bootstrap871 ());
    }

    private static MethodHandle INDY_call871;
    private static MethodHandle INDY_call871 () throws Throwable {
        if (INDY_call871 != null) return INDY_call871;
        CallSite cs = (CallSite) MH_bootstrap871 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap871 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper871 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call871 ().invokeExact(o1, o2, o3); }

    static Object bootstrap871 (Object l, Object n, Object t) throws Throwable { return _mh[ 871 ].invokeExact(l, n, t); }

    // 872
    private static MethodType MT_bootstrap872 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap872 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap872", MT_bootstrap872 ());
    }

    private static MethodHandle INDY_call872;
    private static MethodHandle INDY_call872 () throws Throwable {
        if (INDY_call872 != null) return INDY_call872;
        CallSite cs = (CallSite) MH_bootstrap872 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap872 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper872 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call872 ().invokeExact(o1, o2, o3); }

    static Object bootstrap872 (Object l, Object n, Object t) throws Throwable { return _mh[ 872 ].invokeExact(l, n, t); }

    // 873
    private static MethodType MT_bootstrap873 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap873 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap873", MT_bootstrap873 ());
    }

    private static MethodHandle INDY_call873;
    private static MethodHandle INDY_call873 () throws Throwable {
        if (INDY_call873 != null) return INDY_call873;
        CallSite cs = (CallSite) MH_bootstrap873 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap873 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper873 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call873 ().invokeExact(o1, o2, o3); }

    static Object bootstrap873 (Object l, Object n, Object t) throws Throwable { return _mh[ 873 ].invokeExact(l, n, t); }

    // 874
    private static MethodType MT_bootstrap874 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap874 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap874", MT_bootstrap874 ());
    }

    private static MethodHandle INDY_call874;
    private static MethodHandle INDY_call874 () throws Throwable {
        if (INDY_call874 != null) return INDY_call874;
        CallSite cs = (CallSite) MH_bootstrap874 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap874 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper874 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call874 ().invokeExact(o1, o2, o3); }

    static Object bootstrap874 (Object l, Object n, Object t) throws Throwable { return _mh[ 874 ].invokeExact(l, n, t); }

    // 875
    private static MethodType MT_bootstrap875 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap875 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap875", MT_bootstrap875 ());
    }

    private static MethodHandle INDY_call875;
    private static MethodHandle INDY_call875 () throws Throwable {
        if (INDY_call875 != null) return INDY_call875;
        CallSite cs = (CallSite) MH_bootstrap875 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap875 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper875 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call875 ().invokeExact(o1, o2, o3); }

    static Object bootstrap875 (Object l, Object n, Object t) throws Throwable { return _mh[ 875 ].invokeExact(l, n, t); }

    // 876
    private static MethodType MT_bootstrap876 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap876 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap876", MT_bootstrap876 ());
    }

    private static MethodHandle INDY_call876;
    private static MethodHandle INDY_call876 () throws Throwable {
        if (INDY_call876 != null) return INDY_call876;
        CallSite cs = (CallSite) MH_bootstrap876 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap876 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper876 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call876 ().invokeExact(o1, o2, o3); }

    static Object bootstrap876 (Object l, Object n, Object t) throws Throwable { return _mh[ 876 ].invokeExact(l, n, t); }

    // 877
    private static MethodType MT_bootstrap877 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap877 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap877", MT_bootstrap877 ());
    }

    private static MethodHandle INDY_call877;
    private static MethodHandle INDY_call877 () throws Throwable {
        if (INDY_call877 != null) return INDY_call877;
        CallSite cs = (CallSite) MH_bootstrap877 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap877 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper877 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call877 ().invokeExact(o1, o2, o3); }

    static Object bootstrap877 (Object l, Object n, Object t) throws Throwable { return _mh[ 877 ].invokeExact(l, n, t); }

    // 878
    private static MethodType MT_bootstrap878 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap878 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap878", MT_bootstrap878 ());
    }

    private static MethodHandle INDY_call878;
    private static MethodHandle INDY_call878 () throws Throwable {
        if (INDY_call878 != null) return INDY_call878;
        CallSite cs = (CallSite) MH_bootstrap878 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap878 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper878 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call878 ().invokeExact(o1, o2, o3); }

    static Object bootstrap878 (Object l, Object n, Object t) throws Throwable { return _mh[ 878 ].invokeExact(l, n, t); }

    // 879
    private static MethodType MT_bootstrap879 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap879 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap879", MT_bootstrap879 ());
    }

    private static MethodHandle INDY_call879;
    private static MethodHandle INDY_call879 () throws Throwable {
        if (INDY_call879 != null) return INDY_call879;
        CallSite cs = (CallSite) MH_bootstrap879 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap879 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper879 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call879 ().invokeExact(o1, o2, o3); }

    static Object bootstrap879 (Object l, Object n, Object t) throws Throwable { return _mh[ 879 ].invokeExact(l, n, t); }

    // 880
    private static MethodType MT_bootstrap880 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap880 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap880", MT_bootstrap880 ());
    }

    private static MethodHandle INDY_call880;
    private static MethodHandle INDY_call880 () throws Throwable {
        if (INDY_call880 != null) return INDY_call880;
        CallSite cs = (CallSite) MH_bootstrap880 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap880 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper880 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call880 ().invokeExact(o1, o2, o3); }

    static Object bootstrap880 (Object l, Object n, Object t) throws Throwable { return _mh[ 880 ].invokeExact(l, n, t); }

    // 881
    private static MethodType MT_bootstrap881 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap881 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap881", MT_bootstrap881 ());
    }

    private static MethodHandle INDY_call881;
    private static MethodHandle INDY_call881 () throws Throwable {
        if (INDY_call881 != null) return INDY_call881;
        CallSite cs = (CallSite) MH_bootstrap881 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap881 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper881 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call881 ().invokeExact(o1, o2, o3); }

    static Object bootstrap881 (Object l, Object n, Object t) throws Throwable { return _mh[ 881 ].invokeExact(l, n, t); }

    // 882
    private static MethodType MT_bootstrap882 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap882 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap882", MT_bootstrap882 ());
    }

    private static MethodHandle INDY_call882;
    private static MethodHandle INDY_call882 () throws Throwable {
        if (INDY_call882 != null) return INDY_call882;
        CallSite cs = (CallSite) MH_bootstrap882 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap882 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper882 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call882 ().invokeExact(o1, o2, o3); }

    static Object bootstrap882 (Object l, Object n, Object t) throws Throwable { return _mh[ 882 ].invokeExact(l, n, t); }

    // 883
    private static MethodType MT_bootstrap883 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap883 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap883", MT_bootstrap883 ());
    }

    private static MethodHandle INDY_call883;
    private static MethodHandle INDY_call883 () throws Throwable {
        if (INDY_call883 != null) return INDY_call883;
        CallSite cs = (CallSite) MH_bootstrap883 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap883 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper883 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call883 ().invokeExact(o1, o2, o3); }

    static Object bootstrap883 (Object l, Object n, Object t) throws Throwable { return _mh[ 883 ].invokeExact(l, n, t); }

    // 884
    private static MethodType MT_bootstrap884 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap884 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap884", MT_bootstrap884 ());
    }

    private static MethodHandle INDY_call884;
    private static MethodHandle INDY_call884 () throws Throwable {
        if (INDY_call884 != null) return INDY_call884;
        CallSite cs = (CallSite) MH_bootstrap884 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap884 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper884 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call884 ().invokeExact(o1, o2, o3); }

    static Object bootstrap884 (Object l, Object n, Object t) throws Throwable { return _mh[ 884 ].invokeExact(l, n, t); }

    // 885
    private static MethodType MT_bootstrap885 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap885 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap885", MT_bootstrap885 ());
    }

    private static MethodHandle INDY_call885;
    private static MethodHandle INDY_call885 () throws Throwable {
        if (INDY_call885 != null) return INDY_call885;
        CallSite cs = (CallSite) MH_bootstrap885 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap885 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper885 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call885 ().invokeExact(o1, o2, o3); }

    static Object bootstrap885 (Object l, Object n, Object t) throws Throwable { return _mh[ 885 ].invokeExact(l, n, t); }

    // 886
    private static MethodType MT_bootstrap886 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap886 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap886", MT_bootstrap886 ());
    }

    private static MethodHandle INDY_call886;
    private static MethodHandle INDY_call886 () throws Throwable {
        if (INDY_call886 != null) return INDY_call886;
        CallSite cs = (CallSite) MH_bootstrap886 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap886 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper886 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call886 ().invokeExact(o1, o2, o3); }

    static Object bootstrap886 (Object l, Object n, Object t) throws Throwable { return _mh[ 886 ].invokeExact(l, n, t); }

    // 887
    private static MethodType MT_bootstrap887 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap887 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap887", MT_bootstrap887 ());
    }

    private static MethodHandle INDY_call887;
    private static MethodHandle INDY_call887 () throws Throwable {
        if (INDY_call887 != null) return INDY_call887;
        CallSite cs = (CallSite) MH_bootstrap887 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap887 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper887 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call887 ().invokeExact(o1, o2, o3); }

    static Object bootstrap887 (Object l, Object n, Object t) throws Throwable { return _mh[ 887 ].invokeExact(l, n, t); }

    // 888
    private static MethodType MT_bootstrap888 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap888 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap888", MT_bootstrap888 ());
    }

    private static MethodHandle INDY_call888;
    private static MethodHandle INDY_call888 () throws Throwable {
        if (INDY_call888 != null) return INDY_call888;
        CallSite cs = (CallSite) MH_bootstrap888 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap888 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper888 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call888 ().invokeExact(o1, o2, o3); }

    static Object bootstrap888 (Object l, Object n, Object t) throws Throwable { return _mh[ 888 ].invokeExact(l, n, t); }

    // 889
    private static MethodType MT_bootstrap889 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap889 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap889", MT_bootstrap889 ());
    }

    private static MethodHandle INDY_call889;
    private static MethodHandle INDY_call889 () throws Throwable {
        if (INDY_call889 != null) return INDY_call889;
        CallSite cs = (CallSite) MH_bootstrap889 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap889 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper889 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call889 ().invokeExact(o1, o2, o3); }

    static Object bootstrap889 (Object l, Object n, Object t) throws Throwable { return _mh[ 889 ].invokeExact(l, n, t); }

    // 890
    private static MethodType MT_bootstrap890 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap890 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap890", MT_bootstrap890 ());
    }

    private static MethodHandle INDY_call890;
    private static MethodHandle INDY_call890 () throws Throwable {
        if (INDY_call890 != null) return INDY_call890;
        CallSite cs = (CallSite) MH_bootstrap890 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap890 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper890 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call890 ().invokeExact(o1, o2, o3); }

    static Object bootstrap890 (Object l, Object n, Object t) throws Throwable { return _mh[ 890 ].invokeExact(l, n, t); }

    // 891
    private static MethodType MT_bootstrap891 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap891 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap891", MT_bootstrap891 ());
    }

    private static MethodHandle INDY_call891;
    private static MethodHandle INDY_call891 () throws Throwable {
        if (INDY_call891 != null) return INDY_call891;
        CallSite cs = (CallSite) MH_bootstrap891 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap891 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper891 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call891 ().invokeExact(o1, o2, o3); }

    static Object bootstrap891 (Object l, Object n, Object t) throws Throwable { return _mh[ 891 ].invokeExact(l, n, t); }

    // 892
    private static MethodType MT_bootstrap892 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap892 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap892", MT_bootstrap892 ());
    }

    private static MethodHandle INDY_call892;
    private static MethodHandle INDY_call892 () throws Throwable {
        if (INDY_call892 != null) return INDY_call892;
        CallSite cs = (CallSite) MH_bootstrap892 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap892 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper892 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call892 ().invokeExact(o1, o2, o3); }

    static Object bootstrap892 (Object l, Object n, Object t) throws Throwable { return _mh[ 892 ].invokeExact(l, n, t); }

    // 893
    private static MethodType MT_bootstrap893 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap893 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap893", MT_bootstrap893 ());
    }

    private static MethodHandle INDY_call893;
    private static MethodHandle INDY_call893 () throws Throwable {
        if (INDY_call893 != null) return INDY_call893;
        CallSite cs = (CallSite) MH_bootstrap893 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap893 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper893 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call893 ().invokeExact(o1, o2, o3); }

    static Object bootstrap893 (Object l, Object n, Object t) throws Throwable { return _mh[ 893 ].invokeExact(l, n, t); }

    // 894
    private static MethodType MT_bootstrap894 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap894 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap894", MT_bootstrap894 ());
    }

    private static MethodHandle INDY_call894;
    private static MethodHandle INDY_call894 () throws Throwable {
        if (INDY_call894 != null) return INDY_call894;
        CallSite cs = (CallSite) MH_bootstrap894 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap894 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper894 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call894 ().invokeExact(o1, o2, o3); }

    static Object bootstrap894 (Object l, Object n, Object t) throws Throwable { return _mh[ 894 ].invokeExact(l, n, t); }

    // 895
    private static MethodType MT_bootstrap895 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap895 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap895", MT_bootstrap895 ());
    }

    private static MethodHandle INDY_call895;
    private static MethodHandle INDY_call895 () throws Throwable {
        if (INDY_call895 != null) return INDY_call895;
        CallSite cs = (CallSite) MH_bootstrap895 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap895 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper895 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call895 ().invokeExact(o1, o2, o3); }

    static Object bootstrap895 (Object l, Object n, Object t) throws Throwable { return _mh[ 895 ].invokeExact(l, n, t); }

    // 896
    private static MethodType MT_bootstrap896 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap896 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap896", MT_bootstrap896 ());
    }

    private static MethodHandle INDY_call896;
    private static MethodHandle INDY_call896 () throws Throwable {
        if (INDY_call896 != null) return INDY_call896;
        CallSite cs = (CallSite) MH_bootstrap896 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap896 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper896 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call896 ().invokeExact(o1, o2, o3); }

    static Object bootstrap896 (Object l, Object n, Object t) throws Throwable { return _mh[ 896 ].invokeExact(l, n, t); }

    // 897
    private static MethodType MT_bootstrap897 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap897 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap897", MT_bootstrap897 ());
    }

    private static MethodHandle INDY_call897;
    private static MethodHandle INDY_call897 () throws Throwable {
        if (INDY_call897 != null) return INDY_call897;
        CallSite cs = (CallSite) MH_bootstrap897 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap897 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper897 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call897 ().invokeExact(o1, o2, o3); }

    static Object bootstrap897 (Object l, Object n, Object t) throws Throwable { return _mh[ 897 ].invokeExact(l, n, t); }

    // 898
    private static MethodType MT_bootstrap898 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap898 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap898", MT_bootstrap898 ());
    }

    private static MethodHandle INDY_call898;
    private static MethodHandle INDY_call898 () throws Throwable {
        if (INDY_call898 != null) return INDY_call898;
        CallSite cs = (CallSite) MH_bootstrap898 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap898 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper898 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call898 ().invokeExact(o1, o2, o3); }

    static Object bootstrap898 (Object l, Object n, Object t) throws Throwable { return _mh[ 898 ].invokeExact(l, n, t); }

    // 899
    private static MethodType MT_bootstrap899 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap899 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap899", MT_bootstrap899 ());
    }

    private static MethodHandle INDY_call899;
    private static MethodHandle INDY_call899 () throws Throwable {
        if (INDY_call899 != null) return INDY_call899;
        CallSite cs = (CallSite) MH_bootstrap899 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap899 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper899 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call899 ().invokeExact(o1, o2, o3); }

    static Object bootstrap899 (Object l, Object n, Object t) throws Throwable { return _mh[ 899 ].invokeExact(l, n, t); }

    // 900
    private static MethodType MT_bootstrap900 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap900 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap900", MT_bootstrap900 ());
    }

    private static MethodHandle INDY_call900;
    private static MethodHandle INDY_call900 () throws Throwable {
        if (INDY_call900 != null) return INDY_call900;
        CallSite cs = (CallSite) MH_bootstrap900 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap900 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper900 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call900 ().invokeExact(o1, o2, o3); }

    static Object bootstrap900 (Object l, Object n, Object t) throws Throwable { return _mh[ 900 ].invokeExact(l, n, t); }

    // 901
    private static MethodType MT_bootstrap901 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap901 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap901", MT_bootstrap901 ());
    }

    private static MethodHandle INDY_call901;
    private static MethodHandle INDY_call901 () throws Throwable {
        if (INDY_call901 != null) return INDY_call901;
        CallSite cs = (CallSite) MH_bootstrap901 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap901 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper901 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call901 ().invokeExact(o1, o2, o3); }

    static Object bootstrap901 (Object l, Object n, Object t) throws Throwable { return _mh[ 901 ].invokeExact(l, n, t); }

    // 902
    private static MethodType MT_bootstrap902 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap902 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap902", MT_bootstrap902 ());
    }

    private static MethodHandle INDY_call902;
    private static MethodHandle INDY_call902 () throws Throwable {
        if (INDY_call902 != null) return INDY_call902;
        CallSite cs = (CallSite) MH_bootstrap902 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap902 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper902 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call902 ().invokeExact(o1, o2, o3); }

    static Object bootstrap902 (Object l, Object n, Object t) throws Throwable { return _mh[ 902 ].invokeExact(l, n, t); }

    // 903
    private static MethodType MT_bootstrap903 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap903 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap903", MT_bootstrap903 ());
    }

    private static MethodHandle INDY_call903;
    private static MethodHandle INDY_call903 () throws Throwable {
        if (INDY_call903 != null) return INDY_call903;
        CallSite cs = (CallSite) MH_bootstrap903 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap903 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper903 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call903 ().invokeExact(o1, o2, o3); }

    static Object bootstrap903 (Object l, Object n, Object t) throws Throwable { return _mh[ 903 ].invokeExact(l, n, t); }

    // 904
    private static MethodType MT_bootstrap904 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap904 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap904", MT_bootstrap904 ());
    }

    private static MethodHandle INDY_call904;
    private static MethodHandle INDY_call904 () throws Throwable {
        if (INDY_call904 != null) return INDY_call904;
        CallSite cs = (CallSite) MH_bootstrap904 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap904 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper904 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call904 ().invokeExact(o1, o2, o3); }

    static Object bootstrap904 (Object l, Object n, Object t) throws Throwable { return _mh[ 904 ].invokeExact(l, n, t); }

    // 905
    private static MethodType MT_bootstrap905 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap905 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap905", MT_bootstrap905 ());
    }

    private static MethodHandle INDY_call905;
    private static MethodHandle INDY_call905 () throws Throwable {
        if (INDY_call905 != null) return INDY_call905;
        CallSite cs = (CallSite) MH_bootstrap905 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap905 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper905 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call905 ().invokeExact(o1, o2, o3); }

    static Object bootstrap905 (Object l, Object n, Object t) throws Throwable { return _mh[ 905 ].invokeExact(l, n, t); }

    // 906
    private static MethodType MT_bootstrap906 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap906 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap906", MT_bootstrap906 ());
    }

    private static MethodHandle INDY_call906;
    private static MethodHandle INDY_call906 () throws Throwable {
        if (INDY_call906 != null) return INDY_call906;
        CallSite cs = (CallSite) MH_bootstrap906 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap906 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper906 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call906 ().invokeExact(o1, o2, o3); }

    static Object bootstrap906 (Object l, Object n, Object t) throws Throwable { return _mh[ 906 ].invokeExact(l, n, t); }

    // 907
    private static MethodType MT_bootstrap907 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap907 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap907", MT_bootstrap907 ());
    }

    private static MethodHandle INDY_call907;
    private static MethodHandle INDY_call907 () throws Throwable {
        if (INDY_call907 != null) return INDY_call907;
        CallSite cs = (CallSite) MH_bootstrap907 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap907 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper907 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call907 ().invokeExact(o1, o2, o3); }

    static Object bootstrap907 (Object l, Object n, Object t) throws Throwable { return _mh[ 907 ].invokeExact(l, n, t); }

    // 908
    private static MethodType MT_bootstrap908 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap908 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap908", MT_bootstrap908 ());
    }

    private static MethodHandle INDY_call908;
    private static MethodHandle INDY_call908 () throws Throwable {
        if (INDY_call908 != null) return INDY_call908;
        CallSite cs = (CallSite) MH_bootstrap908 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap908 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper908 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call908 ().invokeExact(o1, o2, o3); }

    static Object bootstrap908 (Object l, Object n, Object t) throws Throwable { return _mh[ 908 ].invokeExact(l, n, t); }

    // 909
    private static MethodType MT_bootstrap909 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap909 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap909", MT_bootstrap909 ());
    }

    private static MethodHandle INDY_call909;
    private static MethodHandle INDY_call909 () throws Throwable {
        if (INDY_call909 != null) return INDY_call909;
        CallSite cs = (CallSite) MH_bootstrap909 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap909 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper909 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call909 ().invokeExact(o1, o2, o3); }

    static Object bootstrap909 (Object l, Object n, Object t) throws Throwable { return _mh[ 909 ].invokeExact(l, n, t); }

    // 910
    private static MethodType MT_bootstrap910 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap910 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap910", MT_bootstrap910 ());
    }

    private static MethodHandle INDY_call910;
    private static MethodHandle INDY_call910 () throws Throwable {
        if (INDY_call910 != null) return INDY_call910;
        CallSite cs = (CallSite) MH_bootstrap910 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap910 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper910 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call910 ().invokeExact(o1, o2, o3); }

    static Object bootstrap910 (Object l, Object n, Object t) throws Throwable { return _mh[ 910 ].invokeExact(l, n, t); }

    // 911
    private static MethodType MT_bootstrap911 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap911 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap911", MT_bootstrap911 ());
    }

    private static MethodHandle INDY_call911;
    private static MethodHandle INDY_call911 () throws Throwable {
        if (INDY_call911 != null) return INDY_call911;
        CallSite cs = (CallSite) MH_bootstrap911 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap911 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper911 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call911 ().invokeExact(o1, o2, o3); }

    static Object bootstrap911 (Object l, Object n, Object t) throws Throwable { return _mh[ 911 ].invokeExact(l, n, t); }

    // 912
    private static MethodType MT_bootstrap912 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap912 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap912", MT_bootstrap912 ());
    }

    private static MethodHandle INDY_call912;
    private static MethodHandle INDY_call912 () throws Throwable {
        if (INDY_call912 != null) return INDY_call912;
        CallSite cs = (CallSite) MH_bootstrap912 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap912 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper912 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call912 ().invokeExact(o1, o2, o3); }

    static Object bootstrap912 (Object l, Object n, Object t) throws Throwable { return _mh[ 912 ].invokeExact(l, n, t); }

    // 913
    private static MethodType MT_bootstrap913 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap913 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap913", MT_bootstrap913 ());
    }

    private static MethodHandle INDY_call913;
    private static MethodHandle INDY_call913 () throws Throwable {
        if (INDY_call913 != null) return INDY_call913;
        CallSite cs = (CallSite) MH_bootstrap913 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap913 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper913 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call913 ().invokeExact(o1, o2, o3); }

    static Object bootstrap913 (Object l, Object n, Object t) throws Throwable { return _mh[ 913 ].invokeExact(l, n, t); }

    // 914
    private static MethodType MT_bootstrap914 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap914 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap914", MT_bootstrap914 ());
    }

    private static MethodHandle INDY_call914;
    private static MethodHandle INDY_call914 () throws Throwable {
        if (INDY_call914 != null) return INDY_call914;
        CallSite cs = (CallSite) MH_bootstrap914 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap914 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper914 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call914 ().invokeExact(o1, o2, o3); }

    static Object bootstrap914 (Object l, Object n, Object t) throws Throwable { return _mh[ 914 ].invokeExact(l, n, t); }

    // 915
    private static MethodType MT_bootstrap915 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap915 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap915", MT_bootstrap915 ());
    }

    private static MethodHandle INDY_call915;
    private static MethodHandle INDY_call915 () throws Throwable {
        if (INDY_call915 != null) return INDY_call915;
        CallSite cs = (CallSite) MH_bootstrap915 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap915 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper915 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call915 ().invokeExact(o1, o2, o3); }

    static Object bootstrap915 (Object l, Object n, Object t) throws Throwable { return _mh[ 915 ].invokeExact(l, n, t); }

    // 916
    private static MethodType MT_bootstrap916 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap916 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap916", MT_bootstrap916 ());
    }

    private static MethodHandle INDY_call916;
    private static MethodHandle INDY_call916 () throws Throwable {
        if (INDY_call916 != null) return INDY_call916;
        CallSite cs = (CallSite) MH_bootstrap916 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap916 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper916 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call916 ().invokeExact(o1, o2, o3); }

    static Object bootstrap916 (Object l, Object n, Object t) throws Throwable { return _mh[ 916 ].invokeExact(l, n, t); }

    // 917
    private static MethodType MT_bootstrap917 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap917 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap917", MT_bootstrap917 ());
    }

    private static MethodHandle INDY_call917;
    private static MethodHandle INDY_call917 () throws Throwable {
        if (INDY_call917 != null) return INDY_call917;
        CallSite cs = (CallSite) MH_bootstrap917 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap917 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper917 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call917 ().invokeExact(o1, o2, o3); }

    static Object bootstrap917 (Object l, Object n, Object t) throws Throwable { return _mh[ 917 ].invokeExact(l, n, t); }

    // 918
    private static MethodType MT_bootstrap918 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap918 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap918", MT_bootstrap918 ());
    }

    private static MethodHandle INDY_call918;
    private static MethodHandle INDY_call918 () throws Throwable {
        if (INDY_call918 != null) return INDY_call918;
        CallSite cs = (CallSite) MH_bootstrap918 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap918 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper918 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call918 ().invokeExact(o1, o2, o3); }

    static Object bootstrap918 (Object l, Object n, Object t) throws Throwable { return _mh[ 918 ].invokeExact(l, n, t); }

    // 919
    private static MethodType MT_bootstrap919 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap919 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap919", MT_bootstrap919 ());
    }

    private static MethodHandle INDY_call919;
    private static MethodHandle INDY_call919 () throws Throwable {
        if (INDY_call919 != null) return INDY_call919;
        CallSite cs = (CallSite) MH_bootstrap919 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap919 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper919 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call919 ().invokeExact(o1, o2, o3); }

    static Object bootstrap919 (Object l, Object n, Object t) throws Throwable { return _mh[ 919 ].invokeExact(l, n, t); }

    // 920
    private static MethodType MT_bootstrap920 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap920 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap920", MT_bootstrap920 ());
    }

    private static MethodHandle INDY_call920;
    private static MethodHandle INDY_call920 () throws Throwable {
        if (INDY_call920 != null) return INDY_call920;
        CallSite cs = (CallSite) MH_bootstrap920 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap920 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper920 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call920 ().invokeExact(o1, o2, o3); }

    static Object bootstrap920 (Object l, Object n, Object t) throws Throwable { return _mh[ 920 ].invokeExact(l, n, t); }

    // 921
    private static MethodType MT_bootstrap921 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap921 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap921", MT_bootstrap921 ());
    }

    private static MethodHandle INDY_call921;
    private static MethodHandle INDY_call921 () throws Throwable {
        if (INDY_call921 != null) return INDY_call921;
        CallSite cs = (CallSite) MH_bootstrap921 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap921 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper921 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call921 ().invokeExact(o1, o2, o3); }

    static Object bootstrap921 (Object l, Object n, Object t) throws Throwable { return _mh[ 921 ].invokeExact(l, n, t); }

    // 922
    private static MethodType MT_bootstrap922 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap922 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap922", MT_bootstrap922 ());
    }

    private static MethodHandle INDY_call922;
    private static MethodHandle INDY_call922 () throws Throwable {
        if (INDY_call922 != null) return INDY_call922;
        CallSite cs = (CallSite) MH_bootstrap922 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap922 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper922 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call922 ().invokeExact(o1, o2, o3); }

    static Object bootstrap922 (Object l, Object n, Object t) throws Throwable { return _mh[ 922 ].invokeExact(l, n, t); }

    // 923
    private static MethodType MT_bootstrap923 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap923 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap923", MT_bootstrap923 ());
    }

    private static MethodHandle INDY_call923;
    private static MethodHandle INDY_call923 () throws Throwable {
        if (INDY_call923 != null) return INDY_call923;
        CallSite cs = (CallSite) MH_bootstrap923 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap923 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper923 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call923 ().invokeExact(o1, o2, o3); }

    static Object bootstrap923 (Object l, Object n, Object t) throws Throwable { return _mh[ 923 ].invokeExact(l, n, t); }

    // 924
    private static MethodType MT_bootstrap924 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap924 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap924", MT_bootstrap924 ());
    }

    private static MethodHandle INDY_call924;
    private static MethodHandle INDY_call924 () throws Throwable {
        if (INDY_call924 != null) return INDY_call924;
        CallSite cs = (CallSite) MH_bootstrap924 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap924 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper924 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call924 ().invokeExact(o1, o2, o3); }

    static Object bootstrap924 (Object l, Object n, Object t) throws Throwable { return _mh[ 924 ].invokeExact(l, n, t); }

    // 925
    private static MethodType MT_bootstrap925 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap925 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap925", MT_bootstrap925 ());
    }

    private static MethodHandle INDY_call925;
    private static MethodHandle INDY_call925 () throws Throwable {
        if (INDY_call925 != null) return INDY_call925;
        CallSite cs = (CallSite) MH_bootstrap925 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap925 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper925 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call925 ().invokeExact(o1, o2, o3); }

    static Object bootstrap925 (Object l, Object n, Object t) throws Throwable { return _mh[ 925 ].invokeExact(l, n, t); }

    // 926
    private static MethodType MT_bootstrap926 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap926 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap926", MT_bootstrap926 ());
    }

    private static MethodHandle INDY_call926;
    private static MethodHandle INDY_call926 () throws Throwable {
        if (INDY_call926 != null) return INDY_call926;
        CallSite cs = (CallSite) MH_bootstrap926 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap926 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper926 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call926 ().invokeExact(o1, o2, o3); }

    static Object bootstrap926 (Object l, Object n, Object t) throws Throwable { return _mh[ 926 ].invokeExact(l, n, t); }

    // 927
    private static MethodType MT_bootstrap927 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap927 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap927", MT_bootstrap927 ());
    }

    private static MethodHandle INDY_call927;
    private static MethodHandle INDY_call927 () throws Throwable {
        if (INDY_call927 != null) return INDY_call927;
        CallSite cs = (CallSite) MH_bootstrap927 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap927 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper927 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call927 ().invokeExact(o1, o2, o3); }

    static Object bootstrap927 (Object l, Object n, Object t) throws Throwable { return _mh[ 927 ].invokeExact(l, n, t); }

    // 928
    private static MethodType MT_bootstrap928 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap928 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap928", MT_bootstrap928 ());
    }

    private static MethodHandle INDY_call928;
    private static MethodHandle INDY_call928 () throws Throwable {
        if (INDY_call928 != null) return INDY_call928;
        CallSite cs = (CallSite) MH_bootstrap928 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap928 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper928 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call928 ().invokeExact(o1, o2, o3); }

    static Object bootstrap928 (Object l, Object n, Object t) throws Throwable { return _mh[ 928 ].invokeExact(l, n, t); }

    // 929
    private static MethodType MT_bootstrap929 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap929 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap929", MT_bootstrap929 ());
    }

    private static MethodHandle INDY_call929;
    private static MethodHandle INDY_call929 () throws Throwable {
        if (INDY_call929 != null) return INDY_call929;
        CallSite cs = (CallSite) MH_bootstrap929 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap929 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper929 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call929 ().invokeExact(o1, o2, o3); }

    static Object bootstrap929 (Object l, Object n, Object t) throws Throwable { return _mh[ 929 ].invokeExact(l, n, t); }

    // 930
    private static MethodType MT_bootstrap930 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap930 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap930", MT_bootstrap930 ());
    }

    private static MethodHandle INDY_call930;
    private static MethodHandle INDY_call930 () throws Throwable {
        if (INDY_call930 != null) return INDY_call930;
        CallSite cs = (CallSite) MH_bootstrap930 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap930 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper930 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call930 ().invokeExact(o1, o2, o3); }

    static Object bootstrap930 (Object l, Object n, Object t) throws Throwable { return _mh[ 930 ].invokeExact(l, n, t); }

    // 931
    private static MethodType MT_bootstrap931 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap931 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap931", MT_bootstrap931 ());
    }

    private static MethodHandle INDY_call931;
    private static MethodHandle INDY_call931 () throws Throwable {
        if (INDY_call931 != null) return INDY_call931;
        CallSite cs = (CallSite) MH_bootstrap931 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap931 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper931 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call931 ().invokeExact(o1, o2, o3); }

    static Object bootstrap931 (Object l, Object n, Object t) throws Throwable { return _mh[ 931 ].invokeExact(l, n, t); }

    // 932
    private static MethodType MT_bootstrap932 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap932 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap932", MT_bootstrap932 ());
    }

    private static MethodHandle INDY_call932;
    private static MethodHandle INDY_call932 () throws Throwable {
        if (INDY_call932 != null) return INDY_call932;
        CallSite cs = (CallSite) MH_bootstrap932 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap932 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper932 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call932 ().invokeExact(o1, o2, o3); }

    static Object bootstrap932 (Object l, Object n, Object t) throws Throwable { return _mh[ 932 ].invokeExact(l, n, t); }

    // 933
    private static MethodType MT_bootstrap933 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap933 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap933", MT_bootstrap933 ());
    }

    private static MethodHandle INDY_call933;
    private static MethodHandle INDY_call933 () throws Throwable {
        if (INDY_call933 != null) return INDY_call933;
        CallSite cs = (CallSite) MH_bootstrap933 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap933 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper933 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call933 ().invokeExact(o1, o2, o3); }

    static Object bootstrap933 (Object l, Object n, Object t) throws Throwable { return _mh[ 933 ].invokeExact(l, n, t); }

    // 934
    private static MethodType MT_bootstrap934 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap934 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap934", MT_bootstrap934 ());
    }

    private static MethodHandle INDY_call934;
    private static MethodHandle INDY_call934 () throws Throwable {
        if (INDY_call934 != null) return INDY_call934;
        CallSite cs = (CallSite) MH_bootstrap934 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap934 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper934 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call934 ().invokeExact(o1, o2, o3); }

    static Object bootstrap934 (Object l, Object n, Object t) throws Throwable { return _mh[ 934 ].invokeExact(l, n, t); }

    // 935
    private static MethodType MT_bootstrap935 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap935 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap935", MT_bootstrap935 ());
    }

    private static MethodHandle INDY_call935;
    private static MethodHandle INDY_call935 () throws Throwable {
        if (INDY_call935 != null) return INDY_call935;
        CallSite cs = (CallSite) MH_bootstrap935 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap935 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper935 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call935 ().invokeExact(o1, o2, o3); }

    static Object bootstrap935 (Object l, Object n, Object t) throws Throwable { return _mh[ 935 ].invokeExact(l, n, t); }

    // 936
    private static MethodType MT_bootstrap936 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap936 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap936", MT_bootstrap936 ());
    }

    private static MethodHandle INDY_call936;
    private static MethodHandle INDY_call936 () throws Throwable {
        if (INDY_call936 != null) return INDY_call936;
        CallSite cs = (CallSite) MH_bootstrap936 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap936 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper936 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call936 ().invokeExact(o1, o2, o3); }

    static Object bootstrap936 (Object l, Object n, Object t) throws Throwable { return _mh[ 936 ].invokeExact(l, n, t); }

    // 937
    private static MethodType MT_bootstrap937 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap937 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap937", MT_bootstrap937 ());
    }

    private static MethodHandle INDY_call937;
    private static MethodHandle INDY_call937 () throws Throwable {
        if (INDY_call937 != null) return INDY_call937;
        CallSite cs = (CallSite) MH_bootstrap937 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap937 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper937 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call937 ().invokeExact(o1, o2, o3); }

    static Object bootstrap937 (Object l, Object n, Object t) throws Throwable { return _mh[ 937 ].invokeExact(l, n, t); }

    // 938
    private static MethodType MT_bootstrap938 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap938 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap938", MT_bootstrap938 ());
    }

    private static MethodHandle INDY_call938;
    private static MethodHandle INDY_call938 () throws Throwable {
        if (INDY_call938 != null) return INDY_call938;
        CallSite cs = (CallSite) MH_bootstrap938 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap938 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper938 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call938 ().invokeExact(o1, o2, o3); }

    static Object bootstrap938 (Object l, Object n, Object t) throws Throwable { return _mh[ 938 ].invokeExact(l, n, t); }

    // 939
    private static MethodType MT_bootstrap939 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap939 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap939", MT_bootstrap939 ());
    }

    private static MethodHandle INDY_call939;
    private static MethodHandle INDY_call939 () throws Throwable {
        if (INDY_call939 != null) return INDY_call939;
        CallSite cs = (CallSite) MH_bootstrap939 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap939 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper939 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call939 ().invokeExact(o1, o2, o3); }

    static Object bootstrap939 (Object l, Object n, Object t) throws Throwable { return _mh[ 939 ].invokeExact(l, n, t); }

    // 940
    private static MethodType MT_bootstrap940 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap940 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap940", MT_bootstrap940 ());
    }

    private static MethodHandle INDY_call940;
    private static MethodHandle INDY_call940 () throws Throwable {
        if (INDY_call940 != null) return INDY_call940;
        CallSite cs = (CallSite) MH_bootstrap940 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap940 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper940 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call940 ().invokeExact(o1, o2, o3); }

    static Object bootstrap940 (Object l, Object n, Object t) throws Throwable { return _mh[ 940 ].invokeExact(l, n, t); }

    // 941
    private static MethodType MT_bootstrap941 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap941 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap941", MT_bootstrap941 ());
    }

    private static MethodHandle INDY_call941;
    private static MethodHandle INDY_call941 () throws Throwable {
        if (INDY_call941 != null) return INDY_call941;
        CallSite cs = (CallSite) MH_bootstrap941 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap941 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper941 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call941 ().invokeExact(o1, o2, o3); }

    static Object bootstrap941 (Object l, Object n, Object t) throws Throwable { return _mh[ 941 ].invokeExact(l, n, t); }

    // 942
    private static MethodType MT_bootstrap942 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap942 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap942", MT_bootstrap942 ());
    }

    private static MethodHandle INDY_call942;
    private static MethodHandle INDY_call942 () throws Throwable {
        if (INDY_call942 != null) return INDY_call942;
        CallSite cs = (CallSite) MH_bootstrap942 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap942 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper942 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call942 ().invokeExact(o1, o2, o3); }

    static Object bootstrap942 (Object l, Object n, Object t) throws Throwable { return _mh[ 942 ].invokeExact(l, n, t); }

    // 943
    private static MethodType MT_bootstrap943 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap943 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap943", MT_bootstrap943 ());
    }

    private static MethodHandle INDY_call943;
    private static MethodHandle INDY_call943 () throws Throwable {
        if (INDY_call943 != null) return INDY_call943;
        CallSite cs = (CallSite) MH_bootstrap943 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap943 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper943 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call943 ().invokeExact(o1, o2, o3); }

    static Object bootstrap943 (Object l, Object n, Object t) throws Throwable { return _mh[ 943 ].invokeExact(l, n, t); }

    // 944
    private static MethodType MT_bootstrap944 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap944 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap944", MT_bootstrap944 ());
    }

    private static MethodHandle INDY_call944;
    private static MethodHandle INDY_call944 () throws Throwable {
        if (INDY_call944 != null) return INDY_call944;
        CallSite cs = (CallSite) MH_bootstrap944 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap944 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper944 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call944 ().invokeExact(o1, o2, o3); }

    static Object bootstrap944 (Object l, Object n, Object t) throws Throwable { return _mh[ 944 ].invokeExact(l, n, t); }

    // 945
    private static MethodType MT_bootstrap945 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap945 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap945", MT_bootstrap945 ());
    }

    private static MethodHandle INDY_call945;
    private static MethodHandle INDY_call945 () throws Throwable {
        if (INDY_call945 != null) return INDY_call945;
        CallSite cs = (CallSite) MH_bootstrap945 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap945 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper945 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call945 ().invokeExact(o1, o2, o3); }

    static Object bootstrap945 (Object l, Object n, Object t) throws Throwable { return _mh[ 945 ].invokeExact(l, n, t); }

    // 946
    private static MethodType MT_bootstrap946 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap946 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap946", MT_bootstrap946 ());
    }

    private static MethodHandle INDY_call946;
    private static MethodHandle INDY_call946 () throws Throwable {
        if (INDY_call946 != null) return INDY_call946;
        CallSite cs = (CallSite) MH_bootstrap946 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap946 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper946 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call946 ().invokeExact(o1, o2, o3); }

    static Object bootstrap946 (Object l, Object n, Object t) throws Throwable { return _mh[ 946 ].invokeExact(l, n, t); }

    // 947
    private static MethodType MT_bootstrap947 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap947 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap947", MT_bootstrap947 ());
    }

    private static MethodHandle INDY_call947;
    private static MethodHandle INDY_call947 () throws Throwable {
        if (INDY_call947 != null) return INDY_call947;
        CallSite cs = (CallSite) MH_bootstrap947 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap947 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper947 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call947 ().invokeExact(o1, o2, o3); }

    static Object bootstrap947 (Object l, Object n, Object t) throws Throwable { return _mh[ 947 ].invokeExact(l, n, t); }

    // 948
    private static MethodType MT_bootstrap948 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap948 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap948", MT_bootstrap948 ());
    }

    private static MethodHandle INDY_call948;
    private static MethodHandle INDY_call948 () throws Throwable {
        if (INDY_call948 != null) return INDY_call948;
        CallSite cs = (CallSite) MH_bootstrap948 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap948 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper948 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call948 ().invokeExact(o1, o2, o3); }

    static Object bootstrap948 (Object l, Object n, Object t) throws Throwable { return _mh[ 948 ].invokeExact(l, n, t); }

    // 949
    private static MethodType MT_bootstrap949 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap949 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap949", MT_bootstrap949 ());
    }

    private static MethodHandle INDY_call949;
    private static MethodHandle INDY_call949 () throws Throwable {
        if (INDY_call949 != null) return INDY_call949;
        CallSite cs = (CallSite) MH_bootstrap949 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap949 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper949 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call949 ().invokeExact(o1, o2, o3); }

    static Object bootstrap949 (Object l, Object n, Object t) throws Throwable { return _mh[ 949 ].invokeExact(l, n, t); }

    // 950
    private static MethodType MT_bootstrap950 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap950 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap950", MT_bootstrap950 ());
    }

    private static MethodHandle INDY_call950;
    private static MethodHandle INDY_call950 () throws Throwable {
        if (INDY_call950 != null) return INDY_call950;
        CallSite cs = (CallSite) MH_bootstrap950 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap950 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper950 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call950 ().invokeExact(o1, o2, o3); }

    static Object bootstrap950 (Object l, Object n, Object t) throws Throwable { return _mh[ 950 ].invokeExact(l, n, t); }

    // 951
    private static MethodType MT_bootstrap951 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap951 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap951", MT_bootstrap951 ());
    }

    private static MethodHandle INDY_call951;
    private static MethodHandle INDY_call951 () throws Throwable {
        if (INDY_call951 != null) return INDY_call951;
        CallSite cs = (CallSite) MH_bootstrap951 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap951 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper951 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call951 ().invokeExact(o1, o2, o3); }

    static Object bootstrap951 (Object l, Object n, Object t) throws Throwable { return _mh[ 951 ].invokeExact(l, n, t); }

    // 952
    private static MethodType MT_bootstrap952 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap952 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap952", MT_bootstrap952 ());
    }

    private static MethodHandle INDY_call952;
    private static MethodHandle INDY_call952 () throws Throwable {
        if (INDY_call952 != null) return INDY_call952;
        CallSite cs = (CallSite) MH_bootstrap952 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap952 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper952 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call952 ().invokeExact(o1, o2, o3); }

    static Object bootstrap952 (Object l, Object n, Object t) throws Throwable { return _mh[ 952 ].invokeExact(l, n, t); }

    // 953
    private static MethodType MT_bootstrap953 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap953 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap953", MT_bootstrap953 ());
    }

    private static MethodHandle INDY_call953;
    private static MethodHandle INDY_call953 () throws Throwable {
        if (INDY_call953 != null) return INDY_call953;
        CallSite cs = (CallSite) MH_bootstrap953 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap953 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper953 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call953 ().invokeExact(o1, o2, o3); }

    static Object bootstrap953 (Object l, Object n, Object t) throws Throwable { return _mh[ 953 ].invokeExact(l, n, t); }

    // 954
    private static MethodType MT_bootstrap954 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap954 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap954", MT_bootstrap954 ());
    }

    private static MethodHandle INDY_call954;
    private static MethodHandle INDY_call954 () throws Throwable {
        if (INDY_call954 != null) return INDY_call954;
        CallSite cs = (CallSite) MH_bootstrap954 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap954 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper954 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call954 ().invokeExact(o1, o2, o3); }

    static Object bootstrap954 (Object l, Object n, Object t) throws Throwable { return _mh[ 954 ].invokeExact(l, n, t); }

    // 955
    private static MethodType MT_bootstrap955 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap955 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap955", MT_bootstrap955 ());
    }

    private static MethodHandle INDY_call955;
    private static MethodHandle INDY_call955 () throws Throwable {
        if (INDY_call955 != null) return INDY_call955;
        CallSite cs = (CallSite) MH_bootstrap955 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap955 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper955 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call955 ().invokeExact(o1, o2, o3); }

    static Object bootstrap955 (Object l, Object n, Object t) throws Throwable { return _mh[ 955 ].invokeExact(l, n, t); }

    // 956
    private static MethodType MT_bootstrap956 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap956 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap956", MT_bootstrap956 ());
    }

    private static MethodHandle INDY_call956;
    private static MethodHandle INDY_call956 () throws Throwable {
        if (INDY_call956 != null) return INDY_call956;
        CallSite cs = (CallSite) MH_bootstrap956 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap956 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper956 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call956 ().invokeExact(o1, o2, o3); }

    static Object bootstrap956 (Object l, Object n, Object t) throws Throwable { return _mh[ 956 ].invokeExact(l, n, t); }

    // 957
    private static MethodType MT_bootstrap957 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap957 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap957", MT_bootstrap957 ());
    }

    private static MethodHandle INDY_call957;
    private static MethodHandle INDY_call957 () throws Throwable {
        if (INDY_call957 != null) return INDY_call957;
        CallSite cs = (CallSite) MH_bootstrap957 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap957 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper957 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call957 ().invokeExact(o1, o2, o3); }

    static Object bootstrap957 (Object l, Object n, Object t) throws Throwable { return _mh[ 957 ].invokeExact(l, n, t); }

    // 958
    private static MethodType MT_bootstrap958 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap958 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap958", MT_bootstrap958 ());
    }

    private static MethodHandle INDY_call958;
    private static MethodHandle INDY_call958 () throws Throwable {
        if (INDY_call958 != null) return INDY_call958;
        CallSite cs = (CallSite) MH_bootstrap958 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap958 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper958 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call958 ().invokeExact(o1, o2, o3); }

    static Object bootstrap958 (Object l, Object n, Object t) throws Throwable { return _mh[ 958 ].invokeExact(l, n, t); }

    // 959
    private static MethodType MT_bootstrap959 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap959 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap959", MT_bootstrap959 ());
    }

    private static MethodHandle INDY_call959;
    private static MethodHandle INDY_call959 () throws Throwable {
        if (INDY_call959 != null) return INDY_call959;
        CallSite cs = (CallSite) MH_bootstrap959 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap959 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper959 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call959 ().invokeExact(o1, o2, o3); }

    static Object bootstrap959 (Object l, Object n, Object t) throws Throwable { return _mh[ 959 ].invokeExact(l, n, t); }

    // 960
    private static MethodType MT_bootstrap960 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap960 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap960", MT_bootstrap960 ());
    }

    private static MethodHandle INDY_call960;
    private static MethodHandle INDY_call960 () throws Throwable {
        if (INDY_call960 != null) return INDY_call960;
        CallSite cs = (CallSite) MH_bootstrap960 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap960 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper960 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call960 ().invokeExact(o1, o2, o3); }

    static Object bootstrap960 (Object l, Object n, Object t) throws Throwable { return _mh[ 960 ].invokeExact(l, n, t); }

    // 961
    private static MethodType MT_bootstrap961 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap961 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap961", MT_bootstrap961 ());
    }

    private static MethodHandle INDY_call961;
    private static MethodHandle INDY_call961 () throws Throwable {
        if (INDY_call961 != null) return INDY_call961;
        CallSite cs = (CallSite) MH_bootstrap961 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap961 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper961 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call961 ().invokeExact(o1, o2, o3); }

    static Object bootstrap961 (Object l, Object n, Object t) throws Throwable { return _mh[ 961 ].invokeExact(l, n, t); }

    // 962
    private static MethodType MT_bootstrap962 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap962 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap962", MT_bootstrap962 ());
    }

    private static MethodHandle INDY_call962;
    private static MethodHandle INDY_call962 () throws Throwable {
        if (INDY_call962 != null) return INDY_call962;
        CallSite cs = (CallSite) MH_bootstrap962 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap962 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper962 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call962 ().invokeExact(o1, o2, o3); }

    static Object bootstrap962 (Object l, Object n, Object t) throws Throwable { return _mh[ 962 ].invokeExact(l, n, t); }

    // 963
    private static MethodType MT_bootstrap963 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap963 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap963", MT_bootstrap963 ());
    }

    private static MethodHandle INDY_call963;
    private static MethodHandle INDY_call963 () throws Throwable {
        if (INDY_call963 != null) return INDY_call963;
        CallSite cs = (CallSite) MH_bootstrap963 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap963 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper963 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call963 ().invokeExact(o1, o2, o3); }

    static Object bootstrap963 (Object l, Object n, Object t) throws Throwable { return _mh[ 963 ].invokeExact(l, n, t); }

    // 964
    private static MethodType MT_bootstrap964 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap964 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap964", MT_bootstrap964 ());
    }

    private static MethodHandle INDY_call964;
    private static MethodHandle INDY_call964 () throws Throwable {
        if (INDY_call964 != null) return INDY_call964;
        CallSite cs = (CallSite) MH_bootstrap964 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap964 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper964 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call964 ().invokeExact(o1, o2, o3); }

    static Object bootstrap964 (Object l, Object n, Object t) throws Throwable { return _mh[ 964 ].invokeExact(l, n, t); }

    // 965
    private static MethodType MT_bootstrap965 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap965 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap965", MT_bootstrap965 ());
    }

    private static MethodHandle INDY_call965;
    private static MethodHandle INDY_call965 () throws Throwable {
        if (INDY_call965 != null) return INDY_call965;
        CallSite cs = (CallSite) MH_bootstrap965 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap965 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper965 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call965 ().invokeExact(o1, o2, o3); }

    static Object bootstrap965 (Object l, Object n, Object t) throws Throwable { return _mh[ 965 ].invokeExact(l, n, t); }

    // 966
    private static MethodType MT_bootstrap966 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap966 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap966", MT_bootstrap966 ());
    }

    private static MethodHandle INDY_call966;
    private static MethodHandle INDY_call966 () throws Throwable {
        if (INDY_call966 != null) return INDY_call966;
        CallSite cs = (CallSite) MH_bootstrap966 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap966 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper966 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call966 ().invokeExact(o1, o2, o3); }

    static Object bootstrap966 (Object l, Object n, Object t) throws Throwable { return _mh[ 966 ].invokeExact(l, n, t); }

    // 967
    private static MethodType MT_bootstrap967 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap967 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap967", MT_bootstrap967 ());
    }

    private static MethodHandle INDY_call967;
    private static MethodHandle INDY_call967 () throws Throwable {
        if (INDY_call967 != null) return INDY_call967;
        CallSite cs = (CallSite) MH_bootstrap967 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap967 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper967 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call967 ().invokeExact(o1, o2, o3); }

    static Object bootstrap967 (Object l, Object n, Object t) throws Throwable { return _mh[ 967 ].invokeExact(l, n, t); }

    // 968
    private static MethodType MT_bootstrap968 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap968 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap968", MT_bootstrap968 ());
    }

    private static MethodHandle INDY_call968;
    private static MethodHandle INDY_call968 () throws Throwable {
        if (INDY_call968 != null) return INDY_call968;
        CallSite cs = (CallSite) MH_bootstrap968 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap968 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper968 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call968 ().invokeExact(o1, o2, o3); }

    static Object bootstrap968 (Object l, Object n, Object t) throws Throwable { return _mh[ 968 ].invokeExact(l, n, t); }

    // 969
    private static MethodType MT_bootstrap969 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap969 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap969", MT_bootstrap969 ());
    }

    private static MethodHandle INDY_call969;
    private static MethodHandle INDY_call969 () throws Throwable {
        if (INDY_call969 != null) return INDY_call969;
        CallSite cs = (CallSite) MH_bootstrap969 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap969 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper969 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call969 ().invokeExact(o1, o2, o3); }

    static Object bootstrap969 (Object l, Object n, Object t) throws Throwable { return _mh[ 969 ].invokeExact(l, n, t); }

    // 970
    private static MethodType MT_bootstrap970 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap970 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap970", MT_bootstrap970 ());
    }

    private static MethodHandle INDY_call970;
    private static MethodHandle INDY_call970 () throws Throwable {
        if (INDY_call970 != null) return INDY_call970;
        CallSite cs = (CallSite) MH_bootstrap970 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap970 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper970 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call970 ().invokeExact(o1, o2, o3); }

    static Object bootstrap970 (Object l, Object n, Object t) throws Throwable { return _mh[ 970 ].invokeExact(l, n, t); }

    // 971
    private static MethodType MT_bootstrap971 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap971 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap971", MT_bootstrap971 ());
    }

    private static MethodHandle INDY_call971;
    private static MethodHandle INDY_call971 () throws Throwable {
        if (INDY_call971 != null) return INDY_call971;
        CallSite cs = (CallSite) MH_bootstrap971 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap971 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper971 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call971 ().invokeExact(o1, o2, o3); }

    static Object bootstrap971 (Object l, Object n, Object t) throws Throwable { return _mh[ 971 ].invokeExact(l, n, t); }

    // 972
    private static MethodType MT_bootstrap972 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap972 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap972", MT_bootstrap972 ());
    }

    private static MethodHandle INDY_call972;
    private static MethodHandle INDY_call972 () throws Throwable {
        if (INDY_call972 != null) return INDY_call972;
        CallSite cs = (CallSite) MH_bootstrap972 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap972 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper972 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call972 ().invokeExact(o1, o2, o3); }

    static Object bootstrap972 (Object l, Object n, Object t) throws Throwable { return _mh[ 972 ].invokeExact(l, n, t); }

    // 973
    private static MethodType MT_bootstrap973 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap973 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap973", MT_bootstrap973 ());
    }

    private static MethodHandle INDY_call973;
    private static MethodHandle INDY_call973 () throws Throwable {
        if (INDY_call973 != null) return INDY_call973;
        CallSite cs = (CallSite) MH_bootstrap973 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap973 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper973 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call973 ().invokeExact(o1, o2, o3); }

    static Object bootstrap973 (Object l, Object n, Object t) throws Throwable { return _mh[ 973 ].invokeExact(l, n, t); }

    // 974
    private static MethodType MT_bootstrap974 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap974 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap974", MT_bootstrap974 ());
    }

    private static MethodHandle INDY_call974;
    private static MethodHandle INDY_call974 () throws Throwable {
        if (INDY_call974 != null) return INDY_call974;
        CallSite cs = (CallSite) MH_bootstrap974 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap974 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper974 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call974 ().invokeExact(o1, o2, o3); }

    static Object bootstrap974 (Object l, Object n, Object t) throws Throwable { return _mh[ 974 ].invokeExact(l, n, t); }

    // 975
    private static MethodType MT_bootstrap975 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap975 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap975", MT_bootstrap975 ());
    }

    private static MethodHandle INDY_call975;
    private static MethodHandle INDY_call975 () throws Throwable {
        if (INDY_call975 != null) return INDY_call975;
        CallSite cs = (CallSite) MH_bootstrap975 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap975 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper975 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call975 ().invokeExact(o1, o2, o3); }

    static Object bootstrap975 (Object l, Object n, Object t) throws Throwable { return _mh[ 975 ].invokeExact(l, n, t); }

    // 976
    private static MethodType MT_bootstrap976 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap976 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap976", MT_bootstrap976 ());
    }

    private static MethodHandle INDY_call976;
    private static MethodHandle INDY_call976 () throws Throwable {
        if (INDY_call976 != null) return INDY_call976;
        CallSite cs = (CallSite) MH_bootstrap976 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap976 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper976 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call976 ().invokeExact(o1, o2, o3); }

    static Object bootstrap976 (Object l, Object n, Object t) throws Throwable { return _mh[ 976 ].invokeExact(l, n, t); }

    // 977
    private static MethodType MT_bootstrap977 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap977 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap977", MT_bootstrap977 ());
    }

    private static MethodHandle INDY_call977;
    private static MethodHandle INDY_call977 () throws Throwable {
        if (INDY_call977 != null) return INDY_call977;
        CallSite cs = (CallSite) MH_bootstrap977 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap977 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper977 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call977 ().invokeExact(o1, o2, o3); }

    static Object bootstrap977 (Object l, Object n, Object t) throws Throwable { return _mh[ 977 ].invokeExact(l, n, t); }

    // 978
    private static MethodType MT_bootstrap978 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap978 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap978", MT_bootstrap978 ());
    }

    private static MethodHandle INDY_call978;
    private static MethodHandle INDY_call978 () throws Throwable {
        if (INDY_call978 != null) return INDY_call978;
        CallSite cs = (CallSite) MH_bootstrap978 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap978 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper978 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call978 ().invokeExact(o1, o2, o3); }

    static Object bootstrap978 (Object l, Object n, Object t) throws Throwable { return _mh[ 978 ].invokeExact(l, n, t); }

    // 979
    private static MethodType MT_bootstrap979 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap979 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap979", MT_bootstrap979 ());
    }

    private static MethodHandle INDY_call979;
    private static MethodHandle INDY_call979 () throws Throwable {
        if (INDY_call979 != null) return INDY_call979;
        CallSite cs = (CallSite) MH_bootstrap979 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap979 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper979 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call979 ().invokeExact(o1, o2, o3); }

    static Object bootstrap979 (Object l, Object n, Object t) throws Throwable { return _mh[ 979 ].invokeExact(l, n, t); }

    // 980
    private static MethodType MT_bootstrap980 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap980 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap980", MT_bootstrap980 ());
    }

    private static MethodHandle INDY_call980;
    private static MethodHandle INDY_call980 () throws Throwable {
        if (INDY_call980 != null) return INDY_call980;
        CallSite cs = (CallSite) MH_bootstrap980 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap980 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper980 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call980 ().invokeExact(o1, o2, o3); }

    static Object bootstrap980 (Object l, Object n, Object t) throws Throwable { return _mh[ 980 ].invokeExact(l, n, t); }

    // 981
    private static MethodType MT_bootstrap981 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap981 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap981", MT_bootstrap981 ());
    }

    private static MethodHandle INDY_call981;
    private static MethodHandle INDY_call981 () throws Throwable {
        if (INDY_call981 != null) return INDY_call981;
        CallSite cs = (CallSite) MH_bootstrap981 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap981 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper981 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call981 ().invokeExact(o1, o2, o3); }

    static Object bootstrap981 (Object l, Object n, Object t) throws Throwable { return _mh[ 981 ].invokeExact(l, n, t); }

    // 982
    private static MethodType MT_bootstrap982 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap982 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap982", MT_bootstrap982 ());
    }

    private static MethodHandle INDY_call982;
    private static MethodHandle INDY_call982 () throws Throwable {
        if (INDY_call982 != null) return INDY_call982;
        CallSite cs = (CallSite) MH_bootstrap982 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap982 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper982 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call982 ().invokeExact(o1, o2, o3); }

    static Object bootstrap982 (Object l, Object n, Object t) throws Throwable { return _mh[ 982 ].invokeExact(l, n, t); }

    // 983
    private static MethodType MT_bootstrap983 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap983 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap983", MT_bootstrap983 ());
    }

    private static MethodHandle INDY_call983;
    private static MethodHandle INDY_call983 () throws Throwable {
        if (INDY_call983 != null) return INDY_call983;
        CallSite cs = (CallSite) MH_bootstrap983 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap983 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper983 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call983 ().invokeExact(o1, o2, o3); }

    static Object bootstrap983 (Object l, Object n, Object t) throws Throwable { return _mh[ 983 ].invokeExact(l, n, t); }

    // 984
    private static MethodType MT_bootstrap984 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap984 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap984", MT_bootstrap984 ());
    }

    private static MethodHandle INDY_call984;
    private static MethodHandle INDY_call984 () throws Throwable {
        if (INDY_call984 != null) return INDY_call984;
        CallSite cs = (CallSite) MH_bootstrap984 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap984 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper984 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call984 ().invokeExact(o1, o2, o3); }

    static Object bootstrap984 (Object l, Object n, Object t) throws Throwable { return _mh[ 984 ].invokeExact(l, n, t); }

    // 985
    private static MethodType MT_bootstrap985 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap985 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap985", MT_bootstrap985 ());
    }

    private static MethodHandle INDY_call985;
    private static MethodHandle INDY_call985 () throws Throwable {
        if (INDY_call985 != null) return INDY_call985;
        CallSite cs = (CallSite) MH_bootstrap985 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap985 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper985 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call985 ().invokeExact(o1, o2, o3); }

    static Object bootstrap985 (Object l, Object n, Object t) throws Throwable { return _mh[ 985 ].invokeExact(l, n, t); }

    // 986
    private static MethodType MT_bootstrap986 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap986 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap986", MT_bootstrap986 ());
    }

    private static MethodHandle INDY_call986;
    private static MethodHandle INDY_call986 () throws Throwable {
        if (INDY_call986 != null) return INDY_call986;
        CallSite cs = (CallSite) MH_bootstrap986 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap986 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper986 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call986 ().invokeExact(o1, o2, o3); }

    static Object bootstrap986 (Object l, Object n, Object t) throws Throwable { return _mh[ 986 ].invokeExact(l, n, t); }

    // 987
    private static MethodType MT_bootstrap987 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap987 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap987", MT_bootstrap987 ());
    }

    private static MethodHandle INDY_call987;
    private static MethodHandle INDY_call987 () throws Throwable {
        if (INDY_call987 != null) return INDY_call987;
        CallSite cs = (CallSite) MH_bootstrap987 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap987 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper987 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call987 ().invokeExact(o1, o2, o3); }

    static Object bootstrap987 (Object l, Object n, Object t) throws Throwable { return _mh[ 987 ].invokeExact(l, n, t); }

    // 988
    private static MethodType MT_bootstrap988 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap988 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap988", MT_bootstrap988 ());
    }

    private static MethodHandle INDY_call988;
    private static MethodHandle INDY_call988 () throws Throwable {
        if (INDY_call988 != null) return INDY_call988;
        CallSite cs = (CallSite) MH_bootstrap988 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap988 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper988 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call988 ().invokeExact(o1, o2, o3); }

    static Object bootstrap988 (Object l, Object n, Object t) throws Throwable { return _mh[ 988 ].invokeExact(l, n, t); }

    // 989
    private static MethodType MT_bootstrap989 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap989 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap989", MT_bootstrap989 ());
    }

    private static MethodHandle INDY_call989;
    private static MethodHandle INDY_call989 () throws Throwable {
        if (INDY_call989 != null) return INDY_call989;
        CallSite cs = (CallSite) MH_bootstrap989 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap989 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper989 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call989 ().invokeExact(o1, o2, o3); }

    static Object bootstrap989 (Object l, Object n, Object t) throws Throwable { return _mh[ 989 ].invokeExact(l, n, t); }

    // 990
    private static MethodType MT_bootstrap990 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap990 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap990", MT_bootstrap990 ());
    }

    private static MethodHandle INDY_call990;
    private static MethodHandle INDY_call990 () throws Throwable {
        if (INDY_call990 != null) return INDY_call990;
        CallSite cs = (CallSite) MH_bootstrap990 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap990 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper990 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call990 ().invokeExact(o1, o2, o3); }

    static Object bootstrap990 (Object l, Object n, Object t) throws Throwable { return _mh[ 990 ].invokeExact(l, n, t); }

    // 991
    private static MethodType MT_bootstrap991 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap991 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap991", MT_bootstrap991 ());
    }

    private static MethodHandle INDY_call991;
    private static MethodHandle INDY_call991 () throws Throwable {
        if (INDY_call991 != null) return INDY_call991;
        CallSite cs = (CallSite) MH_bootstrap991 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap991 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper991 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call991 ().invokeExact(o1, o2, o3); }

    static Object bootstrap991 (Object l, Object n, Object t) throws Throwable { return _mh[ 991 ].invokeExact(l, n, t); }

    // 992
    private static MethodType MT_bootstrap992 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap992 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap992", MT_bootstrap992 ());
    }

    private static MethodHandle INDY_call992;
    private static MethodHandle INDY_call992 () throws Throwable {
        if (INDY_call992 != null) return INDY_call992;
        CallSite cs = (CallSite) MH_bootstrap992 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap992 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper992 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call992 ().invokeExact(o1, o2, o3); }

    static Object bootstrap992 (Object l, Object n, Object t) throws Throwable { return _mh[ 992 ].invokeExact(l, n, t); }

    // 993
    private static MethodType MT_bootstrap993 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap993 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap993", MT_bootstrap993 ());
    }

    private static MethodHandle INDY_call993;
    private static MethodHandle INDY_call993 () throws Throwable {
        if (INDY_call993 != null) return INDY_call993;
        CallSite cs = (CallSite) MH_bootstrap993 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap993 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper993 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call993 ().invokeExact(o1, o2, o3); }

    static Object bootstrap993 (Object l, Object n, Object t) throws Throwable { return _mh[ 993 ].invokeExact(l, n, t); }

    // 994
    private static MethodType MT_bootstrap994 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap994 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap994", MT_bootstrap994 ());
    }

    private static MethodHandle INDY_call994;
    private static MethodHandle INDY_call994 () throws Throwable {
        if (INDY_call994 != null) return INDY_call994;
        CallSite cs = (CallSite) MH_bootstrap994 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap994 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper994 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call994 ().invokeExact(o1, o2, o3); }

    static Object bootstrap994 (Object l, Object n, Object t) throws Throwable { return _mh[ 994 ].invokeExact(l, n, t); }

    // 995
    private static MethodType MT_bootstrap995 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap995 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap995", MT_bootstrap995 ());
    }

    private static MethodHandle INDY_call995;
    private static MethodHandle INDY_call995 () throws Throwable {
        if (INDY_call995 != null) return INDY_call995;
        CallSite cs = (CallSite) MH_bootstrap995 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap995 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper995 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call995 ().invokeExact(o1, o2, o3); }

    static Object bootstrap995 (Object l, Object n, Object t) throws Throwable { return _mh[ 995 ].invokeExact(l, n, t); }

    // 996
    private static MethodType MT_bootstrap996 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap996 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap996", MT_bootstrap996 ());
    }

    private static MethodHandle INDY_call996;
    private static MethodHandle INDY_call996 () throws Throwable {
        if (INDY_call996 != null) return INDY_call996;
        CallSite cs = (CallSite) MH_bootstrap996 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap996 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper996 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call996 ().invokeExact(o1, o2, o3); }

    static Object bootstrap996 (Object l, Object n, Object t) throws Throwable { return _mh[ 996 ].invokeExact(l, n, t); }

    // 997
    private static MethodType MT_bootstrap997 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap997 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap997", MT_bootstrap997 ());
    }

    private static MethodHandle INDY_call997;
    private static MethodHandle INDY_call997 () throws Throwable {
        if (INDY_call997 != null) return INDY_call997;
        CallSite cs = (CallSite) MH_bootstrap997 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap997 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper997 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call997 ().invokeExact(o1, o2, o3); }

    static Object bootstrap997 (Object l, Object n, Object t) throws Throwable { return _mh[ 997 ].invokeExact(l, n, t); }

    // 998
    private static MethodType MT_bootstrap998 () { return MethodType.methodType(Object.class, Object.class, Object.class, Object.class); }

    private static MethodHandle MH_bootstrap998 () throws Exception {
        return MethodHandles.lookup().findStatic(INDIFY_Test.class, "bootstrap998", MT_bootstrap998 ());
    }

    private static MethodHandle INDY_call998;
    private static MethodHandle INDY_call998 () throws Throwable {
        if (INDY_call998 != null) return INDY_call998;
        CallSite cs = (CallSite) MH_bootstrap998 ().invokeWithArguments(MethodHandles.lookup(), "gimmeTarget", MT_bootstrap998 ());
        return cs.dynamicInvoker();
    }

    static Object indyWrapper998 (Object o1, Object o2, Object o3) throws Throwable { return INDY_call998 ().invokeExact(o1, o2, o3); }

    static Object bootstrap998 (Object l, Object n, Object t) throws Throwable { return _mh[ 998 ].invokeExact(l, n, t); }


    // End of BSM+indy pairs

    public boolean run() throws Throwable {

        if ( ! _threadMXBean.isSynchronizerUsageSupported() ) {
            Env.getLog().complain("Platform does not detect deadlocks in synchronizers. Please exclude this test on this platform.");
            return false;
        }

        MethodHandle bsmt = MethodHandles.lookup().findStatic(
                getClass(), "bsmt", MethodType.methodType(Object.class, int.class, Object.class, Object.class, Object.class));

        for ( int i = 0; i < THREAD_NUM; i++ )
            _mh[i] = MethodHandles.insertArguments(bsmt, 0, i);

        for ( int i = 0; i < THREAD_NUM; i++ )
            _locks[i] = new ReentrantLock();

        Stresser stresser = new Stresser(Env.getArgParser().getArguments());
        stresser.start(ITERATIONS);
        try {
            _iteration = 0;
            while ( stresser.iteration() ) {
                if  ( ! test() ) {
                    return false;
                }
                _iteration++;
            }
        } finally {
            stresser.finish();
        }

        return true;
    }

    boolean test() throws Throwable {
        Env.traceNormal("Iteration " + _iteration + " Starting test...");

        // Sanity check that all the locks are available.
        for ( int i = 0; i < THREAD_NUM; i++ ) {
            if ( _locks[i].isLocked() ) {
                Env.getLog().complain("Lock " + i + " is still locked!");
                _testFailed = true;
            }
        }

        if ( _testFailed )
            throw new Exception("Some locks are still locked");

        // Threads generally wait on this after claiming their first lock,
        // and then when released will try to claim the second, which leads
        // to deadlock.
        _threadRaceStartBarrier = new CyclicBarrier(THREAD_NUM + 1);

        // Threads signal this latch after being released from the startbarrier
        // so that they are closer to triggering deadlock before the main thread
        // starts to check for it.
        _threadsRunningLatch = new CountDownLatch(THREAD_NUM);

        _testDone = false;
        _testFailed = false;

        // Start the new batch of threads.
        for ( int i = 0; i < THREAD_NUM; i++ ) {
            (_threads[i] = new DeadlockedThread(i)).start();
        }

        try {
            // If a thread encounters an error before it reaches the start barrier
            // then we will hang here until the test times out. So we do a token
            // check for such failure.
            if (_testFailed) {
                Env.complain("Unexpected thread failure before startBarrier was reached");
                return false;
            }

            _threadRaceStartBarrier.await();
            Env.traceVerbose("Iteration " + _iteration + " Start race...");

            // Wait till all threads poised to deadlock. Again we may hang here
            // if unexpected errors are encountered, so again a token check.
            if (_testFailed) {
                Env.complain("Unexpected thread failure after startBarrier was reached");
                return false;
            }

            _threadsRunningLatch.await();

            // There is a race now between checking for a deadlock and the threads
            // actually engaging in that deadlock. We can't query all of the "locks"
            // involved to see if they are owned and have waiters (no API for built-in
            // monitors). Nor can we check the thread states because they could be blocked
            // on incidental synchronization (like I/O monitors when logging is enabled).
            // So we simply loop checking for a deadlock until we find it, or else the
            // overall test times out.

            long[] deadlockedThreads = null;
            do {
                deadlockedThreads = _threadMXBean.findDeadlockedThreads();
            } while (deadlockedThreads == null && !_testFailed);

            if (_testFailed) {
                Env.complain("Unexpected thread failure while checking for deadlock");
                return false;
            }

            if (deadlockedThreads.length != THREAD_NUM) {
                Env.complain("Found " + deadlockedThreads.length + " deadlocked threads. Expected to find " + THREAD_NUM);
                return false;
            } else {
                Env.traceNormal("Found " + deadlockedThreads.length + " deadlocked threads as expected");
                return true;
            }
        } finally {
            // Tells the locking threads the interrupt was expected.
            _testDone = true;

            // Break the deadlock by dropping the attempt to lock
            // the interruptible locks, which then causes all other
            // locks to be released and allow threads acquiring
            // non-interruptible locks to proceed.
            _threads[0].interrupt();

            // Wait for all threads to terminate before proceeding to next
            // iteration. If we only join() for a limited time and its too short
            // then we not only complain here, but will also find locks that are
            // still locked. It is far simpler to only proceed when all threads
            // are done and rely on the overall test timeout to detect problems.
            for (int i = 0; i < THREAD_NUM; i++) {
                _threads[i].join();
            }

            MutableCallSite.syncAll(_cs);
        }
    }

    static class DeadlockedThread extends Thread {
        int _n;
        boolean _lockedCurrent = false;
        boolean _lockedNext = false;

        public DeadlockedThread(int n) {
            super();
            setDaemon(true);
            _n = n;
        }

        public void run() {
            try {
                Method m = INDIFY_Test.class.getDeclaredMethod("indyWrapper" + _n, Object.class, Object.class, Object.class);
                m.invoke(null, new Object(), new Object(), _n);
            } catch ( Throwable t ) {
                Env.getLog().complain("Exception in thread " + getName());
                t.printStackTrace(Env.getLog().getOutStream());
                _testFailed = true;
            }
        }
    }

    public static void main(String[] args) { MlvmTest.launch(args); }
}
