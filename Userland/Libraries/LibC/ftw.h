/*
 * Copyright (c) 2022, Sviatoslav Peleshko <speles@mail.ua>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/stat.h>

__BEGIN_DECLS

struct FTW {
    int base;
    int level;
};

#define FTW_F 1   /* File. */
#define FTW_D 2   /* Directory. */
#define FTW_DNR 3 /* Directory without read permission. */
#define FTW_DP 4  /* Directory with subdirectories visited. */
#define FTW_NS 5  /* Unknown type, stat() failed. */
#define FTW_SL 6  /* Symbolic link. */
#define FTW_SLN 7 /* Symbolic link that names a non-existent file. */

#define FTW_PHYS (1 << 0)  /* Physical walk, does not follow symbolic links. Otherwise, nftw() will follow links but will not walk down any path that crosses itself. */
#define FTW_MOUNT (1 << 1) /* The walk will not cross a mount point. */
#define FTW_DEPTH (1 << 2) /* All subdirectories will be visited before the directory itself. */
#define FTW_CHDIR (1 << 3) /* The walk will change to each directory before reading it. */

int ftw(const char* dirpath, int (*fn)(const char* fpath, const struct stat* sb, int typeflag), int nopenfd);
int nftw(const char* dirpath, int (*fn)(const char* fpath, const struct stat* sb, int typeflag, struct FTW* ftwbuf), int nopenfd, int flags);

__END_DECLS
