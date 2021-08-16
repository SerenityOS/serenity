/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import static jdk.jpackage.internal.StandardBundlerParam.CONFIG_ROOT;
import static jdk.jpackage.internal.StandardBundlerParam.TEMP_ROOT;
import static jdk.jpackage.internal.StandardBundlerParam.VERBOSE;
import static jdk.jpackage.internal.StandardBundlerParam.APP_NAME;
import static jdk.jpackage.internal.StandardBundlerParam.LICENSE_FILE;
import static jdk.jpackage.internal.StandardBundlerParam.VERSION;
import static jdk.jpackage.internal.StandardBundlerParam.SIGN_BUNDLE;
import static jdk.jpackage.internal.MacBaseInstallerBundler.SIGNING_KEYCHAIN;
import static jdk.jpackage.internal.MacBaseInstallerBundler.SIGNING_KEY_USER;
import static jdk.jpackage.internal.MacAppImageBuilder.APP_STORE;
import static jdk.jpackage.internal.MacAppImageBuilder.MAC_CF_BUNDLE_IDENTIFIER;
import static jdk.jpackage.internal.OverridableResource.createResource;

public class MacPkgBundler extends MacBaseInstallerBundler {

    private static final ResourceBundle I18N = ResourceBundle.getBundle(
            "jdk.jpackage.internal.resources.MacResources");

    private static final String DEFAULT_BACKGROUND_IMAGE = "background_pkg.png";
    private static final String DEFAULT_PDF = "product-def.plist";

    private static final String TEMPLATE_PREINSTALL_SCRIPT =
            "preinstall.template";
    private static final String TEMPLATE_POSTINSTALL_SCRIPT =
            "postinstall.template";

    private static final BundlerParamInfo<Path> PACKAGES_ROOT =
            new StandardBundlerParam<>(
            "mac.pkg.packagesRoot",
            Path.class,
            params -> {
                Path packagesRoot =
                        TEMP_ROOT.fetchFrom(params).resolve("packages");
                try {
                    Files.createDirectories(packagesRoot);
                } catch (IOException ioe) {
                    return null;
                }
                return packagesRoot;
            },
            (s, p) -> Path.of(s));


    protected final BundlerParamInfo<Path> SCRIPTS_DIR =
            new StandardBundlerParam<>(
            "mac.pkg.scriptsDir",
            Path.class,
            params -> {
                Path scriptsDir =
                        CONFIG_ROOT.fetchFrom(params).resolve("scripts");
                try {
                    Files.createDirectories(scriptsDir);
                } catch (IOException ioe) {
                    return null;
                }
                return scriptsDir;
            },
            (s, p) -> Path.of(s));

    public static final
            BundlerParamInfo<String> DEVELOPER_ID_INSTALLER_SIGNING_KEY =
            new StandardBundlerParam<>(
            "mac.signing-key-developer-id-installer",
            String.class,
            params -> {
                    String user = SIGNING_KEY_USER.fetchFrom(params);
                    String keychain = SIGNING_KEYCHAIN.fetchFrom(params);
                    String result = null;
                    if (APP_STORE.fetchFrom(params)) {
                        result = MacBaseInstallerBundler.findKey(
                            "3rd Party Mac Developer Installer: ",
                            user, keychain);
                    }
                    // if either not signing for app store or couldn't find
                    if (result == null) {
                        result = MacBaseInstallerBundler.findKey(
                            "Developer ID Installer: ", user, keychain);
                    }

                    if (result != null) {
                        MacCertificate certificate = new MacCertificate(result);

                        if (!certificate.isValid()) {
                            Log.error(MessageFormat.format(
                                    I18N.getString("error.certificate.expired"),
                                    result));
                        }
                    }

                    return result;
                },
            (s, p) -> s);

    public static final BundlerParamInfo<String> INSTALLER_SUFFIX =
            new StandardBundlerParam<> (
            "mac.pkg.installerName.suffix",
            String.class,
            params -> "",
            (s, p) -> s);

    public Path bundle(Map<String, ? super Object> params,
            Path outdir) throws PackagerException {
        Log.verbose(MessageFormat.format(I18N.getString("message.building-pkg"),
                APP_NAME.fetchFrom(params)));

        IOUtils.writableOutputDir(outdir);

        try {
            Path appImageDir = prepareAppBundle(params);

            if (appImageDir != null && prepareConfigFiles(params)) {

                Path configScript = getConfig_Script(params);
                if (IOUtils.exists(configScript)) {
                    IOUtils.run("bash", configScript);
                }

                return createPKG(params, outdir, appImageDir);
            }
            return null;
        } catch (IOException ex) {
            Log.verbose(ex);
            throw new PackagerException(ex);
        }
    }

