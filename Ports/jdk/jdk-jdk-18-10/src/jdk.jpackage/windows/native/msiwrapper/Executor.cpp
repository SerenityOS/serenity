/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "Executor.h"
#include "Log.h"
#include "WinErrorHandling.h"


namespace {

void escapeArg(std::wstring& str) {
    if (str.empty()) {
        return;
    }

    if (str.front() == L'\"' && str.back() == L'\"' && str.size() > 1) {
        return;
    }

    if (str.find_first_of(L" \t") != std::wstring::npos) {
        str = L'"' + str + L'"';
    }
}

} // namespace


std::wstring Executor::args() const {
    tstring_array tmpArgs;
    // argv[0] is the module name.
    tmpArgs.push_back(appPath);
    tmpArgs.insert(tmpArgs.end(), argsArray.begin(), argsArray.end());

    std::for_each(tmpArgs.begin(), tmpArgs.end(), escapeArg);
    return tstrings::join(tmpArgs.begin(), tmpArgs.end(), _T(" "));
}


int Executor::execAndWaitForExit() const {
    UniqueHandle h = startProcess();

    const DWORD res = ::WaitForSingleObject(h.get(), INFINITE);
    if (WAIT_FAILED ==  res) {
        JP_THROW(SysError("WaitForSingleObject() failed", WaitForSingleObject));
    }

    DWORD exitCode = 0;
    if (!GetExitCodeProcess(h.get(), &exitCode)) {
        // Error reading process's exit code.
        JP_THROW(SysError("GetExitCodeProcess() failed", GetExitCodeProcess));
    }

    const DWORD processId = GetProcessId(h.get());
    if (!processId) {
        JP_THROW(SysError("GetProcessId() failed.", GetProcessId));
    }

    LOG_TRACE(tstrings::any() << "Process with PID=" << processId
                                << " terminated. Exit code=" << exitCode);

    return static_cast<int>(exitCode);
}


UniqueHandle Executor::startProcess() const {
    const std::wstring argsStr = args();

    std::vector<TCHAR> argsBuffer(argsStr.begin(), argsStr.end());
    argsBuffer.push_back(0); // terminating '\0'

    STARTUPINFO startupInfo;
    ZeroMemory(&startupInfo, sizeof(startupInfo));
    startupInfo.cb = sizeof(startupInfo);

    PROCESS_INFORMATION processInfo;
    ZeroMemory(&processInfo, sizeof(processInfo));

    DWORD creationFlags = 0;

    if (!theVisible) {
        // For GUI applications.
        startupInfo.dwFlags |= STARTF_USESHOWWINDOW;
        startupInfo.wShowWindow = SW_HIDE;

        // For console applications.
        creationFlags |= CREATE_NO_WINDOW;
    }

    tstrings::any msg;
    msg << "CreateProcess(" << appPath << ", " << argsStr << ")";

    if (!CreateProcess(appPath.c_str(), argsBuffer.data(), NULL, NULL, FALSE,
                    creationFlags, NULL, NULL, &startupInfo, &processInfo)) {
        msg << " failed";
        JP_THROW(SysError(msg, CreateProcess));
    }

    msg << " succeeded; PID=" << processInfo.dwProcessId;
    LOG_TRACE(msg);

    // Close unneeded handles immediately.
    UniqueHandle(processInfo.hThread);

    // Return process handle.
    return UniqueHandle(processInfo.hProcess);
}
