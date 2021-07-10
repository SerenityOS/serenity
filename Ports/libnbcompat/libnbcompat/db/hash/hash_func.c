/*	$NetBSD: hash_func.c,v 1.1 2008/10/10 00:21:43 joerg Exp $	*/
/*	NetBSD: hash_func.c,v 1.13 2008/09/10 17:52:35 joerg Exp 	*/

/*-
 * Copyright (c) 1990, 1993
 *	The Regents of the University of California.  All rights reserved.
 *
 * This code is derived from software contributed to Berkeley by
 * Margo Seltzer.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

#include <nbcompat.h>
#include <nbcompat/cdefs.h>

__RCSID("$NetBSD: hash_func.c,v 1.1 2008/10/10 00:21:43 joerg Exp $");

#include <sys/types.h>

#include <nbcompat/db.h>
#include "hash.h"
#include "page.h"
#include "extern.h"

#if 0
static uint32_t hash1(const void *, size_t) __attribute__((__unused__));
static uint32_t hash2(const void *, size_t) __attribute__((__unused__));
static uint32_t hash3(const void *, size_t) __attribute__((__unused__));
#endif
static uint32_t hash4(const void *, size_t) __attribute__((__unused__));

/* Global default hash function */
uint32_t (*__default_hash)(const void *, size_t) = hash4;
#if 0
/*
 * HASH FUNCTIONS
 *
 * Assume that we've already split the bucket to which this key hashes,
 * calculate that bucket, and check that in fact we did already split it.
 *
 * This came from ejb's hsearch.
 */

#define PRIME1		37
#define PRIME2		1048583

static uint32_t
hash1(const void *keyarg, size_t len)
{
	const uint8_t *key;
	uint32_t h;

	/* Convert string to integer */
	for (key = keyarg, h = 0; len--;)
		h = h * PRIME1 ^ (*key++ - ' ');
	h %= PRIME2;
	return (h);
}

/*
 * Phong's linear congruential hash
 */
#define dcharhash(h, c)	((h) = 0x63c63cd9*(h) + 0x9c39c33d + (c))

static uint32_t
hash2(const void *keyarg, size_t len)
{
	const uint8_t *e, *key;
	uint32_t h;
	uint8_t c;

	key = keyarg;
	e = key + len;
	for (h = 0; key != e;) {
		c = *key++;
		if (!c && key > e)
			break;
		dcharhash(h, c);
	}
	return (h);
}

/*
 * This is INCREDIBLY ugly, but fast.  We break the string up into 8 byte
 * units.  On the first time through the loop we get the "leftover bytes"
 * (strlen % 8).  On every other iteration, we perform 8 HASHC's so we handle
 * all 8 bytes.  Essentially, this saves us 7 cmp & branch instructions.  If
 * this routine is heavily used enough, it's worth the ugly coding.
 *
 * OZ's original sdbm hash
 */
static uint32_t
hash3(const void *keyarg, size_t len)
{
	const uint8_t *key;
	size_t loop;
	uint32_t h;

#define HASHC   h = *key++ + 65599 * h

	h = 0;
	key = keyarg;
	if (len > 0) {
		loop = (len + 8 - 1) >> 3;

		switch (len & (8 - 1)) {
		case 0:
			do {
				HASHC;
				/* FALLTHROUGH */
		case 7:
				HASHC;
				/* FALLTHROUGH */
		case 6:
				HASHC;
				/* FALLTHROUGH */
		case 5:
				HASHC;
				/* FALLTHROUGH */
		case 4:
				HASHC;
				/* FALLTHROUGH */
		case 3:
				HASHC;
				/* FALLTHROUGH */
		case 2:
				HASHC;
				/* FALLTHROUGH */
		case 1:
				HASHC;
			} while (--loop);
		}
	}
	return (h);
}
#endif

/* Hash function from Chris Torek. */
static uint32_t
hash4(const void *keyarg, size_t len)
{
	const uint8_t *key;
	size_t loop;
	uint32_t h;

#define HASH4a   h = (h << 5) - h + *key++;
#define HASH4b   h = (h << 5) + h + *key++;
#define HASH4 HASH4b

	h = 0;
	key = keyarg;
	if (len > 0) {
		loop = (len + 8 - 1) >> 3;

		switch (len & (8 - 1)) {
		case 0:
			do {
				HASH4;
				/* FALLTHROUGH */
		case 7:
				HASH4;
				/* FALLTHROUGH */
		case 6:
				HASH4;
				/* FALLTHROUGH */
		case 5:
				HASH4;
				/* FALLTHROUGH */
		case 4:
				HASH4;
				/* FALLTHROUGH */
		case 3:
				HASH4;
				/* FALLTHROUGH */
		case 2:
				HASH4;
				/* FALLTHROUGH */
		case 1:
				HASH4;
			} while (--loop);
		}
	}
	return (h);
}
