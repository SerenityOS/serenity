/* $NetBSD: lib.h,v 1.72 2020/12/11 10:06:53 jperkin Exp $ */

/* from FreeBSD Id: lib.h,v 1.25 1997/10/08 07:48:03 charnier Exp */

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
 * Include and define various things wanted by the library routines.
 *
 */

#ifndef _INST_LIB_LIB_H_
#define _INST_LIB_LIB_H_

#if HAVE_CONFIG_H
#include "config.h"
#endif
#include <nbcompat.h>
#if HAVE_SYS_STAT_H
#include <sys/stat.h>
#endif
#if HAVE_SYS_FILE_H
#include <sys/file.h>
#endif
#if HAVE_SYS_QUEUE_H
#include <sys/queue.h>
#endif

#if HAVE_CTYPE_H
#include <ctype.h>
#endif
#if HAVE_DIRENT_H
#include <dirent.h>
#endif
#if HAVE_STDIO_H
#include <stdio.h>
#endif
#if HAVE_STDLIB_H
#include <stdlib.h>
#endif
#if HAVE_STDARG_H
#include <stdarg.h>
#endif
#if HAVE_STRING_H
#include <string.h>
#endif
#if HAVE_UNISTD_H
#include <unistd.h>
#endif

/* Macros */
#ifndef __UNCONST
#define __UNCONST(a)	((void *)(unsigned long)(const void *)(a))
#endif

#define SUCCESS	(0)
#define	FAIL	(-1)

#ifndef TRUE
#define TRUE	(1)
#endif

#ifndef FALSE
#define FALSE	(0)
#endif

#ifndef OPSYS_NAME
#define OPSYS_NAME "NetBSD"
#endif

#ifndef DEF_UMASK
#define DEF_UMASK 022
#endif

#ifndef	PATH_MAX
#  ifdef MAXPATHLEN
#    define PATH_MAX	MAXPATHLEN
#  else
#    define PATH_MAX	1024
#  endif
#endif

enum {
	MaxPathSize = PATH_MAX
};

/* The names of our "special" files */
#define CONTENTS_FNAME		"+CONTENTS"
#define COMMENT_FNAME		"+COMMENT"
#define DESC_FNAME		"+DESC"
#define INSTALL_FNAME		"+INSTALL"
#define DEINSTALL_FNAME		"+DEINSTALL"
#define REQUIRED_BY_FNAME	"+REQUIRED_BY"
#define REQUIRED_BY_FNAME_TMP	"+REQUIRED_BY.tmp"
#define DISPLAY_FNAME		"+DISPLAY"
#define MTREE_FNAME		"+MTREE_DIRS"
#define BUILD_VERSION_FNAME	"+BUILD_VERSION"
#define BUILD_INFO_FNAME	"+BUILD_INFO"
#define INSTALLED_INFO_FNAME	"+INSTALLED_INFO"
#define SIZE_PKG_FNAME		"+SIZE_PKG"
#define SIZE_ALL_FNAME		"+SIZE_ALL"
#define PRESERVE_FNAME		"+PRESERVE"

/* The names of special variables */
#define AUTOMATIC_VARNAME	"automatic"

/* Prefix for extended PLIST cmd */
#define CMD_CHAR		'@'	

/* The name of the "prefix" environment variable given to scripts */
#define PKG_PREFIX_VNAME	"PKG_PREFIX"

/* The name of the "destdir" environment variable given to scripts */
#define PKG_DESTDIR_VNAME	"PKG_DESTDIR"

/*
 * The name of the "metadatadir" environment variable given to scripts.
 * This variable holds the location of the +-files for this package.
 */
#define PKG_METADATA_DIR_VNAME	"PKG_METADATA_DIR"

/*
 * The name of the environment variable holding the location to the
 * reference-counts database directory.
 */
#define PKG_REFCOUNT_DBDIR_VNAME	"PKG_REFCOUNT_DBDIR"

#define	PKG_PATTERN_MAX	MaxPathSize	/* max length of pattern, including nul */
#define	PKG_SUFFIX_MAX	10	/* max length of suffix, including nul */

enum {
	ReadWrite,
	ReadOnly
};


