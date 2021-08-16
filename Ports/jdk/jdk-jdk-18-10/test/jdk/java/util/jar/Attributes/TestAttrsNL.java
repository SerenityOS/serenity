/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8200530
 * @summary Test Attributes newline
 */

import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.io.ByteArrayInputStream;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class TestAttrsNL {

    public static void main(String[] args) throws Throwable {

        String manifestStr =
            "Manifest-Version: 1.0\r\n" +
            "Created-By: 11 (Oracle Corporation)\r\n" +
            "key1: value1\r\n" +
            "key2: value2\r\n END\r\n" +
            "key3: value3\r\n \r\n" +
            "key4: value4\r\n" +
            "\r\n\r\n" +
            "Name: Hello\r\n" +
            "key11: value11\r\n" +
            "key22: value22\r\n END\r\n" +
            "key33: value33\r\n \r\n" +
            "key44: value44\r\n";

        Map<Name, String> mainAttrsExped = Map.of(
                new Name("Manifest-Version"), "1.0",
                new Name("Created-By"), "11 (Oracle Corporation)",
                new Name("key1"), "value1",
                new Name("key2"), "value2END",
                new Name("key3"), "value3",
                new Name("key4"), "value4"
        );

        Map<Name, String> attrsExped = Map.of(
                new Name("key11"), "value11",
                new Name("key22"), "value22END",
                new Name("key33"), "value33",
                new Name("key44"), "value44"
        );

        test(new Manifest(new ByteArrayInputStream(manifestStr.getBytes(UTF_8))),
             mainAttrsExped, attrsExped);

        test(new Manifest(new ByteArrayInputStream(
                    manifestStr.replaceAll("\r\n", "\r").getBytes(UTF_8))),
             mainAttrsExped, attrsExped);

        test(new Manifest(new ByteArrayInputStream(
                    manifestStr.replaceAll("\r\n", "\n").getBytes(UTF_8))),
             mainAttrsExped, attrsExped);

        // mixed
        manifestStr =
            "Manifest-Version: 1.0\r\n" +
            "Created-By: 11 (Oracle Corporation)\n" +
            "key1: value1\r" +
            "key2: value2\r\n END\r" +
            "key3: value3\n \r\n" +
            "key4: value4\r" +
            "\r\n\n" +
            "Name: Hello\r\n" +
            "key11: value11\r" +
            "key22: value22\n END\r\n" +
            "key33: value33\r \n" +
            "key44: value44\n";
        test(new Manifest(new ByteArrayInputStream(manifestStr.getBytes(UTF_8))),
             mainAttrsExped, attrsExped);


    }

    private static void test(Manifest m,
                             Map<Name, String> mainAttrsExped,
                             Map<Name, String> attrsExped) {
        Attributes mainAttrs = m.getMainAttributes();
        mainAttrsExped.forEach( (k, v) -> {
            if (!mainAttrs.containsKey(k) || !mainAttrs.get(k).equals(v)) {
                System.out.printf(" containsKey(%s) : %b%n", k, mainAttrs.containsKey(k));
                System.out.printf("         get(%s) : %s%n", k, mainAttrs.get(k));
                throw new RuntimeException("expected attr: k=<" + k + ">, v=<" + v + ">");
            }
        });

        Attributes attrs = m.getAttributes("Hello");
        attrs.forEach( (k, v) -> {
            if (!attrs.containsKey(k) || !attrs.get(k).equals(v)) {
                System.out.printf(" containsKey(%s) : %b%n", k, attrs.containsKey(k));
                System.out.printf("         get(%s) : %s%n", k, attrs.get(k));
                throw new RuntimeException("expected attr: k=<" + k + ">, v=<" + v + ">");
            }
        });
    }
}
