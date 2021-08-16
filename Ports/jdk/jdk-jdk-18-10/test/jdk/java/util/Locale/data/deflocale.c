/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
/*
 *
 *
 * A simple tool to output all the installed locales on a Windows machine, and
 * corresponding Java default locale/file.encoding using PrintDefaultLocale
 *
 * WARNING:  This tool directly modifies the locale info in the Windows registry.
 * It may not work with the Windows versions after Windows XP SP2.  Also,
 * if the test did not complete or was manually killed, you will need to reset
 * the user default locale in the Control Panel manually. This executable has
 * to be run with the "Administrator" privilege.
 *
 * Usage: "deflocale.exe <java launcher> PrintDefaultLocale
 *
 * How to compile: "cl -DUNICODE -D_UNICODE deflocale.c user32.lib advapi32.lib"
 */
#include <windows.h>
#include <stdio.h>
#include <memory.h>

wchar_t* launcher;
wchar_t szBuffer[MAX_PATH];
LCID LCIDArray[1024];
int numLCIDs = 0;
BOOL isWin7orUp = FALSE;

// for Windows 7
BOOL (WINAPI * pfnEnumSystemLocalesEx)(LPVOID, DWORD, LPARAM, LPVOID);
BOOL (WINAPI * pfnEnumUILanguages)(LPVOID, DWORD, LPARAM);
LCID (WINAPI * pfnLocaleNameToLCID)(LPCWSTR, DWORD);
int (WINAPI * pfnLCIDToLocaleName)(LCID, LPWSTR, int, DWORD);
wchar_t* LocaleNamesArray[1024];
wchar_t* UILangNamesArray[1024];
int numLocaleNames = 0;
int numUILangNames = 0;

void launchAndWait() {
    STARTUPINFO si;
    PROCESS_INFORMATION pi;

    ZeroMemory(&si, sizeof(si));
    si.cb = sizeof(si);
    ZeroMemory(&pi, sizeof(pi));
    if (CreateProcess(NULL, launcher, NULL, NULL, FALSE, 0, NULL, NULL, &si, &pi)==0) {
        wprintf(L"CreateProcess failed with the error code: %x\n", GetLastError());
    }

    WaitForSingleObject( pi.hProcess, INFINITE );
}

void testLocale(int anLCID, wchar_t* pName) {
    HKEY hk;

    if (pName != NULL && wcslen(pName) == 2) {
        // ignore language only locale.
        return;
    }

    wprintf(L"\n");
    wprintf(L"OS Locale (lcid: %x", anLCID);
    if (pName != NULL) {
        wprintf(L", name: %s", pName);
    }
    GetLocaleInfo(anLCID, LOCALE_SENGLANGUAGE, szBuffer, MAX_PATH);
    wprintf(L"): %s (", szBuffer);
    GetLocaleInfo(anLCID, LOCALE_SENGCOUNTRY, szBuffer, MAX_PATH);
    wprintf(L"%s) - ", szBuffer);
    GetLocaleInfo(anLCID, LOCALE_IDEFAULTANSICODEPAGE, szBuffer, MAX_PATH);
    wprintf(L"%s\n", szBuffer);
    fflush(0);

    if (RegOpenKeyEx(HKEY_CURRENT_USER, L"Control Panel\\International", 0, KEY_READ | KEY_WRITE, &hk) == ERROR_SUCCESS) {
        wchar_t originalLocale[16];
        wchar_t testLocale[16];
        wchar_t* pKeyName;
        DWORD cb = sizeof(originalLocale);
        DWORD cbTest;

        if (isWin7orUp) {
            pKeyName = L"LocaleName";
            wcscpy(testLocale, pName);
            cbTest = wcslen(pName) * sizeof(wchar_t);
        } else {
            pKeyName = L"Locale";
            swprintf(testLocale, L"%08x", anLCID);
            cbTest = sizeof(wchar_t) * 8;
        }

        RegQueryValueEx(hk, pKeyName, 0, 0, (LPBYTE)originalLocale, &cb);
        RegSetValueEx(hk, pKeyName, 0, REG_SZ, (LPBYTE)testLocale, cbTest );
        launchAndWait();
        RegSetValueEx(hk, pKeyName, 0, REG_SZ, (LPBYTE)originalLocale, cb);
        RegCloseKey(hk);
    }
}

void testUILang(wchar_t* pName) {
    HKEY hk;

    wprintf(L"\n");
    wprintf(L"OS UI Language (name: %s)\n", pName);
    fflush(0);

    if (RegOpenKeyEx(HKEY_CURRENT_USER, L"Control Panel\\Desktop", 0, KEY_READ | KEY_WRITE, &hk) == ERROR_SUCCESS) {
        wchar_t originalUILang[16];
        wchar_t testUILang[16];
        wchar_t* pKeyName;
        DWORD cb = sizeof(originalUILang);
        DWORD cbTest = wcslen(pName) * sizeof(wchar_t);

        pKeyName = L"PreferredUILanguages";
        wcscpy(testUILang, pName);
        cbTest = wcslen(pName) * sizeof(wchar_t);

        RegQueryValueEx(hk, pKeyName, 0, 0, (LPBYTE)originalUILang, &cb);
        RegSetValueEx(hk, pKeyName, 0, REG_SZ, (LPBYTE)testUILang, cbTest);
        launchAndWait();
        RegSetValueEx(hk, pKeyName, 0, REG_SZ, (LPBYTE)originalUILang, cb);
        RegCloseKey(hk);
    }
}

