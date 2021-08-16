/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.consumer.recordingstream;

import java.util.concurrent.CountDownLatch;

public class TestUtils {

    public static final class TestError extends Error {
        private static final long serialVersionUID = 1L;
    }

    public static final class TestException extends Exception {
        private static final long serialVersionUID = 1L;
        private volatile boolean printed;

        @Override
        public void printStackTrace() {
            super.printStackTrace();
            printed = true;
        }

        public boolean isPrinted() {
            return printed;
        }
    }

    // Can throw checked exception as unchecked.
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void throwUnchecked(Throwable e) throws T {
        throw (T) e;
    }

    public static void installUncaughtException(CountDownLatch receivedError, Throwable expected) {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            if (throwable == expected) {
                System.out.println("Received uncaught exception " + expected.getClass());
                receivedError.countDown();
            }
        });
    }
}
