/* $NetBSD: xwrapper.c,v 1.2 2009/02/02 12:35:01 joerg Exp $ */

/*-
 * Copyright (c) 2008 Joerg Sonnenberger <joerg@NetBSD.org>.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE
 * COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

#if HAVE_CONFIG_H
#include "config.h"
#endif
#include <nbcompat.h>
#if HAVE_SYS_CDEFS_H
#include <sys/cdefs.h>
#endif
__RCSID("$NetBSD: xwrapper.c,v 1.2 2009/02/02 12:35:01 joerg Exp $");

#if HAVE_ERR_H
#include <err.h>
#endif
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "lib.h"

char *
xasprintf(const char *fmt, ...)
{
	va_list ap;
	char *buf;
	
	va_start(ap, fmt);
	if (vasprintf(&buf, fmt, ap) == -1)
		err(1, "asprintf failed");
	va_end(ap);
	return buf;
}

void *
xmalloc(size_t len)
{
	void *ptr;

	if ((ptr = malloc(len)) == NULL)
		err(1, "malloc failed");
	return ptr;
}

void *
xcalloc(size_t len, size_t n)
{
	void *ptr;

	if ((ptr = calloc(len, n)) == NULL)
		err(1, "calloc failed");
	return ptr;
}

void *
xrealloc(void *buf, size_t len)
{
	void *ptr;

	if ((ptr = realloc(buf, len)) == NULL)
		err(1, "realloc failed");
	return ptr;
}

char *
xstrdup(const char *str)
{
	char *buf;

	if ((buf = strdup(str)) == NULL)
		err(1, "strdup failed");
	return buf;
}
