/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import static jdk.jpackage.internal.MacAppBundler.BUNDLE_ID_SIGNING_PREFIX;
import static jdk.jpackage.internal.MacAppBundler.DEVELOPER_ID_APP_SIGNING_KEY;
import static jdk.jpackage.internal.MacBaseInstallerBundler.SIGNING_KEYCHAIN;
import static jdk.jpackage.internal.OverridableResource.createResource;
import static jdk.jpackage.internal.StandardBundlerParam.APP_NAME;
import static jdk.jpackage.internal.StandardBundlerParam.CONFIG_ROOT;
import static jdk.jpackage.internal.StandardBundlerParam.COPYRIGHT;
import static jdk.jpackage.internal.StandardBundlerParam.FA_CONTENT_TYPE;
import static jdk.jpackage.internal.StandardBundlerParam.FA_DESCRIPTION;
import static jdk.jpackage.internal.StandardBundlerParam.FA_EXTENSIONS;
import static jdk.jpackage.internal.StandardBundlerParam.FA_ICON;
import static jdk.jpackage.internal.StandardBundlerParam.FILE_ASSOCIATIONS;
import static jdk.jpackage.internal.StandardBundlerParam.ICON;
import static jdk.jpackage.internal.StandardBundlerParam.MAIN_CLASS;
import static jdk.jpackage.internal.StandardBundlerParam.PREDEFINED_APP_IMAGE;
import static jdk.jpackage.internal.StandardBundlerParam.VERSION;
import static jdk.jpackage.internal.StandardBundlerParam.ADD_LAUNCHERS;
import static jdk.jpackage.internal.StandardBundlerParam.SIGN_BUNDLE;

public class MacAppImageBuilder extends AbstractAppImageBuilder {

    private static final ResourceBundle I18N = ResourceBundle.getBundle(
            "jdk.jpackage.internal.resources.MacResources");

    private static final String TEMPLATE_BUNDLE_ICON = "JavaApp.icns";
    private static final String OS_TYPE_CODE = "APPL";
    private static final String TEMPLATE_INFO_PLIST_LITE =
            "Info-lite.plist.template";
    private static final String TEMPLATE_RUNTIME_INFO_PLIST =
            "Runtime-Info.plist.template";

    private final Path root;
    private final Path contentsDir;
    private final Path resourcesDir;
    private final Path macOSDir;
    private final Path runtimeDir;
    private final Path runtimeRoot;

    private static List<String> keyChains;

    public static final BundlerParamInfo<Boolean>
            MAC_CONFIGURE_LAUNCHER_IN_PLIST = new StandardBundlerParam<>(
                    "mac.configure-launcher-in-plist",
                    Boolean.class,
                    params -> Boolean.FALSE,
                    (s, p) -> Boolean.valueOf(s));

    public static final BundlerParamInfo<String> MAC_CF_BUNDLE_NAME =
            new StandardBundlerParam<>(
                    Arguments.CLIOptions.MAC_BUNDLE_NAME.getId(),
                    String.class,
                    params -> null,
                    (s, p) -> s);

    public static final BundlerParamInfo<String> APP_CATEGORY =
            new StandardBundlerParam<>(
                    Arguments.CLIOptions.MAC_CATEGORY.getId(),
                    String.class,
                    params -> "utilities",
                    (s, p) -> s);

    public static final BundlerParamInfo<String> MAC_CF_BUNDLE_IDENTIFIER =
            new StandardBundlerParam<>(
                    Arguments.CLIOptions.MAC_BUNDLE_IDENTIFIER.getId(),
                    String.class,
                    params -> {
                        // Get identifier from app image if user provided
                        // app image and did not provide the identifier via CLI.
                        String identifier = extractBundleIdentifier(params);
                        if (identifier == null) {
                            identifier =  MacAppBundler.getIdentifier(params);
                        }
                        if (identifier == null) {
                            identifier = APP_NAME.fetchFrom(params);
                        }
                        return identifier;
                    },
                    (s, p) -> s);

