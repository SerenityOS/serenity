/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.net.httpserver.HttpHandler;
import jdk.internal.misc.Unsafe;
import jdk.test.lib.Asserts;
import java.io.InputStream;
import java.net.URL;


// Test cases:
//
//     1. load HttpExchange in apploader, define HttpExchange in parent (platform) loader,
//        then load MyHttpHandler  => fail.
//     2. define HttpExchange in parent (platform) loader, load MyHttpHandler,
//        then try to define HttpExchange in child (app) loader => fail.
//     3. no LinkageError should be throw when linking a class that does not override/implement any
//        methods.
public class LoaderConstraintsApp {
    static void defineHttpExchangeWithAppLoader() throws Exception {
        Unsafe unsafe = Unsafe.getUnsafe();
        URL url = new URL("jrt://modules/jdk.httpserver/com/sun/net/httpserver/HttpExchange.class");
        byte[] bytes;
        try (InputStream is = url.openStream()) {
            bytes = is.readAllBytes();
        }
        Class fakeClass = unsafe.defineClass("com/sun/net/httpserver/HttpExchange", bytes, 0, bytes.length,
                                             LoaderConstraintsApp.class.getClassLoader(),
                                             LoaderConstraintsApp.class.getProtectionDomain());
         System.out.println("fake HttpExchange          = " + fakeClass.hashCode());
         System.out.println("fake HttpExchange (loader) = " + fakeClass.getClassLoader());
    }

    static void resolveHttpExchangeInParentLoader(ClassLoader loader) throws Exception {
        Class realClass = Class.forName("com.sun.net.httpserver.HttpExchange", false, loader);
        System.out.println("real HttpExchange          = " + realClass.hashCode());
        System.out.println("real HttpExchange (loader) = " + realClass.getClassLoader());
    }

    static void doTest(int k) throws Exception {
        ClassLoader appLoader =  LoaderConstraintsApp.class.getClassLoader();
        ClassLoader platformLoader = appLoader.getParent();
        if (k == 1) {
           defineHttpExchangeWithAppLoader();
           // Resolve HttpExchange in parent loader (platform loader) - should be OK.
           resolveHttpExchangeInParentLoader(platformLoader);
           try {
               // This must fail since the two loaders have resolved different versions of HttpExchange
               HttpHandler h1 = new MyHttpHandler();
               throw new RuntimeException("Load HttpExchange with platform loader did not fail as expected");
           } catch (LinkageError e) {
               System.out.println("Expected: " + e);
               Asserts.assertTrue(e.getMessage().contains("loader constraint violation in interface itable initialization for class MyHttpHandler"));
               e.printStackTrace(System.out);
           }
        } else if (k == 2) {
            // Resolve HttpExchange in parent loader (platform loader) - this should succeed
            resolveHttpExchangeInParentLoader(platformLoader);

            // Load MyHttpHandler in app loader -- this should succeed, but it should
            // create a class loader constraint that app loader must resolve the same HttpExchange
            // class as the platform loader
            HttpHandler h2 = new MyHttpHandler();

            // Try to resolve a different HttpExchange class in the app loader. It must fail
            try {
                defineHttpExchangeWithAppLoader();
                throw new RuntimeException("defineHttpExchangeWithAppLoader() did not fail as expected");
            } catch (LinkageError e) {
                System.out.println("Expected: " + e);
                e.printStackTrace(System.out);
            }
        } else if (k == 3) {
            // Resolve a different HttpExchange in platform and app loaders
            resolveHttpExchangeInParentLoader(platformLoader);
            defineHttpExchangeWithAppLoader();

            // MyHttpHandlerB should still link, as it doesn't override HttpHandler.handle(HttpExchange)
            MyHttpHandlerB.touch();

            MyClassLoader loader = new MyClassLoader(platformLoader, appLoader);
            try {
                // MyHttpHandlerC should link, as its loader (MyClassLoader) resolves the same
                // HttpExchange as the platform loader.
                Class C = loader.loadClass("MyHttpHandlerC");
                System.out.println("MyHttpHandlerC          = " + C);
                System.out.println("MyHttpHandlerC (loader) = " + C.getClassLoader());

                HttpHandler handlerC = (HttpHandler)C.newInstance();
                try {
                    // If the following is executed during CDS dynamic dump, a loader constraint is checked when resolving
                    // the HttpHandler.handle(HttpExchange) method reference inside MyHttpHandlerB.test(). This constraint must
                    // not be saved into the CDS archive for MyHttpHandlerB, or it would prevent MyHttpHandlerB
                    // from being linked during runtime.
                    MyHttpHandlerB.test(handlerC);
                    throw new RuntimeException("MyHttpHandlerB.test() did not fail as expected");
                } catch (LinkageError e) {
                    System.out.println("Expected: " + e);
                    Asserts.assertTrue(e.getMessage().matches(".*constraint violation: when resolving interface method .*.HttpHandler.handle.*"));
                    e.printStackTrace(System.out);
                }
            } catch (Exception e) {
                throw new RuntimeException("Unexpected exception", e);
            }
        } else {
            // should not be other value
            throw new RuntimeException("Wrong option specified k = " + k);
        }
    }

    public static void main(String... args) throws Throwable {
        if (args.length < 1) {
            // option of {1, 2}
            throw new RuntimeException("Wrong number of arguments");
        }

        if (args.length >= 2 && "loadClassOnly".equals(args[1])) {
            System.out.println("Loading: " + MyHttpHandler.class);
            System.out.println("Loading: " + MyHttpHandlerB.class);
            System.exit(0);
        }

        int k = Integer.valueOf(args[0]);
        if (k < 1 && k > 3) {
            throw new RuntimeException("Arg is out of range [1,3] k = " + k);
        }

        doTest(k);
    }
}
