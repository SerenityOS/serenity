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

package nsk.share.gc.lock;

import nsk.share.TestBug;
import nsk.share.gc.lock.Locker;

/**
 * CriticalSectionTimedLocker represents a way to lock a resource
 * by entering some critical section for some time.
 */
public abstract class CriticalSectionTimedLocker<T> extends CriticalSectionLocker<T> {
        private long enterTime;
        private long sleepTime;

        public CriticalSectionTimedLocker() {
                this(5000, 10);
        }

        public CriticalSectionTimedLocker(long enterTime, long sleepTime) {
                setEnterTime(enterTime);
                setSleepTime(sleepTime);
        }

        protected final void criticalSection() {
                criticalSection(enterTime, sleepTime);
        }

        /**
         * Enter critical section for enterTime.
         *
         * Usually, something is done in a loop inside this critical section.
         * In this case, sleepTime is time to sleep after each iteration.
         */
        protected abstract void criticalSection(long enterTime, long sleepTime);

        public final void setEnterTime(long enterTime) {
                this.enterTime = enterTime;
        }

        public final void setSleepTime(long sleepTime) {
                this.sleepTime = sleepTime;
        }
}
