/*-
 * Copyright (c) 2009 Joerg Sonnenberger <joerg@NetBSD.org>.
 * Copyright (c) 2003 Johnny Lam <jlam@NetBSD.org>.
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
__RCSID("$NetBSD: pkg_delete.c,v 1.14 2018/03/21 14:35:35 sevan Exp $");

#if HAVE_ERR_H
#include <err.h>
#endif
#include <stdio.h>
#include <stdlib.h>

#include "lib.h"

static const char *pkgdb;
static const char *destdir;
static const char *prefix;

static int keep_preserve;
static int no_deinstall;
static int find_by_filename;
static int unregister_only;
static int pkgdb_update_only;
static int delete_recursive;
static int delete_new_leaves;
static int delete_automatic_leaves;

static void
usage(void)
{
	fprintf(stderr, "usage: pkg_delete [-ADFfkNnORrVv] [-K pkg_dbdir]"
	    " [-P destdir] [-p prefix] pkg-name ...\n");
	exit(1);
}

static int
add_by_filename(lpkg_head_t *pkgs, const char *filename)
{
	lpkg_t *lpp;
	char *s;

	if ((s = pkgdb_retrieve(filename)) == NULL) {
		warnx("No matching package for file `%s' in pkgdb", filename);
		return 1;
	}

	/* XXX Verify that pkgdb is consistent? Trust it for now... */

	lpp = alloc_lpkg(s);
	TAILQ_INSERT_TAIL(pkgs, lpp, lp_link);
	return 0;
}

static int
add_by_pattern(lpkg_head_t *pkgs, const char *pattern)
{
	switch (add_installed_pkgs_by_pattern(pattern, pkgs)) {
	case 0:
		warnx("No package matching `%s' found", pattern);
		return 1;
	case -1:
		warnx("Error while iterating package database for `%s'",
		    pattern);
		return 1;
	default:
		return 0;
	}
}

/*
 * The argument is either a fixed package name or an absolute path.
 * The latter is recognized for legacy compatibility and must point
 * into the package database.
 */
static int
add_by_pkgname(lpkg_head_t *pkgs, char *pkg)
{
	char *s;
	lpkg_t *lpp;
	size_t l;
	const char *orig_pkg = pkg;

	if (pkg[0] == '/') {
		l = strlen(pkgdb);
		if (strncmp(pkg, pkgdb, l) || pkg[l] != '/') {
			warnx("Absolute path is not relative to "
			    "package database, skipping: %s", pkg);
			return 1;
		}
		pkg += l + 1;
	}
	l = strcspn(pkg, "/");
	if (pkg[l + strspn(pkg + l, "/")] != '\0') {
		warnx("`%s' is not a package name, skipping", orig_pkg);
		return 1;
	}
	pkg[l] = '\0';

	s = pkgdb_pkg_file(pkg, CONTENTS_FNAME);
	if (fexists(s)) {
		free(s);
		lpp = alloc_lpkg(pkg);
		TAILQ_INSERT_TAIL(pkgs, lpp, lp_link);
		return 0;
	}
	free(s);

	switch (add_installed_pkgs_by_basename(pkg, pkgs)) {
	case 0:
		warnx("No matching package for basename `%s' of `%s'",
		    pkg, orig_pkg);
		return 1;
	case -1:
		warnx("Error expanding basename `%s' of `%s'",
		    pkg, orig_pkg);
		return 1;
	default:
		return 0;
	}
}

/*
 * Evaluate +REQUIRED_BY.  This function is used for four different
 * tasks:
 * 0: check if no depending packages remain
 * 1: like 0, but prepend the depending packages to pkgs if they exist
 * 2: print remaining packages to stderr
 * 3: check all and at least one depending packages have been removed
 */
