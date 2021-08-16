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
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static jdk.jpackage.internal.StandardBundlerParam.APP_NAME;
import static jdk.jpackage.internal.StandardBundlerParam.INSTALLER_NAME;
import static jdk.jpackage.internal.StandardBundlerParam.LICENSE_FILE;
import static jdk.jpackage.internal.StandardBundlerParam.VERSION;
import static jdk.jpackage.internal.StandardBundlerParam.RELEASE;
import static jdk.jpackage.internal.StandardBundlerParam.TEMP_ROOT;
import static jdk.jpackage.internal.OverridableResource.createResource;
import static jdk.jpackage.internal.StandardBundlerParam.ABOUT_URL;

/**
 * There are two command line options to configure license information for RPM
 * packaging: --linux-rpm-license-type and --license-file. Value of
 * --linux-rpm-license-type command line option configures "License:" section
 * of RPM spec. Value of --license-file command line option specifies a license
 * file to be added to the package. License file is a sort of documentation file
 * but it will be installed even if user selects an option to install the
 * package without documentation. --linux-rpm-license-type is the primary option
 * to set license information. --license-file makes little sense in case of RPM
 * packaging.
 */
public class LinuxRpmBundler extends LinuxPackageBundler {

    // Fedora rules for package naming are used here
    // https://fedoraproject.org/wiki/Packaging:NamingGuidelines?rd=Packaging/NamingGuidelines
    //
    // all Fedora packages must be named using only the following ASCII
    // characters. These characters are displayed here:
    //
    // abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._+
    //
    private static final Pattern RPM_PACKAGE_NAME_PATTERN =
            Pattern.compile("[a-z\\d\\+\\-\\.\\_]+", Pattern.CASE_INSENSITIVE);

    public static final BundlerParamInfo<String> PACKAGE_NAME =
            new StandardBundlerParam<> (
            Arguments.CLIOptions.LINUX_BUNDLE_NAME.getId(),
            String.class,
            params -> {
                String nm = INSTALLER_NAME.fetchFrom(params);
                if (nm == null) return null;

                // make sure to lower case and spaces become dashes
                nm = nm.toLowerCase().replaceAll("[ ]", "-");

                return nm;
            },
            (s, p) -> {
                if (!RPM_PACKAGE_NAME_PATTERN.matcher(s).matches()) {
                    String msgKey = "error.invalid-value-for-package-name";
                    throw new IllegalArgumentException(
                            new ConfigException(MessageFormat.format(
                                    I18N.getString(msgKey), s),
                                    I18N.getString(msgKey + ".advice")));
                }

                return s;
            }
        );

    public static final BundlerParamInfo<String> LICENSE_TYPE =
        new StandardBundlerParam<>(
                Arguments.CLIOptions.LINUX_RPM_LICENSE_TYPE.getId(),
                String.class,
                params -> I18N.getString("param.license-type.default"),
                (s, p) -> s
        );

    public static final BundlerParamInfo<String> GROUP =
            new StandardBundlerParam<>(
            Arguments.CLIOptions.LINUX_CATEGORY.getId(),
            String.class,
            params -> null,
            (s, p) -> s);

    private static final String DEFAULT_SPEC_TEMPLATE = "template.spec";

    public static final String TOOL_RPM = "rpm";
    public static final String TOOL_RPMBUILD = "rpmbuild";
    public static final DottedVersion TOOL_RPMBUILD_MIN_VERSION = DottedVersion.lazy(
            "4.10");

    public LinuxRpmBundler() {
        super(PACKAGE_NAME);
    }

    @Override
    public void doValidate(Map<String, ? super Object> params)
            throws ConfigException {
    }

    private static ToolValidator createRpmbuildToolValidator() {
        Pattern pattern = Pattern.compile(" (\\d+\\.\\d+)");
        return new ToolValidator(TOOL_RPMBUILD).setMinimalVersion(
                TOOL_RPMBUILD_MIN_VERSION).setVersionParser(lines -> {
                    String versionString = lines.limit(1).collect(
                            Collectors.toList()).get(0);
                    Matcher matcher = pattern.matcher(versionString);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                    return null;
                });
    }

    @Override
    protected List<ToolValidator> getToolValidators(
            Map<String, ? super Object> params) {
        return List.of(createRpmbuildToolValidator());
    }

    @Override
    protected Path buildPackageBundle(
            Map<String, String> replacementData,
            Map<String, ? super Object> params, Path outputParentDir) throws
            PackagerException, IOException {

        Path specFile = specFile(params);

        // prepare spec file
        createResource(DEFAULT_SPEC_TEMPLATE, params)
                .setCategory(I18N.getString("resource.rpm-spec-file"))
                .setSubstitutionData(replacementData)
                .saveToFile(specFile);

        return buildRPM(params, outputParentDir);
    }

    @Override
    protected Map<String, String> createReplacementData(
            Map<String, ? super Object> params) throws IOException {
        Map<String, String> data = new HashMap<>();

        final Path prefix = Path.of(LINUX_INSTALL_DIR.fetchFrom(params));

        Path appDirectory = prefix;
        if (!isInstallDirInUsrTree(prefix.toString())) {
            appDirectory = appDirectory.resolve(PACKAGE_NAME.fetchFrom(params));
        }

        data.put("APPLICATION_PREFIX", prefix.toString());
        data.put("APPLICATION_DIRECTORY", appDirectory.toString());
        data.put("APPLICATION_SUMMARY", APP_NAME.fetchFrom(params));
        data.put("APPLICATION_LICENSE_TYPE", LICENSE_TYPE.fetchFrom(params));

        String licenseFile = LICENSE_FILE.fetchFrom(params);
        if (licenseFile != null) {
            licenseFile = Path.of(licenseFile).toAbsolutePath().normalize().toString();
        }
        data.put("APPLICATION_LICENSE_FILE", licenseFile);
        data.put("APPLICATION_GROUP", GROUP.fetchFrom(params));

        data.put("APPLICATION_URL", Optional.ofNullable(ABOUT_URL.fetchFrom(
                params)).orElse(""));

        return data;
    }

