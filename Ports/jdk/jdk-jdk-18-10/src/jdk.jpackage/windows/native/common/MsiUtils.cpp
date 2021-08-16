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

#include "MsiUtils.h"
#include "MsiDb.h"
#include "Resources.h"
#include "Dll.h"
#include "UniqueHandle.h"
#include "FileUtils.h"
#include "WinErrorHandling.h"


#pragma comment(lib, "msi.lib")


namespace msi {

namespace {

template <class Func, class Arg1Type, class Arg2Type>
tstring getProperty(Func func, const LPCSTR funcName, Arg1Type arg1,
                                                            Arg2Type arg2) {

    std::vector<TCHAR> buf(20);
    DWORD size = static_cast<DWORD>(buf.size());

    UINT status = ERROR_MORE_DATA;
    while (ERROR_MORE_DATA ==
                    (status = func(arg1, arg2, &*buf.begin(), &size))) {
        buf.resize(buf.size() * 2);
        size = static_cast<DWORD>(buf.size());
    }

    if (status != ERROR_SUCCESS) {
        JP_THROW(Error(tstrings::any() << funcName << "(" << arg1
                                    << ", " << arg2 << ") failed", status));
    }
    return tstring(buf.begin(), buf.begin() + size);
}

template <class Func, class Arg1Type, class Arg2Type>
tstring getProperty(const std::nothrow_t&, Func func, const LPCSTR funcName,
                                                Arg1Type arg1, Arg2Type arg2) {
    try {
        return getProperty(func, funcName, arg1, arg2);
    } catch (const std::exception&) {
    }
    return tstring();
}


tstring escapePropertyValue(const tstring& value) {
    // Escape quotes as described in
    // http://msdn.microsoft.com/en-us/library/aa367988.aspx
    tstring reply = tstrings::replace(value, _T("\""), _T("\"\""));

    if (reply.empty()) {
        // MSDN: To clear a public property by using the command line,
        //       set its value to an empty string.
        reply = _T("\"\"");
    }

    if (reply.find_first_of(_T(" \t")) != tstring::npos) {
        reply = _T('"') + reply + _T('"');
    }

    return reply;
}

template <class It>
tstring stringifyProperties(It b, It e) {
    tostringstream buf;
    for (; b != e; ++b) {
        const tstring value = escapePropertyValue(b->second);
        buf << _T(" ") << b->first << _T("=") << value;
    }

    tstring reply = tstrings::trim(buf.str());
    return reply;
}


class CallbackTrigger {
    CallbackTrigger(const CallbackTrigger&);
    CallbackTrigger& operator=(const CallbackTrigger&);

    enum { MESSAGE_FILTER = 0xffffffff };

    static int WINAPI adapter(LPVOID ctx, UINT type, LPCWSTR msg) {
        Callback* callback = reinterpret_cast<Callback*>(ctx);
        if (!callback) {
            return 0;
        }

        JP_TRY;

        // MSDN: Handling Progress Messages Using MsiSetExternalUI
        // http://msdn.microsoft.com/en-us/library/aa368786(v=vs.85).aspx
        const INSTALLMESSAGE mt = (INSTALLMESSAGE)(0xFF000000 & type);
        const UINT flags = 0x00FFFFFF & type;

        if (msg) {
            callback->notify(mt, flags, tstrings::toWinString(msg));
        }

        JP_CATCH_ALL;

        return 0;
    }

public:
    explicit CallbackTrigger(Callback& cb) {
        MsiSetExternalUIW(adapter, DWORD(MESSAGE_FILTER), &cb);
    }

    ~CallbackTrigger() {
        // Not restoring the original callback.
        // Just because the original message filter is unknown.
        MsiSetExternalUIW(0, 0, 0);
    }
};

class LogFileTrigger {
    LogFileTrigger(const LogFileTrigger&);
    LogFileTrigger& operator=(const LogFileTrigger&);

public:
    explicit LogFileTrigger(const tstring& path) {
        if (path.empty()) {
            MsiEnableLog(0, NULL, 0);
        } else {
            MsiEnableLog(INSTALLLOGMODE_VERBOSE, path.c_str(), 0);
        }
    }

