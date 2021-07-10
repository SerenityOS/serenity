/*	$NetBSD: limits.h,v 1.3 2008/04/29 05:46:08 martin Exp $	*/

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

#ifndef _NBCOMPAT_LIMITS_H_
#define _NBCOMPAT_LIMITS_H_

#if HAVE_LIMITS_H
# include <limits.h>
#endif

/*
 * Declare functions and macros that may be missing in <limits.h>.
 */

#ifndef UINT_MAX
# define UINT_MAX	4294967295U
#endif

#if !defined(LLONG_MAX)
# if defined(LONG_LONG_MAX)
#  define LLONG_MAX	LONG_LONG_MAX
# else
#  define LLONG_MAX	(0x7fffffffffffffffLL)
# endif
#endif
#if !defined(LLONG_MIN)
# if defined(LONG_LONG_MIN)
#  define LLONG_MIN	LONG_LONG_MIN
# else
#  define LLONG_MIN	(-0x7fffffffffffffffLL-1)
# endif
#endif
#if !defined(ULLONG_MAX)
# define ULLONG_MAX	(0xffffffffffffffffULL)	/* max unsigned long long */
#endif

#ifndef GID_MAX
# define GID_MAX	2147483647U  /* max value for a gid_t (2^31-2) */
#endif

#ifndef UID_MAX
# define UID_MAX	2147483647U  /* max value for a uid_t (2^31-2) */
#endif

#endif	/* !_NBCOMPAT_LIMITS_H_ */
