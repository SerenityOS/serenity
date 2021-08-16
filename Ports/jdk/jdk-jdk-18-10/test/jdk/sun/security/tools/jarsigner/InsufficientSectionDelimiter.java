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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import jdk.test.lib.util.JarUtils;
import jdk.test.lib.SecurityTools;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @test
 * @bug 8217375
 * @library /test/lib
 * @run testng InsufficientSectionDelimiter
 * @summary Checks some cases signing a jar the manifest of which has no or
 * only one line break at the end and no proper delimiting blank line does not
 * result in an invalid signed jar without jarsigner noticing and failing.
 *
 * <p>See also<ul>
 * <li>{@link PreserveRawManifestEntryAndDigest} with an update of a signed
 * jar with a different signer whereas this test just signs with one signer
 * </li>
 * <li>{@link WasSignedByOtherSigner} for a test that detects if
 * {@code wasSigned} in {@link jdk.security.jarsigner.JarSigner#sign0} was set
 * correctly determining whether or not to re-write the manifest, and</li>
 * <li>{@code diffend.sh} for another similar test</li></ul>
 */
public class InsufficientSectionDelimiter {

    static final String KEYSTORE_FILENAME = "test.jks";

    @BeforeTest
    public void prepareCertificate() throws Exception {
        SecurityTools.keytool("-genkeypair -keyalg EC -keystore "
                + KEYSTORE_FILENAME + " -storepass changeit -keypass changeit"
                + " -alias a -dname CN=A").shouldHaveExitValue(0);
    }

    @BeforeTest
    public void prepareFakeSfFile() throws IOException {
        new File("META-INF").mkdir();
        Files.write(Path.of("META-INF/.SF"), (
                Name.SIGNATURE_VERSION + ": 1.0\r\n" +
                "-Digest-Manifest: \r\n\r\n").getBytes(UTF_8));
    }

    @DataProvider(name = "parameters")
    public static Object[][] parameters() {
        return new String[][] { { "" }, { "\n" }, { "\r" }, { "\r\n" } };
    }

    @Factory(dataProvider = "parameters")
    public static Object[] createTests(String lineBreak) {
        return new Object[] { new InsufficientSectionDelimiter(lineBreak) };
    }

    final String lineBreak;
    final String jarFilenameSuffix;

    InsufficientSectionDelimiter(String lineBreak) {
        this.lineBreak = lineBreak;
        jarFilenameSuffix = Utils.escapeStringWithNumbers(lineBreak);
    }

    @BeforeMethod
    public void verbose() {
        System.out.println("lineBreak = "
                + Utils.escapeStringWithNumbers(lineBreak));
    }

    void test(String jarFilenamePrefix, String... files) throws Exception {
        String jarFilename = jarFilenamePrefix + jarFilenameSuffix + ".jar";
        JarUtils.createJarFile(Path.of(jarFilename), new Manifest() {
            @Override public void write(OutputStream out) throws IOException {
                out.write((Name.MANIFEST_VERSION + ": 1.0" +
                        lineBreak).getBytes(UTF_8));
            }
        }, Path.of("."), Stream.of(files).map(Path::of).toArray(Path[]::new)
        );
        Utils.echoManifest(Utils.readJarManifestBytes(
                jarFilename), "unsigned jar");
        try {
            SecurityTools.jarsigner("-keystore " + KEYSTORE_FILENAME +
                    " -storepass changeit -verbose -debug " + jarFilename +
                    " a").shouldHaveExitValue(0);
            Utils.echoManifest(Utils.readJarManifestBytes(
                    jarFilename), "signed jar");
        } catch (Exception e) {
            if (lineBreak.isEmpty()) {
                return; // invalid manifest without trailing line break
            }
            throw e;
        }

        // remove META-INF/.SF from signed jar which would not validate
        // (not added in all the test cases)
        JarUtils.updateJar(jarFilename, "verify-" + jarFilename,
                Map.of("META-INF/.SF", false));
        SecurityTools.jarsigner("-verify -strict -keystore " +
                KEYSTORE_FILENAME + " -storepass changeit -debug -verbose " +
                "verify-" + jarFilename + " a").shouldHaveExitValue(0);
    }

    /**
     * Test that signing a jar which has never been signed yet and contains
     * no signature related files with a manifest that ends immediately after
     * the last main attributes value byte or only one line break and no blank
     * line produces a valid signed jar or an error if the manifest ends
     * without line break.
     */
    @Test
    public void testOnlyMainAttrs() throws Exception {
        test("testOnlyMainAttrs");
    }

    /**
     * Test that signing a jar with a manifest that ends immediately after
     * the last main attributes value byte or with too few line break
     * characters to properly delimit an individual section and has a fake
     * signing related file to trigger a signature update or more specifically
     * wasSigned in JarSigner.sign0 to become true produces a valid signed jar
     * or an error if the manifest ends without line break.
     * <p>
     * Only one line break and hence no blank line ('\r', '\n', or '\r\n')
     * after last main attributes value byte is too little to delimit an
     * individual section to hold a file's digest but acceptable if no
     * individual section has to be added because no contained file has to be
     * signed as is the case in this test.
     *
     * @see #testMainAttrsWasSignedAddFile
     */
    @Test
    public void testMainAttrsWasSigned() throws Exception {
        test("testMainAttrsWasSigned", "META-INF/.SF");
    }

    /**
     * Test that signing a jar with a manifest that ends immediately after
     * the last main attributes value byte or with too few line break
     * characters to properly delimit an individual section and has a fake
     * signing related file to trigger a signature update or more specifically
     * wasSigned in JarSigner.sign0 to become true produces no invalid signed
     * jar or an error if the manifest ends without line break.
     * <p>
     * Only one line break and hence no blank line ('\r', '\n', or '\r\n')
     * after the last main attributes value byte is too little to delimit an
     * individual section which would be required here to save the digest of a
     * contained file to be signed.
     * <p>
     * Changing the delimiters after the main attributes changes the main
     * attributes digest but
     * {@link SignatureFileVerifier#verifyManifestMainAttrs} and
     * {@link ManifestDigester#digestWorkaround} work around it.
     */
    @Test
    public void testMainAttrsWasSignedAddFile() throws Exception {
        Files.write(Path.of("test.txt"), "test.txt".getBytes(UTF_8));
        test("testMainAttrsWasSignedAddFile", "META-INF/.SF", "test.txt");
    }

}
