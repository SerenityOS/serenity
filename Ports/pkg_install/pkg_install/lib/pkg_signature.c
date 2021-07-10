/*	$NetBSD: pkg_signature.c,v 1.13 2017/04/19 21:42:50 joerg Exp $	*/

#if HAVE_CONFIG_H
#include "config.h"
#endif
#include <nbcompat.h>
#if HAVE_SYS_CDEFS_H
#include <sys/cdefs.h>
#endif
__RCSID("$NetBSD: pkg_signature.c,v 1.13 2017/04/19 21:42:50 joerg Exp $");

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

#if HAVE_SYS_WAIT_H
#include <sys/wait.h>
#endif
#include <ctype.h>
#if HAVE_ERR_H
#include <err.h>
#endif
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <stdlib.h>
#ifndef NETBSD
#include <nbcompat/sha2.h>
#else
#include <sha2.h>
#endif
#include <signal.h>
#ifdef NETBSD
#include <unistd.h>
#else
#include <nbcompat/unistd.h>
#endif

#include <archive.h>
#include <archive_entry.h>

#include "lib.h"

#define HASH_FNAME "+PKG_HASH"
#define SIGNATURE_FNAME "+PKG_SIGNATURE"
#define GPG_SIGNATURE_FNAME "+PKG_GPG_SIGNATURE"

struct signature_archive {
	struct archive *archive;
	off_t pkg_size;
	size_t sign_block_len, sign_block_number, sign_cur_block;
	char **sign_blocks;
	unsigned char *sign_buf;
};

static void
hash_block(unsigned char *buf, size_t buf_len,
    char hash[SHA512_DIGEST_STRING_LENGTH])
{
	unsigned char digest[SHA512_DIGEST_LENGTH];
	SHA512_CTX hash_ctx;
	int i;

	SHA512_Init(&hash_ctx);
	SHA512_Update(&hash_ctx, buf, buf_len);
	SHA512_Final(digest, &hash_ctx);
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
}

static ssize_t
verify_signature_read_cb(struct archive *archive, void *cookie, const void **buf)
{
	struct signature_archive *state = cookie;
	char hash[SHA512_DIGEST_STRING_LENGTH];
	ssize_t len, expected;

	if (state->sign_cur_block >= state->sign_block_number)
		return 0;

	/* The following works for sign_block_len > 1 */
	if (state->sign_cur_block + 1 == state->sign_block_number)
		expected = state->pkg_size % state->sign_block_len;
	else
		expected = state->sign_block_len;

	len = archive_read_data(state->archive, state->sign_buf, expected);
	if (len != expected) {
		warnx("Short read from package");
		return -1;
	}

	hash_block(state->sign_buf, len, hash);

	if (strcmp(hash, state->sign_blocks[state->sign_cur_block]) != 0) {
		warnx("Invalid signature of block %llu",
		    (unsigned long long)state->sign_cur_block);
		return -1;
	}
	++state->sign_cur_block;
	*buf = state->sign_buf;
	return len;
}

static void
free_signature_int(struct signature_archive *state)
{
	size_t i;

	if (state->sign_blocks != NULL) {
		for (i = 0; i < state->sign_block_number; ++i)
			free(state->sign_blocks[i]);
	}
	free(state->sign_blocks);
	free(state->sign_buf);
	free(state);
}

static int
verify_signature_close_cb(struct archive *archive, void *cookie)
{
	struct signature_archive *state = cookie;

	archive_read_free(state->archive);
	free_signature_int(state);
	return 0;
}

