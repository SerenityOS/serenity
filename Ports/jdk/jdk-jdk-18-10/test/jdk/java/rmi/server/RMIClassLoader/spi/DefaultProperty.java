/*
 * Copyright (c) 2001, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4418673
 * @summary verify that setting the java.rmi.server.RMIClassLoaderSpi
 * system property to "default" causes the default provider instance to
 * be used by the RMIClassLoader API.
 * @author Peter Jones
 *
 * @library ../../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary ServiceConfiguration Foo
 * @run main/othervm/policy=security.policy DefaultProperty
 */

import java.net.MalformedURLException;
import java.net.URL;

import java.rmi.server.RMIClassLoader;

public class DefaultProperty {

    public static void main(String[] args) throws Exception {

        ServiceConfiguration.installServiceConfigurationFile();

        System.setProperty(
            "java.rmi.server.RMIClassLoaderSpi", "default");

        String classname = "Foo";

        URL codebaseURL = null;
        try {
            codebaseURL = TestLibrary.installClassInCodebase(
                classname, "remote_codebase");
        } catch (MalformedURLException e) {
            TestLibrary.bomb(e);
        }

        TestLibrary.suggestSecurityManager(null);

        Class fooClass = RMIClassLoader.loadClass(codebaseURL, classname);
        if (!fooClass.getName().equals(classname)) {
            throw new RuntimeException(
                "wrong class name, expected: " + classname +
                ", received: " + fooClass.getName());
        }

        String annotation = RMIClassLoader.getClassAnnotation(fooClass);
        if (!annotation.equals(codebaseURL.toString())) {
            throw new RuntimeException(
                "wrong class annotation, expected: " + codebaseURL.toString() +
                ", received: " + annotation);
        }

        System.err.println("TEST PASSED");
    }
}
