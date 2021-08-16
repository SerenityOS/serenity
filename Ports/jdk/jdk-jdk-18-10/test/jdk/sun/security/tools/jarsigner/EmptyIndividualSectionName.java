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

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;

import jdk.test.lib.util.JarUtils;
import jdk.test.lib.SecurityTools;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import static org.testng.Assert.*;

/**
 * @test
 * @bug 8217375
 * @library /test/lib
 * @modules java.base/java.util.jar:+open
 * @run testng/othervm EmptyIndividualSectionName
 * @summary Check that an individual section with an empty name is digested
 * and signed.
 * <p>
 * See also
 * jdk/test/jdk/sun/security/util/ManifestDigester/FindSections.java
 * for much more detailed api level tests
 */
public class EmptyIndividualSectionName {

    static final String KEYSTORE_FILENAME = "test.jks";

    @BeforeClass
    public void prepareCertificate() throws Exception {
        SecurityTools.keytool("-genkeypair -keyalg EC -keystore "
                + KEYSTORE_FILENAME + " -storepass changeit -keypass changeit "
                + "-alias a -dname CN=X").shouldHaveExitValue(0);
    }

    /**
     * Adds an additional section with name {@code sectionName} to the manifest
     * of a JAR before signing it with {@code signOpts}.
     * @return signature file {@code META-INF/A.SF} for further assertions
     */
    Manifest test(String sectionName, String signOpts) throws Exception {
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
        mf.getEntries().put(sectionName, new Attributes());
        String jarFilename = "test" + sectionName +
                (signOpts != null ? signOpts : "") + ".jar";
        JarUtils.createJarFile(Path.of(jarFilename), mf, Path.of("."));
        SecurityTools.jarsigner("-keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -verbose -debug " +
                (signOpts != null ? signOpts + " " : "") + jarFilename + " a")
                .shouldHaveExitValue(0);
        SecurityTools.jarsigner("-verify -keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -debug -verbose " + jarFilename + " a")
                .shouldHaveExitValue(0);

        byte[] mfBytes = Utils.readJarManifestBytes(jarFilename);
        Utils.echoManifest(mfBytes, "manifest");
        mf = new Manifest(new ByteArrayInputStream(mfBytes));
        assertNotNull(mf.getAttributes(sectionName));
        byte[] sfBytes = Utils.readJarEntryBytes(jarFilename, "META-INF/A.SF");
        Utils.echoManifest(sfBytes, "signature file META-INF/A.SF");
        return new Manifest(new ByteArrayInputStream(sfBytes));
    }

    /**
     * Verifies that it makes a difference if the name is empty or not
     * by running the same test as {@link #testNameEmpty} with only a different
     * section name.
     */
    @Test
    public void testNameNotEmpty() throws Exception {
        String sectionName = "X";
        assertNotNull(test(sectionName, null).getAttributes(sectionName));
    }

    /**
     * Verifies that individual sections are digested and signed also if the
     * name of such a section is empty.
     * An empty name of an individual section cannot be tested by adding a file
     * with an empty name to a JAR because such a file name is invalid and
     * cannot be used to add a file because it cannot be created or added to
     * the JAR file in the first place. However, an individual section with an
     * empty name can be added to the manifest.
     * Expected is a corresponding digest in the signature file which was not
     * present or produced before resolution of bug 8217375.
     */
    @Test
    public void testNameEmpty() throws Exception {
        String sectionName = "";
        assertNotNull(test(sectionName, null).getAttributes(sectionName));
    }

    /**
     * Similar to {@link #testNameEmpty} but tries to show a real difference
     * rather than just some internals in a {@code .SF} file, but TODO
     */
    @Test(enabled = false, description = "TODO")
    public void testNameEmptyTrusted() throws Exception {
        String sectionName = "";
        test(sectionName, "-sectionsonly");
        String jarFilename = "test" + sectionName + "-sectionsonly.jar";
        try (JarFile jar = new JarFile(jarFilename, true)) {
            Manifest m = jar.getManifest();
            Method getTrustedAttributes = m.getClass()
                    .getDeclaredMethod("getTrustedAttributes", String.class);
            getTrustedAttributes.setAccessible(true);
            assertThrows(SecurityException.class, () ->
                    getTrustedAttributes.invoke(m, sectionName));
        }
    }

}
