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

package nsk.share.gc.lock.malloc;

import nsk.share.TestBug;
import nsk.share.gc.lock.Locker;
import nsk.share.gc.lock.CriticalSectionTimedLocker;

/**
 * Malloc locker tries to hold malloc lock (if there is any)
 * by calling malloc() and free() in a loop.
 */
public class MallocLocker extends CriticalSectionTimedLocker {
        static {
                System.loadLibrary("MallocLocker");
        }

        public MallocLocker() {
        }

        public MallocLocker(long enterTime, long sleepTime) {
                super(enterTime, sleepTime);
        }

        /**
         * This native method does malloc() / free() in a loop
         * while java field locked is set to true, sleeping
         * for sleepTime between malloc() and free() and after
         * free().
         */
        private native void mallocSection(long enterTime, long sleepTime);

        protected void criticalSection(long enterTime, long sleepTime) {
                mallocSection(enterTime, sleepTime);
        }
}
