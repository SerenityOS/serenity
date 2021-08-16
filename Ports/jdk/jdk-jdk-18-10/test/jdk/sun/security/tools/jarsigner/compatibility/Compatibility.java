/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8217375 8260286
 * @summary This test is used to verify the compatibility of jarsigner across
 *     different JDK releases. It also can be used to check jar signing (w/
 *     and w/o TSA) and to verify some specific signing and digest algorithms.
 *     Note that this is a manual test. For more details about the test and
 *     its usages, please look through the README.
 *
 * @library /test/lib ../warnings
 * @compile -source 1.7 -target 1.7 JdkUtils.java
 * @run main/manual/othervm Compatibility
 */

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.JarUtils;

public class Compatibility {

    private static final String TEST_SRC = System.getProperty("test.src");
    private static final String TEST_CLASSES = System.getProperty("test.classes");
    private static final String TEST_JDK = System.getProperty("test.jdk");
    private static JdkInfo TEST_JDK_INFO;

    private static final String PROXY_HOST = System.getProperty("proxyHost");
    private static final String PROXY_PORT = System.getProperty("proxyPort", "80");

    // An alternative security properties file.
    // The test provides a default one, which only contains two lines:
    // jdk.certpath.disabledAlgorithms=MD2, MD5
    // jdk.jar.disabledAlgorithms=MD2, MD5
    private static final String JAVA_SECURITY = System.getProperty(
            "javaSecurityFile", TEST_SRC + "/java.security");

    private static final String PASSWORD = "testpass";
    private static final String KEYSTORE = "testKeystore.jks";

    private static final String RSA = "RSA";
    private static final String DSA = "DSA";
    private static final String EC = "EC";
    private static String[] KEY_ALGORITHMS;
    private static final String[] DEFAULT_KEY_ALGORITHMS = new String[] {
            RSA,
            DSA,
            EC};

    private static final String SHA1 = "SHA-1";
    private static final String SHA256 = "SHA-256";
    private static final String SHA384 = "SHA-384";
    private static final String SHA512 = "SHA-512";
    private static final String DEFAULT = "DEFAULT";
    private static String[] DIGEST_ALGORITHMS;
    private static final String[] DEFAULT_DIGEST_ALGORITHMS = new String[] {
            SHA1,
            SHA256,
            SHA384,
            SHA512, // note: digests break onto continuation line in manifest
            DEFAULT};

    private static final boolean[] EXPIRED =
            Boolean.valueOf(System.getProperty("expired", "true")) ?
                    new boolean[] { false, true } : new boolean[] { false };

    private static final boolean TEST_COMPREHENSIVE_JAR_CONTENTS =
            Boolean.valueOf(System.getProperty(
                    "testComprehensiveJarContents", "false"));

    private static final boolean TEST_JAR_UPDATE =
            Boolean.valueOf(System.getProperty("testJarUpdate", "false"));

    private static final boolean STRICT =
            Boolean.valueOf(System.getProperty("strict", "false"));

    private static final Calendar CALENDAR = Calendar.getInstance();
    private static final DateFormat DATE_FORMAT
            = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    // The certificate validity period in minutes. The default value is 1440
    // minutes, namely 1 day.
    private static final int CERT_VALIDITY
            = Integer.valueOf(System.getProperty("certValidity", "1440"));
    static {
        if (CERT_VALIDITY < 1 || CERT_VALIDITY > 1440) {
            throw new RuntimeException(
                    "certValidity out of range [1, 1440]: " + CERT_VALIDITY);
        }
    }

    // If true, an additional verifying will be triggered after all of
    // valid certificates expire. The default value is false.
    public static final boolean DELAY_VERIFY
            = Boolean.valueOf(System.getProperty("delayVerify", "false"));

    private static long lastCertStartTime;

    private static DetailsOutputStream detailsOutput;

    private static int sigfileCounter;

    private static String nextSigfileName(String alias, String u, String s) {
        String sigfileName = "" + (++sigfileCounter);
        System.out.println("using sigfile " + sigfileName + " for alias "
                    + alias + " signing " + u + ".jar to " + s + ".jar");
        return sigfileName;
    }

    public static void main(String... args) throws Throwable {
        // Backups stdout and stderr.
        PrintStream origStdOut = System.out;
        PrintStream origStdErr = System.err;

        detailsOutput = new DetailsOutputStream(outfile());

        // Redirects the system output to a custom one.
        PrintStream printStream = new PrintStream(detailsOutput);
        System.setOut(printStream);
        System.setErr(printStream);

        TEST_JDK_INFO = new JdkInfo(TEST_JDK);

        List<TsaInfo> tsaList = tsaInfoList();
        List<JdkInfo> jdkInfoList = jdkInfoList();
        List<CertInfo> certList = createCertificates(jdkInfoList);
        List<SignItem> signItems =
                test(jdkInfoList, tsaList, certList, createJars());

        boolean failed = generateReport(jdkInfoList, tsaList, signItems);

        // Restores the original stdout and stderr.
        System.setOut(origStdOut);
        System.setErr(origStdErr);

        if (failed) {
            throw new RuntimeException("At least one test case failed. "
                    + "Please check the failed row(s) in report.html "
                    + "or failedReport.html.");
        }
    }

    private static SignItem createJarFile(String jar, Manifest m,
            String... files) throws IOException {
        JarUtils.createJarFile(Path.of(jar), m, Path.of("."),
                Arrays.stream(files).map(Path::of).toArray(Path[]::new));
        return SignItem.build()
                .signedJar(jar.replaceAll("[.]jar$", ""))
            .addContentFiles(Arrays.stream(files).collect(Collectors.toList()));
    }

    private static String createDummyFile(String name) throws IOException {
        if (name.contains("/")) new File(name).getParentFile().mkdir();
        try (OutputStream fos = new FileOutputStream(name)) {
            fos.write(name.getBytes(UTF_8));
        }
        return name;
    }

