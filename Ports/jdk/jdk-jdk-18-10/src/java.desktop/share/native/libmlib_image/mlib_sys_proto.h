/*
 * Copyright (c) 1997, 2003, Oracle and/or its affiliates. All rights reserved.
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


#ifndef __ORIG_MLIB_SYS_PROTO_H
#define __ORIG_MLIB_SYS_PROTO_H

#if defined ( __MEDIALIB_OLD_NAMES_ADDED )
#include <../include/mlib_sys_proto.h>
#endif /* defined ( __MEDIALIB_OLD_NAMES_ADDED ) */

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#if defined ( _MSC_VER )
#if ! defined ( __MEDIALIB_OLD_NAMES )
#define __MEDIALIB_OLD_NAMES
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
#endif /* defined ( _MSC_VER ) */


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_malloc mlib_malloc
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
void * __mlib_malloc(mlib_u32 size);

#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_realloc mlib_realloc
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
void * __mlib_realloc(void *ptr,
                      mlib_u32 size);

#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_free mlib_free
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
void  __mlib_free(void *ptr);

#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_memset mlib_memset
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
void * __mlib_memset(void *s,
                     mlib_s32 c,
                     mlib_u32 n);

#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_memcpy mlib_memcpy
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
void * __mlib_memcpy(void *s1,
                     void *s2,
                     mlib_u32 n);

#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_memmove mlib_memmove
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
void * __mlib_memmove(void *s1,
                      void *s2,
                      mlib_u32 n);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_version mlib_version
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
char * __mlib_version();

#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* __ORIG_MLIB_SYS_PROTO_H */
