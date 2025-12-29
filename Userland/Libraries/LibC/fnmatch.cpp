/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <fnmatch.h>

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fnmatch.html
int fnmatch([[maybe_unused]] char const* pattern, [[maybe_unused]] char const* string, [[maybe_unused]] int flags)
{
    // FIXME: Implement fnmatch()
    // Returning FNM_NOMATCH is safer than returning 0 (match) for unimplemented function
    return FNM_NOMATCH;
}