/* Enumerated constants for plist entry types */
typedef enum pl_ent_t {
	PLIST_SHOW_ALL = -1,
	PLIST_FILE,		/*  0 */
	PLIST_CWD,		/*  1 */
	PLIST_CMD,		/*  2 */
	PLIST_CHMOD,		/*  3 */
	PLIST_CHOWN,		/*  4 */
	PLIST_CHGRP,		/*  5 */
	PLIST_COMMENT,		/*  6 */
	PLIST_IGNORE,		/*  7 */
	PLIST_NAME,		/*  8 */
	PLIST_UNEXEC,		/*  9 */
	PLIST_SRC,		/* 10 */
	PLIST_DISPLAY,		/* 11 */
	PLIST_PKGDEP,		/* 12 */
	PLIST_DIR_RM,		/* 13 */
	PLIST_OPTION,		/* 14 */
	PLIST_PKGCFL,		/* 15 */
	PLIST_BLDDEP,		/* 16 */
	PLIST_PKGDIR		/* 17 */
}       pl_ent_t;

/* Enumerated constants for build info */
typedef enum bi_ent_t {
	BI_OPSYS,		/*  0 */
	BI_OS_VERSION,		/*  1 */
	BI_MACHINE_ARCH,	/*  2 */
	BI_IGNORE_RECOMMENDED,	/*  3 */
	BI_USE_ABI_DEPENDS,	/*  4 */
	BI_LICENSE,		/*  5 */
	BI_PKGTOOLS_VERSION,	/*  6 */
	BI_ENUM_COUNT		/*  7 */
}	bi_ent_t;

/* Types */
typedef unsigned int Boolean;

/* This structure describes a packing list entry */
typedef struct plist_t {
	struct plist_t *prev;	/* previous entry */
	struct plist_t *next;	/* next entry */
	char   *name;		/* name of entry */
	Boolean marked;		/* whether entry has been marked */
	pl_ent_t type;		/* type of entry */
}       plist_t;

/* This structure describes a package's complete packing list */
typedef struct package_t {
	plist_t *head;		/* head of list */
	plist_t *tail;		/* tail of list */
}       package_t;

#define SYMLINK_HEADER	"Symlink:"
#define CHECKSUM_HEADER	"MD5:"

enum {
	ChecksumHeaderLen = 4,	/* strlen(CHECKSUM_HEADER) */
	SymlinkHeaderLen = 8,	/* strlen(SYMLINK_HEADER) */
	ChecksumLen = 16,
	LegibleChecksumLen = 33
};

/* List of files */
typedef struct _lfile_t {
        TAILQ_ENTRY(_lfile_t) lf_link;
        char *lf_name;
} lfile_t;
TAILQ_HEAD(_lfile_head_t, _lfile_t);
typedef struct _lfile_head_t lfile_head_t;
#define	LFILE_ADD(lfhead,lfp,str) do {		\
	lfp = xmalloc(sizeof(lfile_t));		\
	lfp->lf_name = str;			\
	TAILQ_INSERT_TAIL(lfhead,lfp,lf_link);	\
	} while(0)

/* List of packages */
typedef struct _lpkg_t {
	TAILQ_ENTRY(_lpkg_t) lp_link;
	char   *lp_name;
}       lpkg_t;
TAILQ_HEAD(_lpkg_head_t, _lpkg_t);
typedef struct _lpkg_head_t lpkg_head_t;

/*
 * To improve performance when handling lists containing a large number of
 * packages, it can be beneficial to use hashed lookups to avoid excessive
 * strcmp() calls when searching for existing entries.
 *
 * The simple hashing function below uses the first 3 characters of either a
 * pattern match or package name (as they are guaranteed to exist).
 *
 * Based on pkgsrc package names across the tree, this can still result in
 * somewhat uneven distribution due to high numbers of packages beginning with
 * "p5-", "php", "py-" etc, and so there are diminishing returns when trying to
 * use a hash size larger than around 16 or so.
 */
#define PKG_HASH_SIZE		16
#define PKG_HASH_ENTRY(x)	(((unsigned char)(x)[0] \
				+ (unsigned char)(x)[1] * 257 \
				+ (unsigned char)(x)[2] * 65537) \
				& (PKG_HASH_SIZE - 1))

