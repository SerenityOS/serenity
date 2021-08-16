/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static jdk.jpackage.internal.OverridableResource.createResource;
import static jdk.jpackage.internal.StandardBundlerParam.ABOUT_URL;
import static jdk.jpackage.internal.StandardBundlerParam.INSTALLER_NAME;
import static jdk.jpackage.internal.StandardBundlerParam.VERSION;
import static jdk.jpackage.internal.StandardBundlerParam.RELEASE;
import static jdk.jpackage.internal.StandardBundlerParam.VENDOR;
import static jdk.jpackage.internal.StandardBundlerParam.LICENSE_FILE;
import static jdk.jpackage.internal.StandardBundlerParam.COPYRIGHT;

public class LinuxDebBundler extends LinuxPackageBundler {

    // Debian rules for package naming are used here
    // https://www.debian.org/doc/debian-policy/ch-controlfields.html#s-f-Source
    //
    // Package names must consist only of lower case letters (a-z),
    // digits (0-9), plus (+) and minus (-) signs, and periods (.).
    // They must be at least two characters long and
    // must start with an alphanumeric character.
    //
    private static final Pattern DEB_PACKAGE_NAME_PATTERN =
            Pattern.compile("^[a-z][a-z\\d\\+\\-\\.]+");

    private static final BundlerParamInfo<String> PACKAGE_NAME =
            new StandardBundlerParam<> (
            Arguments.CLIOptions.LINUX_BUNDLE_NAME.getId(),
            String.class,
            params -> {
                String nm = INSTALLER_NAME.fetchFrom(params);
                if (nm == null) return null;

                // make sure to lower case and spaces/underscores become dashes
                nm = nm.toLowerCase().replaceAll("[ _]", "-");
                return nm;
            },
            (s, p) -> {
                if (!DEB_PACKAGE_NAME_PATTERN.matcher(s).matches()) {
                    throw new IllegalArgumentException(new ConfigException(
                            MessageFormat.format(I18N.getString(
                            "error.invalid-value-for-package-name"), s),
                            I18N.getString(
                            "error.invalid-value-for-package-name.advice")));
                }

                return s;
            });

    private static final String TOOL_DPKG_DEB = "dpkg-deb";
    private static final String TOOL_DPKG = "dpkg";
    private static final String TOOL_FAKEROOT = "fakeroot";

    private static final String DEB_ARCH;
    static {
        String debArch;
        try {
            debArch = Executor.of(TOOL_DPKG, "--print-architecture").saveOutput(
                    true).executeExpectSuccess().getOutput().get(0);
        } catch (IOException ex) {
            debArch = null;
        }
        DEB_ARCH = debArch;
    }

    private static final BundlerParamInfo<String> FULL_PACKAGE_NAME =
            new StandardBundlerParam<>(
                    "linux.deb.fullPackageName", String.class, params -> {
                        return PACKAGE_NAME.fetchFrom(params)
                            + "_" + VERSION.fetchFrom(params)
                            + "-" + RELEASE.fetchFrom(params)
                            + "_" + DEB_ARCH;
                    }, (s, p) -> s);

    private static final BundlerParamInfo<String> EMAIL =
            new StandardBundlerParam<> (
            Arguments.CLIOptions.LINUX_DEB_MAINTAINER.getId(),
            String.class,
            params -> "Unknown",
            (s, p) -> s);

    private static final BundlerParamInfo<String> MAINTAINER =
            new StandardBundlerParam<> (
            Arguments.CLIOptions.LINUX_DEB_MAINTAINER.getId() + ".internal",
            String.class,
            params -> VENDOR.fetchFrom(params) + " <"
                    + EMAIL.fetchFrom(params) + ">",
            (s, p) -> s);

    private static final BundlerParamInfo<String> SECTION =
            new StandardBundlerParam<>(
            Arguments.CLIOptions.LINUX_CATEGORY.getId(),
            String.class,
            params -> "misc",
            (s, p) -> s);

