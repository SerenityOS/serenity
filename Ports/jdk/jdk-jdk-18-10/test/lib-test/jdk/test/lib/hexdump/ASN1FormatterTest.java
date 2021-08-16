/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.hexdump;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.testng.Assert.*;

/*
 * @test
 * @summary ASN.1 formatting
 * @modules java.base/sun.security.util
 * @library /test/lib
 * @compile ASN1FormatterTest.java
 * @run testng jdk.test.lib.hexdump.ASN1FormatterTest
 */
@Test
public class ASN1FormatterTest {
    private static final String DIR = System.getProperty("test.src", ".");

    @Test
    static void testPEM() throws IOException {
        String certFile = "openssl.p12.pem";
        Path certPath = Path.of(DIR, certFile);
        System.out.println("certPath: " + certPath);

        try (InputStream certStream = Files.newInputStream(certPath)) {
            while (certStream.read() != '\n') {
                // Skip first line "-----BEGIN CERTIFICATE-----"
            }
            // Mime decoder for Certificate
            InputStream wis = Base64.getMimeDecoder().wrap(certStream);
            DataInputStream is = new DataInputStream(wis);
            String result = ASN1Formatter.formatter().annotate(is);
            System.out.println(result);

            Assert.assertEquals(result.lines().count(), 76, "Lines");
            Assert.assertEquals(result.lines().filter(s -> s.contains("SEQUENCE")).count(),24, "Sequences");
            Assert.assertEquals(result.lines().filter(s -> s.contains("OBJECT ID")).count(), 17, "ObjectIDs");
            Assert.assertEquals(result.lines().filter(s -> s.contains("UTCTIME")).count(), 2, "UTCTIME");
            Assert.assertEquals(result.lines().filter(s -> s.contains("BIT STRING")).count(), 3, "BitStrings");
        } catch (EOFException eof) {
            // done
        }
    }

    @Test
    static void dumpPEM() throws IOException {
        String file = "openssl.p12.pem";
        Path path = Path.of(DIR, file);
        System.out.println("path: " + path);

        try (InputStream certStream = Files.newInputStream(path)) {
            while (certStream.read() != '\n') {
                // Skip first line "-----BEGIN CERTIFICATE-----"
            }
            // Mime decoder for Certificate
            InputStream wis = Base64.getMimeDecoder().wrap(certStream);

            HexPrinter p = HexPrinter.simple()
                    .formatter(ASN1Formatter.formatter(), "; ", 100);
            String result = p.toString(wis);
            System.out.println(result);

            Assert.assertEquals(result.lines().count(), 126, "Lines");
            Assert.assertEquals(result.lines().filter(s -> s.contains("SEQUENCE")).count(), 24, "Sequences");
            Assert.assertEquals(result.lines().filter(s -> s.contains("OBJECT ID")).count(), 17, "ObjectIDs");
            Assert.assertEquals(result.lines().filter(s -> s.contains("UTCTIME")).count(), 2, "UTCTIME");
            Assert.assertEquals(result.lines().filter(s -> s.contains("BIT STRING")).count(), 3, "BitStrings");
        } catch (EOFException eof) {
            // done
        }
    }

    @Test
    static void testIndefinite() {
        byte[] bytes = {0x24, (byte) 0x80, 4, 2, 'a', 'b', 4, 2, 'c', 'd', 0, 0};
        HexPrinter p = HexPrinter.simple()
                .formatter(ASN1Formatter.formatter(), "; ", 100);
        String result = p.toString(bytes);
        System.out.println(result);

        Assert.assertEquals(result.lines().filter(s -> s.contains("OCTET STRING [INDEFINITE]")).count(),
                1, "Indefinite length");
        Assert.assertEquals(result.lines().filter(s -> s.contains(";   OCTET STRING [2]")).count(),
                2, "Octet Sequences");
        Assert.assertEquals(result.lines().filter(s -> s.contains(";   END-OF-CONTENT")).count(),
                1, "end of content");
    }

    @Test
    static void testMain() {
        String file = "openssl.p12.pem";
        Path path = Path.of(DIR, file);
        String[] args = { path.toString() };
        System.out.println("path: " + path);
        ASN1Formatter.main(args);
    }

}
