/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

#define FNM_NOMATCH 1
#define FNM_PATHNAME 1
#define FNM_NOESCAPE 2
#define FNM_PERIOD 4
#define FNM_FILE_NAME FNM_PATHNAME
#define FNM_LEADING_DIR 8
#define FNM_CASEFOLD 16
#define FNM_EXTMATCH 32

__BEGIN_DECLS

int fnmatch(char const* pattern, char const* string, int flags);

__END_DECLS
