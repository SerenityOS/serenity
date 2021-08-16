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

#include "AppLauncher.h"
#include "app.h"
#include "FileUtils.h"
#include "UnixSysInfo.h"
#include "JvmLauncher.h"


namespace {

Jvm* jvmLauncher = 0;

void launchJvm() {
    // On Mac JLI_Launch() spawns a new thread that actually starts the JVM.
    // This new thread simply re-runs launcher's main() function with
    // arguments passed into JLI_Launch() call.
    // Jvm::launch() calls JLI_Launch() triggering thread spawning.
    jvmLauncher->launch();
}

void initJvmLauncher() {
    const tstring launcherPath = SysInfo::getProcessModulePath();

    // Launcher should be in "Contents/MacOS" subdirectory of app image.
    const tstring appImageRoot = FileUtils::dirname(FileUtils::dirname(
            FileUtils::dirname(launcherPath)));

    // Create JVM launcher and save in global variable.
    jvmLauncher = AppLauncher()
        .setImageRoot(appImageRoot)
        .addJvmLibName(_T("Contents/Home/lib/libjli.dylib"))
        // add backup - older version such as JDK11 have it in jli sub-dir
        .addJvmLibName(_T("Contents/Home/lib/jli/libjli.dylib"))
        .setAppDir(FileUtils::mkpath() << appImageRoot << _T("Contents/app"))
        .setLibEnvVariableName(_T("DYLD_LIBRARY_PATH"))
        .setDefaultRuntimePath(FileUtils::mkpath() << appImageRoot
                << _T("Contents/runtime"))
        .createJvmLauncher();

    // Kick start JVM launching. The function wouldn't return!
    launchJvm();
}

} // namespace


int main(int argc, char *argv[]) {
    if (jvmLauncher) {
        // This is the call from the thread spawned by JVM.
        // Skip initialization phase as we have done this already in the first
        // call of main().
        // Besides we should ignore main() arguments because these are the
        // arguments passed into JLI_Launch() call and not the arguments with
        // which the launcher was started.
        return app::launch(std::nothrow, launchJvm);
    }

    SysInfo::argc = argc;
    SysInfo::argv = argv;
    return app::launch(std::nothrow, initJvmLauncher);
}
