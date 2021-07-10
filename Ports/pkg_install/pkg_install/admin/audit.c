/*	$NetBSD: audit.c,v 1.18 2018/02/26 23:45:02 ginsbach Exp $	*/

#if HAVE_CONFIG_H
#include "config.h"
#endif
#include <nbcompat.h>
#if HAVE_SYS_CDEFS_H
#include <sys/cdefs.h>
#endif
__RCSID("$NetBSD: audit.c,v 1.18 2018/02/26 23:45:02 ginsbach Exp $");

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

#if HAVE_SYS_TYPES_H
#include <sys/types.h>
#endif
#if HAVE_SYS_STAT_H
#include <sys/stat.h>
#endif
#if HAVE_ERR_H
#include <err.h>
#endif
#if HAVE_ERRNO_H
#include <errno.h>
#endif
#if HAVE_FCNTL_H
#include <fcntl.h>
#endif
#if HAVE_SIGNAL_H
#include <signal.h>
#endif
#if HAVE_STDIO_H
#include <stdio.h>
#endif
#if HAVE_STRING_H
#include <string.h>
#endif
#ifdef NETBSD
#include <unistd.h>
#else
#include <nbcompat/unistd.h>
#endif

#include <fetch.h>

#include "admin.h"
#include "lib.h"

static int check_ignored_advisories = 0;
static int check_signature = 0;
static const char *limit_vul_types = NULL;
static int update_pkg_vuln = 0;

static struct pkg_vulnerabilities *pv;

static const char audit_options[] = "eist:";

static void
parse_options(int argc, char **argv, const char *options)
{
	int ch;

	optreset = 1;
	/*
	 * optind == 0 is interpreted as partial reset request
	 * by GNU getopt, so compensate against this and cleanup
	 * at the end.
	 */
	optind = 1;
	++argc;
	--argv;

	while ((ch = getopt(argc, argv, options)) != -1) {
		switch (ch) {
		case 'e':
			check_eol = "yes";
			break;
		case 'i':
			check_ignored_advisories = 1;
			break;
		case 's':
			check_signature = 1;
			break;
		case 't':
			limit_vul_types = optarg;
			break;
		case 'u':
			update_pkg_vuln = 1;
			break;
		default:
			usage();
			/* NOTREACHED */
		}
	}

	--optind; /* See above comment. */
}

static int
check_exact_pkg(const char *pkg)
{
	return audit_package(pv, pkg, limit_vul_types,
			     check_ignored_advisories, quiet ? 0 : 1);
}

static int
check_batch_exact_pkgs(const char *fname)
{
	FILE *f;
	char buf[4096], *line, *eol;
	int ret;

	ret = 0;
	if (strcmp(fname, "-") == 0)
		f = stdin;
	else {
		f = fopen(fname, "r");
		if (f == NULL)
			err(EXIT_FAILURE, "Failed to open input file %s",
			    fname);
	}
	while ((line = fgets(buf, sizeof(buf), f)) != NULL) {
		eol = line + strlen(line);
		if (eol == line)
			continue;
		--eol;
		if (*eol == '\n') {
			if (eol == line)
				continue;
			*eol = '\0';
		}
		ret |= check_exact_pkg(line);
	}
	if (f != stdin)
		fclose(f);

	return ret;
}

static int
check_one_installed_pkg(const char *pkg, void *cookie)
{
	int *ret = cookie;

	*ret |= check_exact_pkg(pkg);
	return 0;
}

static int
check_installed_pattern(const char *pattern)
{
	int ret = 0;

	match_installed_pkgs(pattern, check_one_installed_pkg, &ret);

	return ret;
}

static void
check_and_read_pkg_vulnerabilities(void)
{
	struct stat st;
	time_t now;

	if (pkg_vulnerabilities_file == NULL)
		errx(EXIT_FAILURE, "PKG_VULNERABILITIES is not set");

	if (verbose >= 1) {
		if (stat(pkg_vulnerabilities_file, &st) == -1) {
			if (errno == ENOENT)
				errx(EXIT_FAILURE,
				    "pkg-vulnerabilities not found, run %s -d",
				    getprogname());
			errx(EXIT_FAILURE, "pkg-vulnerabilities not readable");
		}
		now = time(NULL);
		now -= st.st_mtime;
		if (now < 0)
			warnx("pkg-vulnerabilities is from the future");
		else if (now > 86400 * 7)
			warnx("pkg-vulnerabilities is out of date (%ld days old)",
			    (long)(now / 86400));
		else if (verbose >= 2)
			warnx("pkg-vulnerabilities is %ld day%s old",
			    (long)(now / 86400), now / 86400 == 1 ? "" : "s");
	}

	pv = read_pkg_vulnerabilities_file(pkg_vulnerabilities_file, 0, check_signature);
}

