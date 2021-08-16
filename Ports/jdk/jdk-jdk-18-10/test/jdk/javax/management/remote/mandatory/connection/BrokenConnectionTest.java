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

/*
 * @test
 * @bug 4940957 8025205
 * @key intermittent
 * @summary Tests behaviour when connections break
 * @author Eamonn McManus
 *
 * @run clean BrokenConnectionTest
 * @run build BrokenConnectionTest
 * @run main BrokenConnectionTest
 */

import java.io.*;
import java.lang.reflect.*;
import java.nio.channels.ServerSocketChannel;
import java.net.*;
import java.rmi.server.*;
import java.util.*;

import java.rmi.UnmarshalException;

import javax.management.*;
import javax.management.remote.*;
import javax.management.remote.rmi.*;

// resolve ambiguity
import java.lang.reflect.Proxy;

public class BrokenConnectionTest {
    private static ObjectName DELEGATE_NAME;
    private static ObjectName BREAK_NAME;
    private static ObjectName LISTENER_NAME;
    public static void main(String[] args) throws Exception {
        DELEGATE_NAME =
            new ObjectName("JMImplementation:type=MBeanServerDelegate");
        BREAK_NAME = new ObjectName("test:type=Break");
        LISTENER_NAME = new ObjectName("test:type=Listener");

        String failed = "";

        final String[] protos = {"rmi", "jmxmp"};

        for (int i = 0; i < protos.length; i++) {
            final String proto = protos[i];
            System.out.println();
            System.out.println("------- Testing for " + proto + " -------");
            try {
                if (!test(proto))
                    failed += " " + proto;
            } catch (Exception e) {
                System.out.println("FAILED WITH EXCEPTION:");
                e.printStackTrace(System.out);
                failed += " " + proto;
            }
        }

        System.out.println();

        if (failed.length() > 0) {
            System.out.println("TEST FAILED FOR:" + failed);
            System.exit(1);
        }

        System.out.println("Test passed");
    }

    private static boolean test(String proto) throws Exception {
        if (proto.equals("rmi"))
            return rmiTest();
        else if (proto.equals("jmxmp"))
            return jmxmpTest();
        else
            throw new AssertionError(proto);
    }

    private static interface Breakable {
        public JMXConnectorServer createConnectorServer(MBeanServer mbs)
                throws IOException;
        public void setBroken(boolean broken);
    }

    private static interface TestAction {
        public String toString();
        public boolean test(MBeanServerConnection mbsc, Breakable breakable)
                throws Exception;
    }

    private static abstract class Operation implements TestAction {
        public String toString() {
            return opName() + ", break, " + opName();
        }
        void init(MBeanServerConnection mbsc) throws Exception {}
        abstract String opName();
        public boolean test(MBeanServerConnection mbsc, Breakable breakable)
                throws Exception {
            init(mbsc);
            operation(mbsc);
            System.out.println("Client ran " + opName() + " OK");
            breakable.setBroken(true);
            System.out.println("Broke connection, run " + opName() + " again");
            try {
                operation(mbsc);
                System.out.println("TEST FAILED: " + opName() +
                                   " should fail!");
                return false;
            } catch (IOException e) {
                System.out.println("Got IOException as expected (" + e + ")");
            }
            return true;
        }
        abstract void operation(MBeanServerConnection mbsc) throws Exception;
    }

