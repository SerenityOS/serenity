/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

/* Access APIs for WinXP and above */
#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0501
#endif

#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <direct.h>
#include <windows.h>
#include <io.h>
#include <limits.h>
#include <wchar.h>
#include <Winioctl.h>

#include "jni.h"
#include "io_util.h"
#include "jlong.h"
#include "io_util_md.h"
#include "dirent_md.h"
#include "java_io_FileSystem.h"

#define MAX_PATH_LENGTH 1024

static struct {
    jfieldID path;
} ids;

/**
 * GetFinalPathNameByHandle is available on Windows Vista and newer
 */
typedef BOOL (WINAPI* GetFinalPathNameByHandleProc) (HANDLE, LPWSTR, DWORD, DWORD);
static GetFinalPathNameByHandleProc GetFinalPathNameByHandle_func;

JNIEXPORT void JNICALL
Java_java_io_WinNTFileSystem_initIDs(JNIEnv *env, jclass cls)
{
    HMODULE handle;
    jclass fileClass;

    fileClass = (*env)->FindClass(env, "java/io/File");
    CHECK_NULL(fileClass);
    ids.path = (*env)->GetFieldID(env, fileClass, "path", "Ljava/lang/String;");
    CHECK_NULL(ids.path);

    // GetFinalPathNameByHandle requires Windows Vista or newer
    if (GetModuleHandleExW((GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS |
                            GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT),
                           (LPCWSTR)&CreateFileW, &handle) != 0)
    {
        GetFinalPathNameByHandle_func = (GetFinalPathNameByHandleProc)
            GetProcAddress(handle, "GetFinalPathNameByHandleW");
    }
}

/* -- Path operations -- */

extern int wcanonicalize(const WCHAR *path, WCHAR *out, int len);
extern int wcanonicalizeWithPrefix(const WCHAR *canonicalPrefix, const WCHAR *pathWithCanonicalPrefix, WCHAR *out, int len);

/**
 * Retrieves the fully resolved (final) path for the given path or NULL
 * if the function fails.
 */
static WCHAR* getFinalPath(JNIEnv *env, const WCHAR *path)
{
    HANDLE h;
    WCHAR *result;
    DWORD error;

    /* Need Windows Vista or newer to get the final path */
    if (GetFinalPathNameByHandle_func == NULL)
        return NULL;

    h = CreateFileW(path,
                    FILE_READ_ATTRIBUTES,
                    FILE_SHARE_DELETE |
                        FILE_SHARE_READ | FILE_SHARE_WRITE,
                    NULL,
                    OPEN_EXISTING,
                    FILE_FLAG_BACKUP_SEMANTICS,
                    NULL);
    if (h == INVALID_HANDLE_VALUE)
        return NULL;

    /**
     * Allocate a buffer for the resolved path. For a long path we may need
     * to allocate a larger buffer.
     */
    result = (WCHAR*)malloc(MAX_PATH * sizeof(WCHAR));
    if (result != NULL) {
        DWORD len = (*GetFinalPathNameByHandle_func)(h, result, MAX_PATH, 0);
        if (len >= MAX_PATH) {
            /* retry with a buffer of the right size */
            WCHAR* newResult = (WCHAR*)realloc(result, (len+1) * sizeof(WCHAR));
            if (newResult != NULL) {
                result = newResult;
                len = (*GetFinalPathNameByHandle_func)(h, result, len, 0);
            } else {
                len = 0;
                JNU_ThrowOutOfMemoryError(env, "native memory allocation failed");
            }
        }

        if (len > 0) {
            /**
             * Strip prefix (should be \\?\ or \\?\UNC)
             */
            if (result[0] == L'\\' && result[1] == L'\\' &&
                result[2] == L'?' && result[3] == L'\\')
            {
                int isUnc = (result[4] == L'U' &&
                             result[5] == L'N' &&
                             result[6] == L'C');
                int prefixLen = (isUnc) ? 7 : 4;
                int prefixToKeep = (isUnc) ? 1 : 0;
                // the amount to copy includes terminator
                int amountToCopy = len - prefixLen + 1;
                wmemmove(result + prefixToKeep, result + prefixLen, amountToCopy);
            }
        }

        /* unable to get final path */
        if (len == 0 && result != NULL) {
            free(result);
            result = NULL;
        }
    } else {
        JNU_ThrowOutOfMemoryError(env, "native memory allocation failed");
    }

    error = GetLastError();
    if (CloseHandle(h))
        SetLastError(error);
    return result;
}

