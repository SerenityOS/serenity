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
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import static jdk.jpackage.internal.OverridableResource.createResource;
import static jdk.jpackage.internal.StandardBundlerParam.APP_NAME;
import static jdk.jpackage.internal.StandardBundlerParam.ICON;
import static jdk.jpackage.internal.StandardBundlerParam.SOURCE_DIR;
import jdk.jpackage.internal.resources.ResourceLocator;

/*
 * AbstractAppImageBuilder
 *     This is sub-classed by each of the platform dependent AppImageBuilder
 * classes, and contains resource processing code common to all platforms.
 */

public abstract class AbstractAppImageBuilder {

    private final Path root;
    protected final ApplicationLayout appLayout;

    public AbstractAppImageBuilder(Path root) {
        this.root = root;
        appLayout = ApplicationLayout.platformAppImage().resolveAt(root);
    }

    public InputStream getResourceAsStream(String name) {
        return ResourceLocator.class.getResourceAsStream(name);
    }

    public abstract void prepareApplicationFiles(
            Map<String, ? super Object> params) throws IOException;

    protected void writeCfgFile(Map<String, ? super Object> params) throws
            IOException {
        new CfgFile().initFromParams(params).create(root);
    }

    ApplicationLayout getAppLayout() {
        return appLayout;
    }

    protected void copyApplication(Map<String, ? super Object> params)
            throws IOException {
        Path inputPath = SOURCE_DIR.fetchFrom(params);
        if (inputPath != null) {
            IOUtils.copyRecursive(SOURCE_DIR.fetchFrom(params),
                    appLayout.appDirectory());
        }
        AppImageFile.save(root, params);
    }

    public static OverridableResource createIconResource(String defaultIconName,
            BundlerParamInfo<Path> iconParam, Map<String, ? super Object> params,
            Map<String, ? super Object> mainParams) throws IOException {

        if (mainParams != null) {
            params = AddLauncherArguments.merge(mainParams, params, ICON.getID(),
                    iconParam.getID());
        }

        final String resourcePublicName = APP_NAME.fetchFrom(params)
                + IOUtils.getSuffix(Path.of(defaultIconName));

        IconType iconType = getLauncherIconType(params);
        if (iconType == IconType.NoIcon) {
            return null;
        }

        OverridableResource resource = createResource(defaultIconName, params)
                .setCategory("icon")
                .setExternal(iconParam.fetchFrom(params))
                .setPublicName(resourcePublicName);

        if (iconType == IconType.DefaultOrResourceDirIcon && mainParams != null) {
            // No icon explicitly configured for this launcher.
            // Dry-run resource creation to figure out its source.
            final Path nullPath = null;
            if (resource.saveToFile(nullPath)
                    != OverridableResource.Source.ResourceDir) {
                // No icon in resource dir for this launcher, inherit icon
                // configured for the main launcher.
                resource = createIconResource(defaultIconName, iconParam,
                        mainParams, null).setLogPublicName(resourcePublicName);
            }
        }

        return resource;
    }

    private enum IconType { DefaultOrResourceDirIcon, CustomIcon, NoIcon };

    private static IconType getLauncherIconType(Map<String, ? super Object> params) {
        Path launcherIcon = ICON.fetchFrom(params);
        if (launcherIcon == null) {
            return IconType.DefaultOrResourceDirIcon;
        }

        if (launcherIcon.toFile().getName().isEmpty()) {
            return IconType.NoIcon;
        }

        return IconType.CustomIcon;
    }
}
