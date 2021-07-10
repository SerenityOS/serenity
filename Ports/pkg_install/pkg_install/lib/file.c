/*	$NetBSD: file.c,v 1.31 2015/10/15 13:31:27 sevan Exp $	*/

#if HAVE_CONFIG_H
#include "config.h"
#endif
#include <nbcompat.h>
#if HAVE_SYS_CDEFS_H
#include <sys/cdefs.h>
#endif
#if HAVE_SYS_QUEUE_H
#include <sys/queue.h>
#endif
__RCSID("$NetBSD: file.c,v 1.31 2015/10/15 13:31:27 sevan Exp $");

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
 * Miscellaneous file access utilities.
 *
 */

#include "lib.h"

#if HAVE_SYS_WAIT_H
#include <sys/wait.h>
#endif

#if HAVE_ASSERT_H
#include <assert.h>
#endif
#if HAVE_ERR_H
#include <err.h>
#endif
#if HAVE_GLOB_H
#include <glob.h>
#endif
#if HAVE_PWD_H
#include <pwd.h>
#endif
#if HAVE_TIME_H
#include <time.h>
#endif
#if HAVE_FCNTL_H
#include <fcntl.h>
#endif


/*
 * Quick check to see if a file (or dir ...) exists
 */
Boolean
fexists(const char *fname)
{
	struct stat dummy;
	if (!lstat(fname, &dummy))
		return TRUE;
	return FALSE;
}

/*
 * Quick check to see if something is a directory
 */
Boolean
isdir(const char *fname)
{
	struct stat sb;

	if (lstat(fname, &sb) != FAIL && S_ISDIR(sb.st_mode))
		return TRUE;
	else
		return FALSE;
}

/*
 * Check if something is a link to a directory
 */
Boolean
islinktodir(const char *fname)
{
	struct stat sb;

	if (lstat(fname, &sb) != FAIL && S_ISLNK(sb.st_mode)) {
		if (stat(fname, &sb) != FAIL && S_ISDIR(sb.st_mode))
			return TRUE;	/* link to dir! */
		else
			return FALSE;	/* link to non-dir */
	} else
		return FALSE;	/* non-link */
}

/*
 * Check if something is a link that points to nonexistant target.
 */
Boolean
isbrokenlink(const char *fname)
{
	struct stat sb;

	if (lstat(fname, &sb) != FAIL && S_ISLNK(sb.st_mode)) {
		if (stat(fname, &sb) != FAIL)
			return FALSE;	/* link target exists! */
		else
			return TRUE;	/* link target missing*/
	} else
		return FALSE;	/* non-link */
}

/*
 * Check to see if file is a dir, and is empty
 */
Boolean
isemptydir(const char *fname)
{
	if (isdir(fname) || islinktodir(fname)) {
		DIR    *dirp;
		struct dirent *dp;

		dirp = opendir(fname);
		if (!dirp)
			return FALSE;	/* no perms, leave it alone */
		for (dp = readdir(dirp); dp != NULL; dp = readdir(dirp)) {
			if (strcmp(dp->d_name, ".") && strcmp(dp->d_name, "..")) {
				closedir(dirp);
				return FALSE;
			}
		}
		(void) closedir(dirp);
		return TRUE;
	}
	return FALSE;
}

/*
 * Check if something is a regular file
 */
Boolean
isfile(const char *fname)
{
	struct stat sb;
	if (stat(fname, &sb) != FAIL && S_ISREG(sb.st_mode))
		return TRUE;
	return FALSE;
}

/*
 * Check to see if file is a file and is empty. If nonexistent or not
 * a file, say "it's empty", otherwise return TRUE if zero sized.
 */
Boolean
isemptyfile(const char *fname)
{
	struct stat sb;
	if (stat(fname, &sb) != FAIL && S_ISREG(sb.st_mode)) {
		if (sb.st_size != 0)
			return FALSE;
	}
	return TRUE;
}

/* This struct defines the leading part of a valid URL name */
typedef struct url_t {
	const char *u_s;	/* the leading part of the URL */
	int     u_len;		/* its length */
}       url_t;

/* A table of valid leading strings for URLs */
static const url_t urls[] = {
#define	STR_AND_SIZE(str)	{ str, sizeof(str) - 1 }
	STR_AND_SIZE("file://"),
	STR_AND_SIZE("ftp://"),
	STR_AND_SIZE("http://"),
	STR_AND_SIZE("https://"),
#undef STR_AND_SIZE
	{NULL, 0}
};

/*
 * Returns length of leading part of any URL from urls table, or -1
 */