/**
 * Retrieves file information for the specified file. If the file is
 * symbolic link then the information on fully resolved target is
 * returned.
 */
static BOOL getFileInformation(const WCHAR *path,
                               BY_HANDLE_FILE_INFORMATION *finfo)
{
    BOOL result;
    DWORD error;
    HANDLE h = CreateFileW(path,
                           FILE_READ_ATTRIBUTES,
                           FILE_SHARE_DELETE |
                               FILE_SHARE_READ | FILE_SHARE_WRITE,
                           NULL,
                           OPEN_EXISTING,
                           FILE_FLAG_BACKUP_SEMANTICS,
                           NULL);
    if (h == INVALID_HANDLE_VALUE)
        return FALSE;
    result = GetFileInformationByHandle(h, finfo);
    error = GetLastError();
    if (CloseHandle(h))
        SetLastError(error);
    return result;
}

/**
 * path is likely to be a Unix domain socket.
 * Verify and if it is return its attributes
 */
static DWORD getFinalAttributesUnixSocket(const WCHAR *path)
{
    DWORD result;
    BY_HANDLE_FILE_INFORMATION finfo;
    REPARSE_GUID_DATA_BUFFER reparse;

    HANDLE h = CreateFileW(path,
                           FILE_READ_ATTRIBUTES,
                           FILE_SHARE_DELETE |
                               FILE_SHARE_READ | FILE_SHARE_WRITE,
                           NULL,
                           OPEN_EXISTING,
                           FILE_FLAG_BACKUP_SEMANTICS |
                               FILE_FLAG_OPEN_REPARSE_POINT,
                           NULL);

    if (h == INVALID_HANDLE_VALUE)
        return INVALID_FILE_ATTRIBUTES;


    if (!GetFileInformationByHandle(h, &finfo)) {
        DWORD error = GetLastError();
        if (CloseHandle(h)) {
            SetLastError(error);
        }
        return INVALID_FILE_ATTRIBUTES;
    }

    if ((finfo.dwFileAttributes & FILE_ATTRIBUTE_REPARSE_POINT) == 0) {
        CloseHandle(h);
        return INVALID_FILE_ATTRIBUTES;
    }

    /* check the reparse tag */

    if (DeviceIoControl(h, FSCTL_GET_REPARSE_POINT, NULL, 0, &reparse,
                (DWORD)sizeof(reparse), &result, NULL) == 0) {
        CloseHandle(h);
        return INVALID_FILE_ATTRIBUTES;
    }

    if (reparse.ReparseTag != IO_REPARSE_TAG_AF_UNIX) {
        CloseHandle(h);
        return INVALID_FILE_ATTRIBUTES;
    }

    CloseHandle(h);
    return finfo.dwFileAttributes;
}

/**
 * If the given attributes are the attributes of a reparse point, then
 * read and return the attributes of the special cases.
 */
DWORD getFinalAttributesIfReparsePoint(WCHAR *path, DWORD a)
{
    if ((a != INVALID_FILE_ATTRIBUTES) &&
        ((a & FILE_ATTRIBUTE_REPARSE_POINT) != 0))
    {
        BY_HANDLE_FILE_INFORMATION finfo;
        BOOL res = getFileInformation(path, &finfo);
        a = (res) ? finfo.dwFileAttributes : INVALID_FILE_ATTRIBUTES;
    }
    return a;
}

/**
 * Take special cases into account when retrieving the attributes
 * of path
 */
DWORD getFinalAttributes(WCHAR *path)
{
    DWORD attr = INVALID_FILE_ATTRIBUTES;

    WIN32_FILE_ATTRIBUTE_DATA wfad;
    WIN32_FIND_DATAW wfd;
    HANDLE h;

    if (GetFileAttributesExW(path, GetFileExInfoStandard, &wfad)) {
        attr = getFinalAttributesIfReparsePoint(path, wfad.dwFileAttributes);
        if (attr == INVALID_FILE_ATTRIBUTES) {
            if (GetLastError() == ERROR_CANT_ACCESS_FILE) {
                attr = getFinalAttributesUnixSocket(path);
            }
        }
    } else {
        DWORD lerr = GetLastError();
        if ((lerr == ERROR_SHARING_VIOLATION || lerr == ERROR_ACCESS_DENIED) &&
            (h = FindFirstFileW(path, &wfd)) != INVALID_HANDLE_VALUE) {
            attr = getFinalAttributesIfReparsePoint(path, wfd.dwFileAttributes);
            FindClose(h);
        }
    }
    return attr;
}

