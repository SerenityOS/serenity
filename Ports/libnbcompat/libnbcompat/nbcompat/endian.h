/*	$NetBSD: endian.h,v 1.3 2010/01/24 12:41:21 obache Exp $	*/

/*-
 * Copyright (c) 2004 The NetBSD Foundation, Inc.
 * All rights reserved.
 *
 * This code is derived from software contributed to The NetBSD Foundation
 * by Johnny C. Lam.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE NETBSD FOUNDATION, INC. AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

#ifndef _NBCOMPAT_ENDIAN_H_
#define _NBCOMPAT_ENDIAN_H_

#if HAVE_ENDIAN_H
# include <endian.h>
#endif

#if HAVE_SYS_ENDIAN_H
# include <sys/endian.h>
#endif

#if HAVE_MACHINE_ENDIAN_H
# include <machine/endian.h>
#endif

#if HAVE_SYS_BYTEORDER_H
# include <sys/byteorder.h>
#endif

/*
 * Declare macros that may be missing in <endian.h>, <sys/endian.h>,
 * <machine/endian.h> and <sys/byteorder.h>
 */

#ifndef LITTLE_ENDIAN
# define LITTLE_ENDIAN	1234
#endif

#ifndef BIG_ENDIAN
# define BIG_ENDIAN	4321
#endif

#ifndef BYTE_ORDER
# if defined(_BIG_ENDIAN)
#  define BYTE_ORDER BIG_ENDIAN
# elif defined(_LITTE_ENDIAN)
#  define BYTE_ORDER LITTLE_ENDIAN
# elif defined(WORDS_BIGENDIAN)
#  define BYTE_ORDER BIG_ENDIAN
# else
#  define BYTE_ORDER LITTLE_ENDIAN
# endif
#endif

#endif	/* !_NBCOMPAT_ENDIAN_H_ */
