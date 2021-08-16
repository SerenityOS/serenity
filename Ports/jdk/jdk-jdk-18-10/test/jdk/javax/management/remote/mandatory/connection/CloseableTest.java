/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6231888
 * @summary Test that all the JMX Remote API classes that define
 *          the method "void close() throws IOException;" extend
 *          or implement the java.io.Closeable interface.
 * @author Luis-Miguel Alventosa
 *
 * @run clean CloseableTest
 * @run build CloseableTest
 * @run main CloseableTest
 */

import java.io.Closeable;
import javax.management.remote.JMXConnector;
import javax.management.remote.rmi.RMIConnection;
import javax.management.remote.rmi.RMIConnectionImpl;
import javax.management.remote.rmi.RMIConnectionImpl_Stub;
import javax.management.remote.rmi.RMIConnector;
import javax.management.remote.rmi.RMIJRMPServerImpl;
import javax.management.remote.rmi.RMIServerImpl;

public class CloseableTest {
    private static final Class closeArray[] = {
        JMXConnector.class,
        RMIConnector.class,
        RMIConnection.class,
        RMIConnectionImpl.class,
        RMIConnectionImpl_Stub.class,
        RMIServerImpl.class,
        RMIJRMPServerImpl.class
    };

    static int error;

    static void test(Class<?> c) {
        System.out.println("\nTest " + c);
        if (Closeable.class.isAssignableFrom(c)) {
            System.out.println("Test passed!");
        } else {
            error++;
            System.out.println("Test failed!");
        }
    }

    static void test(String cn) {
        try {
            test(Class.forName(cn));
        } catch (ClassNotFoundException ignore) {
            System.out.println("\n" + cn + " not tested.");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Test that all the JMX Remote API classes that " +
                           "define\nthe method \"void close() throws " +
                           "IOException;\" extend\nor implement the " +
                           "java.io.Closeable interface.");
        for (Class<?> c : closeArray) {
            test(c);
        }

        // Stub classes not present if RMI-IIOP not supported
        test("org.omg.stub.javax.management.remote.rmi._RMIConnection_Stub");

        if (error > 0) {
            final String msg = "\nTest FAILED! Got " + error + " error(s)";
            System.out.println(msg);
            throw new IllegalArgumentException(msg);
        } else {
            System.out.println("\nTest PASSED!");
        }
    }
}
