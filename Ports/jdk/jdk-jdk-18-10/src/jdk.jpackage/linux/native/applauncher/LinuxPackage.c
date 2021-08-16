/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
#include <errno.h>
#include <linux/limits.h>
#include <unistd.h>
#include <libgen.h>
#include "JvmLauncher.h"
#include "LinuxPackage.h"


static char* getModulePath(void) {
    char modulePath[PATH_MAX] = { 0 };
    ssize_t modulePathLen = 0;
    char* result = 0;

    modulePathLen = readlink("/proc/self/exe", modulePath,
                                                    sizeof(modulePath) - 1);
    if (modulePathLen < 0) {
        JP_LOG_ERRNO;
        return 0;
    }
    modulePath[modulePathLen] = '\0';
    result = strdup(modulePath);
    if (!result) {
        JP_LOG_ERRNO;
    }

    return result;
}


# define PACKAGE_TYPE_UNKNOWN 0
# define PACKAGE_TYPE_RPM 1
# define PACKAGE_TYPE_DEB 2

typedef struct {
    char* name;
    int type;
} PackageDesc;


static void freePackageDesc(PackageDesc* desc) {
    if (desc) {
        free(desc->name);
        free(desc);
    }
}


static PackageDesc* createPackageDesc(void) {
    PackageDesc* result = 0;

    result = malloc(sizeof(PackageDesc));
    if (!result) {
        JP_LOG_ERRNO;
        goto cleanup;
    }

    result->type = PACKAGE_TYPE_UNKNOWN;
    result->name = 0;

cleanup:
    return result;
}


static PackageDesc* initPackageDesc(PackageDesc* desc, const char* str,
                                                                int type) {
    char *newStr = strdup(str);
    if (!newStr) {
        JP_LOG_ERRNO;
        return 0;
    }

    free(desc->name);
    desc->name = newStr;
    desc->type = type;
    return desc;
}


#define POPEN_CALLBACK_USE 1
#define POPEN_CALLBACK_IGNORE 0

typedef int (*popenCallbackType)(void*, const char*);

static int popenCommand(const char* cmdlineFormat, const char* arg,
                            popenCallbackType callback, void* callbackData) {
    char* cmdline = 0;
    FILE *stream = 0;
    const size_t cmdlineLenth = strlen(cmdlineFormat) + strlen(arg);
    char* strBufBegin = 0;
    char* strBufEnd = 0;
    char* strBufNextChar = 0;
    char* strNewBufBegin = 0;
    size_t strBufCapacity = 0;
    int callbackMode = POPEN_CALLBACK_USE;
    int exitCode = -1;
    int c;

    cmdline = malloc(cmdlineLenth + 1 /* \0 */);
    if (!cmdline) {
        JP_LOG_ERRNO;
        goto cleanup;
    }

#if defined(__GNUC__) && __GNUC__ >= 5
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wformat-nonliteral"
#endif
    if (0 > snprintf(cmdline, cmdlineLenth, cmdlineFormat, arg)) {
        JP_LOG_ERRNO;
        goto cleanup;
    }
#if defined(__GNUC__) && __GNUC__ >= 5
#pragma GCC diagnostic pop
#endif

    JP_LOG_TRACE("popen: (%s)", cmdline);

    stream = popen(cmdline, "r");
    if (!stream) {
        JP_LOG_ERRNO;
        goto cleanup;
    }

    for (;;) {
        c = fgetc(stream);
        if((EOF == c || '\n' == c)) {
            if (POPEN_CALLBACK_USE == callbackMode
                                            && strBufBegin != strBufNextChar) {
                *strBufNextChar = 0;
                JP_LOG_TRACE("popen: [%s]", strBufBegin);
                callbackMode = (*callback)(callbackData, strBufBegin);
                strBufNextChar = strBufBegin;
            }

            if (EOF == c) {
                break;
            }

            continue;
        }

        if (strBufNextChar == strBufEnd) {
            /* Double buffer size */
            strBufCapacity = strBufCapacity * 2 + 1;
            strNewBufBegin = realloc(strBufBegin, strBufCapacity);
            if (!strNewBufBegin) {
                JP_LOG_ERRNO;
                goto cleanup;
            }

            strBufNextChar = strNewBufBegin + (strBufNextChar - strBufBegin);
            strBufEnd = strNewBufBegin + strBufCapacity;
            strBufBegin = strNewBufBegin;
        }

        *strBufNextChar++ = (char)c;
    }

cleanup:
    if (stream) {
        exitCode = pclose(stream);
    }

    if (strBufBegin) {
        free(strBufBegin);
    }

    free(cmdline);

    JP_LOG_TRACE("popen: exit: %d", exitCode);
    return exitCode;
}


