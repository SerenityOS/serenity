/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn009;

import nsk.share.Log;
import nsk.share.jdi.*;

public class forceEarlyReturn009a extends AbstractJDIDebuggee {

    public static void main(String args[]) {
        new forceEarlyReturn009a().doTest(args);
    }

    public final static String testThreadWithSingleFrameName = "forceEarlyReturn009aTestThreadWithSingleFrame";

    // command:[inlineType]
    public final static String COMMAND_RUN_THREAD_WITH_SINGLE_FRAME = "runThreadWithSingleFrame";

    public final static String COMMAND_JOIN_THREAD_WITH_SINGLE_FRAME = "joinThreadWithSingleFrame";

    // type of inlined methods
    enum InlineType {
        // method return const values
        INLINE_METHOD_RETURNING_CONST,

        // method access only internal fields
        INLINE_METHOD_ACCESSIN_INTERNAL_FIELDS,

        // method which should be executed a certain number of times before it is compiled
        INLINE_HOT_METHOD
    }

    /*
     * Thread can has single frame on the stack because of methods which this thread calls was inlined
     *
     * inlineType - type of inlined methods which test thread calls
     */
    private InlineType inlineType;

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        if (command.startsWith(COMMAND_RUN_THREAD_WITH_SINGLE_FRAME)) {
            int index = command.indexOf(':');

            if (index < 0) {
                inlineType = null;
            } else {
                inlineType = InlineType.valueOf(command.substring(index + 1));
                log.display("InlineType: " + inlineType);
            }

            runThreadWithSingleFrame();

            return true;
        } else if (command.equals(COMMAND_JOIN_THREAD_WITH_SINGLE_FRAME)) {
            joinThreadWithSingleFrame();
            return true;
        }

        return false;
    }

    private ThreadWithSingleFrame threadWithSingleFrame;

    static class ThreadWithSingleFrame extends Thread {

        volatile boolean isSingleFrameThreadStarted;

        // this variable is used to stop thread with signle frame if debugger can't stop it by forceEarlyReturn
        // (for example if inlining was not done)
        volatile boolean isSingleFrameThreadStoped;

        private Log log;

        private InlineType inlineType;

        public ThreadWithSingleFrame(Log log, InlineType inlineType) {
            this.log = log;
            this.inlineType = inlineType;

            if (inlineType == InlineType.INLINE_HOT_METHOD) {
                // call inlinedHotMethod1() and inlinedHotMethod2() 20000 times to be sure that this method was compiled/inlined
                for (long i = 0; i < 20000; i++) {
                    inlinedHotMethod1();
                    inlinedHotMethod2();
                }
            }

        }

        private int internalField1;

        private int inlinedMethodAccessingInternalFields1() {
            return internalField1++ + internalField2;
        }

        private int internalField2;

        private int inlinedMethodAccessingInternalFields2() {
            return internalField2++ + internalField1;
        }

        private int inlinedMethodReturningInt() {
            return 0;
        }

        private boolean inlinedMethodReturningBoolean() {
            return true;
        }

        private Object inlinedMethodReturningObject() {
            return null;
        }

        public int publicField1;

        public int publicField2;

        public int inlinedHotMethod1() {
            return publicField1++ + publicField2 * 2;
        }

        public int inlinedHotMethod2() {
            return publicField2++ + publicField1 * 2;
        }

        public void run() {
            Thread.currentThread().setName(testThreadWithSingleFrameName);

            log.display("Thread with single frame started");

            // if inlineType was specified call methods which should be inlined
            if (inlineType == InlineType.INLINE_METHOD_ACCESSIN_INTERNAL_FIELDS) {
                while (!isSingleFrameThreadStoped) {
                    inlinedMethodAccessingInternalFields1();
                    inlinedMethodAccessingInternalFields2();
                    isSingleFrameThreadStarted = true;
                }
            } else if (inlineType == InlineType.INLINE_METHOD_RETURNING_CONST) {
                while (!isSingleFrameThreadStoped) {
                    boolean bool = inlinedMethodReturningBoolean();
                    int integer = inlinedMethodReturningInt();
                    Object object = inlinedMethodReturningObject();
                    isSingleFrameThreadStarted = true;

                }
            } else if (inlineType == InlineType.INLINE_HOT_METHOD) {
                while (!isSingleFrameThreadStoped) {
                    int temp = inlinedHotMethod1() + inlinedHotMethod2();
                    isSingleFrameThreadStarted = true;
                }
            } else {
                while (!isSingleFrameThreadStoped)
                    isSingleFrameThreadStarted = true;
            }
        }
    }

    private void runThreadWithSingleFrame() {
        threadWithSingleFrame = new ThreadWithSingleFrame(log, inlineType);
        threadWithSingleFrame.start();

        while (!threadWithSingleFrame.isSingleFrameThreadStarted)
            Thread.yield();
    }

    private void joinThreadWithSingleFrame() {
        try {
            threadWithSingleFrame.isSingleFrameThreadStoped = true;

            threadWithSingleFrame.join(argHandler.getWaitTime() * 60000);

            if (threadWithSingleFrame.getState() != Thread.State.TERMINATED) {
                setSuccess(false);
                log.complain("ThreadWithSingleThread didn'f finish execution as expected");
            }
        } catch (InterruptedException e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
        }
    }

}
