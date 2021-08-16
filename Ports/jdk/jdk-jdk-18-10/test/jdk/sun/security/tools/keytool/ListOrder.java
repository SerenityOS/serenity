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
 * @bug 8225392
 * @summary Comparison builds are failing due to cacerts file
 * @library /test/lib
 */

import jdk.test.lib.SecurityTools;

import java.util.Random;

public class ListOrder {

    public static void main(String[] args) throws Throwable {

        Random rand = new Random();
        for (int i = 0; i < 10; i++) {
            gen(String.format("a%02d", rand.nextInt(100)));
        }

        String last = "";
        for (String line : SecurityTools.keytool(
                "-keystore ks -storepass changeit -list").asLines()) {
            if (line.contains("PrivateKeyEntry")) {
                // This is the line starting with the alias
                System.out.println(line);
                if (line.compareTo(last) <= 0) {
                    throw new RuntimeException("Not ordered");
                } else {
                    last = line;
                }
            }
        }
    }

    static void gen(String a) throws Exception {
        // Do not check result, there might be duplicated alias(es).
        SecurityTools.keytool("-keystore ks -storepass changeit "
                + "-keyalg ec -genkeypair -alias " + a + " -dname CN=" + a);
    }
}