static int
process_required_by(const char *pkg, lpkg_head_t *pkgs,
    lpkg_head_t *sorted_pkgs, int action)
{
	char line[MaxPathSize], *eol, *fname;
	FILE *fp;
	lpkg_t *lpp;
	int got_match, got_miss;

	fname = pkgdb_pkg_file(pkg, REQUIRED_BY_FNAME);
	if (!fexists(fname)) {
		free(fname);
		return 0;
	}

	if ((fp = fopen(fname, "r")) == NULL) {
		warn("Failed to open `%s'", fname);
		free(fname);
		return -1;
	}
	free(fname);

	got_match = 0;
	got_miss = 0;

	while (fgets(line, sizeof(line), fp)) {
		if ((eol = strrchr(line, '\n')) != NULL)
			*eol = '\0';
		TAILQ_FOREACH(lpp, sorted_pkgs, lp_link) {
			if (strcmp(lpp->lp_name, line) == 0)
				break;
		}
		if (lpp != NULL) {
			got_match = 1;
			continue;
		}
		got_miss = 1;
		if (pkgs) {
			TAILQ_FOREACH(lpp, pkgs, lp_link) {
				if (strcmp(lpp->lp_name, line) == 0)
					break;
			}
			if (lpp != NULL)
				continue;
		}
		switch (action) {
		case 0:
			fclose(fp);
			return 1;
		case 1:
			lpp = alloc_lpkg(line);
			TAILQ_INSERT_HEAD(pkgs, lpp, lp_link);
			break;
		case 2:
			fprintf(stderr, "\t%s\n", line);
			break;
		case 3:
			fclose(fp);
			return 0;
		}
	}

	fclose(fp);

	return (action == 3 ? got_match : got_miss);
}

/*
 * Main function to order the patterns from the command line and
 * add the subtrees for -r processing as needed.
 *
 * The first part ensures that all packages are listed at most once
 * in pkgs. Afterwards the list is scanned for packages without depending
 * packages. Each such package is moved to sorted_pkgs in order.
 * If -r is given, all dependencies are inserted at the head of pkgs.
 * The loop has to continue as long as progress is made. This can happen
 * either because another package has been added to pkgs due to recursion
 * (head of pkgs changed) or because a package has no more depending packages
 * (tail of sorted_pkgs changed).
 *
 * If no progress is made, the remaining packages are moved to sorted_pkgs
 * and an error is returned for the !Force case.
 */
static int
sort_and_recurse(lpkg_head_t *pkgs, lpkg_head_t *sorted_pkgs)
{
	lpkg_t *lpp, *lpp2, *lpp_next, *lpp_old_tail, *lpp_old_head;
	int rv;

	TAILQ_FOREACH_SAFE(lpp, pkgs, lp_link, lpp_next) {
		TAILQ_FOREACH(lpp2, pkgs, lp_link) {
			if (lpp != lpp2 &&
			    strcmp(lpp->lp_name, lpp2->lp_name) == 0)
				break;
		}
		if (lpp2 == NULL)
			continue;
		TAILQ_REMOVE(pkgs, lpp, lp_link);
		free_lpkg(lpp);
	}

	while (!TAILQ_EMPTY(pkgs)) {
		lpp_old_tail = TAILQ_LAST(sorted_pkgs, _lpkg_head_t);
		lpp_old_head = TAILQ_FIRST(pkgs);

		TAILQ_FOREACH_SAFE(lpp, pkgs, lp_link, lpp_next) {
			rv = process_required_by(lpp->lp_name, pkgs,
			    sorted_pkgs, delete_recursive ? 1 : 0);
			if (rv)
				continue;
			TAILQ_REMOVE(pkgs, lpp, lp_link);
			TAILQ_INSERT_TAIL(sorted_pkgs, lpp, lp_link);
		}

		if (lpp_old_tail == TAILQ_LAST(sorted_pkgs, _lpkg_head_t) &&
		    lpp_old_head == TAILQ_FIRST(pkgs))
			break;
	}

	if (TAILQ_EMPTY(pkgs))
		return 0;

	while (!TAILQ_EMPTY(pkgs)) {
		lpp = TAILQ_FIRST(pkgs);
		TAILQ_REMOVE(pkgs, lpp, lp_link);
		fprintf(stderr,
		    "Package `%s' is still required by other packages:\n",
		    lpp->lp_name);		
		process_required_by(lpp->lp_name, NULL, sorted_pkgs, 2);
		if (Force) {
			TAILQ_INSERT_TAIL(sorted_pkgs, lpp, lp_link);
		} else
			free_lpkg(lpp);
	}

	return !Force;
}

struct find_leaves_data {
	lpkg_head_t *pkgs;
	int progress;
};

/*
 * Iterator for finding leaf packages.
 * Packages that are marked as not for deletion are not considered as
 * leaves.  For all other packages it is checked if at least one package
 * that depended on them is to be removed AND no depending package remains.
 * If that is the case, the package is appended to the sorted list.
 * As this package can't have depending packages left, the topological order
 * remains consistent.
 */
