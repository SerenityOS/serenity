/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6448042
 * @summary Test that MXBeans can define their own names in preRegister
 * @author Eamonn McManus
 */

import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.StandardMBean;

/*
 * Test that an MXBean can decide its name by returning a value from
 * the preRegister method.  Also test the same thing for Standard MBeans
 * for good measure.
 */
public class PreRegisterNameTest {
    public static interface SpumeMXBean {}

    public static class Spume implements SpumeMXBean, MBeanRegistration {
        private ObjectName realName;

        public Spume(ObjectName realName) {
            this.realName = realName;
        }

        public void preDeregister() throws Exception {
        }

        public void postDeregister() {
        }

        public void postRegister(Boolean registrationDone) {
        }

        public ObjectName preRegister(MBeanServer server, ObjectName name) {
            return realName;
        }
    }

    public static interface ThingMBean {
        public boolean getNoddy();
    }

    public static class Thing implements ThingMBean, MBeanRegistration {
        private ObjectName realName;

        public Thing(ObjectName realName) {
            this.realName = realName;
        }

        public ObjectName preRegister(MBeanServer mbs, ObjectName name) {
            return realName;
        }

        public void postRegister(Boolean done) {}

        public void preDeregister() {}

        public void postDeregister() {}

        public boolean getNoddy() {
            return true;
        }
    }

    public static class XThing extends StandardMBean implements ThingMBean {
        private ObjectName realName;

        public XThing(ObjectName realName) {
            super(ThingMBean.class, false);
            this.realName = realName;
        }

        @Override
        public ObjectName preRegister(MBeanServer server, ObjectName name) {
            return realName;
        }

        public boolean getNoddy() {
            return false;
        }
    }

    public static class XSpume extends StandardMBean implements SpumeMXBean {
        private ObjectName realName;

        public XSpume(ObjectName realName) {
            super(SpumeMXBean.class, true);
            this.realName = realName;
        }

        @Override
        public ObjectName preRegister(MBeanServer server, ObjectName name)
        throws Exception {
            super.preRegister(server, realName);
            return realName;
        }
    }

    public static void main(String[] args) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        for (Class<?> c : new Class<?>[] {
                Spume.class, Thing.class, XSpume.class, XThing.class
             }) {
            for (ObjectName n : new ObjectName[] {null, new ObjectName("a:b=c")}) {
                System.out.println("Class " + c.getName() + " with name " + n +
                        "...");
                ObjectName realName = new ObjectName("a:type=" + c.getName());
                Constructor<?> constr = c.getConstructor(ObjectName.class);
                Object mbean = constr.newInstance(realName);
                ObjectInstance oi;
                String what =
                    "Registering MBean of type " + c.getName() + " under name " +
                    "<" + n + ">: ";
                try {
                    oi = mbs.registerMBean(mbean, n);
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(what + " got " + e);
                    continue;
                }
                ObjectName registeredName = oi.getObjectName();
                if (!registeredName.equals(realName))
                    fail(what + " registered as " + registeredName);
                if (!mbs.isRegistered(realName)) {
                    fail(what + " not registered as expected");
                }
                mbs.unregisterMBean(registeredName);
            }
        }
        System.err.flush();
        if (failures == 0)
            System.out.println("TEST PASSED");
        else
            throw new Exception("TEST FAILED: " + failure);
    }

    private static void fail(String msg) {
        System.err.println("FAILED: " + msg);
        failure = msg;
        failures++;
    }

    private static int failures;
    private static String failure;
}
