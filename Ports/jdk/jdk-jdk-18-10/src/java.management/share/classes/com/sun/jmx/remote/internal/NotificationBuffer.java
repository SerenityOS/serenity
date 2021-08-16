/*
 * Copyright (c) 2003, 2006, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jmx.remote.internal;

import java.util.Set;
import javax.management.remote.NotificationResult;
import javax.management.remote.TargetedNotification;

/** A buffer of notifications received from an MBean server. */
public interface NotificationBuffer {
    /**
     * <p>Fetch notifications that match the given listeners.</p>
     *
     * <p>The operation only considers notifications with a sequence
     * number at least <code>startSequenceNumber</code>.  It will take
     * no longer than <code>timeout</code>, and will return no more
     * than <code>maxNotifications</code> different notifications.</p>
     *
     * <p>If there are no notifications matching the criteria, the
     * operation will block until one arrives, subject to the
     * timeout.</p>
     *
     * @param filter an object that will add notifications to a
     * {@code List<TargetedNotification>} if they match the current
     * listeners with their filters.
     * @param startSequenceNumber the first sequence number to
     * consider.
     * @param timeout the maximum time to wait.  May be 0 to indicate
     * not to wait if there are no notifications.
     * @param maxNotifications the maximum number of notifications to
     * return.  May be 0 to indicate a wait for eligible notifications
     * that will return a usable <code>nextSequenceNumber</code>.  The
     * {@link TargetedNotification} array in the returned {@link
     * NotificationResult} may contain more than this number of
     * elements but will not contain more than this number of
     * different notifications.
     */
    public NotificationResult
        fetchNotifications(NotificationBufferFilter filter,
                           long startSequenceNumber,
                           long timeout,
                           int maxNotifications)
            throws InterruptedException;

    /**
     * <p>Discard this buffer.</p>
     */
    public void dispose();
}