JNIEXPORT jstring JNICALL
Java_java_io_WinNTFileSystem_canonicalize0(JNIEnv *env, jobject this,
                                           jstring pathname)
{
    jstring rv = NULL;
    WCHAR canonicalPath[MAX_PATH_LENGTH];

    WITH_UNICODE_STRING(env, pathname, path) {
        /* we estimate the max length of memory needed as
           "currentDir. length + pathname.length"
         */
        int len = (int)wcslen(path);
        len += currentDirLength(path, len);
        if (len  > MAX_PATH_LENGTH - 1) {
            WCHAR *cp = (WCHAR*)malloc(len * sizeof(WCHAR));
            if (cp != NULL) {
                if (wcanonicalize(path, cp, len) >= 0) {
                    rv = (*env)->NewString(env, cp, (jsize)wcslen(cp));
                }
                free(cp);
            } else {
                JNU_ThrowOutOfMemoryError(env, "native memory allocation failed");
            }
        } else if (wcanonicalize(path, canonicalPath, MAX_PATH_LENGTH) >= 0) {
            rv = (*env)->NewString(env, canonicalPath, (jsize)wcslen(canonicalPath));
        }
    } END_UNICODE_STRING(env, path);
    if (rv == NULL && !(*env)->ExceptionCheck(env)) {
        JNU_ThrowIOExceptionWithLastError(env, "Bad pathname");
    }
    return rv;
}


JNIEXPORT jstring JNICALL
Java_java_io_WinNTFileSystem_canonicalizeWithPrefix0(JNIEnv *env, jobject this,
                                                     jstring canonicalPrefixString,
                                                     jstring pathWithCanonicalPrefixString)
{
    jstring rv = NULL;
    WCHAR canonicalPath[MAX_PATH_LENGTH];
    WITH_UNICODE_STRING(env, canonicalPrefixString, canonicalPrefix) {
        WITH_UNICODE_STRING(env, pathWithCanonicalPrefixString, pathWithCanonicalPrefix) {
            int len = (int)wcslen(canonicalPrefix) + MAX_PATH;
            if (len > MAX_PATH_LENGTH) {
                WCHAR *cp = (WCHAR*)malloc(len * sizeof(WCHAR));
                if (cp != NULL) {
                    if (wcanonicalizeWithPrefix(canonicalPrefix,
                                                pathWithCanonicalPrefix,
                                                cp, len) >= 0) {
                      rv = (*env)->NewString(env, cp, (jsize)wcslen(cp));
                    }
                    free(cp);
                } else {
                    JNU_ThrowOutOfMemoryError(env, "native memory allocation failed");
                }
            } else if (wcanonicalizeWithPrefix(canonicalPrefix,
                                               pathWithCanonicalPrefix,
                                               canonicalPath, MAX_PATH_LENGTH) >= 0) {
                rv = (*env)->NewString(env, canonicalPath, (jsize)wcslen(canonicalPath));
            }
        } END_UNICODE_STRING(env, pathWithCanonicalPrefix);
    } END_UNICODE_STRING(env, canonicalPrefix);
    if (rv == NULL && !(*env)->ExceptionCheck(env)) {
        JNU_ThrowIOExceptionWithLastError(env, "Bad pathname");
    }
    return rv;
}

/* -- Attribute accessors -- */

/* Check whether or not the file name in "path" is a Windows reserved
   device name (CON, PRN, AUX, NUL, COM[1-9], LPT[1-9]) based on the
   returned result from GetFullPathName, which should be in thr form of
   "\\.\[ReservedDeviceName]" if the path represents a reserved device
   name.
   Note1: GetFullPathName doesn't think "CLOCK$" (which is no longer
   important anyway) is a device name, so we don't check it here.
   GetFileAttributesEx will catch it later by returning 0 on NT/XP/
   200X.

   Note2: Theoretically the implementation could just lookup the table
   below linearly if the first 4 characters of the fullpath returned
   from GetFullPathName are "\\.\". The current implementation should
   achieve the same result. If Microsoft add more names into their
   reserved device name repository in the future, which probably will
   never happen, we will need to revisit the lookup implementation.

static WCHAR* ReservedDEviceNames[] = {
    L"CON", L"PRN", L"AUX", L"NUL",
    L"COM1", L"COM2", L"COM3", L"COM4", L"COM5", L"COM6", L"COM7", L"COM8", L"COM9",
    L"LPT1", L"LPT2", L"LPT3", L"LPT4", L"LPT5", L"LPT6", L"LPT7", L"LPT8", L"LPT9",
    L"CLOCK$"
};
 */

