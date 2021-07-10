/*	$NetBSD: string.h,v 1.5 2019/07/09 09:43:32 sevan Exp $	*/

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

#ifndef _NBCOMPAT_STRING_H_
#define _NBCOMPAT_STRING_H_

#if HAVE_STRING_H
# include <string.h>
#endif

/*
 * Declare functions and macros that may be missing in <string.h>.
 */

#if !HAVE_DECL_STRDUP
char	*strdup(const char *);
#endif

#if !HAVE_STRERROR
char	*strerror(int);
#endif

#if !HAVE_STRLCAT
size_t	strlcat(char *, const char *, size_t);
#endif

#if !HAVE_STRLCPY
size_t	strlcpy(char *, const char *, size_t);
#endif

#if !HAVE_STRNLEN
size_t	strnlen(const char *s, size_t maxlen);
#endif

#if !HAVE_STRSEP
char	*strsep(char **stringp, const char *delim);
#endif

#endif	/* !_NBCOMPAT_STRING_H_ */
