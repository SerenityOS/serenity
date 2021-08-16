/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4898478
 * @summary Tests client default class loader used before JSR 160 loader
 * @author Eamonn McManus
 *
 * @library /test/lib
 *
 * @run clean MethodResultTest
 * @run build MethodResultTest
 * @run main MethodResultTest
 */

import java.io.*;
import java.nio.file.Paths;
import java.net.*;
import java.util.*;
import javax.management.*;
import javax.management.remote.*;
import jdk.test.lib.Utils;

/*
   This test checks that the class loader that is used to deserialize
   the return values from remote MBean server operations is indeed the
   one specified by the user.  The only MBean server operations that
   matter are those than can return an arbitrary Object.  We don't
   care about getMBeanCount or queryNames or whatever because their
   return values are always of classes loaded by the bootstrap loader.
   But for the operations getAttribute, getAttributes, setAttributes,
   and invoke, the return value can include any Java class.  This is
   also true of getMBeanInfo, since the return value can be an exotic
   subclass of MBeanInfo, or a ModelMBeanInfo that refers to an
   arbitrary Object.  The JMX Remote API spec requires that these
   return values be deserialized using the class loader supplied by
   the user (default is context class loader).  In particular it must
   not be deserialized using the system class loader, which it will be
   with RMI unless special precautions are taken.
 */
public class MethodResultTest {
    public static void main(String[] args) throws Exception {
        Class<?> thisClass = MethodResultTest.class;
        Class<?> exoticClass = Exotic.class;
        String exoticClassName = Exotic.class.getName();

        String[] cpaths = System.getProperty("test.classes", ".")
                                .split(File.pathSeparator);
        URL[] urls = new URL[cpaths.length];
        for (int i=0; i < cpaths.length; i++) {
            urls[i] = Paths.get(cpaths[i]).toUri().toURL();
        }

        ClassLoader shadowLoader =
            new ShadowLoader(urls, thisClass.getClassLoader(),
                             new String[] {exoticClassName,
                                           ExoticMBeanInfo.class.getName(),
                                           ExoticException.class.getName()});
        Class<?> cl = shadowLoader.loadClass(exoticClassName);
        if (cl == exoticClass) {
            System.out.println("TEST INVALID: Shadow class loader loaded " +
                               "same class as test class loader");
            System.exit(1);
        }
        Thread.currentThread().setContextClassLoader(shadowLoader);

        ObjectName on = new ObjectName("a:b=c");
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        mbs.createMBean(Thing.class.getName(), on);

        final String[] protos = {"rmi", "iiop", "jmxmp"};

        boolean ok = true;
        for (int i = 0; i < protos.length; i++) {
            try {
                ok &= test(protos[i], mbs, on);
                System.out.println();
            } catch (Exception e) {
                System.out.println("TEST FAILED WITH EXCEPTION:");
                e.printStackTrace(System.out);
                ok = false;
            }
        }

        if (ok)
            System.out.println("Test passed");
        else {
            System.out.println("TEST FAILED");
            System.exit(1);
        }
    }

    private static boolean test(String proto, MBeanServer mbs, ObjectName on)
            throws Exception {
        System.out.println("Testing for protocol " + proto);

        JMXConnectorServer cs;
        JMXServiceURL url = new JMXServiceURL(proto, null, 0);
        try {
            cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null,
                                                                 mbs);
        } catch (MalformedURLException e) {
            System.out.println("System does not recognize URL: " + url +
                               "; ignoring");
            return true;
        }
        cs.start();
        JMXServiceURL addr = cs.getAddress();
        JMXConnector client = connect(addr);
        MBeanServerConnection mbsc = client.getMBeanServerConnection();
        Object getAttributeExotic = mbsc.getAttribute(on, "Exotic");
        AttributeList getAttrs =
            mbsc.getAttributes(on, new String[] {"Exotic"});
        AttributeList setAttrs = new AttributeList();
        setAttrs.add(new Attribute("Exotic", new Exotic()));
        setAttrs = mbsc.setAttributes(on, setAttrs);
        Object invokeExotic =
            mbsc.invoke(on, "anExotic", new Object[] {}, new String[] {});
        MBeanInfo exoticMBI = mbsc.getMBeanInfo(on);

        mbsc.setAttribute(on, new Attribute("Exception", Boolean.TRUE));
        Exception
            getAttributeException, setAttributeException, invokeException;
        try {
            try {
                mbsc.getAttribute(on, "Exotic");
                throw noException("getAttribute");
            } catch (Exception e) {
                getAttributeException = e;
            }
            try {
                mbsc.setAttribute(on, new Attribute("Exotic", new Exotic()));
                throw noException("setAttribute");
            } catch (Exception e) {
                setAttributeException = e;
            }
            try {
                mbsc.invoke(on, "anExotic", new Object[] {}, new String[] {});
                throw noException("invoke");
            } catch (Exception e) {
                invokeException = e;
            }
        } finally {
            mbsc.setAttribute(on, new Attribute("Exception", Boolean.FALSE));
        }
        client.close();
        cs.stop();

        boolean ok = true;

        ok &= checkAttrs("getAttributes", getAttrs);
        ok &= checkAttrs("setAttributes", setAttrs);

