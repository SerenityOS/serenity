/*	$NetBSD: sha1hl.c,v 1.8 2008/10/06 12:36:20 joerg Exp $	*/

/* sha1hl.c
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE" (Revision 42):
 * <phk@login.dkuug.dk> wrote this file.  As long as you retain this notice you
 * can do whatever you want with this stuff. If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.   Poul-Henning Kamp
 * ----------------------------------------------------------------------------
 */

#if 0
#include "namespace.h"
#endif

#include <nbcompat.h>
#include <nbcompat/cdefs.h>
#if HAVE_SYS_FILE_H
#include <sys/file.h>
#endif
#include <nbcompat/types.h>
#if HAVE_SYS_UIO_H
#include <sys/uio.h>
#endif

#include <nbcompat/assert.h>
#if HAVE_ERRNO_H
#include <errno.h>
#endif
#if HAVE_FCNTL_H
#include <fcntl.h>
#endif
#include <nbcompat/sha1.h>
#include <nbcompat/stdio.h>
#include <nbcompat/stdlib.h>
#include <nbcompat/unistd.h>

#if HAVE_NBTOOL_CONFIG_H
#include "nbtool_config.h"
#endif

#if !HAVE_SHA1_H

#if defined(LIBC_SCCS) && !defined(lint)
__RCSID("$NetBSD: sha1hl.c,v 1.8 2008/10/06 12:36:20 joerg Exp $");
#endif /* LIBC_SCCS and not lint */

#if 0
#if defined(__weak_alias)
__weak_alias(SHA1End,_SHA1End)
__weak_alias(SHA1File,_SHA1File)
__weak_alias(SHA1Data,_SHA1Data)
#endif
#endif

/* ARGSUSED */
char *
SHA1End(ctx, buf)
    SHA1_CTX *ctx;
    char *buf;
{
    int i;
    char *p = buf;
    unsigned char digest[20];
    static const char hex[]="0123456789abcdef";

    _DIAGASSERT(ctx != NULL);
    /* buf may be NULL */

    if (p == NULL && (p = malloc(41)) == NULL)
	return 0;

    SHA1Final(digest,ctx);
    for (i = 0; i < 20; i++) {
	p[i + i] = hex[((uint32_t)digest[i]) >> 4];
	p[i + i + 1] = hex[digest[i] & 0x0f];
    }
    p[i + i] = '\0';
    return(p);
}

char *
SHA1File (filename, buf)
    char *filename;
    char *buf;
{
    unsigned char buffer[BUFSIZ];
    SHA1_CTX ctx;
    int fd, num, oerrno;

    _DIAGASSERT(filename != NULL);
    /* XXX: buf may be NULL ? */

    SHA1Init(&ctx);

    if ((fd = open(filename,O_RDONLY)) < 0)
	return(0);

    while ((num = read(fd, buffer, sizeof(buffer))) > 0)
	SHA1Update(&ctx, buffer, (size_t)num);

    oerrno = errno;
    close(fd);
    errno = oerrno;
    return(num < 0 ? 0 : SHA1End(&ctx, buf));
}

char *
SHA1Data (data, len, buf)
    const unsigned char *data;
    size_t len;
    char *buf;
{
    SHA1_CTX ctx;

    _DIAGASSERT(data != NULL);
    /* XXX: buf may be NULL ? */

    SHA1Init(&ctx);
    SHA1Update(&ctx, data, len);
    return(SHA1End(&ctx, buf));
}

#endif /* HAVE_SHA1_H */
