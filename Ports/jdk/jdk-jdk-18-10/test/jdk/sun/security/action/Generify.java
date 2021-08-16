/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5057136
 * @summary Generify sun.security.action.GetPropertyAction and friends
 * @modules java.base/sun.security.action
 */

import java.io.*;
import java.security.*;
import sun.security.action.*;

public class Generify {

    public static void main(String[] args) throws Exception {

        long larg = 1234567890L;

        System.setProperty("boolean", "true");
        System.setProperty("integer", "9");
        System.setProperty("long", Long.toString(larg));
        System.setProperty("property", "propertyvalue");

        Boolean b = AccessController.doPrivileged
                        (new GetBooleanAction("boolean"));
        if (b.booleanValue() == true) {
            System.out.println("boolean test passed");
        } else {
            throw new SecurityException("boolean test failed");
        }

        Integer i = AccessController.doPrivileged
                        (new GetIntegerAction("integer"));
        if (i.intValue() == 9) {
            System.out.println("integer test passed");
        } else {
            throw new SecurityException("integer test failed");
        }

        Long l = AccessController.doPrivileged
                        (new GetLongAction("long"));
        if (l.longValue() == larg) {
            System.out.println("long test passed");
        } else {
            throw new SecurityException("long test failed");
        }

        String prop = AccessController.doPrivileged
                        (new GetPropertyAction("property"));
        if (prop.equals("propertyvalue")) {
            System.out.println("property test passed");
        } else {
            throw new SecurityException("property test failed");
        }

        File f = new File(System.getProperty("test.src", "."), "Generify.java");
        FileInputStream fis = AccessController.doPrivileged
                        (new OpenFileInputStreamAction(f));
        if (fis != null) {
            System.out.println("file test passed");
        } else {
            throw new SecurityException("file test failed");
        }
    }
}
