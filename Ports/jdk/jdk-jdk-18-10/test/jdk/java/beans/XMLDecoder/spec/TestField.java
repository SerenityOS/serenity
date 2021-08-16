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
 * @summary Tests <field> element
 * @run main/othervm -Djava.security.manager=allow TestField
 * @author Sergey Malenkov
 */

import java.beans.XMLDecoder;

public final class TestField extends AbstractTest {
    public static final String XML
            = "<java>\n"
            + " <field name=\"FIELD\" class=\"TestField\"/>\n"
            + " <field name=\"FIELD\" class=\"TestField\">\n"
            + "  <string>static postfix</string>\n"
            + " </field>\n"
            + " <field name=\"FIELD\" class=\"TestField\"/>\n"
            + " <property name=\"owner\">\n"
            + "  <field id=\"prefix\" name=\"field\"/>\n"
            + "  <field name=\"field\">\n"
            + "   <string>postfix</string>\n"
            + "  </field>\n"
            + "  <field id=\"postfix\" name=\"field\"/>\n"
            + " </property>\n"
            + " <var idref=\"prefix\"/>\n"
            + " <var idref=\"postfix\"/>\n"
            + "</java>";

    public static void main(String[] args) {
        new TestField().test(true);
    }

    public static String FIELD;
    public String field;

    @Override
    protected void validate(XMLDecoder decoder) {
        FIELD = "static prefix";
        field = "prefix";
        decoder.setOwner(this);
        validate(decoder, "static prefix");
        validate(decoder, "static postfix");
        validate(decoder, "prefix");
        validate(decoder, "postfix");
    }

    private static void validate(XMLDecoder decoder, String name) {
        if (!decoder.readObject().equals(name)) {
            throw new Error(name + " expected");
        }
    }
}
