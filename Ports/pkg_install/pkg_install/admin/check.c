/*	$NetBSD: check.c,v 1.12 2020/12/02 10:45:47 wiz Exp $	*/

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
__RCSID("$NetBSD: check.c,v 1.12 2020/12/02 10:45:47 wiz Exp $");

/*-
 * Copyright (c) 1999-2008 The NetBSD Foundation, Inc.
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
#else
#include <md5.h>
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

#include "admin.h"
#include "lib.h"

static int checkpattern_fn(const char *, void *);

/*
 * Assumes CWD is in the database directory ($PREFIX/pkgdb/<pkg>)!
 */
static void 
check1pkg(const char *pkgdir, int *filecnt, int *pkgcnt)
{
	FILE   *f;
	plist_t *p;
	package_t Plist;
	char   *PkgName, *dirp = NULL, *md5file;
	char    file[MaxPathSize];
	char   *content;

	content = pkgdb_pkg_file(pkgdir, CONTENTS_FNAME);
	f = fopen(content, "r");
	if (f == NULL)
		err(EXIT_FAILURE, "can't open %s", content);
	free(content);

	read_plist(&Plist, f);
	p = find_plist(&Plist, PLIST_NAME);
	if (p == NULL)
		errx(EXIT_FAILURE, "Package %s has no @name, aborting.",
		    pkgdir);
	PkgName = p->name;
	for (p = Plist.head; p; p = p->next) {
		switch (p->type) {
		case PLIST_FILE:
			if (dirp == NULL) {
				warnx("dirp not initialized, please send-pr!");
				abort();
			}
			
			(void) snprintf(file, sizeof(file), "%s/%s", dirp, p->name);

			if (isfile(file) || islinktodir(file)) {
				if (p->next && p->next->type == PLIST_COMMENT) {
					if (strncmp(p->next->name, CHECKSUM_HEADER, ChecksumHeaderLen) == 0) {
						if ((md5file = MD5File(file, NULL)) != NULL) {
							/* Mismatch? */
							if (strcmp(md5file, p->next->name + ChecksumHeaderLen) != 0)
								printf("%s fails MD5 checksum\n", file);

							free(md5file);
						}
					} else if (strncmp(p->next->name, SYMLINK_HEADER, SymlinkHeaderLen) == 0) {
						char	buf[MaxPathSize + SymlinkHeaderLen];
						int	cc;

						(void) strlcpy(buf, SYMLINK_HEADER, sizeof(buf));
						if ((cc = readlink(file, &buf[SymlinkHeaderLen],
							  sizeof(buf) - SymlinkHeaderLen - 1)) < 0) {
							warnx("can't readlink `%s'", file);
						} else {
							buf[SymlinkHeaderLen + cc] = 0x0;
							if (strcmp(buf, p->next->name) != 0) {
								printf("symlink (%s) is not same as recorded value, %s: %s\n",
								    file, buf, p->next->name);
							}
						}
					}
				}
				
				(*filecnt)++;
			} else if (isbrokenlink(file)) {
				warnx("%s: Symlink `%s' exists and is in %s but target does not exist!", PkgName, file, CONTENTS_FNAME);
			} else {
				warnx("%s: File `%s' is in %s but not on filesystem!", PkgName, file, CONTENTS_FNAME);
			}
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
		case PLIST_PKGDIR:
			break;
		}
	}
	free_plist(&Plist);
	fclose(f);
	(*pkgcnt)++;
}

struct checkpattern_arg {
	int filecnt;
	int pkgcnt;
	int got_match;
};

static int
checkpattern_fn(const char *pkg, void *vp)
{
	struct checkpattern_arg *arg = vp;

	check1pkg(pkg, &arg->filecnt, &arg->pkgcnt);
	if (!quiet)
		printf(".");

	arg->got_match = 1;

	return 0;
}

static void
check_pkg(const char *pkg, int *filecnt, int *pkgcnt, int allow_unmatched)
{
	struct checkpattern_arg arg;
	char *pattern;

	arg.filecnt = *filecnt;
	arg.pkgcnt = *pkgcnt;
	arg.got_match = 0;

	if (match_installed_pkgs(pkg, checkpattern_fn, &arg) == -1)
		errx(EXIT_FAILURE, "Cannot process pkdbdb");
	if (arg.got_match != 0) {
		*filecnt = arg.filecnt;
		*pkgcnt = arg.pkgcnt;
		return;
	}

	if (ispkgpattern(pkg)) {
		if (allow_unmatched)
			return;
		errx(EXIT_FAILURE, "No matching pkg for %s.", pkg);
	}

	pattern = xasprintf("%s-[0-9]*", pkg);

	if (match_installed_pkgs(pattern, checkpattern_fn, &arg) == -1)
		errx(EXIT_FAILURE, "Cannot process pkdbdb");

	if (arg.got_match == 0)
		errx(EXIT_FAILURE, "cannot find package %s", pkg);
	free(pattern);

	*filecnt = arg.filecnt;
	*pkgcnt = arg.pkgcnt;
}

void
check(char **argv)
{
	int filecnt, pkgcnt;

	filecnt = 0;
	pkgcnt = 0;
	setbuf(stdout, NULL);

	if (*argv == NULL) {
		check_pkg("*", &filecnt, &pkgcnt, 1);
	} else {
		for (; *argv != NULL; ++argv)
			check_pkg(*argv, &filecnt, &pkgcnt, 0);
	}

	printf("\n");
	printf("Checked %d file%s from %d package%s.\n",
	    filecnt, (filecnt == 1) ? "" : "s",
	    pkgcnt, (pkgcnt == 1) ? "" : "s");
}