int
URLlength(const char *fname)
{
	const url_t *up;
	int     i;

	if (fname != (char *) NULL) {
		for (i = 0; isspace((unsigned char) *fname); i++) {
			fname++;
		}
		for (up = urls; up->u_s; up++) {
			if (strncmp(fname, up->u_s, up->u_len) == 0) {
				return i + up->u_len;    /* ... + sizeof(up->u_s);  - HF */
			}
		}
	}
	return -1;
}

/*
 * Takes a filename and package name, returning (in "try") the canonical
 * "preserve" name for it.
 */
Boolean
make_preserve_name(char *try, size_t max, const char *name, const char *file)
{
	size_t len, i;

	if ((len = strlen(file)) == 0)
		return FALSE;
	i = len - 1;
	strncpy(try, file, max);
	if (try[i] == '/')	/* Catch trailing slash early and save checking in the loop */
		--i;
	for (; i; i--) {
		if (try[i] == '/') {
			try[i + 1] = '.';
			strncpy(&try[i + 2], &file[i + 1], max - i - 2);
			break;
		}
	}
	if (!i) {
		try[0] = '.';
		strncpy(try + 1, file, max - 1);
	}
	/* I should probably be called rude names for these inline assignments */
	strncat(try, ".", max -= strlen(try));
	strncat(try, name, max -= strlen(name));
	strncat(try, ".", max--);
	strncat(try, "backup", max -= 6);
	return TRUE;
}

void
remove_files(const char *path, const char *pattern)
{
	char	fpath[MaxPathSize];
	glob_t	globbed;
	int	i;
	size_t  j;

	(void) snprintf(fpath, sizeof(fpath), "%s/%s", path, pattern);
	if ((i=glob(fpath, GLOB_NOSORT, NULL, &globbed)) != 0) {
		switch(i) {
		case GLOB_NOMATCH:
			warn("no files matching ``%s'' found", fpath);
			break;
		case GLOB_ABORTED:
			warn("globbing aborted");
			break;
		case GLOB_NOSPACE:
			warn("out-of-memory during globbing");
			break;
		default:
			warn("unknown error during globbing");
			break;
		}
		return;
	}

	/* deleting globbed files */
	for (j = 0; j < globbed.gl_pathc; j++)
		if (unlink(globbed.gl_pathv[j]) < 0)
			warn("can't delete ``%s''", globbed.gl_pathv[j]);

	return;
}

/*
 * Using fmt, replace all instances of:
 *
 * %F	With the parameter "name"
 * %D	With the parameter "dir"
 * %B	Return the directory part ("base") of %D/%F
 * %f	Return the filename part of %D/%F
 *
 * Check that no overflows can occur.
 */
int
format_cmd(char *buf, size_t size, const char *fmt, const char *dir, const char *name)
{
	size_t  remaining, quoted;
	char   *bufp, *tmp;
	char   *cp;

	for (bufp = buf, remaining = size; remaining > 1 && *fmt;) {
		if (*fmt != '%') {
			*bufp++ = *fmt++;
			--remaining;
			continue;
		}

		if (*++fmt != 'D' && name == NULL) {
			warnx("no last file available for '%s' command", buf);
			return -1;
		}
		switch (*fmt) {
		case 'F':
			quoted = shquote(name, bufp, remaining);
			if (quoted >= remaining) {
				warnx("overflow during quoting");
				return -1;
			}
			bufp += quoted;
			remaining -= quoted;
			break;

		case 'D':
			quoted = shquote(dir, bufp, remaining);
			if (quoted >= remaining) {
				warnx("overflow during quoting");
				return -1;
			}
			bufp += quoted;
			remaining -= quoted;
			break;

		case 'B':
			tmp = xasprintf("%s/%s", dir, name);
			cp = strrchr(tmp, '/');
			*cp = '\0';
			quoted = shquote(tmp, bufp, remaining);
			free(tmp);
			if (quoted >= remaining) {
				warnx("overflow during quoting");
				return -1;
			}
			bufp += quoted;
			remaining -= quoted;
			break;

		case 'f':
			tmp = xasprintf("%s/%s", dir, name);
			cp = strrchr(tmp, '/') + 1;
			quoted = shquote(cp, bufp, remaining);
			free(tmp);
			if (quoted >= remaining) {
				warnx("overflow during quoting");
				return -1;
			}
			bufp += quoted;
			remaining -= quoted;
			break;

		default:
			if (remaining == 1) {
				warnx("overflow during quoting");
				return -1;
			}
			*bufp++ = '%';
			*bufp++ = *fmt;
			remaining -= 2;
			break;
		}
		++fmt;
	}
	*bufp = '\0';
	return 0;
}
