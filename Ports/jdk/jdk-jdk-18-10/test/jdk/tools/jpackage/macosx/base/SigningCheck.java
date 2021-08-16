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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jdk.jpackage.test.TKit;
import jdk.jpackage.test.Executor;

import jdk.jpackage.internal.MacCertificate;

public class SigningCheck {

    public static void checkCertificates() {
        List<String> result = findCertificate(SigningBase.APP_CERT, SigningBase.KEYCHAIN);
        String key = findKey(SigningBase.APP_CERT, result);
        validateCertificate(key);
        validateCertificateTrust(SigningBase.APP_CERT);

        result = findCertificate(SigningBase.INSTALLER_CERT, SigningBase.KEYCHAIN);
        key = findKey(SigningBase.INSTALLER_CERT, result);
        validateCertificate(key);
        validateCertificateTrust(SigningBase.INSTALLER_CERT);
    }

    private static List<String> findCertificate(String name, String keyChain) {
        List<String> result = new Executor()
                .setExecutable("/usr/bin/security")
                .addArguments("find-certificate", "-c", name, "-a", keyChain)
                .executeAndGetOutput();

        return result;
    }

    private static String findKey(String name, List<String> result) {
        Pattern p = Pattern.compile("\"alis\"<blob>=\"([^\"]+)\"");
        Matcher m = p.matcher(result.stream().collect(Collectors.joining()));
        if (!m.find()) {
            TKit.trace("Did not found a key for '" + name + "'");
            return null;
        }
        String matchedKey = m.group(1);
        if (m.find()) {
            TKit.trace("Found more than one key for '" + name + "'");
            return null;
        }
        TKit.trace("Using key '" + matchedKey);
        return matchedKey;
    }

    private static void validateCertificate(String key) {
        if (key != null) {
            MacCertificate certificate = new MacCertificate(key);
            if (!certificate.isValid()) {
                TKit.throwSkippedException("Certifcate expired: " + key);
            } else {
                return;
            }
        }

        TKit.throwSkippedException("Cannot find required certifciates: " + key);
    }

    private static void validateCertificateTrust(String name) {
        // Certificates using the default user name must be trusted by user.
        // User supplied certs whose trust is set to "Use System Defaults"
        // will not be listed as trusted by dump-trust-settings
        if (SigningBase.DEV_NAME.equals("jpackage.openjdk.java.net")) {
            List<String> result = new Executor()
                    .setExecutable("/usr/bin/security")
                    .addArguments("dump-trust-settings")
                    .executeWithoutExitCodeCheckAndGetOutput();
            result.stream().forEachOrdered(TKit::trace);
            TKit.assertTextStream(name)
                    .predicate((line, what) -> line.trim().endsWith(what))
                    .orElseThrow(() -> TKit.throwSkippedException(
                            "Certifcate not trusted by current user: " + name))
                    .apply(result.stream());
        }
    }

}
