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

#include <string.h>
#include <stdlib.h>
#include <errno.h>

#include "JvmLauncher.h"


typedef int (JNICALL *JLI_LaunchFuncType)(int argc, char ** argv,
        int jargc, const char** jargv,
        int appclassc, const char** appclassv,
        const char* fullversion,
        const char* dotversion,
        const char* pname,
        const char* lname,
        jboolean javaargs,
        jboolean cpwildcard,
        jboolean javaw,
        jint ergo);


JvmlLauncherData* jvmLauncherCreateJvmlLauncherData(
                                JvmlLauncherAPI* api, JvmlLauncherHandle h) {
    JvmlLauncherData* result = 0;
    void* buf = 0;
    int jvmLauncherDataBufferSize;

    if (!h) {
        return 0;
    }

    jvmLauncherDataBufferSize = jvmLauncherGetJvmlLauncherDataSize(api, h);
    if (jvmLauncherDataBufferSize <= 0) {
        goto cleanup;
    }

    buf = malloc(jvmLauncherDataBufferSize);
    if (!buf) {
        JP_LOG_ERRNO;
        goto cleanup;
    }

    result = jvmLauncherInitJvmlLauncherData(api, h, buf,
                                                jvmLauncherDataBufferSize);
    if (result) {
        /* Don't free the buffer in clean up. */
        buf = 0;
    }

cleanup:
    jvmLauncherCloseHandle(api, h);
    free(buf);

    return result;
}


static void dumpJvmlLauncherData(const JvmlLauncherData* jvmArgs) {
    int i = 0;
    JP_LOG_TRACE("jli lib: [%s]", jvmArgs->jliLibPath);
    for (i = 0; i < jvmArgs->jliLaunchArgc; ++i) {
        JP_LOG_TRACE("jli arg[%d]: [%s]", i, jvmArgs->jliLaunchArgv[i]);
    }
}


int jvmLauncherStartJvm(JvmlLauncherData* jvmArgs, void* JLI_Launch) {
    int exitCode;

    dumpJvmlLauncherData(jvmArgs);
    exitCode = (*((JLI_LaunchFuncType)JLI_Launch))(
        jvmArgs->jliLaunchArgc, jvmArgs->jliLaunchArgv,
        0, 0,
        0, 0,
        "",
        "",
        "java",
        "java",
        JNI_FALSE,
        JNI_FALSE,
        JNI_FALSE,
        0);

    return exitCode;
}


void jvmLauncherLog(const char* format, ...) {
    const char *withLog = getenv("JPACKAGE_DEBUG");
    if (!withLog || strcmp(withLog, "true")) {
        return;
    }

    va_list args;
    va_start(args, format);

#if defined(__GNUC__) && __GNUC__ >= 5
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wformat-nonliteral"
#endif
    vfprintf(stderr, format, args);
    fprintf(stderr, "\n");
#if defined(__GNUC__) && __GNUC__ >= 5
#pragma GCC diagnostic pop
#endif

    va_end (args);
}
