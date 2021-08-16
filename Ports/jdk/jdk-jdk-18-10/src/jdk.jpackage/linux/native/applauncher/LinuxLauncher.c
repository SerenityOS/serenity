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
#include <dlfcn.h>
#include <errno.h>
#include <linux/limits.h>
#include <unistd.h>
#include <libgen.h>
#include "JvmLauncher.h"
#include "LinuxPackage.h"


#define STATUS_FAILURE 1

typedef JvmlLauncherHandle (*JvmlLauncherAPI_CreateFunType)(int argc, char *argv[]);

static int appArgc;
static char **appArgv;


static JvmlLauncherData* initJvmlLauncherData(void) {
    char* launcherLibPath = 0;
    void* jvmLauncherLibHandle = 0;
    JvmlLauncherAPI_GetAPIFunc getApi = 0;
    JvmlLauncherAPI_CreateFunType createJvmlLauncher = 0;
    JvmlLauncherAPI* api = 0;
    JvmlLauncherHandle jvmLauncherHandle = 0;
    JvmlLauncherData* result = 0;

    launcherLibPath = getJvmLauncherLibPath();
    if (!launcherLibPath) {
        goto cleanup;
    }

    jvmLauncherLibHandle = dlopen(launcherLibPath, RTLD_NOW | RTLD_LOCAL);
    if (!jvmLauncherLibHandle) {
        JP_LOG_ERRMSG(dlerror());
        goto cleanup;
    }

    getApi = dlsym(jvmLauncherLibHandle, "jvmLauncherGetAPI");
    if (!getApi) {
        JP_LOG_ERRMSG(dlerror());
        goto cleanup;
    }

    api = (*getApi)();
    if (!api) {
        JP_LOG_ERRMSG("Failed to get JvmlLauncherAPI instance");
        goto cleanup;
    }

    createJvmlLauncher = dlsym(jvmLauncherLibHandle, "jvmLauncherCreate");
    if (!createJvmlLauncher) {
        JP_LOG_ERRMSG(dlerror());
        goto cleanup;
    }

    jvmLauncherHandle = (*createJvmlLauncher)(appArgc, appArgv);
    if (!jvmLauncherHandle) {
        goto cleanup;
    }

    result = jvmLauncherCreateJvmlLauncherData(api, jvmLauncherHandle);
    /* Handle released in jvmLauncherCreateJvmlLauncherData() */
    jvmLauncherHandle = 0;

cleanup:
    if (jvmLauncherHandle) {
        jvmLauncherCloseHandle(api, jvmLauncherHandle);
    }
    if (jvmLauncherLibHandle) {
        dlclose(jvmLauncherLibHandle);
    }
    free(launcherLibPath);

    return result;
}


static int launchJvm(JvmlLauncherData* cfg) {
    void* jliLibHandle = 0;
    void* JLI_Launch;
    int exitCode = STATUS_FAILURE;

    jliLibHandle = dlopen(cfg->jliLibPath, RTLD_NOW | RTLD_LOCAL);
    if (!jliLibHandle) {
        JP_LOG_ERRMSG(dlerror());
        goto cleanup;
    }

    JLI_Launch = dlsym(jliLibHandle, "JLI_Launch");
    if (!JLI_Launch) {
        JP_LOG_ERRMSG(dlerror());
        goto cleanup;
    }

    exitCode = jvmLauncherStartJvm(cfg, JLI_Launch);

cleanup:
    if (jliLibHandle) {
        dlclose(jliLibHandle);
    }

    return exitCode;
}


int main(int argc, char *argv[]) {
    int exitCode = STATUS_FAILURE;
    JvmlLauncherData* jvmLauncherData;

    appArgc = argc;
    appArgv = argv;

    jvmLauncherData = initJvmlLauncherData();
    if (jvmLauncherData) {
        exitCode = launchJvm(jvmLauncherData);
        free(jvmLauncherData);
    }

    return exitCode;
}
