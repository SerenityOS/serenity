/*	$NetBSD: license.c,v 1.58 2021/03/06 04:37:28 ryoon Exp $	*/

/*-
 * Copyright (c) 2009 Joerg Sonnenberger <joerg@NetBSD.org>.
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
#include <stdlib.h>
#include <string.h>

#include "lib.h"

#define	HASH_SIZE	521

const char *default_acceptable_licenses =
    "afl-3.0 "
    "apache-1.1 apache-2.0 "
    "arphic-public "
    "artistic artistic-2.0 "
    "boost-license "
    "cc-by-sa-v3.0 "
    "cc-by-sa-v4.0 "
    "cc-by-v4.0 "
    "cc0-1.0-universal "
    "cddl-1.0 "
    "cecill-2.1 "
    "cecill-b-v1 "
    "cpl-1.0 "
    "epl-v1.0 "
    "eupl-v1.1 "
    "gfsl "
    "gnu-fdl-v1.1 gnu-fdl-v1.2 gnu-fdl-v1.3 "
    "gnu-gpl-v1 "
    "gnu-gpl-v2 gnu-lgpl-v2 gnu-lgpl-v2.1 "
    "gnu-gpl-v3 gnu-lgpl-v3 "
    "happy "
    "hpnd "
    "info-zip "
    "ipafont "
    "ipl-1.0 "
    "isc "
    "lppl-1.0 lppl-1.2 lppl-1.3c "
    "lucent "
    "miros "
    "mit "
    "mpl-1.0 mpl-1.1 mpl-2.0 "
    "mplusfont "
    "odbl-v1 "
    "ofl-v1.0 ofl-v1.1 "
    "openssl "
    "original-bsd modified-bsd 2-clause-bsd "
    "osl "
    "paratype "
    "php "
    "png-license "
    "postgresql-license "
    "public-domain "
    "python-software-foundation "
    "qpl-v1.0 "
    "sgi-free-software-b-v2.0 "
    "sissl-1.1 "
    "sleepycat-public "
    "unicode "
    "unlicense "
    "vera-ttf-license "
    "w3c "
    "x11 "
    "zlib "
    "zpl-2.0 zpl-2.1 "
    "zsh";

#ifdef DEBUG
static size_t hash_collisions;
#endif

static char **license_hash[HASH_SIZE];
static const char license_spaces[] = " \t\n";
static const char license_chars[] =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-.";

static size_t
hash_license(const char *license, size_t len)
{
	size_t hash;

	for (hash = 0; *license && len; ++license, --len)
		hash = *license + hash * 32;
	return hash % HASH_SIZE;
}

static void
add_license_internal(const char *license, size_t len)
{
	char *new_license;
	size_t slot, i;

	slot = hash_license(license, len);

	new_license = malloc(len + 1);
	memcpy(new_license, license, len);
	new_license[len] = '\0';

	if (license_hash[slot] == NULL) {
		license_hash[slot] = calloc(sizeof(char *), 2);
		license_hash[slot][0] = new_license;
	} else {
		for (i = 0; license_hash[slot][i]; ++i) {
			if (!memcmp(license_hash[slot][i], license, len) &&
			    license_hash[slot][i][len] == '\0') {
				free(new_license);
				return;
			}
		}

#ifdef DEBUG
		++hash_collisions;
#endif

		license_hash[slot] = realloc(license_hash[slot],
		    sizeof(char *) * (i + 2));
		license_hash[slot][i] = new_license;
		license_hash[slot][i + 1] = NULL;
	}
}

int
add_licenses(const char *line)
{
	const char *next;

	if (line == NULL)
		return 0;

	for (line += strspn(line, license_spaces); line; ) {
		next = line + strspn(line, license_chars);
		if (next == line)
			return *line ? -1 : 0;
		add_license_internal(line, next - line);
		line = next + strspn(next, license_spaces);
		if (next == line)
			return *line ? -1 : 0;
	}
	return 0;
}

static int
acceptable_license_internal(const char *license, size_t len)
{
	size_t slot, i;

	slot = hash_license(license, len);

	if (license_hash[slot] == NULL)
		return 0;

	for (i = 0; license_hash[slot][i]; ++i) {
		if (strncmp(license_hash[slot][i], license, len) == 0 &&
		    license_hash[slot][i][len] == '\0')
			return 1;
	}

	return 0;
}

int
acceptable_license(const char *license)
{
	size_t len;

	len = strlen(license);
	if (strspn(license, license_chars) != len) {
		warnx("Invalid character in license name at position %" PRIzu, len);
		return -1;
	}

	return acceptable_license_internal(license, len);
}

static int
acceptable_pkg_license_internal(const char **licensep, int toplevel, const char *start)
{
	const char *license = *licensep;
	int need_parenthesis, is_true = 0;
	int expr_type = 0; /* 0: unset, 1: or, 2: and */
	size_t len;

	license += strspn(license, license_spaces);

	if (*license == '(' && !toplevel) {
		need_parenthesis = 1;
		++license;
		license += strspn(license, license_spaces);
	} else {
		need_parenthesis = 0;
	}

	for (;;) {
		if (*license == '(') {
			switch (acceptable_pkg_license_internal(&license, 0, start)) {
			case -1:
				return -1;
			case 0:
				if (expr_type == 2)
					is_true = 0;
				break;
			case 1:
				is_true = 1;
				break;
			}
			license += strspn(license, license_spaces);
		} else {
			len = strspn(license, license_chars);
			if (len == 0) {
				warnx("Invalid character in license name at position %" PRIzu, license - start + 1);
				return -1;
			}

			if (acceptable_license_internal(license, len)) {
				if (expr_type != 2)
					is_true = 1;
			} else if (expr_type == 2) {
				is_true = 0;
			}

			license += len;

			len = strspn(license, license_spaces);
			if (len == 0 && *license && *license  != ')') {
				warnx("Missing space at position %" PRIzu, license - start + 1);
				return -1;
			}
			license += len;
		}

		if (*license == ')') {
			if (!need_parenthesis) {
				warnx("Missing open parenthesis at position %" PRIzu, license - start + 1);
				return -1;
			}
			*licensep = license + 1;
			return is_true;
		}
		if (*license == '\0') {
			if (need_parenthesis) {
				warnx("Unbalanced parenthesis at position %" PRIzu, license - start + 1);
				return -1;
			}
			*licensep = license;
			return is_true;
		}

		if (strncmp(license, "AND", 3) == 0) {
			if (expr_type == 1) {
				warnx("Invalid operator in OR expression at position %" PRIzu, license - start + 1);
				return -1;
			}
			expr_type = 2;
			license += 3;
		} else if (strncmp(license, "OR", 2) == 0) {
			if (expr_type == 2) {
				warnx("Invalid operator in AND expression at position %" PRIzu, license - start + 1);
				return -1;
			}
			expr_type = 1;
			license += 2;
		} else {
			warnx("Invalid operator at position %" PRIzu, license - start + 1);
			return -1;
		}
		len = strspn(license, license_spaces);
		if (len == 0 && *license != '(') {
			warnx("Missing space at position %" PRIzu, license - start + 1);
			return -1;
		}
		license += len;
	}
}

int
acceptable_pkg_license(const char *license)
{
	int ret;

	ret = acceptable_pkg_license_internal(&license, 1, license);
	if (ret == -1)
		return -1;
	license += strspn(license, license_spaces);
	if (*license) {
		warnx("Trailing garbage in license specification");
		return -1;
	}
	return ret;
}

void
load_license_lists(void)
{
	if (add_licenses(getenv("PKGSRC_ACCEPTABLE_LICENSES")))
		errx(EXIT_FAILURE, "syntax error in PKGSRC_ACCEPTABLE_LICENSES");
	if (add_licenses(acceptable_licenses))
		errx(EXIT_FAILURE, "syntax error in ACCEPTABLE_LICENSES");
	if (add_licenses(getenv("PKGSRC_DEFAULT_ACCEPTABLE_LICENSES")))
		errx(EXIT_FAILURE, "syntax error in PKGSRC_DEFAULT_ACCEPTABLE_LICENSES");
	if (add_licenses(default_acceptable_licenses))
		errx(EXIT_FAILURE, "syntax error in DEFAULT_ACCEPTABLE_LICENSES");
}
