/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/POSIX/dirent.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

struct dirent {
    ino_t d_ino;
    off_t d_off;
    unsigned short d_reclen;
    unsigned char d_type;
    char d_name[256];
};

struct __DIR {
    int fd;
    struct dirent cur_ent;
    char* buffer;
    size_t buffer_size;
    char* nextptr;
};
typedef struct __DIR DIR;

DIR* fdopendir(int fd);
DIR* opendir(char const* name);
int closedir(DIR*);
void rewinddir(DIR*);
struct dirent* readdir(DIR*);
int readdir_r(DIR*, struct dirent*, struct dirent**);
int dirfd(DIR*);

int alphasort(const struct dirent** d1, const struct dirent** d2);
int scandir(char const* dirp, struct dirent*** namelist,
    int (*filter)(const struct dirent*),
    int (*compar)(const struct dirent**, const struct dirent**));

__END_DECLS