    private static TestAction[] tests = {
        new Operation() {
            String opName() {
                return "getDefaultDomain";
            }
            void operation(MBeanServerConnection mbsc) throws Exception {
                mbsc.getDefaultDomain();
            }
        },
        new Operation() {
            String opName() {
                return "addNotificationListener(NL)";
            }
            void operation(MBeanServerConnection mbsc) throws Exception {
                mbsc.addNotificationListener(DELEGATE_NAME,
                                             new CountListener(), null, null);
            }
        },
        new Operation() {
            String opName() {
                return "addNotificationListener(MB)";
            }
            void init(MBeanServerConnection mbsc) throws Exception {
                mbsc.createMBean(CountListener.class.getName(),
                                 LISTENER_NAME);
            }
            void operation(MBeanServerConnection mbsc) throws Exception {
                mbsc.addNotificationListener(DELEGATE_NAME, LISTENER_NAME,
                                             null, null);
            }
        },
        new Operation() {
            String opName() {
                return "removeNotificationListener(NL)";
            }
            void init(MBeanServerConnection mbsc) throws Exception {
                for (int i = 0; i < NLISTENERS; i++) {
                    NotificationListener l = new CountListener();
                    mbsc.addNotificationListener(DELEGATE_NAME, l, null, null);
                    listeners.add(l);
                }
            }
            void operation(MBeanServerConnection mbsc) throws Exception {
                NotificationListener l = (NotificationListener)
                    listeners.remove(0);
                mbsc.removeNotificationListener(DELEGATE_NAME, l, null, null);
            }
            static final int NLISTENERS = 2;
            List/*<NotificationListener>*/ listeners = new ArrayList();
        },
        new Operation() {
            String opName() {
                return "removeNotificationListener(MB)";
            }
            void init(MBeanServerConnection mbsc) throws Exception {
                mbsc.createMBean(CountListener.class.getName(),
                                 LISTENER_NAME);
            }
            void operation(MBeanServerConnection mbsc) throws Exception {
                try {
                    mbsc.removeNotificationListener(DELEGATE_NAME,
                                                    LISTENER_NAME,
                                                    null, null);
                    throw new IllegalArgumentException("removeNL should not " +
                                                       "have worked!");
                } catch (ListenerNotFoundException e) {
                    // normal - there isn't one!
                }
            }
        },
        new Operation() {
            String opName() {
                return "createMBean(className, objectName)";
            }
            void operation(MBeanServerConnection mbsc) throws Exception {
                ObjectName name =
                    new ObjectName("test:instance=" + nextInstance());
                mbsc.createMBean(CountListener.class.getName(), name);
            }
            private synchronized int nextInstance() {
                return ++instance;
            }
            private int instance;
        },
        new Operation() {
            String opName() {
                return "getAttribute";
            }
            void operation(MBeanServerConnection mbsc) throws Exception {
                mbsc.getAttribute(DELEGATE_NAME, "ImplementationName");
            }
        },
        new Operation() {
            String opName() {
                return "getAttributes";
            }
            void operation(MBeanServerConnection mbsc) throws Exception {
                mbsc.getAttribute(DELEGATE_NAME, "ImplementationName");
            }
        },
        new Operation() {
            String opName() {
                return "getDomains";
            }
            void operation(MBeanServerConnection mbsc) throws Exception {
                mbsc.getDomains();
            }
        },
        new Operation() {
            String opName() {
                return "getMBeanCount";
            }
            void operation(MBeanServerConnection mbsc) throws Exception {
                mbsc.getMBeanCount();
            }
        },
        new Operation() {
            String opName() {
                return "getMBeanInfo";
            }
            void operation(MBeanServerConnection mbsc) throws Exception {
                mbsc.getMBeanInfo(DELEGATE_NAME);
            }
        },
        new Operation() {
            String opName() {
                return "getObjectInstance";
            }
            void operation(MBeanServerConnection mbsc) throws Exception {
                mbsc.getObjectInstance(DELEGATE_NAME);
            }
        },
        new Operation() {
            String opName() {
                return "invoke";
            }
            void operation(MBeanServerConnection mbsc) throws Exception {
                mbsc.invoke(BREAK_NAME, "doNothing", new Object[0],
                            new String[0]);
            }
        },
        new Operation() {
            String opName() {
                return "isInstanceOf";
            }
            void operation(MBeanServerConnection mbsc) throws Exception {
                mbsc.isInstanceOf(DELEGATE_NAME, "whatsit");
            }
        },
        new Operation() {
            String opName() {
                return "isRegistered";
            }
            void operation(MBeanServerConnection mbsc) throws Exception {
                mbsc.isRegistered(DELEGATE_NAME);
            }
        },
        new Operation() {
            String opName() {
                return "queryMBeans";
            }
            void operation(MBeanServerConnection mbsc) throws Exception {
                mbsc.queryMBeans(new ObjectName("*:*"), null);
            }
        },
        new Operation() {
            String opName() {
                return "queryNames";
            }
            void operation(MBeanServerConnection mbsc) throws Exception {
                mbsc.queryNames(new ObjectName("*:*"), null);
            }
        },
        new Operation() {
            String opName() {
                return "setAttribute";
            }
            void operation(MBeanServerConnection mbsc) throws Exception {
                mbsc.setAttribute(BREAK_NAME,
                                  new Attribute("Nothing", null));
            }
        },
        new Operation() {
            String opName() {
                return "setAttributes";
            }
            void operation(MBeanServerConnection mbsc) throws Exception {
                AttributeList attrs = new AttributeList();
                attrs.add(new Attribute("Nothing", null));
                mbsc.setAttributes(BREAK_NAME, attrs);
            }
        },
        new Operation() {
            String opName() {
                return "unregisterMBean";
            }
            void init(MBeanServerConnection mbsc) throws Exception {
                for (int i = 0; i < NBEANS; i++) {
                    ObjectName name = new ObjectName("test:instance=" + i);
                    mbsc.createMBean(CountListener.class.getName(), name);
                    names.add(name);
                }
            }
            void operation(MBeanServerConnection mbsc) throws Exception {
                ObjectName name = (ObjectName) names.remove(0);
                mbsc.unregisterMBean(name);
            }
            private static final int NBEANS = 2;
            private List/*<ObjectName>*/ names = new ArrayList();
        },
        new TestAction() {
            public String toString() {
                return "break during send for setAttribute";
            }
            public boolean test(MBeanServerConnection mbsc,
                                Breakable breakable) throws Exception {
                Attribute attr =
                    new Attribute("Break", new BreakWhenSerialized(breakable));
                try {
                    mbsc.setAttribute(BREAK_NAME, attr);
                    System.out.println("TEST FAILED: setAttribute with " +
                                       "BreakWhenSerializable did not fail!");
                    return false;
                } catch (IOException e) {
                    System.out.println("Got IOException as expected: " + e);

                    return true;
                }
            }
        },
        new TestAction() {
            public String toString() {
                return "break during receive for getAttribute";
            }
            public boolean test(MBeanServerConnection mbsc,
                                Breakable breakable) throws Exception {
                try {
                    mbsc.getAttribute(BREAK_NAME, "Break");
                    System.out.println("TEST FAILED: getAttribute of " +
                                       "BreakWhenSerializable did not fail!");
                    return false;
                } catch (IOException e) {
                    System.out.println("Got IOException as expected: " + e);

                    return true;
                }
            }
        },
    };

