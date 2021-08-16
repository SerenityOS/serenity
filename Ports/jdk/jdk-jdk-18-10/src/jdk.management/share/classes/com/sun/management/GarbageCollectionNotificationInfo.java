/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.management;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataView;
import javax.management.openmbean.CompositeType;
import com.sun.management.internal.GarbageCollectionNotifInfoCompositeData;

/**
 * The information about a garbage collection
 *
 * <p>
 * A garbage collection notification is emitted by {@link GarbageCollectorMXBean}
 * when the Java virtual machine completes a garbage collection action
 * The notification emitted will contain the garbage collection notification
 * information about the status of the memory:
 * <ul>
 *   <li>The name of the garbage collector used to perform the collection.</li>
 *   <li>The action performed by the garbage collector.</li>
 *   <li>The cause of the garbage collection action.</li>
 *   <li>A {@link GcInfo} object containing some statistics about the GC cycle
          (start time, end time) and the memory usage before and after
          the GC cycle.</li>
 * </ul>
 *
 * <p>
 * A {@link CompositeData CompositeData} representing
 * the {@code GarbageCollectionNotificationInfo} object
 * is stored in the
 * {@linkplain javax.management.Notification#setUserData userdata}
 * of a {@linkplain javax.management.Notification notification}.
 * The {@link #from from} method is provided to convert from
 * a {@code CompositeData} to a {@code GarbageCollectionNotificationInfo}
 * object. For example:
 *
 * <blockquote><pre>
 *      Notification notif;
 *
 *      // receive the notification emitted by a GarbageCollectorMXBean and set to notif
 *      ...
 *
 *      String notifType = notif.getType();
 *      if (notifType.equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
 *          // retrieve the garbage collection notification information
 *          CompositeData cd = (CompositeData) notif.getUserData();
 *          GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from(cd);
 *          ....
 *      }
 * </pre></blockquote>
 *
 * <p>
 * The type of the notification emitted by a {@code GarbageCollectorMXBean} is:
 * <ul>
 *   <li>A {@linkplain #GARBAGE_COLLECTION_NOTIFICATION garbage collection notification}.
 *       <br>Used by every notification emitted by the garbage collector, the details about
 *             the notification are provided in the {@linkplain #getGcAction action} String
 *       </li>
 * </ul>
 **/

public class GarbageCollectionNotificationInfo implements  CompositeDataView {

    private final String gcName;
    private final String gcAction;
    private final String gcCause;
    private final GcInfo gcInfo;
    private final CompositeData cdata;

    /**
     * Notification type denoting that
     * the Java virtual machine has completed a garbage collection cycle.
     * This notification is emitted by a {@link GarbageCollectorMXBean}.
     * The value of this notification type is
     * {@code com.sun.management.gc.notification}.
     */
    public static final String GARBAGE_COLLECTION_NOTIFICATION =
        "com.sun.management.gc.notification";

    /**
     * Constructs a {@code GarbageCollectionNotificationInfo} object.
     *
     * @param gcName The name of the garbage collector used to perform the collection
     * @param gcAction The name of the action performed by the garbage collector
     * @param gcCause The cause of the garbage collection action
     * @param gcInfo  a GcInfo object providing statistics about the GC cycle
     */
    public GarbageCollectionNotificationInfo(String gcName,
                                             String gcAction,
                                             String gcCause,
                                             GcInfo gcInfo)  {
        if (gcName == null) {
            throw new NullPointerException("Null gcName");
        }
        if (gcAction == null) {
            throw new NullPointerException("Null gcAction");
        }
        if (gcCause == null) {
            throw new NullPointerException("Null gcCause");
        }
        this.gcName = gcName;
        this.gcAction = gcAction;
        this.gcCause = gcCause;
        this.gcInfo = gcInfo;
        this.cdata = new GarbageCollectionNotifInfoCompositeData(this);
    }

    GarbageCollectionNotificationInfo(CompositeData cd) {
        GarbageCollectionNotifInfoCompositeData.validateCompositeData(cd);

        this.gcName = GarbageCollectionNotifInfoCompositeData.getGcName(cd);
        this.gcAction = GarbageCollectionNotifInfoCompositeData.getGcAction(cd);
        this.gcCause = GarbageCollectionNotifInfoCompositeData.getGcCause(cd);
        this.gcInfo = GarbageCollectionNotifInfoCompositeData.getGcInfo(cd);
        this.cdata = cd;
    }

    /**
     * Returns the name of the garbage collector used to perform the collection
     *
     * @return the name of the garbage collector used to perform the collection
     */
    public String getGcName() {
        return gcName;
    }

    /**
     * Returns the action performed by the garbage collector
     *
     * @return the action performed by the garbage collector
     */
    public String getGcAction() {
        return gcAction;
    }

    /**
     * Returns the cause of the garbage collection
     *
     * @return the cause of the garbage collection
     */
    public String getGcCause() {
        return gcCause;
    }

    /**
     * Returns the GC information related to the last garbage collection
     *
     * @return the GC information related to the
     * last garbage collection
     */
    public GcInfo getGcInfo() {
        return gcInfo;
    }

    /**
     * Returns a {@code GarbageCollectionNotificationInfo} object represented by the
     * given {@code CompositeData}.
     * The given {@code CompositeData} must contain
     * the following attributes:
     * <blockquote>
     * <table class="striped"><caption style="display:none">description</caption>
     * <thead>
     * <tr>
     *   <th scope="col" style="text-align:left">Attribute Name</th>
     *   <th scope="col" style="text-align:left">Type</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     *   <th scope="row">gcName</th>
     *   <td>{@code java.lang.String}</td>
     * </tr>
     * <tr>
     *   <th scope="row">gcAction</th>
     *   <td>{@code java.lang.String}</td>
     * </tr>
     * <tr>
     *   <th scope="row">gcCause</th>
     *   <td>{@code java.lang.String}</td>
     * </tr>
     * <tr>
     *   <th scope="row">gcInfo</th>
     *   <td>{@code javax.management.openmbean.CompositeData}</td>
     * </tr>
     * </tbody>
     * </table>
     * </blockquote>
     *
     * @param cd {@code CompositeData} representing a
     *           {@code GarbageCollectionNotificationInfo}
     *
     * @throws IllegalArgumentException if {@code cd} does not
     *   represent a {@code GarbaageCollectionNotificationInfo} object.
     *
     * @return a {@code GarbageCollectionNotificationInfo} object represented
     *         by {@code cd} if {@code cd} is not {@code null};
     *         {@code null} otherwise.
     */
    public static GarbageCollectionNotificationInfo from(CompositeData cd) {
        if (cd == null) {
            return null;
        }

        if (cd instanceof GarbageCollectionNotifInfoCompositeData) {
            return ((GarbageCollectionNotifInfoCompositeData) cd).getGarbageCollectionNotifInfo();
        } else {
            return new GarbageCollectionNotificationInfo(cd);
        }
    }

    public CompositeData toCompositeData(CompositeType ct) {
        return cdata;
    }

}
