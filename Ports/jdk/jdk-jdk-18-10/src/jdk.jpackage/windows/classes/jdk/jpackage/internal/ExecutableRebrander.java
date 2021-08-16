/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import static jdk.jpackage.internal.OverridableResource.createResource;
import static jdk.jpackage.internal.StandardBundlerParam.APP_NAME;
import static jdk.jpackage.internal.StandardBundlerParam.COPYRIGHT;
import static jdk.jpackage.internal.StandardBundlerParam.DESCRIPTION;
import static jdk.jpackage.internal.StandardBundlerParam.TEMP_ROOT;
import static jdk.jpackage.internal.StandardBundlerParam.VENDOR;
import static jdk.jpackage.internal.StandardBundlerParam.VERSION;
import static jdk.jpackage.internal.WindowsAppImageBuilder.ICON_ICO;


final class ExecutableRebrander {
    private static final ResourceBundle I18N = ResourceBundle.getBundle(
            "jdk.jpackage.internal.resources.WinResources");

    private static final String LAUNCHER_PROPERTIES_TEMPLATE =
            "WinLauncher.template";

    private static final String INSTALLER_PROPERTIES_TEMPLATE =
            "WinInstaller.template";

    private static final String INSTALLER_PROPERTIES_RESOURE_DIR_ID =
            "WinInstaller.properties";


    void rebrandInstaller(Map<String, ? super Object> params, Path target)
            throws IOException {
        Path icon = ICON_ICO.fetchFrom(params);
        rebrandExecutable(params, icon, () -> {
            return createResource(INSTALLER_PROPERTIES_TEMPLATE, params).setPublicName(
                    INSTALLER_PROPERTIES_RESOURE_DIR_ID);
        }, target);
    }

    void rebrandLauncher(Map<String, ? super Object> params, Path icon,
            Path target) throws IOException {
        rebrandExecutable(params, icon, () -> {
            return createResource(
                    LAUNCHER_PROPERTIES_TEMPLATE, params).setPublicName(
                            APP_NAME.fetchFrom(params) + ".properties");
        }, target);
    }

    private void rebrandExecutable(Map<String, ? super Object> params, Path icon,
            Supplier<OverridableResource> resourceSupplier, Path target) throws
            IOException {
        if (!target.isAbsolute() || (icon != null && !icon.isAbsolute())) {
            Path absIcon = null;
            if (icon != null) {
                absIcon = icon.toAbsolutePath();
            }
            rebrandExecutable(params, absIcon, resourceSupplier,
                    target.toAbsolutePath());
            return;
        }
        rebrandExecutable(params, target, (resourceLock) -> {
            rebrandProperties(resourceLock, resourceSupplier.get(),
                    createSubstituteData(
                            params), target);
            if (icon != null) {
                iconSwap(resourceLock, icon.toString());
            }
        });
    }

    ExecutableRebrander addAction(UpdateResourceAction action) {
        if (extraActions == null) {
            extraActions = new ArrayList<>();
        }
        extraActions.add(action);
        return this;
    }

    private void rebrandExecutable(Map<String, ? super Object> params,
            Path target, UpdateResourceAction action) throws IOException {
        try {
            String tempDirectory = TEMP_ROOT.fetchFrom(params)
                    .toAbsolutePath().toString();
            if (WindowsDefender.isThereAPotentialWindowsDefenderIssue(
                    tempDirectory)) {
                Log.verbose(MessageFormat.format(I18N.getString(
                        "message.potential.windows.defender.issue"),
                        tempDirectory));
            }

            target.toFile().setWritable(true, true);

            long resourceLock = lockResource(target.toString());
            if (resourceLock == 0) {
                throw new RuntimeException(MessageFormat.format(
                    I18N.getString("error.lock-resource"), target));
            }

            try {
                action.editResource(resourceLock);
                if (extraActions != null) {
                    for (UpdateResourceAction extraAction: extraActions) {
                        extraAction.editResource(resourceLock);
                    }
                }
            } finally {
                if (resourceLock != 0) {
                    unlockResource(resourceLock);
                }
            }
        } finally {
            target.toFile().setReadOnly();
        }
    }

    @FunctionalInterface
    static interface UpdateResourceAction {
        public void editResource(long resourceLock) throws IOException;
    }

    private static String getFixedFileVersion(String value) {
        int addComponentsCount = 4
                - DottedVersion.greedy(value).getComponents().length;
        if (addComponentsCount > 0) {
            StringBuilder sb = new StringBuilder(value);
            do {
                sb.append('.');
                sb.append(0);
            } while (--addComponentsCount > 0);
            return sb.toString();
        }
        return value;
    }

    private Map<String, String> createSubstituteData(
            Map<String, ? super Object> params) {
        Map<String, String> data = new HashMap<>();

        String fixedFileVersion = getFixedFileVersion(VERSION.fetchFrom(params));

        // mapping Java parameters in strings for version resource
        validateValueAndPut(data, "COMPANY_NAME", VENDOR, params);
        validateValueAndPut(data, "FILE_DESCRIPTION", DESCRIPTION, params);
        validateValueAndPut(data, "FILE_VERSION", VERSION, params);
        validateValueAndPut(data, "LEGAL_COPYRIGHT", COPYRIGHT, params);
        validateValueAndPut(data, "PRODUCT_NAME", APP_NAME, params);
        data.put("FIXEDFILEINFO_FILE_VERSION", fixedFileVersion);

        return data;
    }

    private void rebrandProperties(long resourceLock, OverridableResource properties,
            Map<String, String> data, Path target) throws IOException {

        String targetExecutableName = IOUtils.getFileName(target).toString();
        data.put("INTERNAL_NAME", targetExecutableName);
        data.put("ORIGINAL_FILENAME", targetExecutableName);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        properties
            .setSubstitutionData(data)
            .setCategory(I18N.getString("resource.executable-properties-template"))
            .saveToStream(buffer);

        final List<String> propList = new ArrayList<>();
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(
                buffer.toByteArray()), StandardCharsets.UTF_8)) {
            final Properties configProp = new Properties();
            configProp.load(reader);
            configProp.forEach((k, v) -> {
                propList.add(k.toString());
                propList.add(v.toString());
            });
        }

        if (versionSwap(resourceLock, propList.toArray(String[]::new)) != 0) {
            throw new RuntimeException(MessageFormat.format(
                    I18N.getString("error.version-swap"), target));
        }
    }

    private static void validateValueAndPut(
            Map<String, String> data, String key,
            BundlerParamInfo<String> param,
            Map<String, ? super Object> params) {
        String value = param.fetchFrom(params);
        if (value.contains("\r") || value.contains("\n")) {
            Log.error("Configuration Parameter " + param.getID()
                    + " contains multiple lines of text, ignore it");
            data.put(key, "");
            return;
        }
        data.put(key, value);
    }

    private List<UpdateResourceAction> extraActions;

    static {
        System.loadLibrary("jpackage");
    }

    private static native long lockResource(String executable);

    private static native void unlockResource(long resourceLock);

    private static native int iconSwap(long resourceLock, String iconTarget);

    private static native int versionSwap(long resourceLock, String[] executableProperties);
}
