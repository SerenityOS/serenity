/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include <windows.h>
#include <stdlib.h>
#include <string>
#include <malloc.h>

using namespace std;

// http://msdn.microsoft.com/en-us/library/ms997538.aspx

typedef struct _ICONDIRENTRY {
    BYTE bWidth;
    BYTE bHeight;
    BYTE bColorCount;
    BYTE bReserved;
    WORD wPlanes;
    WORD wBitCount;
    DWORD dwBytesInRes;
    DWORD dwImageOffset;
} ICONDIRENTRY, * LPICONDIRENTRY;

typedef struct _ICONDIR {
    WORD idReserved;
    WORD idType;
    WORD idCount;
    ICONDIRENTRY idEntries[1];
} ICONDIR, * LPICONDIR;

// #pragmas are used here to insure that the structure's
// packing in memory matches the packing of the EXE or DLL.
#pragma pack(push)
#pragma pack(2)

typedef struct _GRPICONDIRENTRY {
    BYTE bWidth;
    BYTE bHeight;
    BYTE bColorCount;
    BYTE bReserved;
    WORD wPlanes;
    WORD wBitCount;
    DWORD dwBytesInRes;
    WORD nID;
} GRPICONDIRENTRY, * LPGRPICONDIRENTRY;
#pragma pack(pop)

#pragma pack(push)
#pragma pack(2)

typedef struct _GRPICONDIR {
    WORD idReserved;
    WORD idType;
    WORD idCount;
    GRPICONDIRENTRY idEntries[1];
} GRPICONDIR, * LPGRPICONDIR;
#pragma pack(pop)

void PrintError() {
    LPVOID message = NULL;
    DWORD error = GetLastError();

    if (FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER
            | FORMAT_MESSAGE_FROM_SYSTEM
            | FORMAT_MESSAGE_IGNORE_INSERTS, NULL, error,
            MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
            (LPTSTR) & message, 0, NULL) != 0) {
        printf("%S", (LPTSTR) message);
        LocalFree(message);
    }
}

// Note: We do not check here that iconTarget is valid icon.
// Java code will already do this for us.

bool ChangeIcon(HANDLE update, const wstring& iconTarget) {
    WORD language = MAKELANGID(LANG_ENGLISH, SUBLANG_DEFAULT);

    HANDLE icon = CreateFile(iconTarget.c_str(), GENERIC_READ, 0, NULL,
            OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    if (icon == INVALID_HANDLE_VALUE) {
        PrintError();
        return false;
    }

    // Reading .ICO file
    WORD idReserved, idType, idCount;

    DWORD dwBytesRead;
    ReadFile(icon, &idReserved, sizeof (WORD), &dwBytesRead, NULL);
    ReadFile(icon, &idType, sizeof (WORD), &dwBytesRead, NULL);
    ReadFile(icon, &idCount, sizeof (WORD), &dwBytesRead, NULL);

    LPICONDIR lpid = (LPICONDIR) malloc(
            sizeof (ICONDIR) + (sizeof (ICONDIRENTRY) * (idCount - 1)));
    if (lpid == NULL) {
        CloseHandle(icon);
        printf("Error: Failed to allocate memory\n");
        return false;
    }

    lpid->idReserved = idReserved;
    lpid->idType = idType;
    lpid->idCount = idCount;

    ReadFile(icon, &lpid->idEntries[0], sizeof (ICONDIRENTRY) * lpid->idCount,
            &dwBytesRead, NULL);

    LPGRPICONDIR lpgid = (LPGRPICONDIR) malloc(
            sizeof (GRPICONDIR) + (sizeof (GRPICONDIRENTRY) * (idCount - 1)));
    if (lpid == NULL) {
        CloseHandle(icon);
        free(lpid);
        printf("Error: Failed to allocate memory\n");
        return false;
    }

    lpgid->idReserved = idReserved;
    lpgid->idType = idType;
    lpgid->idCount = idCount;

    for (int i = 0; i < lpgid->idCount; i++) {
        lpgid->idEntries[i].bWidth = lpid->idEntries[i].bWidth;
        lpgid->idEntries[i].bHeight = lpid->idEntries[i].bHeight;
        lpgid->idEntries[i].bColorCount = lpid->idEntries[i].bColorCount;
        lpgid->idEntries[i].bReserved = lpid->idEntries[i].bReserved;
        lpgid->idEntries[i].wPlanes = lpid->idEntries[i].wPlanes;
        lpgid->idEntries[i].wBitCount = lpid->idEntries[i].wBitCount;
        lpgid->idEntries[i].dwBytesInRes = lpid->idEntries[i].dwBytesInRes;
        lpgid->idEntries[i].nID = i + 1;
    }

    // Store images in .EXE
    for (int i = 0; i < lpid->idCount; i++) {
        LPBYTE lpBuffer = (LPBYTE) malloc(lpid->idEntries[i].dwBytesInRes);
        SetFilePointer(icon, lpid->idEntries[i].dwImageOffset,
                NULL, FILE_BEGIN);
        ReadFile(icon, lpBuffer, lpid->idEntries[i].dwBytesInRes,
                &dwBytesRead, NULL);
        if (!UpdateResource(update, RT_ICON,
                MAKEINTRESOURCE(lpgid->idEntries[i].nID),
                language, &lpBuffer[0], lpid->idEntries[i].dwBytesInRes)) {
            free(lpBuffer);
            free(lpid);
            free(lpgid);
            CloseHandle(icon);
            PrintError();
            return false;
        }
        free(lpBuffer);
    }

    free(lpid);
    CloseHandle(icon);

    if (!UpdateResource(update, RT_GROUP_ICON, MAKEINTRESOURCE(1),
            language, &lpgid[0], (sizeof (WORD) * 3)
            + (sizeof (GRPICONDIRENTRY) * lpgid->idCount))) {
        free(lpgid);
        PrintError();
        return false;
    }

    free(lpgid);

    return true;
}
