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

/*
 * @test
 * @bug 6175517 6304996
 * @summary General MXBean test: createMBean, registerMBean, immutableInfo,
 *          interfaceClassName, openType, originalType, StandardMBean,
 *          StandardEmitterMBean.
 * @author Luis-Miguel Alventosa
 *
 * @run clean MiscTest
 * @run build MiscTest
 * @run main MiscTest
 */

import java.io.*;
import java.lang.management.*;
import javax.management.*;
import javax.management.openmbean.*;

public class MiscTest {

    private static final MBeanNotificationInfo notifs[] =
        new MBeanNotificationInfo[] {
          new MBeanNotificationInfo(
            new String[] {AttributeChangeNotification.ATTRIBUTE_CHANGE},
            AttributeChangeNotification.class.getName(),
            "This notification is emitted when the reset() method is called.")
    };

    private static Class<?> testClasses[] = {
        Test11.class, Test12.class,
        Test21.class, Test22.class,
        Test31.class, Test32.class,
        Test33.class, Test34.class,
        Test41.class, Test42.class,
        Test43.class, Test44.class,
    };

    private static Class<?> testIntfs[] = {
        Test11MBean.class, Test12MBean.class,
        Test21MXBean.class, Test22MXBean.class,
        Test31SMB.class, Test32SMB.class,
        Test33SMB.class, Test34SMB.class,
        Test41SMX.class, Test42SMX.class,
        Test43SMX.class, Test44SMX.class,
    };

    public interface SuperInterface {
        public String getState();
        public void setState(String s);
        public int getNbChanges();
        public void reset();
        public void close(boolean force);
        public MemoryUsage getMemoryUsage();
    }

    public static class BaseTest {

        public String getState() {
            return state;
        }

        public void setState(String s) {
            state = s;
            nbChanges++;
        }

        public int getNbChanges() {
            return nbChanges;
        }

        public void reset() {
            state = "initial state";
            nbChanges = 0;
            nbResets++;
        }

        public String getName() {
            return "name";
        }

        public void setName(String s) {
        }

        public void close(boolean force) {
        }

        public MemoryUsage getMemoryUsage() {
            return new MemoryUsage(10, 20, 30, 40);
        }

        public int getNbResets() {
            return nbResets;
        }

        private String state = "initial state";
        private int nbChanges = 0;
        private int nbResets = 0;
    }

    public static class BaseEmitterTest
        extends NotificationBroadcasterSupport {

        public String getState() {
            return state;
        }

        public void setState(String s) {
            state = s;
            nbChanges++;
        }

        public int getNbChanges() {
            return nbChanges;
        }

        public void reset() {
            state = "initial state";
            nbChanges = 0;
            nbResets++;
        }

        public String getName() {
            return "name";
        }

        public void setName(String s) {
        }

        public void close(boolean force) {
        }

        public MemoryUsage getMemoryUsage() {
            return new MemoryUsage(10, 20, 30, 40);
        }

        public int getNbResets() {
            return nbResets;
        }

        public MBeanNotificationInfo[] getNotificationInfo() {
            return notifs;
        }

        private String state = "initial state";
        private int nbChanges = 0;
        private int nbResets = 0;
    }

    public static interface Test11MBean extends SuperInterface {
    }

    public static interface Test12MBean extends SuperInterface {
    }

    public static interface Test21MXBean extends SuperInterface {
    }

    public static interface Test22MXBean extends SuperInterface {
    }

    public static interface Test31SMB extends SuperInterface {
    }

    public static interface Test32SMB extends SuperInterface {
    }

    public static interface Test33SMB extends SuperInterface {
    }

    public static interface Test34SMB extends SuperInterface {
    }

    public static interface Test41SMX extends SuperInterface {
    }

    public static interface Test42SMX extends SuperInterface {
    }

    public static interface Test43SMX extends SuperInterface {
    }

    public static interface Test44SMX extends SuperInterface {
    }

    public static class Test11 extends BaseTest
        implements Test11MBean {
    }

    public static class Test12 extends BaseEmitterTest
        implements Test12MBean {
    }

