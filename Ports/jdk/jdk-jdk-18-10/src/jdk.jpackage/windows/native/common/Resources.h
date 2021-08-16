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

#ifndef RESOURCES_H
#define RESOURCES_H

#include "WinSysInfo.h"


class StringResource;

/**
 * Classes for resource loading.
 * Common use cases:
 *  - check if resource is available and save it to file:
 *      Resource res(_T("MyResource"), _T("CustomResourceType"));
 *      if (res.available()) {
 *          res.saveToFile(_T("c:\\temp\\my_resource.bin"));
 *      }
 *
 *  - get string resource:
 *      1) if the resource is not available, exception is thrown:
 *          tstring str = StringResource(MAKEINTRESOURCE(resID)).string();
 *
 *      2) nothrow method (returns default value if the resource is not available):
 *          a) returns empty string on error:
 *              tstring str = StringResource(MAKEINTRESOURCE(resID)).string(std::nothrow);
 *
 *          b) returns provided default value on error:
 *              tstring str = StringResource(MAKEINTRESOURCE(resID)).string(std::nothrow, _T("defaultValue"));
 */

class Resource {
public:
    // name and type can be specified by string id,
    // by integer id (RT_* constants or MAKEINTRESOURCE)
    Resource(LPCWSTR name, LPCWSTR type,
            HINSTANCE module = SysInfo::getCurrentModuleHandle());
    Resource(UINT id, LPCWSTR type,
            HINSTANCE module = SysInfo::getCurrentModuleHandle());

    bool available() const;

    // all this methods throw exception if the resource is not available
    unsigned size() const;
    // gets raw pointer to the resource data
    LPCVOID rawData() const;

    // save the resource to a file
    void saveToFile(const std::wstring &filePath) const;

    typedef std::vector<BYTE> ByteArray;
    // returns the resource as byte array
    ByteArray binary() const;

    friend class StringResource;

private:
    std::wstring nameStr;
    LPCWSTR namePtr;    // can be integer value or point to nameStr.c_str()
    std::wstring typeStr;
    LPCWSTR typePtr;    // can be integer value or point to nameStr.c_str()
    HINSTANCE instance;

    void init(LPCWSTR name, LPCWSTR type, HINSTANCE module);

    // generates error message
    std::string getErrMsg(const std::string &descr) const;
    HRSRC findResource() const;
    LPVOID getPtr(DWORD &size) const;

private:
    // disable copying
    Resource(const Resource&);
    Resource& operator = (const Resource&);
};


// Note: string resources are returned utf16 or utf8 encoded.
// To get Windows-encoded string (utf16/ACP) use tstrings::toWinString().
class StringResource {
public:
    // string resource is always identified by integer id
    StringResource(UINT resourceId, HINSTANCE moduleHandle = SysInfo::getCurrentModuleHandle())
        : impl(resourceId, RT_STRING, moduleHandle) {}

    // returns the resource as string
    tstring string() const;
    // nothrow version (logs error)
    tstring string(const std::nothrow_t &, const tstring &defValue = tstring()) const throw();

    bool available() const throw() {
        return impl.available();
    }

    unsigned size() const {
        return impl.size();
    }

    static tstring load(UINT resourceId,
                    HINSTANCE moduleHandle = SysInfo::getCurrentModuleHandle()) {
        return StringResource(resourceId, moduleHandle).string();
    }

    static tstring load(const std::nothrow_t &, UINT resourceId,
                    HINSTANCE moduleHandle = SysInfo::getCurrentModuleHandle()) throw () {
        return StringResource(resourceId, moduleHandle).string(std::nothrow);
    }

private:
    Resource impl;
};

#endif // RESOURCES_H
