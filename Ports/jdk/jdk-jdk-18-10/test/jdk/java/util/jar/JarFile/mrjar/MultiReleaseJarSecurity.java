/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8132734 8144062
 * @summary Test potential security related issues
 * @library /lib/testlibrary/java/util/jar /test/lib/
 * @build CreateMultiReleaseTestJars
 *        jdk.test.lib.compiler.Compiler
 *        jdk.test.lib.util.JarBuilder
 * @run testng MultiReleaseJarSecurity
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class MultiReleaseJarSecurity {

    static final int MAJOR_VERSION = Runtime.version().major();

    String userdir = System.getProperty("user.dir",".");
    File multirelease = new File(userdir, "multi-release.jar");
    File signedmultirelease = new File(userdir, "signed-multi-release.jar");

    @BeforeClass
    public void initialize() throws Exception {
        CreateMultiReleaseTestJars creator =  new CreateMultiReleaseTestJars();
        creator.compileEntries();
        creator.buildMultiReleaseJar();
        creator.buildSignedMultiReleaseJar();
    }

    @AfterClass
    public void close() throws IOException {
        Files.delete(multirelease.toPath());
        Files.delete(signedmultirelease.toPath());
    }

    @Test
    public void testCertsAndSigners() throws IOException {
        try (JarFile jf = new JarFile(signedmultirelease, true, ZipFile.OPEN_READ, Runtime.version())) {
            CertsAndSigners vcas = new CertsAndSigners(jf, jf.getJarEntry("version/Version.class"));
            CertsAndSigners rcas = new CertsAndSigners(jf, jf.getJarEntry("META-INF/versions/" + MAJOR_VERSION + "/version/Version.class"));
            Assert.assertTrue(Arrays.equals(rcas.getCertificates(), vcas.getCertificates()));
            Assert.assertTrue(Arrays.equals(rcas.getCodeSigners(), vcas.getCodeSigners()));
        }
    }

    private static class CertsAndSigners {
        final private JarFile jf;
        final private JarEntry je;
        private boolean readComplete;

        CertsAndSigners(JarFile jf, JarEntry je) {
            this.jf = jf;
            this.je = je;
        }

        Certificate[] getCertificates() throws IOException {
            readEntry();
            return je.getCertificates();
        }

        CodeSigner[] getCodeSigners() throws IOException {
            readEntry();
            return je.getCodeSigners();
        }

        private void readEntry() throws IOException {
            if (!readComplete) {
                try (InputStream is = jf.getInputStream(je)) {
                    is.readAllBytes();
                }
                readComplete = true;
            }
        }
    }
}
