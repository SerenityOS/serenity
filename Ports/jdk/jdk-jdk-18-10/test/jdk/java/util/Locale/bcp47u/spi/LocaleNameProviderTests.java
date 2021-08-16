/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @test
 * @bug 8176841
 * @summary Tests LocaleNameProvider SPIs
 * @library provider
 * @build provider/module-info provider/foo.LocaleNameProviderImpl
 * @run main/othervm -Djava.locale.providers=SPI LocaleNameProviderTests
 */

import java.util.Locale;

/**
 * Test LocaleNameProvider SPI with BCP47 U extensions
 *
 * Verifies getUnicodeExtensionKey() and getUnicodeExtensionType() methods in
 * LocaleNameProvider works.
 */
public class LocaleNameProviderTests {
    private static final String expected = "foo (foo_ca:foo_japanese)";

    public static void main(String... args) {
        String name = Locale.forLanguageTag("foo-u-ca-japanese").getDisplayName(new Locale("foo"));
        if (!name.equals(expected)) {
            throw new RuntimeException("Unicode extension key and/or type name(s) is incorrect. " +
                "Expected: \"" + expected + "\", got: \"" + name + "\"");
        }
    }
}
