/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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


import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.util.HashMap;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;

import jdk.test.lib.Utils;

/*
 * @test
 * @bug 6697180
 * @summary test on a client notification deadlock.
 * @author Shanliang JIANG
 * @library /test/lib
 *
 * @run clean MultiThreadDeadLockTest
 * @run build MultiThreadDeadLockTest
 * @run main MultiThreadDeadLockTest
 */

public class MultiThreadDeadLockTest {

    private static long serverTimeout = Utils.adjustTimeout(500);

    public static void main(String[] args) throws Exception {
        print("Create the MBean server");
        MBeanServer mbs = MBeanServerFactory.createMBeanServer();

        print("Initialize environment map");
        HashMap env = new HashMap();

        print("Specify a client socket factory to control socket creation.");
        env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE,
                clientFactory);

        print("Specify a server idle timeout to make a server close an idle connection.");
        env.put("jmx.remote.x.server.connection.timeout", serverTimeout);

        print("Disable client heartbeat.");
        env.put("jmx.remote.x.client.connection.check.period", 0);

        env.put("jmx.remote.x.notification.fetch.timeout", serverTimeout);

        print("Create an RMI server");
        JMXServiceURL url = new JMXServiceURL("rmi", null, 0);
        JMXConnectorServer server =
                JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
        server.start();

        url = server.getAddress();

        print("Create jmx client on "+url);
        StateMachine.setState(CREATE_SOCKET); // allow to create client socket
        client = JMXConnectorFactory.connect(url, env);
        Thread.sleep(100);

        totoName = new ObjectName("default:name=toto");
        mbs.registerMBean(toto, totoName);
        print("Register the mbean: " + totoName);

        print("Add listener to toto MBean");
        client.getMBeanServerConnection().addNotificationListener(
                totoName, myListener, null, null);
        Thread.sleep(10);

        print("send notif, listener will block the fetcher");
        toto.sendNotif();
        Thread.sleep(100);

        StateMachine.setState(NO_OP);

        print("Sleep 3 times of server idle timeout: "+serverTimeout+
                ", the sever should close the idle connection.");
        Thread.sleep(serverTimeout*3);

        print("start the user thread to call mbean method, it will get IOexception" +
                " and start the reconnection, the socket factory will block the" +
                " socket creation.");
        UserThread ut = new UserThread();
        ut.start();
        Thread.sleep(10);

        print("Free the listener, the fetcher will get IO and makes " +
                "a deadlock if the bug is not fixed.");
        StateMachine.setState(FREE_LISTENER);
        Thread.sleep(100);

        print("Allow to create new socket for the reconnection");
        StateMachine.setState(CREATE_SOCKET);

        print("Check whether the user thread gets free to call the mbean.");
        if (!ut.waitDone(Utils.adjustTimeout(5000))) {
            throw new RuntimeException("Possible deadlock!");
        }

        print("Remove the listener.");
        client.getMBeanServerConnection().removeNotificationListener(
                totoName, myListener, null, null);
        Thread.sleep(serverTimeout*3);

        print("\nWell passed, bye!");

        client.close();
        Thread.sleep(10);
        server.stop();
    }

    private static ObjectName totoName = null;
    private static JMXConnector client;

    public static class UserThread extends Thread {
        public UserThread() {
            setDaemon(true);
        }

        public void run() {
            try {
                client.getMBeanServerConnection().invoke(
                        totoName, "allowReturn", null, null);
            } catch (Exception e) {
                throw new Error(e);
            }

            synchronized(UserThread.class) {
                done = true;
                UserThread.class.notify();
            }
        }

        public boolean waitDone(long timeout) {
            synchronized(UserThread.class) {
                if(!done) {
                    try {
                        UserThread.class.wait(timeout);
                    } catch (Exception e) {
                        throw new Error(e);
                    }
                }
            }
            return done;
        }

        private boolean done = false;
    }

    public static interface TotoMBean {
        public void allowReturn();
    }

    public static class Toto extends NotificationBroadcasterSupport
            implements TotoMBean {

        public void allowReturn() {
            enter("allowReturn");

            leave("allowReturn");
        }

        public void sendNotif() {
            enter("sendNotif");

            sendNotification(new Notification("Toto", totoName, 0));

            leave("sendNotif");
        }
    }
    private static Toto toto = new Toto();

    public static NotificationListener myListener = new NotificationListener() {
        public void handleNotification(Notification notification, Object handback) {
            enter("handleNotification");

            StateMachine.waitState(FREE_LISTENER);

            leave("handleNotification");
        }
    };

    public static class RMIClientFactory
            implements RMIClientSocketFactory, Serializable {

        public Socket createSocket(String host, int port) throws IOException {
            enter("createSocket");
            //print("Calling createSocket(" + host + " " + port + ")");

            StateMachine.waitState(CREATE_SOCKET);
            Socket s = new Socket(host, port);
            leave("createSocket");

            return s;
        }
    }
    private static RMIClientFactory clientFactory = new RMIClientFactory();

    private static int CREATE_SOCKET = 1;
    private static int FREE_LISTENER = 3;
    private static int NO_OP = 0;

    public static class StateMachine {

        private static int state = NO_OP;
        private static int[] lock = new int[0];

        public static void waitState(int s) {
            synchronized (lock) {
                while (state != s) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ire) {
                        // should not
                        throw new Error(ire);
                    }
                }
            }
        }

        public static int getState() {
            synchronized (lock) {
                return state;
            }
        }

        public static void setState(int s) {
            synchronized (lock) {
                state = s;
                lock.notifyAll();
            }
        }
    }

    private static void print(String m) {
        System.out.println(m);
    }

    private static void enter(String m) {
        System.out.println("\n---Enter the method " + m);
    }

    private static void leave(String m) {
        System.out.println("===Leave the method: " + m);
    }
}