void
audit_pkgdb(int argc, char **argv)
{
	int rv;

	parse_options(argc, argv, audit_options);
	argv += optind;

	check_and_read_pkg_vulnerabilities();

	rv = 0;
	if (*argv == NULL)
		rv |= check_installed_pattern("*");
	else {
		for (; *argv != NULL; ++argv)
			rv |= check_installed_pattern(*argv);
	}
	free_pkg_vulnerabilities(pv);

	if (rv == 0 && verbose >= 1)
		fputs("No vulnerabilities found\n", stderr);
	exit(rv ? EXIT_FAILURE : EXIT_SUCCESS);
}

void
audit_pkg(int argc, char **argv)
{
	int rv;

	parse_options(argc, argv, audit_options);
	argv += optind;

	check_and_read_pkg_vulnerabilities();
	rv = 0;
	for (; *argv != NULL; ++argv)
		rv |= check_exact_pkg(*argv);

	free_pkg_vulnerabilities(pv);

	if (rv == 0 && verbose >= 1)
		fputs("No vulnerabilities found\n", stderr);
	exit(rv ? EXIT_FAILURE : EXIT_SUCCESS);
}

void
audit_batch(int argc, char **argv)
{
	int rv;

	parse_options(argc, argv, audit_options);
	argv += optind;

	check_and_read_pkg_vulnerabilities();
	rv = 0;
	for (; *argv != NULL; ++argv)
		rv |= check_batch_exact_pkgs(*argv);
	free_pkg_vulnerabilities(pv);

	if (rv == 0 && verbose >= 1)
		fputs("No vulnerabilities found\n", stderr);
	exit(rv ? EXIT_FAILURE : EXIT_SUCCESS);
}

void
check_pkg_vulnerabilities(int argc, char **argv)
{
	parse_options(argc, argv, "s");
	if (argc != optind + 1)
		usage();

	pv = read_pkg_vulnerabilities_file(argv[optind], 0, check_signature);
	free_pkg_vulnerabilities(pv);
}

void
fetch_pkg_vulnerabilities(int argc, char **argv)
{
	struct pkg_vulnerabilities *pv_check;
	char *buf;
	size_t buf_len, buf_fetched;
	ssize_t cur_fetched;
	struct url *url;
	struct url_stat st;
	fetchIO *f;
	int fd;
	struct stat sb;
	char my_flags[20];
	const char *flags;

	parse_options(argc, argv, "su");
	if (argc != optind)
		usage();

	if (verbose >= 2)
		fprintf(stderr, "Fetching %s\n", pkg_vulnerabilities_url);

	url = fetchParseURL(pkg_vulnerabilities_url);
	if (url == NULL)
		errx(EXIT_FAILURE,
		    "Could not parse location of pkg_vulnerabilities: %s",
		    fetchLastErrString);

	flags = fetch_flags;
	if (update_pkg_vuln) {
		fd = open(pkg_vulnerabilities_file, O_RDONLY);
		if (fd != -1 && fstat(fd, &sb) != -1) {
			url->last_modified = sb.st_mtime;
			snprintf(my_flags, sizeof(my_flags), "%si",
			    fetch_flags);
			flags = my_flags;
		} else
			update_pkg_vuln = 0;
		if (fd != -1)
			close(fd);
	}

	f = fetchXGet(url, &st, flags);
	if (f == NULL && update_pkg_vuln &&
	    fetchLastErrCode == FETCH_UNCHANGED) {
		if (verbose >= 1)
			fprintf(stderr, "%s is not newer\n",
			    pkg_vulnerabilities_url);
		exit(EXIT_SUCCESS);
	}

	if (f == NULL)
		errx(EXIT_FAILURE, "Could not fetch vulnerability file: %s",
		    fetchLastErrString);

	if (st.size > SSIZE_MAX - 1)
		errx(EXIT_FAILURE, "pkg-vulnerabilities is too large");

	buf_len = st.size;
	buf = xmalloc(buf_len + 1);
	buf_fetched = 0;

	while (buf_fetched < buf_len) {
		cur_fetched = fetchIO_read(f, buf + buf_fetched,
		    buf_len - buf_fetched);
		if (cur_fetched == 0)
			errx(EXIT_FAILURE,
			    "Truncated pkg-vulnerabilities received");
		else if (cur_fetched == -1)
			errx(EXIT_FAILURE,
			    "IO error while fetching pkg-vulnerabilities: %s",
			    fetchLastErrString);
		buf_fetched += cur_fetched;
	}
	
	buf[buf_len] = '\0';

	pv_check = read_pkg_vulnerabilities_memory(buf, buf_len, check_signature);
	free_pkg_vulnerabilities(pv_check);

	fd = open(pkg_vulnerabilities_file, O_WRONLY | O_CREAT | O_TRUNC, 0644);
	if (fd == -1)
		err(EXIT_FAILURE, "Cannot create pkg-vulnerability file %s",
		    pkg_vulnerabilities_file);

	if (write(fd, buf, buf_len) != (ssize_t)buf_len)
		err(EXIT_FAILURE, "Cannot write pkg-vulnerability file");
	if (close(fd) == -1)
		err(EXIT_FAILURE, "Cannot close pkg-vulnerability file after write");

	free(buf);

	exit(EXIT_SUCCESS);
}

