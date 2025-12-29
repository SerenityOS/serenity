/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <fnmatch.h>

int fnmatch(char const*, char const*, int)
{
    // FIXME: Implement fnmatch()
    return FNM_NOMATCH;
}
