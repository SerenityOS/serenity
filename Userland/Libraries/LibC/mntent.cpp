/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <assert.h>
#include <mntent.h>

extern "C" {

struct mntent* getmntent(FILE*)
{
    dbgln("FIXME: Implement getmntent()");
    return nullptr;
}

FILE* setmntent(char const*, char const*)
{
    dbgln("FIXME: Implement setmntent()");
    return nullptr;
}

int endmntent(FILE*)
{
    dbgln("FIXME: Implement endmntent()");
    return 0;
}

struct mntent* getmntent_r(FILE*, struct mntent*, char*, int)
{
    dbgln("FIXME: Implement getmntent_r()");
    return 0;
}
}
