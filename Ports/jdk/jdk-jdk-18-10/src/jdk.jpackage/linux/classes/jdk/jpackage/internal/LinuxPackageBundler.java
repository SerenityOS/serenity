/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static jdk.jpackage.internal.DesktopIntegration.*;
import static jdk.jpackage.internal.StandardBundlerParam.PREDEFINED_RUNTIME_IMAGE;
import static jdk.jpackage.internal.StandardBundlerParam.VERSION;
import static jdk.jpackage.internal.StandardBundlerParam.RELEASE;
import static jdk.jpackage.internal.StandardBundlerParam.VENDOR;
import static jdk.jpackage.internal.StandardBundlerParam.DESCRIPTION;
import static jdk.jpackage.internal.StandardBundlerParam.INSTALL_DIR;

abstract class LinuxPackageBundler extends AbstractBundler {

    LinuxPackageBundler(BundlerParamInfo<String> packageName) {
        this.packageName = packageName;
        appImageBundler = new LinuxAppBundler().setDependentTask(true);
    }

    @Override
    public final boolean validate(Map<String, ? super Object> params)
            throws ConfigException {

        // run basic validation to ensure requirements are met
        // we are not interested in return code, only possible exception
        appImageBundler.validate(params);

        validateInstallDir(LINUX_INSTALL_DIR.fetchFrom(params));

        FileAssociation.verify(FileAssociation.fetchFrom(params));

        // If package name has some restrictions, the string converter will
        // throw an exception if invalid
        packageName.getStringConverter().apply(packageName.fetchFrom(params),
            params);

        for (var validator: getToolValidators(params)) {
            ConfigException ex = validator.validate();
            if (ex != null) {
                throw ex;
            }
        }

        if (!isDefault()) {
            withFindNeededPackages = false;
            Log.verbose(MessageFormat.format(I18N.getString(
                    "message.not-default-bundler-no-dependencies-lookup"),
                    getName()));
        } else {
            withFindNeededPackages = LibProvidersLookup.supported();
            if (!withFindNeededPackages) {
                final String advice;
                if ("deb".equals(getID())) {
                    advice = "message.deb-ldd-not-available.advice";
                } else {
                    advice = "message.rpm-ldd-not-available.advice";
                }
                // Let user know package dependencies will not be generated.
                Log.error(String.format("%s\n%s", I18N.getString(
                        "message.ldd-not-available"), I18N.getString(advice)));
            }
        }

        // Packaging specific validation
        doValidate(params);

        return true;
    }

    @Override
    public final String getBundleType() {
        return "INSTALLER";
    }

    @Override
    public final Path execute(Map<String, ? super Object> params,
            Path outputParentDir) throws PackagerException {
        IOUtils.writableOutputDir(outputParentDir);

        PlatformPackage thePackage = createMetaPackage(params);

        Function<Path, ApplicationLayout> initAppImageLayout = imageRoot -> {
            ApplicationLayout layout = appImageLayout(params);
            layout.pathGroup().setPath(new Object(),
                    AppImageFile.getPathInAppImage(Path.of("")));
            return layout.resolveAt(imageRoot);
        };

        try {
            Path appImage = StandardBundlerParam.getPredefinedAppImage(params);

            // we either have an application image or need to build one
            if (appImage != null) {
                initAppImageLayout.apply(appImage).copy(
                        thePackage.sourceApplicationLayout());
            } else {
                final Path srcAppImageRoot = thePackage.sourceRoot().resolve("src");
                appImage = appImageBundler.execute(params, srcAppImageRoot);
                ApplicationLayout srcAppLayout = initAppImageLayout.apply(
                        appImage);
                if (appImage.equals(PREDEFINED_RUNTIME_IMAGE.fetchFrom(params))) {
                    // Application image points to run-time image.
                    // Copy it.
                    srcAppLayout.copy(thePackage.sourceApplicationLayout());
                } else {
                    // Application image is a newly created directory tree.
                    // Move it.
                    srcAppLayout.move(thePackage.sourceApplicationLayout());
                    IOUtils.deleteRecursive(srcAppImageRoot);
                }
            }

            desktopIntegration = DesktopIntegration.create(thePackage, params);

            Map<String, String> data = createDefaultReplacementData(params);
            if (desktopIntegration != null) {
                data.putAll(desktopIntegration.create());
            } else {
                Stream.of(DESKTOP_COMMANDS_INSTALL, DESKTOP_COMMANDS_UNINSTALL,
                        UTILITY_SCRIPTS).forEach(v -> data.put(v, ""));
            }

            data.putAll(createReplacementData(params));

            Path packageBundle = buildPackageBundle(Collections.unmodifiableMap(
                    data), params, outputParentDir);

            verifyOutputBundle(params, packageBundle).stream()
                    .filter(Objects::nonNull)
                    .forEachOrdered(ex -> {
                Log.verbose(ex.getLocalizedMessage());
                Log.verbose(ex.getAdvice());
            });

            return packageBundle;
        } catch (IOException ex) {
            Log.verbose(ex);
            throw new PackagerException(ex);
        }
    }

