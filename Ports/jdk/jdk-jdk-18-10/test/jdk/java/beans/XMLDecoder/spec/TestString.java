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
 * @summary Tests <string> element
 * @run main/othervm -Djava.security.manager=allow TestString
 * @author Sergey Malenkov
 */

import java.beans.XMLDecoder;

public final class TestString extends AbstractTest {
    public static final String PREFIX = " prefix ";
    public static final String POSTFIX = " postfix ";
    public static final String XML
            = "<java>\n"
            + " <string>" + PREFIX + "</string>\n"
            + " <string>" + POSTFIX + "</string>\n"
            + " <string>" + PREFIX + POSTFIX + "</string>\n"
            + " <string>" + PREFIX + "<char code=\"0\"/>" + POSTFIX + "</string>\n"
            + "</java>";

    public static void main(String[] args) {
        new TestString().test(true);
    }

    @Override
    protected void validate(XMLDecoder decoder) {
        validate(PREFIX, decoder.readObject());
        validate(POSTFIX, decoder.readObject());
        validate(PREFIX + POSTFIX, decoder.readObject());
        validate(PREFIX + '\u0000' + POSTFIX, decoder.readObject());
    }

    private static void validate(String name, Object object) {
        if (!object.equals(name)) {
            throw new Error(name + " expected");
        }
    }
}
