/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6388652 8062588 8210406
 * @summary Checks whether providers can be loaded from classpath.
 * @library providersrc/foobarutils
 *          providersrc/barprovider
 * @build com.foobar.Utils
 *        com.bar.*
 * @run main/othervm -Djava.locale.providers=JRE,SPI ClasspathTest
 */

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ClasspathTest {

    public static void main(String[] s) {
        new ClasspathTest();
    }

    ClasspathTest() {
        /*
         * Since providers can be loaded from the application's classpath,
         * this test will fail if they are NOT loaded from classpath.
         */
        Locale OSAKA = new Locale("ja", "JP", "osaka");
        List<Locale> availableLocales = Arrays.asList(Locale.getAvailableLocales());
        if (!availableLocales.contains(OSAKA)) {
            throw new RuntimeException("LSS providers were NOT loaded from the class path.");
        }
    }
}