    private List<String> getListOfNeededPackages(
            Map<String, ? super Object> params) throws IOException {

        PlatformPackage thePackage = createMetaPackage(params);

        final List<String> xdgUtilsPackage;
        if (desktopIntegration != null) {
            xdgUtilsPackage = desktopIntegration.requiredPackages();
        } else {
            xdgUtilsPackage = Collections.emptyList();
        }

        final List<String> neededLibPackages;
        if (withFindNeededPackages && Files.exists(thePackage.sourceRoot())) {
            LibProvidersLookup lookup = new LibProvidersLookup();
            initLibProvidersLookup(params, lookup);

            neededLibPackages = lookup.execute(thePackage.sourceRoot());
        } else {
            neededLibPackages = Collections.emptyList();
            if (!Files.exists(thePackage.sourceRoot())) {
                Log.info(I18N.getString("warning.foreign-app-image"));
            }
        }

        // Merge all package lists together.
        // Filter out empty names, sort and remove duplicates.
        List<String> result = Stream.of(xdgUtilsPackage, neededLibPackages).flatMap(
                List::stream).filter(Predicate.not(String::isEmpty)).sorted().distinct().toList();

        Log.verbose(String.format("Required packages: %s", result));

        return result;
    }

    private Map<String, String> createDefaultReplacementData(
            Map<String, ? super Object> params) throws IOException {
        Map<String, String> data = new HashMap<>();

        data.put("APPLICATION_PACKAGE", createMetaPackage(params).name());
        data.put("APPLICATION_VENDOR", VENDOR.fetchFrom(params));
        data.put("APPLICATION_VERSION", VERSION.fetchFrom(params));
        data.put("APPLICATION_DESCRIPTION", DESCRIPTION.fetchFrom(params));
        data.put("APPLICATION_RELEASE", RELEASE.fetchFrom(params));

        String defaultDeps = String.join(", ", getListOfNeededPackages(params));
        String customDeps = LINUX_PACKAGE_DEPENDENCIES.fetchFrom(params).strip();
        if (!customDeps.isEmpty() && !defaultDeps.isEmpty()) {
            customDeps = ", " + customDeps;
        }
        data.put("PACKAGE_DEFAULT_DEPENDENCIES", defaultDeps);
        data.put("PACKAGE_CUSTOM_DEPENDENCIES", customDeps);

        return data;
    }

    protected abstract List<ConfigException> verifyOutputBundle(
            Map<String, ? super Object> params, Path packageBundle);

    protected abstract void initLibProvidersLookup(
            Map<String, ? super Object> params,
            LibProvidersLookup libProvidersLookup);

    protected abstract List<ToolValidator> getToolValidators(
            Map<String, ? super Object> params);

    protected abstract void doValidate(Map<String, ? super Object> params)
            throws ConfigException;

