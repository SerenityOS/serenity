/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Tests <array> element
 * @run main/othervm -Djava.security.manager=allow TestArray
 * @author Sergey Malenkov
 */

import java.beans.XMLDecoder;
import java.lang.reflect.Array;

public final class TestArray extends AbstractTest {
    public static final String XML
            = "<java>\n"
            + " <array class=\"java.lang.Number\">\n"
            + "  <byte>-111</byte>\n"
            + "  <long>1111</long>\n"
            + " </array>\n"
            + " <array length=\"3\">\n"
            + "  <void index=\"1\">\n"
            + "   <string>Hello, world!</string>\n"
            + "  </void>\n"
            + " </array>\n"
            + "</java>";

    public static void main(String[] args) {
        new TestArray().test(true);
    }

    @Override
    protected void validate(XMLDecoder decoder) {
        Number[] numbers = getArray(Number.class, 2, decoder.readObject());
        if (!numbers[0].equals(Byte.valueOf("-111"))) { // NON-NLS: hardcoded in XML
            throw new Error("unexpected byte value");
        }
        if (!numbers[1].equals(Long.valueOf("1111"))) { // NON-NLS: hardcoded in XML
            throw new Error("unexpected long value");
        }

        Object[] objects = getArray(Object.class, 3, decoder.readObject());
        if (objects[0] != null) {
            throw new Error("unexpected first value");
        }
        if (!objects[1].equals("Hello, world!")) { // NON-NLS: hardcoded in XML
            throw new Error("unexpected string value");
        }
        if (objects[2] != null) {
            throw new Error("unexpected last value");
        }
    }

    private static <T> T[] getArray(Class<T> component, int length, Object object) {
        Class type = object.getClass();
        if (!type.isArray()) {
            throw new Error("array expected");
        }
        if (!type.getComponentType().equals(component)) {
            throw new Error("unexpected component type");
        }
        if (length != Array.getLength(object)) {
            throw new Error("unexpected array length");
        }
        return (T[]) object;
    }
}