static int
check_pkg_history_pattern(const char *pkg, const char *pattern)
{
	const char *delim, *end_base;

	if (strpbrk(pattern, "*[") != NULL) {
		end_base = NULL;
		for (delim = pattern;
				*delim != '\0' && *delim != '['; delim++) {
			if (*delim == '-')
				end_base = delim;
		}

		if (end_base == NULL)
			errx(EXIT_FAILURE, "Missing - in wildcard pattern %s",
			    pattern);
		if ((delim = strchr(pattern, '>')) != NULL ||
		    (delim = strchr(pattern, '<')) != NULL)
			errx(EXIT_FAILURE,
			    "Mixed relational and wildcard patterns in %s",
			    pattern);
	} else if ((delim = strchr(pattern, '>')) != NULL) {
		end_base = delim;
		if ((delim = strchr(pattern, '<')) != NULL && delim < end_base)
			errx(EXIT_FAILURE, "Inverted operators in %s",
			    pattern);
	} else if ((delim = strchr(pattern, '<')) != NULL) {
		end_base = delim;
	} else if ((end_base = strrchr(pattern, '-')) == NULL) {
		errx(EXIT_FAILURE, "Missing - in absolute pattern %s",
		    pattern);
	}

	if (strncmp(pkg, pattern, end_base - pattern) != 0)
		return 0;
	if (pkg[end_base - pattern] != '\0')
		return 0;

	return 1;
}

static int
check_pkg_history1(const char *pkg, const char *pattern)
{
	const char *open_brace, *close_brace, *inner_brace, *suffix, *iter;
	size_t prefix_len, suffix_len, middle_len;
	char *expanded_pkg;

	open_brace = strchr(pattern, '{');
	if (open_brace == NULL) {
		if ((close_brace = strchr(pattern, '}')) != NULL)
			errx(EXIT_FAILURE, "Unbalanced {} in pattern %s",
			    pattern);
		return check_pkg_history_pattern(pkg, pattern);
	}
	close_brace = strchr(open_brace, '}');
	if (strchr(pattern, '}') != close_brace)
		errx(EXIT_FAILURE, "Unbalanced {} in pattern %s",
		    pattern);

	while ((inner_brace = strchr(open_brace + 1, '{')) != NULL) {
		if (inner_brace >= close_brace)
			break;
		open_brace = inner_brace;
	}

	expanded_pkg = xmalloc(strlen(pattern)); /* {} are going away... */

	prefix_len = open_brace - pattern;
	suffix = close_brace + 1;
	suffix_len = strlen(suffix) + 1;
	memcpy(expanded_pkg, pattern, prefix_len);

	++open_brace;

	do {
		iter = strchr(open_brace, ',');
		if (iter == NULL || iter > close_brace)
			iter = close_brace;

		middle_len = iter - open_brace;
		memcpy(expanded_pkg + prefix_len, open_brace, middle_len);
		memcpy(expanded_pkg + prefix_len + middle_len, suffix,
		    suffix_len);
		if (check_pkg_history1(pkg, expanded_pkg)) {
			free(expanded_pkg);
			return 1;
		}
		open_brace = iter + 1;
	} while (iter < close_brace);

	free(expanded_pkg);
	return 0;
}

static void
check_pkg_history(const char *pkg)
{
	size_t i;

	for (i = 0; i < pv->entries; ++i) {
		if (!quick_pkg_match(pv->vulnerability[i], pkg))
			continue;
		if (strcmp("eol", pv->classification[i]) == 0)
			continue;
		if (check_pkg_history1(pkg, pv->vulnerability[i]) == 0)
			continue;

		printf("%s %s %s\n", pv->vulnerability[i],
		    pv->classification[i], pv->advisory[i]);
	}
}

void
audit_history(int argc, char **argv)
{
	parse_options(argc, argv, "st:");
	argv += optind;

	check_and_read_pkg_vulnerabilities();
	for (; *argv != NULL; ++argv)
		check_pkg_history(*argv);

	free_pkg_vulnerabilities(pv);
	exit(EXIT_SUCCESS);
}