static int
read_file_from_archive(const char *archive_name, struct archive *archive,
    struct archive_entry **entry,
    const char *fname, char **content, size_t *len)
{
	int r;

	*content = NULL;
	*len = 0;

retry:
	if (*entry == NULL &&
	    (r = archive_read_next_header(archive, entry)) != ARCHIVE_OK) {
		if (r == ARCHIVE_FATAL) {
			warnx("Cannot read from archive `%s': %s",
			    archive_name, archive_error_string(archive));
		} else {
			warnx("Premature end of archive `%s'", archive_name);
		}
		*entry = NULL;
		return -1;
	}
	if (strcmp(archive_entry_pathname(*entry), "//") == 0) {
		archive_read_data_skip(archive);
		*entry = NULL;
		goto retry;
	}

	if (strcmp(fname, archive_entry_pathname(*entry)) != 0)
		return 1;

	if (archive_entry_size(*entry) > SSIZE_MAX - 1) {
		warnx("Signature of archive `%s' too large to process",
		    archive_name);
		return 1;
	}
	*len = archive_entry_size(*entry);
	*content = xmalloc(*len + 1);

	if (archive_read_data(archive, *content, *len) != (ssize_t)*len) {
		warnx("Cannot read complete %s from archive `%s'", fname,
		    archive_name);
		free(*content);
		*len = 0;
		*content = NULL;
		return 1;
	}
	(*content)[*len] = '\0';
	*entry = NULL;

	return 0;
}

static int
parse_hash_file(const char *hash_file, char **pkgname,
    struct signature_archive *state)
{
	static const char block1[] = "pkgsrc signature\n\nversion: 1\npkgname: ";
	static const char block2[] = "algorithm: SHA512\nblock size: ";
	static const char block3[] = "file size: ";
	static const char block4[] = "end pkgsrc signature\n";
	char *next;
	size_t i, len;

	*pkgname = NULL;

	if (strncmp(hash_file, block1, strlen(block1)) != 0)
		goto cleanup;
	hash_file += strlen(block1);

	len = strcspn(hash_file, "\n");
	*pkgname = xmalloc(len + 1);
	memcpy(*pkgname, hash_file, len);
	(*pkgname)[len] = '\0';
	for (i = 0; i < len; ++i) {
		if (!isgraph((unsigned char)(*pkgname)[i]))
			goto cleanup;
	}
	hash_file += len + 1;

	if (strncmp(hash_file, block2, strlen(block2)) != 0)
		goto cleanup;
	hash_file += strlen(block2);

	errno = 0;
	if (!isdigit((unsigned char)*hash_file))
		goto cleanup;
	state->sign_block_len = strtoul(hash_file, &next, 10);
	hash_file = next;

	/* Assert sane minimum block size of 1KB */
	if (*hash_file++ != '\n' || errno == ERANGE || state->sign_block_len < 1024)
		goto cleanup;

	if (strncmp(hash_file, block3, strlen(block3)) != 0)
		goto cleanup;
	hash_file += strlen(block3);

	errno = 0;
	if (!isdigit((unsigned char)*hash_file))
		goto cleanup;
	if (/* CONSTCOND */sizeof(off_t) >= sizeof(long long))
		state->pkg_size = strtoll(hash_file, &next, 10);
	else
		state->pkg_size = strtol(hash_file, &next, 10);
	hash_file = next;
	if (*hash_file++ != '\n' || errno == ERANGE || state->pkg_size < 1)
		goto cleanup;

	if (*hash_file++ != '\n')
		goto cleanup;

	if (state->pkg_size / state->sign_block_len > SSIZE_MAX)
		goto cleanup;
	state->sign_block_number = (state->pkg_size +
	    state->sign_block_len - 1) / state->sign_block_len;

	state->sign_buf = xmalloc(state->sign_block_len);
	state->sign_blocks = xcalloc(state->sign_block_number, sizeof(char *));

	for (i = 0; i < state->sign_block_number; ++i) {
		len = strspn(hash_file, "01234567889abcdef");
		if (len != SHA512_DIGEST_LENGTH * 2 || hash_file[len] != '\n')
			goto cleanup_hashes;
		state->sign_blocks[i] = xmalloc(len + 1);
		memcpy(state->sign_blocks[i], hash_file, len);
		state->sign_blocks[i][len] = '\0';
		hash_file += len + 1;
	}

	if (strcmp(hash_file, block4) != 0)
		goto cleanup_hashes;

	return 0;

cleanup_hashes:
	for (i = 0; i < state->sign_block_number; ++i)
		free(state->sign_blocks[i]);
	free(state->sign_blocks);
	state->sign_blocks = NULL;

cleanup:
	warnx("Unknown format of hash file");
	free(*pkgname);
	*pkgname = NULL;
	return -1;
}

