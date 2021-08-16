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

#ifndef JAVA_MD_H
#define JAVA_MD_H

/*
 * This file contains common defines and includes for unix.
 */
#include <limits.h>
#include <unistd.h>
#include <sys/param.h>
#include <dlfcn.h>
#include <pthread.h>
#include "manifest_info.h"
#include "jli_util.h"

#define PATH_SEPARATOR       ':'
#define FILESEP              "/"
#define FILE_SEPARATOR       '/'
#define IS_FILE_SEPARATOR(c) ((c) == '/')
#ifndef MAXNAMELEN
#define MAXNAMELEN           PATH_MAX
#endif

#ifdef _LP64
#define JLONG_FORMAT_SPECIFIER "%ld"
#else
#define JLONG_FORMAT_SPECIFIER "%lld"
#endif

int UnsetEnv(char *name);
char *FindExecName(char *program);
const char *SetExecname(char **argv);
const char *GetExecName();
static jboolean GetJVMPath(const char *jrepath, const char *jvmtype,
                           char *jvmpath, jint jvmpathsize);
static jboolean GetJREPath(char *path, jint pathsize, jboolean speculative);

#if defined(_AIX)
#include "java_md_aix.h"
#endif

#if defined(MACOSX)
#include <crt_externs.h>
#define environ (*_NSGetEnviron())
#else
extern char **environ;
#endif

#endif /* JAVA_MD_H */