static char* concat(const char *x, const char *y) {
    const size_t lenX = strlen(x);
    const size_t lenY = strlen(y);

    char *result = malloc(lenX + lenY + 1 /* \0 */);
    if (!result) {
        JP_LOG_ERRNO;
    } else {
        strcpy(result, x);
        strcat(result, y);
    }

    return result;
}


static int initRpmPackage(void* desc, const char* str) {
    initPackageDesc((PackageDesc*)desc, str, PACKAGE_TYPE_RPM);
    return POPEN_CALLBACK_IGNORE;
}


static int initDebPackage(void* desc, const char* str) {
    char* colonChrPos = strchr(str, ':');
    if (colonChrPos) {
        *colonChrPos = 0;
    }
    initPackageDesc((PackageDesc*)desc, str, PACKAGE_TYPE_DEB);
    return POPEN_CALLBACK_IGNORE;
}


#define LAUNCHER_LIB_NAME "/libapplauncher.so"

static int findLauncherLib(void* launcherLibPath, const char* str) {
    char* buf = 0;
    const size_t strLen = strlen(str);
    const size_t launcherLibNameLen = strlen(LAUNCHER_LIB_NAME);

    if (launcherLibNameLen <= strLen
            && !strcmp(str + strLen - launcherLibNameLen, LAUNCHER_LIB_NAME)) {
        buf = strdup(str);
        if (!buf) {
            JP_LOG_ERRNO;
        } else {
            *(char**)launcherLibPath = buf;
        }
        return POPEN_CALLBACK_IGNORE;
    }
    return POPEN_CALLBACK_USE;
}


static PackageDesc* findOwnerOfFile(const char* path) {
    int popenStatus = -1;
    PackageDesc* pkg = 0;

    pkg = createPackageDesc();
    if (!pkg) {
        return 0;
    }

    popenStatus = popenCommand(
            "rpm --queryformat '%{NAME}' -qf '%s' 2>/dev/null", path,
            initRpmPackage, pkg);
    if (popenStatus) {
        pkg->type = PACKAGE_TYPE_UNKNOWN;
        popenStatus = popenCommand("dpkg -S '%s' 2>/dev/null", path,
                                                        initDebPackage, pkg);
    }

    if (popenStatus) {
        pkg->type = PACKAGE_TYPE_UNKNOWN;
    }

    if (PACKAGE_TYPE_UNKNOWN == pkg->type || !pkg->name) {
        freePackageDesc(pkg);
        pkg = 0;
    }

    if (pkg) {
        JP_LOG_TRACE("owner pkg: (%s|%d)", pkg->name, pkg->type);
    }

    return pkg;
}


char* getJvmLauncherLibPath(void) {
    char* modulePath = 0;
    char* appImageDir = 0;
    char* launcherLibPath = 0;
    const char* pkgQueryCmd = 0;
    int popenStatus = -1;
    PackageDesc* pkg = 0;

    modulePath = getModulePath();
    if (!modulePath) {
        goto cleanup;
    }

    pkg = findOwnerOfFile(modulePath);
    if (!pkg) {
        /* Not a package install */
        /* Launcher should be in "bin" subdirectory of app image. */
        /* Launcher lib should be in "lib" subdirectory of app image. */
        appImageDir = dirname(dirname(modulePath));
        launcherLibPath = concat(appImageDir, "/lib" LAUNCHER_LIB_NAME);
    } else {
        if (PACKAGE_TYPE_RPM == pkg->type) {
            pkgQueryCmd = "rpm -ql '%s' 2>/dev/null";
        } else if (PACKAGE_TYPE_DEB == pkg->type) {
            pkgQueryCmd = "dpkg -L '%s' 2>/dev/null";
        } else {
            /* Should never happen */
            JP_LOG_ERRMSG("Internal error");
            goto cleanup;
        }

        popenStatus = popenCommand(pkgQueryCmd, pkg->name, findLauncherLib,
                                                        &launcherLibPath);
        if (popenStatus) {
            free(launcherLibPath);
            launcherLibPath = NULL;
            goto cleanup;
        }
    }

cleanup:
    free(modulePath);
    freePackageDesc(pkg);

    return launcherLibPath;
}
