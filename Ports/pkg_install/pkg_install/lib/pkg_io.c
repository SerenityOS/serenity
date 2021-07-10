/*	$NetBSD: pkg_io.c,v 1.13 2017/07/16 16:51:48 joerg Exp $	*/
/*-
 * Copyright (c) 2008, 2009 Joerg Sonnenberger <joerg@NetBSD.org>.
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

#if HAVE_CONFIG_H
#include "config.h"
#endif
#include <nbcompat.h>
#if HAVE_SYS_CDEFS_H
#include <sys/cdefs.h>
#endif

__RCSID("$NetBSD: pkg_io.c,v 1.13 2017/07/16 16:51:48 joerg Exp $");

#include <archive.h>
#include <archive_entry.h>
#if HAVE_ERR_H
#include <err.h>
#endif
#if HAVE_ERRNO_H
#include <errno.h>
#endif
#include <stdlib.h>

#ifdef BOOTSTRAP
#undef IS_URL
#define IS_URL(x) 0
#else
#include <fetch.h>
#endif

#include "lib.h"

struct pkg_path {
	TAILQ_ENTRY(pkg_path) pl_link;
	char *pl_path;
};

static char *orig_cwd, *last_toplevel;
static TAILQ_HEAD(, pkg_path) pkg_path = TAILQ_HEAD_INITIALIZER(pkg_path);

#ifndef BOOTSTRAP
struct fetch_archive {
	struct url *url;
	fetchIO *fetch;
	char buffer[32768];
	off_t size;
	int restart;
};

static int
fetch_archive_open(struct archive *a, void *client_data)
{
	struct fetch_archive *f = client_data;
	struct url_stat us;

	f->fetch = fetchXGet(f->url, &us, fetch_flags);
	if (f->fetch == NULL)
		return ENOENT;
	f->size = us.size;
	f->restart = 1;
	f->url->offset = 0;
	return 0;
}

static ssize_t
fetch_archive_read(struct archive *a, void *client_data,
    const void **buffer)
{
	struct fetch_archive *f = client_data;
	struct url_stat us;
	ssize_t rv;	

	*buffer = f->buffer;
	rv = fetchIO_read(f->fetch, f->buffer, sizeof(f->buffer));
	if (rv > 0) {
		f->url->offset += rv;
		return rv;
	}
	if (f->restart == 0)
		return rv;
	if (rv == 0) {
		if (f->size == -1)
			return 0;
		if (f->url->offset == f->size)
			return 0;
	}
	f->restart = 0;
	if (1) {
		char *url = fetchStringifyURL(f->url);
		fprintf(stderr, "Trying to reconnect %s\n", url);
		free(url);
	}
	fetchIO_close(f->fetch);
	f->fetch = fetchXGet(f->url, &us, fetch_flags);
	if (f->fetch == NULL)
		return -1;
	if (us.size != f->size)
		return -1;
	rv = fetchIO_read(f->fetch, f->buffer, sizeof(f->buffer));
	if (rv > 0)
		f->url->offset += rv;
	return rv;
}

static int
fetch_archive_close(struct archive *a, void *client_data)
{
	struct fetch_archive *f = client_data;

	if (f->fetch != NULL)
		fetchIO_close(f->fetch);
	fetchFreeURL(f->url);
	free(f);
	return 0;
}

static struct archive *
open_archive_by_url(struct url *url, char **archive_name)
{
	struct fetch_archive *f;
	struct archive *a;

	f = xmalloc(sizeof(*f));
	f->url = fetchCopyURL(url);

	*archive_name = fetchStringifyURL(url);

	a = prepare_archive();
	if (archive_read_open(a, f, fetch_archive_open, fetch_archive_read,
	    fetch_archive_close)) {
		free(*archive_name);
		*archive_name = NULL;
		archive_read_free(a);
		return NULL;
	}

	return a;
}
#endif /* !BOOTSTRAP */

struct archive *
prepare_archive(void)
{
	struct archive *a = archive_read_new();
	if (a == NULL)
		errx(EXIT_FAILURE, "memory allocation failed");
	archive_read_support_filter_gzip(a);
	archive_read_support_filter_bzip2(a);
	archive_read_support_filter_xz(a);
	archive_read_support_format_ar(a);
	archive_read_support_format_tar(a);
	archive_read_set_options(a, "hdrcharset=BINARY");
	return a;
}

struct archive *
open_archive(const char *url, char **archive_name)
{
	struct url *u;
	struct archive *a;

	*archive_name = NULL;

	if (!IS_URL(url)) {
		a = prepare_archive();
		if (archive_read_open_filename(a, url, 1024)) {
			archive_read_close(a);
			return NULL;
		}
		*archive_name = xstrdup(url);
		return a;
	}

#ifdef BOOTSTRAP
	return NULL;
#else
	if ((u = fetchParseURL(url)) == NULL)
		return NULL;

	a = open_archive_by_url(u, archive_name);

	fetchFreeURL(u);
	return a;
#endif
}

#ifndef BOOTSTRAP
static int
strip_suffix(char *filename)
{
	size_t len;

	len = strlen(filename);
	if (len <= 4)
		return 0;
	if (strcmp(filename + len - 4, ".tgz") == 0 ||
	    strcmp(filename + len - 4, ".tbz") == 0) {
		filename[len - 4] = '\0';
		return 1;
	} else
		return 0;
}