    private Path getPackages_AppPackage(Map<String, ? super Object> params) {
        return PACKAGES_ROOT.fetchFrom(params).resolve(
                APP_NAME.fetchFrom(params) + "-app.pkg");
    }

    private Path getConfig_DistributionXMLFile(
            Map<String, ? super Object> params) {
        return CONFIG_ROOT.fetchFrom(params).resolve("distribution.dist");
    }

    private Path getConfig_PDF(Map<String, ? super Object> params) {
        return CONFIG_ROOT.fetchFrom(params).resolve("product-def.plist");
    }

    private Path getConfig_BackgroundImage(Map<String, ? super Object> params) {
        return CONFIG_ROOT.fetchFrom(params).resolve(
                APP_NAME.fetchFrom(params) + "-background.png");
    }

    private Path getConfig_BackgroundImageDarkAqua(Map<String, ? super Object> params) {
        return CONFIG_ROOT.fetchFrom(params).resolve(
                APP_NAME.fetchFrom(params) + "-background-darkAqua.png");
    }

    private Path getScripts_PreinstallFile(Map<String, ? super Object> params) {
        return SCRIPTS_DIR.fetchFrom(params).resolve("preinstall");
    }

    private Path getScripts_PostinstallFile(
            Map<String, ? super Object> params) {
        return SCRIPTS_DIR.fetchFrom(params).resolve("postinstall");
    }

    private String getAppIdentifier(Map<String, ? super Object> params) {
        return MAC_CF_BUNDLE_IDENTIFIER.fetchFrom(params);
    }

    private void preparePackageScripts(Map<String, ? super Object> params)
            throws IOException {
        Log.verbose(I18N.getString("message.preparing-scripts"));

        Map<String, String> data = new HashMap<>();

        Path appLocation = Path.of(getInstallDir(params, false),
                         APP_NAME.fetchFrom(params) + ".app", "Contents", "app");

        data.put("INSTALL_LOCATION", getInstallDir(params, false));
        data.put("APP_LOCATION", appLocation.toString());

        createResource(TEMPLATE_PREINSTALL_SCRIPT, params)
                .setCategory(I18N.getString("resource.pkg-preinstall-script"))
                .setSubstitutionData(data)
                .saveToFile(getScripts_PreinstallFile(params));
        getScripts_PreinstallFile(params).toFile().setExecutable(true, false);

        createResource(TEMPLATE_POSTINSTALL_SCRIPT, params)
                .setCategory(I18N.getString("resource.pkg-postinstall-script"))
                .setSubstitutionData(data)
                .saveToFile(getScripts_PostinstallFile(params));
        getScripts_PostinstallFile(params).toFile().setExecutable(true, false);
    }

    private static String URLEncoding(String pkgName) throws URISyntaxException {
        URI uri = new URI(null, null, pkgName, null);
        return uri.toASCIIString();
    }

