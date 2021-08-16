/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8031572
 * @summary jarsigner -verify exits with 0 when a jar file is not properly signed
 * @modules java.base/sun.security.tools.keytool
 *          jdk.jartool/sun.security.tools.jarsigner
 *          jdk.jartool/sun.tools.jar
 * @run main EntriesOrder
 */

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class EntriesOrder {

    public static void main(String[] args) throws Exception {

        String[] entries = {
                "META-INF/",
                "META-INF/MANIFEST.MF",
                "META-INF/A.RSA",
                "META-INF/A.SF",
                "META-INF/inf",
                "a"};

        Map<String,byte[]> content = new HashMap<>();

        // We will create a jar containing entries above. Try all permutations
        // and confirm 1) When opened as a JarFile, we can always get 3 signed
        // ones (MANIFEST, inf, a), and 2) When opened as a JarInputStream,
        // when the order is correct (MANIFEST at beginning, followed by RSA/SF,
        // directory ignored), we can get 2 signed ones (inf, a).

        // Prepares raw files
        Files.write(Paths.get("a"), List.of("a"));
        Files.createDirectory(Paths.get("META-INF/"));
        Files.write(Paths.get("META-INF/inf"), List.of("inf"));

        // Pack, sign, and extract to get all files
        sun.tools.jar.Main m =
                new sun.tools.jar.Main(System.out, System.err, "jar");
        if (!m.run("cvf a.jar a META-INF/inf".split(" "))) {
            throw new Exception("jar creation failed");
        }
        sun.security.tools.keytool.Main.main(
                ("-keystore jks -storepass changeit -keypass changeit -dname" +
                        " CN=A -alias a -genkeypair -keyalg rsa").split(" "));
        sun.security.tools.jarsigner.Main.main(
                "-keystore jks -storepass changeit a.jar a".split(" "));
        m = new sun.tools.jar.Main(System.out, System.err, "jar");
        if (!m.run("xvf a.jar".split(" "))) {
            throw new Exception("jar extraction failed");
        }

        // Data
        for (String s: entries) {
            if (!s.endsWith("/")) {
                content.put(s, Files.readAllBytes(Paths.get(s)));
            }
        }

        // Test
        for (List<String> perm: Permute(entries)) {

            // Recreate a jar
            try (ZipOutputStream zos
                         = new ZipOutputStream(new FileOutputStream("x.jar"))) {
                for (String e: perm) {
                    zos.putNextEntry(new ZipEntry(e));
                    if (Paths.get(e).toFile().isDirectory()) continue;
                    zos.write(content.get(e));
                }
            }

            // Open with JarFile, number of signed entries should be 3.
            int cc = 0;
            try (JarFile jf = new JarFile("x.jar")) {
                Enumeration<JarEntry> jes = jf.entries();
                while (jes.hasMoreElements()) {
                    JarEntry je = jes.nextElement();
                    jf.getInputStream(je).readAllBytes();
                    Certificate[] certs = je.getCertificates();
                    if (certs != null && certs.length > 0) {
                        cc++;
                    }
                }
            }

            if (cc != 3) {
                System.out.println(perm + " - jf - " + cc);
                throw new Exception();
            }

            // Open with JarInputStream
            int signed;

            perm.remove("META-INF/");
            if (perm.get(0).equals("META-INF/MANIFEST.MF") &&
                    perm.get(1).contains("/A.") &&
                    perm.get(2).contains("/A.")) {
                signed = 2;     // Good order
            } else {
                signed = 0;     // Bad order. In this case, the number of signed
                                // entries is not documented. Just test impl.
            }

            cc = 0;
            try (JarInputStream jis
                         = new JarInputStream(new FileInputStream("x.jar"))) {
                while (true) {
                    JarEntry je = jis.getNextJarEntry();
                    if (je == null) break;
                    jis.readAllBytes();
                    Certificate[] certs = je.getCertificates();
                    if (certs != null && certs.length > 0) {
                        cc++;
                    }
                }
            }

            if (cc != signed) {
                System.out.println(perm + " - jis - " + cc + " " + signed);
                throw new Exception();
            }
        }
    }

    // Helper method to return all permutations of an array. Each output can
    // be altered without damaging the iteration process.
    static Iterable<List<String>> Permute(String[] entries) {
        return new Iterable<List<String>>() {

            int s = entries.length;
            long c = factorial(s) - 1;      // number of permutations

            private long factorial(int n) {
                return (n == 1) ? 1: (n * factorial(n-1));
            }

            @Override
            public Iterator<List<String>> iterator() {
                return new Iterator<List<String>>() {
                    @Override
                    public boolean hasNext() {
                        return c >= 0;
                    }

                    @Override
                    public List<String> next() {
                        if (c < 0) return null;
                        List<String> result = new ArrayList<>(s);
                        LinkedList<String> source = new LinkedList<>(
                                Arrays.asList(entries));
                        // Treat c as a integer with different radixes at
                        // different digits, i.e. at digit 0, radix is s;
                        // at digit 1, radix is s-1. Thus a s-digit number
                        // is able to represent s! different values.
                        long n = c;
                        for (int i=s; i>=1; i--) {
                            int x = (int)(n % i);
                            result.add(source.remove(x));
                            n = n / i;
                        }
                        c--;
                        return result;
                    }
                };
            }
        };
    }
}
