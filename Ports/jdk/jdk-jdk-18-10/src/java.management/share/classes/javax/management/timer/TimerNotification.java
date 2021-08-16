/*
 * Copyright (c) 1999, 2007, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.timer;

/**
 * This class provides definitions of the notifications sent by timer MBeans.
 * <BR>It defines a timer notification identifier which allows to retrieve a timer notification
 * from the list of notifications of a timer MBean.
 * <P>
 * The timer notifications are created and handled by the timer MBean.
 *
 * @since 1.5
 */
public class TimerNotification extends javax.management.Notification {


    /* Serial version */
    private static final long serialVersionUID = 1798492029603825750L;

    /*
     * ------------------------------------------
     *  PRIVATE VARIABLES
     * ------------------------------------------
     */

    /**
     * @serial Timer notification identifier.
     *         This identifier is used to retrieve a timer notification from the timer list of notifications.
     */
    private Integer notificationID;


    /*
     * ------------------------------------------
     *  CONSTRUCTORS
     * ------------------------------------------
     */

    /**
     * Creates a timer notification object.
     *
     * @param type The notification type.
     * @param source The notification producer.
     * @param sequenceNumber The notification sequence number within the source object.
     * @param timeStamp The notification emission date.
     * @param msg The notification message.
     * @param id The notification identifier.
     *
     */
    public TimerNotification(String type, Object source, long sequenceNumber, long timeStamp, String msg, Integer id) {

        super(type, source, sequenceNumber, timeStamp, msg);
        this.notificationID = id;
    }

    /*
     * ------------------------------------------
     *  PUBLIC METHODS
     * ------------------------------------------
     */

    // GETTERS AND SETTERS
    //--------------------

    /**
     * Gets the identifier of this timer notification.
     *
     * @return The identifier.
     */
    public Integer getNotificationID() {
        return notificationID;
    }

    /*
     * ------------------------------------------
     *  PACKAGE METHODS
     * ------------------------------------------
     */

    /**
     * Creates and returns a copy of this object.
     *
     */
    Object cloneTimerNotification() {

        TimerNotification clone = new TimerNotification(this.getType(), this.getSource(), this.getSequenceNumber(),
                                                        this.getTimeStamp(), this.getMessage(), notificationID);
        clone.setUserData(this.getUserData());
        return clone;
    }
}
