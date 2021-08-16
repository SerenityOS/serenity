/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6403794
 * @summary Test that all the platform MXBeans are wrapped in StandardMBean so
 *          an MBeanServer which does not have support for MXBeans can be used.
 * @author Luis-Miguel Alventosa
 *
 * @run clean MBeanServerMXBeanUnsupportedTest
 * @run build MBeanServerMXBeanUnsupportedTest
 * @run main/othervm MBeanServerMXBeanUnsupportedTest
 */

import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import javax.management.MBeanServerDelegate;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.remote.MBeanServerForwarder;

public class MBeanServerMXBeanUnsupportedTest {

    /**
     * An MBeanServerBuilder that returns an MBeanServer which throws a
     * RuntimeException if MXBeans are not converted into StandardMBean.
     */
    public static class MBeanServerBuilderImpl extends MBeanServerBuilder {

        private final MBeanServerBuilder inner;

        public MBeanServerBuilderImpl() {
            inner = new MBeanServerBuilder();
        }

        public MBeanServer newMBeanServer(
                String defaultDomain,
                MBeanServer outer,
                MBeanServerDelegate delegate) {
            final MBeanServerForwarder mbsf =
                    MBeanServerForwarderInvocationHandler.newProxyInstance();

            final MBeanServer innerMBeanServer =
                    inner.newMBeanServer(defaultDomain,
                    (outer == null ? mbsf : outer),
                    delegate);

            mbsf.setMBeanServer(innerMBeanServer);
            return mbsf;
        }
    }

    /**
     * An MBeanServerForwarderInvocationHandler that throws a
     * RuntimeException if we try to register a non StandardMBean.
     */
    public static class MBeanServerForwarderInvocationHandler
            implements InvocationHandler {

        public static final HashSet<String> excludeList = new HashSet<String>(
            Arrays.asList("com.sun.management:type=DiagnosticCommand"));

        public static MBeanServerForwarder newProxyInstance() {

            final InvocationHandler handler =
                    new MBeanServerForwarderInvocationHandler();

            final Class[] interfaces =
                    new Class[] {MBeanServerForwarder.class};

            Object proxy = Proxy.newProxyInstance(
                    MBeanServerForwarder.class.getClassLoader(),
                    interfaces,
                    handler);

            return MBeanServerForwarder.class.cast(proxy);
        }

        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {

            final String methodName = method.getName();

            if (methodName.equals("getMBeanServer")) {
                return mbs;
            }

            if (methodName.equals("setMBeanServer")) {
                if (args[0] == null)
                    throw new IllegalArgumentException("Null MBeanServer");
                if (mbs != null)
                    throw new IllegalArgumentException("MBeanServer object " +
                            "already initialized");
                mbs = (MBeanServer) args[0];
                return null;
            }

            if (methodName.equals("registerMBean")) {
                Object mbean = args[0];
                ObjectName name = (ObjectName) args[1];
                String domain = name.getDomain();
                System.out.println("registerMBean: class=" +
                        mbean.getClass().getName() + "\tname=" + name);
                Object result = method.invoke(mbs, args);
                if (domain.equals("java.lang") ||
                    domain.equals("java.util.logging") ||
                    domain.equals("com.sun.management")) {
                    if(!excludeList.contains(name.getCanonicalName())) {
                        String mxbean = (String)
                            mbs.getMBeanInfo(name).getDescriptor().getFieldValue("mxbean");
                        if (mxbean == null || !mxbean.equals("true")) {
                            throw new RuntimeException(
                                "Platform MBeans must be MXBeans!");
                        }
                        if (!(mbean instanceof StandardMBean)) {
                            throw new RuntimeException(
                                "MXBeans must be wrapped in StandardMBean!");
                        }
                    }
                }
                return result;
            }

            return method.invoke(mbs, args);
        }

        private MBeanServer mbs;
    }

    /*
     * Standalone entry point.
     *
     * Run the test and report to stdout.
     */
    public static void main(String args[]) throws Exception {
        System.setProperty("javax.management.builder.initial",
                MBeanServerBuilderImpl.class.getName());
        try {
            ManagementFactory.getPlatformMBeanServer();
        } catch (RuntimeException e) {
            System.out.println(">>> Unhappy Bye, Bye!");
            throw e;
        }
        System.out.println(">>> Happy Bye, Bye!");
    }
}
