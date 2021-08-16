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


#ifndef WinErrorHandling_h
#define WinErrorHandling_h


#include "ErrorHandling.h"


class SysError : public std::runtime_error {
public:
    SysError(const tstrings::any& msg, const void* caller,
            DWORD errorCode=GetLastError(), const char* label="System error");

    // returns string "system error <errCode> (error_description)"
    // in UNICODE is not defined, the string returned is utf8-encoded
    static std::wstring getSysErrorMessage(DWORD errCode = GetLastError(),
            HMODULE moduleHandle = NULL);

    // returns string "COM error 0x<hr> (error_description)"
    // in UNICODE is not defined, the string returned is utf8-encoded
    static std::wstring getComErrorMessage(HRESULT hr);
};

#endif // #ifndef WinErrorHandling_h
