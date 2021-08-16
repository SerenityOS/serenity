/*
 * Copyright (c) 1996, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package sun.rmi.transport;

import java.lang.ref.*;
import sun.rmi.runtime.Log;

/**
 * WeakRef objects are used by the RMI runtime to hold potentially weak
 * references to exported remote objects in the local object table.
 *
 * This class extends the functionality of java.lang.ref.WeakReference in
 * several ways.  The methods pin() and unpin() can be used to set
 * whether the contained reference is strong or weak (it is weak upon
 * construction).  The hashCode() and equals() methods are overridden so
 * that WeakRef objects hash and compare to each other according to the
 * object identity of their referents.
 *
 * @author  Ann Wollrath
 * @author  Peter Jones
 */
class WeakRef extends WeakReference<Object> {

    /** value of the referent's "identity" hash code */
    private int hashValue;

    /** strong reference to the referent, for when this WeakRef is "pinned" */
    private Object strongRef = null;

    /**
     * Create a new WeakRef to the given object.
     */
    public WeakRef(Object obj) {
        super(obj);
        setHashValue(obj);      // cache object's "identity" hash code
    }

    /**
     * Create a new WeakRef to the given object, registered with a queue.
     */
    public WeakRef(Object obj, ReferenceQueue<Object> q) {
        super(obj, q);
        setHashValue(obj);      // cache object's "identity" hash code
    }

    /**
     * Pin the contained reference (make this a strong reference).
     */
    public synchronized void pin() {
        if (strongRef == null) {
            strongRef = get();

            if (DGCImpl.dgcLog.isLoggable(Log.VERBOSE)) {
                DGCImpl.dgcLog.log(Log.VERBOSE,
                                   "strongRef = " + strongRef);
            }
        }
    }

    /**
     * Unpin the contained reference (make this a weak reference).
     */
    public synchronized void unpin() {
        if (strongRef != null) {
            if (DGCImpl.dgcLog.isLoggable(Log.VERBOSE)) {
                DGCImpl.dgcLog.log(Log.VERBOSE,
                                   "strongRef = " + strongRef);
            }

            strongRef = null;
        }
    }

    /*
     * Cache referent's "identity" hash code (so that we still have the
     * value after the referent gets cleared).
     *
     * We cannot use the value from the object's hashCode() method, since
     * if the object is of a remote class not extended from RemoteObject
     * and it is trying to implement hashCode() and equals() so that it
     * can be compared to stub objects, its own hash code could not have
     * been initialized yet (see bugid 4102938).  Also, object table keys
     * based on server objects are indeed matched on object identity, so
     * this is the correct hash technique regardless.
     */
    private void setHashValue(Object obj) {
        if (obj != null) {
            hashValue = System.identityHashCode(obj);
        } else {
            hashValue = 0;
        }
    }

    /**
     * Always return the "identity" hash code of the original referent.
     */
    public int hashCode() {
        return hashValue;
    }

    /**
     * Return true if "obj" is this identical WeakRef object, or, if the
     * contained reference has not been cleared, if "obj" is another WeakRef
     * object with the identical non-null referent.  Otherwise, return false.
     */
    public boolean equals(Object obj) {
        if (obj instanceof WeakRef) {
            if (obj == this)
                return true;

            Object referent = get();
            return (referent != null) && (referent == ((WeakRef) obj).get());
        } else {
            return false;
        }
    }
}