    protected abstract Map<String, String> createReplacementData(
            Map<String, ? super Object> params) throws IOException;

    protected abstract Path buildPackageBundle(
            Map<String, String> replacementData,
            Map<String, ? super Object> params, Path outputParentDir) throws
            PackagerException, IOException;

    protected final PlatformPackage createMetaPackage(
            Map<String, ? super Object> params) {

        Supplier<ApplicationLayout> packageLayout = () -> {
            String installDir = LINUX_INSTALL_DIR.fetchFrom(params);
            if (isInstallDirInUsrTree(installDir)) {
                return ApplicationLayout.linuxUsrTreePackageImage(
                        Path.of("/").relativize(Path.of(installDir)),
                        packageName.fetchFrom(params));
            }
            return appImageLayout(params);
        };

        return new PlatformPackage() {
            @Override
            public String name() {
                return packageName.fetchFrom(params);
            }

            @Override
            public Path sourceRoot() {
                return IMAGES_ROOT.fetchFrom(params).toAbsolutePath();
            }

            @Override
            public ApplicationLayout sourceApplicationLayout() {
                return packageLayout.get().resolveAt(
                        applicationInstallDir(sourceRoot()));
            }

            @Override
            public ApplicationLayout installedApplicationLayout() {
                return packageLayout.get().resolveAt(
                        applicationInstallDir(Path.of("/")));
            }

            private Path applicationInstallDir(Path root) {
                String installRoot = LINUX_INSTALL_DIR.fetchFrom(params);
                if (isInstallDirInUsrTree(installRoot)) {
                    return root;
                }

                Path installDir = Path.of(installRoot, name());
                if (installDir.isAbsolute()) {
                    installDir = Path.of("." + installDir.toString()).normalize();
                }
                return root.resolve(installDir);
            }
        };
    }

    private ApplicationLayout appImageLayout(
            Map<String, ? super Object> params) {
        if (StandardBundlerParam.isRuntimeInstaller(params)) {
            return ApplicationLayout.javaRuntime();
        }
        return ApplicationLayout.linuxAppImage();
    }

    private static void validateInstallDir(String installDir) throws
            ConfigException {

        if (installDir.isEmpty()) {
            throw new ConfigException(MessageFormat.format(I18N.getString(
                    "error.invalid-install-dir"), "/"), null);
        }

        boolean valid = false;
        try {
            final Path installDirPath = Path.of(installDir);
            valid = installDirPath.isAbsolute();
            if (valid && !installDirPath.normalize().toString().equals(
                    installDirPath.toString())) {
                // Don't allow '/opt/foo/..' or /opt/.
                valid = false;
            }
        } catch (InvalidPathException ex) {
        }

        if (!valid) {
            throw new ConfigException(MessageFormat.format(I18N.getString(
                    "error.invalid-install-dir"), installDir), null);
        }
    }

    protected static boolean isInstallDirInUsrTree(String installDir) {
        return Set.of("/usr/local", "/usr").contains(installDir);
    }

    private final BundlerParamInfo<String> packageName;
    private final Bundler appImageBundler;
    private boolean withFindNeededPackages;
    private DesktopIntegration desktopIntegration;

    private static final BundlerParamInfo<String> LINUX_PACKAGE_DEPENDENCIES =
            new StandardBundlerParam<>(
            Arguments.CLIOptions.LINUX_PACKAGE_DEPENDENCIES.getId(),
            String.class,
            params -> "",
            (s, p) -> s
    );

    static final BundlerParamInfo<String> LINUX_INSTALL_DIR =
            new StandardBundlerParam<>(
            "linux-install-dir",
            String.class,
            params -> {
                 String dir = INSTALL_DIR.fetchFrom(params);
                 if (dir != null) {
                     if (dir.endsWith("/")) {
                         dir = dir.substring(0, dir.length()-1);
                     }
                     return dir;
                 }
                 return "/opt";
             },
            (s, p) -> s
    );
}
