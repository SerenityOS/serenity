/*
 * Copyright (c) 2021, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <langinfo.h>

static const char* __nl_langinfo(nl_item item)
{
    switch (item) {
    case CODESET:
        return "UTF-8";
    default:
        return "";
    }
}

extern "C" {

char* nl_langinfo(nl_item item)
{
    // POSIX states that returned strings should not be modified,
    // so this cast is probably fine.
    return const_cast<char*>(__nl_langinfo(item));
}
}
