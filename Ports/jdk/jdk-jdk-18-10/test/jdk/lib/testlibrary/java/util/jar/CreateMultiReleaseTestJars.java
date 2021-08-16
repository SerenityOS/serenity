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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertPath;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;
import java.io.File;

import jdk.security.jarsigner.JarSigner;
import jdk.test.lib.compiler.Compiler;
import jdk.test.lib.util.JarBuilder;

public class CreateMultiReleaseTestJars {
    final private String main =
            "package version;\n\n"
            + "public class Main {\n"
            + "    public static void main(String[] args) {\n"
            + "        Version v = new Version();\n"
            + "        System.out.println(\"I am running on version \" + v.getVersion());\n"
            + "    }\n"
            + "}\n";
    final private String java8 =
            "package version;\n\n"
            + "public class Version {\n"
            + "    public int getVersion() {\n"
            + "        return 8;\n"
            + "    }\n"
            + "}\n";
    final private String java9 =
            "package version;\n\n"
            + "public class Version {\n"
            + "    public int getVersion() {\n"
            + "        int version = (new PackagePrivate()).getVersion();\n"
            + "        if (version == 9) return 9;\n"  // strange I know, but easy to test
            + "        return version;\n"
            + "    }\n"
            + "}\n";
    final private String ppjava9 =
            "package version;\n\n"
            + "class PackagePrivate {\n"
            + "    int getVersion() {\n"
            + "        return 9;\n"
            + "    }\n"
            + "}\n";
    final int currentVersion = Runtime.version().feature();
    final String currentVersionStr = Integer.toString(currentVersion);
    final private String javaCurrent = java8.replace("8", currentVersionStr);
    final String readme8 = "This is the root readme file";
    final String readme9 = "This is the version nine readme file";
    final String readmeCurrent = "This is the current version readme file";
    private Map<String,byte[]> rootClasses;
    private Map<String,byte[]> version9Classes;
    private Map<String,byte[]> versionCurrentClasses;

    public void buildUnversionedJar() throws IOException {
        JarBuilder jb = new JarBuilder("unversioned.jar");
        jb.addEntry("README", readme8.getBytes());
        jb.addEntry("version/Main.java", main.getBytes());
        jb.addEntry("version/Main.class", rootClasses.get("version.Main"));
        jb.addEntry("version/Version.java", java8.getBytes());
        jb.addEntry("version/Version.class", rootClasses.get("version.Version"));
        jb.build();
    }

    public void buildMultiReleaseJar() throws IOException {
        JarBuilder jb = customMultiReleaseJar("multi-release.jar", "true");
        addEntries(jb);
        jb.addEntry("META-INF/versions/9/version/Version.class", version9Classes.get("version.Version"));
        jb.build();
    }

    public void buildShortMultiReleaseJar() throws IOException {
        JarBuilder jb = customMultiReleaseJar("short-multi-release.jar", "true");
        addEntries(jb);
        jb.build();
    }

    private JarBuilder customMultiReleaseJar(String filename, String multiReleaseValue)
            throws IOException {
        JarBuilder jb = new JarBuilder(filename);
        jb.addAttribute("Multi-Release", multiReleaseValue);
        return jb;
    }

    public void buildCustomMultiReleaseJar(String filename, String multiReleaseValue,
            Map<String, String> extraAttributes) throws IOException {
        buildCustomMultiReleaseJar(filename, multiReleaseValue, extraAttributes, false);
    }

    public void buildCustomMultiReleaseJar(String filename, String multiReleaseValue,
            Map<String, String> extraAttributes, boolean addEntries) throws IOException {
        JarBuilder jb = new JarBuilder(filename);
        extraAttributes.entrySet()
                .forEach(entry -> jb.addAttribute(entry.getKey(), entry.getValue()));
        if (addEntries) {
            addEntries(jb);
        }
        jb.addAttribute("Multi-Release", multiReleaseValue);
        jb.build();
    }

    private void addEntries(JarBuilder jb) {
        jb.addEntry("README", readme8.getBytes());
        jb.addEntry("version/Main.java", main.getBytes());
        jb.addEntry("version/Main.class", rootClasses.get("version.Main"));
        jb.addEntry("version/Version.java", java8.getBytes());
        jb.addEntry("version/Version.class", rootClasses.get("version.Version"));
        jb.addEntry("META-INF/versions/9/README", readme9.getBytes());
        jb.addEntry("META-INF/versions/9/version/Version.java", java9.getBytes());
        jb.addEntry("META-INF/versions/9/version/PackagePrivate.java", ppjava9.getBytes());
        jb.addEntry("META-INF/versions/9/version/PackagePrivate.class", version9Classes.get("version.PackagePrivate"));
        jb.addEntry("META-INF/versions/" + currentVersionStr + "/README", readmeCurrent.getBytes());
        jb.addEntry("META-INF/versions/" + currentVersionStr + "/version/Version.java", javaCurrent.getBytes());
        jb.addEntry("META-INF/versions/" + currentVersionStr + "/version/Version.class", versionCurrentClasses.get("version.Version"));
    }

    public void buildSignedMultiReleaseJar() throws Exception {
        buildSignedMultiReleaseJar("multi-release.jar", "signed-multi-release.jar");
    }

    public void buildSignedMultiReleaseJar(String multiReleaseJar,
                                           String signedMultiReleaseJar) throws Exception
    {
        String testsrc = System.getProperty("test.src",".");
        String testdir = findTestDir(testsrc);
        String keystore = testdir + "/sun/security/tools/jarsigner/JarSigning.keystore";

        // jarsigner -keystore keystore -storepass "bbbbbb"
        //           -signedJar signed-multi-release.jar multi-release.jar b

        char[] password = "bbbbbb".toCharArray();
        KeyStore ks = KeyStore.getInstance(new File(keystore), password);
        PrivateKey pkb = (PrivateKey)ks.getKey("b", password);
        CertPath cp = CertificateFactory.getInstance("X.509")
                .generateCertPath(Arrays.asList(ks.getCertificateChain("b")));
        JarSigner js = new JarSigner.Builder(pkb, cp).build();
        try (ZipFile in = new ZipFile(multiReleaseJar);
             FileOutputStream os = new FileOutputStream(signedMultiReleaseJar))
        {
            js.sign(in, os);
        }
    }

    String findTestDir(String dir) throws IOException {
        Path path = Paths.get(dir).toAbsolutePath();
        Path child = null;
        while (path != null && !path.endsWith("test")) {
            child = path;
            path = path.getParent();
        }
        if (child == null) {
            throw new IllegalArgumentException(dir + " is not in a test directory");
        }
        if (!Files.isDirectory(child)) {
            throw new IOException(child.toString() + " is not a directory");
        }
        return child.toString();
    }

    void compileEntries() {
        Map<String,String> input = new HashMap<>();
        input.put("version.Main", main);
        input.put("version.Version", java8);
        rootClasses = (new Compiler(input)).setRelease(8).compile();
        input.clear();
        input.put("version.Version", java9);
        input.put("version.PackagePrivate", ppjava9);
        version9Classes = (new Compiler(input)).setRelease(9).compile();
        input.clear();
        input.put("version.Version", javaCurrent);
        versionCurrentClasses = (new Compiler(input)).compile();  // Use default release
    }
}
