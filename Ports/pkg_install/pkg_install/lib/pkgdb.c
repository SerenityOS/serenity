/*	$NetBSD: pkgdb.c,v 1.42 2020/12/18 17:10:54 maya Exp $	*/

#if HAVE_CONFIG_H
#include "config.h"
#endif
#include <nbcompat.h>
#if HAVE_SYS_CDEFS_H
#include <sys/cdefs.h>
#endif
__RCSID("$NetBSD: pkgdb.c,v 1.42 2020/12/18 17:10:54 maya Exp $");

/*-
 * Copyright (c) 1999-2010 The NetBSD Foundation, Inc.
 * All rights reserved.
 *
 * This code is derived from software contributed to The NetBSD Foundation
 * by Hubert Feyrer <hubert@feyrer.de>.
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

#ifdef NETBSD
#include <db.h>
#else
#include <nbcompat/db.h>
#endif
#if HAVE_ERR_H
#include <err.h>
#endif
#if HAVE_ERRNO_H
#include <errno.h>
#endif
#if HAVE_FCNTL_H
#include <fcntl.h>
#endif
#if HAVE_STDARG_H
#include <stdarg.h>
#endif
#if HAVE_STDIO_H
#include <stdio.h>
#endif
#if HAVE_STRING_H
#include <string.h>
#endif

#include "lib.h"

#define PKGDB_FILE	"pkgdb.byfile.db"	/* indexed by filename */

/*
 * Where we put logging information by default if PKG_DBDIR is unset.
 */
#ifndef DEF_LOG_DIR
#define DEF_LOG_DIR		PREFIX "/pkgdb"
#endif

static DB   *pkgdbp;
static char pkgdb_dir_default[] = DEF_LOG_DIR;
static char *pkgdb_dir = pkgdb_dir_default;
static int pkgdb_dir_prio = 0;

/*
 *  Return name of cache file in the buffer that was passed.
 */
char *
pkgdb_get_database(void)
{
	return xasprintf("%s/%s", pkgdb_get_dir(), PKGDB_FILE);
}

/*
 *  Open the pkg-database
 *  Return value:
 *   1: everything ok
 *   0: error
 */
int
pkgdb_open(int mode)
{
	BTREEINFO info;
	char *cachename;

	/* try our btree format first */
	info.flags = 0;
	info.cachesize = 2*1024*1024;
	info.maxkeypage = 0;
	info.minkeypage = 0;
	info.psize = 4096;
	info.compare = NULL;
	info.prefix = NULL;
	info.lorder = 0;
	cachename = pkgdb_get_database();
	pkgdbp = (DB *) dbopen(cachename,
	    (mode == ReadOnly) ? O_RDONLY : O_RDWR | O_CREAT,
	    0644, DB_BTREE, (void *) &info);
	free(cachename);
	return (pkgdbp != NULL);
}

/*
 * Close the pkg database
 */
void
pkgdb_close(void)
{
	if (pkgdbp != NULL) {
		(void) (*pkgdbp->close) (pkgdbp);
		pkgdbp = NULL;
	}
}

/*
 * Store value "val" with key "key" in database
 * Return value is as from ypdb_store:
 *  0: ok
 *  1: key already present
 * -1: some other error, see errno
 */
int
pkgdb_store(const char *key, const char *val)
{
	DBT     keyd, vald;

	if (pkgdbp == NULL)
		return -1;

	keyd.data = __UNCONST(key);
	keyd.size = strlen(key) + 1;
	vald.data = __UNCONST(val);
	vald.size = strlen(val) + 1;

	if (keyd.size > MaxPathSize || vald.size > MaxPathSize)
		return -1;

	return (*pkgdbp->put) (pkgdbp, &keyd, &vald, R_NOOVERWRITE);
}

/*
 * Recall value for given key
 * Return value:
 *  NULL if some error occurred or value for key not found (check errno!)
 *  String for "value" else
 */
char   *
pkgdb_retrieve(const char *key)
{
	DBT     keyd, vald;
	int     status;
	char	*eos;
	static int corruption_warning;

	if (pkgdbp == NULL)
		return NULL;

	keyd.data = __UNCONST(key);
	keyd.size = strlen(key) + 1;
	errno = 0;		/* to be sure it's 0 if the key doesn't match anything */

	vald.data = (void *)NULL;
	vald.size = 0;
	status = (*pkgdbp->get) (pkgdbp, &keyd, &vald, 0);
	if (status)
		return NULL;
	eos = memchr(vald.data, 0, vald.size);
	if (eos == NULL || eos + 1 != (char *)vald.data + vald.size) {
		if (!corruption_warning) {
			warnx("pkgdb corrupted, please run ``pkg_admin rebuild''");
			corruption_warning = 1;
		}
		return NULL;
	}

	return vald.data;
}