int
pkg_verify_signature(const char *archive_name, struct archive **archive,
    struct archive_entry **entry, char **pkgname)
{
	struct signature_archive *state;
	struct archive_entry *my_entry;
	struct archive *a;
	char *hash_file, *signature_file;
	size_t hash_len, signature_len;
	int r, has_sig;

	*pkgname = NULL;

	state = xcalloc(sizeof(*state), 1);

	r = read_file_from_archive(archive_name, *archive, entry, HASH_FNAME,
	    &hash_file, &hash_len);
	if (r == -1) {
		archive_read_free(*archive);
		*archive = NULL;
		free(state);
		goto no_valid_signature;
	} else if (r == 1) {
		free(state);
		goto no_valid_signature;
	}

	if (parse_hash_file(hash_file, pkgname, state))
		goto no_valid_signature;

	r = read_file_from_archive(archive_name, *archive, entry, SIGNATURE_FNAME,
	    &signature_file, &signature_len);
	if (r == -1) {
		archive_read_free(*archive);
		*archive = NULL;
		free(state);
		free(hash_file);
		goto no_valid_signature;
	} else if (r != 0) {
		if (*entry != NULL)
			r = read_file_from_archive(archive_name, *archive,
			    entry, GPG_SIGNATURE_FNAME,
			    &signature_file, &signature_len);
		if (r == -1) {
			archive_read_free(*archive);
			*archive = NULL;
			free(state);
			free(hash_file);
			goto no_valid_signature;
		} else if (r != 0) {
			free(hash_file);
			free(state);
			goto no_valid_signature;
		}
		has_sig = !gpg_verify(hash_file, hash_len, gpg_keyring_verify,
		    signature_file, signature_len);

		free(signature_file);
	} else {
#ifdef HAVE_SSL
		has_sig = !easy_pkcs7_verify(hash_file, hash_len, signature_file,
		    signature_len, certs_packages, 1);

		free(signature_file);
#else
		warnx("No OpenSSL support compiled in, skipping signature");
		has_sig = 0;
		free(signature_file);
#endif
	}

	r = archive_read_next_header(*archive, &my_entry);
	if (r != ARCHIVE_OK) {
		warnx("Cannot read inner package: %s",
		    archive_error_string(*archive));
		free_signature_int(state);
		goto no_valid_signature;
	}

	if (archive_entry_size(my_entry) != state->pkg_size) {
		warnx("Package size doesn't match signature");
		free_signature_int(state);
		goto no_valid_signature;
	}

	state->archive = *archive;

	a = prepare_archive();
	if (archive_read_open(a, state, NULL, verify_signature_read_cb,
	    verify_signature_close_cb)) {
		warnx("Can't open signed package file");
		archive_read_free(a);
		goto no_valid_signature;
	}
	*archive = a;
	*entry = NULL;

	return has_sig ? 0 : -1;

no_valid_signature:
	return -1;
}

int
pkg_full_signature_check(const char *archive_name, struct archive **archive)
{
	struct archive_entry *entry = NULL;
	char *pkgname;
	int r;

	if (pkg_verify_signature(archive_name, archive, &entry, &pkgname))
		return -1;
	if (pkgname == NULL)
		return 0;

	/* XXX read PLIST and compare pkgname */
	while ((r = archive_read_next_header(*archive, &entry)) == ARCHIVE_OK)
		archive_read_data_skip(*archive);

	free(pkgname);
	return r == ARCHIVE_EOF ? 0 : -1;
}

