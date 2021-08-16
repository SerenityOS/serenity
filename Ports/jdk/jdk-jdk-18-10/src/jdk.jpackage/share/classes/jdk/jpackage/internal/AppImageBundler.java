/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import static jdk.jpackage.internal.StandardBundlerParam.PREDEFINED_APP_IMAGE;
import static jdk.jpackage.internal.StandardBundlerParam.PREDEFINED_RUNTIME_IMAGE;
import static jdk.jpackage.internal.StandardBundlerParam.LAUNCHER_DATA;
import static jdk.jpackage.internal.StandardBundlerParam.APP_NAME;


class AppImageBundler extends AbstractBundler {

    @Override
    public final String getName() {
        return I18N.getString("app.bundler.name");
    }

    @Override
    public final String getID() {
        return "app";
    }

    @Override
    public final String getBundleType() {
        return "IMAGE";
    }

    @Override
    public final boolean validate(Map<String, ? super Object> params)
            throws ConfigException {
        try {
            Objects.requireNonNull(params);

            if (!params.containsKey(PREDEFINED_APP_IMAGE.getID())
                    && !StandardBundlerParam.isRuntimeInstaller(params)) {
                LAUNCHER_DATA.fetchFrom(params);
            }

            if (paramsValidator != null) {
                paramsValidator.validate(params);
            }
        } catch (RuntimeException re) {
            if (re.getCause() instanceof ConfigException) {
                throw (ConfigException) re.getCause();
            } else {
                throw new ConfigException(re);
            }
        }

        return true;
    }

    @Override
    public final Path execute(Map<String, ? super Object> params,
            Path outputParentDir) throws PackagerException {
        if (StandardBundlerParam.isRuntimeInstaller(params)) {
            return PREDEFINED_RUNTIME_IMAGE.fetchFrom(params);
        }

        try {
            return createAppBundle(params, outputParentDir);
        } catch (PackagerException pe) {
            throw pe;
        } catch (RuntimeException|IOException|ConfigException ex) {
            Log.verbose(ex);
            throw new PackagerException(ex);
        }
    }

    @Override
    public final boolean supported(boolean runtimeInstaller) {
        return true;
    }

    @Override
    public final boolean isDefault() {
        return false;
    }

    final AppImageBundler setDependentTask(boolean v) {
        dependentTask = v;
        return this;
    }

    final AppImageBundler setAppImageSupplier(
            Function<Path, AbstractAppImageBuilder> v) {
        appImageSupplier = v;
        return this;
    }

    final AppImageBundler setParamsValidator(ParamsValidator v) {
        paramsValidator = v;
        return this;
    }

    @FunctionalInterface
    interface ParamsValidator {
        void validate(Map<String, ? super Object> params) throws ConfigException;
    }

    private Path createRoot(Map<String, ? super Object> params,
            Path outputDirectory) throws PackagerException, IOException {

        IOUtils.writableOutputDir(outputDirectory);

        String imageName = APP_NAME.fetchFrom(params);
        if (Platform.isMac()) {
            imageName = imageName + ".app";
        }

        if (!dependentTask) {
            Log.verbose(MessageFormat.format(
                    I18N.getString("message.creating-app-bundle"),
                    imageName, outputDirectory.toAbsolutePath()));
        }

        // Create directory structure
        Path rootDirectory = outputDirectory.resolve(imageName);
        if (Files.exists(rootDirectory)) {
            throw new PackagerException("error.root-exists",
                    rootDirectory.toAbsolutePath().toString());
        }

        Files.createDirectories(rootDirectory);

        return rootDirectory;
    }

    private Path createAppBundle(Map<String, ? super Object> params,
            Path outputDirectory) throws PackagerException, IOException,
            ConfigException {

        Path rootDirectory = createRoot(params, outputDirectory);
        AbstractAppImageBuilder appBuilder = appImageSupplier.apply(rootDirectory);
        if (PREDEFINED_RUNTIME_IMAGE.fetchFrom(params) == null ) {
            JLinkBundlerHelper.execute(params,
                    appBuilder.getAppLayout().runtimeHomeDirectory());
        } else {
            StandardBundlerParam.copyPredefinedRuntimeImage(
                    params, appBuilder.getAppLayout());
        }
        appBuilder.prepareApplicationFiles(params);
        return rootDirectory;
    }

    private boolean dependentTask;
    private ParamsValidator paramsValidator;
    private Function<Path, AbstractAppImageBuilder> appImageSupplier;
}
