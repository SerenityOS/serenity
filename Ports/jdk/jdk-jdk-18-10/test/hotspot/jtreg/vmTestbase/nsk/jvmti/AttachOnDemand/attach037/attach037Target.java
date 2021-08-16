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
package nsk.jvmti.AttachOnDemand.attach037;

import nsk.share.aod.TargetApplicationWaitingAgents;

public class attach037Target extends TargetApplicationWaitingAgents {

    static class ThreadGeneratingEvents extends Thread {

        ThreadGeneratingEvents() {
            super("ThreadGeneratingEvents");
        }

        // Forces string concat initialization that might otherwise only occur
        // on an error path from a callback function. The problematic 'shape' is:
        // InvokeDynamic #0:makeConcatWithConstants:(Ljava/lang/String;Z)Ljava/lang/String;
        String preInitStringBooleanConcat(String s, boolean b) {
            return "A string " + s + b;
        }

        public void run() {
            try {
                String msg = preInitStringBooleanConcat("test", true);
                //potentially use msg so it isn't elided by the JIT
                if (System.currentTimeMillis() == 0) {
                    log.display("You should never see this " + msg);
                }

                Object monitor = new Object();

                log.display(Thread.currentThread() + " is provoking MonitorWait/MonitorWaited events");

                synchronized (monitor) {
                    monitor.wait(500);
                }
            } catch (Throwable t) {
                setStatusFailed("Unexpected exception: " + t);
                t.printStackTrace(log.getOutStream());
            }
        }
    }

    protected void targetApplicationActions() throws InterruptedException {
        ThreadGeneratingEvents threadGeneratingEvents = new ThreadGeneratingEvents();
        threadGeneratingEvents.start();
        threadGeneratingEvents.join();
    }

    public static void main(String[] args) {
        new attach037Target().runTargetApplication(args);
    }
}
