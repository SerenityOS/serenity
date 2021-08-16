/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
#include "splashscreen.h"
#include "jni.h"
extern void* SplashProcAddress(const char* name); /* in java_md.c */

/*
 * Prototypes of pointers to functions in splashscreen shared lib
 */
typedef int (*SplashLoadMemory_t)(void* pdata, int size);
typedef int (*SplashLoadFile_t)(const char* filename);
typedef int (*SplashInit_t)(void);
typedef void (*SplashClose_t)(void);
typedef void (*SplashSetFileJarName_t)(const char* fileName,
                                       const char* jarName);
typedef void (*SplashSetScaleFactor_t)(float scaleFactor);
typedef jboolean (*SplashGetScaledImageName_t)(const char* fileName,
                        const char* jarName, float* scaleFactor,
                        char *scaleImageName, const size_t scaleImageNameLength);
typedef int (*SplashGetScaledImgNameMaxPstfixLen_t)(const char* filename);

/*
 * This macro invokes a function from the shared lib.
 * it locates a function with SplashProcAddress on demand.
 * if SplashProcAddress fails, def value is returned.
 *
 * it is further wrapped with INVOKEV (works with functions which return
 * void and INVOKE (for all other functions). INVOKEV looks a bit ugly,
 * that's due being unable to return a value of type void in C. INVOKEV
 * works around this by using semicolon instead of return operator.
 */
#define _INVOKE(name,def,ret) \
    static void* proc = NULL; \
    if (!proc) { proc = SplashProcAddress(#name); } \
    if (!proc) { return def; } \
    ret ((name##_t)proc)

#define INVOKE(name,def) _INVOKE(name,def,return)
#define INVOKEV(name) _INVOKE(name, ,;)


int     DoSplashLoadMemory(void* pdata, int size) {
    INVOKE(SplashLoadMemory, 0)(pdata, size);
}

int     DoSplashLoadFile(const char* filename) {
    INVOKE(SplashLoadFile, 0)(filename);
}

int     DoSplashInit(void) {
    INVOKE(SplashInit, 0)();
}

void    DoSplashClose(void) {
    INVOKEV(SplashClose)();
}

void    DoSplashSetFileJarName(const char* fileName, const char* jarName) {
    INVOKEV(SplashSetFileJarName)(fileName, jarName);
}

void    DoSplashSetScaleFactor(float scaleFactor) {
    INVOKEV(SplashSetScaleFactor)(scaleFactor);
}

jboolean DoSplashGetScaledImageName(const char* fileName, const char* jarName,
           float* scaleFactor, char *scaledImageName, const size_t scaledImageNameLength) {
        INVOKE(SplashGetScaledImageName, 0)(fileName, jarName, scaleFactor,
                                            scaledImageName, scaledImageNameLength);
}

int     DoSplashGetScaledImgNameMaxPstfixLen(const char *fileName) {
    INVOKE(SplashGetScaledImgNameMaxPstfixLen, 0)(fileName);
}

