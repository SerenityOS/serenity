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

#include <io.h>
#include <fcntl.h>
#include <windows.h>

#include "AppLauncher.h"
#include "JvmLauncher.h"
#include "Log.h"
#include "Dll.h"
#include "WinApp.h"
#include "Toolbox.h"
#include "FileUtils.h"
#include "UniqueHandle.h"
#include "ErrorHandling.h"
#include "WinSysInfo.h"
#include "WinErrorHandling.h"


// AllowSetForegroundWindow
#pragma comment(lib, "user32")


namespace {

std::unique_ptr<Dll> loadDllWithAlteredPATH(const tstring& dllFullPath) {
    LOG_TRACE_FUNCTION();

    const tstring vanillaPathEnvVariable = SysInfo::getEnvVariable(_T("PATH"));

    tstring pathEnvVariable = vanillaPathEnvVariable
            + _T(";")
            + FileUtils::dirname(dllFullPath);

    SysInfo::setEnvVariable(_T("PATH"), pathEnvVariable);

    LOG_TRACE(tstrings::any() << "New value of PATH: " << pathEnvVariable);

    // Schedule restore of PATH after attempt to load the given dll
    const auto resetPATH = runAtEndOfScope([&vanillaPathEnvVariable]() -> void {
        SysInfo::setEnvVariable(_T("PATH"), vanillaPathEnvVariable);
    });

    return std::unique_ptr<Dll>(new Dll(dllFullPath));
}

std::unique_ptr<Dll> loadDllWithAddDllDirectory(const tstring& dllFullPath) {
    LOG_TRACE_FUNCTION();

    const tstring dirPath = FileUtils::dirname(dllFullPath);

    typedef DLL_DIRECTORY_COOKIE(WINAPI *AddDllDirectoryFunc)(PCWSTR);

    DllFunction<AddDllDirectoryFunc> _AddDllDirectory(
            Dll("kernel32.dll", Dll::System()), "AddDllDirectory");

    AddDllDirectoryFunc func = _AddDllDirectory;
    DLL_DIRECTORY_COOKIE res = func(dirPath.c_str());
    if (!res) {
        JP_THROW(SysError(tstrings::any()
                << "AddDllDirectory(" << dirPath << ") failed", func));
    }

    LOG_TRACE(tstrings::any() << "AddDllDirectory(" << dirPath << "): OK");

    // Important: use LOAD_LIBRARY_SEARCH_DEFAULT_DIRS flag,
    // but not LOAD_LIBRARY_SEARCH_USER_DIRS!
    HMODULE dllHandle = LoadLibraryEx(dllFullPath.c_str(), NULL,
            LOAD_LIBRARY_SEARCH_DEFAULT_DIRS);

    LOG_TRACE(tstrings::any() << "LoadLibraryEx(" << dllFullPath
            << ", LOAD_LIBRARY_SEARCH_DEFAULT_DIRS): " << dllHandle);

    const auto freeDll = runAtEndOfScope([&dllHandle]() -> void {
        Dll::freeLibrary(dllHandle);
    });

    return std::unique_ptr<Dll>(new Dll(dllFullPath));
}


class DllWrapper {
public:
    DllWrapper(const tstring& dllName) {
        try {
            // Adjust the DLL search paths with AddDllDirectory() WINAPI CALL
            dll = loadDllWithAddDllDirectory(dllName);
        } catch (const std::exception&) {
            // Alter PATH environment variable as the last resort.
            dll = loadDllWithAlteredPATH(dllName);
        }
    }

private:
    DllWrapper(const DllWrapper&);
    DllWrapper& operator=(const DllWrapper&);

private:
    std::unique_ptr<Dll> dll;
};


tstring getJvmLibPath(const Jvm& jvm) {
    FileUtils::mkpath path;

    path << FileUtils::dirname(jvm.getPath()) << _T("server") << _T("jvm.dll");

    return path;
}


void launchApp() {
    // [RT-31061] otherwise UI can be left in back of other windows.
    ::AllowSetForegroundWindow(ASFW_ANY);

    const tstring launcherPath = SysInfo::getProcessModulePath();
    const tstring appImageRoot = FileUtils::dirname(launcherPath);
    const tstring runtimeBinPath = FileUtils::mkpath()
            << appImageRoot << _T("runtime") << _T("bin");

    std::unique_ptr<Jvm> jvm(AppLauncher()
        .setImageRoot(appImageRoot)
        .addJvmLibName(_T("bin\\jli.dll"))
        .setAppDir(FileUtils::mkpath() << appImageRoot << _T("app"))
        .setLibEnvVariableName(_T("PATH"))
        .setDefaultRuntimePath(FileUtils::mkpath() << appImageRoot
                << _T("runtime"))
        .createJvmLauncher());

    // zip.dll may be loaded by java without full path
    // make sure it will look in runtime/bin
    SetDllDirectory(runtimeBinPath.c_str());

    const DllWrapper jliDll(jvm->getPath());
    std::unique_ptr<DllWrapper> splashDll;
    if (jvm->isWithSplash()) {
        const DllWrapper jvmDll(getJvmLibPath(*jvm));
        splashDll = std::unique_ptr<DllWrapper>(new DllWrapper(
                FileUtils::mkpath()
                        << FileUtils::dirname(jvm->getPath())
                        << _T("splashscreen.dll")));
    }

    jvm->launch();
}

} // namespace

#ifndef JP_LAUNCHERW

int __cdecl  wmain() {
    return app::launch(std::nothrow, launchApp);
}

#else

int __stdcall wWinMain(HINSTANCE, HINSTANCE, LPWSTR, int) {
    return app::wlaunch(std::nothrow, launchApp);
}

#endif
