/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "FileSystemSupport_md.h"

/**
 * Post-process the given URI path string if necessary.  This is used on
 * win32, e.g., to transform "/c:/foo" into "c:/foo".  The path string
 * still has slash separators; code in the File class will translate them
 * after this method returns.
 */
char* fromURIPath(const char* path);

/**
 * Return the basen path of the given pathname. If the string is already
 * the base path then it is simply returned.
 */
char* basePath(const char* path);

/**
 * Convert the given pathname string to normal form.  If the string is
 * already in normal form then it is simply returned.
 */
char* normalize(const char* path);

/**
 * Tell whether or not the given abstract pathname is absolute.
 */
int isAbsolute(const char * path);

/**
 * Resolve the child pathname string against the parent.
 */
char* resolve(const char* parent, const char* child);