static BOOL isReservedDeviceNameW(WCHAR* path) {
#define BUFSIZE 9
    WCHAR buf[BUFSIZE];
    WCHAR *lpf = NULL;
    DWORD retLen = GetFullPathNameW(path,
                                   BUFSIZE,
                                   buf,
                                   &lpf);
    if ((retLen == BUFSIZE - 1 || retLen == BUFSIZE - 2) &&
        buf[0] == L'\\' && buf[1] == L'\\' &&
        buf[2] == L'.' && buf[3] == L'\\') {
        WCHAR* dname = _wcsupr(buf + 4);
        if (wcscmp(dname, L"CON") == 0 ||
            wcscmp(dname, L"PRN") == 0 ||
            wcscmp(dname, L"AUX") == 0 ||
            wcscmp(dname, L"NUL") == 0)
            return TRUE;
        if ((wcsncmp(dname, L"COM", 3) == 0 ||
             wcsncmp(dname, L"LPT", 3) == 0) &&
            dname[3] - L'0' > 0 &&
            dname[3] - L'0' <= 9)
            return TRUE;
    }
    return FALSE;
}

JNIEXPORT jint JNICALL
Java_java_io_WinNTFileSystem_getBooleanAttributes(JNIEnv *env, jobject this,
                                                  jobject file)
{
    jint rv = 0;

    WCHAR *pathbuf = fileToNTPath(env, file, ids.path);
    if (pathbuf == NULL)
        return rv;
    if (!isReservedDeviceNameW(pathbuf)) {
        DWORD a = getFinalAttributes(pathbuf);
        if (a != INVALID_FILE_ATTRIBUTES) {
            rv = (java_io_FileSystem_BA_EXISTS
                | ((a & FILE_ATTRIBUTE_DIRECTORY)
                    ? java_io_FileSystem_BA_DIRECTORY
                    : java_io_FileSystem_BA_REGULAR)
                | ((a & FILE_ATTRIBUTE_HIDDEN)
                    ? java_io_FileSystem_BA_HIDDEN : 0));
        }
    }
    free(pathbuf);
    return rv;
}


JNIEXPORT jboolean
JNICALL Java_java_io_WinNTFileSystem_checkAccess(JNIEnv *env, jobject this,
                                                 jobject file, jint access)
{
    DWORD attr;
    WCHAR *pathbuf = fileToNTPath(env, file, ids.path);
    if (pathbuf == NULL)
        return JNI_FALSE;
    attr = GetFileAttributesW(pathbuf);
    attr = getFinalAttributesIfReparsePoint(pathbuf, attr);
    free(pathbuf);
    if (attr == INVALID_FILE_ATTRIBUTES)
        return JNI_FALSE;
    switch (access) {
    case java_io_FileSystem_ACCESS_READ:
    case java_io_FileSystem_ACCESS_EXECUTE:
        return JNI_TRUE;
    case java_io_FileSystem_ACCESS_WRITE:
        /* Read-only attribute ignored on directories */
        if ((attr & FILE_ATTRIBUTE_DIRECTORY) ||
            (attr & FILE_ATTRIBUTE_READONLY) == 0)
            return JNI_TRUE;
        else
            return JNI_FALSE;
    default:
        assert(0);
        return JNI_FALSE;
    }
}

JNIEXPORT jboolean JNICALL
Java_java_io_WinNTFileSystem_setPermission(JNIEnv *env, jobject this,
                                           jobject file,
                                           jint access,
                                           jboolean enable,
                                           jboolean owneronly)
{
    jboolean rv = JNI_FALSE;
    WCHAR *pathbuf;
    DWORD a;
    if (access == java_io_FileSystem_ACCESS_READ ||
        access == java_io_FileSystem_ACCESS_EXECUTE) {
        return enable;
    }
    pathbuf = fileToNTPath(env, file, ids.path);
    if (pathbuf == NULL)
        return JNI_FALSE;
    a = GetFileAttributesW(pathbuf);

    /* if reparse point, get final target */
    if ((a != INVALID_FILE_ATTRIBUTES) &&
        ((a & FILE_ATTRIBUTE_REPARSE_POINT) != 0))
    {
        WCHAR *fp = getFinalPath(env, pathbuf);
        if (fp == NULL) {
            a = INVALID_FILE_ATTRIBUTES;
        } else {
            free(pathbuf);
            pathbuf = fp;
            a = GetFileAttributesW(pathbuf);
        }
    }
    if ((a != INVALID_FILE_ATTRIBUTES) &&
        ((a & FILE_ATTRIBUTE_DIRECTORY) == 0))
    {
        if (enable)
            a =  a & ~FILE_ATTRIBUTE_READONLY;
        else
            a =  a | FILE_ATTRIBUTE_READONLY;
        if (SetFileAttributesW(pathbuf, a))
            rv = JNI_TRUE;
    }
    free(pathbuf);
    return rv;
}

