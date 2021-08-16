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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import sun.security.util.ManifestDigester;
import jdk.test.lib.util.JarUtils;
import jdk.test.lib.SecurityTools;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8217375
 * @modules java.base/sun.security.util
 * @library /test/lib /lib/testlibrary
 * @run testng MainAttributesConfused
 * @summary Check that manifest individual section "Manifest-Main-Attributes"
 * does not interfere and is not confused with ManifestDigester internals.
 *
 * See also
 * jdk/test/jdk/sun/security/util/ManifestDigester/ManifestMainAttributes.java
 * for much more detailed api level tests
 */
public class MainAttributesConfused {

    static final String KEYSTORE_FILENAME = "test.jks";
    static final String MAIN_ATTRIBUTES_MARKER = null;

    @BeforeClass
    void prepareKeyStore() throws Exception {
        SecurityTools.keytool("-genkeypair -keyalg EC -keystore "
                + KEYSTORE_FILENAME + " -storepass changeit -keypass changeit"
                + " -alias a -dname CN=X").shouldHaveExitValue(0);
    }

    void testAddManifestSection(String sectionName) throws Exception {
        // create a signed jar
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
        String testFile = "test-" + sectionName;
        Files.write(Path.of(testFile), testFile.getBytes(UTF_8));
        String jarFilename = sectionName + ".jar";
        JarUtils.createJarFile(Path.of(jarFilename), manifest,
                Path.of("."), Path.of(testFile));
        SecurityTools.jarsigner("-keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -verbose -debug " + jarFilename + " a")
                .shouldHaveExitValue(0);

        // get the manifest of the signed jar with the signature digests, add
        // a new individual section, and write it back
        try (JarFile jar = new JarFile(jarFilename)) {
            manifest = jar.getManifest();
        }
        Attributes attrs = sectionName == MAIN_ATTRIBUTES_MARKER
                ? manifest.getMainAttributes()
                : manifest.getEntries().computeIfAbsent(sectionName,
                        n -> new Attributes());
        attrs.put(new Name("Some-Key"), "Some-Value");
        String jarFilenameAttrs = sectionName + "-attrs.jar";
        JarUtils.updateManifest(jarFilename, jarFilenameAttrs, manifest);

        // having just added another manifest entry (individual section) not
        // modifying existing digests or main attributes should not invalidate
        // the existing signature.
        SecurityTools.jarsigner("-verify -keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -debug -verbose " + jarFilenameAttrs +
                " a").shouldHaveExitValue(0);
    }

    @Test
    public void testAddOtherThanManifestMainAttributes() throws Exception {
        // any value but "Manifest-Main-Attributes", even lower case works
        testAddManifestSection("manifest-main-attributes");
    }

    @Test
    public void testAddMainAttributesHeader() throws Exception {
        // adding or changing existing attributes of the main section, however,
        // will invalidate the signature
        assertThrows(() -> testAddManifestSection(MAIN_ATTRIBUTES_MARKER));
    }

    @Test
    public void testAddManifestMainAttributesSection() throws Exception {
        testAddManifestSection(ManifestDigester.MF_MAIN_ATTRS);
    }

}
