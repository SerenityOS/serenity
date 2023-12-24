/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Maxime Friess <M4x1me@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <bits/FILE.h>
#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

struct group {
    char* gr_name;
    char* gr_passwd;
    gid_t gr_gid;
    char** gr_mem;
};

struct group* getgrent(void);
int getgrent_r(struct group* group_buf, char* buffer, size_t buffer_size, struct group** group_entry_ptr);
void setgrent(void);
void endgrent(void);
struct group* getgrnam(char const* name);
int getgrnam_r(char const* name, struct group* group_buf, char* buffer, size_t buffer_size, struct group** group_entry_ptr);
struct group* getgrgid(gid_t);
int getgrgid_r(gid_t gid, struct group* group_buf, char* buffer, size_t buffer_size, struct group** group_entry_ptr);
int putgrent(const struct group*, FILE*);

int initgroups(char const* user, gid_t);

__END_DECLS
