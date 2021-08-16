/*
 * Copyright (c) 2001, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4387038
 * @summary Ensure that java.rmi.Naming.lookup functions properly for names
 *          containing embedded ':' characters.
 *
 * @library ../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary
 * @run main/othervm LookupNameWithColon
 */

import java.rmi.Naming;
import java.rmi.registry.Registry;

public class LookupNameWithColon {
    public static void main(String[] args) throws Exception {
        String[] names = {
            "fairly:simple", "somewhat:more/complicated",
            "multiple:colons:in:name"
        };

        Registry reg = TestLibrary.createRegistryOnEphemeralPort();
        int port = TestLibrary.getRegistryPort(reg);

        for (int i = 0; i < names.length; i++) {
            reg.rebind(names[i], reg);
            Naming.lookup("rmi://localhost:" + port + "/" + names[i]);
        }
    }
}
