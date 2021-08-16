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

#include <cstring>
#include "PlatformLogEvent.h"
#include "FileUtils.h"
#include "Log.h"


namespace {

    tstring retrieveModuleName() {
        try {
            return FileUtils::basename(SysInfo::getCurrentModulePath());
        }
        catch (const std::runtime_error&) {
            return _T("Unknown");
        }
    }

    TCHAR moduleName[MAX_PATH] = { 'U', 'n', 'k', 'n', 'o', 'w', 'n', TCHAR(0) };

    const LPCTSTR formatStr = _T("%04u/%02u/%02u %02u:%02u:%02u.%03u, %s (PID: %u, TID: %u), ");

} // namespace


PlatformLogEvent::PlatformLogEvent() {
    std::memset(static_cast<void*>(this), 0, sizeof(*this));
}


void LogEvent::init(PlatformLogEvent& logEvent) {
    GetLocalTime(&logEvent.ts);
    logEvent.pid = GetCurrentProcessId();
    logEvent.tid = GetCurrentThreadId();
    logEvent.moduleName = ::moduleName;
}


void LogEvent::appendFormatted(const PlatformLogEvent& logEvent,
        tstring& buffer) {
    const tstring str = tstrings::unsafe_format(formatStr,
        unsigned(logEvent.ts.wYear),
        unsigned(logEvent.ts.wMonth),
        unsigned(logEvent.ts.wDay),
        unsigned(logEvent.ts.wHour),
        unsigned(logEvent.ts.wMinute),
        unsigned(logEvent.ts.wSecond),
        unsigned(logEvent.ts.wMilliseconds),
        logEvent.moduleName,
        logEvent.pid,
        logEvent.tid);
    buffer.append(str);
}


void Logger::initializingLogging() {
    moduleName[0] = TCHAR(0);
}


void Logger::initializeLogging() {
    tstring mname = retrieveModuleName();
    mname.resize(_countof(moduleName) - 1);
    std::memcpy(moduleName, mname.c_str(), mname.size());
    moduleName[mname.size()] = TCHAR(0);
}