static int
find_new_leaves_iter(const char *pkg, void *cookie)
{
	char *fname;
	struct find_leaves_data *data = cookie;
	lpkg_t *lpp;

	fname = pkgdb_pkg_file(pkg, PRESERVE_FNAME);
	if (fexists(fname)) {
		free(fname);
		return 0;
	}
	free(fname);

	if (delete_automatic_leaves && !delete_new_leaves &&
	    !is_automatic_installed(pkg))
		return 0;

	/* Check whether this package is already on the list first. */
	TAILQ_FOREACH(lpp, data->pkgs, lp_link) {
		if (strcmp(lpp->lp_name, pkg) == 0)
			return 0;
	}

	if (process_required_by(pkg, NULL, data->pkgs, 3) == 1) {
		lpp = alloc_lpkg(pkg);
		TAILQ_INSERT_TAIL(data->pkgs, lpp, lp_link);
		data->progress = 1;
	}

	return 0;
}

/*
 * Iterate over all installed packages and look for new leaf packages.
 * As long as the loop adds one new leaf package, processing continues.
 */
static void
find_new_leaves(lpkg_head_t *pkgs)
{
	struct find_leaves_data data;

	data.pkgs = pkgs;
	do {
		data.progress = 0;
		iterate_pkg_db(find_new_leaves_iter, &data);
	} while (data.progress);
}

/*
 * Check that no entry on the package list is marked as not for deletion.
 */
static int
find_preserve_pkgs(lpkg_head_t *pkgs)
{
	lpkg_t *lpp, *lpp_next;
	char *fname;
	int found_preserve;

	found_preserve = 0;
	TAILQ_FOREACH_SAFE(lpp, pkgs, lp_link, lpp_next) {
		fname = pkgdb_pkg_file(lpp->lp_name, PRESERVE_FNAME);
		if (!fexists(fname)) {
			free(fname);
			continue;
		}
		free(fname);
		if (keep_preserve) {
			TAILQ_REMOVE(pkgs, lpp, lp_link);
			free_lpkg(lpp);
			continue;
		}
		if (!found_preserve)
			warnx("The following packages are marked as not "
			    "for deletion:");
		found_preserve = 1;
		fprintf(stderr, "\t%s\n", lpp->lp_name);
	}
	if (!found_preserve)
		return 0;
	if (Force == 0 || (!unregister_only && Force == 1))
		return 1;
	fprintf(stderr, "...but will delete them anyway\n");
	return 0;
}

/*
 * Run the +DEINSTALL script. Depending on whether this is
 * pre- or post-deinstall phase, different arguments are passed down.
 */
static int
run_deinstall_script(const char *pkg, int do_postdeinstall)
{
	const char *target, *text;
	char *fname, *pkgdir;
	int rv;

	fname = pkgdb_pkg_file(pkg, DEINSTALL_FNAME);
	if (!fexists(fname)) {
		free(fname);
		return 0;
	}

	if (do_postdeinstall) {
		target = "POST-DEINSTALL";
		text = "post-deinstall";
	} else {
		target = "DEINSTALL";
		text = "deinstall";
	}

	if (Fake) {
		printf("Would execute %s script with argument %s now\n",
		    text, target);
		free(fname);
		return 0;
	}

	pkgdir = pkgdb_pkg_dir(pkg);
	if (chmod(fname, 0555))
		warn("chmod of `%s' failed", fname);
	rv = fcexec(pkgdir, fname, pkg, target, NULL);
	if (rv)
		warnx("%s script returned error status", text);
	free(pkgdir);
	free(fname);
	return rv;
}

/*
 * Copy lines from fname to fname_tmp, filtering out lines equal to text.
 * Afterwards rename fname_tmp to fname;
 */
static int
remove_line(const char *fname, const char *fname_tmp, const char *text)
{
	FILE *fp, *fp_out;
	char line[MaxPathSize], *eol;
	int rv;

	if ((fp = fopen(fname, "r")) == NULL) {
		warn("Unable to open `%s'", fname);
		return 1;
	}
	if ((fp_out = fopen(fname_tmp, "w")) == NULL) {
		warn("Unable to open `%s'", fname_tmp);
		fclose(fp);
		return 1;
	}

	while (fgets(line, sizeof(line), fp) != NULL) {
		if ((eol = strrchr(line, '\n')) != NULL)
			*eol = '\0';
		if (strcmp(line, text) == 0)
			continue;
		fprintf(fp_out, "%s\n", line);
	}
	fclose(fp);

	if (fclose(fp_out) == EOF) {
		remove(fname_tmp);
		warnx("Failure while closing `%s' temp file", fname_tmp);
		return 1;
	}

	if (rename(fname_tmp, fname) == -1) {
		warn("Unable to rename `%s' to `%s'", fname_tmp, fname);
		rv = 1;
	} else
		rv = 0;
	remove(fname_tmp);

	return rv;
}

