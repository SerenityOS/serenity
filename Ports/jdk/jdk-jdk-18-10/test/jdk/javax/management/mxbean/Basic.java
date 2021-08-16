/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

/**
 * Class Basic
 * Basic Description
 */
public class Basic implements BasicMXBean, NotificationEmitter,
                              MBeanRegistration {

    public static final String EXCEPTION_MESSAGE = "from Basic";
    public static final String NOTIFICATION_MESSAGE = "from Basic";
    /** Attribute : IntAtt */
    private int intAtt = 0;
    /** Attribute : IntegerAtt */
    private Integer integerAtt = 0;
    /** Attribute : BoolAtt */
    private boolean boolAtt = false;
    /** Attribute : BooleanAtt */
    private Boolean booleanAtt = false;
    /** Attribute : StringAtt */
    private String stringAtt = null;
    /** Attribute : DateAtt */
    private Date dateAtt = null;
    /** Attribute : ObjectNameAtt */
    private ObjectName objectNameAtt = null;
    /** Attribute : NotifDescriptorAsMapAtt */
    private Map<String, String> notifDescriptorAsMapAtt = null;
    /** Attribute : NotifDescriptorAtt */
    private Descriptor notifDescriptorAtt = null;
    /** Attribute : SqeParameter */
    private SqeParameter sqeParameterAtt = null;

    /* Creates a new instance of Basic */
    @SqeDescriptorKey("CONSTRUCTOR Basic")
    public Basic() {
    }

    /* Creates a new instance of Basic */
    @SqeDescriptorKey("CONSTRUCTOR Basic")
    public Basic(
            @SqeDescriptorKey("CONSTRUCTOR PARAMETER SqeParameter") SqeParameter param) {
    }

    /**
     * Get int attribute
     */
    public int getIntAtt() {
        return intAtt;
    }

    /**
     * Set int attribute
     */
    public void setIntAtt(int value) {
        intAtt = value;
    }

    /**
     * Get Integer attribute
     */
    public Integer getIntegerAtt() {
        return integerAtt;
    }

    /**
     * Set Integer attribute
     */
    public void setIntegerAtt(Integer value) {
        integerAtt = value;
    }

    /**
     * Get boolean attribute
     */
    public boolean getBoolAtt() {
        return boolAtt;
    }

    /**
     * Set boolean attribute
     */
    public void setBoolAtt(boolean value) {
        boolAtt = value;
    }

    /**
     * Get Boolean attribute
     */
    public Boolean getBooleanAtt() {
        return booleanAtt;
    }

    /**
     * Set Boolean attribute
     */
    public void setBooleanAtt(Boolean value) {
        booleanAtt = value;
    }

    /**
     * Get String attribute
     */
    public String getStringAtt() {
        return stringAtt;
    }

    /**
     * Set String attribute
     */
    public void setStringAtt(String value) {
        stringAtt = value;
    }

    /**
     * Get Date attribute
     */
    public Date getDateAtt() {
        return dateAtt;
    }

    /**
     * Set Date attribute
     */
    public void setDateAtt(Date value) {
        dateAtt = value;
    }

    /**
     * Get ObjectName attribute
     */
    public ObjectName getObjectNameAtt() {
        return objectNameAtt;
    }

    /**
     * Set ObjectName attribute
     */
    public void setObjectNameAtt(ObjectName value) {
        objectNameAtt = value;
    }

    /**
     * Get SqeParameter attribute
     */
    public SqeParameter getSqeParameterAtt() throws Exception {
        if (sqeParameterAtt == null) {
            sqeParameterAtt = new SqeParameter();
            sqeParameterAtt.setGlop("INITIALIZED");
        }

        return sqeParameterAtt;
    }

    /**
     * Set SqeParameter attribute
     */
    public void setSqeParameterAtt(SqeParameter value) {
        sqeParameterAtt = value;
    }

    /**
     * Get the Descriptor used to build the NotificationInfo
     * of emitted notifications.
     */
    public Map<String, String> getNotifDescriptorAsMapAtt() {
        if (notifDescriptorAsMapAtt == null) {
            initNotifDescriptorAtt();
        }

        return notifDescriptorAsMapAtt;
    }

    /**
     * Set the Descriptor used to build the NotificationInfo
     * of emitted notifications.
     * <br>A Map<String, Object> would better fit Descriptor needs but then
     * it is not convertible according the MXBean specification so the MBean
     * registration fails.
     * As we plan to test our custom Descriptor finds its way into
     * the metadata of emitted notifications, String is good enough.
     */
    public void setNotifDescriptorAsMapAtt(Map<String, String> value) {
        notifDescriptorAsMapAtt = new HashMap<String, String>(value);
        notifDescriptorAtt = new ImmutableDescriptor(value);
    }

    /**
     * Do nothing
     */
    public void doNothing() {
        // I said NOTHING !
    }

    /**
     * Do take SqeParameter as a parameter
     */
    public void doWeird(SqeParameter param) {
    }

    /**
     * Throw an Exception
     */
    public void throwException() throws Exception {
        throw new Exception(EXCEPTION_MESSAGE);
    }

    /**
     * Throw an Error
     */
    public void throwError() {
        throw new InternalError(EXCEPTION_MESSAGE);
    }

    /**
     * Reset all attributes
     */
    public void reset() {
        intAtt = 0;
        integerAtt = 0;
        boolAtt = false;
        booleanAtt = Boolean.FALSE;
        stringAtt = null;
        dateAtt = null;
        objectNameAtt = null;
    }

    /**
     * Returns the weather for the coming days
     * @param verbose <code>boolean</code> verbosity
     * @throws java.lang.Exception <code>storm</code>
     * @return <code>ObjectName</code>
     */
    public Weather getWeather(boolean verbose)
            throws java.lang.Exception {
        return Weather.SUNNY;
    }

    // Starting here are the 4 methods of MBeanRegistration interface.
    // We use that to grab the ObjectName the MBean is registered with.
    //
    public ObjectName preRegister(MBeanServer server, ObjectName name)
            throws Exception {
        // Grab a reference on the MBeanServer we're registered in.
        mbs = server;
        // Compute the name we're registered with.
        if (name != null) {
            mbeanName = name;
            return name;
        } else {
            mbeanName =
                new ObjectName("sqe:type=" + Basic.class.getName());
            return mbeanName;
        }
    }

    public void postRegister(Boolean registrationDone) {
        // Do nothing
    }

    public void preDeregister() throws Exception {
        // Do nothing
    }

    public void postDeregister() {
        // Do nothing
    }

    /**
     * Send one Notification of the provided notifType type.
     */
    public void sendNotification(String notifType) {
        Notification notification = null;

        if (notifType.equals(NOTIF_TYPE_0)) {
            notification = new Notification(NOTIF_TYPE_0,
                    mbeanName,
                    seqNumber,
                    NOTIFICATION_MESSAGE);
        } else if (notifType.equals(NOTIF_TYPE_1)) {
            notification = new SqeNotification(NOTIF_TYPE_1,
                    mbeanName,
                    seqNumber,
                    NOTIFICATION_MESSAGE);
        }

        seqNumber++;
        broadcaster.sendNotification(notification);
    }

    /**
     * That method starts a set of threads, each thread sends a given number of
     * notifications.
     * The number of threads can be set via the attribute numOfNotificationSenders.
     * The number of notification sent by each thread can be set via
     * the attribute numOfNotificationSenderLoops.
     * Depending on the parameter customNotification we send either custom
     * notification(s) or MBeanServer registration and unregistration notification(s).
     * When customNotification=true the total number of notification(s) sent is
     * (numOfNotificationSenders * numOfNotificationSenderLoops). They are
     * sequentially of type NOTIF_TYPE_0 then NOTIF_TYPE_1 and so on.
     *
     * When customNotification=false the total number of notification(s) sent is
     * (numOfNotificationSenders * numOfNotificationSenderLoops) registration
     * notification(s)
     * +
     * (numOfNotificationSenders * numOfNotificationSenderLoops) unregistration
     * notification(s)
     *
     * @throws java.lang.Exception
     */
    public void sendNotificationWave(boolean customNotification) throws
            Exception {
        // Build the set of notification sender.
        Collection<Callable<Integer>> tasks =
                new HashSet<Callable<Integer>>(numOfNotificationSenders);

        for (int i = 1; i <= numOfNotificationSenders; i++) {
            tasks.add(new NotifSender(numOfNotificationSenderLoops,
                    customNotification, i));
        }

        // Start all notification sender in parallel.
        ExecutorService execServ = null;
        try {
            execServ = Executors.newFixedThreadPool(numOfNotificationSenders);
            List<Future<Integer>> taskHandlers = execServ.invokeAll(tasks);
            checkNotifSenderThreadStatus(taskHandlers);
        } finally {
            if (!execServ.isShutdown()) {
                execServ.shutdown();
            }
        }
    }

    public void setNumOfNotificationSenders(int value) {
        numOfNotificationSenders = value;
    }

    public void setNumOfNotificationSenderLoops(int value) {
        numOfNotificationSenderLoops = value;
    }

    /**
     * MBean Notification support
     * You shouldn't update these methods
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">
    public void addNotificationListener(NotificationListener listener,
                                        NotificationFilter filter,
                                        Object handback)
            throws IllegalArgumentException {
        broadcaster.addNotificationListener(listener, filter, handback);
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
        if (notifDescriptorAtt == null) {
            initNotifDescriptorAtt();
        }

        return new MBeanNotificationInfo[]{
                    new MBeanNotificationInfo(new String[]{
                        NOTIF_TYPE_0
                    },
                    javax.management.Notification.class.getName(),
                    "Standard JMX Notification",
                    notifDescriptorAtt),
                    new MBeanNotificationInfo(new String[]{
                        NOTIF_TYPE_1
                    },
                    SqeNotification.class.getName(),
                    "SQE Notification",
                    notifDescriptorAtt)
                };
    }

    public void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException {
        broadcaster.removeNotificationListener(listener);
    }

    public void removeNotificationListener(NotificationListener listener,
                                           NotificationFilter filter,
                                           Object handback)
            throws ListenerNotFoundException {
        broadcaster.removeNotificationListener(listener, filter, handback);
    }
    // </editor-fold>
    private synchronized long getNextSeqNumber() {
        return seqNumber++;
    }

    private void initNotifDescriptorAtt() {
        String key = "CRABE";
        String value = "TAMBOUR";
        notifDescriptorAtt =
                new ImmutableDescriptor(new String[]{key + "=" + value});
        notifDescriptorAsMapAtt =
                new HashMap<String, String>();
        notifDescriptorAsMapAtt.put(key, value);
    }

    private void checkNotifSenderThreadStatus(
            List<Future<Integer>> taskHandlers)
            throws Exception {
        String msgTag = "Basic::checkNotifSenderThreadStatus: ";
        // Grab back status of each notification sender.
        for (Future<Integer> f : taskHandlers) {
            if (f.isCancelled()) {
                String message = msgTag +
                        "---- ERROR : One thread has been cancelled";
                System.out.println(message);
                throw new RuntimeException(message);
            } else {
                Integer effectiveNumOfLoops = f.get();

                if (effectiveNumOfLoops != numOfNotificationSenderLoops) {
                    String message = msgTag + "---- ERROR : One thread did " +
                            effectiveNumOfLoops + " loops in place of " +
                            numOfNotificationSenderLoops;
                    System.out.println(message);
                    throw new RuntimeException(message);
                }
            }
        }
    }
    //
    private int numOfNotificationSenderLoops = 2;
    private int numOfNotificationSenders = 13;

    private class NotifSender implements Callable<Integer> {

        private int cycles;
        private boolean customNotification;
        private int senderID;

        public NotifSender(int cycles, boolean customNotification, int id) {
            this.cycles = cycles;
            this.customNotification = customNotification;
            this.senderID = id;
        }

        public Integer call() throws Exception {
            int callsDone = 0;

            try {
                for (int i = 1; i <= cycles; i++) {
                    if (customNotification) {
                        if (i % 2 == 0) {
                            sendNotification(NOTIF_TYPE_0);
                        } else {
                            sendNotification(NOTIF_TYPE_1);
                        }
                    } else {
                        ObjectName mbeanName = new ObjectName("SQE:type=" +
                                mbeanClassName + ",senderID=" + senderID);
                        mbs.createMBean(mbeanClassName, mbeanName);
                        mbs.unregisterMBean(mbeanName);
                    }
                    callsDone++;
                }
            } catch (Exception e) {
                System.out.println("NotifSender::call: (ERROR) Thread [" + senderID +
                        "] failed after " + callsDone + " cycles");
                throw e;
            }

            return Integer.valueOf(callsDone);
        }
    }

    //
    private long seqNumber;
    private final NotificationBroadcasterSupport broadcaster =
            new NotificationBroadcasterSupport();
    private ObjectName mbeanName;
    private MBeanServer mbs;
    private String mbeanClassName = "Simple";

    /**
     * Notification types definitions. To use when creating JMX Notifications.
     */
    public static final String NOTIF_TYPE_0 =
            "sqe.notification.a.type";
    public static final String NOTIF_TYPE_1 =
            "sqe.notification.b.type";
}