    ~LogFileTrigger() {
        // Disable log
        MsiEnableLog(0, NULL, 0);
    }
};

struct SuppressUI: public OverrideUI {
    SuppressUI(): OverrideUI(withoutUI()) {
    }
};

class StateImpl: public ActionData::State {
    const OverrideUI overrideUi;
    LogFileTrigger logGuard;
    std::unique_ptr<CallbackTrigger> callbackGuard;

public:
    explicit StateImpl(const ActionData& data): overrideUi(data.uiMode),
                                                    logGuard(data.logFile) {
        if (data.callback) {
            callbackGuard = std::unique_ptr<CallbackTrigger>(
                                        new CallbackTrigger(*data.callback));
        }
    }
};

} // namespace


void closeMSIHANDLE(MSIHANDLE h) {
    if (h) {
        const auto status = MsiCloseHandle(h);
        if (status != ERROR_SUCCESS) {
            LOG_WARNING(tstrings::any() << "MsiCloseHandle("
                                << h << ") failed with error=" << status);
        }
    }
}


// DatabaseRecord::getString() should live in MsiDb.cpp.
// However it can't access handy msi::getProperty() from that location.
tstring DatabaseRecord::getString(unsigned idx) const {
    return ::msi::getProperty(MsiRecordGetString, "MsiRecordGetString",
                                                    handle, UINT(idx));
}


tstring getProductInfo(const Guid& productCode, const tstring& prop) {
    const tstring id = productCode.toMsiString();
    return getProperty(MsiGetProductInfo, "MsiGetProductInfo", id.c_str(),
                                                                prop.c_str());
}


tstring getProductInfo(const std::nothrow_t&, const Guid& productCode,
                                                        const tstring& prop) {
    const tstring id = productCode.toMsiString();
    return getProperty(std::nothrow, MsiGetProductInfo, "MsiGetProductInfo",
                                                    id.c_str(), prop.c_str());
}


tstring getPropertyFromCustomAction(MSIHANDLE h, const tstring& prop) {
    return getProperty(MsiGetProperty, "MsiGetProperty", h, prop.c_str());
}


tstring getPropertyFromCustomAction(const std::nothrow_t&, MSIHANDLE h,
                                                        const tstring& prop) {
    return getProperty(std::nothrow, MsiGetProperty,"MsiGetProperty", h,
                                                                prop.c_str());
}


namespace {
std::string makeMessage(const std::string& msg, UINT errorCode) {
    std::ostringstream err;
    err << "MSI error [" << errorCode << "]";

    const std::wstring msimsg_dll = tstrings::winStringToUtf16(FileUtils::combinePath(
                                                SysInfo::getSystem32Dir(), _T("msimsg.dll")));

    // Convert MSI Error Code to Description String
    // http://msdn.microsoft.com/en-us/library/aa370315(v=vs.85).aspx
    Dll::Handle lib(LoadLibraryExW(msimsg_dll.c_str(), NULL,
                                                LOAD_LIBRARY_AS_DATAFILE));
    if (!lib.get()) {
        JP_THROW(SysError(tstrings::any() << "LoadLibraryExW(" <<
                                            msimsg_dll << ") failed", LoadLibraryExW));
    } else {
        tstring descr;
        try {
            descr = StringResource(errorCode, lib.get()).string();
        } catch (const std::exception &) {
            descr = _T("No description");
        }
        err << "(" << descr << ")";
    }

    return joinErrorMessages(msg, err.str());
}
} // namespace

Error::Error(const tstrings::any& msg, UINT ec): std::runtime_error(
                                makeMessage(msg.str(), ec)), errorCode(ec) {
}


Error::Error(const std::string& msg, UINT ec): std::runtime_error(
                                        makeMessage(msg, ec)), errorCode(ec) {
}


tstring ActionData::getCmdLineArgs() const {
    tstring raw = tstrings::trim(rawCmdLineArgs);
    tstring strProperties = stringifyProperties(props.begin(), props.end());
    if (!raw.empty() && !strProperties.empty()) {
        raw += _T(' ');
    }
    return raw + strProperties;
}


std::unique_ptr<ActionData::State> ActionData::createState() const {
    return std::unique_ptr<ActionData::State>(new StateImpl(*this));
}


namespace {

bool isMsiStatusSuccess(const UINT status) {
    switch (status) {
    case ERROR_SUCCESS:
    case ERROR_SUCCESS_REBOOT_INITIATED:
    case ERROR_SUCCESS_REBOOT_REQUIRED:
        return true;
    default:
        break;
    }
    return false;
}

ActionStatus handleMsiStatus(tstrings::any& logMsg, const UINT status) {
    if (!isMsiStatusSuccess(status)) {
        logMsg << "failed [" << status << "]";
        return ActionStatus(status, logMsg.str());
    }

    logMsg << "succeeded";
    if (status != ERROR_SUCCESS) {
        logMsg << " [" << status << "]";
    }
    LOG_INFO(logMsg);
    return ActionStatus(status);
}

} // namespace


ActionStatus::operator bool() const {
    return isMsiStatusSuccess(value);
}


void ActionStatus::throwIt() const {
    JP_THROW(Error(comment, value));
}


namespace {
template <class T>
ActionStatus msiAction(const T& obj, INSTALLSTATE state,
                                                const tstring& cmdLineArgs) {
    const tstring id = obj.getProductCode().toMsiString();
    const int level = INSTALLLEVEL_MAXIMUM;
    const UINT status = MsiConfigureProductEx(id.c_str(), level, state,
                                                        cmdLineArgs.c_str());

    tstrings::any logMsg;
    logMsg  << "MsiConfigureProductEx("
            << id
            << ", " << level
            << ", " << state
            << ", " << cmdLineArgs
            << ") ";

    return handleMsiStatus(logMsg, status);
}
} // namespace


template <>
ActionStatus action<uninstall>::execute(const uninstall& obj,
                                                const tstring& cmdLineArgs) {
    return msiAction(obj, INSTALLSTATE_ABSENT, cmdLineArgs);
}


template <>
ActionStatus action<update>::execute(const update& obj,
                                                const tstring& cmdLineArgs) {
    return msiAction(obj, INSTALLSTATE_LOCAL, cmdLineArgs);
}


template <>
ActionStatus action<install>::execute(const install& obj,
                                                const tstring& cmdLineArgs) {
    const tstring& msiPath = obj.getMsiPath();

    const UINT status = MsiInstallProduct(msiPath.c_str(),
                                                        cmdLineArgs.c_str());

    tstrings::any logMsg;
    logMsg  << "MsiInstallProduct(" << msiPath << ", " << cmdLineArgs << ") ";

    return handleMsiStatus(logMsg, status);
}


uninstall::uninstall() {
    // Uninstall default behavior is to never reboot.
    setProperty(_T("REBOOT"), _T("ReallySuppress"));
}


bool waitForInstallationCompletion(DWORD timeoutMS)
{
    // "_MSIExecute" mutex is used by the MSI installer service to prevent multiple installations at the same time
    // http://msdn.microsoft.com/en-us/library/aa372909(VS.85).aspx
    LPCTSTR mutexName = _T("Global\\_MSIExecute");
    UniqueHandle h(OpenMutex(SYNCHRONIZE, FALSE, mutexName));
    if (h.get() != NULL) {
        DWORD res = WaitForSingleObject(h.get(), timeoutMS);
        // log only if timeout != 0
        if (timeoutMS != 0) {
            LOG_INFO(tstrings::any() << "finish waiting for mutex: " << res);
        }
        if (res == WAIT_TIMEOUT) {
            return false;
        }
    }
    return true;
}


bool isProductInstalled(const Guid& productCode) {
    // Query any property. If product exists, query should succeed.
    try {
        getProductInfo(productCode, INSTALLPROPERTY_VERSIONSTRING);
    } catch (const Error& e) {
        switch (e.getReason()) {
        case ERROR_UNKNOWN_PRODUCT:
        // if the application being queried is advertised and not installed.
        case ERROR_UNKNOWN_PROPERTY:
            return false;
        }
    }
    return true;
}

} // namespace msi