/*
 * remove_depend is used as iterator function below.
 * The passed-in package name should be removed from the
 * +REQUIRED_BY list of the dependency.  Such an entry
 * can miss in a fully correct package database, if the pattern
 * matches more than one package.
 */
static int
remove_depend(const char *cur_pkg, void *cookie)
{
	const char *pkg = cookie;
	char *fname, *fname2;
	int rv;

	fname = pkgdb_pkg_file(cur_pkg, REQUIRED_BY_FNAME);
	if (isemptyfile(fname)) {
		free(fname);
		return 0;
	}
	fname2 = pkgdb_pkg_file(cur_pkg, REQUIRED_BY_FNAME_TMP);

	rv = remove_line(fname, fname2, pkg);

	free(fname2);
	free(fname);

	return rv;
}

static int
remove_pkg(const char *pkg)
{
	FILE *fp;
	char *fname, *pkgdir;
	package_t plist;
	plist_t *p;
	int rv, late_error;

	if (pkgdb_update_only)
		return pkgdb_remove_pkg(pkg) ? 0 : 1;

	fname = pkgdb_pkg_file(pkg, CONTENTS_FNAME);
	if (!fexists(fname)) {
		warnx("package `%s' is not installed, `%s' missing", pkg, fname);
		free(fname);
		return 1;
	}
	free(fname);

	fname = pkgdb_pkg_file(pkg, CONTENTS_FNAME);
	if ((fp = fopen(fname, "r")) == NULL) {
		warnx("Failed to open `%s'", fname);
		free(fname);
		return 1;
	}
	read_plist(&plist, fp);
	fclose(fp);

	/*
	 * If a prefix has been provided, remove the first @cwd and
	 * prepend that prefix.  This allows removing packages without
	 * @cwd if really necessary.  pkg_admin rebuild is likely needed
	 * afterwards though.
	 */
	if (prefix) {
		delete_plist(&plist, FALSE, PLIST_CWD, NULL);
		add_plist_top(&plist, PLIST_CWD, prefix);
	}
	if ((p = find_plist(&plist, PLIST_CWD)) == NULL) {
		warnx("Package `%s' doesn't have a prefix", pkg);
		return 1;
	}

	if (find_plist(&plist, PLIST_NAME) == NULL) {
		/* Cheat a bit to allow removal of such bad packages. */
		warnx("Package `%s' doesn't have a name", pkg);
		add_plist_top(&plist, PLIST_NAME, pkg);
	}

	setenv(PKG_REFCOUNT_DBDIR_VNAME, config_pkg_refcount_dbdir, 1);
	fname = pkgdb_pkg_dir(pkg);
	setenv(PKG_METADATA_DIR_VNAME, fname, 1);
	free(fname);
	setenv(PKG_PREFIX_VNAME, p->name, 1);

	if (!no_deinstall && !unregister_only) {
		if (run_deinstall_script(pkg, 0) && !Force)
			return 1;
	}

	late_error = 0;

	if (Fake)
		printf("Attempting to delete package `%s'\n", pkg);
	else if (delete_package(FALSE, &plist, unregister_only,
			        destdir) == FAIL) {
		warnx("couldn't entirely delete package `%s'", pkg);
		/*
		 * XXX It could be nice to error out here explicitly,
		 * XXX but this is problematic for missing or changed files.
		 * XXX At least the inability to remove files at all should
		 * XXX be handled though.
		 */
	}

	/*
	 * Past the point of no return. Files are gone, all that is left
	 * is cleaning up registered dependencies and removing the meta data.
	 * Errors in the remaining part are counted, but don't stop the
	 * processing.
	 */

	for (p = plist.head; p; p = p->next) {
	    if (p->type != PLIST_PKGDEP)
		continue;
	    if (Verbose)
		printf("Attempting to remove dependency "
		       "on package `%s'\n", p->name);
	    if (Fake)
		continue;
	    match_installed_pkgs(p->name, remove_depend,
				 __UNCONST(pkg));
	}

	free_plist(&plist);

	if (!no_deinstall && !unregister_only)
		late_error |= run_deinstall_script(pkg, 1);

	if (Fake)
		return 0;

	/*
	 * Kill the pkgdb subdirectory. The files have been removed, so
	 * this is way beyond the point of no return.
	 */
	pkgdir = pkgdb_pkg_dir(pkg);
	(void) remove_files(pkgdir, "+*");
	rv = 1;
	if (isemptydir(pkgdir)&& rmdir(pkgdir) == 0)
		rv = 0;
	else if (!Force)
		warnx("Couldn't remove package directory in `%s'", pkgdir);
	else if (recursive_remove(pkgdir, 1))
		warn("Couldn't remove package directory `%s'", pkgdir);
	else
		warnx("Package directory `%s' forcefully removed", pkgdir);
	free(pkgdir);

	return rv | late_error;
}

