/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <windows.h>
#include <shellapi.h>

#include "WinSysInfo.h"
#include "FileUtils.h"
#include "WinErrorHandling.h"

#pragma comment(lib, "Shell32")

namespace SysInfo {

tstring getTempDir() {
    std::vector<TCHAR> buffer(MAX_PATH);
    DWORD res = GetTempPath(static_cast<DWORD>(buffer.size()), buffer.data());
    if (res > buffer.size()) {
        buffer.resize(res);
        GetTempPath(static_cast<DWORD>(buffer.size()), buffer.data());
    }
    return FileUtils::removeTrailingSlash(buffer.data());
}

namespace {

template <class Func>
tstring getSystemDirImpl(Func func, const std::string& label) {
    std::vector<TCHAR> buffer(MAX_PATH);
    for (int i=0; i<2; i++) {
        DWORD res = func(buffer.data(), static_cast<DWORD>(buffer.size()));
        if (!res) {
            JP_THROW(SysError(label + " failed", func));
        }
        if (res < buffer.size()) {
            return FileUtils::removeTrailingSlash(buffer.data());
        }
        buffer.resize(res + 1);
    }
    JP_THROW("Unexpected reply from" + label);
}

} // namespace

tstring getSystem32Dir() {
    return getSystemDirImpl(GetSystemDirectory, "GetSystemDirectory");
}

tstring getWIPath() {
    return FileUtils::mkpath() << getSystem32Dir() << _T("msiexec.exe");
}

namespace {

tstring getModulePath(HMODULE h)
{
    std::vector<TCHAR> buf(MAX_PATH);
    DWORD len = 0;
    while (true) {
        len = GetModuleFileName(h, buf.data(), (DWORD)buf.size());
        if (len < buf.size()) {
            break;
        }
        // buffer is too small, increase it
        buf.resize(buf.size() * 2);
    }

    if (len == 0) {
        // error occured
        JP_THROW(SysError("GetModuleFileName failed", GetModuleFileName));
    }
    return tstring(buf.begin(), buf.begin() + len);
}

} // namespace

tstring getProcessModulePath() {
    return FileUtils::toAbsolutePath(getModulePath(NULL));
}

HMODULE getCurrentModuleHandle()
{
    // get module handle for the address of this function
    LPCWSTR address = reinterpret_cast<LPCWSTR>(getCurrentModuleHandle);
    HMODULE hmodule = NULL;
    if (!GetModuleHandleExW(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS
            | GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT, address, &hmodule))
    {
        JP_THROW(SysError(tstrings::any() << "GetModuleHandleExW failed",
                GetModuleHandleExW));
    }
    return hmodule;
}

void setEnvVariable(const tstring& name, const tstring& value)
{
    if (!SetEnvironmentVariable(name.c_str(), value.c_str())) {
        JP_THROW(SysError(tstrings::any()
                << "SetEnvironmentVariable("
                << name << ", " << value
                << ") failed", SetEnvironmentVariable));
    }

    if (0 != _tputenv_s(name.c_str(), value.c_str())) {
        JP_THROW(tstrings::any() << "_tputenv_s(" << name << ", " << value << ") failed: " << lastCRTError());
    }
}

tstring getCurrentModulePath()
{
    return getModulePath(getCurrentModuleHandle());
}

tstring_array getCommandArgs(CommandArgProgramNameMode progNameMode)
{
    int argc = 0;
    tstring_array result;

    LPWSTR *parsedArgs = CommandLineToArgvW(GetCommandLineW(), &argc);
    if (parsedArgs == NULL) {
        JP_THROW(SysError("CommandLineToArgvW failed", CommandLineToArgvW));
    }
    // the 1st element contains program name
    for (int i = progNameMode == ExcludeProgramName ? 1 : 0; i < argc; i++) {
        result.push_back(parsedArgs[i]);
    }
    LocalFree(parsedArgs);

    return result;
}

namespace {

tstring getEnvVariableImpl(const tstring& name, bool* errorOccured=0) {
    std::vector<TCHAR> buf(10);
    SetLastError(ERROR_SUCCESS);
    const DWORD size = GetEnvironmentVariable(name.c_str(), buf.data(),
                                                            DWORD(buf.size()));
    if (GetLastError() == ERROR_ENVVAR_NOT_FOUND) {
        if (errorOccured) {
            *errorOccured = true;
            return tstring();
        }
        JP_THROW(SysError(tstrings::any() << "GetEnvironmentVariable("
            << name << ") failed. Variable not set", GetEnvironmentVariable));
    }

    if (size > buf.size()) {
        buf.resize(size);
        GetEnvironmentVariable(name.c_str(), buf.data(), DWORD(buf.size()));
        if (GetLastError() != ERROR_SUCCESS) {
            if (errorOccured) {
                *errorOccured = true;
                return tstring();
            }
            JP_THROW(SysError(tstrings::any() << "GetEnvironmentVariable("
                            << name << ") failed", GetEnvironmentVariable));
        }
    }

    if (errorOccured) {
        *errorOccured = false;
    }
    return tstring(buf.data());
}

} // namespace

tstring getEnvVariable(const tstring& name) {
    return getEnvVariableImpl(name);
}

tstring getEnvVariable(const std::nothrow_t&, const tstring& name,
                                                    const tstring& defValue) {
    bool errorOccured = false;
    const tstring reply = getEnvVariableImpl(name, &errorOccured);
    if (errorOccured) {
        return defValue;
    }
    return reply;
}

bool isEnvVariableSet(const tstring& name) {
    TCHAR unused[1];
    SetLastError(ERROR_SUCCESS);
    GetEnvironmentVariable(name.c_str(), unused, _countof(unused));
    return GetLastError() != ERROR_ENVVAR_NOT_FOUND;
}

} // end of namespace SysInfo
