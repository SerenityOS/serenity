/*	$NetBSD: stdlib.h,v 1.5 2015/04/14 09:23:48 jperkin Exp $	*/

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

#ifndef _NBCOMPAT_STDLIB_H_
#define _NBCOMPAT_STDLIB_H_

#if HAVE_STDLIB_H
# include <stdlib.h>
#endif
#if HAVE_ALLOCA_H
# include <alloca.h>
#endif

/*
 * Declare functions and macros that may be missing in <stdlib.h> and
 * <alloca.h>.
 */

#if !HAVE_SETENV
int	setenv(const char *, const char *, int);
#endif

#if !HAVE_MKSTEMP
int	mkstemp(char *);
#endif

#if !HAVE_MKDTEMP
char	*mkdtemp(char *);
#endif

#if !HAVE_SETPROGNAME
const char *getprogname(void);
void	setprogname(const char *);
#endif

#if HAVE_WORKING_LONG_LONG
# if !defined(HAVE_STRTOLL) && defined(HAVE_LONG_LONG)
long long strtoll(const char *, char **, int);
# endif
#else
# define NO_LONG_LONG	1
#endif  /* ! HAVE_WORKING_LONG_LONG */

#if !HAVE_SHQUOTE
size_t	shquote(const char *, char *, size_t);
#endif

#endif	/* !_NBCOMPAT_STDLIB_H_ */