    public static class Test21 extends BaseTest
        implements Test21MXBean {
    }

    public static class Test22 extends BaseEmitterTest
        implements Test22MXBean {
    }

    public static class Test31 extends BaseTest
        implements Test31SMB {
    }

    public static class Test32 extends BaseEmitterTest
        implements Test32SMB {
    }

    public static class Test33 extends StandardMBean
        implements Test33SMB {

        public Test33() {
            super(Test33SMB.class, false);
        }

        public String getState() {
            return state;
        }

        public void setState(String s) {
            state = s;
            nbChanges++;
        }

        public int getNbChanges() {
            return nbChanges;
        }

        public void reset() {
            state = "initial state";
            nbChanges = 0;
            nbResets++;
        }

        public String getName() {
            return "name";
        }

        public void setName(String s) {
        }

        public void close(boolean force) {
        }

        public MemoryUsage getMemoryUsage() {
            return new MemoryUsage(10, 20, 30, 40);
        }

        public int getNbResets() {
            return nbResets;
        }

        private String state = "initial state";
        private int nbChanges = 0;
        private int nbResets = 0;
    }

    public static class Test34 extends StandardEmitterMBean
        implements Test34SMB {

        public Test34() {
            super(Test34SMB.class, false,
                  new NotificationBroadcasterSupport(notifs));
        }

        public String getState() {
            return state;
        }

        public void setState(String s) {
            state = s;
            nbChanges++;
        }

        public int getNbChanges() {
            return nbChanges;
        }

        public void reset() {
            state = "initial state";
            nbChanges = 0;
            nbResets++;
        }

        public String getName() {
            return "name";
        }

        public void setName(String s) {
        }

        public void close(boolean force) {
        }

        public MemoryUsage getMemoryUsage() {
            return new MemoryUsage(10, 20, 30, 40);
        }

        public int getNbResets() {
            return nbResets;
        }

        private String state = "initial state";
        private int nbChanges = 0;
        private int nbResets = 0;
    }

    public static class Test41 extends BaseTest
        implements Test41SMX {
    }

    public static class Test42 extends BaseEmitterTest
        implements Test42SMX {
    }

   public static class Test43 extends StandardMBean
        implements Test43SMX {

        public Test43() {
            super(Test43SMX.class, true);
        }

        public String getState() {
            return state;
        }

        public void setState(String s) {
            state = s;
            nbChanges++;
        }

        public int getNbChanges() {
            return nbChanges;
        }

        public void reset() {
            state = "initial state";
            nbChanges = 0;
            nbResets++;
        }

        public String getName() {
            return "name";
        }

        public void setName(String s) {
        }

        public void close(boolean force) {
        }

        public MemoryUsage getMemoryUsage() {
            return new MemoryUsage(10, 20, 30, 40);
        }

        public int getNbResets() {
            return nbResets;
        }

        private String state = "initial state";
        private int nbChanges = 0;
        private int nbResets = 0;
    }

    public static class Test44 extends StandardEmitterMBean
        implements Test44SMX {

        public Test44() {
            super(Test44SMX.class, true,
                  new NotificationBroadcasterSupport(notifs));
        }

        public String getState() {
            return state;
        }

        public void setState(String s) {
            state = s;
            nbChanges++;
        }

        public int getNbChanges() {
            return nbChanges;
        }

        public void reset() {
            state = "initial state";
            nbChanges = 0;
            nbResets++;
        }

        public String getName() {
            return "name";
        }

        public void setName(String s) {
        }

        public void close(boolean force) {
        }

        public MemoryUsage getMemoryUsage() {
            return new MemoryUsage(10, 20, 30, 40);
        }

        public int getNbResets() {
            return nbResets;
        }

        private String state = "initial state";
        private int nbChanges = 0;
        private int nbResets = 0;
    }

