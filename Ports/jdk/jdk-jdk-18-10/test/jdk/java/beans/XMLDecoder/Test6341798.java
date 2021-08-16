/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6341798
 * @summary Tests name generation for turkish locale
 * @author Sergey Malenkov
 */

import java.beans.XMLDecoder;
import java.io.ByteArrayInputStream;
import java.util.Locale;

import static java.util.Locale.ENGLISH;

public class Test6341798 {
    private static final Locale TURKISH = new Locale("tr");

    private static final String DATA
            = "<java>\n"
            + " <object class=\"Test6341798$DataBean\">\n"
            + "  <void property=\"illegal\">\n"
            + "   <boolean>true</boolean>\n"
            + "  </void>\n"
            + " </object>\n"
            + "</java> ";

    public static void main(String[] args) {
        Locale reservedLocale = Locale.getDefault();
        try {
            test(ENGLISH, DATA.getBytes());
            test(TURKISH, DATA.getBytes());
        } finally {
            // restore the reserved locale
            Locale.setDefault(reservedLocale);
        }
    }

    private static void test(Locale locale, byte[] data) {
        Locale.setDefault(locale);
        System.out.println("locale = " + locale);

        XMLDecoder decoder = new XMLDecoder(new ByteArrayInputStream(data));
        System.out.println("object = " + decoder.readObject());
        decoder.close();
    }

    public static class DataBean {
        private boolean illegal;

        public boolean isIllegal() {
            return this.illegal;
        }

        public void setIllegal(boolean illegal) {
            this.illegal = illegal;
        }

        public String toString() {
            if (this.illegal) {
                return "property is set";
            }
            throw new Error("property is not set");
        }
    }
}