struct pkg_vulnerabilities {
	size_t	entries;
	char	**vulnerability;
	char	**classification;
	char	**advisory;
};

/* If URLlength()>0, then there is a ftp:// or http:// in the string,
 * and this must be an URL. Hide this behind a more obvious name. */
#define IS_URL(str)	(URLlength(str) > 0)

#define IS_STDIN(str)	((str) != NULL && !strcmp((str), "-"))
#define IS_FULLPATH(str)	((str) != NULL && (str)[0] == '/')

/* Conflict handling (conflicts.c) */
int	some_installed_package_conflicts_with(const char *, const char *, char **, char **);


/* Prototypes */
/* Misc */
void    show_version(void) __attribute__ ((noreturn));
int	fexec(const char *, ...);
int	fexec_skipempty(const char *, ...);
int	fcexec(const char *, const char *, ...);
int	pfcexec(const char *, const char *, const char **);

/* variables file handling */

char   *var_get(const char *, const char *);
char   *var_get_memory(const char *, const char *);
int	var_set(const char *, const char *, const char *);
int     var_copy_list(const char *, const char **);

/* automatically installed as dependency */

Boolean	is_automatic_installed(const char *);
int	mark_as_automatic_installed(const char *, int);

/* String */
const char *basename_of(const char *);
const char *dirname_of(const char *);
const char *suffix_of(const char *);
int     pkg_match(const char *, const char *);
int	pkg_order(const char *, const char *, const char *);
int     ispkgpattern(const char *);
int	quick_pkg_match(const char *, const char *);

/* Iterator functions */
int	iterate_pkg_generic_src(int (*)(const char *, void *), void *,
				const char *(*)(void *),void *);
int	iterate_local_pkg_dir(const char *, int, int, int (*)(const char *, void *),
			      void *);
int	iterate_pkg_db(int (*)(const char *, void *), void *);

int	add_installed_pkgs_by_basename(const char *, lpkg_head_t *);
int	add_installed_pkgs_by_pattern(const char *, lpkg_head_t *);
char	*find_best_matching_installed_pkg(const char *, int);
char	*find_best_matching_file(const char *, const char *, int, int);
int	match_installed_pkgs(const char *, int (*)(const char *, void *), void *);
int	match_local_files(const char *, int, int, const char *, int (*cb)(const char *, void *), void *);

/* File */
Boolean fexists(const char *);
Boolean isdir(const char *);
Boolean islinktodir(const char *);
Boolean isemptydir(const char *);
Boolean isemptyfile(const char *);
Boolean isfile(const char *);
Boolean isbrokenlink(const char *);
Boolean isempty(const char *);
int     URLlength(const char *);
Boolean make_preserve_name(char *, size_t, const char *, const char *);
void    remove_files(const char *, const char *);
int     format_cmd(char *, size_t, const char *, const char *, const char *);

int	recursive_remove(const char *, int);

void	add_pkgdir(const char *, const char *, const char *);
void	delete_pkgdir(const char *, const char *, const char *);
int	has_pkgdir(const char *);

/* pkg_io.c: Local and remote archive handling */
struct archive;
struct archive_entry;

struct archive *prepare_archive(void);
struct archive *open_archive(const char *, char **);
struct archive *find_archive(const char *, int, char **);
void	process_pkg_path(void);
struct url *find_best_package(const char *, const char *, int);

/* Packing list */
plist_t *new_plist_entry(void);
plist_t *last_plist(package_t *);
plist_t *find_plist(package_t *, pl_ent_t);
char   *find_plist_option(package_t *, const char *);
void    plist_delete(package_t *, Boolean, pl_ent_t, char *);
void    free_plist(package_t *);
void    mark_plist(package_t *);
void    csum_plist_entry(char *, plist_t *);
void    add_plist(package_t *, pl_ent_t, const char *);
void    add_plist_top(package_t *, pl_ent_t, const char *);
void    delete_plist(package_t *, Boolean, pl_ent_t, char *);
void    write_plist(package_t *, FILE *, char *);
void	stringify_plist(package_t *, char **, size_t *, const char *);
void	parse_plist(package_t *, const char *);
void    read_plist(package_t *, FILE *);
void    append_plist(package_t *, FILE *);
int     delete_package(Boolean, package_t *, Boolean, const char *);

