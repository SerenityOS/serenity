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
import javax.management.MBeanNotificationInfo;
import javax.management.ObjectName;
import static javax.management.monitor.Monitor.NumericalType.*;
import static javax.management.monitor.MonitorNotification.*;

/**
 * Defines a monitor MBean designed to observe the values of a gauge attribute.
 *
 * <P> A gauge monitor observes an attribute that is continuously
 * variable with time. A gauge monitor sends notifications as
 * follows:
 *
 * <UL>
 *
 * <LI> if the attribute value is increasing and becomes equal to or
 * greater than the high threshold value, a {@link
 * MonitorNotification#THRESHOLD_HIGH_VALUE_EXCEEDED threshold high
 * notification} is sent. The notify high flag must be set to
 * <CODE>true</CODE>.
 *
 * <BR>Subsequent crossings of the high threshold value do not cause
 * further notifications unless the attribute value becomes equal to
 * or less than the low threshold value.</LI>
 *
 * <LI> if the attribute value is decreasing and becomes equal to or
 * less than the low threshold value, a {@link
 * MonitorNotification#THRESHOLD_LOW_VALUE_EXCEEDED threshold low
 * notification} is sent. The notify low flag must be set to
 * <CODE>true</CODE>.
 *
 * <BR>Subsequent crossings of the low threshold value do not cause
 * further notifications unless the attribute value becomes equal to
 * or greater than the high threshold value.</LI>
 *
 * </UL>
 *
 * This provides a hysteresis mechanism to avoid repeated triggering
 * of notifications when the attribute value makes small oscillations
 * around the high or low threshold value.
 *
 * <P> If the gauge difference mode is used, the value of the derived
 * gauge is calculated as the difference between the observed gauge
 * values for two successive observations.
 *
 * <BR>The derived gauge value (V[t]) is calculated using the following method:
 * <UL>
 * <LI>V[t] = gauge[t] - gauge[t-GP]</LI>
 * </UL>
 *
 * This implementation of the gauge monitor requires the observed
 * attribute to be of the type integer or floating-point
 * (<CODE>Byte</CODE>, <CODE>Integer</CODE>, <CODE>Short</CODE>,
 * <CODE>Long</CODE>, <CODE>Float</CODE>, <CODE>Double</CODE>).
 *
 *
 * @since 1.5
 */
public class GaugeMonitor extends Monitor implements GaugeMonitorMBean {

    /*
     * ------------------------------------------
     *  PACKAGE CLASSES
     * ------------------------------------------
     */

    static class GaugeMonitorObservedObject extends ObservedObject {

        public GaugeMonitorObservedObject(ObjectName observedObject) {
            super(observedObject);
        }

        public final synchronized boolean getDerivedGaugeValid() {
            return derivedGaugeValid;
        }
        public final synchronized void setDerivedGaugeValid(
                                                 boolean derivedGaugeValid) {
            this.derivedGaugeValid = derivedGaugeValid;
        }
        public final synchronized NumericalType getType() {
            return type;
        }
        public final synchronized void setType(NumericalType type) {
            this.type = type;
        }
        public final synchronized Number getPreviousScanGauge() {
            return previousScanGauge;
        }
        public final synchronized void setPreviousScanGauge(
                                                  Number previousScanGauge) {
            this.previousScanGauge = previousScanGauge;
        }
        public final synchronized int getStatus() {
            return status;
        }
        public final synchronized void setStatus(int status) {
            this.status = status;
        }

        private boolean derivedGaugeValid;
        private NumericalType type;
        private Number previousScanGauge;
        private int status;
    }

    /*
     * ------------------------------------------
     *  PRIVATE VARIABLES
     * ------------------------------------------
     */

    /**
     * Gauge high threshold.
     *
     * <BR>The default value is a null Integer object.
     */
    private Number highThreshold = INTEGER_ZERO;

    /**
     * Gauge low threshold.
     *
     * <BR>The default value is a null Integer object.
     */
    private Number lowThreshold = INTEGER_ZERO;

    /**
     * Flag indicating if the gauge monitor notifies when exceeding
     * the high threshold.
     *
     * <BR>The default value is <CODE>false</CODE>.
     */
    private boolean notifyHigh = false;

    /**
     * Flag indicating if the gauge monitor notifies when exceeding
     * the low threshold.
     *
     * <BR>The default value is <CODE>false</CODE>.
     */
    private boolean notifyLow = false;

