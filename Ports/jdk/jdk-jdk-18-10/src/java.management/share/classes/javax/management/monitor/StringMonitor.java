/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.jmx.defaults.JmxProperties.MONITOR_LOGGER;
import java.lang.System.Logger.Level;
import javax.management.ObjectName;
import javax.management.MBeanNotificationInfo;
import static javax.management.monitor.MonitorNotification.*;

/**
 * Defines a monitor MBean designed to observe the values of a string
 * attribute.
 * <P>
 * A string monitor sends notifications as follows:
 * <UL>
 * <LI> if the attribute value matches the string to compare value,
 *      a {@link MonitorNotification#STRING_TO_COMPARE_VALUE_MATCHED
 *      match notification} is sent.
 *      The notify match flag must be set to <CODE>true</CODE>.
 *      <BR>Subsequent matchings of the string to compare values do not
 *      cause further notifications unless
 *      the attribute value differs from the string to compare value.
 * <LI> if the attribute value differs from the string to compare value,
 *      a {@link MonitorNotification#STRING_TO_COMPARE_VALUE_DIFFERED
 *      differ notification} is sent.
 *      The notify differ flag must be set to <CODE>true</CODE>.
 *      <BR>Subsequent differences from the string to compare value do
 *      not cause further notifications unless
 *      the attribute value matches the string to compare value.
 * </UL>
 *
 *
 * @since 1.5
 */
public class StringMonitor extends Monitor implements StringMonitorMBean {

    /*
     * ------------------------------------------
     *  PACKAGE CLASSES
     * ------------------------------------------
     */

    static class StringMonitorObservedObject extends ObservedObject {

        public StringMonitorObservedObject(ObjectName observedObject) {
            super(observedObject);
        }

        public final synchronized int getStatus() {
            return status;
        }
        public final synchronized void setStatus(int status) {
            this.status = status;
        }

        private int status;
    }

    /*
     * ------------------------------------------
     *  PRIVATE VARIABLES
     * ------------------------------------------
     */

    /**
     * String to compare with the observed attribute.
     * <BR>The default value is an empty character sequence.
     */
    private String stringToCompare = "";

    /**
     * Flag indicating if the string monitor notifies when matching
     * the string to compare.
     * <BR>The default value is set to <CODE>false</CODE>.
     */
    private boolean notifyMatch = false;

    /**
     * Flag indicating if the string monitor notifies when differing
     * from the string to compare.
     * <BR>The default value is set to <CODE>false</CODE>.
     */
    private boolean notifyDiffer = false;

    private static final String[] types = {
        RUNTIME_ERROR,
        OBSERVED_OBJECT_ERROR,
        OBSERVED_ATTRIBUTE_ERROR,
        OBSERVED_ATTRIBUTE_TYPE_ERROR,
        STRING_TO_COMPARE_VALUE_MATCHED,
        STRING_TO_COMPARE_VALUE_DIFFERED
    };

    private static final MBeanNotificationInfo[] notifsInfo = {
        new MBeanNotificationInfo(
            types,
            "javax.management.monitor.MonitorNotification",
            "Notifications sent by the StringMonitor MBean")
    };

    // Flags needed to implement the matching/differing mechanism.
    //
    private static final int MATCHING                   = 0;
    private static final int DIFFERING                  = 1;
    private static final int MATCHING_OR_DIFFERING      = 2;

    /*
     * ------------------------------------------
     *  CONSTRUCTORS
     * ------------------------------------------
     */

    /**
     * Default constructor.
     */
    public StringMonitor() {
    }

    /*
     * ------------------------------------------
     *  PUBLIC METHODS
     * ------------------------------------------
     */

    /**
     * Starts the string monitor.
     */
    public synchronized void start() {
        if (isActive()) {
            MONITOR_LOGGER.log(Level.TRACE, "the monitor is already active");
            return;
        }
        // Reset values.
        //
        for (ObservedObject o : observedObjects) {
            final StringMonitorObservedObject smo =
                (StringMonitorObservedObject) o;
            smo.setStatus(MATCHING_OR_DIFFERING);
        }
        doStart();
    }