/* dump contents of the database to stdout */
int
pkgdb_dump(void)
{
	DBT     key;
	DBT	val;
	int	type;

	if (pkgdb_open(ReadOnly)) {
		for (type = R_FIRST ; (*pkgdbp->seq)(pkgdbp, &key, &val, type) == 0 ; type = R_NEXT) {
			printf("file: %.*s pkg: %.*s\n",
				(int) key.size, (char *) key.data,
				(int) val.size, (char *) val.data);
		}
		pkgdb_close();
		return 0;
	} else
		return -1;
}

/*
 *  Remove data set from pkgdb
 *  Return value as ypdb_delete:
 *   0: everything ok
 *   1: key not present
 *  -1: some error occurred (see errno)
 */
int
pkgdb_remove(const char *key)
{
	DBT     keyd;

	if (pkgdbp == NULL)
		return -1;

	keyd.data = __UNCONST(key);
	keyd.size = strlen(key) + 1;
	if (keyd.size > MaxPathSize)
		return -1;

	return (*pkgdbp->del) (pkgdbp, &keyd, 0);
}

/*
 *  Remove any entry from the cache which has a data field of `pkg'.
 *  Return value:
 *   1: everything ok
 *   0: error
 */
int
pkgdb_remove_pkg(const char *pkg)
{
	DBT     data;
	DBT     key;
	int	type;
	int	ret;
	size_t	cc;
	char	*cachename;

	if (pkgdbp == NULL) {
		return 0;
	}
	cachename = pkgdb_get_database();
	cc = strlen(pkg);
	for (ret = 1, type = R_FIRST; (*pkgdbp->seq)(pkgdbp, &key, &data, type) == 0 ; type = R_NEXT) {
		if ((cc + 1) == data.size && strncmp(data.data, pkg, cc) == 0) {
			if (Verbose) {
				printf("Removing file `%s' from %s\n", (char *)key.data, cachename);
			}
			switch ((*pkgdbp->del)(pkgdbp, &key, 0)) {
			case -1:
				warn("Error removing `%s' from %s", (char *)key.data, cachename);
				ret = 0;
				break;
			case 1:
				warn("Key `%s' not present in %s", (char *)key.data, cachename);
				ret = 0;
				break;

			}
		}
	}
	free(cachename);
	return ret;
}

/*
 *  Return the location of the package reference counts database directory.
 */
char *
pkgdb_refcount_dir(void)
{
	static char buf[MaxPathSize];
	char *tmp;

	if ((tmp = getenv(PKG_REFCOUNT_DBDIR_VNAME)) != NULL)
		strlcpy(buf, tmp, sizeof(buf));
	else
		snprintf(buf, sizeof(buf), "%s.refcount", pkgdb_get_dir());
	return buf;
}

/*
 *  Return directory where pkgdb is stored
 */
const char *
pkgdb_get_dir(void)
{

#ifdef NETBSD
	/* 
	 * NetBSD upgrade case.
	 * NetBSD used to ship pkg_install with /var/db/pkg as
	 * the default. We support continuing to install to
	 * this location.
	 *
	 * This is NetBSD-specific because we can't assume that
	 * /var/db/pkg is pkgsrc-owned on other systems (OpenBSD,
	 * FreeBSD...)
	 *
	 * XXX: once postinstall is taught to automatically
	 * handle migration, we can deprecate this behaviour.
	 */

#define PREVIOUS_LOG_DIR	"/var/db/pkg"
	static char pkgdb_dir_previous[] = PREVIOUS_LOG_DIR;

	struct stat sb;
	if (strcmp(pkgdb_dir, DEF_LOG_DIR) == 0 &&
	    stat(pkgdb_dir, &sb) == -1 && errno == ENOENT &&
	    stat(PREVIOUS_LOG_DIR, &sb) == 0) {
		return pkgdb_dir_previous;
	}
#endif

        return pkgdb_dir;
}

/*
 *  Set the first place we look for where pkgdb is stored.
 */
void
pkgdb_set_dir(const char *dir, int prio)
{

	if (prio < pkgdb_dir_prio)
		return;

	pkgdb_dir_prio = prio;

	if (dir == pkgdb_dir)
		return;
	if (pkgdb_dir != pkgdb_dir_default)
		free(pkgdb_dir);
	pkgdb_dir = xstrdup(dir);
}

char *
pkgdb_pkg_dir(const char *pkg)
{
	return xasprintf("%s/%s", pkgdb_get_dir(), pkg);
}

char *
pkgdb_pkg_file(const char *pkg, const char *file)
{
	return xasprintf("%s/%s/%s", pkgdb_get_dir(), pkg, file);
}
