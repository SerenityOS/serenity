/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.monitor;


// jmx imports
//
import javax.management.ObjectName;

/**
 * Provides definitions of the notifications sent by monitor MBeans.
 * <P>
 * The notification source and a set of parameters concerning the monitor MBean's state
 * need to be specified when creating a new object of this class.
 *
 * The list of notifications fired by the monitor MBeans is the following:
 *
 * <UL>
 * <LI>Common to all kind of monitors:
 *     <UL>
 *     <LI>The observed object is not registered in the MBean server.
 *     <LI>The observed attribute is not contained in the observed object.
 *     <LI>The type of the observed attribute is not correct.
 *     <LI>Any exception (except the cases described above) occurs when trying to get the value of the observed attribute.
 *     </UL>
 * <LI>Common to the counter and the gauge monitors:
 *     <UL>
 *     <LI>The threshold high or threshold low are not of the same type as the gauge (gauge monitors).
 *     <LI>The threshold or the offset or the modulus are not of the same type as the counter (counter monitors).
 *     </UL>
 * <LI>Counter monitors only:
 *     <UL>
 *     <LI>The observed attribute has reached the threshold value.
 *     </UL>
 * <LI>Gauge monitors only:
 *     <UL>
 *     <LI>The observed attribute has exceeded the threshold high value.
 *     <LI>The observed attribute has exceeded the threshold low value.
 *     </UL>
 * <LI>String monitors only:
 *     <UL>
 *     <LI>The observed attribute has matched the "string to compare" value.
 *     <LI>The observed attribute has differed from the "string to compare" value.
 *     </UL>
 * </UL>
 *
 *
 * @since 1.5
 */
public class MonitorNotification extends javax.management.Notification {


    /*
     * ------------------------------------------
     *  PUBLIC VARIABLES
     * ------------------------------------------
     */

    /**
     * Notification type denoting that the observed object is not registered in the MBean server.
     * This notification is fired by all kinds of monitors.
     * <BR>The value of this notification type is <CODE>jmx.monitor.error.mbean</CODE>.
     */
    public static final String OBSERVED_OBJECT_ERROR = "jmx.monitor.error.mbean";

    /**
     * Notification type denoting that the observed attribute is not contained in the observed object.
     * This notification is fired by all kinds of monitors.
     * <BR>The value of this notification type is <CODE>jmx.monitor.error.attribute</CODE>.
     */
    public static final String OBSERVED_ATTRIBUTE_ERROR = "jmx.monitor.error.attribute";

    /**
     * Notification type denoting that the type of the observed attribute is not correct.
     * This notification is fired by all kinds of monitors.
     * <BR>The value of this notification type is <CODE>jmx.monitor.error.type</CODE>.
     */
    public static final String OBSERVED_ATTRIBUTE_TYPE_ERROR = "jmx.monitor.error.type";

    /**
     * Notification type denoting that the type of the thresholds, offset or modulus is not correct.
     * This notification is fired by counter and gauge monitors.
     * <BR>The value of this notification type is <CODE>jmx.monitor.error.threshold</CODE>.
     */
    public static final String THRESHOLD_ERROR = "jmx.monitor.error.threshold";

    /**
     * Notification type denoting that a non-predefined error type has occurred when trying to get the value of the observed attribute.
     * This notification is fired by all kinds of monitors.
     * <BR>The value of this notification type is <CODE>jmx.monitor.error.runtime</CODE>.
     */
    public static final String RUNTIME_ERROR = "jmx.monitor.error.runtime";

    /**
     * Notification type denoting that the observed attribute has reached the threshold value.
     * This notification is only fired by counter monitors.
     * <BR>The value of this notification type is <CODE>jmx.monitor.counter.threshold</CODE>.
     */
    public static final String THRESHOLD_VALUE_EXCEEDED = "jmx.monitor.counter.threshold";

