/*
 * Copyright (c) 1999, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4152295
 * @summary trivial optimization in RemoteProxy.extendsRemote
 *
 * @author Laird Dornin
 *
 * @library ../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server:+open
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary
 * @run main/othervm GetRemoteClass
 */

import java.io.Serializable;
import java.rmi.Remote;
import sun.rmi.server.Util;
import java.lang.reflect.*;


/**
 * Simple test to make sure that Util.getRemoteClass throws
 * exception in proper circumstances.  Pass two different classes, one
 * that extends Remote and one that only extends Serializable.  Should
 * throw exception for Serializable.
 */
public class GetRemoteClass {
    public interface ExtendRemote extends Remote {
    }

    public static class TestRemote implements ExtendRemote {
    }

    public static class OnlySerializable implements Serializable {
    }

    public static void main(String[] argv) {

        System.err.println("\nregression test for 4152295\n");

        Method utilGetRemoteClass = null;

        try {
            /**
             * Use reflection to set access overrides so that the test
             * can call protected method, getRemoteClass
             */
            Class[] args = new Class[1];
            args[0] = Class.class;
            utilGetRemoteClass =
                Util.class.getDeclaredMethod("getRemoteClass", args);
            utilGetRemoteClass.setAccessible(true);

            // getRemoteClass can be invoked without exception
            utilGetRemoteClass.invoke
                 (null , new Object [] {TestRemote.class});
            System.err.println("remote class flagged as remote");

            ClassNotFoundException cnfe = null;
            try {
                // getRemoteClass can be invoked without exception
                utilGetRemoteClass.invoke
                    (null , new Object [] {OnlySerializable.class});
            } catch (InvocationTargetException e) {
                System.err.println("got ClassNotFoundException; remote " +
                                   "class flagged as nonremote");
                cnfe = (ClassNotFoundException) e.getTargetException();
            }
            if (cnfe == null) {
                TestLibrary.bomb("Serializable class flagged as remote?");
            }

            System.err.println("Test passed.");
        } catch (Exception e) {
            TestLibrary.bomb("Unexpected exception", e);
        }
    }
}
