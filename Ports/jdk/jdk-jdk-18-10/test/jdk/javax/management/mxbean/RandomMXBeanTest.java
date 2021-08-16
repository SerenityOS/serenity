/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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

/* This test essentially duplicates the functionality of MXBeanTest.java.
 * See LeakTest.java for an explanation.
 */

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import javax.management.InstanceAlreadyExistsException;
import javax.management.JMX;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

public class RandomMXBeanTest {
    public static interface StupidMXBean {
        public int ZERO = Integer.parseInt("0");
        public int getZero();
        public int identity(int x);
    }

    public static class StupidImpl implements StupidMXBean {
        public int getZero() {
            return 0;
        }

        public int identity(int x) {
            return x;
        }
    }

    public static interface ReferMXBean {
        public StupidMXBean getStupid();
    }

    public static class ReferImpl implements ReferMXBean {
        private final StupidMXBean stupid;

        ReferImpl(StupidMXBean stupid) {
            this.stupid = stupid;
        }

        public StupidMXBean getStupid() {
            return stupid;
        }
    }

    private static class WrapInvocationHandler implements InvocationHandler {
        private final Object wrapped;

        WrapInvocationHandler(Object wrapped) {
            this.wrapped = wrapped;
        }

        public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable {
            return method.invoke(wrapped, args);
        }
    }

    private static class DullInvocationHandler implements InvocationHandler {
        private static Map<Class<?>, Object> zeroMap =
                new HashMap<Class<?>, Object>();
        static {
            zeroMap.put(byte.class, (byte) 0);
            zeroMap.put(int.class, 0);
            zeroMap.put(short.class, (short) 0);
            zeroMap.put(long.class, 0L);
            zeroMap.put(float.class, 0F);
            zeroMap.put(double.class, 0.0);
            zeroMap.put(boolean.class, false);
            zeroMap.put(char.class, '\0');
        }

        public static Object zeroFor(Class<?> c) {
            if (c.isPrimitive())
                return zeroMap.get(c);
            else
                return null;
        }

        public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable {
            Class<?> retType = method.getReturnType();
            if (!retType.isPrimitive())
                return null;
            return zeroMap.get(retType);
        }
    }

    public static void main(String[] args) throws Exception {
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName name = new ObjectName("a:b=c");
        StupidMXBean stupid = new StupidImpl();
        mbs.registerMBean(stupid, name);
        ObjectName referName = new ObjectName("a:c=d");
        mbs.registerMBean(new ReferImpl(stupid), referName);
        System.out.println(mbs.getMBeanInfo(name));
        StupidMXBean stupid2 = (StupidMXBean)
                Proxy.newProxyInstance(StupidMXBean.class.getClassLoader(),
                    new Class<?>[] {StupidMXBean.class},
                    new WrapInvocationHandler(stupid));
        ObjectName stupidName2 = new ObjectName("a:d=e");
        mbs.registerMBean(stupid2, stupidName2);
        Field zero = StupidMXBean.class.getField("ZERO");
        System.out.println("Zero field = " + zero.get(null));
        test(mbs, MerlinMXBean.class);
        test(mbs, TigerMXBean.class);

        StupidMXBean proxy = JMX.newMXBeanProxy(mbs, name, StupidMXBean.class);
        System.out.println("Zero = " + proxy.getZero());
        System.out.println("One = " + proxy.identity(1));
        ReferMXBean referProxy =
                JMX.newMXBeanProxy(mbs, referName, ReferMXBean.class);
        StupidMXBean stupidProxy2 = referProxy.getStupid();
        System.out.println("Same proxy: " + (proxy == stupidProxy2));
        Method[] methods = StupidMXBean.class.getMethods();
        for (Method method : methods) {
            if (method.getParameterTypes().length == 0)
                method.invoke(proxy, new Object[0]);
        }
    }

    private static <T> void test(MBeanServer mbs, Class<T> c) throws Exception {
        System.out.println("Testing " + c.getName());
        T merlin = c.cast(
            Proxy.newProxyInstance(c.getClassLoader(),
                new Class<?>[] {c},
                new DullInvocationHandler()));
        ObjectName merlinName = new ObjectName("a:type=" + c.getName());
        mbs.registerMBean(merlin, merlinName);
        System.out.println(mbs.getMBeanInfo(merlinName));
        T merlinProxy = JMX.newMXBeanProxy(mbs, merlinName, c);
        Method[] merlinMethods = c.getMethods();
        for (Method m : merlinMethods) {
            Class<?>[] types = m.getParameterTypes();
            Object[] params = new Object[types.length];
            for (int i = 0; i < types.length; i++)
                params[i] = DullInvocationHandler.zeroFor(types[i]);
            System.out.println("Invoking " + m.getName());
            m.invoke(merlinProxy, (Object[]) params);
        }
    }
}
