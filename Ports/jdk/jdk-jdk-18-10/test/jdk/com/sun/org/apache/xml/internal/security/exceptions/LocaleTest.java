/*
 * Copyright (c) 2006, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6454215
 * @summary Make sure there are no runtime errors when throwing Apache XML
 *      Security exceptions in a non-US default locale.
 * @modules java.xml.crypto/com.sun.org.apache.xml.internal.security.exceptions
 * @compile -XDignore.symbol.file LocaleTest.java
 * @run main LocaleTest
 */
import java.util.Locale;
import com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityException;

public class LocaleTest {

    public static void main(String[] args) throws Exception {

        Locale reservedLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ITALY);

            throw new XMLSecurityException("foo");
        } catch (XMLSecurityException xse) {
            System.out.println("Test PASSED");
        } catch (Throwable t) {
            System.out.println("Test FAILED");
            t.printStackTrace();
        } finally {
            // restore the reserved locale
            Locale.setDefault(reservedLocale);
        }
    }
}