BOOL CALLBACK EnumLocalesProc(LPWSTR lpLocaleStr) {
    swscanf(lpLocaleStr, L"%08x", &LCIDArray[numLCIDs]);
    numLCIDs ++;

    return TRUE;
}

BOOL CALLBACK EnumLocalesProcEx(LPWSTR lpLocaleStr, DWORD flags, LPARAM lp) {
    wchar_t* pName = malloc((wcslen(lpLocaleStr) + 1) * sizeof(wchar_t *));
    wcscpy(pName, lpLocaleStr);
    LocaleNamesArray[numLocaleNames] = pName;
    numLocaleNames ++;

    return TRUE;
}

BOOL CALLBACK EnumUILanguagesProc(LPWSTR lpUILangStr, LPARAM lp) {
    wchar_t* pName = malloc((wcslen(lpUILangStr) + 1) * sizeof(wchar_t *));
    wcscpy(pName, lpUILangStr);
    UILangNamesArray[numUILangNames] = pName;
    numUILangNames ++;

    return TRUE;
}

int sortLCIDs(LCID * pLCID1, LCID * pLCID2) {
    if (*pLCID1 < *pLCID2) return (-1);
    if (*pLCID1 == *pLCID2) return 0;
    return 1;
}

int sortLocaleNames(wchar_t** ppName1, wchar_t** ppName2) {
    LCID l1 = pfnLocaleNameToLCID(*ppName1, 0);
    LCID l2 = pfnLocaleNameToLCID(*ppName2, 0);
    return sortLCIDs(&l1, &l2);
}

int main(int argc, char** argv) {
    OSVERSIONINFO osvi;
    LPWSTR commandline = GetCommandLine();
    int i;

    osvi.dwOSVersionInfoSize = sizeof(osvi);
    GetVersionEx(&osvi);
    wprintf(L"# OSVersionInfo\n");
    wprintf(L"# MajorVersion: %d\n", osvi.dwMajorVersion);
    wprintf(L"# MinorVersion: %d\n", osvi.dwMinorVersion);
    wprintf(L"# BuildNumber: %d\n", osvi.dwBuildNumber);
    wprintf(L"# CSDVersion: %s\n", osvi.szCSDVersion);
    wprintf(L"\n");
    fflush(0);

    launcher = wcschr(commandline, L' ')+1;
    while (*launcher == L' ') {
        launcher++;
    }

    isWin7orUp = (osvi.dwMajorVersion > 6) ||
                 (osvi.dwMajorVersion == 6 && osvi.dwMinorVersion >= 1);

    if (!isWin7orUp) {
        // Enumerate locales
        EnumSystemLocales(EnumLocalesProc, LCID_INSTALLED);

        // Sort LCIDs
        qsort(LCIDArray, numLCIDs, sizeof(LCID), (void *)sortLCIDs);
    } else {
        // For Windows 7, use "LocaleName" registry key for the user locale
        // as they seem to switch from "Locale".
        HMODULE hmod = GetModuleHandle(L"kernel32");
        *(FARPROC*)&pfnEnumSystemLocalesEx =
            GetProcAddress(hmod, "EnumSystemLocalesEx");
        *(FARPROC*)&pfnEnumUILanguages =
            GetProcAddress(hmod, "EnumUILanguagesW");
        *(FARPROC*)&pfnLocaleNameToLCID =
            GetProcAddress(hmod, "LocaleNameToLCID");
        *(FARPROC*)&pfnLCIDToLocaleName =
            GetProcAddress(hmod, "LCIDToLocaleName");
        if (pfnEnumSystemLocalesEx != NULL &&
            pfnEnumUILanguages != NULL &&
            pfnLocaleNameToLCID != NULL &&
            pfnLCIDToLocaleName != NULL) {
            // Enumerate locales
            pfnEnumSystemLocalesEx(EnumLocalesProcEx,
                    1, // LOCALE_WINDOWS
                    (LPARAM)NULL, NULL);
            // Enumerate UI Languages.
            pfnEnumUILanguages(EnumUILanguagesProc,
                    0x8, // MUI_LANGUAGE_NAME
                    (LPARAM)NULL);
        } else {
            wprintf(L"Could not get needed entry points. quitting.\n");
            exit(-1);
        }

        // Sort LocaleNames
        qsort(LocaleNamesArray, numLocaleNames,
              sizeof(wchar_t*), (void *)sortLocaleNames);
        qsort(UILangNamesArray, numUILangNames,
              sizeof(wchar_t*), (void *)sortLocaleNames);
    }

    // Execute enumeration of Java default locales
    if (isWin7orUp) {
        for (i = 0; i < numLocaleNames; i ++) {
            testLocale(pfnLocaleNameToLCID(LocaleNamesArray[i], 0),
                                     LocaleNamesArray[i]);
        }
        for (i = 0; i < numUILangNames; i ++) {
            testUILang(UILangNamesArray[i]);
        }
    } else {
        for (i = 0; i < numLCIDs; i ++) {
            testLocale(LCIDArray[i], NULL);
        }
    }
}
