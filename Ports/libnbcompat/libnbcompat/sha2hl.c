/* $NetBSD: sha2hl.c,v 1.8 2011/11/08 18:20:03 joerg Exp $	 */

/*
 * sha2hl.c
 * This code includes some functions taken from sha2.c, hence the
 * following licence reproduction.
 *
 * This code is not a verbatim copy, since some routines have been added,
 * and some bugs have been fixed.
 *
 * Version 1.0.0beta1
 *
 * Written by Aaron D. Gifford <me@aarongifford.com>
 *
 * Copyright 2000 Aaron D. Gifford.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR(S) AND CONTRIBUTOR(S) ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR(S) OR CONTRIBUTOR(S) BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 */

#include <nbcompat.h>
#include <nbcompat/assert.h>
#ifdef HAVE_ERRNO_H
#include <errno.h>
#endif
#ifdef HAVE_FCNTL_H
#include <fcntl.h>
#endif
#include <nbcompat/sha2.h>
#include <nbcompat/stdio.h>
#include <nbcompat/string.h>
#include <nbcompat/stdlib.h>
#include <nbcompat/unistd.h>

#ifndef _DIAGASSERT
#define _DIAGASSERT(cond)	assert(cond)
#endif

#ifndef MEMSET_BZERO
#define MEMSET_BZERO(p,l)	memset((p), 0, (l))
#endif

/*
 * Constant used by SHA256/384/512_End() functions for converting the
 * digest to a readable hexadecimal character string:
 */
static const char sha2_hex_digits[] = "0123456789abcdef";

char           *
SHA256_File(char *filename, char *buf)
{
	unsigned char          buffer[BUFSIZ * 20];
	SHA256_CTX      ctx;
	int             fd, num, oerrno;

	_DIAGASSERT(filename != NULL);
	/* XXX: buf may be NULL ? */

	SHA256_Init(&ctx);

	if ((fd = open(filename, O_RDONLY)) < 0)
		return (0);

	while ((num = read(fd, buffer, sizeof(buffer))) > 0)
		SHA256_Update(&ctx, buffer, (size_t) num);

	oerrno = errno;
	close(fd);
	errno = oerrno;
	return (num < 0 ? 0 : SHA256_End(&ctx, buf));
}


char           *
SHA256_End(SHA256_CTX *ctx, char buffer[])
{
	unsigned char          digest[SHA256_DIGEST_LENGTH], *d = digest;
	unsigned char	       *ret;
	int             i;

	/* Sanity check: */
	assert(ctx != NULL);

	if ((ret = buffer) != NULL) {
		SHA256_Final(digest, ctx);

		for (i = 0; i < SHA256_DIGEST_LENGTH; i++) {
			*buffer++ = sha2_hex_digits[(*d & 0xf0) >> 4];
			*buffer++ = sha2_hex_digits[*d & 0x0f];
			d++;
		}
		*buffer = (char) 0;
	} else {
		(void) MEMSET_BZERO(ctx, sizeof(SHA256_CTX));
	}
	(void) MEMSET_BZERO(digest, SHA256_DIGEST_LENGTH);
	return ret;
}

char           *
SHA256_Data(const uint8_t * data, size_t len, unsigned char *digest)
{
	SHA256_CTX      ctx;

	SHA256_Init(&ctx);
	SHA256_Update(&ctx, data, len);
	return SHA256_End(&ctx, digest);
}

char           *
SHA384_File(char *filename, char *buf)
{
	SHA384_CTX      ctx;
	unsigned char          buffer[BUFSIZ * 20];
	int             fd, num, oerrno;

	_DIAGASSERT(filename != NULL);
	/* XXX: buf may be NULL ? */

	SHA384_Init(&ctx);

	if ((fd = open(filename, O_RDONLY)) < 0)
		return (0);

	while ((num = read(fd, buffer, sizeof(buffer))) > 0)
		SHA384_Update(&ctx, buffer, (size_t) num);

	oerrno = errno;
	close(fd);
	errno = oerrno;
	return (num < 0 ? 0 : SHA384_End(&ctx, buf));
}

char           *
SHA384_End(SHA384_CTX * ctx, char buffer[])
{
	unsigned char          digest[SHA384_DIGEST_LENGTH], *d = digest;
	unsigned char	       *ret;
	int             i;

	/* Sanity check: */
	assert(ctx != NULL);

	if ((ret = buffer) != NULL) {
		SHA384_Final(digest, ctx);

		for (i = 0; i < SHA384_DIGEST_LENGTH; i++) {
			*buffer++ = sha2_hex_digits[(*d & 0xf0) >> 4];
			*buffer++ = sha2_hex_digits[*d & 0x0f];
			d++;
		}
		*buffer = (char) 0;
	} else {
		(void) MEMSET_BZERO(ctx, sizeof(SHA384_CTX));
	}
	(void) MEMSET_BZERO(digest, SHA384_DIGEST_LENGTH);
	return ret;
}

char           *
SHA384_Data(const uint8_t* data, size_t len, char digest[SHA384_DIGEST_STRING_LENGTH])
{
	SHA384_CTX      ctx;

	SHA384_Init(&ctx);
	SHA384_Update(&ctx, data, len);
	return SHA384_End(&ctx, digest);
}

char           *
SHA512_File(char *filename, char *buf)
{
	SHA512_CTX      ctx;
	unsigned char          buffer[BUFSIZ * 20];
	int             fd, num, oerrno;

	_DIAGASSERT(filename != NULL);
	/* XXX: buf may be NULL ? */

	SHA512_Init(&ctx);

	if ((fd = open(filename, O_RDONLY)) < 0)
		return (0);

	while ((num = read(fd, buffer, sizeof(buffer))) > 0)
		SHA512_Update(&ctx, buffer, (size_t) num);

	oerrno = errno;
	close(fd);
	errno = oerrno;
	return (num < 0 ? 0 : SHA512_End(&ctx, buf));
}

char           *
SHA512_End(SHA512_CTX * ctx, char buffer[])
{
	unsigned char          digest[SHA512_DIGEST_LENGTH], *d = digest;
	unsigned char	       *ret;
	int             i;

	/* Sanity check: */
	assert(ctx != NULL);

	if ((ret = buffer) != NULL) {
		SHA512_Final(digest, ctx);

		for (i = 0; i < SHA512_DIGEST_LENGTH; i++) {
			*buffer++ = sha2_hex_digits[(*d & 0xf0) >> 4];
			*buffer++ = sha2_hex_digits[*d & 0x0f];
			d++;
		}
		*buffer = (char) 0;
	} else {
		(void) MEMSET_BZERO(ctx, sizeof(SHA512_CTX));
	}
	(void) MEMSET_BZERO(digest, SHA512_DIGEST_LENGTH);
	return ret;
}

char           *
SHA512_Data(const uint8_t * data, size_t len, char *digest)
{
	SHA512_CTX      ctx;

	SHA512_Init(&ctx);
	SHA512_Update(&ctx, data, len);
	return SHA512_End(&ctx, digest);
}
