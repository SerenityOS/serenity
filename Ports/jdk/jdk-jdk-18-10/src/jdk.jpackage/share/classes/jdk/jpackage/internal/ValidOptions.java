/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import jdk.jpackage.internal.Arguments.CLIOptions;

/**
 * ValidOptions
 *
 * Two basic methods for validating command line options.
 *
 * initArgs()
 *      Computes the Map of valid options for each mode on this Platform.
 *
 * checkIfSupported(CLIOptions arg)
 *      Determine if the given arg is valid on this platform.
 *
 * checkIfImageSupported(CLIOptions arg)
 *      Determine if the given arg is valid for creating app image.
 *
 * checkIfInstallerSupported(CLIOptions arg)
 *      Determine if the given arg is valid for creating installer.
 *
 */
class ValidOptions {

    enum USE {
        ALL,        // valid in all cases
        LAUNCHER,   // valid when creating a launcher
        INSTALL     // valid when creating an installer
    }

    private static final HashMap<String, USE> options = new HashMap<>();


    // initializing list of mandatory arguments
    static {
        options.put(CLIOptions.NAME.getId(), USE.ALL);
        options.put(CLIOptions.VERSION.getId(), USE.ALL);
        options.put(CLIOptions.OUTPUT.getId(), USE.ALL);
        options.put(CLIOptions.TEMP_ROOT.getId(), USE.ALL);
        options.put(CLIOptions.VERBOSE.getId(), USE.ALL);
        options.put(CLIOptions.PREDEFINED_RUNTIME_IMAGE.getId(), USE.ALL);
        options.put(CLIOptions.RESOURCE_DIR.getId(), USE.ALL);
        options.put(CLIOptions.DESCRIPTION.getId(), USE.ALL);
        options.put(CLIOptions.VENDOR.getId(), USE.ALL);
        options.put(CLIOptions.COPYRIGHT.getId(), USE.ALL);
        options.put(CLIOptions.PACKAGE_TYPE.getId(), USE.ALL);
        options.put(CLIOptions.ICON.getId(), USE.ALL);

        options.put(CLIOptions.INPUT.getId(), USE.LAUNCHER);
        options.put(CLIOptions.MODULE.getId(), USE.LAUNCHER);
        options.put(CLIOptions.MODULE_PATH.getId(), USE.LAUNCHER);
        options.put(CLIOptions.ADD_MODULES.getId(), USE.LAUNCHER);
        options.put(CLIOptions.MAIN_JAR.getId(), USE.LAUNCHER);
        options.put(CLIOptions.APPCLASS.getId(), USE.LAUNCHER);
        options.put(CLIOptions.ARGUMENTS.getId(), USE.LAUNCHER);
        options.put(CLIOptions.JAVA_OPTIONS.getId(), USE.LAUNCHER);
        options.put(CLIOptions.ADD_LAUNCHER.getId(), USE.LAUNCHER);
        options.put(CLIOptions.JLINK_OPTIONS.getId(), USE.LAUNCHER);

        options.put(CLIOptions.LICENSE_FILE.getId(), USE.INSTALL);
        options.put(CLIOptions.INSTALL_DIR.getId(), USE.INSTALL);
        options.put(CLIOptions.PREDEFINED_APP_IMAGE.getId(), USE.INSTALL);

        options.put(CLIOptions.ABOUT_URL.getId(), USE.INSTALL);

        options.put(CLIOptions.FILE_ASSOCIATIONS.getId(),
            (Platform.getPlatform() == Platform.MAC) ?  USE.ALL : USE.INSTALL);

        if (Platform.getPlatform() == Platform.WINDOWS) {
            options.put(CLIOptions.WIN_CONSOLE_HINT.getId(), USE.LAUNCHER);

            options.put(CLIOptions.WIN_HELP_URL.getId(), USE.INSTALL);
            options.put(CLIOptions.WIN_UPDATE_URL.getId(), USE.INSTALL);

            options.put(CLIOptions.WIN_MENU_HINT.getId(), USE.INSTALL);
            options.put(CLIOptions.WIN_MENU_GROUP.getId(), USE.INSTALL);
            options.put(CLIOptions.WIN_SHORTCUT_HINT.getId(), USE.INSTALL);
            options.put(CLIOptions.WIN_SHORTCUT_PROMPT.getId(), USE.INSTALL);
            options.put(CLIOptions.WIN_DIR_CHOOSER.getId(), USE.INSTALL);
            options.put(CLIOptions.WIN_UPGRADE_UUID.getId(), USE.INSTALL);
            options.put(CLIOptions.WIN_PER_USER_INSTALLATION.getId(),
                    USE.INSTALL);
        }

        if (Platform.getPlatform() == Platform.MAC) {
            options.put(CLIOptions.MAC_SIGN.getId(), USE.ALL);
            options.put(CLIOptions.MAC_BUNDLE_NAME.getId(), USE.ALL);
            options.put(CLIOptions.MAC_BUNDLE_IDENTIFIER.getId(), USE.ALL);
            options.put(CLIOptions.MAC_BUNDLE_SIGNING_PREFIX.getId(), USE.ALL);
            options.put(CLIOptions.MAC_SIGNING_KEY_NAME.getId(), USE.ALL);
            options.put(CLIOptions.MAC_SIGNING_KEYCHAIN.getId(), USE.ALL);
            options.put(CLIOptions.MAC_APP_STORE.getId(), USE.ALL);
            options.put(CLIOptions.MAC_CATEGORY.getId(), USE.ALL);
            options.put(CLIOptions.MAC_ENTITLEMENTS.getId(), USE.ALL);
        }

        if (Platform.getPlatform() == Platform.LINUX) {
            options.put(CLIOptions.LINUX_BUNDLE_NAME.getId(), USE.INSTALL);
            options.put(CLIOptions.LINUX_DEB_MAINTAINER.getId(), USE.INSTALL);
            options.put(CLIOptions.LINUX_CATEGORY.getId(), USE.INSTALL);
            options.put(CLIOptions.LINUX_RPM_LICENSE_TYPE.getId(), USE.INSTALL);
            options.put(CLIOptions.LINUX_PACKAGE_DEPENDENCIES.getId(),
                    USE.INSTALL);
            options.put(CLIOptions.LINUX_MENU_GROUP.getId(), USE.INSTALL);
            options.put(CLIOptions.RELEASE.getId(), USE.INSTALL);
            options.put(CLIOptions.LINUX_SHORTCUT_HINT.getId(), USE.INSTALL);
        }
    }

    static boolean checkIfSupported(CLIOptions arg) {
        return options.containsKey(arg.getId());
    }

    static boolean checkIfImageSupported(CLIOptions arg) {
        USE use = options.get(arg.getId());
        return USE.ALL == use || USE.LAUNCHER == use;
    }

    static boolean checkIfInstallerSupported(CLIOptions arg) {
        USE use = options.get(arg.getId());
        return USE.ALL == use || USE.INSTALL == use;
    }
}
