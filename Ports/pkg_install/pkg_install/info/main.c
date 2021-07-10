/*	$NetBSD: main.c,v 1.32 2018/04/25 12:20:53 joerg Exp $	*/

#if HAVE_CONFIG_H
#include "config.h"
#endif
#include <nbcompat.h>
#if HAVE_SYS_CDEFS_H
#include <sys/cdefs.h>
#endif
__RCSID("$NetBSD: main.c,v 1.32 2018/04/25 12:20:53 joerg Exp $");

/*
 *
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
 * This is the add module.
 *
 */

#if HAVE_SYS_IOCTL_H
#include <sys/ioctl.h>
#endif

#if HAVE_ERR_H
#include <err.h>
#endif

#include "lib.h"
#include "info.h"

static const char Options[] = ".aBbcDde:E:fFhIiK:kLl:mNnpQ:qrRsSuvVX";

int     Flags = 0;
enum which Which = WHICH_LIST;
Boolean File2Pkg = FALSE;
Boolean Quiet = FALSE;
const char   *InfoPrefix = "";
const char   *BuildInfoVariable = "";
lpkg_head_t pkgs;

static void
usage(void)
{
	fprintf(stderr, "%s\n%s\n%s\n%s\n",
	    "usage: pkg_info [-BbcDdFfhIikLmNnpqRrSsVvX] [-E pkg-name] [-e pkg-name]",
	    "                [-K pkg_dbdir] [-l prefix] pkg-name ...",
	    "       pkg_info [-a | -u] [flags]",
	    "       pkg_info [-Q variable] pkg-name ...");
	exit(1);
}

