/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8206879
 * @summary Currency decimal marker incorrect for Peru.
 * @modules jdk.localedata
 * @run main/othervm -Djava.locale.providers=JRE TestPeruCurrencyFormat
 */

import java.text.NumberFormat;
import java.util.Locale;

public class TestPeruCurrencyFormat {

    public static void main(String[] args) {
        final String expected = "S/.1,234.56";
        NumberFormat currencyFmt =
                NumberFormat.getCurrencyInstance(new Locale("es", "PE"));
        String s = currencyFmt.format(1234.56);

        if (!s.equals(expected)) {
            throw new RuntimeException("Currency format for Peru failed, expected " + expected + ", got " + s);
        }
    }
}
