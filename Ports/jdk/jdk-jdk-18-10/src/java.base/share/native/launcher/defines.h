/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _DEFINES_H
#define _DEFINES_H

#include "java.h"

#define STR_HELPER(x) #x
#define STR(x) STR_HELPER(x)

/*
 * This file contains commonly defined constants used only by main.c
 * and should not be included by another file.
 */
#ifndef VERSION_STRING
/* make sure the compilation fails */
#error "VERSION_STRING must be defined"
#endif

/* Unused, but retained for JLI_Launch compatibility*/
#define DOT_VERSION "0.0"

#ifdef JAVA_ARGS
#ifdef PROGNAME
static const char* const_progname = PROGNAME;
#else
static char* const_progname = NULL;
#endif
static const char* const_jargs[] = JAVA_ARGS;
#ifdef EXTRA_JAVA_ARGS
static const char* const_extra_jargs[] = EXTRA_JAVA_ARGS;
#else
static const char** const_extra_jargs = NULL;
#endif
#else  /* !JAVA_ARGS */
#ifdef EXTRA_JAVA_ARGS
#error "EXTRA_JAVA_ARGS defined without JAVA_ARGS"
#endif
static const char* const_progname = "java";
static const char** const_jargs = NULL;
static const char** const_extra_jargs = NULL;
static const char* const_appclasspath[] = { NULL };
#endif /* JAVA_ARGS */

#ifdef LAUNCHER_NAME
static const char* const_launcher = LAUNCHER_NAME;
#else  /* LAUNCHER_NAME */
static char* const_launcher = NULL;
#endif /* LAUNCHER_NAME */

#ifdef EXPAND_CLASSPATH_WILDCARDS
static const jboolean const_cpwildcard = JNI_TRUE;
#else
static const jboolean const_cpwildcard = JNI_FALSE;
#endif /* EXPAND_CLASSPATH_WILDCARDS */

#ifdef ENABLE_ARG_FILES
static const jboolean const_disable_argfile = JNI_FALSE;
#else
static const jboolean const_disable_argfile = JNI_TRUE;
#endif
#endif /*_DEFINES_H */
