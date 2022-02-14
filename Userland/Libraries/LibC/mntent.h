/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <stdio.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

#define MOUNTED "/etc/mtab"
#define MNTTAB "/etc/fstab"

struct mntent {
    char* mnt_fsname;
    char* mnt_dir;
    char* mnt_type;
    char* mnt_opts;
    int mnt_freq;
    int mnt_passno;
};

#define MNTTYPE_IGNORE "ignore"
#define MNTTYPE_NFS    "nfs"
#define MNTTYPE_SWAP   "swap"

#define MNTOPT_DEFAULTS "defaults"
#define MNTOPT_RO       "ro"
#define MNTOPT_RW       "rw"
#define MNTOPT_SUID     "suid"
#define MNTOPT_NOSUID   "nosuid"
#define MNTOPT_NOAUTO   "noauto"

struct mntent* getmntent(FILE* stream);
FILE* setmntent(char const* filename, char const* type);
int addmntent(FILE* __restrict__ stream, const struct mntent* __restrict__ mnt);
int endmntent(FILE* streamp);
struct mntent* getmntent_r(FILE* streamp, struct mntent* mntbuf, char* buf, int buflen);

__END_DECLS
