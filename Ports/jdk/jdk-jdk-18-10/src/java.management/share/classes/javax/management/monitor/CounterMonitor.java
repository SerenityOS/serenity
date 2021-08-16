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
import static javax.management.monitor.Monitor.NumericalType.*;
import static javax.management.monitor.MonitorNotification.*;

/**
 * Defines a monitor MBean designed to observe the values of a counter
 * attribute.
 *
 * <P> A counter monitor sends a {@link
 * MonitorNotification#THRESHOLD_VALUE_EXCEEDED threshold
 * notification} when the value of the counter reaches or exceeds a
 * threshold known as the comparison level.  The notify flag must be
 * set to <CODE>true</CODE>.
 *
 * <P> In addition, an offset mechanism enables particular counting
 * intervals to be detected.  If the offset value is not zero,
 * whenever the threshold is triggered by the counter value reaching a
 * comparison level, that comparison level is incremented by the
 * offset value.  This is regarded as taking place instantaneously,
 * that is, before the count is incremented.  Thus, for each level,
 * the threshold triggers an event notification every time the count
 * increases by an interval equal to the offset value.
 *
 * <P> If the counter can wrap around its maximum value, the modulus
 * needs to be specified.  The modulus is the value at which the
 * counter is reset to zero.
 *
 * <P> If the counter difference mode is used, the value of the
 * derived gauge is calculated as the difference between the observed
 * counter values for two successive observations.  If this difference
 * is negative, the value of the derived gauge is incremented by the
 * value of the modulus.  The derived gauge value (V[t]) is calculated
 * using the following method:
 *
 * <UL>
 * <LI>if (counter[t] - counter[t-GP]) is positive then
 * V[t] = counter[t] - counter[t-GP]
 * <LI>if (counter[t] - counter[t-GP]) is negative then
 * V[t] = counter[t] - counter[t-GP] + MODULUS
 * </UL>
 *
 * This implementation of the counter monitor requires the observed
 * attribute to be of the type integer (<CODE>Byte</CODE>,
 * <CODE>Integer</CODE>, <CODE>Short</CODE>, <CODE>Long</CODE>).
 *
 *
 * @since 1.5
 */
public class CounterMonitor extends Monitor implements CounterMonitorMBean {

    /*
     * ------------------------------------------
     *  PACKAGE CLASSES
     * ------------------------------------------
     */

    static class CounterMonitorObservedObject extends ObservedObject {

        public CounterMonitorObservedObject(ObjectName observedObject) {
            super(observedObject);
        }

        public final synchronized Number getThreshold() {
            return threshold;
        }
        public final synchronized void setThreshold(Number threshold) {
            this.threshold = threshold;
        }
        public final synchronized Number getPreviousScanCounter() {
            return previousScanCounter;
        }
        public final synchronized void setPreviousScanCounter(
                                                  Number previousScanCounter) {
            this.previousScanCounter = previousScanCounter;
        }
        public final synchronized boolean getModulusExceeded() {
            return modulusExceeded;
        }
        public final synchronized void setModulusExceeded(
                                                 boolean modulusExceeded) {
            this.modulusExceeded = modulusExceeded;
        }
        public final synchronized Number getDerivedGaugeExceeded() {
            return derivedGaugeExceeded;
        }
        public final synchronized void setDerivedGaugeExceeded(
                                                 Number derivedGaugeExceeded) {
            this.derivedGaugeExceeded = derivedGaugeExceeded;
        }
        public final synchronized boolean getDerivedGaugeValid() {
            return derivedGaugeValid;
        }
        public final synchronized void setDerivedGaugeValid(
                                                 boolean derivedGaugeValid) {
            this.derivedGaugeValid = derivedGaugeValid;
        }
        public final synchronized boolean getEventAlreadyNotified() {
            return eventAlreadyNotified;
        }
        public final synchronized void setEventAlreadyNotified(
                                               boolean eventAlreadyNotified) {
            this.eventAlreadyNotified = eventAlreadyNotified;
        }
        public final synchronized NumericalType getType() {
            return type;
        }
        public final synchronized void setType(NumericalType type) {
            this.type = type;
        }

        private Number threshold;
        private Number previousScanCounter;
        private boolean modulusExceeded;
        private Number derivedGaugeExceeded;
        private boolean derivedGaugeValid;
        private boolean eventAlreadyNotified;
        private NumericalType type;
    }

    /*
     * ------------------------------------------
     *  PRIVATE VARIABLES
     * ------------------------------------------
     */

