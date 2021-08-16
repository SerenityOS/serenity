/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
#include <string.h>
#include <stdlib.h>
#include <ctype.h>
#include <Windows.h>
#include <tchar.h>

// This is the default buffer size used for RegQueryValue values.
#define DEFAULT_ALLOC MAX_PATH
// only allocate a buffer as big as MAX_ALLOC for RegQueryValue values.
#define MAX_ALLOC 262144

static LPCTSTR ACCESSIBILITY_USER_KEY =
    _T("Software\\Microsoft\\Windows NT\\CurrentVersion\\Accessibility");
static LPCTSTR ACCESSIBILITY_SYSTEM_KEY =
    _T("SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Accessibility\\Session");
static LPCTSTR ACCESSIBILITY_CONFIG =
    _T("Configuration");
static LPCTSTR STR_ACCESSBRIDGE =
    _T("oracle_javaaccessbridge");

// Note: There are senarios where more than one extension can be specified on the
// asssistive_technologies=
// line but this code only deals with the case of
// assistive_technologies=com.sun.java.accessibility.AccessBridge
// assuming that if additional extensions are desired the user knows how edit the file.

FILE* origFile;
FILE* tempFile;

bool isXP()
{
    static bool isXPFlag = false;
    OSVERSIONINFO  osvi;

    // Initialize the OSVERSIONINFO structure.
    ZeroMemory( &osvi, sizeof( osvi ) );
    osvi.dwOSVersionInfoSize = sizeof( osvi );

    GetVersionEx( &osvi );

    if ( osvi.dwMajorVersion == 5 ) // For Windows XP and Windows 2000
        isXPFlag = true;

    return isXPFlag ;
}

void enableJAB() {
    // Copy lines from orig to temp modifying the line containing
    // assistive_technologies=
    // There are various scenarios:
    // 1) If the line exists exactly as
    //    #assistive_technologies=com.sun.java.accessibility.AccessBridge
    //    replace it with
    //    assistive_technologies=com.sun.java.accessibility.AccessBridge
    // 2) else if the line exists exactly as
    //    assistive_technologies=com.sun.java.accessibility.AccessBridge
    //    use it as is
    // 3) else if a line containing "assistive_technologies" exits
    //    a) if it's already commented out, us it as is (jab will be enabled in step 4)
    //    b) else if it's not commented out, comment it out and add a new line with
    //       assistive_technologies=com.sun.java.accessibility.AccessBridge
    // 4) If the line doesn't exist (or case 3a), add
    //    assistive_technologies=com.sun.java.accessibility.AccessBridge
    // Do the same for screen_magnifier_present=
    char line[512];
    char commentLine[512] = "#";
    char jabLine[] = "assistive_technologies=com.sun.java.accessibility.AccessBridge\n";
    char magLine[] = "screen_magnifier_present=true\n";
    bool foundJabLine = false;
    bool foundMagLine = false;
    while (!feof(origFile)) {
        if (fgets(line, 512, origFile) != NULL) {
            if (_stricmp(line, "#assistive_technologies=com.sun.java.accessibility.AccessBridge\n") == 0) {
                fputs(jabLine, tempFile);
                foundJabLine = true;
            } else if (_stricmp(line, jabLine) == 0) {
                fputs(line, tempFile);
                foundJabLine = true;
            } else if (strstr(line, "assistive_technologies") != NULL) {
                char* context;
                char* firstNonSpaceChar = strtok_s(line, " ", &context);
                if (*firstNonSpaceChar == '#') {
                    fputs(line, tempFile);
                } else {
                    strcat_s(commentLine, line);
                    fputs(commentLine, tempFile);
                    fputs(jabLine, tempFile);
                    foundJabLine = true;
                }
            } else if (_stricmp(line, "#screen_magnifier_present=true\n") == 0) {
                fputs(magLine, tempFile);
                foundMagLine = true;
            } else if (_stricmp(line, magLine) == 0) {
                fputs(line, tempFile);
                foundMagLine = true;
            } else if (strstr(line, "screen_magnifier_present") != NULL) {
                char* context;
                char* firstNonSpaceChar = strtok_s(line, " ", &context);
                if (*firstNonSpaceChar == '#') {
                    fputs(line, tempFile);
                } else {
                    strcat_s(commentLine, line);
                    fputs(commentLine, tempFile);
                    fputs(magLine, tempFile);
                    foundMagLine = true;
                }
            } else {
                fputs(line, tempFile);
            }
        }
    }
    if (!foundJabLine) {
        fputs(jabLine, tempFile);
    }
    if (!foundMagLine) {
        fputs(magLine, tempFile);
    }
}

