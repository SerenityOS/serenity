/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/* 
 * If linux/types.h is already been included, assume it has defined
 * everything we need.  (cross fingers)  Other header files may have 
 * also defined the types that we need.
 */
#if (!defined(_LINUX_TYPES_H) && !defined(_BLKID_TYPES_H) && !defined(_EXT2_TYPES_H))
#    define _EXT2_TYPES_H

#    define __S8_TYPEDEF __signed__ char
#    define __U8_TYPEDEF unsigned char
#    define __S16_TYPEDEF __signed__ short
#    define __U16_TYPEDEF unsigned short
#    define __S32_TYPEDEF __signed__ int
#    define __U32_TYPEDEF unsigned int
#    define __S64_TYPEDEF __signed__ long long
#    define __U64_TYPEDEF unsigned long long

#    ifdef __U8_TYPEDEF
typedef __U8_TYPEDEF __u8;
#    else
typedef unsigned char __u8;
#    endif

#    ifdef __S8_TYPEDEF
typedef __S8_TYPEDEF __s8;
#    else
typedef signed char __s8;
#    endif

#    ifdef __U16_TYPEDEF
typedef __U16_TYPEDEF __u16;
#    else
#        if (4 == 2)
typedef unsigned int __u16;
#        else
#            if (2 == 2)
typedef unsigned short __u16;
#            else
? == error : undefined 16 bit type
#            endif /* SIZEOF_SHORT == 2 */
#        endif     /* SIZEOF_INT == 2 */
#    endif         /* __U16_TYPEDEF */

#    ifdef __S16_TYPEDEF
typedef __S16_TYPEDEF __s16;
#    else
#        if (4 == 2)
typedef int __s16;
#        else
#            if (2 == 2)
typedef short __s16;
#            else
        ? == error
        : undefined 16 bit type
#            endif /* SIZEOF_SHORT == 2 */
#        endif     /* SIZEOF_INT == 2 */
#    endif         /* __S16_TYPEDEF */

#    ifdef __U32_TYPEDEF
typedef __U32_TYPEDEF __u32;
#    else
#        if (4 == 4)
typedef unsigned int __u32;
#        else
#            if (4 == 4)
typedef unsigned long __u32;
#            else
#                if (2 == 4)
        typedef unsigned short __u32;
#                else
            ? == error
            : undefined 32 bit type
#                endif /* SIZEOF_SHORT == 4 */
#            endif     /* SIZEOF_LONG == 4 */
#        endif         /* SIZEOF_INT == 4 */
#    endif             /* __U32_TYPEDEF */

#    ifdef __S32_TYPEDEF
typedef __S32_TYPEDEF __s32;
#    else
#        if (4 == 4)
typedef int __s32;
#        else
#            if (4 == 4)
typedef long __s32;
#            else
#                if (2 == 4)
typedef short __s32;
#                else
                ? == error
                : undefined 32 bit type
#                endif /* SIZEOF_SHORT == 4 */
#            endif     /* SIZEOF_LONG == 4 */
#        endif         /* SIZEOF_INT == 4 */
#    endif             /* __S32_TYPEDEF */

#    ifdef __U64_TYPEDEF
typedef __U64_TYPEDEF __u64;
#    else
#        if (4 == 8)
typedef unsigned int __u64;
#        else
#            if (4 == 8)
typedef unsigned long __u64;
#            else
#                if (8 == 8)
typedef unsigned long long __u64;
#                endif /* SIZEOF_LONG_LONG == 8 */
#            endif     /* SIZEOF_LONG == 8 */
#        endif         /* SIZEOF_INT == 8 */
#    endif             /* __U64_TYPEDEF */

#    ifdef __S64_TYPEDEF
typedef __S64_TYPEDEF __s64;
#    else
#        if (4 == 8)
typedef int __s64;
#        else
#            if (4 == 8)
typedef long __s64;
#            else
#                if (8 == 8)
#                    if defined(__GNUC__)
typedef __signed__ long long __s64;
#                    else
typedef signed long long __s64;
#                    endif /* __GNUC__ */
#                endif     /* SIZEOF_LONG_LONG == 8 */
#            endif         /* SIZEOF_LONG == 8 */
#        endif             /* SIZEOF_INT == 8 */
#    endif                 /* __S64_TYPEDEF */

#    undef __S8_TYPEDEF
#    undef __U8_TYPEDEF
#    undef __S16_TYPEDEF
#    undef __U16_TYPEDEF
#    undef __S32_TYPEDEF
#    undef __U32_TYPEDEF
#    undef __S64_TYPEDEF
#    undef __U64_TYPEDEF

#endif /* _*_TYPES_H */

/* These defines are needed for the public ext2fs.h header file */
#define HAVE_SYS_TYPES_H 1
#undef WORDS_BIGENDIAN
