/*	$NetBSD: fgetln.c,v 1.5 2008/04/29 05:46:08 martin Exp $	*/

/*-
 * Copyright (c) 1998 The NetBSD Foundation, Inc.
 * All rights reserved.
 *
 * This code is derived from software contributed to The NetBSD Foundation
 * by Christos Zoulas.
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

#include <nbcompat.h>
#include <nbcompat/stdio.h>
#include <nbcompat/stdlib.h>

/*
 * XXX: This implementation doesn't quite conform to the specification
 * in the man page, in that it only manages one buffer at all, not one
 * per stdio stream. Since the previous implementation did the same,
 * this won't break anything new.
 */
char *
fgetln(fp, len)
	FILE *fp;
	size_t *len;
{
	static char *buf = NULL;
	static size_t bufsiz = 0;
	static size_t buflen = 0;
	int c;

	if (buf == NULL) {
		bufsiz = BUFSIZ;
		if ((buf = malloc(bufsiz)) == NULL)
			return NULL;
	}

	buflen = 0;
	while ((c = fgetc(fp)) != EOF) {
		if (buflen >= bufsiz) {
			size_t nbufsiz = bufsiz + BUFSIZ;
			char *nbuf = realloc(buf, nbufsiz);

			if (nbuf == NULL) {
				int oerrno = errno;
				free(buf);
				errno = oerrno;
				buf = NULL;
				return NULL;
			}

			buf = nbuf;
			bufsiz = nbufsiz;
		}
		buf[buflen++] = c;
		if (c == '\n')
			break;
	}
	*len = buflen;
	return buflen == 0 ? NULL : buf;
}
