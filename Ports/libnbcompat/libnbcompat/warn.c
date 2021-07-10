/*	$NetBSD: warn.c,v 1.1 2004/08/23 03:32:13 jlam Exp $	*/

/*
 * Copyright 1997-2000 Luke Mewburn <lukem@netbsd.org>.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#if HAVE_NBTOOL_CONFIG_H
#include "nbtool_config.h"
#endif

#include <nbcompat.h>
#include <nbcompat/cdefs.h>
#if defined(LIBC_SCCS) && !defined(lint)
#if 0
static char sccsid[] = "@(#)err.c	8.1 (Berkeley) 6/4/93";
#else
__RCSID("$NetBSD: warn.c,v 1.1 2004/08/23 03:32:13 jlam Exp $");
#endif
#endif /* LIBC_SCCS and not lint */

#ifndef __NO_NAMESPACE_H	/* XXX */
#if 0
#include "namespace.h"
#endif
#endif
#include <nbcompat/err.h>
#if HAVE_STDARG_H
#include <stdarg.h>
#endif

#if 0
#ifdef __weak_alias
__weak_alias(warn, _warn)
#endif
#endif

void
warn(const char *fmt, ...)
{
	va_list	ap;
        int	sverrno;

	sverrno = errno;
        (void)fprintf(stderr, "%s: ", getprogname());
	va_start(ap, fmt);
        if (fmt != NULL) {
                (void)vfprintf(stderr, fmt, ap);
                (void)fprintf(stderr, ": ");
        }
	va_end(ap);
        (void)fprintf(stderr, "%s\n", strerror(sverrno));
}

void
warnx(const char *fmt, ...)
{
	va_list	ap;

        (void)fprintf(stderr, "%s: ", getprogname());
	va_start(ap, fmt);
        if (fmt != NULL)
                (void)vfprintf(stderr, fmt, ap);
	va_end(ap);
        (void)fprintf(stderr, "\n");
}

void
vwarn(fmt, ap)
	const char *fmt;
	va_list ap;
{
	int sverrno;

	sverrno = errno;
	(void)fprintf(stderr, "%s: ", getprogname());
	if (fmt != NULL) {
		(void)vfprintf(stderr, fmt, ap);
		(void)fprintf(stderr, ": ");
	}
	(void)fprintf(stderr, "%s\n", strerror(sverrno));
}

void
vwarnx(fmt, ap)
	const char *fmt;
	va_list ap;
{
	(void)fprintf(stderr, "%s: ", getprogname());
	if (fmt != NULL)
		(void)vfprintf(stderr, fmt, ap);
	(void)fprintf(stderr, "\n");
}
