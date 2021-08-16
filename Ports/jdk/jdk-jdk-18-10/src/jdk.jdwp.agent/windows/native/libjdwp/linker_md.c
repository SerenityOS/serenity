/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * Maintains a list of currently loaded DLLs (Dynamic Link Libraries)
 * and their associated handles. Library names are case-insensitive.
 */

#include <windows.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <io.h>
#include <stdlib.h>

#include "sys.h"

#include "util.h"
#include "path_md.h"

static void dll_build_name(char* buffer, size_t buflen,
                           const char* paths, const char* fname) {
    char *path, *paths_copy, *next_token;
    *buffer = '\0';

    paths_copy = jvmtiAllocate((int)strlen(paths) + 1);
    if (paths_copy == NULL) {
        return;
    }

    strcpy(paths_copy, paths);

    next_token = NULL;
    path = strtok_s(paths_copy, PATH_SEPARATOR, &next_token);

    while (path != NULL) {
        size_t result_len = (size_t)_snprintf(buffer, buflen, "%s\\%s.dll", path, fname);
        if (result_len >= buflen) {
            EXIT_ERROR(JVMTI_ERROR_INVALID_LOCATION, "One or more of the library paths supplied to jdwp, "
                                                     "likely by sun.boot.library.path, is too long.");
        } else if (_access(buffer, 0) == 0) {
            break;
        }
        *buffer = '\0';
        path = strtok_s(NULL, PATH_SEPARATOR, &next_token);
    }

    jvmtiDeallocate(paths_copy);
}

/*
 * From system_md.c v1.54
 */
int
dbgsysGetLastErrorString(char *buf, int len)
{
    long errval;

    if ((errval = GetLastError()) != 0) {
        /* DOS error */
        int n = FormatMessage(FORMAT_MESSAGE_FROM_SYSTEM|FORMAT_MESSAGE_IGNORE_INSERTS,
                              NULL, errval,
                              0, buf, len, NULL);
        if (n > 3) {
            /* Drop final '.', CR, LF */
            if (buf[n - 1] == '\n') n--;
            if (buf[n - 1] == '\r') n--;
            if (buf[n - 1] == '.') n--;
            buf[n] = '\0';
        }
        return n;
    }

    if (errno != 0) {
        /* C runtime error that has no corresponding DOS error code */
        const char *s = strerror(errno);
        int n = (int)strlen(s);
        if (n >= len) n = len - 1;
        strncpy(buf, s, n);
        buf[n] = '\0';
        return n;
    }

    return 0;
}

/*
 * Build a machine dependent library name out of a path and file name.
 */
void
dbgsysBuildLibName(char *holder, int holderlen, const char *pname, const char *fname)
{
    const int pnamelen = pname ? (int)strlen(pname) : 0;

    if (pnamelen == 0) {
        if (pnamelen + (int)strlen(fname) + 10 > holderlen) {
                EXIT_ERROR(JVMTI_ERROR_INVALID_LOCATION, "One or more of the library paths supplied to jdwp, "
                                                         "likely by sun.boot.library.path, is too long.");
        }
        sprintf(holder, "%s.dll", fname);
    } else {
      dll_build_name(holder, holderlen, pname, fname);
    }
}

void *
dbgsysLoadLibrary(const char * name, char *err_buf, int err_buflen)
{
    void *result = LoadLibrary(name);
    if (result == NULL) {
        /* Error message is pretty lame, try to make a better guess. */
        long errcode = GetLastError();
        if (errcode == ERROR_MOD_NOT_FOUND) {
            strncpy(err_buf, "Can't find dependent libraries", err_buflen-2);
            err_buf[err_buflen-1] = '\0';
        } else {
            dbgsysGetLastErrorString(err_buf, err_buflen);
        }
    }
    return result;
}

void dbgsysUnloadLibrary(void *handle)
{
    FreeLibrary(handle);
}

void * dbgsysFindLibraryEntry(void *handle, const char *name)
{
    return GetProcAddress(handle, name);
}
