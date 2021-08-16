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

import nsk.share.gc.gp.GarbageProducer;

/**
 * Locker represents a way to lock a resource
 * which may be critical for GC to work.
 */
public interface Locker<T> {
        /**
         * Enable this locker and make necessary preparations.
         */
        public void enable();

        /**
         * Lock an object.
         *
         * There can be multiple calls to this, but to lock
         * again, one must first unlock.
         */
        public void lock();

        /**
         * Unlock an object.
         *
         */
        public void unlock();

        /**
         * Get any exceptions that occured.
         *
         * @return exception or null if there none
         */
        public Throwable getException();

        /**
         * Disable this locker and make necessary cleanups.
         */
        public void disable();
}
