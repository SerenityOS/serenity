/* $NetBSD: create.h,v 1.16 2016/04/10 19:01:19 joerg Exp $ */

/* from FreeBSD Id: create.h,v 1.13 1997/10/08 07:46:19 charnier Exp */

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
 * Include and define various things wanted by the create command.
 *
 */

#ifndef _INST_CREATE_H_INCLUDE
#define _INST_CREATE_H_INCLUDE

struct memory_file {
	struct stat st;
	const char *name;
	const char *owner;
	const char *group;
	mode_t mode;

	char *data;
	size_t len;
};

extern char *Prefix;
extern char *Comment;
extern char *Desc;
extern char *Display;
extern char *Install;
extern char *DeInstall;
extern char *Contents;
extern char *Pkgdeps;
extern char *BuildPkgdeps;
extern char *Pkgcfl;
extern char *BuildVersion;
extern char *BuildInfo;
extern char *SizePkg;
extern char *SizeAll;
extern char *Preserve;
extern char *realprefix;
extern char *DefaultOwner;
extern char *DefaultGroup;
extern const char *CompressionType;
extern int PlistOnly;
extern int RelativeLinks;

void    check_list(package_t *, const char *);
void    copy_plist(char *, package_t *);

struct memory_file
	*load_memory_file(const char *, const char *,
			  const char *, const char *, mode_t);
struct memory_file
	*make_memory_file(const char *, void *, size_t,
			  const char *, const char *, mode_t);
void	free_memory_file(struct memory_file *);

int	pkg_perform(const char *);
int	pkg_build(const char *, const char *, const char *, package_t *plist);

#endif				/* _INST_CREATE_H_INCLUDE */
