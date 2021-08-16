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

#ifndef _JVM_IO_H_
#define _JVM_IO_H_

#include <sys/stat.h>

#include "jni.h"
#include "jvm_md.h"

#ifdef __cplusplus
extern "C" {
#endif

/*
 * The standard printing functions supported by the Java VM. (Should they
 * be renamed to JVM_* in the future?
 */

/* jio_snprintf() and jio_vsnprintf() behave like snprintf(3) and vsnprintf(3),
 *  respectively, with the following differences:
 * - The string written to str is always zero-terminated, also in case of
 *   truncation (count is too small to hold the result string), unless count
 *   is 0. In case of truncation count-1 characters are written and '\0'
 *   appendend.
 * - If count is too small to hold the whole string, -1 is returned across
 *   all platforms. */

JNIEXPORT int
jio_vsnprintf(char *str, size_t count, const char *fmt, va_list args);

JNIEXPORT int
jio_snprintf(char *str, size_t count, const char *fmt, ...);

JNIEXPORT int
jio_fprintf(FILE *, const char *fmt, ...);

JNIEXPORT int
jio_vfprintf(FILE *, const char *fmt, va_list args);

#ifdef __cplusplus
} /* extern "C" */
#endif /* __cplusplus */

#endif /* _JVM_IO_H_ */
