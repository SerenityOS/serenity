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

struct mntent* getmntent(FILE* stream);
FILE* setmntent(char const* filename, char const* type);
int endmntent(FILE* streamp);
struct mntent* getmntent_r(FILE* streamp, struct mntent* mntbuf, char* buf, int buflen);

__END_DECLS
