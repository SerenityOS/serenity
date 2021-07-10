/*	$NetBSD: pl.c,v 1.16 2020/12/02 10:45:47 wiz Exp $	*/

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
__RCSID("$NetBSD: pl.c,v 1.16 2020/12/02 10:45:47 wiz Exp $");

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
 * Routines for dealing with the packing list.
 *
 */

#include "lib.h"
#include "create.h"
#if HAVE_ERR_H
#include <err.h>
#endif
#ifndef NETBSD
#include <nbcompat/md5.h>
#else
#include <md5.h>
#endif

/*
 * Check that any symbolic link is relative to the prefix
 */
static void
CheckSymlink(char *name, char *prefix, size_t prefixcc)
{
	char    newtgt[MaxPathSize];
	char    oldtgt[MaxPathSize];
	char   *slash;
	int     slashc;
	int     cc;
	int     i;

	if ((cc = readlink(name, oldtgt, sizeof(oldtgt) - 1)) > 0) {
		oldtgt[cc] = 0;
		if (strncmp(oldtgt, prefix, prefixcc) == 0 && oldtgt[prefixcc] == '/') {
			for (slashc = 0, slash = &name[prefixcc + 1]; (slash = strchr(slash, '/')) != (char *) NULL; slash++, slashc++) {
			}
			for (cc = i = 0; i < slashc; i++) {
				strlcpy(&newtgt[cc], "../", sizeof(newtgt) - cc);
				cc += 3;
			}
			strlcpy(&newtgt[cc], &oldtgt[prefixcc + 1], sizeof(newtgt) - cc);
			(void) fprintf(stderr, "Full pathname symlink `%s' is target of `%s' - adjusting to `%s'\n", oldtgt, name, newtgt);
			if (unlink(name) != 0) {
				warn("can't unlink `%s'", name);
			} else if (symlink(newtgt, name) != 0) {
				warn("can't symlink `%s' called `%s'", newtgt, name);
			}
		}
	}
}

/*
 * Check a list for files that require preconversion
 */
void
check_list(package_t *pkg, const char *PkgName)
{
	struct stat st;
	plist_t *tmp;
	plist_t *p;
	char    buf[ChecksumHeaderLen + LegibleChecksumLen];
	char    target[MaxPathSize + SymlinkHeaderLen];
	char    name[MaxPathSize];
	char   *cwd = NULL;
	char   *pkgname = NULL;
	int	cc;

	for (p = pkg->head; p; p = p->next) {
		switch (p->type) {
		case PLIST_CWD:
			cwd = p->name;
			break;
		case PLIST_NAME:
			pkgname = p->name;
			break;
		case PLIST_IGNORE:
			p = p->next;
			break;
		case PLIST_PKGDIR:
			if (cwd == NULL)
				errx(2, "@pkgdir without preceding @cwd found");
			if (pkgname == NULL)
				errx(2, "@pkgdir without preceding @name found");
			break;
		case PLIST_FILE:
			if (cwd == NULL)
				errx(2, "file without preceding @cwd found");
			(void) snprintf(name, sizeof(name), "%s%s%s",
				cwd,
				(strcmp(cwd, "/") == 0) ? "" : "/",
				p->name);
			if (lstat(name, &st) < 0) {
				warnx("can't stat `%s'", name);
				continue;
			}
			switch (st.st_mode & S_IFMT) {
			case S_IFDIR:
				warnx("Warning - directory `%s' in PLIST", name);
				break;
			case S_IFLNK:
				if (RelativeLinks) {
					CheckSymlink(name, cwd, strlen(cwd));
				}
				(void) strlcpy(target, SYMLINK_HEADER,
				    sizeof(target));
				if ((cc = readlink(name, &target[SymlinkHeaderLen],
					  sizeof(target) - SymlinkHeaderLen - 1)) < 0) {
					warnx("can't readlink `%s'", name);
					continue;
				}
				target[SymlinkHeaderLen + cc] = 0x0;
				tmp = new_plist_entry();
				tmp->name = xstrdup(target);
				tmp->type = PLIST_COMMENT;
				tmp->next = p->next;
				tmp->prev = p;
				if (p == pkg->tail) {
					pkg->tail = tmp;
				}
				p->next = tmp;
				p = tmp;
				break;
			case S_IFCHR:
				warnx("Warning - char special device `%s' in PLIST", name);
				break;
			case S_IFBLK:
				warnx("Warning - block special device `%s' in PLIST", name);
				break;
			default:
				(void) strlcpy(buf, CHECKSUM_HEADER,
				    sizeof(buf));
				if (MD5File(name, &buf[ChecksumHeaderLen]) != (char *) NULL) {
					tmp = new_plist_entry();
					tmp->name = xstrdup(buf);
					tmp->type = PLIST_COMMENT;	/* PLIST_MD5 - HF */
					tmp->next = p->next;
					tmp->prev = p;
					if (p == pkg->tail) {
						pkg->tail = tmp;
					}
					p->next = tmp;
					p = tmp;
				}
				break;
			}
			break;
		default:
			break;
		}
	}
}
