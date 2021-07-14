/*	$NetBSD: types.h,v 1.4 2008/12/14 07:50:49 obache Exp $	*/

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

#ifndef _NBCOMPAT_SYS_TYPES_H_
#define _NBCOMPAT_SYS_TYPES_H_

#if HAVE_SYS_TYPES_H
# include <sys/types.h>
#endif

/*
 * BSD code generally assumes that this types are present, so pull this in
 * here for platforms like Mac OS X.
 */
#if HAVE_INTTYPES_H
#include <inttypes.h>
#endif

#if HAVE_STDINT_H
#include <stdint.h>
#endif

/*
 * Declare macros and functions that may be missing in <sys/types.h>.
 */

#if !defined(makedev)
# if HAVE_SYS_MKDEV_H
#  include <sys/mkdev.h>
#  if defined(mkdev)
#   define makedev	mkdev
#  endif
# endif
#endif

#endif	/* !_NBCOMPAT_SYS_TYPES_H_ */
