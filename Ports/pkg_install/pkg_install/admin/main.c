/*	$NetBSD: main.c,v 1.69 2020/12/02 10:45:47 wiz Exp $	*/

#ifdef HAVE_NBTOOL_CONFIG_H
#include "nbtool_config.h"
#else
#if HAVE_CONFIG_H
#include "config.h"
#endif
#include <nbcompat.h>
#if HAVE_SYS_CDEFS_H
#include <sys/cdefs.h>
#endif
#endif
__RCSID("$NetBSD: main.c,v 1.69 2020/12/02 10:45:47 wiz Exp $");

/*-
 * Copyright (c) 1999-2019 The NetBSD Foundation, Inc.
 * All rights reserved.
 *
 * This code is derived from software contributed to The NetBSD Foundation
 * by Hubert Feyrer <hubert@feyrer.de> and
 * by Joerg Sonnenberger <joerg@NetBSD.org>.
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

#if HAVE_SYS_TYPES_H
#include <sys/types.h>
#endif
#if HAVE_SYS_STAT_H
#include <sys/stat.h>
#endif
#if HAVE_DIRENT_H
#include <dirent.h>
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
#ifndef NETBSD
#include <nbcompat/md5.h>
#include <nbcompat/sha2.h>
#else
#include <md5.h>
#include <sha2.h>
#endif
#if HAVE_LIMITS_H
#include <limits.h>
#endif
#if HAVE_STDIO_H
#include <stdio.h>
#endif
#if HAVE_STRING_H
#include <string.h>
#endif

#ifndef BOOTSTRAP
#include <archive.h>
#include <fetch.h>
#endif

#include "admin.h"
#include "lib.h"

#define DEFAULT_SFX	".t[bg]z"	/* default suffix for ls{all,best} */

struct pkgdb_count {
	size_t files;
	size_t directories;
	size_t packages;
};

/*
 * A hashed list of +REQUIRED_BY entries.
 */
struct reqd_by_entry {
	char *pkgname;
	SLIST_ENTRY(reqd_by_entry) entries;
};
SLIST_HEAD(reqd_by_entry_head, reqd_by_entry);

/*
 * A hashed list of packages that contain +REQUIRED_BY entries.
 */
struct pkg_reqd_by {
	char *pkgname;
	struct reqd_by_entry_head required_by[PKG_HASH_SIZE];
	SLIST_ENTRY(pkg_reqd_by) entries;
};
SLIST_HEAD(pkg_reqd_by_head, pkg_reqd_by);

static const char Options[] = "C:K:SVbd:qs:v";

int	quiet, verbose;

static void set_unset_variable(char **, Boolean);
static void digest_input(char **);

/* print usage message and exit */
void 
usage(void)
{
	(void) fprintf(stderr, "usage: %s [-bqSVv] [-C config] [-d lsdir] [-K pkg_dbdir] [-s sfx] command [args ...]\n"
	    "Where 'commands' and 'args' are:\n"
	    " rebuild                     - rebuild pkgdb from +CONTENTS files\n"
	    " rebuild-tree                - rebuild +REQUIRED_BY files from forward deps\n"
	    " check [pkg ...]             - check md5 checksum of installed files\n"
	    " add pkg ...                 - add pkg files to database\n"
	    " set variable=value pkg ...  - set installation variable for package\n"
	    " unset variable pkg ...      - unset installation variable for package\n"
	    " lsall /path/to/pkgpattern   - list all pkgs matching the pattern\n"
	    " lsbest /path/to/pkgpattern  - list pkgs matching the pattern best\n"
	    " dump                        - dump database\n"
	    " pmatch pattern pkg          - returns true if pkg matches pattern, otherwise false\n"
	    " fetch-pkg-vulnerabilities [-s] - fetch new vulnerability file\n"
	    " check-pkg-vulnerabilities [-s] <file> - check syntax and checksums of the vulnerability file\n"
	    " audit [-eis] [-t type] ...       - check installed packages for vulnerabilities\n"
	    " audit-pkg [-eis] [-t type] ...   - check listed packages for vulnerabilities\n"
	    " audit-batch [-eis] [-t type] ... - check packages in listed files for vulnerabilities\n"
	    " audit-history [-t type] ...     - print all advisories for package names\n"
	    " check-license <condition>       - check if condition is acceptable\n"
	    " check-single-license <license>  - check if license is acceptable\n"
	    " config-var name                 - print current value of the configuration variable\n"
	    " check-signature ...             - verify the signature of packages\n"
	    " x509-sign-package pkg spkg key cert  - create X509 signature\n"
	    " gpg-sign-package pkg spkg       - create GPG signature\n",
	    getprogname());
	exit(EXIT_FAILURE);
}

