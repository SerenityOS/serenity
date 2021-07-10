/*	$NetBSD: unvis.c,v 1.13 2015/09/03 09:05:54 joerg Exp $	*/

/*-
 * Copyright (c) 1989, 1993
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

#include <nbcompat.h>
#include <nbcompat/cdefs.h>
#if defined(LIBC_SCCS) && !defined(lint)
#if 0
static char sccsid[] = "@(#)unvis.c	8.1 (Berkeley) 6/4/93";
#else
__RCSID("$NetBSD: unvis.c,v 1.13 2015/09/03 09:05:54 joerg Exp $");
#endif
#endif /* LIBC_SCCS and not lint */

#if 0
#define __LIBC12_SOURCE__
#endif

#if 0
#include "namespace.h"
#endif
#include <nbcompat/types.h>

#include <nbcompat/assert.h>
#include <nbcompat/ctype.h>
#include <nbcompat/stdio.h>
#include <nbcompat/vis.h>

#if 0
#ifdef __weak_alias
__weak_alias(strunvis,_strunvis)
__weak_alias(unvis,_unvis)
#endif
#endif

#if 0
#ifdef __warn_references
__warn_references(unvis,
    "warning: reference to compatibility unvis(); include <vis.h> for correct reference")
#endif
#endif

/*
 * decode driven by state machine
 */
#define	S_GROUND	0	/* haven't seen escape char */
#define	S_START		1	/* start decoding special sequence */
#define	S_META		2	/* metachar started (M) */
#define	S_META1		3	/* metachar more, regular char (-) */
#define	S_CTRL		4	/* control char started (^) */
#define	S_OCTAL2	5	/* octal digit 2 */
#define	S_OCTAL3	6	/* octal digit 3 */
#define S_HEX1		7	/* hex digit */
#define S_HEX2		8	/* hex digit 2 */

#define	isoctal(c)	(((unsigned char)(c)) >= '0' && ((unsigned char)(c)) <= '7')
#define xtod(c)		(isdigit(c) ? (c - '0') : ((tolower(c) - 'a') + 10))

/*
 * unvis - decode characters previously encoded by vis
 */
