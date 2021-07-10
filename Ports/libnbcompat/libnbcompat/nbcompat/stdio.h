/*	$NetBSD: stdio.h,v 1.7 2015/06/08 00:44:46 joerg Exp $	*/

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

#ifndef _NBCOMPAT_STDIO_H_
#define _NBCOMPAT_STDIO_H_

#if HAVE_STDARG_H
# include <stdarg.h>
#endif

#if HAVE_STDIO_H
# include <stdio.h>
#endif

/*
 * Declare functions and macros that may be missing in <stdio.h>.
 */

#if !HAVE_FCLOSE_D
int	fclose(FILE *);
#endif

#if !HAVE_PCLOSE_D
int	pclose(FILE *);
#endif

#if !HAVE_FGETLN
char	*fgetln(FILE *, size_t *);
#endif

#if !HAVE_GETDELIM
ssize_t	getdelim(char **, size_t *, int, FILE *);
#endif

#if !HAVE_GETLINE
ssize_t	getline(char **, size_t *, FILE *);
#endif

#if !HAVE_DECL_SNPRINTF
int	snprintf(char *, size_t, const char *, ...);
int	vsnprintf(char *, size_t, const char *, va_list);
#endif

#if !HAVE_DECL_ASPRINTF
int	asprintf(char **, const char *, ...);
int	vasprintf(char **, const char *, va_list);
#endif

#endif	/* !_NBCOMPAT_STDIO_H_ */