/*
 * add1pkg(<pkg>)
 *	adds the files listed in the +CONTENTS of <pkg> into the
 *	pkgdb.byfile.db database file in the current package dbdir.  It
 *	returns the number of files added to the database file.
 */
static int
add_pkg(const char *pkgdir, void *vp)
{
	FILE	       *f;
	plist_t	       *p;
	package_t	Plist;
	char 	       *contents;
	char *PkgName, *dirp;
	char 		file[MaxPathSize];
	struct pkgdb_count *count;

	if (!pkgdb_open(ReadWrite))
		err(EXIT_FAILURE, "cannot open pkgdb");

	count = vp;
	++count->packages;

	contents = pkgdb_pkg_file(pkgdir, CONTENTS_FNAME);
	if ((f = fopen(contents, "r")) == NULL)
		errx(EXIT_FAILURE, "%s: can't open `%s'", pkgdir, CONTENTS_FNAME);
	free(contents);

	read_plist(&Plist, f);
	if ((p = find_plist(&Plist, PLIST_NAME)) == NULL) {
		errx(EXIT_FAILURE, "Package `%s' has no @name, aborting.", pkgdir);
	}

	PkgName = p->name;
	dirp = NULL;
	for (p = Plist.head; p; p = p->next) {
		switch(p->type) {
		case PLIST_FILE:
			if (dirp == NULL) {
				errx(EXIT_FAILURE, "@cwd not yet found, please send-pr!");
			}
			(void) snprintf(file, sizeof(file), "%s/%s", dirp, p->name);
			if (!(isfile(file) || islinktodir(file))) {
				if (isbrokenlink(file)) {
					warnx("%s: Symlink `%s' exists and is in %s but target does not exist!",
						PkgName, file, CONTENTS_FNAME);
				} else {
					warnx("%s: File `%s' is in %s but not on filesystem!",
						PkgName, file, CONTENTS_FNAME);
				}
			} else {
				pkgdb_store(file, PkgName);
				++count->files;
			}
			break;
		case PLIST_PKGDIR:
			add_pkgdir(PkgName, dirp, p->name);
			++count->directories;
			break;
		case PLIST_CWD:
			if (strcmp(p->name, ".") != 0)
				dirp = p->name;
			else
				dirp = pkgdb_pkg_dir(pkgdir);
			break;
		case PLIST_IGNORE:
			p = p->next;
			break;
		case PLIST_SHOW_ALL:
		case PLIST_SRC:
		case PLIST_CMD:
		case PLIST_CHMOD:
		case PLIST_CHOWN:
		case PLIST_CHGRP:
		case PLIST_COMMENT:
		case PLIST_NAME:
		case PLIST_UNEXEC:
		case PLIST_DISPLAY:
		case PLIST_PKGDEP:
		case PLIST_DIR_RM:
		case PLIST_OPTION:
		case PLIST_PKGCFL:
		case PLIST_BLDDEP:
			break;
		}
	}
	free_plist(&Plist);
	fclose(f);
	pkgdb_close();

	return 0;
}

static void 
rebuild(void)
{
	char *cachename;
	struct pkgdb_count count;

	count.files = 0;
	count.directories = 0;
	count.packages = 0;

	cachename = pkgdb_get_database();
	if (unlink(cachename) != 0 && errno != ENOENT)
		err(EXIT_FAILURE, "unlink %s", cachename);

	setbuf(stdout, NULL);

	iterate_pkg_db(add_pkg, &count);

	printf("\n");
	printf("Stored %" PRIzu " file%s and %" PRIzu " explicit director%s"
	    " from %"PRIzu " package%s in %s.\n",
	    count.files, count.files == 1 ? "" : "s",
	    count.directories, count.directories == 1 ? "y" : "ies",
	    count.packages, count.packages == 1 ? "" : "s",
	    cachename);
}