    public static interface BreakMBean {
        public BreakWhenSerialized getBreak();
        public void setBreak(BreakWhenSerialized x);
//      public void breakOnNotify();
        public void doNothing();
        public void setNothing(Object x);
    }

    public static class Break
            extends NotificationBroadcasterSupport implements BreakMBean {
        public Break(Breakable breakable) {
            this.breakable = breakable;
        }

        public BreakWhenSerialized getBreak() {
            return new BreakWhenSerialized(breakable);
        }

        public void setBreak(BreakWhenSerialized x) {
            throw new IllegalArgumentException("setBreak worked but " +
                                               "should not!");
        }

//      public void breakOnNotify() {
//          Notification broken = new Notification("type", "source", 0L);
//          broken.setUserData(new BreakWhenSerialized(breakable));
//          sendNotification(broken);
//      }

        public void doNothing() {}

        public void setNothing(Object x) {}

        private final Breakable breakable;
    }

    private static class BreakWhenSerialized implements Serializable {
        BreakWhenSerialized(Breakable breakable) {
            this.breakable = breakable;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            breakable.setBroken(true);
        }

        private final transient Breakable breakable;
    }

    private static class FailureNotificationFilter
            implements NotificationFilter {
        public boolean isNotificationEnabled(Notification n) {
            System.out.println("Filter: " + n + " (" + n.getType() + ")");

            final String failed =
                JMXConnectionNotification.FAILED;
            return (n instanceof JMXConnectionNotification
                    && n.getType().equals(JMXConnectionNotification.FAILED));
        }
    }

    public static interface CountListenerMBean {}

    public static class CountListener
            implements CountListenerMBean, NotificationListener {
        public synchronized void handleNotification(Notification n, Object h) {
            count++;
        }

        int count;
    }

    private static boolean test(Breakable breakable)
            throws Exception {
        boolean alreadyMissedFailureNotif = false;
        String failed = "";
        for (int i = 1; i <= tests.length; i++) {
            TestAction ta = tests[i - 1];
            System.out.println();
            System.out.println("Test " + i + ": " + ta);
            MBeanServer mbs = MBeanServerFactory.newMBeanServer();
            Break breakMBean = new Break(breakable);
            mbs.registerMBean(breakMBean, BREAK_NAME);
            JMXConnectorServer cs = breakable.createConnectorServer(mbs);
            System.out.println("Created and started connector server");
            JMXServiceURL addr = cs.getAddress();
            JMXConnector cc = JMXConnectorFactory.connect(addr);
            CountListener failureListener = new CountListener();
            NotificationFilter failureFilter = new FailureNotificationFilter();
            cc.addConnectionNotificationListener(failureListener,
                                                 failureFilter,
                                                 null);
            MBeanServerConnection mbsc = cc.getMBeanServerConnection();
            System.out.println("Client connected OK");
            boolean thisok = ta.test(mbsc, breakable);

            try {
                System.out.println("Stopping server");
                cs.stop();
            } catch (IOException e) {
                System.out.println("Ignoring exception on stop: " + e);
            }
            if (thisok) {
                System.out.println("Waiting for failure notif");
                // pass or test timeout. see 8025205
                do {
                    Thread.sleep(100);
                } while (failureListener.count < 1);

                Thread.sleep(1000); // if more notif coming ...
                if (failureListener.count > 1) {
                    System.out.println("Got too many failure notifs: " +
                                       failureListener.count);
                    thisok = false;
                }
            }
            if (!thisok)
                failed = failed + " " + i;
            System.out.println("Test " + i + (thisok ? " passed" : " FAILED"));
            breakable.setBroken(false);
        }
        if (failed.equals(""))
            return true;
        else {
            System.out.println("FAILING CASES:" + failed);
            return false;
        }
    }

