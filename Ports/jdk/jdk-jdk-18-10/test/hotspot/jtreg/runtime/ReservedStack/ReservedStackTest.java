/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test ReservedStackTest
 *
 * @requires vm.opt.DeoptimizeALot != true
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.vm.annotation
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:CompileCommand=DontInline,java/util/concurrent/locks/ReentrantLock.lock -XX:CompileCommand=exclude,java/util/concurrent/locks/AbstractOwnableSynchronizer.setExclusiveOwnerThread ReservedStackTest
 */

/* The exclusion of java.util.concurrent.locks.AbstractOwnableSynchronizer.setExclusiveOwnerThread()
 * from the compilable methods is required to ensure that the test will be able
 * to trigger a StackOverflowError on the right method.
 *
 * The DontInline directive for ReentrantLock.lock() ensures that lockAndcall
 * is not considered as being annotated by ReservedStackAccess by virtue of
 * it inlining such a method.
 */


/*
 * Notes about this test:
 * This test tries to reproduce a rare but nasty corruption bug that
 * occurs when a StackOverflowError is thrown in some critical sections
 * of the ReentrantLock implementation.
 *
 * Here's the critical section where a corruption could occur
 * (from java.util.concurrent.ReentrantLock.java)
 *
 * final void lock() {
 *     if (compareAndSetState(0, 1))
 *         setExclusiveOwnerThread(Thread.currentThread());
 *     else
 *         acquire(1);
 * }
 *
 * The corruption occurs when the compareAndSetState(0, 1)
 * successfully updates the status of the lock but the method
 * fails to set the owner because of a stack overflow.
 * HotSpot checks for stack overflow on method invocations.
 * The test must trigger a stack overflow either when
 * Thread.currentThread() or setExclusiveOwnerThread() is
 * invoked.
 *
 * The test starts with a recursive invocation loop until a
 * first StackOverflowError is thrown, the Error is caught
 * and a few dozen frames are exited. Now the thread has
 * little free space on its execution stack and will try
 * to trigger a stack overflow in the critical section.
 * The test has a huge array of ReentrantLocks instances.
 * The thread invokes a recursive method which, at each
 * of its invocations, tries to acquire the next lock
 * in the array. The execution continues until a
 * StackOverflowError is thrown or the end of the array
 * is reached.
 * If no StackOverflowError has been thrown, the test
 * is non conclusive (recommendation: increase the size
 * of the ReentrantLock array).
 * The status of all Reentrant locks in the array is checked,
 * if a corruption is detected, the test failed, otherwise
 * the test passed.
 *
 * To have a chance that the stack overflow occurs on one
 * of the two targeted method invocations, the test is
 * repeated in different threads. Each Java thread has a
 * random size area allocated at the beginning of its
 * stack to prevent false sharing. The test relies on this
 * to have different stack alignments when it hits the targeted
 * methods (the test could have been written with a native
 * method with alloca, but using different Java threads makes
 * the test 100% Java).
 *
 * One additional trick is required to ensure that the stack
 * overflow will occur on the Thread.currentThread() getter
 * or the setExclusiveOwnerThread() setter.
 *
 * Potential stack overflows are detected by stack banging,
 * at method invocation time.
 * In interpreted code, the stack banging performed for the
 * lock() method goes further than the stack banging performed
 * for the getter or the setter method, so the potential stack
 * overflow is detected before entering the critical section.
 * In compiled code, the getter and the setter are in-lined,
 * so the stack banging is only performed before entering the
 * critical section.
 * In order to have a stack banging that goes further for the
 * getter/setter methods than for the lock() method, the test
 * exploits the property that interpreter frames are (much)
 * bigger than compiled code frames. When the test is run,
 * a compiler option disables the compilation of the
 * setExclusiveOwnerThread() method.
 *
 */