static int
lspattern(const char *pkg, void *vp)
{
	const char *dir = vp;
	printf("%s/%s\n", dir, pkg);
	return 0;
}

static int
lsbasepattern(const char *pkg, void *vp)
{
	puts(pkg);
	return 0;
}

static int
remove_required_by(const char *pkgname, void *cookie)
{
	char *path;

	path = pkgdb_pkg_file(pkgname, REQUIRED_BY_FNAME);

	if (unlink(path) == -1 && errno != ENOENT)
		err(EXIT_FAILURE, "Cannot remove %s", path);

	free(path);

	return 0;
}

static void
add_required_by(const char *pattern, const char *pkgname, struct pkg_reqd_by_head *hash)
{
	struct pkg_reqd_by_head *phead;
	struct pkg_reqd_by *pkg;
	struct reqd_by_entry_head *ehead;
	struct reqd_by_entry *entry;
	char *best_installed;
	int i;

	best_installed = find_best_matching_installed_pkg(pattern, 1);
	if (best_installed == NULL) {
		warnx("Dependency %s of %s unresolved", pattern, pkgname);
		return;
	}

	/*
	 * Find correct reqd_by head based on hash of best_installed, which is
	 * the package in question that we are adding +REQUIRED_BY entries for.
	 */
	phead = &hash[PKG_HASH_ENTRY(best_installed)];

	/*
	 * Look for an existing entry in this hash list.
	 */
	SLIST_FOREACH(pkg, phead, entries) {
		if (strcmp(pkg->pkgname, best_installed) == 0) {

			/*
			 * Found an entry, now see if it already has a
			 * +REQUIRED_BY entry recorded for this pkgname,
			 * and if not then add it.
			 */
			ehead = &pkg->required_by[PKG_HASH_ENTRY(pkgname)];
			SLIST_FOREACH(entry, ehead, entries) {
				if (strcmp(entry->pkgname, pkgname) == 0)
					break;
			}

			if (entry == NULL) {
				entry = xmalloc(sizeof(*entry));
				entry->pkgname = xstrdup(pkgname);
				SLIST_INSERT_HEAD(ehead, entry, entries);
			}

			break;
		}
	}

	/*
	 * Create new package containing its first +REQUIRED_BY entry.
	 */
	if (pkg == NULL) {
		pkg = xmalloc(sizeof(*pkg));
		pkg->pkgname = xstrdup(best_installed);
		for (i = 0; i < PKG_HASH_SIZE; i++)
		       SLIST_INIT(&pkg->required_by[i]);

		ehead = &pkg->required_by[PKG_HASH_ENTRY(pkgname)];
		entry = xmalloc(sizeof(*entry));
		entry->pkgname = xstrdup(pkgname);
		SLIST_INSERT_HEAD(ehead, entry, entries);

		SLIST_INSERT_HEAD(phead, pkg, entries);
	}

	free(best_installed);
}

static int
add_depends_of(const char *pkgname, void *cookie)
{
	FILE *fp;
	struct pkg_reqd_by_head *h = cookie;
	plist_t *p;
	package_t plist;
	char *path;

	path = pkgdb_pkg_file(pkgname, CONTENTS_FNAME);
	if ((fp = fopen(path, "r")) == NULL)
		errx(EXIT_FAILURE, "Cannot read %s of package %s",
		    CONTENTS_FNAME, pkgname);
	free(path);
	read_plist(&plist, fp);
	fclose(fp);

	for (p = plist.head; p; p = p->next) {
		if (p->type == PLIST_PKGDEP)
			add_required_by(p->name, pkgname, h);
	}

	free_plist(&plist);	

	return 0;
}

