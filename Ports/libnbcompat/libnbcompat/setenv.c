/*	$NetBSD: setenv.c,v 1.10 2004/08/23 03:32:12 jlam Exp $	*/

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

#include <nbcompat.h>
#include <nbcompat/cdefs.h>
#if defined(LIBC_SCCS) && !defined(lint)
#if 0
static char sccsid[] = "@(#)setenv.c	8.1 (Berkeley) 6/4/93";
#else
__RCSID("$NetBSD: setenv.c,v 1.10 2004/08/23 03:32:12 jlam Exp $");
#endif
#endif /* LIBC_SCCS and not lint */

#if 0
#include "namespace.h"
#endif

#include <nbcompat/assert.h>
#if HAVE_ERRNO_H
#include <errno.h>
#endif
#include <nbcompat/stdlib.h>
#include <nbcompat/string.h>
#if 0
#include "local.h"
#include "reentrant.h"
#endif

char *__findenv __P((const char *, int *));

#if 0
#ifdef __weak_alias
__weak_alias(setenv,_setenv)
#endif
#endif

#if 0
#ifdef _REENTRANT
extern rwlock_t __environ_lock;
#endif
#endif

extern char **environ;

#ifndef rwlock_wrlock
#define rwlock_wrlock(lock)	((void)0)
#endif
#ifndef rwlock_unlock
#define rwlock_unlock(lock)	((void)0)
#endif

/*
 * setenv --
 *	Set the value of the environmental variable "name" to be
 *	"value".  If rewrite is set, replace any current value.
 */
int
setenv(name, value, rewrite)
	const char *name;
	const char *value;
	int rewrite;
{
	static int alloced;			/* if allocated space before */
	char *c;
	const char *cc;
	size_t l_value;
	int offset;

	_DIAGASSERT(name != NULL);
	_DIAGASSERT(value != NULL);

	if (*value == '=')			/* no `=' in value */
		++value;
	l_value = strlen(value);
	rwlock_wrlock(&__environ_lock);
	/* find if already exists */
	if ((c = __findenv(name, &offset)) != NULL) {
		if (!rewrite) {
			rwlock_unlock(&__environ_lock);
			return (0);
		}
		if (strlen(c) >= l_value) {	/* old larger; copy over */
			while ((*c++ = *value++) != '\0');
			rwlock_unlock(&__environ_lock);
			return (0);
		}
	} else {					/* create new slot */
		int cnt;
		char **p;

		for (p = environ, cnt = 0; *p; ++p, ++cnt);
		if (alloced) {			/* just increase size */
			environ = realloc(environ,
			    (size_t)(sizeof(char *) * (cnt + 2)));
			if (!environ) {
				rwlock_unlock(&__environ_lock);
				return (-1);
			}
		}
		else {				/* get new space */
			alloced = 1;		/* copy old entries into it */
			p = malloc((size_t)(sizeof(char *) * (cnt + 2)));
			if (!p) {
				rwlock_unlock(&__environ_lock);
				return (-1);
			}
			memcpy(p, environ, cnt * sizeof(char *));
			environ = p;
		}
		environ[cnt + 1] = NULL;
		offset = cnt;
	}
	for (cc = name; *cc && *cc != '='; ++cc)/* no `=' in name */
		continue;
	if (!(environ[offset] =			/* name + `=' + value */
	    malloc((size_t)((int)(cc - name) + l_value + 2)))) {
		rwlock_unlock(&__environ_lock);
		return (-1);
	}
	for (c = environ[offset]; (*c = *name++) && *c != '='; ++c);
	for (*c++ = '='; (*c++ = *value++) != '\0'; );
	rwlock_unlock(&__environ_lock);
	return (0);
}
