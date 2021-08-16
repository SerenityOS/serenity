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
#include <malloc.h>

#include "FileSystemSupport_md.h"

/*
 * Windows implementation of file system support functions
 */

#define slash           '\\'
#define altSlash        '/'

static int isSlash(char c) {
    return (c == '\\') || (c == '/');
}

static int isLetter(char c) {
    return ((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'));
}

char* basePath(const char* path) {
    char* pos = strchr(path, slash);
    char* last = NULL;
    while (pos != NULL) {
        last = pos;
        pos++;
        pos = strchr(pos, slash);
    }
    if (last == NULL) {
        return (char*)path;
    } else {
        int len = (int)(last - path);
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



/* -- Normalization - src/windows/classes/java/io/Win32FileSystem.java */


/* A normal Win32 pathname contains no duplicate slashes, except possibly
 * for a UNC prefix, and does not end with a slash.  It may be the empty
 * string.  Normalized Win32 pathnames have the convenient property that
 * the length of the prefix almost uniquely identifies the type of the path
 * and whether it is absolute or relative:
 *
 *      0  relative to both drive and directory
 *      1  drive-relative (begins with '\\')
 *      2  absolute UNC (if first char is '\\'),
 *         else directory-relative (has form "z:foo")
 *      3  absolute local pathname (begins with "z:\\")
 */
static int normalizePrefix(const char* path, int len, char* sb, int* sbLen) {
    char c;
    int src = 0;
    while ((src < len) && isSlash(path[src])) src++;
    if ((len - src >= 2)
        && isLetter(c = path[src])
        && path[src + 1] == ':') {
        /* Remove leading slashes if followed by drive specifier.
           This hack is necessary to support file URLs containing drive
           specifiers (e.g., "file://c:/path").  As a side effect,
           "/c:/path" can be used as an alternative to "c:/path". */
        sb[(*sbLen)++] = c;
        sb[(*sbLen)++] = ':';
        src += 2;
    } else {
        src = 0;
        if ((len >= 2)
            && isSlash(path[0])
            && isSlash(path[1])) {
            /* UNC pathname: Retain first slash; leave src pointed at
               second slash so that further slashes will be collapsed
               into the second slash.  The result will be a pathname
               beginning with "\\\\" followed (most likely) by a host
               name. */
            src = 1;
            sb[(*sbLen)++] = slash;
        }
    }
    return src;
}

/*
 * Normalize the given pathname, whose length is len, starting at the given
 * offset; everything before this offset is already normal.
 */
static char* normalizePath(const char* path, int len, int off) {
    int src;
    char* sb;
    int sbLen;

    if (len == 0) return (char*)path;
    if (off < 3) off = 0;       /* Avoid fencepost cases with UNC pathnames */

    sb = (char*)malloc(len+1);
    if (sb == NULL) {
        fprintf(stderr, "OOM error in native tmp buffer allocation");
        return NULL;
    }
    sbLen = 0;

    if (off == 0) {
        /* Complete normalization, including prefix */
        src = normalizePrefix(path, len, sb, &sbLen);
    } else {
        /* Partial normalization */
        src = off;
        memcpy(sb+sbLen, path, off);
        sbLen += off;
    }

    /* Remove redundant slashes from the remainder of the path, forcing all
       slashes into the preferred slash */
    while (src < len) {
        char c = path[src++];
        if (isSlash(c)) {
            while ((src < len) && isSlash(path[src])) src++;
            if (src == len) {
                /* Check for trailing separator */
                if ((sbLen == 2) && (sb[1] == ':')) {
                    /* "z:\\" */
                    sb[sbLen++] = slash;
                    break;
                }
                if (sbLen == 0) {
                    /* "\\" */
                    sb[sbLen++] = slash;
                    break;
                }
                if ((sbLen == 1) && (isSlash(sb[0]))) {
                    /* "\\\\" is not collapsed to "\\" because "\\\\" marks
                       the beginning of a UNC pathname.  Even though it is
                       not, by itself, a valid UNC pathname, we leave it as
                       is in order to be consistent with the win32 APIs,
                       which treat this case as an invalid UNC pathname
                       rather than as an alias for the root directory of
                       the current drive. */
                    sb[sbLen++] = slash;
                    break;
                }
                /* Path does not denote a root directory, so do not append
                   trailing slash */
                break;
            } else {
                sb[sbLen++] = slash;
            }
        } else {
            sb[sbLen++] = c;
        }
    }

    sb[sbLen] = '\0';
    return sb;
}

/*
 * Check that the given pathname is normal.  If not, invoke the real
 * normalizer on the part of the pathname that requires normalization.
 * This way we iterate through the whole pathname string only once.
 */
char* normalize(char* path) {
    int n = (int)strlen(path);
    int i;
    char c = 0;
    int prev = 0;
    for (i = 0; i < n; i++) {
        char c = path[i];
        if (c == altSlash)
            return normalizePath(path, n, (prev == slash) ? i - 1 : i);
        if ((c == slash) && (prev == slash) && (i > 1))
            return normalizePath(path, n, i - 1);
        if ((c == ':') && (i > 1))
            return normalizePath(path, n, 0);
        prev = c;
    }
    if (prev == slash)
        return normalizePath(path, n, n - 1);
    return path;
}


/* -- Resolution - src/windows/classes/java/io/Win32FileSystem.java */


char* resolve(const char* parent, const char* child) {
    char* c;
    char* theChars;
    int parentEnd, childStart, len;

    int pn = (int)strlen(parent);
    int cn = (int)strlen(child);

    if (pn == 0) return (char*)child;
    if (cn == 0) return (char*)parent;

    c = (char*)child;
    childStart = 0;
    parentEnd = pn;

    if ((cn > 1) && (c[0] == slash)) {
        if (c[1] == slash) {
            /* Drop prefix when child is a UNC pathname */
            childStart = 2;
        } else {
            /* Drop prefix when child is drive-relative */
            childStart = 1;

        }
        if (cn == childStart) { // Child is double slash
            if (parent[pn - 1] == slash) {
                char* str = strdup(parent);
                str[pn-1] = '\0';
                return str;
            }
            return (char*)parent;
        }
    }

    if (parent[pn - 1] == slash)
        parentEnd--;

    len = parentEnd + cn - childStart;

    if (child[childStart] == slash) {
        theChars = (char*)malloc(len+1);
        if (theChars == NULL) {
            fprintf(stderr, "OOM error in native tmp buffer allocation");
            return NULL;
        }
        memcpy(theChars, parent, parentEnd);
        memcpy(theChars+parentEnd, child+childStart, (cn-childStart));
        theChars[len] = '\0';
    } else {
        theChars = (char*)malloc(len+2);
        if (theChars == NULL) {
            fprintf(stderr, "OOM error in native tmp buffer allocation");
            return NULL;
        }
        memcpy(theChars, parent, parentEnd);
        theChars[parentEnd] = slash;
        memcpy(theChars+parentEnd+1, child+childStart, (cn-childStart));
        theChars[len+1] = '\0';
    }
    return theChars;
}


static int prefixLength(const char* path) {
    char c0, c1;

    int n = (int)strlen(path);
    if (n == 0) return 0;
    c0 = path[0];
    c1 = (n > 1) ? path[1] : 0;
    if (c0 == slash) {
        if (c1 == slash) return 2;      /* Absolute UNC pathname "\\\\foo" */
        return 1;                       /* Drive-relative "\\foo" */
    }
    if (isLetter(c0) && (c1 == ':')) {
        if ((n > 2) && (path[2] == slash))
            return 3;           /* Absolute local pathname "z:\\foo" */
        return 2;                       /* Directory-relative "z:foo" */
    }
    return 0;                   /* Completely relative */
}


int isAbsolute(const char* path) {
    int pl = prefixLength(path);
    return (((pl == 2) && (path[0] == slash)) || (pl == 3));
}


char* fromURIPath(const char* path) {
    int start = 0;
    int len = (int)strlen(path);

    if ((len > 2) && (path[2] == ':')) {
        // "/c:/foo" --> "c:/foo"
        start = 1;
        // "c:/foo/" --> "c:/foo", but "c:/" --> "c:/"
        if ((len > 3) && path[len-1] == '/')
            len--;
    } else if ((len > 1) && path[len-1] == '/') {
        // "/foo/" --> "/foo"
        len--;
    }

    if (start == 0 && len == (int)strlen(path)) {
        return (char*)path;
    } else {
        char* p = (char*)malloc(len+1);
        if (p == NULL) {
            fprintf(stderr, "OOM error in native tmp buffer allocation");
            return NULL;
        }
        memcpy(p, path+start, len);
        p[len] = '\0';
        return p;
    }
}
