/*	$NetBSD: main.c,v 1.32 2015/12/27 12:36:42 joerg Exp $	*/

#if HAVE_CONFIG_H
#include "config.h"
#endif
#include <nbcompat.h>
#if HAVE_SYS_CDEFS_H
#include <sys/cdefs.h>
#endif
__RCSID("$NetBSD: main.c,v 1.32 2015/12/27 12:36:42 joerg Exp $");

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

#if HAVE_ERR_H
#include <err.h>
#endif
#include "lib.h"
#include "add.h"

static char Options[] = "AC:DIK:P:RVfhm:np:t:Uuv";

char   *Destdir = NULL;
char   *OverrideMachine = NULL;
char   *Prefix = NULL;
Boolean NoInstall = FALSE;
Boolean NoRecord = FALSE;
Boolean Automatic = FALSE;
Boolean ForceDepends = FALSE;
/*
 * Normally, updating fails if the dependencies of a depending package
 * are not satisfied by the package to be updated.  ForceDepending
 * turns that failure into a warning.
 */
Boolean ForceDepending = FALSE;

int	LicenseCheck = 0;
int     Replace = 0;
int	ReplaceSame = 0;

static void
usage(void)
{
	(void) fprintf(stderr, "%s\n%s\n%s\n",
	    "usage: pkg_add [-AfhInRuVv] [-C config] [-P destdir] [-K pkg_dbdir]",
	    "               [-m machine] [-p prefix]",
	    "               [[ftp|http]://[user[:password]@]host[:port]][/path/]pkg-name ...");
	exit(1);
}

int
main(int argc, char **argv)
{
	int     ch, error=0;
	lpkg_head_t pkgs;

	setprogname(argv[0]);
	while ((ch = getopt(argc, argv, Options)) != -1) {
		switch (ch) {
		case 'A':
			Automatic = TRUE;
			break;

		case 'C':
			config_file = optarg;
			break;

		case 'D':
			ForceDepending = TRUE;
			break;
			
		case 'P':
			Destdir = optarg;
			break;

		case 'f':
			Force = TRUE;
			ForceDepends = TRUE;
			ForceDepending = TRUE;
			break;

		case 'I':
			NoInstall = TRUE;
			break;

		case 'K':
			pkgdb_set_dir(optarg, 3);
			break;

		case 'R':
			NoRecord = TRUE;
			break;

		case 'm':
			OverrideMachine = optarg;
			break;

		case 'n':
			Fake = TRUE;
			Verbose = TRUE;
			break;

		case 'p':
			Prefix = optarg;
			break;

		case 'U':
			ReplaceSame = 1;
			Replace = 1;
			break;

		case 'u':
			Replace = 1;
			break;

		case 'V':
			show_version();
			/* NOTREACHED */

		case 'v':
			Verbose = TRUE;
			break;

		case 'h':
		case '?':
		default:
			usage();
			break;
		}
	}
	argc -= optind;
	argv += optind;

	pkg_install_config();

	if (Destdir != NULL) {
		char *pkgdbdir;

		pkgdbdir = xasprintf("%s/%s", Destdir, config_pkg_dbdir);
		pkgdb_set_dir(pkgdbdir, 4);
		free(pkgdbdir);
	}

#ifndef BOOTSTRAP
	process_pkg_path();
#endif

	TAILQ_INIT(&pkgs);

	if (argc == 0) {
		/* If no packages, yelp */
		warnx("missing package name(s)");
		usage();
	}

#ifndef BOOTSTRAP
	if (strcasecmp(do_license_check, "no") == 0)
		LicenseCheck = 0;
	else if (strcasecmp(do_license_check, "yes") == 0)
		LicenseCheck = 1;
	else if (strcasecmp(do_license_check, "always") == 0)
		LicenseCheck = 2;
	else
		errx(1, "Unknown value of the configuration variable"
		    "CHECK_LICENSE");

	if (LicenseCheck)
		load_license_lists();
#endif

	/* Get all the remaining package names, if any */
	for (; argc > 0; --argc, ++argv) {
		lpkg_t *lpp;

		if (IS_STDIN(*argv))
			lpp = alloc_lpkg("-");
		else
			lpp = alloc_lpkg(*argv);

		TAILQ_INSERT_TAIL(&pkgs, lpp, lp_link);
	}

	error += pkg_perform(&pkgs);
	if (error != 0) {
		warnx("%d package addition%s failed", error, error == 1 ? "" : "s");
		exit(1);
	}
	exit(0);
}
