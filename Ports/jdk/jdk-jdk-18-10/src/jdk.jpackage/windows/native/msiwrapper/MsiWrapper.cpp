/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <algorithm>
#include <windows.h>

#include "WinApp.h"
#include "Guid.h"
#include "SysInfo.h"
#include "MsiUtils.h"
#include "FileUtils.h"
#include "WinFileUtils.h"
#include "Executor.h"
#include "Resources.h"


namespace {
int exitCode = -1;

void launchApp() {
    const auto cmdline = SysInfo::getCommandArgs();
    if (std::find(cmdline.begin(), cmdline.end(), L"uninstall") != cmdline.end()) {
        // This is uninstall request.

        // Get product code of the product to uninstall.
        const auto productCodeUtf8 = Resource(L"product_code", RT_RCDATA).binary();
        const Guid productCode = Guid(std::string(
                (const char*)productCodeUtf8.data(), productCodeUtf8.size()));

        // Uninstall product.
        msi::uninstall().setProductCode(productCode)();
        exitCode = 0;
        return;
    }

    // Create temporary directory where to extract msi file.
    const auto tempMsiDir = FileUtils::createTempDirectory();

    // Schedule temporary directory for deletion.
    FileUtils::Deleter cleaner;
    cleaner.appendRecursiveDirectory(tempMsiDir);

    const auto msiPath = FileUtils::mkpath() << tempMsiDir << L"main.msi";

    // Extract msi file.
    Resource(L"msi", RT_RCDATA).saveToFile(msiPath);

    // Setup executor to run msiexec
    Executor msiExecutor(SysInfo::getWIPath());
    msiExecutor.arg(L"/i").arg(msiPath);
    const auto args = SysInfo::getCommandArgs();
    std::for_each(args.begin(), args.end(),
            [&msiExecutor] (const tstring& arg) {
        msiExecutor.arg(arg);
    });

    // Install msi file.
    exitCode = msiExecutor.execAndWaitForExit();
}
} // namespace


int __stdcall WinMain(HINSTANCE, HINSTANCE, LPSTR lpCmdLine, int nShowCmd) {
    app::wlaunch(std::nothrow, launchApp);
    return exitCode;
}
