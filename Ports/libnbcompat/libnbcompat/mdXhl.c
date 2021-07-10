/*	$NetBSD: mdXhl.c,v 1.2 2008/10/06 12:36:20 joerg Exp $	*/

/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE" (Revision 42):
 * <phk@login.dkuug.dk> wrote this file.  As long as you retain this notice you
 * can do whatever you want with this stuff. If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.   Poul-Henning Kamp
 * ----------------------------------------------------------------------------
 *
 * from FreeBSD Id: mdXhl.c,v 1.8 1996/10/25 06:48:12 bde Exp
 */

/*
 * Modifed April 29, 1997 by Jason R. Thorpe <thorpej@NetBSD.org>
 */

#include <nbcompat.h>
#include <nbcompat/types.h>

#include <nbcompat/assert.h>
#if HAVE_FCNTL_H
#include <fcntl.h>
#endif
#if HAVE_ERRNO_H
#include <errno.h>
#endif
#include <nbcompat/stdio.h>
#include <nbcompat/stdlib.h>
#include <nbcompat/unistd.h>

#define	CONCAT(x,y)	__CONCAT(x,y)
#define	MDNAME(x)	CONCAT(MDALGORITHM,x)

char *
MDNAME(End)(ctx, buf)
	MDNAME(_CTX) *ctx;
	char *buf;
{
	int i;
	unsigned char digest[16];
	static const char hex[]="0123456789abcdef";

	_DIAGASSERT(ctx != 0);

	if (buf == NULL)
		buf = malloc(33);
	if (buf == NULL)
		return (NULL);

	MDNAME(Final)(digest, ctx);

	for (i = 0; i < 16; i++) {
		buf[i+i] = hex[(uint32_t)digest[i] >> 4];
		buf[i+i+1] = hex[digest[i] & 0x0f];
	}

	buf[i+i] = '\0';
	return (buf);
}

char *
MDNAME(File)(filename, buf)
	const char *filename;
	char *buf;
{
	unsigned char buffer[BUFSIZ];
	MDNAME(_CTX) ctx;
	int f, i, j;

	_DIAGASSERT(filename != 0);
	/* buf may be NULL */

	MDNAME(Init)(&ctx);
	f = open(filename, O_RDONLY, 0666);
	if (f < 0)
		return NULL;

	while ((i = read(f, buffer, sizeof(buffer))) > 0)
		MDNAME(Update)(&ctx, buffer, (unsigned int)i);

	j = errno;
	close(f);
	errno = j;

	if (i < 0)
		return NULL;

	return (MDNAME(End)(&ctx, buf));
}

char *
MDNAME(Data)(data, len, buf)
	const unsigned char *data;
	unsigned int len;
	char *buf;
{
	MDNAME(_CTX) ctx;

	_DIAGASSERT(data != 0);

	MDNAME(Init)(&ctx);
	MDNAME(Update)(&ctx, data, len);
	return (MDNAME(End)(&ctx, buf));
}
