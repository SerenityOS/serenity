/*	$NetBSD: rmd160hl.c,v 1.8 2008/10/06 12:36:20 joerg Exp $	*/

/* rmd160hl.c
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE" (Revision 42):
 * <phk@login.dkuug.dk> wrote this file.  As long as you retain this notice you
 * can do whatever you want with this stuff. If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.   Poul-Henning Kamp
 * ----------------------------------------------------------------------------
 *
 * from OpenBSD: rmd160hl.c,v 1.2 1999/08/17 09:13:12 millert Exp $
 */  

#if HAVE_NBTOOL_CONFIG_H
#include "nbtool_config.h"
#endif

#include <nbcompat.h>
#include <nbcompat/cdefs.h>
#ifndef lint
__RCSID("$NetBSD: rmd160hl.c,v 1.8 2008/10/06 12:36:20 joerg Exp $");
#endif	/* not lint */

#include <nbcompat/types.h>

#if 0
#include "namespace.h"
#endif

#include <nbcompat/assert.h>
#if HAVE_ERRNO_H
#include <errno.h>
#endif
#if HAVE_FCNTL_H
#include <fcntl.h>
#endif
#include <nbcompat/rmd160.h>
#include <nbcompat/stdio.h>
#include <nbcompat/stdlib.h>
#include <nbcompat/unistd.h>

#if !HAVE_RMD160_H

#if 0
#if defined(__weak_alias)
__weak_alias(RMD160End,_RMD160End)
__weak_alias(RMD160File,_RMD160File)
__weak_alias(RMD160Data,_RMD160Data)
#endif
#endif

char *
RMD160End(RMD160_CTX *ctx, char *buf)
{
    int i;
    char *p = buf;
    unsigned char digest[20];
    static const char hex[]="0123456789abcdef";

    _DIAGASSERT(ctx != NULL);
    /* buf may be NULL */

    if (p == NULL && (p = malloc(41)) == NULL)
	return 0;

    RMD160Final(digest,ctx);
    for (i = 0; i < 20; i++) {
	p[i + i] = hex[(uint32_t)digest[i] >> 4];
	p[i + i + 1] = hex[digest[i] & 0x0f];
    }
    p[i + i] = '\0';
    return(p);
}

char *
RMD160File(char *filename, char *buf)
{
    unsigned char buffer[BUFSIZ];
    RMD160_CTX ctx;
    int fd, num, oerrno;

    _DIAGASSERT(filename != NULL);
    /* XXX: buf may be NULL ? */

    RMD160Init(&ctx);

    if ((fd = open(filename, O_RDONLY)) < 0)
	return(0);

    while ((num = read(fd, buffer, sizeof(buffer))) > 0)
	RMD160Update(&ctx, buffer, (size_t)num);

    oerrno = errno;
    close(fd);
    errno = oerrno;
    return(num < 0 ? 0 : RMD160End(&ctx, buf));
}

char *
RMD160Data(const unsigned char *data, size_t len, char *buf)
{
    RMD160_CTX ctx;

    _DIAGASSERT(data != NULL);
    /* XXX: buf may be NULL ? */

    RMD160Init(&ctx);
    RMD160Update(&ctx, data, len);
    return(RMD160End(&ctx, buf));
}

#endif /* HAVE_RMD160_H */
