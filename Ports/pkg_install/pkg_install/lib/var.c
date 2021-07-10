/*	$NetBSD: var.c,v 1.10 2013/09/12 07:28:28 wiz Exp $	*/

/*-
 * Copyright (c) 2005, 2008 The NetBSD Foundation, Inc.
 * All rights reserved.
 *
 * This code is derived from software contributed to The NetBSD Foundation
 * by Dieter Baron, Thomas Klausner, Johnny Lam, and Joerg Sonnenberger.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of The NetBSD Foundation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
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

#if HAVE_CONFIG_H
#include "config.h"
#endif
#include <nbcompat.h>
#if HAVE_SYS_CDEFS_H
#include <sys/cdefs.h>
#endif
__RCSID("$NetBSD: var.c,v 1.10 2013/09/12 07:28:28 wiz Exp $");

#if HAVE_SYS_STAT_H
#include <sys/stat.h>
#endif
#if HAVE_ERR_H
#include <err.h>
#endif
#if HAVE_ERRNO_H
#include <errno.h>
#endif
#if HAVE_STDIO_H
#include <stdio.h>
#endif

#include "lib.h"

static const char *var_cmp(const char *, size_t, const char *, size_t);
static void var_print(FILE *, const char *, const char *);

/*
 * Copy the specified variables from the file fname to stdout.
 */
int
var_copy_list(const char *buf, const char **variables)
{
	const char *eol, *next;
	size_t len;
	int i;

	for (; *buf; buf = next) {
		if ((eol = strchr(buf, '\n')) != NULL) {
			next = eol + 1;
			len = eol - buf;
		} else {
			len = strlen(buf);
			next = buf + len;
		}

		for (i=0; variables[i]; i++) {
			if (var_cmp(buf, len, variables[i],
				       strlen(variables[i])) != NULL) {
				printf("%.*s\n", (int)len, buf);
				break;
			}
		}
	}
	return 0;
}

/*
 * Print the value of variable from the file fname to stdout.
 */
char *
var_get(const char *fname, const char *variable)
{
	FILE   *fp;
	char   *line;
	size_t  len;
	size_t  varlen;
	char   *value;
	size_t  valuelen;
	size_t  thislen;
	const char *p;

	varlen = strlen(variable);
	if (varlen == 0)
		return NULL;

	fp = fopen(fname, "r");
	if (!fp) {
		if (errno != ENOENT)
			warn("var_get: can't open '%s' for reading", fname);
		return NULL;
	}

	value = NULL;
	valuelen = 0;
	
	while ((line = fgetln(fp, &len)) != (char *) NULL) {
		if (line[len - 1] == '\n')
			--len;
		if ((p=var_cmp(line, len, variable, varlen)) == NULL)
			continue;

		thislen = line+len - p;
		if (value) {
			value = xrealloc(value, valuelen+thislen+2);
			value[valuelen++] = '\n';
		}
		else {
			value = xmalloc(thislen+1);
		}
		sprintf(value+valuelen, "%.*s", (int)thislen, p);
		valuelen += thislen;
	}
	(void) fclose(fp);
	return value;
}

/*
 * Print the value of variable from the memory buffer to stdout.
 */
char *
var_get_memory(const char *buf, const char *variable)
{
	const char *eol, *next, *data;
	size_t len, varlen, thislen, valuelen;
	char *value;

	varlen = strlen(variable);
	if (varlen == 0)
		return NULL;

	value = NULL;
	valuelen = 0;

	for (; buf && *buf; buf = next) {
		if ((eol = strchr(buf, '\n')) != NULL) {
			next = eol + 1;
			len = eol - buf;
		} else {
			next = eol;
			len = strlen(buf);
		}
		if ((data = var_cmp(buf, len, variable, varlen)) == NULL)
			continue;

		thislen = buf + len - data;
		if (value) {
			value = xrealloc(value, valuelen+thislen+2);
			value[valuelen++] = '\n';
		}
		else {
			value = xmalloc(thislen+1);
		}
		sprintf(value + valuelen, "%.*s", (int)thislen, data);
		valuelen += thislen;
	}
	return value;
}