    /**
     * Flag indicating if the gauge difference mode is used.  If the
     * gauge difference mode is used, the derived gauge is the
     * difference between two consecutive observed values.  Otherwise,
     * the derived gauge is directly the value of the observed
     * attribute.
     *
     * <BR>The default value is set to <CODE>false</CODE>.
     */
    private boolean differenceMode = false;

    private static final String[] types = {
        RUNTIME_ERROR,
        OBSERVED_OBJECT_ERROR,
        OBSERVED_ATTRIBUTE_ERROR,
        OBSERVED_ATTRIBUTE_TYPE_ERROR,
        THRESHOLD_ERROR,
        THRESHOLD_HIGH_VALUE_EXCEEDED,
        THRESHOLD_LOW_VALUE_EXCEEDED
    };

    private static final MBeanNotificationInfo[] notifsInfo = {
        new MBeanNotificationInfo(
            types,
            "javax.management.monitor.MonitorNotification",
            "Notifications sent by the GaugeMonitor MBean")
    };

    // Flags needed to implement the hysteresis mechanism.
    //
    private static final int RISING             = 0;
    private static final int FALLING            = 1;
    private static final int RISING_OR_FALLING  = 2;

    /*
     * ------------------------------------------
     *  CONSTRUCTORS
     * ------------------------------------------
     */

    /**
     * Default constructor.
     */
    public GaugeMonitor() {
    }

    /*
     * ------------------------------------------
     *  PUBLIC METHODS
     * ------------------------------------------
     */

    /**
     * Starts the gauge monitor.
     */
    public synchronized void start() {
        if (isActive()) {
            MONITOR_LOGGER.log(Level.TRACE, "the monitor is already active");
            return;
        }
        // Reset values.
        //
        for (ObservedObject o : observedObjects) {
            final GaugeMonitorObservedObject gmo =
                (GaugeMonitorObservedObject) o;
            gmo.setStatus(RISING_OR_FALLING);
            gmo.setPreviousScanGauge(null);
        }
        doStart();
    }