    public static final BundlerParamInfo<Path> ICON_ICNS =
            new StandardBundlerParam<>(
            "icon.icns",
            Path.class,
            params -> {
                Path f = ICON.fetchFrom(params);
                if (f != null && f.getFileName() != null && !f.getFileName()
                        .toString().toLowerCase().endsWith(".icns")) {
                    Log.error(MessageFormat.format(
                            I18N.getString("message.icon-not-icns"), f));
                    return null;
                }
                return f;
            },
            (s, p) -> Path.of(s));

    public static final StandardBundlerParam<Boolean> APP_STORE =
            new StandardBundlerParam<>(
            Arguments.CLIOptions.MAC_APP_STORE.getId(),
            Boolean.class,
            params -> false,
            // valueOf(null) is false, we actually do want null in some cases
            (s, p) -> (s == null || "null".equalsIgnoreCase(s)) ?
                    null : Boolean.valueOf(s)
        );

    public static final BundlerParamInfo<Path> ENTITLEMENTS =
            new StandardBundlerParam<>(
            Arguments.CLIOptions.MAC_ENTITLEMENTS.getId(),
            Path.class,
            params -> {
                try {
                    Path path = CONFIG_ROOT.fetchFrom(params).resolve(
                        getLauncherName(params) + ".entitlements");
                    String defPath = (APP_STORE.fetchFrom(params) ?
                        "sandbox.plist" : "entitlements.plist");
                    createResource(defPath, params)
                        .setCategory(I18N.getString("resource.entitlements"))
                        .saveToFile(path);
                    return path;
                } catch (IOException ioe) {
                   Log.verbose(ioe);
                }
                return null;
            },
            (s, p) -> Path.of(s)
        );

    private static final StandardBundlerParam<String> FA_MAC_CFBUNDLETYPEROLE =
             new StandardBundlerParam<>(
                     Arguments.MAC_CFBUNDLETYPEROLE,
                     String.class,
                     params -> "Editor",
                     (s, p) -> s
     );

     private static final StandardBundlerParam<String> FA_MAC_LSHANDLERRANK =
             new StandardBundlerParam<>(
                     Arguments.MAC_LSHANDLERRANK,
                     String.class,
                     params -> "Owner",
                     (s, p) -> s
     );

     private static final StandardBundlerParam<String> FA_MAC_NSSTORETYPEKEY =
             new StandardBundlerParam<>(
                     Arguments.MAC_NSSTORETYPEKEY,
                     String.class,
                     params -> null,
                     (s, p) -> s
     );

     private static final StandardBundlerParam<String> FA_MAC_NSDOCUMENTCLASS =
             new StandardBundlerParam<>(
                     Arguments.MAC_NSDOCUMENTCLASS,
                     String.class,
                     params -> null,
                     (s, p) -> s
     );

     private static final StandardBundlerParam<String> FA_MAC_LSTYPEISPACKAGE =
             new StandardBundlerParam<>(
                     Arguments.MAC_LSTYPEISPACKAGE,
                     String.class,
                     params -> null,
                     (s, p) -> s
     );

     private static final StandardBundlerParam<String> FA_MAC_LSDOCINPLACE =
             new StandardBundlerParam<>(
                     Arguments.MAC_LSDOCINPLACE,
                     String.class,
                     params -> null,
                     (s, p) -> s
     );

     private static final StandardBundlerParam<String> FA_MAC_UIDOCBROWSER =
             new StandardBundlerParam<>(
                     Arguments.MAC_UIDOCBROWSER,
                     String.class,
                     params -> null,
                     (s, p) -> s
     );

     @SuppressWarnings("unchecked")
     private static final StandardBundlerParam<List<String>> FA_MAC_NSEXPORTABLETYPES =
             new StandardBundlerParam<>(
                     Arguments.MAC_NSEXPORTABLETYPES,
                     (Class<List<String>>) (Object) List.class,
                     params -> null,
                     (s, p) -> Arrays.asList(s.split("(,|\\s)+"))
             );

