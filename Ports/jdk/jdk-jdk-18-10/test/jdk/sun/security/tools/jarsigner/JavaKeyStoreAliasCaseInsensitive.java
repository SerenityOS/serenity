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

/*
 * @test
 * @bug 8221719
 * @library /test/lib
 * @run testng JavaKeyStoreAliasCaseInsensitive
 * @summary Checks that jarsigner verifies a signed jar with the same alias as
 * was specified for signing, particularly regarding upper and lower case and
 * its conversion to lower case by JKS
 * ({@link sun.security.provider.JavaKeyStore.JKS#convertAlias(String)}).
 */

import java.nio.file.Files;
import java.nio.file.Paths;
import jdk.test.lib.util.JarUtils;
import jdk.test.lib.SecurityTools;
import org.testng.annotations.Test;

public class JavaKeyStoreAliasCaseInsensitive {

    /**
     * Alias for certificates in the keystore with letters in different
     * (upper and lower) cases.
     */
    static final String ALIAS = "AlIaS";

    @Test
    public void testAliasCase() throws Exception {
        final String KEYSTORE_OPTIONS = "-storetype JKS -keystore "
                + "test-alias-case.jks -storepass changeit";
        SecurityTools.keytool(KEYSTORE_OPTIONS + " -genkeypair -keyalg DSA"
                + " -keypass changeit -alias " + ALIAS + " -dname CN=" + ALIAS)
                .shouldHaveExitValue(0);
        String jarFilename = "test-alias-case.jar";
        JarUtils.createJarFile(Paths.get(jarFilename), Paths.get("."),
                Files.write(Paths.get("aFile"), new byte[1]));

        SecurityTools.jarsigner(KEYSTORE_OPTIONS + " -verbose -debug " +
                jarFilename + " " + ALIAS).shouldHaveExitValue(0);

        SecurityTools.jarsigner("-verify -strict " + KEYSTORE_OPTIONS +
                " -debug -verbose " + jarFilename + " " + ALIAS)
                .shouldHaveExitValue(0)
                .shouldNotContain(
                        "This jar contains signed entries which are not "
                        + "signed by the specified alias(es).");
    }

    /**
     * This test essentially covers compatibility with the previous version of
     * {@link sun.security.tools.jarsigner.Main#inKeyStoreForOneSigner} in case
     * a certificate and alias entry is already in
     * {@link sun.security.tools.jarsigner.Main#storeHash} when
     * {@link sun.security.tools.jarsigner.Main#inKeyStoreForOneSigner} is
     * invoked from a previous invocation of it.
     * It passed with the previous {@code jarsigner} version with a lowercase
     * alias {@link #ALIAS} and basically covers the duplicated portions of
     * code in {@link sun.security.tools.jarsigner.Main#inKeyStoreForOneSigner}
     * near {@code IN_KEYSTORE} and {@code SIGNED_BY_ALIAS} before having
     * refactored and re-unified them in order to demonstrate identical
     * behavior.
     */
    @Test
    public void testAliasCaseStoreHash() throws Exception {
        // Create a keystore with a certificate associated with ALIAS + "2"
        // signed by another certificate associated with ALIAS + "1".
        final String KEYSTORE_OPTIONS = "-storetype JKS -keystore"
                + " test-alias-storeHash-case.jks -storepass changeit";
        SecurityTools.keytool(KEYSTORE_OPTIONS + " -genkeypair -keyalg DSA"
                + " -keypass changeit -alias " + ALIAS + "1 -dname CN=" +
                ALIAS + "1" + " -ext bc:c").shouldHaveExitValue(0);
        SecurityTools.keytool(KEYSTORE_OPTIONS + " -genkeypair -keyalg DSA"
                + " -keypass changeit -alias " + ALIAS + "2 -dname CN="
                + ALIAS + "2").shouldHaveExitValue(0);
        String certReq = SecurityTools.keytool(KEYSTORE_OPTIONS +
                " -certreq -keypass changeit -alias " + ALIAS + "2")
                .shouldHaveExitValue(0).getStdout();
        SecurityTools.setResponse(certReq);
        String cert = SecurityTools.keytool(KEYSTORE_OPTIONS +
                " -gencert -rfc -keypass changeit -alias " + ALIAS + "1")
                .shouldHaveExitValue(0).getOutput();
        SecurityTools.setResponse(cert);
        SecurityTools.keytool(KEYSTORE_OPTIONS +
                " -importcert -keypass changeit -alias " + ALIAS + "2")
                .shouldHaveExitValue(0);

        // Create a jar file signed by ALIAS + "2" and then add another file to
        // that same jar and sign it by ALIAS + "1", ALIAS + "1" being an alias
        // for a certificate which is part of the certificate chain of ALIAS +
        // "2" but not the certificate ALIAS + "2" points to directly.
        String jarFilename = "test-alias-storeHash-case.jar";
        JarUtils.createJarFile(Paths.get(jarFilename), Paths.get("."));
        SecurityTools.jarsigner(KEYSTORE_OPTIONS + " -verbose -debug " +
                jarFilename + " " + ALIAS + "2").shouldHaveExitValue(0);
        JarUtils.updateJarFile(Paths.get(jarFilename), Paths.get("."),
                Files.write(Paths.get("added-file"), new byte[1]));
        SecurityTools.jarsigner(KEYSTORE_OPTIONS + " -verbose -debug " +
                jarFilename + " " + ALIAS + "1").shouldHaveExitValue(0);

        // The later added file "added-file" is signed by the certificate
        // associated with alias ALIAS + "1" directly while the other jarfile
        // contents is signed by a certificate associated with alias ALIAS + "2"
        // which includes the certificate associated with alias ALIAS + "1" in
        // its certification path.
        SecurityTools.jarsigner("-verify -strict " + KEYSTORE_OPTIONS +
                " -debug -verbose " + jarFilename + " " + ALIAS + "1")
                .shouldHaveExitValue(0)
                .shouldNotContain(
                        "This jar contains signed entries which are not "
                        + "signed by the specified alias(es).");
    }

}