    /**
     * Stops the string monitor.
     */
    public synchronized void stop() {
        doStop();
    }

    // GETTERS AND SETTERS
    //--------------------

    /**
     * Gets the derived gauge of the specified object, if this object is
     * contained in the set of observed MBeans, or <code>null</code> otherwise.
     *
     * @param object the name of the MBean whose derived gauge is required.
     *
     * @return The derived gauge of the specified object.
     *
     */
    @Override
    public synchronized String getDerivedGauge(ObjectName object) {
        return (String) super.getDerivedGauge(object);
    }

    /**
     * Gets the derived gauge timestamp of the specified object, if
     * this object is contained in the set of observed MBeans, or
     * <code>0</code> otherwise.
     *
     * @param object the name of the object whose derived gauge
     * timestamp is to be returned.
     *
     * @return The derived gauge timestamp of the specified object.
     *
     */
    @Override
    public synchronized long getDerivedGaugeTimeStamp(ObjectName object) {
        return super.getDerivedGaugeTimeStamp(object);
    }

    /**
     * Returns the derived gauge of the first object in the set of
     * observed MBeans.
     *
     * @return The derived gauge.
     *
     * @deprecated As of JMX 1.2, replaced by
     * {@link #getDerivedGauge(ObjectName)}
     */
    @Deprecated
    public synchronized String getDerivedGauge() {
        if (observedObjects.isEmpty()) {
            return null;
        } else {
            return (String) observedObjects.get(0).getDerivedGauge();
        }
    }

    /**
     * Gets the derived gauge timestamp of the first object in the set
     * of observed MBeans.
     *
     * @return The derived gauge timestamp.
     *
     * @deprecated As of JMX 1.2, replaced by
     * {@link #getDerivedGaugeTimeStamp(ObjectName)}
     */
    @Deprecated
    public synchronized long getDerivedGaugeTimeStamp() {
        if (observedObjects.isEmpty()) {
            return 0;
        } else {
            return observedObjects.get(0).getDerivedGaugeTimeStamp();
        }
    }

    /**
     * Gets the string to compare with the observed attribute common
     * to all observed MBeans.
     *
     * @return The string value.
     *
     * @see #setStringToCompare
     */
    public synchronized String getStringToCompare() {
        return stringToCompare;
    }

    /**
     * Sets the string to compare with the observed attribute common
     * to all observed MBeans.
     *
     * @param value The string value.
     *
     * @exception IllegalArgumentException The specified
     * string to compare is null.
     *
     * @see #getStringToCompare
     */
    public synchronized void setStringToCompare(String value)
        throws IllegalArgumentException {

        if (value == null) {
            throw new IllegalArgumentException("Null string to compare");
        }

        if (stringToCompare.equals(value))
            return;
        stringToCompare = value;

        // Reset values.
        //
        for (ObservedObject o : observedObjects) {
            final StringMonitorObservedObject smo =
                (StringMonitorObservedObject) o;
            smo.setStatus(MATCHING_OR_DIFFERING);
        }
    }

    /**
     * Gets the matching notification's on/off switch value common to
     * all observed MBeans.
     *
     * @return <CODE>true</CODE> if the string monitor notifies when
     * matching the string to compare, <CODE>false</CODE> otherwise.
     *
     * @see #setNotifyMatch
     */
    public synchronized boolean getNotifyMatch() {
        return notifyMatch;
    }

    /**
     * Sets the matching notification's on/off switch value common to
     * all observed MBeans.
     *
     * @param value The matching notification's on/off switch value.
     *
     * @see #getNotifyMatch
     */
    public synchronized void setNotifyMatch(boolean value) {
        if (notifyMatch == value)
            return;
        notifyMatch = value;
    }

