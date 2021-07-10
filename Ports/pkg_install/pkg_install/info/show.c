/*	$NetBSD: show.c,v 1.33 2012/02/21 18:32:14 wiz Exp $	*/

#if HAVE_CONFIG_H
#include "config.h"
#endif
#include <nbcompat.h>
#if HAVE_SYS_CDEFS_H
#include <sys/cdefs.h>
#endif
__RCSID("$NetBSD: show.c,v 1.33 2012/02/21 18:32:14 wiz Exp $");

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
 * 23 Aug 1993
 *
 * Various display routines for the info module.
 *
 */
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

#if HAVE_ERR_H
#include <err.h>
#endif

#include "defs.h"
#include "lib.h"
#include "info.h"

/* Structure to define entries for the "show table" */
typedef struct show_t {
	pl_ent_t sh_type;	/* type of entry */
	const char *sh_quiet;	/* message when quiet */
	const char *sh_verbose;	/* message when verbose */
}       show_t;

/*
 * The entries in this table must be ordered the same as
 * pl_ent_t constants
 */
static const show_t showv[] = {
	{PLIST_FILE, "", "\tFile: "},
	{PLIST_CWD, "@cwd ", "\tCWD to: "},
	{PLIST_CMD, "@exec ", "\tEXEC ''"},
	{PLIST_CHMOD, "@chmod ", "\tCHMOD to "},
	{PLIST_CHOWN, "@chown ", "\tCHOWN to "},
	{PLIST_CHGRP, "@chgrp ", "\tCHGRP to "},
	{PLIST_COMMENT, "@comment ", "\tComment: "},
	{PLIST_IGNORE, "@ignore", "Ignore next file:"},
	{PLIST_NAME, "@name ", "\tPackage name: "},
	{PLIST_UNEXEC, "@unexec ", "\tUNEXEC ''"},
	{PLIST_SRC, "@src: ", "\tSRC to: "},
	{PLIST_DISPLAY, "@display ", "\tInstall message file: "},
	{PLIST_PKGDEP, "@pkgdep ", "\tPackage depends on: "},
	{PLIST_DIR_RM, "@dirrm ", "\tObsolete deinstall directory removal hint: "},
	{PLIST_OPTION, "@option ", "\tPackage has option: "},
	{PLIST_PKGCFL, "@pkgcfl ", "\tPackage conflicts with: "},
	{PLIST_BLDDEP, "@blddep ", "\tPackage depends exactly on: "},
	{PLIST_PKGDIR, "@pkgdir ", "\tManaged directory: "},
	{-1, NULL, NULL}
};

static int print_string_as_var(const char *, const char *);

void
show_file(const char *buf, const char *title, Boolean separator)
{
	size_t len;

	if (!Quiet)
		printf("%s%s", InfoPrefix, title);

	len = strlen(buf);
	if (len == 0 || buf[len - 1] != '\n')
		puts(buf);
	else
		fputs(buf, stdout);

	if (!Quiet || separator)
		printf("\n");
}

void
show_var(const char *buf, const char *variable)
{
	char   *value;

	if (buf == NULL)
		return;

	if ((value = var_get_memory(buf, variable)) != NULL) {
		(void) printf("%s\n", value);
		free(value);
	}
}

void
show_index(const char *buf, const char *title)
{
	size_t len;

	if (!Quiet)
		printf("%s%s", InfoPrefix, title);

	len = strlen(buf);
	if (len == 0 || buf[len - 1] != '\n')
		puts(buf);
	else
		fputs(buf, stdout);	
}

/*
 * Show a packing list item type.  If type is PLIST_SHOW_ALL, show all
 */
void
show_plist(const char *title, package_t *plist, pl_ent_t type)
{
	plist_t *p;
	Boolean ign;

	if (!Quiet) {
		printf("%s%s", InfoPrefix, title);
	}
	for (ign = FALSE, p = plist->head; p; p = p->next) {
		if (p->type == type || type == PLIST_SHOW_ALL) {
			switch (p->type) {
			case PLIST_FILE:
				printf("%s%s",
				    Quiet ? showv[p->type].sh_quiet :
				    showv[p->type].sh_verbose, p->name);
				if (ign) {
					if (!Quiet) {
						printf(" (ignored)");
					}
					ign = FALSE;
				}
				break;
			case PLIST_CHMOD:
			case PLIST_CHOWN:
			case PLIST_CHGRP:
				printf("%s%s",
				    Quiet ?  showv[p->type].sh_quiet :
				    showv[p->type].sh_verbose,
				    p->name ? p->name : "(clear default)");
				break;
			case PLIST_IGNORE:
				printf("%s", Quiet ? showv[p->type].sh_quiet :
				    showv[p->type].sh_verbose);
				ign = TRUE;
				break;
			case PLIST_CWD:
			case PLIST_CMD:
			case PLIST_SRC:
			case PLIST_UNEXEC:
			case PLIST_COMMENT:
			case PLIST_NAME:
			case PLIST_DISPLAY:
			case PLIST_PKGDEP:
			case PLIST_DIR_RM:
			case PLIST_OPTION:
			case PLIST_PKGCFL:
			case PLIST_BLDDEP:
			case PLIST_PKGDIR:
				printf("%s%s",
				    Quiet ? showv[p->type].sh_quiet :
				    showv[p->type].sh_verbose,
				    p->name ? p->name : "(null)");
				break;
			default:
				warnx("unknown command type %d (%s)", p->type, p->name);
			}
			(void) fputc('\n', stdout);
		}
	}
}

