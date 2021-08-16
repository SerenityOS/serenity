/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8215937
 * @modules java.base/sun.security.util
 *          java.base/sun.security.tools.keytool
 *          jdk.jartool/sun.security.tools.jarsigner
 * @summary Check usages of security-related Resources files
 */

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This test checks if the strings in various Resources files are used
 * properly. Each string must be used somewhere, and each getString() call
 * must use an existing string.
 * <p>
 * For each Resources file, the test maintains a list of where the strings are
 * used (a file or a directory) and how they are used (one or more patterns).
 * <p>
 * If this test fails, there can be several reasons:
 * <p>
 * 1. If a string is not found, it has not been added to a Resources file.
 * <p>
 * 2. If a string is not used, maybe the call was removed earlier but the
 * Resources file was not updated. Or, the file is not listed or the
 * pattern is not correct and the usage is not found.
 * <p>
 * Because of #2 above, this test might not be complete. If a getString()
 * is called but either the file and calling pattern is not listed here,
 * we cannot guarantee it exists in a Resources file.
 */
public class Usages {

    // src folder
    static Path SRC = Path.of(
            System.getProperty("test.src"), "../../../../../../src/")
            .normalize();

    // rb.getString(). Used by keytool, jarsigner, and KeyStoreUtil.
    static Pattern RB_GETSTRING = Pattern.compile(
            "(?m)rb[ \\n]*\\.getString[ \\n]*\\([ \\n]*\"(.*?)\"\\)");

    static Pattern EVENT_OCSP_CRL = Pattern.compile(
            "Event\\.report\\(.*, \"(.*?)\",");

    // Command and Option enums in keytool
    static Pattern KT_ENUM = Pattern.compile("\\n +[A-Z]+\\(.*\"(.*)\"");

    // ResourceMgr.getAuthResourceString
    static Pattern GETAUTHSTRING = Pattern.compile(
            "getAuthResourceString[ \\n]*\\([ \\n]*\"(.*?)\"\\)");

    // ResourceMgr.getString
    static Pattern MGR_GETSTRING = Pattern.compile(
            "ResourcesMgr\\.getString[ \\n]*\\([ \\n]*\"(.*?)\"\\)");

    // LocalizedMessage.getNonlocalized("...")
    static Pattern LOC_GETNONLOC = Pattern.compile(
            "LocalizedMessage\\.getNonlocalized[ \\n]*\\([ \\n]*\"(.*?)\"");

    // LocalizedMessage.getNonlocalized(POLICY + "...")
    static Pattern LOC_GETNONLOC_POLICY = Pattern.compile(
            "LocalizedMessage\\.getNonlocalized[ \\n]*\\([ \\n]*(POLICY \\+ \".*?)\"");

    // new LocalizedMessage("...")
    static Pattern NEW_LOC = Pattern.compile(
            "new LocalizedMessage[ \\n]*\\([ \\n]*\"(.*?)\"");

    // ioException in ConfigFile.java
    static Pattern IOEXCEPTION = Pattern.compile(
            "ioException[ \\n]*\\([ \\n]*\"(.*?)\",");

    // For each Resources file, where and how the strings are used.
    static Map<ListResourceBundle, List<Pair>> MAP = Map.of(
            new sun.security.tools.keytool.Resources(), List.of(
                    new Pair("java.base/share/classes/sun/security/tools/keytool/Main.java",
                            List.of(RB_GETSTRING, KT_ENUM)),
                    new Pair("java.base/share/classes/sun/security/tools/KeyStoreUtil.java",
                            List.of(RB_GETSTRING))),
            new sun.security.util.AuthResources(), List.of(
                    new Pair("java.base/share/classes/sun/security/provider/ConfigFile.java",
                            List.of(GETAUTHSTRING, IOEXCEPTION)),
                    new Pair("jdk.security.auth/share/classes/com/sun/security/auth/",
                            List.of(GETAUTHSTRING))),
            new sun.security.tools.jarsigner.Resources(), List.of(
                    new Pair("jdk.jartool/share/classes/sun/security/tools/jarsigner/Main.java",
                            List.of(RB_GETSTRING)),
                    new Pair("java.base/share/classes/sun/security/provider/certpath/OCSP.java",
                            List.of(EVENT_OCSP_CRL)),
                    new Pair("java.base/share/classes/sun/security/provider/certpath/DistributionPointFetcher.java",
                            List.of(EVENT_OCSP_CRL)),
                    new Pair("java.base/share/classes/sun/security/tools/KeyStoreUtil.java",
                            List.of(RB_GETSTRING))),
            new sun.security.util.Resources(), List.of(
                    new Pair("jdk.crypto.cryptoki/share/classes/sun/security/pkcs11/SunPKCS11.java",
                            List.of(MGR_GETSTRING)),
                    new Pair("java.base/share/classes/sun/security/provider/PolicyParser.java",
                            List.of(LOC_GETNONLOC, NEW_LOC)),
                    new Pair("java.base/share/classes/sun/security/provider/PolicyFile.java",
                            List.of(MGR_GETSTRING, LOC_GETNONLOC, LOC_GETNONLOC_POLICY)),
                    new Pair("java.base/share/classes/javax/security/auth/",
                            List.of(MGR_GETSTRING)))
    );

    public static void main(String[] args) {
        if (Files.exists(SRC)) {
            MAP.forEach(Usages::check);
        } else {
            System.out.println("No src directory. Test skipped.");
        }
    }

    private static void check(ListResourceBundle res, List<Pair> fnps) {
        try {
            System.out.println(">>>> Checking " + res.getClass().getName());

            List<String> keys = Collections.list(res.getKeys());

            // Initialize unused to be all keys. Each time a key is used it
            // is removed. We cannot reuse keys because a key might be used
            // multiple times. Make it a Set so we can check duplicates.
            Set<String> unused = new HashSet<>(keys);

            keys.forEach(Usages::checkKeyFormat);
            if (keys.size() != unused.size()) {
                throw new RuntimeException("Duplicates found");
            }

            for (Pair fnp : fnps) {
                Files.find(SRC.resolve(fnp.path), Integer.MAX_VALUE,
                        (p, attr) -> p.toString().endsWith(".java"))
                        .forEach(pa -> {
                            try {
                                String content = Files.readString(pa);
                                for (Pattern p : fnp.patterns) {
                                    Matcher m = p.matcher(content);
                                    while (m.find()) {
                                        String arg = m.group(1);
                                        // Special case in PolicyFile.java:
                                        if (arg.startsWith("POLICY + \"")) {
                                            arg = "java.security.policy"
                                                    + arg.substring(10);
                                        }
                                        if (!keys.contains(arg)) {
                                            throw new RuntimeException(
                                                    "Not found: " + arg);
                                        }
                                        unused.remove(arg);
                                    }
                                }
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
            }
            if (!unused.isEmpty()) {
                throw new RuntimeException("Unused keys: " + unused);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkKeyFormat(String key) {
        for (char c : key.toCharArray()) {
            if (Character.isLetter(c) || Character.isDigit(c) ||
                    c == '{' || c == '}' || c == '.') {
                // OK
            } else {
                throw new RuntimeException(
                        "Illegal char [" + c + "] in key: " + key);
            }
        }
    }

    static class Pair {

        public final String path;
        public final List<Pattern> patterns;

        public Pair(String path, List<Pattern> patterns) {
            this.path = path;
            this.patterns = patterns;
        }
    }
}
