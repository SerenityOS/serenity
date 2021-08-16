/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6320211
 * @summary Check that java.lang.management MXBeans have the same behavior
 *          as user MXBeans
 * @author  Eamonn McManus
 * @modules jdk.management
 * @run     main/othervm MXBeanBehavior
 */

import java.lang.management.*;
import java.lang.reflect.*;
import java.util.*;
import javax.management.*;

public class MXBeanBehavior {
    // Exclude list: list of platform MBeans that are not MXBeans
    public static final HashSet<String> excludeList = new HashSet<>(
            Arrays.asList("com.sun.management:type=DiagnosticCommand"));

    public static void main(String[] args) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        /* Test that all the MBeans in the java.* and com.sun.management*
           domains are MXBeans with the appropriate behavior.  */
        Set<ObjectName> names = mbs.queryNames(new ObjectName("java.*:*"),
                                               null);
        names.addAll(mbs.queryNames(new ObjectName("com.sun.management*:*"),
                                    null));
        for (ObjectName name : names)
            test(mbs, name);

        /* Now do some rudimentary testing of inter-MXBean references.
           It should be possible for a user MXBean to return e.g.  the
           CompilationMXBean from the platform from an attribute of
           type CompilationMXBean, and have the MXBean infrastructure
           map this into that MXBean's standard ObjectName.  It should
           also be possible for a proxy for this user MXBean to have
           this attribute's value mapped back into a CompilationMXBean
           instance, which however will be another proxy rather than
           the original object.  Finally, it should be possible to set
           the attribute in the user's MXBean through a proxy, giving
           the real CompilationMXBean as an argument, and have this be
           translated into that MXBean's standard ObjectName.  The
           user's MXBean will receive a proxy in this case, though we
           don't check that.  */
        ObjectName refName = new ObjectName("d:type=CompilationRef");
        mbs.registerMBean(new CompilationImpl(), refName);
        CompilationRefMXBean refProxy =
            JMX.newMXBeanProxy(mbs, refName, CompilationRefMXBean.class);
        refProxy.getCompilationMXBean();
        refProxy.setCompilationMXBean(ManagementFactory.getCompilationMXBean());
        ObjectName on =
            (ObjectName) mbs.getAttribute(refName, "CompilationMXBean");
        checkEqual(on, new ObjectName(ManagementFactory.COMPILATION_MXBEAN_NAME),
                   "Referenced object name");
        mbs.setAttribute(refName, new Attribute("CompilationMXBean", on));

        System.out.println("TEST PASSED");
    }

    /* Check the behavior of this MXBean to ensure that it conforms to
       what is expected of all MXBeans as detailed in
       javax.management.MXBean.  Its MBeanInfo should have a
       Descriptor with the fields mxbean and interfaceClassName, and
       furthermore we know that our implementation sets immutableInfo
       here.  Each attribute should have Descriptor with the fields
       openType and originalType that have appropriate values.  We
       don't currently check operations though the same considerations
       would apply there.  (If the MBeanInfo and MBeanAttributeInfo
       tests pass we can reasonably suppose that this MXBean will
       behave the same as all other MXBeans, so MBeanOperationInfo,
       MBeanNotificationInfo, and MBeanConstructorInfo will be covered
       by generic MXBean tests.
     */
    private static void test(MBeanServer mbs, ObjectName name) throws Exception {
        if(excludeList.contains(name.getCanonicalName())) {
            // Skipping not MXBean objects.
            return;
        }
        System.out.println("Testing: " + name);

        MBeanInfo mbi = mbs.getMBeanInfo(name);
        Descriptor mbid = mbi.getDescriptor();
        Object[] values = mbid.getFieldValues("immutableInfo",
                                              "interfaceClassName",
                                              "mxbean");
        checkEqual(values[0], "true", name + " immutableInfo field");
        checkEqual(values[2], "true", name + " mxbean field");
        String interfaceClassName = (String) values[1];
        if (!mbs.isInstanceOf(name, interfaceClassName)) {
            throw new RuntimeException(name + " not instance of " +
                                       interfaceClassName);
        }
        Class interfaceClass = Class.forName(interfaceClassName);
        for (MBeanAttributeInfo mbai : mbi.getAttributes()) {
            Descriptor mbaid = mbai.getDescriptor();
            Object[] avalues = mbaid.getFieldValues("openType",
                                                    "originalType");
            if (avalues[0] == null || avalues[1] == null) {
                throw new RuntimeException("Null attribute descriptor fields: " +
                                           Arrays.toString(avalues));
            }
            if (mbai.isReadable()) {
                String mname = (mbai.isIs() ? "is" : "get") + mbai.getName();
                Method m = interfaceClass.getMethod(mname);
                Type t = m.getGenericReturnType();
                String ret =
                    (t instanceof Class) ? ((Class) t).getName() : t.toString();
                if (!ret.equals(avalues[1])) {
                    final String msg =
                        name + " attribute " + mbai.getName() + " has wrong " +
                        "originalType: " + avalues[1] + " vs " + ret;
                    throw new RuntimeException(msg);
                }
            }
        }
    }

    private static void checkEqual(Object x, Object y, String what) {
        final boolean eq;
        if (x == y)
            eq = true;
        else if (x == null)
            eq = false;
        else
            eq = x.equals(y);
        if (!eq)
            throw new RuntimeException(what + " should be " + y + ", is " + x);
    }

    public static interface CompilationRefMXBean {
        public CompilationMXBean getCompilationMXBean();
        public void setCompilationMXBean(CompilationMXBean mxb);
    }

    public static class CompilationImpl implements CompilationRefMXBean {
        public CompilationMXBean getCompilationMXBean() {
            return ManagementFactory.getCompilationMXBean();
        }

        public void setCompilationMXBean(CompilationMXBean mxb) {
            if (mxb == ManagementFactory.getCompilationMXBean())
                return;
            MBeanServerInvocationHandler mbsih = (MBeanServerInvocationHandler)
                Proxy.getInvocationHandler(mxb);
            ObjectName expectedName;
            try {
                expectedName =
                    new ObjectName(ManagementFactory.COMPILATION_MXBEAN_NAME);
            } catch (MalformedObjectNameException e) {
                throw new RuntimeException(e);
            }
            checkEqual(mbsih.getObjectName(), expectedName,
                       "Proxy name in setCompilationMXBean");
        }
    }
}
