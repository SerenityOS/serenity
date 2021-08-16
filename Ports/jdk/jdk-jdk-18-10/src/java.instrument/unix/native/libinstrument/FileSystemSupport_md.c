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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "FileSystemSupport_md.h"

/*
 * Solaris/Linux implementation of the file system support functions.
 */

#define slash           '/'

char* basePath(const char* path) {
    char* last = strrchr(path, slash);
    if (last == NULL) {
        return (char*)path;
    } else {
        int len = last - path;
        char* str = (char*)malloc(len+1);
        if (str == NULL) {
            fprintf(stderr, "OOM error in native tmp buffer allocation");
            return NULL;
        }
        if (len > 0) {
            memcpy(str, path, len);
        }
        str[len] = '\0';
        return str;
    }
}

int isAbsolute(const char* path) {
    return (path[0] == slash) ? 1 : 0;
}

/* Ported from src/solaris/classes/java/io/UnixFileSystem.java */

/* A normal Unix pathname contains no duplicate slashes and does not end
   with a slash.  It may be the empty string. */

/* Normalize the given pathname, whose length is len, starting at the given
   offset; everything before this offset is already normal. */
static char* normalizePath(const char* pathname, int len, int off) {
    char* sb;
    int sbLen, i, n;
    char prevChar;

    if (len == 0) return (char*)pathname;
    n = len;
    while ((n > 0) && (pathname[n - 1] == slash)) n--;
    if (n == 0) return strdup("/");

    sb = (char*)malloc(strlen(pathname)+1);
    if (sb == NULL) {
        fprintf(stderr, "OOM error in native tmp buffer allocation");
        return NULL;
    }
    sbLen = 0;

    if (off > 0) {
        memcpy(sb, pathname, off);
        sbLen = off;
    }

    prevChar = 0;
    for (i = off; i < n; i++) {
        char c = pathname[i];
        if ((prevChar == slash) && (c == slash)) continue;
        sb[sbLen++] = c;
        prevChar = c;
    }
    return sb;
}

/* Check that the given pathname is normal.  If not, invoke the real
   normalizer on the part of the pathname that requires normalization.
   This way we iterate through the whole pathname string only once. */
char* normalize(const char* pathname) {
    int i;
    int n = strlen(pathname);
    char prevChar = 0;
    for (i = 0; i < n; i++) {
        char c = pathname[i];
        if ((prevChar == slash) && (c == slash))
            return normalizePath(pathname, n, i - 1);
        prevChar = c;
    }
    if (prevChar == slash) return normalizePath(pathname, n, n - 1);
    return (char*)pathname;
}

char* resolve(const char* parent, const char* child) {
    int len;
    char* theChars;
    int pn = strlen(parent);
    int cn = strlen(child);
    int childStart = 0;
    int parentEnd = pn;

    if (pn > 0 && parent[pn-1] == slash) {
        parentEnd--;
    }
    len = parentEnd + cn - childStart;
    if (child[0] == slash) {
        theChars = (char*)malloc(len+1);
        if (theChars == NULL) {
            fprintf(stderr, "OOM error in native tmp buffer allocation");
            return NULL;
        }
        if (parentEnd > 0)
            memcpy(theChars, parent, parentEnd);
        if (cn > 0)
            memcpy(theChars+parentEnd, child, cn);
        theChars[len] = '\0';
    } else {
        theChars = (char*)malloc(len+2);
        if (theChars == NULL) {
            fprintf(stderr, "OOM error in native tmp buffer allocation");
            return NULL;
        }
        if (parentEnd > 0)
            memcpy(theChars, parent, parentEnd);
        theChars[parentEnd] = slash;
        if (cn > 0)
            memcpy(theChars+parentEnd+1, child, cn);
        theChars[len+1] = '\0';
    }
    return theChars;
}

char* fromURIPath(const char* path) {
    int len = strlen(path);
    if (len > 1 && path[len-1] == slash) {
        // "/foo/" --> "/foo", but "/" --> "/"
        char* str = (char*)malloc(len);
        if (str == NULL)
        {
            fprintf(stderr, "OOM error in native tmp buffer allocation");
            return NULL;
        }
        memcpy(str, path, len-1);
        str[len-1] = '\0';
        return str;
    } else {
        return (char*)path;
    }
}