    private void prepareDistributionXMLFile(Map<String, ? super Object> params)
            throws IOException {
        Path f = getConfig_DistributionXMLFile(params);

        Log.verbose(MessageFormat.format(I18N.getString(
                "message.preparing-distribution-dist"), f.toAbsolutePath().toString()));

        IOUtils.createXml(f, xml -> {
            xml.writeStartElement("installer-gui-script");
            xml.writeAttribute("minSpecVersion", "1");

            xml.writeStartElement("title");
            xml.writeCharacters(APP_NAME.fetchFrom(params));
            xml.writeEndElement();

            xml.writeStartElement("background");
            xml.writeAttribute("file",
                    getConfig_BackgroundImage(params).getFileName().toString());
            xml.writeAttribute("mime-type", "image/png");
            xml.writeAttribute("alignment", "bottomleft");
            xml.writeAttribute("scaling", "none");
            xml.writeEndElement();

            xml.writeStartElement("background-darkAqua");
            xml.writeAttribute("file",
                    getConfig_BackgroundImageDarkAqua(params).getFileName().toString());
            xml.writeAttribute("mime-type", "image/png");
            xml.writeAttribute("alignment", "bottomleft");
            xml.writeAttribute("scaling", "none");
            xml.writeEndElement();

            String licFileStr = LICENSE_FILE.fetchFrom(params);
            if (licFileStr != null) {
                Path licFile = Path.of(licFileStr);
                xml.writeStartElement("license");
                xml.writeAttribute("file", licFile.toAbsolutePath().toString());
                xml.writeAttribute("mime-type", "text/rtf");
                xml.writeEndElement();
            }

            /*
             * Note that the content of the distribution file
             * below is generated by productbuild --synthesize
             */
            String appId = getAppIdentifier(params);

            xml.writeStartElement("pkg-ref");
            xml.writeAttribute("id", appId);
            xml.writeEndElement(); // </pkg-ref>
            xml.writeStartElement("options");
            xml.writeAttribute("customize", "never");
            xml.writeAttribute("require-scripts", "false");
            xml.writeAttribute("hostArchitectures",
                    Platform.isArmMac() ? "arm64" : "x86_64");
            xml.writeEndElement(); // </options>
            xml.writeStartElement("choices-outline");
            xml.writeStartElement("line");
            xml.writeAttribute("choice", "default");
            xml.writeStartElement("line");
            xml.writeAttribute("choice", appId);
            xml.writeEndElement(); // </line>
            xml.writeEndElement(); // </line>
            xml.writeEndElement(); // </choices-outline>
            xml.writeStartElement("choice");
            xml.writeAttribute("id", "default");
            xml.writeEndElement(); // </choice>
            xml.writeStartElement("choice");
            xml.writeAttribute("id", appId);
            xml.writeAttribute("visible", "false");
            xml.writeStartElement("pkg-ref");
            xml.writeAttribute("id", appId);
            xml.writeEndElement(); // </pkg-ref>
            xml.writeEndElement(); // </choice>
            xml.writeStartElement("pkg-ref");
            xml.writeAttribute("id", appId);
            xml.writeAttribute("version", VERSION.fetchFrom(params));
            xml.writeAttribute("onConclusion", "none");
            try {
                xml.writeCharacters(URLEncoding(
                        getPackages_AppPackage(params).getFileName().toString()));
            } catch (URISyntaxException ex) {
                throw new IOException(ex);
            }
            xml.writeEndElement(); // </pkg-ref>

            xml.writeEndElement(); // </installer-gui-script>
        });
    }

    private boolean prepareConfigFiles(Map<String, ? super Object> params)
            throws IOException {

        createResource(DEFAULT_BACKGROUND_IMAGE, params)
                .setCategory(I18N.getString("resource.pkg-background-image"))
                .saveToFile(getConfig_BackgroundImage(params));

        createResource(DEFAULT_BACKGROUND_IMAGE, params)
                .setCategory(I18N.getString("resource.pkg-background-image"))
                .saveToFile(getConfig_BackgroundImageDarkAqua(params));

        createResource(DEFAULT_PDF, params)
                .setCategory(I18N.getString("resource.pkg-pdf"))
                .saveToFile(getConfig_PDF(params));

        prepareDistributionXMLFile(params);

        createResource(null, params)
                .setCategory(I18N.getString("resource.post-install-script"))
                .saveToFile(getConfig_Script(params));

        return true;
    }

    // name of post-image script
    private Path getConfig_Script(Map<String, ? super Object> params) {
        return CONFIG_ROOT.fetchFrom(params).resolve(
                APP_NAME.fetchFrom(params) + "-post-image.sh");
    }

