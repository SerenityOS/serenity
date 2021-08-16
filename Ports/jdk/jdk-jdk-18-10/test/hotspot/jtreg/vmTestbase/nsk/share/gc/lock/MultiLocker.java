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

import java.util.List;
import java.util.ArrayList;

/**
 * MultiLocker is just a locker that uses several lockers.
 */
public class MultiLocker<T> implements Locker<T> {
        private List<Locker<T>> lockers;

        public MultiLocker() {
                this(new ArrayList<Locker<T>>());
        }

        public MultiLocker(List<Locker<T>> lockers) {
                this.lockers = lockers;
        }

        public void enable() {
                for (Locker locker : lockers)
                        locker.enable();
        }

        public void lock() {
                for (Locker locker : lockers)
                        locker.lock();
        }

        public void unlock() {
                for (Locker locker : lockers)
                        locker.unlock();
        }

        public Throwable getException() {
                return null;
        }

        public void disable() {
                for (Locker locker : lockers)
                        locker.disable();
        }
}
