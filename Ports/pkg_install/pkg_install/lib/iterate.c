/*	$NetBSD: iterate.c,v 1.10 2020/12/02 12:10:50 wiz Exp $	*/

/*-
 * Copyright (c) 2007 Joerg Sonnenberger <joerg@NetBSD.org>.
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

#if HAVE_ERR_H
#include <err.h>
#endif
#if HAVE_ERRNO_H
#include <errno.h>
#endif

#include "lib.h"

/*
 * We define a couple of different caches to hold frequently accessed data.
 *
 * Firstly, we cache the results of readdir() on the package database directory
 * when using iterate_pkg_db_cached().  This helps a lot during recursive calls
 * and avoids exponential system calls, but is not suitable for situations
 * where the database directory may be updated, for example during installs.
 * In those situations the regular iterate_pkg_db() must be used.
 *
 * Secondly, we have a cache for matches of pattern lookups, avoiding expensive
 * pkg_match() calls each time.
 */
struct pkg_db_list {
	char *pkgname;
	SLIST_ENTRY(pkg_db_list) entries;
};
SLIST_HEAD(pkg_db_list_head, pkg_db_list);

struct pkg_match_list {
	char *pattern;
	char *pkgname;
	SLIST_ENTRY(pkg_match_list) entries;
};
SLIST_HEAD(pkg_match_list_head, pkg_match_list);

static struct pkg_db_list_head pkg_list_cache;
static struct pkg_match_list_head pkg_match_cache[PKG_HASH_SIZE];

/*
 * Generic iteration function:
 * - get new entries from srciter, stop on NULL
 * - call matchiter for those entries, stop on non-null return value.
 */
int
iterate_pkg_generic_src(int (*matchiter)(const char *, void *),
    void *match_cookie, const char *(*srciter)(void *), void *src_cookie)
{
	int retval;
	const char *entry;

	retval = 0;

	while ((entry = (*srciter)(src_cookie)) != NULL) {
		if ((retval = (*matchiter)(entry, match_cookie)) != 0)
			break;
	}

	return retval;
}

struct pkg_dir_iter_arg {
	DIR *dirp;
	int filter_suffix;
	int allow_nonfiles;
};

static const char *
pkg_dir_iter(void *cookie)
{
	struct pkg_dir_iter_arg *arg = cookie;
	struct dirent *dp;
	size_t len;

	while ((dp = readdir(arg->dirp)) != NULL) {
#if defined(DT_UNKNOWN) && defined(DT_DIR)
		if (arg->allow_nonfiles == 0 &&
		    dp->d_type != DT_UNKNOWN && dp->d_type != DT_REG)
			continue;
#endif
		len = strlen(dp->d_name);
		/* .tbz or .tgz suffix length + some prefix*/
		if (len < 5)
			continue;
		if (arg->filter_suffix == 0 ||
		    memcmp(dp->d_name + len - 4, ".tgz", 4) == 0 ||
		    memcmp(dp->d_name + len - 4, ".tbz", 4) == 0)
			return dp->d_name;
	}
	return NULL;
}

/*
 * Call matchiter for every package in the directory.
 */
int
iterate_local_pkg_dir(const char *dir, int filter_suffix, int allow_nonfiles,
    int (*matchiter)(const char *, void *), void *cookie)
{
	struct pkg_dir_iter_arg arg;
	int retval;

	if ((arg.dirp = opendir(dir)) == NULL)
		return -1;

	arg.filter_suffix = filter_suffix;
	arg.allow_nonfiles = allow_nonfiles;
	retval = iterate_pkg_generic_src(matchiter, cookie, pkg_dir_iter, &arg);

	if (closedir(arg.dirp) == -1)
		return -1;
	return retval;
}

static const char *
pkg_db_iter(void *cookie)
{
	DIR *dirp = cookie;
	struct dirent *dp;

	while ((dp = readdir(dirp)) != NULL) {
		if (strcmp(dp->d_name, ".") == 0)
			continue;
		if (strcmp(dp->d_name, "..") == 0)
			continue;
		if (strcmp(dp->d_name, "pkgdb.byfile.db") == 0)
			continue;
		if (strcmp(dp->d_name, ".cookie") == 0)
			continue;
		if (strcmp(dp->d_name, "pkg-vulnerabilities") == 0)
			continue;
#if defined(DT_UNKNOWN) && defined(DT_DIR)
		if (dp->d_type != DT_UNKNOWN && dp->d_type != DT_DIR)
			continue;
#endif
		return dp->d_name;
	}
	return NULL;
}

