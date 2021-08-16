/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "kludge_c++11.h"

#include <memory>
#include "JvmLauncher.h"
#include "AppLauncher.h"
#include "FileUtils.h"
#include "UnixSysInfo.h"
#include "Package.h"
#include "Log.h"
#include "app.h"
#include "ErrorHandling.h"


namespace {

size_t hash(const std::string& str) {
    size_t h = 0;
    for(std::string::const_iterator it = str.begin(); it != str.end(); ++it) {
        h = 31 * h + (*it & 0xff);
    }
    return h;
}

Jvm* jvmLauncher;

void launchApp() {
    const tstring launcherPath = SysInfo::getProcessModulePath();

    const Package ownerPackage = Package::findOwnerOfFile(launcherPath);

    AppLauncher appLauncher;
    appLauncher.addJvmLibName(_T("lib/libjli.so"));
    // add backup - older version such as JDK11 have it in jli sub-dir
    appLauncher.addJvmLibName(_T("lib/jli/libjli.so"));

    if (ownerPackage.name().empty()) {
        // Launcher should be in "bin" subdirectory of app image.
        const tstring appImageRoot = FileUtils::dirname(
                FileUtils::dirname(launcherPath));

        appLauncher
            .setImageRoot(appImageRoot)
            .setAppDir(FileUtils::mkpath() << appImageRoot << _T("lib/app"))
            .setLibEnvVariableName(_T("LD_LIBRARY_PATH"))
            .setDefaultRuntimePath(FileUtils::mkpath() << appImageRoot
                    << _T("lib/runtime"));
    } else {
        ownerPackage.initAppLauncher(appLauncher);
    }

    const std::string _JPACKAGE_LAUNCHER = "_JPACKAGE_LAUNCHER";

    std::string launchInfo = SysInfo::getEnvVariable(std::nothrow,
            _JPACKAGE_LAUNCHER, "");

    const std::string thisLdLibraryPath = SysInfo::getEnvVariable(std::nothrow,
            "LD_LIBRARY_PATH", "");

    const size_t thisHash = hash(thisLdLibraryPath);

    if (!launchInfo.empty()) {
        LOG_TRACE(tstrings::any() << "Found "
                << _JPACKAGE_LAUNCHER << "=[" << launchInfo << "]");

        tistringstream iss(launchInfo);
        iss.exceptions(std::ios::failbit | std::ios::badbit);

        size_t hash = 0;
        iss >> hash;

        launchInfo = "";

        if (thisHash != hash) {
            // This launcher execution is the result of execve() call from
            // withing JVM.
            // This means all JVM arguments are already configured in launcher
            // process command line.
            // No need to construct command line for JVM.
            LOG_TRACE("Not building JVM arguments from cfg file");
            appLauncher.setInitJvmFromCmdlineOnly(true);
        }
    } else {
        // Changed LD_LIBRARY_PATH environment variable might result in
        // execve() call from within JVM.
        // Set _JPACKAGE_LAUNCHER environment variable accordingly so that
        // restarted launcher process can detect a restart.

        launchInfo = (tstrings::any() << thisHash).str();
    }

    JP_TRY;
    if (0 != setenv(_JPACKAGE_LAUNCHER.c_str(), launchInfo.c_str(), 1)) {
        JP_THROW(tstrings::any() << "setenv(" << _JPACKAGE_LAUNCHER
                << ", " << launchInfo << ") failed. Error: " << lastCRTError());
    } else {
        LOG_TRACE(tstrings::any() << "Set "
                << _JPACKAGE_LAUNCHER << "=[" << launchInfo << "]");
    }
    JP_CATCH_ALL;

    jvmLauncher = appLauncher.createJvmLauncher();
}

} // namespace


extern "C" {

JNIEXPORT JvmlLauncherHandle jvmLauncherCreate(int argc, char *argv[]) {
    SysInfo::argc = argc;
    SysInfo::argv = argv;
    jvmLauncher = 0;
    app::launch(std::nothrow, launchApp);

    JvmlLauncherHandle jlh = 0;
    if (jvmLauncher) {
        jlh = jvmLauncher->exportLauncher();
        const std::unique_ptr<Jvm> deleter(jvmLauncher);
    }

    return jlh;
}

} // extern "C"


namespace {

void dcon() __attribute__((destructor));

void dcon() {
   LOG_TRACE("unload");
}

} // namespace