     @SuppressWarnings("unchecked")
     private static final StandardBundlerParam<List<String>> FA_MAC_UTTYPECONFORMSTO =
             new StandardBundlerParam<>(
                     Arguments.MAC_UTTYPECONFORMSTO,
                     (Class<List<String>>) (Object) List.class,
                     params -> Arrays.asList("public.data"),
                     (s, p) -> Arrays.asList(s.split("(,|\\s)+"))
             );

    public MacAppImageBuilder(Path imageOutDir) {
        super(imageOutDir);

        this.root = imageOutDir;
        this.contentsDir = root.resolve("Contents");
        this.resourcesDir = appLayout.destktopIntegrationDirectory();
        this.macOSDir = appLayout.launchersDirectory();
        this.runtimeDir = appLayout.runtimeDirectory();
        this.runtimeRoot = appLayout.runtimeHomeDirectory();
    }

    private void writeEntry(InputStream in, Path dstFile) throws IOException {
        Files.createDirectories(dstFile.getParent());
        Files.copy(in, dstFile);
    }

    @Override
    public void prepareApplicationFiles(Map<String, ? super Object> params)
            throws IOException {
        Files.createDirectories(macOSDir);

        Map<String, ? super Object> originalParams = new HashMap<>(params);
        // Generate PkgInfo
        Path pkgInfoFile = contentsDir.resolve("PkgInfo");
        Files.createFile(pkgInfoFile);
        writePkgInfo(pkgInfoFile);

        Path executable = macOSDir.resolve(getLauncherName(params));

        // create the main app launcher
        try (InputStream is_launcher =
                getResourceAsStream("jpackageapplauncher")) {
            // Copy executable and library to MacOS folder
            writeEntry(is_launcher, executable);
        }
        executable.toFile().setExecutable(true, false);
        // generate main app launcher config file
        writeCfgFile(params);

        // create additional app launcher(s) and config file(s)
        List<Map<String, ? super Object>> entryPoints =
                ADD_LAUNCHERS.fetchFrom(params);
        for (Map<String, ? super Object> entryPoint : entryPoints) {
            Map<String, ? super Object> tmp =
                    AddLauncherArguments.merge(originalParams, entryPoint);

            // add executable for add launcher
            Path addExecutable = macOSDir.resolve(getLauncherName(tmp));
            try (InputStream is = getResourceAsStream("jpackageapplauncher");) {
                writeEntry(is, addExecutable);
            }
            addExecutable.toFile().setExecutable(true, false);

            // add config file for add launcher
            writeCfgFile(tmp);
        }

        // Copy class path entries to Java folder
        copyApplication(params);

        /*********** Take care of "config" files *******/

        createResource(TEMPLATE_BUNDLE_ICON, params)
                .setCategory("icon")
                .setExternal(ICON_ICNS.fetchFrom(params))
                .saveToFile(resourcesDir.resolve(APP_NAME.fetchFrom(params)
                        + ".icns"));

        // copy file association icons
        for (Map<String, ?
                super Object> fa : FILE_ASSOCIATIONS.fetchFrom(params)) {
            Path f = FA_ICON.fetchFrom(fa);
            if (IOUtils.exists(f)) {
                IOUtils.copyFile(f, resourcesDir.resolve(f.getFileName()));

            }
        }

        copyRuntimeFiles(params);
        sign(params);
    }

    private void copyRuntimeFiles(Map<String, ? super Object> params)
            throws IOException {
        // Generate Info.plist
        writeInfoPlist(contentsDir.resolve("Info.plist"), params);

        // generate java runtime info.plist
        writeRuntimeInfoPlist(
                runtimeDir.resolve("Contents/Info.plist"), params);

        // copy library
        Path runtimeMacOSDir = Files.createDirectories(
                runtimeDir.resolve("Contents/MacOS"));

        final Path jliName = Path.of("libjli.dylib");
        try (Stream<Path> walk = Files.walk(runtimeRoot.resolve("lib"))) {
            final Path jli = walk
                    .filter(file -> file.getFileName().equals(jliName))
                    .findFirst()
                    .get();
            Files.copy(jli, runtimeMacOSDir.resolve(jliName));
        }
    }

