/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6713777
 * @summary Test that exception messages include all relevant information
 * @author Eamonn McManus
 */

import javax.management.ConstructorParameters;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

public class ExceptionDiagnosisTest {
    private static volatile String failure;

    // ------ Illegal MXBeans ------

    // Test that all of BdelloidMXBean, Rotifer, and File appear in the
    // exception messages.  File is not an allowed type because of recursive
    // getters like "File getParentFile()".
    public static interface BdelloidMXBean {
        public Rotifer getRotifer();
    }

    public static class Bdelloid implements BdelloidMXBean {
        public Rotifer getRotifer() {
            return null;
        }
    }

    public static class Rotifer {
        public File getFile() {
            return null;
        }
    }

    // Test that all of IndirectHashMapMXBean, HashMapContainer, and
    // HashMap<String,String> appear in the exception messages.
    // HashMap<String,String> is not an allowed type because only the
    // java.util interface such as Map are allowed with generic parameters,
    // not their concrete implementations like HashMap.
    public static interface IndirectHashMapMXBean {
        public HashMapContainer getContainer();
    }

    public static class IndirectHashMap implements IndirectHashMapMXBean {
        public HashMapContainer getContainer() {
            return null;
        }
    }

    public static class HashMapContainer {
        public HashMap<String, String> getHashMap() {return null;}
    }

    // ------ MXBeans that are legal but where proxies are not ------

    // Test that all of BlimMXBean, BlimContainer, Blim, and Blam appear
    // in the exception messages for a proxy for this MXBean.  Blam is
    // legal in MXBeans but is not reconstructible so you cannot make
    // a proxy for BlimMXBean.
    public static interface BlimMXBean {
        public BlimContainer getBlimContainer();
    }

    public static class BlimImpl implements BlimMXBean {
        public BlimContainer getBlimContainer() {
            return null;
        }
    }

    public static class BlimContainer {
        public Blim getBlim() {return null;}
        public void setBlim(Blim blim) {}
    }

    public static class Blim {
        public Blam getBlam() {return null;}
        public void setBlam(Blam blam) {}
    }

    public static class Blam {
        public Blam(int x) {}

        public int getX() {return 0;}
    }


    // ------ Property name differing only in case ------

    public static interface CaseProbMXBean {
        public CaseProb getCaseProb();
    }

    public static class CaseProbImpl implements CaseProbMXBean {
        public CaseProb getCaseProb() {return null;}
    }

    public static class CaseProb {
        @ConstructorParameters({"urlPath"})
        public CaseProb(String urlPath) {}

        public String getURLPath() {return null;}
    }


    public static void main(String[] args) throws Exception {
        testMXBeans(new Bdelloid(), BdelloidMXBean.class, Rotifer.class, File.class);
        testMXBeans(new IndirectHashMap(),
                IndirectHashMapMXBean.class, HashMapContainer.class,
                HashMapContainer.class.getMethod("getHashMap").getGenericReturnType());

        testProxies(new BlimImpl(), BlimMXBean.class, BlimMXBean.class,
                BlimContainer.class, Blim.class, Blam.class);

        testCaseProb();

        if (failure == null)
            System.out.println("TEST PASSED");
        else
            throw new Exception("TEST FAILED: " + failure);
    }

    private static void testMXBeans(Object mbean, Type... expectedTypes)
            throws Exception {
        try {
            MBeanServer mbs = MBeanServerFactory.newMBeanServer();
            ObjectName name = new ObjectName("a:b=c");
            mbs.registerMBean(mbean, name);
            fail("No exception from registerMBean for " + mbean);
        } catch (NotCompliantMBeanException e) {
            checkExceptionChain("MBean " + mbean, e, expectedTypes);
        }
    }

    private static <T> void testProxies(
            Object mbean, Class<T> mxbeanClass, Type... expectedTypes)
            throws Exception {
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName name = new ObjectName("a:b=c");
        mbs.registerMBean(mbean, name);
        T proxy = JMX.newMXBeanProxy(mbs, name, mxbeanClass);
        List<Method> methods = new ArrayList<Method>();
        for (Method m : mxbeanClass.getMethods()) {
            if (m.getDeclaringClass() == mxbeanClass)
                methods.add(m);
        }
        if (methods.size() != 1) {
            fail("TEST BUG: expected to find exactly one method in " +
                    mxbeanClass.getName() + ": " + methods);
        }
        Method getter = methods.get(0);
        try {
            try {
                getter.invoke(proxy);
                fail("No exception from proxy method " + getter.getName() +
                        " in " + mxbeanClass.getName());
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception)
                    throw (Exception) cause;
                else
                    throw (Error) cause;
            }
        } catch (IllegalArgumentException e) {
            checkExceptionChain(
                    "Proxy for " + mxbeanClass.getName(), e, expectedTypes);
        }
    }

    private static void testCaseProb() throws Exception {
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        ObjectName name = new ObjectName("a:b=c");
        mbs.registerMBean(new CaseProbImpl(), name);
        CaseProbMXBean proxy = JMX.newMXBeanProxy(mbs, name, CaseProbMXBean.class);
        try {
            CaseProb prob = proxy.getCaseProb();
            fail("No exception from proxy method getCaseProb");
        } catch (IllegalArgumentException e) {
            String messageChain = messageChain(e);
            if (messageChain.contains("URLPath")) {
                System.out.println("Message chain contains URLPath as required: "
                        + messageChain);
            } else {
                fail("Exception chain for CaseProb does not mention property" +
                        " URLPath differing only in case");
                System.out.println("Full stack trace:");
                e.printStackTrace(System.out);
            }
        }
    }

    private static void checkExceptionChain(
            String what, Throwable e, Type[] expectedTypes) {
        System.out.println("Exceptions in chain for " + what + ":");
        for (Throwable t = e; t != null; t = t.getCause())
            System.out.println(".." + t);

        String messageChain = messageChain(e);

        // Now check that each of the classes is mentioned in those messages
        for (Type type : expectedTypes) {
            String name = (type instanceof Class) ?
                ((Class<?>) type).getName() : type.toString();
            if (!messageChain.contains(name)) {
                fail("Exception chain for " + what + " does not mention " +
                        name);
                System.out.println("Full stack trace:");
                e.printStackTrace(System.out);
            }
        }

        System.out.println();
    }

    private static String messageChain(Throwable t) {
        String msg = "//";
        for ( ; t != null; t = t.getCause())
            msg += " " + t.getMessage() + " //";
        return msg;
    }

    private static void fail(String why) {
        failure = why;
        System.out.println("FAIL: " + why);
    }
}
