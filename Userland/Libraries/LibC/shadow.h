/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <bits/FILE.h>
#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

struct spwd {
    char* sp_namp;
    char* sp_pwdp;
    long int sp_lstchg;
    long int sp_min;
    long int sp_max;
    long int sp_warn;
    long int sp_inact;
    long int sp_expire;
    unsigned long int sp_flag;
};

struct spwd* getspent(void);
void setspent(void);
void endspent(void);
struct spwd* getspnam(char const* name);
int putspent(struct spwd* p, FILE* stream);

int getspent_r(struct spwd* spbuf, char* buf, size_t buflen, struct spwd** spbufp);
int getspnam_r(char const* name, struct spwd* spbuf, char* buf, size_t buflen, struct spwd** spbufp);

int fgetspent_r(FILE* fp, struct spwd* spbuf, char* buf, size_t buflen, struct spwd** spbufp);
int sgetspent_r(char const* s, struct spwd* spbuf, char* buf, size_t buflen, struct spwd** spbufp);

__END_DECLS
