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

#ifndef __DLL_H_INCLUDED_
#define __DLL_H_INCLUDED_

#ifdef _WIN32
#include <windows.h>
#else
typedef void* HMODULE;
#endif

#include "kludge_c++11.h"

#include <memory>
#include "tstrings.h"
#include "ErrorHandling.h"


//
// Helper classes to dynamically load DLLs and call the libraries functions.
//

/**
 * Library loader.
 * Usage:
 * - load a library specified by full path:
 *      DLL deployLib(FileUtils::combinePath(javaHome, _T("bin\\deploy.dll"));
 *
 *  Note: library should be specified by full path (due security reasons)
 *
 * - load system library (DLLs from Windows/System32 (SysWow64) directory):
 *      DLL kernel32Lib("kernel32", Dll::System());
 */
class Dll {
public:
    struct System {};

    explicit Dll(const tstrings::any &libPath);
    explicit Dll(const tstrings::any &libName, const System &tag);

    Dll(const Dll& other);

    template <class T>
    void getFunction(const tstrings::any &name, T& addr) const {
        addr = reinterpret_cast<T>(getFunction(name.str(), true));
    }

    // returns false & sets addr to NULL if the function not found
    template <class T>
    bool getFunction(const tstrings::any &name, T& addr, const std::nothrow_t &) const {
        addr = reinterpret_cast<T>(getFunction(name.str(), false));
        return addr != NULL;
    }

    const tstring& path() const {
        return thePath;
    }

    HMODULE getHandle() const {
        return handle.get();
    }

    static void freeLibrary(HMODULE h);

    struct LibraryReleaser {
        typedef HMODULE pointer;

        void operator()(HMODULE h) {
            freeLibrary(h);
        }
    };

    typedef std::unique_ptr<HMODULE, LibraryReleaser> Handle;

private:
    void* getFunction(const std::string &name, bool throwIfNotFound) const;

    tstring thePath;
    Handle handle;
};


/**
 * DllFunction template class helps to check is a library function available and call it.
 * Usage example:
 *      // RegDeleteKeyExW function (from advapi32.dll) is available on Vista+ or on WinXP 64bit
 *      // so to avoid dependency on the OS version we have to check if it's available at runtime
 *
 *      // the function definition
 *      typedef LONG (WINAPI *RegDeleteKeyExWFunc)(HKEY hKey, const wchar_t* lpSubKey, REGSAM samDesired, DWORD Reserved);
 *
 *      DllFunction<RegDeleteKeyExWFunc> _RegDeleteKeyExW(Dll("advapi32", Dll::System()), "RegDeleteKeyExW");
 *      if (_RegDeleteKeyExW.available()) {
 *          // the function is available, call it
 *          LONG result = _RegDeleteKeyExW(hKey, subkeyName, samDesired, 0);
 *      } else {
 *          // the function is not available, handle this
 *          throw std::exception("RegDeleteKeyExW function is not available");
 *      }
 *
 *      // or we can just try to call the function.
 *      // if the function is not available, exception with the corresponding description is thrown
 *      DllFunction<RegDeleteKeyExWFunc> _RegDeleteKeyExW(Dll("advapi32", Dll::System()), "RegDeleteKeyExW");
 *      LONG result = _RegDeleteKeyExW(hKey, subkeyName, samDesired, 0);
 */
template<class funcType>
class DllFunction {
public:
    DllFunction(const Dll& library, const tstrings::any &funcName)
            : lib(library), theName(funcName.str()) {
        lib.getFunction(funcName, funcPtr);
    }

    DllFunction(const std::nothrow_t&, const Dll& library,
                                                const tstrings::any &funcName)
            : lib(library), theName(funcName.str()) {
        lib.getFunction(funcName, funcPtr, std::nothrow);
    }

    bool available() const {
        return funcPtr != NULL;
    }

    std::string name() const {
        return theName;
    }

    const tstring& libPath() const {
        return lib.path();
    }

    operator funcType() const {
        if (!available()) {
            JP_THROW(tstrings::any()    << theName
                                        << "() function is not available in "
                                        << lib.path());
        }
        return funcPtr;
    }

private:
    const Dll lib;
    funcType funcPtr;
    std::string theName;
};

#endif // __DLL_H_INCLUDED_
