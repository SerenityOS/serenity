/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

#define RTLD_DEFAULT 0
#define RTLD_LAZY 2
#define RTLD_NOW 4
#define RTLD_GLOBAL 8
#define RTLD_LOCAL 16

typedef struct __Dl_info {
    char const* dli_fname;
    void* dli_fbase;
    char const* dli_sname;
    void* dli_saddr;
} Dl_info;

int dlclose(void*);
char* dlerror(void);
void* dlopen(char const*, int);
void* dlsym(void*, char const*);
int dladdr(void const*, Dl_info*);

__END_DECLS
