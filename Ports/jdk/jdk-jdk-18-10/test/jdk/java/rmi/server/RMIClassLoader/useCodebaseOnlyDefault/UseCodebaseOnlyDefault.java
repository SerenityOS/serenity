/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8001040
 * @summary Tests proper parsing and defaulting of the
 * "java.rmi.server.useCodebaseOnly" property.
 *
 * @modules java.rmi/sun.rmi.server:+open
 * @run main/othervm UseCodebaseOnlyDefault true
 * @run main/othervm -Djava.rmi.server.useCodebaseOnly=xyzzy UseCodebaseOnlyDefault true
 * @run main/othervm -Djava.rmi.server.useCodebaseOnly UseCodebaseOnlyDefault true
 * @run main/othervm -Djava.rmi.server.useCodebaseOnly=true UseCodebaseOnlyDefault true
 * @run main/othervm -Djava.rmi.server.useCodebaseOnly=false UseCodebaseOnlyDefault false
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import sun.rmi.server.MarshalInputStream;

/**
 * usage: UseCodebaseOnlyDefault expected
 *
 * 'expected' is the expected value of useCodebaseOnly, which
 * must be "true" or "false".
 */
public class UseCodebaseOnlyDefault {
    static final String USAGE = "usage: UseCodebaseOnlyDefault boolean";
    static final String PROPNAME = "java.rmi.server.useCodebaseOnly";

    /**
     * Gets the actual useCodebaseOnly value by creating an instance
     * of MarshalInputStream and reflecting on the useCodebaseOnly field.
     */
    static boolean getActualValue() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject("foo");
        oos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        MarshalInputStream mis = new MarshalInputStream(bais);

        Field f = MarshalInputStream.class.getDeclaredField("useCodebaseOnly");
        f.setAccessible(true);
        return f.getBoolean(mis);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException(USAGE);
        }

        boolean expected;
        if (args[0].equals("true")) {
            expected = true;
        } else if (args[0].equals("false")) {
            expected = false;
        } else {
            throw new IllegalArgumentException(USAGE);
        }
        System.out.println("expected = " + expected);

        String prop = System.getProperty(PROPNAME);
        System.out.print("Property " + PROPNAME);
        if (prop == null) {
            System.out.println(" is not set");
        } else {
            System.out.println(" = '" + prop + "'");
        }

        boolean actual = getActualValue();
        System.out.println("actual = " + actual);

        if (expected != actual)
            throw new AssertionError("actual does not match expected value");
    }
}
