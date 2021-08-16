/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4333920 4994372
 * @summary ChunkedEncoding unit test; MeteredStream/ProgressData problem
 * @modules java.base/sun.net
 *          jdk.httpserver
 * @library /test/lib
 * @run main ChunkedEncodingWithProgressMonitorTest
 */

import java.net.*;
import java.util.BitSet;
import sun.net.ProgressMeteringPolicy;
import sun.net.ProgressMonitor;
import sun.net.ProgressListener;
import sun.net.ProgressEvent;

public class ChunkedEncodingWithProgressMonitorTest {
    public static void main (String[] args) throws Exception {
        ProgressMonitor.setMeteringPolicy(new MyProgressMeteringPolicy());
        ProgressListener listener = new MyProgressListener();
        ProgressMonitor.getDefault().addProgressListener(listener);
        ChunkedEncodingTest.test();
        ProgressMonitor.getDefault().removeProgressListener(listener);

        if (flag.cardinality() != 3) {
            throw new RuntimeException("All three methods in ProgressListener"+
                                       " should be called. Yet the number of"+
                                       " methods actually called are "+
                                       flag.cardinality());
        }
    }

    static class MyProgressMeteringPolicy implements ProgressMeteringPolicy {
        /**
         * Return true if metering should be turned on for a particular network input stream.
         */
        public boolean shouldMeterInput(URL url, String method) {
            return true;
        }

        /**
         * Return update notification threshold.
         */
        public int getProgressUpdateThreshold() {
            return 8192;
        }
    }

    static BitSet flag = new BitSet(3);

    static class MyProgressListener implements ProgressListener {
        /**
         * Start progress.
         */
        public void progressStart(ProgressEvent evt) {
            System.out.println("start: received progressevent "+evt);
            if (flag.nextSetBit(0) == -1)
                flag.set(0);
        }

        /**
         * Update progress.
         */
        public void progressUpdate(ProgressEvent evt) {
            System.out.println("update: received progressevent "+evt);
            if (flag.nextSetBit(1) == -1)
                flag.set(1);
        }

        /**
         * Finish progress.
         */
        public void progressFinish(ProgressEvent evt) {
            System.out.println("finish: received progressevent "+evt);
            if (flag.nextSetBit(2) == -1)
                flag.set(2);
        }
    }
}
