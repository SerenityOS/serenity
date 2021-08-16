/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include "java_io_Console.h"

#include <stdlib.h>
#include <Wincon.h>

static HANDLE hStdOut = INVALID_HANDLE_VALUE;
static HANDLE hStdIn = INVALID_HANDLE_VALUE;
JNIEXPORT jboolean JNICALL
Java_java_io_Console_istty(JNIEnv *env, jclass cls)
{
    if (hStdIn == INVALID_HANDLE_VALUE &&
        (hStdIn = GetStdHandle(STD_INPUT_HANDLE)) == INVALID_HANDLE_VALUE) {
        return JNI_FALSE;
    }
    if (hStdOut == INVALID_HANDLE_VALUE &&
        (hStdOut = GetStdHandle(STD_OUTPUT_HANDLE)) == INVALID_HANDLE_VALUE) {
        return JNI_FALSE;
    }
    if (GetFileType(hStdIn) != FILE_TYPE_CHAR ||
        GetFileType(hStdOut) != FILE_TYPE_CHAR)
        return JNI_FALSE;
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_java_io_Console_encoding(JNIEnv *env, jclass cls)
{
    char buf[64];
    int cp = GetConsoleCP();
    if (cp >= 874 && cp <= 950)
        sprintf(buf, "ms%d", cp);
    else if (cp == 65001)
        sprintf(buf, "UTF-8");
    else
        sprintf(buf, "cp%d", cp);
    return JNU_NewStringPlatform(env, buf);
}

JNIEXPORT jboolean JNICALL
Java_java_io_Console_echo(JNIEnv *env, jclass cls, jboolean on)
{
    DWORD fdwMode;
    jboolean old;
    if (! GetConsoleMode(hStdIn, &fdwMode)) {
        JNU_ThrowIOExceptionWithLastError(env, "GetConsoleMode failed");
        return !on;
    }
    old = (fdwMode & ENABLE_ECHO_INPUT) != 0;
    if (on) {
        fdwMode |= ENABLE_ECHO_INPUT;
    } else {
        fdwMode &= ~ENABLE_ECHO_INPUT;
    }
    if (! SetConsoleMode(hStdIn, fdwMode)) {
        JNU_ThrowIOExceptionWithLastError(env, "SetConsoleMode failed");
    }
    return old;
}