static char *
extract_pkgname(int fd)
{
	package_t plist;
	plist_t *p;
	struct archive *a;
	struct archive_entry *entry;
	char *buf;
	ssize_t len;
	int r;

	a = prepare_archive();
	if (archive_read_open_fd(a, fd, 1024)) {
		warnx("Cannot open binary package: %s",
		    archive_error_string(a));
		archive_read_free(a);
		return NULL;
	}

	r = archive_read_next_header(a, &entry);
	if (r != ARCHIVE_OK) {
		warnx("Cannot extract package name: %s",
		    r == ARCHIVE_EOF ? "EOF" : archive_error_string(a));
		archive_read_free(a);
		return NULL;
	}
	if (strcmp(archive_entry_pathname(entry), "+CONTENTS") != 0) {
		warnx("Invalid binary package, doesn't start with +CONTENTS");
		archive_read_free(a);
		return NULL;
	}
	if (archive_entry_size(entry) > SSIZE_MAX - 1) {
		warnx("+CONTENTS too large to process");
		archive_read_free(a);
		return NULL;
	}

	len = archive_entry_size(entry);
	buf = xmalloc(len + 1);

	if (archive_read_data(a, buf, len) != len) {
		warnx("Short read when extracing +CONTENTS");
		free(buf);
		archive_read_free(a);
		return NULL;
	}
	buf[len] = '\0';

	archive_read_free(a);

	parse_plist(&plist, buf);
	free(buf);
	p = find_plist(&plist, PLIST_NAME);	
	if (p != NULL) {
		buf = xstrdup(p->name);
	} else {
		warnx("Invalid PLIST: missing @name");
		buf = NULL;
	}
	free_plist(&plist);

	if (lseek(fd, 0, SEEK_SET) != 0) {
		warn("Cannot seek in archive");
		free(buf);
		return NULL;
	}

	return buf;
}

static const char hash_template[] =
"pkgsrc signature\n"
"\n"
"version: 1\n"
"pkgname: %s\n"
"algorithm: SHA512\n"
"block size: 65536\n"
"file size: %lld\n"
"\n";

static const char hash_trailer[] = "end pkgsrc signature\n";

#ifdef HAVE_SSL
void
pkg_sign_x509(const char *name, const char *output, const char *key_file, const char *cert_file)
{
	struct archive *pkg;
	struct archive_entry *entry, *hash_entry, *sign_entry;
	int fd;
	struct stat sb;
	char *hash_file, *signature_file, *tmp, *pkgname, hash[SHA512_DIGEST_STRING_LENGTH];
	unsigned char block[65536];
	off_t i, size;
	size_t block_len, signature_len;

	if ((fd = open(name, O_RDONLY)) == -1)
		err(EXIT_FAILURE, "Cannot open binary package %s", name);
	if (fstat(fd, &sb) == -1)
		err(EXIT_FAILURE, "Cannot stat %s", name);

	entry = archive_entry_new();
	archive_entry_copy_stat(entry, &sb);

	pkgname = extract_pkgname(fd);
	hash_file = xasprintf(hash_template, pkgname,
	    (long long)archive_entry_size(entry));
	free(pkgname);

	for (i = 0; i < archive_entry_size(entry); i += block_len) {
		if (i + (off_t)sizeof(block) < archive_entry_size(entry))
			block_len = sizeof(block);
		else
			block_len = archive_entry_size(entry) % sizeof(block);
		if (read(fd, block, block_len) != (ssize_t)block_len)
			err(2, "short read");
		hash_block(block, block_len, hash);
		tmp = xasprintf("%s%s\n", hash_file, hash);
		free(hash_file);
		hash_file = tmp;
	}
	tmp = xasprintf("%s%s", hash_file, hash_trailer);
	free(hash_file);
	hash_file = tmp;

	if (easy_pkcs7_sign(hash_file, strlen(hash_file), &signature_file,
	    &signature_len, key_file, cert_file))
		err(EXIT_FAILURE, "Cannot sign hash file");

	lseek(fd, 0, SEEK_SET);

	sign_entry = archive_entry_clone(entry);
	hash_entry = archive_entry_clone(entry);
	pkgname = strrchr(name, '/');
	archive_entry_set_pathname(entry, pkgname != NULL ? pkgname + 1 : name);
	archive_entry_set_pathname(hash_entry, HASH_FNAME);
	archive_entry_set_pathname(sign_entry, SIGNATURE_FNAME);
	archive_entry_set_size(hash_entry, strlen(hash_file));
	archive_entry_set_size(sign_entry, signature_len);

	pkg = archive_write_new();
	archive_write_set_format_ar_bsd(pkg);
	archive_write_open_filename(pkg, output);

	archive_write_header(pkg, hash_entry);
	archive_write_data(pkg, hash_file, strlen(hash_file));
	archive_write_finish_entry(pkg);
	archive_entry_free(hash_entry);

	archive_write_header(pkg, sign_entry);
	archive_write_data(pkg, signature_file, signature_len);
	archive_write_finish_entry(pkg);
	archive_entry_free(sign_entry);

	size = archive_entry_size(entry);
	archive_write_header(pkg, entry);

	for (i = 0; i < size; i += block_len) {
		if (i + (off_t)sizeof(block) < size)
			block_len = sizeof(block);
		else
			block_len = size % sizeof(block);
		if (read(fd, block, block_len) != (ssize_t)block_len)
			err(2, "short read");
		archive_write_data(pkg, block, block_len);
	}
	archive_write_finish_entry(pkg);
	archive_entry_free(entry);

	archive_write_free(pkg);

	close(fd);

	exit(0);
}
#endif