/*
 * Call matchiter for every installed package.
 */
int
iterate_pkg_db(int (*matchiter)(const char *, void *), void *cookie)
{
	DIR *dirp;
	int retval;

	if ((dirp = opendir(pkgdb_get_dir())) == NULL) {
		if (errno == ENOENT)
			return 0; /* No pkgdb directory == empty pkgdb */
		return -1;
	}

	retval = iterate_pkg_generic_src(matchiter, cookie, pkg_db_iter, dirp);

	if (closedir(dirp) == -1)
		return -1;
	return retval;
}

struct pkg_db_iter_arg {
	struct pkg_db_list_head head;
	struct pkg_db_list *list;
};

static const char *
pkg_db_iter_cached(void *cookie)
{
	struct pkg_db_iter_arg *arg = cookie;

	if (arg->list == NULL)
		arg->list = SLIST_FIRST(&arg->head);
	else
		arg->list = SLIST_NEXT(arg->list, entries);

	if (arg->list != NULL)
		return arg->list->pkgname;

	return NULL;
}

/*
 * Call matchiter for every installed package, using cached data to
 * significantly increase performance during recursive calls.
 *
 * This is not suitable for every situation, for example when finding new
 * matches after package installation/removal.  In those situations the
 * regular iterate_pkg_db() must be used.
 */
static int
iterate_pkg_db_cached(int (*matchiter)(const char *, void *), void *cookie)
{
	DIR *dirp;
	struct pkg_db_iter_arg arg;
	struct pkg_db_list *pkg;
	const char *pkgdir;
	int retval;

	if (SLIST_EMPTY(&pkg_list_cache)) {
		SLIST_INIT(&pkg_list_cache);

		if ((dirp = opendir(pkgdb_get_dir())) == NULL) {
			if (errno == ENOENT)
				return 0; /* Empty pkgdb */
			return -1;
		}

		while ((pkgdir = pkg_db_iter(dirp)) != NULL) {
			pkg = xmalloc(sizeof(struct pkg_db_list));
			pkg->pkgname = xstrdup(pkgdir);
			SLIST_INSERT_HEAD(&pkg_list_cache, pkg, entries);
		}

		if (closedir(dirp) == -1)
			return -1;
	}

	arg.head = pkg_list_cache;
	arg.list = NULL;

	retval = iterate_pkg_generic_src(matchiter, cookie,
	    pkg_db_iter_cached, &arg);

	return retval;
}

static int
match_by_basename(const char *pkg, void *cookie)
{
	const char *target = cookie;
	const char *pkg_version;

	if ((pkg_version = strrchr(pkg, '-')) == NULL) {
		warnx("Entry %s in pkgdb is not a valid package name", pkg);
		return 0;
	}
	if (strncmp(pkg, target, pkg_version - pkg) == 0 &&
	    pkg + strlen(target) == pkg_version)
		return 1;
	else
		return 0;
}

static int
match_by_pattern(const char *pkg, void *cookie)
{
	const char *pattern = cookie;

	return pkg_match(pattern, pkg);
}

struct add_matching_arg {
	lpkg_head_t *pkghead;
	int got_match;
	int (*match_fn)(const char *pkg, void *cookie);
	void *cookie;
};

static int
match_and_add(const char *pkg, void *cookie)
{
	struct add_matching_arg *arg = cookie;
	lpkg_t *lpp;

	if ((*arg->match_fn)(pkg, arg->cookie) == 1) {
		arg->got_match = 1;

		lpp = alloc_lpkg(pkg);
		TAILQ_INSERT_TAIL(arg->pkghead, lpp, lp_link);
	}
	return 0;
}

/*
 * Find all installed packages with the given basename and add them
 * to pkghead.
 * Returns -1 on error, 0 if no match was found and 1 otherwise.
 */