    /**
     * Notification type denoting that the observed attribute has exceeded the threshold high value.
     * This notification is only fired by gauge monitors.
     * <BR>The value of this notification type is <CODE>jmx.monitor.gauge.high</CODE>.
     */
    public static final String THRESHOLD_HIGH_VALUE_EXCEEDED = "jmx.monitor.gauge.high";

    /**
     * Notification type denoting that the observed attribute has exceeded the threshold low value.
     * This notification is only fired by gauge monitors.
     * <BR>The value of this notification type is <CODE>jmx.monitor.gauge.low</CODE>.
     */
    public static final String THRESHOLD_LOW_VALUE_EXCEEDED = "jmx.monitor.gauge.low";

    /**
     * Notification type denoting that the observed attribute has matched the "string to compare" value.
     * This notification is only fired by string monitors.
     * <BR>The value of this notification type is <CODE>jmx.monitor.string.matches</CODE>.
     */
    public static final String STRING_TO_COMPARE_VALUE_MATCHED = "jmx.monitor.string.matches";

    /**
     * Notification type denoting that the observed attribute has differed from the "string to compare" value.
     * This notification is only fired by string monitors.
     * <BR>The value of this notification type is <CODE>jmx.monitor.string.differs</CODE>.
     */
    public static final String STRING_TO_COMPARE_VALUE_DIFFERED = "jmx.monitor.string.differs";


    /*
     * ------------------------------------------
     *  PRIVATE VARIABLES
     * ------------------------------------------
     */

    /* Serial version */
    private static final long serialVersionUID = -4608189663661929204L;

    /**
     * @serial Monitor notification observed object.
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private ObjectName observedObject = null;

    /**
     * @serial Monitor notification observed attribute.
     */
    private String observedAttribute = null;

    /**
     * @serial Monitor notification derived gauge.
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private Object derivedGauge = null;

    /**
     * @serial Monitor notification release mechanism.
     *         This value is used to keep the threshold/string (depending on the
     *         monitor type) that triggered off this notification.
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private Object trigger = null;


    /*
     * ------------------------------------------
     *  CONSTRUCTORS
     * ------------------------------------------
     */

    /**
     * Creates a monitor notification object.
     *
     * @param type The notification type.
     * @param source The notification producer.
     * @param sequenceNumber The notification sequence number within the source object.
     * @param timeStamp The notification emission date.
     * @param msg The notification message.
     * @param obsObj The object observed by the producer of this notification.
     * @param obsAtt The attribute observed by the producer of this notification.
     * @param derGauge The derived gauge.
     * @param trigger The threshold/string (depending on the monitor type) that triggered the notification.
     */
    MonitorNotification(String type, Object source, long sequenceNumber, long timeStamp, String msg,
                               ObjectName obsObj, String obsAtt, Object derGauge, Object trigger) {

        super(type, source, sequenceNumber, timeStamp, msg);
        this.observedObject = obsObj;
        this.observedAttribute = obsAtt;
        this.derivedGauge = derGauge;
        this.trigger = trigger;
    }

    /*
     * ------------------------------------------
     *  PUBLIC METHODS
     * ------------------------------------------
     */

    // GETTERS AND SETTERS
    //--------------------

    /**
     * Gets the observed object of this monitor notification.
     *
     * @return The observed object.
     */
    public ObjectName getObservedObject() {
        return observedObject;
    }

    /**
     * Gets the observed attribute of this monitor notification.
     *
     * @return The observed attribute.
     */
    public String getObservedAttribute() {
        return observedAttribute;
    }

    /**
     * Gets the derived gauge of this monitor notification.
     *
     * @return The derived gauge.
     */
    public Object getDerivedGauge() {
        return derivedGauge;
    }

    /**
     * Gets the threshold/string (depending on the monitor type) that triggered off this monitor notification.
     *
     * @return The trigger.
     */
    public Object getTrigger() {
        return trigger;
    }

}
