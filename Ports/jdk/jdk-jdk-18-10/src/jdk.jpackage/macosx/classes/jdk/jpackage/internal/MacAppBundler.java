/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.jpackage.internal;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Optional;
import static jdk.jpackage.internal.MacBaseInstallerBundler.SIGNING_KEYCHAIN;
import static jdk.jpackage.internal.MacBaseInstallerBundler.SIGNING_KEY_USER;
import static jdk.jpackage.internal.MacAppImageBuilder.APP_STORE;
import static jdk.jpackage.internal.StandardBundlerParam.MAIN_CLASS;
import static jdk.jpackage.internal.StandardBundlerParam.VERBOSE;
import static jdk.jpackage.internal.StandardBundlerParam.VERSION;
import static jdk.jpackage.internal.StandardBundlerParam.SIGN_BUNDLE;

public class MacAppBundler extends AppImageBundler {
     public MacAppBundler() {
        setAppImageSupplier(MacAppImageBuilder::new);
        setParamsValidator(MacAppBundler::doValidate);
    }

    private static final String TEMPLATE_BUNDLE_ICON = "JavaApp.icns";

    public static final BundlerParamInfo<String> MAC_CF_BUNDLE_NAME =
            new StandardBundlerParam<>(
                    Arguments.CLIOptions.MAC_BUNDLE_NAME.getId(),
                    String.class,
                    params -> null,
                    (s, p) -> s);

    public static final BundlerParamInfo<String> DEFAULT_ICNS_ICON =
            new StandardBundlerParam<>(
            ".mac.default.icns",
            String.class,
            params -> TEMPLATE_BUNDLE_ICON,
            (s, p) -> s);

    public static final BundlerParamInfo<String> DEVELOPER_ID_APP_SIGNING_KEY =
            new StandardBundlerParam<>(
            "mac.signing-key-developer-id-app",
            String.class,
            params -> {
                    String user = SIGNING_KEY_USER.fetchFrom(params);
                    String keychain = SIGNING_KEYCHAIN.fetchFrom(params);
                    String result = null;
                    if (APP_STORE.fetchFrom(params)) {
                        result = MacBaseInstallerBundler.findKey(
                            "3rd Party Mac Developer Application: ",
                            user, keychain);
                    }
                    // if either not signing for app store or couldn't find
                    if (result == null) {
                        result = MacBaseInstallerBundler.findKey(
                            "Developer ID Application: ", user, keychain);
                    }

                    if (result != null) {
                        MacCertificate certificate = new MacCertificate(result);

                        if (!certificate.isValid()) {
                            Log.error(MessageFormat.format(I18N.getString(
                                    "error.certificate.expired"), result));
                        }
                    }

                    return result;
                },
            (s, p) -> s);

    public static final BundlerParamInfo<String> BUNDLE_ID_SIGNING_PREFIX =
            new StandardBundlerParam<>(
            Arguments.CLIOptions.MAC_BUNDLE_SIGNING_PREFIX.getId(),
            String.class,
            params -> getIdentifier(params) + ".",
            (s, p) -> s);

    static String getIdentifier(Map<String, ? super Object> params) {
        String s = MAIN_CLASS.fetchFrom(params);
        if (s == null) return null;

        int idx = s.lastIndexOf(".");
        if (idx >= 1) {
            return s.substring(0, idx);
        }
        return s;
    }

    private static void doValidate(Map<String, ? super Object> params)
            throws ConfigException {

        if (StandardBundlerParam.getPredefinedAppImage(params) != null) {
            return;
        }

        // validate short version
        try {
            String version = VERSION.fetchFrom(params);
            CFBundleVersion.of(version);
        } catch (IllegalArgumentException ex) {
            throw new ConfigException(ex.getMessage(), I18N.getString(
                    "error.invalid-cfbundle-version.advice"), ex);
        }

        // reject explicitly set sign to true and no valid signature key
        if (Optional.ofNullable(
                    SIGN_BUNDLE.fetchFrom(params)).orElse(Boolean.FALSE)) {
            String signingIdentity =
                    DEVELOPER_ID_APP_SIGNING_KEY.fetchFrom(params);
            if (signingIdentity == null) {
                throw new ConfigException(
                        I18N.getString("error.explicit-sign-no-cert"),
                        I18N.getString("error.explicit-sign-no-cert.advice"));
            }

            // Signing will not work without Xcode with command line developer tools
            try {
                ProcessBuilder pb = new ProcessBuilder("/usr/bin/xcrun", "--help");
                Process p = pb.start();
                int code = p.waitFor();
                if (code != 0) {
                    throw new ConfigException(
                        I18N.getString("error.no.xcode.signing"),
                        I18N.getString("error.no.xcode.signing.advice"));
                }
            } catch (IOException | InterruptedException ex) {
                throw new ConfigException(ex);
            }
        }
    }
}
