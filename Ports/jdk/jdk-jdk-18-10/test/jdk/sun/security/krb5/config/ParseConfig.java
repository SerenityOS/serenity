/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6319046 8055045
 * @modules java.security.jgss/sun.security.krb5
 * @compile -XDignore.symbol.file ParseConfig.java
 * @run main/othervm ParseConfig
 * @summary Problem with parsing krb5.conf
 */

import sun.security.krb5.Config;

public class ParseConfig {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.security.krb5.conf",
                System.getProperty("test.src", ".") + "/krb5.conf");
        Config config = Config.getInstance();
        config.listTable();

        String sample = "kdc.example.com kdc2.example.com";
        for ( int i = 0; i < 4; i++ ) {
            String expected = config.getAll("realms", "EXAMPLE_" + i + ".COM", "kdc");
            if (!sample.equals(expected)) {
                throw new Exception("krb5.conf: unexpected kdc value \"" +
                        expected + "\"");
            }
        }

        // JDK-8055045: IOOBE when reading an empty value
        config.get("empty1", "NOVAL.COM");
        config.get("empty2", "NOVAL.COM");
        config.get("quote1", "NOVAL.COM");
        config.get("quote2", "NOVAL.COM");
    }
}