    private void sign(Map<String, ? super Object> params) throws IOException {
        if (Optional.ofNullable(
                SIGN_BUNDLE.fetchFrom(params)).orElse(Boolean.TRUE)) {
            try {
                addNewKeychain(params);
            } catch (InterruptedException e) {
                Log.error(e.getMessage());
            }
            String signingIdentity =
                    DEVELOPER_ID_APP_SIGNING_KEY.fetchFrom(params);
            if (signingIdentity != null) {
                signAppBundle(params, root, signingIdentity,
                        BUNDLE_ID_SIGNING_PREFIX.fetchFrom(params),
                        ENTITLEMENTS.fetchFrom(params));
            }
            restoreKeychainList(params);
        }
    }

    private static String getLauncherName(Map<String, ? super Object> params) {
        return APP_NAME.fetchFrom(params);
    }

    private String getBundleName(Map<String, ? super Object> params) {
        if (MAC_CF_BUNDLE_NAME.fetchFrom(params) != null) {
            String bn = MAC_CF_BUNDLE_NAME.fetchFrom(params);
            if (bn.length() > 16) {
                Log.error(MessageFormat.format(I18N.getString(
                        "message.bundle-name-too-long-warning"),
                        MAC_CF_BUNDLE_NAME.getID(), bn));
            }
            return MAC_CF_BUNDLE_NAME.fetchFrom(params);
        } else if (APP_NAME.fetchFrom(params) != null) {
            return APP_NAME.fetchFrom(params);
        } else {
            String nm = MAIN_CLASS.fetchFrom(params);
            if (nm.length() > 16) {
                nm = nm.substring(0, 16);
            }
            return nm;
        }
    }

    private void writeRuntimeInfoPlist(Path file,
            Map<String, ? super Object> params) throws IOException {
        Map<String, String> data = new HashMap<>();
        String identifier = StandardBundlerParam.isRuntimeInstaller(params) ?
                MAC_CF_BUNDLE_IDENTIFIER.fetchFrom(params) :
                "com.oracle.java." + MAC_CF_BUNDLE_IDENTIFIER.fetchFrom(params);
        data.put("CF_BUNDLE_IDENTIFIER", identifier);
        String name = StandardBundlerParam.isRuntimeInstaller(params) ?
                getBundleName(params): "Java Runtime Image";
        data.put("CF_BUNDLE_NAME", name);
        String ver = VERSION.fetchFrom(params);
        String sver = ver;
        int index = ver.indexOf(".");
        if (index > 0 && ((index + 1) < ver.length())) {
            index = ver.indexOf(".", index + 1);
            if (index > 0 ) {
                sver = ver.substring(0, index);
            }
        }
        data.put("CF_BUNDLE_VERSION", ver);
        data.put("CF_BUNDLE_SHORT_VERSION_STRING", sver);

        createResource(TEMPLATE_RUNTIME_INFO_PLIST, params)
                .setPublicName("Runtime-Info.plist")
                .setCategory(I18N.getString("resource.runtime-info-plist"))
                .setSubstitutionData(data)
                .saveToFile(file);
    }

    private void writeStringArrayPlist(StringBuilder sb, String key,
            List<String> values) {
        if (values != null && !values.isEmpty()) {
            sb.append("  <key>").append(key).append("</key>\n").append("   <array>\n");
            values.forEach((value) -> {
                sb.append("    <string>").append(value).append("</string>\n");
            });
            sb.append("   </array>\n");
        }
    }

    private void writeStringPlist(StringBuilder sb, String key, String value) {
        if (value != null && !value.isEmpty()) {
            sb.append("  <key>").append(key).append("</key>\n").append("  <string>")
                    .append(value).append("</string>\n").append("\n");
         }
    }

