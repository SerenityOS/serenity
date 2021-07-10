/*	$NetBSD: snprintf.c,v 1.7 2008/06/19 17:28:09 joerg Exp $	*/

/*-
 * Copyright (c) 2007 Tobias Nygren <tnn@NetBSD.org>
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
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <nbcompat.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#if HAVE_STDARG_H
#include <stdarg.h>
#endif

#ifdef MIN
#undef MIN
#endif
#define MIN(a, b) ((a)<(b)?(a):(b))

int
snprintf(char *str, size_t size, const char *format, ...)
{
	va_list ap;
	int len;

	va_start(ap, format);
	len = vsnprintf(str, size, format, ap);
	va_end(ap);
	return len;
}

int
vsnprintf(char *str, size_t size, const char *format, va_list ap)
{
	int len;
	char buf[129];
	char *p = buf;
	static FILE *devnull = 0;

	if (devnull == NULL) {
		devnull = fopen("/dev/null", "w");
		if (devnull == NULL)
			return -1;
	}

	len = vfprintf(devnull, format, ap);
	if (len < 0)
		return len;

	if (len > 128) {
		p = malloc(len + 1);
		if (p == NULL)
			return -1;
	}

	vsprintf(p, format, ap);

	if (size > 0) {
		memcpy(str, p, MIN(len, size));
		str[MIN(len, size - 1)] = 0;
	}

	if (p != buf)
		free(p);

	return len;
}
