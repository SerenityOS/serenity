/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @bug 8161527 8180568
 * @summary NPE is thrown if exempt application is bundled with specific
 *          cryptoPerms
 * @requires java.runtime.name ~= "OpenJDK.*"
 * @library /test/lib
 * @run main TestExemption
 */
import javax.crypto.*;
import java.nio.file.Path;
import java.security.*;
import java.util.ArrayList;
import java.util.List;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.JarUtils;

public class TestExemption {

    private static final String SRC = System.getProperty("test.src");
    private static final String CLASSES = System.getProperty("test.classes");
    private static final String NAME = TestExemption.class.getName();
    private static final String SRC_CLS = NAME + ".class";
    private static final String JAR_FILE = NAME + ".jar";
    private static final String CRYPT_PERM = "cryptoPerms";

    public static void main(String[] args) throws Exception {

        // With no argument passed, compile the same class, jar it and run the
        // test section of the jar file which is nothing but else section here.
        if (args.length == 0) {
            JarUtils.createJarFile(
                    Path.of(JAR_FILE), Path.of(CLASSES), Path.of(SRC_CLS));
            JarUtils.updateJarFile(
                    Path.of(JAR_FILE), Path.of(SRC), Path.of(CRYPT_PERM));
            OutputAnalyzer oa = ProcessTools.executeTestJava(
                    getParameters().toArray(String[]::new));
            System.out.println(oa.getOutput());
            oa.shouldHaveExitValue(0);
        } else {
            // Set the crypto policy to limited so that additional policy can be
            // supplemented through cryptoPerms when bundled inside a jar file.
            Security.setProperty("crypto.policy", "limited");
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(128);
            SecretKey key128 = kg.generateKey();

            kg.init(192);
            SecretKey key192 = kg.generateKey();

            kg.init(256);
            SecretKey key256 = kg.generateKey();

            int maxAllowed = Cipher.getMaxAllowedKeyLength("AES");
            System.out.println("Max allowed: " + maxAllowed);
            // With limited crypto and bundled cryptoPerms maximum allowed
            // length of AES is upto 192.
            if (maxAllowed > 192) {
                throw new RuntimeException(">192 not supported");
            }

            Cipher c = Cipher.getInstance("AES/CBC/NoPadding");
            System.out.println("Testing 128-bit");
            c.init(Cipher.ENCRYPT_MODE, key128);

            System.out.println("Testing 192-bit");
            c.init(Cipher.ENCRYPT_MODE, key192);
            try {
                System.out.println("Testing 256-bit");
                c.init(Cipher.ENCRYPT_MODE, key256);
                throw new RuntimeException("Shouldn't reach here");
            } catch (InvalidKeyException e) {
                System.out.println("Caught the right exception");
            }
            System.out.println("DONE!");
        }
    }

    private static List<String> getParameters() {

        List<String> cmds = new ArrayList<>();
        cmds.add("-cp");
        cmds.add(JAR_FILE);
        cmds.add(NAME);
        // Argument to run the Test section of class inside the jar file.
        cmds.add("run");
        return cmds;
    }

}
