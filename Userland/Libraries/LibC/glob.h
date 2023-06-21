/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

typedef struct {
    size_t gl_pathc;
    char** gl_pathv;
    size_t gl_offs;
} glob_t;

#define GLOB_APPEND (1 << 0)
#define GLOB_DOOFS (1 << 1)
#define GLOB_ERR (1 << 2)
#define GLOB_MARK (1 << 3)
#define GLOB_NOCHECK (1 << 4)
#define GLOB_NOESCAPE (1 << 5)
#define GLOB_NOSORT (1 << 6)

#define GLOB_ABORTED 1
#define GLOB_NOMATCH 2
#define GLOB_NOSPACE 3

int glob(char const* pattern, int flags, int (*errfunc)(char const* epath, int eerrno), glob_t* pglob);

void globfree(glob_t* pglob);

__END_DECLS
