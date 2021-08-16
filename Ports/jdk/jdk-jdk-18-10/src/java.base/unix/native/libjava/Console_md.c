/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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
#include <unistd.h>
#include <termios.h>

JNIEXPORT jboolean JNICALL
Java_java_io_Console_istty(JNIEnv *env, jclass cls)
{
    return isatty(fileno(stdin)) && isatty(fileno(stdout));
}

JNIEXPORT jstring JNICALL
Java_java_io_Console_encoding(JNIEnv *env, jclass cls)
{
    return NULL;
}

JNIEXPORT jboolean JNICALL
Java_java_io_Console_echo(JNIEnv *env,
                          jclass cls,
                          jboolean on)
{
    struct termios tio;
    jboolean old;
    int tty = fileno(stdin);
    if (tcgetattr(tty, &tio) == -1) {
        JNU_ThrowIOExceptionWithLastError(env, "tcgetattr failed");
        return !on;
    }
    old = (tio.c_lflag & ECHO) != 0;
    if (on) {
        tio.c_lflag |= ECHO;
    } else {
        tio.c_lflag &= ~ECHO;
    }
    if (tcsetattr(tty, TCSANOW, &tio) == -1) {
        JNU_ThrowIOExceptionWithLastError(env, "tcsetattr failed");
    }
    return old;
}