    private static class BreakableRMI implements Breakable {
        public JMXConnectorServer createConnectorServer(MBeanServer mbs)
                throws IOException {
            JMXServiceURL url = new JMXServiceURL("rmi", null, 0);
            Map env = new HashMap();
            env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE,
                    brssf);
            JMXConnectorServer cs =
                JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
            cs.start();
            return cs;
        }

        public void setBroken(boolean broken) {
            brssf.setBroken(broken);
        }

        private final BreakableRMIServerSocketFactory brssf =
            new BreakableRMIServerSocketFactory();
    }

    private static boolean rmiTest() throws Exception {
        System.out.println("RMI broken connection test");
        Breakable breakable = new BreakableRMI();
        return test(breakable);
    }

    private static class BreakableRMIServerSocketFactory
            implements RMIServerSocketFactory {

        public synchronized ServerSocket createServerSocket(int port)
                throws IOException {
            if (broken)
                throw new IOException("ServerSocket has been broken");
            BreakableServerSocket bss = new BreakableServerSocket(port);
            bssList.add(bss);
            return bss;
        }

        synchronized void setBroken(boolean broken) {
            this.broken = broken;
//          System.out.println("BRSSF.setBroken(" + broken + ")");
            for (Iterator it = bssList.iterator(); it.hasNext(); ) {
                BreakableServerSocket bss = (BreakableServerSocket) it.next();
//              System.out.println((broken ? "" : "un") + "break " + bss);
                bss.setBroken(broken);
            }
        }

        private final List/*<BreakableServerSocket>*/ bssList =
            new ArrayList();
        private boolean broken = false;
    }

    private static class BreakableJMXMP implements Breakable {
        BreakableJMXMP() throws IOException {
            bss = new BreakableServerSocket(0);
        }

        public JMXConnectorServer createConnectorServer(MBeanServer mbs)
                throws IOException {
            try {
                InvocationHandler scsih =
                    new SocketConnectionServerInvocationHandler(bss);
                final String mcs =
                    "javax.management.remote.generic.MessageConnectionServer";
                final Class messageConnectionServerClass = Class.forName(mcs);
                final Class[] proxyInterfaces = {messageConnectionServerClass};
                Object socketConnectionServer =
                    Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                           proxyInterfaces,
                                           scsih);
                Map env = new HashMap();
                env.put("jmx.remote.message.connection.server",
                        socketConnectionServer);
                final String gcs =
                    "javax.management.remote.generic.GenericConnectorServer";
                final Class genericConnectorServerClass = Class.forName(gcs);
                final Class[] constrTypes = {Map.class, MBeanServer.class};
                final Constructor constr =
                    genericConnectorServerClass.getConstructor(constrTypes);
                JMXConnectorServer cs = (JMXConnectorServer)
                    constr.newInstance(new Object[] {env, mbs});
                cs.start();
                return cs;
            } catch (Exception e) {
                e.printStackTrace(System.out);
                throw new AssertionError(e);
            }
        }

        public void setBroken(boolean broken) {
            bss.setBroken(broken);
        }

        private final BreakableServerSocket bss;
    }

    private static boolean jmxmpTest() throws Exception {
        System.out.println("JMXMP broken connection test");
        try {
            Class.forName("javax.management.remote.generic.GenericConnector");
        } catch (ClassNotFoundException e) {
            System.out.println("Optional classes not present, skipping test");
            return true;
        }
        Breakable breakable = new BreakableJMXMP();
        return test(breakable);
    }

    private static class BreakableServerSocket extends ServerSocket {
        BreakableServerSocket(int port) throws IOException {
            super();
            ss = new ServerSocket(port);
        }

        synchronized void setBroken(boolean broken) {
            this.broken = broken;
//          System.out.println("BSS.setBroken(" + broken + ")");
            if (!broken)
                return;
            for (Iterator it = sList.iterator(); it.hasNext(); ) {
                Socket s = (Socket) it.next();
                try {
//                  System.out.println("Break: " + s);
                    s.close();
                } catch (IOException e) {
                    System.out.println("Unable to close socket: " + s +
                                       ", ignoring (" + e + ")");
                }
                it.remove();
            }
        }

        public void bind(SocketAddress endpoint) throws IOException {
            ss.bind(endpoint);
        }

        public void bind(SocketAddress endpoint, int backlog)
                throws IOException {
            ss.bind(endpoint, backlog);
        }

        public InetAddress getInetAddress() {
            return ss.getInetAddress();
        }

        public int getLocalPort() {
            return ss.getLocalPort();
        }

        public SocketAddress getLocalSocketAddress() {
            return ss.getLocalSocketAddress();
        }

        public Socket accept() throws IOException {
//          System.out.println("BSS.accept");
            Socket s = ss.accept();
//          System.out.println("BSS.accept returned: " + s);
            if (broken)
                s.close();
            else
                sList.add(s);
            return s;
        }

        public void close() throws IOException {
            ss.close();
        }

        public ServerSocketChannel getChannel() {
            return ss.getChannel();
        }

        public boolean isBound() {
            return ss.isBound();
        }

        public boolean isClosed() {
            return ss.isClosed();
        }

        public void setSoTimeout(int timeout) throws SocketException {
            ss.setSoTimeout(timeout);
        }

        public int getSoTimeout() throws IOException {
            return ss.getSoTimeout();
        }

        public void setReuseAddress(boolean on) throws SocketException {
            ss.setReuseAddress(on);
        }

        public boolean getReuseAddress() throws SocketException {
            return ss.getReuseAddress();
        }

        public String toString() {
            return "BreakableServerSocket wrapping " + ss.toString();
        }

        public void setReceiveBufferSize (int size) throws SocketException {
            ss.setReceiveBufferSize(size);
        }

        public int getReceiveBufferSize() throws SocketException {
            return ss.getReceiveBufferSize();
        }

        private final ServerSocket ss;
        private final List/*<Socket>*/ sList = new ArrayList();
        private boolean broken = false;
    }

    /* We do a lot of messy reflection stuff here because we don't
       want to reference the optional parts of the JMX Remote API in
       an environment (J2SE) where they won't be present.  */

    /* This class implements the logic that allows us to pretend that
       we have a class that looks like this:
       class SocketConnectionServer implements MessageConnectionServer {
           public MessageConnection accept() throws IOException {...}
           public JMXServiceURL getAddress() {...}
           public void start(Map env) throws IOException {...}
           public void stop() throws IOException {...}
       }
     */
    private static class SocketConnectionServerInvocationHandler
            implements InvocationHandler {
        SocketConnectionServerInvocationHandler(ServerSocket ss) {
            this.ss = ss;
        }

        public Object invoke(Object proxy, Method method, Object[] args)
                throws Exception {
            final String mname = method.getName();
            try {
                if (mname.equals("accept"))
                    return accept();
                else if (mname.equals("getAddress"))
                    return getAddress();
                else if (mname.equals("start"))
                    start((Map) args[0]);
                else if (mname.equals("stop"))
                    stop();
                else // probably a method inherited from Object
                    return method.invoke(this, args);
            } catch (InvocationTargetException ite) {
                Throwable t = ite.getCause();
                if (t instanceof IOException) {
                    throw (IOException)t;
                } else if (t instanceof RuntimeException) {
                    throw (RuntimeException)t;
                } else {
                    throw ite;
                }
            }

            return null;
        }

        private Object/*MessageConnection*/ accept() throws Exception {
            System.out.println("SCSIH.accept()");
            Socket s = ss.accept();
            Class socketConnectionClass =
                Class.forName("com.sun.jmx.remote.socket.SocketConnection");
            Constructor constr =
                socketConnectionClass.getConstructor(new Class[] {Socket.class});
            return constr.newInstance(new Object[] {s});
//          InvocationHandler scih = new SocketConnectionInvocationHandler(s);
//          Class messageConnectionClass =
//              Class.forName("javax.management.generic.MessageConnection");
//          return Proxy.newProxyInstance(this.getClass().getClassLoader(),
//                                        new Class[] {messageConnectionClass},
//                                        scih);
        }

        private JMXServiceURL getAddress() throws Exception {
            System.out.println("SCSIH.getAddress()");
            return new JMXServiceURL("jmxmp", null, ss.getLocalPort());
        }

        private void start(Map env) throws IOException {
            System.out.println("SCSIH.start(" + env + ")");
        }

        private void stop() throws IOException {
            System.out.println("SCSIH.stop()");
        }

        private final ServerSocket ss;
    }
}