    /**
     * Gets the differing notification's on/off switch value common to
     * all observed MBeans.
     *
     * @return <CODE>true</CODE> if the string monitor notifies when
     * differing from the string to compare, <CODE>false</CODE> otherwise.
     *
     * @see #setNotifyDiffer
     */
    public synchronized boolean getNotifyDiffer() {
        return notifyDiffer;
    }

    /**
     * Sets the differing notification's on/off switch value common to
     * all observed MBeans.
     *
     * @param value The differing notification's on/off switch value.
     *
     * @see #getNotifyDiffer
     */
    public synchronized void setNotifyDiffer(boolean value) {
        if (notifyDiffer == value)
            return;
        notifyDiffer = value;
    }

    /**
     * Returns a <CODE>NotificationInfo</CODE> object containing the name of
     * the Java class of the notification and the notification types sent by
     * the string monitor.
     */
    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return notifsInfo.clone();
    }

    /*
     * ------------------------------------------
     *  PACKAGE METHODS
     * ------------------------------------------
     */

    /**
     * Factory method for ObservedObject creation.
     *
     * @since 1.6
     */
    @Override
    ObservedObject createObservedObject(ObjectName object) {
        final StringMonitorObservedObject smo =
            new StringMonitorObservedObject(object);
        smo.setStatus(MATCHING_OR_DIFFERING);
        return smo;
    }

    /**
     * Check that the type of the supplied observed attribute
     * value is one of the value types supported by this monitor.
     */
    @Override
    synchronized boolean isComparableTypeValid(ObjectName object,
                                               String attribute,
                                               Comparable<?> value) {
        // Check that the observed attribute is of type "String".
        //
        if (value instanceof String) {
            return true;
        }
        return false;
    }

    @Override
    synchronized void onErrorNotification(MonitorNotification notification) {
        final StringMonitorObservedObject o = (StringMonitorObservedObject)
            getObservedObject(notification.getObservedObject());
        if (o == null)
            return;

        // Reset values.
        //
        o.setStatus(MATCHING_OR_DIFFERING);
    }

    @Override
    synchronized MonitorNotification buildAlarmNotification(
                                               ObjectName object,
                                               String attribute,
                                               Comparable<?> value) {
        String type = null;
        String msg = null;
        Object trigger = null;

        final StringMonitorObservedObject o =
            (StringMonitorObservedObject) getObservedObject(object);
        if (o == null)
            return null;

        // Send matching notification if notifyMatch is true.
        // Send differing notification if notifyDiffer is true.
        //
        if (o.getStatus() == MATCHING_OR_DIFFERING) {
            if (o.getDerivedGauge().equals(stringToCompare)) {
                if (notifyMatch) {
                    type = STRING_TO_COMPARE_VALUE_MATCHED;
                    msg = "";
                    trigger = stringToCompare;
                }
                o.setStatus(DIFFERING);
            } else {
                if (notifyDiffer) {
                    type = STRING_TO_COMPARE_VALUE_DIFFERED;
                    msg = "";
                    trigger = stringToCompare;
                }
                o.setStatus(MATCHING);
            }
        } else {
            if (o.getStatus() == MATCHING) {
                if (o.getDerivedGauge().equals(stringToCompare)) {
                    if (notifyMatch) {
                        type = STRING_TO_COMPARE_VALUE_MATCHED;
                        msg = "";
                        trigger = stringToCompare;
                    }
                    o.setStatus(DIFFERING);
                }
            } else if (o.getStatus() == DIFFERING) {
                if (!o.getDerivedGauge().equals(stringToCompare)) {
                    if (notifyDiffer) {
                        type = STRING_TO_COMPARE_VALUE_DIFFERED;
                        msg = "";
                        trigger = stringToCompare;
                    }
                    o.setStatus(MATCHING);
                }
            }
        }

        return new MonitorNotification(type,
                                       this,
                                       0,
                                       0,
                                       msg,
                                       null,
                                       null,
                                       null,
                                       trigger);
    }
}