    private void writeBoolPlist(StringBuilder sb, String key, String value) {
        if (value != null && !value.isEmpty()) {
            sb.append("  <key>").append(key).append("</key>\n").append("  <")
                    .append(value).append("/>\n").append("\n");
         }
    }

    private void writeInfoPlist(Path file, Map<String, ? super Object> params)
            throws IOException {
        Log.verbose(MessageFormat.format(I18N.getString(
                "message.preparing-info-plist"), file.toAbsolutePath()));

        //prepare config for exe
        //Note: do not need CFBundleDisplayName if we don't support localization
        Map<String, String> data = new HashMap<>();
        data.put("DEPLOY_ICON_FILE", APP_NAME.fetchFrom(params) + ".icns");
        data.put("DEPLOY_BUNDLE_IDENTIFIER",
                MAC_CF_BUNDLE_IDENTIFIER.fetchFrom(params));
        data.put("DEPLOY_BUNDLE_NAME",
                getBundleName(params));
        data.put("DEPLOY_BUNDLE_COPYRIGHT", COPYRIGHT.fetchFrom(params));
        data.put("DEPLOY_LAUNCHER_NAME", getLauncherName(params));
        data.put("DEPLOY_BUNDLE_SHORT_VERSION", VERSION.fetchFrom(params));
        data.put("DEPLOY_BUNDLE_CFBUNDLE_VERSION", VERSION.fetchFrom(params));
        data.put("DEPLOY_APP_CATEGORY", "public.app-category." +
                APP_CATEGORY.fetchFrom(params));

        StringBuilder bundleDocumentTypes = new StringBuilder();
        StringBuilder exportedTypes = new StringBuilder();
        for (Map<String, ? super Object>
                fileAssociation : FILE_ASSOCIATIONS.fetchFrom(params)) {

            List<String> extensions = FA_EXTENSIONS.fetchFrom(fileAssociation);
            if (extensions == null) {
                Log.verbose(I18N.getString(
                        "message.creating-association-with-null-extension"));
            }

            String itemContentType = MAC_CF_BUNDLE_IDENTIFIER.fetchFrom(params)
                    + "." + ((extensions == null || extensions.isEmpty())
                    ? "mime" : extensions.get(0));
            String description = FA_DESCRIPTION.fetchFrom(fileAssociation);
            Path icon = FA_ICON.fetchFrom(fileAssociation);

            bundleDocumentTypes.append(" <dict>\n");
            writeStringArrayPlist(bundleDocumentTypes, "LSItemContentTypes",
                    Arrays.asList(itemContentType));
            writeStringPlist(bundleDocumentTypes, "CFBundleTypeName", description);
            writeStringPlist(bundleDocumentTypes, "LSHandlerRank",
                    FA_MAC_LSHANDLERRANK.fetchFrom(fileAssociation));
            writeStringPlist(bundleDocumentTypes, "CFBundleTypeRole",
                    FA_MAC_CFBUNDLETYPEROLE.fetchFrom(fileAssociation));
            writeStringPlist(bundleDocumentTypes, "NSPersistentStoreTypeKey",
                    FA_MAC_NSSTORETYPEKEY.fetchFrom(fileAssociation));
            writeStringPlist(bundleDocumentTypes, "NSDocumentClass",
                    FA_MAC_NSDOCUMENTCLASS.fetchFrom(fileAssociation));
            writeBoolPlist(bundleDocumentTypes, "LSIsAppleDefaultForType",
                    "true");
            writeBoolPlist(bundleDocumentTypes, "LSTypeIsPackage",
                    FA_MAC_LSTYPEISPACKAGE.fetchFrom(fileAssociation));
            writeBoolPlist(bundleDocumentTypes, "LSSupportsOpeningDocumentsInPlace",
                    FA_MAC_LSDOCINPLACE.fetchFrom(fileAssociation));
            writeBoolPlist(bundleDocumentTypes, "UISupportsDocumentBrowser",
                    FA_MAC_UIDOCBROWSER.fetchFrom(fileAssociation));
            if (IOUtils.exists(icon)) {
                writeStringPlist(bundleDocumentTypes, "CFBundleTypeIconFile",
                        icon.getFileName().toString());
            }
            bundleDocumentTypes.append("  </dict>\n");

            exportedTypes.append("  <dict>\n");
            writeStringPlist(exportedTypes, "UTTypeIdentifier",
                    itemContentType);
            writeStringPlist(exportedTypes, "UTTypeDescription",
                    description);
            writeStringArrayPlist(exportedTypes, "UTTypeConformsTo",
                    FA_MAC_UTTYPECONFORMSTO.fetchFrom(fileAssociation));

            if (IOUtils.exists(icon)) {
                writeStringPlist(exportedTypes, "UTTypeIconFile",
                        icon.getFileName().toString());
            }
            exportedTypes.append("\n")
                    .append("  <key>UTTypeTagSpecification</key>\n")
                    .append("  <dict>\n")
                    .append("\n");
            writeStringArrayPlist(exportedTypes, "public.filename-extension",
                    extensions);
            writeStringArrayPlist(exportedTypes, "public.mime-type",
                    FA_CONTENT_TYPE.fetchFrom(fileAssociation));
            writeStringArrayPlist(exportedTypes, "NSExportableTypes",
                    FA_MAC_NSEXPORTABLETYPES.fetchFrom(fileAssociation));
            exportedTypes.append("  </dict>\n").append(" </dict>\n");
        }
        String associationData;
        if (bundleDocumentTypes.length() > 0) {
            associationData =
                    "\n  <key>CFBundleDocumentTypes</key>\n  <array>\n"
                    + bundleDocumentTypes.toString()
                    + "  </array>\n\n"
                    + "  <key>UTExportedTypeDeclarations</key>\n  <array>\n"
                    + exportedTypes.toString()
                    + "  </array>\n";
        } else {
            associationData = "";
        }
        data.put("DEPLOY_FILE_ASSOCIATIONS", associationData);

        createResource(TEMPLATE_INFO_PLIST_LITE, params)
                .setCategory(I18N.getString("resource.app-info-plist"))
                .setSubstitutionData(data)
                .setPublicName("Info.plist")
                .saveToFile(file);
    }