    private static final BundlerParamInfo<String> LICENSE_TEXT =
            new StandardBundlerParam<> (
            "linux.deb.licenseText",
            String.class,
            params -> {
                try {
                    String licenseFile = LICENSE_FILE.fetchFrom(params);
                    if (licenseFile != null) {
                        return Files.readString(Path.of(licenseFile));
                    }
                } catch (IOException e) {
                    Log.verbose(e);
                }
                return "Unknown";
            },
            (s, p) -> s);

    public LinuxDebBundler() {
        super(PACKAGE_NAME);
    }

    @Override
    public void doValidate(Map<String, ? super Object> params)
            throws ConfigException {

        // Show warning if license file is missing
        if (LICENSE_FILE.fetchFrom(params) == null) {
            Log.verbose(I18N.getString("message.debs-like-licenses"));
        }
    }

    @Override
    protected List<ToolValidator> getToolValidators(
            Map<String, ? super Object> params) {
        return Stream.of(TOOL_DPKG_DEB, TOOL_DPKG, TOOL_FAKEROOT).map(
                ToolValidator::new).toList();
    }

    @Override
    protected Path buildPackageBundle(
            Map<String, String> replacementData,
            Map<String, ? super Object> params, Path outputParentDir) throws
            PackagerException, IOException {

        prepareProjectConfig(replacementData, params);
        adjustPermissionsRecursive(createMetaPackage(params).sourceRoot());
        return buildDeb(params, outputParentDir);
    }

    private static final Pattern PACKAGE_NAME_REGEX = Pattern.compile("^(^\\S+):");

    @Override
    protected void initLibProvidersLookup(
            Map<String, ? super Object> params,
            LibProvidersLookup libProvidersLookup) {

        //
        // `dpkg -S` command does glob pattern lookup. If not the absolute path
        // to the file is specified it might return mltiple package names.
        // Even for full paths multiple package names can be returned as
        // it is OK for multiple packages to provide the same file. `/opt`
        // directory is such an example. So we have to deal with multiple
        // packages per file situation.
        //
        // E.g.: `dpkg -S libc.so.6` command reports three packages:
        // libc6-x32: /libx32/libc.so.6
        // libc6:amd64: /lib/x86_64-linux-gnu/libc.so.6
        // libc6-i386: /lib32/libc.so.6
        // `:amd64` is architecture suffix and can (should) be dropped.
        // Still need to decide what package to choose from three.
        // libc6-x32 and libc6-i386 both depend on libc6:
        // $ dpkg -s libc6-x32
        // Package: libc6-x32
        // Status: install ok installed
        // Priority: optional
        // Section: libs
        // Installed-Size: 10840
        // Maintainer: Ubuntu Developers <ubuntu-devel-discuss@lists.ubuntu.com>
        // Architecture: amd64
        // Source: glibc
        // Version: 2.23-0ubuntu10
        // Depends: libc6 (= 2.23-0ubuntu10)
        //
        // We can dive into tracking dependencies, but this would be overly
        // complicated.
        //
        // For simplicity lets consider the following rules:
        // 1. If there is one item in `dpkg -S` output, accept it.
        // 2. If there are multiple items in `dpkg -S` output and there is at
        //  least one item with the default arch suffix (DEB_ARCH),
        //  accept only these items.
        // 3. If there are multiple items in `dpkg -S` output and there are
        //  no with the default arch suffix (DEB_ARCH), accept all items.
        // So lets use this heuristics: don't accept packages for whom
        //  `dpkg -p` command fails.
        // 4. Arch suffix should be stripped from accepted package names.
        //

        libProvidersLookup.setPackageLookup(file -> {
            Set<String> archPackages = new HashSet<>();
            Set<String> otherPackages = new HashSet<>();

            Executor.of(TOOL_DPKG, "-S", file.toString())
                    .saveOutput(true).executeExpectSuccess()
                    .getOutput().forEach(line -> {
                        Matcher matcher = PACKAGE_NAME_REGEX.matcher(line);
                        if (matcher.find()) {
                            String name = matcher.group(1);
                            if (name.endsWith(":" + DEB_ARCH)) {
                                // Strip arch suffix
                                name = name.substring(0,
                                        name.length() - (DEB_ARCH.length() + 1));
                                archPackages.add(name);
                            } else {
                                otherPackages.add(name);
                            }
                        }
                    });

            if (!archPackages.isEmpty()) {
                return archPackages.stream();
            }
            return otherPackages.stream();
        });
    }

