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

import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;
import jdk.test.lib.util.JarUtils;
import jdk.test.lib.SecurityTools;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

/**
 * @test
 * @bug 8217375
 * @library /test/lib
 * @run testng RemoveDifferentKeyAlgBlockFile
 * @summary Checks that if a signed jar file is signed again with the same
 * signer name and a different algorithm that the signature block file for
 * the previous signature is removed. Example: the jar had META-INF/A.SF and
 * META-INF/A.RSA files and is now signed with DSA. So it should contain
 * an updated META-INF/A.SF and META-INF/A.DSA and the META-INF/A.RSA should
 * be removed because not valid any longer.
 */
public class RemoveDifferentKeyAlgBlockFile {

    static final String KEYSTORE_FILENAME = "test.jks";

    @BeforeClass
    public void prepareCertificates() throws Exception {
        SecurityTools.keytool("-genkeypair -keyalg RSA -keystore "
                + KEYSTORE_FILENAME + " -storepass changeit -keypass changeit"
                + " -alias RSA -dname CN=RSA").shouldHaveExitValue(0);
        SecurityTools.keytool("-genkeypair -keyalg DSA -keystore "
                + KEYSTORE_FILENAME + " -storepass changeit -keypass changeit"
                + " -alias DSA -dname CN=DSA").shouldHaveExitValue(0);
    }

    @Test
    public void testOtherAlgSigBlockFileRemoved() throws Exception {
        String jarFilename = "test.jar";
        JarUtils.createJarFile(Path.of(jarFilename), (Manifest) null,
                Path.of("."));
        SecurityTools.jarsigner("-keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -verbose -debug -sigfile A " +
                jarFilename + " RSA").shouldHaveExitValue(0);

        // change the jar file to invalidate the first signature with RSA
        String jarFilenameModified = "modified.jar";
        try (JarFile jar = new JarFile(jarFilename)) {
            Manifest manifest = jar.getManifest();
            manifest.getMainAttributes().put(
                    new Name("Some-Key"), "Some-Value");
            JarUtils.updateManifest(jarFilename, jarFilenameModified, manifest);
        }

        SecurityTools.jarsigner("-keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -verbose -debug -sigfile A " +
                jarFilenameModified + " DSA").shouldHaveExitValue(0);
        SecurityTools.jarsigner("-verify -keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -debug -verbose " + jarFilenameModified)
                .shouldHaveExitValue(0);
    }

}