int
add_installed_pkgs_by_basename(const char *pkgbase, lpkg_head_t *pkghead)
{
	struct add_matching_arg arg;

	arg.pkghead = pkghead;
	arg.got_match = 0;
	arg.match_fn = match_by_basename;
	arg.cookie = __UNCONST(pkgbase);

	if (iterate_pkg_db(match_and_add, &arg) == -1) {
		warnx("could not process pkgdb");
		return -1;
	}
	return arg.got_match;
}

/*
 * Match all installed packages against pattern, add the matches to pkghead.
 * Returns -1 on error, 0 if no match was found and 1 otherwise.
 */
int
add_installed_pkgs_by_pattern(const char *pattern, lpkg_head_t *pkghead)
{
	struct add_matching_arg arg;

	arg.pkghead = pkghead;
	arg.got_match = 0;
	arg.match_fn = match_by_pattern;
	arg.cookie = __UNCONST(pattern);

	if (iterate_pkg_db(match_and_add, &arg) == -1) {
		warnx("could not process pkgdb");
		return -1;
	}
	return arg.got_match;
}

struct best_installed_match_arg {
	const char *pattern;
	char *best_current_match;
};

static int
match_best_installed(const char *pkg, void *cookie)
{
	struct best_installed_match_arg *arg = cookie;

	switch (pkg_order(arg->pattern, pkg, arg->best_current_match)) {
	case 0:
	case 2:
		/*
		 * Either current package doesn't match or
		 * the older match is better. Nothing to do.
		 */
		break;
	case 1:
		/* Current package is better, remember it. */
		free(arg->best_current_match);
		arg->best_current_match = xstrdup(pkg);
		break;
	}
	return 0;
}

/*
 * Returns a copy of the name of best matching package.
 * If no package matched the pattern or an error occured, return NULL.
 *
 * If use_cached is set, return a cached match entry if it exists, and also use
 * the iterate_pkg_db cache, otherwise clear any matching cache entry and use
 * regular iterate_pkg_db().
 */
char *
find_best_matching_installed_pkg(const char *pattern, int use_cached)
{
	struct best_installed_match_arg arg;
	struct pkg_match_list *pkg;
	int idx = PKG_HASH_ENTRY(pattern), rv;

	if (pattern == NULL)
		return NULL;

	SLIST_FOREACH(pkg, &pkg_match_cache[idx], entries) {
		if (strcmp(pattern, pkg->pattern) == 0) {
			if (use_cached)
				return xstrdup(pkg->pkgname);
			SLIST_REMOVE(&pkg_match_cache[idx], pkg,
			    pkg_match_list, entries);
			free(pkg->pattern);
			free(pkg->pkgname);
			free(pkg);
			break;
		}
	}

	arg.pattern = pattern;
	arg.best_current_match = NULL;

	if (use_cached)
		rv = iterate_pkg_db_cached(match_best_installed, &arg);
	else
		rv = iterate_pkg_db(match_best_installed, &arg);

	if (rv == -1) {
		warnx("could not process pkgdb");
		return NULL;
	}

	if (arg.best_current_match != NULL) {
		pkg = xmalloc(sizeof(struct pkg_match_list));
		pkg->pattern = xstrdup(pattern);
		pkg->pkgname = xstrdup(arg.best_current_match);
		SLIST_INSERT_HEAD(&pkg_match_cache[idx],
		    pkg, entries);
	}

	return arg.best_current_match;
}

struct call_matching_arg {
	const char *pattern;
	int (*call_fn)(const char *pkg, void *cookie);
	void *cookie;
};

static int
match_and_call(const char *pkg, void *cookie)
{
	struct call_matching_arg *arg = cookie;

	if (pkg_match(arg->pattern, pkg) == 1) {
		return (*arg->call_fn)(pkg, arg->cookie);
	} else
		return 0;
}

/*
 * Find all packages that match the given pattern and call the function
 * for each of them. Iteration stops if the callback return non-0.
 * Returns -1 on error, 0 if the iteration finished or whatever the
 * callback returned otherwise.
 */
int
match_installed_pkgs(const char *pattern, int (*cb)(const char *, void *),
    void *cookie)
{
	struct call_matching_arg arg;

	arg.pattern = pattern;
	arg.call_fn = cb;
	arg.cookie = cookie;

	return iterate_pkg_db(match_and_call, &arg);
}

