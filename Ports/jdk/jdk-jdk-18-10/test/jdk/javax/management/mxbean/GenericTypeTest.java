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
 * @bug 6376416 6406447
 * @summary Test use of generic types in MXBeans (mostly illegal).
 * @author Eamonn McManus
 *
 * @run main GenericTypeTest
 */

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import javax.management.*;

public class GenericTypeTest {
    public static class Gen<T> {
        public T getThing() {
            return null;
        }
    }

    // Illegal MXBeans:

    public static interface GenTMXBean {
        public <T> Gen<T> getFoo();
    }

    public static interface GenStringMXBean {
        public Gen<String> getFoo();
    }

    public static class GenMethod {
        public <T> Gen<T> getWhatever() {
            return null;
        }
    }

    public static interface GenMethodMXBean {
        public GenMethod getFoo();
    }

    public static interface TypeVarMXBean {
        public <T> List<T> getFoo();
    }

    private static final Class<?>[] illegalMXBeans = {
        GenTMXBean.class, GenStringMXBean.class, GenMethodMXBean.class,
        TypeVarMXBean.class,
    };

    // Legal MXBeans:

// Not currently supported (see 6406447):
//    public static class ConcreteNoOverride extends Gen<String> {}
//
//    public static interface ConcreteNoOverrideMXBean {
//        public ConcreteNoOverride getFoo();
//    }

    public static class ConcreteOverride extends Gen<String> {
        @Override
        public String getThing() {
            return super.getThing();
        }
    }

    public static interface ConcreteOverrideMXBean {
        public ConcreteOverride getFoo();
    }

    private static final Class<?>[] legalMXBeans = {
        /*ConcreteNoOverrideMXBean.class,*/ ConcreteOverrideMXBean.class,
    };

    private static class NullInvocationHandler implements InvocationHandler {
        public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable {
            return null;
        }
    }
    private static final InvocationHandler nullInvocationHandler =
            new NullInvocationHandler();

    private static <T> T makeNullMXBean(Class<T> intf) throws Exception {
        return intf.cast(Proxy.newProxyInstance(intf.getClassLoader(),
                new Class<?>[] {intf},
                nullInvocationHandler));
    }

    private static String failure;

    public static void main(String[] args) throws Exception {
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        for (boolean legal : new boolean[] {false, true}) {
            Class<?>[] intfs = legal ? legalMXBeans : illegalMXBeans;
            for (Class<?> intf : intfs) {
                String intfName = intf.getName();
                System.out.println("Testing " + intfName);
                Object mxbean = makeNullMXBean(intf);
                ObjectName name = new ObjectName("test:type=" + intfName);
                try {
                    mbs.registerMBean(mxbean, name);
                    if (!legal)
                        fail("Registering " + intfName + " should not succeed");
                } catch (NotCompliantMBeanException e) {
                    if (legal)
                        failX("Registering " + intfName + " should succeed", e);
                } catch (Exception e) {
                    if (legal)
                        failX("Registering " + intfName + " should succeed", e);
                    else {
                        failX("Registering " + intfName + " should produce " +
                                "NotCompliantMBeanException", e);
                    }
                }
            }
        }
        if (failure != null)
            throw new Exception("TEST FAILED: " + failure);
        System.out.println("Test passed");
    }

    private static void fail(String msg) {
        System.out.println("FAILED: " + msg);
        failure = msg;
    }

    private static void failX(String msg, Throwable x) {
        fail(msg + ": " + x);
        x.printStackTrace(System.out);
    }
}
