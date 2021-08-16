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

#include "Resources.h"
#include "FileUtils.h"
#include "WinFileUtils.h"
#include "WinErrorHandling.h"

#include <fstream>


Resource::Resource(LPCTSTR name, LPCTSTR type, HINSTANCE module) {
    init(name, type, module);
}

Resource::Resource(UINT id, LPCTSTR type, HINSTANCE module) {
    init(MAKEINTRESOURCE(id), type, module);
}

void Resource::init(LPCTSTR name, LPCTSTR type, HINSTANCE module) {
    if (IS_INTRESOURCE(name)) {
        std::wostringstream printer;
        printer << L"#" << reinterpret_cast<size_t>(name);
        nameStr = printer.str();
        namePtr = name;
    } else {
        nameStr = name;
        namePtr = nameStr.c_str();
    }
    if (IS_INTRESOURCE(type)) {
        std::wostringstream printer;
        printer << L"#" << reinterpret_cast<size_t>(name);
        typeStr = printer.str();
        typePtr = type;
    } else {
        typeStr = type;
        typePtr = typeStr.c_str();
    }
    instance = module;
}

std::string Resource::getErrMsg(const std::string &descr) const {
    return (tstrings::any() << descr << " (name='" << nameStr <<
            "', type='" << typeStr << "')").str();
}

HRSRC Resource::findResource() const {
    LPCTSTR id = namePtr;
    // string resources are stored in blocks (stringtables)
    // id of the resource is (stringId / 16 + 1)
    if (typePtr == RT_STRING) {
        id = MAKEINTRESOURCE(UINT(size_t(id) / 16 + 1));
    }
    return FindResource(instance, id, typePtr);
}

LPVOID Resource::getPtr(DWORD &size) const
{
    // LoadString returns the same result if value is zero-length or
    // if if the value does not exists,
    // so wee need to ensure the stringtable exists
    HRSRC resInfo = findResource();
    if (resInfo == NULL) {
        JP_THROW(SysError(getErrMsg("cannot find resource"), FindResource));
    }

    HGLOBAL res = LoadResource(instance, resInfo);
    if (res == NULL) {
        JP_THROW(SysError(getErrMsg("cannot load resource"), LoadResource));
    }

    LPVOID ptr = LockResource(res);
    if (res == NULL) {
        JP_THROW(SysError(getErrMsg("cannot lock resource"), LockResource));
    }

    if (typePtr == RT_STRING) {
        // string resources are stored in stringtables and
        // need special handling
        // The simplest way (while we don't need handle resource locale)
        // is LoadString
        // But this adds dependency on user32.dll,
        // so implement custom string extraction

        // number in the block (namePtr is an integer)
        size_t num = size_t(namePtr) & 0xf;
        LPWSTR strPtr = (LPWSTR)ptr;
        for (size_t i = 0; i < num; i++) {
            // 1st symbol contains string length
            strPtr += DWORD(*strPtr) + 1;
        }
        // *strPtr contains string length, string value starts at strPtr+1
        size = DWORD(*strPtr) * sizeof(wchar_t);
        ptr = strPtr+1;
    } else {
        size = SizeofResource(instance, resInfo);
    }

    return ptr;
}

bool Resource::available() const {
    return NULL != findResource();
}

unsigned Resource::size() const {
    DWORD size = 0;
    getPtr(size);
    return size;
}

LPCVOID Resource::rawData() const {
    DWORD size = 0;
    return getPtr(size);
}

void Resource::saveToFile(const std::wstring &filePath) const {
    DWORD size = 0;
    const char *resPtr = (const char *)getPtr(size);

    FileUtils::FileWriter(filePath).write(resPtr, size).finalize();
}

Resource::ByteArray Resource::binary() const {
    DWORD size = 0;
    LPBYTE resPtr = (LPBYTE)getPtr(size);
    return ByteArray(resPtr, resPtr+size);
}


tstring StringResource::string() const
{
    DWORD size = 0;
    // string are stored as UNICODE
    LPWSTR resPtr = reinterpret_cast<LPWSTR>(impl.getPtr(size));
    // size is in bytes;
    return tstrings::fromUtf16(std::wstring(resPtr, size / sizeof(wchar_t)));
}

tstring StringResource::string(const std::nothrow_t &, const tstring &defValue) const throw()
{
    JP_TRY;
    return string();
    JP_CATCH_ALL;
    return defValue;
}