static int
find_best_package_int(struct url *url, const char *pattern,
    struct url **best_url)
{
	char *cur_match, *url_pattern, *best_match = NULL;
	struct url_list ue;
	size_t i;

	if (*best_url) {
		if ((best_match = fetchUnquoteFilename(*best_url)) == NULL)
			return -1;
	} else
		best_match = NULL; 

	if (best_match && strip_suffix(best_match) == 0) {
		free(best_match);
		return -1;
	}

	for (i = 0; pattern[i] != '\0'; ++i) {
		if (!isalnum((unsigned char)(pattern[i])) &&
		    (pattern[i]) != '-')
			break;
	}
	url_pattern = xasprintf("%*.*s*", (int)i, (int)i, pattern);

	fetchInitURLList(&ue);
	if (fetchList(&ue, url, url_pattern, fetch_flags)) {
		char *base_url;
		base_url = fetchStringifyURL(url);
		warnx("Can't process %s/%s: %s", base_url, url_pattern,
		    fetchLastErrString);
		free(base_url);
		free(url_pattern);
		fetchFreeURLList(&ue);
		return -1;
	}
	free(url_pattern);

	for (i = 0; i < ue.length; ++i) {
		cur_match = fetchUnquoteFilename(ue.urls + i);

		if (cur_match == NULL) {
			free(best_match);
			fetchFreeURLList(&ue);
			return -1;
		}
		if (strip_suffix(cur_match) == 0) {
			free(cur_match);
			continue;	
		}
		if (pkg_order(pattern, cur_match, best_match) == 1) {
			if (*best_url)
				fetchFreeURL(*best_url);
			*best_url = fetchCopyURL(ue.urls + i);
			free(best_match);
			best_match = cur_match;
			cur_match = NULL;
			if (*best_url == NULL) {
				free(best_match);
				return -1;
			}
		}
		free(cur_match);
	}
	free(best_match);
	fetchFreeURLList(&ue);
	return 0;
}

void
process_pkg_path(void)
{
	char cwd[PATH_MAX];
	int relative_path;
	struct pkg_path *pl;
	const char *start, *next;
	size_t len;

	if (getcwd(cwd, sizeof(cwd)) == NULL)
		errx(EXIT_FAILURE, "getcwd failed");

	orig_cwd = xstrdup(cwd);

	if (config_pkg_path == NULL)
		return;

	for (start = config_pkg_path; *start; start = next) {
		len = strcspn(start, ";");
		if (*(next = start + len) != '\0')
			++next;

		relative_path = !IS_FULLPATH(start) && !IS_URL(start);
		pl = xmalloc(sizeof(*pl));
		pl->pl_path = xasprintf("%s%s%*.*s",
		    relative_path ? cwd : "", len && relative_path ? "/" : "",
		    (int)len, (int)len, start);
		TAILQ_INSERT_TAIL(&pkg_path, pl, pl_link);
	}
}

struct url *
find_best_package(const char *toplevel, const char *pattern, int do_path)
{
	struct url *url, *best_match = NULL;
	struct pkg_path *pl;

	if (toplevel) {
		url = fetchParseURL(last_toplevel);
		if (url != NULL) {
			find_best_package_int(url, pattern, &best_match);
			/* XXX Check return value and complain */
			fetchFreeURL(url);
		}
	}
	if (!do_path)
		return best_match;

	TAILQ_FOREACH(pl, &pkg_path, pl_link) {
		url = fetchParseURL(pl->pl_path);
		if (url != NULL) {
			find_best_package_int(url, pattern, &best_match);
			/* XXX Check return value and complain */
			fetchFreeURL(url);
		}
	}

	return best_match;
}
#endif /* !BOOTSTRAP */

struct archive *
find_archive(const char *fname, int top_level, char **archive_name)
{
	struct archive *a;
	struct url *best_match;
	char *full_fname, *last_slash;
	int search_path;

	search_path = 0;
	if (IS_FULLPATH(fname) || IS_URL(fname)) {
		full_fname = xstrdup(fname);
	} else {
		if (strchr(fname, '/') == NULL)
			search_path = 1;
		full_fname = xasprintf("%s/%s", orig_cwd, fname);
	}

	last_slash = strrchr(full_fname, '/');
	if (top_level) {
		free(last_toplevel);
		*last_slash = '\0';
		last_toplevel = xstrdup(full_fname);
		*last_slash = '/';
	}

	a = open_archive(full_fname, archive_name);
	if (a != NULL) {
		free(full_fname);
		return a;
	}
#ifndef BOOTSTRAP
	fname = last_slash + 1;
	*last_slash = '\0';

	best_match = find_best_package(full_fname, fname, 0);

	if (search_path && best_match == NULL)
		best_match = find_best_package(last_toplevel, fname, 1);

	free(full_fname);

	if (best_match == NULL)
		return NULL;
	a = open_archive_by_url(best_match, archive_name);
	fetchFreeURL(best_match);
#endif /* !BOOTSTRAP */
	return a;
}