import java.util.concurrent.locks.ReentrantLock;
import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class ReservedStackTest {

    static class ReentrantLockTest {

        private ReentrantLock lockArray[];
        // Frame sizes vary a lot between interpreted code and compiled code
        // so the lock array has to be big enough to cover all cases.
        // If test fails with message "Not conclusive test", try to increase
        // LOCK_ARRAY_SIZE value
        private static final int LOCK_ARRAY_SIZE = 8192;
        private boolean stackOverflowErrorReceived;
        StackOverflowError soe = null;
        private int index = -1;

        public void initialize() {
            lockArray = new ReentrantLock[LOCK_ARRAY_SIZE];
            for (int i = 0; i < LOCK_ARRAY_SIZE; i++) {
                lockArray[i] = new ReentrantLock();
            }
            stackOverflowErrorReceived = false;
        }

        public String getResult() {
            if (!stackOverflowErrorReceived) {
                return "ERROR: Not conclusive test: no StackOverflowError received";
            }
            for (int i = 0; i < LOCK_ARRAY_SIZE; i++) {
                if (lockArray[i].isLocked()) {
                    if (!lockArray[i].isHeldByCurrentThread()) {
                        StringBuilder s = new StringBuilder();
                        s.append("FAILED: ReentrantLock ");
                        s.append(i);
                        s.append(" looks corrupted");
                        return s.toString();
                    }
                }
            }
            return "PASSED";
        }

        public void run() {
            try {
                lockAndCall(0);
            } catch (StackOverflowError e) {
                soe = e;
                stackOverflowErrorReceived = true;
                throw e;
            }
        }

        private void lockAndCall(int i) {
            index = i;
            if (i < LOCK_ARRAY_SIZE) {
                lockArray[i].lock();
                lockAndCall(i + 1);
            }
        }
    }

    static class RunWithSOEContext implements Runnable {

        int counter;
        int deframe;
        int decounter;
        int setupSOEFrame;
        int testStartFrame;
        ReentrantLockTest test;

        public RunWithSOEContext(ReentrantLockTest test, int deframe) {
            this.test = test;
            this.deframe = deframe;
        }

        @Override
        @jdk.internal.vm.annotation.ReservedStackAccess
        public void run() {
            counter = 0;
            decounter = deframe;
            test.initialize();
            recursiveCall();
            String result = test.getResult();
            // The feature is not fully implemented on all platforms,
            // corruptions are still possible.
            if (isSupportedPlatform && !result.contains("PASSED")) {
                throw new Error(result);
            } else {
                // Either the test passed or this platform is not supported.
                // On not supported platforms, we only expect the VM to
                // not crash during the test. This is especially important
                // on Windows where the detection of SOE in annotated
                // sections is implemented but the reserved zone mechanism
                // to avoid the corruption cannot be implemented yet
                // because of JDK-8067946
                System.out.println("PASSED");
            }
        }

        void recursiveCall() {
            // Unused local variables to increase the frame size
            long l1, l2, l3, l4, l5, l6, l7, l8, l9, l10, l11, l12, l13, l14, l15, l16, l17, l18, l19;
            long l20, l21, l22, l23, l24, l25, l26, l27, l28, l30, l31, l32, l33, l34, l35, l36, l37;
            counter++;
            try {
                recursiveCall();
            } catch (StackOverflowError e) {
            }
            decounter--;
            if (decounter == 0) {
                setupSOEFrame = counter;
                testStartFrame = counter - deframe;
                test.run();
            }
        }
    }

    private static boolean isAlwaysSupportedPlatform() {
        return Platform.isAix() ||
            (Platform.isLinux() &&
             (Platform.isPPC() || Platform.isS390x() || Platform.isX64() ||
              Platform.isX86() || Platform.isAArch64())) ||
            Platform.isOSX();
    }

    private static boolean isNeverSupportedPlatform() {
        return !isAlwaysSupportedPlatform();
    }

    private static boolean isSupportedPlatform;

    private static void initIsSupportedPlatform() throws Exception {
        // In order to dynamicaly determine if the platform supports the reserved
        // stack area, run with -XX:StackReservedPages=1 and see if we get the
        // expected warning message for platforms that don't support it.
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("-XX:StackReservedPages=1", "-version");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        System.out.println("StackReservedPages=1 log: [" + output.getOutput() + "]");
        if (output.getExitValue() != 0) {
            String msg = "Could not launch with -XX:StackReservedPages=1: exit " + output.getExitValue();
            System.err.println("FAILED: " + msg);
            throw new RuntimeException(msg);
        }

        isSupportedPlatform = true;
        String matchStr = "Reserved Stack Area not supported on this platform";
        int match_idx = output.getOutput().indexOf(matchStr);
        if (match_idx >= 0) {
            isSupportedPlatform = false;
        }

        // Do a sanity check. Some platforms we know are always supported. Make sure
        // we didn't determine that one of those platforms is not supported.
        if (!isSupportedPlatform && isAlwaysSupportedPlatform()) {
            String msg  = "This platform should be supported: " + Platform.getOsArch();
            System.err.println("FAILED: " +  msg);
            throw new RuntimeException(msg);
        }

        // And some platforms we know are never supported. Make sure
        // we didn't determine that one of those platforms is supported.
        if (isSupportedPlatform && isNeverSupportedPlatform()) {
            String msg  = "This platform should not be supported: " + Platform.getOsArch();
            System.err.println("FAILED: " +  msg);
            throw new RuntimeException(msg);
        }
    }

    public static void main(String[] args) throws Exception {
        initIsSupportedPlatform();
        for (int i = 0; i < 100; i++) {
            // Each iteration has to be executed by a new thread. The test
            // relies on the random size area pushed by the VM at the beginning
            // of the stack of each Java thread it creates.
            Thread thread = new Thread(new RunWithSOEContext(new ReentrantLockTest(), 256));
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException ex) { }
        }
    }
}
