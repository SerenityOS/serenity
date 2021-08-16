/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 *
 * @summary functional test for RMIClassLoader.loadProxyClass; test
 * ensures that the default RMI class loader provider implements
 * RMIClassLoader.loadProxyClass correctly.
 *
 * @author Laird Dornin
 *
 * @library ../../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary FnnClass FnnUnmarshal NonpublicInterface
 *     NonpublicInterface1 PublicInterface PublicInterface1
 * @run main/othervm/policy=security.policy
 *     -Djava.rmi.server.useCodebaseOnly=false LoadProxyClasses
 */

import java.rmi.server.RMIClassLoader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.MarshalledObject;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.Serializable;
import java.io.IOException;

import java.util.Arrays;
import java.util.zip.Checksum;

/**
 *  Invokes RMIClassLoader.loadProxyClass() to load a proxy class with
 *  multiple interfaces using using RMI class unmarshalling.  Test is
 *  composed of cases which each unmarshal a proxy class in a
 *  different environment.  All of the cases create needed class
 *  loaders, load appropriate interfaces, create a proxy class that
 *  implements those interfaces, create a marshalled object from that
 *  proxy class, and finally call .get() on that object.  Get of the
 *  object should pass in some cases and fail in others.
 *
 *  1. Nonpublic interface loaded from the parent of the First
 *  Non-Null class Loader on the execution stack (FNNL).  Public
 *  interface loaded from grandparent of FNNL parent. Proxy class must
 *  be defined in non-null FNNL parent. Should succeed.
 *
 *  2. Nonpublic interface (java.util.zip.ZipConstants) and public
 *  interface (java.util.zip.CheckSum) loaded from bootclasspath,
 *  proxy class defined in null/boot class loader.  Should succeed.
 *
 *  3. Public interface classes loaded in FNNL are also available in
 *  RMI loader parent.  FNNL is grandparent of RMI loader. Proxy class
 *  must be defined in RMI class loader. Should succeed. public
 *  interface must be defined in FNNL.
 *
 *  4. Non-public interfaces have multiple class loaders. Should fail
 *  with a LinkageError.
 *
 *  5. Interface classes loaded from RMI class loader. Proxy class
 *  defined in RMI class loader.
 *
 *  6. Not all interfaces classes can be loaded from a single class
 *  loader; should fail with ClassNotFoundException.  All interface
 *  classes will exist (but not all interfaces will be available from
 *  one class loader).
 *
 *  7. prove that proxy loader has correct annotation.
 *
 *  8. REMIND: may want to add a case where the FNNL is null (This
 *  would be for class unmarshalling in the implemntation of a remote
 *  method invocation).
 */
public class LoadProxyClasses {

    private static URL publicUrl = null;

    public static boolean boomerangSemantics = false;

