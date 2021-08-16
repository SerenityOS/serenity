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
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;

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
 * @run testng EmptyJar
 * @summary Checks that signing an empty jar file does not result in an NPE or
 * other error condition.
 */
public class EmptyJar {

    static final String KEYSTORE_FILENAME = "test.jks";

    @BeforeClass
    public void prepareKeyStore() throws Exception {
        SecurityTools.keytool("-genkeypair -keyalg EC -keystore "
                + KEYSTORE_FILENAME + " -storepass changeit -keypass changeit"
                + " -alias a -dname CN=A").shouldHaveExitValue(0);
    }

    @Test
    public void test() throws Exception {
        String jarFilename = "test.jar";
        JarUtils.createJarFile(Path.of(jarFilename), (Manifest) null,
                Path.of("."));
        SecurityTools.jarsigner("-keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -verbose -debug " + jarFilename + " a")
                .shouldHaveExitValue(0);

        // verify that jarsigner has added a default manifest
        byte[] mfBytes = Utils.readJarManifestBytes(jarFilename);
        Utils.echoManifest(mfBytes, "manifest");
        assertTrue(new String(mfBytes, UTF_8).startsWith(
                Name.MANIFEST_VERSION + ": 1.0\r\nCreated-By: "));
    }

}