    @Override
    protected List<ConfigException> verifyOutputBundle(
            Map<String, ? super Object> params, Path packageBundle) {
        List<ConfigException> errors = new ArrayList<>();

        String controlFileName = "control";

        List<PackageProperty> properties = List.of(
                new PackageProperty("Package", PACKAGE_NAME.fetchFrom(params),
                        "APPLICATION_PACKAGE", controlFileName),
                new PackageProperty("Version", String.format("%s-%s",
                        VERSION.fetchFrom(params), RELEASE.fetchFrom(params)),
                        "APPLICATION_VERSION-APPLICATION_RELEASE",
                        controlFileName),
                new PackageProperty("Architecture", DEB_ARCH, "APPLICATION_ARCH",
                        controlFileName));

        List<String> cmdline = new ArrayList<>(List.of(TOOL_DPKG_DEB, "-f",
                packageBundle.toString()));
        properties.forEach(property -> cmdline.add(property.name));
        try {
            Map<String, String> actualValues = Executor.of(cmdline.toArray(String[]::new))
                    .saveOutput(true)
                    .executeExpectSuccess()
                    .getOutput().stream()
                            .map(line -> line.split(":\\s+", 2))
                            .collect(Collectors.toMap(
                                    components -> components[0],
                                    components -> components[1]));
            properties.forEach(property -> errors.add(property.verifyValue(
                    actualValues.get(property.name))));
        } catch (IOException ex) {
            // Ignore error as it is not critical. Just report it.
            Log.verbose(ex);
        }

        return errors;
    }

    /*
     * set permissions with a string like "rwxr-xr-x"
     *
     * This cannot be directly backport to 22u which is built with 1.6
     */
    private void setPermissions(Path file, String permissions) {
        Set<PosixFilePermission> filePermissions =
                PosixFilePermissions.fromString(permissions);
        try {
            if (Files.exists(file)) {
                Files.setPosixFilePermissions(file, filePermissions);
            }
        } catch (IOException ex) {
            Log.error(ex.getMessage());
            Log.verbose(ex);
        }

    }

    public static boolean isDebian() {
        // we are just going to run "dpkg -s coreutils" and assume Debian
        // or deritive if no error is returned.
        try {
            Executor.of(TOOL_DPKG, "-s", "coreutils").executeExpectSuccess();
            return true;
        } catch (IOException e) {
            // just fall thru
        }
        return false;
    }

