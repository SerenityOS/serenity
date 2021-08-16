/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.management;

import javax.management.openmbean.CompositeData;
import java.util.concurrent.locks.*;
import sun.management.LockInfoCompositeData;

/**
 * Information about a <em>lock</em>.  A lock can be a built-in object monitor,
 * an <em>ownable synchronizer</em>, or the {@link Condition Condition}
 * object associated with synchronizers.
 * <p>
 * <a id="OwnableSynchronizer">An ownable synchronizer</a> is
 * a synchronizer that may be exclusively owned by a thread and uses
 * {@link AbstractOwnableSynchronizer AbstractOwnableSynchronizer}
 * (or its subclass) to implement its synchronization property.
 * {@link ReentrantLock ReentrantLock} and the write-lock (but not
 * the read-lock) of {@link ReentrantReadWriteLock ReentrantReadWriteLock} are
 * two examples of ownable synchronizers provided by the platform.
 *
 * <h2><a id="MappedType">MXBean Mapping</a></h2>
 * {@code LockInfo} is mapped to a {@link CompositeData CompositeData}
 * as specified in the {@link #from from} method.
 *
 * @see java.util.concurrent.locks.AbstractOwnableSynchronizer
 * @see java.util.concurrent.locks.Condition
 *
 * @author  Mandy Chung
 * @since   1.6
 */

public class LockInfo {

    private String className;
    private int    identityHashCode;

    /**
     * Constructs a {@code LockInfo} object.
     *
     * @param className the fully qualified name of the class of the lock object.
     * @param identityHashCode the {@link System#identityHashCode
     *                         identity hash code} of the lock object.
     */
    public LockInfo(String className, int identityHashCode) {
        if (className == null) {
            throw new NullPointerException("Parameter className cannot be null");
        }
        this.className = className;
        this.identityHashCode = identityHashCode;
    }

    /**
     * package-private constructors
     */
    LockInfo(Object lock) {
        this.className = lock.getClass().getName();
        this.identityHashCode = System.identityHashCode(lock);
    }

    /**
     * Returns the fully qualified name of the class of the lock object.
     *
     * @return the fully qualified name of the class of the lock object.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Returns the identity hash code of the lock object
     * returned from the {@link System#identityHashCode} method.
     *
     * @return the identity hash code of the lock object.
     */
    public int getIdentityHashCode() {
        return identityHashCode;
    }

    /**
     * Returns a {@code LockInfo} object represented by the
     * given {@code CompositeData}.
     * The given {@code CompositeData} must contain the following attributes:
     * <table class="striped" style="margin-left:2em;">
     * <caption style="display:none">The attributes and the types the given CompositeData contains</caption>
     * <thead style="text-align:left">
     * <tr>
     *   <th scope="col">Attribute Name</th>
     *   <th scope="col">Type</th>
     * </tr>
     * </thead>
     * <tbody style="text-align:left">
     * <tr>
     *   <th scope="row">className</th>
     *   <td>{@code java.lang.String}</td>
     * </tr>
     * <tr>
     *   <th scope="row">identityHashCode</th>
     *   <td>{@code java.lang.Integer}</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @param cd {@code CompositeData} representing a {@code LockInfo}
     *
     * @throws IllegalArgumentException if {@code cd} does not
     *   represent a {@code LockInfo} with the attributes described
     *   above.
     * @return a {@code LockInfo} object represented
     *         by {@code cd} if {@code cd} is not {@code null};
     *         {@code null} otherwise.
     *
     * @since 1.8
     */
    public static LockInfo from(CompositeData cd) {
        if (cd == null) {
            return null;
        }

        if (cd instanceof LockInfoCompositeData) {
            return ((LockInfoCompositeData) cd).getLockInfo();
        } else {
            return LockInfoCompositeData.toLockInfo(cd);
        }
    }

    /**
     * Returns a string representation of a lock.  The returned
     * string representation consists of the name of the class of the
     * lock object, the at-sign character `@', and the unsigned
     * hexadecimal representation of the <em>identity</em> hash code
     * of the object.  This method returns a string equals to the value of:
     * <blockquote>
     * <pre>
     * lock.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(lock))
     * </pre></blockquote>
     * where {@code lock} is the lock object.
     *
     * @return the string representation of a lock.
     */
    public String toString() {
        return className + '@' + Integer.toHexString(identityHashCode);
    }
}
