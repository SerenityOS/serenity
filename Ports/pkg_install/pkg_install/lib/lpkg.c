/*	$NetBSD: lpkg.c,v 1.6 2009/02/02 12:35:01 joerg Exp $	*/

/*
 * Copyright (c) 1999 Christian E. Hopps
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Package-list auxiliary functions
 */

#if HAVE_CONFIG_H
#include "config.h"
#endif
#include <nbcompat.h>
#if HAVE_ERR_H
#include <err.h>
#endif
#include "lib.h"

/*
 * Add a package to the (add/recursive delete) list
 */
lpkg_t *
alloc_lpkg(const char *pkgname)
{
	lpkg_t *lpp;

	lpp = xmalloc(sizeof(*lpp));
	lpp->lp_name = xstrdup(pkgname);
	return (lpp);
}

void
free_lpkg(lpkg_t *lpp)
{
	free(lpp->lp_name);
	free(lpp);
}

lpkg_t *
find_on_queue(lpkg_head_t *qp, const char *name)
{
	lpkg_t *lpp;

	for (lpp = TAILQ_FIRST(qp); lpp; lpp = TAILQ_NEXT(lpp, lp_link))
		if (!strcmp(name, lpp->lp_name))
			return (lpp);
	return (0);
}