static int
__nbcompat_unvis13(char *cp, int c, int *astate, int flag)
{

	_DIAGASSERT(cp != NULL);
	_DIAGASSERT(astate != NULL);

	if (flag & UNVIS_END) {
		if (*astate == S_OCTAL2 || *astate == S_OCTAL3
		    || *astate == S_HEX2) {
			*astate = S_GROUND;
			return (UNVIS_VALID);
		} 
		return (*astate == S_GROUND ? UNVIS_NOCHAR : UNVIS_SYNBAD);
	}

	switch (*astate) {

	case S_GROUND:
		*cp = 0;
		if (c == '\\') {
			*astate = S_START;
			return (0);
		} 
		if ((flag & VIS_HTTPSTYLE) && c == '%') {
			*astate = S_HEX1;
			return (0);
		}
		*cp = c;
		return (UNVIS_VALID);

	case S_START:
		switch(c) {
		case '\\':
			*cp = c;
			*astate = S_GROUND;
			return (UNVIS_VALID);
		case '0': case '1': case '2': case '3':
		case '4': case '5': case '6': case '7':
			*cp = (c - '0');
			*astate = S_OCTAL2;
			return (0);
		case 'M':
			*cp = (char)0200;
			*astate = S_META;
			return (0);
		case '^':
			*astate = S_CTRL;
			return (0);
		case 'n':
			*cp = '\n';
			*astate = S_GROUND;
			return (UNVIS_VALID);
		case 'r':
			*cp = '\r';
			*astate = S_GROUND;
			return (UNVIS_VALID);
		case 'b':
			*cp = '\b';
			*astate = S_GROUND;
			return (UNVIS_VALID);
		case 'a':
			*cp = '\007';
			*astate = S_GROUND;
			return (UNVIS_VALID);
		case 'v':
			*cp = '\v';
			*astate = S_GROUND;
			return (UNVIS_VALID);
		case 't':
			*cp = '\t';
			*astate = S_GROUND;
			return (UNVIS_VALID);
		case 'f':
			*cp = '\f';
			*astate = S_GROUND;
			return (UNVIS_VALID);
		case 's':
			*cp = ' ';
			*astate = S_GROUND;
			return (UNVIS_VALID);
		case 'E':
			*cp = '\033';
			*astate = S_GROUND;
			return (UNVIS_VALID);
		case '\n':
			/*
			 * hidden newline
			 */
			*astate = S_GROUND;
			return (UNVIS_NOCHAR);
		case '$':
			/*
			 * hidden marker
			 */
			*astate = S_GROUND;
			return (UNVIS_NOCHAR);
		}
		*astate = S_GROUND;
		return (UNVIS_SYNBAD);
		 
	case S_META:
		if (c == '-')
			*astate = S_META1;
		else if (c == '^')
			*astate = S_CTRL;
		else {
			*astate = S_GROUND;
			return (UNVIS_SYNBAD);
		}
		return (0);
		 
	case S_META1:
		*astate = S_GROUND;
		*cp |= c;
		return (UNVIS_VALID);
		 
	case S_CTRL:
		if (c == '?')
			*cp |= 0177;
		else
			*cp |= c & 037;
		*astate = S_GROUND;
		return (UNVIS_VALID);

	case S_OCTAL2:	/* second possible octal digit */
		if (isoctal(c)) {
			/* 
			 * yes - and maybe a third 
			 */
			*cp = (*cp << 3) + (c - '0');
			*astate = S_OCTAL3;	
			return (0);
		} 
		/* 
		 * no - done with current sequence, push back passed char 
		 */
		*astate = S_GROUND;
		return (UNVIS_VALIDPUSH);

	case S_OCTAL3:	/* third possible octal digit */
		*astate = S_GROUND;
		if (isoctal(c)) {
			*cp = (*cp << 3) + (c - '0');
			return (UNVIS_VALID);
		}
		/*
		 * we were done, push back passed char
		 */
		return (UNVIS_VALIDPUSH);
	case S_HEX1:
		if (isxdigit(c)) {
			*cp = xtod(c);
			*astate = S_HEX2;
			return (0);
		}
		/* 
		 * no - done with current sequence, push back passed char 
		 */
		*astate = S_GROUND;
		return (UNVIS_VALIDPUSH);
	case S_HEX2:
                *astate = S_GROUND;
                if (isxdigit(c)) {
                        *cp = xtod(c) | (*cp << 4);
			return (UNVIS_VALID);
		}
                return (UNVIS_VALIDPUSH);
	default:	
		/* 
		 * decoder in unknown state - (probably uninitialized) 
		 */
		*astate = S_GROUND;
		return (UNVIS_SYNBAD);
	}
}

int
unvis(char *cp, int c, int *astate, int flag)
{
	return __nbcompat_unvis13(cp, (int)c, astate, flag);
}

/*
 * strunvis - decode src into dst 
 *
 *	Number of chars decoded into dst is returned, -1 on error.
 *	Dst is null terminated.
 */

int
strunvisx(char *dst, const char *src, int flag)
{
	char c;
	char *start = dst;
	int state = 0;

	_DIAGASSERT(src != NULL);
	_DIAGASSERT(dst != NULL);

	while ((c = *src++) != '\0') {
	again:
		switch (__nbcompat_unvis13(dst, c, &state, flag)) {
		case UNVIS_VALID:
			dst++;
			break;
		case UNVIS_VALIDPUSH:
			dst++;
			goto again;
		case 0:
		case UNVIS_NOCHAR:
			break;
		default:
			return (-1);
		}
	}
	if (__nbcompat_unvis13(dst, c, &state, UNVIS_END) == UNVIS_VALID)
		dst++;
	*dst = '\0';
	return (dst - start);
}

int
strunvis(char *dst, const char *src)
{
	return strunvisx(dst, src, 0);
}
