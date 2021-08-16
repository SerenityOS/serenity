/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4278121
 * @summary Ensure that calling unbind() on an unbound name returns
 *     successfully.
 * @modules java.rmi/sun.rmi.registry java.rmi/sun.rmi.server
 *     java.rmi/sun.rmi.transport java.rmi/sun.rmi.transport.tcp
 *     jdk.naming.rmi
 * @library ../../../../../../java/rmi/testlibrary
 * @build TestLibrary
 * @run main/othervm UnbindIdempotent
 */

import java.rmi.registry.Registry;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

public class UnbindIdempotent {

    public static void main(String[] args) throws Exception {
        Registry registry = TestLibrary.createRegistryOnEphemeralPort();
        int registryPort = TestLibrary.getRegistryPort(registry);
        InitialContext ictx = new InitialContext();
        Context rctx;

        try {
            rctx = (Context)ictx.lookup("rmi://localhost:" + Integer.toString(registryPort));
            System.out.println("Got context: " + rctx.getClass());
        } catch (NamingException e) {
            // Unable to set up for test.
            System.err.println("WARNING: The test skipped due to NamingException: " + e);
            return;
        }

        // Attempt to unbind a name that is not already bound.
        try {
            rctx.unbind("_bogus_4278121_");
        } catch (NameNotFoundException e) {
            throw new Exception("Test failed:  unbind() call not idempotent");
        }
    }
}
