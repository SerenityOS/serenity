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
 * @summary Tests <int> element
 * @run main/othervm -Djava.security.manager=allow TestInt
 * @author Sergey Malenkov
 */

import java.beans.XMLDecoder;

public final class TestInt extends AbstractTest {
    public static final String XML
            = "<java>\n"
            + " <int>0</int>\n"
            + " <int>127</int>\n"
            + " <int>-128</int>\n"
            + " <int>32767</int>\n"
            + " <int>-32768</int>\n"
            + " <int>2147483647</int>\n"
            + " <int>-2147483648</int>\n"
            + "</java>";

    public static void main(String[] args) {
        new TestInt().test(true);
    }

    @Override
    protected void validate(XMLDecoder decoder) {
        validate(0, decoder.readObject());
        validate((int) Byte.MAX_VALUE, decoder.readObject());
        validate((int) Byte.MIN_VALUE, decoder.readObject());
        validate((int) Short.MAX_VALUE, decoder.readObject());
        validate((int) Short.MIN_VALUE, decoder.readObject());
        validate(Integer.MAX_VALUE, decoder.readObject());
        validate(Integer.MIN_VALUE, decoder.readObject());
    }

    private static void validate(int value, Object object) {
        if (!object.equals(Integer.valueOf(value))) {
            throw new Error("int " + value + " expected");
        }
    }
}
