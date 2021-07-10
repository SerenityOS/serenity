/*	$NetBSD: vulnerabilities-file.c,v 1.11 2020/07/21 14:32:00 sjmulder Exp $	*/

/*-
 * Copyright (c) 2008, 2010 Joerg Sonnenberger <joerg@NetBSD.org>.
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
__RCSID("$NetBSD: vulnerabilities-file.c,v 1.11 2020/07/21 14:32:00 sjmulder Exp $");

#if HAVE_SYS_STAT_H
#include <sys/stat.h>
#endif
#if HAVE_SYS_WAIT_H
#include <sys/wait.h>
#endif
#ifndef BOOTSTRAP
#include <archive.h>
#endif
#include <ctype.h>
#if HAVE_ERR_H
#include <err.h>
#endif
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <stdlib.h>
#include <string.h>
#ifndef NETBSD
#include <nbcompat/sha1.h>
#include <nbcompat/sha2.h>
#else
#include <sha1.h>
#include <sha2.h>
#endif
#include <unistd.h>

#include "lib.h"

static struct pkg_vulnerabilities *read_pkg_vulnerabilities_archive(struct archive *, int);
static struct pkg_vulnerabilities *parse_pkg_vuln(const char *, size_t, int);

static const char pgp_msg_start[] = "-----BEGIN PGP SIGNED MESSAGE-----\n";
static const char pgp_msg_end[] = "-----BEGIN PGP SIGNATURE-----\n";
static const char pkcs7_begin[] = "-----BEGIN PKCS7-----\n";
static const char pkcs7_end[] = "-----END PKCS7-----\n";

#ifndef BOOTSTRAP
static struct archive *
prepare_raw_file(void)
{
	struct archive *a = archive_read_new();
	if (a == NULL)
		errx(EXIT_FAILURE, "memory allocation failed");

	archive_read_support_filter_gzip(a);
	archive_read_support_filter_bzip2(a);
	archive_read_support_filter_xz(a);
	archive_read_support_format_raw(a);
	return a;
}
#endif

static void
verify_signature_pkcs7(const char *input)
{
#ifdef HAVE_SSL
	const char *begin_pkgvul, *end_pkgvul, *begin_sig, *end_sig;

	if (strncmp(input, pgp_msg_start, strlen(pgp_msg_start)) == 0) {
		begin_pkgvul = input + strlen(pgp_msg_start);
		if ((end_pkgvul = strstr(begin_pkgvul, pgp_msg_end)) == NULL)
			errx(EXIT_FAILURE, "Invalid PGP signature");
		if ((begin_sig = strstr(end_pkgvul, pkcs7_begin)) == NULL)
			errx(EXIT_FAILURE, "No PKCS7 signature");
	} else {
		begin_pkgvul = input;
		if ((begin_sig = strstr(begin_pkgvul, pkcs7_begin)) == NULL)
			errx(EXIT_FAILURE, "No PKCS7 signature");
		end_pkgvul = begin_sig;		
	}
	if ((end_sig = strstr(begin_sig, pkcs7_end)) == NULL)
		errx(EXIT_FAILURE, "Invalid PKCS7 signature");
	end_sig += strlen(pkcs7_end);

	if (easy_pkcs7_verify(begin_pkgvul, end_pkgvul - begin_pkgvul,
	    begin_sig, end_sig - begin_sig, certs_pkg_vulnerabilities, 0))
		errx(EXIT_FAILURE, "Unable to verify PKCS7 signature");
#else
	errx(EXIT_FAILURE, "OpenSSL support is not compiled in");
#endif
}

static void
verify_signature(const char *input, size_t input_len)
{
	gpg_verify(input, input_len, gpg_keyring_pkgvuln, NULL, 0);
	if (certs_pkg_vulnerabilities != NULL)
		verify_signature_pkcs7(input);
}

static void *
sha512_hash_init(void)
{
	static SHA512_CTX hash_ctx;

	SHA512_Init(&hash_ctx);
	return &hash_ctx;
}

static void
sha512_hash_update(void *ctx, const void *data, size_t len)
{
	SHA512_CTX *hash_ctx = ctx;

	SHA512_Update(hash_ctx, data, len);
}

static const char *
sha512_hash_finish(void *ctx)
{
	static char hash[SHA512_DIGEST_STRING_LENGTH];
	unsigned char digest[SHA512_DIGEST_LENGTH];
	SHA512_CTX *hash_ctx = ctx;
	int i;

	SHA512_Final(digest, hash_ctx);
	for (i = 0; i < SHA512_DIGEST_LENGTH; ++i) {
		unsigned char c;

		c = digest[i] / 16;
		if (c < 10)
			hash[2 * i] = '0' + c;
		else
			hash[2 * i] = 'a' - 10 + c;

		c = digest[i] % 16;
		if (c < 10)
			hash[2 * i + 1] = '0' + c;
		else
			hash[2 * i + 1] = 'a' - 10 + c;
	}
	hash[2 * i] = '\0';

	return hash;
}

static void *
sha1_hash_init(void)
{
	static SHA1_CTX hash_ctx;

	SHA1Init(&hash_ctx);
	return &hash_ctx;
}

static void
sha1_hash_update(void *ctx, const void *data, size_t len)
{
	SHA1_CTX *hash_ctx = ctx;

	SHA1Update(hash_ctx, data, len);
}

static const char *
sha1_hash_finish(void *ctx)
{
	static char hash[SHA1_DIGEST_STRING_LENGTH];
	SHA1_CTX *hash_ctx = ctx;

	SHA1End(hash_ctx, hash);

	return hash;
}

static const struct hash_algorithm {
	const char *name;
	size_t name_len;
	void * (*init)(void);
	void (*update)(void *, const void *, size_t);
	const char * (* finish)(void *);
} hash_algorithms[] = {
	{ "SHA512", 6, sha512_hash_init, sha512_hash_update,
	  sha512_hash_finish },
	{ "SHA1", 4, sha1_hash_init, sha1_hash_update,
	  sha1_hash_finish },
	{ NULL, 0, NULL, NULL, NULL }
};

static void
verify_hash(const char *input, const char *hash_line)
{
	const struct hash_algorithm *hash;
	void *ctx;
	const char *last_start, *next, *hash_value;
	int in_pgp_msg;

	for (hash = hash_algorithms; hash->name != NULL; ++hash) {
		if (strncmp(hash_line, hash->name, hash->name_len))
			continue;
		if (isspace((unsigned char)hash_line[hash->name_len]))
			break;
	}
	if (hash->name == NULL) {
		const char *end_name;
		for (end_name = hash_line; *end_name != '\0'; ++end_name) {
			if (!isalnum((unsigned char)*end_name))
				break;
		}
		warnx("Unsupported hash algorithm: %.*s",
		    (int)(end_name - hash_line), hash_line); 
		return;
	}

	hash_line += hash->name_len;
	if (!isspace((unsigned char)*hash_line))
		errx(EXIT_FAILURE, "Invalid #CHECKSUM");
	while (isspace((unsigned char)*hash_line) && *hash_line != '\n')
		++hash_line;

	if (*hash_line == '\n')
		errx(EXIT_FAILURE, "Invalid #CHECKSUM");

	ctx = (*hash->init)();
	if (strncmp(input, pgp_msg_start, strlen(pgp_msg_start)) == 0) {
		input += strlen(pgp_msg_start);
		in_pgp_msg = 1;
	} else {
		in_pgp_msg = 0;
	}
	for (last_start = input; *input != '\0'; input = next) {
		if ((next = strchr(input, '\n')) == NULL)
			errx(EXIT_FAILURE, "Missing newline in pkg-vulnerabilities");
		++next;
		if (in_pgp_msg && strncmp(input, pgp_msg_end, strlen(pgp_msg_end)) == 0)
			break;
		if (!in_pgp_msg && strncmp(input, pkcs7_begin, strlen(pkcs7_begin)) == 0)
			break;
		if (*input == '\n' ||
		    strncmp(input, "Hash:", 5) == 0 ||
		    strncmp(input, "# $NetBSD", 9) == 0 ||
		    strncmp(input, "#CHECKSUM", 9) == 0) {
			(*hash->update)(ctx, last_start, input - last_start);
			last_start = next;
		}
	}
	(*hash->update)(ctx, last_start, input - last_start);
	hash_value = (*hash->finish)(ctx);
	if (strncmp(hash_line, hash_value, strlen(hash_value)))
		errx(EXIT_FAILURE, "%s hash doesn't match", hash->name);
	hash_line += strlen(hash_value);

	while (isspace((unsigned char)*hash_line) && *hash_line != '\n')
		++hash_line;
	
	if (!isspace((unsigned char)*hash_line))
		errx(EXIT_FAILURE, "Invalid #CHECKSUM");
}

static void
add_vulnerability(struct pkg_vulnerabilities *pv, size_t *allocated, const char *line)
{
	size_t len_pattern, len_class, len_url;
	const char *start_pattern, *start_class, *start_url;

	start_pattern = line;

	start_class = line;
	while (*start_class != '\0' && !isspace((unsigned char)*start_class))
		++start_class;
	len_pattern = start_class - line;

	while (*start_class != '\n' && isspace((unsigned char)*start_class))
		++start_class;

	if (*start_class == '0' || *start_class == '\n')
		errx(EXIT_FAILURE, "Input error: missing classification");

	start_url = start_class;
	while (*start_url != '\0' && !isspace((unsigned char)*start_url))
		++start_url;
	len_class = start_url - start_class;

	while (*start_url != '\n' && isspace((unsigned char)*start_url))
		++start_url;

	if (*start_url == '0' || *start_url == '\n')
		errx(EXIT_FAILURE, "Input error: missing URL");

	line = start_url;
	while (*line != '\0' && !isspace((unsigned char)*line))
		++line;
	len_url = line - start_url;

	if (pv->entries == *allocated) {
		if (*allocated == 0)
			*allocated = 16;
		else if (*allocated <= SSIZE_MAX / 2)
			*allocated *= 2;
		else
			errx(EXIT_FAILURE, "Too many vulnerabilities");
		pv->vulnerability = xrealloc(pv->vulnerability,
		    sizeof(char *) * *allocated);
		pv->classification = xrealloc(pv->classification,
		    sizeof(char *) * *allocated);
		pv->advisory = xrealloc(pv->advisory,
		    sizeof(char *) * *allocated);
	}

	pv->vulnerability[pv->entries] = xmalloc(len_pattern + 1);
	memcpy(pv->vulnerability[pv->entries], start_pattern, len_pattern);
	pv->vulnerability[pv->entries][len_pattern] = '\0';
	pv->classification[pv->entries] = xmalloc(len_class + 1);
	memcpy(pv->classification[pv->entries], start_class, len_class);
	pv->classification[pv->entries][len_class] = '\0';
	pv->advisory[pv->entries] = xmalloc(len_url + 1);
	memcpy(pv->advisory[pv->entries], start_url, len_url);
	pv->advisory[pv->entries][len_url] = '\0';

	++pv->entries;
}

struct pkg_vulnerabilities *
read_pkg_vulnerabilities_memory(void *buf, size_t len, int check_sum)
{
#ifdef BOOTSTRAP
	errx(EXIT_FAILURE, "Audit functions are unsupported during bootstrap");
#else
	struct archive *a;
	struct pkg_vulnerabilities *pv;

	a = prepare_raw_file();
	if (archive_read_open_memory(a, buf, len) != ARCHIVE_OK)
		errx(EXIT_FAILURE, "Cannot open pkg_vulnerabilies buffer: %s",
		    archive_error_string(a));

	pv = read_pkg_vulnerabilities_archive(a, check_sum);

	return pv;
#endif
}

struct pkg_vulnerabilities *
read_pkg_vulnerabilities_file(const char *path, int ignore_missing, int check_sum)
{
#ifdef BOOTSTRAP
	errx(EXIT_FAILURE, "Audit functions are unsupported during bootstrap");
#else
	struct archive *a;
	struct pkg_vulnerabilities *pv;
	int fd;

	if ((fd = open(path, O_RDONLY)) == -1) {
		if (errno == ENOENT && ignore_missing)
			return NULL;
		err(EXIT_FAILURE, "Cannot open %s", path);
	}

	a = prepare_raw_file();
	if (archive_read_open_fd(a, fd, 65536) != ARCHIVE_OK)
		errx(EXIT_FAILURE, "Cannot open ``%s'': %s", path,
		    archive_error_string(a));

	pv = read_pkg_vulnerabilities_archive(a, check_sum);
	close(fd);

	return pv;
#endif
}

#ifndef BOOTSTRAP
static struct pkg_vulnerabilities *
read_pkg_vulnerabilities_archive(struct archive *a, int check_sum)
{
	struct archive_entry *ae;
	struct pkg_vulnerabilities *pv;
	char *buf;
	size_t buf_len, off;
	ssize_t r;

	if (archive_read_next_header(a, &ae) != ARCHIVE_OK)
		errx(EXIT_FAILURE, "Cannot read pkg_vulnerabilities: %s",
		    archive_error_string(a));

	off = 0;
	buf_len = 65536;
	buf = xmalloc(buf_len + 1);

	for (;;) {
		r = archive_read_data(a, buf + off, buf_len - off);
		if (r <= 0)
			break;
		off += r;
		if (off == buf_len) {
			buf_len *= 2;
			if (buf_len < off)
				errx(EXIT_FAILURE, "pkg_vulnerabilties too large");
			buf = xrealloc(buf, buf_len + 1);
		}
	}

	if (r != ARCHIVE_OK)
		errx(EXIT_FAILURE, "Cannot read pkg_vulnerabilities: %s",
		    archive_error_string(a));

	archive_read_close(a);

	buf[off] = '\0';
	pv = parse_pkg_vuln(buf, off, check_sum);
	free(buf);
	return pv;
}

static struct pkg_vulnerabilities *
parse_pkg_vuln(const char *input, size_t input_len, int check_sum)
{
	struct pkg_vulnerabilities *pv;
	long version;
	char *end;
	const char *iter, *next;
	size_t allocated_vulns;
	int in_pgp_msg;

	pv = xmalloc(sizeof(*pv));

	allocated_vulns = pv->entries = 0;
	pv->vulnerability = NULL;
	pv->classification = NULL;
	pv->advisory = NULL;

	if (strlen(input) != input_len)
		errx(1, "Invalid input (NUL character found)");

	if (check_sum)
		verify_signature(input, input_len);

	if (strncmp(input, pgp_msg_start, strlen(pgp_msg_start)) == 0) {
		iter = input + strlen(pgp_msg_start);
		in_pgp_msg = 1;
	} else {
		iter = input;
		in_pgp_msg = 0;
	}

	for (; *iter; iter = next) {
		if ((next = strchr(iter, '\n')) == NULL)
			errx(EXIT_FAILURE, "Missing newline in pkg-vulnerabilities");
		++next;
		if (*iter == '\0' || *iter == '\n')
			continue;
		if (strncmp(iter, "Hash:", 5) == 0)
			continue;
		if (strncmp(iter, "# $NetBSD", 9) == 0)
			continue;
		if (*iter == '#' && isspace((unsigned char)iter[1])) {
			for (++iter; iter != next; ++iter) {
				if (!isspace((unsigned char)*iter))
					errx(EXIT_FAILURE, "Invalid header");
			}
			continue;
		}

		if (strncmp(iter, "#FORMAT", 7) != 0)
			errx(EXIT_FAILURE, "Input header is malformed");

		iter += 7;
		if (!isspace((unsigned char)*iter))
			errx(EXIT_FAILURE, "Invalid #FORMAT");
		++iter;
		version = strtol(iter, &end, 10);
		if (iter == end || version != 1 || *end != '.')
			errx(EXIT_FAILURE, "Input #FORMAT");
		iter = end + 1;
		version = strtol(iter, &end, 10);
		if (iter == end || version != 1 || *end != '.')
			errx(EXIT_FAILURE, "Input #FORMAT");
		iter = end + 1;
		version = strtol(iter, &end, 10);
		if (iter == end || version != 0)
			errx(EXIT_FAILURE, "Input #FORMAT");
		for (iter = end; iter != next; ++iter) {
			if (!isspace((unsigned char)*iter))
				errx(EXIT_FAILURE, "Input #FORMAT");
		}
		break;
	}
	if (*iter == '\0')
		errx(EXIT_FAILURE, "Missing #CHECKSUM or content");

	for (iter = next; *iter; iter = next) {
		if ((next = strchr(iter, '\n')) == NULL)
			errx(EXIT_FAILURE, "Missing newline in pkg-vulnerabilities");
		++next;
		if (*iter == '\0' || *iter == '\n')
			continue;
		if (in_pgp_msg && strncmp(iter, pgp_msg_end, strlen(pgp_msg_end)) == 0)
			break;
		if (!in_pgp_msg && strncmp(iter, pkcs7_begin, strlen(pkcs7_begin)) == 0)
			break;
		if (*iter == '#' &&
		    (iter[1] == '\0' || iter[1] == '\n' || isspace((unsigned char)iter[1])))
			continue;
		if (strncmp(iter, "#CHECKSUM", 9) == 0) {
			iter += 9;
			if (!isspace((unsigned char)*iter))
				errx(EXIT_FAILURE, "Invalid #CHECKSUM");
			while (isspace((unsigned char)*iter))
				++iter;
			verify_hash(input, iter);
			continue;
		}
		if (*iter == '#') {
			/*
			 * This should really be an error,
			 * but it is still used.
			 */
			/* errx(EXIT_FAILURE, "Invalid data line starting with #"); */
			continue;
		}
		add_vulnerability(pv, &allocated_vulns, iter);
	}

	if (pv->entries != allocated_vulns) {
		pv->vulnerability = xrealloc(pv->vulnerability,
		    sizeof(char *) * pv->entries);
		pv->classification = xrealloc(pv->classification,
		    sizeof(char *) * pv->entries);
		pv->advisory = xrealloc(pv->advisory,
		    sizeof(char *) * pv->entries);
	}

	return pv;
}
#endif