    /**
     * Counter modulus.
     * <BR>The default value is a null Integer object.
     */
    private Number modulus = INTEGER_ZERO;

    /**
     * Counter offset.
     * <BR>The default value is a null Integer object.
     */
    private Number offset = INTEGER_ZERO;

    /**
     * Flag indicating if the counter monitor notifies when exceeding
     * the threshold.  The default value is set to
     * <CODE>false</CODE>.
     */
    private boolean notify = false;

    /**
     * Flag indicating if the counter difference mode is used.  If the
     * counter difference mode is used, the derived gauge is the
     * difference between two consecutive observed values.  Otherwise,
     * the derived gauge is directly the value of the observed
     * attribute.  The default value is set to <CODE>false</CODE>.
     */
    private boolean differenceMode = false;

    /**
     * Initial counter threshold.  This value is used to initialize
     * the threshold when a new object is added to the list and reset
     * the threshold to its initial value each time the counter
     * resets.
     */
    private Number initThreshold = INTEGER_ZERO;

    private static final String[] types = {
        RUNTIME_ERROR,
        OBSERVED_OBJECT_ERROR,
        OBSERVED_ATTRIBUTE_ERROR,
        OBSERVED_ATTRIBUTE_TYPE_ERROR,
        THRESHOLD_ERROR,
        THRESHOLD_VALUE_EXCEEDED
    };

    private static final MBeanNotificationInfo[] notifsInfo = {
        new MBeanNotificationInfo(
            types,
            "javax.management.monitor.MonitorNotification",
            "Notifications sent by the CounterMonitor MBean")
    };

    /*
     * ------------------------------------------
     *  CONSTRUCTORS
     * ------------------------------------------
     */

    /**
     * Default constructor.
     */
    public CounterMonitor() {
    }

    /*
     * ------------------------------------------
     *  PUBLIC METHODS
     * ------------------------------------------
     */

    /**
     * Starts the counter monitor.
     */
    public synchronized void start() {
        if (isActive()) {
            MONITOR_LOGGER.log(Level.TRACE, "the monitor is already active");
            return;
        }
        // Reset values.
        //
        for (ObservedObject o : observedObjects) {
            final CounterMonitorObservedObject cmo =
                (CounterMonitorObservedObject) o;
            cmo.setThreshold(initThreshold);
            cmo.setModulusExceeded(false);
            cmo.setEventAlreadyNotified(false);
            cmo.setPreviousScanCounter(null);
        }
        doStart();
    }

    /**
     * Stops the counter monitor.
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
     * @param object the name of the object whose derived gauge is to
     * be returned.
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
     * Gets the current threshold value of the specified object, if
     * this object is contained in the set of observed MBeans, or
     * <code>null</code> otherwise.
     *
     * @param object the name of the object whose threshold is to be
     * returned.
     *
     * @return The threshold value of the specified object.
     *
     */
    public synchronized Number getThreshold(ObjectName object) {
        final CounterMonitorObservedObject o =
            (CounterMonitorObservedObject) getObservedObject(object);
        if (o == null)
            return null;

        // If the counter that is monitored rolls over when it reaches a
        // maximum value, then the modulus value needs to be set to that
        // maximum value. The threshold will then also roll over whenever
        // it strictly exceeds the modulus value. When the threshold rolls
        // over, it is reset to the value that was specified through the
        // latest call to the monitor's setInitThreshold method, before
        // any offsets were applied.
        //
        if (offset.longValue() > 0L &&
            modulus.longValue() > 0L &&
            o.getThreshold().longValue() > modulus.longValue()) {
            return initThreshold;
        } else {
            return o.getThreshold();
        }
    }

    /**
     * Gets the initial threshold value common to all observed objects.
     *
     * @return The initial threshold.
     *
     * @see #setInitThreshold
     *
     */
    public synchronized Number getInitThreshold() {
        return initThreshold;
    }