    private void writePkgInfo(Path file) throws IOException {
        //hardcoded as it does not seem we need to change it ever
        String signature = "????";

        try (Writer out = Files.newBufferedWriter(file)) {
            out.write(OS_TYPE_CODE + signature);
            out.flush();
        }
    }

    public static void addNewKeychain(Map<String, ? super Object> params)
                                    throws IOException, InterruptedException {
        if (Platform.getMajorVersion() < 10 ||
                (Platform.getMajorVersion() == 10 &&
                Platform.getMinorVersion() < 12)) {
            // we need this for OS X 10.12+
            return;
        }

        String keyChain = SIGNING_KEYCHAIN.fetchFrom(params);
        if (keyChain == null || keyChain.isEmpty()) {
            return;
        }

        // get current keychain list
        String keyChainPath = Path.of(keyChain).toAbsolutePath().toString();
        List<String> keychainList = new ArrayList<>();
        int ret = IOUtils.getProcessOutput(
                keychainList, "security", "list-keychains");
        if (ret != 0) {
            Log.error(I18N.getString("message.keychain.error"));
            return;
        }

        boolean contains = keychainList.stream().anyMatch(
                    str -> str.trim().equals("\""+keyChainPath.trim()+"\""));
        if (contains) {
            // keychain is already added in the search list
            return;
        }

        keyChains = new ArrayList<>();
        // remove "
        keychainList.forEach((String s) -> {
            String path = s.trim();
            if (path.startsWith("\"") && path.endsWith("\"")) {
                path = path.substring(1, path.length()-1);
            }
            keyChains.add(path);
        });

        List<String> args = new ArrayList<>();
        args.add("/usr/bin/security");
        args.add("list-keychains");
        args.add("-s");

        args.addAll(keyChains);
        args.add(keyChain);

        ProcessBuilder  pb = new ProcessBuilder(args);
        IOUtils.exec(pb);
    }

