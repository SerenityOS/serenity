/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _JAVA_PROPS_H
#define _JAVA_PROPS_H

#include <jni_util.h>

/* The preferred native type for storing text on the current OS */
#ifdef WIN32
#include <tchar.h>
typedef WCHAR nchar;
#else
typedef char nchar;
#endif

typedef struct {
    char *os_name;
    char *os_version;
    char *os_arch;

#ifdef JDK_ARCH_ABI_PROP_NAME
    char *sun_arch_abi;
#endif

    nchar *tmp_dir;
    nchar *user_dir;

    char *file_separator;
    char *path_separator;
    char *line_separator;

    nchar *user_name;
    nchar *user_home;

    char *format_language;
    char *display_language;
    char *format_script;
    char *display_script;
    char *format_country;
    char *display_country;
    char *format_variant;
    char *display_variant;
    char *encoding;
    char *sun_jnu_encoding;
    char *sun_stdout_encoding;
    char *sun_stderr_encoding;

    char *unicode_encoding;     /* The default endianness of unicode
                                    i.e. UnicodeBig or UnicodeLittle   */

    const char *cpu_isalist;    /* list of supported instruction sets */

    char *cpu_endian;           /* endianness of platform */

    char *data_model;           /* 32 or 64 bit data model */

    char *patch_level;          /* patches/service packs installed */

#ifdef MACOSX
    // These are for proxy-related information.
    // Note that if these platform-specific extensions get out of hand we should make a new
    // structure for them and #include it here.
    int httpProxyEnabled;
    char *httpHost;
    char *httpPort;

    int httpsProxyEnabled;
    char *httpsHost;
    char *httpsPort;

    int ftpProxyEnabled;
    char *ftpHost;
    char *ftpPort;

    int socksProxyEnabled;
    char *socksHost;
    char *socksPort;

    char *exceptionList;
#endif

} java_props_t;

java_props_t *GetJavaProperties(JNIEnv *env);
jstring GetStringPlatform(JNIEnv *env, nchar* str);

#endif /* _JAVA_PROPS_H */