int
main(int argc, char *argv[])
{
	lpkg_head_t pkgs, sorted_pkgs;
	int ch, r, has_error;
	unsigned long bad_count;

	TAILQ_INIT(&pkgs);
	TAILQ_INIT(&sorted_pkgs);

	setprogname(argv[0]);
	while ((ch = getopt(argc, argv, "ADFfK:kNnOP:p:RrVv")) != -1) {
		switch (ch) {
		case 'A':
			delete_automatic_leaves = 1;
			break;
		case 'D':
			no_deinstall = 1;
			break;
		case 'F':
			find_by_filename = 1;
			break;
		case 'f':
			++Force;
			break;
		case 'K':
			pkgdb_set_dir(optarg, 3);
			break;
		case 'k':
			keep_preserve = 1;
			break;
		case 'N':
			unregister_only = 1;
			break;
		case 'n':
			Fake = 1;
			break;
		case 'O':
			pkgdb_update_only = 1;
			break;
		case 'P':
			destdir = optarg;
			break;
		case 'p':
			prefix = optarg;
			break;
		case 'R':
			delete_new_leaves = 1;
			break;
		case 'r':
			delete_recursive = 1;
			break;
		case 'V':
			show_version();
			/* NOTREACHED */
		case 'v':
			++Verbose;
			break;
		default:
			usage();
			break;
		}
	}

	pkg_install_config();

	pkgdb = xstrdup(pkgdb_get_dir());

	if (destdir != NULL) {
		char *pkgdbdir;

		pkgdbdir = xasprintf("%s/%s", destdir, pkgdb);
		pkgdb_set_dir(pkgdbdir, 4);
		free(pkgdbdir);
	}

	argc -= optind;
	argv += optind;

	if (argc == 0) {
		if (find_by_filename)
			warnx("Missing filename(s)");
		else
			warnx("Missing package name(s)");
		usage();
	}

	if (Fake)
		r = pkgdb_open(ReadOnly);
	else
		r = pkgdb_open(ReadWrite);

	if (!r)
		errx(EXIT_FAILURE, "Opening pkgdb failed");

	/* First, process all command line options. */

	has_error = 0;
	for (; argc != 0; --argc, ++argv) {
		if (find_by_filename)
			has_error |= add_by_filename(&pkgs, *argv);
		else if (ispkgpattern(*argv))
			has_error |= add_by_pattern(&pkgs, *argv);
		else
			has_error |= add_by_pkgname(&pkgs, *argv);
	}

	if (has_error && !Force) {
		pkgdb_close();
		return EXIT_FAILURE;
	}

	/* Second, reorder and recursive if necessary. */

	if (sort_and_recurse(&pkgs, &sorted_pkgs)) {
		pkgdb_close();
		return EXIT_FAILURE;
	}

	/* Third, add leaves if necessary. */

	if (delete_new_leaves || delete_automatic_leaves)
		find_new_leaves(&sorted_pkgs);

	/*
	 * Now that all packages to remove are known, check
	 * if all are removable.  After that, start the actual
	 * removal.
	 */

	if (find_preserve_pkgs(&sorted_pkgs)) {
		pkgdb_close();
		return EXIT_FAILURE;
	}

	setenv(PKG_REFCOUNT_DBDIR_VNAME, pkgdb_refcount_dir(), 1);

	bad_count = 0;
	while (!TAILQ_EMPTY(&sorted_pkgs)) {
		lpkg_t *lpp;
		
		lpp = TAILQ_FIRST(&sorted_pkgs);
		TAILQ_REMOVE(&sorted_pkgs, lpp, lp_link);
		if (remove_pkg(lpp->lp_name)) {
			++bad_count;
			if (!Force)
				break;
		}
		free_lpkg(lpp);
	}

	pkgdb_close();

	if (Force && bad_count && Verbose)
		warnx("Removal of %lu packages failed", bad_count);

	return bad_count > 0 ? EXIT_FAILURE : EXIT_SUCCESS;
}