int
main(int argc, char **argv)
{
	char *CheckPkg = NULL;
	char *BestCheckPkg = NULL;
	lpkg_t *lpp;
	int     ch;
	int	rc;

	setprogname(argv[0]);
	while ((ch = getopt(argc, argv, Options)) != -1)
		switch (ch) {
		case '.':	/* for backward compatibility */
			break;

		case 'a':
			Which = WHICH_ALL;
			break;

		case 'B':
			Flags |= SHOW_BUILD_INFO;
			break;

		case 'b':
			Flags |= SHOW_BUILD_VERSION;
			break;

		case 'c':
			Flags |= SHOW_COMMENT;
			break;

		case 'D':
			Flags |= SHOW_DISPLAY;
			break;

		case 'd':
			Flags |= SHOW_DESC;
			break;

		case 'E':
			BestCheckPkg = optarg;
			break;

		case 'e':
			CheckPkg = optarg;
			break;

		case 'f':
			Flags |= SHOW_PLIST;
			break;

		case 'F':
			File2Pkg = 1;
			break;

		case 'I':
			Flags |= SHOW_INDEX;
			break;

		case 'i':
			Flags |= SHOW_INSTALL;
			break;

		case 'K':
			pkgdb_set_dir(optarg, 3);
			break;

		case 'k':
			Flags |= SHOW_DEINSTALL;
			break;

		case 'L':
			Flags |= SHOW_FILES;
			break;

		case 'l':
			InfoPrefix = optarg;
			break;

		case 'm':
			Flags |= SHOW_MTREE;
			break;

		case 'N':
			Flags |= SHOW_BLD_DEPENDS;
			break;

		case 'n':
			Flags |= SHOW_DEPENDS;
			break;

		case 'p':
			Flags |= SHOW_PREFIX;
			break;

		case 'Q':
			Flags |= SHOW_BI_VAR;
			BuildInfoVariable = optarg;
			break;

		case 'q':
			Quiet = TRUE;
			break;

		case 'r':
			Flags |= SHOW_FULL_REQBY;
			break;

		case 'R':
			Flags |= SHOW_REQBY;
			break;

		case 's':
			Flags |= SHOW_PKG_SIZE;
			break;

		case 'S':
			Flags |= SHOW_ALL_SIZE;
			break;

		case 'u':
			Which = WHICH_USER;
			break;

		case 'v':
			Verbose = TRUE;
			/* Reasonable definition of 'everything' */
			Flags = SHOW_COMMENT | SHOW_DESC | SHOW_PLIST | SHOW_INSTALL |
			    SHOW_DEINSTALL | SHOW_DISPLAY | SHOW_MTREE |
			    SHOW_REQBY | SHOW_BLD_DEPENDS | SHOW_DEPENDS | SHOW_PKG_SIZE | SHOW_ALL_SIZE;
			break;

		case 'V':
			show_version();
			/* NOTREACHED */

		case 'X':
			Flags |= SHOW_SUMMARY;
			break;

		case 'h':
		case '?':
		default:
			usage();
			/* NOTREACHED */
		}

	argc -= optind;
	argv += optind;

	pkg_install_config();

	if (argc == 0 && !Flags && !CheckPkg) {
		/* No argument or relevant flags specified - assume -I */
		Flags = SHOW_INDEX;
		/* assume -a if neither -u nor -a is given */
		if (Which == WHICH_LIST)
			Which = WHICH_ALL;
	}

	if (CheckPkg != NULL && BestCheckPkg != NULL) {
		warnx("-E and -e are mutally exlusive");
		usage();
	}

	if (argc != 0 && CheckPkg != NULL) {
		warnx("can't give any additional arguments to -e");
		usage();
	}

	if (argc != 0 && BestCheckPkg != NULL) {
		warnx("can't give any additional arguments to -E");
		usage();
	}

	if (argc != 0 && Which != WHICH_LIST) {
		warnx("can't use both -a/-u and package name");
		usage();
	}

	/* Set some reasonable defaults */
	if (!Flags)
		Flags = SHOW_COMMENT | SHOW_DESC | SHOW_REQBY 
			| SHOW_DEPENDS | SHOW_DISPLAY;

	/* -Fe /filename -> change CheckPkg to real packagename */
	if (CheckPkg) {
		if (File2Pkg) {
			char   *s;

			if (!pkgdb_open(ReadOnly))
				err(EXIT_FAILURE, "cannot open pkgdb");

			s = pkgdb_retrieve(CheckPkg);

			if (s == NULL)
				errx(EXIT_FAILURE, "No matching pkg for %s.", CheckPkg);
			CheckPkg = xstrdup(s);

			pkgdb_close();
		}
		return CheckForPkg(CheckPkg);
	}

	if (BestCheckPkg)
		return CheckForBestPkg(BestCheckPkg);

	TAILQ_INIT(&pkgs);

	/* Get all the remaining package names, if any */
	if (File2Pkg && Which == WHICH_LIST)
		if (!pkgdb_open(ReadOnly)) {
			err(EXIT_FAILURE, "cannot open pkgdb");
		}
	while (*argv) {
		/* pkgdb: if -F flag given, don't add pkgnames to the "pkgs"
		 * queue but rather resolve the given filenames to pkgnames
		 * using pkgdb_retrieve, then add them. */
		if (File2Pkg) {
			char   *s;

			s = pkgdb_retrieve(*argv);

			if (s) {
				lpp = alloc_lpkg(s);
				TAILQ_INSERT_TAIL(&pkgs, lpp, lp_link);
			} else
				errx(EXIT_FAILURE, "No matching pkg for %s.", *argv);
		} else {
			if (ispkgpattern(*argv)) {
				switch (add_installed_pkgs_by_pattern(*argv, &pkgs)) {
				case 0:
					errx(EXIT_FAILURE, "No matching pkg for %s.", *argv);
				case -1:
					errx(EXIT_FAILURE, "Error during search in pkgdb for %s", *argv);
				}
			} else {
				const char *dbdir;
				size_t dbdirlen;

				dbdir = pkgdb_get_dir();
				dbdirlen = strlen(dbdir);
				if (**argv == '/' &&
				    strncmp(*argv, dbdir, dbdirlen) == 0 &&
				    (*argv)[dbdirlen] == '/') {
					*argv += dbdirlen + 1;
					if (**argv && (*argv)[strlen(*argv) - 1] == '/') {
						(*argv)[strlen(*argv) - 1] = 0;
					}
				}
				lpp = alloc_lpkg(*argv);
				TAILQ_INSERT_TAIL(&pkgs, lpp, lp_link);
			}
		}
		argv++;
	}

	if (File2Pkg)
		pkgdb_close();

	/* If no packages, yelp */
	if (TAILQ_FIRST(&pkgs) == NULL && Which == WHICH_LIST && !CheckPkg)
		warnx("missing package name(s)"), usage();

	rc = pkg_perform(&pkgs);
	exit(rc);
	/* NOTREACHED */
}