JNIEXPORT jlong JNICALL
Java_java_io_WinNTFileSystem_getLastModifiedTime(JNIEnv *env, jobject this,
                                                 jobject file)
{
    jlong rv = 0;
    ULARGE_INTEGER modTime;
    FILETIME t;
    HANDLE h;
    WCHAR *pathbuf = fileToNTPath(env, file, ids.path);
    if (pathbuf == NULL)
        return rv;
    h = CreateFileW(pathbuf,
                    /* Device query access */
                    0,
                    /* Share it */
                    FILE_SHARE_DELETE | FILE_SHARE_READ | FILE_SHARE_WRITE,
                    /* No security attributes */
                    NULL,
                    /* Open existing or fail */
                    OPEN_EXISTING,
                    /* Backup semantics for directories */
                    FILE_FLAG_BACKUP_SEMANTICS,
                    /* No template file */
                    NULL);
    if (h != INVALID_HANDLE_VALUE) {
        if (GetFileTime(h, NULL, NULL, &t)) {
            modTime.LowPart = (DWORD) t.dwLowDateTime;
            modTime.HighPart = (LONG) t.dwHighDateTime;
            rv = modTime.QuadPart / 10000;
            rv -= 11644473600000;
        }
        CloseHandle(h);
    }
    free(pathbuf);
    return rv;
}

JNIEXPORT jlong JNICALL
Java_java_io_WinNTFileSystem_getLength(JNIEnv *env, jobject this, jobject file)
{
    jlong rv = 0;
    WIN32_FILE_ATTRIBUTE_DATA wfad;
    WCHAR *pathbuf = fileToNTPath(env, file, ids.path);
    if (pathbuf == NULL)
        return rv;
    if (GetFileAttributesExW(pathbuf,
                             GetFileExInfoStandard,
                             &wfad)) {
        if ((wfad.dwFileAttributes & FILE_ATTRIBUTE_REPARSE_POINT) == 0) {
            rv = wfad.nFileSizeHigh * ((jlong)MAXDWORD + 1) + wfad.nFileSizeLow;
        } else {
            /* file is a reparse point so read attributes of final target */
            BY_HANDLE_FILE_INFORMATION finfo;
            if (getFileInformation(pathbuf, &finfo)) {
                rv = finfo.nFileSizeHigh * ((jlong)MAXDWORD + 1) +
                    finfo.nFileSizeLow;
            }
        }
    } else {
        if (GetLastError() == ERROR_SHARING_VIOLATION) {
            //
            // The error is a "share violation", which means the file/dir
            // must exist. Try FindFirstFile, we know this at least works
            // for pagefile.sys.
            //

            WIN32_FIND_DATAW fileData;
            HANDLE h = FindFirstFileW(pathbuf, &fileData);
            if (h != INVALID_HANDLE_VALUE) {
                if ((fileData.dwFileAttributes &
                     FILE_ATTRIBUTE_REPARSE_POINT) == 0) {
                    WCHAR backslash = L'\\';
                    WCHAR *pslash = wcsrchr(pathbuf, backslash);
                    if (pslash == NULL) {
                        pslash = pathbuf;
                    } else {
                        pslash++;
                    }
                    WCHAR *fslash = wcsrchr(fileData.cFileName, backslash);
                    if (fslash == NULL) {
                        fslash = fileData.cFileName;
                    } else {
                        fslash++;
                    }
                    if (wcscmp(pslash, fslash) == 0) {
                        ULARGE_INTEGER length;
                        length.LowPart = fileData.nFileSizeLow;
                        length.HighPart = fileData.nFileSizeHigh;
                        if (length.QuadPart <= _I64_MAX) {
                            rv = (jlong)length.QuadPart;
                        }
                    }
                }
                FindClose(h);
            }
        }
    }
    free(pathbuf);
    return rv;
}

