/* $NetBSD: add.h,v 1.19 2010/09/14 22:26:18 gdt Exp $ */

/* from FreeBSD Id: add.h,v 1.8 1997/02/22 16:09:15 peter Exp  */

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
 * Include and define various things wanted by the add command.
 *
 */

#ifndef _INST_ADD_H_INCLUDE
#define _INST_ADD_H_INCLUDE

extern char *Destdir;
extern char *OverrideMachine;
extern char *Prefix;
extern char *View;
extern char *Viewbase;
extern Boolean NoView;
extern Boolean NoInstall;
extern Boolean NoRecord;
extern Boolean Force;
extern Boolean Automatic;
extern int LicenseCheck;
extern int Replace;
extern int ReplaceSame;

extern Boolean ForceDepends;
extern Boolean ForceDepending;

int     make_hierarchy(char *);
void    apply_perms(char *, char **, int);

int     pkg_perform(lpkg_head_t *);

#endif				/* _INST_ADD_H_INCLUDE */
