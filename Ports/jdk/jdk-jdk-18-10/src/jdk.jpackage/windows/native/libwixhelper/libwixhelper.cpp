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

#include <Windows.h>
#include <msiquery.h>
#include <shlwapi.h>

extern "C" {

#ifdef JP_EXPORT_FUNCTION
#error Unexpected JP_EXPORT_FUNCTION define
#endif
#define JP_EXPORT_FUNCTION comment(linker, "/EXPORT:" __FUNCTION__ "=" __FUNCDNAME__)

    BOOL WINAPI DllMain(HINSTANCE hInst, ULONG ulReason,
            LPVOID lpvReserved) {
        return TRUE;
    }

    BOOL DirectoryExist(TCHAR *szValue) {
        DWORD attr = GetFileAttributes(szValue);
        if (attr == INVALID_FILE_ATTRIBUTES) {
            return FALSE;
        }

        if (attr & FILE_ATTRIBUTE_DIRECTORY) {
            return TRUE;
        }

        return FALSE;
    }

    UINT __stdcall CheckInstallDir(MSIHANDLE hInstall) {
        #pragma JP_EXPORT_FUNCTION

        TCHAR *szValue = NULL;
        DWORD cchSize = 0;

        UINT result = MsiGetProperty(hInstall, TEXT("INSTALLDIR"),
                TEXT(""), &cchSize);
        if (result == ERROR_MORE_DATA) {
            cchSize = cchSize + 1; // NULL termination
            szValue = new TCHAR[cchSize];
            if (szValue) {
                result = MsiGetProperty(hInstall, TEXT("INSTALLDIR"),
                        szValue, &cchSize);
            } else {
                return ERROR_INSTALL_FAILURE;
            }
        }

        if (result != ERROR_SUCCESS) {
            delete [] szValue;
            return ERROR_INSTALL_FAILURE;
        }

        if (DirectoryExist(szValue)) {
            if (PathIsDirectoryEmpty(szValue)) {
                MsiSetProperty(hInstall, TEXT("INSTALLDIR_VALID"), TEXT("1"));
            } else {
                MsiSetProperty(hInstall, TEXT("INSTALLDIR_VALID"), TEXT("0"));
            }
        } else {
            MsiSetProperty(hInstall, TEXT("INSTALLDIR_VALID"), TEXT("1"));
        }

        delete [] szValue;

        return ERROR_SUCCESS;
    }
}