void disableJAB() {
    // Copy lines from orig to temp modifying the line containing
    // assistive_technologies=
    // There are various scenarios:
    // 1) If the uncommented line exists, comment it out
    // 2) If the line exists but is preceeded by a #, nothing to do
    // 3) If the line doesn't exist, nothing to do
    // Do the same for screen_magnifier_present=
    char line[512];
    char commentLine[512];
    while (!feof(origFile)) {
        if (fgets(line, 512, origFile) != NULL) {
            if (strstr(line, "assistive_technologies") != NULL) {
                char* context;
                char* firstNonSpaceChar = strtok_s(line, " ", &context);
                if (*firstNonSpaceChar != '#') {
                    strcpy_s(commentLine, "#");
                    strcat_s(commentLine, line);
                    fputs(commentLine, tempFile);
                } else {
                    fputs(line, tempFile);
                }
            } else if (strstr(line, "screen_magnifier_present") != NULL) {
                char* context;
                char* firstNonSpaceChar = strtok_s(line, " ", &context);
                if (*firstNonSpaceChar != '#') {
                    strcpy_s(commentLine, "#");
                    strcat_s(commentLine, line);
                    fputs(commentLine, tempFile);
                } else {
                    fputs(line, tempFile);
                }
            } else {
                fputs(line, tempFile);
            }
        }
    }
}

int modify(bool enable) {
    errno_t error = 0;
    char path[_MAX_PATH];
    char tempPath[_MAX_PATH];
    // Get the path for %USERPROFILE%
    char *profilePath;
    size_t len;
    error = _dupenv_s(&profilePath, &len, "USERPROFILE" );
    if (error) {
        printf("Error fetching USERPROFILE.\n");
        perror("Error");
        return error;
    }
    const char acc_props1[] = "\\.accessibility.properties";
    const char acc_props2[] = "\\.acce$$ibility.properties";
    // len must be 234 or less (233 characters)
    // sizeof(path) is 260 (room for 259 characters)
    // sizeof(acc_props1) is 27 (26 characters)
    // path will hold 233 path characters plus 26 file characters plus 1 null character)
    // if len - 1 > 233 then error
    if ( len - 1 > sizeof(path) - sizeof(acc_props1) ||
         len - 1 > sizeof(tempPath) - sizeof(acc_props2) ) {
        printf("The USERPROFILE environment variable is too long.\n");
        printf("It must be no longer than 233 characters.\n");
        free(profilePath);
        return 123;
     }
    path[0] = 0;
    strcat_s(path, _MAX_PATH, profilePath);
    strcat_s(path, acc_props1);
    tempPath[0] = 0;
    strcat_s(tempPath, _MAX_PATH, profilePath);
    strcat_s(tempPath, acc_props2);
    free(profilePath);
    profilePath = 0;
    // Open the original file.  If it doesn't exist and this is an enable request then create it.
    error = fopen_s(&origFile, path, "r");
    if (error) {
        if (enable) {
            error = fopen_s(&origFile, path, "w");
            if (error) {
                printf("Couldn't create file: %s\n", path);
                perror("Error");
            } else {
                char str[100] = "assistive_technologies=com.sun.java.accessibility.AccessBridge\n";
                strcat_s(str, "screen_magnifier_present=true\n");
                fprintf(origFile, str);
                fclose(origFile);
            }
        } else {
            // It's OK if the file isn't there for a -disable
            error = 0;
        }
    } else {
        // open a temp file
        error = fopen_s(&tempFile, tempPath, "w");
        if (error) {
            printf("Couldn't open temp file: %s\n", tempPath);
            perror("Error");
            return error;
        }
        if (enable) {
            enableJAB();
        } else {
            disableJAB();
        }
        fclose(origFile);
        fclose(tempFile);
        // delete the orig file and rename the temp file
        if (remove(path) != 0) {
            printf("Couldn't remove file: %s\n", path);
            perror("Error");
            return errno;
        }
        if (rename(tempPath, path) != 0) {
            printf("Couldn't rename %s to %s.\n", tempPath, path);
            perror("Error");
            return errno;
        }
    }
    return error;
}

void printUsage() {
    printf("\njabswitch [/enable | /disable | /version | /?]\n\n");
    printf("Description:\n");
    printf("  jabswitch enables or disables the Java Access Bridge.\n\n");
    printf("Parameters:\n");
    printf("  /enable   Enable the Java Accessibility Bridge.\n");
    printf("  /disable  Disable the Java Accessibility Bridge.\n");
    printf("  /version  Display the version.\n");
    printf("  /?        Display this usage information.\n");
    printf("\nNote:\n");
    printf("  The Java Access Bridge can also be enabled with the\n");
    printf("  Windows Ease of Access control panel (which can be\n");
    printf("  activated by pressing Windows + U).  The Ease of Access\n");
    printf("  control panel has a Java Access Bridge checkbox.  Please\n");
    printf("  be aware that unchecking the checkbox has no effect and\n");
    printf("  in order to disable the Java Access Bridge you must run\n");
    printf("  jabswitch.exe from the command line.\n");
}