    public static void main(String[] args) throws Exception {
        // Instantiate the MBean server
        //
        echo("\n>>> Create the MBean server");
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        // Get default domain
        //
        echo("\n>>> Get the MBean server's default domain");
        String domain = mbs.getDefaultDomain();
        echo("\tDefault Domain = " + domain);

        for (int i = 0; i < testClasses.length; i++) {
            // Create and register the Test MBean
            //
            String cn = testClasses[i].getName();
            String ons = domain + ":type=" + cn;
            echo("\n>>> Create the " + cn +
                 " MBean within the MBeanServer");
            echo("\tObjectName = " + ons);
            ObjectName on = ObjectName.getInstance(ons);
            if (testClasses[i] == Test31.class ||
                testClasses[i] == Test41.class) {
                StandardMBean s = new StandardMBean(
                                      testClasses[i].newInstance(),
                                      (Class) testIntfs[i],
                                      testClasses[i] == Test41.class);
                mbs.registerMBean(s, on);
            } else if (testClasses[i] == Test32.class ||
                       testClasses[i] == Test42.class) {
                Object o = testClasses[i].newInstance();
                StandardEmitterMBean s = new StandardEmitterMBean(
                               o,
                               (Class) testIntfs[i],
                               testClasses[i] == Test42.class,
                               (NotificationEmitter) o);
                mbs.registerMBean(s, on);
            } else {
                mbs.createMBean(cn, on);
            }

            // Check notifs
            //
            MBeanInfo mbi = mbs.getMBeanInfo(on);
            MBeanNotificationInfo mbni[] = mbi.getNotifications();
            if (i % 2 == 0) {
                if (mbni.length != 0) {
                    throw new IllegalArgumentException(
                    "Should not be a NotificationEmitter");
                }
            } else {
                if (mbni.length != 1) {
                    throw new IllegalArgumentException(
                    "Should not a NotificationEmitter with one notification");
                }
            }
            // Manage the Test MBean
            //
            manageMBean(mbs, on, cn);
        }
    }

