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
package nsk.share.locks;

import nsk.share.*;

/*
 * DeadlockLocker - class used for deadlock creation
 *
 * To create deadlock second 'inner' instance of DeadlockLocker is required.
 *
 * Deadlock creation scenario:
 *      - object acquires its resource
 *      - notify 'inner' through Wicket step1 that resource is locked and 'inner' can try to acquire this object's resource
 *      - wait notification through Wicket step2 from 'inner' that 'inner' acquired its resource
 *      - call readyWicket.unlock to notify thread waiting deadlock creation
 *      - try acquire locked inner's resource, at the same time inner trying to acquire locked this object's resource - deadlock is created
 *
 * Subclasses should implement method doLock() taking in account described scenario.
 *
 *  Objects of DeadlockLocker should not be created directly, to create deadlocked threads use class DeadlockMaker
 */
abstract public class DeadlockLocker {
    protected DeadlockLocker inner;

    protected Wicket step1;

    protected Wicket step2;

    // 'readyWicket' is used to notify DeadlockMacker that deadlock is almost created
    protected Wicket readyWicket;

    public DeadlockLocker(Wicket step1, Wicket step2, Wicket readyWicket) {
        this.step1 = step1;
        this.step2 = step2;
        this.readyWicket = readyWicket;
    }

    public void setInner(DeadlockLocker inner) {
        this.inner = inner;
    }

    protected void checkInnerLocker() {
        if (inner == null) {
            throw new IllegalStateException(getClass().getName() + " deadlockLocker's inner locker is null");
        }
    }

    protected abstract void doLock();

    abstract public Object getLock();

    private int lockCount;

    final public void lock() {
        // call this method once
        if (lockCount++ > 1)
            return;

        // check that inner locker was set
        checkInnerLocker();

        doLock();
    }

}