    public static void main(String[] args) {
        try {
            System.err.println("\nFunctional test to verify that RMI " +
                               "loads proxy classes correctly\n");

            /* install proxy interfaces */
            publicUrl =
                TestLibrary.installClassInCodebase("PublicInterface",
                                                   "public");
            URL publicUrl1 =
                TestLibrary.installClassInCodebase("PublicInterface1",
                                                   "public1");
            URL nonpublicUrl =
                TestLibrary.installClassInCodebase("NonpublicInterface",
                                                   "nonpublic", false);
            URL nonpublicUrl1 =
                TestLibrary.installClassInCodebase("NonpublicInterface1",
                                                   "nonpublic1", false);
            URL bothNonpublicUrl =
                TestLibrary.installClassInCodebase("NonpublicInterface",
                                                   "bothNonpublic");
            TestLibrary.installClassInCodebase("NonpublicInterface1",
                                               "bothNonpublic");
            URL fnnUrl =
                TestLibrary.installClassInCodebase("FnnClass", "fnn");

            TestLibrary.suggestSecurityManager(null);


            /* Case 1 */
            ClassLoader grandParentPublic =
                new URLClassLoader(new URL[] {publicUrl});
            ClassLoader parentNonpublic =
                new URLClassLoader(new URL[] {nonpublicUrl},
                                   grandParentPublic);
            URLClassLoader fnnLoader1 =
                new URLClassLoader(new URL[] {fnnUrl}, parentNonpublic);

            Class nonpublicInterface =
                fnnLoader1.loadClass("NonpublicInterface");
            Class publicInterface =
                fnnLoader1.loadClass("PublicInterface");

            Proxy proxy1 = (Proxy) Proxy.newProxyInstance(parentNonpublic,
                new Class[] {nonpublicInterface, publicInterface},
                new TestInvocationHandler());
            unmarshalProxyClass(proxy1, fnnLoader1, parentNonpublic, 1, null);



            /* Case 2 */
            Class zipConstantsClass =
                Class.forName("java.util.zip.ZipConstants");
            URLClassLoader fnnLoader2 =
                new URLClassLoader(new URL[] {fnnUrl});
            Proxy proxy2 = (Proxy) Proxy.newProxyInstance(null,
                new Class[] {zipConstantsClass, Checksum.class},
                new TestInvocationHandler());
            unmarshalProxyClass(proxy2, fnnLoader2,
                                (ClassLoader) null, 2, null);



            /* Case 3 */
            Thread currentThread = Thread.currentThread();
            ClassLoader fnnLoader3 = new URLClassLoader(
                new URL[] {publicUrl, fnnUrl});
            ClassLoader newCtxLoader =
                new URLClassLoader(new URL[] {publicUrl}, fnnLoader3);
            Class publicInterface3 =
                fnnLoader3.loadClass("PublicInterface");
            ClassLoader currentCtxLoader =
                currentThread.getContextClassLoader();
            currentThread.setContextClassLoader(newCtxLoader);

            Proxy proxy3 = (Proxy) Proxy.newProxyInstance(newCtxLoader,
                new Class[] {publicInterface3},
                new TestInvocationHandler());

            unmarshalProxyClass(proxy3, fnnLoader3, fnnLoader3,
                3, new Case3Checker());

            currentThread.setContextClassLoader(currentCtxLoader);



            /* Case 4 */
            ClassLoader bothNonpublicLoader =
                new URLClassLoader(new URL[] {bothNonpublicUrl});
            Class nonpublicInterface4a =
                bothNonpublicLoader.loadClass("NonpublicInterface");
            Class nonpublicInterface4b =
                bothNonpublicLoader.loadClass("NonpublicInterface1");
            Proxy proxy4 = (Proxy) Proxy.newProxyInstance(bothNonpublicLoader,
                new Class[] {nonpublicInterface4a, nonpublicInterface4b},
                new TestInvocationHandler());

            ClassLoader nonpublicLoaderA =
                new URLClassLoader(new URL[] {nonpublicUrl});
            ClassLoader nonpublicLoaderB =
                new URLClassLoader(new URL[] {nonpublicUrl1}, nonpublicLoaderA);
            currentCtxLoader =
                currentThread.getContextClassLoader();
            currentThread.setContextClassLoader(nonpublicLoaderB);

            IllegalAccessError illegal = null;
            try {
                unmarshalProxyClass(proxy4, fnnLoader2, nonpublicLoaderB,
                                    4, null);
            } catch (IllegalAccessError e) {
                illegal = e;
            }

            if (illegal == null) {
                TestLibrary.bomb("case4: IllegalAccessError not thrown " +
                                 "when multiple nonpublic interfaces have \n" +
                                 "different class loaders");
            } else {
                System.err.println("\ncase4: IllegalAccessError correctly " +
                                   "thrown \n when trying to load proxy " +
                                   "with multiple nonpublic interfaces in \n" +
                                   "  different class loaders");
            }
            currentThread.setContextClassLoader(currentCtxLoader);



            /* Case 5*/
            ClassLoader publicLoader =
                new URLClassLoader(new URL[] {publicUrl});
            Class publicInterface5 =
                publicLoader.loadClass("PublicInterface");
            Proxy proxy5 = (Proxy) Proxy.newProxyInstance(publicLoader,
                new Class[] {publicInterface5},
                new TestInvocationHandler());

            currentCtxLoader =
                currentThread.getContextClassLoader();
            currentThread.setContextClassLoader(publicLoader);
            unmarshalProxyClass(proxy5, fnnLoader2, publicLoader, 5,
                                new Case5Checker());
            currentThread.setContextClassLoader(currentCtxLoader);



            /* Case 6 */
            ClassLoader fnnLoader6 =
                new URLClassLoader(new URL[] {fnnUrl, publicUrl});
            ClassLoader publicLoader6 =
                new URLClassLoader(new URL[] {publicUrl1}, fnnLoader6);

            Class publicInterface6a =
                publicLoader6.loadClass("PublicInterface1");
            Class publicInterface6b =
                fnnLoader6.loadClass("PublicInterface");
            Proxy proxy6 = (Proxy) Proxy.newProxyInstance(publicLoader6,
                new Class[] {publicInterface6a, publicInterface6b},
                new TestInvocationHandler());
            ClassNotFoundException cnfe = null;
            try {
                unmarshalProxyClass(proxy6, fnnLoader6, publicLoader6, 6,
                                    null);
            } catch (ClassNotFoundException e) {
                cnfe = e;
            }
            if (cnfe == null) {
                TestLibrary.bomb("ClassNotFoundException not thrown " +
                                 "when not all proxy interfaces could " +
                                 " be found in a single class loader ");
            } else {
                System.err.println("Case6: ClassNotFoundException " +
                                   "correctly thrown when not all proxy" +
                                   " interfaces could be found in a " +
                                   "single class loader");
                cnfe.printStackTrace();
            }

            System.err.println("TEST PASSED");

        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            TestLibrary.bomb(e);
        }
    }


