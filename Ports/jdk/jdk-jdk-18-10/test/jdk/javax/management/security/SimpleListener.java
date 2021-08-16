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

// JDK
import java.util.Vector;

// JMX
import javax.management.NotificationListener;
import javax.management.Notification;

public class SimpleListener implements NotificationListener {
    private boolean received = false;
    private String type = null;
    private Object handback = null;
    private Vector<Object> handbacks = new Vector<Object>();
    private int nbrec = 0;

    public synchronized void handleNotification(Notification notification,
                                                Object handback) {
        Utils.debug(Utils.DEBUG_STANDARD,
            "SimpleListener::handleNotification :" + notification);
        try {
            received = true;
            type = notification.getType();
            this.handback = handback;
            handbacks.add(handback);
            nbrec++;
            notify();
        } catch(Exception e) {
            System.out.println("(ERROR) SimpleListener::handleNotification :"
                        + " Caught exception "
                        + e) ;
        }
    }

    public synchronized boolean isNotificationReceived() {
        boolean ret = received;
        reset();
        return ret;
    }

    public synchronized Object[] waitForMultiNotifications(int nb) {
        while(true) {
            if(nbrec < nb) {
                Utils.debug(Utils.DEBUG_STANDARD,
                            "SimpleListener::waitForMultiNotifications wait");
                try {
                    wait();
                } catch(InterruptedException ie) {
                    // OK : we wait for being interrupted
                }
                Utils.debug(Utils.DEBUG_STANDARD,
                            "SimpleListener::waitForMultiNotifications wait over");
            }
            else
            break;
        }
        Object[] ret = handbacks.toArray();
        reset();
        return ret;
    }

    private void reset() {
        received = false;
        handback = null;
        handbacks.removeAllElements();
        type = null;
    }

    public synchronized Object waitForNotificationHB() {
        while(true) {
            if(!received) {
                Utils.debug(Utils.DEBUG_STANDARD,
                    "SimpleListener::waitForNotificationHB wait");
                try {
                    wait();
                } catch(InterruptedException ie) {
                    // OK : we wait for being interrupted
                }
                Utils.debug(Utils.DEBUG_STANDARD,
                    "SimpleListener::waitForNotificationHB received");
            }
            else
                break;
        }
        Object ret = handback;
        reset();
        return ret;
    }

    public synchronized String waitForNotification() {
        while(true) {
            if(!received) {
                Utils.debug(Utils.DEBUG_STANDARD,
                    "SimpleListener::waitForNotification wait");
                try {
                    wait();
                } catch(InterruptedException ie) {
                    // OK : we wait for being interrupted
                }
                Utils.debug(Utils.DEBUG_STANDARD,
                    "SimpleListener::waitForNotification received");
            }
            else
                break;
        }
        String ret = type;
        reset();
        return ret;
    }
}
