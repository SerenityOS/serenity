/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <mntent.h>

extern "C" {

struct mntent* getmntent([[maybe_unused]] FILE* stream)
{
    // FIXME: Implement getmntent()
    return nullptr;
}

FILE* setmntent([[maybe_unused]] char const* filename, [[maybe_unused]] char const* type)
{
    // FIXME: Implement setmntent()
    return nullptr;
}

int endmntent([[maybe_unused]] FILE* stream)
{
    // FIXME: Implement endmntent()
    return 1;
}

struct mntent* getmntent_r([[maybe_unused]] FILE* stream, [[maybe_unused]] struct mntent* mntbuf, [[maybe_unused]] char* buf, [[maybe_unused]] int buflen)
{
    // FIXME: Implement getmntent_r()
    return nullptr;
}
}