    private interface LoadChecker {
        void checkLoad(Proxy proxy, ClassLoader expectedLoader);
    }

    private static Proxy unmarshalProxyClass(Proxy proxy,
                                             ClassLoader fnnLoader,
                                             ClassLoader expectedLoader,
                                             int n,
                                             LoadChecker checker)
        throws ClassNotFoundException, IOException,
               InstantiationException, IllegalAccessException
    {
        FnnUnmarshal fnnUnmarshal = (FnnUnmarshal)
                fnnLoader.loadClass("FnnClass").newInstance();
        Proxy unmarshalled = (Proxy)
            fnnUnmarshal.unmarshal(new MarshalledObject(proxy));
        ClassLoader unmarshalledLoader =
            unmarshalled.getClass().getClassLoader();

        if (checker != null) {
            checker.checkLoad(unmarshalled, expectedLoader);
        } else {
            if (unmarshalledLoader != expectedLoader) {
                TestLibrary.bomb("case" + n + ": proxy class not " +
                                 "placed into incorrect loader: " +
                                 unmarshalledLoader);
            } else {
                System.err.println("\ncase" + n + ": proxy class correctly" +
                                   " placed into expected loader: " +
                                   expectedLoader);
            }
        }
        return proxy;
    }

    private static class Case3Checker implements LoadChecker {
        public void checkLoad(Proxy proxy, ClassLoader expectedLoader) {
            ClassLoader ifaceLoader =
                proxy.getClass().getInterfaces()[0].getClassLoader();
            ClassLoader proxyLoader = proxy.getClass().getClassLoader();

            boolean proxyOk = false;

            if (boomerangSemantics) {
                ClassLoader ctxLoader =
                    Thread.currentThread().getContextClassLoader();
                if (proxyLoader == ctxLoader) {
                    proxyOk = true;
                }
            } else if (proxyLoader.getClass().
                       getName().indexOf("sun.rmi") >= 0)
            {
                proxyOk = true;
            }

            if (proxyOk) {
                System.err.println("\ncase3: proxy loaded in" +
                                   " correct loader: " + proxyLoader +
                                   Arrays.asList(((URLClassLoader)
                                                 proxyLoader).getURLs()));
            } else {
                TestLibrary.bomb("case3: proxy class loaded in " +
                                 "incorrect loader: " + proxyLoader +
                                   Arrays.asList(((URLClassLoader)
                                                  proxyLoader).getURLs()));
            }

            if (ifaceLoader == expectedLoader) {
                System.err.println("case3: proxy interface loaded in" +
                                   " correct loader: " + ifaceLoader);
            } else {
                TestLibrary.bomb("public proxy interface loaded in " +
                                 "incorrect loader: " + ifaceLoader);
            }
        }
    }

    private static class Case5Checker implements LoadChecker {
        public void checkLoad(Proxy proxy, ClassLoader expectedLoader) {
            ClassLoader proxyLoader = proxy.getClass().getClassLoader();

            String proxyAnnotation =
                RMIClassLoader.getClassAnnotation(proxy.getClass());

            if ((proxyAnnotation == null) ||
                !proxyAnnotation.equals(publicUrl.toString()))
            {
                TestLibrary.bomb("proxy class had incorrect annotation: " +
                                 proxyAnnotation);
            } else {
                System.err.println("proxy class had correct annotation: " +
                                   proxyAnnotation);
            }

            boolean proxyOk = false;

            if (boomerangSemantics) {
                ClassLoader ctxLoader =
                    Thread.currentThread().getContextClassLoader();
                if (proxyLoader == ctxLoader) {
                    proxyOk = true;
                }
            } else if (proxyLoader.getClass().
                       getName().indexOf("sun.rmi") >= 0)
            {
                proxyOk = true;
            }

            if (proxyOk) {
                System.err.println("\ncase5: proxy loaded from" +
                                   " correct loader: " + proxyLoader);
            } else {
                TestLibrary.bomb("case5: proxy interface loaded from " +
                                 "incorrect loader: " + proxyLoader);
            }
        }
    }

    private static class TestInvocationHandler
        implements InvocationHandler, Serializable
    {
        public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {return null;}
    }
}