/*
 * Add given variable with given value to file, overwriting any
 * previous occurrence.
 */
int
var_set(const char *fname, const char *variable, const char *value)
{
	FILE   *fp;
	FILE   *fout;
	char   *tmpname;
	int     fd;
	char   *line;
	size_t  len;
	size_t  varlen;
	Boolean done;
	struct stat st;

	varlen = strlen(variable);
	if (varlen == 0)
		return 0;

	fp = fopen(fname, "r");
	if (fp == NULL) {
		if (errno != ENOENT) {
			warn("var_set: can't open '%s' for reading", fname);
			return -1;
		}
		if (value == NULL)
			return 0; /* Nothing to do */
	}

	tmpname = xasprintf("%s.XXXXXX", fname);
	if ((fd = mkstemp(tmpname)) < 0) {
		free(tmpname);
		if (fp != NULL)
			fclose(fp);
		warn("var_set: can't open temp file for '%s' for writing",
		      fname);
		return -1;
	}
	if (chmod(tmpname, 0644) < 0) {
		close(fd);
		if (fp != NULL)
			fclose(fp);
		free(tmpname);
		warn("var_set: can't set permissions for temp file for '%s'",
		      fname);
		return -1;
	}
	if ((fout=fdopen(fd, "w")) == NULL) {
		close(fd);
		remove(tmpname);
		free(tmpname);
		if (fp != NULL)
			fclose(fp);
		warn("var_set: can't open temp file for '%s' for writing",
		      fname);
		return -1;
	}

	done = FALSE;

	if (fp) {
		while ((line = fgetln(fp, &len)) != (char *) NULL) {
			if (var_cmp(line, len, variable, varlen) == NULL)
				fprintf(fout, "%.*s", (int)len, line);
			else {
				if (!done && value) {
					var_print(fout, variable, value);
					done = TRUE;
				}
			}
		}
		(void) fclose(fp);
	}

	if (!done && value)
		var_print(fout, variable, value);

	if (fclose(fout) < 0) {
		free(tmpname);
		warn("var_set: write error for '%s'", fname);
		return -1;
	}

	if (stat(tmpname, &st) < 0) {
		free(tmpname);
		warn("var_set: cannot stat tempfile for '%s'", fname);
		return -1;
	}

	if (st.st_size == 0) {
		if (remove(tmpname) < 0) {
			free(tmpname);
			warn("var_set: cannot remove tempfile for '%s'",
			     fname);
			return -1;
		}
		free(tmpname);
		if (remove(fname) < 0) {
			warn("var_set: cannot remove '%s'", fname);
			return -1;
		}
		return 0;
	}

	if (rename(tmpname, fname) < 0) {
		free(tmpname);
		warn("var_set: cannot move tempfile to '%s'", fname);
		return -1;
	}
	free(tmpname);
	return 0;
}

/*
 * Check if line contains variable var, return pointer to its value or NULL.
 */
static const char *
var_cmp(const char *line, size_t linelen, const char *var, size_t varlen)
{
	/*
	 * We expect lines to look like one of the following
	 * forms:
	 *      VAR=value
	 *      VAR= value
	 * We print out the value of VAR, or nothing if it
	 * doesn't exist.
	 */
	if (linelen < varlen+1)
		return NULL;
	if (strncmp(var, line, varlen) != 0)
		return NULL;
	
	line += varlen;
	if (*line != '=')
		return NULL;

	++line;
	linelen -= varlen+1;
	if (linelen > 0 && *line == ' ')
		++line;
	return line;
}

/*
 * Print given variable with value to file f.
 */
static void
var_print(FILE *f, const char *variable, const char *value)
{
	const char *p;

	while ((p=strchr(value, '\n')) != NULL) {
		if (p != value)
			fprintf(f, "%s=%.*s\n", variable, (int)(p-value), value);
		value = p+1;
	}

	if (*value)
		fprintf(f, "%s=%s\n", variable, value);
}