void
pkg_sign_gpg(const char *name, const char *output)
{
	struct archive *pkg;
	struct archive_entry *entry, *hash_entry, *sign_entry;
	int fd;
	struct stat sb;
	char *hash_file, *signature_file, *tmp, *pkgname, hash[SHA512_DIGEST_STRING_LENGTH];
	unsigned char block[65536];
	off_t i, size;
	size_t block_len, signature_len;

	if ((fd = open(name, O_RDONLY)) == -1)
		err(EXIT_FAILURE, "Cannot open binary package %s", name);
	if (fstat(fd, &sb) == -1)
		err(EXIT_FAILURE, "Cannot stat %s", name);

	entry = archive_entry_new();
	archive_entry_copy_stat(entry, &sb);

	pkgname = extract_pkgname(fd);
	hash_file = xasprintf(hash_template, pkgname,
	    (long long)archive_entry_size(entry));
	free(pkgname);

	for (i = 0; i < archive_entry_size(entry); i += block_len) {
		if (i + (off_t)sizeof(block) < archive_entry_size(entry))
			block_len = sizeof(block);
		else
			block_len = archive_entry_size(entry) % sizeof(block);
		if (read(fd, block, block_len) != (ssize_t)block_len)
			err(2, "short read");
		hash_block(block, block_len, hash);
		tmp = xasprintf("%s%s\n", hash_file, hash);
		free(hash_file);
		hash_file = tmp;
	}
	tmp = xasprintf("%s%s", hash_file, hash_trailer);
	free(hash_file);
	hash_file = tmp;

	if (detached_gpg_sign(hash_file, strlen(hash_file), &signature_file,
	    &signature_len, gpg_keyring_sign, gpg_sign_as))
		err(EXIT_FAILURE, "Cannot sign hash file");

	lseek(fd, 0, SEEK_SET);

	sign_entry = archive_entry_clone(entry);
	hash_entry = archive_entry_clone(entry);
	pkgname = strrchr(name, '/');
	archive_entry_set_pathname(entry, pkgname != NULL ? pkgname + 1 : name);
	archive_entry_set_pathname(hash_entry, HASH_FNAME);
	archive_entry_set_pathname(sign_entry, GPG_SIGNATURE_FNAME);
	archive_entry_set_size(hash_entry, strlen(hash_file));
	archive_entry_set_size(sign_entry, signature_len);

	pkg = archive_write_new();
	archive_write_set_format_ar_bsd(pkg);
	archive_write_open_filename(pkg, output);

	archive_write_header(pkg, hash_entry);
	archive_write_data(pkg, hash_file, strlen(hash_file));
	archive_write_finish_entry(pkg);
	archive_entry_free(hash_entry);

	archive_write_header(pkg, sign_entry);
	archive_write_data(pkg, signature_file, signature_len);
	archive_write_finish_entry(pkg);
	archive_entry_free(sign_entry);

	size = archive_entry_size(entry);
	archive_write_header(pkg, entry);

	for (i = 0; i < size; i += block_len) {
		if (i + (off_t)sizeof(block) < size)
			block_len = sizeof(block);
		else
			block_len = size % sizeof(block);
		if (read(fd, block, block_len) != (ssize_t)block_len)
			err(2, "short read");
		archive_write_data(pkg, block, block_len);
	}
	archive_write_finish_entry(pkg);
	archive_entry_free(entry);

	archive_write_free(pkg);

	close(fd);

	exit(0);
}