void printVersion() {
    TCHAR executableFileName[_MAX_PATH];
    if (!GetModuleFileName(0, executableFileName, _MAX_PATH)) {
        printf("Unable to get executable file name.\n");
        return;
    }
    DWORD nParam;
    DWORD nVersionSize = GetFileVersionInfoSize(executableFileName, &nParam);
    if (!nVersionSize) {
        printf("Unable to get version info size.\n");
        return;
    }
    char* pVersionData = new char[nVersionSize];
    if (!GetFileVersionInfo(executableFileName, 0, nVersionSize, pVersionData)) {
        printf("Unable to get version info.\n");
        return;
    }
    LPVOID pVersionInfo;
    UINT nSize;
    if (!VerQueryValue(pVersionData, _T("\\"), &pVersionInfo, &nSize)) {
        printf("Unable to query version value.\n");
        return;
    }
    VS_FIXEDFILEINFO *pVSInfo = (VS_FIXEDFILEINFO *)pVersionInfo;
    char versionString[100];
    sprintf_s( versionString, "version %i.%i.%i.%i",
               pVSInfo->dwProductVersionMS >> 16,
               pVSInfo->dwProductVersionMS & 0xFFFF,
               pVSInfo->dwProductVersionLS >> 16,
               pVSInfo->dwProductVersionLS & 0xFFFF );
    char outputString[100];
    strcpy_s(outputString, "jabswitch ");
    strcat_s(outputString, versionString);
    strcat_s(outputString, "\njabswitch enables or disables the Java Access Bridge.\n");
    printf(outputString);
}

int regEnable() {
    HKEY hKey;
    DWORD retval = -1;
    LSTATUS err;
    err = RegOpenKeyEx(HKEY_CURRENT_USER, ACCESSIBILITY_USER_KEY, NULL, KEY_READ|KEY_WRITE, &hKey);
    if (err == ERROR_SUCCESS) {
        DWORD dataType = REG_SZ;
        DWORD dataLength = DEFAULT_ALLOC;
        TCHAR dataBuffer[DEFAULT_ALLOC];
        TCHAR *data = dataBuffer;
        bool freeData = false;
        err = RegQueryValueEx(hKey, ACCESSIBILITY_CONFIG, 0, &dataType, (BYTE *)data, &dataLength);
        if (err == ERROR_MORE_DATA) {
            if (dataLength > 0 && dataLength < MAX_ALLOC) {
                data = new TCHAR[dataLength];
                err = RegQueryValueEx(hKey, ACCESSIBILITY_CONFIG, 0, &dataType, (BYTE *)data, &dataLength);
            }
        }
        if (err == ERROR_SUCCESS) {
            err = _tcslwr_s(dataBuffer, DEFAULT_ALLOC);
            if (err) {
                return -1;
            }
            if (_tcsstr(dataBuffer, STR_ACCESSBRIDGE) != NULL) {
                return 0;  // This is OK, e.g. ran enable twice and the value is there.
            } else {
                // add oracle_javaaccessbridge to Config key for HKCU
                dataLength = dataLength + (_tcslen(STR_ACCESSBRIDGE) + 1) * sizeof(TCHAR);
                TCHAR *newStr = new TCHAR[dataLength];
                if (newStr != NULL) {
                    wsprintf(newStr, L"%s,%s", dataBuffer, STR_ACCESSBRIDGE);
                    RegSetValueEx(hKey, ACCESSIBILITY_CONFIG, 0, REG_SZ, (BYTE *)newStr, dataLength);
                }
            }
        }
        RegCloseKey(hKey);
    }
    return err;
}

