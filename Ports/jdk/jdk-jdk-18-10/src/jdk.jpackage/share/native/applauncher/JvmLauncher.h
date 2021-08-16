/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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


#ifndef JvmLauncher_h
#define JvmLauncher_h


#include "jni.h" /* JNIEXPORT */


#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    const char* jliLibPath;
    int jliLaunchArgc;
    char** jliLaunchArgv;
} JvmlLauncherData;

typedef void* JvmlLauncherHandle;

typedef void (*JvmlLauncherAPI_CloseHandleFunc)(JvmlLauncherHandle);
typedef int (*JvmlLauncherAPI_GetJvmlLauncherDataSizeFunc)(JvmlLauncherHandle);
typedef JvmlLauncherData* (*JvmlLauncherAPI_InitJvmlLauncherDataFunc)(JvmlLauncherHandle, void*, int);

typedef struct {
    JvmlLauncherAPI_CloseHandleFunc closeHandle;
    JvmlLauncherAPI_GetJvmlLauncherDataSizeFunc getJvmlLauncherDataSize;
    JvmlLauncherAPI_InitJvmlLauncherDataFunc initJvmlLauncherData;
} JvmlLauncherAPI;

typedef JvmlLauncherAPI* (*JvmlLauncherAPI_GetAPIFunc)(void);

JNIEXPORT JvmlLauncherAPI* jvmLauncherGetAPI(void);

static inline void jvmLauncherCloseHandle(JvmlLauncherAPI* api, JvmlLauncherHandle h) {
    (*api->closeHandle)(h);
}

static inline int jvmLauncherGetJvmlLauncherDataSize(JvmlLauncherAPI* api,
                                                        JvmlLauncherHandle h) {
    return (*api->getJvmlLauncherDataSize)(h);
}

static inline JvmlLauncherData* jvmLauncherInitJvmlLauncherData(JvmlLauncherAPI* api,
                            JvmlLauncherHandle h, void* ptr, int bufferSize) {
    return (*api->initJvmlLauncherData)(h, ptr, bufferSize);
}

JvmlLauncherData* jvmLauncherCreateJvmlLauncherData(JvmlLauncherAPI* api, JvmlLauncherHandle h);
int jvmLauncherStartJvm(JvmlLauncherData* jvmArgs, void* JLI_Launch);

void jvmLauncherLog(const char* format, ...);

#define JP_LOG_ERRMSG(msg) do { jvmLauncherLog((msg)); } while (0)
#define JP_LOG_ERRNO JP_LOG_ERRMSG(strerror(errno))
#define JP_LOG_TRACE jvmLauncherLog


#ifdef __cplusplus
}
#endif


#ifdef __cplusplus

#include "tstrings.h"

class CfgFile;


class Jvm {
public:
    Jvm();
    ~Jvm();

    Jvm& initFromConfigFile(const CfgFile& cfgFile);

    Jvm& addArgument(const tstring& value) {
        args.push_back(value);
        return *this;
    }

    Jvm& setPath(const tstring& v) {
        jvmPath = v;
        return *this;
    }

    tstring getPath() const {
        return jvmPath;
    }

    bool isWithSplash() const;

    void launch();

    JvmlLauncherHandle exportLauncher() const;

private:
    tstring jvmPath;
    tstring_array args;
};

#endif // #ifdef __cplusplus

#endif // JvmLauncher_h
