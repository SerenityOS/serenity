/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AccessControlContext;
import java.security.AccessController;
import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Iterator;
import java.util.Set;

import javax.management.MBeanRegistration ;
import javax.management.MBeanServer ;
import javax.management.ObjectName ;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;
import javax.management.Notification;

public class MBS_Light extends NotificationBroadcasterSupport
    implements MBS_LightMBean, MBeanRegistration, NotificationListener
{
    private RjmxMBeanParameter param = null ;
    private String aString = "notset" ;
    private int anInt = 0 ;
    private MBeanServer mbs = null ;
    private ObjectName objname = null ;
    private Exception anException = null ;
    private Error anError = null ;
    private int count = 0;
    private SimpleListener listener = new SimpleListener();

    @SqeDescriptorKey("NO PARAMETER CONSTRUCTOR MBS_Light")
    public MBS_Light() {
    }

    @SqeDescriptorKey("ONE RjmxMBeanParameter PARAMETER CONSTRUCTOR MBS_Light")
    public MBS_Light(@SqeDescriptorKey("CONSTRUCTOR PARAMETER param")
                     RjmxMBeanParameter param) {
        this.param = param ;
    }

    @SqeDescriptorKey("ONE String PARAMETER CONSTRUCTOR MBS_Light")
    public MBS_Light(@SqeDescriptorKey("CONSTRUCTOR PARAMETER param")String param) {
        this.aString = param ;
    }

    // Getter for property param
    public RjmxMBeanParameter getParam() {
        return this.param ;
    }

    // Setter for property param
    public void setParam(RjmxMBeanParameter param) {
        this.param = param ;
    }

    // Getter for property aString
    public String getAstring() {
        return this.aString ;
    }

    // Setter for property aString
    public void setAstring(String aString) {
        this.aString = aString ;
    }

    // Getter for property anInt
    public int getAnInt() {
        return this.anInt ;
    }

    // Setter for property anInt
    public void setAnInt(int anInt) {
        this.anInt = anInt ;
    }

    // Getter for property anException
    public Exception getAnException() {
        return this.anException ;
    }

    // Setter for property anException
    public void setAnException(Exception anException) {
        this.anException = anException ;
    }

    // Getter for property anError
    public Error getAnError() {
        return this.anError ;
    }

    // Setter for property anError
    public void setAnError(Error anError) {
        this.anError = anError ;
    }

    // An operation
    public RjmxMBeanParameter operate1(String name) {
        return new RjmxMBeanParameter(name) ;
    }

    // An operation
    public String operate2(RjmxMBeanParameter param) {
        return param.name ;
    }

    // An operation
    public void throwError() {
        throw new Error("JSR-160-ERROR");
    }

    // An operation
    public void throwException() throws Exception {
        throw new Exception("JSR-160-EXCEPTION");
    }

    // MBeanRegistration method
    public void postDeregister() {
    }

    // MBeanRegistration method
    public void postRegister(Boolean registrationDone) {
    }

    // MBeanRegistration method
    public void preDeregister()
        throws Exception
    {
    }

    // MBeanRegistration method
    public ObjectName preRegister(MBeanServer server, ObjectName name)
        throws Exception
    {
        this.mbs = server ;
        if ( name == null ) {
            this.objname = new ObjectName("protocol:class=MBS_Light") ;
        }
        else {
            this.objname = name ;
        }
        return this.objname ;
    }

    public synchronized void handleNotification(Notification notification,
                                                Object handback) {
        Utils.debug(Utils.DEBUG_STANDARD,
            "MBS_Light::handleNotification: " + notification);
        listener.handleNotification(notification, handback);
    }

    // Send a notification
    public void sendNotification() {
        Notification notification =
            new Notification("JSR160-TCK-NOTIFICATION", this, count++);
        sendNotification(notification);
    }

    public Object waitForNotificationHB() {
        return listener.waitForNotificationHB();
    }

    // Receive multi notifications and send back handbacks
    public synchronized Object[] waitForMultiNotifications(String nb) {
        return listener.waitForMultiNotifications(Integer.valueOf(nb).intValue());
    }

    // Receive a notification
    public synchronized String waitForNotification() {
        return listener.waitForNotification();
    }

    // Is the notification received
    public synchronized Boolean notificationReceived() {
        return Boolean.valueOf(listener.isNotificationReceived());
    }

    // The authorization Id
    public String getAuthorizationId() {
        AccessControlContext acc = AccessController.getContext();
        Subject subject = Subject.getSubject(acc);
        Set<Principal> principals = subject.getPrincipals();
        Iterator<Principal> i = principals.iterator();
        StringBuffer buffer = new StringBuffer();
        while(i.hasNext()) {
            Principal p = i.next();
            buffer.append(p.getName());
            if(i.hasNext())
                buffer.append(" ");
        }

        return buffer.toString();
    }
}
