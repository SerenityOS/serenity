/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test NotifBufferSizePropertyNameTest
 * @bug 6174229
 * @summary Verify the property name specifying server notification buffer size.
 * @author Shanliang JIANG
 *
 * @run clean NotifBufferSizePropertyNameTest
 * @run build NotifBufferSizePropertyNameTest
 * @run main NotifBufferSizePropertyNameTest
 */

import java.io.IOException;
import java.util.*;

import javax.management.*;
import javax.management.remote.*;

/**
 * This class tests also the size of a server notification buffer.
 */
public class NotifBufferSizePropertyNameTest {

    private static ObjectName oname;
    private static JMXServiceURL url;
    private final static NotificationListener listener = new NotificationListener() {
                public void handleNotification(Notification n, Object hb) {
                    // nothing
                }
            };

    public static void main(String[] args) throws Exception {
        System.out.println(
           "Verify the property name specifying the server notification buffer size.");

        oname = new ObjectName ("Default:name=NotificationEmitter");
        url = new JMXServiceURL("rmi", null, 0);
        Map env = new HashMap(2);

        System.out.println("Test the new property name.");
        env.put("jmx.remote.x.notification.buffer.size", String.valueOf(bufferSize));
        test(env);

        System.out.println("Test the old property name.");
        env.remove("jmx.remote.x.notification.buffer.size");
        env.put("jmx.remote.x.buffer.size", String.valueOf(bufferSize));
        test(env);

        System.out.println("Test that the new property name overwrite the old one.");
        env.put("jmx.remote.x.notification.buffer.size", String.valueOf(bufferSize));
        env.put("jmx.remote.x.buffer.size", String.valueOf(bufferSize*6));
        test(env);

        System.out.println("Test the old property name on system.");
        System.setProperty("jmx.remote.x.buffer.size", String.valueOf(bufferSize));
        test(null);

        System.out.println(
             "Test that the new property name overwrite the old one on system.");
        System.setProperty("jmx.remote.x.notification.buffer.size",
                           String.valueOf(bufferSize));
        System.setProperty("jmx.remote.x.buffer.size", String.valueOf(bufferSize*6));
        test(null);
    }


    private static void test(Map env) throws Exception {
        final MBeanServer mbs = MBeanServerFactory.newMBeanServer();

        mbs.registerMBean(new NotificationEmitter(), oname);
        JMXConnectorServer server = JMXConnectorServerFactory.newJMXConnectorServer(
                                                                               url,
                                                                               env,
                                                                               mbs);
        server.start();

        JMXServiceURL addr = server.getAddress();
        JMXConnector client = JMXConnectorFactory.connect(addr);
        client.getMBeanServerConnection().addNotificationListener(oname,
                                                                  listener,
                                                                  null,
                                                                  null);

        Thread.sleep(10); // give time to other notifs
        weakNotifs.clear();

        // send notifd
        mbs.invoke(oname, "sendNotifications",
                   new Object[] {new Integer(toSend)},
                   new String[] {"java.lang.Integer"});

        client.close();
        client = null;

        // give time to GC
        for(int i=0; i<200; i++) {
            if (weakNotifs.keySet().size() > bufferSize) {
                Thread.sleep(10);
                System.gc();
            } else {
                break;
            }
        }

        // check
        if (weakNotifs.keySet().size() != bufferSize) {
            throw new RuntimeException("The buffer size is not correctly specified."+
                   "\nExpected to be <= "+bufferSize+", but got "+weakNotifs.keySet().size());
        }

        server.stop();
        server = null;
    }

//--------------------------
// private classes
//--------------------------
    public static class NotificationEmitter extends NotificationBroadcasterSupport
        implements NotificationEmitterMBean {

        public void sendNotifications(Integer nb) {
            Notification notif;
            for (int i=1; i<=nb.intValue(); i++) {
                notif = new Notification("MyType", this, i);
                weakNotifs.put(notif, null);
                sendNotification(notif);
            }
        }
    }

    public interface NotificationEmitterMBean {
        public void sendNotifications(Integer nb);
    }

    private static final int toSend = 20;
    private static final int bufferSize = 10;
    private static WeakHashMap weakNotifs = new WeakHashMap(toSend);

}