/*
 * Show all files in the packing list (except ignored ones)
 */
void
show_files(const char *title, package_t *plist)
{
	plist_t *p;
	Boolean ign;
	const char *dir = ".";

	if (!Quiet) {
		printf("%s%s", InfoPrefix, title);
	}
	for (ign = FALSE, p = plist->head; p; p = p->next) {
		switch (p->type) {
		case PLIST_FILE:
			if (!ign) {
				printf("%s%s%s\n", dir,
					(strcmp(dir, "/") == 0) ? "" : "/", p->name);
			}
			ign = FALSE;
			break;
		case PLIST_CWD:
			dir = p->name;
			break;
		case PLIST_IGNORE:
			ign = TRUE;
			break;
		default:
			break;
		}
	}
}

/*
 * Show dependencies (packages this pkg requires)
 */
void
show_depends(const char *title, package_t *plist)
{
	plist_t *p;
	int     nodepends;

	nodepends = 1;
	for (p = plist->head; p && nodepends; p = p->next) {
		switch (p->type) {
		case PLIST_PKGDEP:
			nodepends = 0;
			break;
		default:
			break;
		}
	}
	if (nodepends)
		return;

	if (!Quiet) {
		printf("%s%s", InfoPrefix, title);
	}
	for (p = plist->head; p; p = p->next) {
		switch (p->type) {
		case PLIST_PKGDEP:
			printf("%s\n", p->name);
			break;
		default:
			break;
		}
	}

	printf("\n");
}

/*
 * Show exact dependencies (packages this pkg was built with)
 */
void
show_bld_depends(const char *title, package_t *plist)
{
	plist_t *p;
	int     nodepends;

	nodepends = 1;
	for (p = plist->head; p && nodepends; p = p->next) {
		switch (p->type) {
		case PLIST_BLDDEP:
			nodepends = 0;
			break;
		default:
			break;
		}
	}
	if (nodepends)
		return;

	if (!Quiet) {
		printf("%s%s", InfoPrefix, title);
	}
	for (p = plist->head; p; p = p->next) {
		switch (p->type) {
		case PLIST_BLDDEP:
			printf("%s\n", p->name);
			break;
		default:
			break;
		}
	}

	printf("\n");
}


/*
 * Show entry for pkg_summary.txt file.
 */
void
show_summary(struct pkg_meta *meta, package_t *plist, const char *binpkgfile)
{
	static const char *bi_vars[] = {
		"PKGPATH",
		"CATEGORIES",
		"PROVIDES",
		"REQUIRES",
		"PKG_OPTIONS",
		"OPSYS",
		"OS_VERSION",
		"MACHINE_ARCH",
		"LICENSE",
		"HOMEPAGE",
		"PKGTOOLS_VERSION",
		"BUILD_DATE",
		"PREV_PKGPATH",
		"SUPERSEDES",
		NULL
	};
	
	plist_t *p;
	struct stat st;

	for (p = plist->head; p; p = p->next) {
		switch (p->type) {
		case PLIST_NAME:
			printf("PKGNAME=%s\n", p->name);
			break;
		case PLIST_PKGDEP:
			printf("DEPENDS=%s\n", p->name);
			break;
		case PLIST_PKGCFL:
			printf("CONFLICTS=%s\n", p->name);
			break;

		default:
			break;
		}
	}

	print_string_as_var("COMMENT", meta->meta_comment);
	if (meta->meta_size_pkg)
		print_string_as_var("SIZE_PKG", meta->meta_size_pkg);

	if (meta->meta_build_info)
		var_copy_list(meta->meta_build_info, bi_vars);
	else
		warnx("Build information missing");

	if (binpkgfile != NULL && stat(binpkgfile, &st) == 0) {
	    const char *base;

	    base = strrchr(binpkgfile, '/');
	    if (base == NULL)
		base = binpkgfile;
	    else
		base++;
	    printf("FILE_NAME=%s\n", base);
	    printf("FILE_SIZE=%" MY_PRIu64 "\n", (uint64_t)st.st_size);
	    /* XXX: DIGETS */
	}

	print_string_as_var("DESCRIPTION", meta->meta_desc);
	putc('\n', stdout);
}

/*
 * Print the contents of file fname as value of variable var to stdout.
 */
static int
print_string_as_var(const char *var, const char *str)
{
	const char *eol;

	while ((eol = strchr(str, '\n')) != NULL) {
		printf("%s=%.*s\n", var, (int)(eol - str), str);
		str = eol + 1;
	}
	if (*str)
		printf("%s=%s\n", var, str);

	return 0;
}

void
show_list(lpkg_head_t *pkghead, const char *title)
{
	lpkg_t *lpp;

	if (!Quiet)
		printf("%s%s", InfoPrefix, title);

	while ((lpp = TAILQ_FIRST(pkghead)) != NULL) {
		TAILQ_REMOVE(pkghead, lpp, lp_link);
		puts(lpp->lp_name);
		free_lpkg(lpp);
	}

	if (!Quiet)
		printf("\n");
}