    private static void manageMBean(MBeanServer mbs,
                                    ObjectName on,
                                    String cn)
        throws Exception {

        echo("\n>>> Manage the " + cn +
             " MBean using its attributes ");
        echo("    and operations exposed for management");

        // Get attribute values
        printAttributes(mbs, on);

        // Change State attribute
        echo("\n    Setting State attribute to value \"new state\"...");
        Attribute stateAttribute = new Attribute("State","new state");
        mbs.setAttribute(on, stateAttribute);

        // Get attribute values
        printAttributes(mbs, on);

        // Invoking reset operation
        echo("\n    Invoking reset operation...");
        mbs.invoke(on, "reset", null, null);

        // Invoking close operation
        echo("\n    Invoking close operation...");
        String type = on.getKeyProperty("type");
        String signature[] = {"boolean"};
        mbs.invoke(on, "close", new Object[] {true}, signature);

        // Get attribute values
        printAttributes(mbs, on);

        // Create proxy
        if (type.equals(Test11.class.getName())) {
            Test11MBean p = JMX.newMBeanProxy(mbs,
                                              on,
                                              Test11MBean.class);
            // Get attribute values
            echo("\n    Getting attribute values through proxies:");
            echo("\tState       = \"" + p.getState() + "\"");
            echo("\tNbChanges   = " + p.getNbChanges());
            echo("\tMemoryUsage = " + p.getMemoryUsage());
            checkDescriptor(mbs, on, "true", Test11MBean.class.getName());
        } else if (type.equals(Test12.class.getName())) {
            Test12MBean p = JMX.newMBeanProxy(mbs,
                                              on,
                                              Test12MBean.class,
                                              true);
            // Get attribute values
            echo("\n    Getting attribute values through proxies:");
            echo("\tState       = \"" + p.getState() + "\"");
            echo("\tNbChanges   = " + p.getNbChanges());
            echo("\tMemoryUsage = " + p.getMemoryUsage());
            checkDescriptor(mbs, on, "false", Test12MBean.class.getName());
        } else if (type.equals(Test21.class.getName())) {
            Test21MXBean p = JMX.newMXBeanProxy(mbs,
                                                on,
                                                Test21MXBean.class);
            // Get attribute values
            echo("\n    Getting attribute values through proxies:");
            echo("\tState       = \"" + p.getState() + "\"");
            echo("\tNbChanges   = " + p.getNbChanges());
            echo("\tMemoryUsage = " + p.getMemoryUsage());
            checkDescriptor(mbs, on, "true", Test21MXBean.class.getName());
        } else if (type.equals(Test22.class.getName())) {
            Test22MXBean p = JMX.newMXBeanProxy(mbs,
                                                on,
                                                Test22MXBean.class,
                                                true);
            // Get attribute values
            echo("\n    Getting attribute values through proxies:");
            echo("\tState       = \"" + p.getState() + "\"");
            echo("\tNbChanges   = " + p.getNbChanges());
            echo("\tMemoryUsage = " + p.getMemoryUsage());
            checkDescriptor(mbs, on, "true", Test22MXBean.class.getName());
        } else if (type.equals(Test31.class.getName())) {
            Test31SMB p = JMX.newMBeanProxy(mbs,
                                            on,
                                            Test31SMB.class);
            // Get attribute values
            echo("\n    Getting attribute values through proxies:");
            echo("\tState       = \"" + p.getState() + "\"");
            echo("\tNbChanges   = " + p.getNbChanges());
            echo("\tMemoryUsage = " + p.getMemoryUsage());
            checkDescriptor(mbs, on, "true", Test31SMB.class.getName());
        } else if (type.equals(Test32.class.getName())) {
            Test32SMB p = JMX.newMBeanProxy(mbs,
                                            on,
                                            Test32SMB.class,
                                            true);
            // Get attribute values
            echo("\n    Getting attribute values through proxies:");
            echo("\tState       = \"" + p.getState() + "\"");
            echo("\tNbChanges   = " + p.getNbChanges());
            echo("\tMemoryUsage = " + p.getMemoryUsage());
            checkDescriptor(mbs, on, "true", Test32SMB.class.getName());
        } else if (type.equals(Test33.class.getName())) {
            Test33SMB p = JMX.newMBeanProxy(mbs,
                                            on,
                                            Test33SMB.class,
                                            true);
            // Get attribute values
            echo("\n    Getting attribute values through proxies:");
            echo("\tState       = \"" + p.getState() + "\"");
            echo("\tNbChanges   = " + p.getNbChanges());
            echo("\tMemoryUsage = " + p.getMemoryUsage());
            checkDescriptor(mbs, on, "true", Test33SMB.class.getName());
        } else if (type.equals(Test34.class.getName())) {
            Test34SMB p = JMX.newMBeanProxy(mbs,
                                            on,
                                            Test34SMB.class,
                                            true);
            // Get attribute values
            echo("\n    Getting attribute values through proxies:");
            echo("\tState       = \"" + p.getState() + "\"");
            echo("\tNbChanges   = " + p.getNbChanges());
            echo("\tMemoryUsage = " + p.getMemoryUsage());
            checkDescriptor(mbs, on, "true", Test34SMB.class.getName());
        } else if (type.equals(Test41.class.getName())) {
            Test41SMX p = JMX.newMXBeanProxy(mbs,
                                             on,
                                             Test41SMX.class);
            // Get attribute values
            echo("\n    Getting attribute values through proxies:");
            echo("\tState       = \"" + p.getState() + "\"");
            echo("\tNbChanges   = " + p.getNbChanges());
            echo("\tMemoryUsage = " + p.getMemoryUsage());
            checkDescriptor(mbs, on, "true", Test41SMX.class.getName());
        } else if (type.equals(Test42.class.getName())) {
            Test42SMX p = JMX.newMXBeanProxy(mbs,
                                             on,
                                             Test42SMX.class,
                                             true);
            // Get attribute values
            echo("\n    Getting attribute values through proxies:");
            echo("\tState       = \"" + p.getState() + "\"");
            echo("\tNbChanges   = " + p.getNbChanges());
            echo("\tMemoryUsage = " + p.getMemoryUsage());
            checkDescriptor(mbs, on, "true", Test42SMX.class.getName());
        } else if (type.equals(Test43.class.getName())) {
            Test43SMX p = JMX.newMXBeanProxy(mbs,
                                             on,
                                             Test43SMX.class);
            // Get attribute values
            echo("\n    Getting attribute values through proxies:");
            echo("\tState       = \"" + p.getState() + "\"");
            echo("\tNbChanges   = " + p.getNbChanges());
            echo("\tMemoryUsage = " + p.getMemoryUsage());
            checkDescriptor(mbs, on, "true", Test43SMX.class.getName());
        } else if (type.equals(Test44.class.getName())) {
            Test44SMX p = JMX.newMXBeanProxy(mbs,
                                             on,
                                             Test44SMX.class,
                                             true);
            // Get attribute values
            echo("\n    Getting attribute values through proxies:");
            echo("\tState       = \"" + p.getState() + "\"");
            echo("\tNbChanges   = " + p.getNbChanges());
            echo("\tMemoryUsage = " + p.getMemoryUsage());
            checkDescriptor(mbs, on, "true", Test44SMX.class.getName());
        } else {
            throw new IllegalArgumentException("Invalid MBean type");
        }
    }