    public static void restoreKeychainList(Map<String, ? super Object> params)
            throws IOException{
        if (Platform.getMajorVersion() < 10 ||
                (Platform.getMajorVersion() == 10 &&
                Platform.getMinorVersion() < 12)) {
            // we need this for OS X 10.12+
            return;
        }

        if (keyChains == null || keyChains.isEmpty()) {
            return;
        }

        List<String> args = new ArrayList<>();
        args.add("/usr/bin/security");
        args.add("list-keychains");
        args.add("-s");

        args.addAll(keyChains);

        ProcessBuilder  pb = new ProcessBuilder(args);
        IOUtils.exec(pb);
    }

    static void signAppBundle(
            Map<String, ? super Object> params, Path appLocation,
            String signingIdentity, String identifierPrefix, Path entitlements)
            throws IOException {
        AtomicReference<IOException> toThrow = new AtomicReference<>();
        String appExecutable = "/Contents/MacOS/" + APP_NAME.fetchFrom(params);
        String keyChain = SIGNING_KEYCHAIN.fetchFrom(params);

        // sign all dylibs and executables
        try (Stream<Path> stream = Files.walk(appLocation)) {
            stream.peek(path -> { // fix permissions
                try {
                    Set<PosixFilePermission> pfp =
                            Files.getPosixFilePermissions(path);
                    if (!pfp.contains(PosixFilePermission.OWNER_WRITE)) {
                        pfp = EnumSet.copyOf(pfp);
                        pfp.add(PosixFilePermission.OWNER_WRITE);
                        Files.setPosixFilePermissions(path, pfp);
                    }
                } catch (IOException e) {
                    Log.verbose(e);
                }
            }).filter(p -> Files.isRegularFile(p) &&
                      (Files.isExecutable(p) || p.toString().endsWith(".dylib"))
                      && !(p.toString().contains("dylib.dSYM/Contents"))
                      && !(p.toString().endsWith(appExecutable))
                     ).forEach(p -> {
                // noinspection ThrowableResultOfMethodCallIgnored
                if (toThrow.get() != null) return;

                // If p is a symlink then skip the signing process.
                if (Files.isSymbolicLink(p)) {
                    Log.verbose(MessageFormat.format(I18N.getString(
                            "message.ignoring.symlink"), p.toString()));
                } else {
                    List<String> args;
                    // runtime and Framework files will be signed below
                    // but they need to be unsigned first here
                    if ((p.toString().contains("/Contents/runtime")) ||
                        (p.toString().contains("/Contents/Frameworks"))) {

                        args = new ArrayList<>();
                        args.addAll(Arrays.asList("/usr/bin/codesign",
                                "--remove-signature", p.toString()));
                        try {
                            Set<PosixFilePermission> oldPermissions =
                                    Files.getPosixFilePermissions(p);
                            p.toFile().setWritable(true, true);
                            ProcessBuilder pb = new ProcessBuilder(args);
                            IOUtils.exec(pb);
                            Files.setPosixFilePermissions(p,oldPermissions);
                        } catch (IOException ioe) {
                            Log.verbose(ioe);
                            toThrow.set(ioe);
                            return;
                        }
                    }
                    args = new ArrayList<>();
                    args.addAll(Arrays.asList("/usr/bin/codesign",
                            "--timestamp",
                            "--options", "runtime",
                            "-s", signingIdentity,
                            "--prefix", identifierPrefix,
                            "-vvvv"));
                    if (keyChain != null && !keyChain.isEmpty()) {
                        args.add("--keychain");
                        args.add(keyChain);
                    }
                    if (Files.isExecutable(p)) {
                        if (entitlements != null) {
                            args.add("--entitlements");
                            args.add(entitlements.toString());
                        }
                    }
                    args.add(p.toString());
                    try {
                        Set<PosixFilePermission> oldPermissions =
                                Files.getPosixFilePermissions(p);
                        p.toFile().setWritable(true, true);
                        ProcessBuilder pb = new ProcessBuilder(args);
                        IOUtils.exec(pb);
                        Files.setPosixFilePermissions(p, oldPermissions);
                    } catch (IOException ioe) {
                        toThrow.set(ioe);
                    }
                }
            });
        }
        IOException ioe = toThrow.get();
        if (ioe != null) {
            throw ioe;
        }

        // sign all runtime and frameworks
        Consumer<? super Path> signIdentifiedByPList = path -> {
            //noinspection ThrowableResultOfMethodCallIgnored
            if (toThrow.get() != null) return;

            try {
                List<String> args = new ArrayList<>();
                args.addAll(Arrays.asList("/usr/bin/codesign",
                        "--timestamp",
                        "--options", "runtime",
                        "--force",
                        "-s", signingIdentity, // sign with this key
                        "--prefix", identifierPrefix,
                        // use the identifier as a prefix
                        "-vvvv"));

                if (keyChain != null && !keyChain.isEmpty()) {
                    args.add("--keychain");
                    args.add(keyChain);
                }
                if (entitlements != null) {
                    args.add("--entitlements");
                    args.add(entitlements.toString());
                }
                args.add(path.toString());
                ProcessBuilder pb = new ProcessBuilder(args);

                IOUtils.exec(pb);
            } catch (IOException e) {
                toThrow.set(e);
            }
        };

        Path javaPath = appLocation.resolve("Contents/runtime");
        if (Files.isDirectory(javaPath)) {
            signIdentifiedByPList.accept(javaPath);

            ioe = toThrow.get();
            if (ioe != null) {
                throw ioe;
            }
        }
        Path frameworkPath = appLocation.resolve("Contents/Frameworks");
        if (Files.isDirectory(frameworkPath)) {
            try (var fileList = Files.list(frameworkPath)) {
                fileList.forEach(signIdentifiedByPList);
            }

            ioe = toThrow.get();
            if (ioe != null) {
                throw ioe;
            }
        }

        // sign the app itself
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("/usr/bin/codesign",
                "--timestamp",
                "--options", "runtime",
                "--force",
                "-s", signingIdentity,
                "--prefix", identifierPrefix,
                "-vvvv"));

