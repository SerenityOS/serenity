/*
 * Copyright (c) 1998, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4137605
 * @summary When the RMIClassLoader.getClassAnnotation() is called with a
 * class loaded from any URLClassLoader instance (not just those created for
 * internal use by the RMI runtime), then it should return a String containing
 * a space-separated list of the class loader's path of URLs.
 * @author Peter Jones
 *
 * @library ../../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary Dummy
 * @run main/othervm/policy=security.policy/timeout=120 UseGetURLs
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

public class UseGetURLs {

    public static void main(String[] args) {

        System.err.println("\nRegression test for bug 4137605\n");

        TestLibrary.suggestSecurityManager("java.rmi.RMISecurityManager");
        System.err.println("Security manager: " +
                           System.getSecurityManager().getClass().getName());

        /*
         * Install dummy class in first codebase to be loaded from an
         * arbitrary URLClassLoader; the second codebase is used to make
         * the desired annotation more interesting (more than one element).
         */
        URL codebase1 = null;
        URL codebase2 = null;
        try {
            codebase1 = TestLibrary.installClassInCodebase("Dummy",
                                                           "codebase1");

            File cb2file =
                new File(TestLibrary.getProperty("user.dir", "."),
                         "codebase2");
            codebase2 = cb2file.toURL();
        } catch (MalformedURLException e) {
            TestLibrary.bomb("failed to install test classes", e);
        }

        try {
            /*
             * Create an arbitary URLClassLoader for the two codebases.
             */
            ClassLoader loader = URLClassLoader.newInstance(
                new URL[] { codebase1, codebase2 });
            System.err.println(
                "URLs for class loader: " +
                Arrays.asList(((URLClassLoader) loader).getURLs()));
            System.err.println("Expecting annotation: \"" +
                codebase1 + " " + codebase2 + "\"");
            System.err.println("First URL:");
            dumpURL(codebase1);
            System.err.println("Second URL:");
            dumpURL(codebase2);

            /*
             * Load dummy class from the loader, get the annotation string,
             * and verify that it is correct.
             */
            Class cl = loader.loadClass("Dummy");
            String annotation = RMIClassLoader.getClassAnnotation(cl);
            System.err.println("Received annotation:  \"" +
                annotation + "\"");

            if (annotation == null) {
                throw new RuntimeException("annotation was null");
            }
            URL[] urls = pathToURLs(annotation);
            System.err.println(
                "URLs from annotation: " + Arrays.asList(urls));
            if (urls.length != 2) {
                throw new RuntimeException(
                    "wrong number of elements in annotation");
            }
            if (!urls[0].equals(codebase1)) {
                System.err.println("First URL in annotation is incorrect:");
                dumpURL(urls[0]);
                throw new RuntimeException(
                    "first URL in annotation is incorrect");
            }
            if (!urls[1].equals(codebase2)) {
                System.err.println("Second URL in annotation is incorrect:");
                dumpURL(urls[1]);
                throw new RuntimeException(
                    "second URL in annotation is incorrect");
            }

            System.err.println("TEST PASSED: annotation matched codebase");
        } catch (Exception e) {
            TestLibrary.bomb(e.getMessage(), e);
        }
    }

    private static URL[] pathToURLs(String path)
        throws MalformedURLException
    {
        StringTokenizer st = new StringTokenizer(path); // divide by spaces
        URL[] urls = new URL[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++) {
            urls[i] = new URL(st.nextToken());
        }
        return urls;
    }

    private static void dumpURL(URL u) {
        System.err.println("\tprotocol:  " + u.getProtocol());
        System.err.println("\tauthority: " + u.getAuthority());
        System.err.println("\tuser info: " + u.getUserInfo());
        System.err.println("\thost:      " + u.getHost());
        System.err.println("\tport:      " + u.getPort());
        System.err.println("\tpath:      " + u.getPath());
        System.err.println("\tfile:      " + u.getFile());
        System.err.println("\tquery:     " + u.getQuery());
        System.err.println("\tref:       " + u.getRef());
    }
}
