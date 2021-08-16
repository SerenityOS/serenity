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
 * @summary Tests <property> element
 * @run main/othervm -Djava.security.manager=allow TestProperty
 * @author Sergey Malenkov
 */

import java.beans.XMLDecoder;

public final class TestProperty extends AbstractTest {
    public static final String XML
            = "<java>\n"
            + " <property name=\"owner\">\n"
            + "  <property name=\"message\">\n"
            + "   <string>message</string>\n"
            + "  </property>\n"
            + "  <property id=\"message\" name=\"message\"/>\n"
            + "  <property name=\"indexed\" index=\"1\">\n"
            + "   <string>indexed</string>\n"
            + "  </property>\n"
            + "  <property id=\"indexed\" name=\"indexed\" index=\"1\"/>\n"
            + " </property>\n"
            + " <var idref=\"message\"/>\n"
            + " <var idref=\"indexed\"/>\n"
            + "</java>";

    public static void main(String[] args) {
        new TestProperty().test(true);
    }

    private int index;
    private String message;

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getIndexed(int index) {
        if (this.index != index) {
            throw new Error("unexpected index");
        }
        return this.message;
    }

    public void setIndexed(int index, String message) {
        this.index = index;
        this.message = message;
    }

    @Override
    protected void validate(XMLDecoder decoder) {
        decoder.setOwner(this);
        validate(decoder, "message");
        validate(decoder, "indexed");
    }

    private static void validate(XMLDecoder decoder, String name) {
        if (!decoder.readObject().equals(name)) {
            throw new Error(name + " expected");
        }
    }
}
