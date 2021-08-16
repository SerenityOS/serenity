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
package nsk.monitoring.share.thread;

import nsk.share.log.Log;
import java.lang.management.ThreadInfo;
import nsk.share.TestFailure;
import nsk.share.TestBug;

/**
 * Finished Thread is dummy RecursiveMonitoringThread that is getting
 * started and finished.
 */
public class FinishedThread extends RecursiveMonitoringThread {
        public FinishedThread(Log log, RunType recursionType, int maxDepth) {
                super(log, recursionType, maxDepth);
        }

        public void waitState() {
                try {
                        if (runner != null)
                                runner.join();
                } catch (InterruptedException e) {
                        log.warn(e);
                }
        }

        public void finish() {
        }

        protected void runInside() {
        }

        public void checkThreadInfo(ThreadInfo info) {
                verify(info == null, "ThreadInfo != null");
        }
}
