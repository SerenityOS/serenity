/*	$NetBSD: conflicts.c,v 1.10 2010/01/22 13:30:42 joerg Exp $	*/

/*-
 * Copyright (c) 2007 Roland Illig <rillig@NetBSD.org>.
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

/*
 * XXX: Reading the +CONTENTS files of all installed packages is
 * rather slow. Since this check is necessary to avoid conflicting
 * packages, it should not be removed.
 *
 * TODO: Put all the information that is currently in the +CONTENTS
 * files into one large file or another database.
 */

#if HAVE_CONFIG_H
#include "config.h"
#endif

#include <nbcompat.h>

#if HAVE_SYS_CDEFS_H
#include <sys/cdefs.h>
#endif

__RCSID("$NetBSD: conflicts.c,v 1.10 2010/01/22 13:30:42 joerg Exp $");

#if HAVE_ERR_H
#include <err.h>
#endif

#include "dewey.h"
#include "lib.h"

/**
 * Data structure to keep the intermediate result of the conflict
 * search. ''pkgname'' is the package in question. The first
 * installed package that conflicts is filled into
 * ''conflicting_pkgname''. The pattern that leads to the conflict is
 * also filled in to help the user in deciding what to do with the
 * conflict.
 */
struct package_conflict {
	const char *pkgname;
	const char *skip_pkgname;
	char **conflicting_pkgname;
	char **conflicting_pattern;
};

static FILE *
fopen_contents(const char *pkgname, const char *mode)
{
	char *fname;
	FILE *f;

	fname = pkgdb_pkg_file(pkgname, CONTENTS_FNAME);
	f = fopen(fname, mode);
	if (f == NULL) {
		err(EXIT_FAILURE, "%s", fname);
		/* NOTREACHED */
	}
	free(fname);
	return f;
}


static int
check_package_conflict(const char *pkgname, void *v)
{
	struct package_conflict *conflict = v;
	package_t pkg;
	plist_t *p;
	FILE *f;
	int rv;

	if (conflict->skip_pkgname != NULL &&
	    strcmp(conflict->skip_pkgname, pkgname) == 0)
		return 0;

	rv = 0;

	f = fopen_contents(pkgname, "r");
	read_plist(&pkg, f);
	(void)fclose(f);

	for (p = pkg.head; p; p = p->next) {
		if (p->type != PLIST_PKGCFL)
			continue;

		if (pkg_match(p->name, conflict->pkgname) == 1) {
			*(conflict->conflicting_pkgname) = xstrdup(pkgname);
			*(conflict->conflicting_pattern) = xstrdup(p->name);
			rv = 1 /* nonzero, stop iterating */;
			break;
		}
	}

	free_plist(&pkg);
	return rv;
}

/**
 * Checks if some installed package has a pkgcfl entry that matches
 * PkgName.  If such an entry is found, the package name is returned in
 * inst_pkgname, the matching pattern in inst_pattern, and the function
 * returns a non-zero value. Otherwise, zero is returned and the result
 * variables are set to NULL.
 */
int
some_installed_package_conflicts_with(const char *pkgname,
    const char *skip_pkgname, char **inst_pkgname, char **inst_pattern)
{
	struct package_conflict cfl;
	int rv;

	cfl.pkgname = pkgname;
	cfl.skip_pkgname = skip_pkgname;
	*inst_pkgname = NULL;
	*inst_pattern = NULL;
	cfl.conflicting_pkgname = inst_pkgname;
	cfl.conflicting_pattern = inst_pattern;
	rv = iterate_pkg_db(check_package_conflict, &cfl);
	if (rv == -1) {
		errx(EXIT_FAILURE, "Couldn't read list of installed packages.");
		/* NOTREACHED */
	}
	return *inst_pkgname != NULL;
}

#if 0
int main(int argc, char **argv)
{
	char *pkg, *patt;

	if (some_installed_package_conflicts_with(argv[1], &pkg, &patt))
		printf("yes: package %s conflicts with %s, pattern %s\n", pkg, argv[1], patt);
	else
		printf("no\n");
	return 0;
}
#endif