/* -- File operations -- */

JNIEXPORT jboolean JNICALL
Java_java_io_WinNTFileSystem_createFileExclusively(JNIEnv *env, jclass cls,
                                                   jstring path)
{
    HANDLE h = NULL;
    WCHAR *pathbuf = pathToNTPath(env, path, JNI_FALSE);
    if (pathbuf == NULL)
        return JNI_FALSE;
    if (isReservedDeviceNameW(pathbuf)) {
        free(pathbuf);
        return JNI_FALSE;
    }
    h = CreateFileW(
        pathbuf,                              /* Wide char path name */
        GENERIC_READ | GENERIC_WRITE,         /* Read and write permission */
        FILE_SHARE_READ | FILE_SHARE_WRITE,   /* File sharing flags */
        NULL,                                 /* Security attributes */
        CREATE_NEW,                           /* creation disposition */
        FILE_ATTRIBUTE_NORMAL |
            FILE_FLAG_OPEN_REPARSE_POINT,     /* flags and attributes */
        NULL);

    if (h == INVALID_HANDLE_VALUE) {
        DWORD error = GetLastError();
        if ((error != ERROR_FILE_EXISTS) && (error != ERROR_ALREADY_EXISTS)) {
            // return false rather than throwing an exception when there is
            // an existing file.
            DWORD a = GetFileAttributesW(pathbuf);
            if (a == INVALID_FILE_ATTRIBUTES) {
                SetLastError(error);
                JNU_ThrowIOExceptionWithLastError(env, "Could not open file");
            }
        }
        free(pathbuf);
        return JNI_FALSE;
    }
    free(pathbuf);
    CloseHandle(h);
    return JNI_TRUE;
}

static int
removeFileOrDirectory(const jchar *path)
{
    /* Returns 0 on success */
    DWORD a;

    SetFileAttributesW(path, FILE_ATTRIBUTE_NORMAL);
    a = GetFileAttributesW(path);
    if (a == INVALID_FILE_ATTRIBUTES) {
        return 1;
    } else if (a & FILE_ATTRIBUTE_DIRECTORY) {
        return !RemoveDirectoryW(path);
    } else {
        return !DeleteFileW(path);
    }
}

JNIEXPORT jboolean JNICALL
Java_java_io_WinNTFileSystem_delete0(JNIEnv *env, jobject this, jobject file)
{
    jboolean rv = JNI_FALSE;
    WCHAR *pathbuf = fileToNTPath(env, file, ids.path);
    if (pathbuf == NULL) {
        return JNI_FALSE;
    }
    if (removeFileOrDirectory(pathbuf) == 0) {
        rv = JNI_TRUE;
    }
    free(pathbuf);
    return rv;
}