int regDeleteValue(HKEY hFamilyKey, LPCWSTR lpSubKey)
{
    HKEY hKey;
    DWORD retval = -1;
    LSTATUS err;
    err = RegOpenKeyEx(hFamilyKey, lpSubKey, NULL, KEY_READ|KEY_WRITE|KEY_WOW64_64KEY, &hKey);
    if (err != ERROR_SUCCESS)
        err = RegOpenKeyEx(hFamilyKey, lpSubKey, NULL, KEY_READ|KEY_WRITE, &hKey);

    if (err == ERROR_SUCCESS) {
        DWORD dataType = REG_SZ;
        DWORD dataLength = DEFAULT_ALLOC;
        TCHAR dataBuffer[DEFAULT_ALLOC];
        TCHAR searchBuffer[DEFAULT_ALLOC];
        TCHAR *data = dataBuffer;
        bool freeData = false;
        err = RegQueryValueEx(hKey, ACCESSIBILITY_CONFIG, 0, &dataType, (BYTE *)data, &dataLength);
        if (err == ERROR_MORE_DATA) {
            if (dataLength > 0 && dataLength < MAX_ALLOC) {
                data = new TCHAR[dataLength];
                err = RegQueryValueEx(hKey, ACCESSIBILITY_CONFIG, 0, &dataType, (BYTE *)data, &dataLength);
            }
        }
        if (err == ERROR_SUCCESS) {
            err = _tcslwr_s(dataBuffer, DEFAULT_ALLOC);
            if (err) {
                return -1;
            }
            if (_tcsstr(dataBuffer, STR_ACCESSBRIDGE) == NULL) {
                return 0;  // This is OK, e.g. ran disable twice and the value is not there.
            } else {
                // remove oracle_javaaccessbridge from Config key
                TCHAR *newStr = new TCHAR[dataLength];
                TCHAR *nextToken;
                LPTSTR tok, beg1 = dataBuffer;
                bool first = true;
                _tcscpy_s(newStr, dataLength, L"");
                tok = _tcstok_s(beg1, L",", &nextToken);
                while (tok != NULL) {
                    _tcscpy_s(searchBuffer, DEFAULT_ALLOC, tok);
                    err = _tcslwr_s(searchBuffer, DEFAULT_ALLOC);
                    if (err) {
                        return -1;
                    }
                    if (_tcsstr(searchBuffer, STR_ACCESSBRIDGE) == NULL) {
                        if (!first) {
                           _tcscat_s(newStr, dataLength, L",");
                        }
                        first = false;
                        _tcscat_s(newStr, dataLength, tok);
                    }
                    tok = _tcstok_s(NULL, L",", &nextToken);
                }
                dataLength = (_tcslen(newStr) + 1) * sizeof(TCHAR);
                RegSetValueEx(hKey, ACCESSIBILITY_CONFIG, 0, REG_SZ, (BYTE *)newStr, dataLength);
            }
        }
        RegCloseKey(hKey);
    }
    return err;
}

int regDisable()
{
    LSTATUS err;
    // Update value for HKCU
    err=regDeleteValue(HKEY_CURRENT_USER, ACCESSIBILITY_USER_KEY);
    // Update value for HKLM for Session
    TCHAR dataBuffer[DEFAULT_ALLOC];
    DWORD dwSessionId ;
    ProcessIdToSessionId(GetCurrentProcessId(),&dwSessionId ) ;
    if( dwSessionId >= 0 )
    {
        wsprintf(dataBuffer, L"%s%d", ACCESSIBILITY_SYSTEM_KEY, dwSessionId);
        err=regDeleteValue(HKEY_LOCAL_MACHINE, dataBuffer);
    }
    return err;
}

void main(int argc, char* argv[]) {
    bool enableWasRequested = false;
    bool disableWasRequested = false;
    bool badParams = true;
    int error = 0;
    if (argc == 2) {
        if (_stricmp(argv[1], "-?") == 0 || _stricmp(argv[1], "/?") == 0) {
            printUsage();
            badParams = false;
        } else if (_stricmp(argv[1], "-version") == 0 || _stricmp(argv[1], "/version") == 0) {
            printVersion();
            badParams = false;
        } else {
            if (_stricmp(argv[1], "-enable") == 0 || _stricmp(argv[1], "/enable") == 0) {
                badParams = false;
                enableWasRequested = true;
                error = modify(true);
                if (error == 0) {
                   if( !isXP() )
                      regEnable();
                }
            } else if (_stricmp(argv[1], "-disable") == 0 || _stricmp(argv[1], "/disable") == 0) {
                badParams = false;
                disableWasRequested = true;
                error = modify(false);
                if (error == 0) {
                   if( !isXP() )
                      regDisable();
                }
            }
        }
    }
    if (badParams) {
        printUsage();
    } else if (enableWasRequested || disableWasRequested) {
        if (error != 0) {
            printf("There was an error.\n\n");
        }
        printf("The Java Access Bridge has ");
        if (error != 0) {
            printf("not ");
        }
        printf("been ");
        if (enableWasRequested) {
            printf("enabled.\n");
        } else {
            printf("disabled.\n");
        }
        // Use exit so test case can sense for error.
        if (error != 0) {
            exit(error);
        }
    }
}
