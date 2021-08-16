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

#include "WinErrorHandling.h"
#include "Log.h"
#include "SysInfo.h"
#include "FileUtils.h"


namespace {

std::string makeMessage(const std::string& msg, const char* label,
                                            const void* c, DWORD errorCode) {
    std::ostringstream err;
    err << (label ? label : "Some error") << " [" << errorCode << "]";

    HMODULE hmodule = NULL;
    if (c) {
        GetModuleHandleEx(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS
                | GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT,
                reinterpret_cast<LPCTSTR>(c), &hmodule);

        if (!hmodule) {
            LOG_WARNING(tstrings::any() << "GetModuleHandleEx() failed for "
                    << c << " address.");
        }
    }
    if (hmodule || !c) {
        err << "(" << SysError::getSysErrorMessage(errorCode, hmodule) << ")";
    }

    return joinErrorMessages(msg, err.str());
}


std::wstring getSystemMessageDescription(DWORD messageId, HMODULE moduleHandle) {
    LPWSTR pMsg = NULL;
    std::wstring descr;

    // we always retrieve UNICODE description from system,
    // convert it to utf8 if UNICODE is not defined

    while (true) {
        DWORD res = FormatMessageW(FORMAT_MESSAGE_ALLOCATE_BUFFER
                | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS
                | (moduleHandle != NULL ? FORMAT_MESSAGE_FROM_HMODULE : 0),
                moduleHandle, messageId, 0, (LPWSTR)&pMsg, 0, NULL);
        if (res > 0) {
            // replace all non-printed chars with space
            for (DWORD i=0; i<res; i++) {
                if (pMsg[i] < L' ') {
                    pMsg[i] = L' ';
                }
            }
            // trim right (spaces and dots)
            for (DWORD i=res; i>0; i--) {
                if (pMsg[i] > L' ' && pMsg[i] != L'.') {
                    break;
                }
                pMsg[i] = 0;
            }

            descr = pMsg;

            LocalFree(pMsg);
        } else {
            // if we fail to get description for specific moduleHandle,
            // try to get "common" description.
            if (moduleHandle != NULL) {
                moduleHandle = NULL;
                continue;
            }
            descr = L"No description available";
        }
        break;
    }

    return descr;
}

} // namespace


SysError::SysError(const tstrings::any& msg, const void* caller, DWORD ec,
            const char* label):
        std::runtime_error(makeMessage(msg.str(), label, caller, ec)) {
}

std::wstring SysError::getSysErrorMessage(DWORD errCode, HMODULE moduleHandle) {
    tstrings::any msg;
    msg << "system error " << errCode
        << " (" << getSystemMessageDescription(errCode, moduleHandle) << ")";
    return msg.tstr();
}

std::wstring SysError::getComErrorMessage(HRESULT hr) {
    HRESULT hrOrig = hr;
    // for FACILITY_WIN32 facility we need to reset hiword
    if(HRESULT_FACILITY(hr) == FACILITY_WIN32) {
        hr = HRESULT_CODE(hr);
    }
    return tstrings::format(_T("COM error 0x%08X (%s)"), hrOrig,
            getSystemMessageDescription(hr, NULL));
}