JNIEXPORT jobjectArray JNICALL
Java_java_io_WinNTFileSystem_list(JNIEnv *env, jobject this, jobject file)
{
    WCHAR *search_path;
    HANDLE handle;
    WIN32_FIND_DATAW find_data;
    int len, maxlen;
    jobjectArray rv, old;
    DWORD fattr;
    jstring name;
    jclass str_class;
    WCHAR *pathbuf;
    DWORD err;

    str_class = JNU_ClassString(env);
    CHECK_NULL_RETURN(str_class, NULL);

    pathbuf = fileToNTPath(env, file, ids.path);
    if (pathbuf == NULL)
        return NULL;
    search_path = (WCHAR*)malloc(2*wcslen(pathbuf) + 6);
    if (search_path == 0) {
        free (pathbuf);
        errno = ENOMEM;
        JNU_ThrowOutOfMemoryError(env, "native memory allocation failed");
        return NULL;
    }
    wcscpy(search_path, pathbuf);
    free(pathbuf);
    fattr = GetFileAttributesW(search_path);
    if (fattr == INVALID_FILE_ATTRIBUTES) {
        free(search_path);
        return NULL;
    } else if ((fattr & FILE_ATTRIBUTE_DIRECTORY) == 0) {
        free(search_path);
        return NULL;
    }

    /* Remove trailing space chars from directory name */
    len = (int)wcslen(search_path);
    while (search_path[len-1] == L' ') {
        len--;
    }
    search_path[len] = 0;

    /* Append "*", or possibly "\\*", to path */
    if ((search_path[0] == L'\\' && search_path[1] == L'\0') ||
        (search_path[1] == L':'
        && (search_path[2] == L'\0'
        || (search_path[2] == L'\\' && search_path[3] == L'\0')))) {
        /* No '\\' needed for cases like "\" or "Z:" or "Z:\" */
        wcscat(search_path, L"*");
    } else {
        wcscat(search_path, L"\\*");
    }

    /* Open handle to the first file */
    handle = FindFirstFileW(search_path, &find_data);
    free(search_path);
    if (handle == INVALID_HANDLE_VALUE) {
        if (GetLastError() != ERROR_FILE_NOT_FOUND) {
            // error
            return NULL;
        } else {
            // No files found - return an empty array
            rv = (*env)->NewObjectArray(env, 0, str_class, NULL);
            return rv;
        }
    }

    /* Allocate an initial String array */
    len = 0;
    maxlen = 16;
    rv = (*env)->NewObjectArray(env, maxlen, str_class, NULL);
    if (rv == NULL) { // Couldn't allocate an array
        FindClose(handle);
        return NULL;
    }
    /* Scan the directory */
    do {
        if (!wcscmp(find_data.cFileName, L".")
                                || !wcscmp(find_data.cFileName, L".."))
           continue;
        name = (*env)->NewString(env, find_data.cFileName,
                                 (jsize)wcslen(find_data.cFileName));
        if (name == NULL) {
            FindClose(handle);
            return NULL; // error
        }
        if (len == maxlen) {
            old = rv;
            rv = (*env)->NewObjectArray(env, maxlen <<= 1, str_class, NULL);
            if (rv == NULL || JNU_CopyObjectArray(env, rv, old, len) < 0) {
                FindClose(handle);
                return NULL; // error
            }
            (*env)->DeleteLocalRef(env, old);
        }
        (*env)->SetObjectArrayElement(env, rv, len++, name);
        (*env)->DeleteLocalRef(env, name);

    } while (FindNextFileW(handle, &find_data));

    err = GetLastError();
    FindClose(handle);
    if (err != ERROR_NO_MORE_FILES) {
        return NULL; // error
    }

    if (len < maxlen) {
        /* Copy the final results into an appropriately-sized array */
        old = rv;
        rv = (*env)->NewObjectArray(env, len, str_class, NULL);
        if (rv == NULL)
            return NULL; /* error */
        if (JNU_CopyObjectArray(env, rv, old, len) < 0)
            return NULL; /* error */
    }
    return rv;
}


JNIEXPORT jboolean JNICALL
Java_java_io_WinNTFileSystem_createDirectory(JNIEnv *env, jobject this,
                                             jobject file)
{
    BOOL h = FALSE;
    WCHAR *pathbuf = fileToNTPath(env, file, ids.path);
    if (pathbuf == NULL) {
        /* Exception is pending */
        return JNI_FALSE;
    }
    h = CreateDirectoryW(pathbuf, NULL);
    free(pathbuf);

    if (h == 0) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}


JNIEXPORT jboolean JNICALL
Java_java_io_WinNTFileSystem_rename0(JNIEnv *env, jobject this, jobject from,
                                     jobject to)
{

    jboolean rv = JNI_FALSE;
    WCHAR *frompath = fileToNTPath(env, from, ids.path);
    WCHAR *topath = fileToNTPath(env, to, ids.path);
    if (frompath != NULL && topath != NULL && _wrename(frompath, topath) == 0) {
        rv = JNI_TRUE;
    }
    free(frompath);
    free(topath);
    return rv;
}


JNIEXPORT jboolean JNICALL
Java_java_io_WinNTFileSystem_setLastModifiedTime(JNIEnv *env, jobject this,
                                                 jobject file, jlong time)
{
    jboolean rv = JNI_FALSE;
    WCHAR *pathbuf = fileToNTPath(env, file, ids.path);
    HANDLE h;
    if (pathbuf == NULL)
        return JNI_FALSE;
    h = CreateFileW(pathbuf,
                    FILE_WRITE_ATTRIBUTES,
                    FILE_SHARE_READ | FILE_SHARE_WRITE,
                    NULL,
                    OPEN_EXISTING,
                    FILE_FLAG_BACKUP_SEMANTICS,
                    0);
    if (h != INVALID_HANDLE_VALUE) {
        ULARGE_INTEGER modTime;
        FILETIME t;
        modTime.QuadPart = (time + 11644473600000L) * 10000L;
        t.dwLowDateTime = (DWORD)modTime.LowPart;
        t.dwHighDateTime = (DWORD)modTime.HighPart;
        if (SetFileTime(h, NULL, NULL, &t)) {
            rv = JNI_TRUE;
        }
        CloseHandle(h);
    }
    free(pathbuf);

    return rv;
}


