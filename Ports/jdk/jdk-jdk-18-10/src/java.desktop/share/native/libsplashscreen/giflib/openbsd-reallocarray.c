/*
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

/*    $OpenBSD: reallocarray.c,v 1.1 2014/05/08 21:43:49 deraadt Exp $    */
/*
 * Copyright (c) 2008 Otto Moerbeek <otto@drijf.net>
 * SPDX-License-Identifier: MIT
 */

#include <sys/types.h>
#include <errno.h>
#include <stdint.h>
#include <stdlib.h>

#ifndef SIZE_MAX
    #define SIZE_MAX     UINTPTR_MAX
#endif

/*
 * This is sqrt(SIZE_MAX+1), as s1*s2 <= SIZE_MAX
 * if both s1 < MUL_NO_OVERFLOW and s2 < MUL_NO_OVERFLOW
 */
#define MUL_NO_OVERFLOW    ((size_t)1 << (sizeof(size_t) * 4))

void *
openbsd_reallocarray(void *optr, size_t nmemb, size_t size)
{
    if ((nmemb >= MUL_NO_OVERFLOW || size >= MUL_NO_OVERFLOW) &&
        nmemb > 0 && SIZE_MAX / nmemb < size) {
        errno = ENOMEM;
        return NULL;
    }
    /*
     * Head off variations in realloc behavior on different
     * platforms (reported by MarkR <mrogers6@users.sf.net>)
     *
     * The behaviour of reallocarray is implementation-defined if
     * nmemb or size is zero. It can return NULL or non-NULL
     * depending on the platform.
     * https://www.securecoding.cert.org/confluence/display/c/MEM04-C.Beware+of+zero-lengthallocations
     *
     * Here are some extracts from realloc man pages on different platforms.
     *
     * void realloc( void memblock, size_t size );
     *
     * Windows:
     *
     * If there is not enough available memory to expand the block
     * to the given size, the original block is left unchanged,
     * and NULL is returned.  If size is zero, then the block
     * pointed to by memblock is freed; the return value is NULL,
     * and memblock is left pointing at a freed block.
     *
     * OpenBSD:
     *
     * If size or nmemb is equal to 0, a unique pointer to an
     * access protected, zero sized object is returned. Access via
     * this pointer will generate a SIGSEGV exception.
     *
     * Linux:
     *
     * If size was equal to 0, either NULL or a pointer suitable
     * to be passed to free() is returned.
     *
     * OS X:
     *
     * If size is zero and ptr is not NULL, a new, minimum sized
     * object is allocated and the original object is freed.
     *
     * It looks like images with zero width or height can trigger
     * this, and fuzzing behaviour will differ by platform, so
     * fuzzing on one platform may not detect zero-size allocation
     * problems on other platforms.
     */
    if (size == 0 || nmemb == 0)
        return NULL;
    return realloc(optr, size * nmemb);
}