    private static void printAttributes(MBeanServer mbs,
                                        ObjectName on)
        throws Exception {
        echo("\n    Getting attribute values:");
        String state = (String) mbs.getAttribute(on, "State");
        Integer nbChanges = (Integer) mbs.getAttribute(on,"NbChanges");
        echo("\tState     = \"" + state + "\"");
        echo("\tNbChanges = " + nbChanges);
        String type = on.getKeyProperty("type");
        if (type.indexOf("Test2") != -1 || type.indexOf("Test4") != -1) {
            CompositeData memoryUsage =
                (CompositeData) mbs.getAttribute(on, "MemoryUsage");
            echo("\tMemoryUsage = " + memoryUsage);
        } else {
            MemoryUsage memoryUsage =
                (MemoryUsage) mbs.getAttribute(on, "MemoryUsage");
            echo("\tMemoryUsage = " + memoryUsage);
        }
    }

    public static void checkDescriptor(MBeanServer mbs,
                                       ObjectName on,
                                       String immutable,
                                       String intf)
            throws Exception {

        MBeanInfo mbi = mbs.getMBeanInfo(on);

        Descriptor d = mbi.getDescriptor();
        if (d == null || d.getFieldNames().length == 0)
            throw new IllegalArgumentException("Empty descriptor");
        if (!d.getFieldValue("immutableInfo").equals(immutable)) {
            final String msg =
                "Bad descriptor: expected immutableInfo=" + immutable + ": " + d;
            throw new IllegalArgumentException(msg);
        }
        if (!d.getFieldValue("interfaceClassName").equals(intf)) {
            final String msg =
                "Bad descriptor: expected interfaceClassName=" + intf + ": " + d;
            throw new IllegalArgumentException(msg);
        }

        if (intf.indexOf("MX") != -1) {
            MBeanAttributeInfo attrs[] = mbi.getAttributes();
            if (attrs == null || attrs.length == 0)
                throw new IllegalArgumentException("No attributes");
            boolean nbChangesFound = false;
            for (MBeanAttributeInfo attr : attrs) {
                if (attr.getName().equals("NbChanges")) {
                    nbChangesFound = true;
                    Descriptor ad = attr.getDescriptor();
                    OpenType<?> opty = (OpenType<?>)
                        ad.getFieldValue("openType");
                    if (!opty.equals(SimpleType.INTEGER)) {
                        throw new IllegalArgumentException("Open type should " +
                                                           "be INTEGER: " + opty);
                    }
                    String orty =
                        (String) ad.getFieldValue("originalType");
                    if (!orty.equals(Integer.TYPE.getName())) {
                        throw new IllegalArgumentException("Orig type should " +
                                                           "be int: " + orty);
                    }
                }
            }
            if (!nbChangesFound)
                throw new IllegalArgumentException("Did not find NbChanges");
        }
    }

    private static void echo(String msg) {
        System.out.println(msg);
    }
}