void
free_pkg_vulnerabilities(struct pkg_vulnerabilities *pv)
{
	size_t i;

	for (i = 0; i < pv->entries; ++i) {
		free(pv->vulnerability[i]);
		free(pv->classification[i]);
		free(pv->advisory[i]);
	}
	free(pv->vulnerability);
	free(pv->classification);
	free(pv->advisory);
	free(pv);
}

static int
check_ignored_entry(struct pkg_vulnerabilities *pv, size_t i)
{
	const char *iter, *next;
	size_t entry_len, url_len;

	if (ignore_advisories == NULL)
		return 0;

	url_len = strlen(pv->advisory[i]);

	for (iter = ignore_advisories; *iter; iter = next) {
		if ((next = strchr(iter, '\n')) == NULL) {
			entry_len = strlen(iter);
			next = iter + entry_len;
		} else {
			entry_len = next - iter;
			++next;
		}
		if (url_len != entry_len)
			continue;
		if (strncmp(pv->advisory[i], iter, entry_len) == 0)
			return 1;
	}
	return 0;
}

int
audit_package(struct pkg_vulnerabilities *pv, const char *pkgname,
    const char *limit_vul_types, int include_ignored, int output_type)
{
	FILE *output = output_type == 1 ? stdout : stderr;
	size_t i;
	int retval, do_eol, ignored;

	retval = 0;

	do_eol = (strcasecmp(check_eol, "yes") == 0);

	for (i = 0; i < pv->entries; ++i) {
		ignored = check_ignored_entry(pv, i);
		if (ignored && !include_ignored)
			continue;
		if (limit_vul_types != NULL &&
		    strcmp(limit_vul_types, pv->classification[i]))
			continue;
		if (!pkg_match(pv->vulnerability[i], pkgname))
			continue;
		if (strcmp("eol", pv->classification[i]) == 0) {
			if (!do_eol)
				continue;
			retval = 1;
			if (output_type == 0) {
				puts(pkgname);
				continue;
			}
			fprintf(output,
			    "Package %s has reached end-of-life (eol), "
			    "see %s/eol-packages\n", pkgname,
			    tnf_vulnerability_base);
			continue;
		}
		retval = 1;
		if (output_type == 0) {
			fprintf(stdout, "%s%s\n",
				pkgname, ignored ? " (ignored)" : "");
		} else {
			fprintf(output,
			    "Package %s has a%s %s vulnerability, see %s\n",
			    pkgname, ignored ? "n ignored" : "",
			    pv->classification[i], pv->advisory[i]);
		}
	}
	return retval;
}
