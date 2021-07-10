/*	$NetBSD: statvfs.h,v 1.6 2015/12/11 23:28:10 ryoon Exp $	*/

/*-
 * Copyright (c) 2004 The NetBSD Foundation, Inc.
 * All rights reserved.
 *
 * This code is derived from software contributed to The NetBSD Foundation
 * by Christos Zoulas.
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

#ifndef _NBCOMPAT_STATVFS_H_
#define _NBCOMPAT_STATVFS_H_

#if HAVE_SYS_STATVFS_H
# include <sys/statvfs.h>
#endif

#if !HAVE_STATVFS

#include <nbcompat/types.h>
#include <nbcompat/param.h>

#if HAVE_SYS_MOUNT_H
#include <sys/mount.h>
#endif

#if HAVE_SYS_STATFS_H
#include <sys/statfs.h>
#endif

#if HAVE_SYS_VFS_H
#include <sys/vfs.h>
#endif

#if HAVE_STDINT_H
#include <stdint.h>
#endif

#define VFS_NAMELEN	32
#define VFS_MNAMELEN	1024

#if !defined(fsblkcnt_t)
typedef uint64_t	fsblkcnt_t;	/* fs block count (statvfs) */
#endif
#if !defined(fsfilcnt_t)
typedef uint64_t	fsfilcnt_t;	/* fs file count */
#endif

#if !HAVE_FSID_T
typedef struct { int32_t val[2]; } fsid_t;
#endif

struct statvfs {
	unsigned long	f_flag;		/* copy of mount exported flags */
	unsigned long	f_bsize;	/* system block size */
	unsigned long	f_frsize;	/* system fragment size */
	unsigned long	f_iosize;	/* optimal file system block size */

	fsblkcnt_t	f_blocks;	/* number of blocks in file system */
	fsblkcnt_t	f_bfree;	/* free blocks avail in file system */
	fsblkcnt_t	f_bavail;	/* free blocks avail to non-root */
	fsblkcnt_t	f_bresvd;	/* blocks reserved for root */

	fsfilcnt_t	f_files;	/* total file nodes in file system */
	fsfilcnt_t	f_ffree;	/* free file nodes in file system */
	fsfilcnt_t	f_favail;	/* free file nodes avail to non-root */
	fsfilcnt_t	f_fresvd;	/* file nodes reserved for root */

	uint64_t	f_syncreads;	/* count of sync reads since mount */
	uint64_t	f_syncwrites;	/* count of sync writes since mount */

	uint64_t	f_asyncreads;	/* count of async reads since mount */
	uint64_t	f_asyncwrites;	/* count of async writes since mount */

	fsid_t		f_fsidx;	/* NetBSD compatible fsid */
	unsigned long	f_fsid;		/* Posix compatible fsid */
	unsigned long	f_namemax;	/* maximum filename length */
	uint32_t	f_owner;	/* user that mounted the file system */

	uint32_t	f_spare[4];	/* spare space */

	char	f_fstypename[VFS_NAMELEN]; /* fs type name */
	char	f_mntonname[VFS_MNAMELEN];  /* directory on which mounted */
	char	f_mntfromname[VFS_MNAMELEN];  /* mounted file system */
};

#ifndef MNT_RDONLY
#define MNT_RDONLY	0x00000001	/* read only filesystem */
#endif
#ifndef ST_RDONLY
#define ST_RDONLY	MNT_RDONLY
#endif

#ifndef MNT_NOSUID
#define MNT_NOSUID	0x00000008	/* don't honor setuid bits on fs */
#endif
#ifndef ST_NOSUID
#define ST_NOSUID	MNT_NOSUID
#endif

int	statvfs(const char *path, struct statvfs *vfs);
int	fstatvfs(int fd, struct statvfs *vfs);

#endif /* !HAVE_STATVFS */

#endif /* !_NBCOMPAT_STATVFS_H_ */