static void
rebuild_tree(void)
{
	FILE *fp;
	struct pkg_reqd_by_head pkgs[PKG_HASH_SIZE];
	struct pkg_reqd_by *p;
	struct reqd_by_entry *e;
	int fd, i, j;
	char *path;

	for (i = 0; i < PKG_HASH_SIZE; i++)
		SLIST_INIT(&pkgs[i]);

	/*
	 * First, calculate all of the +REQUIRED_BY entries and store in our
	 * pkgs hashed list.
	 */
	if (iterate_pkg_db(add_depends_of, &pkgs) == -1)
		errx(EXIT_FAILURE, "cannot iterate pkgdb");

	/*
	 * Now we can remove all existing +REQUIRED_BY files.
	 */
	if (iterate_pkg_db(remove_required_by, NULL) == -1)
		errx(EXIT_FAILURE, "cannot iterate pkgdb");

	/*
	 * Finally, write out all the new +REQUIRED_BY files.
	 */
	for (i = 0; i < PKG_HASH_SIZE; i++) {
		SLIST_FOREACH(p, &pkgs[i], entries) {
			path = pkgdb_pkg_file(p->pkgname, REQUIRED_BY_FNAME);

			if ((fd = open(path, O_WRONLY | O_APPEND | O_CREAT,
			    0644)) == -1)
				errx(EXIT_FAILURE, "cannot write to %s", path);

			if ((fp = fdopen(fd, "a")) == NULL)
				errx(EXIT_FAILURE, "cannot open %s", path);

			for (j = 0; j < PKG_HASH_SIZE; j++) {
				SLIST_FOREACH(e, &p->required_by[j], entries)
					fprintf(fp, "%s\n", e->pkgname);
			}
			if (fclose(fp) == EOF) {
				remove(path);
				errx(EXIT_FAILURE, "cannot close %s", path);
			}
		}
	}
}

