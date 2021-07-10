/*	$NetBSD: nbcompat.h,v 1.43 2009/04/13 11:30:46 joerg Exp $	*/

/*-
 * Copyright (c) 2003 The NetBSD Foundation, Inc.
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
 *
 * THIS SOFTWARE IS PROVIDED BY THE NETBSD FOUNDATION, INC. AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

#include <nbcompat/nbconfig.h>

#include <nbcompat/cdefs.h>
#include <nbcompat/types.h>
#include <nbcompat/limits.h>
#include <nbcompat/endian.h>
#include <nbcompat/param.h>

#if HAVE_ERRNO_H
#include <errno.h>
#endif

#if HAVE_STDARG_H
#include <stdarg.h>
#endif

#if HAVE_STDDEF_H
#include <stddef.h>
#endif

#include <nbcompat/assert.h>
#include <nbcompat/ctype.h>
#include <nbcompat/dirent.h>
#include <nbcompat/err.h>
#include <nbcompat/fnmatch.h>
#include <nbcompat/grp.h>
#include <nbcompat/paths.h>
#include <nbcompat/pwd.h>
#include <nbcompat/queue.h>
#include <nbcompat/stat.h>
#include <nbcompat/statvfs.h>
#include <nbcompat/stdlib.h>
#include <nbcompat/stdio.h>
#include <nbcompat/string.h>
#include <nbcompat/termcap.h>
#include <nbcompat/time.h>
#include <nbcompat/tzfile.h>
#include <nbcompat/unistd.h>
#include <nbcompat/util.h>

#if HAVE_NBCOMPAT_POLL
# if HAVE_POLL_H
#  undef HAVE_POLL_H
# endif
# if HAVE_SYS_POLL_H
#  undef HAVE_SYS_POLL_H
# endif
# include <nbcompat/poll.h>
#endif

#if HAVE_NBCOMPAT_FTS
# if HAVE_FTS_H
#  undef HAVE_FTS_H
# endif
# include <nbcompat/fts.h>
#endif

#if HAVE_NBCOMPAT_GLOB
# if HAVE_GLOB_H
#  undef HAVE_GLOB_H
# endif
# include <nbcompat/glob.h>
#endif

#if HAVE_NBCOMPAT_REGEX
# if HAVE_REGEX_H
#  undef HAVE_REGEX_H
# endif
# include <nbcompat/regex.h>
#endif

#if HAVE_NBCOMPAT_MD5INIT
# if HAVE_MD5_H
#  undef HAVE_MD5_H
# endif
#endif

#if HAVE_NBCOMPAT_MD5
# if HAVE_MD5_H
#  undef HAVE_MD5_H
# endif
#endif

#if HAVE_NBCOMPAT_RMD160
# if HAVE_RMD160_H
#  undef HAVE_RMD160_H
# endif
#endif

#if HAVE_NBCOMPAT_SHA1
# if HAVE_SHA1_H
#  undef HAVE_SHA1_H
# endif
#endif

#if HAVE_NBCOMPAT_VIS
# if HAVE_VIS_H
#  undef HAVE_VIS_H
# endif
# include <nbcompat/vis.h>
#endif

#if !HAVE_GETOPT_H || !HAVE_STRUCT_OPTION
# undef HAVE_GETOPT_H
# include <nbcompat/getopt.h>
#endif