    private void adjustPermissionsRecursive(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs)
                    throws IOException {
                if (file.endsWith(".so") || !Files.isExecutable(file)) {
                    setPermissions(file, "rw-r--r--");
                } else if (Files.isExecutable(file)) {
                    setPermissions(file, "rwxr-xr-x");
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e)
                    throws IOException {
                if (e == null) {
                    setPermissions(dir, "rwxr-xr-x");
                    return FileVisitResult.CONTINUE;
                } else {
                    // directory iteration failed
                    throw e;
                }
            }
        });
    }

    private class DebianFile {

        DebianFile(Path dstFilePath, String comment) {
            this.dstFilePath = dstFilePath;
            this.comment = comment;
        }

        DebianFile setExecutable() {
            permissions = "rwxr-xr-x";
            return this;
        }

        void create(Map<String, String> data, Map<String, ? super Object> params)
                throws IOException {
            createResource("template." + dstFilePath.getFileName().toString(),
                    params)
                    .setCategory(I18N.getString(comment))
                    .setSubstitutionData(data)
                    .saveToFile(dstFilePath);
            if (permissions != null) {
                setPermissions(dstFilePath, permissions);
            }
        }

        private final Path dstFilePath;
        private final String comment;
        private String permissions;
    }

    private void prepareProjectConfig(Map<String, String> data,
            Map<String, ? super Object> params) throws IOException {

        Path configDir = createMetaPackage(params).sourceRoot().resolve("DEBIAN");
        List<DebianFile> debianFiles = new ArrayList<>();
        debianFiles.add(new DebianFile(
                configDir.resolve("control"),
                "resource.deb-control-file"));
        debianFiles.add(new DebianFile(
                configDir.resolve("preinst"),
                "resource.deb-preinstall-script").setExecutable());
        debianFiles.add(new DebianFile(
                configDir.resolve("prerm"),
                "resource.deb-prerm-script").setExecutable());
        debianFiles.add(new DebianFile(
                configDir.resolve("postinst"),
                "resource.deb-postinstall-script").setExecutable());
        debianFiles.add(new DebianFile(
                configDir.resolve("postrm"),
                "resource.deb-postrm-script").setExecutable());

        final String installDir = LINUX_INSTALL_DIR.fetchFrom(params);

        if (!StandardBundlerParam.isRuntimeInstaller(params)
                || (isInstallDirInUsrTree(installDir) || installDir.startsWith("/usr/"))) {
            debianFiles.add(new DebianFile(
                    getConfig_CopyrightFile(params),
                    "resource.copyright-file"));
        }

        for (DebianFile debianFile : debianFiles) {
            debianFile.create(data, params);
        }
    }

    @Override
    protected Map<String, String> createReplacementData(
            Map<String, ? super Object> params) throws IOException {
        Map<String, String> data = new HashMap<>();

        data.put("APPLICATION_MAINTAINER", MAINTAINER.fetchFrom(params));
        data.put("APPLICATION_SECTION", SECTION.fetchFrom(params));
        data.put("APPLICATION_COPYRIGHT", COPYRIGHT.fetchFrom(params));
        data.put("APPLICATION_LICENSE_TEXT", LICENSE_TEXT.fetchFrom(params));
        data.put("APPLICATION_ARCH", DEB_ARCH);
        data.put("APPLICATION_INSTALLED_SIZE", Long.toString(
                createMetaPackage(params).sourceApplicationLayout().sizeInBytes() >> 10));
        data.put("APPLICATION_HOMEPAGE", Optional.ofNullable(
                ABOUT_URL.fetchFrom(params)).map(value -> "Homepage: " + value).orElse(
                ""));

        return data;
    }

    private Path getConfig_CopyrightFile(Map<String, ? super Object> params) {
        final String installDir = LINUX_INSTALL_DIR.fetchFrom(params);
        final String packageName = PACKAGE_NAME.fetchFrom(params);

        final Path installPath;
        if (isInstallDirInUsrTree(installDir) || installDir.startsWith("/usr/")) {
            installPath = Path.of("/usr/share/doc/", packageName, "copyright");
        } else {
            installPath = Path.of(installDir, packageName, "share/doc/copyright");
        }

        return createMetaPackage(params).sourceRoot().resolve(
                Path.of("/").relativize(installPath));
    }

    private Path buildDeb(Map<String, ? super Object> params,
            Path outdir) throws IOException {
        Path outFile = outdir.resolve(
                FULL_PACKAGE_NAME.fetchFrom(params)+".deb");
        Log.verbose(MessageFormat.format(I18N.getString(
                "message.outputting-to-location"), outFile.toAbsolutePath().toString()));

        PlatformPackage thePackage = createMetaPackage(params);

        List<String> cmdline = new ArrayList<>();
        cmdline.addAll(List.of(TOOL_FAKEROOT, TOOL_DPKG_DEB));
        if (Log.isVerbose()) {
            cmdline.add("--verbose");
        }
        cmdline.addAll(List.of("-b", thePackage.sourceRoot().toString(),
                outFile.toAbsolutePath().toString()));

        // run dpkg
        RetryExecutor.retryOnKnownErrorMessage(
                "semop(1): encountered an error: Invalid argument").execute(
                        cmdline.toArray(String[]::new));

        Log.verbose(MessageFormat.format(I18N.getString(
                "message.output-to-location"), outFile.toAbsolutePath().toString()));

        return outFile;
    }

    @Override
    public String getName() {
        return I18N.getString("deb.bundler.name");
    }

    @Override
    public String getID() {
        return "deb";
    }

    @Override
    public boolean supported(boolean runtimeInstaller) {
        return Platform.isLinux() && (new ToolValidator(TOOL_DPKG_DEB).validate() == null);
    }

    @Override
    public boolean isDefault() {
        return isDebian();
    }
}