int 
main(int argc, char *argv[])
{
	Boolean		 use_default_sfx = TRUE;
	Boolean 	 show_basename_only = FALSE;
	char		 lsdir[MaxPathSize];
	char		 sfx[MaxPathSize];
	char		*lsdirp = NULL;
	int		 ch;

	setprogname(argv[0]);

	if (argc < 2)
		usage();

	while ((ch = getopt(argc, argv, Options)) != -1)
		switch (ch) {
		case 'C':
			config_file = optarg;
			break;

		case 'K':
			pkgdb_set_dir(optarg, 3);
			break;

		case 'S':
			sfx[0] = 0x0;
			use_default_sfx = FALSE;
			break;

		case 'V':
			show_version();
			/* NOTREACHED */

		case 'b':
			show_basename_only = TRUE;
			break;

		case 'd':
			(void) strlcpy(lsdir, optarg, sizeof(lsdir));
			lsdirp = lsdir;
			break;

		case 'q':
			quiet = 1;
			break;

		case 's':
			(void) strlcpy(sfx, optarg, sizeof(sfx));
			use_default_sfx = FALSE;
			break;

		case 'v':
			++verbose;
			break;

		default:
			usage();
			/* NOTREACHED */
		}

	argc -= optind;
	argv += optind;

	if (argc <= 0) {
		usage();
	}

	/*
	 * config-var is reading the config file implicitly,
	 * so skip it here.
	 */
	if (strcasecmp(argv[0], "config-var") != 0)
		pkg_install_config();

	if (use_default_sfx)
		(void) strlcpy(sfx, DEFAULT_SFX, sizeof(sfx));

	if (strcasecmp(argv[0], "pmatch") == 0) {

		char *pattern, *pkg;
		
		argv++;		/* "pmatch" */

		if (argv[0] == NULL || argv[1] == NULL) {
			usage();
		}

		pattern = argv[0];
		pkg = argv[1];

		if (pkg_match(pattern, pkg)){
			return 0;
		} else {
			return 1;
		}
	  
	} else if (strcasecmp(argv[0], "rebuild") == 0) {

		rebuild();
		printf("Done.\n");

	  
	} else if (strcasecmp(argv[0], "rebuild-tree") == 0) {

		rebuild_tree();
		printf("Done.\n");

	} else if (strcasecmp(argv[0], "check") == 0) {
		argv++;		/* "check" */

		check(argv);

		if (!quiet) {
			printf("Done.\n");
		}

	} else if (strcasecmp(argv[0], "lsall") == 0) {
		argv++;		/* "lsall" */

		while (*argv != NULL) {
			/* args specified */
			int     rc;
			const char *basep, *dir;

			dir = lsdirp ? lsdirp : dirname_of(*argv);
			basep = basename_of(*argv);

			if (show_basename_only)
				rc = match_local_files(dir, use_default_sfx, 1, basep, lsbasepattern, NULL);
			else
				rc = match_local_files(dir, use_default_sfx, 1, basep, lspattern, __UNCONST(dir));
			if (rc == -1)
				errx(EXIT_FAILURE, "Error from match_local_files(\"%s\", \"%s\", ...)",
				     dir, basep);

			argv++;
		}

	} else if (strcasecmp(argv[0], "lsbest") == 0) {
		argv++;		/* "lsbest" */

		while (*argv != NULL) {
			/* args specified */
			const char *basep, *dir;
			char *p;

			dir = lsdirp ? lsdirp : dirname_of(*argv);
			basep = basename_of(*argv);

			p = find_best_matching_file(dir, basep, use_default_sfx, 1);

			if (p) {
				if (show_basename_only)
					printf("%s\n", p);
				else
					printf("%s/%s\n", dir, p);
				free(p);
			}
			
			argv++;
		}
	} else if (strcasecmp(argv[0], "list") == 0 ||
	    strcasecmp(argv[0], "dump") == 0) {

		pkgdb_dump();

	} else if (strcasecmp(argv[0], "add") == 0) {
		struct pkgdb_count count;

		count.files = 0;
		count.directories = 0;
		count.packages = 0;

		for (++argv; *argv != NULL; ++argv)
			add_pkg(*argv, &count);
	} else if (strcasecmp(argv[0], "set") == 0) {
		argv++;		/* "set" */
		set_unset_variable(argv, FALSE);
	} else if (strcasecmp(argv[0], "unset") == 0) {
		argv++;		/* "unset" */
		set_unset_variable(argv, TRUE);
	} else if (strcasecmp(argv[0], "digest") == 0) {
		argv++;		/* "digest" */
		digest_input(argv);
	} else if (strcasecmp(argv[0], "config-var") == 0) {
		argv++;
		if (argv == NULL || argv[1] != NULL)
			errx(EXIT_FAILURE, "config-var takes exactly one argument");
		pkg_install_show_variable(argv[0]);
	} else if (strcasecmp(argv[0], "check-license") == 0) {
		if (argv[1] == NULL)
			errx(EXIT_FAILURE, "check-license takes exactly one argument");

		load_license_lists();

		switch (acceptable_pkg_license(argv[1])) {
		case 0:
			puts("no");
			return 0;
		case 1:
			puts("yes");
			return 0;
		case -1:
			errx(EXIT_FAILURE, "invalid license condition");
		}
	} else if (strcasecmp(argv[0], "check-single-license") == 0) {
		if (argv[1] == NULL)
			errx(EXIT_FAILURE, "check-license takes exactly one argument");
		load_license_lists();

		switch (acceptable_license(argv[1])) {
		case 0:
			puts("no");
			return 0;
		case 1:
			puts("yes");
			return 0;
		case -1:
			errx(EXIT_FAILURE, "invalid license");
		}
	}
#ifndef BOOTSTRAP
	else if (strcasecmp(argv[0], "findbest") == 0) {
		struct url *url;
		char *output;
		int rc;

		process_pkg_path();

		rc = 0;
		for (++argv; *argv != NULL; ++argv) {
			url = find_best_package(NULL, *argv, 1);
			if (url == NULL) {
				rc = 1;
				continue;
			}
			output = fetchStringifyURL(url);
			puts(output);
			fetchFreeURL(url);
			free(output);
		}		

		return rc;
	} else if (strcasecmp(argv[0], "fetch-pkg-vulnerabilities") == 0) {
		fetch_pkg_vulnerabilities(--argc, ++argv);
	} else if (strcasecmp(argv[0], "check-pkg-vulnerabilities") == 0) {
		check_pkg_vulnerabilities(--argc, ++argv);
	} else if (strcasecmp(argv[0], "audit") == 0) {
		audit_pkgdb(--argc, ++argv);
	} else if (strcasecmp(argv[0], "audit-pkg") == 0) {
		audit_pkg(--argc, ++argv);
	} else if (strcasecmp(argv[0], "audit-batch") == 0) {
		audit_batch(--argc, ++argv);
	} else if (strcasecmp(argv[0], "audit-history") == 0) {
		audit_history(--argc, ++argv);
	} else if (strcasecmp(argv[0], "check-signature") == 0) {
		struct archive *pkg;
		int rc;

		rc = 0;
		for (--argc, ++argv; argc > 0; --argc, ++argv) {
			char *archive_name;

			pkg = open_archive(*argv, &archive_name);
			if (pkg == NULL) {
				warnx("%s could not be opened", *argv);
				continue;
			}
			if (pkg_full_signature_check(archive_name, &pkg))
				rc = 1;
			free(archive_name);
			if (pkg != NULL)
				archive_read_free(pkg);
		}
		return rc;
	} else if (strcasecmp(argv[0], "x509-sign-package") == 0) {
#ifdef HAVE_SSL
		--argc;
		++argv;
		if (argc != 4)
			errx(EXIT_FAILURE, "x509-sign-package takes exactly four arguments");
		pkg_sign_x509(argv[0], argv[1], argv[2], argv[3]);
#else
		errx(EXIT_FAILURE, "OpenSSL support is not included");
#endif
	} else if (strcasecmp(argv[0], "gpg-sign-package") == 0) {
		--argc;
		++argv;
		if (argc != 2)
			errx(EXIT_FAILURE, "gpg-sign-package takes exactly two arguments");
		pkg_sign_gpg(argv[0], argv[1]);
	}
#endif
	else {
		usage();
	}

	return 0;
}