        if (keyChain != null && !keyChain.isEmpty()) {
            args.add("--keychain");
            args.add(keyChain);
        }

        if (entitlements != null) {
            args.add("--entitlements");
            args.add(entitlements.toString());
        }

        args.add(appLocation.toString());

        ProcessBuilder pb =
                new ProcessBuilder(args.toArray(new String[args.size()]));

        IOUtils.exec(pb);
    }

    private static String extractBundleIdentifier(Map<String, Object> params) {
        if (PREDEFINED_APP_IMAGE.fetchFrom(params) == null) {
            return null;
        }

        try {
            Path infoPList = PREDEFINED_APP_IMAGE.fetchFrom(params).resolve("Contents").
                    resolve("Info.plist");

            DocumentBuilderFactory dbf
                    = DocumentBuilderFactory.newDefaultInstance();
            dbf.setFeature("http://apache.org/xml/features/" +
                           "nonvalidating/load-external-dtd", false);
            DocumentBuilder b = dbf.newDocumentBuilder();
            org.w3c.dom.Document doc = b.parse(Files.newInputStream(infoPList));

            XPath xPath = XPathFactory.newInstance().newXPath();
            // Query for the value of <string> element preceding <key>
            // element with value equal to CFBundleIdentifier
            String v = (String) xPath.evaluate(
                    "//string[preceding-sibling::key = \"CFBundleIdentifier\"][1]",
                    doc, XPathConstants.STRING);

            if (v != null && !v.isEmpty()) {
                return v;
            }
        } catch (Exception ex) {
            Log.verbose(ex);
        }

        return null;
    }
}
