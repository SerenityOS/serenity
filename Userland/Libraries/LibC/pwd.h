/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <bits/FILE.h>
#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

struct passwd {
    char* pw_name;
    char* pw_passwd;
    uid_t pw_uid;
    gid_t pw_gid;
    char* pw_gecos;
    char* pw_dir;
    char* pw_shell;
};

struct passwd* getpwent(void);
int getpwent_r(struct passwd*, char*, size_t, struct passwd**);
void setpwent(void);
void endpwent(void);
struct passwd* getpwnam(char const* name);
struct passwd* getpwuid(uid_t);
int putpwent(const struct passwd* p, FILE* stream);

int getpwnam_r(char const* name, struct passwd* pwd, char* buf, size_t buflen, struct passwd** result);
int getpwuid_r(uid_t, struct passwd* pwd, char* buf, size_t buflen, struct passwd** result);

__END_DECLS
