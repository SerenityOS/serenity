/* $NetBSD: info.h,v 1.22 2014/12/30 15:13:20 wiz Exp $ */

/* from FreeBSD Id: info.h,v 1.10 1997/02/22 16:09:40 peter Exp */

/*-
 * Copyright (c) 2008 Joerg Sonnenberger <joerg@NetBSD.org>.
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
 * 23 August 1993
 *
 * Include and define various things wanted by the info command.
 *
 */

#ifndef _INST_INFO_H_INCLUDE
#define _INST_INFO_H_INCLUDE

struct pkg_meta {
	char *meta_contents;
	char *meta_comment;
	char *meta_desc;
	char *meta_mtree;
	char *meta_build_version;
	char *meta_build_info;
	char *meta_size_pkg;
	char *meta_size_all;
	char *meta_required_by;
	char *meta_display;
	char *meta_install;
	char *meta_deinstall;
	char *meta_preserve;
	char *meta_installed_info;
	int is_installed;
};

#ifndef MAXINDEXSIZE
#define MAXINDEXSIZE 60
#endif

#ifndef MAXNAMESIZE
#define MAXNAMESIZE  20
#endif

#define SHOW_COMMENT		0x00001
#define SHOW_DESC		0x00002
#define SHOW_PLIST		0x00004
#define SHOW_INSTALL		0x00008
#define SHOW_DEINSTALL		0x00010
#define SHOW_PREFIX		0x00040
#define SHOW_INDEX		0x00080
#define SHOW_FILES		0x00100
#define SHOW_DISPLAY		0x00200
#define SHOW_REQBY		0x00400
#define SHOW_MTREE		0x00800
#define SHOW_BUILD_VERSION	0x01000
#define SHOW_BUILD_INFO		0x02000
#define SHOW_DEPENDS		0x04000
#define SHOW_PKG_SIZE		0x08000
#define SHOW_ALL_SIZE		0x10000
#define SHOW_BLD_DEPENDS	0x20000
#define SHOW_BI_VAR		0x40000
#define SHOW_SUMMARY		0x80000
#define SHOW_FULL_REQBY		0x100000

enum which {
    WHICH_ALL,
    WHICH_USER,
    WHICH_LIST
};

extern int Flags;
extern enum which Which;
extern Boolean File2Pkg;
extern Boolean Quiet;
extern const char *InfoPrefix;
extern const char *BuildInfoVariable;
extern lpkg_head_t pkgs;

int CheckForPkg(const char *);
int CheckForBestPkg(const char *);

void	show_file(const char *, const char *, Boolean);
void	show_var(const char *, const char *);
void	show_plist(const char *, package_t *, pl_ent_t);
void	show_files(const char *, package_t *);
void	show_depends(const char *, package_t *);
void	show_bld_depends(const char *, package_t *);
void	show_index(const char *, const char *);
void	show_summary(struct pkg_meta *, package_t *, const char *);
void	show_list(lpkg_head_t *, const char *);

int     pkg_perform(lpkg_head_t *);

#endif				/* _INST_INFO_H_INCLUDE */