    private void patchCPLFile(Path cpl) throws IOException {
        String cplData = Files.readString(cpl);
        String[] lines = cplData.split("\n");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(cpl))) {
            int skip = 0;
            // Used to skip Java.runtime bundle, since
            // pkgbuild with --root will find two bundles app and Java runtime.
            // We cannot generate component proprty list when using
            // --component argument.
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].trim().equals("<key>BundleIsRelocatable</key>")) {
                    out.println(lines[i]);
                    out.println("<false/>");
                    i++;
                } else if (lines[i].trim().equals("<key>ChildBundles</key>")) {
                    ++skip;
                } else if ((skip > 0) && lines[i].trim().equals("</array>")) {
                    --skip;
                } else {
                    if (skip == 0) {
                        out.println(lines[i]);
                    }
                }
            }
        }
    }

    // pkgbuild includes all components from "--root" and subfolders,
    // so if we have app image in folder which contains other images, then they
    // will be included as well. It does have "--filter" option which use regex
    // to exclude files/folder, but it will overwrite default one which excludes
    // based on doc "any .svn or CVS directories, and any .DS_Store files".
    // So easy aproach will be to copy user provided app-image into temp folder
    // if root path contains other files.
    private String getRoot(Map<String, ? super Object> params,
            Path appLocation) throws IOException {
        Path rootDir = appLocation.getParent() == null ?
                Path.of(".") : appLocation.getParent();

        // Not needed for runtime installer and it might break runtime installer
        // if parent does not have any other files
        if (!StandardBundlerParam.isRuntimeInstaller(params)) {
            try (var fileList = Files.list(rootDir)) {
                Path[] list = fileList.toArray(Path[]::new);
                // We should only have app image and/or .DS_Store
                if (list.length == 1) {
                    return rootDir.toString();
                } else if (list.length == 2) {
                    // Check case with app image and .DS_Store
                    if (list[0].toString().toLowerCase().endsWith(".ds_store") ||
                        list[1].toString().toLowerCase().endsWith(".ds_store")) {
                        return rootDir.toString(); // Only app image and .DS_Store
                    }
                }
            }
        }

        // Copy to new root
        Path newRoot = Files.createTempDirectory(
                TEMP_ROOT.fetchFrom(params), "root-");

        Path source, dest;

        if (StandardBundlerParam.isRuntimeInstaller(params)) {
            // firs, is this already a runtime with
            // <runtime>/Contents/Home - if so we need the Home dir
            Path original = appLocation;
            Path home = original.resolve("Contents/Home");
            source = (Files.exists(home)) ? home : original;

            // Then we need to put back the <NAME>/Content/Home
            dest = newRoot.resolve(
                MAC_CF_BUNDLE_IDENTIFIER.fetchFrom(params) + "/Contents/Home");
        } else {
            source = appLocation;
            dest = newRoot.resolve(appLocation.getFileName());
        }
        IOUtils.copyRecursive(source, dest);

        return newRoot.toString();
    }

    private Path createPKG(Map<String, ? super Object> params,
            Path outdir, Path appLocation) {
        // generic find attempt
        try {
            Path appPKG = getPackages_AppPackage(params);

            String root = getRoot(params, appLocation);

            // Generate default CPL file
            Path cpl = CONFIG_ROOT.fetchFrom(params).resolve("cpl.plist");
            ProcessBuilder pb = new ProcessBuilder("/usr/bin/pkgbuild",
                    "--root",
                    root,
                    "--install-location",
                    getInstallDir(params, false),
                    "--analyze",
                    cpl.toAbsolutePath().toString());

            IOUtils.exec(pb, false, null, true, Executor.INFINITE_TIMEOUT);

            patchCPLFile(cpl);

            // build application package
            if (APP_STORE.fetchFrom(params)) {
                pb = new ProcessBuilder("/usr/bin/pkgbuild",
                        "--root",
                        root,
                        "--install-location",
                        getInstallDir(params, false),
                        "--component-plist",
                        cpl.toAbsolutePath().toString(),
                        "--identifier",
                         MAC_CF_BUNDLE_IDENTIFIER.fetchFrom(params),
                        appPKG.toAbsolutePath().toString());
                IOUtils.exec(pb, false, null, true, Executor.INFINITE_TIMEOUT);
            } else {
                preparePackageScripts(params);
                pb = new ProcessBuilder("/usr/bin/pkgbuild",
                        "--root",
                        root,
                        "--install-location",
                        getInstallDir(params, false),
                        "--component-plist",
                        cpl.toAbsolutePath().toString(),
                        "--scripts",
                        SCRIPTS_DIR.fetchFrom(params)
                        .toAbsolutePath().toString(),
                        "--identifier",
                         MAC_CF_BUNDLE_IDENTIFIER.fetchFrom(params),
                        appPKG.toAbsolutePath().toString());
                IOUtils.exec(pb, false, null, true, Executor.INFINITE_TIMEOUT);
            }

            // build final package
            Path finalPKG = outdir.resolve(MAC_INSTALLER_NAME.fetchFrom(params)
                    + INSTALLER_SUFFIX.fetchFrom(params)
                    + ".pkg");
            Files.createDirectories(outdir);

            List<String> commandLine = new ArrayList<>();
            commandLine.add("/usr/bin/productbuild");

            commandLine.add("--resources");
            commandLine.add(CONFIG_ROOT.fetchFrom(params).toAbsolutePath().toString());

            // maybe sign
            if (Optional.ofNullable(
                    SIGN_BUNDLE.fetchFrom(params)).orElse(Boolean.TRUE)) {
                if (Platform.getMajorVersion() > 10 ||
                    (Platform.getMajorVersion() == 10 &&
                    Platform.getMinorVersion() >= 12)) {
                    // we need this for OS X 10.12+
                    Log.verbose(I18N.getString("message.signing.pkg"));
                }

                String signingIdentity =
                        DEVELOPER_ID_INSTALLER_SIGNING_KEY.fetchFrom(params);
                if (signingIdentity != null) {
                    commandLine.add("--sign");
                    commandLine.add(signingIdentity);
                }

                String keychainName = SIGNING_KEYCHAIN.fetchFrom(params);
                if (keychainName != null && !keychainName.isEmpty()) {
                    commandLine.add("--keychain");
                    commandLine.add(keychainName);
                }
            }

            if (APP_STORE.fetchFrom(params)) {
                commandLine.add("--product");
                commandLine.add(getConfig_PDF(params)
                        .toAbsolutePath().toString());
                commandLine.add("--component");
                Path p = Path.of(root, APP_NAME.fetchFrom(params) + ".app");
                commandLine.add(p.toAbsolutePath().toString());
                commandLine.add(getInstallDir(params, false));
            } else {
                commandLine.add("--distribution");
                commandLine.add(getConfig_DistributionXMLFile(params)
                        .toAbsolutePath().toString());
                commandLine.add("--package-path");
                commandLine.add(PACKAGES_ROOT.fetchFrom(params)
                        .toAbsolutePath().toString());
            }
            commandLine.add(finalPKG.toAbsolutePath().toString());

            pb = new ProcessBuilder(commandLine);
            IOUtils.exec(pb, false, null, true, Executor.INFINITE_TIMEOUT);

            return finalPKG;
        } catch (Exception ignored) {
            Log.verbose(ignored);
            return null;
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Implement Bundler
    //////////////////////////////////////////////////////////////////////////

    @Override
    public String getName() {
        return I18N.getString("pkg.bundler.name");
    }

    @Override
    public String getID() {
        return "pkg";
    }

    private static boolean isValidBundleIdentifier(String id) {
        for (int i = 0; i < id.length(); i++) {
            char a = id.charAt(i);
            // We check for ASCII codes first which we accept. If check fails,
            // check if it is acceptable extended ASCII or unicode character.
            if ((a >= 'A' && a <= 'Z') || (a >= 'a' && a <= 'z')
                    || (a >= '0' && a <= '9') || (a == '-' || a == '.')) {
                continue;
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean validate(Map<String, ? super Object> params)
            throws ConfigException {
        try {
            Objects.requireNonNull(params);

            // run basic validation to ensure requirements are met
            // we are not interested in return code, only possible exception
            validateAppImageAndBundeler(params);

            String identifier = MAC_CF_BUNDLE_IDENTIFIER.fetchFrom(params);
            if (identifier == null) {
                throw new ConfigException(
                        I18N.getString("message.app-image-requires-identifier"),
                        I18N.getString(
                            "message.app-image-requires-identifier.advice"));
            }
            if (!isValidBundleIdentifier(identifier)) {
                throw new ConfigException(
                        MessageFormat.format(I18N.getString(
                        "message.invalid-identifier"), identifier),
                        I18N.getString("message.invalid-identifier.advice"));
            }

            // reject explicitly set sign to true and no valid signature key
            if (Optional.ofNullable(
                    SIGN_BUNDLE.fetchFrom(params)).orElse(Boolean.FALSE)) {
                String signingIdentity =
                        DEVELOPER_ID_INSTALLER_SIGNING_KEY.fetchFrom(params);
                if (signingIdentity == null) {
                    throw new ConfigException(
                            I18N.getString("error.explicit-sign-no-cert"),
                            I18N.getString(
                            "error.explicit-sign-no-cert.advice"));
                }
            }

            // hdiutil is always available so there's no need
            // to test for availability.

            return true;
        } catch (RuntimeException re) {
            if (re.getCause() instanceof ConfigException) {
                throw (ConfigException) re.getCause();
            } else {
                throw new ConfigException(re);
            }
        }
    }

    @Override
    public Path execute(Map<String, ? super Object> params,
            Path outputParentDir) throws PackagerException {
        return bundle(params, outputParentDir);
    }

    @Override
    public boolean supported(boolean runtimeInstaller) {
        return true;
    }

    @Override
    public boolean isDefault() {
        return false;
    }

}
