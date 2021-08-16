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

#ifndef MsiUtils_h
#define MsiUtils_h

#include <windows.h>
#include <Msi.h>
#include <iterator>
#include <stdexcept>
#include <new>
#include <map>

#include "ErrorHandling.h"
#include "Toolbox.h"
#include "Guid.h"
#include "Flag.h"
#include "Log.h"


namespace msi {

void closeMSIHANDLE(MSIHANDLE h);

tstring getProductInfo(const Guid& productCode, const tstring& prop);

tstring getProductInfo(const std::nothrow_t&, const Guid& productCode,
                                                        const tstring& prop);

tstring getPropertyFromCustomAction(MSIHANDLE h, const tstring& prop);

tstring getPropertyFromCustomAction(const std::nothrow_t&, MSIHANDLE h,
                                                        const tstring& prop);

inline tstring getPropertyFromDeferredCustomAction(MSIHANDLE h) {
    return getPropertyFromCustomAction(h, _T("CustomActionData"));
}

inline tstring getPropertyFromDeferredCustomAction(const std::nothrow_t&,
                                                              MSIHANDLE h) {
    return getPropertyFromCustomAction(std::nothrow, h,
                                                    _T("CustomActionData"));
}


// UI level flags
class Tag {};
typedef Flag<Tag, INSTALLUILEVEL> UiModeFlag;

inline UiModeFlag defaultUI() {
    return UiModeFlag(INSTALLUILEVEL_DEFAULT);
}

inline UiModeFlag withoutUI() {
    return UiModeFlag(INSTALLUILEVEL_NONE);
}


// UI level control
struct OverrideUI {
    explicit OverrideUI(const UiModeFlag& uiMode):
        origMsiUiLevel(MsiSetInternalUI(uiMode.value(), 0)) {
    }

    ~OverrideUI() {
        MsiSetInternalUI(origMsiUiLevel, 0);
    }

private:
    const INSTALLUILEVEL origMsiUiLevel;
};

struct SuppressUI: public OverrideUI {
    SuppressUI(): OverrideUI(withoutUI()) {
    }
};


// MSI Properties (KEY=VALUE)
typedef std::pair<tstring, tstring> Property;
typedef std::vector<Property> Properties;


// Callback for MSI functions
class Callback {
public:
    virtual ~Callback() {}

    virtual void notify(INSTALLMESSAGE msgType, UINT flags,
                                                    const tstring& msg) = 0;
};


// MSI Error
class Error : public std::runtime_error {
public:
    Error(const tstrings::any& msg, UINT errorCode);
    Error(const std::string& msg, UINT errorCode);
    UINT getReason() const {
        return errorCode;
    }
private:
    UINT errorCode;
};

// "No more items" exception
class NoMoreItemsError : public Error {
public:
    NoMoreItemsError(const tstrings::any& msg)
        : Error(msg, ERROR_NO_MORE_ITEMS)
    {}
};

struct ActionData {
    typedef std::map<tstring, tstring> PropertyMap;
    PropertyMap props;
    tstring rawCmdLineArgs;
    UiModeFlag uiMode;
    Callback* callback;
    tstring logFile;

    struct State {
        virtual ~State() {}
    };

    std::unique_ptr<State> createState() const;

    tstring getCmdLineArgs() const;

    ActionData(): uiMode(withoutUI()), callback(0) {
    }
};


// MSI function execution status.
class ActionStatus {
public:
    ActionStatus(UINT value=ERROR_SUCCESS, const std::string& comment=""):
                                            value(value), comment(comment) {
    }

    explicit operator bool() const;

    UINT getValue() const {
        return value;
    }

    // Unconditionally converts this instance into msi::Error instance and
    // throws it.
    void throwIt() const;

    const std::string& getComment() const {
        return comment;
    }

private:
    std::string comment;
    UINT value;
};


// Some MSI action.
template <class T>
class action {
public:
    T& setProperty(const Property& prop) {
        data.props[prop.first] = prop.second;
        return *static_cast<T*>(this);
    }

    T& setProperty(const tstring& name, const tstring& value) {
        return setProperty(Property(name, value));
    }

    template <class It>
    T& setProperties(It b, It e) {
        std::copy(b, e, std::inserter(data.props, data.props.end()));
        return *static_cast<T*>(this);
    }

    T& setRawCmdLineArgs(const tstring& value) {
        data.rawCmdLineArgs = value;
        return *static_cast<T*>(this);
    }

    T& setUiMode(const UiModeFlag& flag) {
        data.uiMode = flag;
        return *static_cast<T*>(this);
    }

    T& setLogFile(const tstring& path=tstring()) {
        data.logFile = path;
        return *static_cast<T*>(this);
    }

    T& setCallback(Callback* cb) {
        data.callback = cb;
        return *static_cast<T*>(this);
    }

    tstring getCmdLineArgs() const {
        return data.getCmdLineArgs();
    }

    void operator () () const {
        std::unique_ptr<ActionData::State> state(data.createState());
        const ActionStatus status = execute(*static_cast<const T*>(this),
                                                        data.getCmdLineArgs());
        if (!status) {
            status.throwIt();
        }
    }

    ActionStatus operator () (const std::nothrow_t&) const {
        JP_TRY;
        std::unique_ptr<ActionData::State> state(data.createState());
        const ActionStatus status = execute(*static_cast<const T*>(this),
                                                        data.getCmdLineArgs());
        if (!status) {
            LOG_ERROR(status.getComment());
        }
        return status;
        JP_CATCH_ALL;
        return ActionStatus(ERROR_INTERNAL_ERROR, "Unknown error");
    }

private:
    static ActionStatus execute(const T& obj, const tstring& cmdLineArgs);

    ActionData data;
};


// Function object to uninstall product with the given GUID
class uninstall: public action<uninstall> {
    Guid productCode;
public:
    uninstall();

    uninstall& setProductCode(const Guid& pc) {
        productCode = pc;
        return *this;
    }

    const Guid& getProductCode() const {
        return productCode;
    }
};


// Function object to update installed product with the given GUID
class update: public action<update> {
    Guid productCode;
public:
    update& setProductCode(const Guid& pc) {
        productCode = pc;
        return *this;
    }

    const Guid& getProductCode() const {
        return productCode;
    }
};


// Function object to install package from the given msi file
class install: public action<install> {
    tstring msiPath;
public:
    install& setMsiPath(const tstring& path) {
        msiPath = path;
        return *this;
    }

    const tstring& getMsiPath() const {
        return msiPath;
    }
};


// Checks if there is some installation is in progress and waits until it completes.
// returns true if there is no installation is in progress or the installation is completed.
// returns false if timeout exceeded.
// If timeout == 0, just checks that Windows Installer service is free.
bool waitForInstallationCompletion(DWORD timeoutMS);

// Checks if there is some installation is in progress.
inline bool isInstallationInProgress() {
    return !waitForInstallationCompletion(0);
}


/**
 * Returns true if product with the given product code is installed.
 */
bool isProductInstalled(const Guid& productCode);

} // namespace msi

#endif // #ifndef MsiUtils_h