    // Creates one or more jar files to test
    private static List<SignItem> createJars() throws IOException {
        List<SignItem> jarList = new ArrayList<>();

        Manifest m = new Manifest();
        m.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");

        // creates a jar file that contains a dummy file
        jarList.add(createJarFile("test.jar", m, createDummyFile("dummy")));

        if (TEST_COMPREHENSIVE_JAR_CONTENTS) {

            // empty jar file so that jarsigner will add a default manifest
            jarList.add(createJarFile("empty.jar", m));

            // jar file that contains only an empty manifest with empty main
            // attributes (due to missing "Manifest-Version" header)
            JarUtils.createJar("nomainatts.jar");
            jarList.add(SignItem.build().signedJar("nomainatts"));

            // creates a jar file that contains several files.
            jarList.add(createJarFile("files.jar", m,
                    IntStream.range(1, 9).boxed().map(i -> {
                        try {
                            return createDummyFile("dummy" + i);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).toArray(String[]::new)
            ));

            // forces a line break by exceeding the line width limit of 72 bytes
            // in the filename and hence manifest entry name
            jarList.add(createJarFile("longfilename.jar", m,
                    createDummyFile("test".repeat(20))));

            // another interesting case is with different digest algorithms
            // resulting in digests broken across line breaks onto continuation
            // lines. these however are set with the 'digestAlgs' option or
            // include all digest algorithms by default, see SignTwice.java.
        }

        return jarList;
    }

    // updates a signed jar file by adding another file
    private static List<SignItem> updateJar(SignItem prev) throws IOException {
        List<SignItem> jarList = new ArrayList<>();

        // sign unmodified jar again
        Files.copy(Path.of(prev.signedJar + ".jar"),
                Path.of(prev.signedJar + "-signagainunmodified.jar"));
        jarList.add(SignItem.build(prev)
                .signedJar(prev.signedJar + "-signagainunmodified"));

        String oldJar = prev.signedJar;
        String newJar = oldJar + "-addfile";
        String triggerUpdateFile = "addfile";
        JarUtils.updateJar(oldJar + ".jar", newJar + ".jar", triggerUpdateFile);
        jarList.add(SignItem.build(prev).signedJar(newJar)
                .addContentFiles(Arrays.asList(triggerUpdateFile)));

        return jarList;
    }

    // Creates a key store that includes a set of valid/expired certificates
    // with various algorithms.
    private static List<CertInfo> createCertificates(List<JdkInfo> jdkInfoList)
            throws Throwable {
        List<CertInfo> certList = new ArrayList<>();
        Set<String> expiredCertFilter = new HashSet<>();

        for (JdkInfo jdkInfo : jdkInfoList) {
            for (String keyAlgorithm : keyAlgs()) {
                if (!jdkInfo.supportsKeyAlg(keyAlgorithm)) continue;
                for (int keySize : keySizes(keyAlgorithm)) {
                    for (String digestAlgorithm : digestAlgs()) {
                        for(boolean expired : EXPIRED) {
                            // It creates only one expired certificate for one
                            // key algorithm.
                            if (expired
                                    && !expiredCertFilter.add(keyAlgorithm)) {
                                continue;
                            }

                            CertInfo certInfo = new CertInfo(
                                    jdkInfo,
                                    keyAlgorithm,
                                    digestAlgorithm,
                                    keySize,
                                    expired);
                            // If the signature algorithm is not supported by the
                            // JDK, it cannot try to sign jar with this algorithm.
                            String sigalg = certInfo.sigalg();
                            if (sigalg != null &&
                                    !jdkInfo.isSupportedSigalg(sigalg)) {
                                continue;
                            }
                            createCertificate(jdkInfo, certInfo);
                            certList.add(certInfo);
                        }
                    }
                }
            }
        }

        System.out.println("the keystore contents:");
        for (JdkInfo jdkInfo : jdkInfoList) {
            execTool(jdkInfo.jdkPath + "/bin/keytool", new String[] {
                    "-v",
                    "-storetype",
                    "jks",
                    "-storepass",
                    PASSWORD,
                    "-keystore",
                    KEYSTORE,
                    "-list"
            });
        }

        return certList;
    }

    // Creates/Updates a key store that adds a certificate with specific algorithm.
    private static void createCertificate(JdkInfo jdkInfo, CertInfo certInfo)
            throws Throwable {
        List<String> arguments = new ArrayList<>();
        arguments.add("-J-Djava.security.properties=" + JAVA_SECURITY);
        arguments.add("-v");
        arguments.add("-debug");
        arguments.add("-storetype");
        arguments.add("jks");
        arguments.add("-keystore");
        arguments.add(KEYSTORE);
        arguments.add("-storepass");
        arguments.add(PASSWORD);
        arguments.add(jdkInfo.majorVersion < 6 ? "-genkey" : "-genkeypair");
        arguments.add("-keyalg");
        arguments.add(certInfo.keyAlgorithm);
        String sigalg = certInfo.sigalg();
        if (sigalg != null) {
            arguments.add("-sigalg");
            arguments.add(sigalg);
        }
        if (certInfo.keySize != 0) {
            arguments.add("-keysize");
            arguments.add(certInfo.keySize + "");
        }
        arguments.add("-dname");
        arguments.add("CN=" + certInfo);
        arguments.add("-alias");
        arguments.add(certInfo.alias());
        arguments.add("-keypass");
        arguments.add(PASSWORD);

        arguments.add("-startdate");
        arguments.add(startDate(certInfo.expired));
        arguments.add("-validity");
//        arguments.add(DELAY_VERIFY ? "1" : "222"); // > six months no warn
        arguments.add("1");

        OutputAnalyzer outputAnalyzer = execTool(
                jdkInfo.jdkPath + "/bin/keytool",
                arguments.toArray(new String[arguments.size()]));
        if (outputAnalyzer.getExitValue() != 0
                || outputAnalyzer.getOutput().matches("[Ee]xception")
                || outputAnalyzer.getOutput().matches(Test.ERROR + " ?")) {
            System.out.println(outputAnalyzer.getOutput());
            throw new Exception("error generating a key pair: " + arguments);
        }
    }

    // The validity period of a certificate always be 1 day. For creating an
    // expired certificate, the start date is the time before 1 day, then the
    // certificate expires immediately. And for creating a valid certificate,
    // the start date is the time before (1 day - CERT_VALIDITY minutes), then
    // the certificate will expires in CERT_VALIDITY minutes.
    private static String startDate(boolean expiredCert) {
        CALENDAR.setTime(new Date());
        if (DELAY_VERIFY || expiredCert) {
            // corresponds to '-validity 1'
            CALENDAR.add(Calendar.DAY_OF_MONTH, -1);
        }
        if (DELAY_VERIFY && !expiredCert) {
            CALENDAR.add(Calendar.MINUTE, CERT_VALIDITY);
        }
        Date startDate = CALENDAR.getTime();
        if (!expiredCert) {
            lastCertStartTime = startDate.getTime();
        }
        return DATE_FORMAT.format(startDate);
    }

    private static String outfile() {
        return System.getProperty("o");
    }

    // Retrieves JDK info from the file which is specified by property
    // jdkListFile, or from property jdkList if jdkListFile is not available.
    private static List<JdkInfo> jdkInfoList() throws Throwable {
        String[] jdkList = list("jdkList");
        if (jdkList.length == 0) {
            jdkList = new String[] { "TEST_JDK" };
        }

        List<JdkInfo> jdkInfoList = new ArrayList<>();
        int index = 0;
        for (String jdkPath : jdkList) {
            JdkInfo jdkInfo = "TEST_JDK".equalsIgnoreCase(jdkPath) ?
                    TEST_JDK_INFO : new JdkInfo(jdkPath);
            // The JDK version must be unique.
            if (!jdkInfoList.contains(jdkInfo)) {
                jdkInfo.index = index++;
                jdkInfo.version = String.format(
                        "%s(%d)", jdkInfo.version, jdkInfo.index);
                jdkInfoList.add(jdkInfo);
            } else {
                System.out.println("The JDK version is duplicate: " + jdkPath);
            }
        }
        return jdkInfoList;
    }

    private static List<String> keyAlgs() throws IOException {
        if (KEY_ALGORITHMS == null) KEY_ALGORITHMS = list("keyAlgs");
        if (KEY_ALGORITHMS.length == 0)
            return Arrays.asList(DEFAULT_KEY_ALGORITHMS);
        return Arrays.stream(KEY_ALGORITHMS).map(a -> a.split(";")[0])
                .collect(Collectors.toList());
    }

    // Return key sizes according to the specified key algorithm.
    private static int[] keySizes(String keyAlgorithm) throws IOException {
        if (KEY_ALGORITHMS == null) KEY_ALGORITHMS = list("keyAlgs");
        for (String keyAlg : KEY_ALGORITHMS) {
            String[] split = (keyAlg + " ").split(";");
            if (keyAlgorithm.equals(split[0].trim()) && split.length > 1) {
                int sizes[] = new int[split.length - 1];
                for (int i = 1; i <= sizes.length; i++)
                    sizes[i - 1] = split[i].isBlank() ? 0 : // default
                        Integer.parseInt(split[i].trim());
                return sizes;
            }
        }

        // defaults
        if (RSA.equals(keyAlgorithm) || DSA.equals(keyAlgorithm)) {
            return new int[] { 1024, 2048, 0 }; // 0 is no keysize specified
        } else if (EC.equals(keyAlgorithm)) {
            return new int[] { 384, 571, 0 }; // 0 is no keysize specified
        } else {
            throw new RuntimeException("problem determining key sizes");
        }
    }

    private static List<String> digestAlgs() throws IOException {
        if (DIGEST_ALGORITHMS == null) DIGEST_ALGORITHMS = list("digestAlgs");
        if (DIGEST_ALGORITHMS.length == 0)
            return Arrays.asList(DEFAULT_DIGEST_ALGORITHMS);
        return Arrays.asList(DIGEST_ALGORITHMS);
    }

    // Retrieves TSA info from the file which is specified by property tsaListFile,
    // or from property tsaList if tsaListFile is not available.
    private static List<TsaInfo> tsaInfoList() throws IOException {
        String[] tsaList = list("tsaList");

        List<TsaInfo> tsaInfoList = new ArrayList<>();
        for (int i = 0; i < tsaList.length; i++) {
            String[] values = tsaList[i].split(";digests=");

            String[] digests = new String[0];
            if (values.length == 2) {
                digests = values[1].split(",");
            }

            String tsaUrl = values[0];
            if (tsaUrl.isEmpty() || tsaUrl.equalsIgnoreCase("notsa")) {
                tsaUrl = null;
            }
            TsaInfo bufTsa = new TsaInfo(i, tsaUrl);
            for (String digest : digests) {
                bufTsa.addDigest(digest.toUpperCase());
            }
            tsaInfoList.add(bufTsa);
        }

        if (tsaInfoList.size() == 0) {
            throw new RuntimeException("TSA service is mandatory unless "
                    + "'notsa' specified explicitly.");
        }
        return tsaInfoList;
    }

    private static String[] list(String listProp) throws IOException {
        String listFileProp = listProp + "File";
        String listFile = System.getProperty(listFileProp);
        if (!isEmpty(listFile)) {
            System.out.println(listFileProp + "=" + listFile);
            List<String> list = new ArrayList<>();
            BufferedReader reader = new BufferedReader(
                    new FileReader(listFile));
            String line;
            while ((line = reader.readLine()) != null) {
                String item = line.trim();
                if (!item.isEmpty()) {
                    list.add(item);
                }
            }
            reader.close();
            return list.toArray(new String[list.size()]);
        }

        String list = System.getProperty(listProp);
        System.out.println(listProp + "=" + list);
        return !isEmpty(list) ? list.split("#") : new String[0];
    }

    private static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    // A JDK (signer) signs a jar with a variety of algorithms, and then all of
    // JDKs (verifiers), including the signer itself, try to verify the signed
    // jars respectively.
    private static List<SignItem> test(List<JdkInfo> jdkInfoList,
            List<TsaInfo> tsaInfoList, List<CertInfo> certList,
            List<SignItem> jars) throws Throwable {
        detailsOutput.transferPhase();
        List<SignItem> signItems = new ArrayList<>();
        signItems.addAll(signing(jdkInfoList, tsaInfoList, certList, jars));
        if (TEST_JAR_UPDATE) {
            signItems.addAll(signing(jdkInfoList, tsaInfoList, certList,
                    updating(signItems.stream().filter(
                            x -> x.status != Status.ERROR)
                    .collect(Collectors.toList()))));
        }

        detailsOutput.transferPhase();
        for (SignItem signItem : signItems) {
            for (JdkInfo verifierInfo : jdkInfoList) {
                if (!verifierInfo.supportsKeyAlg(
                        signItem.certInfo.keyAlgorithm)) continue;
                VerifyItem verifyItem = VerifyItem.build(verifierInfo);
                verifyItem.addSignerCertInfos(signItem);
                signItem.addVerifyItem(verifyItem);
                verifying(signItem, verifyItem);
            }
        }

        // if lastCertExpirationTime passed already now, probably some
        // certificate was already expired during jar signature verification
        // (jarsigner -verify) and the test should probably be repeated with an
        // increased validity period -DcertValidity CERT_VALIDITY
        long lastCertExpirationTime = lastCertStartTime + 24 * 60 * 60 * 1000;
        if (lastCertExpirationTime < System.currentTimeMillis()) {
            throw new AssertionError("CERT_VALIDITY (" + CERT_VALIDITY
                    + " [minutes]) was too short. "
                    + "Creating and signing the jars took longer, "
                    + "presumably at least "
                    + ((lastCertExpirationTime - System.currentTimeMillis())
                            / 60 * 1000 + CERT_VALIDITY) + " [minutes].");
        }

        if (DELAY_VERIFY) {
            detailsOutput.transferPhase();
            System.out.print("Waiting for delay verifying");
            while (System.currentTimeMillis() < lastCertExpirationTime) {
                TimeUnit.SECONDS.sleep(30);
                System.out.print(".");
            }
            System.out.println();

            System.out.println("Delay verifying starts");
            for (SignItem signItem : signItems) {
                for (VerifyItem verifyItem : signItem.verifyItems) {
                    verifying(signItem, verifyItem);
                }
            }
        }

        detailsOutput.transferPhase();
        return signItems;
    }

    private static List<SignItem> signing(List<JdkInfo> jdkInfos,
            List<TsaInfo> tsaList, List<CertInfo> certList,
            List<SignItem> unsignedJars) throws Throwable {
        List<SignItem> signItems = new ArrayList<>();

        for (CertInfo certInfo : certList) {
            JdkInfo signerInfo = certInfo.jdkInfo;
            String keyAlgorithm = certInfo.keyAlgorithm;
            String sigDigestAlgorithm = certInfo.digestAlgorithm;
            int keySize = certInfo.keySize;
            boolean expired = certInfo.expired;

            for (String jarDigestAlgorithm : digestAlgs()) {
                if (DEFAULT.equals(jarDigestAlgorithm)) {
                    jarDigestAlgorithm = null;
                }

                for (TsaInfo tsaInfo : tsaList) {
                    String tsaUrl = tsaInfo.tsaUrl;

                    List<String> tsaDigestAlgs = digestAlgs();
                    // no point in specifying a tsa digest algorithm
                    // for no TSA, except maybe it would issue a warning.
                    if (tsaUrl == null) tsaDigestAlgs = Arrays.asList(DEFAULT);
                    // If the JDK doesn't support option -tsadigestalg, the
                    // associated cases can just be ignored.
                    if (!signerInfo.supportsTsadigestalg) {
                        tsaDigestAlgs = Arrays.asList(DEFAULT);
                    }
                    for (String tsaDigestAlg : tsaDigestAlgs) {
                        if (DEFAULT.equals(tsaDigestAlg)) {
                            tsaDigestAlg = null;
                        } else if (!tsaInfo.isDigestSupported(tsaDigestAlg)) {
                            // It has to ignore the digest algorithm, which
                            // is not supported by the TSA server.
                            continue;
                        }

                        if (tsaUrl != null && TsaFilter.filter(
                                signerInfo.version,
                                tsaDigestAlg,
                                expired,
                                tsaInfo.index)) {
                            continue;
                        }

                        for (SignItem prevSign : unsignedJars) {
                            String unsignedJar = prevSign.signedJar;

                            SignItem signItem = SignItem.build(prevSign)
                                    .certInfo(certInfo)
                                    .jdkInfo(signerInfo);
                            String signedJar = unsignedJar + "-" + "JDK_" + (
                                    signerInfo.version + "-CERT_" + certInfo).
                                    replaceAll("[^a-z_0-9A-Z.]+", "-");

                            if (jarDigestAlgorithm != null) {
                                signedJar += "-DIGESTALG_" + jarDigestAlgorithm;
                                signItem.digestAlgorithm(jarDigestAlgorithm);
                            }
                            if (tsaUrl == null) {
                                signItem.tsaIndex(-1);
                            } else {
                                signedJar += "-TSA_" + tsaInfo.index;
                                signItem.tsaIndex(tsaInfo.index);
                                if (tsaDigestAlg != null) {
                                    signedJar += "-TSADIGALG_" + tsaDigestAlg;
                                    signItem.tsaDigestAlgorithm(tsaDigestAlg);
                                }
                            }
                            signItem.signedJar(signedJar);

                            String signingId = signingId(signItem);
                            detailsOutput.writeAnchorName(signingId,
                                    "Signing: " + signingId);

                            OutputAnalyzer signOA = signJar(
                                    signerInfo.jarsignerPath,
                                    certInfo.sigalg(),
                                    jarDigestAlgorithm,
                                    tsaDigestAlg,
                                    tsaUrl,
                                    certInfo.alias(),
                                    unsignedJar,
                                    signedJar);
                            Status signingStatus = signingStatus(signOA,
                                    tsaUrl != null);
                            signItem.status(signingStatus);
                            signItems.add(signItem);
                        }
                    }
                }
            }
        }

        return signItems;
    }

    private static List<SignItem> updating(List<SignItem> prevSignItems)
            throws IOException {
        List<SignItem> updateItems = new ArrayList<>();
        for (SignItem prevSign : prevSignItems) {
            updateItems.addAll(updateJar(prevSign));
        }
        return updateItems;
    }

    private static void verifying(SignItem signItem, VerifyItem verifyItem)
            throws Throwable {
        // TODO: how will be ensured that the first verification is not after valid period expired which is only one minute?
        boolean delayVerify = verifyItem.status != Status.NONE;
        String verifyingId = verifyingId(signItem, verifyItem, delayVerify);
        detailsOutput.writeAnchorName(verifyingId, "Verifying: " + verifyingId);
        OutputAnalyzer verifyOA = verifyJar(verifyItem.jdkInfo.jarsignerPath,
                signItem.signedJar, verifyItem.certInfo == null ? null :
                verifyItem.certInfo.alias());
        Status verifyingStatus = verifyingStatus(signItem, verifyItem, verifyOA);

        try {
            String match = "^  ("
                    + "  Signature algorithm: " + signItem.certInfo.
                            expectedSigalg() + ", " + signItem.certInfo.
                            expectedKeySize() + "-bit key"
                    + ")|("
                    + "  Digest algorithm: " + signItem.expectedDigestAlg()
                    + (isWeakAlg(signItem.expectedDigestAlg()) ? " \\(weak\\)" : "")
                    + (signItem.tsaIndex < 0 ? "" :
                      ")|("
                    + "Timestamped by \".+\" on .*"
                    + ")|("
                    + "  Timestamp digest algorithm: "
                            + signItem.expectedTsaDigestAlg()
                    + ")|("
                    + "  Timestamp signature algorithm: .*"
                      )
                    + ")$";
            verifyOA.stdoutShouldMatchByLine(
                    "^- Signed by \"CN=" +  signItem.certInfo.toString()
                            .replaceAll("[.]", "[.]") + "\"$",
                    "^(- Signed by \"CN=.+\")?$",
                    match);
        } catch (Throwable e) {
            e.printStackTrace();
            verifyingStatus = Status.ERROR;
        }

        if (!delayVerify) {
            verifyItem.status(verifyingStatus);
        } else {
            verifyItem.delayStatus(verifyingStatus);
        }

        if (verifyItem.prevVerify != null) {
            verifying(signItem, verifyItem.prevVerify);
        }
    }

    // Determines the status of signing.
    private static Status signingStatus(OutputAnalyzer outputAnalyzer,
            boolean tsa) {
        if (outputAnalyzer.getExitValue() != 0) {
            return Status.ERROR;
        }
        if (!outputAnalyzer.getOutput().contains(Test.JAR_SIGNED)) {
            return Status.ERROR;
        }

        boolean warning = false;
        for (String line : outputAnalyzer.getOutput().lines()
                .toArray(String[]::new)) {
            if (line.matches(Test.ERROR + " ?")) return Status.ERROR;
            if (line.matches(Test.WARNING + " ?")) warning = true;
        }
        return warning ? Status.WARNING : Status.NORMAL;
    }

    // Determines the status of verifying.
    private static Status verifyingStatus(SignItem signItem, VerifyItem
            verifyItem, OutputAnalyzer outputAnalyzer) {
        List<String> expectedSignedContent = new ArrayList<>();
        if (verifyItem.certInfo == null) {
            expectedSignedContent.addAll(signItem.jarContents);
        } else {
            SignItem i = signItem;
            while (i != null) {
                if (i.certInfo != null && i.certInfo.equals(verifyItem.certInfo)) {
                    expectedSignedContent.addAll(i.jarContents);
                }
                i = i.prevSign;
            }
        }
        List<String> expectedUnsignedContent =
                new ArrayList<>(signItem.jarContents);
        expectedUnsignedContent.removeAll(expectedSignedContent);

        int expectedExitCode = !STRICT || expectedUnsignedContent.isEmpty() ? 0 : 32;
        if (outputAnalyzer.getExitValue() != expectedExitCode) {
            System.out.println("verifyingStatus: error: exit code != " + expectedExitCode + ": " + outputAnalyzer.getExitValue() + " != " + expectedExitCode);
            return Status.ERROR;
        }
        String expectedSuccessMessage = expectedUnsignedContent.isEmpty() ?
                Test.JAR_VERIFIED : Test.JAR_VERIFIED_WITH_SIGNER_ERRORS;
        if (!outputAnalyzer.getOutput().contains(expectedSuccessMessage)) {
            System.out.println("verifyingStatus: error: expectedSuccessMessage not found: " + expectedSuccessMessage);
            return Status.ERROR;
        }

        boolean tsa = signItem.tsaIndex >= 0;
        boolean warning = false;
        for (String line : outputAnalyzer.getOutput().lines()
                .toArray(String[]::new)) {
            if (line.isBlank()) {
                // If line is blank and warning flag is true, it is the end of warnings section
                // This is needed when some info is added after warnings, such as timestamp expiration date
                if (warning) warning = false;
                continue;
            }
            if (Test.JAR_VERIFIED.equals(line)) continue;
            if (line.matches(Test.ERROR + " ?") && expectedExitCode == 0) {
                System.out.println("verifyingStatus: error: line.matches(" + Test.ERROR + "\" ?\"): " + line);
                return Status.ERROR;
            }
            if (line.matches(Test.WARNING + " ?")) {
                warning = true;
                continue;
            }
            if (!warning) continue;
            line = line.strip();
            if (Test.NOT_YET_VALID_CERT_SIGNING_WARNING.equals(line)) continue;
            if (Test.HAS_EXPIRING_CERT_SIGNING_WARNING.equals(line)) continue;
            if (Test.HAS_EXPIRING_CERT_VERIFYING_WARNING.equals(line)) continue;
            if (line.matches("^" + Test.NO_TIMESTAMP_SIGNING_WARN_TEMPLATE
                    .replaceAll(
                        "\\(%1\\$tY-%1\\$tm-%1\\$td\\)", "\\\\([^\\\\)]+\\\\)"
                        + "( or after any future revocation date)?")
                    .replaceAll("[.]", "[.]") + "$") && !tsa) continue;
            if (line.matches("^" + Test.NO_TIMESTAMP_VERIFYING_WARN_TEMPLATE
                    .replaceAll("\\(as early as %1\\$tY-%1\\$tm-%1\\$td\\)",
                        "\\\\([^\\\\)]+\\\\)"
                        + "( or after any future revocation date)?")
                    .replaceAll("[.]", "[.]") + "$") && !tsa) continue;
            if (line.matches("^This jar contains signatures that do(es)? not "
                    + "include a timestamp[.] Without a timestamp, users may "
                    + "not be able to validate this jar after the signer "
                    + "certificate's expiration date \\([^\\)]+\\) or after "
                    + "any future revocation date[.]") && !tsa) continue;

            if (isWeakAlg(signItem.expectedDigestAlg())
                    && line.contains(Test.WEAK_ALGORITHM_WARNING)) continue;
            if (Test.CERTIFICATE_SELF_SIGNED.equals(line)) continue;
            if (Test.HAS_EXPIRED_CERT_VERIFYING_WARNING.equals(line)
                    && signItem.certInfo.expired) continue;
            System.out.println("verifyingStatus: unexpected line: " + line);
            return Status.ERROR; // treat unexpected warnings as error
        }
        return warning ? Status.WARNING : Status.NORMAL;
    }

    private static boolean isWeakAlg(String alg) {
        return SHA1.equals(alg);
    }

    // Using specified jarsigner to sign the pre-created jar with specified
    // algorithms.
    private static OutputAnalyzer signJar(String jarsignerPath, String sigalg,
            String jarDigestAlgorithm,
            String tsadigestalg, String tsa, String alias, String unsignedJar,
            String signedJar) throws Throwable {
        List<String> arguments = new ArrayList<>();

        if (PROXY_HOST != null && PROXY_PORT != null) {
            arguments.add("-J-Dhttp.proxyHost=" + PROXY_HOST);
            arguments.add("-J-Dhttp.proxyPort=" + PROXY_PORT);
            arguments.add("-J-Dhttps.proxyHost=" + PROXY_HOST);
            arguments.add("-J-Dhttps.proxyPort=" + PROXY_PORT);
        }
        arguments.add("-J-Djava.security.properties=" + JAVA_SECURITY);
        arguments.add("-debug");
        arguments.add("-verbose");
        if (jarDigestAlgorithm != null) {
            arguments.add("-digestalg");
            arguments.add(jarDigestAlgorithm);
        }
        if (sigalg != null) {
            arguments.add("-sigalg");
            arguments.add(sigalg);
        }
        if (tsa != null) {
            arguments.add("-tsa");
            arguments.add(tsa);
        }
        if (tsadigestalg != null) {
            arguments.add("-tsadigestalg");
            arguments.add(tsadigestalg);
        }
        arguments.add("-keystore");
        arguments.add(KEYSTORE);
        arguments.add("-storepass");
        arguments.add(PASSWORD);
        arguments.add("-sigfile");
        arguments.add(nextSigfileName(alias, unsignedJar, signedJar));
        arguments.add("-signedjar");
        arguments.add(signedJar + ".jar");
        arguments.add(unsignedJar + ".jar");
        arguments.add(alias);

        OutputAnalyzer outputAnalyzer = execTool(jarsignerPath,
                arguments.toArray(new String[arguments.size()]));
        return outputAnalyzer;
    }

    // Using specified jarsigner to verify the signed jar.
    private static OutputAnalyzer verifyJar(String jarsignerPath,
            String signedJar, String alias) throws Throwable {
        List<String> arguments = new ArrayList<>();
        arguments.add("-J-Djava.security.properties=" + JAVA_SECURITY);
        arguments.add("-debug");
        arguments.add("-verbose");
        arguments.add("-certs");
        arguments.add("-keystore");
        arguments.add(KEYSTORE);
        arguments.add("-verify");
        if (STRICT) arguments.add("-strict");
        arguments.add(signedJar + ".jar");
        if (alias != null) arguments.add(alias);
        OutputAnalyzer outputAnalyzer = execTool(jarsignerPath,
                arguments.toArray(new String[arguments.size()]));
        return outputAnalyzer;
    }

    // Generates the test result report.
    private static boolean generateReport(List<JdkInfo> jdkList, List<TsaInfo> tsaList,
            List<SignItem> signItems) throws IOException {
        System.out.println("Report is being generated...");

        StringBuilder report = new StringBuilder();
        report.append(HtmlHelper.startHtml());
        report.append(HtmlHelper.startPre());

        // Generates JDK list
        report.append("JDK list:\n");
        for(JdkInfo jdkInfo : jdkList) {
            report.append(String.format("%d=%s%n",
                    jdkInfo.index,
                    jdkInfo.runtimeVersion));
        }

        // Generates TSA URLs
        report.append("TSA list:\n");
        for(TsaInfo tsaInfo : tsaList) {
            report.append(
                    String.format("%d=%s%n", tsaInfo.index,
                            tsaInfo.tsaUrl == null ? "notsa" : tsaInfo.tsaUrl));
        }
        report.append(HtmlHelper.endPre());

        report.append(HtmlHelper.startTable());
        // Generates report headers.
        List<String> headers = new ArrayList<>();
        headers.add("[Jarfile]");
        headers.add("[Signing Certificate]");
        headers.add("[Signer JDK]");
        headers.add("[Signature Algorithm]");
        headers.add("[Jar Digest Algorithm]");
        headers.add("[TSA Digest Algorithm]");
        headers.add("[TSA]");
        headers.add("[Signing Status]");
        headers.add("[Verifier JDK]");
        headers.add("[Verifying Certificate]");
        headers.add("[Verifying Status]");
        if (DELAY_VERIFY) {
            headers.add("[Delay Verifying Status]");
        }
        headers.add("[Failed]");
        report.append(HtmlHelper.htmlRow(
                headers.toArray(new String[headers.size()])));

        StringBuilder failedReport = new StringBuilder(report.toString());

        boolean failed = signItems.isEmpty();

        // Generates report rows.
        for (SignItem signItem : signItems) {
            failed = failed || signItem.verifyItems.isEmpty();
            for (VerifyItem verifyItem : signItem.verifyItems) {
                String reportRow = reportRow(signItem, verifyItem);
                report.append(reportRow);
                boolean isFailedCase = isFailed(signItem, verifyItem);
                if (isFailedCase) {
                    failedReport.append(reportRow);
                }
                failed = failed || isFailedCase;
            }
        }

        report.append(HtmlHelper.endTable());
        report.append(HtmlHelper.endHtml());
        generateFile("report.html", report.toString());
        if (failed) {
            failedReport.append(HtmlHelper.endTable());
            failedReport.append(HtmlHelper.endPre());
            failedReport.append(HtmlHelper.endHtml());
            generateFile("failedReport.html", failedReport.toString());
        }

        System.out.println("Report is generated.");
        return failed;
    }

    private static void generateFile(String path, String content)
            throws IOException {
        FileWriter writer = new FileWriter(new File(path));
        writer.write(content);
        writer.close();
    }

    private static String jarsignerPath(String jdkPath) {
        return jdkPath + "/bin/jarsigner";
    }

    // Executes the specified function on JdkUtils by the specified JDK.
    private static String execJdkUtils(String jdkPath, String method,
            String... args) throws Throwable {
        String[] cmd = new String[args.length + 5];
        cmd[0] = jdkPath + "/bin/java";
        cmd[1] = "-cp";
        cmd[2] = TEST_CLASSES;
        cmd[3] = JdkUtils.class.getName();
        cmd[4] = method;
        System.arraycopy(args, 0, cmd, 5, args.length);
        return ProcessTools.executeCommand(cmd).getOutput();
    }

    // Executes the specified JDK tools, such as keytool and jarsigner, and
    // ensures the output is in US English.
    private static OutputAnalyzer execTool(String toolPath, String... args)
            throws Throwable {
        long start = System.currentTimeMillis();
        try {

            String[] cmd = new String[args.length + 4];
            cmd[0] = toolPath;
            cmd[1] = "-J-Duser.language=en";
            cmd[2] = "-J-Duser.country=US";
            cmd[3] = "-J-Djava.security.egd=file:/dev/./urandom";
            System.arraycopy(args, 0, cmd, 4, args.length);
            return ProcessTools.executeCommand(cmd);

        } finally {
            long end = System.currentTimeMillis();
            System.out.println("child process duration [ms]: " + (end - start));
        }
    }

    private static class JdkInfo {

        private int index;
        private final String jdkPath;
        private final String jarsignerPath;
        private final String runtimeVersion;
        private String version;
        private final int majorVersion;
        private final boolean supportsTsadigestalg;

        private Map<String, Boolean> sigalgMap = new HashMap<>();

        private JdkInfo(String jdkPath) throws Throwable {
            this.jdkPath = jdkPath;
            jarsignerPath = jarsignerPath(jdkPath);
            runtimeVersion = execJdkUtils(jdkPath, JdkUtils.M_JAVA_RUNTIME_VERSION);
            if (runtimeVersion == null || runtimeVersion.isBlank()) {
                throw new RuntimeException(
                        "Cannot determine the JDK version: " + jdkPath);
            }
            version = execJdkUtils(jdkPath, JdkUtils.M_JAVA_VERSION);
            majorVersion = Integer.parseInt((runtimeVersion.matches("^1[.].*") ?
                    runtimeVersion.substring(2) : runtimeVersion).replaceAll("[^0-9].*$", ""));
            supportsTsadigestalg = execTool(jarsignerPath, "-help")
                    .getOutput().contains("-tsadigestalg");
        }

        private boolean isSupportedSigalg(String sigalg) throws Throwable {
            if (!sigalgMap.containsKey(sigalg)) {
                boolean isSupported = Boolean.parseBoolean(
                        execJdkUtils(
                                jdkPath,
                                JdkUtils.M_IS_SUPPORTED_SIGALG,
                                sigalg));
                sigalgMap.put(sigalg, isSupported);
            }

            return sigalgMap.get(sigalg);
        }

        private boolean isAtLeastMajorVersion(int minVersion) {
            return majorVersion >= minVersion;
        }

        private boolean supportsKeyAlg(String keyAlgorithm) {
            // JDK 6 doesn't support EC
            return isAtLeastMajorVersion(6) || !EC.equals(keyAlgorithm);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((runtimeVersion == null) ? 0 : runtimeVersion.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            JdkInfo other = (JdkInfo) obj;
            if (runtimeVersion == null) {
                if (other.runtimeVersion != null)
                    return false;
            } else if (!runtimeVersion.equals(other.runtimeVersion))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "JdkInfo[" + runtimeVersion + ", " + jdkPath + "]";
        }
    }

    private static class TsaInfo {

        private final int index;
        private final String tsaUrl;
        private Set<String> digestList = new HashSet<>();

        private TsaInfo(int index, String tsa) {
            this.index = index;
            this.tsaUrl = tsa;
        }

        private void addDigest(String digest) {
            digestList.add(digest);
        }

        private boolean isDigestSupported(String digest) {
            return digest == null || digestList.isEmpty()
                    || digestList.contains(digest);
        }

        @Override
        public String toString() {
            return "TsaInfo[" + index + ", " + tsaUrl + "]";
        }
    }

    private static class CertInfo {

        private static int certCounter;

        // nr distinguishes cert CNs in jarsigner -verify output
        private final int nr = ++certCounter;
        private final JdkInfo jdkInfo;
        private final String keyAlgorithm;
        private final String digestAlgorithm;
        private final int keySize;
        private final boolean expired;

        private CertInfo(JdkInfo jdkInfo, String keyAlgorithm,
                String digestAlgorithm, int keySize, boolean expired) {
            this.jdkInfo = jdkInfo;
            this.keyAlgorithm = keyAlgorithm;
            this.digestAlgorithm = digestAlgorithm;
            this.keySize = keySize;
            this.expired = expired;
        }

        private String sigalg() {
            return DEFAULT.equals(digestAlgorithm) ? null : expectedSigalg();
        }

        private String expectedSigalg() {
            return (DEFAULT.equals(this.digestAlgorithm) ? this.digestAlgorithm
                    : "SHA-256").replace("-", "") + "with" +
                    keyAlgorithm + (EC.equals(keyAlgorithm) ? "DSA" : "");
        }

        private int expectedKeySize() {
            if (keySize != 0) return keySize;

            // defaults
            if (RSA.equals(keyAlgorithm) || DSA.equals(keyAlgorithm)) {
                return 2048;
            } else if (EC.equals(keyAlgorithm)) {
                return 256;
            } else {
                throw new RuntimeException("problem determining key size");
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + (digestAlgorithm == null ? 0 : digestAlgorithm.hashCode());
            result = prime * result + (expired ? 1231 : 1237);
            result = prime * result
                    + (jdkInfo == null ? 0 : jdkInfo.hashCode());
            result = prime * result
                    + (keyAlgorithm == null ? 0 : keyAlgorithm.hashCode());
            result = prime * result + keySize;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CertInfo other = (CertInfo) obj;
            if (digestAlgorithm == null) {
                if (other.digestAlgorithm != null)
                    return false;
            } else if (!digestAlgorithm.equals(other.digestAlgorithm))
                return false;
            if (expired != other.expired)
                return false;
            if (jdkInfo == null) {
                if (other.jdkInfo != null)
                    return false;
            } else if (!jdkInfo.equals(other.jdkInfo))
                return false;
            if (keyAlgorithm == null) {
                if (other.keyAlgorithm != null)
                    return false;
            } else if (!keyAlgorithm.equals(other.keyAlgorithm))
                return false;
            if (keySize != other.keySize)
                return false;
            return true;
        }

        private String alias() {
            return (jdkInfo.version + "_" + toString())
                    // lower case for jks due to
                    // sun.security.provider.JavaKeyStore.JDK.convertAlias
                    .toLowerCase(Locale.ENGLISH);
        }

        @Override
        public String toString() {
            return "nr" + nr + "_"
                    + keyAlgorithm + "_" + digestAlgorithm
                    + (keySize == 0 ? "" : "_" + keySize)
                    + (expired ? "_Expired" : "");
        }
    }

    // It does only one timestamping for the same JDK, digest algorithm and
    // TSA service with an arbitrary valid/expired certificate.
    private static class TsaFilter {

        private static final Set<Condition> SET = new HashSet<>();

        private static boolean filter(String signerVersion,
                String digestAlgorithm, boolean expiredCert, int tsaIndex) {
            return !SET.add(new Condition(signerVersion, digestAlgorithm,
                    expiredCert, tsaIndex));
        }

        private static class Condition {

            private final String signerVersion;
            private final String digestAlgorithm;
            private final boolean expiredCert;
            private final int tsaIndex;

            private Condition(String signerVersion, String digestAlgorithm,
                    boolean expiredCert, int tsaIndex) {
                this.signerVersion = signerVersion;
                this.digestAlgorithm = digestAlgorithm;
                this.expiredCert = expiredCert;
                this.tsaIndex = tsaIndex;
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result
                        + ((digestAlgorithm == null) ? 0 : digestAlgorithm.hashCode());
                result = prime * result + (expiredCert ? 1231 : 1237);
                result = prime * result
                        + ((signerVersion == null) ? 0 : signerVersion.hashCode());
                result = prime * result + tsaIndex;
                return result;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                Condition other = (Condition) obj;
                if (digestAlgorithm == null) {
                    if (other.digestAlgorithm != null)
                        return false;
                } else if (!digestAlgorithm.equals(other.digestAlgorithm))
                    return false;
                if (expiredCert != other.expiredCert)
                    return false;
                if (signerVersion == null) {
                    if (other.signerVersion != null)
                        return false;
                } else if (!signerVersion.equals(other.signerVersion))
                    return false;
                if (tsaIndex != other.tsaIndex)
                    return false;
                return true;
            }
        }}

    private static enum Status {

        // No action due to pre-action fails.
        NONE,

        // jar is signed/verified with error
        ERROR,

        // jar is signed/verified with warning
        WARNING,

        // jar is signed/verified without any warning and error
        NORMAL
    }

    private static class SignItem {

        private SignItem prevSign;
        private CertInfo certInfo;
        private JdkInfo jdkInfo;
        private String digestAlgorithm;
        private String tsaDigestAlgorithm;
        private int tsaIndex;
        private Status status;
        private String unsignedJar;
        private String signedJar;
        private List<String> jarContents = new ArrayList<>();

        private List<VerifyItem> verifyItems = new ArrayList<>();

        private static SignItem build() {
            return new SignItem()
                    .addContentFiles(Arrays.asList("META-INF/MANIFEST.MF"));
        }

        private static SignItem build(SignItem prevSign) {
            return build().prevSign(prevSign).unsignedJar(prevSign.signedJar)
                    .addContentFiles(prevSign.jarContents);
        }

        private SignItem prevSign(SignItem prevSign) {
            this.prevSign = prevSign;
            return this;
        }

        private SignItem certInfo(CertInfo certInfo) {
            this.certInfo = certInfo;
            return this;
        }

        private SignItem jdkInfo(JdkInfo jdkInfo) {
            this.jdkInfo = jdkInfo;
            return this;
        }

        private SignItem digestAlgorithm(String digestAlgorithm) {
            this.digestAlgorithm = digestAlgorithm;
            return this;
        }

        String expectedDigestAlg() {
            return digestAlgorithm != null ? digestAlgorithm : "SHA-256";
        }

        private SignItem tsaDigestAlgorithm(String tsaDigestAlgorithm) {
            this.tsaDigestAlgorithm = tsaDigestAlgorithm;
            return this;
        }

        String expectedTsaDigestAlg() {
            return tsaDigestAlgorithm != null ? tsaDigestAlgorithm : "SHA-256";
        }

        private SignItem tsaIndex(int tsaIndex) {
            this.tsaIndex = tsaIndex;
            return this;
        }

        private SignItem status(Status status) {
            this.status = status;
            return this;
        }

        private SignItem unsignedJar(String unsignedJar) {
            this.unsignedJar = unsignedJar;
            return this;
        }

        private SignItem signedJar(String signedJar) {
            this.signedJar = signedJar;
            return this;
        }

        private SignItem addContentFiles(List<String> files) {
            this.jarContents.addAll(files);
            return this;
        }

        private void addVerifyItem(VerifyItem verifyItem) {
            verifyItems.add(verifyItem);
        }

        private boolean isErrorInclPrev() {
            if (prevSign != null && prevSign.isErrorInclPrev()) {
                System.out.println("SignItem.isErrorInclPrev: returning true from previous");
                return true;
            }

            return status == Status.ERROR;
        }
        private List<String> toStringWithPrev(Function<SignItem,String> toStr) {
            List<String> s = new ArrayList<>();
            if (prevSign != null) {
                s.addAll(prevSign.toStringWithPrev(toStr));
            }
            if (status != null) { // no status means jar creation or update item
                s.add(toStr.apply(this));
            }
            return s;
        }
    }

    private static class VerifyItem {

        private VerifyItem prevVerify;
        private CertInfo certInfo;
        private JdkInfo jdkInfo;
        private Status status = Status.NONE;
        private Status delayStatus = Status.NONE;

        private static VerifyItem build(JdkInfo jdkInfo) {
            VerifyItem verifyItem = new VerifyItem();
            verifyItem.jdkInfo = jdkInfo;
            return verifyItem;
        }

        private VerifyItem certInfo(CertInfo certInfo) {
            this.certInfo = certInfo;
            return this;
        }

        private void addSignerCertInfos(SignItem signItem) {
            VerifyItem prevVerify = this;
            CertInfo lastCertInfo = null;
            while (signItem != null) {
                // (signItem.certInfo == null) means create or update jar step
                if (signItem.certInfo != null
                        && !signItem.certInfo.equals(lastCertInfo)) {
                    lastCertInfo = signItem.certInfo;
                    prevVerify = prevVerify.prevVerify =
                            build(jdkInfo).certInfo(signItem.certInfo);
                }
                signItem = signItem.prevSign;
            }
        }

        private VerifyItem status(Status status) {
            this.status = status;
            return this;
        }

        private boolean isErrorInclPrev() {
            if (prevVerify != null && prevVerify.isErrorInclPrev()) {
                System.out.println("VerifyItem.isErrorInclPrev: returning true from previous");
                return true;
            }

            return status == Status.ERROR || delayStatus == Status.ERROR;
        }

        private VerifyItem delayStatus(Status status) {
            this.delayStatus = status;
            return this;
        }

        private List<String> toStringWithPrev(
                Function<VerifyItem,String> toStr) {
            List<String> s = new ArrayList<>();
            if (prevVerify != null) {
                s.addAll(prevVerify.toStringWithPrev(toStr));
            }
            s.add(toStr.apply(this));
            return s;
        }
    }

    // The identifier for a specific signing.
    private static String signingId(SignItem signItem) {
        return signItem.signedJar;
    }

    // The identifier for a specific verifying.
    private static String verifyingId(SignItem signItem, VerifyItem verifyItem,
            boolean delayVerify) {
        return signingId(signItem) + (delayVerify ? "-DV" : "-V")
                + "_" + verifyItem.jdkInfo.version +
                (verifyItem.certInfo == null ? "" : "_" + verifyItem.certInfo);
    }

    private static String reportRow(SignItem signItem, VerifyItem verifyItem) {
        List<String> values = new ArrayList<>();
        Consumer<Function<SignItem, String>> s_values_add = f -> {
            values.add(String.join("<br/><br/>", signItem.toStringWithPrev(f)));
        };
        Consumer<Function<VerifyItem, String>> v_values_add = f -> {
            values.add(String.join("<br/><br/>", verifyItem.toStringWithPrev(f)));
        };
        s_values_add.accept(i -> i.unsignedJar + " -> " + i.signedJar);
        s_values_add.accept(i -> i.certInfo.toString());
        s_values_add.accept(i -> i.jdkInfo.version);
        s_values_add.accept(i -> i.certInfo.expectedSigalg());
        s_values_add.accept(i ->
                null2Default(i.digestAlgorithm, i.expectedDigestAlg()));
        s_values_add.accept(i -> i.tsaIndex == -1 ? "" :
                null2Default(i.tsaDigestAlgorithm, i.expectedTsaDigestAlg()));
        s_values_add.accept(i -> i.tsaIndex == -1 ? "" : i.tsaIndex + "");
        s_values_add.accept(i -> HtmlHelper.anchorLink(
                PhaseOutputStream.fileName(PhaseOutputStream.Phase.SIGNING),
                signingId(i),
                "" + i.status));
        values.add(verifyItem.jdkInfo.version);
        v_values_add.accept(i ->
                i.certInfo == null ? "no alias" : "" + i.certInfo);
        v_values_add.accept(i -> HtmlHelper.anchorLink(
                PhaseOutputStream.fileName(PhaseOutputStream.Phase.VERIFYING),
                verifyingId(signItem, i, false),
                "" + i.status.toString()));
        if (DELAY_VERIFY) {
            v_values_add.accept(i -> HtmlHelper.anchorLink(
                    PhaseOutputStream.fileName(
                            PhaseOutputStream.Phase.DELAY_VERIFYING),
                    verifyingId(signItem, verifyItem, true),
                    verifyItem.delayStatus.toString()));
        }
        values.add(isFailed(signItem, verifyItem) ? "X" : "");
        return HtmlHelper.htmlRow(values.toArray(new String[values.size()]));
    }

    private static boolean isFailed(SignItem signItem, VerifyItem verifyItem) {
        System.out.println("isFailed: signItem = " + signItem + ", verifyItem = " + verifyItem);
        // TODO: except known failing cases

        // Note about isAtLeastMajorVersion in the following conditions:
        // signItem.jdkInfo is the jdk which signed the jar last and
        // signItem.prevSign.jdkInfo is the jdk which signed the jar first
        // assuming only two successive signatures as there actually are now.
        // the first signature always works and always has. subject here is
        // the update of an already signed jar. the following conditions always
        // depend on the second jdk that updated the jar with another signature
        // and the first one (signItem(.prevSign)+.jdkInfo) can be ignored.
        // this is different for verifyItem. verifyItem.prevVerify refers to
        // the first signature created by signItem(.prevSign)+.jdkInfo.
        // all verifyItem(.prevVerify)+.jdkInfo however point always to the same
        // jdk, only their certInfo is different. the same signatures are
        // verified with different jdks in different top-level VerifyItems
        // attached directly to signItem.verifyItems and not to
        // verifyItem.prevVerify.

        // ManifestDigester fails to parse manifests ending in '\r' with
        // IndexOutOfBoundsException at ManifestDigester.java:87 before 8217375
        if (signItem.signedJar.startsWith("eofr")
                && !signItem.jdkInfo.isAtLeastMajorVersion(13)
                && !verifyItem.jdkInfo.isAtLeastMajorVersion(13)) return false;

        // if there is no blank line after main attributes, JarSigner adds
        // individual sections nevertheless without being properly delimited
        // in JarSigner.java:777..790 without checking for blank line
        // before 8217375
//        if (signItem.signedJar.startsWith("eofn-")
//                && signItem.signedJar.contains("-addfile-")
//                && !signItem.jdkInfo.isAtLeastMajorVersion(13)
//                && !verifyItem.jdkInfo.isAtLeastMajorVersion(13)) return false; // FIXME

//        System.out.println("isFailed: signItem.isErrorInclPrev() " + signItem.isErrorInclPrev());
//        System.out.println("isFailed: verifyItem.isErrorInclPrev() " + verifyItem.isErrorInclPrev());
        boolean isFailed = signItem.isErrorInclPrev() || verifyItem.isErrorInclPrev();
        System.out.println("isFailed: returning " + isFailed);
        return isFailed;
    }

    // If a value is null, then displays the default value or N/A.
    private static String null2Default(String value, String defaultValue) {
        return value != null ? value :
               DEFAULT + "(" + (defaultValue == null
                                  ? "N/A"
                                  : defaultValue) + ")";
    }

}
