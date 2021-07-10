/*	$NetBSD: md5hl.c,v 1.7 2004/08/23 03:32:12 jlam Exp $	*/

/*
 * Written by Jason R. Thorpe <thorpej@NetBSD.org>, April 29, 1997.
 * Public domain.
 */

#define	MDALGORITHM	MD5

#if 0
#include "namespace.h"
#endif
#include <nbcompat.h>
#include <nbcompat/md5.h>

#if HAVE_NBTOOL_CONFIG_H
#include "nbtool_config.h"
#endif

#if !HAVE_MD5_H
#include "mdXhl.c"
#endif
