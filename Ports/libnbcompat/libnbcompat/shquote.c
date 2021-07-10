/* $NetBSD: shquote.c,v 1.1 2008/10/06 12:36:20 joerg Exp $ */

/*
 * Copyright (c) 2001 Christopher G. Demetriou
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
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *          This product includes software developed for the
 *          NetBSD Project.  See http://www.NetBSD.org/ for
 *          information about NetBSD.
 * 4. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * <<Id: LICENSE,v 1.2 2000/06/14 15:57:33 cgd Exp>>
 */

#include <nbcompat.h>

/*
 * Define SHQUOTE_USE_MULTIBYTE if you want shquote() to handle multibyte
 * characters using mbrtowc().
 *
 * Please DO NOT rip this #ifdef out of the code.  It's also here to help
 * portability.
 */
#undef	SHQUOTE_USE_MULTIBYTE

#include <nbcompat/stdlib.h>
#include <string.h>
#ifdef SHQUOTE_USE_MULTIBYTE
#include <limits.h>
#include <stdio.h>
#include <wchar.h>
#endif

/*
 * shquote():
 *
 * Requotes arguments so that they'll be interpreted properly by the
 * shell (/bin/sh).
 *
 * Wraps single quotes around the string, and replaces single quotes
 * in the string with the sequence:
 *	'\''
 *
 * Returns the number of characters required to hold the resulting quoted
 * argument.
 *
 * The buffer supplied is filled in and NUL-terminated.  If 'bufsize'
 * indicates that the buffer is too short to hold the output string, the
 * first (bufsize - 1) bytes of quoted argument are filled in and the
 * buffer is NUL-terminated.
 *
 * Changes could be made to optimize the length of strings output by this
 * function:
 *
 *	* if there are no metacharacters or whitespace in the input,
 *	  the output could be the input string.
 */

#ifdef SHQUOTE_USE_MULTIBYTE

#define	XLATE_OUTCH(x)		wcrtomb(outch, (x), &mbso)
#define	XLATE_INCH()						\
    do {							\
	n = mbrtowc(&c, arg, MB_CUR_MAX, &mbsi);		\
    } while (/*LINTED const cond*/0)

#else

#define	XLATE_OUTCH(x)		(outch[0] = (x), 1)
#define	XLATE_INCH()						\
    do {							\
	n = ((c = *arg) != '\0') ? 1 : 0;			\
    } while (/*LINTED const cond*/0)

#endif

#define	PUT(x)							\
    do {							\
	outchlen = XLATE_OUTCH(x);				\
	if (outchlen == (size_t)-1)				\
		goto bad;					\
	rv += outchlen;						\
	if (bufsize != 0) {					\
		if (bufsize < outchlen ||			\
		    (bufsize == outchlen &&			\
		     outch[outchlen - 1] != '\0')) {		\
			*buf = '\0';				\
			bufsize = 0;				\
		} else {					\
			memcpy(buf, outch, outchlen);		\
			buf += outchlen;			\
			bufsize -= outchlen;			\
		}						\
	}							\
    } while (/*LINTED const cond*/0)

size_t
shquote(const char *arg, char *buf, size_t bufsize)
{
#ifdef SHQUOTE_USE_MULTIBYTE
	char outch[MB_LEN_MAX];
	mbstate_t mbsi, mbso;
	wchar_t c, lastc;
	size_t outchlen;
#else
	char outch[1];
	char c, lastc;
	size_t outchlen;
#endif
	size_t rv;
	int n;

	rv = 0;
	lastc = 0;
#ifdef SHQUOTE_USE_MULTIBYTE
	memset(&mbsi, 0, sizeof mbsi);
	memset(&mbso, 0, sizeof mbso);
#endif

	if (*arg != '\'')
		PUT('\'');
	for (;;) {
		XLATE_INCH();
		if (n <= 0)
			break;
		arg += n;
		lastc = c;

		if (c == '\'') {
			if (rv != 0)
				PUT('\'');
			PUT('\\');
			PUT('\'');
			for (;;) {
				XLATE_INCH();
				if (n <= 0 || c != '\'')
					break;
				PUT('\\');
				PUT('\'');
				arg += n;
			}
			if (n > 0)
				PUT('\'');
		} else
			PUT(c);
	}
	if (lastc != '\'')
		PUT('\'');

	/* Put multibyte or NUL terminator, but don't count the NUL. */
	PUT('\0');
	rv--;

	return rv;

bad:
	/* A multibyte character encoding or decoding error occurred. */
	return (size_t)-1;
}