struct best_file_match_arg {
	const char *pattern;
	char *best_current_match_filtered;
	char *best_current_match;
	int filter_suffix;
};

static int
match_best_file(const char *filename, void *cookie)
{
	struct best_file_match_arg *arg = cookie;
	const char *active_filename;
	char *filtered_filename;

	if (arg->filter_suffix) {
		size_t len;

		len = strlen(filename);
		if (len < 5 ||
		    (memcmp(filename + len - 4, ".tgz", 4) != 0 &&
		     memcmp(filename + len - 4, ".tbz", 4) != 0)) {
			warnx("filename %s does not contain a recognized suffix", filename);
			return -1;
		}
		filtered_filename = xmalloc(len - 4 + 1);
		memcpy(filtered_filename, filename, len - 4);
		filtered_filename[len - 4] = '\0';
		active_filename = filtered_filename;
	} else {
		filtered_filename = NULL;
		active_filename = filename;
	}

	switch (pkg_order(arg->pattern, active_filename, arg->best_current_match_filtered)) {
	case 0:
	case 2:
		/*
		 * Either current package doesn't match or
		 * the older match is better. Nothing to do.
		 */
		free(filtered_filename);
		return 0;
	case 1:
		/* Current package is better, remember it. */
		free(arg->best_current_match);
		free(arg->best_current_match_filtered);
		arg->best_current_match = xstrdup(filename);
		if (filtered_filename != NULL)
			arg->best_current_match_filtered = filtered_filename;
		else
			arg->best_current_match_filtered = xstrdup(active_filename);
		return 0;
	default:
		errx(EXIT_FAILURE, "Invalid error from pkg_order");
		/* NOTREACHED */
	}
}

/*
 * Returns a copy of the name of best matching file.
 * If no package matched the pattern or an error occured, return NULL.
 */
char *
find_best_matching_file(const char *dir, const char *pattern, int filter_suffix, int allow_nonfiles)
{
	struct best_file_match_arg arg;

	arg.filter_suffix = filter_suffix;
	arg.pattern = pattern;
	arg.best_current_match = NULL;
	arg.best_current_match_filtered = NULL;

	if (iterate_local_pkg_dir(dir, filter_suffix, allow_nonfiles, match_best_file, &arg) == -1) {
		warnx("could not process directory");
		return NULL;
	}
	free(arg.best_current_match_filtered);

	return arg.best_current_match;
}

struct call_matching_file_arg {
	const char *pattern;
	int (*call_fn)(const char *pkg, void *cookie);
	void *cookie;
	int filter_suffix;
};

static int
match_file_and_call(const char *filename, void *cookie)
{
	struct call_matching_file_arg *arg = cookie;
	const char *active_filename;
	char *filtered_filename;
	int ret;

	if (arg->filter_suffix) {
		size_t len;

		len = strlen(filename);
		if (len < 5 ||
		    (memcmp(filename + len - 4, ".tgz", 4) != 0 &&
		     memcmp(filename + len - 4, ".tbz", 4) != 0)) {
			warnx("filename %s does not contain a recognized suffix", filename);
			return -1;
		}
		filtered_filename = xmalloc(len - 4 + 1);
		memcpy(filtered_filename, filename, len - 4);
		filtered_filename[len - 4] = '\0';
		active_filename = filtered_filename;
	} else {
		filtered_filename = NULL;
		active_filename = filename;
	}

	ret = pkg_match(arg->pattern, active_filename);
	free(filtered_filename);

	if (ret == 1)
		return (*arg->call_fn)(filename, arg->cookie);
	else
		return 0;
}

/*
 * Find all packages that match the given pattern and call the function
 * for each of them. Iteration stops if the callback return non-0.
 * Returns -1 on error, 0 if the iteration finished or whatever the
 * callback returned otherwise.
 */
int
match_local_files(const char *dir, int filter_suffix, int allow_nonfiles, const char *pattern,
    int (*cb)(const char *, void *), void *cookie)
{
	struct call_matching_file_arg arg;

	arg.pattern = pattern;
	arg.call_fn = cb;
	arg.cookie = cookie;
	arg.filter_suffix = filter_suffix;

	return iterate_local_pkg_dir(dir, filter_suffix, allow_nonfiles, match_file_and_call, &arg);
}
