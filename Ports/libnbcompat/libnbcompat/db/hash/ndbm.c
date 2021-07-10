/*	$NetBSD: ndbm.c,v 1.2 2008/10/29 11:23:17 joerg Exp $	*/
/*	NetBSD: ndbm.c,v 1.23 2008/09/11 12:58:00 joerg Exp 	*/

/*-
 * Copyright (c) 1990, 1993
 *	The Regents of the University of California.  All rights reserved.
 *
 * This code is derived from software contributed to Berkeley by
 * Margo Seltzer.
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

__RCSID("$NetBSD: ndbm.c,v 1.2 2008/10/29 11:23:17 joerg Exp $");

/*
 * This package provides a dbm compatible interface to the new hashing
 * package described in db(3).
 */
#include <nbcompat/param.h>

#include <fcntl.h>
#include <stdio.h>
#include <string.h>

#include <nbcompat/ndbm.h>
#include "hash.h"

/*
 * Returns:
 * 	*DBM on success
 *	 NULL on failure
 */
DBM *
dbm_open(const char *file, int flags, mode_t mode)
{
	HASHINFO info;
	char path[MAXPATHLEN];

	info.bsize = 4096;
	info.ffactor = 40;
	info.nelem = 1;
	info.cachesize = 0;
	info.hash = NULL;
	info.lorder = 0;
	(void)strncpy(path, file, sizeof(path) - 1);
	(void)strncat(path, DBM_SUFFIX, sizeof(path) - strlen(path) - 1);
	if ((flags & O_ACCMODE) == O_WRONLY) {
		flags &= ~O_WRONLY;
		flags |= O_RDWR;
	}
	return ((DBM *)__hash_open(path, flags, mode, &info, 0));
}

void
dbm_close(DBM *db)
{
	(void)(db->close)(db);
}

int
dbm_error(DBM *db)
{
	HTAB *hp;

	hp = db->internal;
	return (hp->err);
}

int
dbm_clearerr(DBM *db)
{
	HTAB *hp;

	hp = db->internal;
	hp->err = 0;
	return (0);
}

int
dbm_dirfno(DBM *db)
{
	HTAB *hp;

	hp = db->internal;
	return hp->fp;
}
