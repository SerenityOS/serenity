/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.test.lib.security.CertUtils;

/*
 * Utilities for testing.
 */
public class Utils {

    public static final String PROP_JDK_LIST_FILE = "test.jdk.list.file";

    public static final String PROP_SEC_PROPS_FILE = "test.sec.props.file";
    public static final String SEC_PROPS_FILE = System.getProperty(
            PROP_SEC_PROPS_FILE,
            System.getProperty("test.src") + "/java.security");

    public static final Cert RSA_CERT = new Cert(
            KeyAlgorithm.RSA,
            SignatureAlgorithm.RSA,
            HashAlgorithm.SHA256,
            CertUtils.RSA_CERT, CertUtils.RSA_KEY);
    public static final Cert ECDSA_CERT = new Cert(
            KeyAlgorithm.EC,
            SignatureAlgorithm.ECDSA,
            HashAlgorithm.SHA256,
            CertUtils.ECDSA_CERT, CertUtils.ECDSA_KEY);
    public static final Cert ECRSA_CERT = new Cert(
            KeyAlgorithm.EC,
            SignatureAlgorithm.RSA,
            HashAlgorithm.SHA256,
            CertUtils.ECRSA_CERT, CertUtils.ECRSA_KEY);
    public static final Cert DSA_CERT = new Cert(
            KeyAlgorithm.DSA,
            SignatureAlgorithm.DSA,
            HashAlgorithm.SHA256,
            CertUtils.DSA_CERT, CertUtils.DSA_KEY);

    // Retrieves JDK info from the file which is specified by system property
    // test.jdk.list.file.
    public static Set<JdkInfo> jdkInfoList() {
        List<String> jdkList = jdkList();

        Set<JdkInfo> jdkInfoList = new LinkedHashSet<>();
        for (String jdkPath : jdkList) {
            JdkInfo jdkInfo = new JdkInfo(Paths.get(jdkPath, "bin", "java"));
            // JDK version must be unique.
            if (!jdkInfoList.add(jdkInfo)) {
                System.out.println("The JDK version is duplicate: " + jdkPath);
            }
        }
        return jdkInfoList;
    }

    private static List<String> jdkList() {
        String listFile = System.getProperty(PROP_JDK_LIST_FILE);
        System.out.println("jdk list file: " + listFile);
        if (listFile != null && Files.exists(Paths.get(listFile))) {
            try (Stream<String> lines = Files.lines(Paths.get(listFile))) {
                return lines.filter(line -> {
                    return !line.trim().isEmpty();
                }).collect(Collectors.toList());
            } catch (IOException e) {
                throw new RuntimeException("Cannot get jdk list", e);
            }
        } else {
            return new ArrayList<>();
        }
    }

    public static Cert getCert(KeyExAlgorithm keyExAlgorithm) {
        if (keyExAlgorithm == KeyExAlgorithm.RSA
                || keyExAlgorithm == KeyExAlgorithm.DHE_RSA
                || keyExAlgorithm == KeyExAlgorithm.ECDHE_RSA) {
            return RSA_CERT;
        } else if (keyExAlgorithm == KeyExAlgorithm.DHE_DSS) {
            return DSA_CERT;
        } else if (keyExAlgorithm == KeyExAlgorithm.ECDH_RSA) {
            return ECRSA_CERT;
        } else {
            return ECDSA_CERT;
        }
    }
}