        ok &= checkType("getAttribute", getAttributeExotic, Exotic.class);
        ok &= checkType("getAttributes", attrValue(getAttrs), Exotic.class);
        ok &= checkType("setAttributes", attrValue(setAttrs), Exotic.class);
        ok &= checkType("invoke", invokeExotic, Exotic.class);
        ok &= checkType("getMBeanInfo", exoticMBI, ExoticMBeanInfo.class);

        ok &= checkExceptionType("getAttribute", getAttributeException,
                                 ExoticException.class);
        ok &= checkExceptionType("setAttribute", setAttributeException,
                                 ExoticException.class);
        ok &= checkExceptionType("invoke", invokeException,
                                 ExoticException.class);

        if (ok)
            System.out.println("Test passes for protocol " + proto);
        return ok;
    }

    private static JMXConnector connect(JMXServiceURL addr) {
        final long timeout = Utils.adjustTimeout(100);

        JMXConnector connector = null;
        while (connector == null) {
            try {
                connector = JMXConnectorFactory.connect(addr);
            } catch (IOException e) {
                System.out.println("Connection error. Retrying after delay...");
                delay(timeout);
            } catch (Exception otherException) {
                System.out.println("Unexpected exception while connecting " + otherException);
                throw new RuntimeException(otherException);
            }
        }
        return connector;
    }

    private static void delay(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Exception noException(String what) {
        final String msg =
            "Operation " + what + " returned when exception expected";
        return new IllegalStateException(msg);
    }

    private static Object attrValue(AttributeList attrs) {
        return ((Attribute) attrs.get(0)).getValue();
    }

    private static boolean checkType(String what, Object object,
                                     Class<?> wrongClass) {
        return checkType(what, object, wrongClass, false);
    }

    private static boolean checkType(String what, Object object,
                                     Class<?> wrongClass, boolean isException) {
        final String type = isException ? "exception" : "object";
        final String rendered = isException ? "thrown" : "returned";
        System.out.println("For " + type + " " + rendered + " by " + what +
                           ":");
        if (wrongClass.isInstance(object)) {
            System.out.println("TEST FAILS: " + type + " loaded by test " +
                               "classloader");
            return false;
        }
        String className = object.getClass().getName();
        if (!className.equals(wrongClass.getName())) {
            System.out.println("TEST FAILS: " + rendered + " " + type +
                               " has wrong class name: " + className);
            return false;
        }
        System.out.println("Test passes: " + rendered + " " + type +
                           " has same class name but is not same class");
        return true;
    }

    private static boolean checkExceptionType(String what, Exception exception,
                                              Class<?> wrongClass) {
        if (!(exception instanceof MBeanException)) {
            System.out.println("Exception thrown by " + what + " is not an " +
                               MBeanException.class.getName() +
                               ":");
            exception.printStackTrace(System.out);
            return false;
        }

        exception = ((MBeanException) exception).getTargetException();

        return checkType(what, exception, wrongClass, true);
    }

    private static boolean checkAttrs(String what, AttributeList attrs) {
        if (attrs.size() != 1) {
            System.out.println("TEST FAILS: list returned by " + what +
                               " does not have size 1: " + attrs);
            return false;
        }
        Attribute attr = (Attribute) attrs.get(0);
        if (!"Exotic".equals(attr.getName())) {
            System.out.println("TEST FAILS: " + what + " returned wrong " +
                               "attribute: " + attr);
            return false;
        }

        return true;
    }

    public static class Thing
            extends StandardMBean implements ThingMBean {
        public Thing() throws NotCompliantMBeanException {
            super(ThingMBean.class);
        }

        public Exotic getExotic() throws ExoticException {
            if (exception)
                throw new ExoticException();
            return new Exotic();
        }

        public void setExotic(Exotic x) throws ExoticException {
            if (exception)
                throw new ExoticException();
        }

        public Exotic anExotic() throws ExoticException {
            if (exception)
                throw new ExoticException();
            return new Exotic();
        }

        public void cacheMBeanInfo(MBeanInfo mbi) {
            if (mbi != null)
                mbi = new ExoticMBeanInfo(mbi);
            super.cacheMBeanInfo(mbi);
        }

        public void setException(boolean x) {
            this.exception = x;
        }

        private boolean exception;
    }

    public static interface ThingMBean {
        public Exotic getExotic() throws ExoticException;
        public void setExotic(Exotic x) throws ExoticException;
        public Exotic anExotic() throws ExoticException;
        public void setException(boolean x);
    }

    public static class Exotic implements Serializable {}

    public static class ExoticException extends Exception {}

    public static class ExoticMBeanInfo extends MBeanInfo {
        public ExoticMBeanInfo(MBeanInfo mbi) {
            super(mbi.getClassName(),
                  mbi.getDescription(),
                  mbi.getAttributes(),
                  mbi.getConstructors(),
                  mbi.getOperations(),
                  mbi.getNotifications());
        }
    }

    private static class ShadowLoader extends URLClassLoader {
        ShadowLoader(URL[] urls, ClassLoader realLoader,
                     String[] shadowClassNames) {
            super(urls, null);
            this.realLoader = realLoader;
            this.shadowClassNames = Arrays.asList(shadowClassNames);
        }

        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (shadowClassNames.contains(name))
                return super.findClass(name);
            else
                return realLoader.loadClass(name);
        }

        private final ClassLoader realLoader;
        private final List shadowClassNames;
    }
}