    /**
     * Sets the initial threshold value common to all observed objects.
     *
     * <BR>The current threshold of every object in the set of
     * observed MBeans is updated consequently.
     *
     * @param value The initial threshold value.
     *
     * @exception IllegalArgumentException The specified
     * threshold is null or the threshold value is less than zero.
     *
     * @see #getInitThreshold
     *
     */
    public synchronized void setInitThreshold(Number value)
        throws IllegalArgumentException {

        if (value == null) {
            throw new IllegalArgumentException("Null threshold");
        }
        if (value.longValue() < 0L) {
            throw new IllegalArgumentException("Negative threshold");
        }

        if (initThreshold.equals(value))
            return;
        initThreshold = value;

        // Reset values.
        //
        int index = 0;
        for (ObservedObject o : observedObjects) {
            resetAlreadyNotified(o, index++, THRESHOLD_ERROR_NOTIFIED);
            final CounterMonitorObservedObject cmo =
                (CounterMonitorObservedObject) o;
            cmo.setThreshold(value);
            cmo.setModulusExceeded(false);
            cmo.setEventAlreadyNotified(false);
        }
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
     * Gets the threshold value of the first object in the set of
     * observed MBeans.
     *
     * @return The threshold value.
     *
     * @see #setThreshold
     *
     * @deprecated As of JMX 1.2, replaced by {@link #getThreshold(ObjectName)}
     */
    @Deprecated
    public synchronized Number getThreshold() {
        return getThreshold(getObservedObject());
    }

    /**
     * Sets the initial threshold value.
     *
     * @param value The initial threshold value.
     *
     * @exception IllegalArgumentException The specified threshold is
     * null or the threshold value is less than zero.
     *
     * @see #getThreshold()
     *
     * @deprecated As of JMX 1.2, replaced by {@link #setInitThreshold}
     */
    @Deprecated
    public synchronized void setThreshold(Number value)
        throws IllegalArgumentException {
        setInitThreshold(value);
    }

    /**
     * Gets the offset value common to all observed MBeans.
     *
     * @return The offset value.
     *
     * @see #setOffset
     */
    public synchronized Number getOffset() {
        return offset;
    }

    /**
     * Sets the offset value common to all observed MBeans.
     *
     * @param value The offset value.
     *
     * @exception IllegalArgumentException The specified
     * offset is null or the offset value is less than zero.
     *
     * @see #getOffset
     */
    public synchronized void setOffset(Number value)
        throws IllegalArgumentException {

        if (value == null) {
            throw new IllegalArgumentException("Null offset");
        }
        if (value.longValue() < 0L) {
            throw new IllegalArgumentException("Negative offset");
        }

        if (offset.equals(value))
            return;
        offset = value;

        int index = 0;
        for (ObservedObject o : observedObjects) {
            resetAlreadyNotified(o, index++, THRESHOLD_ERROR_NOTIFIED);
        }
    }

    /**
     * Gets the modulus value common to all observed MBeans.
     *
     * @see #setModulus
     *
     * @return The modulus value.
     */
    public synchronized Number getModulus() {
        return modulus;
    }

    /**
     * Sets the modulus value common to all observed MBeans.
     *
     * @param value The modulus value.
     *
     * @exception IllegalArgumentException The specified
     * modulus is null or the modulus value is less than zero.
     *
     * @see #getModulus
     */
    public synchronized void setModulus(Number value)
        throws IllegalArgumentException {

        if (value == null) {
            throw new IllegalArgumentException("Null modulus");
        }
        if (value.longValue() < 0L) {
            throw new IllegalArgumentException("Negative modulus");
        }

        if (modulus.equals(value))
            return;
        modulus = value;

        // Reset values.
        //
        int index = 0;
        for (ObservedObject o : observedObjects) {
            resetAlreadyNotified(o, index++, THRESHOLD_ERROR_NOTIFIED);
            final CounterMonitorObservedObject cmo =
                (CounterMonitorObservedObject) o;
            cmo.setModulusExceeded(false);
        }
    }

    /**
     * Gets the notification's on/off switch value common to all
     * observed MBeans.
     *
     * @return <CODE>true</CODE> if the counter monitor notifies when
     * exceeding the threshold, <CODE>false</CODE> otherwise.
     *
     * @see #setNotify
     */
    public synchronized boolean getNotify() {
        return notify;
    }

    /**
     * Sets the notification's on/off switch value common to all
     * observed MBeans.
     *
     * @param value The notification's on/off switch value.
     *
     * @see #getNotify
     */
    public synchronized void setNotify(boolean value) {
        if (notify == value)
            return;
        notify = value;
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
            final CounterMonitorObservedObject cmo =
                (CounterMonitorObservedObject) o;
            cmo.setThreshold(initThreshold);
            cmo.setModulusExceeded(false);
            cmo.setEventAlreadyNotified(false);
            cmo.setPreviousScanCounter(null);
        }
    }

    /**
     * Returns a <CODE>NotificationInfo</CODE> object containing the
     * name of the Java class of the notification and the notification
     * types sent by the counter monitor.
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
     * @param scanCounter The value of the observed attribute.
     * @param o The observed object.
     * @return <CODE>true</CODE> if the derived gauge value is valid,
     * <CODE>false</CODE> otherwise.  The derived gauge value is
     * invalid when the differenceMode flag is set to
     * <CODE>true</CODE> and it is the first notification (so we
     * haven't 2 consecutive values to update the derived gauge).
     */
    private synchronized boolean updateDerivedGauge(
        Object scanCounter, CounterMonitorObservedObject o) {

        boolean is_derived_gauge_valid;

        // The counter difference mode is used.
        //
        if (differenceMode) {

            // The previous scan counter has been initialized.
            //
            if (o.getPreviousScanCounter() != null) {
                setDerivedGaugeWithDifference((Number)scanCounter, null, o);

                // If derived gauge is negative it means that the
                // counter has wrapped around and the value of the
                // threshold needs to be reset to its initial value.
                //
                if (((Number)o.getDerivedGauge()).longValue() < 0L) {
                    if (modulus.longValue() > 0L) {
                        setDerivedGaugeWithDifference((Number)scanCounter,
                                                      modulus, o);
                    }
                    o.setThreshold(initThreshold);
                    o.setEventAlreadyNotified(false);
                }
                is_derived_gauge_valid = true;
            }
            // The previous scan counter has not been initialized.
            // We cannot update the derived gauge...
            //
            else {
                is_derived_gauge_valid = false;
            }
            o.setPreviousScanCounter((Number)scanCounter);
        }
        // The counter difference mode is not used.
        //
        else {
            o.setDerivedGauge((Number)scanCounter);
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
        CounterMonitorObservedObject o) {

        MonitorNotification n = null;

        // Send notification if notify is true.
        //
        if (!o.getEventAlreadyNotified()) {
            if (((Number)o.getDerivedGauge()).longValue() >=
                o.getThreshold().longValue()) {
                if (notify) {
                    n = new MonitorNotification(THRESHOLD_VALUE_EXCEEDED,
                                                this,
                                                0,
                                                0,
                                                "",
                                                null,
                                                null,
                                                null,
                                                o.getThreshold());
                }
                if (!differenceMode) {
                    o.setEventAlreadyNotified(true);
                }
            }
        } else {
            if (MONITOR_LOGGER.isLoggable(Level.TRACE)) {
                final StringBuilder strb = new StringBuilder()
                .append("The notification:")
                .append("\n\tNotification observed object = ")
                .append(o.getObservedObject())
                .append("\n\tNotification observed attribute = ")
                .append(getObservedAttribute())
                .append("\n\tNotification threshold level = ")
                .append(o.getThreshold())
                .append("\n\tNotification derived gauge = ")
                .append(o.getDerivedGauge())
                .append("\nhas already been sent");
                MONITOR_LOGGER.log(Level.TRACE, strb::toString);
            }
        }

        return n;
    }

    /**
     * Updates the threshold attribute of the observed object.
     * @param o The observed object.
     */
    private synchronized void updateThreshold(CounterMonitorObservedObject o) {

        // Calculate the new threshold value if the threshold has been
        // exceeded and if the offset value is greater than zero.
        //
        if (((Number)o.getDerivedGauge()).longValue() >=
            o.getThreshold().longValue()) {

            if (offset.longValue() > 0L) {

                // Increment the threshold until its value is greater
                // than the one for the current derived gauge.
                //
                long threshold_value = o.getThreshold().longValue();
                while (((Number)o.getDerivedGauge()).longValue() >=
                       threshold_value) {
                    threshold_value += offset.longValue();
                }

                // Set threshold attribute.
                //
                switch (o.getType()) {
                    case INTEGER:
                        o.setThreshold(Integer.valueOf((int)threshold_value));
                        break;
                    case BYTE:
                        o.setThreshold(Byte.valueOf((byte)threshold_value));
                        break;
                    case SHORT:
                        o.setThreshold(Short.valueOf((short)threshold_value));
                        break;
                    case LONG:
                        o.setThreshold(Long.valueOf(threshold_value));
                        break;
                    default:
                        // Should never occur...
                        MONITOR_LOGGER.log(Level.TRACE,
                                "the threshold type is invalid");
                        break;
                }

                // If the counter can wrap around when it reaches
                // its maximum and we are not dealing with counter
                // differences then we need to reset the threshold
                // to its initial value too.
                //
                if (!differenceMode) {
                    if (modulus.longValue() > 0L) {
                        if (o.getThreshold().longValue() >
                            modulus.longValue()) {
                            o.setModulusExceeded(true);
                            o.setDerivedGaugeExceeded(
                                (Number) o.getDerivedGauge());
                        }
                    }
                }

                // Threshold value has been modified so we can notify again.
                //
                o.setEventAlreadyNotified(false);
            } else {
                o.setModulusExceeded(true);
                o.setDerivedGaugeExceeded((Number) o.getDerivedGauge());
            }
        }
    }

    /**
     * Sets the derived gauge of the specified observed object when the
     * differenceMode flag is set to <CODE>true</CODE>.  Integer types
     * only are allowed.
     *
     * @param scanCounter The value of the observed attribute.
     * @param mod The counter modulus value.
     * @param o The observed object.
     */
    private synchronized void setDerivedGaugeWithDifference(
        Number scanCounter, Number mod, CounterMonitorObservedObject o) {
        /* We do the arithmetic using longs here even though the
           result may end up in a smaller type.  Since
           l == (byte)l (mod 256) for any long l,
           (byte) ((byte)l1 + (byte)l2) == (byte) (l1 + l2),
           and likewise for subtraction.  So it's the same as if
           we had done the arithmetic in the smaller type.*/

        long derived =
            scanCounter.longValue() - o.getPreviousScanCounter().longValue();
        if (mod != null)
            derived += modulus.longValue();

        switch (o.getType()) {
        case INTEGER: o.setDerivedGauge(Integer.valueOf((int) derived)); break;
        case BYTE: o.setDerivedGauge(Byte.valueOf((byte) derived)); break;
        case SHORT: o.setDerivedGauge(Short.valueOf((short) derived)); break;
        case LONG: o.setDerivedGauge(Long.valueOf(derived)); break;
        default:
            // Should never occur...
            MONITOR_LOGGER.log(Level.TRACE,
                    "the threshold type is invalid");
            break;
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
        final CounterMonitorObservedObject cmo =
            new CounterMonitorObservedObject(object);
        cmo.setThreshold(initThreshold);
        cmo.setModulusExceeded(false);
        cmo.setEventAlreadyNotified(false);
        cmo.setPreviousScanCounter(null);
        return cmo;
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
        final CounterMonitorObservedObject o =
            (CounterMonitorObservedObject) getObservedObject(object);
        if (o == null)
            return false;

        // Check that the observed attribute is of type "Integer".
        //
        if (value instanceof Integer) {
            o.setType(INTEGER);
        } else if (value instanceof Byte) {
            o.setType(BYTE);
        } else if (value instanceof Short) {
            o.setType(SHORT);
        } else if (value instanceof Long) {
            o.setType(LONG);
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
        final CounterMonitorObservedObject o =
            (CounterMonitorObservedObject) getObservedObject(object);
        if (o == null)
            return null;

        // Check if counter has wrapped around.
        //
        if (o.getModulusExceeded()) {
            if (((Number)o.getDerivedGauge()).longValue() <
                o.getDerivedGaugeExceeded().longValue()) {
                    o.setThreshold(initThreshold);
                    o.setModulusExceeded(false);
                    o.setEventAlreadyNotified(false);
            }
        }

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
        final CounterMonitorObservedObject o = (CounterMonitorObservedObject)
            getObservedObject(notification.getObservedObject());
        if (o == null)
            return;

        // Reset values.
        //
        o.setModulusExceeded(false);
        o.setEventAlreadyNotified(false);
        o.setPreviousScanCounter(null);
    }

    @Override
    synchronized MonitorNotification buildAlarmNotification(
                                               ObjectName object,
                                               String attribute,
                                               Comparable<?> value) {
        final CounterMonitorObservedObject o =
            (CounterMonitorObservedObject) getObservedObject(object);
        if (o == null)
            return null;

        // Notify the listeners and update the threshold if
        // the updated derived gauge value is valid.
        //
        final MonitorNotification alarm;
        if (o.getDerivedGaugeValid()) {
            alarm = updateNotifications(o);
            updateThreshold(o);
        } else {
            alarm = null;
        }
        return alarm;
    }

    /**
     * Tests if the threshold, offset and modulus of the specified observed
     * object are of the same type as the counter. Only integer types are
     * allowed.
     *
     * Note:
     *   If the optional offset or modulus have not been initialized, their
     *   default value is an Integer object with a value equal to zero.
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
        final CounterMonitorObservedObject o =
            (CounterMonitorObservedObject) getObservedObject(object);
        if (o == null)
            return false;

        Class<? extends Number> c = classForType(o.getType());
        return (c.isInstance(o.getThreshold()) &&
                isValidForType(offset, c) &&
                isValidForType(modulus, c));
    }
}