/* Package Database */
int     pkgdb_open(int);
void    pkgdb_close(void);
int     pkgdb_store(const char *, const char *);
char   *pkgdb_retrieve(const char *);
int	pkgdb_dump(void);
int     pkgdb_remove(const char *);
int	pkgdb_remove_pkg(const char *);
char   *pkgdb_refcount_dir(void);
char   *pkgdb_get_database(void);
const char   *pkgdb_get_dir(void);
/*
 * Priorities:
 * 0 builtin default
 * 1 config file
 * 2 environment
 * 3 command line
 */
void	pkgdb_set_dir(const char *, int);
char   *pkgdb_pkg_dir(const char *);
char   *pkgdb_pkg_file(const char *, const char *);

/* List of packages functions */
lpkg_t *alloc_lpkg(const char *);
lpkg_t *find_on_queue(lpkg_head_t *, const char *);
void    free_lpkg(lpkg_t *);

/* Read pkg_vulnerabilities from file */
struct pkg_vulnerabilities *read_pkg_vulnerabilities_file(const char *, int, int);
/* Read pkg_vulnerabilities from memory */
struct pkg_vulnerabilities *read_pkg_vulnerabilities_memory(void *, size_t, int);
void free_pkg_vulnerabilities(struct pkg_vulnerabilities *);
int audit_package(struct pkg_vulnerabilities *, const char *, const char *,
    int, int);

/* Parse configuration file */
void pkg_install_config(void);
/* Print configuration variable */
void pkg_install_show_variable(const char *);

/* Package signature creation and validation */
int pkg_verify_signature(const char *, struct archive **, struct archive_entry **, char **);
int pkg_full_signature_check(const char *, struct archive **);
#ifdef HAVE_SSL
void pkg_sign_x509(const char *, const char *, const char *, const char *);
#endif

void pkg_sign_gpg(const char *, const char *);

#ifdef HAVE_SSL
/* PKCS7 signing/verification */
int easy_pkcs7_verify(const char *, size_t, const char *, size_t,
    const char *, int);
int easy_pkcs7_sign(const char *, size_t, char **, size_t *, const char *,
    const char *);
#endif

int gpg_verify(const char *, size_t, const char *, const char *, size_t);
int detached_gpg_sign(const char *, size_t, char **, size_t *, const char *,
    const char *);

/* License handling */
int add_licenses(const char *);
int acceptable_license(const char *);
int acceptable_pkg_license(const char *);
void load_license_lists(void);

/* Helper functions for memory allocation */
char *xstrdup(const char *);
void *xrealloc(void *, size_t);
void *xcalloc(size_t, size_t);
void *xmalloc(size_t);
#if defined(__GNUC__) && __GNUC__ >= 2
char	*xasprintf(const char *, ...)
			   __attribute__((__format__(__printf__, 1, 2)));
#else
char	*xasprintf(const char *, ...);
#endif

/* Externs */
extern Boolean Verbose;
extern Boolean Fake;
extern Boolean Force;
extern const char *cert_chain_file;
extern const char *certs_packages;
extern const char *certs_pkg_vulnerabilities;
extern const char *check_eol;
extern const char *check_os_version;
extern const char *check_vulnerabilities;
extern const char *config_file;
extern const char *config_pkg_dbdir;
extern const char *config_pkg_path;
extern const char *config_pkg_refcount_dbdir;
extern const char *do_license_check;
extern const char *verified_installation;
extern const char *gpg_cmd;
extern const char *gpg_keyring_pkgvuln;
extern const char *gpg_keyring_sign;
extern const char *gpg_keyring_verify;
extern const char *gpg_sign_as;
extern char fetch_flags[];

extern const char *pkg_vulnerabilities_dir;
extern const char *pkg_vulnerabilities_file;
extern const char *pkg_vulnerabilities_url;
extern const char *ignore_advisories;
extern const char tnf_vulnerability_base[];

extern const char *acceptable_licenses;
extern const char *default_acceptable_licenses;

#endif				/* _INST_LIB_LIB_H_ */