    /**
     * Stops the gauge monitor.
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
     * @param object the name of the MBean.
     *
     * @return The derived gauge of the specified object.
     *
     */
    @Override
    public synchronized Number getDerivedGauge(ObjectName object) {
        return (Number) super.getDerivedGauge(object);
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
    public synchronized Number getDerivedGauge() {
        if (observedObjects.isEmpty()) {
            return null;
        } else {
            return (Number) observedObjects.get(0).getDerivedGauge();
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
     * Gets the high threshold value common to all observed MBeans.
     *
     * @return The high threshold value.
     *
     * @see #setThresholds
     */
    public synchronized Number getHighThreshold() {
        return highThreshold;
    }

    /**
     * Gets the low threshold value common to all observed MBeans.
     *
     * @return The low threshold value.
     *
     * @see #setThresholds
     */
    public synchronized Number getLowThreshold() {
        return lowThreshold;
    }

    /**
     * Sets the high and the low threshold values common to all
     * observed MBeans.
     *
     * @param highValue The high threshold value.
     * @param lowValue The low threshold value.
     *
     * @exception IllegalArgumentException The specified high/low
     * threshold is null or the low threshold is greater than the high
     * threshold or the high threshold and the low threshold are not
     * of the same type.
     *
     * @see #getHighThreshold
     * @see #getLowThreshold
     */
    public synchronized void setThresholds(Number highValue, Number lowValue)
        throws IllegalArgumentException {

        if ((highValue == null) || (lowValue == null)) {
            throw new IllegalArgumentException("Null threshold value");
        }

        if (highValue.getClass() != lowValue.getClass()) {
            throw new IllegalArgumentException("Different type " +
                                               "threshold values");
        }

        if (isFirstStrictlyGreaterThanLast(lowValue, highValue,
                                           highValue.getClass().getName())) {
            throw new IllegalArgumentException("High threshold less than " +
                                               "low threshold");
        }

        if (highThreshold.equals(highValue) && lowThreshold.equals(lowValue))
            return;
        highThreshold = highValue;
        lowThreshold = lowValue;

        // Reset values.
        //
        int index = 0;
        for (ObservedObject o : observedObjects) {
            resetAlreadyNotified(o, index++, THRESHOLD_ERROR_NOTIFIED);
            final GaugeMonitorObservedObject gmo =
                (GaugeMonitorObservedObject) o;
            gmo.setStatus(RISING_OR_FALLING);
        }
    }

    /**
     * Gets the high notification's on/off switch value common to all
     * observed MBeans.
     *
     * @return <CODE>true</CODE> if the gauge monitor notifies when
     * exceeding the high threshold, <CODE>false</CODE> otherwise.
     *
     * @see #setNotifyHigh
     */
    public synchronized boolean getNotifyHigh() {
        return notifyHigh;
    }

    /**
     * Sets the high notification's on/off switch value common to all
     * observed MBeans.
     *
     * @param value The high notification's on/off switch value.
     *
     * @see #getNotifyHigh
     */
    public synchronized void setNotifyHigh(boolean value) {
        if (notifyHigh == value)
            return;
        notifyHigh = value;
    }

    /**
     * Gets the low notification's on/off switch value common to all
     * observed MBeans.
     *
     * @return <CODE>true</CODE> if the gauge monitor notifies when
     * exceeding the low threshold, <CODE>false</CODE> otherwise.
     *
     * @see #setNotifyLow
     */
    public synchronized boolean getNotifyLow() {
        return notifyLow;
    }

    /**
     * Sets the low notification's on/off switch value common to all
     * observed MBeans.
     *
     * @param value The low notification's on/off switch value.
     *
     * @see #getNotifyLow
     */
    public synchronized void setNotifyLow(boolean value) {
        if (notifyLow == value)
            return;
        notifyLow = value;
    }

    /**
     * Gets the difference mode flag value common to all observed MBeans.
     *
     * @return <CODE>true</CODE> if the difference mode is used,
     * <CODE>false</CODE> otherwise.
     *
     * @see #setDifferenceMode
     */
    public synchronized boolean getDifferenceMode() {
        return differenceMode;
    }

    /**
     * Sets the difference mode flag value common to all observed MBeans.
     *
     * @param value The difference mode flag value.
     *
     * @see #getDifferenceMode
     */
    public synchronized void setDifferenceMode(boolean value) {
        if (differenceMode == value)
            return;
        differenceMode = value;

        // Reset values.
        //
        for (ObservedObject o : observedObjects) {
            final GaugeMonitorObservedObject gmo =
                (GaugeMonitorObservedObject) o;
            gmo.setStatus(RISING_OR_FALLING);
            gmo.setPreviousScanGauge(null);
        }
    }

   /**
     * Returns a <CODE>NotificationInfo</CODE> object containing the
     * name of the Java class of the notification and the notification
     * types sent by the gauge monitor.
     */
    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {
        return notifsInfo.clone();
    }

    /*
     * ------------------------------------------
     *  PRIVATE METHODS
     * ------------------------------------------
     */

    /**
     * Updates the derived gauge attribute of the observed object.
     *
     * @param scanGauge The value of the observed attribute.
     * @param o The observed object.
     * @return <CODE>true</CODE> if the derived gauge value is valid,
     * <CODE>false</CODE> otherwise.  The derived gauge value is
     * invalid when the differenceMode flag is set to
     * <CODE>true</CODE> and it is the first notification (so we
     * haven't 2 consecutive values to update the derived gauge).
     */
    private synchronized boolean updateDerivedGauge(
        Object scanGauge, GaugeMonitorObservedObject o) {

        boolean is_derived_gauge_valid;

        // The gauge difference mode is used.
        //
        if (differenceMode) {

            // The previous scan gauge has been initialized.
            //
            if (o.getPreviousScanGauge() != null) {
                setDerivedGaugeWithDifference((Number)scanGauge, o);
                is_derived_gauge_valid = true;
            }
            // The previous scan gauge has not been initialized.
            // We cannot update the derived gauge...
            //
            else {
                is_derived_gauge_valid = false;
            }
            o.setPreviousScanGauge((Number)scanGauge);
        }
        // The gauge difference mode is not used.
        //
        else {
            o.setDerivedGauge((Number)scanGauge);
            is_derived_gauge_valid = true;
        }

        return is_derived_gauge_valid;
    }

    /**
     * Updates the notification attribute of the observed object
     * and notifies the listeners only once if the notify flag
     * is set to <CODE>true</CODE>.
     * @param o The observed object.
     */
    private synchronized MonitorNotification updateNotifications(
        GaugeMonitorObservedObject o) {

        MonitorNotification n = null;

        // Send high notification if notifyHigh is true.
        // Send low notification if notifyLow is true.
        //
        if (o.getStatus() == RISING_OR_FALLING) {
            if (isFirstGreaterThanLast((Number)o.getDerivedGauge(),
                                       highThreshold,
                                       o.getType())) {
                if (notifyHigh) {
                    n = new MonitorNotification(
                            THRESHOLD_HIGH_VALUE_EXCEEDED,
                            this,
                            0,
                            0,
                            "",
                            null,
                            null,
                            null,
                            highThreshold);
                }
                o.setStatus(FALLING);
            } else if (isFirstGreaterThanLast(lowThreshold,
                                              (Number)o.getDerivedGauge(),
                                              o.getType())) {
                if (notifyLow) {
                    n = new MonitorNotification(
                            THRESHOLD_LOW_VALUE_EXCEEDED,
                            this,
                            0,
                            0,
                            "",
                            null,
                            null,
                            null,
                            lowThreshold);
                }
                o.setStatus(RISING);
            }
        } else {
            if (o.getStatus() == RISING) {
                if (isFirstGreaterThanLast((Number)o.getDerivedGauge(),
                                           highThreshold,
                                           o.getType())) {
                    if (notifyHigh) {
                        n = new MonitorNotification(
                                THRESHOLD_HIGH_VALUE_EXCEEDED,
                                this,
                                0,
                                0,
                                "",
                                null,
                                null,
                                null,
                                highThreshold);
                    }
                    o.setStatus(FALLING);
                }
            } else if (o.getStatus() == FALLING) {
                if (isFirstGreaterThanLast(lowThreshold,
                                           (Number)o.getDerivedGauge(),
                                           o.getType())) {
                    if (notifyLow) {
                        n = new MonitorNotification(
                                THRESHOLD_LOW_VALUE_EXCEEDED,
                                this,
                                0,
                                0,
                                "",
                                null,
                                null,
                                null,
                                lowThreshold);
                    }
                    o.setStatus(RISING);
                }
            }
        }

        return n;
    }

    /**
     * Sets the derived gauge when the differenceMode flag is set to
     * <CODE>true</CODE>.  Both integer and floating-point types are
     * allowed.
     *
     * @param scanGauge The value of the observed attribute.
     * @param o The observed object.
     */
    private synchronized void setDerivedGaugeWithDifference(
        Number scanGauge, GaugeMonitorObservedObject o) {
        Number prev = o.getPreviousScanGauge();
        Number der;
        switch (o.getType()) {
        case INTEGER:
            der = Integer.valueOf(((Integer)scanGauge).intValue() -
                                  ((Integer)prev).intValue());
            break;
        case BYTE:
            der = Byte.valueOf((byte)(((Byte)scanGauge).byteValue() -
                                      ((Byte)prev).byteValue()));
            break;
        case SHORT:
            der = Short.valueOf((short)(((Short)scanGauge).shortValue() -
                                        ((Short)prev).shortValue()));
            break;
        case LONG:
            der = Long.valueOf(((Long)scanGauge).longValue() -
                               ((Long)prev).longValue());
            break;
        case FLOAT:
            der = Float.valueOf(((Float)scanGauge).floatValue() -
                                ((Float)prev).floatValue());
            break;
        case DOUBLE:
            der = Double.valueOf(((Double)scanGauge).doubleValue() -
                                 ((Double)prev).doubleValue());
            break;
        default:
            // Should never occur...
            MONITOR_LOGGER.log(Level.TRACE,
                    "the threshold type is invalid");
            return;
        }
        o.setDerivedGauge(der);
    }

    /**
     * Tests if the first specified Number is greater than or equal to
     * the last.  Both integer and floating-point types are allowed.
     *
     * @param greater The first Number to compare with the second.
     * @param less The second Number to compare with the first.
     * @param type The number type.
     * @return <CODE>true</CODE> if the first specified Number is
     * greater than or equal to the last, <CODE>false</CODE>
     * otherwise.
     */
    private boolean isFirstGreaterThanLast(Number greater,
                                           Number less,
                                           NumericalType type) {

        switch (type) {
        case INTEGER:
        case BYTE:
        case SHORT:
        case LONG:
            return (greater.longValue() >= less.longValue());
        case FLOAT:
        case DOUBLE:
            return (greater.doubleValue() >= less.doubleValue());
        default:
            // Should never occur...
            MONITOR_LOGGER.log(Level.TRACE,
                    "the threshold type is invalid");
            return false;
        }
    }

    /**
     * Tests if the first specified Number is strictly greater than the last.
     * Both integer and floating-point types are allowed.
     *
     * @param greater The first Number to compare with the second.
     * @param less The second Number to compare with the first.
     * @param className The number class name.
     * @return <CODE>true</CODE> if the first specified Number is
     * strictly greater than the last, <CODE>false</CODE> otherwise.
     */
    private boolean isFirstStrictlyGreaterThanLast(Number greater,
                                                   Number less,
                                                   String className) {

        if (className.equals("java.lang.Integer") ||
            className.equals("java.lang.Byte") ||
            className.equals("java.lang.Short") ||
            className.equals("java.lang.Long")) {

            return (greater.longValue() > less.longValue());
        }
        else if (className.equals("java.lang.Float") ||
                 className.equals("java.lang.Double")) {

            return (greater.doubleValue() > less.doubleValue());
        }
        else {
            // Should never occur...
            MONITOR_LOGGER.log(Level.TRACE,
                    "the threshold type is invalid");
            return false;
        }
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
        final GaugeMonitorObservedObject gmo =
            new GaugeMonitorObservedObject(object);
        gmo.setStatus(RISING_OR_FALLING);
        gmo.setPreviousScanGauge(null);
        return gmo;
    }

    /**
     * This method globally sets the derived gauge type for the given
     * "object" and "attribute" after checking that the type of the
     * supplied observed attribute value is one of the value types
     * supported by this monitor.
     */
    @Override
    synchronized boolean isComparableTypeValid(ObjectName object,
                                               String attribute,
                                               Comparable<?> value) {
        final GaugeMonitorObservedObject o =
            (GaugeMonitorObservedObject) getObservedObject(object);
        if (o == null)
            return false;

        // Check that the observed attribute is either of type
        // "Integer" or "Float".
        //
        if (value instanceof Integer) {
            o.setType(INTEGER);
        } else if (value instanceof Byte) {
            o.setType(BYTE);
        } else if (value instanceof Short) {
            o.setType(SHORT);
        } else if (value instanceof Long) {
            o.setType(LONG);
        } else if (value instanceof Float) {
            o.setType(FLOAT);
        } else if (value instanceof Double) {
            o.setType(DOUBLE);
        } else {
            return false;
        }
        return true;
    }

    @Override
    synchronized Comparable<?> getDerivedGaugeFromComparable(
                                                  ObjectName object,
                                                  String attribute,
                                                  Comparable<?> value) {
        final GaugeMonitorObservedObject o =
            (GaugeMonitorObservedObject) getObservedObject(object);
        if (o == null)
            return null;

        // Update the derived gauge attributes and check the
        // validity of the new value. The derived gauge value
        // is invalid when the differenceMode flag is set to
        // true and it is the first notification, i.e. we
        // haven't got 2 consecutive values to update the
        // derived gauge.
        //
        o.setDerivedGaugeValid(updateDerivedGauge(value, o));

        return (Comparable<?>) o.getDerivedGauge();
    }

    @Override
    synchronized void onErrorNotification(MonitorNotification notification) {
        final GaugeMonitorObservedObject o = (GaugeMonitorObservedObject)
            getObservedObject(notification.getObservedObject());
        if (o == null)
            return;

        // Reset values.
        //
        o.setStatus(RISING_OR_FALLING);
        o.setPreviousScanGauge(null);
    }

    @Override
    synchronized MonitorNotification buildAlarmNotification(
                                               ObjectName object,
                                               String attribute,
                                               Comparable<?> value) {
        final GaugeMonitorObservedObject o =
            (GaugeMonitorObservedObject) getObservedObject(object);
        if (o == null)
            return null;

        // Notify the listeners if the updated derived
        // gauge value is valid.
        //
        final MonitorNotification alarm;
        if (o.getDerivedGaugeValid())
            alarm = updateNotifications(o);
        else
            alarm = null;
        return alarm;
    }

    /**
     * Tests if the threshold high and threshold low are both of the
     * same type as the gauge.  Both integer and floating-point types
     * are allowed.
     *
     * Note:
     *   If the optional lowThreshold or highThreshold have not been
     *   initialized, their default value is an Integer object with
     *   a value equal to zero.
     *
     * @param object The observed object.
     * @param attribute The observed attribute.
     * @param value The sample value.
     * @return <CODE>true</CODE> if type is the same,
     * <CODE>false</CODE> otherwise.
     */
    @Override
    synchronized boolean isThresholdTypeValid(ObjectName object,
                                              String attribute,
                                              Comparable<?> value) {
        final GaugeMonitorObservedObject o =
            (GaugeMonitorObservedObject) getObservedObject(object);
        if (o == null)
            return false;

        Class<? extends Number> c = classForType(o.getType());
        return (isValidForType(highThreshold, c) &&
                isValidForType(lowThreshold, c));
    }
}
