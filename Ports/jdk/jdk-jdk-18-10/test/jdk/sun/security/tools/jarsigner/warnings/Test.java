/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/**
 * Base class.
 */
public abstract class Test {

    static final String TEST_SOURCES = System.getProperty("test.src", ".");
    static final String TEST_CLASSES = System.getProperty("test.classes");
    static final String FS = System.getProperty("file.separator");
    static final String JAVA_HOME = System.getProperty("java.home");
    static final String KEYTOOL = JAVA_HOME + FS + "bin" + FS + "keytool";
    static final String JARSIGNER = JAVA_HOME + FS + "bin" + FS + "jarsigner";
    static final String UNSIGNED_JARFILE = "unsigned.jar";
    static final String SIGNED_JARFILE = "signed.jar";
    static final String UPDATED_SIGNED_JARFILE = "updated_signed.jar";
    static final String FIRST_FILE = "first.txt";
    static final String SECOND_FILE = "second.txt";
    static final String PASSWORD = "password";
    static final String FIRST_KEY_KEYSTORE = "first_key.jks";
    static final String KEYSTORE = "keystore.jks";
    static final String FIRST_KEY_ALIAS = "first";
    static final String SECOND_KEY_ALIAS = "second";
    static final String KEY_ALG = "RSA";
    static final String KEY_ALIAS = "alias";
    static final String CERT_REQUEST_FILENAME = "test.req";
    static final String CERT_FILENAME = "test.crt";
    static final String CA_KEY_ALIAS = "ca";
    static final String CA2_KEY_ALIAS = "ca2";
    static final int KEY_SIZE = 2048;
    static final int TIMEOUT = 6 * 60 * 1000;   // in millis
    static final int VALIDITY = 365;

    static final String WARNING = "Warning:";
    static final String ERROR = "[Ee]rror:";
    static final String WARNING_OR_ERROR = "(" + WARNING + "|" + ERROR + ")";

    static final String CHAIN_NOT_VALIDATED_VERIFYING_WARNING
            = "This jar contains entries "
            + "whose certificate chain is invalid.";

    static final String CERTIFICATE_SELF_SIGNED
            = "The signer's certificate is self-signed.";

    static final String ALIAS_NOT_IN_STORE_VERIFYING_WARNING
            = "This jar contains signed entries "
            + "that are not signed by alias in this keystore.";

    static final String BAD_EXTENDED_KEY_USAGE_SIGNING_WARNING
            = "The signer certificate's ExtendedKeyUsage extension "
            + "doesn't allow code signing.";

    static final String BAD_EXTENDED_KEY_USAGE_VERIFYING_WARNING
            = "This jar contains entries whose signer certificate's "
            + "ExtendedKeyUsage extension doesn't allow code signing.";

    static final String BAD_KEY_USAGE_SIGNING_WARNING
            = "The signer certificate's KeyUsage extension "
            + "doesn't allow code signing.";

    static final String BAD_KEY_USAGE_VERIFYING_WARNING
            = "This jar contains entries whose signer certificate's KeyUsage "
            + "extension doesn't allow code signing.";

    static final String BAD_NETSCAPE_CERT_TYPE_SIGNING_WARNING
            = "The signer certificate's NetscapeCertType extension "
            + "doesn't allow code signing.";

    static final String BAD_NETSCAPE_CERT_TYPE_VERIFYING_WARNING
            = "This jar contains entries "
            + "whose signer certificate's NetscapeCertType extension "
            + "doesn't allow code signing.";

    static final String CHAIN_NOT_VALIDATED_SIGNING_WARNING
            = "The signer's certificate chain is invalid.";

    static final String HAS_EXPIRING_CERT_SIGNING_WARNING
            = "The signer certificate will expire within six months.";

    static final String HAS_EXPIRING_CERT_VERIFYING_WARNING
            = "This jar contains entries "
            + "whose signer certificate will expire within six months.";

    static final String HAS_EXPIRED_CERT_SIGNING_WARNING
            = "The signer certificate has expired.";

    static final String HAS_EXPIRED_CERT_VERIFYING_WARNING
            = "This jar contains entries whose signer certificate has expired.";

    static final String HAS_UNSIGNED_ENTRY_VERIFYING_WARNING
            = "This jar contains unsigned entries "
            + "which have not been integrity-checked.";

    static final String NOT_SIGNED_BY_ALIAS_VERIFYING_WARNING
            = "This jar contains signed entries "
            + "which are not signed by the specified alias(es).";

    static final String NO_TIMESTAMP_SIGNING_WARN_TEMPLATE
            = "No -tsa or -tsacert is provided "
            + "and this jar is not timestamped. "
            + "Without a timestamp, users may not be able to validate this jar "
            + "after the signer certificate's expiration date "
            + "(%1$tY-%1$tm-%1$td).";

