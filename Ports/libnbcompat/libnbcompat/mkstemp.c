/*	$NetBSD: mkstemp.c,v 1.4 2004/08/23 03:32:12 jlam Exp $	*/

/*
 * Copyright (c) 1987, 1993
 *	The Regents of the University of California.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

#if HAVE_NBTOOL_CONFIG_H
#include "nbtool_config.h"
#endif

#if !HAVE_NBTOOL_CONFIG_H || !HAVE_MKSTEMP

#include <nbcompat.h>
#include <nbcompat/cdefs.h>
#if defined(LIBC_SCCS) && !defined(lint)
#if 0
static char sccsid[] = "@(#)mktemp.c	8.1 (Berkeley) 6/4/93";
#else
__RCSID("$NetBSD: mkstemp.c,v 1.4 2004/08/23 03:32:12 jlam Exp $");
#endif
#endif /* LIBC_SCCS and not lint */

#if HAVE_NBTOOL_CONFIG_H
#define	GETTEMP		gettemp
#else
#include <nbcompat/assert.h>
#if HAVE_ERRNO_H
#include <errno.h>
#endif
#include <nbcompat/stdio.h>
#include <nbcompat/stdlib.h>
#include <nbcompat/unistd.h>
#if 0
#include "reentrant.h"
#include "local.h"
#define	GETTEMP		__gettemp
#else
#define	GETTEMP		gettemp
extern int	gettemp __P((char *, int *, int));
#endif
#endif

int
mkstemp(path)
	char *path;
{
	int fd;

	_DIAGASSERT(path != NULL);

	return (GETTEMP(path, &fd, 0) ? fd : -1);
}

#endif /* !HAVE_NBTOOL_CONFIG_H || !HAVE_MKSTEMP */