struct set_installed_info_arg {
	char *variable;
	char *value;
	int got_match;
};

static int
set_installed_info_var(const char *name, void *cookie)
{
	struct set_installed_info_arg *arg = cookie;
	char *filename;
	int retval;

	filename = pkgdb_pkg_file(name, INSTALLED_INFO_FNAME);

	retval = var_set(filename, arg->variable, arg->value);

	free(filename);
	arg->got_match = 1;

	return retval;
}

static void
set_unset_variable(char **argv, Boolean unset)
{
	struct set_installed_info_arg arg;
	char *eq;
	char *variable;
	int ret = 0;

	if (argv[0] == NULL || argv[1] == NULL)
		usage();
	
	variable = NULL;

	if (unset) {
		arg.variable = argv[0];
		arg.value = NULL;
	} else {	
		eq = NULL;
		if ((eq=strchr(argv[0], '=')) == NULL)
			usage();
		
		variable = xmalloc(eq-argv[0]+1);
		strlcpy(variable, argv[0], eq-argv[0]+1);
		
		arg.variable = variable;
		arg.value = eq+1;
		
		if (strcmp(variable, AUTOMATIC_VARNAME) == 0 &&
		    strcasecmp(arg.value, "yes") != 0 &&
		    strcasecmp(arg.value, "no") != 0) {
			errx(EXIT_FAILURE,
			     "unknown value `%s' for " AUTOMATIC_VARNAME,
			     arg.value);
		}
	}
	if (strpbrk(arg.variable, "ABCDEFGHIJKLMNOPQRSTUVWXYZ") != NULL) {
		free(variable);
		errx(EXIT_FAILURE,
		     "variable name must not contain uppercase letters");
	}

	argv++;
	while (*argv != NULL) {
		arg.got_match = 0;
		if (match_installed_pkgs(*argv, set_installed_info_var, &arg) == -1)
			errx(EXIT_FAILURE, "Cannot process pkdbdb");
		if (arg.got_match == 0) {
			char *pattern;

			if (ispkgpattern(*argv)) {
				warnx("no matching pkg for `%s'", *argv);
				ret++;
			} else {
				pattern = xasprintf("%s-[0-9]*", *argv);

				if (match_installed_pkgs(pattern, set_installed_info_var, &arg) == -1)
					errx(EXIT_FAILURE, "Cannot process pkdbdb");

				if (arg.got_match == 0) {
					warnx("cannot find package %s", *argv);
					++ret;
				}
				free(pattern);
			}
		}

		argv++;
	}

	if (ret > 0)
		exit(EXIT_FAILURE);

	free(variable);

	return;
}

static void
digest_input(char **argv)
{
	char digest[SHA256_DIGEST_STRING_LENGTH];
	int failures = 0;

	while (*argv != NULL) {
		if (SHA256_File(*argv, digest)) {
			puts(digest);
		} else {
			warn("cannot process %s", *argv);
			++failures;
		}
		argv++;
	}
	if (failures)
		exit(EXIT_FAILURE);
}