    @Override
    protected void initLibProvidersLookup(
            Map<String, ? super Object> params,
            LibProvidersLookup libProvidersLookup) {
        libProvidersLookup.setPackageLookup(file -> {
            return Executor.of(TOOL_RPM,
                "-q", "--queryformat", "%{name}\\n",
                "-q", "--whatprovides", file.toString())
                .saveOutput(true).executeExpectSuccess().getOutput().stream();
        });
    }

    @Override
    protected List<ConfigException> verifyOutputBundle(
            Map<String, ? super Object> params, Path packageBundle) {
        List<ConfigException> errors = new ArrayList<>();

        String specFileName = specFile(params).getFileName().toString();

        try {
            List<PackageProperty> properties = List.of(
                    new PackageProperty("Name", PACKAGE_NAME.fetchFrom(params),
                            "APPLICATION_PACKAGE", specFileName),
                    new PackageProperty("Version", VERSION.fetchFrom(params),
                            "APPLICATION_VERSION", specFileName),
                    new PackageProperty("Release", RELEASE.fetchFrom(params),
                            "APPLICATION_RELEASE", specFileName),
                    new PackageProperty("Arch", rpmArch(), null, specFileName));

            List<String> actualValues = Executor.of(TOOL_RPM, "-qp", "--queryformat",
                    properties.stream().map(entry -> String.format("%%{%s}",
                    entry.name)).collect(Collectors.joining("\\n")),
                    packageBundle.toString()).saveOutput(true).executeExpectSuccess().getOutput();

            Iterator<String> actualValuesIt = actualValues.iterator();
            properties.forEach(property -> errors.add(property.verifyValue(
                    actualValuesIt.next())));
        } catch (IOException ex) {
            // Ignore error as it is not critical. Just report it.
            Log.verbose(ex);
        }

        return errors;
    }

    /**
     * Various ways to get rpm arch. Needed to address JDK-8233143. rpmbuild is
     * mandatory for rpm packaging, try it first. rpm is optional and may not be
     * available, use as the last resort.
     */
    private enum RpmArchReader {
        Rpmbuild(TOOL_RPMBUILD, "--eval=%{_target_cpu}"),
        Rpm(TOOL_RPM, "--eval=%{_target_cpu}");

        RpmArchReader(String... cmdline) {
            this.cmdline = cmdline;
        }

        String getRpmArch() throws IOException {
            Executor exec = Executor.of(cmdline).saveOutput(true);
            if (this == values()[values().length - 1]) {
                exec.executeExpectSuccess();
            } else if (exec.execute() != 0) {
                return null;
            }

            return exec.getOutput().get(0);
        }

        private final String[] cmdline;
    }

    private String rpmArch() throws IOException {
        if (rpmArch == null) {
            for (var rpmArchReader : RpmArchReader.values()) {
                rpmArch = rpmArchReader.getRpmArch();
                if (rpmArch != null) {
                    break;
                }
            }
        }
        return rpmArch;
    }

    private Path specFile(Map<String, ? super Object> params) {
        return TEMP_ROOT.fetchFrom(params).resolve(Path.of("SPECS",
                PACKAGE_NAME.fetchFrom(params) + ".spec"));
    }

    private Path buildRPM(Map<String, ? super Object> params,
            Path outdir) throws IOException {

        Path rpmFile = outdir.toAbsolutePath().resolve(String.format(
                "%s-%s-%s.%s.rpm", PACKAGE_NAME.fetchFrom(params),
                VERSION.fetchFrom(params), RELEASE.fetchFrom(params), rpmArch()));

        Log.verbose(MessageFormat.format(I18N.getString(
                "message.outputting-bundle-location"),
                rpmFile.getParent()));

        PlatformPackage thePackage = createMetaPackage(params);

        //run rpmbuild
        Executor.of(
                TOOL_RPMBUILD,
                "-bb", specFile(params).toAbsolutePath().toString(),
                "--define", String.format("%%_sourcedir %s",
                        thePackage.sourceRoot()),
                // save result to output dir
                "--define", String.format("%%_rpmdir %s", rpmFile.getParent()),
                // do not use other system directories to build as current user
                "--define", String.format("%%_topdir %s",
                        TEMP_ROOT.fetchFrom(params).toAbsolutePath()),
                "--define", String.format("%%_rpmfilename %s", rpmFile.getFileName())
        ).executeExpectSuccess();

        Log.verbose(MessageFormat.format(
                I18N.getString("message.output-bundle-location"),
                rpmFile.getParent()));

        return rpmFile;
    }

    @Override
    public String getName() {
        return I18N.getString("rpm.bundler.name");
    }

    @Override
    public String getID() {
        return "rpm";
    }

    @Override
    public boolean supported(boolean runtimeInstaller) {
        return Platform.isLinux() && (createRpmbuildToolValidator().validate() == null);
    }

    @Override
    public boolean isDefault() {
        return !LinuxDebBundler.isDebian();
    }

    private String rpmArch;
}