JNIEXPORT jboolean JNICALL
Java_java_io_WinNTFileSystem_setReadOnly(JNIEnv *env, jobject this,
                                         jobject file)
{
    jboolean rv = JNI_FALSE;
    DWORD a;
    WCHAR *pathbuf = fileToNTPath(env, file, ids.path);
    if (pathbuf == NULL)
        return JNI_FALSE;
    a = GetFileAttributesW(pathbuf);

    /* if reparse point, get final target */
    if ((a != INVALID_FILE_ATTRIBUTES) &&
        ((a & FILE_ATTRIBUTE_REPARSE_POINT) != 0))
    {
        WCHAR *fp = getFinalPath(env, pathbuf);
        if (fp == NULL) {
            a = INVALID_FILE_ATTRIBUTES;
        } else {
            free(pathbuf);
            pathbuf = fp;
            a = GetFileAttributesW(pathbuf);
        }
    }

    if ((a != INVALID_FILE_ATTRIBUTES) &&
        ((a & FILE_ATTRIBUTE_DIRECTORY) == 0)) {
        if (SetFileAttributesW(pathbuf, a | FILE_ATTRIBUTE_READONLY))
            rv = JNI_TRUE;
    }
    free(pathbuf);
    return rv;
}

/* -- Filesystem interface -- */


JNIEXPORT jobject JNICALL
Java_java_io_WinNTFileSystem_getDriveDirectory(JNIEnv *env, jobject this,
                                               jint drive)
{
    jstring ret = NULL;
    jchar *p = currentDir(drive);
    jchar *pf = p;
    if (p == NULL) return NULL;
    if (iswalpha(*p) && (p[1] == L':')) p += 2;
    ret = (*env)->NewString(env, p, (jsize)wcslen(p));
    free (pf);
    return ret;
}

JNIEXPORT jint JNICALL
Java_java_io_WinNTFileSystem_listRoots0(JNIEnv *env, jclass ignored)
{
    return GetLogicalDrives();
}

JNIEXPORT jlong JNICALL
Java_java_io_WinNTFileSystem_getSpace0(JNIEnv *env, jobject this,
                                       jobject file, jint t)
{
    WCHAR volname[MAX_PATH_LENGTH + 1];
    jlong rv = 0L;
    WCHAR *pathbuf = fileToNTPath(env, file, ids.path);

    if (GetVolumePathNameW(pathbuf, volname, MAX_PATH_LENGTH)) {
        ULARGE_INTEGER totalSpace, freeSpace, usableSpace;
        if (GetDiskFreeSpaceExW(volname, &usableSpace, &totalSpace, &freeSpace)) {
            switch(t) {
            case java_io_FileSystem_SPACE_TOTAL:
                rv = long_to_jlong(totalSpace.QuadPart);
                break;
            case java_io_FileSystem_SPACE_FREE:
                rv = long_to_jlong(freeSpace.QuadPart);
                break;
            case java_io_FileSystem_SPACE_USABLE:
                rv = long_to_jlong(usableSpace.QuadPart);
                break;
            default:
                assert(0);
            }
        }
    }

    free(pathbuf);
    return rv;
}

// pathname is expected to be either null or to contain the root
// of the path terminated by a backslash
JNIEXPORT jint JNICALL
Java_java_io_WinNTFileSystem_getNameMax0(JNIEnv *env, jobject this,
                                         jstring pathname)
{
    BOOL res = 0;
    DWORD maxComponentLength;

    if (pathname == NULL) {
            res = GetVolumeInformationW(NULL,
                                        NULL,
                                        0,
                                        NULL,
                                        &maxComponentLength,
                                        NULL,
                                        NULL,
                                        0);
    } else {
        WITH_UNICODE_STRING(env, pathname, path) {
            res = GetVolumeInformationW(path,
                                        NULL,
                                        0,
                                        NULL,
                                        &maxComponentLength,
                                        NULL,
                                        NULL,
                                        0);
        } END_UNICODE_STRING(env, path);
    }

    if (res == 0) {
        JNU_ThrowIOExceptionWithLastError(env,
            "Could not get maximum component length");
    }

    return (jint)maxComponentLength;
}
