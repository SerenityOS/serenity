/*	$NetBSD: perform.c,v 1.28 2020/07/01 10:03:20 jperkin Exp $	*/

#if HAVE_CONFIG_H
#include "config.h"
#endif
#include <nbcompat.h>
#if HAVE_SYS_CDEFS_H
#include <sys/cdefs.h>
#endif
__RCSID("$NetBSD: perform.c,v 1.28 2020/07/01 10:03:20 jperkin Exp $");

/*
 * FreeBSD install - a package for the installation and maintainance
 * of non-core utilities.
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
 * Jordan K. Hubbard
 * 18 July 1993
 *
 * This is the main body of the create module.
 *
 */

#include "lib.h"
#include "create.h"

#if HAVE_ERR_H
#include <err.h>
#endif
#if HAVE_FCNTL_H
#include <fcntl.h>
#endif
#if HAVE_UNISTD_H
#include <unistd.h>
#endif

static void
sanity_check(void)
{
	if (!Comment)
		errx(2, "required package comment string is missing (-c comment)");
	if (!Desc)
		errx(2, "required package description string is missing (-d desc)");
	if (!Contents)
		errx(2, "required package contents list is missing (-f [-]file)");
}

static void
register_depends(package_t *plist, char *deps, int build_only)
{
	char *cp;

	if (Verbose && !PlistOnly) {
		if (build_only)
			printf("Registering build depends:");
		else
			printf("Registering depends:");
	}
	while (deps) {
		cp = strsep(&deps, " \t\n");
		if (*cp) {
			char *best_installed;
			best_installed = find_best_matching_installed_pkg(cp, 1);
			if (best_installed != NULL) {
				add_plist(plist, PLIST_BLDDEP, best_installed);
				if (Verbose && !PlistOnly && build_only)
					printf(" %s", cp);
			} else
				warnx("No matching package installed for %s", cp);
			free(best_installed);
			if (!build_only) {
				add_plist(plist, PLIST_PKGDEP, cp);
				if (Verbose && !PlistOnly)
					printf(" %s", cp);
			}
		}
	}
	if (Verbose && !PlistOnly)
		printf(".\n");
}

/*
 *  Expect "fname" to point at a file, and read it into
 *  the buffer returned.
 */
static char   *
fileGetContents(char *fname)
{
	char   *contents;
	struct stat sb;
	int     fd;

	if (stat(fname, &sb) == FAIL) {
		errx(2, "can't stat '%s'", fname);
	}

	contents = xmalloc((size_t) (sb.st_size) + 1);
	fd = open(fname, O_RDONLY, 0);
	if (fd == FAIL) {
		errx(2, "unable to open '%s' for reading", fname);
	}
	if (read(fd, contents, (size_t) sb.st_size) != (ssize_t) sb.st_size) {
		errx(2, "short read on '%s' - did not get %lld bytes",
		    fname, (long long) sb.st_size);
	}
	close(fd);
	contents[(size_t) sb.st_size] = '\0';
	return contents;
}

/*
 * Get a string parameter as a file spec or as a "contents follow -" spec
 */
static void
get_dash_string(char **s)
{
	if (**s == '-')
		*s = xstrdup(*s + 1);
	else
		*s = fileGetContents(*s); 
}

int
pkg_perform(const char *pkg)
{
	char   *cp;
	FILE   *pkg_in;
	package_t plist;
	const char *full_pkg, *suffix;
	char *allocated_pkg;
	int retval;

	/* Break the package name into base and desired suffix (if any) */
	if ((cp = strrchr(pkg, '.')) != NULL) {
		allocated_pkg = xmalloc(cp - pkg + 1);
		memcpy(allocated_pkg, pkg, cp - pkg);
		allocated_pkg[cp - pkg] = '\0';
		suffix = cp + 1;
		full_pkg = pkg;
		pkg = allocated_pkg;
	} else {
		allocated_pkg = NULL;
		full_pkg = pkg;
		suffix = "tgz";
	}

	/* Preliminary setup */
	sanity_check();
	if (Verbose && !PlistOnly)
		printf("Creating package %s\n", pkg);
	get_dash_string(&Comment);
	get_dash_string(&Desc);
	if (IS_STDIN(Contents))
		pkg_in = stdin;
	else {
		pkg_in = fopen(Contents, "r");
		if (!pkg_in)
			errx(2, "unable to open contents file '%s' for input", Contents);
	}

	plist.head = plist.tail = NULL;

	/* Stick the dependencies, if any, at the top */
	if (Pkgdeps)
		register_depends(&plist, Pkgdeps, 0);

	/*
	 * Put the build dependencies after the dependencies.
	 * This works due to the evaluation order in pkg_add.
	 */
	if (BuildPkgdeps)
		register_depends(&plist, BuildPkgdeps, 1);

	/* Put the conflicts directly after the dependencies, if any */
	if (Pkgcfl) {
		if (Verbose && !PlistOnly)
			printf("Registering conflicts:");
		while (Pkgcfl) {
			cp = strsep(&Pkgcfl, " \t\n");
			if (*cp) {
				add_plist(&plist, PLIST_PKGCFL, cp);
				if (Verbose && !PlistOnly)
					printf(" %s", cp);
			}
		}
		if (Verbose && !PlistOnly)
			printf(".\n");
	}

	/* Slurp in the packing list */
	append_plist(&plist, pkg_in);

	if (pkg_in != stdin)
		fclose(pkg_in);

	/* Prefix should override the packing list */
	if (Prefix) {
		delete_plist(&plist, FALSE, PLIST_CWD, NULL);
		add_plist_top(&plist, PLIST_CWD, Prefix);
	}
	/*
         * Run down the list and see if we've named it, if not stick in a name
         * at the top.
         */
	if (find_plist(&plist, PLIST_NAME) == NULL) {
		add_plist_top(&plist, PLIST_NAME, basename_of(pkg));
	}

	/* Make first "real contents" pass over it */
	check_list(&plist, basename_of(pkg));

	/*
         * We're just here for to dump out a revised plist for the FreeBSD ports
         * hack.  It's not a real create in progress.
         */
	if (PlistOnly) {
		write_plist(&plist, stdout, realprefix);
		retval = TRUE;
	} else {
		retval = pkg_build(pkg, full_pkg, suffix, &plist);
	}

	/* Cleanup */
	free(Comment);
	free(Desc);
	free_plist(&plist);

	free(allocated_pkg);

	return retval;
}