    static final String NO_TIMESTAMP_VERIFYING_WARN_TEMPLATE
            = "This jar contains signatures that do not include a timestamp. "
            + "Without a timestamp, users may not be able to validate this jar "
            + "after any of the signer certificates expire "
            + "(as early as %1$tY-%1$tm-%1$td).";

    static final String NOT_YET_VALID_CERT_SIGNING_WARNING
            = "The signer certificate is not yet valid.";

    static final String NOT_YET_VALID_CERT_VERIFYING_WARNING
            = "This jar contains entries "
            + "whose signer certificate is not yet valid.";

    static final String WEAK_ALGORITHM_WARNING
            = "algorithm is considered a security risk. "
            + "This algorithm will be disabled in a future update.";

    static final String JAR_SIGNED = "jar signed.";

    static final String JAR_VERIFIED = "jar verified.";

    static final String JAR_VERIFIED_WITH_SIGNER_ERRORS
            = "jar verified, with signer errors.";

    static final int CHAIN_NOT_VALIDATED_EXIT_CODE = 4;
    static final int HAS_EXPIRED_CERT_EXIT_CODE = 4;
    static final int BAD_KEY_USAGE_EXIT_CODE = 8;
    static final int BAD_EXTENDED_KEY_USAGE_EXIT_CODE = 8;
    static final int BAD_NETSCAPE_CERT_TYPE_EXIT_CODE = 8;
    static final int HAS_UNSIGNED_ENTRY_EXIT_CODE = 16;
    static final int ALIAS_NOT_IN_STORE_EXIT_CODE = 32;
    static final int NOT_SIGNED_BY_ALIAS_EXIT_CODE = 32;

    protected void createAlias(String alias, String ... options)
            throws Throwable {
        List<String> cmd = new ArrayList<>();
        cmd.addAll(List.of(
                "-genkeypair",
                "-alias", alias,
                "-keyalg", KEY_ALG,
                "-keysize", Integer.toString(KEY_SIZE),
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-dname", "CN=" + alias));
        cmd.addAll(Arrays.asList(options));

        keytool(cmd.toArray(new String[cmd.size()]))
                .shouldHaveExitValue(0);
    }

    protected void issueCert(String alias, String ... options)
            throws Throwable {
        keytool("-certreq",
                "-alias", alias,
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-file", alias + ".req")
                    .shouldHaveExitValue(0);

        List<String> cmd = new ArrayList<>();
        cmd.addAll(List.of(
                "-gencert",
                "-alias", CA_KEY_ALIAS,
                "-infile", alias + ".req",
                "-outfile", alias + ".cert",
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-file", alias + ".req"));
        cmd.addAll(Arrays.asList(options));

        keytool(cmd.toArray(new String[cmd.size()]))
                .shouldHaveExitValue(0);

        keytool("-importcert",
                "-alias", alias,
                "-keystore", KEYSTORE,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-file", alias + ".cert")
                    .shouldHaveExitValue(0);
    }

    protected void checkVerifying(OutputAnalyzer analyzer, int expectedExitCode,
            String... warnings) {
        analyzer.shouldHaveExitValue(expectedExitCode);
        int count = 0;
        for (String warning : warnings) {
            if (warning.startsWith("!")) {
                analyzer.shouldNotContain(warning.substring(1));
            } else {
                count++;
                analyzer.shouldContain(warning);
            }
        }
        if (count > 0) {
            analyzer.shouldMatch(WARNING_OR_ERROR);
        }
        if (expectedExitCode == 0) {
            analyzer.shouldContain(JAR_VERIFIED);
        } else {
            analyzer.shouldContain(JAR_VERIFIED_WITH_SIGNER_ERRORS);
        }
    }

    protected void checkSigning(OutputAnalyzer analyzer, String... warnings) {
        analyzer.shouldHaveExitValue(0);
        int count = 0;
        for (String warning : warnings) {
            if (warning.startsWith("!")) {
                analyzer.shouldNotContain(warning.substring(1));
            } else {
                count++;
                analyzer.shouldContain(warning);
            }
        }
        if (count > 0) {
            analyzer.shouldMatch(WARNING_OR_ERROR);
        }
        analyzer.shouldContain(JAR_SIGNED);
    }

    protected OutputAnalyzer keytool(String... cmd) throws Throwable {
        return tool(KEYTOOL, cmd);
    }

    protected OutputAnalyzer jarsigner(String... cmd) throws Throwable {
        return tool(JARSIGNER, cmd);
    }

    private OutputAnalyzer tool(String tool, String... args) throws Throwable {
        List<String> cmd = new ArrayList<>();
        cmd.add(tool);
        cmd.add("-J-Duser.language=en");
        cmd.add("-J-Duser.country=US");
        cmd.addAll(Arrays.asList(args).stream().filter(arg -> {
            return arg != null && !arg.isEmpty();
        }).collect(Collectors.toList()));
        return ProcessTools.executeCommand(cmd.toArray(new String[cmd.size()]));
    }
}
