/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jvmti.AttachOnDemand.attach042;

import nsk.share.aod.TargetApplicationWaitingAgents;

public class attach042Target extends TargetApplicationWaitingAgents {
    static class TestThread extends Thread {
        public void run() {
            try {
                log.display(Thread.currentThread() + " is running");
            } catch (Throwable t) {
                setStatusFailed("Unexpected exception: " + t);
                t.printStackTrace(log.getOutStream());
            }
        }
    }

    /*
     * NOTE: constant value should be up-to-date with constants in the agent's code
     */

    static final String STARTED_TEST_THREAD_NAME = "attach042-TestThread";

    protected void targetApplicationActions() throws InterruptedException {
        TestThread thread = new TestThread();
        thread.setName(STARTED_TEST_THREAD_NAME);
        thread.start();
    }

    public static void main(String[] args) {
        new attach042Target().runTargetApplication(args);
    }
}
