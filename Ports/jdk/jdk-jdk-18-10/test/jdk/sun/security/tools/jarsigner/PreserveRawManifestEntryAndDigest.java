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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.Platform;
import jdk.test.lib.SecurityTools;
import jdk.test.lib.util.JarUtils;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8217375
 * @library /test/lib
 * @modules jdk.jartool/sun.security.tools.jarsigner
 * @run testng/timeout=1200 PreserveRawManifestEntryAndDigest
 * @summary Verifies that JarSigner does not change manifest file entries
 * in a binary view if its decoded map view does not change so that an
 * unchanged (individual section) entry continues to produce the same digest.
 * The same manifest (in terms of {@link Manifest#equals}) could be encoded
 * with different line breaks ("{@code \r}", "{@code \n}", or "{@code \r\n}")
 * or with arbitrary line break positions (as is also the case with the change
 * of the default line width in JDK 11, bug 6372077) resulting in a different
 * digest for manifest entries with identical values.
 *
 * <p>See also:<ul>
 * <li>{@code oldsig.sh} and {@code diffend.sh} in
 * {@code /test/jdk/sun/security/tools/jarsigner/}</li>
 * <li>{@code Compatibility.java} in
 * {@code /test/jdk/sun/security/tools/jarsigner/compatibility}</li>
 * <li>{@link ReproduceRaw} testing relevant
 * {@sun.security.util.ManifestDigester} api in much more detail</li>
 * </ul>
 */
/*
 * debug with "run testng" += "/othervm -Djava.security.debug=jar"
 */
public class PreserveRawManifestEntryAndDigest {

    static final String KEYSTORE_FILENAME = "test.jks";
    static final String FILENAME_INITIAL_CONTENTS = "initial-contents";
    static final String FILENAME_UPDATED_CONTENTS = "updated-contents";

    /**
     * @see sun.security.tools.jarsigner.Main#run
     */
    static final int NOTSIGNEDBYALIASORALIASNOTINSTORE = 32;

    @BeforeTest
    public void prepareContentFiles() throws IOException {
        Files.write(Path.of(FILENAME_INITIAL_CONTENTS),
                FILENAME_INITIAL_CONTENTS.getBytes(UTF_8));
        Files.write(Path.of(FILENAME_UPDATED_CONTENTS),
                FILENAME_UPDATED_CONTENTS.getBytes(UTF_8));
    }

    @BeforeTest
    public void prepareCertificates() throws Exception {
        SecurityTools.keytool("-genkeypair -keyalg DSA -keystore "
                + KEYSTORE_FILENAME + " -storepass changeit -keypass changeit"
                + " -alias a -dname CN=A").shouldHaveExitValue(0);
        SecurityTools.keytool("-genkeypair -keyalg DSA -keystore "
                + KEYSTORE_FILENAME + " -storepass changeit -keypass changeit"
                + " -alias b -dname CN=B").shouldHaveExitValue(0);
    }

    static class TeeOutputStream extends FilterOutputStream {
        final OutputStream tee; // don't flush or close

        public TeeOutputStream(OutputStream out, OutputStream tee) {
            super(out);
            this.tee = tee;
        }

        @Override
        public void write(int b) throws IOException {
            super.write(b);
            tee.write(b);
        }
    }

    /**
     * runs jarsigner in its own child process and captures exit code and the
     * output of stdout and stderr, as opposed to {@link #karsignerMain}
     */
    OutputAnalyzer jarsignerProc(String args) throws Exception {
        long start = System.currentTimeMillis();
        try {
            return SecurityTools.jarsigner(args);
        } finally {
            long end = System.currentTimeMillis();
            System.out.println("jarsignerProc duration [ms]: " + (end - start));
        }
    }

    /**
     * assume non-zero exit code would call System.exit but is faster than
     * {@link #jarsignerProc}
     */
    void jarsignerMain(String args) throws Exception {
        long start = System.currentTimeMillis();
        try {
            new sun.security.tools.jarsigner.Main().run(args.split("\\s+"));
        } finally {
            long end = System.currentTimeMillis();
            System.out.println("jarsignerMain duration [ms]: " + (end - start));
        }
    }

    void createSignedJarA(String jarFilename, Manifest manifest,
            String additionalJarsignerOptions, String dummyContentsFilename)
                    throws Exception {
        JarUtils.createJarFile(Path.of(jarFilename), manifest, Path.of("."),
                dummyContentsFilename == null ? new Path[]{} :
                    new Path[] { Path.of(dummyContentsFilename) });
        jarsignerMain("-keystore " + KEYSTORE_FILENAME + " -storepass changeit"
                + (additionalJarsignerOptions == null ? "" :
                    " " + additionalJarsignerOptions) +
                " -verbose -debug " + jarFilename + " a");
        Utils.echoManifest(Utils.readJarManifestBytes(
                jarFilename), "original signed jar by signer a");
        // check assumption that jar is valid at this point
        jarsignerMain("-verify -keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -verbose -debug " + jarFilename + " a");
    }

    void manipulateManifestSignAgainA(String srcJarFilename, String tmpFilename,
            String dstJarFilename, String additionalJarsignerOptions,
            Function<Manifest, byte[]> manifestManipulation) throws Exception {
        Manifest mf;
        try (JarFile jar = new JarFile(srcJarFilename)) {
            mf = jar.getManifest();
        }
        byte[] manipulatedManifest = manifestManipulation.apply(mf);
        Utils.echoManifest(manipulatedManifest, "manipulated manifest");
        JarUtils.updateJar(srcJarFilename, tmpFilename, Map.of(
                JarFile.MANIFEST_NAME, manipulatedManifest,
                // add a fake sig-related file to trigger wasSigned in JarSigner
                "META-INF/.SF", Name.SIGNATURE_VERSION + ": 1.0\r\n"));
        jarsignerMain("-keystore " + KEYSTORE_FILENAME + " -storepass changeit"
                + (additionalJarsignerOptions == null ? "" :
                    " " + additionalJarsignerOptions) +
                " -verbose -debug " + tmpFilename + " a");
        // remove META-INF/.SF from signed jar again which would not validate
        JarUtils.updateJar(tmpFilename, dstJarFilename,
                Map.of("META-INF/.SF", false));

        Utils.echoManifest(Utils.readJarManifestBytes(
                dstJarFilename), "manipulated jar signed again with a");
        // check assumption that jar is valid at this point
        jarsignerMain("-verify -keystore " + KEYSTORE_FILENAME + " " +
                "-storepass changeit -verbose -debug " + dstJarFilename + " a");
    }

    OutputAnalyzer signB(String jarFilename, String additionalJarsignerOptions,
            int updateExitCodeVerifyA) throws Exception {
        jarsignerMain("-keystore " + KEYSTORE_FILENAME + " -storepass changeit"
                + (additionalJarsignerOptions == null ? "" :
                    " " + additionalJarsignerOptions)
                + " -verbose -debug " + jarFilename + " b");
        Utils.echoManifest(Utils.readJarManifestBytes(
                jarFilename), "signed again with signer b");
        // check assumption that jar is valid at this point with any alias
        jarsignerMain("-verify -strict -keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -debug -verbose " + jarFilename);
        // check assumption that jar is valid at this point with b just signed
        jarsignerMain("-verify -strict -keystore " + KEYSTORE_FILENAME +
                " -storepass changeit -debug -verbose " + jarFilename + " b");
        // return result of verification of signature by a before update
        return jarsignerProc("-verify -strict " + "-keystore " +
                KEYSTORE_FILENAME + " -storepass changeit " + "-debug " +
                "-verbose " + jarFilename + " a")
                .shouldHaveExitValue(updateExitCodeVerifyA);
    }

    String[] fromFirstToSecondEmptyLine(String[] lines) {
        int from = 0;
        for (int i = 0; i < lines.length; i++) {
            if ("".equals(lines[i])) {
                from = i + 1;
                break;
            }
        }

        int to = lines.length - 1;
        for (int i = from; i < lines.length; i++) {
            if ("".equals(lines[i])) {
                to = i - 1;
                break;
            }
        }

        return Arrays.copyOfRange(lines, from, to + 1);
    }

    /**
     * @see "concise_jarsigner.sh"
     */
    String[] getExpectedJarSignerOutputUpdatedContentNotValidatedBySignerA(
            String jarFilename, String digestalg,
            String firstAddedFilename, String secondAddedFilename) {
        final String TS = ".{28,29}"; // matches a timestamp
        List<String> expLines = new ArrayList<>();
        expLines.add("s k   *\\d+ " + TS + " META-INF/MANIFEST[.]MF");
        expLines.add("      *\\d+ " + TS + " META-INF/B[.]SF");
        expLines.add("      *\\d+ " + TS + " META-INF/B[.]DSA");
        expLines.add("      *\\d+ " + TS + " META-INF/A[.]SF");
        expLines.add("      *\\d+ " + TS + " META-INF/A[.]DSA");
        if (firstAddedFilename != null) {
            expLines.add("smk   *\\d+ " + TS + " " + firstAddedFilename);
        }
        if (secondAddedFilename != null) {
            expLines.add("smkX  *\\d+ " + TS + " " + secondAddedFilename);
        }
        return expLines.toArray(new String[expLines.size()]);
    }

    void assertMatchByLines(String[] actLines, String[] expLines) {
        for (int i = 0; i < actLines.length && i < expLines.length; i++) {
            String actLine = actLines[i];
            String expLine = expLines[i];
            assertTrue(actLine.matches("^" + expLine + "$"),
                "\"" + actLine + "\" should have matched \"" + expLine + "\"");
        }
        assertEquals(actLines.length, expLines.length);
    }

    String test(String name, Function<Manifest, byte[]> mm) throws Exception {
        return test(name, FILENAME_INITIAL_CONTENTS, FILENAME_UPDATED_CONTENTS,
                mm);
    }

    String test(String name,
            String firstAddedFilename, String secondAddedFilename,
            Function<Manifest, byte[]> mm) throws Exception {
        return test(name, firstAddedFilename, secondAddedFilename, mm, null,
                true, true);
    }

    /**
     * Essentially, creates a first signed JAR file with a single contained
     * file or without and a manipulation applied to its manifest signed by
     * signer a and then signes it again with a different signer b.
     * The jar file is signed twice with signer a in order to make the digests
     * available to the manipulation function that might use it.
     *
     * @param name Prefix for the JAR filenames used throughout the test.
     * @param firstAddedFilename Name of a file to add before the first
     * signature by signer a or null. The name will also become the contents
     * if not null.
     * @param secondAddedFilename Name of a file to add after the first
     * signature by signer a and before the second signature by signer b or
     * null. The name will also become the contents if not null.
     * @param manifestManipulation A callback hook to manipulate the manifest
     * after the first signature by signer a and before the second signature by
     * signer b.
     * @param digestalg The digest algorithm name to be used or null for
     * default.
     * @param assertMainAttrsDigestsUnchanged Assert that the
     * manifest main attributes digests have not changed. In any case the test
     * also checks that the digests are still valid whether changed or not
     * by {@code jarsigner -verify} which might use
     * {@link ManifestDigester.Entry#digestWorkaround}
     * @param assertFirstAddedFileDigestsUnchanged Assert that the
     * digest of the file firstAddedFilename has not changed with the second
     * signature. In any case the test checks that the digests are valid whether
     * changed or not by {@code jarsigner -verify} which might use
     * {@link ManifestDigester.Entry#digestWorkaround}
     * @return The name of the resulting JAR file that has passed the common
     * assertions ready for further examination
     */
    String test(String name,
            String firstAddedFilename, String secondAddedFilename,
            Function<Manifest, byte[]> manifestManipulation,
            String digestalg, boolean assertMainAttrsDigestsUnchanged,
            boolean assertFirstAddedFileDigestsUnchanged)
                    throws Exception {
        String digOpts = (digestalg != null ? "-digestalg " + digestalg : "");
        String jarFilename1 = "test-" + name + "-step1.jar";
        createSignedJarA(jarFilename1,
                /* no manifest will let jarsigner create a default one */ null,
                digOpts, firstAddedFilename);

        // manipulate the manifest, write it back, and sign the jar again with
        // the same certificate a as before overwriting the first signature
        String jarFilename2 = "test-" + name + "-step2.jar";
        String jarFilename3 = "test-" + name + "-step3.jar";
        manipulateManifestSignAgainA(jarFilename1, jarFilename2, jarFilename3,
                digOpts, manifestManipulation);

        // add another file, sign it with the other certificate, and verify it
        String jarFilename4 = "test-" + name + "-step4.jar";
        JarUtils.updateJar(jarFilename3, jarFilename4,
                secondAddedFilename != null ?
                Map.of(secondAddedFilename, secondAddedFilename)
                : Collections.EMPTY_MAP);
        OutputAnalyzer o = signB(jarFilename4, digOpts,
           secondAddedFilename != null ? NOTSIGNEDBYALIASORALIASNOTINSTORE : 0);
        // check that secondAddedFilename is the only entry which is not signed
        // by signer with alias "a" unless secondAddedFilename is null
        assertMatchByLines(
                fromFirstToSecondEmptyLine(o.getStdout().split("\\R")),
                getExpectedJarSignerOutputUpdatedContentNotValidatedBySignerA(
                        jarFilename4, digestalg,
                        firstAddedFilename, secondAddedFilename));

        // double-check reading the files with a verifying JarFile
        try (JarFile jar = new JarFile(jarFilename4, true)) {
            if (firstAddedFilename != null) {
                JarEntry je1 = jar.getJarEntry(firstAddedFilename);
                jar.getInputStream(je1).readAllBytes();
                assertTrue(je1.getCodeSigners().length > 0);
            }
            if (secondAddedFilename != null) {
                JarEntry je2 = jar.getJarEntry(secondAddedFilename);
                jar.getInputStream(je2).readAllBytes();
                assertTrue(je2.getCodeSigners().length > 0);
            }
        }

        // assert that the signature of firstAddedFilename signed by signer
        // with alias "a" is not lost and its digest remains the same
        try (ZipFile zip = new ZipFile(jarFilename4)) {
            ZipEntry ea = zip.getEntry("META-INF/A.SF");
            Manifest sfa = new Manifest(zip.getInputStream(ea));
            ZipEntry eb = zip.getEntry("META-INF/B.SF");
            Manifest sfb = new Manifest(zip.getInputStream(eb));
            if (assertMainAttrsDigestsUnchanged) {
                String mainAttrsDigKey =
                        (digestalg != null ? digestalg : "SHA-256") +
                        "-Digest-Manifest-Main-Attributes";
                assertEquals(sfa.getMainAttributes().getValue(mainAttrsDigKey),
                             sfb.getMainAttributes().getValue(mainAttrsDigKey));
            }
            if (assertFirstAddedFileDigestsUnchanged) {
                assertEquals(sfa.getAttributes(firstAddedFilename),
                             sfb.getAttributes(firstAddedFilename));
            }
        }

        return jarFilename4;
    }

    /**
     * Test that signing a jar with manifest entries with arbitrary line break
     * positions in individual section headers does not destroy an existing
     * signature<ol>
     * <li>create two self-signed certificates</li>
     * <li>sign a jar with at least one non-META-INF file in it with a JDK
     * before 11 or place line breaks not at 72 bytes in an individual section
     * header</li>
     * <li>add a new file to the jar</li>
     * <li>sign the jar with a JDK 11, 12, or 13 with bug 8217375 not yet
     * resolved with a different signer</li>
     * </ol>&rarr; first signature will not validate anymore even though it
     * should.
     */
    @Test
    public void arbitraryLineBreaksSectionName() throws Exception {
        test("arbitraryLineBreaksSectionName", m -> {
            return (
                Name.MANIFEST_VERSION + ": 1.0\r\n" +
                "Created-By: " +
                        m.getMainAttributes().getValue("Created-By") + "\r\n" +
                "\r\n" +
                "Name: Test\r\n" +
                " -\r\n" +
                " Section\r\n" +
                "Key: Value \r\n" +
                "\r\n" +
                "Name: " + FILENAME_INITIAL_CONTENTS.substring(0, 1) + "\r\n" +
                " " + FILENAME_INITIAL_CONTENTS.substring(1, 8) + "\r\n" +
                " " + FILENAME_INITIAL_CONTENTS.substring(8) + "\r\n" +
                "SHA-256-Digest: " + m.getAttributes(FILENAME_INITIAL_CONTENTS)
                        .getValue("SHA-256-Digest") + "\r\n" +
                "\r\n"
            ).getBytes(UTF_8);
        });
    }

    /**
     * Test that signing a jar with manifest entries with arbitrary line break
     * positions in individual section headers does not destroy an existing
     * signature<ol>
     * <li>create two self-signed certificates</li>
     * <li>sign a jar with at least one non-META-INF file in it with a JDK
     * before 11 or place line breaks not at 72 bytes in an individual section
     * header</li>
     * <li>add a new file to the jar</li>
     * <li>sign the jar with a JDK 11 or 12 with a different signer</li>
     * </ol>&rarr; first signature will not validate anymore even though it
     * should.
     */
    @Test
    public void arbitraryLineBreaksHeader() throws Exception {
        test("arbitraryLineBreaksHeader", m -> {
            String digest = m.getAttributes(FILENAME_INITIAL_CONTENTS)
                    .getValue("SHA-256-Digest");
            return (
                Name.MANIFEST_VERSION + ": 1.0\r\n" +
                "Created-By: " +
                        m.getMainAttributes().getValue("Created-By") + "\r\n" +
                "\r\n" +
                "Name: Test-Section\r\n" +
                "Key: Value \r\n" +
                " with\r\n" +
                "  strange \r\n" +
                " line breaks.\r\n" +
                "\r\n" +
                "Name: " + FILENAME_INITIAL_CONTENTS + "\r\n" +
                "SHA-256-Digest: " + digest.substring(0, 11) + "\r\n" +
                " " + digest.substring(11) + "\r\n" +
                "\r\n"
            ).getBytes(UTF_8);
        });
    }

    /**
     * Breaks {@code line} at 70 bytes even though the name says 72 but when
     * also counting the line delimiter ("{@code \r\n}") the line totals to 72
     * bytes.
     * Borrowed from {@link Manifest#make72Safe} before JDK 11
     *
     * @see Manifest#make72Safe
     */
    static void make72Safe(StringBuffer line) {
        int length = line.length();
        if (length > 72) {
            int index = 70;
            while (index < length - 2) {
                line.insert(index, "\r\n ");
                index += 72;
                length += 3;
            }
        }
        return;
    }

    /**
     * Test that signing a jar with manifest entries with line breaks at
     * position where Manifest would not place them now anymore (72 instead of
     * 70 bytes after line starts) does not destroy an existing signature<ol>
     * <li>create two self-signed certificates</li>
     * <li>simulate a manifest as it would have been written by a JDK before 11
     * by re-positioning line breaks at 70 bytes (which makes a difference by
     * digests that grow headers longer than 70 characters such as SHA-512 as
     * opposed to default SHA-256, long file names, or manual editing)</li>
     * <li>add a new file to the jar</li>
     * <li>sign the jar with a JDK 11 or 12 with a different signer</li>
     * </ol><p>&rarr;
     * The first signature will not validate anymore even though it should.
     */
    public void lineWidth70(String name, String digestalg) throws Exception {
        Files.write(Path.of(name), name.getBytes(UTF_8));
        test(name, name, FILENAME_UPDATED_CONTENTS, m -> {
            // force a line break with a header exceeding line width limit
            m.getEntries().put("Test-Section", new Attributes());
            m.getAttributes("Test-Section").put(
                    Name.IMPLEMENTATION_VERSION, "1" + "0".repeat(100));

            StringBuilder sb = new StringBuilder();
            StringBuffer[] buf = new StringBuffer[] { null };
            manifestToString(m).lines().forEach(line -> {
                if (line.startsWith(" ")) {
                    buf[0].append(line.substring(1));
                } else {
                    if (buf[0] != null) {
                        make72Safe(buf[0]);
                        sb.append(buf[0].toString());
                        sb.append("\r\n");
                    }
                    buf[0] = new StringBuffer();
                    buf[0].append(line);
                }
            });
            make72Safe(buf[0]);
            sb.append(buf[0].toString());
            sb.append("\r\n");
            return sb.toString().getBytes(UTF_8);
        }, digestalg, false, false);
    }

    @Test
    public void lineWidth70Filename() throws Exception {
        lineWidth70(
            "lineWidth70".repeat(6) /* 73 chars total with "Name: " */, null);
    }

    @Test
    public void lineWidth70Digest() throws Exception {
        lineWidth70("lineWidth70digest", "SHA-512");
    }

    /**
     * Test that signing a jar with a manifest with line delimiter other than
     * "{@code \r\n}" does not destroy an existing signature<ol>
     * <li>create two self-signed certificates</li>
     * <li>sign a jar with at least one non-META-INF file in it</li>
     * <li>extract the manifest, and change its line delimiters
     * (for example dos2unix)</li>
     * <li>update the jar with the updated manifest</li>
     * <li>sign it again with the same signer as before</li>
     * <li>add a new file to the jar</li>
     * <li>sign the jar with a JDK before 13 with a different signer<li>
     * </ol><p>&rarr;
     * The first signature will not validate anymore even though it should.
     */
    public void lineBreak(String lineBreak) throws Exception {
        test("lineBreak" + byteArrayToIntList(lineBreak.getBytes(UTF_8)).stream
                ().map(i -> "" + i).collect(Collectors.joining("")), m -> {
            StringBuilder sb = new StringBuilder();
            manifestToString(m).lines().forEach(l -> {
                sb.append(l);
                sb.append(lineBreak);
            });
            return sb.toString().getBytes(UTF_8);
        });
    }

    @Test
    public void lineBreakCr() throws Exception {
        lineBreak("\r");
    }

    @Test
    public void lineBreakLf() throws Exception {
        lineBreak("\n");
    }

    @Test
    public void lineBreakCrLf() throws Exception {
        lineBreak("\r\n");
    }

    @Test
    public void testAdjacentRepeatedSection() throws Exception {
        test("adjacent", m -> {
            return (manifestToString(m) +
                    "Name: " + FILENAME_INITIAL_CONTENTS + "\r\n" +
                    "Foo: Bar\r\n" +
                    "\r\n"
            ).getBytes(UTF_8);
        });
    }

    @Test
    public void testIntermittentRepeatedSection() throws Exception {
        test("intermittent", m -> {
            return (manifestToString(m) +
                    "Name: don't know\r\n" +
                    "Foo: Bar\r\n" +
                    "\r\n" +
                    "Name: " + FILENAME_INITIAL_CONTENTS + "\r\n" +
                    "Foo: Bar\r\n" +
                    "\r\n"
            ).getBytes(UTF_8);
        });
    }

    @Test
    public void testNameImmediatelyContinued() throws Exception {
        test("testNameImmediatelyContinued", m -> {
            // places a continuation line break and space at the first allowed
            // position after ": " and before the first character of the value
            return (manifestToString(m).replaceAll(FILENAME_INITIAL_CONTENTS,
                    "\r\n " + FILENAME_INITIAL_CONTENTS + "\r\nFoo: Bar")
            ).getBytes(UTF_8);
        });
    }

    /*
     * "malicious" '\r' after continuation line continued
     */
    @Test
    public void testNameContinuedContinuedWithCr() throws Exception {
        test("testNameContinuedContinuedWithCr", m -> {
            return (manifestToString(m).replaceAll(FILENAME_INITIAL_CONTENTS,
                    FILENAME_INITIAL_CONTENTS.substring(0, 1) + "\r\n " +
                    FILENAME_INITIAL_CONTENTS.substring(1, 4) + "\r " +
                    FILENAME_INITIAL_CONTENTS.substring(4) + "\r\n" +
                    "Foo: Bar")
            ).getBytes(UTF_8);
        });
    }

    /*
     * "malicious" '\r' after continued continuation line
     */
    @Test
    public void testNameContinuedContinuedEndingWithCr() throws Exception {
        test("testNameContinuedContinuedEndingWithCr", m -> {
            return (manifestToString(m).replaceAll(FILENAME_INITIAL_CONTENTS,
                    FILENAME_INITIAL_CONTENTS.substring(0, 1) + "\r\n " +
                    FILENAME_INITIAL_CONTENTS.substring(1, 4) + "\r\n " +
                    FILENAME_INITIAL_CONTENTS.substring(4) + "\r" + // no '\n'
                    "Foo: Bar")
            ).getBytes(UTF_8);
        });
    }

    @DataProvider(name = "trailingSeqParams", parallel = true)
    public static Object[][] trailingSeqParams() {
        return new Object[][] {
            {""},
            {"\r"},
            {"\n"},
            {"\r\n"},
            {"\r\r"},
            {"\n\n"},
            {"\n\r"},
            {"\r\r\r"},
            {"\r\r\n"},
            {"\r\n\r"},
            {"\r\n\n"},
            {"\n\r\r"},
            {"\n\r\n"},
            {"\n\n\r"},
            {"\n\n\n"},
            {"\r\r\r\n"},
            {"\r\r\n\r"},
            {"\r\r\n\n"},
            {"\r\n\r\r"},
            {"\r\n\r\n"},
            {"\r\n\n\r"},
            {"\r\n\n\n"},
            {"\n\r\r\n"},
            {"\n\r\n\r"},
            {"\n\r\n\n"},
            {"\n\n\r\n"},
            {"\r\r\n\r\n"},
            {"\r\n\r\r\n"},
            {"\r\n\r\n\r"},
            {"\r\n\r\n\n"},
            {"\r\n\n\r\n"},
            {"\n\r\n\r\n"},
            {"\r\n\r\n\r\n"},
            {"\r\n\r\n\r\n\r\n"}
        };
    }

    boolean isSufficientSectionDelimiter(String trailingSeq) {
        if (trailingSeq.length() < 2) return false;
        if (trailingSeq.startsWith("\r\n")) {
            trailingSeq = trailingSeq.substring(2);
        } else if (trailingSeq.startsWith("\r") ||
                   trailingSeq.startsWith("\n")) {
            trailingSeq = trailingSeq.substring(1);
        } else {
            return false;
        }
        if (trailingSeq.startsWith("\r\n")) {
            return true;
        } else if (trailingSeq.startsWith("\r") ||
                trailingSeq.startsWith("\n")) {
            return true;
        }
        return false;
    }

    Function<Manifest, byte[]> replaceTrailingLineBreaksManipulation(
            String trailingSeq) {
        return m -> {
            StringBuilder sb = new StringBuilder(manifestToString(m));
            // cut off default trailing line break characters
            while ("\r\n".contains(sb.substring(sb.length() - 1))) {
                sb.deleteCharAt(sb.length() - 1);
            }
            // and instead add another trailing sequence
            sb.append(trailingSeq);
            return sb.toString().getBytes(UTF_8);
        };
    }

    boolean abSigFilesEqual(String jarFilename,
            Function<Manifest,Object> getter) throws IOException {
        try (ZipFile zip = new ZipFile(jarFilename)) {
            ZipEntry ea = zip.getEntry("META-INF/A.SF");
            Manifest sfa = new Manifest(zip.getInputStream(ea));
            ZipEntry eb = zip.getEntry("META-INF/B.SF");
            Manifest sfb = new Manifest(zip.getInputStream(eb));
            return getter.apply(sfa).equals(getter.apply(sfb));
        }
    }

    /**
     * Create a signed JAR file with a strange sequence of line breaks after
     * the main attributes and no individual section and hence no file contained
     * within the JAR file in order not to produce an individual section,
     * then add no other file and sign it with a different signer.
     * The manifest is not expected to be changed during the second signature.
     */
    @Test(dataProvider = "trailingSeqParams")
    public void emptyJarTrailingSeq(String trailingSeq) throws Exception {
        String trailingSeqEscaped = byteArrayToIntList(trailingSeq.getBytes(
              UTF_8)).stream().map(i -> "" + i).collect(Collectors.joining(""));
        System.out.println("trailingSeq = " + trailingSeqEscaped);
        if (trailingSeq.isEmpty()) {
            return; // invalid manifest without trailing line break
        }

        test("emptyJarTrailingSeq" + trailingSeqEscaped, null, null,
                replaceTrailingLineBreaksManipulation(trailingSeq));

        // test called above already asserts by default that the main attributes
        // digests have not changed.
    }

    /**
     * Create a signed JAR file with a strange sequence of line breaks after
     * the main attributes and no individual section and hence no file contained
     * within the JAR file in order not to produce an individual section,
     * then add another file and sign it with a different signer so that the
     * originally trailing sequence after the main attributes might have to be
     * completed to a full section delimiter or reproduced only partially
     * before the new individual section with the added file digest can be
     * appended. The main attributes digests are expected to change if the
     * first signed trailing sequence did not contain a blank line and are not
     * expected to change if superfluous parts of the trailing sequence were
     * not reproduced. All digests are expected to validate either with digest
     * or with digestWorkaround.
     */
    @Test(dataProvider = "trailingSeqParams")
    public void emptyJarTrailingSeqAddFile(String trailingSeq) throws Exception{
        String trailingSeqEscaped = byteArrayToIntList(trailingSeq.getBytes(
              UTF_8)).stream().map(i -> "" + i).collect(Collectors.joining(""));
        System.out.println("trailingSeq = " + trailingSeqEscaped);
        if (!isSufficientSectionDelimiter(trailingSeq)) {
            return; // invalid manifest without trailing blank line
        }
        boolean expectUnchangedDigests =
                isSufficientSectionDelimiter(trailingSeq);
        System.out.println("expectUnchangedDigests = " + expectUnchangedDigests);
        String jarFilename = test("emptyJarTrailingSeqAddFile" +
                trailingSeqEscaped, null, FILENAME_UPDATED_CONTENTS,
                replaceTrailingLineBreaksManipulation(trailingSeq),
                null, expectUnchangedDigests, false);

        // Check that the digests have changed only if another line break had
        // to be added before a new individual section. That both also are valid
        // with either digest or digestWorkaround has been checked by test
        // before.
        assertEquals(abSigFilesEqual(jarFilename, sf -> sf.getMainAttributes()
                        .getValue("SHA-256-Digest-Manifest-Main-Attributes")),
                     expectUnchangedDigests);
    }

    /**
     * Create a signed JAR file with a strange sequence of line breaks after
     * the only individual section holding the digest of the only file contained
     * within the JAR file,
     * then add no other file and sign it with a different signer.
     * The manifest is expected to be changed during the second signature only
     * by removing superfluous line break characters which are not digested
     * and the manifest entry digest is expected not to change.
     * The individual section is expected to be reproduced without additional
     * line breaks even if the trailing sequence does not properly delimit
     * another section.
     */
    @Test(dataProvider = "trailingSeqParams")
    public void singleIndividualSectionTrailingSeq(String trailingSeq)
            throws Exception {
        String trailingSeqEscaped = byteArrayToIntList(trailingSeq.getBytes(
              UTF_8)).stream().map(i -> "" + i).collect(Collectors.joining(""));
        System.out.println("trailingSeq = " + trailingSeqEscaped);
        if (trailingSeq.isEmpty()) {
            return; // invalid manifest without trailing line break
        }
        String jarFilename = test("singleIndividualSectionTrailingSeq"
                + trailingSeqEscaped, FILENAME_INITIAL_CONTENTS, null,
                replaceTrailingLineBreaksManipulation(trailingSeq));

        assertTrue(abSigFilesEqual(jarFilename, sf -> sf.getAttributes(
                FILENAME_INITIAL_CONTENTS).getValue("SHA-256-Digest")));
    }

    /**
     * Create a signed JAR file with a strange sequence of line breaks after
     * the first individual section holding the digest of the only file
     * contained within the JAR file and a second individual section with the
     * same name to be both digested into the same entry digest,
     * then add no other file and sign it with a different signer.
     * The manifest is expected to be changed during the second signature
     * by removing superfluous line break characters which are not digested
     * anyway or if the trailingSeq is not a sufficient delimiter that both
     * intially provided sections are treated as only one which is maybe not
     * perfect but does at least not result in an invalid signed jar file.
     */
    @Test(dataProvider = "trailingSeqParams")
    public void firstIndividualSectionTrailingSeq(String trailingSeq)
            throws Exception {
        String trailingSeqEscaped = byteArrayToIntList(trailingSeq.getBytes(
              UTF_8)).stream().map(i -> "" + i).collect(Collectors.joining(""));
        System.out.println("trailingSeq = " + trailingSeqEscaped);
        String jarFilename;
        jarFilename =  test("firstIndividualSectionTrailingSeq"
                + trailingSeqEscaped, FILENAME_INITIAL_CONTENTS, null, m -> {
                StringBuilder sb = new StringBuilder(manifestToString(m));
                // cut off default trailing line break characters
                while ("\r\n".contains(sb.substring(sb.length() - 1))) {
                    sb.deleteCharAt(sb.length() - 1);
                }
                // and instead add another trailing sequence
                sb.append(trailingSeq);
                // now add another section with the same name assuming sb
                // already contains one entry for FILENAME_INITIAL_CONTENTS
                sb.append("Name: " + FILENAME_INITIAL_CONTENTS + "\r\n");
                sb.append("Foo: Bar\r\n");
                sb.append("\r\n");
                return sb.toString().getBytes(UTF_8);
        });

        assertTrue(abSigFilesEqual(jarFilename, sf -> sf.getAttributes(
                FILENAME_INITIAL_CONTENTS).getValue("SHA-256-Digest")));
    }

    /**
     * Create a signed JAR file with two individual sections for the same
     * contained file (corresponding by name) the first of which properly
     * delimited and the second of which followed by a strange sequence of
     * line breaks both digested into the same entry digest,
     * then add no other file and sign it with a different signer.
     * The manifest is expected to be changed during the second signature
     * by removing superfluous line break characters which are not digested
     * anyway.
     */
    @Test(dataProvider = "trailingSeqParams")
    public void secondIndividualSectionTrailingSeq(String trailingSeq)
            throws Exception {
        String trailingSeqEscaped = byteArrayToIntList(trailingSeq.getBytes(
              UTF_8)).stream().map(i -> "" + i).collect(Collectors.joining(""));
        System.out.println("trailingSeq = " + trailingSeqEscaped);
        String jarFilename = test("secondIndividualSectionTrailingSeq" +
                trailingSeqEscaped, FILENAME_INITIAL_CONTENTS, null, m -> {
            StringBuilder sb = new StringBuilder(manifestToString(m));
            sb.append("Name: " + FILENAME_INITIAL_CONTENTS + "\r\n");
            sb.append("Foo: Bar");
            sb.append(trailingSeq);
            return sb.toString().getBytes(UTF_8);
        });

        assertTrue(abSigFilesEqual(jarFilename, sf -> sf.getAttributes(
                FILENAME_INITIAL_CONTENTS).getValue("SHA-256-Digest")));
    }

    /**
     * Create a signed JAR file with a strange sequence of line breaks after
     * the only individual section holding the digest of the only file contained
     * within the JAR file,
     * then add another file and sign it with a different signer.
     * The manifest is expected to be changed during the second signature by
     * removing superfluous line break characters which are not digested
     * anyway or adding another line break to complete to a proper section
     * delimiter blank line.
     * The first file entry digest is expected to change only if another
     * line break has been added.
     */
    @Test(dataProvider = "trailingSeqParams")
    public void singleIndividualSectionTrailingSeqAddFile(String trailingSeq)
            throws Exception {
        String trailingSeqEscaped = byteArrayToIntList(trailingSeq.getBytes(
              UTF_8)).stream().map(i -> "" + i).collect(Collectors.joining(""));
        System.out.println("trailingSeq = " + trailingSeqEscaped);
        if (!isSufficientSectionDelimiter(trailingSeq)) {
            return; // invalid manifest without trailing blank line
        }
        String jarFilename = test("singleIndividualSectionTrailingSeqAddFile"
                + trailingSeqEscaped,
                FILENAME_INITIAL_CONTENTS, FILENAME_UPDATED_CONTENTS,
                replaceTrailingLineBreaksManipulation(trailingSeq),
                null, true, true);

        assertTrue(abSigFilesEqual(jarFilename, sf -> sf.getAttributes(
                        FILENAME_INITIAL_CONTENTS).getValue("SHA-256-Digest")));
    }

    /**
     * Create a signed JAR file with a strange sequence of line breaks after
     * the first individual section holding the digest of the only file
     * contained within the JAR file and a second individual section with the
     * same name to be both digested into the same entry digest,
     * then add another file and sign it with a different signer.
     * The manifest is expected to be changed during the second signature
     * by removing superfluous line break characters which are not digested
     * anyway or if the trailingSeq is not a sufficient delimiter that both
     * intially provided sections are treated as only one which is maybe not
     * perfect but does at least not result in an invalid signed jar file.
     */
    @Test(dataProvider = "trailingSeqParams")
    public void firstIndividualSectionTrailingSeqAddFile(String trailingSeq)
            throws Exception {
        String trailingSeqEscaped = byteArrayToIntList(trailingSeq.getBytes(
              UTF_8)).stream().map(i -> "" + i).collect(Collectors.joining(""));
        System.out.println("trailingSeq = " + trailingSeqEscaped);
        String jarFilename =  test("firstIndividualSectionTrailingSeqAddFile"
                + trailingSeqEscaped,
                FILENAME_INITIAL_CONTENTS, FILENAME_UPDATED_CONTENTS, m -> {
                StringBuilder sb = new StringBuilder(manifestToString(m));
                // cut off default trailing line break characters
                while ("\r\n".contains(sb.substring(sb.length() - 1))) {
                    sb.deleteCharAt(sb.length() - 1);
                }
                // and instead add another trailing sequence
                sb.append(trailingSeq);
                // now add another section with the same name assuming sb
                // already contains one entry for FILENAME_INITIAL_CONTENTS
                sb.append("Name: " + FILENAME_INITIAL_CONTENTS + "\r\n");
                sb.append("Foo: Bar\r\n");
                sb.append("\r\n");
                return sb.toString().getBytes(UTF_8);
        });

        assertTrue(abSigFilesEqual(jarFilename, sf -> sf.getAttributes(
                FILENAME_INITIAL_CONTENTS).getValue("SHA-256-Digest")));
    }

    /**
     * Create a signed JAR file with two individual sections for the same
     * contained file (corresponding by name) the first of which properly
     * delimited and the second of which followed by a strange sequence of
     * line breaks both digested into the same entry digest,
     * then add another file and sign it with a different signer.
     * The manifest is expected to be changed during the second signature
     * by removing superfluous line break characters which are not digested
     * anyway or by adding a proper section delimiter.
     * The digests are expected to be changed only if another line break is
     * added to properly delimit the next section both digests of which are
     * expected to validate with either digest or digestWorkaround.
     */
    @Test(dataProvider = "trailingSeqParams")
    public void secondIndividualSectionTrailingSeqAddFile(String trailingSeq)
            throws Exception {
        String trailingSeqEscaped = byteArrayToIntList(trailingSeq.getBytes(
              UTF_8)).stream().map(i -> "" + i).collect(Collectors.joining(""));
        System.out.println("trailingSeq = " + trailingSeqEscaped);
        if (!isSufficientSectionDelimiter(trailingSeq)) {
            return; // invalid manifest without trailing blank line
        }
        String jarFilename = test("secondIndividualSectionTrailingSeqAddFile" +
                trailingSeqEscaped,
                FILENAME_INITIAL_CONTENTS, FILENAME_UPDATED_CONTENTS, m -> {
            StringBuilder sb = new StringBuilder(manifestToString(m));
            sb.append("Name: " + FILENAME_INITIAL_CONTENTS + "\r\n");
            sb.append("Foo: Bar");
            sb.append(trailingSeq);
            return sb.toString().getBytes(UTF_8);
        }, null, true, true);

        assertTrue(abSigFilesEqual(jarFilename, sf -> sf.getAttributes(
                FILENAME_INITIAL_CONTENTS).getValue("SHA-256-Digest")));
    }

    String manifestToString(Manifest mf) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            mf.write(out);
            return new String(out.toByteArray(), UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static List<Integer> byteArrayToIntList(byte[] bytes) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < bytes.length; i++) {
            list.add((int) bytes[i]);
        }
        return list;
    }

}
